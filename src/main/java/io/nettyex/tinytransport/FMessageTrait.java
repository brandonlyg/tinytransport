package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-20
 */

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

/**
 * FMessage消息特征接口,根据不同的contentType进行Frame和Frame直接的转换
 */
public interface FMessageTrait {

    /**
     * 得到匹配的contentType
     * @return contentType的值
     */
    int getContentType();

    Frame encode(FMessage fmsg) throws EncoderException;
    FMessage decode(Frame frame) throws DecoderException;
}
