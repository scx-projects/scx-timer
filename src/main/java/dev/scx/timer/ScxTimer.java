package dev.scx.timer;

import dev.scx.function.Function0;
import dev.scx.function.Function0Void;

import java.util.concurrent.TimeUnit;

/// ScxTimer
///
/// @author scx567888
/// @version 0.0.1
public interface ScxTimer {

    <X extends Throwable> TaskHandle<Void, X> runAfter(Function0Void<X> action, long delay, TimeUnit unit);

    <R, X extends Throwable> TaskHandle<R, X> runAfter(Function0<R, X> action, long delay, TimeUnit unit);

}
