package cool.scx.timer;

import cool.scx.functional.ScxCallable;
import cool.scx.functional.ScxRunnable;

import java.util.concurrent.TimeUnit;

/// ScxTimer
///
/// @author scx567888
/// @version 0.0.1
public interface ScxTimer {

    <E extends Throwable> TaskHandle<Void, E> runAfter(ScxRunnable<E> action, long delay, TimeUnit unit);

    <V, E extends Throwable> TaskHandle<V, E> runAfter(ScxCallable<V, E> action, long delay, TimeUnit unit);

}
