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
    getFileInputPath();
    String content = readInputFile();
    System.out.println("CONTENT");
    System.out.println(content);
    String[] instructionLines = content.lines().toArray(String[]::new);

    HashMap<Integer, AssemblerConverterResult> conversionResult = new HashMap<>();
    // Pass 1: build symbol table
    AssemblerLabelResolver resolver = new AssemblerLabelResolver();
    AssemblerConverter converter = new AssemblerConverter();

    try {
      SymbolTable labels = resolver.process(instructionLines, conversionResult);

      converter.convertInstructions(instructionLines,
          labels, conversionResult);

      if (!converter.hasErrors && !resolver.hasErrors) {
        outputLoadFile(conversionResult);
      }
      outputListingFile(conversionResult);
    } catch (Exception error) {
      System.out.println(error.getMessage());
      System.out.println(error.getStackTrace());
      System.out.println("Critical error occurred while parsing symbols. See listing file.");
    }
  }

  private void getFileInputPath() {
    boolean filePathValid = false;
    File file = null;
    Scanner filePathScanner = new Scanner(System.in);
    do {
      System.out.println("Please enter path to the assembler file to read");
      this.assemblerFilePath = filePathScanner.nextLine();
      System.out.println("Trying to read path at " + this.assemblerFilePath);
      file = new File(this.assemblerFilePath);
      if (!file.exists()) {
        System.out.println("Error: File not found. Please try again.");
      } else if (!file.isFile()) {
        System.out.println("Error: Path does not point to a file. Please try again.");
      } else {
        filePathValid = true;
        System.out.println("Valid file found: " + file.getAbsolutePath());
      }
    } while (!filePathValid);

    filePathScanner.close();
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
      System.out.println("Please try a different file path.");
    }
    return content.toString();
  }

  // https://stackoverflow.com/questions/56004215/writing-to-a-text-file-line-by-line
  private void outputListingFile(HashMap<Integer, AssemblerConverterResult> conversionResult) {
    int index = 0;
    StringBuilder content = new StringBuilder();
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

  // private void outputLoadFile(HashMap<Integer, AssemblerConverterResult>
  // conversionResult) {
  // int index = 0;
  // StringBuilder content = new StringBuilder();
  // try (BufferedWriter writer = new BufferedWriter(new
  // FileWriter("load-file.txt", true))) {
  // writer.write(this.rawInstructions[index]);
  // if (errors[index] != null) {
  // writer.write(" **ERROR**: ");
  // }
  // writer.newLine();
  // } catch (IOException ex) {
  // ex.printStackTrace();
  // }
  // }
}
