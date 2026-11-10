package com.radaeli.verticaltrapdoors.fabric.client.mixin;

import com.radaeli.verticaltrapdoors.fabric.client.AltModifierClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class BlockUseMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void verticalTrapdoors$syncAltBeforeBlockUse(LocalPlayer player, InteractionHand hand,
            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        AltModifierClient.syncState(Minecraft.getInstance());
    }
}
