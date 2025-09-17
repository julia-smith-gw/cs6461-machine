package group11.assembler;

public class AssemblerLabelResolver {
    public SymbolTable process(String[] inputInstructions) {
        SymbolTable labels = new SymbolTable();
        int addressCounter = 0;
        for (int i = 0; i < inputInstructions.length; ++i) {
            String instructionLine = inputInstructions[i];
            if (instructionLine.trim().isEmpty()) {
                continue;
            }
            String[] instructionParts = instructionLine.trim().split("\\s+");
            String label = InstructionStringUtil.extractLabel(instructionParts);
            String op = InstructionStringUtil.extractOp(instructionParts, label);
            int argStartIndex = label != null ? 2 : 1;
            String[] args = InstructionStringUtil.extractArgs(instructionParts, argStartIndex);

            if (label != null) {
                labels.addSymbol(label, addressCounter);
            }

            // validate integer exists later
            if (op.equalsIgnoreCase("LOC")) {
                System.out.println("PARSE LOC " + args[0]);
                addressCounter = Integer.parseInt(args[0]);
            } else {
                addressCounter += 1;
            }
          
        }
        return labels;
    }


}
