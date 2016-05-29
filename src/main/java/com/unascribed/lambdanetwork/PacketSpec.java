package com.unascribed.lambdanetwork;

import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;

public final class PacketSpec {
	private final LambdaNetworkBuilder parent;
	private final String identifier;
	public final Map<String, DataType> data;
	private final Multiset<DataType> types;
	private final List<String> booleanKeys;
	
	/*
	 * if we wanted to be obsessive about immutability, we could have separate
	 * final fields for immutable clones. but at that point, we're protecting
	 * against reflection, and reflection can just strip the 'final' attribute.
	 * it's a pointless endeavour.
	 */
	private BiConsumer<EntityPlayer, Token> consumer;
	private Side side = null;
	private int minimumSize = 1; // discriminator is 1 byte
	
	private PacketSpec(LambdaNetworkBuilder parent, String identifier) {
		this.data = Maps.newLinkedHashMap();
		this.types = EnumMultiset.create(DataType.class);
		this.parent = parent;
		this.identifier = identifier;
		this.booleanKeys = Lists.newArrayList();
	}
	
	private PacketSpec(PacketSpec in) {
		this.data = ImmutableMap.copyOf(in.data);
		this.types = ImmutableMultiset.copyOf(in.types);
		this.parent = null; // parent is only needed for mutable versions
		this.identifier = in.identifier;
		this.consumer = in.consumer;
		this.side = in.side;
		this.booleanKeys = ImmutableList.copyOf(in.booleanKeys);
		this.minimumSize = in.minimumSize;
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	public Side getSide() {
		return side;
	}
	
	public DataType getType(String key) {
		if (!data.containsKey(key)) {
			throw new IllegalArgumentException("No such data '"+key+"'");
		}
		return data.get(key);
	}
	
	public BiConsumer<EntityPlayer, Token> getConsumer() {
		return consumer;
	}
	
	public int getAmountOfType(DataType type) {
		return types.count(type);
	}
	
	public List<String> getBooleanKeys() {
		return booleanKeys;
	}
	
	public Map<String, DataType> getData() {
		return data;
	}
	
	public int getMinimumSize() {
		return minimumSize;
	}
	
	
	public static PacketSpec mutableBuilder(LambdaNetworkBuilder parent, String identifier) {
		return new PacketSpec(parent, identifier);
	}
	
	public static PacketSpec immutableClone(PacketSpec spec) {
		return new PacketSpec(spec);
	}
	
	
	public PacketSpec with(DataType type, String name) {
		if (parent == null) illegalStateImmutableClone();
		if (data.containsKey(name))
			illegalArgument("defined multiple data entries with the same name");
		data.put(name, type);
		types.add(type);
		if (type == DataType.BOOLEAN) {
			booleanKeys.add(name);
		}
		minimumSize += type.minimumSize;
		return this;
	}
	
	/**
	 * Sets which side this packet is "bound" to. This is the side on which
	 * the handler will be run.
	 * 
	 * @param side the side to bind this packet to
	 */
	public PacketSpec boundTo(Side side) {
		if (parent == null) illegalStateImmutableClone();
		Preconditions.checkNotNull(side);
		this.side = side;
		return this;
	}

	@Deprecated
	public LambdaNetworkBuilder handledBy(BiConsumer<EntityPlayer, Token> consumer) {
		return handledOnMainThreadBy(consumer);
	}

	public LambdaNetworkBuilder handledOnMainThreadBy(BiConsumer<EntityPlayer, Token> consumer) {
		if (parent == null) illegalStateImmutableClone();
		checkNull(side, "isn't bound to any side");
		checkNull(consumer, "can't have a null handler");
		this.consumer = consumer;
		parent.addPacket(this);
		return parent;
	}

	private void illegalStateImmutableClone() {
		throw new IllegalStateException("Cannot use builder methods on an immutable clone");
	}
	
	private void checkNull(Object o, String msg) {
		if (o == null) illegalArgument(msg);
	}

	private void illegalArgument(String msg) {
		throw new IllegalArgumentException("Packet '"+identifier+"' owned by '"+parent.currentChannel+"' "+msg+"!");
	}

}
