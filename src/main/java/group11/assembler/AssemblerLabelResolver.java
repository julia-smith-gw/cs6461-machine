package group11.assembler;

import java.util.HashMap;

public class AssemblerLabelResolver {
    Boolean hasErrors = false;

    /**
     * Creates label table for instructions
     * 
     * @param inputInstructions Raw instruction lines from file
     * @param conversionResult  Map storing conversion results and errors. Used to
     *                          store any processing errors
     * @return symbol table defining labels matching locations
     */
    public SymbolTable process(String[] inputInstructions,
            HashMap<Integer, AssemblerConverterResult> conversionResult) {
        SymbolTable labels = new SymbolTable();
        int addressCounter = 0;
        for (int i = 0; i < inputInstructions.length; ++i) {

            // process instruction line for separate op, args, label, etc.
            String instructionLine = inputInstructions[i];
            if (instructionLine.trim().isEmpty()) {
                continue;
            }
            String[] instructionParts = instructionLine.trim().split("\\s+");
            String label = InstructionStringUtil.extractLabel(instructionParts);
            String op = InstructionStringUtil.extractOp(instructionParts, label);
            int argStartIndex = label != null ? 2 : 1;
            String[] args = InstructionStringUtil.extractArgs(instructionParts, argStartIndex);

            // add label to label map.
            // label should not be defined twice.
            if (label != null) {
                if (labels.contains(label)) {
                    conversionResult.put(i,
                            new AssemblerConverterResult(-1, addressCounter, "Label cannot be redefined"));
                    this.hasErrors = true;
                } else {
                    labels.addSymbol(label, addressCounter);
                }
            }

            // account for loc address change if necessary
            if (op.equalsIgnoreCase("LOC")) {
                try {
                    int newLoc = Integer.parseInt(args[0]);
                    if (newLoc < 0) {
                        conversionResult.put(i,
                                new AssemblerConverterResult(-1, addressCounter, "Location should be integer >=1"));
                        this.hasErrors = true;
                    } else {
                        addressCounter = newLoc;
                    }
                } catch (Exception error) {
                    conversionResult.put(i,
                            new AssemblerConverterResult(-1, addressCounter, "Location should be integer >=1"));
                    this.hasErrors = true;
                }
            } else {
                addressCounter += 1;
            }

        }
        return labels;
    }

}
