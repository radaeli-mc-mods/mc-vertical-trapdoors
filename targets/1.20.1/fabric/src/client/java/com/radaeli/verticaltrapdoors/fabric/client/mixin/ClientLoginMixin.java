package com.radaeli.verticaltrapdoors.fabric.client.mixin;

import com.radaeli.verticaltrapdoors.fabric.network.FabricCompatibilityHandshake;
import com.radaeli.verticaltrapdoors.fabric.network.FabricCompatibilityHandshakeClient.LoginConfirmation;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientHandshakePacketListenerImpl.class)
public abstract class ClientLoginMixin implements LoginConfirmation {
    @Shadow @Final private Connection connection;
    @Unique private boolean verticalTrapdoors$serverConfirmed;

    @Override
    public void verticalTrapdoors$confirmServer() {
        verticalTrapdoors$serverConfirmed = true;
    }

    @Override
    public void verticalTrapdoors$reject(Component reason) {
        connection.disconnect(reason);
    }

    @Inject(method = "handleGameProfile", at = @At("HEAD"), cancellable = true)
    private void verticalTrapdoors$requireCompatibleServer(ClientboundGameProfilePacket packet, CallbackInfo callback) {
        if (!verticalTrapdoors$serverConfirmed) {
            verticalTrapdoors$reject(FabricCompatibilityHandshake.MISSING_MOD);
            callback.cancel();
        }
    }
}
