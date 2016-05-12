package com.unascribed.lambdanetwork;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LambdaChannel {
	private final String channel;
	private final ImmutableMap<String, PacketSpec> packets;
	private final ImmutableList<String> ids;
	
	
	public LambdaChannel(String channel, List<PacketSpec> packets) {
		this.channel = channel;
		ImmutableMap.Builder<String, PacketSpec> builder = ImmutableMap.builder();
		ImmutableList.Builder<String> ids = ImmutableList.builder();
		for (PacketSpec ps : packets) {
			builder.put(ps.getIdentifier(), PacketSpec.immutableClone(ps));
			ids.add(ps.getIdentifier());
		}
		this.packets = builder.build();
		this.ids = ids.build();
		NetworkRegistry.INSTANCE.newEventDrivenChannel(channel).register(this);
	}


	public PacketSpec getPacketSpec(String packet) {
		if (!packets.containsKey(packet)) {
			throw new IllegalArgumentException("No such packet '"+packet+"' on channel '"+channel+"'");
		}
		return packets.get(packet);
	}
	
	
	public FMLProxyPacket getPacketFrom(PendingPacket pp) {
		if (pp.getData().size() != pp.getPacket().getData().size())
			throw new IllegalArgumentException("Missing data for keys "+Lists.newArrayList(pp.getPacket().getData().keySet()).removeAll(pp.getData().keySet()));
		PacketSpec spec = pp.getPacket();
		PacketBuffer payload = new PacketBuffer(Unpooled.buffer(spec.getMinimumSize()));
		payload.writeByte(ids.indexOf(spec.getIdentifier()));
		int booleanBits = spec.getAmountOfType(DataType.BOOLEAN);
		for (int i = 0; i < Math.ceil(booleanBits/8f); i++) {
			int by = 0;
			for (int j = i*8; j < Math.min(spec.getBooleanKeys().size(), i+8); j++) {
				String key = spec.getBooleanKeys().get(j);
				if (((Boolean)pp.getData().get(key))) {
					by |= (1 << j);
				}
			}
			payload.writeByte(by);
		}
		for (Map.Entry<String, DataType> en : spec.getData().entrySet()) {
			if (en.getValue().writer != null) {
				en.getValue().writer.accept(payload, pp.getData().get(en.getKey()));
			}
		}
		return new FMLProxyPacket(payload, channel);
	}


	@SubscribeEvent
	public void onServerCustomPacket(ServerCustomPacketEvent e) {
		ByteBuf payload = e.packet.payload();
		readPacket(e.side(), ((NetHandlerPlayServer)e.handler).playerEntity, payload);
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onClientCustomPacket(ClientCustomPacketEvent e) {
		ByteBuf payload = e.packet.payload();
		readPacket(e.side(), Minecraft.getMinecraft().thePlayer, payload);
	}
	
	
	private void readPacket(Side side, EntityPlayer p, ByteBuf payload) {
		int id = payload.readUnsignedByte();
		if (id >= ids.size()) {
			throw new IllegalArgumentException("Unknown lambda packet id "+id);
		}
		String strId = ids.get(id);
		PacketSpec spec = packets.get(strId);
		if (spec.getSide() != side) {
			throw new IllegalArgumentException("Packet '"+spec.getIdentifier()+"' is not valid for side "+side);
		}
		Token token = new Token(spec);
		int booleanBits = spec.getAmountOfType(DataType.BOOLEAN);
		for (int i = 0; i < Math.ceil(booleanBits/8f); i++) {
			int by = payload.readUnsignedByte();
			for (int j = i*8; j < Math.min(spec.getBooleanKeys().size(), i+8); j++) {
				token.putData(spec.getBooleanKeys().get(j), (by & (1 << (j-i))) != 0);
			}
		}
		for (Map.Entry<String, DataType> en : spec.getData().entrySet()) {
			if (en.getValue().reader != null) {
				token.putData(en.getKey(), en.getValue().reader.apply(payload));
			}
		}
		spec.getConsumer().accept(p, token);
	}



}
