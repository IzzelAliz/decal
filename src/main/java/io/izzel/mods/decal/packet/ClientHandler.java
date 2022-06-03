package io.izzel.mods.decal.packet;

import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.izzel.mods.decal.DecalConfig;
import io.izzel.mods.decal.DecalHandles;
import io.izzel.mods.decal.DecalMod;
import io.izzel.mods.decal.cache.FileBasedRepo;
import io.izzel.mods.decal.cache.LocalRepository;
import io.izzel.mods.decal.packet.login.C2SDecalLoginManifestAck;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = DecalMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {

    private static final ResourceLocation LOGIN_WRAPPER = new ResourceLocation("fml:loginwrapper");
    private static LocalRepository REPOSITORY;

    public static LocalRepository getRepo() {
        return REPOSITORY;
    }

    public static boolean handleLoginPacket(NetworkEvent.Context ctx, ResourceLocation channel, ByteBuf data, int loginIndex) {
        DecalMod.LOGGER.debug("Received login wrapper packet event for channel {} with index {}", channel, loginIndex);
        var readerIndex = data.readerIndex();
        DecalHandles.findTarget(channel).ifPresent(ni -> {
            var loginPayloadEvent = DecalHandles.newLoginPayloadEvent(new FriendlyByteBuf(data), () -> ctx, loginIndex);
            DecalHandles.dispatchLoginPacket(ni, loginPayloadEvent);
        });
        if (ctx.getPacketHandled()) {
            data.readerIndex(readerIndex);
            var dataWithChannel = Unpooled.wrappedBuffer(
                new FriendlyByteBuf(Unpooled.buffer())
                    .writeResourceLocation(channel)
                    .writeVarInt(data.readableBytes()),
                data
            );
            var tag = Hashing.sha256().hashBytes(dataWithChannel.nioBuffer()).toString();
            DecalMod.LOGGER.debug("Caching packet from channel {} index {}: {}", channel, loginIndex, tag);
            getRepo().put(tag, dataWithChannel);
        }
        return ctx.getPacketHandled() && advanceFrom(loginIndex + 1, ctx);
    }

    private static volatile Int2ObjectArrayMap<Callable<ByteBuf>> pendingPackets;

    public static boolean handleLoginManifest(NetworkEvent.Context ctx, String[] tags) {
        var present = new Int2ObjectArrayMap<Callable<ByteBuf>>(tags.length);
        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i];
            var path = getRepo().find(tag);
            if (path.isPresent()) {
                present.put(i, path.get());
            }
        }
        DecalMod.LOGGER.debug("Received {} packets from manifest, {} cache hits", tags.length, present.size());
        DecalChannel.channel().reply(new C2SDecalLoginManifestAck(present.keySet().toIntArray()), ctx);
        pendingPackets = present;
        return advanceFrom(0, ctx);
    }

    private static boolean advanceFrom(int index, NetworkEvent.Context ctx) {
        var packets = pendingPackets;
        if (packets == null) return false;
        for (Callable<ByteBuf> payload; (payload = packets.get(index)) != null; index++) {
            if (!cacheHitDispatchLogin(index, payload, ctx)) {
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

    private static final ScheduledExecutorService POOL =
        Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
            .setNameFormat("decal-scheduler-%s")
            .setDaemon(true).build());

    @SubscribeEvent
    public static void onSetup(FMLClientSetupEvent event) throws IOException {
        REPOSITORY = new FileBasedRepo(Paths.get("decal"), DecalConfig.getMaxSize(), DecalConfig.getExpireMillis());
        POOL.scheduleAtFixedRate(getRepo()::tick, DecalConfig.getManageMillis(), DecalConfig.getManageMillis(), TimeUnit.MILLISECONDS);
        var thread = new Thread(ClientHandler::onShutdown, "decal cleanup");
        thread.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(thread);
    }

    private static void onShutdown() {
        try {
            POOL.shutdown();
            if (!POOL.awaitTermination(30, TimeUnit.SECONDS)) {
                POOL.shutdownNow();
            }
            REPOSITORY.tick();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
