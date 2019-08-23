package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-22
 */

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 *TCP Client连接器
 */
public class TcpConnector {
    public static final String H_FRAME_DECODER = "FrameDecoder";
    public static final String H_FRAME_ENCODER = "FrameEncoder";
    public static final String H_READ_TIMEOUT = "ReadTimeout";
    public static final String H_FM_DECODER = "FMessageDecoder";
    public static final String H_FM_ENCODER = "FMessageEncoder";
    public static final String H_FM_HANDLER = "FMessagehandler";

    private static final Logger log = LoggerFactory.getLogger(TcpConnector.class);

    private static final int MIN_READ_TIMEOUT = 30;

    private Bootstrap bootstrap;
    private EventLoopGroup workerElg;

    //读超时时间(s)
    private int readTimeout;
    //ping的时间间隔
    private int pingInterval;

    //默认的ChannelHandler
    protected FrameEncoder frameEncoder = new FrameEncoder();
    protected FrameToMessageDecoder fmDecoder = new FrameToMessageDecoder();
    protected MessageToFrameEncoder fmEncoder = new MessageToFrameEncoder();

    protected ClientHandler clientHandler = new ClientHandler();

    public TcpConnector(int workerThreads, int readTimeout){
        workerElg = new NioEventLoopGroup(workerThreads);

        bootstrap = new Bootstrap();
        bootstrap.group(workerElg)
                .channel(NioSocketChannel.class)
                .handler(new HandlerInitalizer())
        ;

        this.readTimeout = readTimeout < MIN_READ_TIMEOUT ? MIN_READ_TIMEOUT : readTimeout;
        this.pingInterval = this.readTimeout - 5;

        fmDecoder.addFMessageTrait(FMTraits.FMTBytes);
        fmEncoder.addFMessageTrait(FMTraits.FMTBytes);
    }

    public void addFMessageTrait(FMessageTrait trait){
        fmEncoder.addFMessageTrait(trait);
        fmDecoder.addFMessageTrait(trait);
    }

    public TcpClient connect(InetSocketAddress address) throws Exception{
        ChannelFuture future = bootstrap.connect(address);
        Channel channel = future.channel();

        TcpClient client = new TcpClient(channel, workerElg.next());
        channel.attr(TcpClient.CLIENT).set(client);

        future.sync();

        return client;
    }

    public void destroy(){
        workerElg.shutdownGracefully();
        log.info("tcp connector is destroyed");

    }

    protected void doInitChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pl = ch.pipeline();

        pl.addLast(H_FRAME_DECODER, new FrameDecoder());
        pl.addLast(H_FRAME_ENCODER, frameEncoder);

        pl.addLast(H_READ_TIMEOUT, new ReadTimeoutHandler(readTimeout, TimeUnit.SECONDS));

        pl.addLast(H_FM_DECODER, fmDecoder);
        pl.addLast(H_FM_ENCODER, fmEncoder);

        pl.addLast(H_FM_HANDLER, clientHandler);
    }

    private class HandlerInitalizer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            // TODO Auto-generated method stub
            //log.info("initChannel");
            doInitChannel(ch);
        }
    }

    @ChannelHandler.Sharable
    protected class ClientHandler extends FrameMessageHandler {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Channel chnl = ctx.channel();
            ScheduledFuture<?> schedule = workerElg.next().scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    log.info("send ping");
                    FMessage ping = FMessage.buildPing();
                    chnl.writeAndFlush(ping);
                }
            }, 1, pingInterval, TimeUnit.SECONDS);
            chnl.attr(TcpClient.SCHEDULE).set(schedule);

            log.info("tcp client connected. remote address:{}", chnl.remoteAddress().toString());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel chnl = ctx.channel();
            java.util.concurrent.ScheduledFuture<?> future = chnl.attr(TcpClient.SCHEDULE).getAndSet(null);
            if(null != future){
                future.cancel(true);
            }
            chnl.attr(TcpClient.CLIENT).set(null);
            log.info("tcp client closed. remote address:{}", chnl.remoteAddress().toString());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("", cause.getMessage());
        }

        @Override
        protected void onPong(ChannelHandlerContext ctx, FMessage msg) throws Exception {
        }

        @Override
        protected void onResponse(ChannelHandlerContext ctx, FMessage msg) throws Exception {
           TcpClient client = ctx.channel().attr(TcpClient.CLIENT).get();
           if(null == client) return;
           client.onResponse(msg);
        }
    }


}
