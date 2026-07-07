package dev.scx.timer;

import dev.scx.function.Function0Void;

import java.util.concurrent.TimeUnit;

/// ScxTimer
///
/// @author scx567888
public interface ScxTimer {

    TaskHandle runAfter(Function0Void<?> action, long delay, TimeUnit unit);

}
