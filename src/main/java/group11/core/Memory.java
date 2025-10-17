//written with GPT assistance 
package group11.core;


/**
 * Simple memory emulation containing 2048 words.
 */
public class Memory {

    public final int MEMORY_SIZE = 2048;
    public int[] memory;
    public Integer MAR;
    public Integer MBR;

    public Memory() {
        memory = new int[MEMORY_SIZE];
        reset();
    }

    public int readMARAddress() {
        return MAR;
    }

    public void writeMARAddress(int address) {
        checkAddress(address);
        this.MAR = address;

    }

    public void writeMBR(int data) {
        this.MBR = data & 0xFFFF;
    }

    public int readMBR() {
        return this.MBR;
    }

    public void writeMemory(int address, int data) {
        checkAddress(address);
        writeMARAddress(address);
        writeMBR(data);
        memory[MAR] = MBR;

    }

    public void readMemory(int address) {
        checkAddress(address);
        writeMARAddress(address);
        MBR = memory[MAR];
    }

    public void reset() {
        for (int i = 0; i < MEMORY_SIZE; i++) {
            memory[i] = 0;
        }
        MAR = null;
        MBR = null;
    }

    private void checkAddress(int address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Address out of bounds: " + address);
        }
    }

    public void dump(int start, int end) {
        checkAddress(start);
        checkAddress(end);
        for (int i = start; i <= end; i++) {
            System.out.printf("Addr %04d : %04X\n", i, memory[i]);
        }
    }
}
