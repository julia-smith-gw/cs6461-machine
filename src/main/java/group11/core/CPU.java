package group11.core;

import java.io.IOException;

import group11.core.RomLoader.LoadException;
import group11.events.EventBus;
import group11.events.GPRChanged;
import group11.events.IRChanged;
import group11.events.IXRChanged;
import group11.events.MARChanged;
import group11.events.MBRChanged;
import group11.events.MessageChanged;
import group11.events.PCChanged;
import group11.events.SetGPR;
import group11.events.SetIXR;
import group11.events.SetMAR;
import group11.events.SetMBR;
import group11.events.SetPC;
import javax.swing.Timer;
import java.nio.file.Path;

// assisted by chatgpt
public class CPU implements AutoCloseable {

    // event bus and subscriptions
    private final EventBus bus;

    private final Cache cache;

    private final AutoCloseable SetGPRSub;
    private final AutoCloseable SetIXRSub;
    private final AutoCloseable SetPCSub;
    private final AutoCloseable SetMARSub;
    private final AutoCloseable SetMBRSub;

    // cpu running information
    public int[] GPR = new int[4]; // R0-R3
    public int[] IXR = new int[4]; // X1-X3
    public Integer PC = null; // Program Counter
    public Integer IR = null; // Instruction Register

    public boolean running = false;
    public Memory memory;
    public RomLoader romLoader;

    public int pendingInstructions;
    public int completedInstructions;

    public int effectiveAddress;
    public int R;
    public int IX;
    public int I;
    public int opcode;
    public String message;
    public boolean[] CC = new boolean[4]; // CC[0]=OVERFLOW, CC[1]=UNDERFLOW, CC[2]=DIVZERO, CC[3]=EQUALORNOT
    private int carry = 0; // 0 or 1

    public CPU(Memory memory, EventBus bus, Cache cache, RomLoader romLoader) {
        this.memory = memory;
        this.bus = bus;
        this.romLoader = romLoader;
        this.cache = cache;

        // initialize listeners on input fields to watch for changes
        this.SetGPRSub = bus.subscribe(SetGPR.class, cmd -> {
            this.GPR[cmd.GPRNum()] = cmd.value();
        });
        this.SetIXRSub = bus.subscribe(SetIXR.class, cmd -> {
            this.IXR[cmd.IXRNum()] = cmd.value();
        });
        this.SetMARSub = bus.subscribe(SetMAR.class, cmd -> {
            this.memory.MAR = cmd.value();
        });
        this.SetMBRSub = bus.subscribe(SetMBR.class, cmd -> {
            this.memory.MBR = cmd.value();
        });
        this.SetPCSub = bus.subscribe(SetPC.class, cmd -> {
            this.PC = cmd.value();
        });
    }

    /**
     * fetches next instruction from memory when running or stepping through a file
     */
    private void fetch() {
        // fetch instruction from memory and read into IR
        this.memory.writeMARAddress(PC); // Send address to memory
        this.memory.readMemory(PC); // Memory loads MBR
        this.IR = memory.readMBR(); // Place into IR
        this.bus.post(new PCChanged(this.PC));
        this.PC++;
        this.bus.post(new IRChanged(this.IR));

    }

    /**
     * Allows user to load the value of an arbitrary address outside of run
     */
    public void loadFrontPanel() {
        try {
            if (this.memory.MAR == null) {
                this.bus.post(new MessageChanged("MAR must be defined to use the load button."));
                return;
            }
            memory.readMemory(this.memory.MAR);
            this.bus.post(new MessageChanged(
                    "Value " + this.memory.MBR + " was previously stored at address " + this.memory.MAR + "."));
            this.bus.post(new MBRChanged(this.memory.MBR));
        } catch (Exception e) {
            this.bus.post(new MessageChanged(e.getMessage()));
        }
    }

    /**
     * Allows user to store their entered MBR into an arbitrary address outside of
     * run
     */
    public void storeFrontPanel() {

        try {
            if (this.memory.MAR == null || this.memory.MBR == null) {
                this.bus.post(new MessageChanged("MAR and MBR must be defined to use the store button."));
                return;
            }

            memory.writeMemory(this.memory.MAR, this.memory.MBR);
            this.bus.post(new MessageChanged("MBR " + this.memory.MBR + " stored at address " + this.memory.MAR + "."));
        } catch (Exception e) {
            this.bus.post(new MessageChanged(e.getMessage()));
        }

    }

    /**
     * Steps through consecutive addresses starting at PC for executing loaded
     * instructions one at a time
     */
    public void step() {
        int instrCount = completedInstructions + 1;
        this.bus.post(new MessageChanged("Executing instruction " + instrCount + ". "
                + "PC: " + this.PC));
        fetch();
        decodeAndExecute();
        this.completedInstructions++;
    }

