//written with GPT assistance 
package group11.core;

public class Memory 
{

    public final int MEMORY_SIZE = 2048;    
    private int[] memory;                     
    private int MAR;                          
    private int MBR;      // should we declare MAR and MBR in CPU.java?

    public Memory() 
    {
        memory = new int[MEMORY_SIZE];
        reset();   
    }

    
    public int readMARAddress() 
    {
        return MAR;
    }

    public void writeMARAddress(int address) 
    {
        try 
        {
            checkAddress(address);
            this.MAR = address;
        } 
        catch (IllegalArgumentException e) 
        {
            System.err.println("Invalid MAR address: " + e.getMessage());
        }
    }

    
    public void writeMBR(int data) 
    {
        this.MBR = data & 0xFFFF; 
    }

    
    public int readMBR() 
    {
        return this.MBR;
    }

   
    public void writeMemory(int address, int data) 
    {
        try 
        {
            checkAddress(address);
            writeMARAddress(address);
            writeMBR(data);
            memory[MAR] = MBR;
        } 
        catch (IllegalArgumentException e) 
        {
            System.err.println("Memory write error: " + e.getMessage());
        }
    }

    
    public void readMemory(int address) 
    {
        try 
        {
            checkAddress(address);
            writeMARAddress(address);
            MBR = memory[MAR];
        } 
        catch (IllegalArgumentException e) 
        {
            System.err.println("Memory read error: " + e.getMessage());
        }
    }

    
    public void reset() 
    {
        for (int i = 0; i < MEMORY_SIZE; i++) 
        {
            memory[i] = 0;
        }
        MAR = 0;
        MBR = 0;
    }

   
    private void checkAddress(int address) 
    {
        if (address < 0 || address >= MEMORY_SIZE) 
        {
            throw new IllegalArgumentException("Address out of bounds: " + address);
        }
    }

  
    public void dump(int start, int end) 
    {
        try 
        {
            checkAddress(start);
            checkAddress(end);
            for (int i = start; i <= end; i++) 
            {
                System.out.printf("Addr %04d : %04X\n", i, memory[i]);
            }
        } 
        catch (IllegalArgumentException e) 
        {
            System.err.println("Dump error: " + e.getMessage());
        }
    }
}
