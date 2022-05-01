package io.izzel.mods.decal.packet.login;

import io.izzel.mods.decal.packet.ClientHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CDecalLoginWrapper extends DecalLoginPacket {

    private final ResourceLocation channel;
    private final ByteBuf data;

    public S2CDecalLoginWrapper(ResourceLocation channel, ByteBuf data) {
        this.channel = channel;
        this.data = data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(channel);
        buf.writeVarInt(data.readableBytes());
        buf.writeBytes(data);
    }

    public static S2CDecalLoginWrapper decode(FriendlyByteBuf buf) {
        var channel = buf.readResourceLocation();
        var len = buf.readVarInt();
        return new S2CDecalLoginWrapper(channel, buf.readSlice(len));
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        return ClientHandler.handleLoginPacket(ctx.get(), channel, data, getLoginIndex());
    }
}
