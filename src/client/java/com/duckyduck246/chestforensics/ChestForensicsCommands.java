package com.duckyduck246.chestforensics;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.io.IOException;


public class ChestForensicsCommands {
    public static int reset = 0;
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                dispatcher.register(
                        ClientCommands.literal("chestforensics")
                                .then(ClientCommands.literal("logmode")
                                        .executes(context -> {
                                            reset = 0;
                                            int l = ChestForensicsClient.loggingMode;
                                            Component text = Component.literal("Current logging mode: " + l + " - " + ChestForensicsClient.loggingDefine[l]);
                                            Component feedback = text.copy().withStyle(ChatFormatting.YELLOW);
                                            context.getSource().sendFeedback(feedback);
                                            return l;
                                        })
                                        .then(ClientCommands.argument("mode", IntegerArgumentType.integer(0, 3))
                                                .executes(context -> {
                                                    reset = 0;
                                                    int level = IntegerArgumentType.getInteger(context, "mode");
                                                    ChestForensicsClient.loggingMode = level;
                                                    Component text = Component.literal("Logging mode set to: " + level + " - " + ChestForensicsClient.loggingDefine[level]);
                                                    Component feedback = text.copy().withStyle(ChatFormatting.YELLOW);
                                                    context.getSource().sendFeedback(feedback);
                                                    return level;
                                                })
                                        )
                                )
                                .then(ClientCommands.literal("reset")
                                        .then(ClientCommands.literal("all")
                                                .executes(context -> {
                                                    if(reset == 0) {
                                                        reset = 1;
                                                        Component text = Component.literal("Are you sure? This will reset and clear all container data you have stored in this world, and the mod won't be able to detect any changes from before the reset. Redo the command to confirm.\n");
                                                        Component text2 = Component.literal("\n-NOTICE-\n").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                                                        Component feedback = text2.copy().append(text.copy().withStyle(ChatFormatting.GOLD));
                                                        context.getSource().sendFeedback(feedback);

                                                    }
                                                    else {
                                                        try {
                                                            ChestForensicsClient.deleteALLContainersInJSON();
                                                        } catch (IOException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                        
                                                        Component text = Component.literal("All container information from this world has been deleted");
                                                        Component feedback = text.copy().withStyle(ChatFormatting.YELLOW);
                                                        context.getSource().sendFeedback(feedback);
                                                        reset = 0;
                                                    }
                                                    return reset;
                                                })
                                        )
                                )
                );
        });
    }

}
