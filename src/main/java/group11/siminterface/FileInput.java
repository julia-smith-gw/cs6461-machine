package group11.siminterface;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

 // https://chatgpt.com/share/68ec33a9-a518-8007-ac90-d03566374f14
 // https://chatgpt.com/share/68f3eb7a-ea8c-8007-a3f8-b1635d62b988
/**
 * A reusable Swing component that lets the user select a file from the filesystem.
 * 
 * It contains a non-editable text field showing the selected path and a "Browse" button.
 */
public class FileInput extends JPanel {
    private final JTextField filePathField;
    private final JButton browseButton;

    /**
     * Constructs a FileInput with a custom button label.
     *
     * @param buttonLabel text to display on the browse button
     */
    public FileInput(String buttonLabel, File inputFile) {
      

        super(new BorderLayout(8, 0)); // 8px horizontal gap
        setBorder(new EmptyBorder(4, 4, 4, 4));

        
        JLabel label = new JLabel("Program File");

  
        filePathField = new JTextField();
        filePathField.setEditable(false);
        browseButton = new JButton(buttonLabel);

        // Choose file on click
        browseButton.addActionListener(_ -> chooseFile());
        add(label, BorderLayout.WEST);
        add(filePathField, BorderLayout.CENTER);
        add(browseButton, BorderLayout.EAST);
           if (inputFile!= null) {
            filePathField.setText(inputFile.getAbsolutePath());
        }
    }

    /**
     * Opens a JFileChooser to pick a file, and updates the text field.
     */
    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

         if (!filePathField.getText().isEmpty()) {
            chooser.setSelectedFile(new File(filePathField.getText()));
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            filePathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * Returns the Path of the selected file, or empty string if none chosen.
     */
    public Path getSelectedPath() {
        return Paths.get(filePathField.getText().toString());
    }

}
