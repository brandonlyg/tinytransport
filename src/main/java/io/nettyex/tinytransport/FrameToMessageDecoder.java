package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-20
 */

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 把Frame转换成FMessage
 */
@ChannelHandler.Sharable
public class FrameToMessageDecoder extends MessageToMessageDecoder<Frame> {

    private Map<Integer, FMessageTrait> fmTraits = new HashMap<>();


    public void addFMessageTrait(FMessageTrait trait){
        fmTraits.put(trait.getContentType(), trait);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Frame frame, List<Object> out) throws Exception {
        int contentType = frame.getHeader().getContentType();
        FMessageTrait trait = fmTraits.get(contentType);
        if(null == trait){
            throw new EncoderException("can't find trait. contentType:"+contentType);
        }

        FMessage fmsg = trait.decode(frame);
        out.add(fmsg);
    }
}
