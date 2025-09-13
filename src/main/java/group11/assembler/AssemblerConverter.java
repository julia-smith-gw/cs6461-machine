package group11.assembler;

import java.util.HashMap;

public class AssemblerConverter {

    AssemblerConverter() {

    }

    // https://docs.vultr.com/java/examples/convert-octal-number-to-decimal-and-vice-versa
    private String toOctal(Integer decimalNumber) {
        String octalString = Integer.toOctalString(decimalNumber);
        String sixDigitOctal = String.format("%06d", Integer.parseInt(octalString));
        return sixDigitOctal;
    }

    public String[] convertInstructions(String[] instructions, SymbolTable labels) {
        OpcodeTable opcodeTable = new OpcodeTable();
        int locationCounter = 0;
        HashMap<String, String> conversionResult = new HashMap<>();
        HashMap<Number, String> errors = new HashMap<>();

        for (int i = 0; i < instructions.length; ++i) {
            String instruction = instructions[i];
            String[] instructionParts = instruction.trim().split("\\s+", 2);
            String label = InstructionStringUtil.extractLabel(instructionParts);
            String op = InstructionStringUtil.extractOp(instructionParts, label);
            int argStartIndex = label != null ? 2 : 1;
            String[] args = InstructionStringUtil.extractArgs(instructionParts, argStartIndex);

            // validation needed to ensure correct behavior if op code info does not exist
            OpcodeInfo opcodeInfo = opcodeTable.lookup(op);
            int opcode = opcodeInfo.getOpcode();

            int operationResult = 0;
            operationResult = operationResult << toOctal(opcode);
          // todo: add try/catch and behavior for returning output if args are bad
           // String validateArgs = somevalidationfunction(opcodeInfo);
           for (String arg: args) {
            // how do we convert to appropraite number of bits for our bitwise shift??
            int numberArg = Integer.parseInt(arg);
            operationResult = operationResult << numberArg;
           }

           
           conversionResult.put(locationCounter, operationResult);

          // only return our conversionResult if there are no errors

        }

    }

}
