package group11.assembler;
import java.util.Map;
import java.util.HashMap;

public class OpcodeTable {
    private Map<String, OpcodeInfo> table = new HashMap<>();
    // arguments:
    // [
        // {min: 0 , max: 1, optional : false},
        // {min: 0, max: 32, optional: false},
        // {min: 0, max: 1, optional :true}
    // ]
    

    public OpcodeTable() {
        table.put("LDR", new OpcodeInfo(01, 3, true));  // LDR r,x,address[,I]
        table.put("STR", new OpcodeInfo(02, 3, true));
        table.put("LDA", new OpcodeInfo(03, 3, true));
        table.put("LDX", new OpcodeInfo(041, 2, true)); // LDX x,address[,I]
        table.put("STX", new OpcodeInfo(042, 2, true));
        table.put("JZ",  new OpcodeInfo(010, 3, true)); // JZ r,x,address[,I]
        table.put("JNE", new OpcodeInfo(011, 3, true));
        table.put("JCC", new OpcodeInfo(012, 3, true));
        table.put("JMA", new OpcodeInfo(013, 2, true));
        table.put("JSR", new OpcodeInfo(014, 2, true));
        table.put("RFS", new OpcodeInfo(015, 1, false)); 
        table.put("SOB", new OpcodeInfo(016, 3, true));
        table.put("JGE", new OpcodeInfo(017, 3, true));

        // Arithmetic/Logic
        table.put("AMR", new OpcodeInfo(04, 3, true));
        table.put("SMR", new OpcodeInfo(05, 3, true));
        table.put("AIR", new OpcodeInfo(06, 2, false)); // r,immed
        table.put("SIR", new OpcodeInfo(07, 2, false)); // r,immed
        table.put("MLT", new OpcodeInfo(070, 2, false)); // MLT rx,ry
        table.put("DVD", new OpcodeInfo(071, 2, false)); // DVD rx,ry
        table.put("TRR", new OpcodeInfo(072, 2, false));
        table.put("AND", new OpcodeInfo(073, 2, false));
        table.put("ORR", new OpcodeInfo(074, 2, false));
        table.put("NOT", new OpcodeInfo(075, 1, false));

        // Shift/Rotate
        table.put("SRC", new OpcodeInfo(031, 4, false)); // SRC r,count,L/R,A/L
        table.put("RRC", new OpcodeInfo(032, 4, false)); // RRC r,count,L/R,A/L

        // I/O
        table.put("IN",  new OpcodeInfo(061, 2, false));
        table.put("OUT", new OpcodeInfo(062, 2, false));
        table.put("CHK", new OpcodeInfo(063, 2, false));
    }
    

    public OpcodeInfo lookup(String mnemonic) {
        return table.get(mnemonic.toUpperCase());
    }
}