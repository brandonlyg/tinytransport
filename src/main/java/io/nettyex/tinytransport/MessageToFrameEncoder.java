package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-20
 */

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 把FMessage转换成Frame
 */
@ChannelHandler.Sharable
public class MessageToFrameEncoder extends MessageToMessageEncoder<FMessage> {

    private Map<Integer, FMessageTrait> fmTraits = new HashMap<>();


    public void addFMessageTrait(FMessageTrait trait){
        fmTraits.put(trait.getContentType(), trait);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, FMessage msg, List<Object> out) throws Exception {
        FMessageTrait trait = fmTraits.get(msg.getContentType());
        if(null == trait){
            throw new EncoderException("can't find trait. contentType:"+msg.getContentType());
        }
        //System.out.println("encode frame");
        Frame frame = trait.encode(msg);
        out.add(frame);
    }
}
