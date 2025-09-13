package group11.assembler;

public class Instruction {
    String[] instructionParts;
    String instructionString;
    String label;
    String op;
    String[] args;
    Integer addressCounter;
    
    Instruction(String _instructionString, Integer _addressCounter) {
        this.instructionString = _instructionString;
        this.addressCounter = _addressCounter;
        this.parseInstructionFields();
    }

    private void parseInstructionFields() {

    int argumentStartIndex = 2;
    this.op = instructionParts[1];
    if (this.label==null){
        this.op = instructionParts[0];
        argumentStartIndex = 1;
    }

   }

}
