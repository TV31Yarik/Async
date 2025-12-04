package async.Task3;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Task3_2 {
    static class FileSearchTask extends RecursiveTask<Integer> {
        private final File directory;
        private final long minSize;

        public FileSearchTask(File directory, long minSize) {
            this.directory = directory;
            this.minSize = minSize;
        }

        @Override
        protected Integer compute() {
            int foundFiles = 0;
            List<FileSearchTask> subTasks = new ArrayList<>();

            File[] contents = directory.listFiles();

            if (contents == null) {
                return 0;
            }

            for (File file : contents) {
                if (file.isDirectory()) {
                    FileSearchTask subTask = new FileSearchTask(file, minSize);
                    subTask.fork();
                    subTasks.add(subTask);
                } else {
                    if (file.length() > minSize) {
                        foundFiles++;
                    }
                }
            }

            for (FileSearchTask task : subTasks) {
                foundFiles += task.join();
            }

            return foundFiles;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.println("--- Пошук Файлів за Розміром (Work Stealing) ---");
            System.out.print("Введіть шлях до директорії: ");
            String path = scanner.nextLine();

            System.out.print("Введіть мінімальний розмір файлу в байтах (наприклад, 1024): ");
            long minSize = scanner.nextLong();

            if (minSize < 0) {
                System.err.println("Розмір не може бути від'ємним.");
                return;
            }

            File rootDir = new File(path);

            if (!rootDir.exists() || !rootDir.isDirectory()) {
                System.err.println("Помилка: Вказаний шлях не існує або не є директорією.");
                return;
            }

            long startTime = System.currentTimeMillis();

            ForkJoinPool pool = ForkJoinPool.commonPool();
            FileSearchTask mainTask = new FileSearchTask(rootDir, minSize);

            int totalFound = pool.invoke(mainTask);

            long endTime = System.currentTimeMillis();

            System.out.println("\n--- РЕЗУЛЬТАТ ---");
            System.out.println("Директорія для пошуку: " + rootDir.getAbsolutePath());
            System.out.println("Мінімальний розмір: " + minSize + " байт");
            System.out.println("Кількість знайдених файлів, більших за " + minSize + " байт: " + totalFound);
            System.out.println("Час роботи програми: " + (endTime - startTime) + " мс");


        } catch (Exception e) {
            System.err.println("Помилка: Некоректний ввід або проблема з файловою системою: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}