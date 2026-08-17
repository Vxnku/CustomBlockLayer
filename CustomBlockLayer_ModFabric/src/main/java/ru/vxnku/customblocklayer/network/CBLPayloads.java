package ru.vxnku.customblocklayer.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class CBLPayloads {

    /**
     * S2C: Set a single block override
     */
    public record SetBlockPayload(BlockPos pos, String customId) implements CustomPayload {
        public static final CustomPayload.Id<SetBlockPayload> ID = new CustomPayload.Id<>(Identifier.of("customblocklayer", "set_block"));
        public static final PacketCodec<PacketByteBuf, SetBlockPayload> CODEC = CustomPayload.codecOf(
            (value, buf) -> {
                buf.writeBlockPos(value.pos);
                buf.writeString(value.customId);
            },
            buf -> new SetBlockPayload(buf.readBlockPos(), buf.readString())
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * S2C: Clear a single block override
     */
    public record ClearBlockPayload(BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Id<ClearBlockPayload> ID = new CustomPayload.Id<>(Identifier.of("customblocklayer", "clear_block"));
        public static final PacketCodec<PacketByteBuf, ClearBlockPayload> CODEC = CustomPayload.codecOf(
            (value, buf) -> buf.writeBlockPos(value.pos),
            buf -> new ClearBlockPayload(buf.readBlockPos())
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * S2C: Bulk update for a chunk (supports full 1.18+ -64..320 world height)
     */
    public record ChunkBlocksPayload(int chunkX, int chunkZ, Map<Integer, String> blocks) implements CustomPayload {
        public static final CustomPayload.Id<ChunkBlocksPayload> ID = new CustomPayload.Id<>(Identifier.of("customblocklayer", "chunk_blocks"));
        public static final PacketCodec<PacketByteBuf, ChunkBlocksPayload> CODEC = CustomPayload.codecOf(
            (value, buf) -> {
                buf.writeInt(value.chunkX);
                buf.writeInt(value.chunkZ);
                buf.writeVarInt(value.blocks.size());
                for (Map.Entry<Integer, String> entry : value.blocks.entrySet()) {
                    buf.writeInt(entry.getKey());
                    buf.writeString(entry.getValue());
                }
            },
            buf -> {
                int cx = buf.readInt();
                int cz = buf.readInt();
                int size = buf.readVarInt();
                Map<Integer, String> map = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    int key = buf.readInt();
                    String val = buf.readString();
                    map.put(key, val);
                }
                return new ChunkBlocksPayload(cx, cz, map);
            }
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * C2S: Handshake sent to server upon joining
     */
    public record HandshakePayload(String version) implements CustomPayload {
        public static final CustomPayload.Id<HandshakePayload> ID = new CustomPayload.Id<>(Identifier.of("customblocklayer", "handshake"));
        public static final PacketCodec<PacketByteBuf, HandshakePayload> CODEC = CustomPayload.codecOf(
            (value, buf) -> buf.writeString(value.version),
            buf -> new HandshakePayload(buf.readString())
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
