package io.izzel.mods.decal;

import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.LoginWrapper;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkInstance;
import net.minecraftforge.network.NetworkRegistry;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;
import java.util.function.Supplier;

public final class DecalHandles {

    private DecalHandles() {
    }

    private static final MethodHandle H_findTarget;
    private static final MethodHandle H_newLoginPayloadEvent;
    private static final MethodHandle H_dispatchLoginPacket;
    private static final MethodHandle H_FML_HANDSHAKE_HANDLER;
    private static final MethodHandle H_sendServerToClientLoginPacket;

    static {
        try {
            var lookup = MethodHandles.lookup();
            H_findTarget = MethodHandles.privateLookupIn(NetworkRegistry.class, lookup)
                .findStatic(NetworkRegistry.class, "findTarget", MethodType.methodType(Optional.class, ResourceLocation.class));
            H_newLoginPayloadEvent = MethodHandles.privateLookupIn(NetworkEvent.LoginPayloadEvent.class, lookup)
                .findConstructor(NetworkEvent.LoginPayloadEvent.class, MethodType.methodType(void.class, FriendlyByteBuf.class, Supplier.class, int.class));
            H_dispatchLoginPacket = MethodHandles.privateLookupIn(NetworkInstance.class, lookup)
                .findVirtual(NetworkInstance.class, "dispatchLoginPacket", MethodType.methodType(void.class, NetworkEvent.LoginPayloadEvent.class));
            H_FML_HANDSHAKE_HANDLER = MethodHandles.privateLookupIn(NetworkConstants.class, lookup)
                .findStaticGetter(NetworkConstants.class, "FML_HANDSHAKE_HANDLER", AttributeKey.class);
            H_sendServerToClientLoginPacket = MethodHandles.privateLookupIn(LoginWrapper.class, lookup)
                .findVirtual(LoginWrapper.class, "sendServerToClientLoginPacket", MethodType.methodType(void.class, ResourceLocation.class, FriendlyByteBuf.class, int.class, Connection.class));
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unchecked")
    public static Optional<NetworkInstance> findTarget(ResourceLocation rl) {
        try {
            return (Optional<NetworkInstance>) H_findTarget.invokeExact(rl);
        } catch (Throwable e) {
            return throwException(e);
        }
    }

    public static NetworkEvent.LoginPayloadEvent newLoginPayloadEvent(FriendlyByteBuf payload, Supplier<NetworkEvent.Context> source, int loginIndex) {
        try {
            return (NetworkEvent.LoginPayloadEvent) H_newLoginPayloadEvent.invokeExact(payload, source, loginIndex);
        } catch (Throwable e) {
            return throwException(e);
        }
    }

    public static void dispatchLoginPacket(NetworkInstance ni, NetworkEvent.LoginPayloadEvent loginPayloadEvent) {
        try {
            H_dispatchLoginPacket.invokeExact(ni, loginPayloadEvent);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static HandshakeHandler getHandshakeHandler(Connection connection) {
        try {
            var key = (AttributeKey<HandshakeHandler>) H_FML_HANDSHAKE_HANDLER.invokeExact();
            return connection.channel().attr(key).get();
        } catch (Throwable e) {
            return throwException(e);
        }
    }

    public static void sendServerToClientLoginPacket(LoginWrapper loginWrapper, ResourceLocation rl, FriendlyByteBuf buffer, int index, Connection manager) {
        try {
            H_sendServerToClientLoginPacket.invokeExact(loginWrapper, rl, buffer, index, manager);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T throwException(Throwable t) throws E {
        throw (E) t;
    }
}
