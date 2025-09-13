package group11.assembler;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class Assembler {


    public void assemble(){
       String content = readInputFile();
       String [] instructionLines = content.lines().toArray(String[]::new);
       // Pass 1: build symbol table
        AssemblerLabelResolver resolver = new AssemblerLabelResolver();
        AssemblerConverter converter = new AssemblerConverter();
        try {
          resolver.process(instructionLines);
          SomeType instructionOutputs = converter.convertInstructions(instructionLines, resolver.labels);
        } catch (Throwable throwable) {
          System.out.println("Critical error occurred while parsing symbols. See listing file.");
        }

        // Pass 2: generate machine code
        // PassTwo passTwo = new PassTwo(symbolTable, opcodeTable);
        // List<String> listing = passTwo.process(lines);



      content.lines().forEach(line -> {
        

      });

    }

    //https://www.w3schools.com/java/java_user_input.asp
    //https://www.java-success.com/reading-a-text-file-in-java-with-the-scanner/
    private String readInputFile(){
        System.out.println("Please enter path to the assembler file to read");
        try (Scanner filePathScanner = new Scanner(System.in)) {
            StringBuilder content = new StringBuilder();

             //try with resource auto close the file
            try (Scanner fileScanner = new Scanner(new File(filePathScanner.nextLine()))) {
                  while (fileScanner.hasNextLine()) {
                    content.append(fileScanner.nextLine()).append(System.lineSeparator()); 
                 }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            return content.toString();
        }
    }


    private String validateInstruction(Instruction instruction){

    }

    
    
}
