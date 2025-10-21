package group11.siminterface;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

@FunctionalInterface
interface Action {
    void run();
}

// https://chatgpt.com/share/68ec33a9-a518-8007-ac90-d03566374f14
public class OctalInputWithButton {
    String label;
    Boolean hasActionButton;
    Action action;
    OctalTextField field;
    Boolean editable;

    OctalInputWithButton(String label, Boolean hasActionButton, Boolean editable, Action action) {
        this.label = label;
        this.hasActionButton = hasActionButton;
        this.action = action;
        this.editable=editable;
    }

    public Integer getValue(){
       return field.getValue();
    }

    public void setFromOctal(int newValue){
        this.field.setFromOctal(newValue);
    }

    /**
     * Initialize input field on Swing interface with labels and return
     * @return JComponent representing combo text field with button
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
        this.field.setEditable(this.editable);
        JLabel label = new JLabel(this.label);

        left.add(label);

        left.add(Box.createHorizontalStrut(6));
        left.add(field);

        if (this.hasActionButton) {
            JButton apply = new JButton("Apply");

            // Button action: parse octal -> decimal, set on Data
            apply.addActionListener((ActionEvent _) -> {
                    this.action.run();
            });

            // Pressing Enter in the field triggers the button
            field.addActionListener(_ -> apply.doClick());
            row.add(apply, BorderLayout.EAST);
        }

        row.add(left, BorderLayout.CENTER);

        root.add(row, BorderLayout.NORTH);
        return root;
    }

    /**
     * Filtered JTextField that accepts only octal digits (0–7)
     */
    static class OctalTextField extends JTextField {
    private static final String OCTAL_PATTERN = "^[0-7]*$";

        OctalTextField(int columns) {
            super(columns);
            ((AbstractDocument) getDocument()).setDocumentFilter(new OctalFilter());
            setToolTipText("Enter an octal number (digits 0–7).");
        }

        /**
         * Gets text value in input as integer
         * @return integer value of input
         */
        public Integer getValue(){
            String s = getText().trim();
            if (s.isEmpty()){
                return null;
            }
        
            try {
                return Integer.parseInt(s, 8);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        /**Helper to set from octal int value */
        void setFromOctal(int octal) {
               if (octal < 0)
                throw new IllegalArgumentException("Only non-negative values supported");
            setText(String.format("%06o", octal));
        }

        /** Filter that allows only 0–7, paste-safe. */
        private static class OctalFilter extends DocumentFilter {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string != null)
                    replace(fb, offset, 0, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                Document doc = fb.getDocument();
                String old = doc.getText(0, doc.getLength());
                StringBuilder sb = new StringBuilder(old);
                sb.replace(offset, offset + length, text == null ? "" : text);
                if (sb.toString().matches(OCTAL_PATTERN)) {
                    super.replace(fb, offset, length, text, attrs);
                } // else reject
            }
        }
    }
}