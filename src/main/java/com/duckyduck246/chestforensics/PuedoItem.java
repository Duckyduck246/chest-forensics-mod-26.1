package com.duckyduck246.chestforensics;
import net.fabricmc.api.ClientModInitializer;

import com.duckyduck246.chestforensics.ChestForensicsClient;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.item.ItemStack;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Objects;

import net.minecraft.client.gui.screen.ingame.HandledScreen;

import static com.duckyduck246.chestforensics.ChestForensicsClient.loggingMode;

public class PuedoItem {
    public final int count;
    public final String name;
    public final String itemComponents;
    public final String nbt;
    public PuedoItem(int c, ComponentMap m, String n, String a){
        count = c;
        itemComponents = m.toString();
        name = n;
        nbt = a;
        if(loggingMode > 2)
            ChestForensicsClient.LOGGER.info("new PuedoItem created: count: " + c + "componets: " + m + "nbt: " + a);
    }
    public String getString(){
        return count + "x " + name +  "            " + itemComponents.toString() + "             " + nbt;
    }
    public Boolean isEmpty(){
        if(itemComponents == null || name.equals("Air")){
            return true;
        }
        return false;
    }
}
