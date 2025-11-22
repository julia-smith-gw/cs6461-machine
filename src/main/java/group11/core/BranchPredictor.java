package group11.core;

import group11.events.BranchPredictionStatsChanged;
import group11.events.EventBus;

public final class BranchPredictor {

    // Each entry holds a 2-bit saturating counter [0..3]
    private final byte[] bht;

    // Optional stats
    private int branchCount = 0;
    private int correctPredictions = 0;
    public EventBus eventBus;

    private final int BHT_SIZE = 128;

    // ------------- Constructors ------------//

    public BranchPredictor(EventBus eventBus) {
        this.eventBus = eventBus;
        this.bht = new byte[BHT_SIZE];
        reset();
    }

    private void postBranchPredictionStats() {
        eventBus.post(new BranchPredictionStatsChanged(
                getBranchCount(),
                getCorrectPredictions(),
                getAccuracy()));
    }

    // ------------- Public API -------------

    /** Reset all counters to "weakly not taken" (01b) and clear stats. */
    public void reset() {
        for (int i = 0; i < bht.length; i++) {
            bht[i] = 1; // 01b = weakly not taken
        }
        branchCount = 0;
        correctPredictions = 0;
        this.postBranchPredictionStats();
    }

    /**
     * Predict whether the branch at the given PC will be taken.
     * Caller is responsible for passing the *branch instruction address*.
     */
    public boolean predictTaken(int branchPc) {
        int idx = index(branchPc);
        int counter = bht[idx] & 0x03; // ensure [0..3]
        // MSB of 2-bit counter: 1 => predict taken
        return (counter & 0b10) != 0;
    }

    /**
     * Update the predictor with the actual outcome of the branch.
     */
    public void update(int branchPc, boolean actuallyTaken) {
        int idx = index(branchPc);
        int counter = bht[idx] & 0x03;

        if (actuallyTaken) {
            if (counter < 3)
                counter++; // saturate at 3
        } else {
            if (counter > 0)
                counter--; // saturate at 0
        }

        bht[idx] = (byte) counter;
    }

    /**
     * Record whether a particular prediction was correct.
     * (You can call this from CPU after comparing predicted vs actual next PC.)
     */
    public void recordPredictionResult(boolean correct) {
        branchCount++;
        if (correct) {
            correctPredictions++;
        }
        this.postBranchPredictionStats();
    }

    public int getBranchCount() {
        return branchCount;
    }

    public int getCorrectPredictions() {
        return correctPredictions;
    }

    public double getAccuracy() {
        if (branchCount == 0)
            return 1.0;
        return (double) correctPredictions / branchCount;
    }

    // ------------- Internal helpers -------------

    private int index(int pc) {
        // Map PC to a BHT index. If your memory is 0..2047, you can mask low bits.
        int masked = pc & 0x7FF; // keep 11 bits if you like
        return masked & (BHT_SIZE - 1); // bhtSize is power of 2
    }
}