package group11.events;

public record SetGPR(int GPRNum, int value) implements UiCommand {};
