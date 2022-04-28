package io.izzel.mods.decal.packet;

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
        buf.writeBytes(data);
    }

    public static S2CDecalLoginWrapper decode(FriendlyByteBuf buf) {
        return new S2CDecalLoginWrapper(buf.readResourceLocation(), buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {

        return false;
    }
}
