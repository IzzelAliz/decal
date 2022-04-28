package io.izzel.mods.decal.packet;

import java.util.function.IntSupplier;

public abstract class DecalLoginPacket implements IntSupplier {

    private int loginIndex;

    public int getLoginIndex() {
        return loginIndex;
    }

    public void setLoginIndex(int loginIndex) {
        this.loginIndex = loginIndex;
    }

    @Override
    public int getAsInt() {
        return getLoginIndex();
    }
}
