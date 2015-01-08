package com.linda.framework.rpc.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;

import com.linda.framework.rpc.RpcObject;
import com.linda.framework.rpc.net.AbstractRpcConnector;
import com.linda.framework.rpc.net.AbstractRpcWriter;

public class RpcNettyConnector extends AbstractRpcConnector{
	
	private AbstractChannel channel;
	
	private NioEventLoopGroup eventLoopGroup = null;
	
	private Logger logger = Logger.getLogger(RpcNettyConnector.class);
	
	public RpcNettyConnector(){
		this(null);
	}
	
	public RpcNettyConnector(AbstractChannel channel){
		super(null);
		this.channel = channel;
		if(this.channel!=null){
			this.setAddress(channel);
		}
	}
	
	private RpcNettyConnector(AbstractRpcWriter rpcWriter,String str) {
		super(rpcWriter);
	}
	
	private void setAddress(AbstractChannel channel){
		InetSocketAddress address = (InetSocketAddress)channel.remoteAddress();
		this.setHost(address.getHostName());
		this.setPort(address.getPort());
	}

	@Override
	public void startService() {
		if(this.channel==null){
			eventLoopGroup = new NioEventLoopGroup(3);
			Bootstrap boot = NettyUtils.buildBootStrap(eventLoopGroup);
			boot.remoteAddress(host, port);
			try {
				ChannelFuture f = boot.connect().sync();
				f.await();
				this.channel = (AbstractChannel)f.channel();
			} catch (InterruptedException e) {
				logger.info("interrupted start to exist");
				this.stopService();
			}
		}
	}

	@Override
	public void stopService() {
		this.executor.shutdown();
		this.rpcContext.clear();
		this.sendQueueCache.clear();
		this.callListeners.clear();
		channel.close();
		if(eventLoopGroup!=null){
			try {
				eventLoopGroup.shutdownGracefully().sync();
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public void handleNetException(Exception e) {
		logger.error(this.getHost()+":"+this.getPort()+" "+e+"     connector start to shutdown");
		this.stopService();
	}

	@Override
	public boolean sendRpcObject(RpcObject rpc, int timeout) {
		ChannelFuture future = channel.writeAndFlush(rpc);
		try {
			future.await(timeout);
			logger.info("sended:"+future.isSuccess());
			return future.isSuccess();
		} catch (InterruptedException e) {
			return false;
		}
	}
}