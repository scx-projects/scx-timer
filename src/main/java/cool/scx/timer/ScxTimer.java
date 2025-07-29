package cool.scx.timer;

import cool.scx.function.CallableX;
import cool.scx.function.RunnableX;

import java.util.concurrent.TimeUnit;

/// ScxTimer
///
/// @author scx567888
/// @version 0.0.1
public interface ScxTimer {

    <X extends Throwable> TaskHandle<Void, X> runAfter(RunnableX<X> action, long delay, TimeUnit unit);

    <V, X extends Throwable> TaskHandle<V, X> runAfter(CallableX<V, X> action, long delay, TimeUnit unit);

}
