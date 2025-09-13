package group11.assembler;

public class AssemblerLabelResolver {
    SymbolTable labels;

    AssemblerLabelResolver() {
        this.labels = new SymbolTable();
    }

    public void process(String[] inputInstructions) {

        int addressCounter = 0;
        for (int i = 0; i < inputInstructions.length; ++i) {
            String instructionLine = inputInstructions[i];
            String[] instructionParts = instructionLine.trim().split("\\s+", 2);
            String label = InstructionStringUtil.extractLabel(instructionParts);
            String op = InstructionStringUtil.extractOp(instructionParts, label);
            int argStartIndex = label != null ? 2 : 1;
            String[] args = InstructionStringUtil.extractArgs(instructionParts, argStartIndex);
            // validate integer exists later
            if (op == "LOC") {
                addressCounter = Integer.parseInt(args[0]);
            }

            if (label != null) {
                labels.addSymbol(label, addressCounter);
            }
            addressCounter += 1;
        }
    }

}
