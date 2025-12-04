package async.Task3;

import java.util.*;
import java.util.concurrent.*;

public class Task3_1 {
    static class ColumnSumTask extends RecursiveTask<long[]> {
        private static final int THRESHOLD = 1;
        private final int[][] matrix;
        private final int startCol;
        private final int endCol;

        public ColumnSumTask(int[][] matrix, int startCol, int endCol) {
            this.matrix = matrix;
            this.startCol = startCol;
            this.endCol = endCol;
        }

        @Override
        protected long[] compute() {
            int numCols = endCol - startCol;

            if (numCols <= THRESHOLD) {
                long[] result = new long[numCols];
                for (int c = startCol; c < endCol; c++) {
                    long sum = 0;
                    for (int r = 0; r < matrix.length; r++) {
                        sum += matrix[r][c];
                    }
                    result[c - startCol] = sum;
                }
                return result;
            } else {
                int midCol = startCol + numCols / 2;
                ColumnSumTask leftTask = new ColumnSumTask(matrix, startCol, midCol);
                ColumnSumTask rightTask = new ColumnSumTask(matrix, midCol, endCol);

                leftTask.fork();
                long[] rightResult = rightTask.compute();
                long[] leftResult = leftTask.join();

                return combineResults(leftResult, rightResult);
            }
        }

        private long[] combineResults(long[] left, long[] right) {
            long[] combined = new long[left.length + right.length];
            System.arraycopy(left, 0, combined, 0, left.length);
            System.arraycopy(right, 0, combined, left.length, right.length);
            return combined;
        }
    }
    static class ColumnSumCallable implements Callable<Long> {
        private final int[][] matrix;
        private final int columnIndex;

        public ColumnSumCallable(int[][] matrix, int columnIndex) {
            this.matrix = matrix;
            this.columnIndex = columnIndex;
        }

        @Override
        public Long call() {
            long sum = 0;
            for (int r = 0; r < matrix.length; r++) {
                sum += matrix[r][columnIndex];
            }
            return sum;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.println("--- Генерація матриці ---");
            System.out.print("Введіть кількість рядків (N): ");
            int N = scanner.nextInt();
            System.out.print("Введіть кількість стовпців (M): ");
            int M = scanner.nextInt();
            System.out.print("Введіть мінімальне значення елементів: ");
            int minVal = scanner.nextInt();
            System.out.print("Введіть максимальне значення елементів: ");
            int maxVal = scanner.nextInt();

            if (N <= 0 || M <= 0 || minVal > maxVal) {
                System.err.println("Некоректні вхідні дані. Розмірності > 0, minVal <= maxVal.");
                return;
            }

            int[][] matrix = generateMatrix(N, M, minVal, maxVal);
            printMatrix(matrix);

            long startTimeStealing = System.currentTimeMillis();
            long[] resultStealing = runWorkStealing(matrix);
            long endTimeStealing = System.currentTimeMillis();

            System.out.println("\n--- Результат Work Stealing (ForkJoinPool) ---");
            System.out.println("Суми стовпців: " + Arrays.toString(resultStealing));
            System.out.println("Час виконання: " + (endTimeStealing - startTimeStealing) + " мс");

            long startTimeDealing = System.currentTimeMillis();
            long[] resultDealing = runWorkDealing(matrix);
            long endTimeDealing = System.currentTimeMillis();

            System.out.println("\n--- Результат Work Dealing (FixedThreadPool) ---");
            System.out.println("Суми стовпців: " + Arrays.toString(resultDealing));
            System.out.println("Час виконання: " + (endTimeDealing - startTimeDealing) + " мс");


        } catch (Exception e) {
            System.err.println("Помилка вводу: Будь ласка, вводьте лише цілі числа.");
        } finally {
            scanner.close();
        }
    }

    private static int[][] generateMatrix(int N, int M, int minVal, int maxVal) {
        int[][] matrix = new int[N][M];
        Random random = new Random();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                matrix[i][j] = random.nextInt(maxVal - minVal + 1) + minVal;
            }
        }
        return matrix;
    }

    private static void printMatrix(int[][] matrix) {
        System.out.println("\nЗгенерована матриця (" + matrix.length + "x" + matrix[0].length + "):");
        for (int[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
    }

    private static long[] runWorkStealing(int[][] matrix) {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        ColumnSumTask mainTask = new ColumnSumTask(matrix, 0, matrix[0].length);
        return pool.invoke(mainTask);
    }

    private static long[] runWorkDealing(int[][] matrix) throws InterruptedException, ExecutionException {
        int M = matrix[0].length;
        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<Long>> futures = new ArrayList<>();

        for (int c = 0; c < M; c++) {
            ColumnSumCallable task = new ColumnSumCallable(matrix, c);
            futures.add(executor.submit(task));
        }

        long[] result = new long[M];
        for (int c = 0; c < M; c++) {
            result[c] = futures.get(c).get();
        }

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        return result;
    }
}