    /**
     * Allows user to load the value of an arbitrary address outside of run.
     * Increments MAR to next address location after load.
     */
    public void loadPlus() {
        try {
            if (this.memory.MAR == null) {
                this.bus.post(new MessageChanged("MAR must be defined to use loadPlus."));
                return;
            }
            memory.readMemory(this.memory.MAR);
            // update +1 MAR before load plus is read again if applicable
            this.bus.post(new MARChanged(this.memory.MAR));
            this.bus.post(new MBRChanged(this.memory.MBR));
            this.bus.post(new MessageChanged("Value of " + this.memory.MBR + " was stored at address " + this.memory.MAR
                    + ".New MAR at " + this.memory.MAR + 1));
            memory.writeMARAddress(this.memory.MAR + 1);
        } catch (Exception e) {
            this.bus.post(new MessageChanged(e.getMessage()));
        }
    }

    /*
     * Allows user to store their entered MBR into an arbitrary address outside of
     * run
     * Increments MAR to next address after store is complete.
     */
    public void storePlus() {
        try {
            if (this.memory.MAR == null || this.memory.MBR == null) {
                this.bus.post(new MessageChanged("MAR and MBR must be defined to use storePlus."));
                return;
            }
            memory.writeMemory(this.memory.MAR, this.memory.MBR);
            this.bus.post(new MessageChanged("MBR value of " + this.memory.MBR + " stored at address " + this.memory.MAR
                    + ". New MAR at " + this.memory.MAR + 1));
            int newMAR = memory.readMARAddress() + 1;
            memory.writeMARAddress(newMAR);
            this.memory.MBR = null;
            this.bus.post(new MARChanged(this.memory.MAR));
            this.bus.post(new MBRChanged(this.memory.MBR));
        } catch (Exception e) {
            this.bus.post(new MessageChanged(e.getMessage()));
        }
    }

    /**
     * Op code LDX. Loads index register value from memory.
     */
    public void loadIndexRegister() {
        int value = cache.load(effectiveAddress) & 0xFFFF;

        // Update simulator-visible memory registers for listeners
        this.memory.MAR = effectiveAddress;
        this.memory.MBR = value;
        this.bus.post(new MARChanged(this.memory.MAR));
        this.bus.post(new MBRChanged(this.memory.MBR));
        IXR[IX] = value;
        this.bus.post(new IXRChanged(R, IXR[IX]));
    }

    /**
     * Op code STX. Stores index register value into memory.
     */
    public void storeIndexRegister() {
        cache.store(effectiveAddress, IXR[IX] & 0xFFFF);

        // Keep simulator-visible memory registers consistent
        this.memory.MAR = effectiveAddress;
        this.memory.MBR = IXR[IX] & 0xFFFF;
        this.bus.post(new MARChanged(this.memory.MAR));
        this.bus.post(new MBRChanged(this.memory.MBR));
    }

    /**
     * Gets if effective address is valid
     * 
     * @param ignoreReserved          Whether we should ignore reserved memory
     *                                addresses for
     *                                validation (user can mess with reserved memory
     *                                outside
     *                                of run).
     * @param pendingEffectiveAddress address to check
     * @return true if valid or throws an error if not
     */
    private boolean getIsValidAddress(Boolean ignoreReserved, int pendingEffectiveAddress) {
        if (pendingEffectiveAddress >= this.memory.MEMORY_SIZE
                || pendingEffectiveAddress < 0) {
            throw new IllegalArgumentException("Memory out of bounds access");
        }
        if (!!ignoreReserved && (pendingEffectiveAddress >= 0 && pendingEffectiveAddress <= 5)) {
            throw new IllegalArgumentException(
                    "Illegal memory register access at effective address " + pendingEffectiveAddress);
        }
        return true;
    }

    /**
     * Sets effective address given load/store arguments.
     * If effective address is invalid, an error will be thrown and execution will
     * cease.
     * 
     * @param opcode octal opcode of instruction
     */
    private void setEffectiveAddress(int opcode) {
        int[] pendingEffectiveAddressInfo = calculateEffectiveAddress(opcode);
        this.effectiveAddress = pendingEffectiveAddressInfo[3];
        R = pendingEffectiveAddressInfo[1];
        IX = pendingEffectiveAddressInfo[0];
        I = pendingEffectiveAddressInfo[2];
    }

