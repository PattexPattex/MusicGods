package com.pattexpattex.musicgods.util;

import com.sedmelluq.lava.common.tools.DaemonThreadFactory;

import java.util.concurrent.*;

public class TimeoutTimer {
    
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new DaemonThreadFactory("timeout-timer"));
    
    private final long timeout;
    private final TimeUnit timeUnit;
    private final Runnable runnable;
    private ScheduledFuture<?> future;
    private final Object lock = new Object();
    
    public TimeoutTimer(long timeout, TimeUnit timeUnit, Runnable onTimeout) {
        this.runnable = onTimeout;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.future = executor.schedule(onTimeout, timeout, timeUnit);
    }
    
    public void reset() {
        synchronized (lock) {
            future.cancel(false);
            future = executor.schedule(runnable, timeout, timeUnit);
        }
    }
    
    public boolean isDone() {
        return future.isDone();
    }
}
