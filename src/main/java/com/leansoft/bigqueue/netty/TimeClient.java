package com.leansoft.bigqueue.netty;

import com.leansoft.bigqueue.BigQueueImpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * Created by Tony on 2017/8/3.
 */
public class TimeClient  {
    private BigQueueImpl bigQueue;
    private  String echo;

    public String getEcho() {
        return echo;
    }

    public void setEcho(String echo) {
        this.echo = echo;
    }

    public TimeClient(BigQueueImpl bigQueue){
        this.bigQueue = bigQueue;
    }
    public TimeClient(){

    }
    public void connect(int port , String host) throws Exception{

        //配置客户端NIO线程组
        //ChannelOption.TCP_NODELAY 保证最快有数据回复
        EventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception{
                            //解决TCP粘包问题,以"$_"作为分隔
                            ByteBuf delimiter = Unpooled.copiedBuffer("$_".getBytes());
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024,delimiter));
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new TimeClientHandler(bigQueue,echo));
                        }
                    });
            //发起异步连接操作
            ChannelFuture f = b.connect(host, port).sync();
            //等待客户端链路关闭
            f.channel().closeFuture().sync();
        }finally{
            //释放NIO线程
            group.shutdownGracefully();
        }

    }
}
