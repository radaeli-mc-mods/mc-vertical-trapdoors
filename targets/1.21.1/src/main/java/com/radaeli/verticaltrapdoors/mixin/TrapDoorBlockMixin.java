package com.radaeli.verticaltrapdoors.mixin;

import com.radaeli.verticaltrapdoors.TrapDoorProperties;
import com.radaeli.verticaltrapdoors.TrapDoorOrientation;
import com.radaeli.verticaltrapdoors.AltModifierState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
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
        BlockState defaultState = block.defaultBlockState();
        if (defaultState.hasProperty(TrapDoorProperties.VERTICAL) && defaultState.hasProperty(TrapDoorProperties.REVERSED)) {
            ((BlockDefaultStateInvoker) (Object) this).verticalTrapdoors$registerDefaultState(
                    defaultState.setValue(TrapDoorProperties.VERTICAL, false).setValue(TrapDoorProperties.REVERSED, false));
        }
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

        if (placedState != null && placedState.hasProperty(TrapDoorProperties.VERTICAL)
                && placedState.hasProperty(TrapDoorProperties.REVERSED)) {
            boolean shiftDown = player != null && player.isShiftKeyDown();
            boolean altDown = player != null && AltModifierState.isDown(player);
            callback.setReturnValue(TrapDoorOrientation.placementState(placedState, clickedFace, shiftDown, altDown));
        }
    }

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void verticalTrapdoors$useRotatedBlockShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> callback) {
        VoxelShape panel = TrapDoorOrientation.panelShape(state);
        if (panel != null) callback.setReturnValue(panel);
    }
}
