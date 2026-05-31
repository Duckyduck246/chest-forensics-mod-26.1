package com.duckyduck246.chestforensics;
import net.fabricmc.api.ClientModInitializer;

import com.duckyduck246.chestforensics.ChestForensicsClient;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Objects;

import static com.duckyduck246.chestforensics.ChestForensicsClient.loggingMode;

public class PuedoItem {
    public final int count;
    public final String name;
    public final String itemComponents;
    public final String nbt;
    public PuedoItem(int c, DataComponentMap m, String n, String a){
        count = c;
        itemComponents = m.toString();
        name = n;
        nbt = a;
        if(loggingMode > 2)
            ChestForensicsClient.LOGGER.info("new PuedoItem created: count: " + c + "componets: " + m + "nbt: " + a);
    }
    public String toString(){
        return count + "x " + name +  "            " + itemComponents.toString() + "             " + nbt;
    }
    public Boolean isEmpty(){
        if(itemComponents == null || name.equals("Air")){
            return true;
        }
        return false;
    }
}
