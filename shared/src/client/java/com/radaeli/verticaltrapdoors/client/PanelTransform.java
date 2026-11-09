package com.radaeli.verticaltrapdoors.client;

import com.radaeli.verticaltrapdoors.TrapDoorProperties;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/** Shared upright-panel model transform, kept out of loader model wrappers. */
public final class PanelTransform {
    private PanelTransform() {}

    public static Matrix4f matrix(BlockState sourceState, BlockState placedState) {
        boolean reversed = placedState.getValue(TrapDoorProperties.REVERSED);
        boolean opened = placedState.getValue(TrapDoorBlock.OPEN);
        Direction closedFacing = sourceState.getValue(TrapDoorBlock.FACING);
        Direction openFacing = placedState.getValue(TrapDoorBlock.FACING);
        float quarterTurn = (float) (Math.PI / 2);
        Matrix4f matrix = new Matrix4f();
        if (opened) {
            float travel = 1.0F - 3.0F / 16;
            matrix.translate(-openFacing.getStepX() * travel, 0, -openFacing.getStepZ() * travel);
        }
        matrix.translate(.5F, .5F, .5F);
        if (opened) matrix.rotateY(reversed ? -quarterTurn : quarterTurn);
        matrix.rotate(reversed ? quarterTurn : -quarterTurn,
                closedFacing.getStepX(), 0, closedFacing.getStepZ());
        return matrix.translate(-.5F, -.5F, -.5F);
    }

}
