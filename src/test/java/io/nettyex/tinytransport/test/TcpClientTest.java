package io.nettyex.tinytransport.test;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.nettyex.tinytransport.FMTraits;
import io.nettyex.tinytransport.FMessage;
import io.nettyex.tinytransport.TcpClient;
import io.nettyex.tinytransport.TcpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class TcpClientTest {
    private static final Logger log = LoggerFactory.getLogger(TcpClientTest.class);

    public static void main(String[] args) throws Exception{
        MyConnector connector = new MyConnector(2, 30);

        connector.addFMessageTrait(FMTraits.FMTString);
        connector.addFMessageTrait(FMTraits.FMTJson);
        connector.addFMessageTrait(new FMTraits.FMTraitProtobuf(
                PBContent.ProtoContent.newBuilder().getDefaultInstanceForType())
        );


        TcpClient client = connector.connect(new InetSocketAddress("127.0.0.1", 9312));
        FMessage req = null;
        FMessage push = null;

        //test String
        req = FMessage.buildRequest(FMTraits.STRING, "this is test request");
        client.send(req, TimeUnit.SECONDS, 5).addListener(new GenericFutureListener<Future<? super FMessage>>() {
            @Override
            public void operationComplete(Future<? super FMessage> future) throws Exception {
                if(!future.isSuccess()){
                    log.error("", future.cause());
                    return;
                }
                FMessage res = (FMessage)future.get();
                log.info("onResponse: '{}'", res.toString());
            }
        });
        push = FMessage.buildPush(FMTraits.STRING, "this is test push");
        client.send(push);

        //test json
        JSONObject json = new JSONObject();
        json.put("name", "test");
        json.put("age", 21);
        json.put("firends", Arrays.asList("aaa", "bbb", "ccc"));
        json.put("type", "request");
        req = FMessage.buildRequest(FMTraits.JSON, json);
        client.send(req, TimeUnit.SECONDS, 5).addListener(new GenericFutureListener<Future<? super FMessage>>() {
            @Override
            public void operationComplete(Future<? super FMessage> future) throws Exception {
                if(!future.isSuccess()){
                    log.error("", future.cause());
                    return;
                }
                FMessage res = (FMessage)future.get();
                log.info("onResponse: '{}'", res.toString());
            }
        });

        json = (JSONObject)json.clone();
        json.put("type", "push");
        push = FMessage.buildPush(FMTraits.JSON, json);
        client.send(push);


        //test protoBuf
        PBContent.GetPersonInfoReq pbReq = PBContent.GetPersonInfoReq.newBuilder()
                .setArea(111)
                .setPersonId("123445")
                .build();
        PBContent.ProtoContent pbContent = PBContent.ProtoContent.newBuilder()
                .setCmd(1)
                .setData(pbReq.toByteString())
                .build();

        req = FMessage.buildRequest(FMTraits.PROTOBUF, pbContent);
        client.send(req, TimeUnit.SECONDS, 5).addListener(new GenericFutureListener<Future<? super FMessage>>() {
            @Override
            public void operationComplete(Future<? super FMessage> future) throws Exception {
                if(!future.isSuccess()){
                    log.error("", future.cause());
                    return;
                }
                FMessage res = (FMessage)future.get();

                PBContent.ProtoContent pbRes = (PBContent.ProtoContent)res.getContent();
                log.info("on PB response. cmd:{} errCode:{} errMsg:{}", pbRes.getCmd(), pbRes.getErrCode(), pbRes.getErrMsg());
                if(1 == pbRes.getCmd()){
                    PBContent.PersonInfoRes pinfo = PBContent.PersonInfoRes.parseFrom(pbRes.getData());
                    log.info("pbRes personInfo:'{}'", pinfo.toString());
                }

            }
        });

    }

    private static class MyConnector extends TcpConnector {

        public MyConnector(int workerThreads, int readTimeout) {
            super(workerThreads, readTimeout);
            clientHandler = new MyClientHandler();
        }

        @ChannelHandler.Sharable
        protected class MyClientHandler extends ClientHandler{

            @Override
            protected void onPong(ChannelHandlerContext ctx, FMessage msg) throws Exception {
                log.info("onPong");
            }

            @Override
            protected void onPush(ChannelHandlerContext ctx, FMessage msg) throws Exception {
                super.onPush(ctx, msg);
            }
        }
    }

}
