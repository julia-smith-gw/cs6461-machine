package group11.siminterface;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import group11.core.CPU;
import group11.core.RomLoader;
import group11.core.RomLoader.LoadException;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class MainPanel {

    public JPanel initializeInterface(CPU cpu, RomLoader romLoader) {

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        JPanel firstRow = new JPanel();
        firstRow.setLayout(new BoxLayout(firstRow, BoxLayout.X_AXIS));
        JPanel secondRow = new JPanel();
        secondRow.setLayout(new BoxLayout(secondRow, BoxLayout.X_AXIS));
        JPanel thirdRow = new JPanel();
        thirdRow.setLayout(new BoxLayout(thirdRow, BoxLayout.X_AXIS));
        // GPR inputs
        JPanel GPRInputs = new JPanel();
        GPRInputs.setBorder(new EmptyBorder(12, 12, 12, 12));
        GPRInputs.setLayout(new BoxLayout(GPRInputs, BoxLayout.Y_AXIS));
        GPRInputs.add(new JLabel("GPR"));
        GPRInputs.add(new OctalInputWithButton("GPR 0", true).buildInput());
        GPRInputs.add(Box.createVerticalStrut(2)); // spacing
        GPRInputs.add(new OctalInputWithButton("GPR 1", true).buildInput());
        GPRInputs.add(Box.createVerticalStrut(2));
        GPRInputs.add(new OctalInputWithButton("GPR 2", true).buildInput());
        GPRInputs.add(Box.createVerticalStrut(2));
        GPRInputs.add(new OctalInputWithButton("GPR 3", true).buildInput());
        GPRInputs.add(Box.createHorizontalStrut(20));
        firstRow.add(GPRInputs);

        // IXR inputs
        JPanel IXRInputs = new JPanel();
        IXRInputs.setBorder(new EmptyBorder(12, 12, 12, 12));
        IXRInputs.setLayout(new BoxLayout(IXRInputs, BoxLayout.Y_AXIS));
        IXRInputs.add(new JLabel("IXR"));
        IXRInputs.add(new OctalInputWithButton("IXR 1", true).buildInput());
        IXRInputs.add(Box.createVerticalStrut(2));
        IXRInputs.add(new OctalInputWithButton("IXR 2", true).buildInput());
        IXRInputs.add(Box.createVerticalStrut(2));
        IXRInputs.add(new OctalInputWithButton("IXR 3", true).buildInput());
        IXRInputs.add(Box.createVerticalStrut(2));
        IXRInputs.add(Box.createHorizontalStrut(20));
        firstRow.add(IXRInputs);

        // PC/MAR/MBR/IR
        JPanel programInputs = new JPanel();
        programInputs.setBorder(new EmptyBorder(12, 12, 12, 12));
        programInputs.setLayout(new BoxLayout(programInputs, BoxLayout.Y_AXIS));
        programInputs.add(new OctalInputWithButton("PC", true).buildInput());
        programInputs.add(Box.createVerticalStrut(2));
        programInputs.add(new OctalInputWithButton("MAR", true).buildInput());
        programInputs.add(Box.createVerticalStrut(2));
        programInputs.add(new OctalInputWithButton("MBR", true).buildInput());
        programInputs.add(Box.createVerticalStrut(2));
        programInputs.add(new OctalInputWithButton("IR", false).buildInput());
        programInputs.add(Box.createVerticalStrut(2));
        firstRow.add(programInputs);

        firstRow.add(Box.createVerticalStrut(20));
    

        // binary display/octal
        JPanel actionInputs = new JPanel();
        actionInputs.setBorder(new EmptyBorder(12, 12, 12, 12));
        actionInputs.setLayout(new BoxLayout(actionInputs, BoxLayout.Y_AXIS));
        actionInputs.add(new OctalInputWithButton("Binary", false).buildInput());
        actionInputs.add(Box.createVerticalStrut(10));
        actionInputs.add(new OctalInputWithButton("Octal Input", true).buildInput());
        secondRow.add(actionInputs);

                // action buttons
        JPanel actionButtons = new JPanel();
        JPanel actionButtonsColumnOne = new JPanel();
        actionButtons.setBorder(new EmptyBorder(12, 12, 12, 12));
        actionButtonsColumnOne.setLayout(new BoxLayout(actionButtonsColumnOne, BoxLayout.Y_AXIS));
        actionButtonsColumnOne.add(new ActionButton("Load", () -> {
        }));
        actionButtonsColumnOne.add(Box.createVerticalStrut(2));
        actionButtonsColumnOne.add(new ActionButton("Load+", () -> {
        }));
        actionButtonsColumnOne.add(Box.createVerticalStrut(2));
        actionButtonsColumnOne.add(new ActionButton("Store", () -> {
        }));
        actionButtonsColumnOne.add(Box.createVerticalStrut(2));
        actionButtonsColumnOne.add(new ActionButton("Store+", () ->  cpu.storePlus(0, 0)));
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
        errorCodes.add(new OctalInputWithButton("CC", false).buildInput());
        errorCodes.add(Box.createVerticalStrut(2));
        errorCodes.add(new OctalInputWithButton("MFR", false).buildInput());
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
        thirdRow.add(new ActionButton("IPL", ()->{
            try {
                romLoader.load(fileInputHandler.getSelectedPath());
            } catch (IOException | LoadException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }));

        root.add(firstRow);
        root.add(secondRow);
        root.add(thirdRow);

        return root;
    }

}
