package com.duckyduck246.chestforensics.mixin;


import com.duckyduck246.chestforensics.PuedoItem;
import com.duckyduck246.chestforensics.ChestForensicsClient;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

import static com.duckyduck246.chestforensics.ChestForensicsClient.*;

@Mixin(AbstractContainerMenu.class)
public abstract class ContainerDetectionMixin{

    @Inject(method = "initializeContents", at = @At("TAIL"))
    private void chestforensics$onUpdateAll(int revision, List<ItemStack> stacks, ItemStack cursorStack, CallbackInfo ci) {
        processBulkUpdate(stacks);
    }

    @Inject(method = "setItem", at = @At("TAIL"))
    private void chestforensics$onSetStack(int slotIndex, int revision, ItemStack stack, CallbackInfo ci) {
        processSingleUpdate(slotIndex, stack);
    }

    private void processBulkUpdate(List<ItemStack> stacks){
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof AbstractContainerScreen<?> handledScreen){
            if (handledScreen.getMenu() instanceof ChestMenu container) {
                int containerSize = container.getRowCount() * 9;
                if(loggingMode > 1)
                    ChestForensicsClient.LOGGER.info("batch updates received for: " + containerSize + " slots");
                for (int i = 0; i < Math.min(stacks.size(), containerSize); i++) {
                    ItemStack stack = stacks.get(i);
                    if (!stack.isEmpty()) {
                        if(loggingMode > 2)
                            ChestForensicsClient.LOGGER.info("Slot " + i + ": " + stack.getHoverName().getString() + " x" + stack.getCount());
                    }
                }
                if(detectedPos == null){
                    if(loggingMode > 0)
                        ChestForensicsClient.LOGGER.info("DETECTED POS IS NULL");
                    return;
                }
                ArrayList<PuedoItem> compare1 = ChestForensicsClient.getCompare();
                for (int o = 0; o < compare1.size(); o++) {
                    String string = compare1.get(o).count + "x " + compare1.get(o).name;
                    if(loggingMode > 1)
                        ChestForensicsClient.LOGGER.info("Compared: " + string);
                    if(!compare1.get(o).isEmpty()){
                        int finalO = o;

                        if(compare1.get(o).count > 0){
                            string = "+" + string;
                        }
                        
                        Component text = Component.literal(string);
                        Component nbt = Component.literal(compare1.get(finalO).nbt);

                        Component newText;
                        if(compare1.get(o).count < 0){
                            newText = text.copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                        }
                        else if(compare1.get(o).count > 0){
                            newText = text.copy().withStyle(ChatFormatting.GREEN);
                        }
                        else{
                            newText = text.copy().withStyle(ChatFormatting.GRAY);
                        }
                        newText = newText.copy().withStyle(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(nbt.toString())).withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy full NBT data to clipboard"))));
                        if(loggingMode > 1)
                            ChestForensicsClient.LOGGER.info(compare1.get(o).name);

                        //Component containerText = Component.literal("Container ").withStyle(ChatFormatting.GRAY);
                        Component containerText = Component.literal(ChestForensicsClient.containerName + " ").withStyle(ChatFormatting.GRAY);

                        if(ChestForensicsClient.containerName.equals("Chest")){
                           containerText = Component.literal("Chest ").withStyle(ChatFormatting.GOLD);
                        }
                        if(ChestForensicsClient.containerName.equals("Barrel")){
                           containerText = Component.literal("Barrel ").withStyle(ChatFormatting.YELLOW);
                        }
                        if(ChestForensicsClient.containerName.equals("Large Chest")){
                           containerText = Component.literal("Large Chest ").withStyle(ChatFormatting.GOLD);
                        }
                        if(!(detectedPos == null)){
                            containerText = containerText.copy().withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal("Position: " + detectedPos + " Dimension: " + dimension + " Name: " + ChestForensicsClient.containerName))));
                        }
                        Component changesText = Component.literal("Detected Changes: ");
                        newText = Component.empty().append(containerText.copy()).append(changesText.copy()).append(newText.copy());

                        if (client.player != null) {
                            client.player.sendSystemMessage(newText);
                        }
                    }
                }
            }
        }
    }

    private void processSingleUpdate(int slotIndex, ItemStack stack){
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof AbstractContainerScreen<?> handledScreen) {
            if (handledScreen.getMenu() instanceof ChestMenu container) {
                int containerSize = container.getRowCount() * 9;
                if (slotIndex < containerSize) {
                    if(loggingMode > 1)
                        ChestForensicsClient.LOGGER.info("single slot " + slotIndex + " update: " + stack.getHoverName().getString());
                }
            }
        }
    }
}