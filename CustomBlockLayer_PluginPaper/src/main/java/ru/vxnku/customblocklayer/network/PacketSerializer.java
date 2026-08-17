package ru.vxnku.customblocklayer.network;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PacketSerializer {

    public static long packBlockPos(int x, int y, int z) {
        long l = 0L;
        l |= ((long)x & 0x3FFFFFFL) << 38;
        l |= ((long)z & 0x3FFFFFFL) << 12;
        l |= ((long)y & 0xFFFL);
        return l;
    }

    public static void writeVarInt(ByteArrayDataOutput out, int value) {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    public static void writeString(ByteArrayDataOutput out, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    @NotNull
    public static byte[] serializeSetBlock(@NotNull Location loc, @NotNull String customId) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeLong(packBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        writeString(out, customId);
        return out.toByteArray();
    }

    @NotNull
    public static byte[] serializeClearBlock(@NotNull Location loc) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeLong(packBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        return out.toByteArray();
    }

    @NotNull
    public static byte[] serializeChunkBlocks(int chunkX, int chunkZ, @NotNull Map<Integer, String> blocks) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeInt(chunkX);
        out.writeInt(chunkZ);
        writeVarInt(out, blocks.size());
        for (Map.Entry<Integer, String> entry : blocks.entrySet()) {
            out.writeInt(entry.getKey());
            writeString(out, entry.getValue());
        }
        return out.toByteArray();
    }
}
