package group11.assembler;

public class InstructionStringUtil {
    public static String extractLabel(String[] instructionParts) {
        if (instructionParts[0].endsWith(":")) {
            return instructionParts[0].substring(0, instructionParts[0].length() - 1); // remove :
        }
        return null;
    }

    public static String extractOp(String[] instructionParts, String label) {
        String op = instructionParts[1];
        if (label == null) {
            op = instructionParts[0];
        }
        return op.toUpperCase();
    }

    // validation needed: if arg start index is out of bounds of instructionParts
    // array
    public static String[] extractArgs(String[] instructionParts, int argStartIndex) {
        return instructionParts[argStartIndex].trim().split(",");
    }
}