    private int[] calculateEffectiveAddress(int opcode) {
        int pendingEffectiveAddress = 0;
        int ix = (IR >> 8) & 0x03; // [9..8] IX (2 bits)
        int r = (IR >> 6) & 0x03; // [7..6] R (2 bits)
        int i = (IR >> 5) & 0x01; // [5] I
        pendingEffectiveAddress = IR & 0x1F;
        if (ix != 0) {
            if (ix < 1 || ix > 3) {
                throw new IllegalArgumentException("Bad index register in instruction");
            }
            pendingEffectiveAddress += IXR[ix];
        }

        if (r < 0 || r > 3) {
            throw new IllegalArgumentException("Bad general purpose register. in instruction. GPR can be 0, 1, 2, 3");
        }

        if (i == 1) {
            // check bounds before reading
            try {
                getIsValidAddress(running, pendingEffectiveAddress);
                memory.readMemory(pendingEffectiveAddress);
                pendingEffectiveAddress = memory.readMBR();
            } catch (Exception e) {
                throw e;
            }
        }

        // check returned address if indirect or if calculated based on index register
        // above
        try {
            getIsValidAddress(running, pendingEffectiveAddress);
        } catch (Exception e) {
            throw e;
        }

        int[] result = { ix, r, i, pendingEffectiveAddress };
        return result;

    }

    // helpers for logical/shift/rotate
    private int mask16(int v) {
        return v & 0xFFFF;
    }

    private int rotLeft16(int v, int c) {
        c = c & 15;
        return mask16((v << c) | ((v & 0xFFFF) >>> (16 - c)));
    }

    private int rotRight16(int v, int c) {
        c = c & 15;
        return mask16(((v & 0xFFFF) >>> c) | (v << (16 - c)));
    }

    /**
     * Bit-field accessors for SRC/RRC
     */
    private int srFieldR(int ir) {
        return (ir >> 8) & 0x03;
    }

    private int srFieldAL(int ir) {
        return (ir >> 7) & 0x01;
    }

    private int srFieldLR(int ir) {
        return (ir >> 6) & 0x01;
    }

    private int srFieldCount(int ir) {
        return (ir >> 2) & 0x0F;
    }

    private int getCarry() {
        return (carry != 0) ? 1 : 0;
    }

    private void setCarry(int c) {
        carry = (c != 0) ? 1 : 0;
    }

