package com.radaeli.verticaltrapdoors.neoforge.client.mixin;

import com.radaeli.verticaltrapdoors.neoforge.client.AltInputSync;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Updates Alt before vanilla predicts placement and sends its block-use packet. */
@Mixin(MultiPlayerGameMode.class)
public abstract class BlockUseMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void verticalTrapdoors$syncAltBeforeBlockUse(LocalPlayer player, InteractionHand hand,
            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        AltInputSync.syncState();
    }
}
