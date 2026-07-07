package dev.scx.timer;

/// TaskHandle
///
/// @author scx567888
public interface TaskHandle {

    /// 取消任务, 仅会取消还未执行的任务.
    ///
    /// @return 任务是否取消成功, 仅在 任务还未执行时返回 true.
    boolean cancel();

    /// 当前任务执行状态.
    TaskStatus status();

    /// 获取 异常, 仅在任务执行失败后可用.
    ///
    /// 因为 我们无法保证 任务中抛出的异常一定是 X, 也有可能是其他 运行时异常.
    /// 所以此处 我们使用更加通用的 Throwable 表示任务产生的所有异常
    ///
    /// @throws TaskStateException 任务状态异常, 如任务 未执行, 已取消 或者 已成功.
    Throwable exception() throws TaskStateException;

}
