package cool.scx.timer;

/// TaskStatus
///
/// @author scx567888
/// @version 0.0.1
public enum TaskStatus {

    /// 已提交, 等待执行
    PENDING,

    /// 正在执行
    RUNNING,

    /// 执行成功
    SUCCESS,

    /// 执行失败
    FAILED,

    /// 已取消
    CANCELLED

}
