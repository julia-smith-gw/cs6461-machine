package group11.core;

import java.io.IOException;

import group11.core.RomLoader.LoadException;
import group11.events.CChanged;
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
import java.util.Arrays;
import java.util.InputMismatchException;

// assisted by chatgpt
public class CPU implements AutoCloseable {

    // event bus and subscriptions
    private final EventBus bus;
    private final BranchPredictor branchPredictor;
    private final Cache cache;

    private final AutoCloseable SetGPRSub;
    private final AutoCloseable SetIXRSub;
    private final AutoCloseable SetPCSub;
    private final AutoCloseable SetMARSub;
    private final AutoCloseable SetMBRSub;

    // cpu running information
    public int[] GPR = new int[4]; // R0-R3
    public int[] IXR = new int[4]; // X1-X3

    public int[] FR = new int[2];  // FR0 and FR1

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

    // --- Console input coordination (non-blocking wait) ---
    private volatile boolean waitingForConsoleInput = false;
    private int waitingInDestReg = -1;
    private Integer inputBaseAddr = null;

    public Timer cpuTick;
    public boolean[] CC = new boolean[4]; // CC[0]=OVERFLOW, CC[1]=UNDERFLOW, CC[2]=DIVZERO, CC[3]=EQUALORNOT
    private int carry = 0; // 0 or 1

