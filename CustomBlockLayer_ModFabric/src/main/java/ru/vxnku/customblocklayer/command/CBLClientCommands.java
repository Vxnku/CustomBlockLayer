package ru.vxnku.customblocklayer.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import ru.vxnku.customblocklayer.cache.BlockOverrideCache;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;

import java.util.Collection;

public class CBLClientCommands {

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("cbl")
                    .then(
                        ClientCommandManager.literal("set")
                            .then(
                                ClientCommandManager.argument("customId", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        Collection<CustomBlockDefinition> defs = CustomBlockRegistry.getAllDefinitions();
                                        return CommandSource.suggestMatching(defs.stream().map(CustomBlockDefinition::getId), builder);
                                    })
                                    .executes(context -> {
                                        String customId = StringArgumentType.getString(context, "customId");
                                        FabricClientCommandSource source = context.getSource();
                                        MinecraftClient client = source.getClient();

                                        CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
                                        if (def == null) {
                                            source.sendError(Text.literal("§c[CBL] Определение '" + customId + "' не найдено!"));
                                            return 0;
                                        }

                                        HitResult crosshairTarget = client.crosshairTarget;
                                        if (crosshairTarget instanceof BlockHitResult blockHit && crosshairTarget.getType() == HitResult.Type.BLOCK) {
                                            BlockPos pos = blockHit.getBlockPos();
                                            if (client.world != null) {
                                                BlockState state = client.world.getBlockState(pos);
                                                Identifier blockId = Registries.BLOCK.getId(state.getBlock());
                                                if (!def.matchesBlock(blockId)) {
                                                    source.sendError(Text.literal("§c[CBL] Блок " + blockId + " не подходит для '" + customId + "'! Поддерживаются: " + def.getMatchBlocks()));
                                                    return 0;
                                                }
                                            }

                                            BlockOverrideCache.set(pos, customId);
                                            source.sendFeedback(Text.literal("§a[CBL] Установлен кастомный блок '" + customId + "' на позиции " + pos.toShortString()));
                                            return 1;
                                        } else {
                                            source.sendError(Text.literal("§c[CBL] Вы должны смотреть на блок!"));
                                            return 0;
                                        }
                                    })
                            )
                    )
                    .then(
                        ClientCommandManager.literal("clear")
                            .executes(context -> {
                                FabricClientCommandSource source = context.getSource();
                                MinecraftClient client = source.getClient();

                                HitResult crosshairTarget = client.crosshairTarget;
                                if (crosshairTarget instanceof BlockHitResult blockHit && crosshairTarget.getType() == HitResult.Type.BLOCK) {
                                    BlockPos pos = blockHit.getBlockPos();
                                    BlockOverrideCache.remove(pos);
                                    source.sendFeedback(Text.literal("§e[CBL] Очищен кастомный блок на позиции " + pos.toShortString()));
                                    return 1;
                                } else {
                                    source.sendError(Text.literal("§c[CBL] Вы должны смотреть на блок!"));
                                    return 0;
                                }
                            })
                    )
                    .then(
                        ClientCommandManager.literal("list")
                            .executes(context -> {
                                FabricClientCommandSource source = context.getSource();
                                Collection<CustomBlockDefinition> defs = CustomBlockRegistry.getAllDefinitions();
                                source.sendFeedback(Text.literal("§6[CBL] Загружено кастомных блоков: " + defs.size()));
                                for (CustomBlockDefinition def : defs) {
                                    source.sendFeedback(Text.literal(" §7- §b" + def.getId() + " §8(textures: " + def.getAllReferencedTextures().size() + ", match: " + def.getMatchBlocks() + ")"));
                                }
                                return defs.size();
                            })
                    )
                    .then(
                        ClientCommandManager.literal("count")
                            .executes(context -> {
                                FabricClientCommandSource source = context.getSource();
                                int count = BlockOverrideCache.size();
                                source.sendFeedback(Text.literal("§6[CBL] Активных кастомных блоков в памяти клиента: §e" + count));
                                return count;
                            })
                    )
                    .then(
                        ClientCommandManager.literal("clearall")
                            .executes(context -> {
                                FabricClientCommandSource source = context.getSource();
                                BlockOverrideCache.clear();
                                if (source.getClient().worldRenderer != null) {
                                    source.getClient().worldRenderer.reload();
                                }
                                source.sendFeedback(Text.literal("§e[CBL] Все кастомные блоки очищены из памяти."));
                                return 1;
                            })
                    )
            );
        });
    }
}
