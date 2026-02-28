package com.duckyduck246.chestforensics;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.item.ItemStack;

import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChestForensicsClient implements ClientModInitializer {
    public static final String MOD_ID = "chest-forensics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static String containerName = "not yet";
    int containerID = 0;
    public static BlockPos detectedPos;
    public static Direction facing;
    public static ArrayList<ContainerInfo> allContainers = new ArrayList<ContainerInfo>();
    static String id;
    public static ArrayList<PuedoItem> compare = new ArrayList<>();
    boolean allAir;
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Identifier dimension;
    public static ArrayList<String> defaultTags = new ArrayList<>(List.of("all"));
    public static int loggingMode = 1;
    /*
    loggingMode 0: no logging except init;
    loggingMode 1: errors and init only;
    loggingMode 2: no repetitive logging (less lag);
    loggingMode 3: all logging;
    */

    @Override
    public void onInitializeClient(){
        LOGGER.info("Chest Forensics Mod Initialized :D");
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if(loggingMode > 0)
                LOGGER.info("loading container data");
            try {
                loadContainersFromJSON();
            } catch (IOException e) {
                if(loggingMode > 0)
                    LOGGER.error("failure! no load correctly! ", e);
            }
        });
        ScreenEvents.AFTER_INIT.register((minecraftClient, screen, i, i1) -> {
                containerName = screen.getTitle().getString();
                if (screen instanceof HandledScreen<?> handledScreen){
                    if (!(minecraftClient.crosshairTarget instanceof BlockHitResult blockHit)) {
                        if(loggingMode > 0)
                            LOGGER.info("may be opening an entity");
                        return;
                    }
                    if(loggingMode > 0)
                        LOGGER.info("DETECTED POS: " + detectedPos);
                    if(detectedPos == null){
                        if(loggingMode > 0)
                            LOGGER.info("DETECTED POS IS NULL");
                        return;
                    }
                    if(loggingMode > 0)
                        LOGGER.info("~~~CHEST OPENNENEND~~~");
                    MinecraftClient client = MinecraftClient.getInstance();
                    containerName = screen.getTitle().getString();
                    containerID = handledScreen.getScreenHandler().syncId;
                    ScreenHandler handler = handledScreen.getScreenHandler();


                    if (!(handler instanceof GenericContainerScreenHandler)) {
                        if(loggingMode > 0)
                            LOGGER.info("(handler instanceof GenericContainerScreenHandler)");
                        return;
                    }

                    GenericContainerScreenHandler g = (GenericContainerScreenHandler) handler;

                    if (g.getInventory() instanceof VehicleInventory) {
                        if(loggingMode > 0)
                            LOGGER.info("Detected vehicle inventory (Chest Boat/Minecart)");
                        return;
                    }

                    if (g.getInventory() instanceof Entity) {
                        if(loggingMode > 0)
                            LOGGER.info("may be opening an entity2");
                        return;
                    }

                    net.minecraft.text.Text screenTitley = client.currentScreen.getTitle();
                    String keyThing = "";
                    if (screenTitley.getContent() instanceof net.minecraft.text.TranslatableTextContent translatable) {
                        keyThing = translatable.getKey();
                    }
                    if(loggingMode > 0)
                        LOGGER.info("title: " + keyThing);
                    dimension = ContainerInfo.getDimension();
                    if(loggingMode > 0)
                        LOGGER.info("dimension set: " + dimension);

                    ScreenEvents.remove(screen).register(closedScreen -> {
                        if(loggingMode > 0) {
                            LOGGER.info("~~~CHEST CLOCLOLOSOSESESED~~~");
                            LOGGER.info("Name: " + containerName);
                            LOGGER.info("ID: " + containerID);
                        }

                        if (getIfDoubleChest(client.world.getBlockEntity(detectedPos))) {
                            if(loggingMode > 0)
                                LOGGER.info("is a large chest");
                            BlockPos mainContainer;
                            BlockPos subContainer;
                            if (client.world != null) {
                                mainContainer = getMainContainer(client.world.getBlockEntity(detectedPos));
                                subContainer = getSubContainer(client.world.getBlockEntity(detectedPos));
                                if(loggingMode > 0)
                                    LOGGER.info("Pos" + mainContainer);

                                addContainerInfo(containerName, mainContainer, ContainerInfo.listItems(2), defaultTags, subContainer, dimension);
                            }
                            else{
                                if(loggingMode > 0)
                                    LOGGER.info("ERROR: WORLD NOT INSTANTIATED");
                                addContainerInfo(containerName, detectedPos, ContainerInfo.listItems(2), defaultTags, dimension);
                            }
                        }
                        else {
                            if(loggingMode > 0)
                                LOGGER.info("Pos" + detectedPos);
                            addContainerInfo(containerName, detectedPos, ContainerInfo.listItems(2), defaultTags, dimension);
                        }
                        saveContainersToTXT();


                        detectedPos = null;

                    });
               }
        });



        ClientReceiveMessageEvents.CHAT.register((text, signedMessage, gameProfile, parameters, instant) -> {
            String message1 = text.getString();
            String sendername = gameProfile.name();
            if(loggingMode > 0)
                LOGGER.info("CHAT: " + message1);
            MinecraftClient client = MinecraftClient.getInstance();
            client.player.sendMessage(text, true);


        ;});
    }

    public static void capturePos(){
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.crosshairTarget instanceof BlockHitResult blockHit){
            detectedPos = blockHit.getBlockPos();
        }
    }

    public void addContainerInfo(String t, BlockPos p, ArrayList<ItemStack> i, ArrayList<String> a, Direction d, BlockPos o, Identifier b){
        String id = ContainerInfo.getID(t, p, d, o, b);
        for (int j = 0; j < allContainers.size(); j++){
            if(allContainers.get(j).id.equals(id)){
                allContainers.set(j, new ContainerInfo(t, p, i, a, o, b));
                return;
            }
        }
        allContainers.add(new ContainerInfo(t, p, i, a, o, b));
    }

    public void addContainerInfo(String t, BlockPos p, ArrayList<ItemStack> i, ArrayList<String> a, Direction d, Identifier b){
        String id = ContainerInfo.getID(t, p, d, b);
        for (int j = 0; j < allContainers.size(); j++){
            if(allContainers.get(j).id.equals(id)){
                allContainers.set(j, new ContainerInfo(t, p, i, a, b));
                return;
            }
        }
        allContainers.add(new ContainerInfo(t, p, i, a, b));
    }

    public void addContainerInfo(String t, BlockPos p, ArrayList<ItemStack> i, ArrayList<String> a, BlockPos o, Identifier b){
        String id = ContainerInfo.getID(t, p, o, b);
        for (int j = 0; j < allContainers.size(); j++){
            if(allContainers.get(j).id.equals(id)){
                allContainers.set(j, new ContainerInfo(t, p, i, a, o, b));
                return;
            }
        }
        allContainers.add(new ContainerInfo(t, p, i, a, o, b));
    }

    public void addContainerInfo(String t, BlockPos p, ArrayList<ItemStack> i, ArrayList<String> a, Identifier b){
        String id = ContainerInfo.getID(t, p, b);
        for (int j = 0; j < allContainers.size(); j++){
            if(allContainers.get(j).id.equals(id)){
                allContainers.set(j, new ContainerInfo(t, p, i, a, b));
                return;
            }
        }
        allContainers.add(new ContainerInfo(t, p, i, a, b));
    }

    public static ArrayList<PuedoItem> getCompare(){
        MinecraftClient client = MinecraftClient.getInstance();
        ArrayList<PuedoItem> compared = new ArrayList<>();
        if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
            ScreenHandler handler = handledScreen.getScreenHandler();
            id = "ERROR 1389843204";
            if(loggingMode > 0) {
                LOGGER.info("id set");
                LOGGER.info("" + Objects.requireNonNull(detectedPos));
            }


            if (getIfDoubleChest(client.world.getBlockEntity(detectedPos))) {
                if(loggingMode > 0)
                    LOGGER.info("is a large chest");
                BlockPos mainContainer;
                BlockPos subContainer;
                if (client.world != null) {
                    mainContainer = getMainContainer(client.world.getBlockEntity(detectedPos));
                    subContainer = getSubContainer(client.world.getBlockEntity(detectedPos));
                    id = ContainerInfo.getID(containerName, mainContainer, subContainer, dimension);
                } else {
                    if(loggingMode > 0)
                        LOGGER.info("ERROR: WORLD NOT INSTANTIATED");
                    id = ContainerInfo.getID(containerName, detectedPos, dimension);
                }
            } else {
                id = ContainerInfo.getID(containerName, detectedPos, dimension);
            }
            if(loggingMode > 0)
                LOGGER.info("got after geting id");
            for (int j = 0; j < allContainers.size(); j++) {
                if (allContainers.get(j).id.equals(id)) {
                    if(loggingMode > 2)
                        LOGGER.info("new stack:" + ContainerInfo.listItems(2));
                    compared = ContainerInfo.compareItems(ForensicsNbt.fromJsonString(allContainers.get(j).items), ContainerInfo.listItems(2));
                }
            }
            if(loggingMode > 2)
                LOGGER.info("returned compared: " + compared);
            return compared;
        }
        return compared;
    }

    public static void saveContainersToTXT(){
        try {
            saveContainersToJSON();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("chest-forensics");
        File file = configDir.resolve("chest_forensices_data.txt").toFile();
        file.getParentFile().mkdirs();
        try(FileWriter writer = new FileWriter(file, true)){
            writer.write("Chest Forensics Export\n");
            writer.write("World: " + getWorldId() + "\n");
            writer.write("Updated: " + java.time.LocalDateTime.now() + "\n\n");
            for(ContainerInfo container : allContainers){
                writer.write("Container Type: " + container.type  + "\n");
                writer.write("Pos: " + container.pos  + "\n");
                writer.write("ID: " + container.id  + "\n");
                writer.write("\n\n");
            }
            if(loggingMode > 0)
                LOGGER.info("exported to da txt");

        }
        catch (IOException e){
            if(loggingMode > 0)
                LOGGER.info("broke; not exported to da txt");
        }
    }
    
    public static void saveContainersToJSON() throws IOException {
    
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("chest-forensics");
        Files.createDirectories(configDir);
        File file = configDir.resolve(getWorldId() + "_allContainers.json").toFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(allContainers);
        Files.writeString(file.toPath(), json);
        if(loggingMode > 0)
            LOGGER.info("saved allContainers to da json named: " + getWorldId() + "_allContainers.json");
    
    }
    
    public static void loadContainersFromJSON() throws IOException {
    
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("chest-forensics");
        Files.createDirectories(configDir);
        File file = configDir.resolve(getWorldId() + "_allContainers.json").toFile();
        
        if(!file.exists()){
            if(loggingMode > 0)
                LOGGER.info("no saved container JSON found");
            return;
        }
        
        String json = Files.readString(file.toPath());
        ArrayList<ContainerInfo> loadedContainers = GSON.fromJson(json, new TypeToken<ArrayList<ContainerInfo>>() {}.getType());
        if(!(loadedContainers == null)){
            allContainers = loadedContainers;
            if(loggingMode > 0)
                LOGGER.info("loaded containers from json");
        }       
        else{
            if(loggingMode > 0)
                LOGGER.info("failed to load containers from json");
        }
        
    
    }

    public static BlockPos getMainContainer(BlockEntity blockEntity){
        if(loggingMode > 0)
            LOGGER.info("getMainContainer method called");
        if (!(blockEntity instanceof ChestBlockEntity chest)){
            if(loggingMode > 0)
                LOGGER.info("not a chest");
            return blockEntity.getPos();
        }
        ChestType type = chest.getCachedState().get(ChestBlock.CHEST_TYPE);
        if (type == ChestType.SINGLE || type == ChestType.LEFT){
            if(loggingMode > 0)
                LOGGER.info("single chest or left chest");
            return chest.getPos();
        }
        if (type == ChestType.RIGHT){
            if(loggingMode > 0)
                LOGGER.info("finnally is a right chest");
            World world = chest.getWorld();
            if (world != null){
                Direction chestFacing = chest.getCachedState().get(ChestBlock.FACING);
                BlockPos neighbor;
                if(loggingMode > 0)
                    LOGGER.info(chestFacing.asString());
                switch(chestFacing){
                    case NORTH: 
                        neighbor = chest.getPos().west();
                        break;
                    case SOUTH:
                        neighbor = chest.getPos().east();
                        break;
                    case WEST:
                        neighbor = chest.getPos().south();
                        break;
                    case EAST:
                        neighbor = chest.getPos().north();
                        break;
                    default:
                        neighbor = chest.getPos().up();
                        break;
                }
                BlockEntity neighborEntity = world.getBlockEntity(neighbor);
                if (neighborEntity instanceof ChestBlockEntity neighborChest){
                    ChestType neighborType = neighborChest.getCachedState().get(ChestBlock.CHEST_TYPE);
                    if (neighborType == ChestType.LEFT){
                        if(loggingMode > 0)
                            LOGGER.info("neighbor is a left chest, returning pos");
                        return neighborChest.getPos();
                    }
                    else{
                        if(loggingMode > 0)
                            LOGGER.info("neighbor not a left chest? they were: " + neighborType.asString());
                    }
                }
                else{
                    if(loggingMode > 0)
                        LOGGER.info("neighbor not a chest?");
                }
            }
        }

        return blockEntity.getPos();
    }


    public static boolean getIfDoubleChest(BlockEntity blockEntity){
        if(loggingMode > 0)
            LOGGER.info("getIfDoubleChest method called");
        if (!(blockEntity instanceof ChestBlockEntity chest)){
            if(loggingMode > 0)
                LOGGER.info("not a chest");
            return false;
        }
        ChestType type = chest.getCachedState().get(ChestBlock.CHEST_TYPE);
        if (type == ChestType.SINGLE){
            if(loggingMode > 0)
                LOGGER.info("single chest");
            return false;
        }

        return true;

    }

    public static BlockPos getSubContainer(BlockEntity blockEntity){
        if(loggingMode > 0)
            LOGGER.info("getSubContainer method called");
        if (!(blockEntity instanceof ChestBlockEntity chest)){
            if(loggingMode > 0)
                LOGGER.info("not a chest...");
            return blockEntity.getPos();
        }
        ChestType type = chest.getCachedState().get(ChestBlock.CHEST_TYPE);
        if (type == ChestType.SINGLE || type == ChestType.RIGHT){
            if(loggingMode > 0)
                LOGGER.info("single chest or right chest");
            return chest.getPos();
        }
        if (type == ChestType.LEFT){
            if(loggingMode > 0)
                LOGGER.info("finally is a left chest");
            World world = chest.getWorld();
            if (world != null){
                Direction chestFacing = chest.getCachedState().get(ChestBlock.FACING);
                BlockPos neighbor;
                if(loggingMode > 0)
                    LOGGER.info(chestFacing.asString());
                switch(chestFacing){
                    case SOUTH:
                        neighbor = chest.getPos().west();
                        break;
                    case NORTH:
                        neighbor = chest.getPos().east();
                        break;
                    case EAST:
                        neighbor = chest.getPos().south();
                        break;
                    case WEST:
                        neighbor = chest.getPos().north();
                        break;
                    default:
                        neighbor = chest.getPos().up();
                        break;
                }
                BlockEntity neighborEntity = world.getBlockEntity(neighbor);
                if (neighborEntity instanceof ChestBlockEntity neighborChest){
                    ChestType neighborType = neighborChest.getCachedState().get(ChestBlock.CHEST_TYPE);
                    if (neighborType == ChestType.RIGHT){
                        if(loggingMode > 0)
                            LOGGER.info("neighbor is a right chest, returning pos");
                        return neighborChest.getPos();
                    }
                    else{
                        if(loggingMode > 0)
                            LOGGER.info("neighbor not a right chest? they were: " + neighborType.asString());
                    }
                }
                else{
                    if(loggingMode > 0)
                        LOGGER.info("neighbor not a chest??");
                }
            }
        }

        return blockEntity.getPos();
    }

    public static String getWorldId() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isInSingleplayer()) {
            return client.getServer().getSavePath(WorldSavePath.ROOT).getParent().getFileName().toString();
        } else if (client.getCurrentServerEntry() != null) {
            return client.getCurrentServerEntry().address.replace(":", "_").replace("/", "_");
        }
        return "idk_either_man";
    }

}