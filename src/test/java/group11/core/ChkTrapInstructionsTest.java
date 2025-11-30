package group11.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import group11.events.EventBus;

public class ChkTrapInstructionsTest {
     private CPU cpu;
    private Memory mem;

    @BeforeEach
    void setup() {

        mem = new Memory();
        mem.MAR = 0;
        mem.MBR = 0;
        EventBus eventBus = new EventBus();
        RomLoader romLoader = new RomLoader(mem);
        Cache cache = new Cache(mem, eventBus);
        cpu = new CPU(mem, eventBus, cache, romLoader);
        // Clear regs and PC
        setPC(6); // per project, 6 is first non-reserved location
        for (int r = 0; r < 4; r++)
            setGPR(r, 0);
        for (int x = 1; x <= 3; x++)
            setIX(x, 0);
        setCC(0, false);
        setCC(1, false);
        setCC(2, false);
        setCC(3, false);
    }

    /*
     * -------------------------- Adapters (tweak here) --------------------------
     */

    private void setPC(int v) {
        cpu.PC = (v & 0xFFF);
    }

    private int getPC() {
        return cpu.PC & 0xFFF;
    }

    private void setGPR(int r, int v) {
        cpu.GPR[r] = (v & 0xFFFF);
    }

    private int getGPR(int r) {
        return cpu.GPR[r] & 0xFFFF;
    }

    private void setIX(int x, int v) {
        cpu.IXR[x] = v & 0xFFFF;
    }

    private int getIX(int x) {
        return cpu.IXR[x] & 0xFFFF;
    }

    private void setCC(int bit, boolean on) {
        cpu.CC[bit] = on;
    }

    private boolean getCC(int bit) {
        return cpu.CC[bit];
    }

    private void poke(int addr, int word) {
        mem.writeMemory(addr & 0xFFF, word & 0xFFFF);
    }

    private int peek(int addr) {
        return mem.getMemoryValueAt(addr & 0xFFF) & 0xFFFF;
    }

    /** Execute a single instruction located at current PC. */
    private void execAtPC(int instr) {
        poke(getPC(), instr);
        cpu.step();
    }

    /**
     * Encode using ISA B (Load/Store/Transfer layout).
     * opcodeOct is *octal* (e.g., 010 for JZ), rOrCc=0..3, ix=0..3, i=0/1,
     * addr=5-bit (0..31).
     */
    private static int enc(int opcodeOct, int rOrCc, int ix, int i, int addr5) {
        return ((opcodeOct & 0x3F) << 10)
                | ((rOrCc & 0x03) << 8)
                | ((ix & 0x03) << 6)
                | ((i & 0x01) << 5)
                | (addr5 & 0x1F);
    }

    /**
     * Compute EA the same way as your CPU should for these instructions. (Used in
     * test setup.)
     * EA = A + (ix==0 ? 0 : c(IX[ix])); if I==1 then EA = M[EA].
     */
    private int ea(int a5, int ix, int i) {
        int base = a5 & 0x1F;
        int idx = (ix == 0) ? 0 : getIX(ix);
        int ea = (base + idx) & 0xFFF;
        if (i == 1)
            ea = peek(ea) & 0xFFF;
        return ea;
    }
}
