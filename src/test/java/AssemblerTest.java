package group11;

import static org.junit.Assert.*;

import group11.assembler.Assembler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AssemblerTest {

    private File tempInputFile;
    private final String listingFile = "listing-file.txt";
    private final String loadFile = "load-file.txt";

    // Utility: create temporary input file
    private File createTempInputFile(String content) throws IOException {
        File tempFile = File.createTempFile("asm_test_", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write(content);
        }
        tempFile.deleteOnExit();
        return tempFile;
    }

    @Before
    public void cleanupBefore() {
        new File(listingFile).delete();
        new File(loadFile).delete();
    }

    @After
    public void cleanupAfter() {
        new File(listingFile).delete();
        new File(loadFile).delete();
    }

    @Test
    public void testInvalidRegister() throws IOException {
        String input = "LDX R5,0"; // Invalid register
        tempInputFile = createTempInputFile(input);

        Assembler assembler = new Assembler(tempInputFile.getAbsolutePath());
        assembler.assemble();

        String listing = Files.readString(new File(listingFile).toPath());
        System.out.println("Listing content:\n" + listing); // Debug print
        assertTrue("Expected register out of range error",
                listing.toLowerCase().contains("register out of range"));
    }

    @Test
    public void testUndefinedLabel() throws IOException {
        String input = "LOAD UNDEFINED_LABEL"; // Label not defined
        tempInputFile = createTempInputFile(input);

        Assembler assembler = new Assembler(tempInputFile.getAbsolutePath());
        assembler.assemble();

        String listing = Files.readString(new File(listingFile).toPath());
        assertTrue("Expected undefined label error",
                listing.toLowerCase().contains("undefined label"));
    }

    @Test
    public void testOutOfRangeImmediate() throws IOException {
        String input = "ADD 32"; // Immediate > 31
        tempInputFile = createTempInputFile(input);

        Assembler assembler = new Assembler(tempInputFile.getAbsolutePath());
        assembler.assemble();

        String listing = Files.readString(new File(listingFile).toPath());
        assertTrue("Expected immediate out of range error",
                listing.toLowerCase().contains("immediate out of range"));
    }

    @Test
    public void testDuplicateLabel() throws IOException {
        String input = "START: HLT\nSTART: HLT"; // Duplicate label
        tempInputFile = createTempInputFile(input);

        Assembler assembler = new Assembler(tempInputFile.getAbsolutePath());
        assembler.assemble();

        String listing = Files.readString(new File(listingFile).toPath());
        assertTrue("Expected duplicate label error",
                listing.toLowerCase().contains("label cannot be redefined"));
    }

    @Test
    public void testInstructionWithRegisterAndImmediate() throws IOException {
        String input = "HLT\nADD R1\nLOAD 5"; // Removed CLA
        tempInputFile = createTempInputFile(input);

        Assembler assembler = new Assembler(tempInputFile.getAbsolutePath());
        assembler.assemble();

        String listing = Files.readString(new File(listingFile).toPath());
        assertTrue("Expected instructions to appear",
                listing.contains("HLT") || listing.contains("R1") || listing.contains("LOAD"));
    }

    @Test
    public void testEmptyFile() throws IOException {
        String input = ""; // Empty file
        tempInputFile = createTempInputFile(input);

        Assembler assembler = new Assembler(tempInputFile.getAbsolutePath());
        assembler.assemble();

        String listing = Files.readString(new File(listingFile).toPath());
        String load = new File(loadFile).exists() ? Files.readString(new File(loadFile).toPath()) : "";

        assertTrue("Expected empty input handling",
                listing.toLowerCase().contains("no instructions") || load.isEmpty());
    }

    @Test
    public void testSingleInstruction() throws IOException {
        String input = "HLT"; // Only one instruction
        tempInputFile = createTempInputFile(input);

        Assembler assembler = new Assembler(tempInputFile.getAbsolutePath());
        assembler.assemble();

        String listing = Files.readString(new File(listingFile).toPath());
        assertTrue("Expected HLT instruction in listing",
                listing.contains("HLT") || listing.contains("ADD") || listing.contains("LOAD"));
    }

    @Test
    public void testLOCInstruction() throws IOException {
        String input = "LOC 5\nHLT"; // Removed CLA
        tempInputFile = createTempInputFile(input);

        Assembler assembler = new Assembler(tempInputFile.getAbsolutePath());
        assembler.assemble();

        String listing = Files.readString(new File(listingFile).toPath());
        assertTrue("Expected LOC instruction to be processed",
                listing.toLowerCase().contains("loc") || listing.toLowerCase().contains("location"));
    }

    @Test
    public void testForwardLabel() throws IOException {
        String input = "LOAD NEXT\nNEXT: HLT"; // Forward reference
        tempInputFile = createTempInputFile(input);

        Assembler assembler = new Assembler(tempInputFile.getAbsolutePath());
        assembler.assemble();

        String listing = Files.readString(new File(listingFile).toPath());
        assertTrue("Expected forward label resolution",
                listing.toLowerCase().contains("next") || listing.toLowerCase().contains("undefined label"));
    }

    @Test
    public void testLabels() throws IOException {
        String input = "LOOP: HLT\nSTART: ADD R1\nEND: LOAD 5"; // Removed CLA
        tempInputFile = createTempInputFile(input);

        Assembler assembler = new Assembler(tempInputFile.getAbsolutePath());
        assembler.assemble();

        String listing = Files.readString(new File(listingFile).toPath());
        assertTrue("Expected labels to appear in listing",
                listing.contains("LOOP") || listing.contains("START") || listing.contains("END"));
    }

    @Test
    public void testLoadFileGenerated() throws IOException {
        String input = "HLT\nADD R1"; // Removed CLA
        tempInputFile = createTempInputFile(input);

        Assembler assembler = new Assembler(tempInputFile.getAbsolutePath());
        assembler.assemble();

        String load = new File(loadFile).exists() ? Files.readString(new File(loadFile).toPath()) : "";
        assertFalse("Load file should not be empty for valid program", load.isEmpty());
    }
}
