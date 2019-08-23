package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-19
 */

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

import static io.netty.buffer.Unpooled.wrappedBuffer;

/**
 * 常用的FMessageTrait实现
 */
public class FMTraits {

    public static abstract class AbstractFMTrait implements FMessageTrait{
        @Override
        public Frame encode(FMessage fmsg) throws EncoderException {
            if(fmsg.getContentType() != this.getContentType()){
                throw new EncoderException("conentType is not match. "+this.getContentType()+"-"+fmsg.getContentType());
            }

            Frame.Header header = new Frame.Header();
            header.setCmd(fmsg.getCmd());
            header.setSequenceId(fmsg.getSequenceId());
            header.setResCode((byte)(fmsg.getResCode() & 0XFF));
            header.setContentType((byte)(fmsg.getContentType()));

            Frame frame = new Frame();
            frame.setHeader(header);
            ByteBuf content = encodeContent(fmsg);
            frame.setContent(content);

            return frame;
        }

        @Override
        public FMessage decode(Frame frame) throws DecoderException {
            Frame.Header header = frame.getHeader();
            if(header.getContentType() != this.getContentType()){
                throw new DecoderException("conentType is not match. "+this.getContentType()+"-"+header.getContentType());
            }

            Object content = decodeContent(frame);
            FMessage fmsg = new FMessage();
            fmsg.setCmd(header.getCmd());
            fmsg.setContentType(getContentType());
            fmsg.setSequenceId(header.getSequenceId());
            fmsg.setResCode(header.getResCode());
            fmsg.setContent(content);

            return fmsg;
        }

        protected abstract ByteBuf encodeContent(FMessage fmsg) throws EncoderException;
        protected abstract Object decodeContent(Frame frame) throws DecoderException;
    }

    /**
     * content 是byte[]
     */
    public static final int BYTES = 0x01;
    public static final FMessageTrait FMTBytes = new FMTraitBytes();
    public static class FMTraitBytes extends AbstractFMTrait {
        protected int contentType;

        public FMTraitBytes(){
            this(BYTES);
        }

        public FMTraitBytes(int contentType){
            this.contentType = contentType;
        }

        @Override
        public int getContentType() {
            return contentType;
        }

        @Override
        protected ByteBuf encodeContent(FMessage fmsg) throws EncoderException{
            byte[] bytes = (byte[])fmsg.getContent();

            ByteBuf buf = null;
            if(null != bytes && bytes.length > 0){
                buf = ByteBufAllocator.DEFAULT.buffer(bytes.length);
                buf.writeBytes(bytes);
            }

            return buf;
        }

        @Override
        protected Object decodeContent(Frame frame) throws DecoderException {
            ByteBuf buf = frame.getContent();
            byte[] bytes = null;
            if(null != buf && buf.readableBytes() > 0){
                bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
            }

            return bytes;
        }
    }

    /**
     * content 是String
     */
    public static final int STRING = 0x02;
    public static final FMessageTrait FMTString = new FMTraitString();
    public static class FMTraitString extends FMTraitBytes {
        public FMTraitString(){
            super(STRING);
        }

        @Override
        protected ByteBuf encodeContent(FMessage fmsg) throws EncoderException{
            String str = (String)fmsg.getContent();
            ByteBuf buf = null;
            try {
                if (null != str && str.length() > 0) {
                    byte[] bytes = str.getBytes("utf-8");

                    buf = ByteBufAllocator.DEFAULT.buffer(bytes.length);
                    buf.writeBytes(bytes);
                }
            }catch (Exception e){
                if(null != buf) buf.release();

                throw new EncoderException(e.getMessage());
            }
            return buf;
        }
        @Override
        protected Object decodeContent(Frame frame) throws DecoderException{
            byte[] bytes = (byte[])super.decodeContent(frame);
            String str = null;
            if(null != bytes){
                str = new String(bytes);
            }
            return str;
        }

    }

    /**
     * content 是Json格式的String
     */
    public static final int JSON = 0x03;
    public static final FMessageTrait FMTJson = new FMTraitJson();
    public static class FMTraitJson extends FMTraitString {
        public FMTraitJson(){
            contentType = JSON;
        }

        @Override
        protected ByteBuf encodeContent(FMessage fmsg) throws EncoderException{
            JSONObject json = (JSONObject)fmsg.getContent();
            ByteBuf buf = null;
            try {
                if (null != json) {
                    byte[] bytes = json.toJSONString().getBytes("utf-8");

                    buf = ByteBufAllocator.DEFAULT.buffer(bytes.length);
                    buf.writeBytes(bytes);
                }
            }catch (Exception e){
                if(null != buf) buf.release();
                throw new EncoderException(e.getMessage());
            }
            return buf;
        }
        @Override
        protected Object decodeContent(Frame frame) throws DecoderException{
            String str = (String)super.decodeContent(frame);
            JSONObject json = null;
            if(null != str){
                json = com.alibaba.fastjson.JSON.parseObject(str);
            }
            return json;
        }

    }


    /**
     * content 是ProtoBuf格式
     */
    public static final int PROTOBUF = 0x04;
    public static class FMTraitProtobuf extends FMTraitBytes {
        private final MessageLite prototype;

        public FMTraitProtobuf(MessageLite prototype){
            super(PROTOBUF);
            this.prototype = prototype;
        }

        @Override
        protected ByteBuf encodeContent(FMessage fmsg) throws EncoderException{
            Object content = fmsg.getContent();
            if(null == content){
                return null;
            }

            ByteBuf buf = null;
            if (content instanceof MessageLite) {
                buf = wrappedBuffer(((MessageLite) content).toByteArray());
            }else if (content instanceof MessageLite.Builder) {
                buf = wrappedBuffer(((MessageLite.Builder) content).build().toByteArray());
            }

            return buf;
        }
        @Override
        protected Object decodeContent(Frame frame) throws DecoderException{
            ByteBuf content = frame.getContent();
            MessageLite res = null;
            if(null == content){
                return res;
            }
            if(content.readableBytes() == 0){
                return res;
            }

            try {
                res = prototype.getParserForType().parseFrom(content.array(),
                        content.arrayOffset() + content.readerIndex(), content.readableBytes());
            }catch (Exception e){
                throw new DecoderException(e.getMessage());
            }

            return res;
        }

    }


}
