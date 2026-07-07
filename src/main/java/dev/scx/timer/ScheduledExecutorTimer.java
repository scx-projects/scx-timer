package dev.scx.timer;

import dev.scx.exception.ScxWrappedException;
import dev.scx.function.Function0Void;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static dev.scx.timer.TaskStatus.*;

/// ScheduledExecutorTimer
///
/// @author scx567888
public final class ScheduledExecutorTimer implements ScxTimer {

    private final ScheduledExecutorService executor;

    public ScheduledExecutorTimer(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public TaskHandle runAfter(Function0Void<?> action, long delay, TimeUnit unit) {
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
        return new TaskHandleImpl(future, taskStatus);
    }

    private record TaskHandleImpl(
        ScheduledFuture<?> future,
        AtomicReference<TaskStatus> taskStatus
    ) implements TaskHandle {

        @Override
        public boolean cancel() {
            // 我们不中断任务执行 也就是说 只有在 任务还未开始的时候 cancel 才有意义
            return future.cancel(false);
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

        @Override
        public Throwable exception() throws TaskStateException {
            try {
                // 这里 exceptionNow 的返回值是 ScxWrappedException, 我们取 getCause 就是真实的 异常.
                return future.exceptionNow().getCause();
            } catch (IllegalStateException e) {
                throw new TaskStateException(e.getMessage());
            }
        }

    }

}
