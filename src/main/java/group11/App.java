package group11;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import group11.assembler.Assembler;
import group11.core.CPU;
import group11.core.Memory;
import group11.core.RomLoader;
import group11.events.EventBus;
import group11.siminterface.MainPanel;


public class App 
{
    public static void main( String[] args )
    {

        Memory memory = new Memory();
        EventBus eventBus = new EventBus();
        RomLoader romLoader = new RomLoader(memory);
        CPU cpu = new CPU(memory, eventBus);
            SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Group 11 Computer Simulator");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(new MainPanel(eventBus).initializeInterface(romLoader, cpu));
            f.pack();
            f.setLocationByPlatform(true);
            f.setVisible(true);
        });

        // Assembler assembler = new Assembler();
        // assembler.assemble();
    }
}
