package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-22
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 对Frame的conent进行GZip压缩/解压
 */
@ChannelHandler.Sharable
public class FrameGZipCodec extends MessageToMessageCodec<Frame, Frame> {
    private static final byte ALGORITHM = 0x01;
    private static final int DEFAULT_COMPRESSTHRESHOLD = 4096;

    //压缩阈值，content长度>=compressThreshold时才压缩数据
    private int compressThreshold;

    public FrameGZipCodec(){
        this(DEFAULT_COMPRESSTHRESHOLD);
    }

    public FrameGZipCodec(int compressThreshold){
        this.compressThreshold = compressThreshold;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Frame msg, List<Object> out) throws Exception {
        ByteBuf cont = msg.getContent();

        if(null != cont && cont.readableBytes() > compressThreshold){
            ByteArrayOutputStream outs = new ByteArrayOutputStream();
            GZIPOutputStream gzip = null;
            try {
                gzip = new GZIPOutputStream(outs);
                gzip.write(cont.array(), cont.arrayOffset()+cont.readerIndex(), cont.readableBytes());
                cont.clear();
                cont.writeBytes(outs.toByteArray());

                msg.getHeader().setCompression(ALGORITHM);
            } catch ( Exception e) {
                throw new EncoderException(e.getMessage());
            } finally {
                if(null != gzip) gzip.close();
            }
        }
        msg.retain();

        out.add(msg);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Frame msg, List<Object> out) throws Exception {
        ByteBuf cont = msg.getContent();
        if(null != cont && cont.readableBytes() > 0 && msg.getHeader().getCompression() == ALGORITHM){
            GZIPInputStream ungzip = null;
            try {
                ungzip = new GZIPInputStream(new ByteArrayInputStream(ByteBufUtil.getBytes(cont)));
                cont.clear();
                byte[] buf = new byte[4096];
                int rlen;

                while((rlen = ungzip.read(buf)) > 0){
                    cont.writeBytes(buf, 0, rlen);
                }
            } catch (IOException e) {
                throw new DecoderException(e.getMessage());
            } finally {
                if(null != ungzip) ungzip.close();
            }

        }
        msg.retain();
        out.add(msg);
    }
}
