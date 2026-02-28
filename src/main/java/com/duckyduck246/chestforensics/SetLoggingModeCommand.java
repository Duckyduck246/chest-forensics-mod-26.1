package com.duckyduck246.chestforensics;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;


public class SetLoggingModeCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("chestforensics")
                            .then(ClientCommandManager.literal("logmode")
                                .executes(context -> {
                                    int l = ChestForensicsClient.loggingMode;
                                    Text feedback = Text.literal("Current logging mode: " + l);
                                    context.getSource().sendFeedback(feedback);
                                    return l;
                                })
                                .then(ClientCommandManager.argument("mode", IntegerArgumentType.integer(0, 4))
                                        .executes(context -> {
                                            int level = IntegerArgumentType.getInteger(context, "mode");
                                            ChestForensicsClient.loggingMode = level;
                                            Text feedback = Text.literal("Logging mode set to: " + level);
                                            context.getSource().sendFeedback(feedback);
                                            return level;
                                        })
                                )
                            )
            );
        });
    }

}
