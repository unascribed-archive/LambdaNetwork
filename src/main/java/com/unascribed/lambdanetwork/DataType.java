package com.unascribed.lambdanetwork;

import com.google.common.collect.ImmutableList;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public enum DataType {
	/** variable-size protobuf integer, maximum 5 bytes */
	VARINT(Integer.class,
			(ByteBuf buf, Integer i) ->
				ByteBufUtils.writeVarInt(buf, i, 5),
			(ByteBuf buf) ->
				ByteBufUtils.readVarInt(buf, 5),
			1,
			ExternalType.INTEGER
		),
	
	/**
	 * variable-size protobuf zigzag integer, maximum 5 bytes. negative
	 * numbers take up less space than in regular varints
	 */
	VARINT_ZIGZAG(Integer.class,
			(ByteBuf buf, Integer i) -> ByteBufUtils.writeVarInt(buf, (i << 1) ^ (i >> 31), 5),
			(ByteBuf buf) -> {
				int i = ByteBufUtils.readVarInt(buf, 5);
				return (i >>> 1) ^ -(i & 1);
			},
			1,
			ExternalType.INTEGER
		),
	
	/** unsigned 8-bit (closest Java type: short) */
	UINT_8(ByteBuf::writeByte, ByteBuf::readUnsignedByte, 1, ExternalType.INTEGER),
	/** signed 8-bit (Java type: byte) */
	INT_8(ByteBuf::writeByte, ByteBuf::readByte, 1, ExternalType.INTEGER),
	
	/** unsigned 16-bit (Java type: char, closest Java numeric type: int) */
	UINT_16(Integer.class, ByteBuf::writeShort, ByteBuf::readUnsignedShort, 2, ExternalType.INTEGER),
	/** signed 16-bit (Java type: short) */
	INT_16(ByteBuf::writeShort, ByteBuf::readShort, 2, ExternalType.INTEGER),
	
	/** unsigned 24-bit (closest Java type: int) */
	UINT_24(Integer.class, ByteBuf::writeMedium, ByteBuf::readUnsignedMedium, 3, ExternalType.INTEGER),
	/** signed 24-bit (closest Java type: int) */
	INT_24(Integer.class, ByteBuf::writeMedium, ByteBuf::readMedium, 3, ExternalType.INTEGER),
	
	/** unsigned 32-bit (closest Java type: long) */
	UINT_32(ByteBuf::writeInt, ByteBuf::readUnsignedInt, 4, ExternalType.INTEGER),
	/** signed 32-bit (Java type: int) */
	INT_32(Integer.class, ByteBuf::writeInt, ByteBuf::readInt, 4, ExternalType.INTEGER),
	
	/**
	 * signed 64-bit (Java type: long) - this is the largest integer
	 * representable with Java primitives. if you need even more range, use
	 * {@link ARBITRARY} and marshal to/from BigInteger.
	 */
	INT_64(Long.class, ByteBuf::writeLong, ByteBuf::readLong, 8, ExternalType.INTEGER),
	
	/**
	 * true/false value - multiple booleans in one packet will be combined
	 * into a bitfield
	 */
	BOOLEAN(1, ExternalType.BOOLEAN), // handled specially for bitfield optimization
	
	/** 32-bit floating point value (Java type: float) */
	FLOAT_32(Float.class, ByteBuf::writeFloat, ByteBuf::readFloat, 4, ExternalType.INTEGER, ExternalType.FLOATING),
	/**
	 * 64-bit floating point value (Java type: double) - this is the largest
	 * floating point representable with Java primitives. if you need even more
	 * range or precision, use {@link ARBITRARY} and marshal to/from BigDecimal.
	 */
	FLOAT_64(Double.class, ByteBuf::writeDouble, ByteBuf::readDouble, 8, ExternalType.INTEGER, ExternalType.FLOATING),
	
	
	/** UTF-8 string (varint length-prefixed) */
	STRING(String.class, ByteBufUtils::writeUTF8String, ByteBufUtils::readUTF8String, 1, ExternalType.STRING),
	/** arbitrary data, i.e. a byte array (varint length-prefixed) */
	ARBITRARY(byte[].class,
			(ByteBuf buf, byte[] arr) -> {
				ByteBufUtils.writeVarInt(buf, arr.length, 5);
				buf.writeBytes(arr);
			},
			(ByteBuf buf) -> {
				byte[] arr = new byte[ByteBufUtils.readVarInt(buf, 5)];
				buf.readBytes(arr);
				return arr;
			},
			1,
			ExternalType.DATA
		),
	/** arbitrary structured NBT data */
	NBT_COMPOUND(NBTTagCompound.class, ByteBufUtils::writeTag, ByteBufUtils::readTag, 5, ExternalType.NBT),
	;

	/** alias for INT_8 */
	public static final DataType BYTE = INT_8;
	/** alias for INT_16 */
	public static final DataType SHORT = INT_16;
	/** alias for INT_32 */
	public static final DataType INT = INT_32;
	/** alias for INT_64 */
	public static final DataType LONG = INT_64;
	/** alias for FLOAT_32 */
	public static final DataType FLOAT = FLOAT_32;
	/** alias for FLOAT_64 */
	public static final DataType DOUBLE = FLOAT_64;
	/** alias for ARBITRARY */
	public static final DataType BYTE_ARRAY = ARBITRARY;
	
	private enum ExternalType {
		INTEGER,
		FLOATING,
		BOOLEAN,
		STRING,
		NBT,
		DATA
	}
	
	public final BiConsumer<ByteBuf, Object> writer;
	public final Function<ByteBuf, ?> reader;
	
	public final int minimumSize;
	
	private final ImmutableList<ExternalType> validTypes;
	
	private DataType(int minimumSize, ExternalType... validTypes) {
		writer = null;
		reader = null;
		this.validTypes = ImmutableList.copyOf(validTypes);
		this.minimumSize = minimumSize;
	}
	
	private <T> DataType(Class<T> clazz, BiConsumer<ByteBuf, T> writer, Function<ByteBuf, T> reader, int minimumSize, ExternalType... validTypes) {
		this.writer = (BiConsumer<ByteBuf, Object>) writer;
		this.reader = reader;
		this.validTypes = ImmutableList.copyOf(validTypes);
		this.minimumSize = minimumSize;
	}
	
	private <T> DataType(BiIntConsumer<ByteBuf> writer, Function<ByteBuf, T> reader, int minimumSize, ExternalType... validTypes) {
		this.writer = (buf, i) -> writer.accept(buf, (int)i);
		this.reader = reader;
		this.validTypes = ImmutableList.copyOf(validTypes);
		this.minimumSize = minimumSize;
	}
	
	private <T> DataType(BiLongConsumer<ByteBuf> writer, Function<ByteBuf, T> reader, int minimumSize, ExternalType... validTypes) {
		this.writer = (buf, i) -> writer.accept(buf, (long)i);
		this.reader = reader;
		this.validTypes = ImmutableList.copyOf(validTypes);
		this.minimumSize = minimumSize;
	}
	
	private <T> DataType(BiDoubleConsumer<ByteBuf> writer, Function<ByteBuf, T> reader, int minimumSize, ExternalType... validTypes) {
		this.writer = (buf, i) -> writer.accept(buf, (double)i);
		this.reader = reader;
		this.validTypes = ImmutableList.copyOf(validTypes);
		this.minimumSize = minimumSize;
	}
	
	
	public boolean isValidForInteger() {
		return validTypes.contains(ExternalType.INTEGER);
	}
	
	public boolean isValidForFloating() {
		return validTypes.contains(ExternalType.FLOATING); 
	}
	
	public boolean isValidForBoolean() {
		return validTypes.contains(ExternalType.BOOLEAN); 
	}
	
	public boolean isValidForString() {
		return validTypes.contains(ExternalType.STRING); 
	}
	
	public boolean isValidForNBT() {
		return validTypes.contains(ExternalType.NBT); 
	}
	
	public boolean isValidForData() {
		return validTypes.contains(ExternalType.DATA); 
	}
}