    /**
     * Decodes and executes instruction from file.
     */
    private void decodeAndExecute() {
        // case to ensure we don't mix halt with just data
        if (IR == 0) {
            halt();
        } else {
            opcode = (IR >> 10) & 0x3F; // Top 6 bits

            switch (opcode) {
                // LDR
                case 01: {
                    try {
                        this.setEffectiveAddress(opcode);
                        // Read from cache (which will load the block on miss)
                        int word = cache.load(effectiveAddress) & 0xFFFF; // ensure 16-bit

                        // Update simulator-visible memory registers so bus events are correct
                        this.memory.MAR = effectiveAddress;
                        this.memory.MBR = word;

                        // Post bus events just like before
                        this.bus.post(new MARChanged(this.memory.MAR));
                        this.bus.post(new MBRChanged(this.memory.MBR));
                        // memory.readMemory(effectiveAddress);
                        // this.bus.post(new MARChanged(this.memory.MAR));
                        // this.bus.post(new MBRChanged(this.memory.MBR));
                        GPR[R] = memory.readMBR();
                        this.bus.post(new GPRChanged(R, GPR[R]));
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        this.bus.post(new MessageChanged(e.getMessage()));
                        this.halt();
                    }
                    break;
                }
                // STR
                case 02: {
                    try {
                        this.setEffectiveAddress(opcode);
                        // Update cache
                        cache.store(effectiveAddress, GPR[R] & 0xFFFF);

                        // Keep simulator-visible memory registers consistent for bus listeners
                        this.memory.MAR = effectiveAddress;
                        this.memory.MBR = GPR[R] & 0xFFFF;

                        this.bus.post(new MARChanged(this.memory.MAR));
                        this.bus.post(new MBRChanged(this.memory.MBR));
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        this.bus.post(new MessageChanged(e.getMessage()));
                        this.halt();
                    }
                    break;
                }
                // LDA
                case 03: {
                    try {
                        int[] addressInfo = this.calculateEffectiveAddress(opcode);
                        GPR[addressInfo[1]] = addressInfo[3];
                        R = addressInfo[1];
                        effectiveAddress = addressInfo[3];
                        this.bus.post(new GPRChanged(addressInfo[1], GPR[addressInfo[1]]));
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        this.bus.post(new MessageChanged(e.getMessage()));
                        this.halt();
                    }
                    break;
                }
                // LDX x, address[,I]
                case 041: {
                    try {
                        this.setEffectiveAddress(opcode);
                        loadIndexRegister();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        this.bus.post(new MessageChanged(e.getMessage()));
                        this.halt();
                    }
                    break;
                }
                // STX x, address[,I]
                case 042: {
                    try {
                        this.setEffectiveAddress(opcode);
                        storeIndexRegister();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        this.bus.post(new MessageChanged(e.getMessage()));
                        this.halt();
                    }
                    break;
                }
                case 04: { // AMR r,x,address[,I]
                    try {
                        this.setEffectiveAddress(opcode);
                        // Read via cache (fills block on miss)
                        int memValue = cache.load(effectiveAddress) & 0xFFFF;

                        // Keep MAR/MBR consistent for bus listeners
                        this.memory.MAR = effectiveAddress;
                        this.memory.MBR = memValue;
                        this.bus.post(new MARChanged(this.memory.MAR));
                        this.bus.post(new MBRChanged(this.memory.MBR));

                        // Update register and post events
                        GPR[R] = (GPR[R] + memValue) & 0xFFFF; // 16-bit register
                        bus.post(new GPRChanged(R, GPR[R]));
                        bus.post(new MessageChanged("AMR executed: R" + R + " = " + GPR[R]));
                    } catch (Exception e) {
                        bus.post(new MessageChanged("AMR failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }
                case 05: { // SMR r,x,address[,I]
                    try {
                        this.setEffectiveAddress(opcode);

                        // Read via cache (fills block on miss)
                        int memValue = cache.load(effectiveAddress) & 0xFFFF;

                        // Keep MAR/MBR consistent for bus listeners
                        this.memory.MAR = effectiveAddress;
                        this.memory.MBR = memValue;
                        this.bus.post(new MARChanged(this.memory.MAR));
                        this.bus.post(new MBRChanged(this.memory.MBR));

                        // Update register and post events
                        GPR[R] = (GPR[R] - memValue) & 0xFFFF; // 16-bit register
                        bus.post(new GPRChanged(R, GPR[R]));
                        bus.post(new MessageChanged("SMR executed: R" + R + " = " + GPR[R]));
                    } catch (Exception e) {
                        bus.post(new MessageChanged("SMR failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }
                case 06: { // AIR r, immed
                    try {
                        int r = (IR >> 6) & 0x03;
                        int immed = IR & 0x3F; // lower 6 bits (adjust depending on ISA spec)

                        if (r < 0 || r > 3) {
                            throw new Exception("Bad register number at AIR instruction");
                        }

                        if (immed == 0) {
                            bus.post(new MessageChanged("AIR skipped (Immed = 0)"));
                            break;
                        }

                        if (GPR[r] == 0) {
                            GPR[r] = immed;
                        } else {
                            GPR[r] = (GPR[r] + immed) & 0xFFFF;
                        }

                        bus.post(new GPRChanged(r, GPR[r]));
                        bus.post(new MessageChanged("AIR executed: R" + r + " = " + GPR[r]));
                    } catch (Exception e) {
                        bus.post(new MessageChanged("AIR failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 07: { // SIR r, immed
                    try {
                        int r = (IR >> 6) & 0x03;
                        int immed = IR & 0x3F; // lower 6 bits

                        if (r < 0 || r > 3) {
                            throw new Exception("Bad register number at SIR instruction");
                        }

                        if (immed == 0) {
                            bus.post(new MessageChanged("SIR skipped (Immed = 0)"));
                            break;
                        }

                        if (GPR[r] == 0) {
                            GPR[r] = (-immed) & 0xFFFF;
                        } else {
                            GPR[r] = (GPR[r] - immed) & 0xFFFF;
                        }

                        bus.post(new GPRChanged(r, GPR[r]));
                        bus.post(new MessageChanged("SIR executed: R" + r + " = " + GPR[r]));
                    } catch (Exception e) {
                        bus.post(new MessageChanged("SIR failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }
                // written with gpt assistance
                case 010: { // JZ r,x,address[,I]
                    try {
                        this.setEffectiveAddress(opcode);
                        boolean isZero = (GPR[R] & 0xFFFF) == 0;
                        CC[3] = isZero; // EQUAL flag reflects result

                        if (isZero) {
                            this.PC = this.effectiveAddress;
                            bus.post(new MessageChanged("JZ taken: R" + R + "=0 → PC=" + this.PC));
                        } else {
                            bus.post(new MessageChanged("JZ not taken: R" + R + "=" + GPR[R]));
                        }
                    } catch (Exception e) {
                        bus.post(new MessageChanged("JZ failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 011: { // JNE r,x,address[,I]
                    try {
                        this.setEffectiveAddress(opcode);
                        boolean notZero = (GPR[R] & 0xFFFF) != 0;
                        CC[3] = !notZero ? true : false; // If equal, set; if not equal, clear

                        if (notZero) {
                            this.PC = this.effectiveAddress;
                            bus.post(new MessageChanged("JNE taken: R" + R + "!=0 → PC=" + this.PC));
                        } else {
                            bus.post(new MessageChanged("JNE not taken: R" + R + "=0"));
                        }
                    } catch (Exception e) {
                        bus.post(new MessageChanged("JNE failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 012: { // JCC cc,x,address[,I]
                    try {
                        this.setEffectiveAddress(opcode);
                        int ccIndex = R; // R field = condition code bit index (0–3)
                        if (ccIndex < 0 || ccIndex > 3)
                            throw new IllegalArgumentException("Invalid CC index: " + ccIndex);

                        if (CC[ccIndex]) {
                            this.PC = this.effectiveAddress;
                            bus.post(new MessageChanged("JCC taken: CC[" + ccIndex + "]=1 → PC=" + this.PC));
                        } else {
                            bus.post(new MessageChanged("JCC not taken: CC[" + ccIndex + "]=0"));
                        }
                    } catch (Exception e) {
                        bus.post(new MessageChanged("JCC failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 013: { // JMA x,address[,I] – unconditional jump
                    try {
                        this.setEffectiveAddress(opcode);
                        this.PC = this.effectiveAddress;
                        bus.post(new MessageChanged("JMA executed: PC <- " + this.PC));
                    } catch (Exception e) {
                        bus.post(new MessageChanged("JMA failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 014: { // JSR x,address[,I]
                    try {
                        this.setEffectiveAddress(opcode);
                        // Save return address (current PC, which already points to next instruction)
                        GPR[3] = this.PC & 0xFFFF;
                        bus.post(new GPRChanged(3, GPR[3]));

                        // Jump to subroutine target
                        this.PC = this.effectiveAddress;
                        bus.post(new MessageChanged("JSR executed: Saved R3=" + GPR[3] + ", PC=" + this.PC));
                    } catch (Exception e) {
                        bus.post(new MessageChanged("JSR failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }
                case 015: { // RFS immed – return from subroutine
                    try {
                        int immed = this.IR & 0x1F; // lower 5 bits are immediate
                        GPR[0] = immed & 0xFFFF;
                        bus.post(new GPRChanged(0, GPR[0]));

                        // Restore PC from R3
                        this.PC = GPR[3] & 0xFFFF;
                        bus.post(new MessageChanged("RFS executed: R0 <- " + immed + ", PC <- " + this.PC));
                    } catch (Exception e) {
                        bus.post(new MessageChanged("RFS failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 016: { // SOB r,x,address[,I]
                    try {
                        this.setEffectiveAddress(opcode);
                        int before = GPR[R];
                        int after = (before - 1) & 0xFFFF;
                        GPR[R] = after;
                        bus.post(new GPRChanged(R, GPR[R]));

                        // Update EQUAL flag: if result == 0, CC[3]=true
                        CC[3] = (after == 0);

                        if ((short) after > 0) {
                            this.PC = this.effectiveAddress;
                            bus.post(new MessageChanged(
                                    "SOB taken: R" + R + " " + before + "→" + after + ", PC=" + this.PC));
                        } else {
                            bus.post(new MessageChanged("SOB not taken: R" + R + " " + before + "→" + after));
                        }
                    } catch (Exception e) {
                        bus.post(new MessageChanged("SOB failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 017: { // JGE r,x,address[,I]
                    try {
                        this.setEffectiveAddress(opcode);
                        int signedVal = (short) GPR[R];
                        CC[3] = (signedVal == 0); // equal flag if exactly zero
                        if (signedVal >= 0) {
                            this.PC = this.effectiveAddress;
                            bus.post(new MessageChanged("JGE taken: R" + R + "=" + signedVal + " ≥ 0 → PC=" + this.PC));
                        } else {
                            bus.post(new MessageChanged("JGE not taken: R" + R + "=" + signedVal + " < 0"));
                        }
                    } catch (Exception e) {
                        bus.post(new MessageChanged("JGE failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }

                // https://chatgpt.com/share/6901599b-9c2c-8007-861c-a8aa6f7043d4
                case 031: { // SRC r, count, L/R, A/L
                    try {
                        int r = srFieldR(IR);
                        int al = srFieldAL(IR);
                        int lr = srFieldLR(IR);
                        int count = srFieldCount(IR) & 0x0F;

                        if (r < 0 || r > 3) {
                            throw new Exception("Bad register number at SRC instruction (must be 0, 1, 2, 3)");
                        }

                        if (al != 0 && al != 1) {
                            throw new IllegalArgumentException("Bad AL arg in SRC instruction");
                        }

                        if (lr != 0 && lr != 1) {
                            throw new IllegalArgumentException("Bad LR arg in SRC instruction");
                        }

                        if (count < 0 || count > 15) {
                            throw new IllegalArgumentException("Bad count arg in SRC instruction");
                        }

                        if (count == 0) {
                            bus.post(new MessageChanged("SRC skipped (Count=0)"));
                            break;
                        }

                        int v = GPR[r] & 0xFFFF;
                        int lastOut = 0; // the bit that will become carry after the operation

                        // For left shifts (lr == 1)
                        if (lr == 1) {
                            // treat arithmetic-left the same as logical-left (common ISA convention).
                            // logical left: fill LSB with zeros
                            int n = count & 0x0F; // 1..15 (count==0 already handled)
                            // compute lastOut = bit that will be shifted out last:
                            lastOut = (n == 0) ? getCarry() : ((v >> (16 - n)) & 0x1);
                            v = (v << n) & 0xFFFF;
                        } else {
                            // right shifts (lr == 0)
                            int n = count & 0x0F;
                            // lastOut: the last bit shifted out from the LSB side is bit (n-1)
                            lastOut = (n == 0) ? getCarry() : ((v >> (n - 1)) & 0x1);

                            if (al == 1) {
                                // L == logical right shift (fill with zeros)
                                v = (v & 0xFFFF) >>> n;
                            } else {
                                // A == arithmetic right shift: preserve sign
                                // sign-extend the 16-bit value to 32-bit signed int, shift arithmetically, mask
                                // back
                                int signed = (v & 0x8000) != 0 ? (v | 0xFFFF0000) : v;
                                int shifted = signed >> n; // arithmetic shift in Java
                                v = shifted & 0xFFFF;
                            }
                        }

                        // Set carry to lastOut
                        setCarry(lastOut);

                        // Store result
                        GPR[r] = v & 0xFFFF;
                        bus.post(new GPRChanged(r, GPR[r]));

                        // Update EQUALORNOT (CC[3]) - 1 if result == 0 else 0
                        CC[3] = (GPR[r] == 0) ? true : false;
                        bus.post(new GPRChanged(r, GPR[r]));
                        bus.post(new MessageChanged(
                                "SRC executed: R" + r + " = " + String.format("0x%04X", GPR[r]) +
                                        " (count=" + count + ", " + (lr == 1 ? "L" : "R") + ")"));
                    } catch (Exception e) {
                        bus.post(new MessageChanged("SRC failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }
                case 032: { // RRC r, count, L/R, A/L
                    try {
                        int r = srFieldR(IR);
                        int lr = srFieldLR(IR);
                        int count = srFieldCount(IR) & 0x0F;
                        int al = srFieldAL(IR) & 0x01; // 1 => A (through-carry), 0 => L (logical)

                        if (r < 0 || r > 3) {
                            throw new Exception("Bad register number at SRC instruction (must be 0, 1, 2, 3)");
                        }

                        if (al != 0 && al != 1) {
                            throw new IllegalArgumentException("Bad AL arg in SRC instruction");
                        }

                        if (lr != 0 && lr != 1) {
                            throw new IllegalArgumentException("Bad LR arg in SRC instruction");
                        }

                        if (count < 0 || count > 15) {
                            throw new IllegalArgumentException("Bad count arg in SRC instruction");
                        }

                        if (count == 0) {
                            bus.post(new MessageChanged("RRC skipped (Count=0)"));
                            break;
                        }

                        int v = GPR[r] & 0xFFFF;
                        if (al == 0) {
                            if (lr == 1) {
                                v = rotLeft16(v, count);
                                int lastOut = (v >> (16 - (count % 16))) & 0x1;
                                setCarry(lastOut);
                            } else { // right
                                v = rotRight16(v, count);
                                // lastOut is the bit that was shifted out on the right (LSB) after count
                                // rotates:
                                int lastOut = (v >> ((count - 1) % 16)) & 0x1; // (count-1) maps to the LSB moved out
                                setCarry(lastOut);
                            }
                        } else {
                            // --- Rotate THROUGH carry (A/L == A) ---
                            // rotate acro ss 17 bits: 16-bit register + carry bit.
                            // Effective rotation size is 17, so reduce count mod 17.
                            int currentCarry = getCarry();
                            int n = count % 17;
                            for (int i = 0; i < n; i++) {
                                if (lr == 1) { // left through carry
                                    int newCarry = (v >> 15) & 0x1; // MSB becomes new carry
                                    v = ((v << 1) & 0xFFFF) | (carry & 0x1);
                                    carry = newCarry;
                                } else { // right through carry
                                    int newCarry = v & 0x1; // LSB becomes new carry
                                    v = ((carry & 0x1) << 15) | (v >>> 1);
                                    carry = newCarry;
                                }
                            }
                            setCarry(currentCarry);
                        }

                        GPR[r] = v & 0XFFFF;

                        // Update equality flag CC[3] according to result == 0.
                        CC[3] = GPR[r] == 0 ? true : false;
                        bus.post(new GPRChanged(r, GPR[r]));
                        bus.post(new MessageChanged(
                                "RRC executed: R" + r + " = " + String.format("0x%04X", GPR[r]) +
                                        " (count=" + count + ", " + (lr == 1 ? "L" : "R") + ")"));
                    } catch (Exception e) {
                        bus.post(new MessageChanged("RRC failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }
                case 070: { // MLT rx, ry
                    try {
                        int ry = (IR >> 8) & 0x03;
                        int rx = (IR >> 6) & 0x03;

                        if ((rx != 0 && rx != 2) || (ry != 0 && ry != 2)) {
                            throw new IllegalArgumentException("MLT: rx and ry must be 0 or 2.");
                        }

                        long result = (long) GPR[rx] * (long) GPR[ry];
                        GPR[rx] = (int) ((result >> 16) & 0xFFFF); // high order bits
                        GPR[rx + 1] = (int) (result & 0xFFFF); // low order bits

                        // Set overflow flag if result exceeds 32 bits
                        if (result > 0xFFFFFFFFL || result < 0) {
                            CC[0] = true; // Overflow flag
                        } else {
                            CC[0] = false;
                        }

                        bus.post(new GPRChanged(rx, GPR[rx]));
                        bus.post(new GPRChanged(rx + 1, GPR[rx + 1]));
                    } catch (Exception e) {
                        bus.post(new MessageChanged(e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 071: { // DVD rx, ry
                    try {
                        int ry = (IR >> 8) & 0x03;
                        int rx = (IR >> 6) & 0x03;

                        if ((rx != 0 && rx != 2) || (ry != 0 && ry != 2)) {
                            throw new IllegalArgumentException("DVD: rx and ry must be 0 or 2.");
                        }

                        if (GPR[ry] == 0) {
                            CC[2] = true; // DIVZERO flag
                            throw new ArithmeticException("Division by zero in DVD instruction.");
                        }

                        int quotient = GPR[rx] / GPR[ry];
                        int remainder = GPR[rx] % GPR[ry];
                        GPR[rx] = quotient;
                        GPR[rx + 1] = remainder;

                        bus.post(new GPRChanged(rx, GPR[rx]));
                        bus.post(new GPRChanged(rx + 1, GPR[rx + 1]));
                    } catch (Exception e) {
                        bus.post(new MessageChanged(e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 072: { // TRR rx, ry
                    try {
                        int ry = (IR >> 8) & 0x03;
                        int rx = (IR >> 6) & 0x03;
                        if ((rx < 0 || rx > 3) || (ry < 0 && ry > 3)) {
                            throw new IllegalArgumentException("TRR: rx and ry must be 0, 1, 2, 3.");
                        }

                        if (GPR[rx] == GPR[ry]) {
                            CC[3] = true; // EQUAL flag
                        } else {
                            CC[3] = false;
                        }

                        bus.post(new MessageChanged("TRR executed. rx=" + GPR[rx] + ", ry=" + GPR[ry] +
                                ", EqualFlag=" + CC[3]));
                    } catch (Exception e) {
                        bus.post(new MessageChanged(e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 073: { // AND rx, ry
                    try {
                        int ry = (IR >> 8) & 0x03;
                        int rx = (IR >> 6) & 0x03;
                        GPR[rx] = (GPR[rx] & GPR[ry]) & 0xFFFF;
                        bus.post(new GPRChanged(rx, GPR[rx]));
                        bus.post(new MessageChanged("AND executed: R" + rx + " = " + GPR[rx] + " (& R" + ry + ")"));
                    } catch (Exception e) {
                        bus.post(new MessageChanged(e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 074: { // ORR rx, ry
                    try {
                        int ry = (IR >> 8) & 0x03;
                        int rx = (IR >> 6) & 0x03;
                        if ((rx < 0 || rx > 3) || (ry < 0 && ry > 3)) {
                            throw new IllegalArgumentException("ORR: rx and ry must be 0, 1, 2, 3.");
                        }
                        GPR[rx] = (GPR[rx] | GPR[ry]) & 0xFFFF;
                        bus.post(new GPRChanged(rx, GPR[rx]));
                        bus.post(new MessageChanged("ORR executed: R" + rx + " = " + GPR[rx] + " (| R" + ry + ")"));
                    } catch (Exception e) {
                        bus.post(new MessageChanged(e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 075: { // NOT rx
                    try {
                        int rx = (IR >> 6) & 0x03;
                        if ((rx < 0 || rx > 3)) {
                            throw new IllegalArgumentException("NOT: rx must be 0, 1, 2, 3.");
                        }
                        GPR[rx] = (~GPR[rx]) & 0xFFFF;
                        bus.post(new GPRChanged(rx, GPR[rx]));
                        bus.post(new MessageChanged("NOT executed: R" + rx + " = " + GPR[rx]));
                    } catch (Exception e) {
                        bus.post(new MessageChanged(e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 061: { // IN r, devid
                    try {
                        int r = (IR >> 6) & 0x03;
                        int devid = (IR >> 8) & 0x1F;
                        int value = 0;
                        if ((r < 0 || r > 3)) {
                            throw new IllegalArgumentException("IN: r must be 0, 1, 2, 3.");
                        }
                        if ((devid < 0 || devid == 1 || devid > 31)) {
                            throw new IllegalArgumentException(
                                    "IN: devid must be 0 (keyboard), 2 (card reader), or 3-31 (misc)");
                        }
                        GPR[r] = value & 0xFFFF;
                        bus.post(new GPRChanged(r, GPR[r]));
                        bus.post(new MessageChanged("IN executed: R" + r + " <- device[" + devid + "] (stubbed=0)"));
                    } catch (Exception e) {
                        bus.post(new MessageChanged("IN failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }
                case 062: { // OUT r, devid
                    try {
                        int r = (IR >> 6) & 0x03;
                        int devid = (IR >> 8) & 0x1F;
                        int value = GPR[r] & 0xFFFF;
                        if ((r < 0 || r > 3)) {
                            throw new IllegalArgumentException("OUT: r must be 0, 1, 2, 3.");
                        }
                        if ((devid < 0 || (devid < 3 && devid != 1) || devid > 31)) {
                            throw new IllegalArgumentException("OUT: devid must be 1 (printer) or 3-31 (misc)");
                        }
                        bus.post(new MessageChanged(
                                "OUT executed: device[" + devid + "] <- R" + r + " (" + value + ")"));
                    } catch (Exception e) {
                        bus.post(new MessageChanged("OUT failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }

                default: {
                    System.out.println("Unknown opcode. May be data: " + opcode);
                    break;
                }
            }

        }
    }

    /**
     * Halts execution of running program.
     */
    public void halt() {
        running = false;
    }

    // https://stackoverflow.com/questions/28432164/java-swing-timer-loop
    /**
     * Runs pending insturctions in memory
     */
    public void run() {
        running = true;
        completedInstructions = 0;

        // we apply timer of 2000 seconds here to allow user to properly see interface
        // update.
        Timer cpuTick = new Timer(2000, e -> {

            step();
            if (!running || pendingInstructions == completedInstructions) {
                ((javax.swing.Timer) e.getSource()).stop();
                return;
            }
        });
        cpuTick.setInitialDelay(0);
        cpuTick.start();
    }

    /**
     * Loads set of instructions from load file
     * 
     * @param selectedPath String of selected file path to load.
     */
    public void loadFromROM(Path selectedPath) {
        {
            try {
                if (this.PC == null) {
                    this.bus.post(new MessageChanged("Program counter should be set"));
                    return;
                }
                this.pendingInstructions = this.romLoader.load(selectedPath);
                this.completedInstructions = 0;
                this.bus.post(new MessageChanged("Instructions loaded into memory at PC counter "
                        + this.PC +
                        " from " + selectedPath));
            } catch (IOException | LoadException e) {
                this.bus.post(new MessageChanged("Failed to load file into memory :" + e.getMessage()));
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        try {
            SetGPRSub.close();
        } catch (Exception ignored) {
        }
        try {
            SetIXRSub.close();
        } catch (Exception ignored) {
        }
        try {
            SetMARSub.close();
        } catch (Exception ignored) {
        }
        try {
            SetMBRSub.close();
        } catch (Exception ignored) {
        }
        try {
            SetPCSub.close();
        } catch (Exception ignored) {
        }
    }

}
