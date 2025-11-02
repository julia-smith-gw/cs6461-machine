package group11.siminterface;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import group11.core.CPU;
import group11.events.CChanged;
import group11.events.CacheChanged;
import group11.events.EventBus;
import group11.events.GPRChanged;
import group11.events.IRChanged;
import group11.events.IXRChanged;
import group11.events.MARChanged;
import group11.events.MBRChanged;
import group11.events.MessageChanged;
import group11.events.PCChanged;
import group11.events.SetGPR;
import group11.events.SetIXR;
import group11.events.SetMAR;
import group11.events.SetMBR;
import group11.events.SetPC;
import java.nio.file.Path;

import java.awt.*;

// https://chatgpt.com/share/68ec33a9-a518-8007-ac90-d03566374f14

/**
 * Main interface panel declaring all inputs
 */
public class MainPanel implements AutoCloseable {

    // event bus subscribers and stuff
    private EventBus bus;
    private final AutoCloseable GPRChangedSub;
    private final AutoCloseable IXRChangedSub;
    private final AutoCloseable PCChangedSub;
    private final AutoCloseable MARChangedSub;
    private final AutoCloseable MBRChangedSub;
    private final AutoCloseable IRChangedSub;
    private final AutoCloseable messageChangedSub;
    private final AutoCloseable cacheChangedSub;
    private final AutoCloseable CChangedSub;

    public OctalInputWithButton MBRField;
    public OctalInputWithButton MARField;
    public OctalInputWithButton IXR1Field;
    public OctalInputWithButton IXR2Field;
    public OctalInputWithButton IXR3Field;
    public OctalInputWithButton GPR0Field;
    public OctalInputWithButton GPR1Field;
    public OctalInputWithButton GPR2Field;
    public OctalInputWithButton GPR3Field;
    public OctalInputWithButton PCField;
    public OctalInputWithButton IRField;
    public OctalInputWithButton octalInputField;
    public BinaryField binaryField;
    public JTextArea messageField;
    public JTextArea cacheField;
    public JTextField consoleInput;
    public JTextField ccCodeField;

    public MainPanel(EventBus bus) {
        this.bus = bus;

        // create handlers binding inputs to values changed in CPU
        this.GPRChangedSub = bus.subscribe(GPRChanged.class, cmd -> {
            switch (cmd.GPRNum()) {
                case 0:
                    this.GPR0Field.setFromOctal(cmd.value());
                    break;
                case 1:
                    this.GPR1Field.setFromOctal(cmd.value());
                    break;
                case 2:
                    this.GPR2Field.setFromOctal(cmd.value());
                    break;
                case 3:
                    this.GPR3Field.setFromOctal(cmd.value());
                    break;
                default:
                    break;
            }
        });
        this.IXRChangedSub = bus.subscribe(IXRChanged.class, cmd -> {
            switch (cmd.IXRNum()) {
                case 1:
                    this.IXR1Field.setFromOctal(cmd.value());
                    break;
                case 2:
                    this.IXR2Field.setFromOctal(cmd.value());
                    break;
                case 3:
                    this.IXR3Field.setFromOctal(cmd.value());
                    break;
                default:
                    break;
            }
        });
        this.IRChangedSub = bus.subscribe(IRChanged.class, cmd -> {
              if (cmd.value() != null) {
                this.IRField.setFromOctal(cmd.value());
            } else {
                this.IRField.field.setText("");
            }
        });
        this.MARChangedSub = bus.subscribe(MARChanged.class, cmd -> {
            if (cmd.value() != null) {
                this.MARField.setFromOctal(cmd.value());
            } else {
                this.MARField.field.setText("");
            }
        });
        this.MBRChangedSub = bus.subscribe(MBRChanged.class, cmd -> {
            if (cmd.value() != null) {
                this.MBRField.setFromOctal(cmd.value());
            } else {
                this.MBRField.field.setText("");
            }
        });
        this.PCChangedSub = bus.subscribe(PCChanged.class, cmd -> {
            if (cmd.value()!= null) {
                this.PCField.setFromOctal(cmd.value());
            } else {
                this.PCField.field.setText("");
            }
        });
        this.messageChangedSub = bus.subscribe(MessageChanged.class, cmd -> {
            this.messageField.setText(cmd.value());
        });
        this.cacheChangedSub = bus.subscribe(CacheChanged.class, cmd -> {
            this.cacheField.setText(cmd.cacheContent());
        });
        this.CChangedSub = bus.subscribe(CChanged.class, cmd -> {
            this.ccCodeField.setText(cmd.ccContent());
        });
    }

