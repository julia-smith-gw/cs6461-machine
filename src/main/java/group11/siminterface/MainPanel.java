package group11.siminterface;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import group11.core.CPU;
import group11.core.RomLoader;
import group11.core.RomLoader.LoadException;
import group11.events.EventBus;
import group11.events.GPRChanged;
import group11.events.IRChanged;
import group11.events.IXRChanged;
import group11.events.MARChanged;
import group11.events.MBRChanged;
import group11.events.PCChanged;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class MainPanel implements AutoCloseable {

    // record GPRChanged(int GPRNum, int value) implements ModelEvent {}
    // record IXRChanged(int IXRNum, int value) implements ModelEvent {}
    // record PCChanged(int value) implements ModelEvent {}
    // record MARChanged(int value) implements ModelEvent {}
    // record MBRChanged(int value) implements ModelEvent {}
    // record IRChanged(int value) implements ModelEvent {}

    // event bus subscribers and stuff
    private EventBus bus;
    private final AutoCloseable GPRChangedSub;
    private final AutoCloseable IXRChangedSub;
    private final AutoCloseable PCChangedSub;
    private final AutoCloseable MARChangedSub;
    private final AutoCloseable MBRChangedSub;
    private final AutoCloseable IRChangedSub;

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
    public JLabel errorField;

   public MainPanel(EventBus bus) {
        this.bus = bus;
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
                case 4:
                    this.IXR3Field.setFromOctal(cmd.value());
                    break;
                default:
                    break;
            }
        });
        this.IRChangedSub = bus.subscribe(IRChanged.class, cmd -> {
            this.IRField.setFromOctal(cmd.value());
        });
        this.MARChangedSub = bus.subscribe(MARChanged.class, cmd -> {
            this.MARField.setFromOctal(cmd.value());
        });
        this.MBRChangedSub = bus.subscribe(MBRChanged.class, cmd -> {
            this.MBRField.setFromOctal(cmd.value());
        });
        this.PCChangedSub = bus.subscribe(PCChanged.class, cmd -> {
            this.PCField.setFromOctal(cmd.value());
        });
    }

    public JPanel initializeInterface(RomLoader romLoader, CPU cpu) {

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
        this.GPR0Field = new OctalInputWithButton("GPR 0", true, () -> {
            this.setFieldFromOctal(this.GPR0Field);
         this.bus.post(new GPRChanged(0, this.GPR0Field.getValue()));
        });
        GPRInputs.add(GPR0Field.buildInput());
        GPRInputs.add(Box.createVerticalStrut(2)); // spacing
        this.GPR1Field = new OctalInputWithButton("GPR 1", true, () -> {
            this.setFieldFromOctal(this.GPR1Field);
             this.bus.post(new GPRChanged(1, this.GPR1Field.getValue()));
        });
        GPRInputs.add(GPR1Field.buildInput());
        GPRInputs.add(Box.createVerticalStrut(2));
        this.GPR2Field = new OctalInputWithButton("GPR 2", true, () -> {
            this.setFieldFromOctal(this.GPR2Field);
             this.bus.post(new GPRChanged(2, this.GPR2Field.getValue()));
        });
        GPRInputs.add(GPR2Field.buildInput());
        GPRInputs.add(Box.createVerticalStrut(2));
        this.GPR3Field = new OctalInputWithButton("GPR 3", true, () -> {
            this.setFieldFromOctal(this.GPR3Field);
            this.bus.post(new GPRChanged(3, this.GPR3Field.getValue()));
        });
        GPRInputs.add(this.GPR3Field.buildInput());
        GPRInputs.add(Box.createHorizontalStrut(20));
        firstRow.add(GPRInputs);

        // IXR inputs
        JPanel IXRInputs = new JPanel();
        IXRInputs.setBorder(new EmptyBorder(12, 12, 12, 12));
        IXRInputs.setLayout(new BoxLayout(IXRInputs, BoxLayout.Y_AXIS));
        IXRInputs.add(new JLabel("IXR"));
        this.IXR1Field = new OctalInputWithButton("IXR 1", true, () -> {
            this.setFieldFromOctal(this.IXR1Field);
            this.bus.post(new IXRChanged(1, this.IXR1Field.getValue()));
        });
        IXRInputs.add(this.IXR1Field.buildInput());
        IXRInputs.add(Box.createVerticalStrut(2));
        this.IXR2Field = new OctalInputWithButton("IXR 2", true, () -> {
            this.setFieldFromOctal(this.IXR2Field);
              this.bus.post(new IXRChanged(2, this.IXR2Field.getValue()));
        });
        IXRInputs.add(IXR2Field.buildInput());
        IXRInputs.add(Box.createVerticalStrut(2));
        this.IXR3Field = new OctalInputWithButton("IXR 3", true, () -> {
            this.setFieldFromOctal(this.IXR3Field);
            this.bus.post(new IXRChanged(3, this.IXR3Field.getValue()));
        });
        IXRInputs.add(IXR3Field.buildInput());
        IXRInputs.add(Box.createVerticalStrut(2));
        IXRInputs.add(Box.createHorizontalStrut(20));
        firstRow.add(IXRInputs);

        // PC/MAR/MBR/IR
        JPanel programInputs = new JPanel();
        programInputs.setBorder(new EmptyBorder(12, 12, 12, 12));
        programInputs.setLayout(new BoxLayout(programInputs, BoxLayout.Y_AXIS));
        this.PCField = new OctalInputWithButton("PC", true, () -> {
            this.setFieldFromOctal(this.PCField);
            this.bus.post(new PCChanged(this.PCField.getValue()));
        });
        programInputs.add(this.PCField.buildInput());
        programInputs.add(Box.createVerticalStrut(2));
        this.MARField = new OctalInputWithButton("MAR", true, () -> {
            this.setFieldFromOctal(this.MARField);
            this.bus.post(new MARChanged(this.MARField.getValue()));
        });
        programInputs.add(this.MARField.buildInput());
        programInputs.add(Box.createVerticalStrut(2));
        this.MBRField = new OctalInputWithButton("MBR", true, () -> {
            this.setFieldFromOctal(this.MBRField);
            this.bus.post(new MBRChanged(this.MBRField.getValue()));
        });
        programInputs.add(this.MBRField.buildInput());
        programInputs.add(Box.createVerticalStrut(2));
        this.IRField = new OctalInputWithButton("IR", false, () -> {
        });
        programInputs.add(this.IRField.buildInput());
        programInputs.add(Box.createVerticalStrut(2));
        firstRow.add(programInputs);

        firstRow.add(Box.createVerticalStrut(20));

        // binary display/octal
        JPanel actionInputs = new JPanel();
        actionInputs.setBorder(new EmptyBorder(12, 12, 12, 12));
        actionInputs.setLayout(new BoxLayout(actionInputs, BoxLayout.Y_AXIS));
        actionInputs.add(new OctalInputWithButton("Binary", false, () -> {
        }).buildInput());
        actionInputs.add(Box.createVerticalStrut(10));
        this.octalInputField = new OctalInputWithButton("Octal Input", false, () -> {
        });
        actionInputs.add(this.octalInputField.buildInput());
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
        actionButtons.add(actionButtonsColumnTwo);
        secondRow.add(actionButtons);

        // CC/MFR
        JPanel errorCodes = new JPanel();
        errorCodes.setBorder(new EmptyBorder(12, 12, 12, 12));
        errorCodes.setLayout(new BoxLayout(errorCodes, BoxLayout.Y_AXIS));
        errorCodes.add(new OctalInputWithButton("CC", false, () -> {
        }).buildInput());
        errorCodes.add(Box.createVerticalStrut(2));
        errorCodes.add(new OctalInputWithButton("MFR", false, () -> {
        }).buildInput());
        secondRow.add(errorCodes);

        secondRow.add(Box.createVerticalStrut(20));

        // file input
        JPanel fileInput = new JPanel();
        fileInput.setBorder(new EmptyBorder(12, 12, 12, 12));
        fileInput.setLayout(new BoxLayout(fileInput, BoxLayout.Y_AXIS));
        FileInput fileInputHandler = new FileInput();
        fileInput.add(fileInputHandler);
        fileInput.add(Box.createHorizontalStrut(10));
        thirdRow.add(fileInput);
        thirdRow.add(new ActionButton("IPL", () -> {
            try {
                System.out.println("LOAD");
                romLoader.load(fileInputHandler.getSelectedPath());
            } catch (IOException | LoadException e) {
                e.printStackTrace();
            }
        }));

        // error message
        this.errorField = new JLabel("");
        this.errorField.setForeground(Color.RED);
        fourthRow.add(this.errorField);

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
    }

}
