package ru.vxnku.customblocklayer.integration.worldedit;

import com.sk89q.worldedit.math.BlockVector3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CBLPatternContext {
    private static final ThreadLocal<Map<BlockVector3, String>> PENDING_BLOCKS = ThreadLocal.withInitial(ConcurrentHashMap::new);

    public static void setPending(BlockVector3 pos, String customId) {
        PENDING_BLOCKS.get().put(pos, customId);
    }

    public static String getAndRemovePending(BlockVector3 pos) {
        return PENDING_BLOCKS.get().remove(pos);
    }

    public static void clear() {
        PENDING_BLOCKS.get().clear();
    }
}
