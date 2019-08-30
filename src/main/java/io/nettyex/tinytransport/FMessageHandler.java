package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-19
 */


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 处理FrameMessage消息的ChannelInboundHandler
 */
public class FMessageHandler extends SimpleChannelInboundHandler<FMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FMessage msg) throws Exception {
        Command cmd = msg.getCmd();
        if(Command.PING.equals(cmd)){
            onPing(ctx, msg);
        }else if(Command.PONG.equals(cmd)){
            onPong(ctx, msg);
        }else if(Command.REQUEST.equals(cmd)){
            onRequest(ctx, msg);
        }else if(Command.RESPONSE.equals(cmd)){
            onResponse(ctx, msg);
        }else if(Command.PUSH.equals(cmd)){
            onPush(ctx, msg);
        }
    }

    /**
     * 收到PING
     * @param ctx
     * @param msg
     * @throws Exception
     */
    protected void onPing(ChannelHandlerContext ctx, FMessage msg) throws Exception{
        FMessage pong = FMessage.buildPong(msg);
        ctx.channel().writeAndFlush(pong);
    }

    /**
     * 收到PONG
     * @param ctx
     * @param msg
     * @throws Exception
     */
    protected void onPong(ChannelHandlerContext ctx, FMessage msg) throws Exception{
        ctx.fireChannelRead(msg);
    }

    /**
     * 收到REQUEST
     * @param ctx
     * @param msg
     * @throws Exception
     */
    protected void onRequest(ChannelHandlerContext ctx, FMessage msg) throws Exception{
        ctx.fireChannelRead(msg);

    }

    /**
     * 收到RESPONSE
     * @param ctx
     * @param msg
     * @throws Exception
     */
    protected void onResponse(ChannelHandlerContext ctx, FMessage msg) throws Exception{
        ctx.fireChannelRead(msg);

    }

    /**
     * 收到PUSH
     * @param ctx
     * @param msg
     * @throws Exception
     */
    protected void onPush(ChannelHandlerContext ctx, FMessage msg) throws Exception{
        ctx.fireChannelRead(msg);
    }
}
