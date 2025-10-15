package async;
import java.util.concurrent.Semaphore;
class Warehouse {
    private Semaphore semaphore;
    private int products;
    static int hour = 10;
    public Warehouse(int capacity) {
        semaphore = new Semaphore(capacity);
        products = 0;
    }


    public synchronized void addProduct() {
        try {
            semaphore.acquire();
            products++;
            System.out.println("Постачальник додав 1 товар. Тепер на складі: " + products);
        } catch (InterruptedException e) {
            System.out.println("Постачальник був перерваний!");
        }
    }
    public synchronized void takeProduct() {
        if (hour < 9 || hour > 18) {
            System.out.println("Покупець не може взяти товар — зараз неробочий час!");
            return;
        }

        if (products <= 0) {
            System.out.println("Покупець намагається взяти товар, але склад порожній!");
            return;
        }

        products--;
        semaphore.release();
        System.out.println("Покупець забрав 1 товар. Тепер на складі: " + products);
    }
    public void stop(){
        hour+=2;
        if (hour>23){
            System.out.println("Робочий день завершено! Програма зупиняється.");
            System.exit(0);
        }
    }
}

class Supplier implements Runnable {
    private Warehouse warehouse;

    public Supplier(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    @Override
    public void run() {
        while (true) {
            warehouse.addProduct();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Постачальник зупинений!");
                break;
            }
        }
    }
}


class Customer implements Runnable {
    private Warehouse warehouse;

    public Customer(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    @Override
    public void run() {
        while (true) {
            warehouse.takeProduct();
            warehouse.stop();
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                System.out.println("Покупець зупинений!");
                break;
            }
        }
    }
}


public class Task1 {
    public static void main(String[] args) {
        Warehouse warehouse = new Warehouse(5);

        Thread supplierThread = new Thread(new Supplier(warehouse));
        Thread customerThread = new Thread(new Customer(warehouse));

        supplierThread.start();
        customerThread.start();

    }
}
