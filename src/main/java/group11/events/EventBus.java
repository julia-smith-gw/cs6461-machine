package group11.events;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import group11.siminterface.MainPanel;

//https://chatgpt.com/share/68f265e2-9bc0-8007-a9ad-966b559bf088

// UI -> Model (intents/commands)
// sealed interface UiCommand permits SetGPR, SetIXR, SetPC, SetMAR, SetMBR {}
// record SetGPR(int GPRNum, int value) implements UiCommand {};
// record SetIXR(int IXRNum, int value) implements UiCommand {};
// record SetMAR(int value) implements UiCommand {};
// record SetMBR(int value) implements UiCommand {};
// record SetPC(int value) implements UiCommand {};

// // Model -> UI (state changes)

// record GPRChanged(int GPRNum, int value) implements CPUEvent {}
// record IXRChanged(int IXRNum, int value) implements CPUEvent {}
// record PCChanged(int value) implements CPUEvent {}
// record MARChanged(int value) implements CPUEvent {}
// record MBRChanged(int value) implements CPUEvent {}
// record IRChanged(int value) implements CPUEvent {}

public final class EventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    public <T> AutoCloseable subscribe(Class<T> type, Consumer<T> handler) {
        var list = listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
        list.add(handler);
        // Return an AutoCloseable so you can easily unsubscribe (avoid leaks).
        return () -> {
            var l = listeners.get(type);
            if (l != null) l.remove(handler);
        };
    }

    public void post(Object event) {
        Objects.requireNonNull(event, "event");
        // Notify exact class listeners
        notifyType(event.getClass(), event);
        // Also notify superclass/interfaces listeners (optional but handy)
        Class<?> c = event.getClass().getSuperclass();
        while (c != null) {
            notifyType(c, event);
            c = c.getSuperclass();
        }
        for (Class<?> iface : event.getClass().getInterfaces()) {
            notifyType(iface, event);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void notifyType(Class<T> type, Object event) {
        List<Consumer<?>> list = listeners.get(type);
        if (list == null) return;
        for (Consumer<?> raw : list) {
            ((Consumer<T>) raw).accept(type.cast(event));
        }
    }
}
    