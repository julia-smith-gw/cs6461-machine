package group11.assembler;

import java.util.HashMap;

public class AssemblerConverter {

    AssemblerConverter() {

    }

    public String[] convertInstructions(String[] instructions, SymbolTable labels) {
        OpcodeTable opcodeTable = new OpcodeTable();
        int locationCounter = 0;
        HashMap<Integer, Integer> conversionResult = new HashMap<>();
        HashMap<Number, String> errors = new HashMap<>();

        for (int i = 0; i < instructions.length; ++i) {
            String instruction = instructions[i];
            String[] instructionParts = instruction.trim().split("\\s+", 2);
            String label = InstructionStringUtil.extractLabel(instructionParts);
            String op = InstructionStringUtil.extractOp(instructionParts, label);
            int argStartIndex = label != null ? 2 : 1;
            String[] args = InstructionStringUtil.extractArgs(instructionParts, argStartIndex);

            // int instruction = (opcode << 10) | (r << 8) | (ix << 6) | (i << 5) | address;
            // validation needed to ensure correct behavior if op code info does not exist
            OpcodeInfo opcodeInfo = opcodeTable.lookup(op);
            int opcode = opcodeInfo.getOpcode();

            int operationResult = 0;
            operationResult = (opcode << 10);

            // 16 - 6 bits to account for op

            int bitsShift = 10;
           // todo: add try/catch and behavior for returning output if args are bad
           // String validateArgs = somevalidationfunction(opcodeInfo);
           for (int argI = 0; argI < args.length; ++argI ) {
            // how do we convert to appropraite number of bits for our bitwise shift??
            int numberArg = Integer.parseInt(args[argI]);
            bitsShift = bitsShift-opcodeInfo.args[argI].bits;
            operationResult = operationResult | (numberArg << (bitsShift));
           }
           
           conversionResult.put(locationCounter, operationResult);
          // only return our conversionResult if there are no errors
        }

    }

}
