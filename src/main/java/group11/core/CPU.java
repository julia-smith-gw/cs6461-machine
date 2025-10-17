package group11.core;

import java.io.File;

import group11.events.EventBus;
import group11.events.GPRChanged;
import group11.events.IRChanged;
import group11.events.IXRChanged;
import group11.events.MARChanged;
import group11.events.MBRChanged;
import group11.events.PCChanged;
import group11.events.SetGPR;
import group11.events.SetIXR;
import group11.events.SetMAR;
import group11.events.SetMBR;
import group11.events.SetPC;

//call using the below in gui 
//CPU cpu = new CPU(memory);
//cpu.run();
public class CPU implements AutoCloseable  {
    // event bus and subscriptions
    private final EventBus bus;

    private final AutoCloseable SetGPRSub;
    private final AutoCloseable SetIXRSub;
    private final AutoCloseable SetPCSub;
    private final AutoCloseable SetMARSub;
    private final AutoCloseable SetMBRSub;

    public int[] GPR = new int[4];    // R0-R3
    public int[] IXR = new int[4];    // X1-X3
    public int PC;                    // Program Counter
    public int IR;                    // Instruction Register

    public File selectedFile;          // loaded program file (if applicable)

    public boolean running = false;
    public Memory memory;

    public int address; 
    public int R;       
    public int IX;      
    public int I;       
    public int opcode; 

    public CPU(Memory memory, EventBus bus) {
        this.memory = memory;
        this.bus = bus;
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

    // FETCH
    private void fetch() {
        this.memory.writeMARAddress(PC); // Send address to memory
        this.memory.readMemory(PC);      // Memory loads MBR
        this.IR = memory.readMBR();      // Place into IR
        this.PC++;
        System.out.println("FETCHED FROM MEMORY");
        System.out.println(memory.toString());
        System.out.println(this.memory.MAR);
        System.out.println(this.memory.MBR);
        this.bus.post(new MARChanged(this.memory.MAR));
        this.bus.post(new MBRChanged(this.memory.MBR));
        this.bus.post(new IRChanged(this.IR));
        this.bus.post(new PCChanged(this.PC));
    }

    public void load(){
        memory.readMemory(address);
        this.bus.post(new MARChanged(this.memory.MBR));
        this.bus.post(new MBRChanged(this.memory.MBR));
        GPR[R] = memory.readMBR();
        this.bus.post(new GPRChanged(R, GPR[R]));
    }

    public void loadFrontPanel() {
        memory.readMemory(this.memory.MAR);
        // this.MBR = memory.readMemory(address) 
        // GPR[R] = memory.readMBR();
    }

    public void storeFrontPanel(){
        memory.writeMemory(this.memory.MAR, this.memory.MBR);
    }

    public void store() {
        int reg = (IR >> 8) & 0x03;
        int addr = IR & 0xFF;

        memory.writeMemory(addr, GPR[reg]);
        //System.out.printf("STORE M[%03o] ← R%d = %06o\n", addr, reg, GPR[reg]);
    }

    public void step() {
        System.out.println("step");
        fetch();
        decodeAndExecute();
    }

    public void loadPlus() {
        try {
            memory.writeMARAddress(address);
            memory.readMemory(address);
            this.bus.post(new MARChanged(this.memory.MAR));
            this.bus.post(new MBRChanged(this.memory.MBR));
            // int data = memory.readMBR();
            //GPR[registerNumber] = data;
            int newMAR = memory.readMARAddress() + 1;
            memory.writeMARAddress(newMAR);
        } catch (Exception e) {
            System.err.println("Error in loadPlus: " + e.getMessage());
        }
    }

    public void storePlus() {
        try {
            memory.writeMARAddress(address);
            this.bus.post(new MARChanged(this.memory.MAR));
            // int data = GPR[registerNumber];// data must be fetched from the UI
            // memory.writeMBR(data);
            memory.writeMemory(this.memory.MAR, this.memory.MBR);
            int newMAR = memory.readMARAddress() + 1;
            memory.writeMARAddress(newMAR);
        } catch (Exception e) {
            System.err.println("Error in storePlus: " + e.getMessage());
        }
    }

    public void loadIndexRegister() {
       memory.readMemory(address);
       this.bus.post(new MARChanged(this.memory.MAR));
       this.bus.post(new MBRChanged(this.memory.MBR));
       IXR[R] = memory.readMBR();
       this.bus.post(new IXRChanged(R, IXR[R]));
    }

    public void storeIndexRegister(){
        int reg = (IR >> 8) & 0x03;
        int addr = IR & 0xFF;

        memory.writeMemory(addr, IXR[reg]);
    }

    private void decodeAndExecute() {
        try {
       opcode = (IR >> 10) & 0x3F; // Top 6 bits
        R = (IR >> 8) & 0x03;       // Next 2 bits
        IX = (IR >> 6) & 0x03;      // Next 2 bits
        I = (IR >> 5) & 0x01;       // Next 1 bit
        address = IR & 0x1F;        // Last 5 bits
        if (IX != 0) {
            address += IXR[IX];     // Add index register if used
        }
        if (I == 1) {
            memory.readMemory(address);
            address = memory.readMBR();  // M[EA] contains pointer to actual location
        }
        switch (opcode) {
            case 00 -> halt(); 
            case 01 -> load();   
            case 02 -> store();
            case 041 -> loadIndexRegister();
            case 042 -> storeIndexRegister();
            default -> System.out.println("Unknown opcode: " + opcode);
        }
        } catch(Exception e) {
            System.out.println("EXCEPTION OCCURRED");
            e.printStackTrace();
        }
 
    }
    public void halt() {
        running = false;
        //System.out.println("HALT executed.");
    }

    public void run() {
        System.out.println("RUN PLEASE");
        running = true;
        while (running) {
            step();
            try {
                Thread.sleep(200); // UI blink delay
            } catch (InterruptedException ignored) {}
        }
    }

    @Override public void close() {
        try { SetGPRSub.close(); } catch (Exception ignored) {}
        try { SetIXRSub.close(); } catch (Exception ignored) {}
        try { SetMARSub.close(); } catch (Exception ignored) {}
        try { SetMBRSub.close(); } catch (Exception ignored) {}
        try { SetPCSub.close(); } catch (Exception ignored) {}
    }
    
}
