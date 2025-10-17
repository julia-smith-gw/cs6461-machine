package group11.events;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import group11.siminterface.MainPanel;

//https://chatgpt.com/share/68f265e2-9bc0-8007-a9ad-966b559bf088

/**
 * This event bus utilizes Java's AutoCloseable listeners to convey value changes between the CPU values
 * and the frontend, binding them to each other.
 */
public final class EventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    /**
     * Subscribe to command (see events folder for all possible commands).
     * @param <T> Desired event grouping interface
     * @param type Desired event to subscribe to
     * @param handler Callback to invoke when change occurs
     * @return Instance of new autocloseable subscription 
     */
    public <T> AutoCloseable subscribe(Class<T> type, Consumer<T> handler) {
        var list = listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
        list.add(handler);
        // Return an AutoCloseable so you can easily unsubscribe (avoid leaks).
        return () -> {
            var l = listeners.get(type);
            if (l != null) l.remove(handler);
        };
    }

    /**
     * Posts new value change to subscribers
     * @param event A new event instance with arguments. See 'CPUEvent' or 'UiCommand' for all events.
     */
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
    