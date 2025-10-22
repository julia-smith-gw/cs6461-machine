package group11.core;

import java.util.LinkedList;

import java.util.Arrays;
import java.util.Deque;
import group11.events.EventBus;

//https://chatgpt.com/share/68f82dbc-e1d0-8007-9397-bfb5a3b367d9
public class Cache {
    public static final int WORDS_IN_MEMORY = 2048; // 2^11
    public static final int LINE_SIZE_WORDS = 8;    // 2^3 words per cache line
    public static final int CACHE_LINES = 16;
    public static final int OFFSET_BITS = Integer.numberOfTrailingZeros(LINE_SIZE_WORDS); // 3
    public static final int ADDRESS_BITS = Integer.numberOfTrailingZeros(WORDS_IN_MEMORY); // 11
    public static final int TAG_BITS = ADDRESS_BITS - OFFSET_BITS; // 8

     private static final int ADDR_MASK = 0x7FF;
    public static final int OFFSET_MASK = (1 << OFFSET_BITS) - 1; // 0b111
  private Deque<CacheLine> queue;
  Memory memory;
  EventBus eventBus;

    public Cache(Memory memory, EventBus eventBus) {
        this.memory= memory;
        this.eventBus = eventBus;
        CacheLine [] cacheLines = new CacheLine[CACHE_LINES];
        Arrays.fill(cacheLines, new CacheLine());
        this.queue = new LinkedList<>(Arrays.asList(cacheLines));
    }

      /** READ: fill on miss, FIFO replacement. */
    public int load(int address) {
        address &= ADDR_MASK;
        int tag = tag(address);
        int offset = blockOffset(address);
        CacheLine hit = findLine(tag);
        if (hit != null) {
            return hit.data[offset];
        } else {
        // Miss: pick a line (prefer any invalid; else evict oldest valid)
        CacheLine newlyWrittenLine = this.getNextWriteLine();
        this.fillFromMemory(newlyWrittenLine, address);
        return newlyWrittenLine.data[offset];
        }
    }

    /**
     * WRITE-THROUGH + NO-WRITE-ALLOCATE:
     *  - Hit: update cache word + write memory immediately.
     *  - Miss: write memory directly; do NOT load the block.
     */
    public void store(int address, int value) {
        address &= ADDR_MASK;
        int tag = tag(address);
        int off = blockOffset(address);

        CacheLine hit = findLine(tag);
        if (hit != null) {
            hit.data[off] = value & 0xFFFF;
            memory.writeMemoryDirect(address, value);
        } else {
            // No-write-allocate on miss: bypass cache
            memory.writeMemoryDirect(address, value);
        }
    }

    public CacheLine getNextWriteLine() {
        for (CacheLine line: this.queue) {
            if (!line.valid) {
                return line;
            }
        }
        return this.queue.removeFirst();
    }

    public CacheLine findLine(int tag){
        for (CacheLine line: this.queue) {
            if (line.tag==tag && line.valid) {
               return line;
            }
        }
        return null;
    }

    /** Fill target line with the 8-word block for 'address'; append at tail (newest). */
    private void fillFromMemory(CacheLine ln, int address) {
        int block = blockNumber(address);
        int tag = tag(address);
        for (int i = 0; i < LINE_SIZE_WORDS; i++) {
            ln.data[i] = this.memory.getMemoryValueAt((block + i) & ADDR_MASK) & 0xFFFF;
        }
        ln.block = block;
        ln.tag = tag;
        ln.valid = true;
        this.queue.addLast(ln); // becomes newest in FIFO
    }

    // -------------------- Optional: debug --------------------

    public String dumpSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cache {LINES=").append(CACHE_LINES)
          .append(", BLOCK_SIZE=").append(LINE_SIZE_WORDS)
          .append(", policy=WRITE_THROUGH}\n");
        int lineIndex = 0;
        for (CacheLine line: this.queue) {
            sb.append(String.format("L%02d: V=%d TAG=%03d BASE=%04d DATA=%s%n",
                    lineIndex, line.valid ? 1 : 0, line.tag, line.block, Arrays.toString(line.data)));
                    lineIndex++;
        }
        return sb.toString();
    }

    // https://chatgpt.com/share/68f82524-1d5c-8007-8d8c-d23ce9706e81
        /** 0..7: which word within the 8-word line */
    public static int blockOffset(int address) {
        rangeCheck(address);
        return address & OFFSET_MASK;
    }

    /** 0..255: which 8-word block in memory (also the tag for fully-associative) */
    public static int blockNumber(int address) {
        rangeCheck(address);
        return address >>> OFFSET_BITS;
    }

    /** Tag bits stored in a cache line (fully-associative → tag == block number) */
    public static int tag(int address) {
        return blockNumber(address);
    }

    /** Rebuild a word address from tag + offset (useful for testing) */
    public static int makeAddress(int tag, int offset) {
        if (offset < 0 || offset >= LINE_SIZE_WORDS) {
            throw new IllegalArgumentException("offset out of range 0..7: " + offset);
        }
        // For fully-assoc, tag range is 0..255 (256 blocks)
        if (tag < 0 || tag >= (1 << TAG_BITS)) {
            throw new IllegalArgumentException("tag out of range 0.." + ((1 << TAG_BITS) - 1) + ": " + tag);
        }
        return (tag << OFFSET_BITS) | offset;
    }

    private static void rangeCheck(int address) {
        if (address < 0 || address >= WORDS_IN_MEMORY) {
            throw new IllegalArgumentException("address out of range 0..2047: " + address);
        }
    }


    
}
