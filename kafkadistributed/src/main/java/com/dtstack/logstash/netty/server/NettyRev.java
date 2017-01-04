package com.dtstack.logstash.netty.server;

import com.dtstack.logstash.annotation.Required;
import com.dtstack.logstash.logmerge.LogPool;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * netty 接收zk动态平衡的时候其他服务发送的消息
 * Date: 2017/1/3
 * Company: www.dtstack.com
 *
 * @ahthor xuchao
 */

public class NettyRev {

    private static Logger logger = LoggerFactory.getLogger(NettyRev.class);

    @Required(required = true)
    private static int port;

    private static String host = "0.0.0.0";

    private static String encoding = "utf-8";

    private static int receiveBufferSize = 1024 * 1024 * 20;// 设置缓存区大小20M

    private static String delimiter = System.getProperty("line.separator");

    private ServerBootstrap bootstrap;

    private Executor bossExecutor;

    private Executor workerExecutor;

    public NettyRev(int port){
        this.port = port;
    }

    public void startup(){
        try {
            bossExecutor = Executors.newCachedThreadPool();
            workerExecutor = Executors.newCachedThreadPool();
            bootstrap = new ServerBootstrap(
                    new NioServerSocketChannelFactory(
                            bossExecutor,workerExecutor));
            final NettyServerHandler nettyServerHandler = new NettyServerHandler();
            // 设置一个处理客户端消息和各种消息事件的类(Handler)
            bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() throws Exception {
                    ChannelPipeline pipeline = Channels.pipeline();
                    pipeline.addLast(
                            "decoder",
                            new DelimiterBasedFrameDecoder(Integer.MAX_VALUE,
                                    false, true, ChannelBuffers.copiedBuffer(
                                    delimiter,
                                    Charset.forName(encoding))));
                    pipeline.addLast("handler", nettyServerHandler);
                    return pipeline;
                }
            });
            bootstrap.setOption("child.receiveBufferSize", receiveBufferSize);
            bootstrap.setOption("child.keepAlive", true);
            bootstrap.setOption("child.tcpNoDelay", true);
            bootstrap.bind(new InetSocketAddress(InetAddress.getByName(host),
                    port));

            logger.info("netty server start up success port:{}.", port);
        } catch (Exception e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
    }


    private static class NettyServerHandler extends SimpleChannelHandler {

        public NettyServerHandler() {
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
                throws Exception {
            Object message = e.getMessage();
            if (message != null) {
                if (message instanceof ChannelBuffer) {
                    String mes = ((ChannelBuffer) message).toString(Charset.forName(encoding));
                    //将数据加入到merge队列里面
                    LogPool.getInstance().addLog(mes);
                }
            }
        }

        @Override
        public void exceptionCaught(
                ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            logger.debug("netty io error:", e.getCause());
            ctx.sendUpstream(e);
        }

    }
}