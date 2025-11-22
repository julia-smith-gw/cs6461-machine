package group11.events;

public record BranchPredictionStatsChanged(int totalBranches, int correctPredictions, double accuracy) implements CPUEvent {}
