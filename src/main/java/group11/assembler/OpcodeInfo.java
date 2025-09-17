package group11.assembler;

import java.util.function.Function;

public class OpcodeInfo {
    private int opcode;        // the numeric opcode (e.g., 01 for LDR)
    private ArgTypes[] argConfiguration;
    private int[] argNumber;
    Function<int[], Integer> encoder;

    // constructor
    public OpcodeInfo(int opcode, int [] argNumber,  ArgTypes[] argConfiguration, Function<int[], Integer> encoder) {
        this.opcode = opcode;
        this.argConfiguration= argConfiguration;
        this.argNumber=argNumber;
        this.encoder=encoder;
    }

    public int getMinArgs(){
        return argNumber[0];
    }

    public int getMaxArgs(){
        return argNumber[1];
    }

    public ArgTypes[] getArgConfiguration(){
        return this.argConfiguration;
    }

    // getters
    public int getOpcode() {
        return opcode;
    }
}
