package group11.assembler;

import java.util.Map;
import java.util.HashMap;

// Idea for arg types: https://chatgpt.com/c/68c8c667-20f0-8321-b132-6ed1401b7183
enum ArgTypes{
    REGISTER,
    DATA,
    INDEX_REGISTER,
    ADDRESS,
    IA_FLAG,
    DEVICE,
    LR,
    COUNT,
    AL,
    IMMEDIATE,
    LOCATION,
    NONE,
    CONDITION_CODE
}

/**
 * Table mapping instruction string to information required to validate and convert op code
 */
public class OpcodeTable {
    private Map<String, OpcodeInfo> table = new HashMap<>();

    // Much of op code info outputted after creating a few entries exemplifying initial entry format by https://chatgpt.com/share/68ca23d1-7968-8007-97c6-a270263b78a0
    // https://stackoverflow.com/questions/16433781/how-to-set-value-of-octal-in-java
    public OpcodeTable() {
        table.put("DATA", new OpcodeInfo(-02, new int[]{1, 1}, new ArgTypes[]{ArgTypes.DATA}, args -> args[0]));
        table.put("LOC", new OpcodeInfo(-01, new int[]{1, 1}, new ArgTypes[]{ArgTypes.LOCATION}, _ -> null));
        table.put("HLT", new OpcodeInfo(00, new int[] {0,0}, new ArgTypes[]{}, _ -> 0));
        table.put("TRAP", new OpcodeInfo(030, new int[] {1,1}, new ArgTypes[]{ArgTypes.COUNT}, args-> (030 << 10) | (args[0] & 0x1F)));
        table.put("LDR", new OpcodeInfo(
                01,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (01 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("STR", new OpcodeInfo(
                02,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (02 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("LDA", new OpcodeInfo(
                03,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (03 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("LDX", new OpcodeInfo(
                041,
                new int[]{2, 3},
                new ArgTypes[]{ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (041 << 10) | (args[0] << 6)
                        | ((args.length == 3 ? args[2] : 0) << 5) | (args[1] & 0x1F)
        ));

        table.put("STX", new OpcodeInfo(
                042,
                new int[]{2, 3},
                new ArgTypes[]{ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (042 << 10) | (args[0] << 6)
                        | ((args.length == 3 ? args[2] : 0) << 5) | (args[1] & 0x1F)
        ));

        // Branch/Jump
        table.put("JZ", new OpcodeInfo(
                010,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (010 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("JNE", new OpcodeInfo(
                011,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (011 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("JCC", new OpcodeInfo(
                012,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.CONDITION_CODE, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (012 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("JMA", new OpcodeInfo(
                013,
                new int[]{2, 3},
                new ArgTypes[]{ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (013 << 10) | (args[0] << 6)
                        | ((args.length == 3 ? args[2] : 0) << 5) | (args[1] & 0x1F)
        ));

        table.put("JSR", new OpcodeInfo(
                014,
                new int[]{2, 3},
                new ArgTypes[]{ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (014 << 10) | (args[0] << 6)
                        | ((args.length == 3 ? args[2] : 0) << 5) | (args[1] & 0x1F)
        ));

        table.put("RFS", new OpcodeInfo(
                015,
                new int[]{1,1},
                new ArgTypes[]{ArgTypes.IMMEDIATE},
                args -> (015 << 10) | (args[0] & 0x1F)
        ));

        table.put("SOB", new OpcodeInfo(
                016,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (016 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("JGE", new OpcodeInfo(
                017,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (017 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("AMR", new OpcodeInfo(
                04,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (04 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("SMR", new OpcodeInfo(
                05,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (05 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("AIR", new OpcodeInfo(06, new int[]{2, 2} , 
        new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.IMMEDIATE}, 
        args -> (06 << 10) | (args[0] << 8) | (args[1] & 0x1F)));

        table.put("SIR", new OpcodeInfo(07, new int[]{2, 2} , 
        new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.IMMEDIATE}, 
        args -> (07 << 10) | (args[0] << 8) | (args[1] & 0x1F)));

        // -----------------------------
        // Type 2: Register to Register
        // [Opcode(6) | Rx(2) | Ry(2) | unused(6)]
        // -----------------------------
        table.put("MLT", new OpcodeInfo(
                070,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.REGISTER},
                args -> (070 << 10) | (args[0] << 8) | (args[1] << 6)
        ));

        table.put("DVD", new OpcodeInfo(
                071,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.REGISTER},
                args -> (071 << 10) | (args[0] << 8) | (args[1] << 6)
        ));

        table.put("TRR", new OpcodeInfo(
                072,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.REGISTER},
                args -> (072 << 10) | (args[0] << 8) | (args[1] << 6)
        ));

        table.put("AND", new OpcodeInfo(
                073,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.REGISTER},
                args -> (073 << 10) | (args[0] << 8) | (args[1] << 6)
        ));

        table.put("ORR", new OpcodeInfo(
                074,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.REGISTER},
                args -> (074 << 10) | (args[0] << 8) | (args[1] << 6)
        ));

        table.put("NOT", new OpcodeInfo(
                075,
                new int[]{1,1},
                new ArgTypes[]{ArgTypes.REGISTER},
                args -> (075 << 10) | (args[0] << 8)
        ));

        // -----------------------------
        // Type 3: Shift/Rotate
        // [Opcode(6) | R(2) | Count(5) | L/R(1) | A/L(1) | unused(1)]
        // -----------------------------
        table.put("SRC", new OpcodeInfo(
                031,
                new int[]{4,4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.IMMEDIATE, ArgTypes.LR, ArgTypes.AL},
                args -> (031 << 10) | (args[0] << 8) | (args[1] << 4)
                        | (args[2] << 3)
                        | (args[3] << 2)
        ));

        table.put("RRC", new OpcodeInfo(
                032,
                new int[]{4,4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.IMMEDIATE, ArgTypes.LR, ArgTypes.AL},
                args -> (032 << 10) | (args[0] << 8) | (args[1] << 4)
                        | (args[2] << 3)
                        | (args[3] << 2)
        ));

        // -----------------------------
        // Type 4: I/O Instructions
        // [Opcode(6) | R(2) | DeviceID(5) | unused(3)]
        // -----------------------------
        table.put("IN", new OpcodeInfo(
                061,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.DEVICE},
                args -> (061 << 10) | (args[0] << 8) | (args[1] << 3)
        ));

        table.put("OUT", new OpcodeInfo(
                062,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.DEVICE},
                args -> (062 << 10) | (args[0] << 8) | (args[1] << 3)
        ));

        table.put("CHK", new OpcodeInfo(
                063,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.DEVICE},
                args -> (063 << 10) | (args[0] << 8) | (args[1] << 3)
        ));
    }

    public OpcodeInfo lookup(String mnemonic) {
        return table.get(mnemonic.toUpperCase());
    }
}