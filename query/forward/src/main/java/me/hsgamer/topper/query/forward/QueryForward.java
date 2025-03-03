package me.hsgamer.topper.query.forward;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class QueryForward<A, C extends QueryForwardContext<A>> {
    private final List<C> contexts = new ArrayList<>();
    private final List<Consumer<C>> forwarders = new ArrayList<>();

    public void addContext(C context) {
        contexts.add(context);
        forwarders.forEach(forwarder -> forwarder.accept(context));
    }

    public void addForwarder(Consumer<C> forwarder) {
        forwarders.add(forwarder);
        contexts.forEach(forwarder);
    }

    public void removeContext(C context) {
        contexts.remove(context);
    }

    public void removeForwarder(Consumer<C> forwarder) {
        forwarders.remove(forwarder);
    }

    public void clearContexts() {
        contexts.clear();
    }

    public void clearForwarders() {
        forwarders.clear();
    }
}
