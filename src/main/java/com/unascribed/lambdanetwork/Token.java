package com.unascribed.lambdanetwork;

import java.util.Map;

import com.google.common.collect.Maps;

import net.minecraft.nbt.NBTTagCompound;

public class Token {
	private PacketSpec packet;
	private Map<String, Object> data = Maps.newHashMap();

	public Token(PacketSpec spec) {
		this.packet = spec;
	}
	
	protected void putData(String key, Object value) {
		data.put(key, value);
	}
	
	public int getInt(String key) {
		if (!packet.getType(key).isValidForInteger()) {
			invalidType(key, "int");
		}
		return ((Number)data.get(key)).intValue();
	}
	
	public long getLong(String key) {
		if (!packet.getType(key).isValidForInteger()) {
			invalidType(key, "long");
		}
		return ((Number)data.get(key)).longValue();
	}
	
	public float getFloat(String key) {
		if (!packet.getType(key).isValidForInteger() && !packet.getType(key).isValidForFloating()) {
			invalidType(key, "float");
		}
		return ((Number)data.get(key)).floatValue();
	}
	
	public double getDouble(String key) {
		if (!packet.getType(key).isValidForInteger() && !packet.getType(key).isValidForFloating()) {
			invalidType(key, "double");
		}
		return ((Number)data.get(key)).doubleValue();
	}
	
	public boolean getBoolean(String key) {
		if (!packet.getType(key).isValidForBoolean()) {
			invalidType(key, "boolean");
		}
		return ((Boolean)data.get(key));
	}
	
	public String getString(String key) {
		if (!packet.getType(key).isValidForString()) {
			invalidType(key, "String");
		}
		return String.valueOf(data.get(key));
	}
	
	public byte[] getData(String key) {
		if (!packet.getType(key).isValidForData()) {
			invalidType(key, "byte[]");
		}
		return ((byte[])data.get(key));
	}
	
	public NBTTagCompound getNBT(String key) {
		if (!packet.getType(key).isValidForNBT()) {
			invalidType(key, "NBTTagCompound");
		}
		return ((NBTTagCompound)data.get(key));
	}
	
	private void invalidType(String key, String type) {
		throw new IllegalArgumentException("Type "+type+" is not valid for '"+key+"' (data type "+packet.getType(key)+") in packet '"+packet.getIdentifier()+"'");
	}
}
