package ru.vxnku.customblocklayer.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.vxnku.customblocklayer.cache.BlockOverrideCache;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;
import ru.vxnku.customblocklayer.util.CBLItemHelper;

@Mixin(TrapdoorBlock.class)
public class TrapdoorBlockMixin {

    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    private void customBlockLayer$onGetPlacementState(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state == null) return;

        ItemStack stack = ctx.getStack();
        String customId = CBLItemHelper.getCustomBlockId(stack);
        if (customId != null) {
            CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
            if (def != null && def.isVerticalSlab()) {
                // If player is NOT sneaking (No Shift), inherit facing from below slab
                boolean isSneaking = ctx.getPlayer() != null && ctx.getPlayer().isSneaking();
                if (!isSneaking) {
                    String belowId = BlockOverrideCache.get(ctx.getBlockPos().down());
                    BlockState belowState = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
                    if (belowId != null && belowState.contains(Properties.HORIZONTAL_FACING)) {
                        state = state.with(Properties.HORIZONTAL_FACING, belowState.get(Properties.HORIZONTAL_FACING));
                    }
                }

                // Force open=true so it stands vertically without intersecting the player
                state = state.with(TrapdoorBlock.OPEN, true);
                cir.setReturnValue(state);
            }
        }
    }
}
