package dev.scx.timer.test;

import dev.scx.timer.ScheduledExecutorTimer;
import dev.scx.timer.ScxTimer;
import dev.scx.timer.TaskHandle;
import dev.scx.timer.TaskStateException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static dev.scx.timer.TaskStatus.*;

public class TimerTest {

    private static ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private static ScxTimer timer;

    public static void main(String[] args) throws Exception {
        beforeTest();
        test1();
        test2();
        testTaskExecution();
        testTaskCancel();
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

    @Test
    public static void test1() throws InterruptedException {
        TaskHandle taskHandle = timer.runAfter(() -> {
            throw new NullPointerException("Test Exception");
        }, 1, TimeUnit.SECONDS);

        Assert.assertEquals(taskHandle.status(), PENDING);

        Thread.sleep(1500);

        Assert.assertEquals(taskHandle.status(), FAILED);

        var exception = taskHandle.exception();

        Assert.assertEquals(exception.getClass(), NullPointerException.class);
    }

    @Test
    public static void test2() throws InterruptedException {
        TaskHandle taskHandle = timer.runAfter(() -> {
            throw new TaskStateException("Test Exception");
        }, 1, TimeUnit.SECONDS);

        Assert.assertEquals(taskHandle.status(), PENDING);

        Thread.sleep(1500);

        Assert.assertEquals(taskHandle.status(), FAILED);

        var exception = taskHandle.exception();

        Assert.assertEquals(exception.getClass(), TaskStateException.class);
    }

    @Test
    public static void testTaskExecution() throws InterruptedException {
        AtomicReference<String> result = new AtomicReference<>();
        var taskHandle = timer.runAfter(() -> {
            result.set("Task Completed");
        }, 1, TimeUnit.SECONDS);

        Thread.sleep(1500);

        Assert.assertEquals(result.get(), "Task Completed");
    }

    @Test
    public static void testTaskCancel() {
        AtomicReference<String> result = new AtomicReference<>("Not Executed");
        var taskHandle = timer.runAfter(() -> {
            result.set("Task Executed");
        }, 1, TimeUnit.SECONDS);

        // 在任务执行前取消任务
        boolean isCancelled = taskHandle.cancel();

        // 由于任务被取消，状态应该是 CANCELLED
        Assert.assertTrue(isCancelled);
        Assert.assertEquals(result.get(), "Not Executed");
    }

    @Test
    public static void testTaskStatus() throws InterruptedException {
        var taskHandle = timer.runAfter(() -> {
            // 模拟任务执行
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 1, TimeUnit.SECONDS);

        // 确保任务从 PENDING -> RUNNING -> SUCCESS
        Assert.assertEquals(taskHandle.status(), PENDING);

        Thread.sleep(1200);

        Assert.assertEquals(taskHandle.status(), RUNNING);

        Thread.sleep(500);

        Assert.assertEquals(taskHandle.status(), SUCCESS);
    }

    @Test
    public static void testTaskThreadInterruption() throws Exception {
        var taskHandle = timer.runAfter(() -> {
            // 模拟任务运行过程中中断
            Thread.sleep(1000);
            Thread.currentThread().interrupt();  // 模拟中断
        }, 1, TimeUnit.SECONDS);

        Thread.sleep(2500);

        Assert.assertEquals(taskHandle.status(), SUCCESS);
    }

}
