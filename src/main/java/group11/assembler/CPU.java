package group11.assembler;

//call using the below in gui 
//CPU cpu = new CPU(memory);
//cpu.run();
public class CPU {
    private int[] GPR = new int[4];    // R0-R3
    private int[] IXR = new int[4];    // X1-X3
    private int PC;                    // Program Counter
    private int IR;                    // Instruction Register
    private int MAR;                   // CPU copy of MAR
    private int MBR;                   // CPU copy of MBR

    private boolean running = false;
    private Memory memory;

    private int address; 
    private int R;       
    private int IX;      
    private int I;       
    private int opcode; 

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


    private void executeLOAD() {
        memory.readMemory(address);
        GPR[R] = memory.readMBR();;
    }

    private void executeSTORE() {
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
            case 1 -> executeLOAD();   
            case 2 -> executeSTORE();
            case 0 -> halt();
            default -> System.out.println("Unknown opcode: " + opcode);
        }
    }
    private void halt() {
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
