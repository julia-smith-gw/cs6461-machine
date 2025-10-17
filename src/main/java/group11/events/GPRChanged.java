package group11.events;

public record GPRChanged(int GPRNum, int value) implements CPUEvent {}
