package group11.assembler;
import java.util.HashMap;
import java.util.Map;

/**
 * Table holding our labels mapped to a location in the assembler file
 */
public class SymbolTable {
 public  Map<String, Integer> symbols = new HashMap<>();

    public void addSymbol(String label, int address) {
        symbols.put(label, address);
    }

    public int getAddress(String label) {
        return symbols.getOrDefault(label, -1);
    }

    public boolean contains(String label) {
        return symbols.containsKey(label);
    }

}
