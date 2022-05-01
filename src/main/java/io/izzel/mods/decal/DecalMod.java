package io.izzel.mods.decal;

import io.izzel.mods.decal.packet.DecalChannel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

@Mod(DecalMod.MODID)
public class DecalMod {

    public static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "decal";

    public DecalMod() {
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
            () -> new IExtensionPoint.DisplayTest(
                () -> NetworkConstants.IGNORESERVERONLY,
                (a, b) -> true
            ));
        Objects.requireNonNull(DecalChannel.channel(), "channel");
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> DecalConfig::register);
    }
}
