package ru.vxnku.customblocklayer.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vxnku.customblocklayer.cache.BlockOverrideCache;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;
import ru.vxnku.customblocklayer.util.CBLItemHelper;

@Mixin(MinecraftClient.class)
public class MinecraftClientPickBlockMixin {

    @Shadow public HitResult crosshairTarget;
    @Shadow public ClientPlayerEntity player;
    @Shadow public ClientPlayerInteractionManager interactionManager;

    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void customBlockLayer$onPickBlock(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (this.crosshairTarget == null || this.player == null || this.interactionManager == null || client.world == null) {
            return;
        }

        if (this.crosshairTarget instanceof BlockHitResult blockHit && this.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            if (BlockOverrideCache.has(pos)) {
                String customId = BlockOverrideCache.get(pos);
                CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
                
                // Get the exact base block item in world
                BlockState state = client.world.getBlockState(pos);
                Item baseItem = state.getBlock().asItem();
                if (baseItem == Items.AIR && def != null && !def.getMatchBlocks().isEmpty()) {
                    baseItem = net.minecraft.registry.Registries.BLOCK.get(def.getMatchBlocks().iterator().next()).asItem();
                }

                ItemStack customStack = CBLItemHelper.createCustomBlockItem(
                    baseItem,
                    customId,
                    def != null ? def.getDisplayName() : null
                );

                if (this.player.getAbilities().creativeMode) {
                    this.player.getInventory().addPickBlock(customStack);
                    this.interactionManager.clickCreativeStack(
                        this.player.getStackInHand(Hand.MAIN_HAND),
                        36 + this.player.getInventory().selectedSlot
                    );
                    ci.cancel();
                } else {
                    int foundSlot = -1;
                    for (int i = 0; i < this.player.getInventory().main.size(); i++) {
                        ItemStack stack = this.player.getInventory().main.get(i);
                        if (customId.equals(CBLItemHelper.getCustomBlockId(stack)) && stack.getItem() == customStack.getItem()) {
                            foundSlot = i;
                            break;
                        }
                    }

                    if (foundSlot != -1) {
                        if (PlayerInventoryAccessor.isHotbarSlot(foundSlot)) {
                            this.player.getInventory().selectedSlot = foundSlot;
                        } else {
                            this.interactionManager.pickFromInventory(foundSlot);
                        }
                        ci.cancel();
                    }
                }
            }
        }
    }

    private static class PlayerInventoryAccessor {
        public static boolean isHotbarSlot(int slot) {
            return slot >= 0 && slot < 9;
        }
    }
}
