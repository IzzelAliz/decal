package io.izzel.mods.decal;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.function.Predicate;

public class DecalConfig {

    private static ForgeConfigSpec.ConfigValue<Long> maxSize;
    private static ForgeConfigSpec.ConfigValue<String> expireDuration;
    private static ForgeConfigSpec.ConfigValue<String> manageDuration;

    static void register() {
        var builder = new ForgeConfigSpec.Builder();
        maxSize = builder.comment("Max size of client cache in bytes").define("max-size", 64 * 1024 * 1024L);
        Predicate<Object> durationValidate = s -> {
            try {
                Duration.parse(Objects.toString(s));
                return true;
            } catch (DateTimeParseException e) {
                return false;
            }
        };
        expireDuration = builder.comment("Max time duration since last access before cache is purged, formatted in PnDTnHnMn.nS")
            .define("expire-duration", Duration.ofDays(30).toString(), durationValidate);
        manageDuration = builder.comment("Period purging client cache, formatted in PnDTnHnMn.nS")
            .define("manage-duration", Duration.ofMinutes(5).toString(), durationValidate);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, builder.build());
    }

    public static long getMaxSize() {
        return maxSize.get();
    }

    public static long getExpireMillis() {
        return Duration.parse(expireDuration.get()).toMillis();
    }

    public static long getManageMillis() {
        return Duration.parse(manageDuration.get()).toMillis();
    }
}
