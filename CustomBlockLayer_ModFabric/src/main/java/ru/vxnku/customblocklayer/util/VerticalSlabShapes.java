package ru.vxnku.customblocklayer.util;

import net.minecraft.block.Block;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

public class VerticalSlabShapes {
    public static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 8.0);
    public static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(0.0, 0.0, 8.0, 16.0, 16.0, 16.0);
    public static final VoxelShape WEST_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 8.0, 16.0, 16.0);
    public static final VoxelShape EAST_SHAPE = Block.createCuboidShape(8.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public static VoxelShape getShape(Direction facing) {
        if (facing == null) return SOUTH_SHAPE;
        // In vanilla Trapdoor: when facing=NORTH, the open door is attached to SOUTH edge
        return switch (facing) {
            case NORTH -> SOUTH_SHAPE;
            case SOUTH -> NORTH_SHAPE;
            case WEST  -> EAST_SHAPE;
            case EAST  -> WEST_SHAPE;
            default    -> SOUTH_SHAPE;
        };
    }
}