    /**
     * Initializes interface in proper arrangement with all proper listeners
     * 
     * @param cpu Instance of cpu
     * @return Jpanel root interface box containing all input sections
     */
    public JPanel initializeInterface(CPU cpu, Path defaultFilePath) {

        // panels defining rows for layout
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        JPanel firstRow = new JPanel();
        firstRow.setLayout(new BoxLayout(firstRow, BoxLayout.X_AXIS));
        JPanel secondRow = new JPanel();
        secondRow.setLayout(new BoxLayout(secondRow, BoxLayout.X_AXIS));
        JPanel thirdRow = new JPanel();
        thirdRow.setLayout(new BoxLayout(thirdRow, BoxLayout.X_AXIS));
        JPanel fourthRow = new JPanel();
        fourthRow.setLayout(new BoxLayout(fourthRow, BoxLayout.X_AXIS));

        // GPR inputs
        JPanel GPRInputs = new JPanel();
        GPRInputs.setBorder(new EmptyBorder(12, 12, 12, 12));
        GPRInputs.setLayout(new BoxLayout(GPRInputs, BoxLayout.Y_AXIS));
        GPRInputs.add(new JLabel("GPR"));
        this.GPR0Field = new OctalInputWithButton("GPR 0", true, false, () -> {
            this.setFieldFromOctal(this.GPR0Field);
            this.bus.post(new SetGPR(0, this.GPR0Field.getValue()));
        });
        GPRInputs.add(GPR0Field.buildInput());
        GPRInputs.add(Box.createVerticalStrut(2)); // spacing
        this.GPR1Field = new OctalInputWithButton("GPR 1", true, false, () -> {
            this.setFieldFromOctal(this.GPR1Field);
            this.bus.post(new SetGPR(1, this.GPR1Field.getValue()));
        });
        GPRInputs.add(GPR1Field.buildInput());
        GPRInputs.add(Box.createVerticalStrut(2));
        this.GPR2Field = new OctalInputWithButton("GPR 2", true, false, () -> {
            this.setFieldFromOctal(this.GPR2Field);
            this.bus.post(new SetGPR(2, this.GPR2Field.getValue()));
        });
        GPRInputs.add(GPR2Field.buildInput());
        GPRInputs.add(Box.createVerticalStrut(2));
        this.GPR3Field = new OctalInputWithButton("GPR 3", true, false, () -> {
            this.setFieldFromOctal(this.GPR3Field);
            this.bus.post(new SetGPR(3, this.GPR3Field.getValue()));
        });
        GPRInputs.add(this.GPR3Field.buildInput());
        GPRInputs.add(Box.createHorizontalStrut(20));
        firstRow.add(GPRInputs);

        // IXR inputs
        JPanel IXRInputs = new JPanel();
        IXRInputs.setBorder(new EmptyBorder(12, 12, 12, 12));
        IXRInputs.setLayout(new BoxLayout(IXRInputs, BoxLayout.Y_AXIS));
        IXRInputs.add(new JLabel("IXR"));
        this.IXR1Field = new OctalInputWithButton("IXR 1", true, false, () -> {
            this.setFieldFromOctal(this.IXR1Field);
            this.bus.post(new SetIXR(1, this.IXR1Field.getValue()));
        });
        IXRInputs.add(this.IXR1Field.buildInput());
        IXRInputs.add(Box.createVerticalStrut(2));
        this.IXR2Field = new OctalInputWithButton("IXR 2", true, false, () -> {
            this.setFieldFromOctal(this.IXR2Field);
            this.bus.post(new SetIXR(2, this.IXR2Field.getValue()));
        });
        IXRInputs.add(IXR2Field.buildInput());
        IXRInputs.add(Box.createVerticalStrut(2));
        this.IXR3Field = new OctalInputWithButton("IXR 3", true, false, () -> {
            this.setFieldFromOctal(this.IXR3Field);
            this.bus.post(new SetIXR(3, this.IXR3Field.getValue()));
        });
        IXRInputs.add(IXR3Field.buildInput());
        IXRInputs.add(Box.createVerticalStrut(2));
        IXRInputs.add(Box.createHorizontalStrut(20));
        firstRow.add(IXRInputs);

        // PC/MAR/MBR/IR
        JPanel programInputs = new JPanel();
        programInputs.setBorder(new EmptyBorder(12, 12, 12, 12));
        programInputs.setLayout(new BoxLayout(programInputs, BoxLayout.Y_AXIS));
        this.PCField = new OctalInputWithButton("PC", true, false, () -> {
            this.setFieldFromOctal(this.PCField);
            this.bus.post(new SetPC(this.PCField.getValue()));
        });
        programInputs.add(this.PCField.buildInput());
        programInputs.add(Box.createVerticalStrut(2));
        this.MARField = new OctalInputWithButton("MAR", true, false, () -> {
            this.setFieldFromOctal(this.MARField);
            this.bus.post(new SetMAR(this.MARField.getValue()));
        });
        programInputs.add(this.MARField.buildInput());
        programInputs.add(Box.createVerticalStrut(2));
        this.MBRField = new OctalInputWithButton("MBR", true, false, () -> {
            this.setFieldFromOctal(this.MBRField);
            this.bus.post(new SetMBR(this.MBRField.getValue()));
        });
        programInputs.add(this.MBRField.buildInput());
        programInputs.add(Box.createVerticalStrut(2));
        this.IRField = new OctalInputWithButton("IR", false, false, () -> {
        });
        programInputs.add(this.IRField.buildInput());
        programInputs.add(Box.createVerticalStrut(2));
        programInputs.add(Box.createHorizontalStrut(10));
        firstRow.add(programInputs);

        // cache field
        JPanel cachePanel = new JPanel();
        cachePanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        cachePanel.setLayout(new BoxLayout(cachePanel, BoxLayout.Y_AXIS));
        this.cacheField = new JTextArea(10, 20);
        this.cacheField.setEditable(false);
        cachePanel.add(new JLabel("Cache Content"));
        cachePanel.add(this.cacheField);
        firstRow.add(cachePanel);

        firstRow.add(Box.createVerticalStrut(20));

        // binary display/octal
        JPanel actionInputs = new JPanel();
        actionInputs.setBorder(new EmptyBorder(12, 12, 12, 12));
        actionInputs.setLayout(new BoxLayout(actionInputs, BoxLayout.Y_AXIS));
        actionInputs.add(Box.createVerticalStrut(2));
        this.octalInputField = new OctalInputWithButton("Octal Input", false, true, () -> {
        });
        actionInputs.add(this.octalInputField.buildInput());
        this.binaryField = new BinaryField(this.octalInputField);
        actionInputs.add(binaryField.buildInput());
        secondRow.add(actionInputs);

        // action buttons
        JPanel actionButtons = new JPanel();
        JPanel actionButtonsColumnOne = new JPanel();
        actionButtons.setBorder(new EmptyBorder(12, 12, 12, 12));
        actionButtonsColumnOne.setLayout(new BoxLayout(actionButtonsColumnOne, BoxLayout.Y_AXIS));
        actionButtonsColumnOne.add(new ActionButton("Load", () -> {
            cpu.loadFrontPanel();
        }));
        actionButtonsColumnOne.add(Box.createVerticalStrut(2));
        actionButtonsColumnOne.add(new ActionButton("Load+", () -> {
            cpu.loadPlus();
        }));
        actionButtonsColumnOne.add(Box.createVerticalStrut(2));
        actionButtonsColumnOne.add(new ActionButton("Store", () -> {
            cpu.storeFrontPanel();
        }));
        actionButtonsColumnOne.add(Box.createVerticalStrut(2));
        actionButtonsColumnOne.add(new ActionButton("Store+", () -> cpu.storePlus()));
        actionButtonsColumnOne.add(Box.createHorizontalStrut(10));
        actionButtons.add(actionButtonsColumnOne);
        JPanel actionButtonsColumnTwo = new JPanel();
        actionButtonsColumnTwo.setLayout(new BoxLayout(actionButtonsColumnTwo, BoxLayout.Y_AXIS));
        actionButtonsColumnTwo.add(new ActionButton("Run", () -> cpu.run()));
        actionButtonsColumnTwo.add(Box.createVerticalStrut(2));
        actionButtonsColumnTwo.add(new ActionButton("Step", () -> cpu.step()));
        actionButtonsColumnTwo.add(Box.createVerticalStrut(2));
        actionButtonsColumnTwo.add(new ActionButton("Halt", () -> cpu.halt()));
        actionButtonsColumnTwo.add(Box.createVerticalStrut(2));
        actionButtonsColumnTwo.add(new ActionButton("Reset", () -> cpu.reset()));
        actionButtons.add(actionButtonsColumnTwo);
        secondRow.add(actionButtons);

        // CC/MFR
        JPanel errorCodes = new JPanel();
        errorCodes.setBorder(new EmptyBorder(12, 12, 12, 12));
        errorCodes.setLayout(new BoxLayout(errorCodes, BoxLayout.Y_AXIS));

        JPanel ccCodeLabelAndInput = new JPanel();
        ccCodeLabelAndInput.setLayout(new BoxLayout(ccCodeLabelAndInput, BoxLayout.X_AXIS));
        this.ccCodeField = new JTextField();
        this.ccCodeField.setEditable(false);
        JLabel label = new JLabel("CC");
        ccCodeLabelAndInput.add(label);
        ccCodeLabelAndInput.add(Box.createHorizontalStrut(6));
        ccCodeLabelAndInput.add(ccCodeField);

        errorCodes.add(ccCodeLabelAndInput);
        errorCodes.add(Box.createVerticalStrut(2));
        errorCodes.add(new OctalInputWithButton("MFR", false, false, () -> {
        }).buildInput());
        secondRow.add(errorCodes);
        secondRow.add(Box.createVerticalStrut(20));

        // file input/ipl
        JPanel fileInput = new JPanel();
        fileInput.setBorder(new EmptyBorder(12, 12, 12, 12));
        fileInput.setLayout(new BoxLayout(fileInput, BoxLayout.Y_AXIS));
        FileInput fileInputHandler = new FileInput("Browse...",
                defaultFilePath != null ? defaultFilePath.toFile() : null);
        fileInput.add(fileInputHandler);
        fileInput.add(Box.createHorizontalStrut(10));
        thirdRow.add(fileInput);
        thirdRow.add(new ActionButton("IPL", () -> cpu.loadFromROM(fileInputHandler.getSelectedPath())));

        // message printing
        JPanel messagePanel = new JPanel();
        messagePanel.setBorder(new EmptyBorder(12, 12, 24, 12));
        messagePanel.setLayout(new BorderLayout());
        this.messageField = new JTextArea(5, 10);
        this.messageField.setEditable(false);
        this.messageField.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(this.messageField); // Wrap in JScrollPane for scrolling
        messagePanel.add(new JLabel("Console Output"), BorderLayout.NORTH); // Add the label to the west (left)
        messagePanel.add(scrollPane, BorderLayout.CENTER);
        fourthRow.add(messagePanel);

        // console input
        JPanel inputPanel = new JPanel();
        inputPanel.setBorder(new EmptyBorder(12, 12, 24, 12));
        inputPanel.setLayout(new BorderLayout());
        this.consoleInput = new JTextField();
        inputPanel.add(new JLabel("Console Input"), BorderLayout.NORTH);
        inputPanel.add(this.consoleInput);
        ActionButton consoleInputButton = new ActionButton("Submit", () -> {
            cpu.submitConsoleInput(this.consoleInput.getText());
        });
        inputPanel.add(consoleInputButton, BorderLayout.EAST);
        fourthRow.add(inputPanel);
        root.add(firstRow);
        root.add(secondRow);
        root.add(thirdRow);
        root.add(fourthRow);

        return root;
    }

    public void setFieldFromOctal(OctalInputWithButton target) {
        int octalValue = this.octalInputField.getValue();
        target.setFromOctal(octalValue);
    }

    @Override
    public void close() {
        try {
            GPRChangedSub.close();
        } catch (Exception ignored) {
        }
        try {
            IXRChangedSub.close();
        } catch (Exception ignored) {
        }
        try {
            MARChangedSub.close();
        } catch (Exception ignored) {
        }
        try {
            MBRChangedSub.close();
        } catch (Exception ignored) {
        }
        try {
            IRChangedSub.close();
        } catch (Exception ignored) {
        }
        try {
            PCChangedSub.close();
        } catch (Exception ignored) {
        }
        try {
            messageChangedSub.close();
        } catch (Exception ignored) {
        }
        try {
            cacheChangedSub.close();
        } catch (Exception ignored) {
        }
        try {
            this.CChangedSub.close();
        } catch (Exception ignored) {
        }

    }

}
