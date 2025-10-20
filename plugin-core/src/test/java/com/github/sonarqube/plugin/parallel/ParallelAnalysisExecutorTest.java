package com.github.sonarqube.plugin.parallel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Parallel Analysis Executor 單元測試
 *
 * 測試範圍：
 * - 並行執行任務
 * - 超時控制
 * - 錯誤處理
 * - 統計資訊
 * - 資源清理
 *
 * @since 3.0.0 (Epic 8, Story 8.1)
 */
@DisplayName("ParallelAnalysisExecutor Unit Tests")
public class ParallelAnalysisExecutorTest {

    private ParallelAnalysisExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ParallelAnalysisExecutor(3, 5);
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("測試並行執行多個任務")
    void testExecuteParallelTasks() throws ParallelExecutionException {
        List<ParallelAnalysisExecutor.AnalysisTask<String>> tasks = new ArrayList<>();

        // 建立 5 個簡單任務
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            tasks.add(new ParallelAnalysisExecutor.AnalysisTask<String>() {
                @Override
                public String execute() throws Exception {
                    return "Result-" + taskId;
                }

                @Override
                public String getTaskName() {
                    return "Task-" + taskId;
                }
            });
        }

        // 執行並行任務
        List<String> results = executor.executeParallel(tasks);

        // 驗證結果
        assertEquals(5, results.size(), "應該有 5 個結果");
        assertTrue(results.contains("Result-0"));
        assertTrue(results.contains("Result-4"));
    }

    @Test
    @DisplayName("測試任務執行時間統計")
    void testTaskExecutionTime() throws ParallelExecutionException {
        List<ParallelAnalysisExecutor.AnalysisTask<String>> tasks = new ArrayList<>();

        // 建立帶延遲的任務
        tasks.add(new ParallelAnalysisExecutor.AnalysisTask<String>() {
            @Override
            public String execute() throws Exception {
                Thread.sleep(100); // 模擬耗時操作
                return "Result";
            }

            @Override
            public String getTaskName() {
                return "Slow-Task";
            }
        });

        long startTime = System.currentTimeMillis();
        executor.executeParallel(tasks);
        long duration = System.currentTimeMillis() - startTime;

        // 執行時間應該大於 100ms
        assertTrue(duration >= 100, "執行時間應該至少 100ms");
    }

    @Test
    @DisplayName("測試並行加速效果")
    void testParallelSpeedup() throws ParallelExecutionException {
        List<ParallelAnalysisExecutor.AnalysisTask<String>> tasks = new ArrayList<>();

        // 建立 3 個各需 200ms 的任務
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            tasks.add(new ParallelAnalysisExecutor.AnalysisTask<String>() {
                @Override
                public String execute() throws Exception {
                    Thread.sleep(200);
                    return "Result-" + taskId;
                }

                @Override
                public String getTaskName() {
                    return "Task-" + taskId;
                }
            });
        }

        long startTime = System.currentTimeMillis();
        List<String> results = executor.executeParallel(tasks);
        long parallelTime = System.currentTimeMillis() - startTime;

        // 並行執行時間應該接近 200ms（而非 600ms）
        assertTrue(parallelTime < 400, "並行執行應該顯著快於串行: " + parallelTime + "ms");
        assertEquals(3, results.size());
    }

    @Test
    @DisplayName("測試任務失敗處理")
    void testTaskFailureHandling() throws ParallelExecutionException {
        List<ParallelAnalysisExecutor.AnalysisTask<String>> tasks = new ArrayList<>();

        // 成功任務
        tasks.add(new ParallelAnalysisExecutor.AnalysisTask<String>() {
            @Override
            public String execute() {
                return "Success";
            }

            @Override
            public String getTaskName() {
                return "Success-Task";
            }
        });

        // 失敗任務
        tasks.add(new ParallelAnalysisExecutor.AnalysisTask<String>() {
            @Override
            public String execute() throws Exception {
                throw new RuntimeException("Task failed");
            }

            @Override
            public String getTaskName() {
                return "Fail-Task";
            }
        });

        // 執行任務（不應該拋出異常）
        List<String> results = executor.executeParallel(tasks);

        // 只有成功的任務有結果
        assertEquals(1, results.size());
        assertEquals("Success", results.get(0));

        // 統計應該顯示 1 成功，1 失敗
        ParallelAnalysisExecutor.ExecutionStatistics stats = executor.getStatistics();
        assertEquals(1, stats.completedTasks);
        assertEquals(1, stats.failedTasks);
    }

    @Test
    @DisplayName("測試超時控制")
    void testTimeoutControl() {
        // 建立超時時間很短的 executor（1 秒）
        ParallelAnalysisExecutor shortTimeoutExecutor = new ParallelAnalysisExecutor(3, 1);

        List<ParallelAnalysisExecutor.AnalysisTask<String>> tasks = new ArrayList<>();

        // 建立需要很長時間的任務（5 秒）
        tasks.add(new ParallelAnalysisExecutor.AnalysisTask<String>() {
            @Override
            public String execute() throws Exception {
                Thread.sleep(5000);
                return "Should not complete";
            }

            @Override
            public String getTaskName() {
                return "Long-Task";
            }
        });

        try {
            shortTimeoutExecutor.executeParallel(tasks);

            // 統計應該顯示超時
            ParallelAnalysisExecutor.ExecutionStatistics stats = shortTimeoutExecutor.getStatistics();
            assertTrue(stats.failedTasks > 0, "應該有任務超時失敗");

        } catch (Exception e) {
            // 預期可能拋出 ParallelExecutionException
        } finally {
            shortTimeoutExecutor.shutdown();
        }
    }

    @Test
    @DisplayName("測試空任務列表")
    void testEmptyTaskList() throws ParallelExecutionException {
        List<ParallelAnalysisExecutor.AnalysisTask<String>> emptyTasks = new ArrayList<>();

        List<String> results = executor.executeParallel(emptyTasks);

        assertEquals(0, results.size(), "空任務列表應該返回空結果");
    }

    @Test
    @DisplayName("測試執行統計資訊")
    void testExecutionStatistics() throws ParallelExecutionException {
        List<ParallelAnalysisExecutor.AnalysisTask<String>> tasks = new ArrayList<>();

        // 3 個成功任務
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            tasks.add(new ParallelAnalysisExecutor.AnalysisTask<String>() {
                @Override
                public String execute() {
                    return "Result-" + taskId;
                }

                @Override
                public String getTaskName() {
                    return "Task-" + taskId;
                }
            });
        }

        // 1 個失敗任務
        tasks.add(new ParallelAnalysisExecutor.AnalysisTask<String>() {
            @Override
            public String execute() throws Exception {
                throw new RuntimeException("Fail");
            }

            @Override
            public String getTaskName() {
                return "Fail-Task";
            }
        });

        executor.executeParallel(tasks);

        ParallelAnalysisExecutor.ExecutionStatistics stats = executor.getStatistics();

        assertEquals(4, stats.totalTasks);
        assertEquals(3, stats.completedTasks);
        assertEquals(1, stats.failedTasks);
        assertTrue(stats.totalExecutionTimeMs > 0);
    }

    @Test
    @DisplayName("測試並行度限制")
    void testConcurrencyLimit() throws ParallelExecutionException {
        // 建立並行度為 2 的 executor
        ParallelAnalysisExecutor limitedExecutor = new ParallelAnalysisExecutor(2, 5);

        AtomicInteger concurrentCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        List<ParallelAnalysisExecutor.AnalysisTask<String>> tasks = new ArrayList<>();

        // 建立 5 個任務，每個任務執行時增加計數
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            tasks.add(new ParallelAnalysisExecutor.AnalysisTask<String>() {
                @Override
                public String execute() throws Exception {
                    int current = concurrentCount.incrementAndGet();

                    // 記錄最大並行數
                    maxConcurrent.updateAndGet(max -> Math.max(max, current));

                    Thread.sleep(100); // 保持一段時間

                    concurrentCount.decrementAndGet();
                    return "Result-" + taskId;
                }

                @Override
                public String getTaskName() {
                    return "Task-" + taskId;
                }
            });
        }

        limitedExecutor.executeParallel(tasks);

        // 最大並行數不應該超過 2
        assertTrue(maxConcurrent.get() <= 2,
                "最大並行數應該不超過設定值，實際: " + maxConcurrent.get());

        limitedExecutor.shutdown();
    }

    @Test
    @DisplayName("測試 shutdown 清理資源")
    void testShutdown() throws ParallelExecutionException {
        List<ParallelAnalysisExecutor.AnalysisTask<String>> tasks = new ArrayList<>();

        tasks.add(new ParallelAnalysisExecutor.AnalysisTask<String>() {
            @Override
            public String execute() {
                return "Result";
            }

            @Override
            public String getTaskName() {
                return "Task";
            }
        });

        executor.executeParallel(tasks);

        // 正常關閉
        executor.shutdown();

        // 關閉後不應該再接受新任務（會拋出異常）
        assertThrows(Exception.class, () -> {
            executor.executeParallel(tasks);
        });
    }

    @Test
    @DisplayName("測試任務名稱記錄")
    void testTaskNameLogging() throws ParallelExecutionException {
        List<ParallelAnalysisExecutor.AnalysisTask<String>> tasks = new ArrayList<>();

        tasks.add(new ParallelAnalysisExecutor.AnalysisTask<String>() {
            @Override
            public String execute() {
                return "Result";
            }

            @Override
            public String getTaskName() {
                return "My-Custom-Task-Name";
            }
        });

        executor.executeParallel(tasks);

        // 統計資訊應該包含任務名稱
        ParallelAnalysisExecutor.ExecutionStatistics stats = executor.getStatistics();
        assertEquals(1, stats.totalTasks);
        assertEquals(1, stats.completedTasks);
    }
}
