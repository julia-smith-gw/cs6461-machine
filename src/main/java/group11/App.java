package group11;

import java.nio.file.Path;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import group11.core.CPU;
import group11.core.Cache;
import group11.core.Memory;
import group11.core.RomLoader;
import group11.events.EventBus;
import group11.siminterface.MainPanel;
import group11.util.ResourceUtil;


public class App 
{
    public static void main( String[] args )
    {

        Memory memory = new Memory();
        EventBus eventBus = new EventBus();
        RomLoader romLoader = new RomLoader(memory);
        Cache cache = new Cache(memory, eventBus);
        CPU cpu = new CPU(memory, eventBus, cache, romLoader);
  
            SwingUtilities.invokeLater(() -> {
                Path defaultRom = null;
            try {
                defaultRom = ResourceUtil.extractResourceToTemp("/test-files/load-file-nominal.txt", "rom-", ".txt");
            } catch (Exception e) {
                e.printStackTrace();
            }
            JFrame f = new JFrame("Group 11 Computer Simulator");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(new MainPanel(eventBus).initializeInterface(cpu, defaultRom));
            f.pack();
            f.setLocationByPlatform(true);
            f.setVisible(true);
        });

        // Assembler assembler = new Assembler();
        // assembler.assemble();
    }
}
