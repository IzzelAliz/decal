package io.izzel.mods.decal.packet.login;

import io.izzel.mods.decal.DecalMod;
import io.izzel.mods.decal.bridge.HandshakeHandlerBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SDecalLoginManifestAck extends DecalLoginPacket {

    private final int[] handled;

    public C2SDecalLoginManifestAck(int[] handled) {
        this.handled = handled;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarIntArray(this.handled);
    }

    public static C2SDecalLoginManifestAck decode(FriendlyByteBuf buf) {
        return new C2SDecalLoginManifestAck(buf.readVarIntArray(Math.min(1 << 16, buf.readableBytes())));
    }

    public static void handle(HandshakeHandler handler, C2SDecalLoginManifestAck msg, Supplier<NetworkEvent.Context> ctx) {
        DecalMod.LOGGER.debug("Client cache hits: {} packets", msg.handled.length);
        ((HandshakeHandlerBridge) handler).decalClientHit(msg.handled);
        ctx.get().setPacketHandled(true);
    }
}
