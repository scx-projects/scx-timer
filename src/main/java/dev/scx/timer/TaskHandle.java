package dev.scx.timer;

import dev.scx.exception.ScxWrappedException;

/// TaskHandle
///
/// @author scx567888
/// @version 0.0.1
public interface TaskHandle<R, X extends Throwable> {

    /// 取消任务, 仅会取消还未执行的任务.
    ///
    /// @return 任务是否取消成功, 仅在 任务还未执行时返回 true.
    boolean cancel();

    /// 同步等待任务完成, 并返回结果. (此方法是幂等的)
    ///
    /// 如果任务还未完成 (如仍处于 PENDING 或 RUNNING 状态), 则会阻塞直到任务完成.
    /// 如果任务执行失败, 将抛出相应的异常.
    ///
    /// @return 任务的结果
    /// @throws X                   任务执行时抛出的异常
    /// @throws ScxWrappedException 仅在任务执行时本身也抛出 TaskStateException 异常才会触发 (极少见), 目的是防止和 await 本身的 TaskStateException 混淆.
    /// @throws TaskStateException  任务已取消, 无法获取结果
    R await() throws X, ScxWrappedException, TaskStateException;

    /// 当前任务执行状态.
    TaskStatus status();

    /// 获取 结果, 仅在任务执行成功后可用.
    ///
    /// @throws TaskStateException 任务状态异常, 如任务 未执行, 已取消 或者 已失败.
    R result() throws TaskStateException;

    /// 获取 异常, 仅在任务执行失败后可用.
    ///
    /// 因为 我们无法保证 任务中抛出的异常一定是 X, 也有可能是其他 运行时异常.
    /// 所以此处 我们使用更加通用的 Throwable 表示任务产生的所有异常
    ///
    /// @throws TaskStateException 任务状态异常, 如任务 未执行, 已取消 或者 已成功.
    Throwable exception() throws TaskStateException;

}
