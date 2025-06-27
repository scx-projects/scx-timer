package cool.scx.timer;

/// TaskHandle
///
/// @author scx567888
/// @version 0.0.1
public interface TaskHandle<V, E extends Throwable> {

    /// 取消任务, 仅会取消还未执行的任务
    boolean cancel();

    /// 同步等待任务完成, 并返回结果
    ///
    /// 如果任务还未完成 (如仍处于 PENDING 或 RUNNING 状态), 则会阻塞直到任务完成.
    /// 如果任务执行失败, 将抛出相应的异常.
    ///
    /// @return 任务的结果
    /// @throws E                     任务执行时抛出的异常
    /// @throws IllegalStateException 任务已取消, 无法获取结果
    V await() throws E, IllegalStateException;

    /// 状态
    TaskStatus status();

    /// 获取 结果, 仅在任务执行成功后可用
    ///
    /// @throws IllegalStateException 任务状态异常, 如任务 未执行,已取消 或者 已失败
    V result() throws IllegalStateException;

    /// 获取 异常, 仅在任务执行失败后可用
    ///
    /// @throws IllegalStateException 任务状态异常, 如任务 未执行,已取消 或者 已成功
    E exception() throws IllegalStateException;

}
