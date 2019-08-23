package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-19
 */


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;


public class FrameDecoder extends LengthFieldBasedFrameDecoder {

    public FrameDecoder(){
        super(Frame.MAX_LENGTH, Frame.HEADER_LENGTH - 2, 2);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        //找到begin位置
        int start = in.readerIndex();
        int begin = in.getInt(start + 0);
        if(begin != Frame.BEGIN){
            dropFailedData(in);
        }

        //解码得到Frame对象
        ByteBuf dataPack = null;
        try{
            dataPack = (ByteBuf)super.decode(ctx, in);
            Frame frame = Frame.decode(dataPack);
            return frame;
        }finally {
            if(null != dataPack){
                dataPack.release();
            }
        }
    }

    /**
     * 丢弃掉错误的数据，直至找到BEGIN为止
     * @param in
     */
    private void dropFailedData(ByteBuf in){
        int readIndex = 0;
        int start = in.readerIndex();
        for(; readIndex < in.readableBytes(); ++ readIndex){
            int begin = in.getInt(start + readIndex);
            if(Frame.BEGIN == begin){
                break;
            }
        }
        in.skipBytes(readIndex);
        throw new CorruptedFrameException("drop failed data cause of not begin with BEGIN");
    }
}
