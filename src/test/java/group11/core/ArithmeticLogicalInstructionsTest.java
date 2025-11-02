package group11.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import group11.events.EventBus;

//https://chatgpt.com/share/6906f0ff-36d8-8007-a4d1-f8dac5a1f999
class ArithmeticLogicalInstructionsTest {

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

    private boolean getCC(int bit) {
        return cpu.CC[bit];
    }

    private void poke(int addr, int word) {
        mem.writeMemory(addr & 0xFFF, word & 0xFFFF);
    }

    /** Execute a single instruction located at current PC. */
    private void execAtPC(int instr) {
        poke(getPC(), instr);
        cpu.step();
    }

    /** Load/Store/Arithmetic-to-memory style (Table 7): opcode in octal. */
    private static int encLS(int opcodeOct, int r, int ix, int i, int addr5) {
        return ((opcodeOct & 0x3F) << 10)
                | ((r & 0x03) << 8)
                | ((ix & 0x03) << 6)
                | ((i & 0x01) << 5)
                | (addr5 & 0x1F);
    }

    /** Register-to-register format (Table 8): opcode in octal. */
    private static int encRR(int opcodeOct, int rx, int ry) {
        return ((opcodeOct & 0x3F) << 10)
                | ((rx & 0x03) << 8)
                | ((ry & 0x03) << 6);
        // low 6 bits ignored
    }

    /*
     * ====================== Table 7: AMR / SMR / AIR / SIR =====================
     */

    @Test
    void AMR_adds_memory_to_register_with_indexing_and_indirection() {
        // r1 := r1 + M[EA]; EA uses IX2 and indirection
        setGPR(1, 0x0010);
        setIX(2, 40);
        // A=10 => tempEA=50; I=1 => finalEA=M[50]=77
        poke(50, 77);
        poke(77, 0x0005);
        int instr = encLS(004, /* r */1, /* ix */2, /* I */1, /* A */10); // AMR
        execAtPC(instr);
        assertEquals(0x0015, getGPR(1), "AMR should add memory into register");
    } // Spec: AMR r <- c(r) + c(EA). :contentReference[oaicite:1]{index=1}

    @Test
    void SMR_subtracts_memory_from_register() {
        setGPR(0, 0x0012);
        poke(20, 0x0007);
        int instr = encLS(005, /* r */0, /* ix */0, /* I */0, /* A */20); // SMR
        execAtPC(instr);
        assertEquals(0x000B, getGPR(0), "SMR should subtract memory from register");
    } // r <- c(r) - c(EA). :contentReference[oaicite:2]{index=2}

    @Test
    void AIR_immed_zero_is_noop() {
        setGPR(2, 0x1234);
        int instr = encLS(006, /* r */2, /* ix */0, /* I */0, /* immed */0); // AIR
        execAtPC(instr);
        assertEquals(0x1234, getGPR(2), "AIR with immed=0 does nothing");
    } // AIR notes: immed=0 => does nothing. :contentReference[oaicite:3]{index=3}

    @Test
    void AIR_loads_immed_when_register_is_zero() {
        setGPR(3, 0);
        int instr = encLS(006, /* r */3, 0, 0, /* immed */017); // octal 17 => 15
        execAtPC(instr);
        assertEquals(0x000F, getGPR(3) & 0xFFFF, "AIR loads immed when r==0");
    } // AIR note 2. :contentReference[oaicite:4]{index=4}

    @Test
    void SIR_immed_zero_is_noop() {
        setGPR(1, 0xABCD);
        int instr = encLS(007, /* r */1, 0, 0, /* immed */0);
        execAtPC(instr);
        assertEquals(0xABCD, getGPR(1), "SIR with immed=0 does nothing");
    } // SIR note 1. :contentReference[oaicite:5]{index=5}

    @Test
    void SIR_loads_negative_immed_when_register_is_zero() {
        setGPR(1, 0);
        int instr = encLS(007, /* r */1, 0, 0, /* immed */005); // 5
        execAtPC(instr);
        // loads r with -immed => 0xFFFF - 4 == 0xFFFB for 16-bit two's complement
        assertEquals(0xFFFF - 4, getGPR(1), "SIR loads -(immed) when r==0");
    } // SIR note 2. :contentReference[oaicite:6]{index=6}

    @Test
    void AMR_sets_overflow_flag_on_signed_overflow() {
        // Example: 0x7FFF (+32767) + 1 => 0x8000 (negative) => OVERFLOW set
        setGPR(0, 0x7FFF);
        poke(12, 0x0001);
        int instr = encLS(004, 0, 0, 0, 12);
        execAtPC(instr);
        assertEquals(0x8000, getGPR(0));
        assertTrue(getCC(0), "OVERFLOW flag should be set after signed overflow");
    } // Arithmetic ops set CC per ISA. :contentReference[oaicite:7]{index=7}

