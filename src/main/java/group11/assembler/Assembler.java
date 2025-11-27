package group11.assembler;

import java.awt.desktop.SystemSleepEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Assembler {
  String assemblerFilePath;

  /**
   * Main function that parses labels, converts instructions, and delivers files
   * (if applicable)
   */
  public void assemble() {
    getFileInputPath();
    String content = readInputFile();

    String[] instructionLines = content.lines().toArray(String[]::new);

    HashMap<Integer, AssemblerConverterResult> conversionResult = new HashMap<>();
    AssemblerLabelResolver resolver = new AssemblerLabelResolver();
    AssemblerConverter converter = new AssemblerConverter();

    try {

      // label resolution
      SymbolTable labels = resolver.process(instructionLines, conversionResult);

      // instruction conversion to octal
      converter.convertInstructions(instructionLines,
          labels, conversionResult);

      // output load file if no errors. always output listing file
      System.out.println("convert has errors: " + converter.hasErrors);
      System.out.println("resolver has errors: " + resolver.hasErrors);
      if (!converter.hasErrors && !resolver.hasErrors) {
        outputLoadFile(conversionResult);
      }
      outputListingFile(conversionResult, instructionLines, converter.hasErrors || resolver.hasErrors);
    } catch (Exception error) {
      System.out.println(error.getMessage());
      error.printStackTrace(System.out); 
      System.out.println("Critical error occurred while parsing symbols. See stack trace.");
    }
  }

  /**
   * Gets path of source file to read from user
   */
  private void getFileInputPath() {
    boolean filePathValid = false;
    File file = null;
    Scanner filePathScanner = new Scanner(System.in);
    do {
      System.out.println("Please enter path to the assembler file to read (.txt)");
      this.assemblerFilePath = filePathScanner.nextLine();
      System.out.println("Trying to read path at " + this.assemblerFilePath);
      file = new File(this.assemblerFilePath);
      if (!file.exists()) {
        System.out.println("Error: File not found. Please try again.");
      } else if (!file.isFile()) {
        System.out.println("Error: Path does not point to a file. Please try again.");
      } else if (!file.getName().toLowerCase().endsWith(".txt")) {
        System.out.println("Error: Path does not point to a .txt file. Please try again.");
      } else {
        filePathValid = true;
        System.out.println("Valid file found: " + file.getAbsolutePath());
      }
    } while (!filePathValid);

    filePathScanner.close();
  }

  // https://www.w3schools.com/java/java_user_input.asp
  // https://www.java-success.com/reading-a-text-file-in-java-with-the-scanner/
  /**
   * Reads file in or returns error if file is not found or not correct
   * 
   * @return File content as string
   */
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
  /**
   * Outputs listing file
   * 
   * @param conversionResult Map of conversion results (incl. octal representation
   *                         and errors, if applicable)
   * @param rawInstructions  Raw file input
   * @param hasErrors        Whether conversion process recorded errors in source
   *                         file
   */
  private void outputListingFile(HashMap<Integer, AssemblerConverterResult> conversionResult,
      String[] rawInstructions, Boolean hasErrors) {
    int index = 0;
    int maxIndex = rawInstructions.length;
    StringBuilder content = new StringBuilder();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter("listing-file.txt", false))) {
      while (index < maxIndex) {
        AssemblerConverterResult entry = conversionResult.get(index);
        if (entry != null) {
          writer.write(entry.getResultInOctal());
          writer.write("\t");
        } else {
          writer.write(" ".repeat(13) + "\t");
        }
        writer.write(rawInstructions[index]);
        if (entry != null && entry.error != null) {
          writer.newLine();
          writer.write("** ERROR: **" + entry.error);
          System.out.println("error " +  entry.error);
        }
        writer.newLine();
        index++;
      }
      if (!hasErrors) {
        writer.newLine();
        writer.write("** NO ERRORS FOUND DURING ASSEMBLY. LOAD FILE PRODUCED **");
      }
    } catch (IOException ex) {
      System.out.println("A critical file error occurred during writing the listing file");
      ex.printStackTrace();
    }
    Path filePath = Paths.get("listing-file.txt");
    System.out.println("Listing file written at " + filePath.toAbsolutePath());
  }

  // https://www.baeldung.com/java-iterate-map
  /**
   * Outputs load file
   * 
   * @param conversionResult Map of conversion results (incl. octal representation
   *                         and errors, if applicable)
   */
  private void outputLoadFile(HashMap<Integer, AssemblerConverterResult> conversionResult) {
    int index = 0;
    StringBuilder content = new StringBuilder();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter("load-file.txt", false))) {
      for (AssemblerConverterResult value : conversionResult.values()) {
        writer.write(value.getResultInOctal());
        writer.newLine();
      }
    } catch (IOException ex) {
      System.out.println("A critical file error occurred during writing the load file");
      ex.printStackTrace();
    }
    Path filePath = Paths.get("load-file.txt");
    System.out.println("Load file written at " + filePath.toAbsolutePath());
  }
}
