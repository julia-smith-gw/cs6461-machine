package group11.siminterface;

import javax.swing.*;
import java.awt.event.ActionListener;

// https://chatgpt.com/share/68ec33a9-a518-8007-ac90-d03566374f14
/**
 * A button that performs a custom action when clicked.
 */
public class ActionButton extends JButton {

    /**
     * Constructs an ActionButton with a label and an action.
     *
     * @param text   the button label
     * @param action the action to perform when the button is clicked
     */
    public ActionButton(String text, Runnable action) {
        super(text);

        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }

        // Wrap the Runnable in an ActionListener
        addActionListener(_ -> action.run());
    }

    /**
     * Alternate constructor that takes a Swing ActionListener directly.
     *
     * @param text   the button label
     * @param listener the ActionListener to attach
     */
    public ActionButton(String text, ActionListener listener) {
        super(text);

        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        addActionListener(listener);
    }
}

