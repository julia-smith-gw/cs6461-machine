package group11;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.util.function.IntConsumer;
import group11.events.EventBus;
import group11.core.Memory;
import group11.core.Cache;
import group11.core.RomLoader;
import group11.core.CPU;

/**
 * Unit tests for C6461 Transfer instructions:
 * JZ, JNE, JCC, JMA, JSR, RFS, SOB, JGE.
 *
 * Assumptions about your API — change these adapters to match your classes:
 *  - CPU has 16-bit GPR[0..3], IXR[1..3], a 12-bit PC, 4-bit CC (bits 0..3),
 *    and executes one instruction via cpu.step().
 *  - Memory is word-addressable (0..2047), read/write 16-bit words.
 *  - CPU exposes getters/setters used below (or replace with your own).
 */
public class TransferInstructionsTest {

    private CPU cpu;
    private Memory mem;

    @BeforeEach
    void setup() {
   
        Memory mem = new Memory();
        EventBus eventBus = new EventBus();
        RomLoader romLoader = new RomLoader(mem);
        Cache cache = new Cache(mem, eventBus);
        CPU cpu = new CPU(mem, eventBus, cache, romLoader);
        // Clear regs and PC
        setPC(6); // per project, 6 is first non-reserved location
        for (int r = 0; r < 4; r++) setGPR(r, 0);
        for (int x = 1; x <= 3; x++) setIX(x, 0);
        setCC(0, false); setCC(1, false); setCC(2, false); setCC(3, false);
    }

    /* -------------------------- Adapters (tweak here) -------------------------- */

    private void setPC(int v) { cpu.PC = (v & 0xFFF); }
    private int  getPC() { return cpu.PC & 0xFFF; }

    private void setGPR(int r, int v) { cpu.GPR[r] = (v & 0xFFFF); }
    private int  getGPR(int r) { return cpu.GPR[r] & 0xFFFF; }

    private void setIX(int x, int v) { cpu.IXR[x] = v & 0xFFFF; }
    private int  getIX(int x) { return cpu.IXR[x] & 0xFFFF; }

    private void setCC(int bit, boolean on) { cpu.CC[bit]=on; }
    private boolean getCC(int bit) { return cpu.CC[bit]; }

    private void poke(int addr, int word) { mem.writeMemoryDirect(addr & 0xFFF, word & 0xFFFF); }
    private int  peek(int addr) { return mem.getMemoryValueAt(addr & 0xFFF) & 0xFFFF; }

    /** Execute a single instruction located at current PC. */
    private void execAtPC(int instr) {
        poke(getPC(), instr);
        cpu.step();
    }

    /** Encode using ISA B (Load/Store/Transfer layout).
     *  opcodeOct is *octal* (e.g., 010 for JZ), rOrCc=0..3, ix=0..3, i=0/1, addr=5-bit (0..31).
     */
    private static int enc(int opcodeOct, int rOrCc, int ix, int i, int addr5) {
        int opcode = Integer.parseInt(Integer.toString(opcodeOct), 8); // octal→int
        return ((opcode & 0x3F) << 10)
             | ((rOrCc & 0x03) << 8)
             | ((ix & 0x03) << 6)
             | ((i & 0x01) << 5)
             |  (addr5 & 0x1F);
    }

    /** Compute EA the same way as your CPU should for these instructions. (Used in test setup.)
     *  EA = A + (ix==0 ? 0 : c(IX[ix])); if I==1 then EA = M[EA].
     */
    private int ea(int a5, int ix, int i) {
        int base = a5 & 0x1F;
        int idx = (ix == 0) ? 0 : getIX(ix);
        int ea = (base + idx) & 0xFFF;
        if (i == 1) ea = peek(ea) & 0xFFF;
        return ea;
    }

    /* ------------------------------- Tests ------------------------------------ */

    @Test
    void JZ_taken_when_r_is_zero() {
        setPC(20);
        setGPR(0, 0);
        int ix = 0, i = 0, a = 12;                  // EA = 12
        int instr = enc(010, /*JZ*/ 0, ix, i, a);   // JZ r=0
        execAtPC(instr);
        assertEquals(ea(a, ix, i), getPC(), "PC should jump to EA when R==0");
    }

    @Test
    void JZ_not_taken_when_r_nonzero() {
        setPC(30);
        setGPR(0, 5);
        int instr = enc(010, 0, 0, 0, 7); // JZ r=0, EA=7
        execAtPC(instr);
        assertEquals(31, getPC(), "PC should advance by 1 when R!=0");
     }

    // @Test
    // void JNE_taken_when_r_nonzero() {
    //     setPC(100);
    //     setGPR(1, 3);
    //     int instr = enc(011, 1, 0, 0, 25); // JNE r=1, EA=25
    //     execAtPC(instr);
    //     assertEquals(25, getPC(), "PC should jump to EA when R!=0");
    // }

