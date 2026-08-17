package ru.vxnku.customblocklayer.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.vxnku.customblocklayer.cache.BlockOverrideCache;
import ru.vxnku.customblocklayer.render.RetexturedModelManager;

@Mixin(BlockRenderManager.class)
public class BlockRenderManagerMixin {

    @Unique
    private static final ThreadLocal<BlockPos> customBlockLayer$CURRENT_POS = new ThreadLocal<>();

    @Inject(
        method = "renderBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;)V",
        at = @At("HEAD")
    )
    private void customBlockLayer$beforeRenderBlock(
        BlockState state,
        BlockPos pos,
        BlockRenderView world,
        MatrixStack matrices,
        VertexConsumer vertexConsumer,
        boolean cull,
        Random random,
        CallbackInfo ci
    ) {
        customBlockLayer$CURRENT_POS.set(pos);
    }

    @Inject(
        method = "renderBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;)V",
        at = @At("RETURN")
    )
    private void customBlockLayer$afterRenderBlock(
        BlockState state,
        BlockPos pos,
        BlockRenderView world,
        MatrixStack matrices,
        VertexConsumer vertexConsumer,
        boolean cull,
        Random random,
        CallbackInfo ci
    ) {
        customBlockLayer$CURRENT_POS.remove();
    }

    @Inject(
        method = "getModel(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/model/BakedModel;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void customBlockLayer$onGetModel(BlockState state, CallbackInfoReturnable<BakedModel> cir) {
        BlockPos pos = customBlockLayer$CURRENT_POS.get();
        if (pos != null && BlockOverrideCache.has(pos)) {
            String customId = BlockOverrideCache.get(pos);
            BakedModel originalModel = cir.getReturnValue();
            BakedModel customModel = RetexturedModelManager.getModel(originalModel, customId, state);
            if (customModel != null) {
                cir.setReturnValue(customModel);
            }
        }
    }
}
