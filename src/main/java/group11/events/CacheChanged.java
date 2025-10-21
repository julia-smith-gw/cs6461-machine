package group11.events;

public record CacheChanged(String cacheContent) implements CPUEvent {}