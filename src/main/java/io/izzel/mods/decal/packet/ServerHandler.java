package io.izzel.mods.decal.packet;

import com.google.common.hash.Hashing;
import io.izzel.mods.decal.DecalHandles;
import io.izzel.mods.decal.DecalMod;
import io.izzel.mods.decal.bridge.HandshakeHandlerBridge;
import io.izzel.mods.decal.packet.login.S2CDecalLoginManifest;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.LoginWrapper;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;

import java.util.List;

public class ServerHandler {

    public static <T extends NetworkEvent> void onClientMissing(T event) {
        var context = event.getSource().get();
        if (context.getDirection() == NetworkDirection.LOGIN_TO_SERVER
            && event.getLoginIndex() == DecalChannel.getManifestId() && event.getPayload() == null) {
            DecalMod.LOGGER.debug("Client decal missing, revert to forge handshake");
            ((HandshakeHandlerBridge) DecalHandles.getHandshakeHandler(context.getNetworkManager())).decalClientMissing();
            context.setPacketHandled(true);
        }
    }

    public static void sendManifest(List<NetworkRegistry.LoginPayload> payloads, LoginWrapper loginWrapper, Connection connection) {
        var tags = payloads.stream().map(payload -> {
            var head = new FriendlyByteBuf(Unpooled.buffer());
            head.writeResourceLocation(payload.getChannelName());
            head.writeVarInt(payload.getData().readableBytes());
            var hashCode = Hashing.sha256().hashBytes(Unpooled.wrappedBuffer(head, payload.getData()).nioBuffer());
            return hashCode.asBytes();
        }).toArray(byte[][]::new);
        var target = new FriendlyByteBuf(Unpooled.buffer());
        var message = new S2CDecalLoginManifest(tags);
        message.setLoginIndex(DecalChannel.getManifestId());
        DecalChannel.channel().encodeMessage(message, target);
        DecalHandles.sendServerToClientLoginPacket(loginWrapper, DecalChannel.getChannelName(), target, DecalChannel.getManifestId(), connection);
    }
}
