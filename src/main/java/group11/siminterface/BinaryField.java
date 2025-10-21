package group11.siminterface;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import group11.siminterface.OctalInputWithButton.OctalTextField;

// Document listener info from https://chatgpt.com/share/68f2be28-3990-8007-816f-739b9df814e1
/**
 * Field displaying octal number in binary format
 */
public class BinaryField {
    JTextField field;

    public BinaryField(OctalInputWithButton octalInputField) {
        octalInputField.field.getDocument().addDocumentListener(new DocumentListener() {
            private void updateBinary() {
                String octalText = octalInputField.field.getText().trim();
                if (octalText.isEmpty()) {
                    setValue("");
                    return;
                }
                try {
                    // Validate octal input
                    if (!octalText.matches("[0-7]{1,6}")) {
                        setValue("Invalid octal");
                        return;
                    }
                    int decimal = Integer.parseInt(octalText, 8);
                    String binary = String.format("%16s", Integer.toBinaryString(decimal))
                            .replace(' ', '0'); // pad to 16 bits
                    setValue(binary);
                } catch (NumberFormatException ex) {
                    setValue("Error");
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateBinary();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateBinary();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateBinary();
            }
        });
    }

    public void setValue(String value) {
        this.field.setText(value);
    }

    /**
     * Function returning input field, properly formatted for screen
     * @return Jcomponent for binary field
     */
    public JComponent buildInput() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(3, 12, 3, 12));

        // Row container: [ OctalTextField ][ JLabel ] ------glue------ [ JButton ]
        JPanel row = new JPanel(new BorderLayout(8, 0)); // 8px h-gap

        // Left side (field + label)
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));

        OctalTextField field = new OctalTextField(10);
        this.field = field;
        field.setEditable(false);
        JLabel label = new JLabel("Binary Octal ");

        left.add(label);

        left.add(Box.createHorizontalStrut(6));
        left.add(field);
        row.add(left, BorderLayout.CENTER);

        root.add(row, BorderLayout.NORTH);
        return root;
    }
}
