package ru.vxnku.customblocklayer.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.vxnku.customblocklayer.cache.BlockOverrideCache;
import ru.vxnku.customblocklayer.render.RetexturedModelManager;

@Mixin(BlockModelRenderer.class)
public class BlockModelRendererMixin {

    @ModifyVariable(
        method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private BakedModel customBlockLayer$modifyModel(
        BakedModel originalModel,
        BlockRenderView world,
        BakedModel model,
        BlockState state,
        BlockPos pos,
        MatrixStack matrices,
        VertexConsumer vertexConsumer,
        boolean cull,
        Random random,
        long seed,
        int overlay
    ) {
        if (pos != null && BlockOverrideCache.has(pos)) {
            String customId = BlockOverrideCache.get(pos);
            return RetexturedModelManager.getModel(originalModel, customId, state);
        }
        return originalModel;
    }
}
