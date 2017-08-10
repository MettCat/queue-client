package com.leansoft.bigqueue.netty;

import com.leansoft.bigqueue.BigQueueHandler;
import com.leansoft.bigqueue.BigQueueImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.*;


/**
 * Created by Tony on 2017/8/3.
 */
public class TimeClientHandler extends ChannelInboundHandlerAdapter {
    private byte[] req;
    private BigQueueImpl bigQueue;
    private String echo;

    public TimeClientHandler(BigQueueImpl bigQueue,String echo){
        this.bigQueue = bigQueue;
        this.echo=echo;
    }

    public TimeClientHandler()throws IOException{
            req  = ("我是数据"+"$_").getBytes();
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

            ByteBuf message = null;
            req =  (echo+"$_").getBytes();
            message = Unpooled.buffer(req.length);
            message.writeBytes(req);
            ctx.writeAndFlush(message);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String body =  (String)msg;
        System.out.println("服务器返回接收的数据为 : "+body+" ");
        File file = new File("D://logs//error//SendError.log");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
                System.out.println("文件中的数据2: " + tempString);
                if (tempString .equals(body)){
                    FileWriter fw =  new FileWriter(file,false);
                    fw.write("");
                    fw.close();
                }
            }
            System.out.println("----------------------结束-------------------------");
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
        BigQueueHandler bigQueueHandler = new BigQueueHandler(bigQueue);
        bigQueueHandler.dequeue();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //释放资源
        ctx.close();
    }
}