    public CPU(Memory memory, EventBus bus, Cache cache, RomLoader romLoader) {
        this.memory = memory;
        this.bus = bus;
        this.romLoader = romLoader;
        this.cache = cache;
        this.branchPredictor = new BranchPredictor(bus);

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

    // https://chatgpt.com/share/69041255-bacc-8007-a578-4898db192eb6
    /**
     * Function that takes in and parses console input for numbers.
     * Continues program execution if program is running
     * 
     * @param line Console input
     */
    public void submitConsoleInput(String line) {
        if (!waitingForConsoleInput) {
            bus.post(new MessageChanged("No IN pending; submit ignored."));
            return;
        }
        waitingForConsoleInput = false;
        int r = waitingInDestReg;
        waitingInDestReg = -1;
        int value = 0;
        try {
            if (line == null || line.trim().isEmpty()) {
                value = 0;
            } else {
                // gets count of numbers to accept from console input
                int count = GPR[3] & 0xFFFF;
                if (count <= 0)
                    count = 20; // default

                String normalized = (line == null) ? "" : line.trim();
                String[] parts = normalized.isEmpty() ? new String[0] : normalized.split("[,\\s]+");

                // throw error if user did not provide all input requested
                if (parts.length < count) {
                    throw new InputMismatchException("User entered less than " + count + " numbers. Please try again");
                }

                // How many numbers we will actually consume
                int n = Math.min(count, parts.length);
                System.out.printf("IN: desired=%d, tokens_seen=%d, writing=%d, base=%04o%n",
                        count, parts.length, n, inputBaseAddr & 0x7FF);
                short[] numbers = new short[count];
                System.out.printf("IN base check: R%d=%o -> base=%o%n", r, GPR[r], inputBaseAddr);

                System.out.printf("IN: r=%d R[r]=%04o (%d) base=%04o (%d) desired=%d%n",
                        r, GPR[r] & 0x7FF, GPR[r] & 0x7FF, inputBaseAddr, inputBaseAddr, count);
                // parse numbers and store to memory
                for (int i = 0; i < count; i++) {
                    try {
                        numbers[i] = Short.parseShort(parts[i]);
                        int address = (inputBaseAddr + i) & 0x7FF;
                        cache.store(address, numbers[i]);
                        System.out.printf("IN write addr=%04o (%d) val=%d%n", address, address, numbers[i]);
                    } catch (NumberFormatException e) {
                        throw new InputMismatchException(
                                "Input from IN includes invalid numbers. Numbers must be between -32,768 and 32,767. Please try again.");
                    }
                }
                value = numbers[0];
            }

            // set register value to first value
            GPR[r] = value;
            bus.post(new GPRChanged(r, GPR[r]));
            bus.post(new MessageChanged("Input accepted."));
            waitingInDestReg = -1;
            waitingForConsoleInput = false;
            if (running) {
                cpuTick.start();
            }
        } catch (Exception e) {
            bus.post(new MessageChanged("IN failed: " + e.getMessage()));
        }
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
        if (waitingForConsoleInput)
            return;
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
            this.memory.MBR = this.cache.load(this.memory.MAR);
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
            this.cache.store(this.memory.MAR, this.memory.MBR);
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
     * Loads value into given index register from memory (cache)
     */
    public void loadIndexRegister() {
        // EA computed already with setEffectiveAddress(opcode)
        int value = cache.load(effectiveAddress) & 0xFFFF;

        // Update front-panel regs
        this.memory.MAR = effectiveAddress;
        this.memory.MBR = value;
        this.bus.post(new MARChanged(this.memory.MAR));
        this.bus.post(new MBRChanged(this.memory.MBR));

        // *** Use IX (1..3) as the destination index register ***
        if (IX < 1 || IX > 3) {
            throw new IllegalArgumentException("LDX: index register must be 1..3, got " + IX);
        }
        IXR[IX] = value;
        this.bus.post(new IXRChanged(IX, IXR[IX]));
        System.out.printf("LDX debug: set IXR[%d]=%o from EA=%o%n", IX, IXR[IX], effectiveAddress);
    }

    /**
     * Stores index register value into cache/memory
     */
    public void storeIndexRegister() {
        if (IX < 1 || IX > 3) {
            throw new IllegalArgumentException("STX: index register must be 1..3, got " + IX);
        }
        cache.store(effectiveAddress, IXR[IX] & 0xFFFF);

        // Keep simulator-visible memory regs consistent
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
    private boolean getIsValidAddress(Boolean ignoreReserved, boolean allowIndexingInEA, int pendingEffectiveAddress) {
        if (pendingEffectiveAddress >= this.memory.MEMORY_SIZE
                || pendingEffectiveAddress < 0) {
            throw new IllegalArgumentException("Memory out of bounds access");
        }
        if (!!ignoreReserved && !allowIndexingInEA && (pendingEffectiveAddress >= 0 && pendingEffectiveAddress <= 5)) {
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

    /**
     * Calculates effective address given r, ix, i, opcode.
     * 
     * @param opcode 10 bit number indicating operation
     * @return int [] array of info about instruction
     */
    private int[] calculateEffectiveAddress(int opcode) {
        int pendingEffectiveAddress = 0;
        int r = (IR >> 8) & 0x03; // [9..8] R
        int ix = (IR >> 6) & 0x03; // [7..6] X
        int i = (IR >> 5) & 0x01; // [5] I
        pendingEffectiveAddress = IR & 0x1F;

        boolean allowIndexingInEA = !(opcode == 041 || opcode == 042);
        if (ix != 0 && allowIndexingInEA) {
            if (ix < 1 || ix > 3) {
                throw new IllegalArgumentException("Bad index register in instruction");
            }
            pendingEffectiveAddress += (IXR[ix] & 0x7FF);
            System.out.println("IXR add" + pendingEffectiveAddress);
        }

        if (r < 0 || r > 3) {
            throw new IllegalArgumentException("Bad general purpose register. in instruction. GPR can be 0, 1, 2, 3");
        }

        if (i == 1) {
            // check bounds before reading
            try {
                getIsValidAddress(running, allowIndexingInEA, pendingEffectiveAddress);
                // Use cache for coherence with LDR/LDX paths
                int ptrWord = cache.load(pendingEffectiveAddress) & 0xFFFF;

                // Keep MAR/MBR + bus in sync (like LDR path)
                this.memory.MAR = pendingEffectiveAddress & 0x7FF;
                this.memory.MBR = ptrWord;
                this.bus.post(new MARChanged(this.memory.MAR));
                this.bus.post(new MBRChanged(this.memory.MBR));

                // FINAL EA becomes the pointee
                pendingEffectiveAddress = ptrWord & 0x7FF;
            } catch (Exception e) {
                System.out.printf("EA debug: r=%d x=%d i=%d a5=%o ix=%o -> EA=%o%n",
                        r, ix, i, IR & 0x1F, (ix == 0 ? 0 : IXR[ix]), pendingEffectiveAddress & 0x7FF);
                throw e;
            }
        }

        // check returned address if indirect or if calculated based on index register
        // above
        try {
            getIsValidAddress(running, allowIndexingInEA, pendingEffectiveAddress);
        } catch (Exception e) {
            System.out.printf("EA debug: r=%d x=%d i=%d a5=%o ix=%o -> EA=%o%n",
                    r, ix, i, IR & 0x1F, (ix == 0 ? 0 : IXR[ix]), pendingEffectiveAddress & 0x7FF);

            throw e;
        }

        int eaMasked = pendingEffectiveAddress & 0x7FF;
        int[] result = { ix, r, i, eaMasked };
        System.out.printf("EA debug: r=%d x=%d i=%d a5=%o ix=%o -> EA=%o%n",
                r, ix, i, IR & 0x1F, (ix == 0 ? 0 : IXR[ix] & 0x7FF), eaMasked);
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

    private int srFieldCount(int ir) {
        return (ir >> 4) & 0x1F;
    } // 5 bits

    private int srFieldLR(int ir) {
        return (ir >> 3) & 0x01;
    }

    private int srFieldAL(int ir) {
        return (ir >> 2) & 0x01;
    }

    private int getCarry() {
        return (carry != 0) ? 1 : 0;
    }

    private void setCarry(int c) {
        carry = (c != 0) ? 1 : 0;
    }

    /**
     * Return true if 16-bit signed subtraction a - b overflows (i.e., "underflow"
     * for your CC[1]).
     */
    private boolean subOverflow16(int a, int b, int diff) {
        // Overflow iff a and b have different signs, and diff’s sign differs from a’s
        // sign.
        return (((a ^ b) & 0x8000) != 0) && (((a ^ diff) & 0x8000) != 0);
    }

    /**
     * Empties all registers, memory blocks, and cache, effectively restarting
     * machine
     */
    public void reset() {
        running = false;
        pendingInstructions = 0;
        completedInstructions = 0;
        effectiveAddress = 0;
        R = 0;
        IX = 0;
        I = 0;
        opcode = 0;
        message = "CPU reset. Cache emptied, memory emptied, all registers zeroed.";
        GPR = new int[4]; // R0-R3
        IXR = new int[4]; // X1-X3
        CC = new boolean[4];
        PC = 0; // Program Counter
        IR = 0; // Instruction Register

        FR = new int[2];

        branchPredictor.reset();

        memory.reset();
        cache.reset();
        this.bus.post(new GPRChanged(0, GPR[0]));
        this.bus.post(new GPRChanged(1, GPR[1]));
        this.bus.post(new GPRChanged(2, GPR[2]));
        this.bus.post(new GPRChanged(3, GPR[3]));
        this.bus.post(new IXRChanged(1, IXR[1]));
        this.bus.post(new IXRChanged(2, IXR[2]));
        this.bus.post(new IXRChanged(3, IXR[3]));
        this.bus.post(new MBRChanged(this.memory.MBR));
        this.bus.post(new MARChanged(this.memory.MAR));
        this.bus.post(new PCChanged(PC));
        this.bus.post(new IRChanged(IR));
        this.bus.post(new CChanged(Arrays.toString(CC)));
        this.bus.post(new MessageChanged(message));
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
                    System.out.println("LDR exec: IR=" + Integer.toOctalString(IR));
                    System.out.printf("Before LDR: IXR[1]=%o IXR[2]=%o IXR[3]=%o%n", IXR[1], IXR[2], IXR[3]);
                    try {
                        this.setEffectiveAddress(opcode);
                        System.out.println("LDR effective address R:" + R + " ix: " + IX + " i flag: " + I
                                + " effective address: " + this.effectiveAddress);
                        // Read from cache (which will load the block on miss)
                        int word = cache.load(effectiveAddress) & 0xFFFF; // ensure 16-bit
                        System.out.println(
                                "LDR executed. Loaded from cache and or memory: " + word + "at " + effectiveAddress);
                        // Update simulator-visible memory registers so bus events are correct
                        this.memory.MAR = effectiveAddress;
                        this.memory.MBR = word;

                        // Post bus events just like before
                        this.bus.post(new MARChanged(this.memory.MAR));
                        this.bus.post(new MBRChanged(this.memory.MBR));
                        GPR[R] = word;
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
                    System.out.println("STR exec: IR=" + Integer.toOctalString(IR));
                    try {
                        this.setEffectiveAddress(opcode);
                        System.out.println("STR effective address R:" + R + " ix: " + IX + " i flag: " + I
                                + " effective address: " + this.effectiveAddress);
                        // Update cache
                        cache.store(effectiveAddress, GPR[R] & 0xFFFF);
                        System.out.println(
                                "STR EXECUTED. " + "memory address " + effectiveAddress + "=" + (GPR[R] & 0xFFFF));
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
                    System.out.println("LDA exec: IR=" + Integer.toOctalString(IR));
                    try {
                        int[] addressInfo = this.calculateEffectiveAddress(opcode);
                        R = addressInfo[1];
                        effectiveAddress = addressInfo[3];
                        System.out.println(
                                "LDA effective address R: " + R + " effective address: " + this.effectiveAddress);
                        GPR[R] = effectiveAddress;
                        System.out.println("LDA EXECUTED " + "effectiveAddress=" + effectiveAddress);
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
                    System.out.println("LDX exec: IR=" + Integer.toOctalString(IR));
                    try {
                        this.setEffectiveAddress(opcode);
                        System.out.println("LDX effective address R:" + R + " ix: " + IX + " i flag: " + I
                                + " effective address: " + this.effectiveAddress);
                        loadIndexRegister();
                        System.out.println("LDX EXECUTED. IXR 1: " + IXR[1] + "IXR 2: " + IXR[2] + "IXR 3: " + IXR[3]);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        this.bus.post(new MessageChanged(e.getMessage()));
                        this.halt();
                    }
                    break;
                }
                // STX x, address[,I]
                case 042: {
                    System.out.println("STX exec: IR=" + Integer.toOctalString(IR));
                    try {
                        this.setEffectiveAddress(opcode);
                        System.out.println("STX effective address R:" + R + " ix: " + IX + " i flag: " + I
                                + " effective address: " + this.effectiveAddress);
                        storeIndexRegister();
                        System.out.println("STX EXECUTED. IXR 1: " + IXR[1] + "IXR 2: " + IXR[2] + "IXR 3: " + IXR[3]);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        this.bus.post(new MessageChanged(e.getMessage()));
                        this.halt();
                    }
                    break;
                }
                case 04: { // AMR r,x,address[,I]
                    System.out.println("AMR exec: IR=" + Integer.toOctalString(IR));
                    try {
                        this.setEffectiveAddress(opcode);
                        System.out.println("AMR effective address R:" + R + " ix: " + IX + " i flag: " + I
                                + " effective address: " + this.effectiveAddress);
                        // Read via cache (fills block on miss)
                        int memValue = cache.load(effectiveAddress) & 0xFFFF;

                        // Keep MAR/MBR consistent for bus listeners
                        this.memory.MAR = effectiveAddress;
                        this.memory.MBR = memValue;
                        this.bus.post(new MARChanged(this.memory.MAR));
                        this.bus.post(new MBRChanged(this.memory.MBR));

                        // --- Signed-overflow detection for 16-bit add (two's complement) ---
                        int a = GPR[R] & 0xFFFF;
                        int b = memValue & 0xFFFF;
                        int sum = (a + b) & 0xFFFF;

                        // https://chatgpt.com/share/6906f0ff-36d8-8007-a4d1-f8dac5a1f999
                        // Interpret sign bits (bit 15) for two's-complement overflow
                        boolean sameSignOperands = ((a ^ b) & 0x8000) == 0;
                        boolean resultSignDiffers = ((a ^ sum) & 0x8000) != 0;
                        boolean overflow = sameSignOperands && resultSignDiffers;

                        // Update register
                        GPR[R] = sum;
                        bus.post(new GPRChanged(R, GPR[R]));

                        // --- Condition Codes per ISA ---
                        CC[0] = overflow; // OVERFLOW
                        CC[1] = false; // UNDERFLOW (not used for addition)
                        bus.post(new CChanged(Arrays.toString(CC)));
                        bus.post(new MessageChanged("AMR executed: R" + R + " = " + GPR[R]));
                        System.out.println("AMR executed: R" + R + " = " + GPR[R]);
                    } catch (Exception e) {
                        bus.post(new MessageChanged("AMR failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }
                case 05: { // SMR r,x,address[,I]
                    System.out.println("SMR exec: IR=" + Integer.toOctalString(IR));
                    try {
                        this.setEffectiveAddress(opcode);
                        System.out.println("SMR effective address R:" + R + " ix: " + IX + " i flag: " + I
                                + " effective address: " + this.effectiveAddress);
                        // Read via cache (fills block on miss)
                        int memValue = cache.load(effectiveAddress) & 0xFFFF;

                        // Keep MAR/MBR consistent for bus listeners
                        this.memory.MAR = effectiveAddress;
                        this.memory.MBR = memValue;
                        this.bus.post(new MARChanged(this.memory.MAR));
                        this.bus.post(new MBRChanged(this.memory.MBR));

                        // https://chatgpt.com/share/6906f0ff-36d8-8007-a4d1-f8dac5a1f999
                        // --- Signed-underflow detection for 16-bit add (two's complement) ---
                        int a = GPR[R] & 0xFFFF;
                        int b = memValue & 0xFFFF;

                        int diff = (a - b) & 0xFFFF;

                        boolean underflow = subOverflow16(a, b, diff);

                        GPR[R] = diff;
                        bus.post(new GPRChanged(R, GPR[R]));

                        // CC: mark UNDERFLOW for subtract; clear OVERFLOW (reserved for add)
                        CC[0] = false; // OVERFLOW (for addition)
                        CC[1] = underflow; // UNDERFLOW (for subtraction
                        bus.post(new CChanged(Arrays.toString(CC)));
                        bus.post(new MessageChanged("SMR executed: R" + R + " = " + GPR[R]));
                        System.out.println("SMR executed: R" + R + " = " + GPR[R]);
                    } catch (Exception e) {
                        bus.post(new MessageChanged("SMR failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }
                case 06: { // AIR r, immed
                    System.out.println("AIR exec: IR=" + Integer.toOctalString(IR));

                    try {
                        int r = (IR >> 8) & 0x03; // R at bits 9..8
                        int immed = IR & 0x1F; // lower 6 bits (adjust depending on ISA spec)
                        System.out.printf("AIR pre: R%d=%d (oct %o), imm=%d\n", r, GPR[r], GPR[r], immed);
                        System.out.println("AIR decoded:=" + Integer.toOctalString(r) + " immed: " + immed);
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
                        System.out.println("AIR executed: R" + r + " = " + GPR[r]);
                    } catch (Exception e) {
                        bus.post(new MessageChanged("AIR failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 07: { // SIR r, immed
                    try {
                        int r = (IR >> 8) & 0x03; // R at bits 9..8
                        int imm5 = IR & 0x1F;

                        if (r < 0 || r > 3) {
                            throw new Exception("Bad register number at SIR instruction");
                        }

                        if (imm5 == 0) {
                            bus.post(new MessageChanged("SIR skipped (Immed = 0)"));
                            break;
                        }

                        int a = GPR[r] & 0xFFFF;
                        int b = imm5 & 0xFFFF;

                        int diff = (a - b) & 0xFFFF;
                        boolean underflow = subOverflow16(a, b, diff);

                        GPR[r] = diff;
                        bus.post(new GPRChanged(r, GPR[r]));

                        CC[0] = false; // OVERFLOW (for add)
                        CC[1] = underflow; // UNDERFLOW (for subtract)
                        bus.post(new CChanged(Arrays.toString(CC)));
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
                        System.out.println("JZ effective address R:" + R + " ix: " + IX + " i flag: " + I
                                + " effective address: " + this.effectiveAddress);

                        int branchPC = (this.PC - 1) & 0x7FF; // address of this JZ
                        int fallthroughPC = this.PC; // PC was incremented in fetch()
                        int targetPC = this.effectiveAddress;

                        // --- prediction ---
                        boolean predictedTaken = branchPredictor.predictTaken(branchPC);
                        int predictedNextPC = predictedTaken ? targetPC : fallthroughPC;

                        // --- actual outcome ---
                        boolean isZero = (GPR[R] & 0xFFFF) == 0;
                        CC[3] = isZero; // EQUAL flag

                        boolean actuallyTaken = isZero;
                        int actualNextPC = actuallyTaken ? targetPC : fallthroughPC;

                        // --- record stats + update predictor ---
                        boolean correct = (predictedNextPC == actualNextPC);
                        branchPredictor.recordPredictionResult(correct);

                        branchPredictor.update(branchPC, actuallyTaken);

                        // --- commit real next PC ---
                        this.PC = actualNextPC;

                        if (actuallyTaken) {
                            bus.post(new MessageChanged("JZ taken: R" + R + "=0 → PC=" + this.PC));
                        } else {
                            bus.post(new MessageChanged("JZ not taken: R" + R + "=" + GPR[R]));
                        }
                        bus.post(new CChanged(Arrays.toString(CC)));
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

                        int branchPC = (this.PC - 1) & 0x7FF; // address of this jne
                        int fallthroughPC = this.PC; // PC was incremented in fetch()
                        int targetPC = this.effectiveAddress;

                        // --- prediction ---
                        boolean predictedTaken = branchPredictor.predictTaken(branchPC);
                        int predictedNextPC = predictedTaken ? targetPC : fallthroughPC;

                        boolean actuallyTaken = notZero;
                        int actualNextPC = actuallyTaken ? targetPC : fallthroughPC;

                        // --- record stats + update predictor ---
                        boolean correct = (predictedNextPC == actualNextPC);
                        branchPredictor.recordPredictionResult(correct);

                        branchPredictor.update(branchPC, actuallyTaken);

                        // --- commit real next PC ---
                        this.PC = actualNextPC;

                        if (notZero) {
                            bus.post(new MessageChanged("JNE taken: R" + R + "!=0 → PC=" + this.PC));
                        } else {
                            bus.post(new MessageChanged("JNE not taken: R" + R + "=0"));
                        }
                        bus.post(new CChanged(Arrays.toString(CC)));
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

                        int branchPC = (this.PC - 1) & 0x7FF; // address of this JCC
                        int fallthroughPC = this.PC; // PC was incremented in fetch()
                        int targetPC = this.effectiveAddress;

                        // --- prediction ---
                        boolean predictedTaken = branchPredictor.predictTaken(branchPC);
                        int predictedNextPC = predictedTaken ? targetPC : fallthroughPC;

                        boolean actuallyTaken = CC[ccIndex];
                        int actualNextPC = actuallyTaken ? targetPC : fallthroughPC;

                        // --- record stats + update predictor ---
                        boolean correct = (predictedNextPC == actualNextPC);
                        branchPredictor.recordPredictionResult(correct);
                        branchPredictor.update(branchPC, actuallyTaken);

                        // --- commit real next PC ---
                        this.PC = actualNextPC;

                        if (CC[ccIndex]) {
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
                    System.out.println("SOB exec: IR=" + Integer.toOctalString(IR));
                    try {
                        System.out.println("SOB effective address R:" + R + " ix: " + IX + " i flag: " + I
                                + " effective address: " + this.effectiveAddress);
                        this.setEffectiveAddress(opcode);
                        int before = GPR[R];
                        int after = (before - 1) & 0xFFFF;
                        GPR[R] = after;
                        bus.post(new GPRChanged(R, GPR[R]));

                        // Update EQUAL flag: if result == 0, CC[3]=true
                        CC[3] = (after == 0);

                        int branchPC = (this.PC - 1) & 0x7FF; // address of this SOB
                        int fallthroughPC = this.PC; // PC was incremented in fetch()
                        int targetPC = this.effectiveAddress;

                        // --- prediction ---
                        boolean predictedTaken = branchPredictor.predictTaken(branchPC);
                        int predictedNextPC = predictedTaken ? targetPC : fallthroughPC;

                        boolean actuallyTaken = (short) after > 0;
                        int actualNextPC = actuallyTaken ? targetPC : fallthroughPC;

                        // --- record stats + update predictor ---
                        boolean correct = (predictedNextPC == actualNextPC);
                        branchPredictor.recordPredictionResult(correct);

                        branchPredictor.update(branchPC, actuallyTaken);

                        // --- commit real next PC ---
                        this.PC = actualNextPC;

                        if ((short) after > 0) {
                            bus.post(new MessageChanged(
                                    "SOB taken: R" + R + " " + before + "→" + after + ", PC=" + this.PC));
                            System.out.println("SOB taken: R" + R + " " + before + "→" + after + ", PC=" + this.PC);
                        } else {
                            bus.post(new MessageChanged("SOB not taken: R" + R + " " + before + "→" + after));
                            System.out.println("SOB not taken: R" + R + " " + before + "→" + after);
                        }
                        bus.post(new CChanged(Arrays.toString(CC)));
                    } catch (Exception e) {
                        bus.post(new MessageChanged("SOB failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }

                case 017: { // JGE r,x,address[,I]
                    System.out.println("JGE exec: IR=" + Integer.toOctalString(IR));
                    try {
                        this.setEffectiveAddress(opcode);
                        System.out.println("JGE effective address R:" + R + " ix: " + IX + " i flag: " + I
                                + " effective address: " + this.effectiveAddress);
                        int signedVal = (short) GPR[R];
                        CC[3] = (signedVal == 0); // equal flag if exactly zero

                        int branchPC = (this.PC - 1) & 0x7FF; // address of this JGE
                        int fallthroughPC = this.PC; // PC was incremented in fetch()
                        int targetPC = this.effectiveAddress;

                        // --- prediction ---
                        boolean predictedTaken = branchPredictor.predictTaken(branchPC);
                        int predictedNextPC = predictedTaken ? targetPC : fallthroughPC;

                        boolean actuallyTaken = signedVal >= 0;
                        int actualNextPC = actuallyTaken ? targetPC : fallthroughPC;

                        // --- record stats + update predictor ---
                        boolean correct = (predictedNextPC == actualNextPC);
                        branchPredictor.recordPredictionResult(correct);

                        branchPredictor.update(branchPC, actuallyTaken);

                        // --- commit real next PC ---
                        this.PC = actualNextPC;

                        if (signedVal >= 0) {
                            System.out.println("JGE taken: R" + R + "=" + signedVal + " ≥ 0 → PC=" + this.PC);
                            bus.post(new MessageChanged("JGE taken: R" + R + "=" + signedVal + " ≥ 0 → PC=" + this.PC));
                        } else {
                            System.out.println("JGE not taken: R" + R + "=" + signedVal + " < 0");
                            bus.post(new MessageChanged("JGE not taken: R" + R + "=" + signedVal + " < 0"));
                        }
                        bus.post(new CChanged(Arrays.toString(CC)));
                    } catch (Exception e) {
                        bus.post(new MessageChanged("JGE failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }

                // https://chatgpt.com/share/6901599b-9c2c-8007-861c-a8aa5f7043d4
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
                        bus.post(new CChanged(Arrays.toString(CC)));
                        bus.post(new GPRChanged(r, GPR[r]));
                        bus.post(new MessageChanged(
                                "SRC executed: R" + r + " = " + GPR[r]) +
                                " (count=" + count + ", " + (lr == 1 ? "L" : "R") + ")");
                        System.out.println("SRC executed: R" + r + " = " + GPR[r] +
                                " (count=" + count + ", " + (lr == 1 ? "L" : "R") + ")");
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
                        bus.post(new CChanged(Arrays.toString(CC)));
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
                // 033- 51 witten with GPT assistance
                case 033: {   // FADD
                this.setEffectiveAddress(opcode);

                if (R > 1) {
                bus.post(new MessageChanged("FADD error: FR must be 0 or 1"));
                break;
                }

                int operand;
                if (I == 1) {
                int indirectAddr = cache.load(effectiveAddress) & 0x7FF;
                operand = cache.load(indirectAddr) & 0xFFFF;
                } else {
                operand = cache.load(effectiveAddress) & 0xFFFF;
                }

                FR[R] = floatingAdd(FR[R], operand, false);
                bus.post(new MessageChanged("FADD complete on FR" + R));
                break;
            }
                case 034: {   // FSUB
                this.setEffectiveAddress(opcode);

                if (R > 1) {
                bus.post(new MessageChanged("FSUB error: FR must be 0 or 1"));
                break;
                }

                int operand;
                if (I == 1) {
                int indirectAddr = cache.load(effectiveAddress) & 0x7FF;
                operand = cache.load(indirectAddr) & 0xFFFF;
                } else {
                operand = cache.load(effectiveAddress) & 0xFFFF;
                }

                FR[R] = floatingAdd(FR[R], operand, true);
                bus.post(new MessageChanged("FSUB complete on FR" + R));
                break;
            }

                case 035: {   // VADD
                this.setEffectiveAddress(opcode);

                int length = FR[R];
                int ptr1 = cache.load(effectiveAddress);
                int ptr2 = cache.load(effectiveAddress + 1);

                for (int i = 0; i < length; i++) {
                    int v1 = cache.load(ptr1 + i);
                    int v2 = cache.load(ptr2 + i);

                int result = (v1 + v2) & 0xFFFF;
                cache.store(ptr1 + i, result);

                memory.MAR = ptr1 + i;
                memory.MBR = result;
                }

                bus.post(new MessageChanged("VADD done length " + length));
                break;
            }

                case 036: {   // VSUB
                this.setEffectiveAddress(opcode);

                int length = FR[R];
                int ptr1 = cache.load(effectiveAddress);
                int ptr2 = cache.load(effectiveAddress + 1);

                for (int i = 0; i < length; i++) {
                    int v1 = cache.load(ptr1 + i);
                    int v2 = cache.load(ptr2 + i);

                int result = (v1 - v2) & 0xFFFF;
                cache.store(ptr1 + i, result);

                memory.MAR = ptr1 + i;
                memory.MBR = result;
                }

                bus.post(new MessageChanged("VSUB done length " + length));
                break;
            }

                case 037: {   // CNVRT
                this.setEffectiveAddress(opcode);

                int memValue = cache.load(effectiveAddress) & 0xFFFF;

                if (GPR[R] == 0) {
                // Convert floating to fixed
                GPR[R] = floatingToFixed(memValue);
                } else {
                // Convert fixed to floating (goes into FR0 always)
                FR[0] = fixedToFloating(memValue);
                }

                bus.post(new MessageChanged("CNVRT executed"));
                break;
            }
                case 050: {   // LDFR
                this.setEffectiveAddress(opcode);

                if (R > 1) {
                bus.post(new MessageChanged("LDFR error: FR must be 0 or 1"));
                break;
                }

                FR[R] = cache.load(effectiveAddress) & 0xFFFF;
                bus.post(new MessageChanged("LDFR loaded FR" + R));
                break;
            }
                case 051: {   // STFR
                this.setEffectiveAddress(opcode);

                if (R > 1) {
                bus.post(new MessageChanged("STFR error: FR must be 0 or 1"));
                break;
                }

                cache.store(effectiveAddress, FR[R]);

                memory.MAR = effectiveAddress;
                memory.MBR = FR[R];

                bus.post(new MessageChanged("STFR stored FR" + R));
                break;
            }


                case 070: { // MLT rx, ry
                    try {
                        int rx = (IR >> 8) & 0x03;
                        int ry = (IR >> 6) & 0x03;

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
                        bus.post(new CChanged(Arrays.toString(CC)));
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
                        int rx = (IR >> 8) & 0x03;
                        int ry = (IR >> 6) & 0x03;

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
                        bus.post(new CChanged(Arrays.toString(CC)));
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
                        int rx = (IR >> 8) & 0x03;
                        int ry = (IR >> 6) & 0x03;
                        if ((rx < 0 || rx > 3) || (ry < 0 && ry > 3)) {
                            throw new IllegalArgumentException("TRR: rx and ry must be 0, 1, 2, 3.");
                        }

                        if (GPR[rx] == GPR[ry]) {
                            CC[3] = true; // EQUAL flag
                        } else {
                            CC[3] = false;
                        }
                        bus.post(new CChanged(Arrays.toString(CC)));
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
                        int rx = (IR >> 8) & 0x03;
                        int ry = (IR >> 6) & 0x03;
                        ;
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
                        int rx = (IR >> 8) & 0x03;
                        int ry = (IR >> 6) & 0x03;
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
                        int rx = (IR >> 8) & 0x03;
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
                    System.out.println("AWAITING IN");
                    int r = (IR >> 8) & 0x03;
                    int devid = (IR >> 3) & 0x1F;
                    if ((r < 0 || r > 3)) {
                        throw new IllegalArgumentException("IN: r must be 0, 1, 2, 3.");
                    }
                    if ((devid < 0 || devid == 1 || devid > 31)) {
                        throw new IllegalArgumentException(
                                "IN: devid must be 0 (keyboard), 2 (card reader), or 3-31 (misc)");
                    }
                    if (devid == 0) {
                        inputBaseAddr = GPR[r] & 0x7FF; // word address in 0..2047
                        waitingInDestReg = r;
                        waitingForConsoleInput = true;

                        if (running) {
                            cpuTick.stop();
                        }

                    } else {
                        // your existing device path (if any)
                        int value = 0;
                        GPR[r] = value & 0xFFFF;
                        bus.post(new GPRChanged(r, GPR[r]));
                    }
                    break;
                }
                case 062: { // OUT r, devid
                    try {
                        int r = (IR >> 8) & 0x03; // [9..8] OK
                        int devid = (IR >> 3) & 0x1F; // [7..3] ✅
                        int value = GPR[r] & 0xFFFF;
                        if ((r < 0 || r > 3)) {
                            throw new IllegalArgumentException("OUT: r must be 0, 1, 2, 3.");
                        }

                        if ((devid < 0 || (devid < 3 && devid != 1) || devid > 31)) {
                            throw new IllegalArgumentException("OUT:s devid must be 1 (printer) or 3-31 (misc)");
                        }
                        if (devid == 1) { // console / guidance
                            int base = GPR[0] & 0x7FF; // base address lives in R0
                            int count = GPR[3] & 0xFFFF; // count lives in R3
                            if (count <= 0)
                                count = 20; // default if unset
                            System.out.printf(
                                    "OUT sanity: R0=%04o R1=%04o R2=%04o R3(dec)=%d (octal %06o) IR=%06o MBR=%06o\n",
                                    GPR[0] & 0x7FF, GPR[1] & 0x7FF, GPR[2] & 0x7FF, GPR[3] & 0xFFFF,
                                    GPR[3] & 0xFFFF, IR & 0xFFFF, memory.MBR & 0xFFFF);
                            this.bus.post(new MessageChanged(String.format(
                                    "Enter %d numbers (decimal). They will be stored at %04o..%04o.",
                                    count, base, (base + count - 1) & 0x7FF)));
                        } else {
                            this.bus.post(new MessageChanged("OUT dbg: r=" + r + " devid=" + devid + " val=" + value));
                            System.out.println("OUT dbg: r=" + r + " devid=" + devid + " val=" + value);
                        }

                    } catch (Exception e) {
                        bus.post(new MessageChanged("OUT failed: " + e.getMessage()));
                        halt();
                    }
                    break;
                }

                default: {
                    bus.post(new MessageChanged("Unknown opcode. May be data: " + opcode));
                    break;
                }
            }

        }
    }

    /**
     * Halts execution of running programx.
     */
    public void halt() {
        running = false;
        if (this.cpuTick != null) {
            this.cpuTick.stop();
        }
        System.out.println(" mem output, 0020: " + memory.memory[0020] + " 0021: " + memory.memory[0021] + " 0022: "
                + memory.memory[0022] + " 0023: " + memory.memory[0023]);
        System.out.println("program halted");
        bus.post(new MessageChanged("Program halted"));
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
        this.cpuTick = new Timer(500, e -> {

            step();
            if (!running) {
                ((javax.swing.Timer) e.getSource()).stop();
                return;
            }
        });
        this.cpuTick.setInitialDelay(0);
        this.cpuTick.start();
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
    //written with GPT assistance
    private int floatingAdd(int f1, int f2, boolean subtract) {
        int s1 = (f1 >> 15) & 1;
        int e1 = (f1 >> 8) & 0x7F;
        int m1 = (f1 & 0xFF) | 0x100;

        int s2 = (f2 >> 15) & 1;
        int e2 = (f2 >> 8) & 0x7F;
        int m2 = (f2 & 0xFF) | 0x100;

        if (subtract) s2 ^= 1;

        while (e1 > e2) { m2 >>= 1; e2++; }
        while (e2 > e1) { m1 >>= 1; e1++; }

        if (s1 == 1) m1 = -m1;
        if (s2 == 1) m2 = -m2;

        int result = m1 + m2;
        int sign = result < 0 ? 1 : 0;
        result = Math.abs(result);

        while (result > 0x1FF) result >>= 1, e1++;
        while (result < 0x100 && result != 0) result <<= 1, e1--;

        return (sign << 15) | ((e1 & 0x7F) << 8) | (result & 0xFF);
    }

    //witten with GPT assistance
    private int fixedToFloating(int val) {
        int sign = val < 0 ? 1 : 0;
        val = Math.abs(val);
        int exp = 0;

        while (val > 255) {
            val >>= 1;
            exp++;
        }

        exp &= 0x7F;
        return (sign << 15) | (exp << 8) | (val & 0xFF);
    }
    //witten with GPT assistance
    private int floatingToFixed(int val) {
        int sign = (val >> 15) & 1;
        int exp = (val >> 8) & 0x7F;
        int mant = val & 0xFF;
        mant |= 0x100;

        while (exp > 0) {
            mant <<= 1;
            exp--;
        }

        return sign == 1 ? -mant : mant;
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
