package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-19
 */

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class FrameEncoder extends MessageToByteEncoder<Frame> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Frame msg, ByteBuf out) throws Exception{
        msg.encode(out);
        //System.out.println("out readable:"+out.readableBytes());
    }

}
