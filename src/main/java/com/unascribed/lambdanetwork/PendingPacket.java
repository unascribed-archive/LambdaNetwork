package com.unascribed.lambdanetwork;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.Maps;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PendingPacket {
	private LambdaNetwork owner;

	private LambdaChannel channel;

	private PacketSpec packet;

	private String packetId;

	private Map<String, Object> data = Maps.newHashMap();

	public PendingPacket(LambdaNetwork owner) {
		this.owner = owner;
		if (owner.channelCount() == 1) {
			channel = owner.getSoleChannel();
		}
	}

	public PacketSpec getPacket() {
		return packet;
	}

	public Map<String, ?> getData() {
		return data;
	}



	public PendingPacket packet(String packet) {
		if (channel != null) {
			this.packet = channel.getPacketSpec(packet);
		}
		this.packetId = packet;
		return this;
	}

	public PendingPacket onChannel(String channel) {
		this.channel = owner.getChannel(channel);
		if (packetId != null) {
			this.packet = this.channel.getPacketSpec(packetId);
		}
		return this;
	}


	public PendingPacket with(String key, int value) {
		checkHasPacket();
		if (!packet.getType(key).isValidForInteger()) {
			invalidType(key, "int");
		}
		data.put(key, value);
		return this;
	}

	public PendingPacket with(String key, long value) {
		checkHasPacket();
		if (!packet.getType(key).isValidForInteger()) {
			invalidType(key, "long");
		}
		data.put(key, value);
		return this;
	}

	public PendingPacket with(String key, boolean value) {
		checkHasPacket();
		if (!packet.getType(key).isValidForBoolean()) {
			invalidType(key, "boolean");
		}
		data.put(key, value);
		return this;
	}

	public PendingPacket with(String key, float value) {
		checkHasPacket();
		if (!packet.getType(key).isValidForFloating()) {
			invalidType(key, "float");
		}
		data.put(key, value);
		return this;
	}

	public PendingPacket with(String key, double value) {
		checkHasPacket();
		if (!packet.getType(key).isValidForFloating()) {
			invalidType(key, "double");
		}
		data.put(key, value);
		return this;
	}

	public PendingPacket with(String key, String value) {
		checkHasPacket();
		if (!packet.getType(key).isValidForString()) {
			invalidType(key, "String");
		}
		if (value == null) {
			throw new IllegalArgumentException("Cannot use null for String value");
		}
		data.put(key, value);
		return this;
	}

	public PendingPacket with(String key, NBTTagCompound value) {
		checkHasPacket();
		if (!packet.getType(key).isValidForNBT()) {
			invalidType(key, "NBTTagCompound");
		}
		if (value == null) {
			throw new IllegalArgumentException("Cannot use null for NBTTagCompound value");
		}
		data.put(key, value);
		return this;
	}

	public PendingPacket with(String key, byte[] value) {
		checkHasPacket();
		if (!packet.getType(key).isValidForData()) {
			invalidType(key, "byte[]");
		}
		if (value == null) {
			throw new IllegalArgumentException("Cannot use null for byte[] value");
		}
		data.put(key, value);
		return this;
	}

	private void invalidType(String key, String type) {
		throw new IllegalArgumentException("Type "+type+" is not valid for '"+key+"' (data type "+packet.getType(key)+") in packet '"+packet.getIdentifier()+"'");
	}


	private void checkHasPacket() {
		if (packet == null) {
			throw new IllegalArgumentException("Must specify packet before data");
		}
	}

	/**
	 * For use on the server-side. Sends this packet to the given player.
	 */
	public void to(EntityPlayer player) {
		if (packet.getSide().isServer()) wrongSide();
		if (player instanceof EntityPlayerMP) {
			((EntityPlayerMP)player).playerNetServerHandler.sendPacket(toClientboundVanillaPacket());
		}
	}


	/**
	 * For use on the server-side. Sends this packet to every player that is
	 * within the given radius of the given position. <i>It is almost always
	 * better to use {@link #toAllWatching(Entity)}, this is only useful for
	 * certain special cases.</i>
	 */
	public void toAllAround(World world, Entity entity, double radius) {
		toAllAround(world, entity.posX, entity.posY, entity.posZ, radius);
	}

	/**
	 * For use on the server-side. Sends this packet to every player that is
	 * within the given radius of the given position. <i>It is almost always
	 * better to use {@link #toAllWatching(World, BlockPos)}, this is only
	 * useful for certain special cases.</i>
	 */
	public void toAllAround(World world, Vec3 pos, double radius) {
		toAllAround(world, pos.xCoord, pos.yCoord, pos.zCoord, radius);
	}

	/**
	 * For use on the server-side. Sends this packet to every player that is
	 * within the given radius of the given position.
	 */
	public void toAllAround(World world, double x, double y, double z, double radius) {
		if (packet.getSide().isServer()) wrongSide();
		double sq = radius*radius;
		Packet packet = toClientboundVanillaPacket();
		playerMPsOnly(world.playerEntities).forEach(ep -> {
				if (ep.getDistanceSq(x, y, z) <= sq) {
					ep.playerNetServerHandler.sendPacket(packet);
				}
			}
		);
	}

	/**
	 * For use on the server-side. Sends this packet to every player that can
	 * see the given block.
	 */
	public void toAllWatching(World world, int x, int z) {
		if (packet.getSide().isServer()) wrongSide();
		if (world instanceof WorldServer) {
			WorldServer srv = (WorldServer)world;
			Chunk c = srv.getChunkFromBlockCoords(x, z);
			if (srv.getPlayerManager().func_152621_a(c.xPosition, c.zPosition)) {
				Packet packet = toClientboundVanillaPacket();
				playerMPsOnly(world.playerEntities).forEach(ep -> {
					if (srv.getPlayerManager().isPlayerWatchingChunk(ep, c.xPosition, c.zPosition)) {
						ep.playerNetServerHandler.sendPacket(packet);
					}
				});
			}
		}
	}

	/**
	 * For use on the server-side. Sends this packet to every player that can
	 * see the given tile entity.
	 */
	public void toAllWatching(TileEntity te) {
		toAllWatching(te.getWorldObj(), te.xCoord, te.zCoord);
	}


	/**
	 * For use on the server-side. Sends this packet to every player that can
	 * see the given entity.
	 */
	public void toAllWatching(Entity e) {
		if (packet.getSide().isServer()) wrongSide();
		if (e.worldObj instanceof WorldServer) {
			WorldServer srv = (WorldServer)e.worldObj;
			srv.getEntityTracker().func_151247_a(e, toClientboundVanillaPacket());
		}
	}

	/**
	 * For use on the server-side. Sends this packet to every player in the
	 * given world.
	 */
	public void toAllIn(World world) {
		if (packet.getSide().isServer()) wrongSide();
		Packet packet = toClientboundVanillaPacket();
		playerMPsOnly(world.playerEntities).forEach(ep -> {
            ep.playerNetServerHandler.sendPacket(packet);
		});
	}


	/**
	 * For use on the server-side. Sends this packet to every player currently
	 * connected to the server. Use sparingly, you almost never need to send
	 * a packet to everyone.
	 */
	public void toEveryone() {
		if (packet.getSide().isServer()) wrongSide();
		Packet packet = toClientboundVanillaPacket();

		playerMPsOnly(MinecraftServer.getServer().getConfigurationManager().playerEntityList).forEach(ep -> {
            ep.playerNetServerHandler.sendPacket(packet);
		});
	}

	/**
	 * For use on the <i>client</i>-side. This is the only valid method for use
	 * on the client side.
	 */
	@SideOnly(Side.CLIENT)
	public void toServer() {
		if (packet.getSide().isClient()) wrongSide();
		Minecraft.getMinecraft().getNetHandler().addToSendQueue(toServerboundVanillaPacket());
	}

	private void wrongSide() {
		throw new IllegalStateException("Packet '"+packet.getIdentifier()+"' cannot be sent from side "+packet.getSide());
	}

	/**
	 * Mainly intended for internal use, but can be useful for more complex
	 * use cases.
	 */
	public Packet toServerboundVanillaPacket() {
		return channel.getPacketFrom(this).toC17Packet();
	}

	/**
	 * Mainly intended for internal use, but can be useful for more complex
	 * use cases.
	 */
	public Packet toClientboundVanillaPacket() {
        return channel.getPacketFrom(this).toS3FPacket();
	}

    static Stream<EntityPlayerMP> playerMPsOnly(Collection i) {
		return i.stream().filter(x -> x instanceof EntityPlayerMP);
    }
}