    @Test
    void SMR_sets_underflow_flag_on_signed_underflow() {
        // 0x8000 (-32768) - 1 => 0x7FFF with underflow
        setGPR(0, 0x8000);
        poke(10, 0x0001);
        int instr = encLS(005, 0, 0, 0, 10);
        execAtPC(instr);
        assertEquals(0x7FFF, getGPR(0));
        assertTrue(getCC(1), "UNDERFLOW flag should be set after signed underflow");
    } // CC bits defined as OVERFLOW/UNDERFLOW/DIVZERO/EQUAL.
      // :contentReference[oaicite:8]{index=8}

    /*
     * ==================== Table 8: MLT / DVD / TRR / AND / ORR / NOT
     * ====================
     */

    @Test
    void MLT_multiplies_16x16_into_rx_and_rxplus1() {
        // rx must be 0 or 2. Use rx=0, ry=2.
        setGPR(0, 300); // multiplicand
        setGPR(2, 40); // multiplier
        setGPR(1, 0xDEAD);
        int instr = encRR(070, /* rx */0, /* ry */2); // MLT
        execAtPC(instr);
        int product = 300 * 40; // 12000 = 0x2EE0
        assertEquals((product >>> 16) & 0xFFFF, getGPR(0), "High 16 bits go to rx");
        assertEquals(product & 0xFFFF, getGPR(1), "Low 16 bits go to rx+1");
        assertFalse(getCC(0), "No overflow for values that fit in 32 bits");
    } // MLT semantics. :contentReference[oaicite:9]{index=9}

    @Test
    void DVD_divides_rx_by_ry_puts_quotient_and_remainder_and_sets_divzero() {
        // Successful divide
        setGPR(0, 1000);
        setGPR(2, 64);
        int instr = encRR(071, /* rx */0, /* ry */2); // DVD
        execAtPC(instr);
        assertEquals(1000 / 64, getGPR(0), "Quotient in rx");
        assertEquals(1000 % 64, getGPR(1), "Remainder in rx+1");
        assertFalse(getCC(2), "DIVZERO flag should not be set");

        // Div-by-zero case sets DIVZERO
        setPC(getPC() + 1); // move along; next fetch
        setGPR(0, 10);
        setGPR(2, 0);
        execAtPC(instr);
        assertTrue(getCC(2), "DIVZERO flag must be set when dividing by zero");
    } // DVD semantics incl. DIVZERO. :contentReference[oaicite:10]{index=10}

    @Test
    void TRR_sets_equal_flag_when_rx_equals_ry_clears_otherwise() {
        setGPR(0, 0x0001);
        setGPR(2, 0x0001);
        int trr = encRR(072, 0, 2);
        execAtPC(trr);
        assertTrue(getCC(3), "EQUAL flag set when registers equal");

        setPC(getPC() + 1);
        setGPR(2, 0x0002);
        execAtPC(trr);
        assertFalse(getCC(3), "EQUAL flag cleared when registers differ");
    } // TRR behavior. :contentReference[oaicite:11]{index=11}

    @Test
    void AND_bitwise_and_rx_with_ry() {
        setGPR(1, 0b1100_1010_1111_0000);
        setGPR(2, 0b1010_1111_0000_1111);
        int instr = encRR(073, 1, 2); // AND
        execAtPC(instr);
        assertEquals(0b1000_1010_0000_0000, getGPR(1));
    } // AND rx <- rx AND ry. :contentReference[oaicite:12]{index=12}

    @Test
    void ORR_bitwise_or_rx_with_ry() {
        setGPR(1, 0b0101_0000_0000_0001);
        setGPR(2, 0b0001_0000_0000_0010);
        int instr = encRR(074, 1, 2); // ORR
        execAtPC(instr);
        assertEquals(0b0101_0000_0000_0011, getGPR(1));
    } // ORR rx <- rx OR ry. :contentReference[oaicite:13]{index=13}

    @Test
    void NOT_bitwise_complement_of_rx() {
        setGPR(3, 0b1000_0000_0001_1011);
        int instr = encRR(075, 3, 0); // NOT; ry field ignored
        execAtPC(instr);
        assertEquals(0b0111_1111_1110_0100, getGPR(3));
    } // NOT rx. :contentReference[oaicite:14]{index=14}
}