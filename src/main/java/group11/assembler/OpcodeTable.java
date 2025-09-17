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

public class OpcodeTable {
    private Map<String, OpcodeInfo> table = new HashMap<>();

    // Much of op code info outputted after creating a few entries exemplifying initial entry format by https://chatgpt.com/c/68c0489b-4168-8331-b700-c10c184b7218
    public OpcodeTable() {
        table.put("DATA", new OpcodeInfo(-02, new int[]{1, 1}, new ArgTypes[]{ArgTypes.DATA}, args -> args[0]));
        table.put("LOC", new OpcodeInfo(-01, new int[]{1, 1}, new ArgTypes[]{ArgTypes.LOCATION}, _ -> null));
        table.put("LDR", new OpcodeInfo(
                0x01,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x01 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("STR", new OpcodeInfo(
                0x02,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x02 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("LDA", new OpcodeInfo(
                0x03,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x03 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("LDX", new OpcodeInfo(
                0x41,
                new int[]{2, 3},
                new ArgTypes[]{ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x41 << 10) | (args[0] << 6)
                        | ((args.length == 3 ? args[2] : 0) << 5) | (args[1] & 0x1F)
        ));

        table.put("STX", new OpcodeInfo(
                0x42,
                new int[]{2, 3},
                new ArgTypes[]{ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x42 << 10) | (args[0] << 6)
                        | ((args.length == 3 ? args[2] : 0) << 5) | (args[1] & 0x1F)
        ));

        // Branch/Jump
        table.put("JZ", new OpcodeInfo(
                0x10,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x10 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("JNE", new OpcodeInfo(
                0x11,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x11 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("JCC", new OpcodeInfo(
                0x12,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.CONDITION_CODE, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x12 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("JMA", new OpcodeInfo(
                0x13,
                new int[]{2, 3},
                new ArgTypes[]{ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x13 << 10) | (args[0] << 6)
                        | ((args.length == 3 ? args[2] : 0) << 5) | (args[1] & 0x1F)
        ));

        table.put("JSR", new OpcodeInfo(
                0x14,
                new int[]{2, 3},
                new ArgTypes[]{ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x14 << 10) | (args[0] << 6)
                        | ((args.length == 3 ? args[2] : 0) << 5) | (args[1] & 0x1F)
        ));

        table.put("RFS", new OpcodeInfo(
                0x15,
                new int[]{1,1},
                new ArgTypes[]{ArgTypes.IMMEDIATE},
                args -> (0x15 << 10) | (args[0] & 0x1F)
        ));

        table.put("SOB", new OpcodeInfo(
                0x16,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x16 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("JGE", new OpcodeInfo(
                0x17,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x17 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("AMR", new OpcodeInfo(
                0x04,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x04 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("SMR", new OpcodeInfo(
                0x05,
                new int[]{3, 4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.INDEX_REGISTER, ArgTypes.ADDRESS, ArgTypes.IA_FLAG},
                args -> (0x05 << 10) | (args[0] << 8) | (args[1] << 6)
                        | ((args.length == 4 ? args[3] : 0) << 5) | (args[2] & 0x1F)
        ));

        table.put("AIR", new OpcodeInfo(0x06, new int[]{2, 2} , 
        new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.IMMEDIATE}, 
        args -> (0x06 << 10) | (args[0] << 8) | (args[1] & 0x1F)));

        table.put("SIR", new OpcodeInfo(0x07, new int[]{2, 2} , 
        new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.IMMEDIATE}, 
        args -> (0x07 << 10) | (args[0] << 8) | (args[1] & 0x1F)));

        // -----------------------------
        // Type 2: Register to Register
        // [Opcode(6) | Rx(2) | Ry(2) | unused(6)]
        // -----------------------------
        table.put("MLT", new OpcodeInfo(
                0x70,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.REGISTER},
                args -> (0x70 << 10) | (args[0] << 8) | (args[1] << 6)
        ));

        table.put("DVD", new OpcodeInfo(
                0x71,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.REGISTER},
                args -> (0x71 << 10) | (args[0] << 8) | (args[1] << 6)
        ));

        table.put("TRR", new OpcodeInfo(
                0x72,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.REGISTER},
                args -> (0x72 << 10) | (args[0] << 8) | (args[1] << 6)
        ));

        table.put("AND", new OpcodeInfo(
                0x73,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.REGISTER},
                args -> (0x73 << 10) | (args[0] << 8) | (args[1] << 6)
        ));

        table.put("ORR", new OpcodeInfo(
                0x74,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.REGISTER},
                args -> (0x74 << 10) | (args[0] << 8) | (args[1] << 6)
        ));

        table.put("NOT", new OpcodeInfo(
                0x75,
                new int[]{1,1},
                new ArgTypes[]{ArgTypes.REGISTER},
                args -> (0x75 << 10) | (args[0] << 8)
        ));

        // -----------------------------
        // Type 3: Shift/Rotate
        // [Opcode(6) | R(2) | Count(5) | L/R(1) | A/L(1) | unused(1)]
        // -----------------------------
        table.put("SRC", new OpcodeInfo(
                0x31,
                new int[]{4,4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.IMMEDIATE, ArgTypes.LR, ArgTypes.AL},
                args -> (0x31 << 10) | (args[0] << 8) | (args[1] << 4)
                        | (args[2] << 3)
                        | (args[3] << 2)
        ));

        table.put("RRC", new OpcodeInfo(
                0x32,
                new int[]{4,4},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.IMMEDIATE, ArgTypes.LR, ArgTypes.AL},
                args -> (0x32 << 10) | (args[0] << 8) | (args[1] << 4)
                        | (args[2] << 3)
                        | (args[3] << 2)
        ));

        // -----------------------------
        // Type 4: I/O Instructions
        // [Opcode(6) | R(2) | DeviceID(5) | unused(3)]
        // -----------------------------
        table.put("IN", new OpcodeInfo(
                0x61,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.DEVICE},
                args -> (0x61 << 10) | (args[0] << 8) | (args[1] << 3)
        ));

        table.put("OUT", new OpcodeInfo(
                0x62,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.DEVICE},
                args -> (0x62 << 10) | (args[0] << 8) | (args[1] << 3)
        ));

        table.put("CHK", new OpcodeInfo(
                0x63,
                new int[]{2,2},
                new ArgTypes[]{ArgTypes.REGISTER, ArgTypes.DEVICE},
                args -> (0x63 << 10) | (args[0] << 8) | (args[1] << 3)
        ));
    }

    public OpcodeInfo lookup(String mnemonic) {
        return table.get(mnemonic.toUpperCase());
    }
}