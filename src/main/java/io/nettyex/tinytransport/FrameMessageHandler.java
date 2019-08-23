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
public class FrameMessageHandler extends SimpleChannelInboundHandler<FMessage> {

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

    protected void onPing(ChannelHandlerContext ctx, FMessage msg) throws Exception{
        FMessage pong = FMessage.buildPong(msg);
        ctx.channel().writeAndFlush(pong);
    }

    protected void onPong(ChannelHandlerContext ctx, FMessage msg) throws Exception{
        ctx.fireChannelRead(msg);
    }
    protected void onRequest(ChannelHandlerContext ctx, FMessage msg) throws Exception{
        ctx.fireChannelRead(msg);

    }
    protected void onResponse(ChannelHandlerContext ctx, FMessage msg) throws Exception{
        ctx.fireChannelRead(msg);

    }
    protected void onPush(ChannelHandlerContext ctx, FMessage msg) throws Exception{
        ctx.fireChannelRead(msg);
    }
}
