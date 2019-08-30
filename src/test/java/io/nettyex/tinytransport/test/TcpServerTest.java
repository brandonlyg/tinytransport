package io.nettyex.tinytransport.test;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.nettyex.tinytransport.FMTraits;
import io.nettyex.tinytransport.FMessage;
import io.nettyex.tinytransport.FMessageHandler;
import io.nettyex.tinytransport.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class TcpServerTest {
    private static final Logger log = LoggerFactory.getLogger(TcpServerTest.class);

    public static void main(String[] args) throws Exception{
        MyServer server = new MyServer();

        server.setBacklog(512);
        server.setWorkerThreads(2);
        server.setReadTimeout(30);
        server.setLocalAddress(new InetSocketAddress("127.0.0.1", 9312));

        server.addFMessageTrait(FMTraits.FMTString);
        server.addFMessageTrait(FMTraits.FMTJson);
        server.addFMessageTrait(new FMTraits.FMTraitProtobuf(
                PBContent.ProtoContent.newBuilder().getDefaultInstanceForType())
        );

        server.start();



    }

    private static ServerHandler serverHandler = new ServerHandler();

    private static class MyServer extends TcpServer {
        @Override
        protected void doInitChannel(SocketChannel ch) throws Exception {
            super.doInitChannel(ch);
            ch.pipeline().addLast(serverHandler);
        }
    }

    @ChannelHandler.Sharable
    private static class ServerHandler extends FMessageHandler {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            log.info("channel connected: {}", ctx.channel().remoteAddress().toString());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            log.info("channel closed: {}", ctx.channel().remoteAddress().toString());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("channel {}", ctx.channel().remoteAddress().toString());
            log.error("", cause);
        }

        @Override
        protected void onPing(ChannelHandlerContext ctx, FMessage msg) throws Exception {
            super.onPing(ctx, msg);
            log.info("onPing");
        }

        @Override
        protected void onRequest(ChannelHandlerContext ctx, FMessage msg) throws Exception {
            log.info("onRequest: '{}'", msg.toString());
            FMessage res = null;
            if(FMTraits.STRING == msg.getContentType()){
                String content = (String)msg.getContent();
                content = content + "-response";
                res = FMessage.buildResponse(msg, content);
            }else if(FMTraits.JSON == msg.getContentType()){
                JSONObject json = (JSONObject)msg.getContent();
                json.put("response", 1);
                res = FMessage.buildResponse(msg, json);
            }else if(FMTraits.PROTOBUF == msg.getContentType()){
                PBContent.ProtoContent pbContent = (PBContent.ProtoContent)msg.getContent();
                if(1 == pbContent.getCmd()){
                    PBContent.GetPersonInfoReq pbReq = PBContent.GetPersonInfoReq.parseFrom(pbContent.getData());
                    log.info("pbReq: '{}'", pbReq.toString());
                }

                PBContent.PersonInfoRes pbRes = PBContent.PersonInfoRes.newBuilder()
                        .setArea(2)
                        .setPersonId("212121")
                        .setName("Tom")
                        .setInterest("music")
                        .build();
                pbContent = PBContent.ProtoContent.newBuilder()
                        .setCmd(1)
                        .setErrCode(0).setErrMsg("OK")
                        .setData(pbRes.toByteString())
                        .build();
                res = FMessage.buildResponse(msg, pbContent);
            }

            if(null != res){
                ctx.channel().writeAndFlush(res);
            }
        }

        @Override
        protected void onPush(ChannelHandlerContext ctx, FMessage msg) throws Exception {
            log.info("onPush: '{}'", msg.toString());
        }


    }
}
