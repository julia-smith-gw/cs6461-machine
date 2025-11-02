package group11.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import group11.events.EventBus;

//https://chatgpt.com/share/6906f0ff-36d8-8007-a4d1-f8dac5a1f999
class ShiftRotateInstructionsTest {

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

    private void setCC(int bit, boolean on) {
        cpu.CC[bit] = on;
    }

    private void poke(int addr, int word) {
        mem.writeMemory(addr & 0xFFF, word & 0xFFFF);
    }

    /** Execute a single instruction located at current PC. */
    private void execAtPC(int instr) {
        poke(getPC(), instr);
        cpu.step();
    }

    /*
     * ------------- Encoding helpers (adjust if your bit layout differs)
     * -------------
     * Common student layout for SRC/RRC (16-bit word):
     * [15..10]=opcode, [9..8]=R, [7..4]=count(0..15), [3]=LR (0=L,1=R), [2]=AL
     * (0=L,1=A), [1..0]=0
     * opcodes: SRC=031₈, RRC=032₈
     * -----------------------------------------------------------------------------
     * --
     */
    // ------- Encoding helpers (match your bitfields) -------
    // Common student layout you’re using for SRC/RRC:
    // [15..10]=opcode, [9..8]=R, [7..4]=count, [3]=L/R, [2]=A/L, [1..0]=ignored
    // ISA polarity: L/R: 1=Left, 0=Right ; A/L: 1=Logical, 0=Arithmetic
    private static int OCT(String s) {
        return Integer.parseInt(s, 8);
    }

    private static int encShift(int opcodeOct, int r, int count, int LR, int AL) {
        return ((opcodeOct & 0x3F) << 10)
                | ((r & 0x03) << 8)
                | ((count & 0x0F) << 4)
                | ((LR & 0x01) << 3)
                | ((AL & 0x01) << 2);
    }

    /* =============================== SRC =============================== */
    @Test
    void SRC_left_logical_shifts_in_zeros() {
        // R0 = 0x0003; shift left by 6 (LR=1), logical (AL=1) => 0x00C0
        setGPR(0, 0x0003);
        int instr = encShift(OCT("031"), /* r */0, /* count */6, /* LR(left) */1, /* AL(logical) */1);
        execAtPC(instr);
        assertEquals(0x00C0, getGPR(0), "Left logical should insert zeros");
    }

    @Test
    void SRC_right_logical_inserts_zeros() {
        // 0b1011_0000_0000_0000 >>> 4 => 0b0000_1011_0000_0000
        setGPR(1, 0b1011_0000_0000_0000);
        int instr = encShift(OCT("031"), 1, 4, /* LR(right) */0, /* AL(logical) */1);
        execAtPC(instr);
        assertEquals(0b0000_1011_0000_0000, getGPR(1));
    }

    @Test
    void SRC_right_arithmetic_preserves_sign_bit() {
        // negative value (sign bit 1). SAR by 3 keeps sign-propagation.
        setGPR(2, 0b1001_0000_0000_0000); // 0x9000
        int instr = encShift(OCT("031"), 2, 3, /* LR(right) */0, /* AL(arith) */0);
        execAtPC(instr);
        assertEquals(0b1111_0010_0000_0000, getGPR(2), "Arithmetic right should replicate sign bit");
    }

    @Test
    void SRC_left_logical_masks_to_16_bits() {
        // Shift left logical by 12; drop bits beyond 16
        setGPR(3, 0xABCD);
        int instr = encShift(OCT("031"), 3, 12, /* LR(left) */1, /* AL(logical) */1);
        execAtPC(instr);
        assertEquals((0xABCD << 12) & 0xFFFF, getGPR(3)); // 0xD000
    }

    @Test
    void SRC_count_zero_is_noop() {
        setGPR(0, 0x5A5A);
        int instr = encShift(OCT("031"), 0, 0, /* LR(any) */1, /* AL(any) */1);
        execAtPC(instr);
        assertEquals(0x5A5A, getGPR(0), "Count=0 should skip operation");
    }

    @Test
    void SRC_right_logical_count_15_boundary() {
        // Count range is 0..15. Using 15 as a boundary.
        setGPR(1, 0b1000_0000_0000_0001);
        int instr = encShift(OCT("031"), 1, 15, /* LR(right) */0, /* AL(logical) */1);
        execAtPC(instr);
        assertEquals(0b0000_0000_0000_0001, getGPR(1));
    }

    /* =============================== RRC =============================== */
    @Test
    void RRC_left_rotates_bits_back_in_from_right() {
        // R1 = 0x8001; ROL by 1 (LR=1) => 0x0003
        setGPR(1, 0x8001);
        int instr = encShift(OCT("032"), /* r */1, /* count */1, /* LR(left) */1, /* AL(ignored) */0);
        execAtPC(instr);
        assertEquals(0x0003, getGPR(1));
    }

    @Test
    void RRC_right_rotates_bits_back_in_from_left() {
        // R2 = 0x0003; ROR by 1 (LR=0) => 0x8001
        setGPR(2, 0x0003);
        int instr = encShift(OCT("032"), 2, 1, /* LR(right) */0, /* AL(ignored) */0);
        execAtPC(instr);
        assertEquals(0x8001, getGPR(2));
    }

    @Test
    void RRC_rotation_by_8_swaps_high_low_bytes() {
        setGPR(3, 0x12AB);
        int rol8 = encShift(OCT("032"), 3, 8, /* LR(left) */1, 0);
        execAtPC(rol8);
        assertEquals(0xAB12, getGPR(3));

        int ror8 = encShift(OCT("032"), 3, 8, /* LR(right) */0, 0);
        execAtPC(ror8);
        assertEquals(0x12AB, getGPR(3));
    }

    @Test
    void RRC_count_zero_is_noop() {
        setGPR(0, 0xCAFE);
        int instr = encShift(OCT("032"), 0, 0, /* LR(any) */1, 1);
        execAtPC(instr);
        assertEquals(0xCAFE, getGPR(0));
    }

}