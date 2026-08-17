package ru.vxnku.customblocklayer.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.vxnku.customblocklayer.cache.BlockOverrideCache;
import ru.vxnku.customblocklayer.render.RetexturedModelManager;

/**
 * Direct hook for Sodium 0.6+ chunk meshing pipeline.
 * Uses @Pseudo and matches method by name to support both Dev and Production mappings.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public class SodiumBlockRendererMixin {

    @ModifyVariable(
        method = "renderModel",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private BakedModel customBlockLayer$modifySodiumModel(
        BakedModel originalModel,
        BakedModel model,
        BlockState state,
        BlockPos pos,
        BlockPos origin
    ) {
        if (pos != null && BlockOverrideCache.has(pos)) {
            String customId = BlockOverrideCache.get(pos);
            return RetexturedModelManager.getModel(originalModel, customId, state);
        }
        return originalModel;
    }
}
