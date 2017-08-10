package com.leansoft.bigqueue.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Created by Tony on 2017/8/3.
 */
public class TimeServer  {

    public void bind(int port) throws Exception{

        //第一个用户服务器接收客户端的连接
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        //第二个用户SocketChannel的网络读写
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            //创建ServerBootstrap对象，启动 NIO服务端的辅助启动类
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).
                    //设置为NIO
                            channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChildChannelHandler());

            //绑定端口，同步等待成功
            ChannelFuture f = b.bind(port).sync();

            //等待服务器监听端口关闭
            f.channel().closeFuture().sync();

        }finally{
            //释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel>{

        @Override
        protected void initChannel(SocketChannel arg0) throws Exception {
            //解决TCP粘包问题,以"$_"作为分隔
            ByteBuf delimiter = Unpooled.copiedBuffer("$_".getBytes());
            arg0.pipeline().addLast(new DelimiterBasedFrameDecoder(1024,delimiter));
            arg0.pipeline().addLast(new StringDecoder());
            arg0.pipeline().addLast(new TimeServerHandler());

        }
    }

    public static void main(String[] args) throws Exception {

        int port = 8083;
        if(args != null && args.length>0){
            try{
                port = Integer.valueOf(args[0]);
            }catch(Exception e){
                //采用默认值
            }
        }
        new TimeServer().bind(port);
    }
}
