package com.radaeli.verticaltrapdoors;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Orientation shared by the physical panel and its original model lookup. */
public final class TrapDoorOrientation {
    private TrapDoorOrientation() {}

    public static BlockState placementState(BlockState vanilla, Direction clickedFace, boolean shift, boolean alt) {
        boolean vertical = clickedFace.getAxis().isHorizontal() && (shift || alt);
        boolean reversed = vertical && alt && !shift;
        Direction facing = vanilla.getValue(TrapDoorBlock.FACING);
        if (vertical) {
            facing = reversed ? facing.getCounterClockWise() : facing.getClockWise();
        }
        if (!vanilla.hasProperty(TrapDoorProperties.VERTICAL) || !vanilla.hasProperty(TrapDoorProperties.REVERSED)) {
            return vanilla;
        }
        return vanilla.setValue(TrapDoorBlock.FACING, facing)
                .setValue(TrapDoorProperties.VERTICAL, vertical)
                .setValue(TrapDoorProperties.REVERSED, reversed);
    }

    public static Direction panelFacing(BlockState state) {
        Direction facing = state.getValue(TrapDoorBlock.FACING);
        if (state.getValue(TrapDoorBlock.OPEN)) {
            return facing;
        }
        return closedFacing(state);
    }

    /** Collision and outline shape for the upright panel, or {@code null} for vanilla states. */
    public static VoxelShape panelShape(BlockState state) {
        if (!state.hasProperty(TrapDoorProperties.VERTICAL) || !state.hasProperty(TrapDoorProperties.REVERSED)
                || !state.getValue(TrapDoorProperties.VERTICAL)) {
            return null;
        }
        return switch (panelFacing(state)) {
            case NORTH -> Block.box(0, 0, 13, 16, 16, 16);
            case SOUTH -> Block.box(0, 0, 0, 16, 16, 3);
            case WEST -> Block.box(13, 0, 0, 16, 16, 16);
            case EAST -> Block.box(0, 0, 0, 3, 16, 16);
            default -> throw new IllegalStateException("Trapdoor facing must be horizontal: " + panelFacing(state));
        };
    }

    public static Direction closedFacing(BlockState state) {
        Direction facing = state.getValue(TrapDoorBlock.FACING);
        return state.getValue(TrapDoorProperties.REVERSED)
                ? facing.getClockWise() : facing.getCounterClockWise();
    }

    public static BlockState modelState(BlockState state) {
        if (!state.hasProperty(TrapDoorProperties.VERTICAL) || !state.hasProperty(TrapDoorProperties.REVERSED)) {
            return state;
        }
        boolean vertical = state.getValue(TrapDoorProperties.VERTICAL);
        BlockState original = state.setValue(TrapDoorProperties.VERTICAL, false)
                .setValue(TrapDoorProperties.REVERSED, false);
        // A vertical panel has no upper/lower placement variant. Use the upper
        // vanilla model for both click heights, including previously placed panels.
        return vertical ? original.setValue(TrapDoorBlock.HALF, Half.TOP)
                .setValue(TrapDoorBlock.OPEN, true)
                .setValue(TrapDoorBlock.FACING, closedFacing(state)) : original;
    }
}
