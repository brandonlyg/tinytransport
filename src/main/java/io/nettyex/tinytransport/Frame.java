package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-19
 */


import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.ReferenceCounted;

/**
 *数据报类型
 *数据报格式
 * | begin | version | cmd | contentType | compression | sequenceId | resCode | contentLength | content |
 * begin: 开始标志, uint32, 常量: 0xADEF4BC9
 * version: 协议版本号， uint8, 当前是1
 * cmd: 命令字ID, uint8
 * contentType: 数据报中conent的类型, uint8
 * compression: 压缩算法, uint8
 * sequenceId:  数据包的序列号, uintr32
 * resCode: 响应码
 * contentLength: conent的长度
 * conent: 数据包的内容，Byte[contentLength]
 *
 */

public class Frame implements ReferenceCounted {
    public static final int BEGIN = 0xADEF4BC9;
    public static final int VERSION = 1;

    private static final int MAX_CONTENT_LENGTH = 0XFFFF;
    public static final int HEADER_LENGTH = 15;
    public static final int MAX_LENGTH = HEADER_LENGTH + MAX_CONTENT_LENGTH;

    private Header  header;
    private ByteBuf content;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public ByteBuf getContent() {
        return content;
    }

    public void setContent(ByteBuf content) {
        ByteBuf old = this.content;
        this.content = content;
        if(null != old){
            old.release();
        }
        if(null != this.content){
            this.content.retain();
        }
    }

    /**
     * 把Frame对象编码成数据包
     * @param out
     */
    public void encode(ByteBuf out){
        out.writeInt(BEGIN);
        out.writeByte(header.getVersion());
        out.writeByte(header.getCmd().getValue());
        out.writeByte(header.getContentType());
        out.writeByte(header.getCompression());
        out.writeInt(header.getSequenceId());
        out.writeByte(header.getResCode());

        int contentLength = 0;
        if(null != content){
            contentLength = content.readableBytes();
        }
        if(contentLength > MAX_CONTENT_LENGTH){
            throw new TooLongFrameException("content too long. contentLength:"+contentLength);
        }
        out.writeShort(contentLength);
        if(null != content){
            out.writeBytes(content);
        }
    }

    /**
     * 从数据包解码得到Frame
     * @param in 一个完整的数据包
     * @return Frame对象
     */
    public static Frame decode(ByteBuf in){
        if(in.readableBytes() < HEADER_LENGTH){
            throw new CorruptedFrameException("pack length less than header length("+HEADER_LENGTH+")");
        }

        //得到header
        Header header = new Header();
        in.readInt();
        header.setVersion(in.readByte());
        header.setCmd(Command.valueOf(in.readByte() & 0xFF));
        header.setContentType((byte)(in.readByte() & 0xFF));
        header.setCompression((byte)(in.readByte() & 0xFF));
        header.setSequenceId(in.readInt());
        header.setResCode((byte)(in.readByte() & 0xFF));

        //读出content
        int contentLength = in.readShort() & 0xFFFF;
        if(in.readableBytes() != contentLength){
            throw new CorruptedFrameException("content is not match."+in.readableBytes() + "-" + contentLength);
        }

        ByteBuf content = contentLength > 0 ? in.retainedSlice(in.readerIndex(), contentLength) : null;
        in.skipBytes(contentLength);

        //创建Frame对象
        Frame frame = new Frame();
        frame.setHeader(header);
        frame.setContent(content);

        if(null != content) content.release();

        return frame;
    }

    /**ReferenceCounted**/
    @Override
    public int refCnt() {
        return null == content ? 0 : content.refCnt();
    }

    @Override
    public ReferenceCounted retain() {
        if(null != content){
            content.retain();
        }
        return this;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        if(null != content){
            return content.retain(increment);
        }
        return this;
    }

    @Override
    public ReferenceCounted touch() {
        if(null != content){
            content.touch();
        }

        return this;
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        if(null != content){
            content.touch(hint);
        }
        return this;
    }

    @Override
    public boolean release() {
        return null == content ? false : content.release();
    }

    @Override
    public boolean release(int decrement) {
        return null == content ? false : content.release(decrement);
    }

    /**--ReferenceCounted--**/


    public static final class Header {
        private byte version;
        private Command cmd;
        private byte contentType;
        private byte compression;
        private int sequenceId;
        private byte resCode;

        public Header(){
            version = VERSION;
        }

        public byte getVersion() {
            return version;
        }

        public void setVersion(byte version) {
            this.version = version;
        }

        public Command getCmd() {
            return cmd;
        }

        public void setCmd(Command cmd) {
            this.cmd = cmd;
        }

        public byte getContentType() {
            return contentType;
        }

        public void setContentType(byte contentType) {
            this.contentType = contentType;
        }

        public byte getCompression() {
            return compression;
        }

        public void setCompression(byte compression) {
            this.compression = compression;
        }

        public int getSequenceId() {
            return sequenceId;
        }

        public void setSequenceId(int sequenceId) {
            this.sequenceId = sequenceId;
        }

        public byte getResCode() {
            return resCode;
        }

        public void setResCode(byte resCode) {
            this.resCode = resCode;
        }
    }

}
