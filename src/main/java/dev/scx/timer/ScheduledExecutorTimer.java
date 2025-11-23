package dev.scx.timer;

import dev.scx.exception.ScxWrappedException;
import dev.scx.function.Function0;
import dev.scx.function.Function0Void;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static dev.scx.timer.TaskStatus.*;

/// ScheduledExecutorTimer
///
/// @author scx567888
/// @version 0.0.1
public final class ScheduledExecutorTimer implements ScxTimer {

    private final ScheduledExecutorService executor;

    public ScheduledExecutorTimer(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public <X extends Throwable> TaskHandle<Void, X> runAfter(Function0Void<X> action, long delay, TimeUnit unit) {
        var taskStatus = new AtomicReference<>(PENDING);
        var future = executor.schedule(() -> {
            taskStatus.set(RUNNING);
            try {
                action.apply();
                taskStatus.set(SUCCESS);
            } catch (Throwable e) {
                taskStatus.set(FAILED);
                throw new ScxWrappedException(e);
            }
        }, delay, unit);
        return new TaskHandleImpl<>(future, taskStatus);
    }

    @Override
    public <R, X extends Throwable> TaskHandle<R, X> runAfter(Function0<R, X> action, long delay, TimeUnit unit) {
        var taskStatus = new AtomicReference<>(PENDING);
        var future = executor.schedule(() -> {
            taskStatus.set(RUNNING);
            try {
                var result = action.apply();
                taskStatus.set(SUCCESS);
                return result;
            } catch (Throwable e) {
                taskStatus.set(FAILED);
                throw new ScxWrappedException(e);
            }
        }, delay, unit);
        return new TaskHandleImpl<>(future, taskStatus);
    }

    private record TaskHandleImpl<R, X extends Throwable>(
        ScheduledFuture<?> future,
        AtomicReference<TaskStatus> taskStatus
    ) implements TaskHandle<R, X> {

        @Override
        public boolean cancel() {
            // 我们不中断任务执行 也就是说 只有在 任务还未开始的时候 cancel 才有意义
            return future.cancel(false);
        }

        @SuppressWarnings("unchecked")
        @Override
        public R await() throws X, ScxWrappedException, TaskStateException {
            try {
                return (R) future.get(); // 等待任务完成并获取结果, 这里会阻塞直到任务完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 保留中断状态
                throw new TaskStateException("Task was interrupted");
            } catch (CancellationException e) {
                throw new TaskStateException("Task was cancelled");
            } catch (ExecutionException e) {
                // 1, 先获取 包装异常.
                var wrappedX = (ScxWrappedException) e.getCause();
                // 2, 获取真实异常.
                var realX = wrappedX.getCause();
                // 3, 处理易混淆异常
                if (realX instanceof TaskStateException) {
                    throw wrappedX;
                }
                // 4, 我们不确定 异常一定能转换成 X, 这里尝试转换一下.
                try {
                    throw (X) realX;
                } catch (ClassCastException ex) {
                    // 失败则表示 可能是 运行时异常 或者 Error (只可能是这两种).
                    if (realX instanceof RuntimeException) {
                        throw (RuntimeException) realX;
                    } else { // 这里只可能是 Error
                        throw (Error) realX;
                    }
                }
            }
        }

        @Override
        public TaskStatus status() {
            // 1, 任务完成 需要细化判断具体原因
            if (future.isDone()) {
                var status = taskStatus.get();
                // 只有在任务没开始的时候, 我们才认为 isCancelled 的值有意义
                if (status == PENDING && future.isCancelled()) {
                    return CANCELLED;
                }
                // 这里只剩下 三种情况 RUNNING, SUCCESS, FAILED
                // 但是因为 isDone 表示的是整个代码块执行完毕, 所以 RUNNING 是不可能的
                // 只剩下 SUCCESS, FAILED 我们可以安全返回
                return status;
            } else {// 2, 任务还在执行中 这时我们可以使用 taskStatus 来判断 现在的真正状态
                var status = taskStatus.get();
                // 这时还没有真正执行
                if (status == PENDING) {
                    return PENDING;
                } else {
                    // 这时虽然 可能 status 已经被设置为 SUCCESS 或 FAILED
                    // 但鉴于整个 整个代码块 实际上可能还没有执行完毕 (尽管可能性极低), 所以 只返回 RUNNING
                    return RUNNING;
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public R result() throws TaskStateException {
            try {
                return (R) future.resultNow();
            } catch (IllegalStateException e) {
                throw new TaskStateException(e.getMessage());
            }
        }

        @Override
        public Throwable exception() throws TaskStateException {
            try {
                return future.exceptionNow().getCause();
            } catch (IllegalStateException e) {
                throw new TaskStateException(e.getMessage());
            }
        }

    }

}
