package com.unascribed.lambdanetwork;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class LambdaNetwork {
	private LambdaChannel soleChannel;
	private final ImmutableMap<String, LambdaChannel> channels;
	
	public LambdaNetwork(Map<String, List<PacketSpec>> packets) {
		
		ImmutableMap.Builder<String, LambdaChannel> builder = ImmutableMap.builder();
		for (Map.Entry<String, List<PacketSpec>> en : packets.entrySet()) {
			LambdaChannel channel = new LambdaChannel(en.getKey(), en.getValue());
			builder.put(en.getKey(), channel);
			if (packets.size() == 1) {
				soleChannel = channel;
			}
		}
		this.channels = builder.build();
	}
	
	public PendingPacket send() {
		return new PendingPacket(this);
	}
	
	public static LambdaNetworkBuilder builder() {
		return new LambdaNetworkBuilder();
	}

	public LambdaChannel getChannel(String channel) {
		if (!channels.containsKey(channel)) {
			throw new IllegalArgumentException("No such channel '"+channel+"'");
		}
		return channels.get(channel);
	}

	public int channelCount() {
		return channels.size();
	}
	
	public LambdaChannel getSoleChannel() {
		if (channels.size() > 1) throw new IllegalStateException("Cannot get the sole channel when there are multiple");
		return soleChannel;
	}
}
