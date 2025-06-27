package cool.scx.timer;

import cool.scx.functional.ScxCallable;
import cool.scx.functional.ScxRunnable;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static cool.scx.timer.TaskStatus.*;

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
    public <E extends Throwable> TaskHandle<Void, E> runAfter(ScxRunnable<E> action, long delay, TimeUnit unit) {
        var taskStatus = new AtomicReference<>(PENDING);
        var future = executor.schedule(() -> {
            taskStatus.set(RUNNING);
            try {
                action.run();
                taskStatus.set(SUCCESS);
            } catch (Throwable e) {
                taskStatus.set(FAILED);
                throw new WrapperRuntimeException(e);
            }
        }, delay, unit);
        return new TaskHandleImpl<>(future, taskStatus);
    }

    @Override
    public <V, E extends Throwable> TaskHandle<V, E> runAfter(ScxCallable<V, E> action, long delay, TimeUnit unit) {
        var taskStatus = new AtomicReference<>(PENDING);
        var future = executor.schedule(() -> {
            taskStatus.set(RUNNING);
            try {
                var result = action.call();
                taskStatus.set(SUCCESS);
                return result;
            } catch (Throwable e) {
                taskStatus.set(FAILED);
                throw new WrapperRuntimeException(e);
            }
        }, delay, unit);
        return new TaskHandleImpl<>(future, taskStatus);
    }

    private record TaskHandleImpl<V, E extends Throwable>(ScheduledFuture<?> future,
                                                          AtomicReference<TaskStatus> taskStatus) implements TaskHandle<V, E> {

        @Override
        public boolean cancel() {
            // 我们不中断任务执行 也就是说 只有在 任务还未开始的时候 cancel 才有意义
            return future.cancel(false);
        }

        @SuppressWarnings("unchecked")
        @Override
        public V await() throws E {
            try {
                // 等待任务完成并获取结果
                return (V) future.get(); // 这里会阻塞直到任务完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 保留中断状态
                throw new IllegalStateException("Task was interrupted", e);
            } catch (ExecutionException e) {
                // 执行异常，将其转换为原始异常 E
                Throwable cause = e.getCause();
                // 处理 包装异常
                if (cause instanceof WrapperRuntimeException) {
                    cause = cause.getCause();
                }
                // 抛出原始异常
                throw (E) cause;
            } catch (CancellationException e) {
                throw new IllegalStateException("Task was cancelled", e);
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
        public V result() {
            return (V) future.resultNow();
        }

        @SuppressWarnings("unchecked")
        @Override
        public E exception() {
            var e = future.exceptionNow();
            // 处理 包装异常
            if (e instanceof WrapperRuntimeException) {
                e = e.getCause();
            }
            return (E) e;
        }

    }

    /// 异常包装类
    private static class WrapperRuntimeException extends RuntimeException {

        public WrapperRuntimeException(Throwable cause) {
            super(cause);
        }

    }

}
