package io.izzel.mods.decal.packet;

import io.izzel.mods.decal.DecalHandles;
import io.izzel.mods.decal.DecalMod;
import io.izzel.mods.decal.packet.login.C2SDecalLoginManifestAck;
import io.izzel.mods.decal.packet.login.S2CDecalLoginManifest;
import io.izzel.mods.decal.packet.login.S2CDecalLoginWrapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.ThreadLocalRandom;

public class DecalChannel {

    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL;
    private static final int MANIFEST_ID;
    private static final ResourceLocation CHANNEL_NAME;

    static {
        CHANNEL_NAME = new ResourceLocation(DecalMod.MODID, "ch");
        CHANNEL = NetworkRegistry.newSimpleChannel(
            CHANNEL_NAME,
            () -> VERSION,
            NetworkRegistry.acceptMissingOr(VERSION),
            NetworkRegistry.acceptMissingOr(VERSION)
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
            .add();
        var ni = DecalHandles.findTarget(CHANNEL_NAME).orElseThrow();
        ni.addListener(ServerHandler::onClientMissing);
        MANIFEST_ID = ThreadLocalRandom.current().nextInt(1 << 15, 1 << 16); // this is big enough
    }

    public static SimpleChannel channel() {
        return CHANNEL;
    }

    public static int getManifestId() {
        return MANIFEST_ID;
    }

    public static ResourceLocation getChannelName() {
        return CHANNEL_NAME;
    }
}
