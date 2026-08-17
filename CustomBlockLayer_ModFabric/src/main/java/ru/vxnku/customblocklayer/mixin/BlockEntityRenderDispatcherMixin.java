package ru.vxnku.customblocklayer.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.vxnku.customblocklayer.cache.BlockOverrideCache;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;
import ru.vxnku.customblocklayer.render.chest.CustomChestModelManager;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    @Inject(
        method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private <E extends BlockEntity> void customBlockLayer$onRenderBlockEntity(
        E blockEntity, float tickDelta, MatrixStack matrices,
        VertexConsumerProvider vertexConsumers, CallbackInfo ci
    ) {
        if (blockEntity instanceof ChestBlockEntity chest) {
            BlockPos pos = chest.getPos();
            if (pos != null && BlockOverrideCache.has(pos)) {
                String customId = BlockOverrideCache.get(pos);
                CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
                if (def != null && def.isChest()) {
                    int light = chest.getWorld() != null ? WorldRenderer.getLightmapCoordinates(chest.getWorld(), chest.getPos()) : 15728880;
                    boolean rendered = CustomChestModelManager.renderChest(
                        chest, tickDelta, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV
                    );
                    if (rendered) {
                        ci.cancel();
                    }
                }
            }
        }
    }
}
