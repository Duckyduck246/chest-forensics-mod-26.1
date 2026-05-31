package com.duckyduck246.chestforensics;

import com.google.gson.*;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import static com.duckyduck246.chestforensics.ChestForensicsClient.loggingMode;

public class ForensicsNbt {

    public static ArrayList<String> toJsonString(ArrayList<ItemStack> stacks) {
        ArrayList<String> returned = new ArrayList<>();
        var registryManager = Minecraft.getInstance().getConnection().registryAccess();
        var ops = RegistryOps.create(JsonOps.INSTANCE, registryManager);
        for (ItemStack stack : stacks) {
            DataResult<JsonElement> result = ItemStack.CODEC.encodeStart(ops, stack);
            result.result().ifPresentOrElse(
                    json -> returned.add(normalize(json.toString())),
                    () -> returned.add("{}")
            );
        }
        return returned;
    }

    public static ArrayList<ItemStack> fromJsonString(ArrayList<String> stringJson) {
        ArrayList<ItemStack> returned = new ArrayList<>();
        var registryManager = Minecraft.getInstance().getConnection().registryAccess();
        var ops = RegistryOps.create(JsonOps.INSTANCE, registryManager);
        for (String jsonStr : stringJson) {
            try {
                JsonElement element = JsonParser.parseString(jsonStr);
                DataResult<ItemStack> result = ItemStack.CODEC.parse(ops, element);
                result.result().ifPresentOrElse(
                        returned::add,
                        () -> returned.add(ItemStack.EMPTY)
                );
            } catch (Exception e) {
                returned.add(ItemStack.EMPTY);            }
        }
        return returned;
    }
    
    public static String toJsonString(ItemStack stack) {
        var registryManager = Minecraft.getInstance().getConnection().registryAccess();
        var ops = RegistryOps.create(JsonOps.INSTANCE, registryManager);
        DataResult<JsonElement> result = ItemStack.CODEC.encodeStart(ops, stack);
        String output = result.result().map(e -> normalize(e.toString())).orElse("{}");
        if (loggingMode > 1)
            ChestForensicsClient.LOGGER.info("successfully converted: " + stack + " to json " + output);
        return output;
    }

    public static ItemStack fromJsonString(String stringJson) {
        try{
            var registryManager = Minecraft.getInstance().getConnection().registryAccess();
            var ops = RegistryOps.create(JsonOps.INSTANCE, registryManager);
            JsonElement element = JsonParser.parseString(stringJson);
            DataResult<ItemStack> result = ItemStack.CODEC.parse(ops, element);
            return result.result().orElse(ItemStack.EMPTY);
            
        } catch(Exception e) {
            if (loggingMode > 0)
                ChestForensicsClient.LOGGER.info("failed to get itemstack from json string, exception: " + e);
            return ItemStack.EMPTY;
        }
    }
    public static String normalize(String json){
        JsonElement root = JsonParser.parseString(json);
        if(loggingMode > 2)
            ChestForensicsClient.LOGGER.info("before sorting: " + norm(root));
        JsonElement normalized = sort(norm(root));
        if(loggingMode > 2)
            ChestForensicsClient.LOGGER.info("after sorting: " + normalized);
        return normalized.toString();
    }
    public static JsonElement norm(JsonElement json){
        if(json.isJsonObject()){
            JsonObject object = new JsonObject();
            for(var thing : json.getAsJsonObject().entrySet()){
                object.add(thing.getKey(), norm(thing.getValue()));
            }
            return object;
        }
        if(json.isJsonArray()){
            JsonArray array = new JsonArray();
            for(var thing : json.getAsJsonArray()){
                array.add(norm(thing));
            }
            return array;
        }
        if(json.isJsonPrimitive()){
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            if(primitive.isNumber()){
                double v = primitive.getAsDouble();

                double nearestTenth = Math.round(v * 10.0) / 10.0;
                if (Math.abs(primitive.getAsInt() - v) < 0.0000001) {
                    v = (int)v;
                }

                boolean temp = false;
                if (Math.abs(nearestTenth - v) < 0.0000001 && !(Math.abs(nearestTenth - v) == 0)) {
                    v = nearestTenth;
                    temp = true;
                }
                if (v == Math.rint(v)) {
                    return new JsonPrimitive((long) v);
                }
                else if(temp){
                    return new JsonPrimitive(v);
                }
            }

            return primitive;
        }
        return json;

    }
    public static JsonElement sort(JsonElement json){
        if(json.isJsonObject()){
            JsonObject input = json.getAsJsonObject();
            JsonObject output = new JsonObject();
            input.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry -> output.add(entry.getKey(), sort(entry.getValue())));
            return output;
        }
        if(json.isJsonArray()){
            JsonArray input = json.getAsJsonArray();
            JsonArray output = new JsonArray();
            for(JsonElement element : input){
                output.add(sort(element));
            }
            return output;
        }
        return json;
    }
}
