package com.leansoft.bigqueue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by Tony on 2017/7/26.
 */
public class BlockingDeque {
    //阻塞队列，FIFO
   // private static LinkedBlockingQueue<String> concurrentLinkedQueue = new LinkedBlockingQueue<String>();

    public BlockingDeque() throws IOException {
    }

    public static void main(String[] args) throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(new Producer("producer1"));
        executorService.submit(new Consumer("consumer1"));
    }
    static class Producer implements Runnable {
        BigQueueImpl bigQueue = new BigQueueImpl("D://queue_db//unit","queue_Test01");
        private String name;
        public Producer(String name) throws IOException {
            this.name = name;
        }

        public void run() {
            JavaFile javaFile = new JavaFile();
            try {
                List lists = javaFile.filereadline("D://logs//sql.log");
                for (int i = 0; i < lists.size(); i++) {
                    System.out.println("存入--->"+lists.get(i));
                    String value = (String) lists.get(i);
                    try {
                        bigQueue.enqueue(value.getBytes());
                        Thread.sleep(200); //模拟慢速的生产，产生阻塞的效果
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class Consumer implements Runnable {
        BigQueueImpl bigQueue = new BigQueueImpl("D://queue_db//unit","queue_Test01");
        private String name;
        public Consumer(String name) throws IOException {
            this.name = name;
        }
        public void run() {
            for (int i = 1; i < 10; ++i) {
                try {
                    System.out.println("取出--->"+bigQueue.dequeue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
