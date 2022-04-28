package io.izzel.mods.decal.client;

import io.izzel.mods.decal.DecalMod;
import io.izzel.mods.decal.cache.LocalRepository;
import io.izzel.mods.decal.packet.C2SDecalLoginManifestAck;
import io.izzel.mods.decal.packet.DecalChannel;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.concurrent.Callable;

@Mod.EventBusSubscriber(modid = DecalMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientHandler {

    private static final ResourceLocation LOGIN_WRAPPER = new ResourceLocation("fml:loginwrapper");

    public static LocalRepository getRepo() {
        throw new RuntimeException(); // todo
    }

    public static boolean handleLoginPacket(NetworkEvent.Context ctx, ResourceLocation channel, ByteBuf data) {

    }

    public static boolean handleLoginManifest(NetworkEvent.Context ctx, String[] tags) {
        var present = new Int2ObjectArrayMap<Callable<ByteBuf>>(tags.length);
        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i];
            var path = getRepo().find(tag);
            if (path.isPresent()) {
                present.put(i, path.get());
            }
        }
        DecalChannel.channel().reply(new C2SDecalLoginManifestAck(present.keySet().toIntArray()), ctx);
        for (var entry : present.int2ObjectEntrySet()) {
            if (!cacheHitDispatchLogin(entry.getIntKey(), entry.getValue(), ctx)) {
                return false;
            }
        }
        return true;
    }

    private static boolean cacheHitDispatchLogin(int loginIndex, Callable<ByteBuf> dataSource, NetworkEvent.Context ctx) {
        try {
            ByteBuf buf = dataSource.call();
            var packet = new ClientboundCustomQueryPacket(loginIndex, LOGIN_WRAPPER, new FriendlyByteBuf(buf));
            DecalMod.LOGGER.debug("Dispatching cached login packet {}", loginIndex);
            return NetworkHooks.onCustomPayload(packet, ctx.getNetworkManager());
        } catch (Exception e) {
            DecalMod.LOGGER.error("Error reading login packet " + loginIndex, e);
            return false;
        }
    }
}
