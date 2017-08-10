package com.leansoft.bigqueue;

import com.leansoft.bigqueue.netty.TimeClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Tony on 2017/7/27.
 */

public class FileRead implements Runnable{
    private String testDir = TestUtil.TEST_BASE_DIR + "bigqueue/unit";
    private BigQueueImpl bigQueue;
    //文件读取指针游标
    public static long pointer;
    public FileRead(BigQueueImpl bigQueue){
        this.bigQueue = bigQueue;
    }
    public FileRead(){

    }


    public void enqueue() throws FileNotFoundException {
        System.out.println("入队：");
/*        try {
            bigQueue = new BigQueueImpl(testDir, "queueTest66");
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        while (true){
          //  *
          //   * 判断文件是否存在，不存在则创建存储指针游标pointer的文件
          //   * 创建完文件后将pointer的值赋为0
          //   *
          //   * 第二次之后执行时pointer的值为上一次读取到的point

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String currentTime = df.format(new Date());
            File file2 = new File("D://logs//point");
            File fileDir = new File(file2,"point.txt");//point_"+currentTime+".txt
            if(!file2.isDirectory())
                file2.mkdir();
            if(!fileDir.isFile()){
                try{
                    fileDir.createNewFile();
                    FileWriter fw = new FileWriter(fileDir);
                    pointer = 0;
                    String startpoint = String.valueOf(pointer);
                    fw.write(startpoint);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            FileInputStream fin=new FileInputStream(fileDir);
            byte[] bs=new byte[1024];
            int count=0;
            try {
                while((count=fin.read(bs))>0)
                {
                    String str=new String(bs,0,count);//反复定义新变量：每一次都 重新定义新变量，接收新读取的数据
                    long outpointer = Long.parseLong(str);
                    //System.out.println("读取文件中的point:"+outpointer);
                    pointer = outpointer;//将文件中的新的point赋给新的开始读取pointer
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String path ="D://logs//sendlogs//sql.log";//D://logs//sql_"+currentTime+".log  D://logs//sendlogs//sql.log
            try {
                randomRed(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                // System.out.println("停顿开始："+System.currentTimeMillis());
                //停顿10秒，方便操作日志文件，看效果。
                Thread.sleep(1000);
                // System.out.println("停顿结束："+System.currentTimeMillis());
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    /**
     * 读取文件的方法
     * @param path 文件路径
     * **/
    public  void randomRed(String path)throws IOException{

            File file = new File(path);
            if(file == null){
                System.out.println("文件不存在");
            }
            RandomAccessFile raf=new RandomAccessFile(file, "r");
            //获取RandomAccessFile对象文件指针的位置，初始位置是0
            raf.seek(pointer);//移动文件指针位置
            String line =null;
            List list = new ArrayList();
            SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentTime1 = df1.format(new Date());
            //循环读取
            while((line = raf.readLine())!=null){
                if(line.equals("")){
                    continue;
                }
                //打印读取的内容,并将字节转为字符串输入，做转码处理，要不然中文会乱码
                line = new String(line.getBytes("ISO-8859-1"),"utf-8");
                System.out.println("line :"+currentTime1+"--->"+line);
                bigQueue.enqueue(line.getBytes());
            }
           //  String value = String.valueOf(bigQueue.dequeue());
          //   System.out.println("出队--->"+value);
/*            try {
                new TimeClient(bigQueue).connect(8083, "127.0.0.1");
            } catch (Exception e) {
                e.printStackTrace();
            }*/

        //文件读取完毕后，将文件指针游标设置为当前指针位置 。 运用这个方法可以做很多文章，比如查到自己想要的行的话，可以记下来，下次直接从这行读取
            File file3 = new File("D://logs//point");
            File fileDir1 = new File(file3,"point.txt");//point_"+currentTime1+".txt
           // System.out.println("上次读取到的point:"+raf.getFilePointer());
            FileWriter fw = new FileWriter(fileDir1);
            String inpointer = String.valueOf(raf.getFilePointer());
           // System.out.println("写入文件的新point:"+inpointer);
            fw.write(inpointer);
            fw.close();
    }

    @Override
    public void run() {
        try {
            enqueue();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
