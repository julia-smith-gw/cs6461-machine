package group11.siminterface;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A reusable Swing component that lets the user select a file from the filesystem.
 * 
 * It contains a non-editable text field showing the selected path and a "Browse" button.
 */
public class FileInput extends JPanel {
    private final JTextField filePathField;
    private final JButton browseButton;
    private File selectedFile;

    /**
     * Constructs a FileInput with a default label "Browse".
     */
    public FileInput() {
        this("Browse...");
    }

    /**
     * Constructs a FileInput with a custom button label.
     *
     * @param buttonLabel text to display on the browse button
     */
    public FileInput(String buttonLabel) {
        super(new BorderLayout(8, 0)); // 8px horizontal gap
        setBorder(new EmptyBorder(4, 4, 4, 4));

        JLabel label = new JLabel("Program File");

  
        filePathField = new JTextField();
        filePathField.setEditable(false);
        browseButton = new JButton(buttonLabel);

        // Choose file on click
        browseButton.addActionListener(e -> chooseFile());
        add(label, BorderLayout.WEST);
        add(filePathField, BorderLayout.CENTER);
        add(browseButton, BorderLayout.EAST);
    }

    /**
     * Opens a JFileChooser to pick a file, and updates the text field.
     */
    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (selectedFile != null) {
            chooser.setCurrentDirectory(selectedFile.getParentFile());
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Returns the currently selected File, or null if none chosen.
     */
    public File getSelectedFile() {
        return selectedFile;
    }

    /**
     * Sets the selected file programmatically and updates the text field.
     */
    public void setSelectedFile(File file) {
        this.selectedFile = file;
        filePathField.setText(file != null ? file.getAbsolutePath() : "");
    }

    /**
     * Returns the Path of the selected file, or empty string if none chosen.
     */
    public Path getSelectedPath() {
        return Paths.get(filePathField.getText().toString());
    }


    /**
     * Returns the path string of the selected file, or empty string if none chosen.
     */
    public String getSelectedPathAsString() {
        return filePathField.getText();
    }
}
