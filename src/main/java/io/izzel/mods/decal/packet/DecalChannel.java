package io.izzel.mods.decal.packet;

import io.izzel.mods.decal.DecalMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class DecalChannel {

    private static final SimpleChannel CHANNEL;

    static {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DecalMod.MODID, "ch"),
            () -> "1",
            s -> true,
            s -> true
        );
        CHANNEL.messageBuilder(S2CDecalLoginManifest.class, 0, NetworkDirection.LOGIN_TO_CLIENT)
            .loginIndex(S2CDecalLoginManifest::getLoginIndex, S2CDecalLoginManifest::setLoginIndex)
            .encoder(S2CDecalLoginManifest::encode)
            .decoder(S2CDecalLoginManifest::decode)
            .consumer(S2CDecalLoginManifest::handle)
            .add();
        CHANNEL.messageBuilder(C2SDecalLoginManifestAck.class, 1, NetworkDirection.LOGIN_TO_SERVER)
            .loginIndex(C2SDecalLoginManifestAck::getLoginIndex, C2SDecalLoginManifestAck::setLoginIndex)
            .encoder(C2SDecalLoginManifestAck::encode)
            .decoder(C2SDecalLoginManifestAck::decode)
            .consumer(HandshakeHandler.indexFirst(C2SDecalLoginManifestAck::handle))
            .add();
        CHANNEL.messageBuilder(S2CDecalLoginWrapper.class, 2, NetworkDirection.LOGIN_TO_CLIENT)
            .loginIndex(S2CDecalLoginWrapper::getLoginIndex, S2CDecalLoginWrapper::setLoginIndex)
            .encoder(S2CDecalLoginWrapper::encode)
            .decoder(S2CDecalLoginWrapper::decode)
            .consumer(S2CDecalLoginWrapper::handle)
    }

    public static SimpleChannel channel() {
        return CHANNEL;
    }
}
