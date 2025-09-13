package group11.assembler;
public class OpcodeInfo {
    private int opcode;        // the numeric opcode (e.g., 01 for LDR)
    private int operandCount;  // how many operands (e.g., 3 for LDR r,x,addr)
    private boolean indirectAllowed; // does it support indirect addressing?

    // constructor
    public OpcodeInfo(int opcode, int operandCount, boolean indirectAllowed) {
        this.opcode = opcode;
        this.operandCount = operandCount;
        this.indirectAllowed = indirectAllowed;
    }

    // getters
    public int getOpcode() {
        return opcode;
    }

    public int getOperandCount() {
        return operandCount;
    }

    public boolean isIndirectAllowed() {
        return indirectAllowed;
    }
}
