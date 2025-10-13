package group11;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import group11.assembler.Assembler;
import group11.core.CPU;
import group11.core.Memory;
import group11.core.RomLoader;
import group11.siminterface.MainPanel;

public class App 
{
    public static void main( String[] args )
    {

        Memory memory = new Memory();
        RomLoader romLoader = new RomLoader(memory);
        CPU cpu = new CPU(memory);

            SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Group 11 Computer Simulator");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(new MainPanel().initializeInterface(cpu, romLoader));
            f.pack();
            f.setLocationByPlatform(true);
            f.setVisible(true);
        });

        // Assembler assembler = new Assembler();
        // assembler.assemble();
    }
}
