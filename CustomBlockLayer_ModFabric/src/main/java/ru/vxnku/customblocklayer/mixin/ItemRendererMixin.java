package ru.vxnku.customblocklayer.mixin;

import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.vxnku.customblocklayer.render.RetexturedModelManager;
import ru.vxnku.customblocklayer.util.CBLItemHelper;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Inject(
        method = "getModel(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)Lnet/minecraft/client/render/model/BakedModel;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void customBlockLayer$onGetItemModel(
        ItemStack stack,
        World world,
        LivingEntity entity,
        int seed,
        CallbackInfoReturnable<BakedModel> cir
    ) {
        if (CBLItemHelper.isCustomBlock(stack)) {
            String customId = CBLItemHelper.getCustomBlockId(stack);
            BakedModel originalModel = cir.getReturnValue();
            BakedModel customModel = RetexturedModelManager.getModel(originalModel, customId, null);
            if (customModel != null) {
                cir.setReturnValue(customModel);
            }
        }
    }
}
