package ru.vxnku.customblocklayer.mixin;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;
import ru.vxnku.customblocklayer.render.chest.CustomChestModelManager;
import ru.vxnku.customblocklayer.util.CBLItemHelper;

@Mixin(BuiltinModelItemRenderer.class)
public class BuiltinModelItemRendererMixin {

    @Shadow @Final private BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    @Shadow @Final private ChestBlockEntity renderChestNormal;

    @Inject(
        method = "render(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void customBlockLayer$renderCustomChestItem(
        ItemStack stack, ModelTransformationMode mode,
        MatrixStack matrices, VertexConsumerProvider vertexConsumers,
        int light, int overlay, CallbackInfo ci
    ) {
        if (!CBLItemHelper.isCustomBlock(stack)) return;

        String customId = CBLItemHelper.getCustomBlockId(stack);
        CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
        if (def == null || !def.isChest()) return;

        // If item has a 2D icon, let standard quad renderer handle it
        if (def.getItemTexture() != null) return;

        try {
            CustomChestModelManager.CURRENT_ITEM_CUSTOM_ID.set(customId);
            this.blockEntityRenderDispatcher.renderEntity(this.renderChestNormal, matrices, vertexConsumers, light, overlay);
            ci.cancel();
        } finally {
            CustomChestModelManager.CURRENT_ITEM_CUSTOM_ID.remove();
        }
    }
}
