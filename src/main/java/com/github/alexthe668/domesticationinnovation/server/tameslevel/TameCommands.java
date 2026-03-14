package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.TLMigrationImportData;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.misc.DITameProgressData;
import com.github.alexthe668.domesticationinnovation.server.misc.LanternRequest;
import com.github.alexthe668.domesticationinnovation.server.item.DIItemRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.TameClass;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAbilityEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDeathRecord;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TLAdminRuntimeSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameTransferService;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.registries.ForgeRegistries;
import com.github.alexthe668.domesticationinnovation.server.misc.DIWorldData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public class TameCommands {
    private static final UUID COLLAR_ARMOR_UUID = UUID.fromString("e6e52fdd-8e14-4c0d-9ac1-8fbc60f3dd01");
    private static final UUID COLLAR_ARMOR_TOUGHNESS_UUID = UUID.fromString("f2f6c7ab-8a73-4d1c-95e4-07f171ddca8f");
    private static final String DOC_RESOURCE_BASE = "assets/domesticationinnovation/tameslevel/old docus/";
    private static final Path DOC_SOURCE_BASE = Path.of("src", "main", "java", "com", "github", "alexthe668", "domesticationinnovation", "server", "tameslevel", "old docus");
    private static final Pattern LEVEL_PREFIX_PATTERN = Pattern.compile("^\\s*\\[(?:(?:lvl|level)\\s*)?\\d+\\]\\s*", Pattern.CASE_INSENSITIVE);
    private static final long DUEL_INVITE_TIMEOUT_MS = 120_000L;
    private static final Map<UUID, Map<UUID, DuelInvite>> DUEL_INVITES = new HashMap<>();
    private static final int UNLOADED_TP_TIMEOUT_TICKS = 1200;
    private static final int MORNING_LANTERN_TIMEOUT_TICKS = 200;
    private static final int MORNING_LANTERN_RADIUS = 64;
    private static final Map<UUID, PendingMorningLanternRecall> PENDING_MORNING_LANTERN = new HashMap<>();
    private static long lastMorningRegistrySweepDay = Long.MIN_VALUE;
    private static long tlMigrationLastScanned = 0L;
    private static long tlMigrationLastMatchedPayload = 0L;
    private static long tlMigrationLastMissingPayload = 0L;
    private static long tlMigrationLastAppliedNew = 0L;
    private static long tlMigrationLastAppliedUpdate = 0L;
    private static long tlMigrationLastSkippedStale = 0L;
    private static String tlMigrationLastMode = "none";
    private enum MovementOrder {
        FOLLOW,
        SIT,
        WANDER
    }

    private enum DuelSelectionKind {
        GROUP,
        TYPE,
        SINGLE,
        ALL
    }

    private static final class PendingMorningLanternRecall {
        private final UUID tameUuid;
        private final UUID tlId;
        private final UUID ownerUuid;
        private final ResourceKey<Level> sourceDimension;
        private final BlockPos sourcePos;
        private final ResourceKey<Level> targetDimension;
        private final BlockPos lanternPos;
        private final long createdTick;
        private final String tameName;

        private PendingMorningLanternRecall(UUID tameUuid, UUID tlId, UUID ownerUuid, ResourceKey<Level> sourceDimension, BlockPos sourcePos, ResourceKey<Level> targetDimension, BlockPos lanternPos, long createdTick, String tameName) {
            this.tameUuid = tameUuid;
            this.tlId = tlId;
            this.ownerUuid = ownerUuid;
            this.sourceDimension = sourceDimension;
            this.sourcePos = sourcePos;
            this.targetDimension = targetDimension;
            this.lanternPos = lanternPos;
            this.createdTick = createdTick;
            this.tameName = tameName;
        }
    }

    private static final class DuelSelection {
        private final DuelSelectionKind kind;
        private final String value;

        private DuelSelection(DuelSelectionKind kind, String value) {
            this.kind = kind;
            this.value = value == null ? "" : value.trim();
        }

        private static DuelSelection group(String name) {
            return new DuelSelection(DuelSelectionKind.GROUP, name);
        }

        private static DuelSelection type(String name) {
            return new DuelSelection(DuelSelectionKind.TYPE, name);
        }

        private static DuelSelection single(String name) {
            return new DuelSelection(DuelSelectionKind.SINGLE, name);
        }

        private static DuelSelection all() {
            return new DuelSelection(DuelSelectionKind.ALL, "");
        }
    }

    private static final class DuelSelectionResult {
        private final List<TamableAnimal> tames;
        private final String error;

        private DuelSelectionResult(List<TamableAnimal> tames, String error) {
            this.tames = tames;
            this.error = error == null ? "" : error;
        }

        private static DuelSelectionResult ok(List<TamableAnimal> tames) {
            return new DuelSelectionResult(tames, "");
        }

        private static DuelSelectionResult fail(String error) {
            return new DuelSelectionResult(List.of(), error);
        }
    }

    private static final class RecoverResult {
        private final TamableAnimal entity;
        private final String error;

        private RecoverResult(TamableAnimal entity, String error) {
            this.entity = entity;
            this.error = error;
        }

        private static RecoverResult ok(TamableAnimal entity) {
            return new RecoverResult(entity, "");
        }

        private static RecoverResult fail(String error) {
            return new RecoverResult(null, error == null ? "unknown error" : error);
        }
    }

    private static final class DeathHistoryRow {
        private final String tameName;
        private final int level;
        private final String message;
        private final String dimension;
        private final int x;
        private final int y;
        private final int z;
        private final long gameTime;
        private final long unixMillis;

        private DeathHistoryRow(String tameName, int level, String message, String dimension, int x, int y, int z, long gameTime, long unixMillis) {
            this.tameName = tameName == null || tameName.isBlank() ? "unknown" : tameName;
            this.level = Math.max(1, level);
            this.message = message == null ? "" : message;
            this.dimension = dimension == null ? "" : dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.gameTime = gameTime;
            this.unixMillis = unixMillis;
        }
    }

    private static final class UnloadedTpResult {
        private final boolean success;
        private final boolean queued;
        private final String error;

        private UnloadedTpResult(boolean success, boolean queued, String error) {
            this.success = success;
            this.queued = queued;
            this.error = error == null ? "" : error;
        }

        private static UnloadedTpResult queued() {
            return new UnloadedTpResult(true, true, "");
        }

        private static UnloadedTpResult fail(String error) {
            return new UnloadedTpResult(false, false, error);
        }
    }

    private static final class DuelInvite {
        private final UUID challengerUuid;
        private final UUID targetUuid;
        private final DuelSelection challengerSelection;
        private final long createdAtMs;

        private DuelInvite(UUID challengerUuid, UUID targetUuid, DuelSelection challengerSelection, long createdAtMs) {
            this.challengerUuid = challengerUuid;
            this.targetUuid = targetUuid;
            this.challengerSelection = challengerSelection;
            this.createdAtMs = createdAtMs;
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(
                Commands.literal("tames")
                        .executes(ctx -> list(ctx.getSource()))

                        .then(Commands.literal("list")
                                .executes(ctx -> list(ctx.getSource())))
                        .then(Commands.literal("loaded")
                                .executes(ctx -> loadedStatus(ctx.getSource())))

                        .then(Commands.literal("strongest")
                                .executes(ctx -> strongest(ctx.getSource())))

                        .then(Commands.literal("stat")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNamesAll(ctx.getSource(), b))
                                        .executes(ctx -> statLong(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNamesAll(ctx.getSource(), b))
                                        .executes(ctx -> inspectPet(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("search")
                                .then(Commands.literal("ability")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestAbilities(b))
                                                .executes(ctx -> searchTamesByAbility(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("attribute")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestAttributes(b))
                                                .executes(ctx -> searchTamesByAttribute(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("class")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestClasses(b))
                                                .executes(ctx -> searchTamesByClass(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))))
                        .then(Commands.literal("deaths")
                                .executes(ctx -> recentDeaths(ctx.getSource(), 10))
                                .then(Commands.argument("number", IntegerArgumentType.integer(1, 200))
                                        .executes(ctx -> recentDeaths(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "number")))))
                        .then(Commands.literal("show")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                .executes(ctx -> showStatsToPlayer(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "name")
                                                )))))
                        .then(Commands.literal("duel")
                                .then(Commands.literal("vs")
                                        .then(Commands.literal("group")
                                                .then(Commands.argument("left", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                        .then(Commands.literal("group")
                                                                .then(Commands.argument("right", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "right"))
                                                                        ))))
                                                        .then(Commands.literal("type")
                                                                .then(Commands.argument("right", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "right"))
                                                                        ))))
                                                        .then(Commands.literal("name")
                                                                .then(Commands.argument("right", StringArgumentType.string())
                                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "right"))
                                                                        ))))))
                                        .then(Commands.literal("type")
                                                .then(Commands.argument("left", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                        .then(Commands.literal("group")
                                                                .then(Commands.argument("right", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "right"))
                                                                        ))))
                                                        .then(Commands.literal("type")
                                                                .then(Commands.argument("right", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "right"))
                                                                        ))))
                                                        .then(Commands.literal("name")
                                                                .then(Commands.argument("right", StringArgumentType.string())
                                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "right"))
                                                                        ))))))
                                        .then(Commands.literal("name")
                                                .then(Commands.argument("left", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                        .then(Commands.literal("group")
                                                                .then(Commands.argument("right", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "right"))
                                                                        ))))
                                                        .then(Commands.literal("type")
                                                                .then(Commands.argument("right", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "right"))
                                                                        ))))
                                                        .then(Commands.literal("name")
                                                                .then(Commands.argument("right", StringArgumentType.string())
                                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "right"))
                                                                        )))))))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("group", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .executes(ctx -> duelAcceptQuick(
                                                        ctx.getSource(),
                                                        DuelSelection.group(StringArgumentType.getString(ctx, "group"))
                                                ))
                                                .then(Commands.argument("player", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                                        .executes(ctx -> duelInviteSelection(
                                                                ctx.getSource(),
                                                                DuelSelection.group(StringArgumentType.getString(ctx, "group")),
                                                                StringArgumentType.getString(ctx, "player")
                                                        )))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .executes(ctx -> duelAcceptQuick(
                                                        ctx.getSource(),
                                                        DuelSelection.type(StringArgumentType.getString(ctx, "type"))
                                                ))
                                                .then(Commands.argument("player", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                                        .executes(ctx -> duelInviteSelection(
                                                                ctx.getSource(),
                                                                DuelSelection.type(StringArgumentType.getString(ctx, "type")),
                                                                StringArgumentType.getString(ctx, "player")
                                                        )))))
                                .then(Commands.literal("all")
                                        .executes(ctx -> duelAcceptQuick(ctx.getSource(), DuelSelection.all()))
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                                .executes(ctx -> duelInviteSelection(
                                                        ctx.getSource(),
                                                        DuelSelection.all(),
                                                        StringArgumentType.getString(ctx, "player")
                                                ))))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                        .executes(ctx -> duelAcceptQuick(
                                                ctx.getSource(),
                                                DuelSelection.single(StringArgumentType.getString(ctx, "name"))
                                        ))
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                                .executes(ctx -> duelInviteSelection(
                                                        ctx.getSource(),
                                                        DuelSelection.single(StringArgumentType.getString(ctx, "name")),
                                                        StringArgumentType.getString(ctx, "player")
                                                ))))
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                                .executes(ctx -> duelInviteSelection(
                                                        ctx.getSource(),
                                                        DuelSelection.group(StringArgumentType.getString(ctx, "group")),
                                                        StringArgumentType.getString(ctx, "player")
                                                ))))
                                .then(Commands.literal("accept")
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestIncomingDuelChallengers(ctx.getSource(), b))
                                                .then(Commands.literal("group")
                                                        .then(Commands.argument("group", StringArgumentType.word())
                                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                                .executes(ctx -> duelAcceptSelection(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        DuelSelection.group(StringArgumentType.getString(ctx, "group"))
                                                                ))))
                                                .then(Commands.literal("type")
                                                        .then(Commands.argument("type", StringArgumentType.word())
                                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                                .executes(ctx -> duelAcceptSelection(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        DuelSelection.type(StringArgumentType.getString(ctx, "type"))
                                                                ))))
                                                .then(Commands.literal("all")
                                                        .executes(ctx -> duelAcceptSelection(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "player"),
                                                                DuelSelection.all()
                                                        )))
                                                .then(Commands.argument("name", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                        .executes(ctx -> duelAcceptSelection(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "player"),
                                                                DuelSelection.single(StringArgumentType.getString(ctx, "name"))
                                                        )))
                                                .then(Commands.argument("group", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                        .executes(ctx -> duelAcceptSelection(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "player"),
                                                                DuelSelection.group(StringArgumentType.getString(ctx, "group"))
                                                        )))))
                                .then(Commands.literal("decline")
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestIncomingDuelChallengers(ctx.getSource(), b))
                                                .executes(ctx -> duelDecline(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player")
                                                ))))
                                .then(Commands.literal("ff")
                                        .executes(ctx -> duelForfeit(ctx.getSource())))
                                .then(Commands.literal("inbox")
                                        .executes(ctx -> duelInbox(ctx.getSource()))))

                        .then(Commands.literal("info")
                                .executes(ctx -> infoOverview(ctx.getSource()))
                                .then(Commands.literal("attribute")
                                        .executes(ctx -> infoDetail(ctx.getSource(), "attribute"))
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestAttributes(b))
                                                .executes(ctx -> infoAttribute(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name")
                                                ))))
                                .then(Commands.literal("ability")
                                        .executes(ctx -> infoDetail(ctx.getSource(), "ability"))
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestAbilities(b))
                                                .executes(ctx -> infoAbility(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name")
                                                ))))
                                .then(Commands.literal("class")
                                        .executes(ctx -> infoDetail(ctx.getSource(), "class"))
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestClasses(b))
                                                .executes(ctx -> infoClass(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name")
                                                ))))
                                .then(Commands.argument("command", StringArgumentType.word())
                                        .executes(ctx -> infoDetail(ctx.getSource(), StringArgumentType.getString(ctx, "command")))))

                        .then(Commands.literal("leaderboard")
                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, 10))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, IntegerArgumentType.getInteger(ctx, "limit"))))
                                .then(Commands.literal("everytame")
                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, Integer.MAX_VALUE)))
                                .then(Commands.literal("all")
                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", true, null, 10))
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", true, null, IntegerArgumentType.getInteger(ctx, "limit"))))
                                        .then(Commands.literal("everytame")
                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", true, null, Integer.MAX_VALUE)))
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestLeaderboardTypes(b))
                                                .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), true, null, 10))
                                                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), true, null, IntegerArgumentType.getInteger(ctx, "limit"))))
                                                .then(Commands.literal("everytame")
                                                        .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), true, null, Integer.MAX_VALUE)))))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, StringArgumentType.getString(ctx, "name"), 10))
                                                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, StringArgumentType.getString(ctx, "name"), IntegerArgumentType.getInteger(ctx, "limit"))))
                                                .then(Commands.literal("everytame")
                                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, StringArgumentType.getString(ctx, "name"), Integer.MAX_VALUE)))))
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((ctx, b) -> suggestLeaderboardTypes(b))
                                        .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), false, null, 10))
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), false, null, IntegerArgumentType.getInteger(ctx, "limit"))))
                                        .then(Commands.literal("everytame")
                                                .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), false, null, Integer.MAX_VALUE)))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), true, null, 10))
                                                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), true, null, IntegerArgumentType.getInteger(ctx, "limit"))))
                                                .then(Commands.literal("everytame")
                                                        .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), true, null, Integer.MAX_VALUE))))))

                        .then(Commands.literal("follow")
                                .executes(ctx -> setMovementState(ctx.getSource(), true, MovementOrder.FOLLOW))
                                .then(Commands.literal("all")
                                        .executes(ctx -> setMovementStateAllLoaded(ctx.getSource(), MovementOrder.FOLLOW)))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                        .executes(ctx -> setPetMovementState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.FOLLOW)))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .executes(ctx -> groupMovementState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.FOLLOW))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .executes(ctx -> typeMovementState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.FOLLOW))))
                                .then(Commands.literal("state")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestMovementStates(b))
                                                .executes(ctx -> stateMovementState(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name"),
                                                        MovementOrder.FOLLOW
                                                )))))

                        .then(Commands.literal("sit")
                                .executes(ctx -> setMovementState(ctx.getSource(), true, MovementOrder.SIT))
                                .then(Commands.literal("all")
                                        .executes(ctx -> setMovementStateAllLoaded(ctx.getSource(), MovementOrder.SIT)))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                        .executes(ctx -> setPetMovementState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.SIT)))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .executes(ctx -> groupMovementState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.SIT))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .executes(ctx -> typeMovementState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.SIT))))
                                .then(Commands.literal("state")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestMovementStates(b))
                                                .executes(ctx -> stateMovementState(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name"),
                                                        MovementOrder.SIT
                                                )))))

                        .then(Commands.literal("wander")
                                .executes(ctx -> setMovementState(ctx.getSource(), true, MovementOrder.WANDER))
                                .then(Commands.literal("all")
                                        .executes(ctx -> setMovementStateAllLoaded(ctx.getSource(), MovementOrder.WANDER)))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                        .executes(ctx -> setPetMovementState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.WANDER)))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .executes(ctx -> groupMovementState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.WANDER))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .executes(ctx -> typeMovementState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.WANDER))))
                                .then(Commands.literal("state")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestMovementStates(b))
                                                .executes(ctx -> stateMovementState(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name"),
                                                        MovementOrder.WANDER
                                                )))))

                        .then(Commands.literal("tp")
                                .then(Commands.literal("all")
                                        .executes(ctx -> teleportAll(ctx.getSource()))
                                        .then(Commands.literal("dim")
                                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                        .executes(ctx -> teleportAllFromDimension(
                                                                ctx.getSource(),
                                                                DimensionArgument.getDimension(ctx, "dimension")
                                                        )))))
                                .then(Commands.literal("dim")
                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                        .executes(ctx -> teleportAllFromDimension(
                                                                ctx.getSource(),
                                                                DimensionArgument.getDimension(ctx, "dimension")
                                                        ))))
                                .then(Commands.literal("unloaded")
                                        .executes(ctx -> teleportUnloaded(ctx.getSource())))
                                .then(Commands.literal("follow")
                                        .executes(ctx -> teleportByMovementState(ctx.getSource(), MovementOrder.FOLLOW)))
                                .then(Commands.literal("sit")
                                        .executes(ctx -> teleportByMovementState(ctx.getSource(), MovementOrder.SIT)))
                                .then(Commands.literal("wander")
                                        .executes(ctx -> teleportByMovementState(ctx.getSource(), MovementOrder.WANDER)))
                                .then(Commands.literal("state")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestMovementStates(b))
                                                .executes(ctx -> teleportByState(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name")
                                                ))))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .executes(ctx -> groupTp(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .executes(ctx -> typeTp(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                        .executes(ctx -> teleportPet(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("respawn")
                                .then(Commands.literal("all")
                                        .executes(ctx -> respawnAll(ctx.getSource(), false))
                                        .then(Commands.literal("toMe")
                                                .executes(ctx -> respawnAll(ctx.getSource(), true))))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .executes(ctx -> respawnGroup(ctx.getSource(), StringArgumentType.getString(ctx, "name"), false))
                                                .then(Commands.literal("toMe")
                                                        .executes(ctx -> respawnGroup(ctx.getSource(), StringArgumentType.getString(ctx, "name"), true)))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .executes(ctx -> respawnType(ctx.getSource(), StringArgumentType.getString(ctx, "name"), false))
                                                .then(Commands.literal("toMe")
                                                        .executes(ctx -> respawnType(ctx.getSource(), StringArgumentType.getString(ctx, "name"), true)))))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedDeadPetNames(ctx.getSource(), b))
                                        .executes(ctx -> respawnPet(ctx.getSource(), StringArgumentType.getString(ctx, "name"), false))
                                        .then(Commands.literal("toMe")
                                                .executes(ctx -> respawnPet(ctx.getSource(), StringArgumentType.getString(ctx, "name"), true)))))
                        .then(Commands.literal("group")
                                .executes(ctx -> groupOverview(ctx.getSource()))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                        .executes(ctx -> groupTames(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .executes(ctx -> groupSet(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "pet"),
                                                                StringArgumentType.getString(ctx, "name"))))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .executes(ctx -> groupRemove(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "pet"),
                                                                StringArgumentType.getString(ctx, "name"))))))
                                .then(Commands.literal("removefromallgroups")
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                .executes(ctx -> groupClear(ctx.getSource(), StringArgumentType.getString(ctx, "pet"))))))

                        .then(Commands.literal("mode")
                                .then(Commands.literal("all")
                                        .then(Commands.argument("mode", StringArgumentType.greedyString())
                                                .suggests((ctx, b) -> suggestModes(b))
                                                .executes(ctx -> allMode(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "mode")
                                                ))))
                                .then(Commands.argument("pet", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                        .then(Commands.argument("mode", StringArgumentType.greedyString())
                                                .suggests((ctx, b) -> suggestModes(b))
                                                .executes(ctx -> setMode(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "pet"),
                                                        StringArgumentType.getString(ctx, "mode")))))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .then(Commands.argument("mode", StringArgumentType.greedyString())
                                                        .suggests((ctx, b) -> suggestModes(b))
                                                        .executes(ctx -> groupMode(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "name"),
                                                                StringArgumentType.getString(ctx, "mode"))))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .then(Commands.argument("mode", StringArgumentType.greedyString())
                                                        .suggests((ctx, b) -> suggestModes(b))
                                                        .executes(ctx -> typeMode(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "name"),
                                                                StringArgumentType.getString(ctx, "mode"))))))
                                .then(Commands.literal("state")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestMovementStates(b))
                                                .then(Commands.argument("mode", StringArgumentType.greedyString())
                                                        .suggests((ctx, b) -> suggestModes(b))
                                                        .executes(ctx -> stateMode(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "name"),
                                                                StringArgumentType.getString(ctx, "mode")))))))

                        .then(Commands.literal("debug")
                                .executes(ctx -> debugStatus(ctx.getSource()))
                                .then(Commands.literal("enemyKilled")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> setDebugEnemyKilled(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                                .then(Commands.literal("attributeUsed")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> setDebugAttributeUsed(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                                .then(Commands.literal("levelUp")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> setDebugLevelUp(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled"))))))

                        .then(Commands.literal("admin")
                                .requires(source -> source.hasPermission(2))

                                .then(Commands.literal("resetWolvesBase")
                                        .executes(ctx -> adminResetWolvesBase(ctx.getSource())))

                                .then(Commands.literal("resetServerProgress")
                                        .executes(ctx -> adminResetServerProgress(ctx.getSource())))
                                .then(Commands.literal("reloadTames")
                                        .executes(ctx -> adminReloadTames(ctx.getSource())))
                                .then(Commands.literal("doubleHpBonus")
                                        .executes(ctx -> adminDoubleHpBonus(ctx.getSource())))
                                .then(Commands.literal("halfHpBonus")
                                        .executes(ctx -> adminHalfHpBonus(ctx.getSource())))
                                .then(Commands.literal("rerollHalfDamageBonus")
                                        .executes(ctx -> adminRerollHalfDamageBonus(ctx.getSource())))
                                .then(Commands.literal("normalizeBonuses")
                                        .executes(ctx -> adminNormalizeAllBonuses(ctx.getSource()))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> adminNormalizeAllBonuses(ctx.getSource())))
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                .executes(ctx -> adminNormalizePetBonus(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "pet")
                                                ))))

                                .then(Commands.literal("setClass")
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                .then(Commands.argument("class", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestClasses(b))
                                                        .executes(ctx -> adminSetClass(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "pet"),
                                                                StringArgumentType.getString(ctx, "class")
                                                        )))))
                                .then(Commands.literal("friendlyFire")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> adminSetFriendlyFire(
                                                        ctx.getSource(),
                                                        BoolArgumentType.getBool(ctx, "enabled")
                                                ))))
                                .then(Commands.literal("debug")
                                        .executes(ctx -> adminDebugStatus(ctx.getSource()))
                                        .then(Commands.literal("abilityUsed")
                                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                        .executes(ctx -> adminSetDebugAbilityUsed(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                                        .then(Commands.literal("damage")
                                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                        .executes(ctx -> adminSetDebugDamage(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                                        .then(Commands.literal("damageDealt")
                                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                        .executes(ctx -> adminSetDebugDamage(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled"))))))
                                .then(Commands.literal("damageNerf")
                                        .executes(ctx -> adminAbilityDamageNerfStatus(ctx.getSource()))
                                        .then(Commands.literal("single")
                                                .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.0D))
                                                        .executes(ctx -> adminSetAbilityDamageNerf(
                                                                ctx.getSource(),
                                                                "single",
                                                                DoubleArgumentType.getDouble(ctx, "multiplier")
                                                        ))))
                                        .then(Commands.literal("aoe")
                                                .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.0D))
                                                        .executes(ctx -> adminSetAbilityDamageNerf(
                                                                ctx.getSource(),
                                                                "aoe",
                                                                DoubleArgumentType.getDouble(ctx, "multiplier")
                                                        )))))
                                .then(Commands.literal("cooldownNerf")
                                        .executes(ctx -> adminAbilityCooldownNerfStatus(ctx.getSource()))
                                        .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0.0D))
                                                .executes(ctx -> adminSetAbilityCooldownNerf(
                                                        ctx.getSource(),
                                                        DoubleArgumentType.getDouble(ctx, "percent")
                                                ))))
                                .then(Commands.literal("uniteDuplicates")
                                        .executes(ctx -> adminUniteDuplicates(ctx.getSource(), "", true))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> adminUniteDuplicates(ctx.getSource(), "", true)))
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                .executes(ctx -> adminUniteDuplicates(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name"),
                                                        false
                                                ))))
                                .then(Commands.literal("respawn")
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestAllRespawnableTameNames(b))
                                                .executes(ctx -> adminRespawnPet(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "pet"),
                                                        0
                                                ))
                                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> adminRespawnPet(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "pet"),
                                                                IntegerArgumentType.getInteger(ctx, "index")
                                                        )))))
                                .then(Commands.literal("forceReincarnate")
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                .executes(ctx -> adminForceReincarnate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "pet"),
                                                        0
                                                ))
                                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> adminForceReincarnate(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "pet"),
                                                                IntegerArgumentType.getInteger(ctx, "index")
                                                        )))))
                                .then(Commands.literal("terminate")
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestAllRespawnableTameNames(b))
                                                .executes(ctx -> adminTerminatePet(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "pet")
                                                ))))
                                .then(Commands.literal("clean")
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestKnownPlayerOwners(ctx.getSource(), b))
                                                .executes(ctx -> adminCleanPlayerList(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player")
                                                ))
                                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> adminCleanPlayerIndex(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "player"),
                                                                IntegerArgumentType.getInteger(ctx, "index")
                                                        )))))
                                .then(Commands.literal("approve")
                                        .then(Commands.literal("item")
                                                .executes(ctx -> adminApproveHeldItem(ctx.getSource()))))
                                .then(Commands.literal("collar")
                                        .then(Commands.literal("stripDiEnchants")
                                                .executes(ctx -> adminStripDiEnchantsFromHeldCollar(ctx.getSource())))
                                        .then(Commands.literal("stripAll")
                                                .executes(ctx -> adminStripAllCollarTagEnchants(ctx.getSource()))))
                                .then(Commands.literal("registry")
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("name", StringArgumentType.string())
                                                        .executes(ctx -> adminRegistryRemove(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "name")
                                                        ))))
                                        .then(Commands.literal("importTL")
                                                .executes(ctx -> adminImportTlData(ctx.getSource(), ""))
                                                .then(Commands.argument("file", StringArgumentType.word())
                                                        .executes(ctx -> adminImportTlData(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "file")
                                                        )))))
                                .then(Commands.literal("tp")
                                        .then(Commands.literal("allOwners")
                                                .executes(ctx -> adminTpAllOwners(ctx.getSource(), false))
                                                .then(Commands.literal("unloaded")
                                                        .executes(ctx -> adminTpAllOwners(ctx.getSource(), true)))))

                                .then(Commands.literal("xp")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("pet", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> xpAdd(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "pet"),
                                                                        IntegerArgumentType.getInteger(ctx, "amount")
                                                                )))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("pet", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> xpRemove(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "pet"),
                                                                        IntegerArgumentType.getInteger(ctx, "amount")
                                                                ))))))

                                .then(Commands.literal("ability")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestAdminAbilities(b))
                                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                                .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                                .executes(ctx -> abilityAdd(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "pet"),
                                                                        StringArgumentType.getString(ctx, "id"),
                                                                        1
                                                                ))
                                                                .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> abilityAdd(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "pet"),
                                                                                StringArgumentType.getString(ctx, "id"),
                                                                                IntegerArgumentType.getInteger(ctx, "levels")
                                                                        ))))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestAdminAbilities(b))
                                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                                .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                                .executes(ctx -> abilityRemove(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "pet"),
                                                                        StringArgumentType.getString(ctx, "id"),
                                                                        1
                                                                ))
                                                                .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> abilityRemove(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "pet"),
                                                                                StringArgumentType.getString(ctx, "id"),
                                                                                IntegerArgumentType.getInteger(ctx, "levels")
                                                                        ))))))
                                        .then(Commands.literal("clear")
                                                .then(Commands.argument("pet", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                        .executes(ctx -> abilityClear(ctx.getSource(), StringArgumentType.getString(ctx, "pet")))))
                                        .then(Commands.literal("list")
                                                .then(Commands.argument("pet", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                        .executes(ctx -> abilityList(ctx.getSource(), StringArgumentType.getString(ctx, "pet"))))))

                                .then(Commands.literal("attribute")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestAttributes(b))
                                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                                .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                                .executes(ctx -> attributeAdd(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "pet"),
                                                                        StringArgumentType.getString(ctx, "id"),
                                                                        1
                                                                ))
                                                                .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> attributeAdd(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "pet"),
                                                                                StringArgumentType.getString(ctx, "id"),
                                                                                IntegerArgumentType.getInteger(ctx, "levels")
                                                                        ))))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestAttributes(b))
                                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                                .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                                .executes(ctx -> attributeRemove(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "pet"),
                                                                        StringArgumentType.getString(ctx, "id"),
                                                                        1
                                                                ))
                                                                .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> attributeRemove(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "pet"),
                                                                                StringArgumentType.getString(ctx, "id"),
                                                                                IntegerArgumentType.getInteger(ctx, "levels")
                                                                        ))))))
                                        .then(Commands.literal("clear")
                                                .then(Commands.argument("pet", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                        .executes(ctx -> attributeClear(ctx.getSource(), StringArgumentType.getString(ctx, "pet")))))
                                        .then(Commands.literal("list")
                                                .then(Commands.argument("pet", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                        .executes(ctx -> attributeList(ctx.getSource(), StringArgumentType.getString(ctx, "pet"))))))

                                .then(Commands.literal("removeTarget")
                                        .executes(ctx -> removeTargetAll(ctx.getSource()))
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                .executes(ctx -> removeTargetPet(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))
                                        .then(Commands.literal("group")
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                        .executes(ctx -> removeTargetGroup(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                        .then(Commands.literal("type")
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                        .executes(ctx -> removeTargetType(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                        .then(Commands.literal("state")
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestMovementStates(b))
                                                        .executes(ctx -> removeTargetState(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))))

                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(ctx -> statLong(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))
                        );

        dispatcher.register(Commands.literal("tame").redirect(root));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        processMorningRegistrySweep(server);
        processPendingUnloadedTeleportsFromWorldData(server);
        processPendingMorningLanternRecalls(server);
    }

    private static void processMorningRegistrySweep(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        long dayTime = overworld.getDayTime();
        if ((dayTime % 24000L) != 1L) {
            return;
        }
        long day = dayTime / 24000L;
        if (day == lastMorningRegistrySweepDay) {
            return;
        }
        lastMorningRegistrySweepDay = day;

        DIWorldData worldData = DIWorldData.get(overworld);
        if (worldData != null) {
            worldData.clearAllRespawnRequests();
            worldData.clearLanternRequestsByMode(LanternRequest.MODE_LANTERN);
        }
        processMorningPetBedRespawns(server);
        scheduleMorningWaywardLanternRecalls(server, overworld.getGameTime());
    }

    private static void processMorningPetBedRespawns(MinecraftServer server) {
        for (TameData data : new ArrayList<>(TameRegistry.TAMES.values())) {
            if (data == null || !data.dead) {
                continue;
            }
            if (findLoadedTameByIdentity(server, data.uuid, data.tlId) != null) {
                continue;
            }
            SpawnTarget target = resolveMorningRespawnTarget(server, data);
            if (target == null) {
                continue;
            }
            RespawnResult result = respawnDeadTameAtServer(data, target.level, target.pos, target.yRot, target.xRot);
            if (!result.success) {
                continue;
            }
            clearMatchingDiBedRespawnRequests(server, data);
            ServerPlayer owner = data.ownerUUID == null ? null : server.getPlayerList().getPlayer(data.ownerUUID);
            TamableAnimal respawned = findLoadedTameByUuid(server, data.uuid);
            if (owner != null && respawned != null) {
                owner.displayClientMessage(Component.translatable("message.domesticationinnovation.respawn", respawned.getName()), false);
            }
        }
    }

    private static SpawnTarget resolveMorningRespawnTarget(MinecraftServer server, TameData data) {
        if (server == null || data == null) {
            return null;
        }
        if (data.hasPetBed && data.petBedDimension != null && !data.petBedDimension.isBlank()) {
            ResourceLocation bedDimId = ResourceLocation.tryParse(data.petBedDimension);
            if (bedDimId != null) {
                ServerLevel bedLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, bedDimId));
                if (bedLevel != null) {
                    BlockPos bedPos = new BlockPos(data.petBedX, data.petBedY, data.petBedZ);
                    bedLevel.getChunk(bedPos);
                    if (bedLevel.getBlockEntity(bedPos) instanceof com.github.alexthe668.domesticationinnovation.server.block.PetBedBlockEntity) {
                        Direction facing = bedLevel.getBlockState(bedPos).hasProperty(com.github.alexthe668.domesticationinnovation.server.block.PetBedBlock.FACING)
                                ? bedLevel.getBlockState(bedPos).getValue(com.github.alexthe668.domesticationinnovation.server.block.PetBedBlock.FACING)
                                : Direction.NORTH;
                        return new SpawnTarget(bedLevel, Vec3.upFromBottomCenterOf(bedPos, 0.8F), yawFromDirection(facing), 0.0F);
                    }
                }
            }
        }
        SpawnTarget ownerBedTarget = resolveOwnerBedTarget(server, data.ownerUUID);
        if (ownerBedTarget != null) {
            return ownerBedTarget;
        }
        if (data.ownerUUID != null) {
            ServerPlayer owner = server.getPlayerList().getPlayer(data.ownerUUID);
            if (owner != null) {
                return new SpawnTarget(owner.serverLevel(), owner.position(), owner.getYRot(), owner.getXRot());
            }
        }
        ServerLevel overworld = server.overworld();
        return new SpawnTarget(overworld, Vec3.atCenterOf(overworld.getSharedSpawnPos()), 0.0F, 0.0F);
    }

    private static void scheduleMorningWaywardLanternRecalls(MinecraftServer server, long now) {
        for (TameData data : new ArrayList<>(TameRegistry.TAMES.values())) {
            if (!isEligibleForMorningLanternRecall(server, data)) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(data.ownerUUID);
            if (owner == null) {
                continue;
            }
            BlockPos lanternPos = findNearestWaywardLantern(owner);
            if (lanternPos == null) {
                continue;
            }
            ResourceLocation sourceId = ResourceLocation.tryParse(data.lastKnownDimension);
            if (sourceId == null || !owner.level().dimension().location().equals(sourceId)) {
                continue;
            }
            ResourceKey<Level> sourceKey = ResourceKey.create(Registries.DIMENSION, sourceId);
            PENDING_MORNING_LANTERN.put(data.ensureTlId(), new PendingMorningLanternRecall(
                    data.uuid,
                    data.tlId,
                    owner.getUUID(),
                    sourceKey,
                    new BlockPos(data.lastKnownX, data.lastKnownY, data.lastKnownZ),
                    owner.serverLevel().dimension(),
                    lanternPos,
                    now,
                    data.name == null ? "unknown" : data.name
            ));
            ServerLevel sourceLevel = server.getLevel(sourceKey);
            if (sourceLevel != null) {
                loadChunksAround(sourceLevel, data.uuid, new BlockPos(data.lastKnownX, data.lastKnownY, data.lastKnownZ), true);
            }
        }
    }

    private static void processPendingMorningLanternRecalls(MinecraftServer server) {
        if (PENDING_MORNING_LANTERN.isEmpty()) {
            return;
        }
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        long now = overworld == null ? 0L : overworld.getGameTime();
        List<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, PendingMorningLanternRecall> entry : PENDING_MORNING_LANTERN.entrySet()) {
            PendingMorningLanternRecall pending = entry.getValue();
            if (pending == null) {
                finished.add(entry.getKey());
                continue;
            }
            ServerLevel sourceLevel = server.getLevel(pending.sourceDimension);
            ServerLevel targetLevel = server.getLevel(pending.targetDimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(pending.ownerUuid);
            if (sourceLevel == null || targetLevel == null || owner == null) {
                finished.add(entry.getKey());
                continue;
            }
            if (!sourceLevel.dimension().equals(targetLevel.dimension())) {
                finished.add(entry.getKey());
                continue;
            }

            loadChunksAround(sourceLevel, pending.tameUuid, pending.sourcePos, true);
            TamableAnimal tame = findLoadedTameByIdentity(sourceLevel, pending.tameUuid, pending.tlId);
            if (tame != null && tame.isAlive()) {
                BlockPos putAt = findLanternPlacement(targetLevel, pending.lanternPos, tame);
                tame.teleportTo(putAt.getX() + 0.5D, putAt.getY(), putAt.getZ() + 0.5D);
                tame.setDeltaMovement(0.0D, 0.0D, 0.0D);
                TameData data = pending.tlId != null ? TameRegistry.getByTlId(pending.tlId) : TameRegistry.get(tame.getUUID());
                if (data != null) {
                    if (!Objects.equals(data.uuid, tame.getUUID())) {
                        TameRegistry.rebindEntityUuid(data, tame.getUUID());
                    }
                    TameRegistry.bindEntityToData(tame, data);
                    data.lastKnownDimension = tame.level().dimension().location().toString();
                    data.lastKnownX = tame.blockPosition().getX();
                    data.lastKnownY = tame.blockPosition().getY();
                    data.lastKnownZ = tame.blockPosition().getZ();
                    data.lastKnownGameTime = tame.level().getGameTime();
                    CompoundTag refreshedSnapshot = new CompoundTag();
                    tame.save(refreshedSnapshot);
                    data.entitySnapshot = refreshedSnapshot;
                    TameRegistry.markDirty();
                }
                owner.displayClientMessage(Component.translatable("message.domesticationinnovation.wayward_lantern_return", tame.getName()), false);
                loadChunksAround(sourceLevel, pending.tameUuid, pending.sourcePos, false);
                finished.add(entry.getKey());
                continue;
            }

            if ((now - pending.createdTick) >= MORNING_LANTERN_TIMEOUT_TICKS) {
                loadChunksAround(sourceLevel, pending.tameUuid, pending.sourcePos, false);
                finished.add(entry.getKey());
            }
        }
        for (UUID id : finished) {
            PENDING_MORNING_LANTERN.remove(id);
        }
    }

    private static void processPendingUnloadedTeleportsFromWorldData(MinecraftServer server) {
        if (server == null) return;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        DIWorldData worldData = DIWorldData.get(overworld);
        if (worldData == null) return;
        List<LanternRequest> requests = worldData.getLanternRequestsSnapshot();
        if (requests.isEmpty()) return;

        long now = overworld.getGameTime();
        for (LanternRequest request : requests) {
            if (request == null || !request.isPlayerTeleportMode()) continue;

            ServerPlayer owner = server.getPlayerList().getPlayer(request.getOwnerUUID());
            TameData data = request.getTlId() != null ? TameRegistry.getByTlId(request.getTlId()) : TameRegistry.get(request.getPetUUID());
            String targetDimString = request.getTargetDimension();
            ResourceLocation targetDimId = ResourceLocation.tryParse(targetDimString);
            ServerLevel sourceLevel = null;
            ServerLevel targetLevel = null;
            ResourceLocation sourceDimId = null;
            if (data != null && data.lastKnownDimension != null && !data.lastKnownDimension.isBlank()) {
                sourceDimId = ResourceLocation.tryParse(data.lastKnownDimension);
            }
            if (sourceDimId == null) {
                sourceDimId = targetDimId;
            }
            if (sourceDimId != null) {
                ResourceKey<Level> sourceKey = ResourceKey.create(Registries.DIMENSION, sourceDimId);
                sourceLevel = server.getLevel(sourceKey);
            }
            if (targetDimId != null) {
                ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, targetDimId);
                targetLevel = server.getLevel(targetKey);
            }
            if (owner == null || sourceLevel == null || targetLevel == null) {
                worldData.removeLanternRequest(request);
                continue;
            }

            if (!sourceLevel.dimension().equals(targetLevel.dimension())) {
                owner.sendSystemMessage(Component.literal("Unloaded tp blocked for " + request.getNametag() + ": cross-dimension unloaded tp is disabled.").withStyle(ChatFormatting.RED));
                worldData.removeLanternRequest(request);
                continue;
            }

            loadChunksAround(sourceLevel, request.getPetUUID(), request.getChunkPosition(), true);
            TamableAnimal tame = findLoadedTameForRequest(sourceLevel, request);

            if (tame != null && tame.isTame() && tame.isAlive()) {
                tame.teleportTo(request.getTargetX(), request.getTargetY(), request.getTargetZ());
                tame.setYRot(request.getTargetYaw());
                tame.setXRot(request.getTargetPitch());
                tame.setOrderedToSit(false);
                tame.setTarget(null);
                tame.getNavigation().stop();
                tame.getNavigation().moveTo(owner, 1.0D);

                data = request.getTlId() != null ? TameRegistry.getByTlId(request.getTlId()) : TameRegistry.get(tame.getUUID());
                if (data != null) {
                    if (!Objects.equals(data.uuid, tame.getUUID())) {
                        TameRegistry.rebindEntityUuid(data, tame.getUUID());
                    }
                    TameRegistry.bindEntityToData(tame, data);
                    data.lastKnownDimension = tame.level().dimension().location().toString();
                    data.lastKnownX = tame.blockPosition().getX();
                    data.lastKnownY = tame.blockPosition().getY();
                    data.lastKnownZ = tame.blockPosition().getZ();
                    data.lastKnownGameTime = tame.level().getGameTime();
                    CompoundTag refreshedSnapshot = new CompoundTag();
                    tame.save(refreshedSnapshot);
                    data.entitySnapshot = refreshedSnapshot;
                    TameRegistry.markDirty();
                }
                owner.sendSystemMessage(Component.literal("Teleported unloaded " + request.getNametag() + " to your position.").withStyle(ChatFormatting.GREEN));
                worldData.removeLanternRequest(request);
                loadChunksAround(sourceLevel, request.getPetUUID(), request.getChunkPosition(), false);
                continue;
            }

            if ((now - request.getTimestamp()) >= UNLOADED_TP_TIMEOUT_TICKS) {
                boolean recovered = false;
                String recoverError = "unknown";
                if (data != null && isSameDimensionUnloadedRespawnFallback(owner, data)) {
                    RecoverResult recoverResult = recoverPetEntity(null, owner, data);
                    recovered = recoverResult.entity != null;
                    recoverError = recoverResult.error;
                }
                if (recovered) {
                    owner.sendSystemMessage(Component.literal("Respawned unloaded " + request.getNametag() + " at your position after entity load timeout.").withStyle(ChatFormatting.YELLOW));
                } else {
                    String detail = recoverError == null || recoverError.isBlank() ? "" : " Recover failed: " + recoverError + ".";
                    owner.sendSystemMessage(Component.literal("Failed to tp unloaded " + request.getNametag() + " (entity load timeout)." + detail).withStyle(ChatFormatting.RED));
                }
                worldData.removeLanternRequest(request);
                loadChunksAround(sourceLevel, request.getPetUUID(), request.getChunkPosition(), false);
            }
        }
    }

    private static void loadChunksAround(ServerLevel serverLevel, UUID ticket, BlockPos center, boolean load) {
        if (serverLevel == null || ticket == null || center == null) return;
        ChunkPos chunkPos = new ChunkPos(center);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                ForgeChunkManager.forceChunk(serverLevel, DomesticationMod.MODID, ticket, chunkPos.x + i, chunkPos.z + j, load, true);
            }
        }
    }

    private static int list(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        List<TameData> tames = new ArrayList<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            tames.add(d);
        }
        if (tames.isEmpty()) return error(p, "You have no registered tames.");
        tames.sort(Comparator.comparingInt((TameData d) -> d.level).reversed().thenComparing(d -> d.name.toLowerCase(Locale.ROOT)));
        p.sendSystemMessage(Component.literal("---- Your Tames ----"));
        for (int i = 0; i < tames.size(); i++) {
            TameData d = tames.get(i);
            long days = daysAlive(source, d);
            String line = (i + 1) + ". [" + d.level + "] " + d.name + " (" + d.kills + "/" + d.assists + "/" + days + "/" + d.deaths + ")"
                    + (isDeadEntry(d.uuid) ? " [DEAD]" : "");
            p.sendSystemMessage(Component.literal(line).withStyle(isDeadEntry(d.uuid) ? ChatFormatting.GRAY : ChatFormatting.WHITE));
        }
        return 1;
    }

    private static int strongest(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        TameData best = null;
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            if (isDeadEntry(d.uuid)) continue;
            if (best == null || d.level > best.level || (d.level == best.level && d.kills > best.kills)) best = d;
        }
        if (best == null) return error(p, "You have no registered tames.");
        return statLong(source, best.name);
    }

    private static int loadedStatus(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        List<TameData> tames = new ArrayList<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            tames.add(d);
        }
        if (tames.isEmpty()) return error(p, "You have no registered tames.");

        List<TameData> loaded = new ArrayList<>();
        List<TameData> unloaded = new ArrayList<>();
        List<TameData> dead = new ArrayList<>();
        for (TameData d : tames) {
            if (isDeadEntry(d.uuid)) {
                dead.add(d);
            } else if (isLoadedAnywhere(source.getServer(), d.uuid)) {
                loaded.add(d);
            } else {
                unloaded.add(d);
            }
        }

        loaded.sort(Comparator.comparing(d -> d.name.toLowerCase(Locale.ROOT)));
        unloaded.sort(Comparator.comparing(d -> d.name.toLowerCase(Locale.ROOT)));
        dead.sort(Comparator.comparing(d -> d.name.toLowerCase(Locale.ROOT)));

        p.sendSystemMessage(Component.literal("---- Tame Load Status ----").withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.literal("Loaded (" + loaded.size() + "):").withStyle(ChatFormatting.GREEN));
        for (TameData d : loaded) {
            p.sendSystemMessage(Component.literal("- [" + d.level + "] " + d.name).withStyle(ChatFormatting.GREEN));
        }
        p.sendSystemMessage(Component.literal("Unloaded (" + unloaded.size() + "):").withStyle(ChatFormatting.RED));
        for (TameData d : unloaded) {
            String where = (d.lastKnownDimension == null || d.lastKnownDimension.isBlank())
                    ? "unknown"
                    : (d.lastKnownDimension + " @ " + d.lastKnownX + " " + d.lastKnownY + " " + d.lastKnownZ);
            p.sendSystemMessage(Component.literal("- [" + d.level + "] " + d.name + " (" + where + ")").withStyle(ChatFormatting.RED));
        }
        p.sendSystemMessage(Component.literal("Dead (" + dead.size() + "):").withStyle(ChatFormatting.GRAY));
        for (TameData d : dead) {
            String where = (d.deathDimension == null || d.deathDimension.isBlank())
                    ? "unknown"
                    : (d.deathDimension + " @ " + d.deathX + " " + d.deathY + " " + d.deathZ);
            p.sendSystemMessage(Component.literal("- [" + d.level + "] " + d.name + " (" + where + ")").withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static int infoOverview(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        p.sendSystemMessage(Component.literal("/tame is an alias for /tames"));
        p.sendSystemMessage(Component.literal("/tames, /tames strongest, /tames <name>"));
        p.sendSystemMessage(Component.literal("/tames stat <name>, /tames inspect <name>, /tames search ability|attribute|class <id>"));
        p.sendSystemMessage(Component.literal("/tames loaded"));
        p.sendSystemMessage(Component.literal("/tames deaths <number>"));
        p.sendSystemMessage(Component.literal("/tames leaderboard [mix|kills|deaths|assists|lvl|days] [all] [<number>|everytame]"));
        p.sendSystemMessage(Component.literal("/tames follow|sit|wander [<name>|all|group <name>|type <name>|state <follow|wander|sit>]"));
        p.sendSystemMessage(Component.literal("/tames tp <name|all|follow|sit|wander|state <follow|wander|sit>|group <name>|type <name>>"));
        p.sendSystemMessage(Component.literal("/tames respawn <name|all|group <name>|type <name>> [toMe]"));
        p.sendSystemMessage(Component.literal("/tames group <name>|add|remove|removefromallgroups"));
        p.sendSystemMessage(Component.literal("/tames mode <name> <mode>, /tames mode <all|group|type|state> ... <mode>"));
        p.sendSystemMessage(Component.literal("/tames info attribute [name] | ability [name] | class | inspect | search"));
        p.sendSystemMessage(Component.literal("/tames debug enemyKilled|abilityUsed|attributeUsed|damage <true|false>"));
        p.sendSystemMessage(Component.literal("/tames admin normalizeBonuses <all|pet>, /tames admin approve item, /tames admin xp|ability|attribute|removeTarget ..."));
        p.sendSystemMessage(Component.literal("/tames berserk|passive"));
        return 1;
    }

    private static int infoDetail(CommandSourceStack source, String topic) {
        ServerPlayer p = source.getPlayer();
        String key = topic.trim().toLowerCase(Locale.ROOT);
        if (key.equals("leaderboard")) p.sendSystemMessage(Component.literal("/tames leaderboard [mix|kills|deaths|assists|lvl|days] [all] [<number>|everytame]"));
        else if (key.equals("deaths")) p.sendSystemMessage(Component.literal("/tames deaths <number>"));
        else if (key.equals("loaded")) p.sendSystemMessage(Component.literal("/tames loaded"));
        else if (key.equals("group")) p.sendSystemMessage(Component.literal("/tames group <name> | add <pet> <group> | remove <pet> <group> | removefromallgroups <pet>"));
        else if (key.equals("mode")) p.sendSystemMessage(Component.literal("/tames mode <pet> <mode>, /tames mode <all|group|type|state> ... <mode>"));
        else if (key.equals("follow") || key.equals("sit") || key.equals("wander")) p.sendSystemMessage(Component.literal("/tames " + key + " [<name>|all|group <group>|type <type>|state <follow|wander|sit>]"));
        else if (key.equals("tp")) p.sendSystemMessage(Component.literal("/tames tp <name|all|follow|sit|wander|state <follow|wander|sit>|group <group>|type <type>>"));
        else if (key.equals("inspect")) {
            p.sendSystemMessage(Component.literal("/tames inspect <pet>").withStyle(ChatFormatting.GOLD));
            p.sendSystemMessage(Component.literal("Shows live combat inspection for a loaded tame.").withStyle(ChatFormatting.GRAY));
            p.sendSystemMessage(Component.literal("Includes current base stats, runtime damage multipliers, exact ability DPS where formula is known, and attribute combat notes where DPS depends on target or hit rate.").withStyle(ChatFormatting.GRAY));
        }
        else if (key.equals("search")) {
            p.sendSystemMessage(Component.literal("/tames search ability <id>").withStyle(ChatFormatting.GOLD));
            p.sendSystemMessage(Component.literal("/tames search attribute <id>").withStyle(ChatFormatting.GOLD));
            p.sendSystemMessage(Component.literal("/tames search class <id>").withStyle(ChatFormatting.GOLD));
            p.sendSystemMessage(Component.literal("Searches your registered tames by ability, attribute, or tame class.").withStyle(ChatFormatting.GRAY));
        }
        else if (key.equals("debug")) p.sendSystemMessage(Component.literal("/tames debug enemyKilled|abilityUsed|attributeUsed|damage <true|false>"));
        else if (key.equals("attribute")) {
            p.sendSystemMessage(Component.literal("Attribute docs (General):").withStyle(ChatFormatting.GOLD));
            sendDocLines(p, readDocSectionByHeading(
                    docPath("AttributesDocu.md"),
                    "## General",
                    "## "
            ));
            p.sendSystemMessage(Component.literal("Use /tames info attribute <name> for exact doc text and scaling details.").withStyle(ChatFormatting.DARK_AQUA));
        }
        else if (key.equals("ability")) {
            p.sendSystemMessage(Component.literal("Ability docs (General):").withStyle(ChatFormatting.GOLD));
            sendDocLines(p, readDocSectionByHeading(
                    docPath("AbilitiesDocu.md"),
                    "## General",
                    "## "
            ));
            p.sendSystemMessage(Component.literal("Use /tames info ability <name> for exact doc text and scaling details.").withStyle(ChatFormatting.DARK_AQUA));
        }
        else if (key.equals("class")) {
            p.sendSystemMessage(Component.literal("Class docs (General):").withStyle(ChatFormatting.GOLD));
            sendDocLines(p, readDocSectionByHeading(
                    docPath("ClassesDocu.md"),
                    "## General",
                    "## "
            ));
            p.sendSystemMessage(Component.literal("Use /tames info class <name> for exact class documentation.").withStyle(ChatFormatting.DARK_AQUA));
        }
        else p.sendSystemMessage(Component.literal("Unknown topic."));
        return 1;
    }

    private static int infoAbility(CommandSourceStack source, String abilityName) {
        ServerPlayer p = source.getPlayer();
        String id = abilityName == null ? "" : abilityName.trim().toLowerCase(Locale.ROOT);
        if (id.isBlank()) return error(p, "Ability name cannot be blank.");
        if (!LevelSystem.knownAbilityIds().contains(id)) return error(p, "Unknown ability: " + id);

        List<String> block = readDocSectionByHeading(
                docPath("AbilitiesDocu.md"),
                "### `" + id + "`",
                "### `"
        );
        if (block.isEmpty()) {
            return error(p, "No documentation section found for ability: " + id);
        }

        p.sendSystemMessage(Component.literal("Ability doc: " + id).withStyle(ChatFormatting.GOLD));
        sendDocLines(p, block);
        return 1;
    }

    private static int infoAttribute(CommandSourceStack source, String attributeName) {
        ServerPlayer p = source.getPlayer();
        String id = attributeName == null ? "" : attributeName.trim().toLowerCase(Locale.ROOT);
        if (id.isBlank()) return error(p, "Attribute name cannot be blank.");
        if (!LevelSystem.knownAttributeIds().contains(id)) return error(p, "Unknown attribute: " + id);

        List<String> block = readDocBulletBlock(
                docPath("AttributesDocu.md"),
                "- `" + id + "`"
        );
        if (block.isEmpty()) {
            return error(p, "No documentation section found for attribute: " + id);
        }

        p.sendSystemMessage(Component.literal("Attribute doc: " + id).withStyle(ChatFormatting.GOLD));
        sendDocLines(p, block);
        return 1;
    }

    private static int infoClass(CommandSourceStack source, String className) {
        ServerPlayer p = source.getPlayer();
        String id = className == null ? "" : className.trim().toUpperCase(Locale.ROOT);
        if (id.isBlank()) return error(p, "Class name cannot be blank.");
        try {
            TameClass.valueOf(id);
        } catch (IllegalArgumentException ex) {
            return error(p, "Unknown class: " + id);
        }

        List<String> block = readDocSectionByHeading(
                docPath("ClassesDocu.md"),
                "### `" + id + "`",
                "### `"
        );
        if (block.isEmpty()) {
            return error(p, "No documentation section found for class: " + id);
        }

        p.sendSystemMessage(Component.literal("Class doc: " + id).withStyle(ChatFormatting.GOLD));
        sendDocLines(p, block);
        return 1;
    }

    private static void sendDocLines(ServerPlayer player, List<String> lines) {
        if (player == null || lines == null) return;
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;
            player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }

    private static List<String> readDocSectionByHeading(Path path, String heading, String nextHeadingPrefix) {
        List<String> lines = readDocFile(path);
        if (lines.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        boolean inBlock = false;
        for (String line : lines) {
            if (!inBlock) {
                if (line.trim().equalsIgnoreCase(heading.trim())) {
                    inBlock = true;
                    out.add(line);
                }
                continue;
            }
            if (line.startsWith(nextHeadingPrefix) && !line.trim().equalsIgnoreCase(heading.trim())) {
                break;
            }
            out.add(line);
        }
        return trimTrailingBlankLines(out);
    }

    private static List<String> readDocBulletBlock(Path path, String bulletPrefix) {
        List<String> lines = readDocFile(path);
        if (lines.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        boolean inBlock = false;
        for (String line : lines) {
            if (!inBlock) {
                if (line.trim().startsWith(bulletPrefix)) {
                    inBlock = true;
                    out.add(line);
                }
                continue;
            }
            boolean nextTopLevelBullet = line.startsWith("- `");
            boolean nextSection = line.startsWith("## ");
            if (nextTopLevelBullet || nextSection) {
                break;
            }
            out.add(line);
        }
        return trimTrailingBlankLines(out);
    }

    private static List<String> trimTrailingBlankLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) return List.of();
        int end = lines.size();
        while (end > 0 && (lines.get(end - 1) == null || lines.get(end - 1).isBlank())) {
            end--;
        }
        if (end <= 0) return List.of();
        return new ArrayList<>(lines.subList(0, end));
    }
    private static List<String> readDocFile(Path path) {
        if (path == null) return List.of();
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        if (!fileName.isBlank()) {
            try (var stream = TameCommands.class.getClassLoader().getResourceAsStream(DOC_RESOURCE_BASE + fileName)) {
                if (stream != null) {
                    return new ArrayList<>(new java.io.BufferedReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).lines().toList());
                }
            } catch (IOException ignored) {
            }
        }
        try {
            return Files.readAllLines(path);
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static Path docPath(String fileName) {
        return DOC_SOURCE_BASE.resolve(fileName);
    }

    private static int statShort(CommandSourceStack source, String petName) {
        ServerPlayer p = source.getPlayer();
        TameData d = findOwnedTameAny(p.getUUID(), petName);
        if (d == null) return error(p, "Pet not found.");
        sendTameStats(source, p, d, false);
        return 1;
    }

    private static int statLong(CommandSourceStack source, String petName) {
        ServerPlayer p = source.getPlayer();
        TameData d = findOwnedTameAny(p.getUUID(), petName);
        if (d == null) return error(p, "Pet not found.");
        sendTameStats(source, p, d, true);
        return 1;
    }

    private static int showStatsToPlayer(CommandSourceStack source, String targetPlayerName, String petName) {
        ServerPlayer sender = source.getPlayer();
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(targetPlayerName);
        if (target == null) {
            return error(sender, "Player is not online.");
        }
        TameData d = findOwnedTameAny(sender.getUUID(), petName);
        if (d == null) {
            return error(sender, "Pet not found.");
        }
        target.sendSystemMessage(Component.literal("Stats shared by " + sender.getName().getString() + ":").withStyle(ChatFormatting.GOLD));
        sendTameStats(source, target, d, true);
        sender.sendSystemMessage(Component.literal("Shared stats of " + d.name + " with " + target.getName().getString() + ".").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int duelInviteSelection(CommandSourceStack source, DuelSelection selection, String targetPlayerName) {
        ServerPlayer challenger = source.getPlayer();
        if (challenger.getName().getString().equalsIgnoreCase(targetPlayerName)) {
            return error(challenger, "You cannot duel yourself.");
        }
        ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(targetPlayerName);
        if (targetPlayer == null) {
            return error(challenger, "Target player is not online.");
        }

        cleanupExpiredDuelInvites();
        DuelSelectionResult challengerResult = resolveLoadedDuelSelection(source, challenger.getUUID(), selection);
        if (!challengerResult.error.isBlank()) return error(challenger, challengerResult.error);
        List<TamableAnimal> challengerGroup = challengerResult.tames;
        if (challengerGroup.isEmpty()) return error(challenger, "Your selected duel tames are not loaded/alive.");

        DUEL_INVITES.computeIfAbsent(targetPlayer.getUUID(), ignored -> new HashMap<>())
                .put(challenger.getUUID(), new DuelInvite(challenger.getUUID(), targetPlayer.getUUID(), selection, System.currentTimeMillis()));

        String challengerSelectionText = duelSelectionLabel(selection);
        challenger.sendSystemMessage(Component.literal("Sent duel invite to " + targetPlayer.getName().getString() + " using " + challengerSelectionText + ".").withStyle(ChatFormatting.GREEN));
        targetPlayer.sendSystemMessage(Component.literal(challenger.getName().getString() + " invited you to a duel with " + challengerSelectionText + ".").withStyle(ChatFormatting.GOLD));
        targetPlayer.sendSystemMessage(Component.literal("Accept: /tames duel accept " + challenger.getName().getString() + " <group|type|all|name>").withStyle(ChatFormatting.AQUA));
        targetPlayer.sendSystemMessage(Component.literal("Quick accept: /tames duel group <group>, /tames duel type <type>, /tames duel <tamename>, /tames duel all").withStyle(ChatFormatting.AQUA));
        targetPlayer.sendSystemMessage(Component.literal("Decline: /tames duel decline " + challenger.getName().getString()).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int duelStartSameOwner(CommandSourceStack source, DuelSelection leftSelection, DuelSelection rightSelection) {
        ServerPlayer owner = source.getPlayer();
        DuelSelectionResult leftResult = resolveLoadedDuelSelection(source, owner.getUUID(), leftSelection);
        if (!leftResult.error.isBlank()) return error(owner, leftResult.error);
        DuelSelectionResult rightResult = resolveLoadedDuelSelection(source, owner.getUUID(), rightSelection);
        if (!rightResult.error.isBlank()) return error(owner, rightResult.error);

        List<TamableAnimal> leftGroup = leftResult.tames;
        List<TamableAnimal> rightGroup = rightResult.tames;
        if (leftGroup.isEmpty()) return error(owner, "Left duel selection has no loaded/alive tames.");
        if (rightGroup.isEmpty()) return error(owner, "Right duel selection has no loaded/alive tames.");

        Set<UUID> leftIds = new HashSet<>();
        Set<UUID> rightIds = new HashSet<>();
        for (TamableAnimal tame : leftGroup) {
            if (tame == null || !tame.isAlive()) continue;
            leftIds.add(tame.getUUID());
        }
        for (TamableAnimal tame : rightGroup) {
            if (tame == null || !tame.isAlive()) continue;
            rightIds.add(tame.getUUID());
        }
        Set<UUID> overlap = new HashSet<>(leftIds);
        overlap.retainAll(rightIds);
        if (!overlap.isEmpty()) {
            return error(owner, "Selections overlap. Choose distinct teams.");
        }
        if (leftIds.isEmpty() || rightIds.isEmpty()) {
            return error(owner, "Selection resolved to an empty team.");
        }

        for (TamableAnimal own : leftGroup) {
            applySitFollowOverride(own, false);
            TamableAnimal enemy = nearestLoadedOpponent(own, rightGroup);
            if (enemy != null) {
                own.setTarget(enemy);
            }
        }
        for (TamableAnimal own : rightGroup) {
            applySitFollowOverride(own, false);
            TamableAnimal enemy = nearestLoadedOpponent(own, leftGroup);
            if (enemy != null) {
                own.setTarget(enemy);
            }
        }

        TameDuelManager.startGroupDuel(source.getServer(), owner.getUUID(), leftIds, owner.getUUID(), rightIds);
        owner.sendSystemMessage(Component.literal("Duel started: " + duelSelectionLabel(leftSelection) + " vs " + duelSelectionLabel(rightSelection) + ".").withStyle(ChatFormatting.RED));
        return 1;
    }

    private static int duelAcceptSelection(CommandSourceStack source, String challengerName, DuelSelection targetSelection) {
        ServerPlayer targetPlayer = source.getPlayer();
        cleanupExpiredDuelInvites();
        ServerPlayer challenger = source.getServer().getPlayerList().getPlayerByName(challengerName);
        if (challenger == null) return error(targetPlayer, "Challenger is not online.");
        if (challenger.getUUID().equals(targetPlayer.getUUID())) return error(targetPlayer, "You cannot duel yourself.");

        DuelInvite invite = popDuelInvite(targetPlayer.getUUID(), challenger.getUUID());
        if (invite == null) return error(targetPlayer, "No pending duel invite from " + challengerName + ".");

        DuelSelectionResult challengerResult = resolveLoadedDuelSelection(source, challenger.getUUID(), invite.challengerSelection);
        if (!challengerResult.error.isBlank()) return error(targetPlayer, challengerResult.error);
        List<TamableAnimal> challengerGroup = challengerResult.tames;
        if (challengerGroup.isEmpty()) return error(targetPlayer, "Challenger selected duel tames are not loaded/alive.");
        DuelSelectionResult targetResult = resolveLoadedDuelSelection(source, targetPlayer.getUUID(), targetSelection);
        if (!targetResult.error.isBlank()) return error(targetPlayer, targetResult.error);
        List<TamableAnimal> targetGroup = targetResult.tames;
        if (targetGroup.isEmpty()) return error(targetPlayer, "Your selected duel tames are not loaded/alive.");

        Set<UUID> challengerIds = new HashSet<>();
        Set<UUID> targetIds = new HashSet<>();
        for (TamableAnimal tame : challengerGroup) {
            applySitFollowOverride(tame, false);
            challengerIds.add(tame.getUUID());
        }
        for (TamableAnimal tame : targetGroup) {
            applySitFollowOverride(tame, false);
            targetIds.add(tame.getUUID());
        }

        for (TamableAnimal own : challengerGroup) {
            TamableAnimal enemy = nearestLoadedOpponent(own, targetGroup);
            if (enemy != null) {
                own.setTarget(enemy);
            }
        }
        for (TamableAnimal own : targetGroup) {
            TamableAnimal enemy = nearestLoadedOpponent(own, challengerGroup);
            if (enemy != null) {
                own.setTarget(enemy);
            }
        }
        TameDuelManager.startGroupDuel(source.getServer(), challenger.getUUID(), challengerIds, targetPlayer.getUUID(), targetIds);

        String challengerSelectionText = duelSelectionLabel(invite.challengerSelection);
        String targetSelectionText = duelSelectionLabel(targetSelection);
        challenger.sendSystemMessage(Component.literal(targetPlayer.getName().getString() + " accepted duel: " + challengerSelectionText + " vs " + targetSelectionText + ".").withStyle(ChatFormatting.RED));
        targetPlayer.sendSystemMessage(Component.literal("Duel started: " + targetSelectionText + " vs " + challengerSelectionText + ".").withStyle(ChatFormatting.RED));
        return 1;
    }

    private static int duelAcceptQuick(CommandSourceStack source, DuelSelection targetSelection) {
        ServerPlayer targetPlayer = source.getPlayer();
        cleanupExpiredDuelInvites();
        Map<UUID, DuelInvite> incoming = DUEL_INVITES.get(targetPlayer.getUUID());
        if (incoming == null || incoming.isEmpty()) {
            return error(targetPlayer, "No pending duel invites.");
        }
        if (incoming.size() > 1) {
            return error(targetPlayer, "Multiple duel invites pending. Use /tames duel accept <player> <group|type|all|name>.");
        }

        DuelInvite invite = incoming.values().iterator().next();
        ServerPlayer challenger = source.getServer().getPlayerList().getPlayer(invite.challengerUuid);
        if (challenger == null) {
            popDuelInvite(targetPlayer.getUUID(), invite.challengerUuid);
            return error(targetPlayer, "Challenger is not online.");
        }
        if (challenger.getUUID().equals(targetPlayer.getUUID())) {
            return error(targetPlayer, "You cannot duel yourself.");
        }
        popDuelInvite(targetPlayer.getUUID(), invite.challengerUuid);

        DuelSelectionResult challengerResult = resolveLoadedDuelSelection(source, challenger.getUUID(), invite.challengerSelection);
        if (!challengerResult.error.isBlank()) return error(targetPlayer, challengerResult.error);
        List<TamableAnimal> challengerGroup = challengerResult.tames;
        if (challengerGroup.isEmpty()) return error(targetPlayer, "Challenger selected duel tames are not loaded/alive.");
        DuelSelectionResult targetResult = resolveLoadedDuelSelection(source, targetPlayer.getUUID(), targetSelection);
        if (!targetResult.error.isBlank()) return error(targetPlayer, targetResult.error);
        List<TamableAnimal> targetGroup = targetResult.tames;
        if (targetGroup.isEmpty()) return error(targetPlayer, "Your selected duel tames are not loaded/alive.");

        Set<UUID> challengerIds = new HashSet<>();
        Set<UUID> targetIds = new HashSet<>();
        for (TamableAnimal tame : challengerGroup) {
            applySitFollowOverride(tame, false);
            challengerIds.add(tame.getUUID());
        }
        for (TamableAnimal tame : targetGroup) {
            applySitFollowOverride(tame, false);
            targetIds.add(tame.getUUID());
        }

        for (TamableAnimal own : challengerGroup) {
            TamableAnimal enemy = nearestLoadedOpponent(own, targetGroup);
            if (enemy != null) own.setTarget(enemy);
        }
        for (TamableAnimal own : targetGroup) {
            TamableAnimal enemy = nearestLoadedOpponent(own, challengerGroup);
            if (enemy != null) own.setTarget(enemy);
        }
        TameDuelManager.startGroupDuel(source.getServer(), challenger.getUUID(), challengerIds, targetPlayer.getUUID(), targetIds);

        String challengerSelectionText = duelSelectionLabel(invite.challengerSelection);
        String targetSelectionText = duelSelectionLabel(targetSelection);
        challenger.sendSystemMessage(Component.literal(targetPlayer.getName().getString() + " accepted duel: " + challengerSelectionText + " vs " + targetSelectionText + ".").withStyle(ChatFormatting.RED));
        targetPlayer.sendSystemMessage(Component.literal("Duel started: " + targetSelectionText + " vs " + challengerSelectionText + ".").withStyle(ChatFormatting.RED));
        return 1;
    }

    private static int duelDecline(CommandSourceStack source, String challengerName) {
        ServerPlayer targetPlayer = source.getPlayer();
        cleanupExpiredDuelInvites();
        ServerPlayer challenger = source.getServer().getPlayerList().getPlayerByName(challengerName);
        if (challenger == null) return error(targetPlayer, "Challenger is not online.");
        DuelInvite invite = popDuelInvite(targetPlayer.getUUID(), challenger.getUUID());
        if (invite == null) return error(targetPlayer, "No pending duel invite from " + challengerName + ".");

        targetPlayer.sendSystemMessage(Component.literal("Declined duel invite from " + challengerName + ".").withStyle(ChatFormatting.YELLOW));
        challenger.sendSystemMessage(Component.literal(targetPlayer.getName().getString() + " declined your duel invite.").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int duelInbox(CommandSourceStack source) {
        ServerPlayer targetPlayer = source.getPlayer();
        cleanupExpiredDuelInvites();
        Map<UUID, DuelInvite> incoming = DUEL_INVITES.get(targetPlayer.getUUID());
        if (incoming == null || incoming.isEmpty()) {
            targetPlayer.sendSystemMessage(Component.literal("No pending duel invites.").withStyle(ChatFormatting.GRAY));
            return 1;
        }

        targetPlayer.sendSystemMessage(Component.literal("Pending duel invites:").withStyle(ChatFormatting.GOLD));
        for (DuelInvite invite : incoming.values()) {
            ServerPlayer challenger = source.getServer().getPlayerList().getPlayer(invite.challengerUuid);
            String challengerName = challenger == null ? invite.challengerUuid.toString() : challenger.getName().getString();
            targetPlayer.sendSystemMessage(Component.literal("- " + challengerName + " using " + duelSelectionLabel(invite.challengerSelection)).withStyle(ChatFormatting.AQUA));
        }
        return 1;
    }

    private static int duelForfeit(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        int ended = TameDuelManager.endDuelsForOwner(source.getServer(), p.getUUID());
        if (ended <= 0) {
            return error(p, "No active tame duels to forfeit.");
        }
        p.sendSystemMessage(Component.literal("Forfeited " + ended + " group duel(s).").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static DuelInvite popDuelInvite(UUID targetUuid, UUID challengerUuid) {
        Map<UUID, DuelInvite> incoming = DUEL_INVITES.get(targetUuid);
        if (incoming == null) return null;
        DuelInvite invite = incoming.remove(challengerUuid);
        if (incoming.isEmpty()) {
            DUEL_INVITES.remove(targetUuid);
        }
        return invite;
    }

    private static void cleanupExpiredDuelInvites() {
        long now = System.currentTimeMillis();
        List<UUID> emptyTargets = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, DuelInvite>> entry : DUEL_INVITES.entrySet()) {
            Map<UUID, DuelInvite> incoming = entry.getValue();
            incoming.entrySet().removeIf(e -> (now - e.getValue().createdAtMs) > DUEL_INVITE_TIMEOUT_MS);
            if (incoming.isEmpty()) {
                emptyTargets.add(entry.getKey());
            }
        }
        for (UUID target : emptyTargets) {
            DUEL_INVITES.remove(target);
        }
    }

    private static void sendTameStats(CommandSourceStack source, ServerPlayer receiver, TameData d, boolean detailed) {
        receiver.sendSystemMessage(Component.literal("=== " + d.name + " ===").withStyle(ChatFormatting.GOLD));
        receiver.sendSystemMessage(Component.literal("Status " + (isDeadEntry(d.uuid) ? "dead" : "alive")).withStyle(isDeadEntry(d.uuid) ? ChatFormatting.GRAY : ChatFormatting.GREEN));
        receiver.sendSystemMessage(Component.literal("Lvl " + d.level + "  XP " + d.xp + "/" + d.xpToNext).withStyle(ChatFormatting.YELLOW));
        receiver.sendSystemMessage(Component.literal("K " + d.kills + "  A " + d.assists + "  D " + d.deaths).withStyle(ChatFormatting.AQUA));
        receiver.sendSystemMessage(Component.literal("Mode " + TameMode.byId(d.mode).key() + "  Class " + (d.tameClass == null ? "-" : d.tameClass.name().toLowerCase(Locale.ROOT))).withStyle(ChatFormatting.GREEN));
        receiver.sendSystemMessage(Component.literal("Group " + (d.group == null || d.group.isBlank() ? "-" : d.group)).withStyle(ChatFormatting.DARK_GREEN));
        receiver.sendSystemMessage(Component.literal("Bed " + formatBedLocation(d)).withStyle(ChatFormatting.DARK_AQUA));
        if (!detailed) {
            return;
        }

        TamableAnimal loaded = findLoadedTameByUuid(source, d.uuid);
        double actualHp = getBaseAttributeValue(loaded, Attributes.MAX_HEALTH);
        double actualDmg = getBaseAttributeValue(loaded, Attributes.ATTACK_DAMAGE);
        double actualSpd = getBaseAttributeValue(loaded, Attributes.MOVEMENT_SPEED);
        double actualArm = getBaseAttributeValue(loaded, Attributes.ARMOR);
        double actualTgh = getBaseAttributeValue(loaded, Attributes.ARMOR_TOUGHNESS);
        double actualKb = getBaseAttributeValue(loaded, Attributes.ATTACK_KNOCKBACK);
        double actualKbr = getBaseAttributeValue(loaded, Attributes.KNOCKBACK_RESISTANCE);

        receiver.sendSystemMessage(Component.literal("Actual HP" + fmtStat(actualHp) + " DMG" + fmtStat(actualDmg) + " SPD" + fmtStat(actualSpd)
                + " ARM" + fmtStat(actualArm) + " TGH" + fmtStat(actualTgh)
                + " KB" + fmtStat(actualKb) + " KBR" + fmtStat(actualKbr)).withStyle(ChatFormatting.GRAY));
        receiver.sendSystemMessage(Component.literal("Base HP+" + fmt(d.bonusHealth) + " DMG+" + fmt(d.bonusDamage) + " SPD+" + fmt(d.bonusSpeed)
                + " ARM+" + fmt(d.bonusArmor) + " TGH+" + fmt(d.bonusArmorToughness)
                + " KB+" + fmt(d.bonusKnockback) + " KBR+" + fmt(d.bonusKnockbackResist)).withStyle(ChatFormatting.GRAY));
        receiver.sendSystemMessage(Component.literal("Attributes: " + formatLevelsCompact(d.attributeLevels)).withStyle(ChatFormatting.LIGHT_PURPLE));
        receiver.sendSystemMessage(Component.literal("Abilities: " + formatLevelsCompact(d.abilityLevels)).withStyle(ChatFormatting.BLUE));
    }

    private static int inspectPet(CommandSourceStack source, String petName) {
        ServerPlayer player = source.getPlayer();
        TameData data = findOwnedTameAny(player.getUUID(), petName);
        if (data == null) {
            return error(player, "Pet not found.");
        }

        TamableAnimal tame = findLoadedTameByUuid(source, data.uuid);
        if (tame == null) {
            player.sendSystemMessage(Component.literal("=== " + data.name + " ===").withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.literal("Inspect needs the tame to be loaded for exact runtime DPS math.").withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("Stored abilities: " + formatLevelsCompact(data.abilityLevels)).withStyle(ChatFormatting.BLUE));
            player.sendSystemMessage(Component.literal("Stored attributes: " + formatLevelsCompact(data.attributeLevels)).withStyle(ChatFormatting.LIGHT_PURPLE));
            return 0;
        }

        double abilityPowerMultiplier = inspectAbilityPowerMultiplier(data);
        int attackAbilityCount = inspectOwnedAttackAbilityCount(data);
        double cooldownMultiplier = inspectAttackCooldownMultiplier(attackAbilityCount);

        player.sendSystemMessage(Component.literal("=== Inspect " + data.name + " ===").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("Bonus DMG " + fmt(data.bonusDamage) + "  ability_power x" + fmt(abilityPowerMultiplier)
                + "  attack abilities " + attackAbilityCount + "  attack cooldown x" + fmt(cooldownMultiplier)).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("Runtime multipliers: single x" + fmt(TLAdminRuntimeSettings.singleTargetAbilityDamageMultiplier())
                + "  aoe x" + fmt(TLAdminRuntimeSettings.aoeAbilityDamageMultiplier())
                + "  cooldown nerf " + fmt(TLAdminRuntimeSettings.abilityCountCooldownNerfPercent()) + "%").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("Assumptions: exact where formula is known; projectile/utility abilities may be noted as situational. Crossbow assumes all arrows hit one target.").withStyle(ChatFormatting.DARK_GRAY));

        List<String> abilityLines = buildAbilityInspectLines(tame, data);
        if (abilityLines.isEmpty()) {
            player.sendSystemMessage(Component.literal("Abilities: none").withStyle(ChatFormatting.BLUE));
        } else {
            player.sendSystemMessage(Component.literal("Abilities").withStyle(ChatFormatting.BLUE));
            for (String line : abilityLines) {
                player.sendSystemMessage(Component.literal("- " + line).withStyle(ChatFormatting.GRAY));
            }
        }

        List<String> attributeLines = buildAttributeInspectLines(data);
        if (attributeLines.isEmpty()) {
            player.sendSystemMessage(Component.literal("Attributes: none").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            player.sendSystemMessage(Component.literal("Attributes").withStyle(ChatFormatting.LIGHT_PURPLE));
            for (String line : attributeLines) {
                player.sendSystemMessage(Component.literal("- " + line).withStyle(ChatFormatting.GRAY));
            }
        }
        return 1;
    }

    private static int searchTamesByAbility(CommandSourceStack source, String abilityId) {
        ServerPlayer player = source.getPlayer();
        String id = abilityId == null ? "" : abilityId.trim().toLowerCase(Locale.ROOT);
        if (id.isBlank()) return error(player, "Ability name cannot be blank.");
        if (!LevelSystem.knownAbilityIds().contains(id)) return error(player, "Unknown ability: " + id);
        return searchOwnedTames(source, "Ability", id, data -> LevelSystem.hasAbility(data, id));
    }

    private static int searchTamesByAttribute(CommandSourceStack source, String attributeId) {
        ServerPlayer player = source.getPlayer();
        String id = attributeId == null ? "" : attributeId.trim().toLowerCase(Locale.ROOT);
        if (id.isBlank()) return error(player, "Attribute name cannot be blank.");
        if (!LevelSystem.knownAttributeIds().contains(id)) return error(player, "Unknown attribute: " + id);
        return searchOwnedTames(source, "Attribute", id, data -> LevelSystem.getAttributeLevel(data, id) > 0);
    }

    private static int searchTamesByClass(CommandSourceStack source, String className) {
        ServerPlayer player = source.getPlayer();
        String id = className == null ? "" : className.trim().toLowerCase(Locale.ROOT);
        if (id.isBlank()) return error(player, "Class name cannot be blank.");
        TameClass tameClass;
        try {
            tameClass = TameClass.valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return error(player, "Unknown class: " + id);
        }
        return searchOwnedTames(source, "Class", tameClass.name().toLowerCase(Locale.ROOT), data -> data.tameClass == tameClass);
    }

    private static int searchOwnedTames(CommandSourceStack source, String category, String query, java.util.function.Predicate<TameData> predicate) {
        ServerPlayer player = source.getPlayer();
        List<TameData> matches = new ArrayList<>();
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.name == null || !player.getUUID().equals(data.ownerUUID)) continue;
            if (!predicate.test(data)) continue;
            matches.add(data);
        }
        if (matches.isEmpty()) {
            return error(player, "No tames matched " + category.toLowerCase(Locale.ROOT) + " " + query + ".");
        }

        matches.sort(Comparator
                .comparing((TameData data) -> isDeadEntry(data.uuid))
                .thenComparing((TameData data) -> !isLoadedAnywhere(source.getServer(), data.uuid))
                .thenComparing((TameData data) -> data.name == null ? "" : data.name.toLowerCase(Locale.ROOT))
                .thenComparing((TameData data) -> -data.level));

        player.sendSystemMessage(Component.literal("=== Search " + category + ": " + query + " ===").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("Matches: " + matches.size()).withStyle(ChatFormatting.GRAY));
        for (TameData data : matches) {
            boolean dead = isDeadEntry(data.uuid);
            boolean loaded = !dead && isLoadedAnywhere(source.getServer(), data.uuid);
            String state = dead ? "dead" : loaded ? "loaded" : "unloaded";
            ChatFormatting color = dead ? ChatFormatting.DARK_GRAY : loaded ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
            String extra = switch (category) {
                case "Ability" -> " L" + Math.max(1, LevelSystem.getAbilityLevel(data, query));
                case "Attribute" -> " L" + Math.max(1, LevelSystem.getAttributeLevel(data, query));
                case "Class" -> "";
                default -> "";
            };
            player.sendSystemMessage(Component.literal("- [" + data.level + "] " + data.name + " (" + state + ")" + extra).withStyle(color));
        }
        return 1;
    }

    private static List<String> buildAbilityInspectLines(TamableAnimal tame, TameData data) {
        List<String> lines = new ArrayList<>();
        for (String id : LevelSystem.knownAbilityIds()) {
            if (!LevelSystem.hasAbility(data, id)) continue;
            int level = Math.max(1, LevelSystem.getAbilityLevel(data, id));
            String line = buildAbilityInspectLine(tame, data, id, level);
            if (line != null && !line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static String buildAbilityInspectLine(TamableAnimal tame, TameData data, String id, int level) {
        return switch (id) {
            case "arrow_shot", "creeper_explosion", "ghast_fireball", "blaze_attack", "guardian_beam",
                    "elder_guardian_beam", "trident", "crossbow", "fishing", "dash", "dragon_fireball",
                    "llama_spit", "snowball_shot", "lightning_strike", "warden_scream", "wither_skull",
                    "evoker_fangs", "shulker_bullet", "sky_launch" -> inspectReworkedAbilityLine(data, id, level);
            case "shadow_hands" -> id + " L" + level + ": complex sustained runtime based on hand count, windup, and target uptime; exact closed-form DPS not reliable from command";
            case "battle_strength", "defensive_aura", "ender_pearl_jump", "berserker", "bloodlust", "retaliation_slow", "immunity_frame", "deflection", "defusal", "psychic_wall", "healing_aura", "healing_bottle", "guardian_repulse", "last_stand_fury", "shield_block",
                    "guardian_intercept", "emergency_shield", "body_block", "battlefield_medic", "triage_pulse", "revitalizing_presence", "cleanse_touch", "pack_guard", "life_gift" ->
                    id + " L" + level + ": utility/support ability, no fixed direct DPS";
            default -> id + " L" + level + ": no inspect profile";
        };
    }

    private static String inspectDamageAbilityLine(String id, int level, double castDamage, long cooldownTicks) {
        double cooldownSeconds = cooldownTicks / 20.0D;
        double dps = cooldownSeconds <= 0.0D ? 0.0D : castDamage / cooldownSeconds;
        return id + " L" + level + ": " + fmt(castDamage) + " dmg / " + fmt(cooldownSeconds) + "s = " + fmt(dps) + " DPS";
    }

    private static String inspectReworkedAbilityLine(TameData data, String id, int level) {
        long budgetTicks = TameAbilityEvents.offensiveAbilityBudgetCooldownTicks(id, level);
        long effectiveTicks = inspectEffectiveCooldownTicks(data, id, budgetTicks);
        double castDamage = TameAbilityEvents.offensiveAbilityCastDamage(data, id, level);
        String suffix = switch (id) {
            case "creeper_explosion", "dragon_fireball" -> " per target";
            case "ghast_fireball" -> " per target in blast";
            case "warden_scream" -> " per target in beam";
            case "elder_guardian_beam" -> " + mining fatigue";
            case "lightning_strike" -> " + visual lightning";
            case "fishing" -> " + pull";
            case "dash" -> " per target hit in sweep";
            case "crossbow" -> " total cast damage assuming all " + Math.max(1, level) + " arrows hit";
            case "shulker_bullet" -> " + levitation utility";
            case "sky_launch" -> " + launch";
            default -> "";
        };
        return inspectDamageAbilityLine(id, level, castDamage, effectiveTicks) + suffix;
    }

    private static List<String> buildAttributeInspectLines(TameData data) {
        List<String> lines = new ArrayList<>();
        for (String id : LevelSystem.knownAttributeIds()) {
            int level = LevelSystem.getAttributeLevel(data, id);
            if (level <= 0) continue;
            String line = buildAttributeInspectLine(data, id, level);
            if (line != null && !line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static String buildAttributeInspectLine(TameData data, String id, int level) {
        return switch (id) {
            case "lifesteal" -> id + " L" + level + ": heals " + fmt((0.04D + level * 0.02D) * 100.0D) + "% of actual final damage on real base attacks only";
            case "killer" -> id + " L" + level + ": damage x(1 + missingHP% * " + fmt(0.20D + 0.10D * level) + ")";
            case "pacifist" -> id + " L" + level + ": damage x(1 + targetHP% * " + fmt(0.20D + 0.10D * level) + ")";
            case "bosskiller" -> id + " L" + level + ": on hit vs large-max-HP targets, grants short str/res/speed buffs; threshold " + Math.max(40, 100 - (level - 1) * 10) + " HP";
            case "pierce" -> id + " L" + level + ": ignores " + fmt(Math.min(0.80D, 0.20D + Math.max(0, level - 1) * 0.15D) * 100.0D) + "% of target armor";
            case "smite" -> id + " L" + level + ": +" + fmt((0.15D + 0.10D * level) * 100.0D) + "% damage vs undead";
            case "bane_of_arthropods" -> id + " L" + level + ": +" + fmt((0.15D + 0.10D * level) * 100.0D) + "% damage vs arthropods + slowness";
            case "lightningfang" -> id + " L" + level + ": " + fmt(Math.min(0.35D, 0.10D + 0.05D * level) * 100.0D) + "% proc for " + fmt((2.0D + 2.0D * level) * (1.0D + 0.02D * Math.max(0.0D, data.bonusDamage))) + " bonus damage";
            case "firefang" -> id + " L" + level + ": burns target for " + (2 + level) + "s on hit" + (level >= 5 ? " and adds +2 damage" : level >= 3 ? " and adds +1 damage" : "");
            case "poison_fang" -> id + " L" + level + ": applies Poison " + (40 + level * 20) + " ticks on hit, amp " + (level >= 3 ? "II" : "I");
            case "witherfang" -> id + " L" + level + ": applies Wither " + (40 + level * 20) + " ticks on hit, amp " + (level >= 4 ? "II" : "I");
            case "frost_fang" -> id + " L" + level + ": " + fmt(Math.min(0.45D, 0.15D + Math.max(0, level - 1) * 0.075D) * 100.0D) + "% slow proc, amp " + (level >= 5 ? "III" : level >= 3 ? "II" : "I");
            case "chain_lightning" -> id + " L" + level + ": " + fmt(Math.min(0.28D, 0.12D + Math.max(0, level - 1) * 0.04D) * 100.0D) + "% proc; chains up to " + (1 + level) + " targets for " + fmt((2.0D + level) * (1.0D + 0.01D * Math.max(0.0D, data.bonusDamage))) + " aoe damage each";
            case "sweeping_edge" -> id + " L" + level + ": splash radius " + fmt(1.4D + Math.max(0, level - 1) * 0.20D) + ", splash scaling " + fmt(Math.min(0.50D, 0.20D + Math.max(0, level - 1) * 0.075D) * 100.0D) + "%";
            case "victim_siphon" -> id + " L" + level + ": on kill heals " + fmt(Math.min(0.35D, 0.04D + 0.04D * level) * 100.0D) + "% of victim max HP";
            case "killexploder" -> id + " L" + level + ": on kill/assist explodes for " + fmt((4.0D + Math.max(0, level - 1) * 1.5D) * (1.0D + 0.01D * Math.max(0.0D, data.bonusDamage))) + " aoe damage, radius " + fmt(2.0D + Math.max(0, level - 1) * 0.40D);
            case "positive_effect_steal" -> id + " L" + level + ": " + fmt(Math.min(0.38D, 0.08D + 0.06D * level) * 100.0D) + "% chance to steal one beneficial effect on hit";
            case "negative_effect_transfer" -> id + " L" + level + ": transfers harmful effects with x" + fmt(inspectAttributeLevelMultiplier(level)) + " duration";
            case "feather_falling" -> id + " L" + level + ": reduces fall damage by " + fmt(Math.min(0.70D, 0.20D + Math.max(0, level - 1) * 0.125D) * 100.0D) + "%";
            case "explosion_resistance" -> id + " L" + level + ": reduces explosion damage by " + fmt(Math.min(0.55D, 0.15D + Math.max(0, level - 1) * 0.10D) * 100.0D) + "%";
            case "regeneration" -> id + " L" + level + ": heals " + fmt(0.5D + 0.5D * level) + " every " + fmt(level >= 5 ? 1.0D : level >= 3 ? 1.5D : 2.0D) + "s while damaged";
            case "ability_power" -> id + " L" + level + ": ability damage/effects x" + fmt(1.0D + level * 0.10D);
            case "emergency_cooldown_reduction" -> id + " L" + level + ": at <=" + fmt((0.25D + Math.max(0, level - 1) * 0.025D) * 100.0D) + "% HP, " + fmt(Math.min(0.38D, 0.08D + 0.06D * level) * 100.0D) + "% chance to force next cooldown to 1s";
            case "totem" -> id + " L" + level + ": lethal save, cooldown " + fmt(Math.max(1L, 10L - Math.max(0, level - 1))) + "m";
            case "magnetic" -> id + " L" + level + ": pull utility; stronger target drag each level";
            case "speed", "strength", "resistance", "jump_boost" -> id + " L" + level + ": self-buff amplifier " + (level >= 5 ? "III" : level >= 3 ? "II" : "I");
            case "fire_resistance", "poison_resistance" -> id + " L" + level + ": binary resistance effect";
            case "comfort" -> id + " L" + level + ": when out of battle, heals " + fmt(level) + " every 5.0s";
            case "health_siphon", "bubbling", "herding", "amphibious", "void_cloud", "charisma", "disc_jockey", "warping_bite", "ore_scenting", "gluttonous", "tethered_teleport", "muffled", "blazing_protection", "rejuvenation", "linked_inventory" ->
                    id + " L" + level + ": utility/survival attribute; inspect is situational rather than fixed DPS";
            default -> id + " L" + level + ": no inspect profile";
        };
    }

    private static double inspectAttributeLevelMultiplier(int level) {
        int safeLevel = Math.max(1, level);
        int tier = (safeLevel - 1) / 5;
        int step = (safeLevel - 1) % 5;
        return Math.pow(2.0D, tier) * (1.0D + 0.25D * step);
    }

    private static double inspectAbilityPowerMultiplier(TameData data) {
        return 1.0D + (LevelSystem.getAttributeLevel(data, "ability_power") * 0.10D);
    }

    private static double inspectSingleTargetDamage(double amount) {
        return amount * TLAdminRuntimeSettings.singleTargetAbilityDamageMultiplier();
    }

    private static double inspectAoeDamage(double amount) {
        return amount * TLAdminRuntimeSettings.aoeAbilityDamageMultiplier();
    }

    private static long inspectEffectiveCooldownTicks(TameData data, String sourceId, long baseTicks) {
        return Math.max(1L, Math.round(baseTicks * inspectAbilityCooldownMultiplier(data, sourceId)));
    }

    private static double inspectAbilityCooldownMultiplier(TameData data, String sourceId) {
        if (!LevelSystem.isAttackAbility(sourceId)) {
            return 1.0D;
        }
        return inspectAttackCooldownMultiplier(inspectOwnedAttackAbilityCount(data));
    }

    private static double inspectAttackCooldownMultiplier(int abilityCount) {
        if (abilityCount <= 1) {
            return 1.0D;
        }
        double percent = TLAdminRuntimeSettings.abilityCountCooldownNerfPercent() / 100.0D;
        return 1.0D + percent * (Math.log(abilityCount) / Math.log(2.0D));
    }

    private static int inspectOwnedAttackAbilityCount(TameData data) {
        if (data == null) {
            return 0;
        }
        int count = 0;
        for (String id : LevelSystem.knownAbilityIds()) {
            if (LevelSystem.hasAbility(data, id) && LevelSystem.isAttackAbility(id)) {
                count++;
            }
        }
        return count;
    }

    private static String formatBedLocation(TameData data) {
        if (data == null || !data.hasPetBed || data.petBedDimension == null || data.petBedDimension.isBlank()) {
            return "-";
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(data.petBedDimension);
        String dimensionName = dimensionId == null ? data.petBedDimension : dimensionId.getPath();
        return dimensionName + ": " + data.petBedX + " " + data.petBedY + " " + data.petBedZ;
    }

    private static double getBaseAttributeValue(TamableAnimal tame, Attribute attribute) {
        if (tame == null || attribute == null) {
            return Double.NaN;
        }
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) {
            return Double.NaN;
        }
        return instance.getBaseValue();
    }

    private static String fmtStat(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "?";
        }
        return fmt(value);
    }

    private static int setMode(CommandSourceStack source, String pet, String modeName) {
        ServerPlayer p = source.getPlayer();
        TameData d = findOwnedTame(p.getUUID(), pet);
        if (d == null) return error(p, "Pet not found.");
        TameMode mode = TameMode.tryByName(modeName);
        if (mode == null) return error(p, "Invalid mode.");
        d.mode = mode.id();
        Entity e = p.serverLevel().getEntity(d.uuid);
        if (e instanceof TamableAnimal ta && mode != TameMode.PASSIVE) ta.setOrderedToSit(false);
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Mode set to " + mode.key() + " for " + d.name + "."));
        return 1;
    }

    private static int groupMode(CommandSourceStack source, String group, String modeName) {
        ServerPlayer p = source.getPlayer();
        TameMode mode = TameMode.tryByName(modeName);
        if (mode == null) return error(p, "Invalid mode.");
        int count = 0;
        for (TameData d : ownedGroup(p.getUUID(), group)) {
            d.mode = mode.id();
            Entity e = p.serverLevel().getEntity(d.uuid);
            if (e instanceof TamableAnimal ta && mode != TameMode.PASSIVE) ta.setOrderedToSit(false);
            count++;
        }
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Set mode " + mode.key() + " for " + count + " tames."));
        return 1;
    }

    private static int typeMode(CommandSourceStack source, String typeFilter, String modeName) {
        ServerPlayer p = source.getPlayer();
        TameMode mode = TameMode.tryByName(modeName);
        if (mode == null) return error(p, "Invalid mode.");
        int count = 0;
        for (TameData d : ownedType(p.getUUID(), typeFilter)) {
            d.mode = mode.id();
            Entity e = p.serverLevel().getEntity(d.uuid);
            if (e instanceof TamableAnimal ta && mode != TameMode.PASSIVE) {
                applySitFollowOverride(ta, false);
            }
            count++;
        }
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Set mode " + mode.key() + " for " + count + " tames of type '" + typeFilter + "'."));
        return 1;
    }

    private static int allMode(CommandSourceStack source, String modeName) {
        ServerPlayer p = source.getPlayer();
        TameMode mode = TameMode.tryByName(modeName);
        if (mode == null) return error(p, "Invalid mode.");
        int count = 0;
        for (TameData d : ownedTames(p.getUUID())) {
            Entity e = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (!(e instanceof TamableAnimal ta) || !ta.isAlive()) continue;
            d.mode = mode.id();
            count++;
        }
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Set mode " + mode.key() + " for " + count + " loaded tames."));
        return 1;
    }

    private static int stateMode(CommandSourceStack source, String stateName, String modeName) {
        ServerPlayer p = source.getPlayer();
        MovementOrder selectedState = parseMovementOrder(stateName);
        if (selectedState == null) return error(p, "Invalid state. Use follow, wander, or sit.");
        TameMode mode = TameMode.tryByName(modeName);
        if (mode == null) return error(p, "Invalid mode.");

        int count = 0;
        for (TamableAnimal tame : loadedOwnedStateTames(source, p.getUUID(), selectedState)) {
            TameData d = TameRegistry.get(tame.getUUID());
            if (d == null) continue;
            d.mode = mode.id();
            if (mode != TameMode.PASSIVE) {
                applySitFollowOverride(tame, false);
            }
            count++;
        }
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Set mode " + mode.key() + " for " + count + " loaded " + movementLabel(selectedState) + " tames."));
        return 1;
    }

    private static int groupSet(CommandSourceStack source, String pet, String group) {
        ServerPlayer p = source.getPlayer();
        TameData d = findOwnedTame(p.getUUID(), pet);
        if (d == null) return error(p, "Pet not found.");
        d.group = group;
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Group set for " + d.name + " -> " + group));
        return 1;
    }

    private static int groupRemove(CommandSourceStack source, String pet, String group) {
        ServerPlayer p = source.getPlayer();
        TameData d = findOwnedTame(p.getUUID(), pet);
        if (d == null) return error(p, "Pet not found.");
        if (d.group == null || d.group.isBlank()) return error(p, d.name + " has no group.");
        if (!d.group.equalsIgnoreCase(group)) return error(p, d.name + " is not in group " + group + ".");
        d.group = "";
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Removed " + d.name + " from group " + group + "."));
        return 1;
    }

    private static int groupClear(CommandSourceStack source, String pet) {
        ServerPlayer p = source.getPlayer();
        TameData d = findOwnedTame(p.getUUID(), pet);
        if (d == null) return error(p, "Pet not found.");
        d.group = "";
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Removed " + d.name + " from all groups."));
        return 1;
    }

    private static int groupTames(CommandSourceStack source, String group) {
        ServerPlayer p = source.getPlayer();
        List<TameData> t = ownedGroup(p.getUUID(), group);
        if (t.isEmpty()) return error(p, "No tames in group.");
        for (TameData d : t) p.sendSystemMessage(Component.literal("- " + d.name + " [Lvl " + d.level + "]"));
        return 1;
    }

    private static int groupOverview(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        Map<String, List<TameData>> groups = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            if (isDeadEntry(d.uuid)) continue;
            if (d.group == null || d.group.isBlank()) continue;
            groups.computeIfAbsent(d.group, ignored -> new ArrayList<>()).add(d);
        }
        if (groups.isEmpty()) return error(p, "No groups found.");

        int index = 1;
        for (Map.Entry<String, List<TameData>> entry : groups.entrySet()) {
            List<String> names = new ArrayList<>();
            for (TameData d : entry.getValue()) {
                names.add(d.name);
            }
            names.sort(String::compareToIgnoreCase);
            String joined = String.join(", ", names);
            p.sendSystemMessage(
                    Component.literal(index + ". ").withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(entry.getKey() + ": ").withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(joined).withStyle(ChatFormatting.YELLOW))
            );
            index++;
        }
        return 1;
    }

    private static int groupTp(CommandSourceStack source, String group) {
        ServerPlayer p = source.getPlayer();
        List<TameData> requested = ownedGroup(p.getUUID(), group);
        int deadSkipped = ownedDeadGroup(p.getUUID(), group).size();
        List<TamableAnimal> targets = new ArrayList<>();
        int queued = 0;
        int queueFailed = 0;
        List<String> failedQueueNames = new ArrayList<>();
        int cost = 0;
        int crossDimension = 0;
        for (TameData d : requested) {
            TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (ta == null) {
                UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, p, d);
                if (unloaded.success) {
                    queued++;
                } else {
                    queueFailed++;
                    failedQueueNames.add((d.name == null ? "unknown" : d.name) + " (" + unloaded.error + ")");
                }
                continue;
            }
            boolean cross = isCrossDimension(ta, p);
            targets.add(ta);
            cost += teleportCostFor(d, cross);
            if (cross) crossDimension++;
        }
        if (!payTeleportXp(p, cost)) return 0;
        for (TamableAnimal ta : targets) teleportTameToPlayer(ta, p);
        sendTeleportSummary(p, "TP group " + group, targets.size(), queued, deadSkipped, queueFailed, cost, crossDimension);
        if (!failedQueueNames.isEmpty()) {
            p.sendSystemMessage(Component.literal("Unloaded tp failures: " + String.join("; ", failedQueueNames)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int typeTp(CommandSourceStack source, String typeFilter) {
        ServerPlayer p = source.getPlayer();
        List<TameData> requested = ownedType(p.getUUID(), typeFilter);
        if (requested.isEmpty()) return error(p, "No tames found for type '" + typeFilter + "'.");
        int deadSkipped = ownedDeadType(p.getUUID(), typeFilter).size();

        List<TamableAnimal> targets = new ArrayList<>();
        int queued = 0;
        int queueFailed = 0;
        List<String> failedQueueNames = new ArrayList<>();
        int cost = 0;
        int crossDimension = 0;
        for (TameData d : requested) {
            TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (ta == null) {
                UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, p, d);
                if (unloaded.success) {
                    queued++;
                } else {
                    queueFailed++;
                    failedQueueNames.add((d.name == null ? "unknown" : d.name) + " (" + unloaded.error + ")");
                }
                continue;
            }
            boolean cross = isCrossDimension(ta, p);
            targets.add(ta);
            cost += teleportCostFor(d, cross);
            if (cross) crossDimension++;
        }
        if (!payTeleportXp(p, cost)) return 0;
        for (TamableAnimal ta : targets) teleportTameToPlayer(ta, p);
        sendTeleportSummary(p, "TP type " + typeFilter, targets.size(), queued, deadSkipped, queueFailed, cost, crossDimension);
        if (!failedQueueNames.isEmpty()) {
            p.sendSystemMessage(Component.literal("Unloaded tp failures: " + String.join("; ", failedQueueNames)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int groupMovementState(CommandSourceStack source, String group, MovementOrder order) {
        ServerPlayer p = source.getPlayer();
        int count = 0;
        for (TameData d : ownedGroup(p.getUUID(), group)) {
            Entity e = p.serverLevel().getEntity(d.uuid);
            if (e instanceof TamableAnimal ta && ta.isTame()) {
                applyMovementOverride(ta, order);
                count++;
            }
        }
        p.sendSystemMessage(Component.literal("Set " + count + " tames in group to " + movementLabel(order) + "."));
        return 1;
    }

    private static int typeMovementState(CommandSourceStack source, String typeFilter, MovementOrder order) {
        ServerPlayer p = source.getPlayer();
        int count = 0;
        for (TameData d : ownedType(p.getUUID(), typeFilter)) {
            Entity e = p.serverLevel().getEntity(d.uuid);
            if (e instanceof TamableAnimal ta && ta.isTame()) {
                applyMovementOverride(ta, order);
                count++;
            }
        }
        p.sendSystemMessage(Component.literal("Set " + count + " tames of type '" + typeFilter + "' to " + movementLabel(order) + "."));
        return 1;
    }

    private static int stateMovementState(CommandSourceStack source, String stateName, MovementOrder targetOrder) {
        ServerPlayer p = source.getPlayer();
        MovementOrder selectedState = parseMovementOrder(stateName);
        if (selectedState == null) return error(p, "Invalid state. Use follow, wander, or sit.");
        List<TamableAnimal> selected = loadedOwnedStateTames(source, p.getUUID(), selectedState);
        for (TamableAnimal tame : selected) {
            applyMovementOverride(tame, targetOrder);
        }
        p.sendSystemMessage(Component.literal("Set " + selected.size() + " loaded " + movementLabel(selectedState) + " tames to " + movementLabel(targetOrder) + "."));
        return 1;
    }

    private static int setPetMovementState(CommandSourceStack source, String pet, MovementOrder order) {
        ServerPlayer p = source.getPlayer();
        TameData d = findOwnedTame(p.getUUID(), pet);
        if (d == null) return error(p, "Pet not found.");
        Entity e = p.serverLevel().getEntity(d.uuid);
        if (!(e instanceof TamableAnimal ta) || !ta.isTame()) return error(p, "Pet is not loaded.");
        applyMovementOverride(ta, order);
        p.sendSystemMessage(Component.literal("Set " + d.name + " to " + movementLabel(order) + "."));
        return 1;
    }

    private static int setMovementState(CommandSourceStack source, boolean nearbyOnly, MovementOrder order) {
        ServerPlayer p = source.getPlayer();
        int count = 0;
        for (TamableAnimal ta : p.level().getEntitiesOfClass(TamableAnimal.class, p.getBoundingBox().inflate(nearbyOnly ? 32 : 500))) {
            if (!ta.isTame() || !p.getUUID().equals(ta.getOwnerUUID())) continue;
            applyMovementOverride(ta, order);
            count++;
        }
        p.sendSystemMessage(Component.literal("Set " + count + " tames to " + movementLabel(order) + "."));
        return 1;
    }

    private static int setMovementStateAllLoaded(CommandSourceStack source, MovementOrder order) {
        ServerPlayer p = source.getPlayer();
        int count = 0;
        for (TamableAnimal tame : loadedOwnedAllTames(source, p.getUUID())) {
            if (!tame.isTame()) continue;
            applyMovementOverride(tame, order);
            count++;
        }
        p.sendSystemMessage(Component.literal("Set " + count + " loaded tames to " + movementLabel(order) + "."));
        return 1;
    }

    private static int teleportPet(CommandSourceStack source, String pet) {
        ServerPlayer p = source.getPlayer();
        TameData d = findOwnedTame(p.getUUID(), pet);
        if (d == null) return error(p, "Pet not found.");
        TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
        if (ta == null) {
            UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, p, d);
            if (!unloaded.success) return error(p, "Failed to queue unloaded tp: " + unloaded.error);
            p.sendSystemMessage(Component.literal("Queued unloaded tp for " + d.name + " to your position.").withStyle(ChatFormatting.GREEN));
            return 1;
        }
        boolean crossDimension = isCrossDimension(ta, p);
        int cost = teleportCostFor(d, crossDimension);
        if (!payTeleportXp(p, cost)) return 0;
        teleportTameToPlayer(ta, p);
        p.sendSystemMessage(Component.literal("Teleported " + d.name + " (-" + cost + " XP points" + (crossDimension ? ", cross-dimension" : "") + ")."));
        return 1;
    }

    private static int recoverPet(CommandSourceStack source, String pet) {
        ServerPlayer p = source.getPlayer();
        TameData data = findOwnedTame(p.getUUID(), pet);
        if (data == null) return error(p, "Pet not found.");
        if (isEffectivelyLoaded(source, p, data)) {
            return error(p, "Pet is already loaded/carried. Recover is only for lost/unloaded tames.");
        }
        RecoverResult recovered = recoverPetEntity(source, p, data);
        if (recovered.entity == null) return error(p, "Failed to recover tame: " + recovered.error);
        p.sendSystemMessage(Component.literal("Recovered " + data.name + " (no XP cost).").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int respawnPet(CommandSourceStack source, String pet, boolean toMe) {
        ServerPlayer p = source.getPlayer();
        TameData data = findOwnedDeadTame(p.getUUID(), pet);
        if (data == null) return error(p, "No dead tame found with that name.");
        return respawnDeadBatch(source, p, List.of(data), toMe, "Respawned");
    }

    private static int respawnGroup(CommandSourceStack source, String group, boolean toMe) {
        ServerPlayer p = source.getPlayer();
        List<TameData> dead = ownedDeadGroup(p.getUUID(), group);
        if (dead.isEmpty()) return error(p, "No dead tames in group '" + group + "'.");
        return respawnDeadBatch(source, p, dead, toMe, "Respawned group '" + group + "'");
    }

    private static int respawnType(CommandSourceStack source, String typeFilter, boolean toMe) {
        ServerPlayer p = source.getPlayer();
        List<TameData> dead = ownedDeadType(p.getUUID(), typeFilter);
        if (dead.isEmpty()) return error(p, "No dead tames of type '" + typeFilter + "'.");
        return respawnDeadBatch(source, p, dead, toMe, "Respawned type '" + typeFilter + "'");
    }

    private static int respawnAll(CommandSourceStack source, boolean toMe) {
        ServerPlayer p = source.getPlayer();
        List<TameData> dead = ownedDeadTames(p.getUUID());
        if (dead.isEmpty()) return error(p, "You have no dead tames to respawn.");
        return respawnDeadBatch(source, p, dead, toMe, "Respawned all dead tames");
    }

    private static int respawnDeadBatch(CommandSourceStack source, ServerPlayer player, List<TameData> candidates, boolean toMe, String label) {
        if (source == null || player == null || candidates == null || candidates.isEmpty()) return 0;
        int success = 0;
        int failed = 0;
        int spent = 0;
        List<String> failReasons = new ArrayList<>();

        for (TameData data : candidates) {
            if (data == null || data.uuid == null) {
                failed++;
                failReasons.add("unknown (invalid registry entry)");
                continue;
            }
            if (!isDeadEntry(data.uuid)) {
                continue;
            }
            if (findLoadedOwnedTameByUuid(source, player.getUUID(), data.uuid) != null) {
                failed++;
                failReasons.add(data.name + " (already loaded)");
                continue;
            }
            int baseCost = Math.max(0, LevelSystem.estimateInvestedXp(data));
            int cost = toMe ? baseCost * 2 : baseCost;
            if (cost > 0 && currentXpPoints(player) < cost) {
                failed++;
                failReasons.add(data.name + " (needs " + cost + " XP)");
                continue;
            }
            SpawnTarget target = resolveRespawnTarget(source, player, data, toMe);
            if (target.level == null) {
                failed++;
                failReasons.add(data.name + " (invalid target dimension)");
                continue;
            }
            RespawnResult result = respawnDeadTameAt(source, data, target.level, target.pos, target.yRot, target.xRot);
            if (!result.success) {
                failed++;
                failReasons.add(data.name + " (" + result.error + ")");
                continue;
            }
            if (cost > 0) {
                player.giveExperiencePoints(-cost);
            }
            spent += cost;
            success++;
        }

        if (success <= 0) {
            return error(player, "No dead tames respawned. " + (failReasons.isEmpty() ? "" : "Reasons: " + String.join("; ", failReasons)));
        }
        player.sendSystemMessage(Component.literal(label + ": " + success + " tame(s), spent " + spent + " XP points" + (toMe ? " (toMe x2)" : "") + ".").withStyle(ChatFormatting.GREEN));
        if (failed > 0 && !failReasons.isEmpty()) {
            player.sendSystemMessage(Component.literal("Respawn failed for " + failed + ": " + String.join("; ", failReasons)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private record SpawnTarget(ServerLevel level, Vec3 pos, float yRot, float xRot) {}

    private static SpawnTarget resolveRespawnTarget(CommandSourceStack source, ServerPlayer player, TameData data, boolean toMe) {
        if (toMe || data == null) {
            return new SpawnTarget(source.getLevel(), source.getPosition(), source.getRotation().y, source.getRotation().x);
        }
        if (data.hasPetBed && data.petBedDimension != null && !data.petBedDimension.isBlank()) {
            ResourceLocation bedId = ResourceLocation.tryParse(data.petBedDimension);
            if (bedId != null) {
                ServerLevel bedLevel = source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, bedId));
                if (bedLevel != null) {
                    return new SpawnTarget(bedLevel, new Vec3(data.petBedX + 0.5D, data.petBedY, data.petBedZ + 0.5D), 0.0F, 0.0F);
                }
            }
        }
        SpawnTarget queuedBedTarget = resolveBedTargetFromDiQueue(source, data);
        if (queuedBedTarget != null) {
            return queuedBedTarget;
        }
        SpawnTarget ownerBedTarget = resolveOwnerBedTarget(source, player);
        if (ownerBedTarget != null) {
            return ownerBedTarget;
        }
        // No tame bed and no owner bed -> force respawn at player position.
        return new SpawnTarget(source.getLevel(), source.getPosition(), source.getRotation().y, source.getRotation().x);
    }

    private static SpawnTarget resolveOwnerBedTarget(CommandSourceStack source, ServerPlayer player) {
        if (source == null || source.getServer() == null || player == null) {
            return null;
        }
        return resolveOwnerBedTarget(source.getServer(), player.getUUID());
    }

    private static SpawnTarget resolveOwnerBedTarget(MinecraftServer server, UUID ownerUuid) {
        if (server == null || ownerUuid == null) {
            return null;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(ownerUuid);
        if (player == null) {
            return null;
        }
        BlockPos respawnPos = player.getRespawnPosition();
        if (respawnPos == null) {
            return null;
        }
        ServerLevel respawnLevel = server.getLevel(player.getRespawnDimension());
        if (respawnLevel == null) {
            return null;
        }
        return new SpawnTarget(
                respawnLevel,
                new Vec3(respawnPos.getX() + 0.5D, respawnPos.getY(), respawnPos.getZ() + 0.5D),
                player.getRespawnAngle(),
                0.0F
        );
    }

    private static SpawnTarget resolveBedTargetFromDiQueue(CommandSourceStack source, TameData data) {
        if (source == null || source.getServer() == null || data == null || data.uuid == null) {
            return null;
        }
        DIWorldData worldData = DIWorldData.get(source.getLevel());
        if (worldData == null) {
            return null;
        }
        for (com.github.alexthe668.domesticationinnovation.server.misc.RespawnRequest request : worldData.getRespawnRequestsSnapshot()) {
            if (request == null || !matchesRespawnRequestIdentity(request, data)) {
                continue;
            }
            ResourceLocation dimId = ResourceLocation.tryParse(request.getDimension());
            if (dimId == null) {
                continue;
            }
            ServerLevel bedLevel = source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
            if (bedLevel == null) {
                continue;
            }
            BlockPos bedPos = request.getBedPosition();
            return new SpawnTarget(bedLevel, new Vec3(bedPos.getX() + 0.5D, bedPos.getY(), bedPos.getZ() + 0.5D), 0.0F, 0.0F);
        }
        return null;
    }

    private record RespawnResult(boolean success, String error) {
        static RespawnResult ok() {
            return new RespawnResult(true, "");
        }
        static RespawnResult fail(String error) {
            return new RespawnResult(false, error == null ? "unknown error" : error);
        }
    }

    private static RespawnResult respawnDeadTameAt(CommandSourceStack source, TameData data, ServerLevel level, Vec3 pos, float yRot, float xRot) {
        if (data == null || level == null || pos == null) return RespawnResult.fail("invalid context");

        String typeId = recoverEntityTypeId(data);
        if (typeId.isBlank()) return RespawnResult.fail("missing saved entity type");

        ResourceLocation id = ResourceLocation.tryParse(typeId);
        if (id == null) return RespawnResult.fail("invalid entity type '" + typeId + "'");

        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (entityType == null) return RespawnResult.fail("unknown entity type '" + typeId + "'");

        Entity spawned = entityType.create(level);
        if (!(spawned instanceof TamableAnimal respawned)) {
            return RespawnResult.fail("stored type is not tamable");
        }

        CompoundTag snapshot = data.entitySnapshot == null ? new CompoundTag() : data.entitySnapshot.copy();
        if (!snapshot.isEmpty()) {
            respawned.load(snapshot);
        }

        respawned.setUUID(data.uuid);
        respawned.moveTo(pos.x, pos.y, pos.z, yRot, xRot);
        respawned.setDeltaMovement(0.0D, 0.0D, 0.0D);
        enforceTamedOwnerPreserveCollar(respawned, data.ownerUUID);

        if (!level.addFreshEntity(respawned)) {
            return RespawnResult.fail("spawn failed (UUID conflict or invalid state)");
        }

        boolean normalized = applyTypeBasePlusBonus(respawned, data);
        if (!normalized) {
            LevelSystem.updateTameName(respawned, data);
            respawned.setHealth(respawned.getMaxHealth());
        }
        finalizeRespawnState(respawned, data);
        clearMatchingDiBedRespawnRequests(source, data);
        TameRegistry.markDirty();
        return RespawnResult.ok();
    }

    public static boolean respawnDeadTameForDuel(MinecraftServer server, UUID tameUuid) {
        if (server == null || tameUuid == null) {
            return false;
        }
        TameData data = TameRegistry.get(tameUuid);
        if (data == null || !data.dead) {
            return false;
        }
        SpawnTarget target = resolveDuelRespawnTarget(server, data);
        if (target == null || target.level == null || target.pos == null) {
            return false;
        }
        RespawnResult result = respawnDeadTameAtServer(data, target.level, target.pos, target.yRot, target.xRot);
        if (!result.success) {
            return false;
        }
        clearMatchingDiBedRespawnRequests(server, data);
        TameRegistry.markDirty();
        return true;
    }

    public static boolean resetDuelCombatState(MinecraftServer server, UUID tameUuid) {
        if (server == null || tameUuid == null) {
            return false;
        }
        TameData data = TameRegistry.get(tameUuid);
        if (data == null) {
            return false;
        }
        boolean changed = false;
        if (data.dead) {
            changed = respawnDeadTameForDuel(server, tameUuid);
            data = TameRegistry.get(tameUuid);
            if (data == null) {
                return changed;
            }
        }
        data.cooldowns.clear();
        changed = true;

        TamableAnimal loaded = findLoadedTameByUuid(server, tameUuid);
        if (loaded != null && loaded.isAlive()) {
            loaded.removeAllEffects();
            loaded.setSecondsOnFire(0);
            loaded.setHealth(loaded.getMaxHealth());
            loaded.setTarget(null);
            loaded.getNavigation().stop();
            loaded.setLastHurtByMob(null);
            loaded.setLastHurtMob(null);
            CompoundTag refreshed = new CompoundTag();
            loaded.save(refreshed);
            data.entitySnapshot = refreshed;
        }
        TameRegistry.markDirty();
        return changed;
    }

    private static SpawnTarget resolveDuelRespawnTarget(MinecraftServer server, TameData data) {
        if (server == null || data == null) {
            return null;
        }
        if (data.hasPetBed && data.petBedDimension != null && !data.petBedDimension.isBlank()) {
            ResourceLocation bedId = ResourceLocation.tryParse(data.petBedDimension);
            if (bedId != null) {
                ServerLevel bedLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, bedId));
                if (bedLevel != null) {
                    return new SpawnTarget(bedLevel, new Vec3(data.petBedX + 0.5D, data.petBedY, data.petBedZ + 0.5D), 0.0F, 0.0F);
                }
            }
        }
        if (data.ownerUUID != null) {
            ServerPlayer owner = server.getPlayerList().getPlayer(data.ownerUUID);
            if (owner != null) {
                return new SpawnTarget(owner.serverLevel(), owner.position(), owner.getYRot(), owner.getXRot());
            }
        }
        if (data.lastKnownDimension != null && !data.lastKnownDimension.isBlank()) {
            ResourceLocation lastDim = ResourceLocation.tryParse(data.lastKnownDimension);
            if (lastDim != null) {
                ServerLevel lastLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, lastDim));
                if (lastLevel != null) {
                    return new SpawnTarget(lastLevel, new Vec3(data.lastKnownX + 0.5D, data.lastKnownY, data.lastKnownZ + 0.5D), 0.0F, 0.0F);
                }
            }
        }
        ServerLevel overworld = server.overworld();
        Vec3 spawnPos = Vec3.atCenterOf(overworld.getSharedSpawnPos());
        return new SpawnTarget(overworld, spawnPos, 0.0F, 0.0F);
    }

    private static RespawnResult respawnDeadTameAtServer(TameData data, ServerLevel level, Vec3 pos, float yRot, float xRot) {
        if (data == null || level == null || pos == null) return RespawnResult.fail("invalid context");

        String typeId = recoverEntityTypeId(data);
        if (typeId.isBlank()) return RespawnResult.fail("missing saved entity type");

        ResourceLocation id = ResourceLocation.tryParse(typeId);
        if (id == null) return RespawnResult.fail("invalid entity type '" + typeId + "'");

        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (entityType == null) return RespawnResult.fail("unknown entity type '" + typeId + "'");

        Entity spawned = entityType.create(level);
        if (!(spawned instanceof TamableAnimal respawned)) {
            return RespawnResult.fail("stored type is not tamable");
        }

        CompoundTag snapshot = data.entitySnapshot == null ? new CompoundTag() : data.entitySnapshot.copy();
        if (!snapshot.isEmpty()) {
            respawned.load(snapshot);
        }

        respawned.setUUID(data.uuid);
        respawned.moveTo(pos.x, pos.y, pos.z, yRot, xRot);
        respawned.setDeltaMovement(0.0D, 0.0D, 0.0D);
        enforceTamedOwnerPreserveCollar(respawned, data.ownerUUID);

        if (!level.addFreshEntity(respawned)) {
            return RespawnResult.fail("spawn failed (UUID conflict or invalid state)");
        }

        boolean normalized = applyTypeBasePlusBonus(respawned, data);
        if (!normalized) {
            LevelSystem.updateTameName(respawned, data);
            respawned.setHealth(respawned.getMaxHealth());
        }
        finalizeRespawnState(respawned, data);
        return RespawnResult.ok();
    }

    public static boolean respawnDeadTameAtBed(ServerLevel level, BlockPos bedPos, Direction facing, TameData data) {
        if (level == null || bedPos == null || data == null || data.uuid == null || !data.dead) {
            return false;
        }
        Vec3 spawnPos = Vec3.upFromBottomCenterOf(bedPos, 0.8F);
        float yRot = yawFromDirection(facing);
        RespawnResult result = respawnDeadTameAtServer(data, level, spawnPos, yRot, 0.0F);
        if (!result.success) {
            return false;
        }
        TamableAnimal loaded = findLoadedTameByUuid(level.getServer(), data.uuid);
        if (loaded != null) {
            if (loaded instanceof IComandableMob commandableMob) {
                commandableMob.setCommand(1);
            }
            loaded.setOrderedToSit(true);
        }
        return true;
    }

    private static float yawFromDirection(Direction direction) {
        if (direction == null) {
            return 0.0F;
        }
        return switch (direction) {
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    public static void handleExternalRespawn(TamableAnimal tame) {
        if (tame == null) {
            return;
        }
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) {
            return;
        }
        finalizeRespawnState(tame, data);
        TameRegistry.markDirty();
    }

    private static void finalizeRespawnState(TamableAnimal tame, TameData data) {
        if (tame == null || data == null) {
            return;
        }
        TameRegistry.bindEntityToData(tame, data);
        applyLatestDeathSnapshotIfAvailable(tame, data);
        tame.hurtTime = 0;
        tame.deathTime = 0;
        tame.invulnerableTime = 0;
        tame.setRemainingFireTicks(0);
        tame.fallDistance = 0.0F;
        tame.setDeltaMovement(0.0D, 0.0D, 0.0D);
        tame.setTarget(null);
        tame.getNavigation().stop();
        tame.setNoAi(false);
        data.dead = false;
        data.deadGameTime = 0L;
        data.deadUnixMillis = 0L;
        data.deathDimension = "";
        data.deathX = 0;
        data.deathY = 0;
        data.deathZ = 0;
        data.lastKnownDimension = tame.level().dimension().location().toString();
        data.lastKnownX = tame.blockPosition().getX();
        data.lastKnownY = tame.blockPosition().getY();
        data.lastKnownZ = tame.blockPosition().getZ();
        data.lastKnownGameTime = tame.level().getGameTime();
        CompoundTag refreshedSnapshot = new CompoundTag();
        TameRegistry.bindEntityToData(tame, data);
        tame.save(refreshedSnapshot);
        data.entitySnapshot = refreshedSnapshot;
    }

    private static void clearMatchingDiBedRespawnRequests(CommandSourceStack source, TameData data) {
        if (source == null || source.getServer() == null || data == null) return;
        DIWorldData worldData = DIWorldData.get(source.getLevel());
        if (worldData == null) return;
        if (data.uuid != null && worldData.removeRespawnRequestsForPet(data.uuid) > 0) return;
        if (data.tlId != null && worldData.removeRespawnRequestsForPet(data.tlId) > 0) return;

        if (!data.hasPetBed || data.petBedDimension == null || data.petBedDimension.isBlank()) return;
        ResourceLocation bedDim = ResourceLocation.tryParse(data.petBedDimension);
        if (bedDim == null) return;
        ServerLevel bedLevel = source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, bedDim));
        if (bedLevel == null) return;
        BlockPos bedPos = new BlockPos(data.petBedX, data.petBedY, data.petBedZ);
        String wantedType = recoverEntityTypeId(data);
        String wantedName = stripLevelPrefixes(data.name == null ? "" : data.name);
        List<com.github.alexthe668.domesticationinnovation.server.misc.RespawnRequest> requests =
                new ArrayList<>(worldData.getRespawnRequestsFor(bedLevel, bedPos));
        for (com.github.alexthe668.domesticationinnovation.server.misc.RespawnRequest request : requests) {
            if (request == null) continue;
            if (wantedType != null && !wantedType.isBlank()) {
                String reqType = request.getEntityTypeLoc();
                if (reqType == null || !reqType.equalsIgnoreCase(wantedType)) continue;
            }
            String reqName = stripLevelPrefixes(request.getNametag() == null ? "" : request.getNametag());
            if (!wantedName.isBlank() && !reqName.isBlank() && !wantedName.equalsIgnoreCase(reqName)) continue;
            worldData.removeRespawnRequest(request);
        }
    }

    private static void clearMatchingDiBedRespawnRequests(MinecraftServer server, TameData data) {
        if (server == null || data == null || (data.uuid == null && data.tlId == null)) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            DIWorldData worldData = DIWorldData.get(level);
            if (worldData == null) {
                continue;
            }
            if (data.uuid != null) {
                worldData.removeRespawnRequestsForPet(data.uuid);
            }
            if (data.tlId != null) {
                worldData.removeRespawnRequestsForPet(data.tlId);
            }
        }
    }

    private static boolean matchesRespawnRequestIdentity(com.github.alexthe668.domesticationinnovation.server.misc.RespawnRequest request, TameData data) {
        if (request == null || data == null) {
            return false;
        }
        CompoundTag entityData = request.getEntityData();
        if (entityData == null) {
            return false;
        }
        if (data.tlId != null && entityData.hasUUID("TLID") && data.tlId.equals(entityData.getUUID("TLID"))) {
            return true;
        }
        UUID uuid = data.uuid;
        if (uuid == null) {
            return false;
        }
        if (entityData.hasUUID("TLRegistryUUID")) {
            return uuid.equals(entityData.getUUID("TLRegistryUUID"));
        }
        return entityData.contains("UUID", Tag.TAG_INT_ARRAY)
                && entityData.hasUUID("UUID")
                && uuid.equals(entityData.getUUID("UUID"));
    }

    private static RecoverResult recoverPetEntity(CommandSourceStack source, ServerPlayer p, TameData data) {
        if (p == null || data == null) return RecoverResult.fail("invalid context");
        if (data.uuid == null) return RecoverResult.fail("missing tame UUID");
        if (isDeadEntry(data.uuid)) return RecoverResult.fail("tame is marked dead");
        String logicalKey = logicalTameKey(data);
        if (!logicalKey.isBlank() && hasLoadedLogicalDuplicate(p.getServer(), data, logicalKey)) {
            return RecoverResult.fail("duplicate already loaded");
        }
        String typeId = recoverEntityTypeId(data);
        if (typeId.isBlank()) {
            return RecoverResult.fail("missing saved entity type");
        }

        ResourceLocation id = ResourceLocation.tryParse(typeId);
        if (id == null) {
            return RecoverResult.fail("invalid entity type '" + typeId + "'");
        }
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (entityType == null) {
            return RecoverResult.fail("unknown entity type '" + typeId + "'");
        }

        ServerLevel level = p.serverLevel();
        Entity spawned = entityType.create(level);
        if (!(spawned instanceof TamableAnimal recovered)) {
            return RecoverResult.fail("stored type is not tamable");
        }

        CompoundTag snapshot = data.entitySnapshot == null ? new CompoundTag() : data.entitySnapshot.copy();
        if (!snapshot.isEmpty()) {
            recovered.load(snapshot);
        }

        recovered.setUUID(data.uuid);
        recovered.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
        recovered.setDeltaMovement(0.0D, 0.0D, 0.0D);
        UUID ownerId = p.getUUID();
        enforceTamedOwnerPreserveCollar(recovered, ownerId);

        if (!level.addFreshEntity(recovered)) {
            return RecoverResult.fail("spawn failed (UUID conflict or invalid state)");
        }

        LevelSystem.updateTameName(recovered, data);
        recovered.setHealth(recovered.getMaxHealth());

        data.ownerUUID = p.getUUID();
        data.lastKnownDimension = level.dimension().location().toString();
        data.lastKnownX = recovered.blockPosition().getX();
        data.lastKnownY = recovered.blockPosition().getY();
        data.lastKnownZ = recovered.blockPosition().getZ();
        data.lastKnownGameTime = level.getGameTime();
        CompoundTag refreshedSnapshot = new CompoundTag();
        recovered.save(refreshedSnapshot);
        data.entitySnapshot = refreshedSnapshot;

        TameRegistry.markDirty();
        return RecoverResult.ok(recovered);
    }

    private static int teleportAll(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        List<TameData> requested = ownedTames(p.getUUID());
        List<TamableAnimal> targets = new ArrayList<>();
        int queued = 0;
        int queueFailed = 0;
        List<String> failedQueueNames = new ArrayList<>();
        int cost = 0;
        int crossDimension = 0;
        for (TameData d : requested) {
            TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (ta == null) {
                UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, p, d);
                if (unloaded.success) {
                    queued++;
                } else {
                    queueFailed++;
                    failedQueueNames.add((d.name == null ? "unknown" : d.name) + " (" + unloaded.error + ")");
                }
                continue;
            }
            boolean cross = isCrossDimension(ta, p);
            targets.add(ta);
            cost += teleportCostFor(d, cross);
            if (cross) crossDimension++;
        }
        if (!payTeleportXp(p, cost)) return 0;
        for (TamableAnimal ta : targets) teleportTameToPlayer(ta, p);
        sendTeleportSummary(p, "TP all", targets.size(), queued, ownedDeadTames(p.getUUID()).size(), queueFailed, cost, crossDimension);
        if (!failedQueueNames.isEmpty()) {
            p.sendSystemMessage(Component.literal("Unloaded tp failures: " + String.join("; ", failedQueueNames)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int teleportAllFromDimension(CommandSourceStack source, ServerLevel fromDimension) {
        ServerPlayer p = source.getPlayer();
        if (fromDimension == null) return error(p, "Invalid dimension.");

        List<TameData> requested = ownedTames(p.getUUID());
        List<TamableAnimal> targets = new ArrayList<>();
        int queued = 0;
        int queueFailed = 0;
        List<String> failedQueueNames = new ArrayList<>();
        int cost = 0;
        int crossDimension = 0;
        ResourceLocation dimensionId = fromDimension.dimension().location();

        for (TameData d : requested) {
            TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (ta == null) {
                if (!matchesDimensionFilter(d, null, dimensionId)) {
                    continue;
                }
                UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, p, d);
                if (unloaded.success) {
                    queued++;
                } else {
                    queueFailed++;
                    failedQueueNames.add((d.name == null ? "unknown" : d.name) + " (" + unloaded.error + ")");
                }
                continue;
            }
            if (!matchesDimensionFilter(d, ta, dimensionId)) {
                continue;
            }
            boolean cross = isCrossDimension(ta, p);
            targets.add(ta);
            cost += teleportCostFor(d, cross);
            if (cross) crossDimension++;
        }

        if (!payTeleportXp(p, cost)) return 0;
        for (TamableAnimal ta : targets) teleportTameToPlayer(ta, p);
        int deadSkipped = 0;
        for (TameData d : ownedDeadTames(p.getUUID())) {
            if (matchesDimensionFilter(d, null, dimensionId)) {
                deadSkipped++;
            }
        }
        sendTeleportSummary(p, "TP dim " + dimensionId, targets.size(), queued, deadSkipped, queueFailed, cost, crossDimension);
        if (!failedQueueNames.isEmpty()) {
            p.sendSystemMessage(Component.literal("Unloaded tp failures: " + String.join("; ", failedQueueNames)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int teleportUnloaded(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        List<TameData> requested = ownedTames(p.getUUID());
        int queued = 0;
        int failed = 0;
        List<String> failedQueueNames = new ArrayList<>();

        for (TameData d : requested) {
            if (isEffectivelyLoaded(source, p, d)) {
                continue;
            }
            UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, p, d);
            if (unloaded.success) {
                queued++;
            } else {
                failed++;
                failedQueueNames.add((d.name == null ? "unknown" : d.name) + " (" + unloaded.error + ")");
            }
        }

        sendTeleportSummary(p, "TP unloaded", 0, queued, ownedDeadTames(p.getUUID()).size(), failed, 0, 0);
        if (!failedQueueNames.isEmpty()) {
            p.sendSystemMessage(Component.literal("Unloaded tp failures: " + String.join("; ", failedQueueNames)).withStyle(ChatFormatting.RED));
        }
        return queued > 0 ? 1 : 0;
    }

    private static int adminTpAllOwners(CommandSourceStack source, boolean unloadedOnly) {
        ServerPlayer admin = source.getPlayer();
        if (admin == null || source.getServer() == null) {
            return 0;
        }
        if (!admin.serverLevel().dimension().equals(Level.OVERWORLD)) {
            return error(admin, "This command is only allowed in the Overworld.");
        }

        List<TamableAnimal> loadedTargets = new ArrayList<>();
        int queued = 0;
        int queueFailed = 0;
        List<String> failedQueueNames = new ArrayList<>();
        String overworldId = Level.OVERWORLD.location().toString();

        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.uuid == null) continue;
            if (isDeadEntry(data.uuid)) continue;

            TamableAnimal loaded = findLoadedTameByUuid(source, data.uuid);
            if (loaded != null) {
                if (!loaded.level().dimension().equals(Level.OVERWORLD)) continue;
                if (!unloadedOnly) {
                    loadedTargets.add(loaded);
                }
                continue;
            }

            if (data.lastKnownDimension == null || !overworldId.equals(data.lastKnownDimension)) {
                continue;
            }
            String queueError = tryQueueUnloadedTeleportToPlayer(source, admin, data);
            if (queueError == null) {
                queued++;
            } else {
                queueFailed++;
                failedQueueNames.add((data.name == null ? "unknown" : data.name) + " (" + queueError + ")");
            }
        }

        int movedLoaded = 0;
        if (!unloadedOnly) {
            for (TamableAnimal tame : loadedTargets) {
                if (tame == null || !tame.isAlive()) continue;
                teleportTameToPlayer(tame, admin);
                movedLoaded++;
            }
        }

        String label = unloadedOnly ? "Admin TP unloaded" : "Admin TP allOwners";
        admin.sendSystemMessage(buildTeleportSummary(label, movedLoaded, queued, 0, queueFailed, 0, 0));
        if (!failedQueueNames.isEmpty()) {
            admin.sendSystemMessage(Component.literal("Queue failures: " + String.join("; ", failedQueueNames)).withStyle(ChatFormatting.RED));
        }
        return (movedLoaded + queued) > 0 ? 1 : 0;
    }

    private static UnloadedTpResult tpUnloadedViaLanternOrRecover(CommandSourceStack source, ServerPlayer owner, TameData data) {
        String queueError = tryQueueUnloadedTeleportToPlayer(source, owner, data);
        if (queueError == null) {
            return UnloadedTpResult.queued();
        }
        if (isUnloadedRespawnFallbackBlockedType(data)) {
            return UnloadedTpResult.fail(queueError + "; respawn fallback disabled for " + recoverEntityTypeId(data));
        }
        if (isSameDimensionUnloadedRespawnFallback(owner, data)) {
            RecoverResult recoverResult = recoverPetEntity(source, owner, data);
            if (recoverResult.entity != null) {
                return UnloadedTpResult.queued();
            }
            if (recoverResult.error != null && !recoverResult.error.isBlank()) {
                return UnloadedTpResult.fail(queueError + "; respawn failed: " + recoverResult.error);
            }
        }
        return UnloadedTpResult.fail(queueError);
    }

    private static boolean isUnloadedRespawnFallbackBlockedType(TameData data) {
        String typeId = recoverEntityTypeId(data);
        return "alexsmobs:flutter".equals(typeId);
    }

    private static boolean isSameDimensionUnloadedRespawnFallback(ServerPlayer owner, TameData data) {
        if (owner == null || data == null || data.dead || data.lastKnownDimension == null || data.lastKnownDimension.isBlank()) {
            return false;
        }
        ResourceLocation lastKnown = ResourceLocation.tryParse(data.lastKnownDimension);
        if (lastKnown == null || !owner.serverLevel().dimension().location().equals(lastKnown)) {
            return false;
        }
        return owner.getServer() == null || findLoadedTameByIdentity(owner.getServer(), data.uuid, data.tlId) == null;
    }

    private static String tryQueueUnloadedTeleportToPlayer(CommandSourceStack source, ServerPlayer owner, TameData data) {
        if (source == null || owner == null || data == null || data.uuid == null) {
            return "invalid context";
        }
        if (source.getServer() == null) {
            return "server unavailable";
        }
        if (isLoadedAnywhere(source.getServer(), data.uuid)) {
            return "already loaded";
        }
        if (data.lastKnownDimension == null || data.lastKnownDimension.isBlank()) {
            return "missing last known dimension";
        }
        ResourceLocation lastKnown = ResourceLocation.tryParse(data.lastKnownDimension);
        if (lastKnown == null) {
            return "invalid last known dimension";
        }
        if (!source.getLevel().dimension().location().equals(lastKnown)) {
            return "cross-dimension unloaded tp disabled";
        }
        ResourceKey<Level> sourceDimension = ResourceKey.create(Registries.DIMENSION, lastKnown);
        ResourceKey<Level> targetDimension = owner.serverLevel().dimension();
        if (!sourceDimension.equals(targetDimension)) {
            return "cross-dimension unloaded tp disabled";
        }
        ServerLevel sourceLevel = source.getServer().getLevel(sourceDimension);
        if (sourceLevel == null) {
            return "source level unavailable";
        }
        DIWorldData worldData = DIWorldData.get(source.getLevel());
        if (worldData == null) {
            return "world data unavailable";
        }
        String typeId = recoverEntityTypeId(data);
        if (typeId == null || typeId.isBlank()) {
            return "missing entity type";
        }
        worldData.removeMatchingLanternRequests(data.uuid);
        if (data.tlId != null) {
            worldData.removeMatchingLanternRequests(data.tlId);
        }
        LanternRequest request = new LanternRequest(
                data.uuid,
                data.tlId,
                typeId,
                owner.getUUID(),
                new BlockPos(data.lastKnownX, data.lastKnownY, data.lastKnownZ),
                source.getLevel().getGameTime(),
                data.name == null ? "unknown" : data.name,
                LanternRequest.MODE_PLAYER_TP,
                targetDimension.location().toString(),
                owner.getX(),
                owner.getY(),
                owner.getZ(),
                owner.getYRot(),
                owner.getXRot()
        );
        worldData.addLanternRequest(request);
        loadChunksAround(sourceLevel, data.uuid, request.getChunkPosition(), true);
        return null;
    }

    private static boolean matchesDimensionFilter(TameData data, TamableAnimal loaded, ResourceLocation targetDimensionId) {
        if (targetDimensionId == null) return false;
        if (loaded != null) {
            return loaded.level().dimension().location().equals(targetDimensionId);
        }
        if (data == null || data.lastKnownDimension == null || data.lastKnownDimension.isBlank()) {
            return false;
        }
        ResourceLocation stored = ResourceLocation.tryParse(data.lastKnownDimension);
        return stored != null && stored.equals(targetDimensionId);
    }

    private static boolean isEffectivelyLoaded(CommandSourceStack source, ServerPlayer owner, TameData data) {
        if (source == null || owner == null || data == null) return false;
        if (findLoadedOwnedTameByUuid(source, owner.getUUID(), data.uuid) != null) {
            return true;
        }
        return hasPortableTypeInInventory(owner, data);
    }

    private static boolean hasPortableTypeInInventory(ServerPlayer player, TameData data) {
        if (player == null || data == null) return false;
        String typeId = tameTypeId(data);
        if (typeId.isBlank()) return false;
        ResourceLocation target = ResourceLocation.tryParse(typeId);
        if (target == null) return false;
        String targetPath = target.getPath();

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || stack.isEmpty()) continue;
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId == null) continue;
            if (itemId.equals(target)) return true;
            if (!targetPath.isBlank() && itemId.getPath().equalsIgnoreCase(targetPath)) return true;
        }
        return false;
    }

    private static boolean isCarriedByOwner(MinecraftServer server, TameData data) {
        if (server == null || data == null || data.ownerUUID == null) return false;
        ServerPlayer owner = server.getPlayerList().getPlayer(data.ownerUUID);
        if (owner == null) return false;
        return hasPortableTypeInInventory(owner, data);
    }

    private static boolean isLoadedAnywhere(MinecraftServer server, UUID tameUuid) {
        if (server == null || tameUuid == null) return false;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(tameUuid);
            if (entity instanceof TamableAnimal tame && tame.isTame() && tame.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private static String logicalTameKey(TameData data) {
        if (data == null || data.ownerUUID == null) return "";
        String name = data.name == null ? "" : data.name.trim().toLowerCase(Locale.ROOT);
        if (name.isBlank()) {
            name = String.valueOf(data.uuid);
        }
        String type = tameTypeId(data);
        if (type.isBlank()) {
            type = "unknown";
        }
        return data.ownerUUID + "|" + name + "|" + type;
    }

    private static boolean hasLoadedLogicalDuplicate(MinecraftServer server, TameData data, String logicalKey) {
        if (server == null || data == null || logicalKey == null || logicalKey.isBlank()) return false;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal tame) || !tame.isTame() || !tame.isAlive()) continue;
                TameData loadedData = TameRegistry.get(tame.getUUID());
                if (loadedData == null || loadedData.uuid == null) continue;
                if (loadedData.uuid.equals(data.uuid)) continue;
                if (!logicalKey.equals(logicalTameKey(loadedData))) continue;
                return true;
            }
        }
        return false;
    }
    //tleeports
    //tp
    private static int teleportByMovementState(CommandSourceStack source, MovementOrder order) {
        ServerPlayer p = source.getPlayer();
        List<TameData> requested = ownedTames(p.getUUID());
        List<TamableAnimal> targets = new ArrayList<>();
        int cost = 0;
        int skipped = 0;
        int crossDimension = 0;
        for (TameData d : requested) {
            TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (ta == null) {
                skipped++;
                continue;
            }
            if (!matchesMovementOrder(ta, order)) continue;
            boolean cross = isCrossDimension(ta, p);
            targets.add(ta);
            cost += teleportCostFor(d, cross);
            if (cross) crossDimension++;
        }
        if (!payTeleportXp(p, cost)) return 0;
        for (TamableAnimal ta : targets) teleportTameToPlayer(ta, p);
        sendTeleportSummary(p, "TP " + movementLabel(order), targets.size(), 0, 0, skipped, cost, crossDimension);
        return 1;
    }

    private static int teleportByState(CommandSourceStack source, String stateName) {
        ServerPlayer p = source.getPlayer();
        MovementOrder order = parseMovementOrder(stateName);
        if (order == null) return error(p, "Invalid state. Use follow, wander, or sit.");
        return teleportByMovementState(source, order);
    }

    private static boolean payTeleportXp(ServerPlayer player, int cost) {
        cost = Math.max(0, cost);
        if (cost <= 0) return true;
        int currentXp = currentXpPoints(player);
        if (currentXp < cost) {
            player.sendSystemMessage(Component.literal("Not enough XP points. Required: " + cost + ", you have: " + currentXp + "."));
            return false;
        }
        player.giveExperiencePoints(-cost);
        return true;
    }

    private static int teleportCostFor(TameData data, boolean crossDimension) {
        int base = Math.max(1, data == null ? 1 : data.level);
        return crossDimension ? (base * 2) : base;
    }

    private static boolean isCrossDimension(TamableAnimal tame, ServerPlayer player) {
        return tame != null && player != null && !tame.level().dimension().equals(player.level().dimension());
    }

    private static int recoverCostFor(TameData data) {
        if (data == null) return 0;
        int invested = Math.max(0, LevelSystem.estimateInvestedXp(data));
        if (invested <= 0) return 0;
        return Math.max(1, (int) Math.ceil(invested / 4.0D));
    }

    private static String recoverEntityTypeId(TameData data) {
        if (data != null && data.entitySnapshot != null && data.entitySnapshot.contains("id")) {
            String fromSnapshot = data.entitySnapshot.getString("id");
            if (fromSnapshot != null && !fromSnapshot.isBlank()) {
                return fromSnapshot.trim().toLowerCase(Locale.ROOT);
            }
        }

        String raw = data == null ? "" : data.type;
        if (raw == null || raw.isBlank()) return "";

        String normalized = raw.trim();
        if (normalized.startsWith("entity.")) {
            normalized = normalized.substring("entity.".length());
            int firstDot = normalized.indexOf('.');
            if (firstDot > 0 && !normalized.contains(":")) {
                normalized = normalized.substring(0, firstDot) + ":" + normalized.substring(firstDot + 1);
            }
        }
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static void teleportTameToPlayer(TamableAnimal tame, ServerPlayer player) {
        if (tame == null || player == null) return;
        TameData data = TameRegistry.get(tame.getUUID());
        TameTransferService.TransferResult result = TameTransferService.transferToPlayer(tame, player, data);
        if (!result.success()) {
            System.err.println("[TamesLevel] Command teleport failed for tame " + tame.getUUID() + ": " + result.error());
        }
    }

    private static int leaderboard(CommandSourceStack source, String mode, boolean includeAll, String groupFilter, int requestedLimit) {
        ServerPlayer p = source.getPlayer();
        String m = mode == null ? "mix" : mode.trim().toLowerCase(Locale.ROOT);
        List<TameData> entries = new ArrayList<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!includeAll && !p.getUUID().equals(d.ownerUUID)) continue;
            if (groupFilter != null && (d.group == null || d.group.isBlank() || !d.group.equalsIgnoreCase(groupFilter))) continue;
            entries.add(d);
        }
        if (m.equals("mix") || m.equals("score")) entries.sort((a, b) -> Integer.compare(weightedCombatScore(b), weightedCombatScore(a)));
        else if (m.equals("kills")) entries.sort((a, b) -> Integer.compare(b.kills, a.kills));
        else if (m.equals("deaths")) entries.sort((a, b) -> Integer.compare(b.deaths, a.deaths));
        else if (m.equals("assists")) entries.sort((a, b) -> Integer.compare(b.assists, a.assists));
        else if (m.equals("days")) entries.sort((a, b) -> Long.compare(daysAlive(source, b), daysAlive(source, a)));
        else entries.sort((a, b) -> Integer.compare(b.level, a.level));

        if (entries.isEmpty()) return error(p, "No entries found.");
        int limit = Math.min(Math.max(1, requestedLimit), entries.size());
        String scope = includeAll ? "all players" : "your tames";
        String groupText = (groupFilter == null || groupFilter.isBlank()) ? "" : (" | group: " + groupFilter);
        p.sendSystemMessage(Component.literal("---- Leaderboard (" + m + ") | " + scope + groupText + " | showing " + limit + "/" + entries.size() + " ----").withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < limit; i++) {
            TameData d = entries.get(i);
            int score = weightedCombatScore(d);
            p.sendSystemMessage(Component.literal((i + 1) + ". ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("(" + ownerInitials(source.getServer(), d.ownerUUID) + ") ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("[" + d.level + "] ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(d.name + " ").withStyle(ChatFormatting.AQUA))
                    .append((m.equals("mix") || m.equals("score")) ? Component.literal("S:" + score + " ").withStyle(ChatFormatting.LIGHT_PURPLE) : Component.empty())
                    .append(Component.literal("K:" + d.kills + " ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("A:" + d.assists + " ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("D:" + d.deaths + " ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Days:" + daysAlive(source, d)).withStyle(ChatFormatting.DARK_AQUA)));
        }
        return 1;
    }

    private static void sendTeleportSummary(ServerPlayer player, String label, int loaded, int unloaded, int deadSkipped, int failed, int xpCost, int crossDimension) {
        if (player == null) {
            return;
        }
        player.sendSystemMessage(buildTeleportSummary(label, loaded, unloaded, deadSkipped, failed, xpCost, crossDimension));
    }

    private static Component buildTeleportSummary(String label, int loaded, int unloaded, int deadSkipped, int failed, int xpCost, int crossDimension) {
        net.minecraft.network.chat.MutableComponent line = Component.literal(label + ": ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("teleported ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("(").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("l:" + loaded).withStyle(ChatFormatting.GREEN))
                .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("u:" + unloaded).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("d:" + deadSkipped).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("f:" + failed).withStyle(failed > 0 ? ChatFormatting.RED : ChatFormatting.GRAY))
                .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
        if (xpCost > 0) {
            line = line.append(Component.literal(" xp:" + xpCost).withStyle(ChatFormatting.AQUA));
        }
        if (crossDimension > 0) {
            line = line.append(Component.literal(" xdim:" + crossDimension).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return line;
    }

    private static String ownerInitials(MinecraftServer server, UUID ownerUuid) {
        if (ownerUuid == null) {
            return "--";
        }
        String name = null;
        if (server != null) {
            ServerPlayer live = server.getPlayerList().getPlayer(ownerUuid);
            if (live != null) {
                name = live.getGameProfile().getName();
            } else if (server.getProfileCache() != null) {
                java.util.Optional<GameProfile> profile = server.getProfileCache().get(ownerUuid);
                if (profile.isPresent()) {
                    name = profile.get().getName();
                }
            }
        }
        if (name == null || name.isBlank()) {
            return "??";
        }
        String trimmed = name.trim();
        return trimmed.substring(0, Math.min(2, trimmed.length()));
    }

    private static int weightedCombatScore(TameData data) {
        if (data == null) return 0;
        return Math.max(0, data.kills) * 4 + Math.max(0, data.assists);
    }

    private static int recentDeaths(CommandSourceStack source, int requestedLimit) {
        ServerPlayer p = source.getPlayer();
        List<DeathHistoryRow> entries = collectDeathHistoryRows(p.getUUID());
        if (entries.isEmpty()) {
            return error(p, "No death history found.");
        }

        int limit = Math.min(Math.max(1, requestedLimit), entries.size());
        p.sendSystemMessage(Component.literal("Last " + limit + " deaths:").withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < limit; i++) {
            DeathHistoryRow r = entries.get(i);
            String when = formatDeathTime(r.unixMillis);
            String where = (r.dimension == null || r.dimension.isBlank())
                    ? "unknown"
                    : (r.dimension + " @ " + r.x + " " + r.y + " " + r.z);
            String how = r.message == null || r.message.isBlank() ? "unknown" : r.message;
            p.sendSystemMessage(Component.literal("[" + r.level + "] ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(r.tameName + " ").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("\"" + how + "\" ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("(" + where + ") ").withStyle(ChatFormatting.DARK_AQUA))
                    .append(Component.literal("{" + when + "}").withStyle(ChatFormatting.DARK_PURPLE)));
        }
        return 1;
    }

    private static List<DeathHistoryRow> collectDeathHistoryRows(UUID ownerUuid) {
        List<DeathHistoryRow> rows = new ArrayList<>();
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.ownerUUID == null || !ownerUuid.equals(data.ownerUUID)) continue;
            String tameName = data.name == null ? "unknown" : data.name;
            for (CompoundTag row : data.deathHistory) {
                if (row == null || row.isEmpty()) continue;
                rows.add(new DeathHistoryRow(
                        tameName,
                        row.getInt("level"),
                        row.getString("message"),
                        row.getString("dimension"),
                        row.getInt("x"),
                        row.getInt("y"),
                        row.getInt("z"),
                        row.contains("gameTime", Tag.TAG_LONG) ? row.getLong("gameTime") : 0L,
                        row.contains("unixMillis", Tag.TAG_LONG) ? row.getLong("unixMillis") : 0L
                ));
            }
        }
        // Legacy fallback for worlds that still have old death history records.
        if (rows.isEmpty()) {
            for (TameDeathRecord record : TameRegistry.DEATH_HISTORY) {
                if (record == null || record.ownerUUID == null || !ownerUuid.equals(record.ownerUUID)) continue;
                rows.add(new DeathHistoryRow(
                        record.name,
                        record.level,
                        "",
                        record.deathDimension,
                        record.deathX,
                        record.deathY,
                        record.deathZ,
                        record.deathGameTime,
                        record.deathUnixMillis
                ));
            }
        }
        rows.sort((a, b) -> {
            long ua = a.unixMillis;
            long ub = b.unixMillis;
            if (ua != ub) return Long.compare(ub, ua);
            return Long.compare(b.gameTime, a.gameTime);
        });
        return rows;
    }

    private static String formatDeathTime(long unixMillis) {
        if (unixMillis <= 0L) return "unknown";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(unixMillis));
    }

    private static int currentXpPoints(ServerPlayer player) {
        int level = Math.max(0, player.experienceLevel);
        int base = xpPointsAtLevelStart(level);
        int inLevel = (int) Math.floor(player.experienceProgress * player.getXpNeededForNextLevel());
        return Math.max(0, base + Math.max(0, inLevel));
    }

    private static int xpPointsAtLevelStart(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5D * level * level - 40.5D * level + 360.0D);
        return (int) (4.5D * level * level - 162.5D * level + 2220.0D);
    }

    private static int setDebugEnemyKilled(CommandSourceStack source, boolean enabled) {
        ServerPlayer p = source.getPlayer();
        PlayerDebugSettings.setEnemyKilled(p.getUUID(), enabled);
        p.sendSystemMessage(Component.literal("Debug enemyKilled set to " + enabled + ".").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int setDebugAbilityUsed(CommandSourceStack source, boolean enabled) {
        ServerPlayer p = source.getPlayer();
        PlayerDebugSettings.setAbilityUsed(p.getUUID(), enabled);
        p.sendSystemMessage(Component.literal("Debug abilityUsed set to " + enabled + ".").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int adminSetDebugAbilityUsed(CommandSourceStack source, boolean enabled) {
        ServerPlayer p = source.getPlayer();
        PlayerDebugSettings.setAbilityUsed(p.getUUID(), enabled);
        p.sendSystemMessage(Component.literal("Admin debug abilityUsed set to " + enabled + ".").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int setDebugAttributeUsed(CommandSourceStack source, boolean enabled) {
        ServerPlayer p = source.getPlayer();
        PlayerDebugSettings.setAttributeUsed(p.getUUID(), enabled);
        p.sendSystemMessage(Component.literal("Debug attributeUsed set to " + enabled + ".").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int setDebugLevelUp(CommandSourceStack source, boolean enabled) {
        ServerPlayer p = source.getPlayer();
        PlayerDebugSettings.setLevelUp(p.getUUID(), enabled);
        p.sendSystemMessage(Component.literal("Debug levelUp set to " + enabled + ".").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int adminSetDebugDamage(CommandSourceStack source, boolean enabled) {
        ServerPlayer p = source.getPlayer();
        PlayerDebugSettings.setDamage(p.getUUID(), enabled);
        p.sendSystemMessage(Component.literal("Admin debug damageDealt set to " + enabled + ".").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int debugStatus(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        boolean enemy = PlayerDebugSettings.enemyKilled(p.getUUID());
        boolean attribute = PlayerDebugSettings.attributeUsed(p.getUUID());
        boolean levelUp = PlayerDebugSettings.levelUp(p.getUUID());
        p.sendSystemMessage(Component.literal("Debug -> enemyKilled: " + enemy + ", attributeUsed: " + attribute + ", levelUp: " + levelUp).withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int adminDebugStatus(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        boolean ability = PlayerDebugSettings.abilityUsed(p.getUUID());
        boolean damage = PlayerDebugSettings.damage(p.getUUID());
        p.sendSystemMessage(Component.literal("Admin Debug -> abilityUsed: " + ability + ", damageDealt: " + damage).withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int emergencyBerserk(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        int count = 0;
        for (TamableAnimal ta : p.level().getEntitiesOfClass(TamableAnimal.class, p.getBoundingBox().inflate(32))) {
            if (!ta.isTame() || !p.getUUID().equals(ta.getOwnerUUID())) continue;
            TameData d = TameRegistry.get(ta.getUUID());
            if (d == null) continue;
            d.mode = TameMode.MONSTER_HUNTER.id();
            ta.setOrderedToSit(false);
            count++;
        }
        if (count > 0) TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Set " + count + " nearby tames to monster_hunter mode."));
        return 1;
    }

    private static int emergencyRun(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        int count = 0;
        for (TamableAnimal ta : p.level().getEntitiesOfClass(TamableAnimal.class, p.getBoundingBox().inflate(32))) {
            if (!ta.isTame() || !p.getUUID().equals(ta.getOwnerUUID())) continue;
            TameData d = TameRegistry.get(ta.getUUID());
            if (d == null) continue;
            d.escapeMode = true;
            d.escapeActive = true;
            ta.setTarget(null);
            ta.getNavigation().moveTo(p, 1.45D);
            count++;
        }
        if (count > 0) TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Marked " + count + " nearby tames as runaway."));
        return 1;
    }

    private static int emergencyPassive(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        int count = 0;
        for (TamableAnimal ta : p.level().getEntitiesOfClass(TamableAnimal.class, p.getBoundingBox().inflate(32))) {
            if (!ta.isTame() || !p.getUUID().equals(ta.getOwnerUUID())) continue;
            TameData d = TameRegistry.get(ta.getUUID());
            if (d == null) continue;
            d.mode = TameMode.PASSIVE.id();
            ta.setTarget(null);
            count++;
        }
        if (count > 0) TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Set " + count + " nearby tames to passive."));
        return 1;
    }

    private static int removeTargetAll(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        int count = 0;
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (ta == null) continue;
            ta.setTarget(null);
            count++;
        }
        p.sendSystemMessage(Component.literal("Removed targets from " + count + " loaded tames."));
        return 1;
    }

    private static int removeTargetPet(CommandSourceStack source, String pet) {
        ServerPlayer p = source.getPlayer();
        TameData d = findOwnedTame(p.getUUID(), pet);
        if (d == null) return error(p, "Pet not found.");
        TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
        if (ta == null) return error(p, "Pet is not loaded.");
        ta.setTarget(null);
        p.sendSystemMessage(Component.literal("Removed target for " + d.name + "."));
        return 1;
    }

    private static int removeTargetGroup(CommandSourceStack source, String group) {
        ServerPlayer p = source.getPlayer();
        int count = 0;
        for (TameData d : ownedGroup(p.getUUID(), group)) {
            TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (ta == null) continue;
            ta.setTarget(null);
            count++;
        }
        p.sendSystemMessage(Component.literal("Removed targets from " + count + " loaded tames in group " + group + "."));
        return 1;
    }

    private static int removeTargetType(CommandSourceStack source, String typeFilter) {
        ServerPlayer p = source.getPlayer();
        int count = 0;
        for (TameData d : ownedType(p.getUUID(), typeFilter)) {
            TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (ta == null) continue;
            ta.setTarget(null);
            count++;
        }
        p.sendSystemMessage(Component.literal("Removed targets from " + count + " loaded tames of type '" + typeFilter + "'."));
        return 1;
    }

    private static int removeTargetState(CommandSourceStack source, String stateName) {
        ServerPlayer p = source.getPlayer();
        MovementOrder order = parseMovementOrder(stateName);
        if (order == null) return error(p, "Invalid state. Use follow, wander, or sit.");
        int count = 0;
        for (TamableAnimal tame : loadedOwnedStateTames(source, p.getUUID(), order)) {
            tame.setTarget(null);
            count++;
        }
        p.sendSystemMessage(Component.literal("Removed targets from " + count + " loaded " + movementLabel(order) + " tames."));
        return 1;
    }

    private static int xpAdd(CommandSourceStack source, String pet, int amount) {
        ServerPlayer p = source.getPlayer();
        TameData d = resolveAdminAliveTame(p, pet);
        if (d == null) return 0;
        Entity e = findLoadedTameByUuid(source, d.uuid);
        if (!(e instanceof TamableAnimal ta) || !ta.isTame()) return error(p, "Pet is not loaded.");
        LevelSystem.grantXP(ta, d, amount);
        p.sendSystemMessage(Component.literal("Added " + amount + " XP."));
        return 1;
    }

    private static int xpRemove(CommandSourceStack source, String pet, int amount) {
        ServerPlayer p = source.getPlayer();
        TameData d = resolveAdminAliveTame(p, pet);
        if (d == null) return 0;
        d.xp = Math.max(0, d.xp - amount);
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Removed " + amount + " XP."));
        return 1;
    }

    private static int abilityAdd(CommandSourceStack source, String pet, String id, int levels) {
        ServerPlayer p = source.getPlayer();
        TameData d = resolveAdminAliveTame(p, pet);
        if (d == null) return 0;
        if ("berserker".equalsIgnoreCase(id) || "passive".equalsIgnoreCase(id)) {
            return error(p, "This admin ability is obsolete.");
        }
        if (!LevelSystem.knownAbilityIds().contains(id)) return error(p, "Unknown ability.");
        if (!LevelSystem.addAbility(d, id, levels)) return error(p, "No change.");
        p.sendSystemMessage(Component.literal("Added/updated ability " + id + " Lv " + LevelSystem.getAbilityLevel(d, id)));
        return 1;
    }

    private static int abilityRemove(CommandSourceStack source, String pet, String id, int levels) {
        ServerPlayer p = source.getPlayer();
        TameData d = resolveAdminAliveTame(p, pet);
        if (d == null) return 0;
        if ("berserker".equalsIgnoreCase(id) || "passive".equalsIgnoreCase(id)) {
            return error(p, "This admin ability is obsolete.");
        }
        if (!LevelSystem.knownAbilityIds().contains(id)) return error(p, "Unknown ability.");
        if (!LevelSystem.removeAbility(d, id, levels)) return error(p, "No change.");
        p.sendSystemMessage(Component.literal("Updated ability " + id + " Lv " + LevelSystem.getAbilityLevel(d, id)));
        return 1;
    }

    private static int abilityClear(CommandSourceStack source, String pet) {
        ServerPlayer p = source.getPlayer();
        TameData d = resolveAdminAliveTame(p, pet);
        if (d == null) return 0;
        d.abilities.clear();
        d.abilityLevels.clear();
        d.cooldowns.clear();
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Abilities cleared."));
        return 1;
    }

    private static int abilityList(CommandSourceStack source, String pet) {
        ServerPlayer p = source.getPlayer();
        TameData d = resolveAdminAliveTame(p, pet);
        if (d == null) return 0;
        if (d.abilityLevels.isEmpty()) {
            p.sendSystemMessage(Component.literal("No abilities."));
            return 1;
        }
        d.abilityLevels.forEach((id, lvl) -> p.sendSystemMessage(Component.literal("- " + id + " (Lv " + lvl + ")")));
        return 1;
    }

    private static int attributeAdd(CommandSourceStack source, String pet, String id, int levels) {
        ServerPlayer p = source.getPlayer();
        TameData d = resolveAdminAliveTame(p, pet);
        if (d == null) return 0;
        if (!LevelSystem.knownAttributeIds().contains(id)) return error(p, "Unknown attribute.");
        if (!LevelSystem.addAttribute(d, id, levels)) return error(p, "No change.");
        p.sendSystemMessage(Component.literal("Added/updated attribute " + id + " Lv " + LevelSystem.getAttributeLevel(d, id)));
        return 1;
    }

    private static int attributeRemove(CommandSourceStack source, String pet, String id, int levels) {
        ServerPlayer p = source.getPlayer();
        TameData d = resolveAdminAliveTame(p, pet);
        if (d == null) return 0;
        if (!LevelSystem.knownAttributeIds().contains(id)) return error(p, "Unknown attribute.");
        if (!LevelSystem.removeAttribute(d, id, levels)) return error(p, "No change.");
        p.sendSystemMessage(Component.literal("Updated attribute " + id + " Lv " + LevelSystem.getAttributeLevel(d, id)));
        return 1;
    }

    private static int attributeClear(CommandSourceStack source, String pet) {
        ServerPlayer p = source.getPlayer();
        TameData d = resolveAdminAliveTame(p, pet);
        if (d == null) return 0;
        d.attributeLevels.clear();
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Attributes cleared."));
        return 1;
    }

    private static int attributeList(CommandSourceStack source, String pet) {
        ServerPlayer p = source.getPlayer();
        TameData d = resolveAdminAliveTame(p, pet);
        if (d == null) return 0;
        if (d.attributeLevels.isEmpty()) {
            p.sendSystemMessage(Component.literal("No attributes."));
            return 1;
        }
        d.attributeLevels.forEach((id, lvl) -> p.sendSystemMessage(Component.literal("- " + id + " (Lv " + lvl + ")")));
        return 1;
    }

    private static int adminResetWolvesBase(CommandSourceStack source) {
        int resetData = 0;
        int resetLoaded = 0;
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!"entity.minecraft.wolf".equalsIgnoreCase(d.type) && !"minecraft:wolf".equalsIgnoreCase(d.type)) continue;
            resetProgressData(d, false);
            resetData++;
        }
        for (var level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Wolf wolf) || !wolf.isTame()) continue;
                setTamableBaseCombatStats(wolf);
                TameData d = TameRegistry.get(wolf.getUUID());
                if (d != null) {
                    LevelSystem.updateTameName(wolf, d);
                }
                wolf.setHealth(wolf.getMaxHealth());
                resetLoaded++;
            }
        }
        TameRegistry.markDirty();
        final int loadedCount = resetLoaded;
        final int dataCount = resetData;
        source.sendSuccess(() -> Component.literal("Reset wolf base stats for " + loadedCount + " loaded wolves; reset progress data for " + dataCount + " wolf entries."), true);
        return 1;
    }

    private static int adminResetServerProgress(CommandSourceStack source) {
        int resetData = TameRegistry.TAMES.size();
        int resetDeaths = TameRegistry.DEATH_HISTORY.size();
        int resetLoaded = 0;
        int clearedRespawnRequests = 0;
        // Normalize loaded tame entities first so stale attribute bonuses are removed immediately.
        TameRegistry.LAST_DEATHS.clear();
        for (var level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal tame) || !tame.isTame()) continue;
                setTamableBaseCombatStats(tame);
                tame.setHealth(tame.getMaxHealth());
                resetLoaded++;
            }
        }
        // Wipe all persisted progression/history as if the mod was freshly installed.
        TameRegistry.TAMES.clear();
        TameRegistry.LAST_DEATHS.clear();
        TameRegistry.DEATH_HISTORY.clear();
        DIWorldData worldData = DIWorldData.get(source.getLevel());
        if (worldData != null) {
            clearedRespawnRequests = worldData.clearAllRespawnRequests();
        }
        TameRegistry.markDirty();
        final int loadedCount = resetLoaded;
        final int dataCount = resetData;
        final int deathCount = resetDeaths;
        final int clearedRequests = clearedRespawnRequests;
        source.sendSuccess(() -> Component.literal("Server progress fully wiped: " + dataCount + " tame entries, " + deathCount + " death-history entries, " + clearedRequests + " queued bed respawns, and " + loadedCount + " loaded tames normalized to base stats."), true);
        return 1;
    }

    private static int adminSetClass(CommandSourceStack source, String petName, String className) {
        TameClass tameClass;
        try {
            tameClass = TameClass.valueOf(className.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return error(source.getPlayer(), "Unknown class. Use one of: " + String.join(", ", classNames()));
        }

        List<TameData> matches = findAliveTamesByName(petName);
        if (matches.isEmpty()) {
            return error(source.getPlayer(), "No alive tame found with that name.");
        }
        if (matches.size() > 1) {
            return error(source.getPlayer(), "Ambiguous tame name (" + matches.size() + " matches). Rename duplicates first.");
        }

        TameData data = matches.get(0);
        data.tameClass = tameClass;
        TameRegistry.markDirty();

        TamableAnimal tame = findLoadedTameByUuid(source, data.uuid);
        if (tame != null) {
            LevelSystem.updateTameName(tame, data);
        }

        source.sendSuccess(() -> Component.literal("Set class of " + data.name + " to " + tameClass.name() + "."), true);
        return 1;
    }

    private static int adminUniteDuplicates(CommandSourceStack source, String nameFilter, boolean all) {
        String wanted = nameFilter == null ? "" : nameFilter.trim();
        int groupsProcessed = 0;
        int removedEntries = 0;
        int removedEntities = 0;
        int[] strict = processDuplicateUnitePass(source, wanted, all, false);
        groupsProcessed += strict[0];
        removedEntries += strict[1];
        removedEntities += strict[2];
        if (removedEntries == 0) {
            int[] relaxed = processDuplicateUnitePass(source, wanted, all, true);
            groupsProcessed += relaxed[0];
            removedEntries += relaxed[1];
            removedEntities += relaxed[2];
        }

        if (removedEntries > 0) {
            TameRegistry.markDirty();
        }
        final int groupsDone = groupsProcessed;
        final int removedRows = removedEntries;
        final int removedLoaded = removedEntities;
        source.sendSuccess(() -> Component.literal(
                "Unite duplicates: groups " + groupsDone + ", removed entries " + removedRows + ", removed loaded clones " + removedLoaded + "."
        ), true);
        return removedEntries > 0 ? 1 : 0;
    }

    private static int[] processDuplicateUnitePass(CommandSourceStack source, String wanted, boolean all, boolean relaxedKey) {
        Map<String, List<TameData>> groups = new HashMap<>();
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.uuid == null || data.ownerUUID == null) continue;
            if (isDeadEntry(data.uuid)) continue;
            if (!all) {
                if (wanted == null || wanted.isBlank()) continue;
                if (data.name == null || !data.name.equalsIgnoreCase(wanted)) continue;
            }
            String key = relaxedKey ? duplicateGroupKeyRelaxed(data) : duplicateGroupKey(data);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(data);
        }

        int groupsProcessed = 0;
        int removedEntries = 0;
        int removedEntities = 0;
        for (List<TameData> group : groups.values()) {
            if (group == null || group.size() <= 1) continue;
            groupsProcessed++;
            group.sort((a, b) -> {
                boolean aLoaded = findLoadedTameByUuid(source, a.uuid) != null;
                boolean bLoaded = findLoadedTameByUuid(source, b.uuid) != null;
                if (aLoaded != bLoaded) return aLoaded ? -1 : 1;
                if (a.level != b.level) return Integer.compare(b.level, a.level);
                if (a.kills != b.kills) return Integer.compare(b.kills, a.kills);
                return String.valueOf(a.uuid).compareTo(String.valueOf(b.uuid));
            });

            TameData keeper = group.get(0);
            for (int i = 1; i < group.size(); i++) {
                TameData duplicate = group.get(i);
                if (duplicate == null || duplicate.uuid == null || duplicate.uuid.equals(keeper.uuid)) continue;
                mergeDuplicateIntoKeeper(keeper, duplicate);

                TamableAnimal loaded = findLoadedTameByUuid(source, duplicate.uuid);
                if (loaded != null) {
                    TameDuelManager.endDuelForTame(source.getServer(), loaded.getUUID());
                    if (loaded.isAlive()) {
                        loaded.discard();
                    }
                    removedEntities++;
                }

                if (TameRegistry.TAMES.remove(duplicate.uuid) != null) {
                    removedEntries++;
                }
                TameRegistry.LAST_DEATHS.remove(duplicate.uuid);
                TameRegistry.DEATH_HISTORY.removeIf(r -> r != null && duplicate.uuid.equals(r.uuid));
            }

            TamableAnimal keeperLoaded = findLoadedTameByUuid(source, keeper.uuid);
            if (keeperLoaded != null) {
                LevelSystem.updateTameName(keeperLoaded, keeper);
            }
        }
        return new int[]{groupsProcessed, removedEntries, removedEntities};
    }

    private static String duplicateGroupKey(TameData data) {
        if (data == null || data.ownerUUID == null) return "";
        String name = data.name == null ? "unknown" : stripLevelPrefixes(data.name).toLowerCase(Locale.ROOT);
        String type = tameTypeId(data);
        if (type.isBlank()) type = "unknown";
        return data.ownerUUID + "|" + name + "|" + type;
    }

    private static String duplicateGroupKeyRelaxed(TameData data) {
        if (data == null || data.ownerUUID == null) return "";
        String name = data.name == null ? "unknown" : stripLevelPrefixes(data.name).toLowerCase(Locale.ROOT);
        return data.ownerUUID + "|" + name;
    }

    private static void mergeDuplicateIntoKeeper(TameData keeper, TameData duplicate) {
        if (keeper == null || duplicate == null) return;
        keeper.level = Math.max(keeper.level, duplicate.level);
        keeper.xp = Math.max(keeper.xp, duplicate.xp);
        keeper.xpToNext = Math.max(keeper.xpToNext, duplicate.xpToNext);
        keeper.kills = Math.max(keeper.kills, duplicate.kills);
        keeper.assists = Math.max(keeper.assists, duplicate.assists);
        keeper.deaths = Math.max(keeper.deaths, duplicate.deaths);
        keeper.bornDayTime = keeper.bornDayTime <= 0L ? duplicate.bornDayTime : Math.min(keeper.bornDayTime, duplicate.bornDayTime);

        keeper.bonusHealth = Math.max(keeper.bonusHealth, duplicate.bonusHealth);
        keeper.bonusDamage = Math.max(keeper.bonusDamage, duplicate.bonusDamage);
        keeper.bonusSpeed = Math.max(keeper.bonusSpeed, duplicate.bonusSpeed);
        keeper.bonusArmor = Math.max(keeper.bonusArmor, duplicate.bonusArmor);
        keeper.bonusArmorToughness = Math.max(keeper.bonusArmorToughness, duplicate.bonusArmorToughness);
        keeper.bonusKnockback = Math.max(keeper.bonusKnockback, duplicate.bonusKnockback);
        keeper.bonusKnockbackResist = Math.max(keeper.bonusKnockbackResist, duplicate.bonusKnockbackResist);

        keeper.abilities.addAll(duplicate.abilities);
        for (Map.Entry<String, Integer> entry : duplicate.abilityLevels.entrySet()) {
            int current = keeper.abilityLevels.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > current) {
                keeper.abilityLevels.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, Integer> entry : duplicate.attributeLevels.entrySet()) {
            int current = keeper.attributeLevels.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > current) {
                keeper.attributeLevels.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, Long> entry : duplicate.cooldowns.entrySet()) {
            long current = keeper.cooldowns.getOrDefault(entry.getKey(), Long.MAX_VALUE);
            keeper.cooldowns.put(entry.getKey(), Math.min(current, entry.getValue()));
        }

        if (!keeper.hasSavedProgress && duplicate.hasSavedProgress) {
            keeper.hasSavedProgress = true;
            keeper.savedProgressCost = duplicate.savedProgressCost;
            keeper.savedLevel = duplicate.savedLevel;
            keeper.savedXp = duplicate.savedXp;
            keeper.savedXpToNext = duplicate.savedXpToNext;
            keeper.savedKills = duplicate.savedKills;
            keeper.savedAssists = duplicate.savedAssists;
            keeper.savedBonusHealth = duplicate.savedBonusHealth;
            keeper.savedBonusDamage = duplicate.savedBonusDamage;
            keeper.savedBonusSpeed = duplicate.savedBonusSpeed;
            keeper.savedBonusArmor = duplicate.savedBonusArmor;
            keeper.savedBonusArmorToughness = duplicate.savedBonusArmorToughness;
            keeper.savedBonusKnockback = duplicate.savedBonusKnockback;
            keeper.savedBonusKnockbackResist = duplicate.savedBonusKnockbackResist;
            keeper.savedAbilities.clear();
            keeper.savedAbilities.addAll(duplicate.savedAbilities);
            keeper.savedAbilityLevels.clear();
            keeper.savedAbilityLevels.putAll(duplicate.savedAbilityLevels);
            keeper.savedAttributeLevels.clear();
            keeper.savedAttributeLevels.putAll(duplicate.savedAttributeLevels);
        }

        if (!keeper.hasHome && duplicate.hasHome) {
            keeper.hasHome = true;
            keeper.homeDimension = duplicate.homeDimension;
            keeper.homeX = duplicate.homeX;
            keeper.homeY = duplicate.homeY;
            keeper.homeZ = duplicate.homeZ;
        }
        if ((keeper.entitySnapshot == null || keeper.entitySnapshot.isEmpty()) && duplicate.entitySnapshot != null) {
            keeper.entitySnapshot = duplicate.entitySnapshot.copy();
        }
    }

    private static int adminForceReincarnate(CommandSourceStack source, String petName, int index) {
        List<TameData> matches = findAliveTamesByName(petName);
        if (matches.isEmpty()) {
            return error(source.getPlayer(), "No alive tame found with that name.");
        }
        if (matches.size() > 1) {
            return error(source.getPlayer(), "Ambiguous tame name (" + matches.size() + " matches). Rename duplicates first.");
        }

        TameData liveData = matches.get(0);
        if (liveData.ownerUUID == null) {
            return error(source.getPlayer(), "Target tame has no owner UUID.");
        }
        TamableAnimal tame = findLoadedTameByUuid(source, liveData.uuid);
        if (tame == null) {
            return error(source.getPlayer(), "Target tame is not loaded.");
        }

        List<TameDeathRecord> entries = deathHistoryForName(liveData.ownerUUID, liveData.name, liveData.tlId);
        if (entries.isEmpty()) {
            backfillLegacyDeathRecordsForName(liveData.ownerUUID, liveData.name, liveData.tlId);
            entries = deathHistoryForName(liveData.ownerUUID, liveData.name, liveData.tlId);
        }
        if (entries.isEmpty()) {
            return error(source.getPlayer(), "No death history found for that tame.");
        }
        if (index <= 0) {
            source.getPlayer().sendSystemMessage(Component.literal("Reincarnation history for '" + liveData.name + "':").withStyle(ChatFormatting.YELLOW));
            int shown = Math.min(8, entries.size());
            for (int i = 0; i < shown; i++) {
                TameDeathRecord r = entries.get(i);
                source.getPlayer().sendSystemMessage(Component.literal((i + 1) + ". L" + r.level + " K" + r.kills + " A" + r.assists + " D" + r.deaths
                        + " at " + formatDeathTime(r.deathUnixMillis) + " (" + r.deathDimension + " " + r.deathX + "," + r.deathY + "," + r.deathZ + ")")
                        .withStyle(ChatFormatting.GRAY));
            }
            if (entries.size() > shown) {
                source.getPlayer().sendSystemMessage(Component.literal("... and " + (entries.size() - shown) + " more.").withStyle(ChatFormatting.DARK_GRAY));
            }
            return error(source.getPlayer(), "Use /tames admin forceReincarnate <pet> <index> (1.." + entries.size() + ").");
        }
        if (index < 1 || index > entries.size()) {
            return error(source.getPlayer(), "Invalid history index.");
        }

        TameDeathRecord chosen = entries.get(index - 1);
        if (chosen.snapshot == null || chosen.snapshot.isEmpty()) {
            return error(source.getPlayer(), "That death entry has no saved snapshot.");
        }

        TameData snapshot = TameData.fromTag(chosen.snapshot.copy());
        applySnapshotToTame(tame, liveData, snapshot);
        chosen.reincarnated = true;
        TameRegistry.removeLastDeath(chosen);
        TameRegistry.markDirty();
        source.sendSuccess(() -> Component.literal(
                "Force-reincarnated " + liveData.name + " from history #" + index + " (no XP cost)."
        ), true);
        return 1;
    }

    private static int adminTerminatePet(CommandSourceStack source, String petName) {
        List<TameData> aliveMatches = findAliveTamesByName(petName);
        if (aliveMatches.size() > 1) {
            return error(source.getPlayer(), "Ambiguous alive tame name (" + aliveMatches.size() + " matches). Rename duplicates first.");
        }

        int removedRegistry = 0;
        int removedLastDeaths = 0;
        int removedHistory = 0;
        UUID targetUuid = null;
        UUID targetTlId = null;

        if (!aliveMatches.isEmpty()) {
            TameData data = aliveMatches.get(0);
            targetUuid = data.uuid;
            targetTlId = data.tlId;
            TamableAnimal loaded = findLoadedTameByUuid(source, data.uuid);
            if (loaded != null) {
                TameDuelManager.endDuelForTame(source.getServer(), loaded.getUUID());
                loaded.kill();
                if (loaded.isAlive()) {
                    loaded.discard();
                }
            }
            if (TameRegistry.TAMES.remove(data.uuid) != null) {
                removedRegistry++;
            }
        }

        if (targetUuid != null) {
            removedLastDeaths += TameRegistry.removeDeathsForIdentity(targetUuid, targetTlId);
            UUID removeUuid = targetUuid;
            UUID removeTl = targetTlId;
            int before = TameRegistry.DEATH_HISTORY.size();
            TameRegistry.DEATH_HISTORY.removeIf(r -> r != null && (removeUuid.equals(r.uuid) || (removeTl != null && removeTl.equals(r.tlId))));
            removedHistory += Math.max(0, before - TameRegistry.DEATH_HISTORY.size());
        } else {
            int beforeLast = TameRegistry.LAST_DEATHS.size();
            TameRegistry.LAST_DEATHS.entrySet().removeIf(e -> {
                TameDeathRecord r = e.getValue();
                return r != null && r.name != null && r.name.equalsIgnoreCase(petName);
            });
            removedLastDeaths += Math.max(0, beforeLast - TameRegistry.LAST_DEATHS.size());

            int beforeHistory = TameRegistry.DEATH_HISTORY.size();
            TameRegistry.DEATH_HISTORY.removeIf(r -> r != null && r.name != null && r.name.equalsIgnoreCase(petName));
            removedHistory += Math.max(0, beforeHistory - TameRegistry.DEATH_HISTORY.size());
        }

        if (removedRegistry <= 0 && removedLastDeaths <= 0 && removedHistory <= 0) {
            return error(source.getPlayer(), "No tame or death entries found for '" + petName + "'.");
        }

        TameRegistry.markDirty();
        int removedRegistryCount = removedRegistry;
        int removedLastCount = removedLastDeaths;
        int removedHistoryCount = removedHistory;
        source.sendSuccess(() -> Component.literal(
                "Terminated '" + petName + "': removed " + removedRegistryCount + " live entry, "
                        + removedLastCount + " last-death entry, " + removedHistoryCount + " death-history entries."
        ), true);
        return 1;
    }

    private static int adminSetFriendlyFire(CommandSourceStack source, boolean enabled) {
        TLAdminRuntimeSettings.setFriendlyFireEnabled(enabled);
        source.sendSuccess(() -> Component.literal(
                "Temporary admin setting: friendly fire is now " + (enabled ? "ENABLED" : "DISABLED") + "."
        ).withStyle(enabled ? ChatFormatting.YELLOW : ChatFormatting.GREEN), true);
        return 1;
    }

    private static int adminAbilityDamageNerfStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "Ability damage nerfs -> single: "
                        + TLAdminRuntimeSettings.singleTargetAbilityDamageMultiplier()
                        + ", aoe: "
                        + TLAdminRuntimeSettings.aoeAbilityDamageMultiplier()
        ).withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int adminSetAbilityDamageNerf(CommandSourceStack source, String type, double multiplier) {
        float value = (float) Math.max(0.0D, multiplier);
        if ("single".equalsIgnoreCase(type)) {
            TLAdminRuntimeSettings.setSingleTargetAbilityDamageMultiplier(value);
        } else if ("aoe".equalsIgnoreCase(type)) {
            TLAdminRuntimeSettings.setAoeAbilityDamageMultiplier(value);
        } else {
            return error(source.getPlayer(), "Unknown nerf type: " + type);
        }
        source.sendSuccess(() -> Component.literal(
                "Temporary admin setting: " + type + " ability damage multiplier is now " + value + "."
        ).withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int adminAbilityCooldownNerfStatus(CommandSourceStack source) {
        float percent = TLAdminRuntimeSettings.abilityCountCooldownNerfPercent();
        source.sendSuccess(() -> Component.literal(
                "Attack-ability cooldown nerf percent is " + percent + "%. Multiplier = 1 + (" + percent + "% * log2(attackAbilityCount)). Heal/support abilities do not count and are not affected."
        ).withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int adminSetAbilityCooldownNerf(CommandSourceStack source, double percent) {
        float value = (float) Math.max(0.0D, percent);
        TLAdminRuntimeSettings.setAbilityCountCooldownNerfPercent(value);
        source.sendSuccess(() -> Component.literal(
                "Temporary admin setting: attack-ability cooldown nerf is now " + value + "% per log2 attack-ability step. Heal/support abilities do not count and are not affected."
        ).withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int adminCleanPlayerList(CommandSourceStack source, String playerName) {
        MinecraftServer server = source.getServer();
        UUID ownerId = resolveKnownOwnerUuid(server, playerName);
        if (ownerId == null) {
            return error(source.getPlayer(), "Unknown player: " + playerName);
        }
        String ownerName = resolveKnownOwnerName(server, ownerId, playerName);
        List<TameData> entries = ownedAllTamesSorted(ownerId);
        if (entries.isEmpty()) {
            return error(source.getPlayer(), ownerName + " has no registered tames.");
        }
        source.getPlayer().sendSystemMessage(Component.literal("---- " + ownerName + " Tames (admin clean) ----").withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < entries.size(); i++) {
            TameData d = entries.get(i);
            long days = daysAlive(source, d);
            String line = (i + 1) + ". [" + d.level + "] " + d.name + " (" + d.kills + "/" + d.assists + "/" + days + "/" + d.deaths + ")"
                    + (isDeadEntry(d.uuid) ? " [DEAD]" : "");
            source.getPlayer().sendSystemMessage(Component.literal(line).withStyle(isDeadEntry(d.uuid) ? ChatFormatting.GRAY : ChatFormatting.WHITE));
        }
        source.getPlayer().sendSystemMessage(Component.literal("Use /tames admin clean \"" + ownerName + "\" <index> to terminate an entry.").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int adminCleanPlayerIndex(CommandSourceStack source, String playerName, int index) {
        MinecraftServer server = source.getServer();
        UUID ownerId = resolveKnownOwnerUuid(server, playerName);
        if (ownerId == null) {
            return error(source.getPlayer(), "Unknown player: " + playerName);
        }
        String ownerName = resolveKnownOwnerName(server, ownerId, playerName);
        List<TameData> entries = ownedAllTamesSorted(ownerId);
        if (entries.isEmpty()) {
            return error(source.getPlayer(), ownerName + " has no registered tames.");
        }
        if (index < 1 || index > entries.size()) {
            return error(source.getPlayer(), "Invalid index. Use 1.." + entries.size() + ".");
        }
        TameData selected = entries.get(index - 1);
        return adminTerminateByUuid(source, selected.uuid, selected.name, ownerName, index);
    }

    private static UUID resolveKnownOwnerUuid(MinecraftServer server, String playerName) {
        if (server == null || playerName == null || playerName.isBlank()) {
            return null;
        }
        ServerPlayer online = server.getPlayerList().getPlayerByName(playerName);
        if (online != null) {
            return online.getUUID();
        }
        Optional<GameProfile> cached = server.getProfileCache().get(playerName);
        if (cached.isPresent() && cached.get().getId() != null) {
            return cached.get().getId();
        }
        return null;
    }

    private static String resolveKnownOwnerName(MinecraftServer server, UUID ownerId, String fallback) {
        if (server == null || ownerId == null) {
            return fallback == null || fallback.isBlank() ? "Unknown" : fallback;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(ownerId);
        if (online != null) {
            return online.getName().getString();
        }
        Optional<GameProfile> cached = server.getProfileCache().get(ownerId);
        if (cached.isPresent() && cached.get().getName() != null && !cached.get().getName().isBlank()) {
            return cached.get().getName();
        }
        return fallback == null || fallback.isBlank() ? ownerId.toString() : fallback;
    }

    private static int adminTerminateByUuid(CommandSourceStack source, UUID tameUuid, String tameName, String ownerName, int index) {
        if (tameUuid == null) {
            return error(source.getPlayer(), "Selected entry has no UUID.");
        }

        int removedRegistry = 0;
        int removedLastDeaths = 0;
        int removedHistory = 0;

        TamableAnimal loaded = findLoadedTameByUuid(source, tameUuid);
        if (loaded != null) {
            TameDuelManager.endDuelForTame(source.getServer(), loaded.getUUID());
            loaded.kill();
            if (loaded.isAlive()) {
                loaded.discard();
            }
        }
        if (TameRegistry.TAMES.remove(tameUuid) != null) {
            removedRegistry++;
        }
        TameData registryData = TameRegistry.get(tameUuid);
        UUID tlId = registryData == null ? null : registryData.tlId;
        removedLastDeaths += TameRegistry.removeDeathsForIdentity(tameUuid, tlId);
        int beforeHistory = TameRegistry.DEATH_HISTORY.size();
        TameRegistry.DEATH_HISTORY.removeIf(r -> r != null && (tameUuid.equals(r.uuid) || (tlId != null && tlId.equals(r.tlId))));
        removedHistory += Math.max(0, beforeHistory - TameRegistry.DEATH_HISTORY.size());

        if (removedRegistry <= 0 && removedLastDeaths <= 0 && removedHistory <= 0) {
            return error(source.getPlayer(), "No registry/death entries removed for selected tame.");
        }
        TameRegistry.markDirty();
        int rr = removedRegistry;
        int rd = removedLastDeaths;
        int rh = removedHistory;
        source.sendSuccess(() -> Component.literal(
                "Cleaned " + ownerName + " #" + index + " '" + tameName + "': removed " + rr + " live/dead row, "
                        + rd + " last-death row, " + rh + " death-history rows."
        ), true);
        return 1;
    }

    private static int adminRespawnPet(CommandSourceStack source, String petName, int index) {
        List<TameData> matches = findAliveTamesByName(petName);
        if (matches.size() > 1) {
            return error(source.getPlayer(), "Ambiguous tame name (" + matches.size() + " matches). Rename duplicates first.");
        }
        TameData data;
        TameDeathRecord deadRecord = null;
        if (!matches.isEmpty()) {
            data = matches.get(0);
        } else {
            List<TameDeathRecord> deadMatches = findDeadRespawnRecordsByName(petName);
            if (!deadMatches.isEmpty()) {
                if (index <= 0 && deadMatches.size() > 1) {
                    source.getPlayer().sendSystemMessage(Component.literal("Multiple dead matches for '" + petName + "':").withStyle(ChatFormatting.YELLOW));
                    int shown = Math.min(5, deadMatches.size());
                    for (int i = 0; i < shown; i++) {
                        TameDeathRecord r = deadMatches.get(i);
                        source.getPlayer().sendSystemMessage(Component.literal((i + 1) + ". L" + r.level + " K" + r.kills + " A" + r.assists + " D" + r.deaths
                                + " at " + formatDeathTime(r.deathUnixMillis) + " (" + r.deathDimension + " " + r.deathX + "," + r.deathY + "," + r.deathZ + ")")
                                .withStyle(ChatFormatting.GRAY));
                    }
                    if (deadMatches.size() > shown) {
                        source.getPlayer().sendSystemMessage(Component.literal("... and " + (deadMatches.size() - shown) + " more.").withStyle(ChatFormatting.DARK_GRAY));
                    }
                    return error(source.getPlayer(), "Use /tames admin respawn <pet> <index> (1.." + deadMatches.size() + ").");
                }
                int chosenIndex = index <= 0 ? 1 : index;
                if (chosenIndex < 1 || chosenIndex > deadMatches.size()) {
                    return error(source.getPlayer(), "Invalid dead-match index. Use 1.." + deadMatches.size() + ".");
                }
                deadRecord = deadMatches.get(chosenIndex - 1);
                if (deadRecord.snapshot == null || deadRecord.snapshot.isEmpty()) {
                    return error(source.getPlayer(), "Cannot respawn this dead tame: missing saved snapshot.");
                }
                data = TameData.fromTag(deadRecord.snapshot.copy());
                if (data.name == null || data.name.isBlank()) {
                    data.name = deadRecord.name == null ? petName : deadRecord.name;
                }
                if (data.uuid == null) {
                    return error(source.getPlayer(), "Cannot respawn this dead tame: missing UUID.");
                }
                TameRegistry.register(data);
            } else {
                List<TameData> deadRows = findDeadRegistryRowsByName(petName);
                if (deadRows.isEmpty()) {
                    return error(source.getPlayer(), "No tame found with that name.");
                }
                if (index <= 0 && deadRows.size() > 1) {
                    source.getPlayer().sendSystemMessage(Component.literal("Multiple dead registry matches for '" + petName + "':").withStyle(ChatFormatting.YELLOW));
                    int shown = Math.min(5, deadRows.size());
                    for (int i = 0; i < shown; i++) {
                        TameData r = deadRows.get(i);
                        source.getPlayer().sendSystemMessage(Component.literal((i + 1) + ". [" + r.level + "] " + r.name
                                + " at " + formatDeathTime(r.deadUnixMillis) + " (" + r.deathDimension + " " + r.deathX + "," + r.deathY + "," + r.deathZ + ")")
                                .withStyle(ChatFormatting.GRAY));
                    }
                    if (deadRows.size() > shown) {
                        source.getPlayer().sendSystemMessage(Component.literal("... and " + (deadRows.size() - shown) + " more.").withStyle(ChatFormatting.DARK_GRAY));
                    }
                    return error(source.getPlayer(), "Use /tames admin respawn <pet> <index> (1.." + deadRows.size() + ").");
                }
                int chosenIndex = index <= 0 ? 1 : index;
                if (chosenIndex < 1 || chosenIndex > deadRows.size()) {
                    return error(source.getPlayer(), "Invalid dead-match index. Use 1.." + deadRows.size() + ".");
                }
                data = deadRows.get(chosenIndex - 1);
            }
        }
        TamableAnimal loaded = findLoadedTameByUuid(source, data.uuid);
        if (loaded != null) {
            loaded.discard();
        }

        String typeId = recoverEntityTypeId(data);
        if (typeId.isBlank()) return error(source.getPlayer(), "Cannot respawn this tame: missing saved entity type.");

        ResourceLocation id = ResourceLocation.tryParse(typeId);
        if (id == null) return error(source.getPlayer(), "Cannot respawn this tame: invalid entity type '" + typeId + "'.");

        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (entityType == null) return error(source.getPlayer(), "Cannot respawn this tame: unknown entity type '" + typeId + "'.");

        ServerLevel level = source.getLevel();
        Entity spawned = entityType.create(level);
        if (!(spawned instanceof TamableAnimal respawned)) {
            return error(source.getPlayer(), "Cannot respawn this tame: stored type is not tamable.");
        }

        CompoundTag snapshot = data.entitySnapshot == null ? new CompoundTag() : data.entitySnapshot.copy();
        if (!snapshot.isEmpty()) {
            respawned.load(snapshot);
        }

        Vec3 pos = source.getPosition();
        respawned.setUUID(data.uuid);
        respawned.moveTo(pos.x, pos.y, pos.z, source.getRotation().y, source.getRotation().x);
        respawned.setDeltaMovement(0.0D, 0.0D, 0.0D);
        enforceTamedOwnerPreserveCollar(respawned, data.ownerUUID);

        if (!level.addFreshEntity(respawned)) {
            if (deadRecord != null) {
                TameRegistry.remove(data.uuid);
            }
            return error(source.getPlayer(), "Failed to respawn tame (UUID conflict or invalid state).");
        }

        boolean normalized = applyTypeBasePlusBonus(respawned, data);
        if (!normalized) {
            LevelSystem.updateTameName(respawned, data);
            respawned.setHealth(respawned.getMaxHealth());
        }
        finalizeRespawnState(respawned, data);
        if (deadRecord != null) {
            deadRecord.reincarnated = true;
            deadRecord.autoReincarnateOnRespawn = false;
            TameRegistry.removeLastDeath(deadRecord);
        }
        TameRegistry.markDirty();
        if (normalized) {
            source.sendSuccess(() -> Component.literal("Respawned " + data.name + " at your position and normalized bonuses."), true);
        } else {
            source.sendSuccess(() -> Component.literal("Respawned " + data.name + " at your position (normalize failed: type template unavailable)."), true);
        }
        return 1;
    }

    private static int adminApproveHeldItem(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        ItemStack held = p.getMainHandItem();
        if (held.isEmpty()) {
            return error(p, "Hold an item in your main hand.");
        }
        String itemId = heldItemId(held);
        if (itemId == null || itemId.isBlank()) {
            return error(p, "Could not resolve held item id.");
        }

        boolean added = TameRegistry.APPROVED_REINCARNATE_ITEMS.add(itemId);
        TameRegistry.markDirty();
        if (added) {
            p.sendSystemMessage(Component.literal("Approved reincarnation item: " + itemId).withStyle(ChatFormatting.GREEN));
        } else {
            p.sendSystemMessage(Component.literal("Item already approved: " + itemId).withStyle(ChatFormatting.YELLOW));
        }
        return 1;
    }

    private static void backfillLegacyDeathRecordsForName(UUID owner, String name, UUID tlId) {
        if (owner == null || name == null || name.isBlank()) {
            return;
        }
        Set<UUID> existing = new HashSet<>();
        Set<UUID> existingTlIds = new HashSet<>();
        for (TameDeathRecord record : TameRegistry.DEATH_HISTORY) {
            if (record == null || record.uuid == null) continue;
            if (!owner.equals(record.ownerUUID)) continue;
            if (record.name == null || !record.name.equalsIgnoreCase(name)) continue;
            existing.add(record.uuid);
            if (record.tlId != null) {
                existingTlIds.add(record.tlId);
            }
        }
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.uuid == null) continue;
            if (!owner.equals(data.ownerUUID)) continue;
            if (data.name == null || !data.name.equalsIgnoreCase(name)) continue;
            if (!isDeadEntry(data.uuid)) continue;
            if (existing.contains(data.uuid)) continue;
            if (data.tlId != null && existingTlIds.contains(data.tlId)) continue;

            TameDeathRecord record = TameDeathRecord.fromTame(data, null, data.deadGameTime);
            record.deathUnixMillis = data.deadUnixMillis;
            record.deathDimension = data.deathDimension == null ? "" : data.deathDimension;
            record.deathX = data.deathX;
            record.deathY = data.deathY;
            record.deathZ = data.deathZ;
            TameRegistry.archiveDeath(record);
            existing.add(data.uuid);
            if (record.tlId != null) {
                existingTlIds.add(record.tlId);
            }
        }
    }

    private static int adminStripDiEnchantsFromHeldCollar(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        if (p == null || source.getServer() == null) {
            return 0;
        }

        int withCollar = 0;
        int stripped = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (!TameableUtils.couldBeTamed(living) || !TameableUtils.isTamed(living)) continue;
                if (!TameableUtils.hasCollar(living)) continue;
                withCollar++;
                if (!TameableUtils.hasAnyEnchants(living)) continue;
                TameableUtils.clearEnchants(living);
                stripped++;
            }
        }
        if (stripped <= 0) {
            return error(p, "No collar-tag enchantments found on loaded tames.");
        }
        p.sendSystemMessage(Component.literal("Removed all collar-tag enchantments from " + stripped + " loaded tame(s) with collars (" + withCollar + " collar wearer(s) checked).").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int adminStripAllCollarTagEnchants(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        MinecraftServer server = source.getServer();
        if (player == null || server == null) {
            return 0;
        }

        int wolvesStripped = 0;
        int inventoryStacksStripped = 0;
        int containersChecked = 0;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Wolf wolf && wolf.isTame() && TameableUtils.hasCollar(wolf) && TameableUtils.hasAnyEnchants(wolf)) {
                    TameableUtils.clearEnchants(wolf);
                    refreshRegistrySnapshotFor(wolf);
                    wolvesStripped++;
                }
                if (entity instanceof Container container && !(entity instanceof ServerPlayer)) {
                    inventoryStacksStripped += stripCollarTagEnchantsFromContainer(container);
                    containersChecked++;
                }
            }
        }

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            inventoryStacksStripped += stripCollarTagEnchantsFromContainer(online.getInventory());
            inventoryStacksStripped += stripCollarTagEnchantsFromContainer(online.getEnderChestInventory());
        }

        if (wolvesStripped == 0 && inventoryStacksStripped == 0) {
            return error(player, "No enchanted collar tags found on loaded wolves or loaded inventories.");
        }
        int finalWolvesStripped = wolvesStripped;
        int finalInventoryStacksStripped = inventoryStacksStripped;
        int finalContainersChecked = containersChecked;
        source.sendSuccess(() -> Component.literal("Stripped enchantments from " + finalWolvesStripped + " wolf collar(s) and " + finalInventoryStacksStripped + " collar tag stack(s) in inventories (" + finalContainersChecked + " loaded container(s) checked).").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int stripCollarTagEnchantsFromContainer(Container container) {
        if (container == null) {
            return 0;
        }
        int stripped = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!clearCollarTagEnchantments(stack)) {
                continue;
            }
            container.setItem(slot, stack);
            stripped++;
        }
        if (container instanceof BlockEntity blockEntity) {
            blockEntity.setChanged();
        }
        return stripped;
    }

    private static boolean clearCollarTagEnchantments(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(DIItemRegistry.COLLAR_TAG.get())) {
            return false;
        }
        if (!stack.isEnchanted()) {
            return false;
        }
        EnchantmentHelper.setEnchantments(Map.of(), stack);
        if (stack.getTag() != null && stack.getTag().contains("StoredEnchantments", Tag.TAG_LIST)) {
            stack.getTag().remove("StoredEnchantments");
        }
        return true;
    }

    private static void refreshRegistrySnapshotFor(TamableAnimal tame) {
        if (tame == null) {
            return;
        }
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null && TameData.getTlId(tame) != null) {
            data = TameRegistry.getByTlId(TameData.getTlId(tame));
        }
        if (data == null) {
            return;
        }
        CompoundTag snapshot = new CompoundTag();
        tame.save(snapshot);
        data.entitySnapshot = snapshot;
        data.lastKnownDimension = tame.level().dimension().location().toString();
        data.lastKnownX = tame.blockPosition().getX();
        data.lastKnownY = tame.blockPosition().getY();
        data.lastKnownZ = tame.blockPosition().getZ();
        data.lastKnownGameTime = tame.level().getGameTime();
        TameRegistry.markDirty();
    }

    private static int adminReloadTames(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        int scanned = 0;
        int added = 0;

        for (var level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal ta)) continue;
                if (!ta.isTame() || !p.getUUID().equals(ta.getOwnerUUID())) continue;
                scanned++;
                if (TameRegistry.get(ta.getUUID()) != null) continue;
                TameData data = TameSpawnEvents.registerOrRestoreTame(ta, false);
                if (data != null) {
                    added++;
                }
            }
        }

        p.sendSystemMessage(Component.literal("Reload tames complete: scanned " + scanned + ", added " + added + ".").withStyle(ChatFormatting.GREEN));
        return added > 0 ? 1 : 0;
    }

    private static int adminDoubleHpBonus(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        if (p == null || source.getServer() == null) {
            return 0;
        }

        int updatedEntries = 0;
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null) continue;
            boolean changed = false;
            if (data.bonusHealth != 0.0D) {
                data.bonusHealth *= 2.0D;
                changed = true;
            }
            if (data.savedBonusHealth != 0.0D) {
                data.savedBonusHealth *= 2.0D;
                changed = true;
            }
            if (changed) {
                updatedEntries++;
            }
        }

        int appliedLoaded = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal tame) || !tame.isTame()) continue;
                TameData data = TameRegistry.get(tame.getUUID());
                if (data == null) continue;
                if (applyTypeBasePlusBonus(tame, data)) {
                    appliedLoaded++;
                } else {
                    LevelSystem.updateTameName(tame, data);
                    tame.setHealth(tame.getMaxHealth());
                }
            }
        }

        if (updatedEntries > 0) {
            TameRegistry.markDirty();
            p.sendSystemMessage(Component.literal("Doubled HP bonus for " + updatedEntries + " tame registry entries; refreshed " + appliedLoaded + " loaded tames.").withStyle(ChatFormatting.GREEN));
            return 1;
        }
        p.sendSystemMessage(Component.literal("No tame entries had HP bonus to double.").withStyle(ChatFormatting.YELLOW));
        return 0;
    }

    private static int adminHalfHpBonus(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        if (p == null || source.getServer() == null) {
            return 0;
        }

        int updatedEntries = 0;
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null) continue;
            boolean changed = false;
            if (data.bonusHealth != 0.0D) {
                data.bonusHealth *= 0.5D;
                changed = true;
            }
            if (data.savedBonusHealth != 0.0D) {
                data.savedBonusHealth *= 0.5D;
                changed = true;
            }
            if (changed) {
                updatedEntries++;
            }
        }

        int appliedLoaded = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal tame) || !tame.isTame()) continue;
                TameData data = TameRegistry.get(tame.getUUID());
                if (data == null) continue;
                if (applyTypeBasePlusBonus(tame, data)) {
                    appliedLoaded++;
                } else {
                    LevelSystem.updateTameName(tame, data);
                    tame.setHealth(tame.getMaxHealth());
                }
            }
        }

        if (updatedEntries > 0) {
            TameRegistry.markDirty();
            p.sendSystemMessage(Component.literal("Halved HP bonus for " + updatedEntries + " tame registry entries; refreshed " + appliedLoaded + " loaded tames.").withStyle(ChatFormatting.GREEN));
            return 1;
        }
        p.sendSystemMessage(Component.literal("No tame entries had HP bonus to halve.").withStyle(ChatFormatting.YELLOW));
        return 0;
    }

    private static int adminRerollHalfDamageBonus(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        if (p == null || source.getServer() == null) {
            return 0;
        }

        int updatedEntries = 0;
        int rerolledDamagePoints = 0;
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null) continue;
            int rerolled = LevelSystem.rerollHalfDamageBonus(data);
            if (rerolled > 0) {
                updatedEntries++;
                rerolledDamagePoints += rerolled;
            }
        }

        int appliedLoaded = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal tame) || !tame.isTame()) continue;
                TameData data = TameRegistry.get(tame.getUUID());
                if (data == null) continue;
                if (applyTypeBasePlusBonus(tame, data)) {
                    appliedLoaded++;
                } else {
                    LevelSystem.updateTameName(tame, data);
                    tame.setHealth(tame.getMaxHealth());
                }
            }
        }

        if (updatedEntries > 0) {
            TameRegistry.markDirty();
            p.sendSystemMessage(Component.literal("Rerolled " + rerolledDamagePoints + " damage bonus points across " + updatedEntries + " tame registry entries; refreshed " + appliedLoaded + " loaded tames.").withStyle(ChatFormatting.GREEN));
            return 1;
        }
        p.sendSystemMessage(Component.literal("No tame entries had enough damage bonus to reroll.").withStyle(ChatFormatting.YELLOW));
        return 0;
    }

    private static int adminNormalizeAllBonuses(CommandSourceStack source) {
        int normalized = 0;
        int missingData = 0;
        int nonTamable = 0;

        for (var level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal tame) || !tame.isTame()) continue;
                TameData data = TameRegistry.get(tame.getUUID());
                if (data == null) {
                    missingData++;
                    continue;
                }
                if (!applyTypeBasePlusBonus(tame, data)) {
                    nonTamable++;
                    continue;
                }
                normalized++;
            }
        }

        TameRegistry.markDirty();
        final int done = normalized;
        final int skippedNoData = missingData;
        final int skippedType = nonTamable;
        source.sendSuccess(() -> Component.literal(
                "Normalized loaded tame bonuses: " + done + " fixed, " + skippedNoData + " missing registry data, " + skippedType + " type template failed."
        ), true);
        return done > 0 ? 1 : 0;
    }

    private static int adminNormalizePetBonus(CommandSourceStack source, String petName) {
        List<TameData> matches = findAliveTamesByName(petName);
        if (matches.isEmpty()) {
            return error(source.getPlayer(), "No alive tame found with that name.");
        }
        if (matches.size() > 1) {
            return error(source.getPlayer(), "Ambiguous tame name (" + matches.size() + " matches). Rename duplicates first.");
        }

        TameData data = matches.get(0);
        TamableAnimal tame = findLoadedTameByUuid(source, data.uuid);
        if (tame == null) {
            return error(source.getPlayer(), "Target tame is not loaded.");
        }
        if (!applyTypeBasePlusBonus(tame, data)) {
            return error(source.getPlayer(), "Could not resolve base stats template for this tame type.");
        }

        TameRegistry.markDirty();
        source.sendSuccess(() -> Component.literal("Normalized bonuses for " + data.name + "."), true);
        return 1;
    }

    private static int adminRegistryRemove(CommandSourceStack source, String name) {
        ServerPlayer p = source.getPlayer();
        String wanted = name == null ? "" : name.trim();
        if (wanted.isBlank()) return error(p, "Name cannot be blank.");

        List<UUID> matches = new ArrayList<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (d == null || d.name == null) continue;
            if (d.name.equalsIgnoreCase(wanted)) {
                matches.add(d.uuid);
            }
        }
        if (matches.isEmpty()) {
            return error(p, "No registry entries found with that name.");
        }

        int removed = 0;
        int skippedLoaded = 0;
        for (UUID uuid : matches) {
            if (findLoadedTameByUuid(source, uuid) != null) {
                skippedLoaded++;
                continue;
            }
            if (TameRegistry.TAMES.remove(uuid) != null) {
                removed++;
            }
        }

        if (removed > 0) {
            TameRegistry.markDirty();
        }

        p.sendSystemMessage(Component.literal(
                "Registry remove \"" + wanted + "\": removed " + removed + ", skipped loaded " + skippedLoaded + "."
        ).withStyle(ChatFormatting.YELLOW));
        return removed > 0 ? 1 : 0;
    }

    private static int adminImportTlData(CommandSourceStack source, String fileHint) {
        MinecraftServer server = source.getServer();
        ServerPlayer p = source.getPlayer();
        if (server == null || p == null) {
            return 0;
        }

        Path dataDir = server.getWorldPath(LevelResource.ROOT).resolve("data");
        Path file = resolveTlImportFile(dataDir, fileHint);
        if (file == null || !Files.exists(file)) {
            return error(p, "TL import file not found in world/data. Tried: " + tlImportCandidates(fileHint));
        }

        CompoundTag root;
        try {
            root = NbtIo.readCompressed(file.toFile());
        } catch (IOException e) {
            return error(p, "Failed to read TL import file: " + e.getMessage());
        }
        if (root == null || root.isEmpty()) {
            return error(p, "TL import file is empty: " + file.getFileName());
        }

        CompoundTag dataTag = root.contains("data", Tag.TAG_COMPOUND) ? root.getCompound("data") : root;

        int imported = 0;
        int merged = 0;
        int skipped = 0;
        int parseErrors = 0;
        int lastDeathsImported = 0;
        int deathHistoryImported = 0;
        int approvedItemsImported = 0;

        if (dataTag.contains("tames", Tag.TAG_LIST)) {
            for (Tag tag : dataTag.getList("tames", Tag.TAG_COMPOUND)) {
                if (!(tag instanceof CompoundTag row)) {
                    skipped++;
                    continue;
                }
                try {
                    TameData incoming = TameData.fromTag(row);
                    if (incoming.uuid == null) {
                        skipped++;
                        continue;
                    }
                    TameData existing = incoming.tlId != null ? TameRegistry.getByTlId(incoming.tlId) : null;
                    if (existing == null) {
                        existing = TameRegistry.get(incoming.uuid);
                    }
                    if (existing == null) {
                        TameRegistry.register(incoming);
                        imported++;
                    } else {
                        mergeDuplicateIntoKeeper(existing, incoming);
                        TameRegistry.markDirty();
                        merged++;
                    }
                } catch (Throwable t) {
                    parseErrors++;
                }
            }
        } else if (dataTag.contains("entries", Tag.TAG_LIST)) {
            // Accept migration-store format as fallback.
            for (Tag tag : dataTag.getList("entries", Tag.TAG_COMPOUND)) {
                if (!(tag instanceof CompoundTag row)) {
                    skipped++;
                    continue;
                }
                if (!row.hasUUID("uuid") || !row.contains("payload", Tag.TAG_COMPOUND)) {
                    skipped++;
                    continue;
                }
                try {
                    TameData incoming = TameData.fromTag(row.getCompound("payload"));
                    if (incoming.uuid == null) {
                        skipped++;
                        continue;
                    }
                    TameData existing = incoming.tlId != null ? TameRegistry.getByTlId(incoming.tlId) : null;
                    if (existing == null) {
                        existing = TameRegistry.get(incoming.uuid);
                    }
                    if (existing == null) {
                        TameRegistry.register(incoming);
                        imported++;
                    } else {
                        mergeDuplicateIntoKeeper(existing, incoming);
                        TameRegistry.markDirty();
                        merged++;
                    }
                } catch (Throwable t) {
                    parseErrors++;
                }
            }
        } else {
            return error(p, "No supported TL data section found (expected 'tames' or 'entries').");
        }

        if (dataTag.contains("lastDeaths", Tag.TAG_LIST)) {
            for (Tag tag : dataTag.getList("lastDeaths", Tag.TAG_COMPOUND)) {
                if (!(tag instanceof CompoundTag row)) continue;
                try {
                    TameDeathRecord record = TameDeathRecord.fromTag(row);
                    if (record.uuid == null) continue;
                    TameDeathRecord existing = null;
                    if (record.tlId != null) {
                        for (TameDeathRecord candidate : TameRegistry.LAST_DEATHS.values()) {
                            if (candidate != null && record.tlId.equals(candidate.tlId)) {
                                existing = candidate;
                                break;
                            }
                        }
                    }
                    if (existing == null) {
                        existing = TameRegistry.LAST_DEATHS.get(record.uuid);
                    }
                    if (existing == null || record.deathUnixMillis >= existing.deathUnixMillis) {
                        if (existing != null) {
                            TameRegistry.removeLastDeath(existing);
                        }
                        TameRegistry.LAST_DEATHS.put(record.uuid, record);
                        lastDeathsImported++;
                    }
                } catch (Throwable ignored) {
                    parseErrors++;
                }
            }
        }

        if (dataTag.contains("deathHistory", Tag.TAG_LIST)) {
            for (Tag tag : dataTag.getList("deathHistory", Tag.TAG_COMPOUND)) {
                if (!(tag instanceof CompoundTag row)) continue;
                try {
                    TameDeathRecord record = TameDeathRecord.fromTag(row);
                    if (!hasDeathRecord(TameRegistry.DEATH_HISTORY, record)) {
                        TameRegistry.DEATH_HISTORY.add(record);
                        deathHistoryImported++;
                    }
                } catch (Throwable ignored) {
                    parseErrors++;
                }
            }
        }

        if (dataTag.contains("approvedReincarnateItems", Tag.TAG_LIST)) {
            for (Tag tag : dataTag.getList("approvedReincarnateItems", Tag.TAG_STRING)) {
                String itemId = tag.getAsString();
                if (itemId == null || itemId.isBlank()) continue;
                if (TameRegistry.APPROVED_REINCARNATE_ITEMS.add(itemId.trim().toLowerCase(Locale.ROOT))) {
                    approvedItemsImported++;
                }
            }
        }

        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal(
                "TL import (" + file.getFileName() + "): imported " + imported
                        + ", merged " + merged
                        + ", skipped " + skipped
                        + ", parseErrors " + parseErrors
                        + ", lastDeaths " + lastDeathsImported
                        + ", deathHistory " + deathHistoryImported
                        + ", approvedItems " + approvedItemsImported + "."
        ).withStyle(ChatFormatting.GOLD));
        return (imported + merged) > 0 ? 1 : 0;
    }

    private static Path resolveTlImportFile(Path dataDir, String fileHint) {
        if (dataDir == null) return null;
        String hint = fileHint == null ? "" : fileHint.trim();
        if (!hint.isBlank()) {
            if (hint.endsWith(".dat")) {
                return dataDir.resolve(hint);
            }
            Path withDat = dataDir.resolve(hint + ".dat");
            if (Files.exists(withDat)) return withDat;
            return dataDir.resolve(hint);
        }
        for (String name : tlImportCandidates("")) {
            Path candidate = dataDir.resolve(name);
            if (Files.exists(candidate)) return candidate;
        }
        return null;
    }

    private static List<String> tlImportCandidates(String ignored) {
        List<String> list = new ArrayList<>();
        list.add("tameslevel_registry.dat");
        list.add("tameslevel.dat");
        list.add("domesticationinnovation_tameslevel_migration.dat");
        return list;
    }

    private static boolean hasDeathRecord(List<TameDeathRecord> records, TameDeathRecord target) {
        if (records == null || target == null) return false;
        for (TameDeathRecord row : records) {
            if (row == null) continue;
            if (!Objects.equals(row.uuid, target.uuid)) continue;
            if (row.deathGameTime != target.deathGameTime) continue;
            if (row.deathUnixMillis != target.deathUnixMillis) continue;
            return true;
        }
        return false;
    }

    private static boolean isEligibleForMorningLanternRecall(MinecraftServer server, TameData data) {
        if (server == null || data == null || data.dead || data.ownerUUID == null || data.uuid == null) {
            return false;
        }
        if (data.lastKnownDimension == null || data.lastKnownDimension.isBlank()) {
            return false;
        }
        if (data.hasProtectionZone) {
            return false;
        }
        if (data.tlId != null && PENDING_MORNING_LANTERN.containsKey(data.tlId)) {
            return false;
        }
        if (findLoadedTameByIdentity(server, data.uuid, data.tlId) != null) {
            return false;
        }
        if (isCarriedByOwner(server, data)) {
            return false;
        }
        return shouldLanternRecallFromSnapshot(data.entitySnapshot);
    }

    private static boolean shouldLanternRecallFromSnapshot(CompoundTag snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return true;
        }
        if (DomesticationMod.CONFIG.trinaryCommandSystem.get()) {
            Integer command = findSnapshotCommand(snapshot);
            if (command != null) {
                return command == 2;
            }
        } else {
            Integer command = findSnapshotCommand(snapshot);
            if (command != null) {
                return command == 1;
            }
            if (snapshot.contains("Sitting", Tag.TAG_BYTE)) {
                return !snapshot.getBoolean("Sitting");
            }
            if (snapshot.contains("orderedToSit", Tag.TAG_BYTE)) {
                return !snapshot.getBoolean("orderedToSit");
            }
            if (snapshot.contains("OrderedToSit", Tag.TAG_BYTE)) {
                return !snapshot.getBoolean("OrderedToSit");
            }
        }
        return true;
    }

    private static Integer findSnapshotCommand(CompoundTag snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return null;
        }
        for (String key : snapshot.getAllKeys()) {
            if (!key.endsWith("Command")) {
                continue;
            }
            if (snapshot.contains(key, Tag.TAG_BYTE) || snapshot.contains(key, Tag.TAG_INT)) {
                return snapshot.getInt(key);
            }
        }
        return null;
    }

    private static BlockPos findNearestWaywardLantern(ServerPlayer owner) {
        if (owner == null) {
            return null;
        }
        ServerLevel level = owner.serverLevel();
        BlockPos ownerPos = owner.blockPosition();
        int chunkRadius = Math.max(1, MORNING_LANTERN_RADIUS / 16);
        double bestDistance = (double) MORNING_LANTERN_RADIUS * (double) MORNING_LANTERN_RADIUS;
        BlockPos best = null;
        int centerChunkX = ownerPos.getX() >> 4;
        int centerChunkZ = ownerPos.getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(centerChunkX + dx, centerChunkZ + dz);
                if (chunk == null) {
                    continue;
                }
                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (!(blockEntity instanceof com.github.alexthe668.domesticationinnovation.server.block.WaywardLanternBlockEntity)) {
                        continue;
                    }
                    double dist = pos.distSqr(ownerPos);
                    if (dist > bestDistance) {
                        continue;
                    }
                    bestDistance = dist;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }

    private static BlockPos findLanternPlacement(ServerLevel level, BlockPos lanternPos, Entity entity) {
        if (level == null || lanternPos == null || entity == null) {
            return lanternPos == null ? BlockPos.ZERO : lanternPos.above();
        }
        int maxDist = (int) Math.max(entity.getBbWidth() + 1, 10);
        for (int i = 0; i < 10; i++) {
            BlockPos at = lanternPos.offset(level.random.nextInt(maxDist) - maxDist / 2, 1, level.random.nextInt(maxDist) - maxDist / 2);
            while (level.getBlockState(at).isAir() && at.getY() > level.getMinBuildHeight() && level.noCollision(entity.getType().getAABB(at.getX() + 0.5F, at.getY() - 1, at.getZ() + 0.5F))) {
                at = at.below();
            }
            if (level.noCollision(entity.getType().getAABB(at.getX() + 0.5F, at.getY(), at.getZ() + 0.5F))) {
                return at;
            }
            if (entity.isInWall()) {
                return lanternPos.above();
            }
        }
        return lanternPos.above();
    }

    private static TamableAnimal findLoadedTameByIdentity(MinecraftServer server, UUID tameUuid, UUID tlId) {
        if (server == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            TamableAnimal found = findLoadedTameByIdentity(level, tameUuid, tlId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static TamableAnimal findLoadedTameByIdentity(ServerLevel level, UUID tameUuid, UUID tlId) {
        if (level == null) {
            return null;
        }
        if (tlId != null) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal tame) || !tame.isTame()) {
                    continue;
                }
                if (tlId.equals(TameData.getTlId(tame))) {
                    return tame;
                }
            }
        }
        if (tameUuid != null) {
            Entity entity = level.getEntity(tameUuid);
            if (entity instanceof TamableAnimal tame && tame.isTame()) {
                return tame;
            }
        }
        return null;
    }

    private static TamableAnimal findLoadedTameForRequest(ServerLevel level, LanternRequest request) {
        if (level == null || request == null) {
            return null;
        }
        return findLoadedTameByIdentity(level, request.getPetUUID(), request.getTlId());
    }

    private static int adminMigrateToDI(CommandSourceStack source, boolean dryRun) {
        MinecraftServer server = source.getServer();
        if (server == null) {
            source.sendFailure(Component.literal("Server unavailable."));
            return 0;
        }
        TLMigrationImportData migrationData = TLMigrationImportData.get(source.getLevel());
        if (migrationData == null) {
            source.sendFailure(Component.literal("Migration store unavailable."));
            return 0;
        }
        long now = server.overworld().getGameTime();

        int total = 0;
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int skipped = 0;

        for (TameData tameData : TameRegistry.TAMES.values()) {
            if (tameData == null || tameData.uuid == null) {
                skipped++;
                continue;
            }
            total++;
            CompoundTag payload = buildDIMigrationPayload(tameData, now);
            boolean hasEntry = migrationData.hasEntry(tameData.uuid);
            boolean same = migrationData.hasSamePayload(tameData.uuid, payload);

            if (!hasEntry) {
                created++;
                if (!dryRun) {
                    migrationData.upsert(tameData.uuid, payload);
                }
                continue;
            }

            if (same) {
                unchanged++;
                continue;
            }

            updated++;
            if (!dryRun) {
                migrationData.upsert(tameData.uuid, payload);
            }
        }

        final int fTotal = total;
        final int fCreated = created;
        final int fUpdated = updated;
        final int fUnchanged = unchanged;
        final int fSkipped = skipped;
        final int fStoreSize = dryRun ? migrationData.size() : migrationData.size();
        String mode = dryRun ? "dryrun" : "apply";
        source.sendSuccess(
                () -> Component.literal(
                        "DI migration (" + mode + "): scanned " + fTotal
                                + ", create " + fCreated
                                + ", update " + fUpdated
                                + ", unchanged " + fUnchanged
                                + ", skipped " + fSkipped
                                + ", store size " + fStoreSize
                                + ". Rerun is safe; entries are overwritten."
                ).withStyle(ChatFormatting.AQUA),
                true
        );
        return fTotal > 0 ? 1 : 0;
    }

    private static int showTlMigrationStatus(CommandSourceStack source) {
        TLMigrationImportData migrationData = TLMigrationImportData.get(source.getLevel());
        int migrationEntries = migrationData == null ? 0 : migrationData.size();
        DITameProgressData progressData = DITameProgressData.get(source.getLevel());
        int progressEntries = progressData == null ? 0 : progressData.size();
        source.sendSuccess(() -> Component.literal(
                "[DI] TL migration status: migrationEntries=" + migrationEntries
                        + ", progressEntries=" + progressEntries
                        + ", lastMode=" + tlMigrationLastMode
                        + ", scanned=" + tlMigrationLastScanned
                        + ", matchedPayload=" + tlMigrationLastMatchedPayload
                        + ", missingPayload=" + tlMigrationLastMissingPayload
                        + ", appliedNew=" + tlMigrationLastAppliedNew
                        + ", appliedUpdate=" + tlMigrationLastAppliedUpdate
                        + ", skippedStale=" + tlMigrationLastSkippedStale
        ), false);
        return 1;
    }

    private static int applyTlMigrationToLoaded(CommandSourceStack source, boolean dryRun) {
        MinecraftServer server = source.getServer();
        TLMigrationImportData data = TLMigrationImportData.get(source.getLevel());
        if (data == null) {
            source.sendFailure(Component.literal("[DI] TL migration data store unavailable."));
            return 0;
        }

        long scanned = 0L;
        long matchedPayload = 0L;
        long missingPayload = 0L;
        long appliedNew = 0L;
        long appliedUpdate = 0L;
        long skippedStale = 0L;

        for (ServerLevel serverLevel : server.getAllLevels()) {
            for (Entity entity : serverLevel.getAllEntities()) {
                if (!(entity instanceof LivingEntity living) || !TameableUtils.couldBeTamed(living) || !TameableUtils.isTamed(living)) {
                    continue;
                }
                scanned++;
                CompoundTag payload = data.getPayload(living.getUUID());
                if (payload == null || payload.isEmpty()) {
                    missingPayload++;
                    continue;
                }
                payload = normalizeDIMigrationPayload(payload);
                matchedPayload++;
                CompoundTag current = TameableUtils.getDIProgressData(living);
                if (current == null || current.isEmpty()) {
                    if (!dryRun) {
                        TameableUtils.setDIProgressData(living, payload);
                    }
                    appliedNew++;
                    continue;
                }
                if (shouldReplaceMigrationPayload(current, payload)) {
                    if (!dryRun) {
                        TameableUtils.setDIProgressData(living, payload);
                    }
                    appliedUpdate++;
                } else {
                    skippedStale++;
                }
            }
        }

        tlMigrationLastMode = dryRun ? "dryrun" : "apply";
        tlMigrationLastScanned = scanned;
        tlMigrationLastMatchedPayload = matchedPayload;
        tlMigrationLastMissingPayload = missingPayload;
        tlMigrationLastAppliedNew = appliedNew;
        tlMigrationLastAppliedUpdate = appliedUpdate;
        tlMigrationLastSkippedStale = skippedStale;

        final long fScanned = scanned;
        final long fMatchedPayload = matchedPayload;
        final long fMissingPayload = missingPayload;
        final long fAppliedNew = appliedNew;
        final long fAppliedUpdate = appliedUpdate;
        final long fSkippedStale = skippedStale;
        source.sendSuccess(() -> Component.literal(
                "[DI] TL migration " + tlMigrationLastMode + ": scanned=" + fScanned
                        + ", matchedPayload=" + fMatchedPayload
                        + ", missingPayload=" + fMissingPayload
                        + ", appliedNew=" + fAppliedNew
                        + ", appliedUpdate=" + fAppliedUpdate
                        + ", skippedStale=" + fSkippedStale
        ), true);
        return 1;
    }

    private static boolean shouldReplaceMigrationPayload(CompoundTag current, CompoundTag incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return false;
        }
        if (current == null || current.isEmpty()) {
            return true;
        }
        long currentExport = getExportedAtGameTime(current);
        long incomingExport = getExportedAtGameTime(incoming);
        if (incomingExport > currentExport) {
            return true;
        }
        if (incomingExport < currentExport) {
            return false;
        }
        return !incoming.equals(current);
    }

    private static long getExportedAtGameTime(CompoundTag payload) {
        if (payload != null && payload.contains("exportedAtGameTime", Tag.TAG_LONG)) {
            return payload.getLong("exportedAtGameTime");
        }
        return Long.MIN_VALUE;
    }

    private static CompoundTag buildDIMigrationPayload(TameData tameData, long exportedAtGameTime) {
        CompoundTag payload = tameData.toTag();
        payload.putString("sourceModId", "tameslevel");
        payload.putInt("schemaVersion", 1);
        payload.putLong("exportedAtGameTime", exportedAtGameTime);
        payload.putLong("exportedAtEpochMillis", System.currentTimeMillis());
        return normalizeDIMigrationPayload(payload);
    }

    private static CompoundTag normalizeDIMigrationPayload(CompoundTag payload) {
        if (payload == null || payload.isEmpty()) {
            return payload;
        }
        CompoundTag normalized = payload.copy();

        // One-time migration for HP bonus scale change (legacy +1 HP bonus -> new +2 HP bonus).
        if (!normalized.getBoolean("diMigrationNormalizedV2")) {
            if (normalized.contains("bonusHealth", Tag.TAG_DOUBLE)) {
                normalized.putDouble("bonusHealth", normalized.getDouble("bonusHealth") * 2.0D);
            }
            if (normalized.contains("savedBonusHealth", Tag.TAG_DOUBLE)) {
                normalized.putDouble("savedBonusHealth", normalized.getDouble("savedBonusHealth") * 2.0D);
            }
            normalized.putBoolean("diMigrationNormalizedV2", true);
        }

        int level = Math.max(1, normalized.getInt("level"));
        ensureGuaranteedMilestoneAttributes(normalized, "attributeLevels", level);

        int savedLevel = Math.max(1, normalized.getInt("savedLevel"));
        ensureGuaranteedMilestoneAttributes(normalized, "savedAttributeLevels", savedLevel);
        return normalized;
    }

    private static void ensureGuaranteedMilestoneAttributes(CompoundTag payload, String tagKey, int level) {
        if (payload == null || tagKey == null || tagKey.isBlank()) return;
        CompoundTag attributes = payload.contains(tagKey, Tag.TAG_COMPOUND)
                ? payload.getCompound(tagKey).copy()
                : new CompoundTag();
        if (level >= 10) {
            attributes.putInt("tethered_teleport", Math.max(1, attributes.getInt("tethered_teleport")));
        }
        if (level >= 30) {
            attributes.putInt("gluttonous", Math.max(1, attributes.getInt("gluttonous")));
        }
        payload.put(tagKey, attributes);
    }

    private static void resetProgressData(TameData data, boolean clearClass) {
        data.level = 1;
        data.xp = 0;
        data.xpToNext = LevelSystem.xpRequiredForLevel(data.level);
        data.kills = 0;
        data.assists = 0;
        data.deaths = 0;
        data.abilities.clear();
        data.abilityLevels.clear();
        data.attributeLevels.clear();
        data.cooldowns.clear();
        data.bonusHealth = 0;
        data.bonusDamage = 0;
        data.bonusSpeed = 0;
        data.bonusArmor = 0;
        data.bonusArmorToughness = 0;
        data.bonusKnockback = 0;
        data.bonusKnockbackResist = 0;

        data.hasSavedProgress = false;
        data.savedProgressCost = 0;
        data.savedLevel = 1;
        data.savedXp = 0;
        data.savedXpToNext = LevelSystem.xpRequiredForLevel(1);
        data.savedKills = 0;
        data.savedAssists = 0;
        data.savedBonusHealth = 0;
        data.savedBonusDamage = 0;
        data.savedBonusSpeed = 0;
        data.savedBonusArmor = 0;
        data.savedBonusArmorToughness = 0;
        data.savedBonusKnockback = 0;
        data.savedBonusKnockbackResist = 0;
        data.savedAbilities.clear();
        data.savedAbilityLevels.clear();
        data.savedAttributeLevels.clear();

        if (clearClass) {
            data.tameClass = null;
        }
    }

    private static void setTamableBaseCombatStats(TamableAnimal tame) {
        setAttributeToDefault(tame, Attributes.MAX_HEALTH);
        setAttributeToDefault(tame, Attributes.ATTACK_DAMAGE);
        setAttributeToDefault(tame, Attributes.MOVEMENT_SPEED);
        setAttributeToDefault(tame, Attributes.ARMOR);
        setAttributeToDefault(tame, Attributes.ARMOR_TOUGHNESS);
        setAttributeToDefault(tame, Attributes.ATTACK_KNOCKBACK);
        setAttributeToDefault(tame, Attributes.KNOCKBACK_RESISTANCE);
    }

    private static boolean applyTypeBasePlusBonus(TamableAnimal tame, TameData data) {
        if (tame == null || data == null || !(tame.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Entity spawned = tame.getType().create(serverLevel);
        if (!(spawned instanceof TamableAnimal template)) {
            return false;
        }
        prepareTemplateAsTamed(template, tame, data);

        scrubLegacyManagedModifiers(tame);
        setAttributeBaseValue(tame, Attributes.MAX_HEALTH, readBaseOrDefault(template, Attributes.MAX_HEALTH) + data.bonusHealth);
        setAttributeBaseValue(tame, Attributes.ATTACK_DAMAGE, readBaseOrDefault(template, Attributes.ATTACK_DAMAGE) + data.bonusDamage);
        setAttributeBaseValue(tame, Attributes.MOVEMENT_SPEED, readBaseOrDefault(template, Attributes.MOVEMENT_SPEED) + data.bonusSpeed);
        setAttributeBaseValue(tame, Attributes.ARMOR, readBaseOrDefault(template, Attributes.ARMOR) + data.bonusArmor);
        setAttributeBaseValue(tame, Attributes.ARMOR_TOUGHNESS, readBaseOrDefault(template, Attributes.ARMOR_TOUGHNESS) + data.bonusArmorToughness);
        setAttributeBaseValue(tame, Attributes.ATTACK_KNOCKBACK, clampAttributeBaseValue(Attributes.ATTACK_KNOCKBACK, readBaseOrDefault(template, Attributes.ATTACK_KNOCKBACK) + data.bonusKnockback));
        setAttributeBaseValue(tame, Attributes.KNOCKBACK_RESISTANCE, clampAttributeBaseValue(Attributes.KNOCKBACK_RESISTANCE, readBaseOrDefault(template, Attributes.KNOCKBACK_RESISTANCE) + data.bonusKnockbackResist));

        LevelSystem.updateTameName(tame, data);
        tame.setHealth(tame.getMaxHealth());
        return true;
    }

    private static void prepareTemplateAsTamed(TamableAnimal template, TamableAnimal liveTame, TameData data) {
        if (template == null) return;
        template.setTame(true);
        UUID owner = liveTame.getOwnerUUID();
        if (owner == null && data != null) {
            owner = data.ownerUUID;
        }
        if (owner != null) {
            template.setOwnerUUID(owner);
        }
    }

    private static void enforceTamedOwnerPreserveCollar(TamableAnimal tame, UUID ownerId) {
        if (tame == null) return;
        DyeColor collar = null;
        if (tame instanceof Wolf wolf) {
            collar = wolf.getCollarColor();
        }
        if (ownerId == null && !tame.isTame()) {
            tame.setTame(true);
        }
        if (ownerId != null && !ownerId.equals(tame.getOwnerUUID())) {
            tame.setOwnerUUID(ownerId);
        }
        if (collar != null && tame instanceof Wolf wolf) {
            wolf.setCollarColor(collar);
        }
    }

    private static double readBaseOrDefault(TamableAnimal tame, Attribute attribute) {
        if (tame == null || attribute == null) return 0.0D;
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) return attribute.getDefaultValue();
        return instance.getBaseValue();
    }

    private static void setAttributeToValue(TamableAnimal tame, Attribute attribute, double value) {
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) return;
        for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
            instance.removeModifier(modifier);
        }
        instance.setBaseValue(value);
    }

    private static void setAttributeBaseValue(TamableAnimal tame, Attribute attribute, double value) {
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) return;
        instance.setBaseValue(clampAttributeBaseValue(attribute, value));
    }

    private static void scrubLegacyManagedModifiers(TamableAnimal tame) {
        scrubUnknownModifiers(tame, Attributes.MAX_HEALTH);
        scrubUnknownModifiers(tame, Attributes.ATTACK_DAMAGE);
        scrubUnknownModifiers(tame, Attributes.MOVEMENT_SPEED);
        scrubUnknownModifiers(tame, Attributes.ARMOR, COLLAR_ARMOR_UUID);
        scrubUnknownModifiers(tame, Attributes.ARMOR_TOUGHNESS, COLLAR_ARMOR_TOUGHNESS_UUID);
        scrubUnknownModifiers(tame, Attributes.ATTACK_KNOCKBACK);
        scrubUnknownModifiers(tame, Attributes.KNOCKBACK_RESISTANCE);
    }

    private static void scrubUnknownModifiers(TamableAnimal tame, Attribute attribute, UUID... preservedModifierIds) {
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) return;
        Set<UUID> preserved = preservedModifierIds.length == 0
                ? Set.of()
                : new HashSet<>(List.of(preservedModifierIds));
        for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
            if (!preserved.contains(modifier.getId())) {
                instance.removeModifier(modifier);
            }
        }
    }

    private static double clampAttributeBaseValue(Attribute attribute, double value) {
        if (attribute == Attributes.ATTACK_KNOCKBACK) {
            return Mth.clamp(value, 0.0D, 2.0D);
        }
        return value;
    }

    private static void setAttributeToDefault(TamableAnimal tame, Attribute attribute) {
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) return;
        // Remove all modifiers so legacy scripted bonuses are fully cleared.
        for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
            instance.removeModifier(modifier);
        }
        instance.setBaseValue(attribute.getDefaultValue());
    }

    private static List<TameData> findAliveTamesByName(String name) {
        List<TameData> matches = new ArrayList<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (isDeadEntry(d.uuid)) continue;
            if (!d.name.equalsIgnoreCase(name)) continue;
            matches.add(d);
        }
        return matches;
    }

    private static List<TameData> ownedAllTamesSorted(UUID owner) {
        List<TameData> list = new ArrayList<>();
        if (owner == null) {
            return list;
        }
        for (TameData d : TameRegistry.TAMES.values()) {
            if (d == null || d.uuid == null || d.ownerUUID == null) continue;
            if (!owner.equals(d.ownerUUID)) continue;
            list.add(d);
        }
        list.sort(Comparator.comparingInt((TameData d) -> d.level).reversed().thenComparing(d -> d.name == null ? "" : d.name.toLowerCase(Locale.ROOT)));
        return list;
    }

    private static List<TameData> findDeadRegistryRowsByName(String name) {
        List<TameData> matches = new ArrayList<>();
        if (name == null || name.isBlank()) {
            return matches;
        }
        for (TameData d : TameRegistry.TAMES.values()) {
            if (d == null || d.uuid == null || d.name == null) continue;
            if (!isDeadEntry(d.uuid)) continue;
            if (!d.name.equalsIgnoreCase(name)) continue;
            if (d.entitySnapshot == null || d.entitySnapshot.isEmpty()) continue;
            matches.add(d);
        }
        matches.sort((a, b) -> {
            if (a.deadUnixMillis != b.deadUnixMillis) return Long.compare(b.deadUnixMillis, a.deadUnixMillis);
            if (a.deadGameTime != b.deadGameTime) return Long.compare(b.deadGameTime, a.deadGameTime);
            return Integer.compare(b.level, a.level);
        });
        return matches;
    }

    private static TameData resolveAdminAliveTame(ServerPlayer executor, String name) {
        if (executor == null || name == null || name.isBlank()) {
            return null;
        }
        List<TameData> matches = findAliveTamesByName(name);
        if (matches.isEmpty()) {
            error(executor, "Pet not found.");
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        error(executor, "Ambiguous pet name (" + matches.size() + " matches). Rename duplicates first.");
        return null;
    }

    private static List<TameDeathRecord> findDeadRespawnRecordsByName(String name) {
        List<TameDeathRecord> matches = new ArrayList<>();
        for (TameDeathRecord record : TameRegistry.LAST_DEATHS.values()) {
            if (record == null) continue;
            if (record.name == null || !record.name.equalsIgnoreCase(name)) continue;
            if (record.snapshot == null || record.snapshot.isEmpty()) continue;
            matches.add(record);
        }
        matches.sort((a, b) -> Long.compare(b.deathGameTime, a.deathGameTime));
        return matches;
    }

    private static int notImplemented(CommandSourceStack source, String message) { source.getPlayer().sendSystemMessage(Component.literal(message)); return 0; }
    private static int error(ServerPlayer p, String message) { p.sendSystemMessage(Component.literal(message)); return 0; }
    private static String fmt(double v) { return Math.floor(v) == v ? Integer.toString((int) v) : String.format(Locale.ROOT, "%.2f", v); }
    private static String formatLevelsCompact(Map<String, Integer> levels) {
        if (levels == null || levels.isEmpty()) return "none";
        List<String> keys = new ArrayList<>(levels.keySet());
        keys.sort(String::compareToIgnoreCase);
        List<String> parts = new ArrayList<>();
        for (String key : keys) {
            parts.add(key + "[" + levels.getOrDefault(key, 0) + "]");
        }
        return String.join(", ", parts);
    }

    private static String heldItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return null;
        return key.toString().toLowerCase(Locale.ROOT);
    }

    private static String movementLabel(MovementOrder order) {
        return switch (order) {
            case FOLLOW -> "follow";
            case SIT -> "sit";
            case WANDER -> "wander";
        };
    }

    private static MovementOrder parseMovementOrder(String raw) {
        if (raw == null) return null;
        String key = raw.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "follow" -> MovementOrder.FOLLOW;
            case "sit" -> MovementOrder.SIT;
            case "wander" -> MovementOrder.WANDER;
            default -> null;
        };
    }

    private static boolean matchesMovementOrder(TamableAnimal tame, MovementOrder order) {
        if (tame instanceof IComandableMob commandable) {
            int command = commandable.getCommand();
            return switch (order) {
                case FOLLOW -> command == 2;
                case SIT -> command == 1 || tame.isOrderedToSit();
                case WANDER -> command == 0;
            };
        }
        return switch (order) {
            case FOLLOW -> !tame.isOrderedToSit();
            case SIT -> tame.isOrderedToSit();
            case WANDER -> false;
        };
    }

    private static List<TamableAnimal> loadedOwnedStateTames(CommandSourceStack source, UUID owner, MovementOrder order) {
        List<TamableAnimal> list = new ArrayList<>();
        for (TameData data : ownedTames(owner)) {
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, owner, data.uuid);
            if (tame == null || !tame.isAlive()) continue;
            if (!matchesMovementOrder(tame, order)) continue;
            list.add(tame);
        }
        return list;
    }

    private static void applySitFollowOverride(TamableAnimal tame, boolean sit) {
        applyMovementOverride(tame, sit ? MovementOrder.SIT : MovementOrder.FOLLOW);
    }

    private static void applyMovementOverride(TamableAnimal tame, MovementOrder order) {
        if (tame == null) return;
        boolean sit = order == MovementOrder.SIT;
        tame.setOrderedToSit(sit);
        if (sit || order == MovementOrder.WANDER) {
            tame.setTarget(null);
            tame.getNavigation().stop();
        } else {
            tame.getNavigation().stop();
        }
        // Best-effort compatibility with Domesticated Innovation wandering/order state.
        clearExternalWanderingState(tame, order);
    }

    private static void clearExternalWanderingState(TamableAnimal tame, MovementOrder order) {
        boolean sit = order == MovementOrder.SIT;
        boolean follow = order == MovementOrder.FOLLOW;
        boolean wander = order == MovementOrder.WANDER;
        tryInvokeBooleanSetter(tame, "setWandering", wander);
        tryInvokeBooleanSetter(tame, "setWander", wander);
        tryInvokeBooleanSetter(tame, "setDrumWandering", wander);
        tryInvokeBooleanSetter(tame, "setCommandWander", wander);
        tryInvokeBooleanSetter(tame, "setFollowing", follow);
        tryInvokeBooleanSetter(tame, "setFollow", follow);
        tryInvokeBooleanSetter(tame, "setSitting", sit);
        tryInvokeBooleanSetter(tame, "setSit", sit);
        // Common int-based command APIs used by tame mods.
        tryInvokeIntSetter(tame, "setCommand", preferredCommandInt(order));
        tryInvokeIntSetter(tame, "setPetCommand", preferredCommandInt(order));
        tryInvokeIntSetter(tame, "setOrder", preferredCommandInt(order));
        tryInvokeIntSetter(tame, "setMode", preferredCommandInt(order));
        // Try DI helper class variants (if present at runtime).
        tryInvokeStaticHelper("com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils", tame, order);
        tryInvokeStaticHelper("com.github.alexthe668.domesticationinnovation.server.misc.TameableUtils", tame, order);
    }

    private static void tryInvokeBooleanSetter(TamableAnimal tame, String methodName, boolean value) {
        try {
            Method m = tame.getClass().getMethod(methodName, boolean.class);
            m.setAccessible(true);
            m.invoke(tame, value);
        } catch (Throwable ignored) {
        }
    }

    private static void tryInvokeIntSetter(TamableAnimal tame, String methodName, int value) {
        try {
            Method m = tame.getClass().getMethod(methodName, int.class);
            m.setAccessible(true);
            m.invoke(tame, value);
        } catch (Throwable ignored) {
        }
        try {
            Method m = tame.getClass().getMethod(methodName, Integer.class);
            m.setAccessible(true);
            m.invoke(tame, Integer.valueOf(value));
        } catch (Throwable ignored) {
        }
    }

    private static int preferredCommandInt(MovementOrder order) {
        return switch (order) {
            case WANDER -> 0;
            case SIT -> 1;
            case FOLLOW -> 2;
        };
    }

    private static void tryInvokeStaticHelper(String helperClassName, TamableAnimal tame, MovementOrder order) {
        try {
            Class<?> helper = Class.forName(helperClassName);
            for (Method method : helper.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) continue;
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 2) continue;
                if (!params[0].isAssignableFrom(tame.getClass())) continue;

                String name = method.getName().toLowerCase(Locale.ROOT);
                Class<?> arg1 = params[1];

                if (arg1 == boolean.class && name.contains("wander")) {
                    method.invoke(null, tame, order == MovementOrder.WANDER);
                    continue;
                }
                if ((arg1 == boolean.class || arg1 == Boolean.class) && name.contains("follow")) {
                    method.invoke(null, tame, order == MovementOrder.FOLLOW);
                    continue;
                }
                if ((arg1 == boolean.class || arg1 == Boolean.class) && name.contains("sit")) {
                    method.invoke(null, tame, order == MovementOrder.SIT);
                    continue;
                }
                if ((arg1 == int.class || arg1 == Integer.class) && (name.contains("command") || name.contains("order") || name.contains("mode"))) {
                    for (int candidate : commandCandidates(order)) {
                        try {
                            method.invoke(null, tame, candidate);
                            break;
                        } catch (Throwable ignored) {
                        }
                    }
                    continue;
                }
                if (arg1.isEnum() && (name.contains("command") || name.contains("order") || name.contains("mode"))) {
                    Object chosen = pickEnumConstant(arg1, order);
                    if (chosen != null) {
                        method.invoke(null, tame, chosen);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object pickEnumConstant(Class<?> enumType, MovementOrder order) {
        try {
            Object[] constants = enumType.getEnumConstants();
            if (constants == null || constants.length == 0) return null;
            String[] preferred = switch (order) {
                case SIT -> new String[]{"sit", "stay", "stop"};
                case FOLLOW -> new String[]{"follow", "escort"};
                case WANDER -> new String[]{"wander", "roam", "free"};
            };
            for (String token : preferred) {
                for (Object constant : constants) {
                    if (constant.toString().toLowerCase(Locale.ROOT).contains(token)) {
                        return constant;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static int[] commandCandidates(MovementOrder order) {
        return switch (order) {
            case WANDER -> new int[]{0, 2, 1, 3};
            case SIT -> new int[]{1, 2, 0, 3};
            case FOLLOW -> new int[]{2, 1, 0, 3};
        };
    }

    private static List<TameData> ownedTames(UUID owner) {
        List<TameData> list = new ArrayList<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!owner.equals(d.ownerUUID)) continue;
            if (isDeadEntry(d.uuid)) continue;
            list.add(d);
        }
        return list;
    }

    private static List<TameData> ownedDeadTames(UUID owner) {
        List<TameData> list = new ArrayList<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (d == null || d.uuid == null) continue;
            if (!owner.equals(d.ownerUUID)) continue;
            if (!isDeadEntry(d.uuid)) continue;
            list.add(d);
        }
        return list;
    }

    private static TameData findOwnedDeadTame(UUID owner, String name) {
        if (owner == null || name == null) return null;
        TameData best = null;
        for (TameData d : TameRegistry.TAMES.values()) {
            if (d == null || d.uuid == null || d.name == null) continue;
            if (!owner.equals(d.ownerUUID)) continue;
            if (!isDeadEntry(d.uuid)) continue;
            if (!d.name.equalsIgnoreCase(name)) continue;
            if (best == null
                    || d.deadUnixMillis > best.deadUnixMillis
                    || (d.deadUnixMillis == best.deadUnixMillis && d.deadGameTime > best.deadGameTime)) {
                best = d;
            }
        }
        return best;
    }

    private static List<TameDeathRecord> deathHistoryForName(UUID owner, String name, UUID tlId) {
        List<TameDeathRecord> list = new ArrayList<>();
        for (TameDeathRecord record : TameRegistry.DEATH_HISTORY) {
            if (!owner.equals(record.ownerUUID)) continue;
            if (record.reincarnated) continue;
            if (tlId != null && tlId.equals(record.tlId)) {
                list.add(record);
                continue;
            }
            if (record.name == null || !record.name.equalsIgnoreCase(name)) continue;
            list.add(record);
        }
        list.sort((a, b) -> Long.compare(b.deathGameTime, a.deathGameTime));
        return list;
    }

    private static TameDeathRecord latestAvailableDeathForTame(UUID owner, UUID tameUuid, UUID tlId, String name) {
        TameDeathRecord bestByTlId = null;
        long bestByTlIdTime = Long.MIN_VALUE;
        if (owner != null && tlId != null) {
            for (TameDeathRecord record : TameRegistry.DEATH_HISTORY) {
                if (record == null || record.reincarnated) continue;
                if (!owner.equals(record.ownerUUID)) continue;
                if (!tlId.equals(record.tlId)) continue;
                if (record.deathGameTime >= bestByTlIdTime) {
                    bestByTlId = record;
                    bestByTlIdTime = record.deathGameTime;
                }
            }
        }
        if (bestByTlId != null) return bestByTlId;
        TameDeathRecord bestByUuid = null;
        long bestByUuidTime = Long.MIN_VALUE;
        if (owner != null && tameUuid != null) {
            for (TameDeathRecord record : TameRegistry.DEATH_HISTORY) {
                if (record == null || record.reincarnated) continue;
                if (!owner.equals(record.ownerUUID)) continue;
                if (!tameUuid.equals(record.uuid)) continue;
                if (record.deathGameTime >= bestByUuidTime) {
                    bestByUuid = record;
                    bestByUuidTime = record.deathGameTime;
                }
            }
        }
        if (bestByUuid != null) return bestByUuid;
        if (owner == null || name == null || name.isBlank()) return null;
        TameDeathRecord bestByName = null;
        long bestByNameTime = Long.MIN_VALUE;
        for (TameDeathRecord record : TameRegistry.DEATH_HISTORY) {
            if (record == null || record.reincarnated) continue;
            if (!owner.equals(record.ownerUUID)) continue;
            if (record.name == null || !record.name.equalsIgnoreCase(name)) continue;
            if (record.deathGameTime >= bestByNameTime) {
                bestByName = record;
                bestByNameTime = record.deathGameTime;
            }
        }
        return bestByName;
    }

    private static boolean applyLatestDeathSnapshotIfAvailable(TamableAnimal tame, TameData current) {
        if (tame == null || current == null || current.ownerUUID == null) {
            return false;
        }
        TameDeathRecord record = latestAvailableDeathForTame(current.ownerUUID, current.uuid, current.tlId, current.name);
        if (record == null || record.snapshot == null || record.snapshot.isEmpty()) {
            return false;
        }
        TameData snapshot = TameData.fromTag(record.snapshot.copy());
        applySnapshotToTame(tame, current, snapshot);
        record.reincarnated = true;
        record.autoReincarnateOnRespawn = true;
        TameRegistry.removeLastDeath(record);
        return true;
    }

    private static void applySnapshotToTame(TamableAnimal tame, TameData current, TameData snapshot) {
        current.level = snapshot.level;
        current.xp = snapshot.xp;
        current.xpToNext = snapshot.xpToNext;
        current.kills = snapshot.kills;
        current.assists = snapshot.assists;
        current.deaths = snapshot.deaths;

        current.bonusHealth = snapshot.bonusHealth;
        current.bonusDamage = snapshot.bonusDamage;
        current.bonusSpeed = snapshot.bonusSpeed;
        current.bonusArmor = snapshot.bonusArmor;
        current.bonusArmorToughness = snapshot.bonusArmorToughness;
        current.bonusKnockback = snapshot.bonusKnockback;
        current.bonusKnockbackResist = snapshot.bonusKnockbackResist;

        current.tameClass = snapshot.tameClass;
        current.mode = snapshot.mode;
        // Reincarnation starts a new life timer used by the days leaderboard/stat.
        current.bornDayTime = tame.level().getDayTime();

        current.abilities.clear();
        current.abilities.addAll(snapshot.abilities);
        current.abilityLevels.clear();
        current.abilityLevels.putAll(snapshot.abilityLevels);
        current.attributeLevels.clear();
        current.attributeLevels.putAll(snapshot.attributeLevels);
        current.cooldowns.clear();
        current.hasSavedProgress = false;
        current.savedProgressCost = 0;

        // Recompute base stats from tame-type baseline + saved bonuses to avoid drift/stacking.
        if (!applyTypeBasePlusBonus(tame, current)) {
            LevelSystem.updateTameName(tame, current);
            tame.setHealth(tame.getMaxHealth());
        }
    }

    private static void addToAttribute(TamableAnimal tame, Attribute attribute, double amount) {
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null || amount == 0.0D) return;
        instance.setBaseValue(instance.getBaseValue() + amount);
    }

    private static List<TameData> ownedGroup(UUID owner, String group) {
        List<TameData> list = new ArrayList<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!owner.equals(d.ownerUUID)) continue;
            if (d.group == null || d.group.isBlank()) continue;
            if (d.group.equalsIgnoreCase(group)) list.add(d);
        }
        return list;
    }

    private static List<TameData> ownedDeadGroup(UUID owner, String group) {
        List<TameData> list = new ArrayList<>();
        for (TameData d : ownedGroup(owner, group)) {
            if (d == null || d.uuid == null) continue;
            if (!isDeadEntry(d.uuid)) continue;
            list.add(d);
        }
        return list;
    }

    private static List<TamableAnimal> loadedOwnedGroupTames(CommandSourceStack source, UUID owner, String group) {
        List<TamableAnimal> list = new ArrayList<>();
        for (TameData data : ownedGroup(owner, group)) {
            if (isDeadEntry(data.uuid)) continue;
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, owner, data.uuid);
            if (tame == null || !tame.isAlive()) continue;
            list.add(tame);
        }
        return list;
    }

    private static List<TamableAnimal> loadedOwnedAllTames(CommandSourceStack source, UUID owner) {
        List<TamableAnimal> list = new ArrayList<>();
        for (TameData data : ownedTames(owner)) {
            if (isDeadEntry(data.uuid)) continue;
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, owner, data.uuid);
            if (tame == null || !tame.isAlive()) continue;
            list.add(tame);
        }
        return list;
    }

    private static DuelSelectionResult resolveLoadedDuelSelection(CommandSourceStack source, UUID owner, DuelSelection selection) {
        if (selection == null) return DuelSelectionResult.fail("Invalid duel selection.");
        return switch (selection.kind) {
            case GROUP -> resolveGroupDuelSelection(source, owner, selection.value);
            case TYPE -> resolveTypeDuelSelection(source, owner, selection.value);
            case SINGLE -> {
                TameData data = findOwnedTame(owner, selection.value);
                if (data == null) {
                    yield DuelSelectionResult.fail("You do not own a living tame named '" + selection.value + "'.");
                }
                if (isDeadEntry(data.uuid)) {
                    yield DuelSelectionResult.fail("That tame is dead and cannot duel.");
                }
                TamableAnimal tame = findLoadedOwnedTameByUuid(source, owner, data.uuid);
                if (tame == null || !tame.isAlive()) {
                    yield DuelSelectionResult.fail("That tame is not loaded/alive.");
                }
                yield DuelSelectionResult.ok(List.of(tame));
            }
            case ALL -> {
                List<TameData> owned = ownedTames(owner);
                if (owned.isEmpty()) {
                    yield DuelSelectionResult.fail("You have no living tames to duel.");
                }
                List<String> unloaded = new ArrayList<>();
                List<TamableAnimal> loaded = new ArrayList<>();
                for (TameData data : owned) {
                    TamableAnimal tame = findLoadedOwnedTameByUuid(source, owner, data.uuid);
                    if (tame == null || !tame.isAlive()) {
                        unloaded.add(data.name == null || data.name.isBlank() ? "unknown" : data.name);
                    } else {
                        loaded.add(tame);
                    }
                }
                if (!unloaded.isEmpty()) {
                    yield DuelSelectionResult.fail("Load all selected tames first. Unloaded: " + String.join(", ", unloaded));
                }
                if (loaded.isEmpty()) {
                    yield DuelSelectionResult.fail("No loaded alive tames found for duel.");
                }
                yield DuelSelectionResult.ok(loaded);
            }
        };
    }

    private static DuelSelectionResult resolveGroupDuelSelection(CommandSourceStack source, UUID owner, String group) {
        List<TameData> groupMembers = ownedGroup(owner, group);
        if (groupMembers.isEmpty()) {
            return DuelSelectionResult.fail("Group '" + group + "' has no tames.");
        }
        List<String> dead = new ArrayList<>();
        List<String> unloaded = new ArrayList<>();
        List<TamableAnimal> loaded = new ArrayList<>();
        for (TameData data : groupMembers) {
            String name = data.name == null || data.name.isBlank() ? "unknown" : data.name;
            if (isDeadEntry(data.uuid)) {
                dead.add(name);
                continue;
            }
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, owner, data.uuid);
            if (tame == null || !tame.isAlive()) {
                unloaded.add(name);
                continue;
            }
            loaded.add(tame);
        }
        if (!dead.isEmpty()) {
            return DuelSelectionResult.fail("Group '" + group + "' contains dead tames: " + String.join(", ", dead));
        }
        if (!unloaded.isEmpty()) {
            return DuelSelectionResult.fail("Group '" + group + "' has unloaded tames: " + String.join(", ", unloaded));
        }
        if (loaded.isEmpty()) {
            return DuelSelectionResult.fail("Group '" + group + "' has no loaded alive tames.");
        }
        return DuelSelectionResult.ok(loaded);
    }

    private static DuelSelectionResult resolveTypeDuelSelection(CommandSourceStack source, UUID owner, String type) {
        List<TameData> typeMembers = ownedType(owner, type);
        if (typeMembers.isEmpty()) {
            return DuelSelectionResult.fail("Type '" + type + "' has no tames.");
        }
        List<String> dead = new ArrayList<>();
        List<String> unloaded = new ArrayList<>();
        List<TamableAnimal> loaded = new ArrayList<>();
        for (TameData data : typeMembers) {
            String name = data.name == null || data.name.isBlank() ? "unknown" : data.name;
            if (isDeadEntry(data.uuid)) {
                dead.add(name);
                continue;
            }
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, owner, data.uuid);
            if (tame == null || !tame.isAlive()) {
                unloaded.add(name);
                continue;
            }
            loaded.add(tame);
        }
        if (!dead.isEmpty()) {
            return DuelSelectionResult.fail("Type '" + type + "' contains dead tames: " + String.join(", ", dead));
        }
        if (!unloaded.isEmpty()) {
            return DuelSelectionResult.fail("Type '" + type + "' has unloaded tames: " + String.join(", ", unloaded));
        }
        if (loaded.isEmpty()) {
            return DuelSelectionResult.fail("Type '" + type + "' has no loaded alive tames.");
        }
        return DuelSelectionResult.ok(loaded);
    }

    private static String duelSelectionLabel(DuelSelection selection) {
        if (selection == null) return "selected tames";
        return switch (selection.kind) {
            case GROUP -> "group " + selection.value;
            case TYPE -> "type " + selection.value;
            case SINGLE -> "tame " + selection.value;
            case ALL -> "all loaded tames";
        };
    }

    private static TamableAnimal nearestLoadedOpponent(TamableAnimal from, List<TamableAnimal> opponents) {
        if (from == null || opponents == null || opponents.isEmpty()) return null;
        TamableAnimal best = null;
        double bestDist = Double.MAX_VALUE;
        for (TamableAnimal opponent : opponents) {
            if (opponent == null || !opponent.isAlive()) continue;
            if (opponent.level() != from.level()) continue;
            double dist = from.distanceToSqr(opponent);
            if (dist < bestDist) {
                bestDist = dist;
                best = opponent;
            }
        }
        return best;
    }

    private static List<TameData> ownedType(UUID owner, String typeFilter) {
        List<TameData> list = new ArrayList<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!owner.equals(d.ownerUUID)) continue;
            if (isDeadEntry(d.uuid)) continue;
            if (!matchesTypeFilter(d, typeFilter)) continue;
            list.add(d);
        }
        return list;
    }

    private static List<TameData> ownedDeadType(UUID owner, String typeFilter) {
        List<TameData> list = new ArrayList<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!owner.equals(d.ownerUUID)) continue;
            if (!isDeadEntry(d.uuid)) continue;
            if (!matchesTypeFilter(d, typeFilter)) continue;
            list.add(d);
        }
        return list;
    }

    private static boolean matchesTypeFilter(TameData data, String typeFilter) {
        String requested = normalizeTypeFilter(typeFilter);
        if (requested.isBlank()) return false;
        String full = tameTypeId(data);
        if (full.isBlank()) return false;
        if (requested.contains(":")) {
            return full.equalsIgnoreCase(requested);
        }
        int sep = full.indexOf(':');
        String path = sep >= 0 ? full.substring(sep + 1) : full;
        return path.equalsIgnoreCase(requested);
    }

    private static String tameTypeId(TameData data) {
        String full = recoverEntityTypeId(data);
        return full == null ? "" : full.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeTypeFilter(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) return "";
        if (value.startsWith("entity.")) {
            value = value.substring("entity.".length());
            int firstDot = value.indexOf('.');
            if (firstDot > 0 && !value.contains(":")) {
                value = value.substring(0, firstDot) + ":" + value.substring(firstDot + 1);
            }
        }
        return value;
    }

    private static TameData findOwnedTame(UUID owner, String name) {
        if (owner == null || name == null) return null;
        TameData best = null;
        for (TameData d : TameRegistry.TAMES.values()) {
            if (d == null || d.name == null) continue;
            if (!owner.equals(d.ownerUUID)) continue;
            if (isDeadEntry(d.uuid)) continue;
            if (!d.name.equalsIgnoreCase(name)) continue;
            if (best == null
                    || d.level > best.level
                    || (d.level == best.level && String.valueOf(d.uuid).compareTo(String.valueOf(best.uuid)) < 0)) {
                best = d;
            }
        }
        return best;
    }

    private static TameData findOwnedTameAny(UUID owner, String name) {
        TameData alive = findOwnedTame(owner, name);
        if (alive != null) {
            return alive;
        }
        if (owner == null || name == null) return null;
        TameData bestDead = null;
        for (TameData d : TameRegistry.TAMES.values()) {
            if (d == null || d.name == null) continue;
            if (!owner.equals(d.ownerUUID)) continue;
            if (!d.name.equalsIgnoreCase(name)) continue;
            if (!isDeadEntry(d.uuid)) continue;
            if (bestDead == null
                    || d.deadUnixMillis > bestDead.deadUnixMillis
                    || (d.deadUnixMillis == bestDead.deadUnixMillis && d.deadGameTime > bestDead.deadGameTime)
                    || (d.deadUnixMillis == bestDead.deadUnixMillis
                    && d.deadGameTime == bestDead.deadGameTime
                    && d.level > bestDead.level)) {
                bestDead = d;
            }
        }
        return bestDead;
    }

    private static boolean isDeadEntry(UUID tameUuid) {
        if (tameUuid == null) return false;
        TameData data = TameRegistry.get(tameUuid);
        if (data != null) {
            return data.dead;
        }
        // Backward compatibility with older persisted death registries.
        return TameRegistry.LAST_DEATHS.containsKey(tameUuid);
    }

    private static long daysAlive(CommandSourceStack source, TameData data) {
        if (data == null || data.bornDayTime <= 0L) return 0L;
        long now = source.getServer().overworld().getDayTime();
        long ticks = Math.max(0L, now - data.bornDayTime);
        return ticks / 24000L;
    }

    private static TamableAnimal findLoadedOwnedTameByUuid(CommandSourceStack source, UUID owner, UUID tameUuid) {
        for (var level : source.getServer().getAllLevels()) {
            Entity entity = level.getEntity(tameUuid);
            if (!(entity instanceof TamableAnimal ta) || !ta.isTame()) continue;
            if (!owner.equals(ta.getOwnerUUID())) continue;
            return ta;
        }
        return null;
    }

    private static TamableAnimal findLoadedTameByUuid(CommandSourceStack source, UUID tameUuid) {
        for (var level : source.getServer().getAllLevels()) {
            Entity entity = level.getEntity(tameUuid);
            if (!(entity instanceof TamableAnimal ta) || !ta.isTame()) continue;
            return ta;
        }
        return null;
    }

    private static TamableAnimal findLoadedTameByUuid(MinecraftServer server, UUID tameUuid) {
        if (server == null || tameUuid == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(tameUuid);
            if (!(entity instanceof TamableAnimal ta) || !ta.isTame()) continue;
            return ta;
        }
        return null;
    }

    private static TamableAnimal findLoadedOwnedTameByName(CommandSourceStack source, UUID owner, String name) {
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!owner.equals(d.ownerUUID)) continue;
            if (isDeadEntry(d.uuid)) continue;
            if (!d.name.equalsIgnoreCase(name)) continue;
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, owner, d.uuid);
            if (tame != null) return tame;
        }
        return null;
    }

    private static CompletableFuture<Suggestions> suggestOwnedPetNames(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return b.buildFuture();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            if (isDeadEntry(d.uuid)) continue;
            suggestCommandString(b, d.name);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOwnedPetNamesAll(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return b.buildFuture();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            suggestCommandString(b, d.name);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOwnedUnloadedPetNames(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return b.buildFuture();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            if (isDeadEntry(d.uuid)) continue;
            if (isEffectivelyLoaded(source, p, d)) continue;
            suggestCommandString(b, d.name);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOwnedDeadPetNames(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return b.buildFuture();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (d == null || d.name == null || d.name.isBlank()) continue;
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            if (!isDeadEntry(d.uuid)) continue;
            suggestCommandString(b, d.name);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandSourceStack source, SuggestionsBuilder b) {
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            b.suggest(player.getGameProfile().getName());
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestKnownPlayerOwners(CommandSourceStack source, SuggestionsBuilder b) {
        Set<String> names = new HashSet<>();
        MinecraftServer server = source.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            names.add(player.getGameProfile().getName());
        }
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.ownerUUID == null) continue;
            String resolved = resolveKnownOwnerName(server, data.ownerUUID, "");
            if (!resolved.isBlank()) {
                names.add(resolved);
            }
        }
        for (String name : names) {
            b.suggest(name);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestIncomingDuelChallengers(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return b.buildFuture();
        cleanupExpiredDuelInvites();
        Map<UUID, DuelInvite> incoming = DUEL_INVITES.get(p.getUUID());
        if (incoming == null || incoming.isEmpty()) return b.buildFuture();
        for (UUID challengerId : incoming.keySet()) {
            ServerPlayer challenger = source.getServer().getPlayerList().getPlayer(challengerId);
            if (challenger != null) {
                b.suggest(challenger.getGameProfile().getName());
            }
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOwnedGroups(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return b.buildFuture();
        Set<String> seen = new HashSet<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID) || d.group == null || d.group.isBlank()) continue;
            if (seen.add(d.group)) b.suggest(d.group);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOwnedTypes(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return b.buildFuture();
        Set<String> seenPath = new HashSet<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            if (isDeadEntry(d.uuid)) continue;
            String full = tameTypeId(d);
            if (full.isBlank()) continue;
            int sep = full.indexOf(':');
            String path = sep >= 0 ? full.substring(sep + 1) : full;
            if (!path.isBlank() && seenPath.add(path)) b.suggest(path);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestDeadPetNames(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return b.buildFuture();
        Set<String> seen = new HashSet<>();
        for (TameDeathRecord r : TameRegistry.DEATH_HISTORY) {
            if (!p.getUUID().equals(r.ownerUUID) || r.name == null || r.name.isBlank()) continue;
            if (seen.add(r.name)) suggestCommandString(b, r.name);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestModes(SuggestionsBuilder b) {
        for (TameMode mode : TameMode.values()) b.suggest(mode.key());
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestMovementStates(SuggestionsBuilder b) {
        b.suggest("follow");
        b.suggest("wander");
        b.suggest("sit");
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAbilities(SuggestionsBuilder b) {
        LevelSystem.knownAbilityIds().forEach(b::suggest);
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAdminAbilities(SuggestionsBuilder b) {
        for (String id : LevelSystem.knownAbilityIds()) {
            if ("berserker".equalsIgnoreCase(id) || "passive".equalsIgnoreCase(id)) continue;
            b.suggest(id);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAttributes(SuggestionsBuilder b) {
        LevelSystem.knownAttributeIds().forEach(b::suggest);
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestClasses(SuggestionsBuilder b) {
        classNames().forEach(b::suggest);
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAllAliveTameNames(SuggestionsBuilder b) {
        Set<String> seen = new HashSet<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (isDeadEntry(d.uuid)) continue;
            if (d.name == null || d.name.isBlank()) continue;
            if (seen.add(d.name)) suggestCommandString(b, d.name);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAllRespawnableTameNames(SuggestionsBuilder b) {
        Set<String> seen = new HashSet<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (d.name == null || d.name.isBlank()) continue;
            if (seen.add(d.name)) suggestCommandString(b, d.name);
        }
        for (TameDeathRecord r : TameRegistry.LAST_DEATHS.values()) {
            if (r == null || r.name == null || r.name.isBlank()) continue;
            if (seen.add(r.name)) suggestCommandString(b, r.name);
        }
        return b.buildFuture();
    }

    private static void suggestCommandString(SuggestionsBuilder b, String value) {
        if (b == null || value == null || value.isBlank()) {
            return;
        }
        b.suggest(StringArgumentType.escapeIfRequired(value));
    }

    private static List<String> classNames() {
        List<String> names = new ArrayList<>();
        for (TameClass value : TameClass.values()) {
            names.add(value.name().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private static CompletableFuture<Suggestions> suggestLeaderboardTypes(SuggestionsBuilder b) {
        b.suggest("mix"); b.suggest("kills"); b.suggest("deaths"); b.suggest("assists"); b.suggest("lvl"); b.suggest("days");
        return b.buildFuture();
    }

    private static String stripLevelPrefixes(String name) {
        if (name == null) return "";
        String cleaned = name;
        while (true) {
            String next = LEVEL_PREFIX_PATTERN.matcher(cleaned).replaceFirst("");
            if (next.equals(cleaned)) break;
            cleaned = next;
        }
        return cleaned.trim();
    }
}
