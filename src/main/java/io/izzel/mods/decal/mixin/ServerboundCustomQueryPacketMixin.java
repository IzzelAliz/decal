package io.izzel.mods.decal.mixin;

import io.izzel.mods.decal.packet.DecalChannel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ICustomPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(ServerboundCustomQueryPacket.class)
public abstract class ServerboundCustomQueryPacketMixin {

    // @formatter:off
    @Shadow @Final @Nullable private FriendlyByteBuf data;
    @Shadow public abstract int getTransactionId();
    // @formatter:on

    @Inject(method = "getName", remap = false, cancellable = true, at = @At("HEAD"))
    public void getName(CallbackInfoReturnable<ResourceLocation> cir) {
        if (this.getTransactionId() == DecalChannel.getManifestId() && this.data == null) {
            cir.setReturnValue(DecalChannel.getChannelName());
        }
    }
}
