package com.unascribed.lambdanetwork;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class LambdaNetworkBuilder {
	private Map<String, List<PacketSpec>> packets = Maps.newHashMap();
	protected String currentChannel;
	
	protected void addPacket(PacketSpec builder) {
		List<PacketSpec> li;
		if (packets.containsKey(currentChannel)) {
			li = packets.get(currentChannel);
		} else {
			li = Lists.newArrayList();
			packets.put(currentChannel, li);
		}
		li.add(builder);
	}
	
	/**
	 * Switches the channel packets are being assigned to. One
	 * LambdaNetwork can manage multiple channels.
	 * 
	 * @param channel
	 *            the channel to switch to. usually your modid, optionally
	 *            followed by a pipe ({@code |}) and a sub-channel name, e.g.
	 *            Ascribe, Ascribe|Handshake. usually you don't need
	 *            sub-channels.
	 */
	public LambdaNetworkBuilder channel(String channel) {
		this.currentChannel = channel;
		return this;
	}
	
	public PacketSpec packet(String identifier) {
		if (currentChannel == null) {
			throw new IllegalArgumentException("Cannot add packets without a channel!");
		}
		return PacketSpec.mutableBuilder(this, identifier);
	}
	
	public LambdaNetwork build() {
		return new LambdaNetwork(packets);
	}
}
