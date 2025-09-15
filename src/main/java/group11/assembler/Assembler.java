package group11.assembler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Assembler {
  // String [] rawInstructions;
  String assemblerFilePath;

  public void assemble() {
    String content = readInputFile();
    String[] instructionLines = content.lines().toArray(String[]::new);
    // Pass 1: build symbol table
    AssemblerLabelResolver resolver = new AssemblerLabelResolver();
    AssemblerConverter converters = new AssemblerConverter();
    try {
      resolver.process(instructionLines);
      // SomeType instructionOutputs = converter.convertInstructions(instructionLines,
      // resolver.labels);
    } catch (Throwable throwable) {
      System.out.println("Critical error occurred while parsing symbols. See listing file.");
    }

    // Pass 2: generate machine code
    // PassTwo passTwo = new PassTwo(symbolTable, opcodeTable);
    // List<String> listing = passTwo.process(lines);

    content.lines().forEach(line -> {

    });

  }

  private void getFileInputPath() {
    System.out.println("Please enter path to the assembler file to read");
    try (Scanner filePathScanner = new Scanner(System.in)) {
      this.assemblerFilePath = filePathScanner.nextLine();
    }

  }

  // https://www.w3schools.com/java/java_user_input.asp
  // https://www.java-success.com/reading-a-text-file-in-java-with-the-scanner/
  private String readInputFile() {
      StringBuilder content = new StringBuilder();
      // try with resource auto close the file
      try (Scanner fileScanner = new Scanner(new File(this.assemblerFilePath))) {
        while (fileScanner.hasNextLine()) {
          content.append(fileScanner.nextLine()).append(System.lineSeparator());
        }
      } catch (FileNotFoundException e) {
        this.assemblerFilePath = null;
        e.printStackTrace();
      }
      return content.toString();
    }

  // https://stackoverflow.com/questions/56004215/writing-to-a-text-file-line-by-line
  private void outputListingFile(String[] errors) {
    int index = 0;
    try (BufferedWriter writer = new BufferedWriter(new FileWriter("listing-file.txt", true))) {
      writer.write(this.rawInstructions[index]);
      if (errors[index] != null) {
        writer.write(" **ERROR**: ");
      }

      writer.newLine();
    } catch (IOException ex) {
      ex.printStackTrace();
    }

  }

  private void outputLoadFile(HashMap<Integer, Integer> conversionResult) {

  }

  private String validateInstruction(Instruction instruction) {

  }

}
