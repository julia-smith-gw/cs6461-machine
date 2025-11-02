package group11.assembler;

import java.util.Arrays;

/**
 * Utility for extracting the different parts of assembler instruction (opcode, args, label)
 * out of raw string
 */
public class InstructionStringUtil {
    public static String extractLabel(String[] instructionParts) {
        if (instructionParts[0].endsWith(":")) {
            return instructionParts[0].substring(0, instructionParts[0].length() - 1); // remove :
        }
        return null;
    }

    public static String extractOp(String[] instructionParts, String label) {
        String op;
        if (label == null) {
            op = instructionParts[0];
        } else {
            op = instructionParts[1];
        }
        return op.toUpperCase();
    }

    public static String[] extractArgs(String[] instructionParts, int argStartIndex) {
        System.out.println(Arrays.toString(instructionParts));
        System.out.println(argStartIndex);
        if (instructionParts.length==1) {
            return new String[0];
        }
           if (argStartIndex==instructionParts.length) {
            return new String[0];
        }
        if(instructionParts[argStartIndex].replaceAll("\\s+","").startsWith(";")) {
            return new String[0];
        }
        String [] res = instructionParts[argStartIndex].replaceAll("\\s+","").split(",");
        return res;
    }
}
