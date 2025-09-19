package group11.assembler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

// A lot of this file--specifically the validation utility functions and enum idea/parseArg for arg types
// is derived at least partially from https://chatgpt.com/share/68c0a144-2a68-8007-b81c-48d71fa5f65b and 
// https://chatgpt.com/share/68ca23d1-7968-8007-97c6-a270263b78a0
public class AssemblerConverter {
    HashMap<Integer, String> labels;
    int locationCounter = 0;
    boolean hasErrors = false;

    /**
     * Parses argument based on definition enum value for correctness
     * 
     * @param arg           Raw argument from instruction
     * @param configuration ArgTypes enum value specifying arg type
     * @param labels        Dictionary mapping location to labels
     * @return Integer representation of arg
     * @throws {Exception} Exception if arg parsing error occurred
     */
    public int parseArg(String arg, ArgTypes configuration, SymbolTable labels) {
        switch (configuration) {
            case LOCATION:
                int location = parseDecimal(arg);
                if (location < 0) {
                    throw new IllegalArgumentException("Location should be integer >= 0");
                }
                return location;
            case DATA:
                int dataValue = 0;
                if (labels.contains(arg)) {
                    dataValue = labels.getAddress(arg);
                } else {
                    dataValue = parseDecimal(arg);
                }
                return dataValue;
            case REGISTER:
                int register = parseRegister(arg);
                return register;
            case INDEX_REGISTER:
                int indexRegister = parseIndexRegister(arg);
                return indexRegister;
            case ADDRESS:
                int destinationAddress = parseAddressOrLabel(arg, labels);
                return destinationAddress;
            case IMMEDIATE:
                int imm = parseAddressOrLabel(arg, labels);
                if (imm < 0 || imm > 31)
                    throw new IllegalArgumentException("Immediate out of range");
                return imm;
            case DEVICE:
                int dev = parseAddressOrLabel(arg, labels);
                if (dev < 0 || dev > 31)
                    throw new IllegalArgumentException("Device id out of range");
                return dev;
            case COUNT:
                int c = Integer.parseInt(arg);
                if (c < 0 || c > 15)
                    throw new IllegalArgumentException("Shift count out of range");
                return c;
            case LR:
                int flag = 0;
                if ("L".equalsIgnoreCase(arg)) {
                    flag = 1;
                } else if ("R".equalsIgnoreCase(arg)) {
                    flag = 0;
                } else {
                    throw new IllegalArgumentException("Flag must be L (shift left) or R (shift right)");
                }
                return flag;
            case AL:
                int alFlag = 0;
                if ("A".equalsIgnoreCase(arg)) {
                    alFlag = 0;
                } else if ("L".equalsIgnoreCase(arg)) {
                    alFlag = 1;
                } else {
                    throw new IllegalArgumentException("Flag must be A (arithmetic) or L (logical)");
                }
                return alFlag;
            case IA_FLAG:
                int iaFlag = parseIndirectAddressingFlag(arg);
                return iaFlag;
            case CONDITION_CODE:
                int cc = parseDecimal(arg);
                if (cc < 0 || cc > 3) {
                    throw new IllegalArgumentException("Condition code can be 0, 1, 2, or 3");
                }
                return cc;
            case NONE:
            default:
                return 0;
        }
    }

    /**
     * Encodes individual instruction
     * 
     * @param instruction Raw instruction line
     * @param opcodeTable Result table
     * @param labels      Table mapping our locations to labels
     * @return Octal representation of our instruction
     * @throws Exception Error if some validation problem occurred
     */
    public Integer encodeInstruction(String instruction, OpcodeTable opcodeTable, SymbolTable labels) throws Exception {
        // extract instruction parts
        String[] instructionParts = instruction.trim().split("\\s+");
        String label = InstructionStringUtil.extractLabel(instructionParts);
        String op = InstructionStringUtil.extractOp(instructionParts, label);
        int argStartIndex = label != null ? 2 : 1;
        String[] args = InstructionStringUtil.extractArgs(instructionParts, argStartIndex);

        OpcodeInfo opcodeInfo = opcodeTable.lookup(op);
        if (opcodeInfo == null) {
            throw new NoSuchElementException("Op code not recognized");
        }

        ArgTypes[] bitCodeConfig = opcodeInfo.getArgConfiguration();
        int minArgs = opcodeInfo.getMinArgs();
        int maxArgs = opcodeInfo.getMaxArgs();
        if (args.length < minArgs || args.length > maxArgs) {
            throw new IllegalArgumentException(
                    "Wrong operand count for " + op + ". Operands should be minimum " + minArgs + " and maximum "
                            + maxArgs);
        }
        ;

        int[] processedArgs = new int[maxArgs];

        for (int i = 0; i < args.length; ++i) {
            ArgTypes configuration = bitCodeConfig[i];
            int processedArg = parseArg(args[i], configuration, labels);
            processedArgs[i] = processedArg;
        }

        if (op.equalsIgnoreCase("LOC")) {
            this.locationCounter = processedArgs[0];
        }

        return opcodeInfo.encoder.apply(processedArgs);
    }

