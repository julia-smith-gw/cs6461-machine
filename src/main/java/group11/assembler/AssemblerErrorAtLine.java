package group11.assembler;

public class AssemblerErrorAtLine extends Exception {
    Integer index;
    public AssemblerErrorAtLine(String errorMessage, Throwable error, Integer index) {
        super(errorMessage, error);
        this.index = index;
    }
    
}
