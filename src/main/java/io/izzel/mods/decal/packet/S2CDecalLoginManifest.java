package io.izzel.mods.decal.packet;

import io.izzel.mods.decal.client.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.function.Supplier;

public class S2CDecalLoginManifest extends DecalLoginPacket {

    private final byte[][] tags;

    public S2CDecalLoginManifest(byte[][] tags) {
        this.tags = tags;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(tags.length);
        for (byte[] tag : tags) {
            buf.writeByteArray(tag);
        }
    }

    public static S2CDecalLoginManifest decode(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        byte[][] tags = new byte[len][];
        for (int i = 0; i < len; i++) {
            tags[i] = buf.readByteArray();
        }
        return new S2CDecalLoginManifest(tags);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        return ClientHandler.handleLoginManifest(ctx.get(), Arrays.stream(this.tags).map(this::hexToStr).toArray(String[]::new));
    }

    private String hexToStr(byte[] bytes) {
        var sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
        }
        return sb.toString();
    }

    private static final char[] hexDigits = "0123456789abcdef".toCharArray();
}