    // @Test
    // void JNE_not_taken_when_r_zero() {
    //     setPC(101);
    //     setGPR(1, 0);
    //     int instr = enc(011, 1, 0, 0, 25);
    //     execAtPC(instr);
    //     assertEquals(102, getPC(), "PC should advance by 1 when R==0");
    // }

    // @Test
    // void JCC_taken_when_CC_bit_is_one() {
    //     setPC(200);
    //     setCC(2, true);                 // cc=2 set
    //     int instr = enc(012, /*cc*/2, 0, 0, 17); // JCC cc=2, EA=17
    //     execAtPC(instr);
    //     assertEquals(17, getPC(), "PC should jump to EA when CC[2]==1");
    // }

    // @Test
    // void JCC_not_taken_when_CC_bit_is_zero() {
    //     setPC(201);
    //     setCC(1, false);                // ensure off
    //     int instr = enc(012, /*cc*/1, 0, 0, 17);
    //     execAtPC(instr);
    //     assertEquals(202, getPC(), "PC should advance by 1 when CC bit==0");
    // }

    // @Test
    // void JMA_unconditional_jump_direct() {
    //     setPC(300);
    //     setGPR(3, 0xBEEF); // should be ignored
    //     int instr = enc(013, /*r ignored*/0, 0, 0, 31); // JMA EA=31
    //     execAtPC(instr);
    //     assertEquals(31, getPC(), "JMA must unconditionally set PC=EA");
    // }

    // @Test
    // void JMA_with_index_and_indirect_addressing() {
    //     setPC(301);
    //     setIX(2, 40);
    //     // Base A=10; IX2=40 => temp EA=50; I=1 => final EA = M[50]
    //     poke(50, 77);
    //     int instr = enc(013, 0, /*ix*/2, /*I*/1, /*A*/10);
    //     execAtPC(instr);
    //     assertEquals(77, getPC(), "JMA should use EA with indexing and optional indirection");
    // }

    // @Test
    // void JSR_saves_return_in_R3_and_jumps_to_EA() {
    //     setPC(400);
    //     setIX(1, 20);
    //     int ea = ea(/*A*/12, /*ix*/1, /*I*/0); // 12+20=32
    //     int instr = enc(014, /*r ignored*/0, 1, 0, 12); // JSR
    //     execAtPC(instr);
    //     assertEquals(401, getGPR(3), "R3 should receive PC+1");
    //     assertEquals(ea, getPC(), "PC should jump to EA");
    // }

    // @Test
    // void RFS_loads_R0_with_immed_and_returns_to_R3() {
    //     // Arrange: pretend a subroutine set R3 to return point 512+1
    //     setGPR(3, 512);
    //     setPC(500);
    //     int immed = 0o07;                   // octal 7, small positive code
    //     int instr = enc(015, /*ignored*/0, /*ix*/0, /*I*/0, /*addr=immed*/immed);
    //     execAtPC(instr);
    //     assertEquals(immed, getGPR(0) & 0x1F, "R0 should receive 5-bit Immed value");
    //     assertEquals(512, getPC(), "PC should return to c(R3)");
    // }

    // @Test
    // void SOB_taken_when_result_gt_zero() {
    //     setPC(600);
    //     setGPR(2, 2);                    // after decrement => 1 > 0
    //     int instr = enc(016, /*r*/2, 0, 0, 5); // EA=5
    //     execAtPC(instr);
    //     assertEquals(1, getGPR(2), "SOB must decrement the register first");
    //     assertEquals(5, getPC(), "Branch taken when new value > 0");
    // }

    // @Test
    // void SOB_not_taken_when_result_eq_zero() {
    //     setPC(601);
    //     setGPR(2, 1);                    // after decrement => 0 -> not > 0
    //     int instr = enc(016, 2, 0, 0, 5);
    //     execAtPC(instr);
    //     assertEquals(0, getGPR(2), "Register decremented to 0");
    //     assertEquals(602, getPC(), "Advance by 1 when new value is not > 0");
    // }

    // @Test
    // void JGE_taken_on_nonnegative_values_including_zero() {
    //     setPC(700);
    //     setGPR(1, 0);                    // nonnegative
    //     int instr = enc(017, /*r*/1, 0, 0, 22);
    //     execAtPC(instr);
    //     assertEquals(22, getPC(), "Jump when c(r) >= 0 (zero is allowed)");
    // }

    // @Test
    // void JGE_not_taken_on_negative_value() {
    //     setPC(701);
    //     setGPR(1, 0xFFFF);               // -1 in two's complement
    //     int instr = enc(017, 1, 0, 0, 22);
    //     execAtPC(instr);
    //     assertEquals(702, getPC(), "Do not jump when c(r) < 0");
    // }
}
