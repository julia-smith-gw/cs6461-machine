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

    public CPU(Memory memory, EventBus bus, RomLoader romLoader) {
        this.memory = memory;
        this.bus = bus;
        this.romLoader = romLoader;

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
        memory.readMemory(effectiveAddress);
        this.bus.post(new MARChanged(this.memory.MAR));
        this.bus.post(new MBRChanged(this.memory.MBR));
        IXR[R] = memory.readMBR();
        this.bus.post(new IXRChanged(R, IXR[R]));
    }

    /**
     * Op code STX. Stores index register value into memory.
     */
    public void storeIndexRegister() {
        memory.writeMemory(effectiveAddress, IXR[R]);
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
        
        // check returned address if indirect or if calculated based on index register above
        try {
            getIsValidAddress(running, pendingEffectiveAddress);
        } catch (Exception e) {
            throw e;
        }

        this.effectiveAddress = pendingEffectiveAddress;
        R = r;
        IX = ix;
        I = i;
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
                case 01: {
                    try {
                        this.setEffectiveAddress(opcode);
                        memory.readMemory(effectiveAddress);
                        this.bus.post(new MARChanged(this.memory.MAR));
                        this.bus.post(new MBRChanged(this.memory.MBR));
                        GPR[R] = memory.readMBR();
                        this.bus.post(new GPRChanged(R, GPR[R]));
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        this.bus.post(new MessageChanged(e.getMessage()));
                        this.halt();
                    }
                    break;
                }
                case 02: {
                    try {
                        this.setEffectiveAddress(opcode);
                        memory.writeMemory(effectiveAddress, GPR[R]);
                        this.bus.post(new MARChanged(this.memory.MAR));
                        this.bus.post(new MBRChanged(this.memory.MBR));
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        this.bus.post(new MessageChanged(e.getMessage()));
                        this.halt();
                    }
                    break;
                }
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
