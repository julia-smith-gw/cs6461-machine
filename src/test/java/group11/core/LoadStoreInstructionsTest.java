package group11.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import group11.events.EventBus;

//https://chatgpt.com/share/6906f0ff-36d8-8007-a4d1-f8dac5a1f999
class LoadStoreInstructionsTest {

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
     * ISA B (load/store/transfer/arithmetic-to-memory) encode helper.
     * opcodeOct: pass as octal literal, e.g. 01, 02, 03, 041, 042
     * r: GPR for LDR/STR/LDA; ignored for LDX/STX
     * ix: 0..3 (for EA; for LDX/STX this selects the index register x)
     * i: 0=direct, 1=indirect
     * addr5: 0..31
     */
    private static int encLS(int opcodeOct, int r, int ix, int i, int addr5) {
        int opcode = opcodeOct; // already octal at call site
        return ((opcode & 0x3F) << 10)
                | ((r & 0x03) << 8)
                | ((ix & 0x03) << 6)
                | ((i & 0x01) << 5)
                | (addr5 & 0x1F);
    }

    /* ================================ LDR ================================ */

    @Test
    void LDR_direct_loads_word_at_EA_into_register() {
        poke(20, 0xBEEF);
        int instr = encLS(01, /* r */0, /* ix */0, /* i */0, /* A */20);
        execAtPC(instr);
        assertEquals(0xBEEF, getGPR(0));
    }

    @Test
    void LDR_indexed_and_indirect_addressing() {
        // IX2=40; A=10 => tempEA=50; I=1 => finalEA=M[50]=77; M[77]=0x1234
        setIX(2, 40);
        poke(50, 77);
        poke(77, 0x1234);
        int instr = encLS(01, /* r */1, /* ix */2, /* i */1, /* A */10);
        execAtPC(instr);
        assertEquals(0x1234, getGPR(1));
    }

    @Test
    void LDR_masks_to_16_bits() {
        poke(7, 0x1FFFF); // ensure high bits are dropped
        int instr = encLS(01, 2, 0, 0, 7);
        execAtPC(instr);
        assertEquals(0xFFFF, getGPR(2));
    }

    /* ================================ STR ================================ */

    @Test
    void STR_writes_register_value_to_EA_and_can_be_read_back() {
        setGPR(3, 0xCAFE);
        int instrSTR = encLS(02, /* r */3, /* ix */0, /* i */0, /* A */31);
        execAtPC(instrSTR);

        // Read back via LDR (goes through cache/load path)
        setGPR(0, 0);
        int instrLDR = encLS(01, /* r */0, /* ix */0, /* i */0, /* A */31);
        execAtPC(instrLDR);
        assertEquals(0xCAFE, getGPR(0), "Stored value should be readable via LDR");
    }

    @Test
    void STR_indexed_indirect_writes_to_final_EA_read_back_via_same_addressing() {
        // IX1 = 25; A=5 => tempEA=30; I=1 => finalEA=M[30] (we set to 90)
        setIX(1, 25);
        poke(30, 90);

        setGPR(0, 0x0042);
        int instrSTR = encLS(02, /* r */0, /* ix */1, /* i */1, /* A */5);
        execAtPC(instrSTR);

        // Read back using the *same* addressing mode (ix=1, i=1, A=5), not by absolute
        // 90.
        int instrLDR = encLS(01, /* r */2, /* ix */1, /* i */1, /* A */5);
        execAtPC(instrLDR);

        assertEquals(0x0042, getGPR(2), "STR then LDR (same IX/I/A) should round-trip the value");
    }

    /* ================================ LDX ================================ */

    @Test
    void LDX_indirect_ignores_index_register_for_EA() {
        // Goal: M[6] = 44 (pointer), M[44] = 0xFACE, then LDX ix=3, A=6, I=1 => IX3 =
        // 0xFACE

        // 1) pointer at 6 (A=6 is fine)
        setGPR(0, 44);
        execAtPC(encLS(02, /* STR */ 0, /* ix */0, /* i */0, /* A */6)); // M[6] = 44

        // 2) write 0xFACE to address 44 using INDEXING with LDR/STR
        setIX(1, 40); // IX1 = 40
        setGPR(0, 0xFACE);
        execAtPC(encLS(02, /* STR */ 0, /* ix */1, /* i */0, /* A */4)); // EA = 4 + IX1 = 44 → M[44] = 0xFACE

        // 3) sanity checks that avoid A>31
        execAtPC(encLS(01, /* LDR */ 0, /* ix */0, /* i */0, /* A */6)); // M[6]
        assertEquals(44, getGPR(0), "M[6] should be 44 before LDX");

        execAtPC(encLS(01, /* LDR */ 0, /* ix */1, /* i */0, /* A */4)); // EA=4+IX1=44
        assertEquals(0xFACE, getGPR(0), "M[44] should be 0xFACE before LDX");

        // 4) now perform LDX (no indexing for 041, but I=1 is honored)
        execAtPC(encLS(041, /* LDX */ 0, /* ix(select x=3) */3, /* i */1, /* A */6));
        assertEquals(0xFACE, getIX(3));
    }

    /* ================================ STX ================================ */

    @Test
    void STX_stores_index_register_value_to_EA_read_back_via_LDR() {
        // Select x=1 (ix=1)
        setIX(1, 0x1234);
        int instrSTX = encLS(042, /* r ignored */0, /* ix(select x=1) */1, /* i */0, /* A */9);
        execAtPC(instrSTX);

        // Read back
        int instrLDR = encLS(01, /* r */0, /* ix */0, /* i */0, /* A */9);
        execAtPC(instrLDR);
        assertEquals(0x1234, getGPR(0));
    }

    @Test
    void STX_indexed_indirect_stores_to_final_EA_read_back_via_same_addressing() {
        // Select x=2 and also use IX2 for indexing.
        // Build indirection chain with A=58 -> M[58]=75 (so EA=75).
        setIX(2, 0xABCD); // value to store
        poke(58, 75);

        int instrSTX = encLS(042, /* r ignored */0, /* ix(select x=2 & index by IX2) */2, /* i */1, /* A */58);
        execAtPC(instrSTX);

        // Read back using the *same* addressing (ix=2, i=1, A=58)
        int instrLDR = encLS(01, /* r */1, /* ix */2, /* i */1, /* A */58);
        execAtPC(instrLDR);

        assertEquals(0xABCD, getGPR(1), "STX then LDR (same IX/I/A) should round-trip the value");
    }

    /* ================================ Masking ================================ */

    @Test
    void store_is_16bit_masked_and_readable() {
        setGPR(0, 0x1ABCD); // 17 bits set; only low 16 should be stored
        int instrSTR = encLS(02, 0, 0, 0, 7);
        execAtPC(instrSTR);

        int instrLDR = encLS(01, 1, 0, 0, 7);
        execAtPC(instrLDR);
        assertEquals(0xABCD, getGPR(1));
    }
}
