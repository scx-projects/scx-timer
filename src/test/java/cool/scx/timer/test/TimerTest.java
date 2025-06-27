package cool.scx.timer.test;

import cool.scx.timer.ScheduledExecutorTimer;
import cool.scx.timer.ScxTimer;
import cool.scx.timer.TaskHandle;
import cool.scx.timer.TaskStatus;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TimerTest {

    private static ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private static ScxTimer timer;

    public static void main(String[] args) throws Exception {
        beforeTest();
        testTaskExecution();
        testTaskCancel();
        testTaskExceptionHandling();
        testTaskStatus();
        testTaskThreadInterruption();
        afterTest();
    }

    @BeforeTest
    public static void beforeTest() {
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        timer = new ScheduledExecutorTimer(scheduledThreadPoolExecutor);  // 使用一个单线程的 ScheduledExecutorService
    }

    @AfterTest
    public static void afterTest() {
        scheduledThreadPoolExecutor.shutdown();
    }

    // 1. 测试任务是否按预期执行（成功）
    @Test
    public static void testTaskExecution() {
        AtomicReference<String> result = new AtomicReference<>();
        var taskHandle = timer.runAfter(() -> {
            result.set("Task Completed");
        }, 1, TimeUnit.SECONDS);

        taskHandle.await();  // 等待任务执行完成

        Assert.assertEquals(result.get(), "Task Completed", "Task did not complete as expected.");
    }

    // 2. 测试任务取消功能
    @Test
    public static void testTaskCancel() {
        AtomicReference<String> result = new AtomicReference<>("Not Executed");
        TaskHandle<Void, Exception> taskHandle = timer.runAfter(() -> {
            result.set("Task Executed");
        }, 1, TimeUnit.SECONDS);

        // 在任务执行前取消任务
        boolean isCancelled = taskHandle.cancel();

        // 由于任务被取消，状态应该是 CANCELLED
        Assert.assertTrue(isCancelled, "Task was not cancelled properly.");
        Assert.assertEquals(result.get(), "Not Executed", "Task executed despite being cancelled.");
    }

    // 3. 测试任务超时异常处理
    @Test
    public static void testTaskExceptionHandling() {
        TaskHandle<Void, Exception> taskHandle = timer.runAfter(() -> {
            throw new IOException("Test Exception");
        }, 1, TimeUnit.SECONDS);

        Assert.assertThrows(IOException.class, () -> {
            var v = taskHandle.await();  // 期望抛出异常
        });
    }

    // 4. 测试任务状态管理
    @Test
    public static void testTaskStatus() {
        var taskHandle = timer.runAfter(() -> {
            // 模拟任务执行
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 1, TimeUnit.SECONDS);

        // 确保任务从 PENDING -> RUNNING -> SUCCESS
        Assert.assertEquals(taskHandle.status(), TaskStatus.PENDING, "Initial status should be PENDING.");

        // 等待任务执行完毕
        taskHandle.await();

        Assert.assertEquals(taskHandle.status(), TaskStatus.SUCCESS, "Final status should be SUCCESS.");
    }

    // 5. 测试线程中断处理
    @Test
    public static void testTaskThreadInterruption() throws Exception {
        var taskHandle = timer.runAfter(() -> {
            // 模拟任务运行过程中中断
            Thread.sleep(1000);
            Thread.currentThread().interrupt();  // 模拟中断
            return 123;
        }, 1, TimeUnit.SECONDS);

        Integer await = taskHandle.await();// 期望

        Assert.assertEquals(await, 123);
    }

}
