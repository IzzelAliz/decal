package io.izzel.mods.decal.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import io.izzel.mods.decal.DecalMod;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

public class FileBasedRepo implements LocalRepository {

    private static class Entry {

        private final Path path;
        private final long size;
        private final String tag;

        private long lastAccess;

        @Nullable private ByteBuf data;

        private Entry(Path path, long size, String tag) {
            this.path = path;
            this.size = size;
            this.tag = tag;
        }

        private Entry(Path path, long size, String tag, long lastAccess) {
            this(path, size, tag);
            this.lastAccess = lastAccess;
        }

        public ByteBuf readBytes() throws IOException {
            this.lastAccess = System.currentTimeMillis();
            var data = this.data;
            if (data != null) {
                return data.slice();
            } else {
                return Unpooled.wrappedBuffer(Files.readAllBytes(this.path));
            }
        }

        @Nullable
        public ByteBuf getData() {
            return data;
        }

        public long getLastAccess() {
            return lastAccess;
        }

        public void write() throws IOException {
            if (data != null) {
                synchronized (this) {
                    var data = this.data;
                    if (data != null) {
                        var channel = Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                        channel.write(data.nioBuffer());
                        this.data = null;
                    }
                }
            }
        }

        public void delete() throws IOException {
            Files.deleteIfExists(path);
        }
    }

    private class EntrySerializer implements JsonSerializer<Entry>, JsonDeserializer<Entry> {

        @Override
        public Entry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var object = json.getAsJsonObject();
            var tag = object.get("tag").getAsString();
            var size = object.get("size").getAsLong();
            var lastAccess = object.get("lastAccess").getAsLong();
            var path = baseDir.resolve(tag);
            return new Entry(path, size, tag, lastAccess);
        }

        @Override
        public JsonElement serialize(Entry src, Type typeOfSrc, JsonSerializationContext context) {
            var object = new JsonObject();
            object.addProperty("size", src.size);
            object.addProperty("tag", src.tag);
            object.addProperty("lastAccess", src.lastAccess);
            return object;
        }
    }

    private final Gson gson = new GsonBuilder().registerTypeAdapter(Entry.class, new EntrySerializer()).setPrettyPrinting().create();

    private final Object2ObjectLinkedOpenHashMap<String, Entry> map = new Object2ObjectLinkedOpenHashMap<>();

    private final Path baseDir;
    private final long maxSize;
    private final long expireMillis;

    public FileBasedRepo(Path baseDir, long maxSize, long expireMillis) throws IOException {
        this.baseDir = baseDir;
        this.maxSize = maxSize;
        this.expireMillis = expireMillis;
        Files.createDirectories(baseDir);
        this.loadData();
    }

    private void loadData() throws IOException {
        var path = this.baseDir.resolve("data.json");
        if (Files.exists(path)) {
            var entries = gson.fromJson(Files.newBufferedReader(path), Entry[].class);
            for (Entry entry : entries) {
                map.put(entry.tag, entry);
            }
        }
    }

    private void saveData() throws IOException {
        var path = this.baseDir.resolve("data.json");
        Entry[] array;
        synchronized (this) {
            array = map.values().toArray(Entry[]::new);
        }
        var content = gson.toJson(array);
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    @Override
    public synchronized Optional<Callable<ByteBuf>> find(String tag) {
        var entry = map.getAndMoveToFirst(tag);
        if (entry != null) {
            return Optional.of(entry::readBytes);
        } else {
            return Optional.empty();
        }
    }

    private Entry newEntry(String tag, ByteBuf data) {
        var entry = new Entry(baseDir.resolve(tag), data.readableBytes(), tag);
        entry.data = data;
        entry.lastAccess = System.currentTimeMillis();
        return entry;
    }

    @Override
    public synchronized void put(String tag, ByteBuf data) {
        var entry = newEntry(tag, data);
        this.map.put(tag, entry);
    }

    @Override
    public void tick() {
        long time = System.currentTimeMillis();
        List<Entry> toRemove = new ArrayList<>();
        List<Entry> toWrite = new ArrayList<>();
        synchronized (this) {
            var totalSize = 0L;
            for (var iterator = map.object2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
                var mapEntry = iterator.next();
                var entry = mapEntry.getValue();
                if (entry.getData() != null) {
                    toWrite.add(entry);
                }
                if (entry.getLastAccess() + this.expireMillis < time) {
                    toRemove.add(entry);
                    iterator.remove();
                } else {
                    totalSize += entry.size;
                }
            }
            if (totalSize > this.maxSize) {
                var sortedEntry = map.values().stream().sorted(Comparator.comparing(it -> it.lastAccess)).toList();
                for (Entry entry : sortedEntry) {
                    totalSize -= entry.size;
                    toRemove.add(entry);
                    if (totalSize <= this.maxSize) {
                        break;
                    }
                }
            }
        }
        for (Entry entry : toWrite) {
            try {
                entry.write();
            } catch (IOException e) {
                DecalMod.LOGGER.error("Failed to write packet cache " + entry.path, e);
            }
        }
        for (Entry entry : toRemove) {
            try {
                entry.delete();
            } catch (IOException e) {
                DecalMod.LOGGER.error("Failed to purge packet cache " + entry.path, e);
            }
        }
        try {
            this.saveData();
        } catch (IOException e) {
            DecalMod.LOGGER.error("Failed to write data", e);
        }
    }
}
