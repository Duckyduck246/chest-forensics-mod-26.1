package com.duckyduck246.chestforensics.mixin;

import com.duckyduck246.chestforensics.ChestForensicsClient;
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
public class ContainerInteractMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void onInteractBlock(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        ChestForensicsClient.detectedPos = null;
        ChestForensicsClient.detectedPos = hitResult.getBlockPos();
    }
}