    static int parseRegister(String s) throws IllegalArgumentException {
        s = s.trim().toUpperCase();
        int register;

        try {
            register = s.charAt(0) == 'R' ? Integer.parseInt(s.substring(1)) : Integer.parseInt(s);
        } catch (Throwable error) {
            throw new IllegalArgumentException(
                    "Invalid general register format: " + s + ". Register can be: 0, 1, 2, 3 or R0, R1, R2, R3.");
        }

        if (register < 0 || register > 3) {
            throw new IllegalArgumentException("Register out of range. Register can be: 0, 1, 2, 3 or R0, R1, R2, R3.");
        }

        return register;
    }

    static int parseIndirectAddressingFlag(String s) throws IllegalArgumentException {
        int indirectAddressingFlag = 0;
        if (s != null) {
            try {
                indirectAddressingFlag = Integer.parseInt(s);
                if (indirectAddressingFlag != 1 && indirectAddressingFlag != 0) {
                    throw new IllegalArgumentException("IA flag must be 0/1 if present");
                }
            } catch (Exception error) {
                if (s != null && s.equalsIgnoreCase("I")) {
                    indirectAddressingFlag = 1;
                } else {
                    throw new IllegalArgumentException("Last operand must be I (0/1) if present");
                }
            }
        }

        return indirectAddressingFlag;
    }

    static int parseIndexRegister(String s) throws IllegalArgumentException {
        s = s.trim().toUpperCase();
        int indexRegister;

        if (s.equals("0"))
            return 0;

        try {
            indexRegister = s.charAt(0) == 'X' ? Integer.parseInt(s.substring(1)) : Integer.parseInt(s);
        } catch (Throwable error) {
            throw new IllegalArgumentException(
                    "Invalid index register format: " + s + ". Index can be: 0, 1, 2, 3 or X1, X2, X3.");
        }

        if (indexRegister < 1 || indexRegister > 3) {
            throw new IllegalArgumentException(
                    "Index register out of range. Register can be: 0, 1, 2, 3 or X1, X2, X3.");
        }

        return indexRegister;
    }

    static int parseAddressOrLabel(String s, SymbolTable labels) throws NoSuchElementException {
        s = s.trim();
        Integer val = tryParseDecimal(s);
        if (val != null) {
            return val;
        }

        int address = labels.getAddress(s);
        if (address == -1) {
            throw new IllegalArgumentException("Undefined label: " + s);
        }

        if (address < 0 || address > 31) {
            throw new IllegalAccessError("Address for label" + s + " is out of memory range from 0 to 31.");
        }
        return address;
    }

    static int parseDecimal(String s) throws IllegalArgumentException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a decimal integer: " + s);
        }
    }

    static Integer tryParseDecimal(String s) {
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Converts instructions to encoded representation line by line
     * @param instructions Array of instruction lines
     * @param labels Table mapping label to location
     * @param conversionResult Map recording final conversion result and errors
     */
    public void convertInstructions(String[] instructions, SymbolTable labels,
            HashMap<Integer, AssemblerConverterResult> conversionResult) {
        OpcodeTable opcodeTable = new OpcodeTable();
        boolean produceLoadFile = true;
        this.hasErrors = false;

        for (int i = 0; i < instructions.length; ++i) {
            String instruction = instructions[i];

            // do not process empty instruction lines
            if (instruction.trim().isEmpty()) {
                continue;
            }

            // if encoding fails, mark assembler process as having errors and place error in
            // result dictionary
            try {
                Integer encoding = encodeInstruction(instruction, opcodeTable, labels);
                if (encoding != null) {
                    conversionResult.put(i, new AssemblerConverterResult(encoding, locationCounter, null));
                    this.locationCounter += 1;
                }
            } catch (Exception error) {
                System.out.println(error.getMessage());
                this.hasErrors = true;
                conversionResult.put(i, new AssemblerConverterResult(-1, locationCounter, error.getMessage()));
                this.locationCounter += 1;
            }
        }
    }

}
