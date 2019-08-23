package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-19
 */

import com.alibaba.fastjson.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息类型
 */
public class FMessage {

    private static AtomicInteger seqId = new AtomicInteger(1);

    private Command cmd;
    private int     sequenceId;
    private int   resCode;
    private int   contentType;

    private Object content;

    public Command getCmd() {
        return cmd;
    }

    public void setCmd(Command cmd) {
        this.cmd = cmd;
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }

    public int getResCode() {
        return resCode;
    }

    public void setResCode(int resCode) {
        this.resCode = resCode;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public static int newSequenceId(){
        if(seqId.get() <= 0) seqId.set(1);

        return seqId.getAndIncrement();
    }

    @Override
    public String toString(){
        String str = "cmd:" + cmd.getValue() + " sequenceId:" + sequenceId + " resCode:"+resCode
                + " contentType:"+contentType;

        if(FMTraits.STRING == contentType){
            str = str + " content:'"+(String)content+"'";
        }else if(FMTraits.JSON == contentType){
            str = str + " content: '" + ((JSONObject)content).toJSONString() + "'";
        }

        return str;
    }



    public static FMessage buildRequest(int contentType, Object content){
        FMessage msg = new FMessage();
        msg.setCmd(Command.REQUEST);
        msg.setSequenceId(FMessage.newSequenceId());
        msg.setContentType(contentType);
        msg.setContent(content);

        return msg;
    }

    public static FMessage buildResponse(FMessage request, Object content){
        FMessage msg = new FMessage();
        msg.setCmd(Command.RESPONSE);
        msg.setSequenceId(request.getSequenceId());
        msg.setResCode(0);
        msg.setContentType(request.getContentType());
        msg.setContent(content);
        return msg;
    }

    public static FMessage buildPing(){
        FMessage ping = buildRequest(FMTraits.BYTES, null);
        ping.setCmd(Command.PING);
        return ping;
    }
    public static FMessage buildPong(FMessage ping){
        FMessage pong = buildResponse(ping, null);
        pong.setCmd(Command.PONG);

        return pong;
    }

    public static FMessage buildPush(int contentType, Object content){
        FMessage push = buildRequest(contentType, content);
        push.setCmd(Command.PUSH);
        return push;
    }



}
