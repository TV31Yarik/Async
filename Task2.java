package async;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Task2 {
    static class SquareCalculatorTask implements Callable<CopyOnWriteArraySet<Double>> {
        private final double[] subArray;

        public SquareCalculatorTask(double[] subArray) {
            this.subArray = subArray;
        }

        @Override
        public CopyOnWriteArraySet<Double> call() {
            CopyOnWriteArraySet<Double> squares = new CopyOnWriteArraySet<>();
            for (double num : subArray) {
                squares.add(num * num);
            }
            return squares;
        }
    }
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Введіть мінімальне значення: ");
        double min = scanner.nextDouble();

        System.out.print("Введіть максимальне значення: ");
        double max = scanner.nextDouble();

        Random random = new Random();
        int arraySize = 40 + random.nextInt(21);
        System.out.println("Розмір масиву: " + arraySize);

        double[] numbers = new double[arraySize];
        for (int i = 0; i < arraySize; i++) {
            numbers[i] = min + (max - min) * random.nextDouble();
        }

        System.out.println("Початковий масив: " + Arrays.toString(numbers));

        long startTime = System.currentTimeMillis();

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int threadCount = Math.max(2, Math.min(4, availableProcessors));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<CopyOnWriteArraySet<Double>>> futures = new ArrayList<>();
        int chunkSize = (int) Math.ceil((double) arraySize / threadCount);

        System.out.println("Кількість потоків: " + threadCount + ", розмір частини: " + chunkSize);

        for (int i = 0; i < threadCount; i++) {
            int startIndex = i * chunkSize;
            int endIndex = Math.min(startIndex + chunkSize, arraySize);

            if (startIndex < arraySize) {
                double[] subArray = Arrays.copyOfRange(numbers, startIndex, endIndex);
                SquareCalculatorTask task = new SquareCalculatorTask(subArray);
                Future<CopyOnWriteArraySet<Double>> future = executor.submit(task);
                futures.add(future);
                System.out.println("Створено задачу для індексів " + startIndex + "-" + (endIndex-1));
            }
        }

        CopyOnWriteArraySet<Double> allSquares = new CopyOnWriteArraySet<>();
        boolean allTasksCompleted = true;

        for (int i = 0; i < futures.size(); i++) {
            Future<CopyOnWriteArraySet<Double>> future = futures.get(i);

            try {
                System.out.println("\nПеревірка задачі " + i + ":");
                System.out.println("isDone(): " + future.isDone());
                System.out.println("isCancelled(): " + future.isCancelled());

                CopyOnWriteArraySet<Double> result = future.get(10, TimeUnit.SECONDS);
                allSquares.addAll(result);

                System.out.println("Задача " + i + " успішно завершена. Результатів: " + result.size());

            } catch (TimeoutException e) {
                System.err.println("Задача " + i + " перевищила час очікування!");
                allTasksCompleted = false;
                future.cancel(true); // Скасовуємо задачу

            } catch (InterruptedException e) {
                System.err.println("Задача " + i + " була перервана!");
                allTasksCompleted = false;
                Thread.currentThread().interrupt();

            } catch (ExecutionException e) {
                System.err.println("Помилка виконання задачі " + i + ": " + e.getCause());
                allTasksCompleted = false;
            }
        }

        executor.shutdown();

        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        System.out.println("\n=== РЕЗУЛЬТАТИ ===");
        System.out.println("Всі задачі завершені: " + allTasksCompleted);
        System.out.println("Загальна кількість квадратів: " + allSquares.size());

        List<Double> sortedSquares = allSquares.stream()
                .sorted()
                .collect(Collectors.toList());

        System.out.println("Перші 10 квадратів: " +
                sortedSquares.stream().limit(10).collect(Collectors.toList()));
        System.out.println("Останні 10 квадратів: " +
                sortedSquares.stream().skip(Math.max(0, sortedSquares.size() - 10)).collect(Collectors.toList()));

        System.out.println("\nЧас виконання програми: " + executionTime + " мс");

        scanner.close();
    }
}
