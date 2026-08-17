package ru.vxnku.customblocklayer.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LidOpenable;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vxnku.customblocklayer.render.chest.CustomChestModelManager;

@Mixin(ChestBlockEntityRenderer.class)
public abstract class ChestBlockEntityRendererMixin<T extends BlockEntity & LidOpenable> implements BlockEntityRenderer<T> {

    @Shadow @Final private ModelPart singleChestLid;
    @Shadow @Final private ModelPart singleChestLatch;
    @Shadow @Final private ModelPart singleChestBase;

    @Shadow @Final private ModelPart doubleChestLeftLid;
    @Shadow @Final private ModelPart doubleChestLeftLatch;
    @Shadow @Final private ModelPart doubleChestLeftBase;

    @Shadow @Final private ModelPart doubleChestRightLid;
    @Shadow @Final private ModelPart doubleChestRightLatch;
    @Shadow @Final private ModelPart doubleChestRightBase;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void customBlockLayer$onInit(BlockEntityRendererFactory.Context ctx, CallbackInfo ci) {
        CustomChestModelManager.initParts(
            this.singleChestLid, this.singleChestLatch, this.singleChestBase,
            this.doubleChestLeftLid, this.doubleChestLeftLatch, this.doubleChestLeftBase,
            this.doubleChestRightLid, this.doubleChestRightLatch, this.doubleChestRightBase
        );
    }

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
        if (entity instanceof ChestBlockEntity chest) {
            boolean handled = CustomChestModelManager.renderChest(chest, tickDelta, matrices, vertexConsumers, light, overlay);
            if (handled) {
                ci.cancel();
            }
        }
    }
}
