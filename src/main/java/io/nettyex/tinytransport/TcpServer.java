package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-19
 */

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * TCP Server实现
 */
public class TcpServer {
    private static final Logger log = LoggerFactory.getLogger(TcpServer.class);

    public static final String H_FRAME_DECODER = "FrameDecoder";
    public static final String H_FRAME_ENCODER = "FrameEncoder";
    public static final String H_READ_TIMEOUT = "ReadTimeout";
    public static final String H_FM_DECODER = "FMessageDecoder";
    public static final String H_FM_ENCODER = "FMessageEncoder";


    private ServerBootstrap bootstrap;
    private EventLoopGroup bossElg;
    private EventLoopGroup workerElg;

    private Channel serverChannel;

    //TCP 设置
    private int backlog;
    private InetSocketAddress localAddress;

    //读超时时间(s)
    private int readTimeout;

    //默认的ChannelHandler
    protected FrameEncoder frameEncoder = new FrameEncoder();

    protected FrameToMessageDecoder fmDecoder = new FrameToMessageDecoder();
    protected MessageToFrameEncoder fmEncoder = new MessageToFrameEncoder();


    public TcpServer(){
        bossElg = new NioEventLoopGroup();

        fmDecoder.addFMessageTrait(FMTraits.FMTBytes);
        fmEncoder.addFMessageTrait(FMTraits.FMTBytes);
    }

    public void setWorkerThreads(int threads){
        if(null != workerElg){
            return;
        }

        threads = threads <= 0 ? 0 : threads;
        workerElg = new NioEventLoopGroup(threads);
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void addFMessageTrait(FMessageTrait trait){
        fmEncoder.addFMessageTrait(trait);
        fmDecoder.addFMessageTrait(trait);
    }

    public void start() throws Exception{
        try {
            bootstrap = new ServerBootstrap();
            bootstrap.group(bossElg, workerElg)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HandlerInitalizer())
                    .option(ChannelOption.SO_BACKLOG, backlog)
                    .option(ChannelOption.SO_REUSEADDR, true)
            ;

            serverChannel = bootstrap.bind(localAddress).sync().channel();

            log.info("Tiny Tcp Server listen at:{}", localAddress.toString());
        }catch (Exception e){
            log.error("start Tiny Tcp Server at:{} error", localAddress.toString());
            throw e;
        }
    }

    public void shutdown(){
        serverChannel.close();
        bossElg.shutdownGracefully();
        workerElg.shutdownGracefully();
        log.info("Tiny Tcp Server shutdown. at:{}", localAddress.toString());
    }

    protected void doInitChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pl = ch.pipeline();

        pl.addLast(H_FRAME_DECODER, new FrameDecoder());
        pl.addLast(H_FRAME_ENCODER, frameEncoder);

        pl.addLast(H_READ_TIMEOUT, new ReadTimeoutHandler(readTimeout, TimeUnit.SECONDS));

        pl.addLast(H_FM_DECODER, fmDecoder);
        pl.addLast(H_FM_ENCODER, fmEncoder);
    }

    private class HandlerInitalizer extends ChannelInitializer<SocketChannel> {

        private DefaultEventExecutorGroup executor = null;

        public HandlerInitalizer(){
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            // TODO Auto-generated method stub

            doInitChannel(ch);
        }

    }

}
