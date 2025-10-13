package group11.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

//https://chatgpt.com/share/68ec33a9-a518-8007-ac90-d03566374f14
public class RomLoader {

    Memory memory;
    public RomLoader(Memory memory) {
        this.memory=memory;
    }

    /**
     * Load ROM from a text file containing octal address/word pairs.
     *
     * @param path   path to the text file
     * @param memory target memory
     * @throws IOException   on I/O errors
     * @throws LoadException on format or address errors
     */
    public void load(Path path) throws IOException, LoadException {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            int lineNo = 0;

            while ((line = br.readLine()) != null) {
                lineNo++;

                // strip comments and trim
                String s = stripComment(line).trim();
                if (s.isEmpty()) continue;

                String[] parts = s.split("\\s+");
                if (parts.length < 2) {
                    throw new LoadException(lineNo, "Expected: <addr> <word> (octal)");
                }

                int addr = parseOctal(parts[0], lineNo);
                int word = parseOctal(parts[1], lineNo);

                if (addr < 0 || addr >= memory.MEMORY_SIZE) {
                    throw new LoadException(lineNo, "Address out of range: " + parts[0] + " (octal)");
                }

                memory.writeMemory(addr, word & 0xFFFF); // mask to 16 bits if desired
            }
        }
    }

    // ---- helpers ----

    private static String stripComment(String s) {
        int cut = s.length();
        int p1 = s.indexOf(';');
        int p2 = s.indexOf('#');
        int p3 = s.indexOf("//");
        if (p1 >= 0) cut = Math.min(cut, p1);
        if (p2 >= 0) cut = Math.min(cut, p2);
        if (p3 >= 0) cut = Math.min(cut, p3);
        return s.substring(0, cut);
    }

    private static int parseOctal(String tok, int lineNo) throws LoadException {
        try {
            // Allow underscores for readability (e.g., 000_012)
            return Integer.parseUnsignedInt(tok.replace("_", ""), 8);
        } catch (NumberFormatException e) {
            throw new LoadException(lineNo, "Invalid octal number: " + tok);
        }
    }

    /** Simple exception carrying a line number. */
    public static class LoadException extends Exception {
        public final int line;
        public LoadException(int line, String msg) {
            super("Line " + line + ": " + msg);
            this.line = line;
        }
    }
    
}
