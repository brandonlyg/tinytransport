package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-22
 */

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * TCP client 实现
 */
public class TcpClient {

    public static final AttributeKey<TcpClient> CLIENT = AttributeKey.valueOf("TcpClient");
    public static final AttributeKey<ScheduledFuture<?>> SCHEDULE = AttributeKey.valueOf("schedule");

    private volatile Channel channel;
    private EventExecutor executor;

    private Map<Integer, Promise<FMessage>> waitingPromises = new ConcurrentHashMap<>();

    private Map<String, Object> attrs = new ConcurrentHashMap<>();

    public TcpClient(Channel channel, EventExecutor executor){
        this.channel = channel;
        this.executor = executor;
    }

    public void setAttr(String name, String value){
        attrs.put(name, value);
    }
    public Object getAttr(String name){
        return attrs.get(name);
    }

    public boolean isConnected(){
        Channel chnl = channel;
        return null != chnl && chnl.isActive();
    }

    public boolean send(FMessage msg){
        Channel chnl = channel;
        if(null == chnl){
            return false;
        }

        if(!chnl.isActive()){
            return false;
        }
        chnl.writeAndFlush(msg);
        return true;
    }

    public Promise<FMessage> send(FMessage msg, TimeUnit timeUnit, long timeout){
        Channel chnl = channel;

        Promise<FMessage> res = new DefaultPromise<>(executor);
        if(null == chnl){
            res.tryFailure(new Exception("channel is null"));
            return res;
        }

        if(!chnl.isActive()){
            res.tryFailure(new Exception("chanell is inactive"));
            return res;
        }

        waitResponse(msg, res, timeUnit, timeout);
        chnl.writeAndFlush(msg);
        return res;
    }

    public void close(){
        Channel chnl = channel;
        if(null == chnl) return;
        channel = null;

        chnl.close();
    }

    protected void waitResponse(FMessage msg, Promise<FMessage> res, TimeUnit timeUnit, long timeout){
        ScheduledFuture<?> future = executor.schedule(new Runnable() {
            @Override
            public void run() {
                Promise<FMessage> wres = waitingPromises.remove(msg.getSequenceId());
                if(null == wres){
                    return;
                }
                if(wres.isDone()){
                    return;
                }
                wres.tryFailure(new Exception("response timeout"));
            }
        }, timeout, timeUnit);
        waitingPromises.put(msg.getSequenceId(), res);
    }

    public void onResponse(FMessage msg){
        Promise<FMessage> res = waitingPromises.remove(msg.getSequenceId());
        if(null == res){
            return;
        }
        if(null == msg){
            res.tryFailure(new Exception("response message is null"));
            return;
        }

        res.trySuccess(msg);
    }

}
