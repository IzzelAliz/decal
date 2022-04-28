package io.izzel.mods.decal.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.buffer.ByteBuf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.concurrent.Callable;

public class FileBasedRepo implements LocalRepository {

    private final Cache<String, Path> cache;

    public FileBasedRepo(long maxSize, long expireMillis) {
        cache = CacheBuilder.newBuilder()
            .<String, Path>weigher((k, v) -> Files.size(v))
            .build();
    }

    @Override
    public Optional<Callable<ByteBuf>> find(String tag) {
        Files.setLastModifiedTime(null, FileTime.fromMillis())
        return Optional.empty();
    }

    @Override
    public void put(String tag, ByteBuf data) {

    }
}
