package group11.core;

import java.io.File;

//call using the below in gui 
//CPU cpu = new CPU(memory);
//cpu.run();
public class CPU {
    public int[] GPR = new int[4];    // R0-R3
    public int[] IXR = new int[4];    // X1-X3
    public int PC;                    // Program Counter
    public int IR;                    // Instruction Register
    public int MAR;                   // CPU copy of MAR
    public int MBR;                   // CPU copy of MBR
    public File selectedFile;          // loaded program file (if applicable)

    public boolean running = false;
    public Memory memory;

   public int address; 
    public int R;       
    public int IX;      
    public int I;       
    public int opcode; 

    public CPU(Memory memory) {
        this.memory = memory;
    }

    // FETCH
    private void fetch() {
        MAR = PC;
        memory.writeMARAddress(MAR);     // Send address to memory
        memory.readMemory(MAR);          // Memory loads MBR
        MBR = memory.readMBR();          // CPU copies it
        IR = MBR;                        // Place into IR
        PC++;
    }

    public void load() {
        memory.readMemory(address);
        GPR[R] = memory.readMBR();;
    }

    public void store() {
        int reg = (IR >> 8) & 0x03;
        int addr = IR & 0xFF;

        memory.writeMemory(addr, GPR[reg]);
        //System.out.printf("STORE M[%03o] ← R%d = %06o\n", addr, reg, GPR[reg]);
    }

    public void step() {
        fetch();
        decodeAndExecute();
    }

    public void loadPlus(int registerNumber, int address) {
        try {
            memory.writeMARAddress(address);
            memory.readMemory(address);
            int data = memory.readMBR();
            GPR[registerNumber] = data;
            int newMAR = memory.readMARAddress() + 1;
            memory.writeMARAddress(newMAR);
        } catch (Exception e) {
            System.err.println("Error in loadPlus: " + e.getMessage());
        }
    }


    public void storePlus(int registerNumber, int address) {
        try {
            memory.writeMARAddress(address);
            int data = GPR[registerNumber];// data must be fetched from the UI
            memory.writeMBR(data);
            memory.writeMemory(address, data);
            int newMAR = memory.readMARAddress() + 1;
            memory.writeMARAddress(newMAR);
        } catch (Exception e) {
            System.err.println("Error in storePlus: " + e.getMessage());
        }
    }

    private void decodeAndExecute() {
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
            case 1 -> load();   
            case 2 -> store();
            case 0 -> halt();
            default -> System.out.println("Unknown opcode: " + opcode);
        }
    }
    public void halt() {
        running = false;
        //System.out.println("HALT executed.");
    }

    public void run() {
        running = true;
        while (running) {
            step();
            try {
                Thread.sleep(200); // UI blink delay
            } catch (InterruptedException ignored) {}
        }
    }
    
}
