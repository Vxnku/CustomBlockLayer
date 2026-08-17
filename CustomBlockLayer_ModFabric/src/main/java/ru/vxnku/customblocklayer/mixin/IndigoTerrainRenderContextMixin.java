package ru.vxnku.customblocklayer.mixin;

import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.vxnku.customblocklayer.cache.BlockOverrideCache;
import ru.vxnku.customblocklayer.render.RetexturedModelManager;

@Mixin(value = TerrainRenderContext.class, remap = false)
public class IndigoTerrainRenderContextMixin {

    @ModifyVariable(
        method = "tessellateBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/client/util/math/MatrixStack;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private BakedModel customBlockLayer$modifyIndigoModel(
        BakedModel originalModel,
        BlockState blockState,
        BlockPos blockPos,
        BakedModel model,
        MatrixStack matrixStack
    ) {
        if (blockPos != null && BlockOverrideCache.has(blockPos)) {
            String customId = BlockOverrideCache.get(blockPos);
            return RetexturedModelManager.getModel(originalModel, customId, blockState);
        }
        return originalModel;
    }
}
