package ru.vxnku.customblocklayer.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LidOpenable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vxnku.customblocklayer.cache.BlockOverrideCache;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;
import ru.vxnku.customblocklayer.render.chest.CustomChestModelManager;

@Mixin(ChestBlockEntityRenderer.class)
public abstract class ChestBlockEntityRendererMixin<T extends BlockEntity & LidOpenable> implements BlockEntityRenderer<T> {

    @Shadow @Final private ModelPart singleChestLid;
    @Shadow @Final private ModelPart singleChestLatch;
    @Shadow @Final private ModelPart singleChestBase;

    @Shadow
    protected abstract void render(
        MatrixStack matrices, VertexConsumer vertices, ModelPart lid,
        ModelPart latch, ModelPart base, float openFactor, int light, int overlay
    );

    @Inject(
        method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void customBlockLayer$renderCustomChest(
        T entity, float tickDelta, MatrixStack matrices,
        VertexConsumerProvider vertexConsumers, int light, int overlay,
        CallbackInfo ci
    ) {
        String customId = null;
        if (entity.hasWorld()) {
            BlockPos pos = entity.getPos();
            if (pos != null && BlockOverrideCache.has(pos)) {
                customId = BlockOverrideCache.get(pos);
            }
        } else {
            customId = CustomChestModelManager.CURRENT_ITEM_CUSTOM_ID.get();
        }

        if (customId == null) return;

        CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
        if (def == null) return;

        BlockState state = entity.hasWorld() ? entity.getCachedState() : Blocks.CHEST.getDefaultState();
        Direction facing = state.contains(Properties.HORIZONTAL_FACING) ? state.get(Properties.HORIZONTAL_FACING) : Direction.SOUTH;

        // Mode 1: Arbitrary 3D JSON Model
        if (def.isJsonModel() && CustomBlockRegistry.hasJsonModel(customId)) {
            BakedModel customBakedModel = CustomBlockRegistry.getJsonModel(customId);
            if (customBakedModel != null) {
                matrices.push();
                matrices.translate(0.5, 0.5, 0.5);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
                matrices.translate(-0.5, -0.5, -0.5);

                VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getCutoutMipped());
                if (entity.hasWorld()) {
                    MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer().render(
                        entity.getWorld(), customBakedModel, state, entity.getPos(), matrices, vertexConsumer, false,
                        entity.getWorld().getRandom(), 42L, overlay
                    );
                } else {
                    MinecraftClient.getInstance().getItemRenderer().renderItem(
                        Blocks.CHEST.asItem().getDefaultStack(),
                        net.minecraft.client.render.model.json.ModelTransformationMode.NONE,
                        false, matrices, vertexConsumers, light, overlay, customBakedModel
                    );
                }

                matrices.pop();
                ci.cancel();
                return;
            }
        }

        // Mode 2: Animated Chest with Custom 64x64 Texture Map
        RenderLayer customLayer = CustomChestModelManager.getChestRenderLayer(customId);
        if (customLayer != null) {
            matrices.push();
            float rot = facing.asRotation();
            matrices.translate(0.5F, 0.5F, 0.5F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rot));
            matrices.translate(-0.5F, -0.5F, -0.5F);

            float openProgress = 0.0F;
            if (entity.hasWorld()) {
                openProgress = entity.getAnimationProgress(tickDelta);
                openProgress = 1.0F - openProgress;
                openProgress = 1.0F - openProgress * openProgress * openProgress;
            }

            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(customLayer);
            this.render(
                matrices, vertexConsumer,
                this.singleChestLid, this.singleChestLatch, this.singleChestBase,
                openProgress, light, overlay
            );

            matrices.pop();
            ci.cancel();
        }
    }
}
