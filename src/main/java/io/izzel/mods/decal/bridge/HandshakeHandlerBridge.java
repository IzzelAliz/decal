package io.izzel.mods.decal.bridge;

public interface HandshakeHandlerBridge {

    void decalClientMissing();

    void decalClientHit(int[] packets);
}
