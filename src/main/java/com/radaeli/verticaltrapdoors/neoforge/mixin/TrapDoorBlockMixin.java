package com.radaeli.verticaltrapdoors.neoforge.mixin;

import com.radaeli.verticaltrapdoors.neoforge.block.TrapDoorProperties;
import com.radaeli.verticaltrapdoors.neoforge.network.AltModifierState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds optional vertical, door-like placement to trapdoors. */
@Mixin(TrapDoorBlock.class)
public abstract class TrapDoorBlockMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void verticalTrapdoors$setVanillaDefaults(
            BlockSetType type,
            BlockBehaviour.Properties properties,
            CallbackInfo callback) {
        Block block = (Block) (Object) this;
        BlockState defaultState = block.defaultBlockState()
                .setValue(TrapDoorProperties.VERTICAL, false)
                .setValue(TrapDoorProperties.REVERSED, false);
        ((BlockDefaultStateInvoker) (Object) this).verticalTrapdoors$registerDefaultState(defaultState);
    }

    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void verticalTrapdoors$addVerticalState(
            StateDefinition.Builder<Block, BlockState> builder,
            CallbackInfo callback) {
        builder.add(TrapDoorProperties.VERTICAL, TrapDoorProperties.REVERSED);
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void verticalTrapdoors$markSneakPlacementVertical(
            BlockPlaceContext context,
            CallbackInfoReturnable<BlockState> callback) {
        BlockState placedState = callback.getReturnValue();
        Player player = context.getPlayer();
        Direction clickedFace = context.getClickedFace();

        if (placedState != null) {
            boolean wallPlacement = clickedFace.getAxis().isHorizontal();
            boolean shiftDown = player != null && player.isShiftKeyDown();
            boolean altDown = player != null && AltModifierState.isDown(player);
            boolean vertical = wallPlacement && (shiftDown || altDown);
            boolean reversed = vertical && altDown && !shiftDown;

            callback.setReturnValue(placedState
                    .setValue(TrapDoorProperties.VERTICAL, vertical)
                    .setValue(TrapDoorProperties.REVERSED, reversed));
        }
    }

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void verticalTrapdoors$useDoorLikeShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> callback) {
        if (!state.getValue(TrapDoorProperties.VERTICAL)) {
            return;
        }

        Direction panelFacing = state.getValue(TrapDoorBlock.FACING);
        if (state.getValue(TrapDoorBlock.OPEN) && state.getValue(TrapDoorProperties.REVERSED)) {
            panelFacing = panelFacing.getCounterClockWise();
        } else if (state.getValue(TrapDoorBlock.OPEN)) {
            panelFacing = panelFacing.getClockWise();
        }

        callback.setReturnValue(switch (panelFacing) {
            case NORTH -> Block.box(0, 0, 13, 16, 16, 16);
            case SOUTH -> Block.box(0, 0, 0, 16, 16, 3);
            case WEST -> Block.box(13, 0, 0, 16, 16, 16);
            case EAST -> Block.box(0, 0, 0, 3, 16, 16);
            default -> throw new IllegalStateException("Trapdoor facing must be horizontal: " + panelFacing);
        });
    }
}
