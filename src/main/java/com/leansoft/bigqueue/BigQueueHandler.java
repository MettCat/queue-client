package com.leansoft.bigqueue;

import com.leansoft.bigqueue.netty.TimeClient;


import java.io.*;

/**
 * Created by Tony on 2017/8/4.
 */
public class BigQueueHandler implements Runnable{

    private BigQueueImpl bigQueue;


    public BigQueueHandler(BigQueueImpl bigQueue)
    {
        this.bigQueue = bigQueue;
    }
    public BigQueueHandler(){

    }
    TimeClient timeClient = new TimeClient(bigQueue);


    public void dequeue() throws IOException{

        while (true){
            if(bigQueue.isEmpty()){
                continue;
            }
            try {
                System.out.println("----------------------开始-------------------------");
                File file = new File("D://logs//error");
                File fileDir = new File(file,"SendError.log");
                if(!file.isDirectory())
                    file.mkdir();
                if(!fileDir.isFile()){
                    fileDir.createNewFile();
                }
                if (fileDir.exists() && fileDir.length() == 0){
                    String value = new String(bigQueue.dequeue());
                    System.out.println("出队-----》开始传输：------》"+value);
                    FileWriter fw = new FileWriter(fileDir);
                    fw.write(value);
                    fw.close();
                }
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(fileDir));
                    String tempString = null;
                    // 一次读入一行，直到读入null为文件结束
                    while ((tempString = reader.readLine()) != null) {
                        // 显示行号
                        System.out.println("文件中的数据1: " + tempString);
                        timeClient.setEcho(tempString);
                    }
                    timeClient.connect(8083,"10.190.3.8");
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e1) {
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    @Override
    public void run() {
        try {
            dequeue();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
