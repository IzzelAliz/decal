package io.izzel.mods.decal.mixin;

import io.izzel.mods.decal.bridge.HandshakeHandlerBridge;
import io.izzel.mods.decal.packet.DecalChannel;
import io.izzel.mods.decal.packet.ServerHandler;
import io.izzel.mods.decal.packet.login.S2CDecalLoginWrapper;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.LoginWrapper;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = HandshakeHandler.class, remap = false)
public class HandshakeHandlerMixin implements HandshakeHandlerBridge {

    // @formatter:off
    @Shadow private List<NetworkRegistry.LoginPayload> messageList;
    @Shadow private List<Integer> sentMessages;
    @Shadow @Final private static LoginWrapper loginWrapper;
    @Shadow @Final private Connection manager;
    @Shadow private int packetPosition;
    // @formatter:on

    @Unique private List<NetworkRegistry.LoginPayload> pendingList;
    @Unique private boolean manifestSend = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void decal$hijackLogin(Connection networkManager, NetworkDirection side, CallbackInfo ci) {
        // a remote dedicated connection
        if (side == NetworkDirection.LOGIN_TO_CLIENT && !manager.isMemoryConnection()) {
            if (!this.messageList.isEmpty()) {
                this.pendingList = this.messageList;
                this.messageList = new ArrayList<>();
                this.sentMessages.add(DecalChannel.getManifestId());
                this.manifestSend = true;
            }
        }
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void decal$sendManifest(CallbackInfoReturnable<Boolean> cir) {
        // skip sent(null) packets
        while (this.packetPosition < this.messageList.size()) {
            if (this.messageList.get(this.packetPosition) == null) {
                this.packetPosition++;
            } else {
                break;
            }
        }
        if (manifestSend) {
            ServerHandler.sendManifest(this.pendingList, loginWrapper, this.manager);
            manifestSend = false;
        }
    }

    @Override
    public void decalClientMissing() {
        this.messageList = this.pendingList;
        this.pendingList = null;
        this.sentMessages.clear();
    }

    @Override
    public void decalClientHit(int[] packets) {
        for (int packet : packets) {
            if (this.pendingList.set(packet, null).needsResponse()) {
                this.sentMessages.add(packet);
            }
        }
        for (var payload : this.pendingList) {
            if (payload != null) {
                var wrapper = new S2CDecalLoginWrapper(payload.getChannelName(), payload.getData());
                var buffer = new FriendlyByteBuf(Unpooled.buffer());
                DecalChannel.channel().encodeMessage(wrapper, buffer);
                this.messageList.add(new NetworkRegistry.LoginPayload(
                    buffer,
                    DecalChannel.getChannelName(),
                    payload.getMessageContext(),
                    payload.needsResponse()
                ));
            } else {
                this.messageList.add(null);
            }
        }
    }
}
