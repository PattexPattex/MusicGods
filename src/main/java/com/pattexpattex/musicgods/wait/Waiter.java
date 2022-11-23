package com.pattexpattex.musicgods.wait;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@SuppressWarnings("rawtypes")
public class Waiter extends ListenerAdapter {

    private static final AtomicLong id = new AtomicLong();

    private final Map<Class, Set<WaitingEvent>> waiters;
    private final ScheduledExecutorService executor;

    public Waiter() {
        this.waiters = new HashMap<>();
        this.executor = Executors.newSingleThreadScheduledExecutor(factory ->
                new Thread(factory, "bot-waiter-" + id.incrementAndGet()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onGenericEvent(@NotNull GenericEvent event) {
        Class c = event.getClass();

        while (c != null) {
            Set<WaitingEvent> set = waiters.get(c);

            if (set != null) {
                set.removeIf(waiter -> waiter.attempt(event));
            }

            if (event instanceof ShutdownEvent) {
                executor.shutdown();
            }

            c = c.getSuperclass();
        }
    }

    public <T extends GenericEvent> CompletableFuture<T> waitForEvent(Class<T> type, Predicate<T> condition) {
        return waitForEvent(type, condition, -1, null);
    }

    public <T extends GenericEvent> CompletableFuture<T> waitForEvent(Class<T> type, Predicate<T> condition,
                                                                      long timeout, TimeUnit timeUnit) {
        if (executor.isShutdown())
            throw new IllegalStateException("Attempted to register a waiter while this the executor was already shutdown");
        if (type == null) throw new NullPointerException("Type cannot be null");
        if (condition == null) throw new NullPointerException("Condition cannot be null");

        WaitingEvent<T> event = new WaitingEvent<>(condition);
        Set<WaitingEvent> set = waiters.computeIfAbsent(type, c -> ConcurrentHashMap.newKeySet());
        set.add(event);

        if (timeout > 0 && timeUnit != null) {
            executor.schedule(() -> {
                if (set.remove(event)) event.future.completeExceptionally(new TimeoutException());
            }, timeout, timeUnit);
        }

        return event.future;
    }

    private static class WaitingEvent<T extends GenericEvent> {
        final Predicate<T> condition;
        final CompletableFuture<T> future;

        private WaitingEvent(Predicate<T> condition) {
            this.condition = condition;
            this.future = new CompletableFuture<>();
        }

        public boolean attempt(T event) {
            if (condition.test(event)) {
                future.complete(event);
                return true;
            }

            return false;
        }
    }
}
