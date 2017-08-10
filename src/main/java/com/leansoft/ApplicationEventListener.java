package com.leansoft;

import com.leansoft.bigqueue.*;
import com.leansoft.bigqueue.netty.TimeClient;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by train on 17/5/17.
 */
public class ApplicationEventListener implements ApplicationListener {
    private String testDir = TestUtil.TEST_BASE_DIR + "bigqueue/unit";
    //public static IBigQueue bigQueue;
    @Override
    public void onApplicationEvent(ApplicationEvent event) {

        // 在这里可以监听到Spring Boot的生命周期
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            // 初始化环境变量
        } else if (event instanceof ApplicationPreparedEvent) {
            // 初始化完成
        } else if (event instanceof ContextRefreshedEvent) {
            // 应用刷新
        } else if (event instanceof ApplicationReadyEvent) {
            // 应用已启动完成
            try {
                BigQueueImpl bigQueue = new BigQueueImpl(testDir,"testqueue001");
      /*          FileRead fileRead = new FileRead(bigQueue);
                System.out.println("已经进入入队方法：");
                fileRead.run();*/
        /*        System.out.println("已经进入出队方法：");
                BigQueueHandler bigQueueHandler = new BigQueueHandler(bigQueue);
                bigQueueHandler.dequeue();*/
                FileRead fileRead = new FileRead(bigQueue);
                Thread thread1 = new Thread(fileRead);
                BigQueueHandler bigQueueHandler = new BigQueueHandler(bigQueue);
                Thread thread = new Thread(bigQueueHandler);
                thread1.start();
                thread.start();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (event instanceof ContextStartedEvent) {
            //应用启动，需要在代码动态添加监听器才可捕获
        } else if (event instanceof ContextStoppedEvent) {
            // 应用停止
        } else if (event instanceof ContextClosedEvent) {
            // 应用关闭
        } else {

        }
    }
}
