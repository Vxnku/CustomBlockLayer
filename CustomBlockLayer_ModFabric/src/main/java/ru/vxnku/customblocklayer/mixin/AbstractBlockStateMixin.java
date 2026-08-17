package ru.vxnku.customblocklayer.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.vxnku.customblocklayer.cache.BlockOverrideCache;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;
import ru.vxnku.customblocklayer.util.VerticalSlabShapes;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {

    @Shadow
    protected abstract BlockState asBlockState();

    @Inject(method = "onUseWithItem", at = @At("HEAD"), cancellable = true)
    private void customBlockLayer$onUseWithItem(
        ItemStack stack,
        World world,
        PlayerEntity player,
        Hand hand,
        BlockHitResult hit,
        CallbackInfoReturnable<ItemActionResult> cir
    ) {
        String customId = BlockOverrideCache.get(hit.getBlockPos());
        if (customId != null) {
            CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
            if (def != null && def.isVerticalSlab()) {
                cir.setReturnValue(ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
            }
        }
    }

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void customBlockLayer$onUse(
        World world,
        PlayerEntity player,
        BlockHitResult hit,
        CallbackInfoReturnable<ActionResult> cir
    ) {
        String customId = BlockOverrideCache.get(hit.getBlockPos());
        if (customId != null) {
            CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
            if (def != null && def.isVerticalSlab()) {
                cir.setReturnValue(ActionResult.PASS);
            }
        }
    }

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void customBlockLayer$onGetCollisionShape(BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        BlockState state = asBlockState();
        if (state.isAir()) return;

        String customId = BlockOverrideCache.get(pos);
        if (customId != null) {
            CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
            if (def != null && def.isVerticalSlab()) {
                Direction facing = state.contains(Properties.HORIZONTAL_FACING) ? state.get(Properties.HORIZONTAL_FACING) : (state.contains(Properties.FACING) ? state.get(Properties.FACING) : Direction.NORTH);
                cir.setReturnValue(VerticalSlabShapes.getShape(facing));
            }
        }
    }

    @Inject(method = "getOutlineShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void customBlockLayer$onGetOutlineShape(BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        BlockState state = asBlockState();
        if (state.isAir()) return;

        String customId = BlockOverrideCache.get(pos);
        if (customId != null) {
            CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
            if (def != null && def.isVerticalSlab()) {
                Direction facing = state.contains(Properties.HORIZONTAL_FACING) ? state.get(Properties.HORIZONTAL_FACING) : (state.contains(Properties.FACING) ? state.get(Properties.FACING) : Direction.NORTH);
                cir.setReturnValue(VerticalSlabShapes.getShape(facing));
            }
        }
    }

    @Inject(method = "getCameraCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void customBlockLayer$onGetCameraCollisionShape(BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        BlockState state = asBlockState();
        if (state.isAir()) return;

        String customId = BlockOverrideCache.get(pos);
        if (customId != null) {
            CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
            if (def != null && def.isVerticalSlab()) {
                Direction facing = state.contains(Properties.HORIZONTAL_FACING) ? state.get(Properties.HORIZONTAL_FACING) : (state.contains(Properties.FACING) ? state.get(Properties.FACING) : Direction.NORTH);
                cir.setReturnValue(VerticalSlabShapes.getShape(facing));
            }
        }
    }

    @Inject(method = "getRaycastShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/shape/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void customBlockLayer$onGetRaycastShape(BlockView world, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        BlockState state = asBlockState();
        if (state.isAir()) return;

        String customId = BlockOverrideCache.get(pos);
        if (customId != null) {
            CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
            if (def != null && def.isVerticalSlab()) {
                Direction facing = state.contains(Properties.HORIZONTAL_FACING) ? state.get(Properties.HORIZONTAL_FACING) : (state.contains(Properties.FACING) ? state.get(Properties.FACING) : Direction.NORTH);
                cir.setReturnValue(VerticalSlabShapes.getShape(facing));
            }
        }
    }
}
