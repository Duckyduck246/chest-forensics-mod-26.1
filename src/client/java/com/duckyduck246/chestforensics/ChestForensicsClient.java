package com.duckyduck246.chestforensics;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.format.DateTimeFormatter;
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
    public static String[] loggingDefine = {"No Logging", "Errors Only (default)", "Debug Logging", "Heavy Debug Logging (may cause lag)"};
    /*
    loggingMode 0: no logging except init;
    loggingMode 1: errors and init only;
    loggingMode 2: no repetitive logging (less lag);
    loggingMode 3: all logging;
    */


    @Override
    public void onInitializeClient(){
        ChestForensicsCommands.register();
        LOGGER.info("Chest Forensics Mod Initialized :D");
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if(loggingMode > 1)
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
                if (screen instanceof AbstractContainerScreen<?> handledScreen){
                    if (!(minecraftClient.hitResult instanceof BlockHitResult blockHit)) {
                        if(loggingMode > 1)
                            LOGGER.info("may be opening an entity");
                        return;
                    }
                    if(loggingMode > 1)
                        LOGGER.info("DETECTED POS: " + detectedPos);
                    if(detectedPos == null){
                        if(loggingMode > 0)
                            LOGGER.info("DETECTED POS IS NULL");
                        return;
                    }
                    if(loggingMode > 1)
                        LOGGER.info("~~~CHEST OPENNENEND~~~");
                    Minecraft client = Minecraft.getInstance();
                    containerName = screen.getTitle().getString();
                    containerID = handledScreen.getMenu().containerId;
                    AbstractContainerMenu handler = handledScreen.getMenu();


                    if (!(handler instanceof ChestMenu)) {
                        if(loggingMode > 1)
                            LOGGER.info("(handler instanceof GenericContainerScreenHandler)");
                        return;
                    }

                    ChestMenu g = (ChestMenu) handler;

                    if (g.getContainer() instanceof ContainerEntity) {
                        if(loggingMode > 1)
                            LOGGER.info("Detected vehicle inventory (Chest Boat/Minecart)");
                        return;
                    }

                    if (g.getContainer() instanceof Entity) {
                        if(loggingMode > 1)
                            LOGGER.info("may be opening an entity2");
                        return;
                    }

                    net.minecraft.network.chat.Component screenTitley = client.screen.getTitle();
                    String keyThing = "";
                    if (screenTitley.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
                        keyThing = translatable.getKey();
                    }
                    if(loggingMode > 1)
                        LOGGER.info("title: " + keyThing);
                    dimension = ContainerInfo.getDimension();
                    if(loggingMode > 1)
                        LOGGER.info("dimension set: " + dimension);

                    ScreenEvents.remove(screen).register(closedScreen -> {
                        if(loggingMode > 1) {
                            LOGGER.info("~~~CHEST CLOCLOLOSOSESESED~~~");
                            LOGGER.info("Name: " + containerName);
                            LOGGER.info("ID: " + containerID);
                        }

                        if (getIfDoubleChest(client.level.getBlockEntity(detectedPos))) {
                            if(loggingMode > 1)
                                LOGGER.info("is a large chest");
                            BlockPos mainContainer;
                            BlockPos subContainer;
                            if (client.level != null) {
                                mainContainer = getMainContainer(client.level.getBlockEntity(detectedPos));
                                subContainer = getSubContainer(client.level.getBlockEntity(detectedPos));
                                if(loggingMode > 1)
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
                            if(loggingMode > 1)
                                LOGGER.info("Pos" + detectedPos);
                            addContainerInfo(containerName, detectedPos, ContainerInfo.listItems(2), defaultTags, dimension);
                        }
                        //saveContainersToTXT();


                        detectedPos = null;

                    });
               }
        });



        ClientReceiveMessageEvents.CHAT.register((text, signedMessage, gameProfile, parameters, instant) -> {
            String message1 = text.getString();
            String sendername = gameProfile.name();
            if(loggingMode > 1)
                LOGGER.info("CHAT: " + message1);
            Minecraft client = Minecraft.getInstance();
            //client.player.sendMessage(text, true);


        ;});
    }

    public static void capturePos(){
        Minecraft client = Minecraft.getInstance();
        if (client.hitResult instanceof BlockHitResult blockHit){
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
        Minecraft client = Minecraft.getInstance();
        ArrayList<PuedoItem> compared = new ArrayList<>();
        if (client.screen instanceof AbstractContainerScreen<?> handledScreen) {
            AbstractContainerMenu handler = handledScreen.getMenu();
            id = "ERROR 1389843204";
            if(loggingMode > 1) {
                LOGGER.info("id set");
                LOGGER.info("" + Objects.requireNonNull(detectedPos));
            }


            if (getIfDoubleChest(client.level.getBlockEntity(detectedPos))) {
                if(loggingMode > 1)
                    LOGGER.info("is a large chest");
                BlockPos mainContainer;
                BlockPos subContainer;
                if (client.level != null) {
                    mainContainer = getMainContainer(client.level.getBlockEntity(detectedPos));
                    subContainer = getSubContainer(client.level.getBlockEntity(detectedPos));
                    id = ContainerInfo.getID(containerName, mainContainer, subContainer, dimension);
                } else {
                    if(loggingMode > 0)
                        LOGGER.info("ERROR: WORLD NOT INSTANTIATED");
                    id = ContainerInfo.getID(containerName, detectedPos, dimension);
                }
            } else {
                id = ContainerInfo.getID(containerName, detectedPos, dimension);
            }
            if(loggingMode > 1)
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
            if(loggingMode > 1)
                LOGGER.info("exported to da txt");

        }
        catch (IOException e){
            if(loggingMode > 0)
                LOGGER.info("broke; not exported to da txt");
        }
    }

    public static String exportContainersToTXT(){
        try {
            saveContainersToJSON();
        } catch (Exception e) {
            if(loggingMode > 0)
                LOGGER.info("broke; not exported to da txt");
            return("");
        }
        Path directory = FabricLoader.getInstance().getGameDir().resolve("chest-forensics-exports");
        File file = directory.resolve(getWorldId() + "-" + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS")) + ".txt").toFile();
        file.getParentFile().mkdirs();
        try(FileWriter writer = new FileWriter(file, false)){
            writer.write("Chest Forensics Export\n");
            writer.write("World: " + getWorldId() + "\n");
            writer.write("Updated: " + java.time.LocalDateTime.now() + "\n\n");
            for(ContainerInfo container : allContainers){
                writer.write("Container Type: " + container.type  + "\n");
                writer.write("Pos: " + container.pos  + "\n");
                writer.write("ID: " + container.id  + "\n");
                writer.write("\n\n");
            }
            if(loggingMode > 1)
                LOGGER.info("exported to da txt");
            return(directory.toString());

        }
        catch (IOException e){
            if(loggingMode > 0)
                LOGGER.info("broke; not exported to da txt");
            return("");
        }
    }
    
    public static void saveContainersToJSON() throws IOException {
    
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("chest-forensics");
        Files.createDirectories(configDir);
        File file = configDir.resolve(getWorldId() + "_allContainers.json").toFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(allContainers);
        Files.writeString(file.toPath(), json);
        if(loggingMode > 1)
            LOGGER.info("saved allContainers to da json named: " + getWorldId() + "_allContainers.json");
    
    }
    
    public static void loadContainersFromJSON() throws IOException {
    
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("chest-forensics");
        Files.createDirectories(configDir);
        File file = configDir.resolve(getWorldId() + "_allContainers.json").toFile();
        
        if(!file.exists()){
            if(loggingMode > 1)
                LOGGER.info("no saved container JSON found");
            return;
        }
        
        String json = Files.readString(file.toPath());
        ArrayList<ContainerInfo> loadedContainers = GSON.fromJson(json, new TypeToken<ArrayList<ContainerInfo>>() {}.getType());
        if(!(loadedContainers == null)){
            allContainers = loadedContainers;
            if(loggingMode > 1)
                LOGGER.info("loaded containers from json");
        }       
        else{
            if(loggingMode > 0)
                LOGGER.info("failed to load containers from json");
        }


    }

    public static void deleteALLContainersInJSON() throws IOException {
        ArrayList<ContainerInfo> emptyContainer;
        emptyContainer = new ArrayList<ContainerInfo>();
        allContainers = emptyContainer;
        saveContainersToJSON();
        if(loggingMode > 1)
            LOGGER.info("deleted ALL CONTAINER INFO");
    }

    public static BlockPos getMainContainer(BlockEntity blockEntity){
        if(loggingMode > 1)
            LOGGER.info("getMainContainer method called");
        if (!(blockEntity instanceof ChestBlockEntity chest)){
            if(loggingMode > 1)
                LOGGER.info("not a chest");
            return blockEntity.getBlockPos();
        }
        ChestType type = chest.getBlockState().getValue(ChestBlock.TYPE);
        if (type == ChestType.SINGLE || type == ChestType.LEFT){
            if(loggingMode > 1)
                LOGGER.info("single chest or left chest");
            return chest.getBlockPos();
        }
        if (type == ChestType.RIGHT){
            if(loggingMode > 1)
                LOGGER.info("finnally is a right chest");
            Level world = chest.getLevel();
            if (world != null){
                Direction chestFacing = chest.getBlockState().getValue(ChestBlock.FACING);
                BlockPos neighbor;
                if(loggingMode > 1)
                    LOGGER.info(chestFacing.getSerializedName());
                switch(chestFacing){
                    case NORTH: 
                        neighbor = chest.getBlockPos().west();
                        break;
                    case SOUTH:
                        neighbor = chest.getBlockPos().east();
                        break;
                    case WEST:
                        neighbor = chest.getBlockPos().south();
                        break;
                    case EAST:
                        neighbor = chest.getBlockPos().north();
                        break;
                    default:
                        neighbor = chest.getBlockPos().above();
                        break;
                }
                BlockEntity neighborEntity = world.getBlockEntity(neighbor);
                if (neighborEntity instanceof ChestBlockEntity neighborChest){
                    ChestType neighborType = neighborChest.getBlockState().getValue(ChestBlock.TYPE);
                    if (neighborType == ChestType.LEFT){
                        if(loggingMode > 1)
                            LOGGER.info("neighbor is a left chest, returning pos");
                        return neighborChest.getBlockPos();
                    }
                    else{
                        if(loggingMode > 1)
                            LOGGER.info("neighbor not a left chest? they were: " + neighborType.getSerializedName());
                    }
                }
                else{
                    if(loggingMode > 1)
                        LOGGER.info("neighbor not a chest?");
                }
            }
        }

        return blockEntity.getBlockPos();
    }


    public static boolean getIfDoubleChest(BlockEntity blockEntity){
        if(loggingMode > 1)
            LOGGER.info("getIfDoubleChest method called");
        if (!(blockEntity instanceof ChestBlockEntity chest)){
            if(loggingMode > 1)
                LOGGER.info("not a chest");
            return false;
        }
        ChestType type = chest.getBlockState().getValue(ChestBlock.TYPE);
        if (type == ChestType.SINGLE){
            if(loggingMode > 1)
                LOGGER.info("single chest");
            return false;
        }

        return true;

    }

    public static BlockPos getSubContainer(BlockEntity blockEntity){
        if(loggingMode > 1)
            LOGGER.info("getSubContainer method called");
        if (!(blockEntity instanceof ChestBlockEntity chest)){
            if(loggingMode > 1)
                LOGGER.info("not a chest...");
            return blockEntity.getBlockPos();
        }
        ChestType type = chest.getBlockState().getValue(ChestBlock.TYPE);
        if (type == ChestType.SINGLE || type == ChestType.RIGHT){
            if(loggingMode > 1)
                LOGGER.info("single chest or right chest");
            return chest.getBlockPos();
        }
        if (type == ChestType.LEFT){
            if(loggingMode > 1)
                LOGGER.info("finally is a left chest");
            Level world = chest.getLevel();
            if (world != null){
                Direction chestFacing = chest.getBlockState().getValue(ChestBlock.FACING);
                BlockPos neighbor;
                if(loggingMode > 1)
                    LOGGER.info(chestFacing.getSerializedName());
                switch(chestFacing){
                    case SOUTH:
                        neighbor = chest.getBlockPos().west();
                        break;
                    case NORTH:
                        neighbor = chest.getBlockPos().east();
                        break;
                    case EAST:
                        neighbor = chest.getBlockPos().south();
                        break;
                    case WEST:
                        neighbor = chest.getBlockPos().north();
                        break;
                    default:
                        neighbor = chest.getBlockPos().above();
                        break;
                }
                BlockEntity neighborEntity = world.getBlockEntity(neighbor);
                if (neighborEntity instanceof ChestBlockEntity neighborChest){
                    ChestType neighborType = neighborChest.getBlockState().getValue(ChestBlock.TYPE);
                    if (neighborType == ChestType.RIGHT){
                        if(loggingMode > 1)
                            LOGGER.info("neighbor is a right chest, returning pos");
                        return neighborChest.getBlockPos();
                    }
                    else{
                        if(loggingMode > 1)
                            LOGGER.info("neighbor not a right chest? they were: " + neighborType.getSerializedName());
                    }
                }
                else{
                    if(loggingMode > 1)
                        LOGGER.info("neighbor not a chest??");
                }
            }
        }

        return blockEntity.getBlockPos();
    }

    public static String getWorldId() {
        Minecraft client = Minecraft.getInstance();
        if (client.isLocalServer()) {
            return client.getSingleplayerServer()






                    .getWorldPath(LevelResource.ROOT).getParent().getFileName().toString();
        } else if (client.getCurrentServer() != null) {
            return client.getCurrentServer().ip.replace(":", "_").replace("/", "_");
        }
        return "idk_either_man";
    }

}