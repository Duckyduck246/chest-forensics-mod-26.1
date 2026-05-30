package com.duckyduck246.chestforensics;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.fabricmc.api.ClientModInitializer;
import com.duckyduck246.chestforensics.ChestForensicsClient;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Objects;

import static com.duckyduck246.chestforensics.ChestForensicsClient.loggingMode;

public class ContainerInfo {
    
    public String type;
    public BlockPos pos;
    public ArrayList<String> items;
    public ArrayList<String> tags;
    public Direction dir;
    public String id;
    public static int total = 0;
    public boolean doubleChest;
    public BlockPos otherPos;
    public Identifier dimension;



    public ContainerInfo(String t, BlockPos p, ArrayList<ItemStack> i, ArrayList<String> a, Direction d, Identifier b){
        type = t;
        pos = p;
        items = ForensicsNbt.toJsonString(i);
        tags = a;
        dir = d;
        dimension = b;
        id = "containerId:" + type + pos.toString() + dir.toString() + dimension.toString();
        doubleChest = false;
        total++;
    }

    public ContainerInfo(String t, BlockPos p, ArrayList<ItemStack> i, ArrayList<String> a, Identifier b){
        type = t;
        pos = p;
        if(loggingMode > 2)
            ChestForensicsClient.LOGGER.info("before:" + i);
        items = ForensicsNbt.toJsonString(i);
        if(loggingMode > 2)
            ChestForensicsClient.LOGGER.info("after" + items);
        tags = a;
        dimension = b;
        id = "containerId:" + type + pos.toString() + dimension.toString();
        doubleChest = false;
        total++;
    }
    
    public ContainerInfo(String t, BlockPos p, ArrayList<ItemStack> i, ArrayList<String> a, Direction d, BlockPos o, Identifier b){
        type = t;
        pos = p;
        items = ForensicsNbt.toJsonString(i);
        tags = a;
        dir = d;
        otherPos = o;
        dimension = b;
        id = "containerId:" + type + pos.toString() + dir.toString() + otherPos.toString() + dimension.toString();
        doubleChest = true;
        total++;
    }

    public ContainerInfo(String t, BlockPos p, ArrayList<ItemStack> i, ArrayList<String> a, BlockPos o, Identifier b){
        type = t;
        pos = p;
        items = ForensicsNbt.toJsonString(i);
        tags = a;
        otherPos = o;
        dimension = b;
        id = "containerId:" + type + pos.toString() + otherPos.toString() + dimension.toString();
        doubleChest = true;
        total++;
    }
    
    public void logInfo(){
        if(loggingMode > 1) {
            ChestForensicsClient.LOGGER.info("type: " + type);
            ChestForensicsClient.LOGGER.info("tags: " + tags);
            ChestForensicsClient.LOGGER.info("direction: " + dir);
            ChestForensicsClient.LOGGER.info(id);
            ChestForensicsClient.LOGGER.info("isDoubleChest? " + doubleChest);
            ChestForensicsClient.LOGGER.info("pos: " + pos);
        }
        if(doubleChest){
            if(loggingMode > 1)
                ChestForensicsClient.LOGGER.info("other pos: " + id);
        }
        if(loggingMode > 2)
            ChestForensicsClient.LOGGER.info("items: " + items);
    }
    
    public void logTotal(){
        if(loggingMode > 1)
            ChestForensicsClient.LOGGER.info("" + total);
    }

