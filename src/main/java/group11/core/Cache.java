package group11.core;

import java.util.LinkedList;

import java.util.Arrays;
import java.util.Deque;

import group11.events.CacheChanged;
import group11.events.EventBus;

//https://chatgpt.com/share/68f82dbc-e1d0-8007-9397-bfb5a3b367d9
public class Cache {
    public static final int WORDS_IN_MEMORY = 2048; // 2^11
    public static final int LINE_SIZE_WORDS = 8; // 2^3 words per cache line
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
        this.memory = memory;
        this.eventBus = eventBus;
        this.reset();
    }

    /** READ: fill on miss, FIFO replacement. */
    public int load(int address) {
        address &= ADDR_MASK;

        int off = blockOffset(address);
        CacheLine hit = findLine(tag(address));

        if (hit != null)
            return hit.data[off];

        CacheLine line = chooseLineForWrite();
        fillFromMemory(line, address);
        promoteToMRU(line);
        this.dumpSummary();
        return line.data[off];
    }

    /**
     * WRITE-THROUGH + WRITE-ALLOCATE:
     * - Hit: update cache word + write memory immediately.
     * - Miss: write memory directly; do NOT load the block.
     */
    public void store(int address, int value) {
        address &= ADDR_MASK;
        int off = blockOffset(address);
        int tag = tag(address);
        CacheLine hit = findLine(tag);

        if (hit == null) {
            // write-allocate: bring the block in, then write into it
            CacheLine line = chooseLineForWrite();
            fillFromMemory(line, address); // or zero-init if you prefer
            promoteToMRU(line);
            hit = line;
        }

        hit.data[off] = value & 0xFFFF; // keep cache coherent
        memory.writeMemoryDirect(address, value & 0xFFFF); // write-through
        dumpSummary();
    }

    private CacheLine chooseLineForWrite() {
        for (CacheLine ln : queue) {
            if (!ln.valid)
                return ln; // reuse invalid line in place
        }
        return queue.peekFirst(); // candidate to evict (LRU), still not removed
    }

    // 2) After a successful fill, move the line to MRU (tail).
    private void promoteToMRU(CacheLine ln) {
        // This is safe even if ln is already in the deque (middle/head).
        queue.remove(ln);
        queue.addLast(ln);
    }

    public CacheLine findLine(int tag) {
        CacheLine found = null;
        for (CacheLine line : queue) {
            if (line.valid && line.tag == tag) {
                found = line;
                break;
            }
        }
        if (found != null)
            promoteToMRU(found);
        return found;
    }

    /**
     * Dumps all lines from cache, zeroing it out
     */
    public void reset() {
        CacheLine[] cacheLines = new CacheLine[CACHE_LINES];
        for (int i = 0; i < CACHE_LINES; i++)
            cacheLines[i] = new CacheLine();
        this.queue = new LinkedList<>(Arrays.asList(cacheLines));
        this.dumpSummary();
    }

    /**
     * Fill target line with the 8-word block for 'address'; append at tail
     * (newest).
     */
    private void fillFromMemory(CacheLine ln, int address) {
        int block = blockNumber(address); // 0..255
        int baseAddr = (block << OFFSET_BITS); // start address of the 8-word block
        int tag = tag(address);

        for (int i = 0; i < LINE_SIZE_WORDS; i++) {
            ln.data[i] = memory.getMemoryValueAt((baseAddr + i) & ADDR_MASK) & 0xFFFF;
        }
        ln.block = block;
        ln.tag = tag;
        ln.valid = true;
    }

    // -------------------- Optional: debug --------------------

    public void dumpSummary() {
        StringBuilder sb = new StringBuilder();

        sb.append("Cache {LINES=").append(CACHE_LINES)
                .append(", BLOCK_SIZE=").append(LINE_SIZE_WORDS)
                .append(", policy=WRITE_THROUGH, WRITE-ALLOCATE}\n");
        int lineIndex = 0;
        for (CacheLine line : this.queue) {
            int baseAddr = (line.tag << OFFSET_BITS);
            sb.append(String.format("L%02d: V=%d TAG=%03d BLOCK=%02d BASE=%04d DATA=%s%n",
                    lineIndex, line.valid ? 1 : 0, line.tag, line.block, baseAddr, Arrays.toString(line.data)));
            lineIndex++;
        }
        this.eventBus.post(new CacheChanged(sb.toString()));
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
