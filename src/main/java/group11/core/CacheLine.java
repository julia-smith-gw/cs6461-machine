package group11.core;

import java.util.Arrays;

/**
 * Representation of cache line holding 8 words
 */
public class CacheLine {
    int tag;
    int block;
    // each cache line holds 8 words
    int [] data = new int[8];
    boolean valid;

    // constructor for filled cache line
    public CacheLine(int tag, int block, int [] data) {
        this.tag = tag;
        this.data=data;
        this.block=block;
    }

    // constructor for empty cache line
    public CacheLine(){
        this.invalidate();
    }

    void invalidate(){
        this.tag=0;
        this.block=0;
        this.valid=false;
        Arrays.fill(data, 0);
    }
}