    public static ArrayList<ItemStack> listItems(int mode){
        Minecraft client = Minecraft.getInstance();
        switch(mode) {
            case 1:
                if(client.player != null && client.player.containerMenu != null) {
                    if (client.screen instanceof AbstractContainerScreen<?> handledScreen) {
                        AbstractContainerMenu handler = handledScreen.getMenu();
                        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
                        if(loggingMode > 1)
                            ChestForensicsClient.LOGGER.info("slots: " + handler.slots.size());
                        for (int a = 0; a < handler.slots.size(); a++) {
                            ItemStack stack = handler.getSlot(a).getItem();
                            if (!(handler.getSlot(a).container instanceof Inventory)) {
                                items.add(stack.copy());
                                String nameOfItem = stack.getItem().getName().getString();
                                String dataOfItem = stack.getComponents().toString();
                                int count = stack.getCount();
                                if(loggingMode > 2)
                                    ChestForensicsClient.LOGGER.info(a + ": " + count + "x " + nameOfItem + "      (" + dataOfItem + ")");
                            }
                            else {
                            }
                        }
                        if(loggingMode > 1)
                            ChestForensicsClient.LOGGER.info("returned items");
                        return items;
                    }
                    else{
                        if(loggingMode > 0)
                            ChestForensicsClient.LOGGER.info("current screen is not an instance of handled screen");
                    }
                }
                else{
                    if(loggingMode > 0)
                        ChestForensicsClient.LOGGER.info("client.player or client.player.currentScreenHandler is null");
                }
                break;
            case 2:
                if(client.player != null && client.player.containerMenu != null) {
                    if (client.screen instanceof AbstractContainerScreen<?> handledScreen) {
                        AbstractContainerMenu handler = handledScreen.getMenu();
                        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
                        for (int a = 0; a < handler.slots.size(); a++) {
                            ItemStack stack = handler.getSlot(a).getItem();
                            if (!(handler.getSlot(a).container instanceof Inventory)) {
                                items.add(stack.copy());
                                String nameOfItem = stack.getItem().getName().getString();
                                String dataOfItem = stack.getComponents().toString();
                                int count = stack.getCount();
                            }
                            else {
                            }
                        }
                        return items;
                    }
                }
                break;

        }
        return null;
    }

    public static ArrayList<PuedoItem> compareItems(ArrayList<ItemStack> oldStack, ArrayList<ItemStack> currentStack){
        if(loggingMode > 1)
            ChestForensicsClient.LOGGER.info("compareItems method called");
        ArrayList<PuedoItem> diff = new ArrayList<>();
        if(loggingMode > 2) {
            ChestForensicsClient.LOGGER.info("old contents: " + oldStack);
            ChestForensicsClient.LOGGER.info("new contents: " + currentStack);
        }
        if(loggingMode > 1) {
            ChestForensicsClient.LOGGER.info("old size: " + oldStack.size());
            ChestForensicsClient.LOGGER.info("new size: " + currentStack.size());
        }

        for(int i = 0; i < oldStack.size(); i++){
            if(i < currentStack.size()){
                ItemStack itemStackA = oldStack.get(i);
                ItemStack itemStackB = currentStack.get(i);
                String stackA = ForensicsNbt.toJsonString(itemStackA);
                String stackB = ForensicsNbt.toJsonString(itemStackB);
                int countA = itemStackA.getCount();
                int countB = itemStackB.getCount();
                if(!((stackA.equals(stackB)) && (countA == countB))) {
                    if (stackA.equals(stackB)) {
                        if(loggingMode > 2)
                            ChestForensicsClient.LOGGER.info("index: " + i);
                        PuedoItem itemStack = new PuedoItem(itemStackB.getCount() - itemStackA.getCount(), itemStackA.getComponents(), itemStackA.getHoverName().getString(), stackA);
                        diff.add(itemStack);
                    } else {
                        if(loggingMode > 2) {
                            ChestForensicsClient.LOGGER.info("index: " + i);
                            ChestForensicsClient.LOGGER.info(itemStackB.getComponents().toString());
                        }
                        PuedoItem itemStack1 = new PuedoItem(-itemStackA.getCount(), itemStackA.getComponents(), itemStackA.getHoverName().getString(), stackA);
                        diff.add(itemStack1);
                        PuedoItem itemStack2 = new PuedoItem(itemStackB.getCount(), itemStackB.getComponents(), itemStackB.getHoverName().getString(), stackB);
                        diff.add(itemStack2);
                    }
                }
            }
        }
        if(loggingMode > 2)
            ChestForensicsClient.LOGGER.info("returned diff: " + diff);
        return diff;
    }

    public static String getID(String t, BlockPos p, Direction d, Identifier b){
        return "containerId:" + t + p.toString() + d.toString() + b.toString();
    }

    public static String getID(String t, BlockPos p, Identifier b){
        return "containerId:" + t + p.toString() + b.toString();
    }

    public static String getID(String t, BlockPos p, BlockPos o, Identifier b){
        return "containerId:" + t + p.toString() + o.toString() + b.toString();
    }

    public static String getID(String t, BlockPos p, Direction d, BlockPos o, Identifier b){
        return "containerId:" + t + p.toString() + d.toString() + o.toString() + b.toString();
    }
    
    public static Identifier getDimension(){
        Minecraft client = Minecraft.getInstance();
        Level world = client.level;
        if(world != null){
            Identifier dimensionId = world.dimension().identifier();
            if(loggingMode > 1)
                ChestForensicsClient.LOGGER.info("dimension: " + dimensionId);
            return dimensionId;
        }
        return null;
    }
}
