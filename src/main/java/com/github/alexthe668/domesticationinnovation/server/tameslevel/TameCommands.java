package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.CommonProxy;
import com.github.alexthe668.domesticationinnovation.server.TLMigrationImportData;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.misc.DITameProgressData;
import com.github.alexthe668.domesticationinnovation.server.misc.LanternRequest;
import com.github.alexthe668.domesticationinnovation.server.item.DIItemRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalInstaller;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.TameClass;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAbilityEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TimedTameArrow;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TimedTameDragonFireball;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TimedTameLlamaSpit;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TimedTameShulkerBullet;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TimedTameSmallFireball;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TimedTameSnowball;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TimedTameThrownPotion;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TimedTameTrident;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TimedTameWitherSkull;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.NoGriefLargeFireball;
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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import com.github.alexthe668.domesticationinnovation.server.misc.DIWorldData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.function.Predicate;
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
    private static final String JOIN_FIX_STALE_SKIP_TAG = "DITLJoinFixStaleSkip";
    private static final UUID COLLAR_ARMOR_UUID = UUID.fromString("e6e52fdd-8e14-4c0d-9ac1-8fbc60f3dd01");
    private static final UUID COLLAR_ARMOR_TOUGHNESS_UUID = UUID.fromString("f2f6c7ab-8a73-4d1c-95e4-07f171ddca8f");
    private static final String DOC_RESOURCE_BASE = "assets/domesticationinnovation/tameslevel/old docus/";
    private static final Path DOC_SOURCE_BASE = Path.of("src", "main", "java", "com", "github", "alexthe668", "domesticationinnovation", "server", "tameslevel", "old docus");
    private static final Pattern LEVEL_PREFIX_PATTERN = Pattern.compile("^\\s*\\[(?:(?:lvl|level)\\s*)?\\d+\\]\\s*", Pattern.CASE_INSENSITIVE);
    private static final long DUEL_INVITE_TIMEOUT_MS = 120_000L;
    private static final int CLASS_REROLL_CONFIRM_TICKS = 20 * 30;
    private static final int MAX_CLASS_REROLLS = 3;
    private static final Map<UUID, Map<UUID, DuelInvite>> DUEL_INVITES = new HashMap<>();
    private static final Map<UUID, PendingDuelMatch> PENDING_DUEL_MATCHES = new HashMap<>();
    private static final Map<UUID, UUID> PENDING_DUEL_MATCH_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, PendingClassReroll> PENDING_CLASS_REROLLS = new HashMap<>();
    private static final int UNLOADED_TP_TIMEOUT_TICKS = 1200;
    private static final int MORNING_LANTERN_TIMEOUT_TICKS = 200;
    private static final int MORNING_LANTERN_RADIUS = 64;
    private static final int IMMEDIATE_CHUNK_TP_INITIAL_DELAY_TICKS = 5;
    private static final int IMMEDIATE_CHUNK_TP_MAX_WAIT_TICKS = 200;
    private static final String TAG_GUARDIAN_TOOL_ORDER = "DIGuardianToolOrder";
    private static final long GUARDIAN_TOOL_CONFIRM_TICKS = 20L * 60L;
    private static final Map<UUID, PendingMorningLanternRecall> PENDING_MORNING_LANTERN = new HashMap<>();
    private static final Map<UUID, PendingImmediateChunkTeleport> PENDING_IMMEDIATE_CHUNK_TELEPORTS = new HashMap<>();
    private static final Map<UUID, PendingGuardianToolConfirm> PENDING_GUARDIAN_TOOL_CONFIRMS = new HashMap<>();
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
        WANDER,
        GUARDIAN
    }

    private enum DuelSelectionKind {
        GROUP,
        TYPE,
        STATE,
        SINGLE,
        ALL
    }

    private enum RespawnOrderMode {
        DEFAULT("default"),
        LEVEL("level"),
        LEADERBOARD("leaderboard");

        private final String id;

        RespawnOrderMode(String id) {
            this.id = id;
        }

        private static RespawnOrderMode parse(String raw) {
            if (raw == null) {
                return DEFAULT;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            for (RespawnOrderMode value : values()) {
                if (value.id.equals(normalized)) {
                    return value;
                }
            }
            return DEFAULT;
        }
    }

    private enum GuardianToolOrder {
        LEVEL("lvl"),
        NAME("name"),
        POINTS("points"),
        TYPE("type");

        private final String id;

        GuardianToolOrder(String id) {
            this.id = id;
        }

        private static GuardianToolOrder parse(String raw) {
            if (raw == null) {
                return LEVEL;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("level")) {
                return LEVEL;
            }
            for (GuardianToolOrder value : values()) {
                if (value.id.equals(normalized)) {
                    return value;
                }
            }
            return LEVEL;
        }
    }

    private enum ReviveMode {
        RESPAWN(false, "Respawned"),
        ARISE(true, "Arose");

        private final boolean toMe;
        private final String label;

        ReviveMode(boolean toMe, String label) {
            this.toMe = toMe;
            this.label = label;
        }
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

    private enum GuardianToolConfirmAction {
        CLEAR_ALL
    }

    private record PendingGuardianToolConfirm(GuardianToolConfirmAction action, String selectorRaw, String setName, String dimensionId, int x, int y, int z, long expiresAtTick) {
    }

    private static final class PendingImmediateChunkTeleport {
        private final UUID ticketId;
        private final UUID tameUuid;
        private final UUID tlId;
        private final UUID ownerUuid;
        private final ResourceKey<Level> sourceDimension;
        private final BlockPos sourcePos;
        private final SpawnTarget target;
        private final long createdTick;
        private long nextAttemptTick;
        private long chunksReadyTick;
        private final String tameName;
        private final boolean liveEntityOnly;
        private final boolean silent;

        private PendingImmediateChunkTeleport(UUID ticketId, UUID tameUuid, UUID tlId, UUID ownerUuid, ResourceKey<Level> sourceDimension, BlockPos sourcePos, SpawnTarget target, long createdTick, long nextAttemptTick, long chunksReadyTick, String tameName, boolean liveEntityOnly, boolean silent) {
            this.ticketId = ticketId;
            this.tameUuid = tameUuid;
            this.tlId = tlId;
            this.ownerUuid = ownerUuid;
            this.sourceDimension = sourceDimension;
            this.sourcePos = sourcePos;
            this.target = target;
            this.createdTick = createdTick;
            this.nextAttemptTick = nextAttemptTick;
            this.chunksReadyTick = chunksReadyTick;
            this.tameName = tameName == null ? "unknown" : tameName;
            this.liveEntityOnly = liveEntityOnly;
            this.silent = silent;
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

        private static DuelSelection state(String name) {
            return new DuelSelection(DuelSelectionKind.STATE, name);
        }

        private static DuelSelection single(String name) {
            return new DuelSelection(DuelSelectionKind.SINGLE, name);
        }

        private static DuelSelection all() {
            return new DuelSelection(DuelSelectionKind.ALL, "");
        }
    }

    private static final class TeamSelection {
        private final boolean includeSelf;
        private final List<DuelSelection> tameSelections;

        private TeamSelection(boolean includeSelf, List<DuelSelection> tameSelections) {
            this.includeSelf = includeSelf;
            this.tameSelections = tameSelections == null ? List.of() : List.copyOf(tameSelections);
        }

        private static TeamSelection tameOnly(DuelSelection selection) {
            return new TeamSelection(false, selection == null ? List.of() : List.of(selection));
        }

        private static TeamSelection selfOnly() {
            return new TeamSelection(true, List.of());
        }

        private static TeamSelection of(boolean includeSelf, DuelSelection tameSelection) {
            return new TeamSelection(includeSelf, tameSelection == null ? List.of() : List.of(tameSelection));
        }

        private static TeamSelection of(boolean includeSelf, List<DuelSelection> tameSelections) {
            return new TeamSelection(includeSelf, tameSelections);
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

    private static final class TeamSelectionResult {
        private final List<LivingEntity> members;
        private final List<TamableAnimal> tames;
        private final String error;

        private TeamSelectionResult(List<LivingEntity> members, List<TamableAnimal> tames, String error) {
            this.members = members;
            this.tames = tames;
            this.error = error == null ? "" : error;
        }

        private static TeamSelectionResult ok(List<LivingEntity> members, List<TamableAnimal> tames) {
            return new TeamSelectionResult(members, tames, "");
        }

        private static TeamSelectionResult fail(String error) {
            return new TeamSelectionResult(List.of(), List.of(), error);
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

    private record PaymentResult(boolean success, String label, String error) {
        private static PaymentResult ok(String label) {
            return new PaymentResult(true, label == null ? "" : label, "");
        }

        private static PaymentResult fail(String error) {
            return new PaymentResult(false, "", error == null ? "unknown payment error" : error);
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
        private final TeamSelection challengerSelection;
        private final DuelSpectators spectators;
        private final long createdAtMs;

        private DuelInvite(UUID challengerUuid, UUID targetUuid, TeamSelection challengerSelection, DuelSpectators spectators, long createdAtMs) {
            this.challengerUuid = challengerUuid;
            this.targetUuid = targetUuid;
            this.challengerSelection = challengerSelection;
            this.spectators = spectators == null ? DuelSpectators.empty() : spectators;
            this.createdAtMs = createdAtMs;
        }
    }

    private static final class DuelSpectators {
        private final boolean broadcastToServer;
        private final Set<UUID> playerIds;

        private DuelSpectators(boolean broadcastToServer, Set<UUID> playerIds) {
            this.broadcastToServer = broadcastToServer;
            this.playerIds = playerIds == null ? Set.of() : Set.copyOf(playerIds);
        }

        private static DuelSpectators empty() {
            return new DuelSpectators(false, Set.of());
        }

        private boolean isEmpty() {
            return !broadcastToServer && playerIds.isEmpty();
        }
    }

    private static final class DuelSpectatorParseResult {
        private final DuelSpectators spectators;
        private final String error;

        private DuelSpectatorParseResult(DuelSpectators spectators, String error) {
            this.spectators = spectators == null ? DuelSpectators.empty() : spectators;
            this.error = error == null ? "" : error;
        }

        private static DuelSpectatorParseResult ok(DuelSpectators spectators) {
            return new DuelSpectatorParseResult(spectators, "");
        }

        private static DuelSpectatorParseResult fail(String error) {
            return new DuelSpectatorParseResult(DuelSpectators.empty(), error);
        }
    }

    private static final class CompactDuelSideParseResult {
        private final TeamSelection selection;
        private final List<String> targetPlayerNames;
        private final String error;

        private CompactDuelSideParseResult(TeamSelection selection, List<String> targetPlayerNames, String error) {
            this.selection = selection;
            this.targetPlayerNames = targetPlayerNames == null ? List.of() : List.copyOf(targetPlayerNames);
            this.error = error == null ? "" : error;
        }

        private static CompactDuelSideParseResult ok(TeamSelection selection, List<String> targetPlayerNames) {
            return new CompactDuelSideParseResult(selection, targetPlayerNames, "");
        }

        private static CompactDuelSideParseResult fail(String error) {
            return new CompactDuelSideParseResult(null, List.of(), error);
        }
    }

    private static final class PendingDuelParticipant {
        private final UUID playerUuid;
        private final boolean sideA;
        private UUID invitedBy;
        private TeamSelection acceptedSelection;
        private boolean accepted;

        private PendingDuelParticipant(UUID playerUuid, boolean sideA, UUID invitedBy, TeamSelection acceptedSelection, boolean accepted) {
            this.playerUuid = playerUuid;
            this.sideA = sideA;
            this.invitedBy = invitedBy;
            this.acceptedSelection = acceptedSelection;
            this.accepted = accepted;
        }
    }

    private static final class PendingDuelMatch {
        private final UUID matchId;
        private final UUID initiatorUuid;
        private final long createdAtMs;
        private final DuelSpectators spectators;
        private final Map<UUID, PendingDuelParticipant> participants = new LinkedHashMap<>();

        private PendingDuelMatch(UUID matchId, UUID initiatorUuid, DuelSpectators spectators, long createdAtMs) {
            this.matchId = matchId;
            this.initiatorUuid = initiatorUuid;
            this.spectators = spectators == null ? DuelSpectators.empty() : spectators;
            this.createdAtMs = createdAtMs;
        }
    }

    private static final class PendingClassReroll {
        private final UUID tameUuid;
        private final UUID tameTlId;
        private final String tameName;
        private final long expiresAtGameTime;

        private PendingClassReroll(UUID tameUuid, UUID tameTlId, String tameName, long expiresAtGameTime) {
            this.tameUuid = tameUuid;
            this.tameTlId = tameTlId;
            this.tameName = tameName == null ? "unknown" : tameName;
            this.expiresAtGameTime = expiresAtGameTime;
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildLegacyDuelCommand() {
        return Commands.literal("duelOld")
                .then(Commands.literal("vs")
                        .then(Commands.literal("group")
                                .then(Commands.argument("left", StringArgumentType.word())
                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> duelStartSameOwner(
                                                        ctx.getSource(),
                                                        DuelSelection.group(StringArgumentType.getString(ctx, "left")),
                                                        DuelSelection.all()
                                                )))
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
                                        .then(Commands.literal("all")
                                                .executes(ctx -> duelStartSameOwner(
                                                        ctx.getSource(),
                                                        DuelSelection.type(StringArgumentType.getString(ctx, "left")),
                                                        DuelSelection.all()
                                                )))
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
                                        .then(Commands.literal("all")
                                                .executes(ctx -> duelStartSameOwner(
                                                        ctx.getSource(),
                                                        DuelSelection.single(StringArgumentType.getString(ctx, "left")),
                                                        DuelSelection.all()
                                                )))
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
                                .executes(ctx -> duelAcceptQuickLegacy(
                                        ctx.getSource(),
                                        DuelSelection.group(StringArgumentType.getString(ctx, "group"))
                                ))
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                        .executes(ctx -> duelInviteSelectionLegacy(
                                                ctx.getSource(),
                                                DuelSelection.group(StringArgumentType.getString(ctx, "group")),
                                                StringArgumentType.getString(ctx, "player")
                                        )))))
                .then(Commands.literal("type")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                .executes(ctx -> duelAcceptQuickLegacy(
                                        ctx.getSource(),
                                        DuelSelection.type(StringArgumentType.getString(ctx, "type"))
                                ))
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                        .executes(ctx -> duelInviteSelectionLegacy(
                                                ctx.getSource(),
                                                DuelSelection.type(StringArgumentType.getString(ctx, "type")),
                                                StringArgumentType.getString(ctx, "player")
                                        )))))
                .then(Commands.literal("all")
                        .executes(ctx -> duelAcceptQuickLegacy(ctx.getSource(), DuelSelection.all()))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                .executes(ctx -> duelInviteSelectionLegacy(
                                        ctx.getSource(),
                                        DuelSelection.all(),
                                        StringArgumentType.getString(ctx, "player")
                                ))))
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                        .executes(ctx -> duelAcceptQuickLegacy(
                                ctx.getSource(),
                                DuelSelection.single(StringArgumentType.getString(ctx, "name"))
                        ))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                .executes(ctx -> duelInviteSelectionLegacy(
                                        ctx.getSource(),
                                        DuelSelection.single(StringArgumentType.getString(ctx, "name")),
                                        StringArgumentType.getString(ctx, "player")
                                ))))
                .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                .executes(ctx -> duelInviteSelectionLegacy(
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
                                                .executes(ctx -> duelAcceptSelectionLegacy(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"),
                                                        DuelSelection.group(StringArgumentType.getString(ctx, "group"))
                                                ))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .executes(ctx -> duelAcceptSelectionLegacy(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"),
                                                        DuelSelection.type(StringArgumentType.getString(ctx, "type"))
                                                ))))
                                .then(Commands.literal("all")
                                        .executes(ctx -> duelAcceptSelectionLegacy(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                DuelSelection.all()
                                        )))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                        .executes(ctx -> duelAcceptSelectionLegacy(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                DuelSelection.single(StringArgumentType.getString(ctx, "name"))
                                        )))
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                        .executes(ctx -> duelAcceptSelectionLegacy(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                DuelSelection.group(StringArgumentType.getString(ctx, "group"))
                                        )))))
                .then(Commands.literal("decline")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, b) -> suggestIncomingDuelChallengers(ctx.getSource(), b))
                                .executes(ctx -> duelDeclineLegacy(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")
                                ))))
                .then(Commands.literal("ff")
                        .executes(ctx -> duelForfeit(ctx.getSource())))
                .then(Commands.literal("inbox")
                        .executes(ctx -> duelInboxLegacy(ctx.getSource())));
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
                        .then(Commands.literal("stats")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNamesAll(ctx.getSource(), b))
                                        .executes(ctx -> statLong(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNamesAll(ctx.getSource(), b))
                                        .executes(ctx -> inspectPet(ctx.getSource(), StringArgumentType.getString(ctx, "name"), true))
                                        .then(Commands.literal("long")
                                                .executes(ctx -> inspectPet(ctx.getSource(), StringArgumentType.getString(ctx, "name"), true)))))
                        .then(Commands.literal("reincarnate")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNamesAll(ctx.getSource(), b))
                                        .executes(ctx -> reincarnatePet(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("rerollClass")
                                .then(Commands.literal("confirm")
                                        .executes(ctx -> confirmRerollClass(ctx.getSource())))
                                .then(Commands.literal("cancel")
                                        .executes(ctx -> cancelRerollClass(ctx.getSource())))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNamesAll(ctx.getSource(), b))
                                        .executes(ctx -> requestRerollClass(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("reincarnation")
                                .executes(ctx -> reincarnationOverview(ctx.getSource()))
                                .then(Commands.literal("approvedItems")
                                        .executes(ctx -> listApprovedReincarnationItems(ctx.getSource())))
                                .then(Commands.literal("auto")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> setAutoReincarnation(
                                                        ctx.getSource(),
                                                        BoolArgumentType.getBool(ctx, "enabled")
                                                ))))
                                .then(Commands.literal("all")
                                        .executes(ctx -> reincarnateBatchAll(ctx.getSource())))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .executes(ctx -> reincarnateBatchGroup(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .executes(ctx -> reincarnateBatchType(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))))
                        .then(Commands.literal("doNotAttack")
                                .executes(ctx -> listDoNotAttack(ctx.getSource()))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("mobtype", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestCurrentPlayerDoNotAttackTypes(ctx.getSource(), b))
                                                .executes(ctx -> removeDoNotAttackType(ctx.getSource(), StringArgumentType.getString(ctx, "mobtype")))))
                                .then(Commands.argument("mobtype", StringArgumentType.word())
                                        .suggests((ctx, b) -> suggestEntityTypes(b))
                                        .executes(ctx -> toggleDoNotAttackType(ctx.getSource(), StringArgumentType.getString(ctx, "mobtype")))))
                        .then(Commands.literal("bed")
                                .executes(ctx -> listOwnedBeds(ctx.getSource()))
                                .then(Commands.literal("long")
                                        .executes(ctx -> listOwnedBedsLong(ctx.getSource())))
                                .then(Commands.literal("noBed")
                                        .executes(ctx -> listOwnedBedsWithoutBed(ctx.getSource())))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                .executes(ctx -> setOwnedBed(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                .then(Commands.literal("remove")
                                        .then(Commands.literal("all")
                                                .executes(ctx -> removeOwnedBedsAll(ctx.getSource())))
                                        .then(Commands.literal("group")
                                                .then(Commands.argument("group", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                        .executes(ctx -> removeOwnedBedsGroup(ctx.getSource(), StringArgumentType.getString(ctx, "group")))))
                                        .then(Commands.literal("type")
                                                .then(Commands.argument("type", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                        .executes(ctx -> removeOwnedBedsType(ctx.getSource(), StringArgumentType.getString(ctx, "type")))))
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestOwnedPetNamesWithBeds(ctx.getSource(), b))
                                                .executes(ctx -> removeOwnedBed(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))))
                        .then(Commands.literal("doNotAttackAnimals")
                                .executes(ctx -> listDoNotAttack(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setDoNotAttackAnimals(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("healthSiphon")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setHealthSiphonEnabled(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("enterPortalsByThemselves")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setEnterPortalsByThemselves(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("graveyard")
                                .executes(ctx -> graveyard(ctx.getSource(), 10))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                        .executes(ctx -> graveyard(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "limit")))))
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
                        .then(Commands.literal("_duelOldCompat")
                                .then(Commands.literal("vs")
                                        .then(Commands.literal("group")
                                                .then(Commands.argument("left", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                        .then(Commands.literal("all")
                                                                .executes(ctx -> duelStartSameOwner(
                                                                        ctx.getSource(),
                                                                        DuelSelection.group(StringArgumentType.getString(ctx, "left")),
                                                                        DuelSelection.all()
                                                                ))
                                                                .then(Commands.literal("spectator")
                                                                        .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                .executes(ctx -> duelStartSameOwnerWithSpectators(
                                                                                        ctx.getSource(),
                                                                                        DuelSelection.group(StringArgumentType.getString(ctx, "left")),
                                                                                        DuelSelection.all(),
                                                                                        StringArgumentType.getString(ctx, "players")
                                                                                )))))
                                                        .then(Commands.literal("group")
                                                                .then(Commands.argument("right", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "right"))
                                                                        ))
                                                                        .then(Commands.literal("spectator")
                                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                        .executes(ctx -> duelStartSameOwnerWithSpectators(
                                                                                                ctx.getSource(),
                                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "left")),
                                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "right")),
                                                                                                StringArgumentType.getString(ctx, "players")
                                                                                        ))))))
                                                        .then(Commands.literal("type")
                                                                .then(Commands.argument("right", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "right"))
                                                                        ))
                                                                        .then(Commands.literal("spectator")
                                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                        .executes(ctx -> duelStartSameOwnerWithSpectators(
                                                                                                ctx.getSource(),
                                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "left")),
                                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "right")),
                                                                                                StringArgumentType.getString(ctx, "players")
                                                                                        ))))))
                                                        .then(Commands.literal("name")
                                                                .then(Commands.argument("right", StringArgumentType.string())
                                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "right"))
                                                                        ))
                                                                        .then(Commands.literal("spectator")
                                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                        .executes(ctx -> duelStartSameOwnerWithSpectators(
                                                                                                ctx.getSource(),
                                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "left")),
                                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "right")),
                                                                                                StringArgumentType.getString(ctx, "players")
                                                                                        ))))))))
                                        .then(Commands.literal("type")
                                                .then(Commands.argument("left", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                        .then(Commands.literal("all")
                                                                .executes(ctx -> duelStartSameOwner(
                                                                        ctx.getSource(),
                                                                        DuelSelection.type(StringArgumentType.getString(ctx, "left")),
                                                                        DuelSelection.all()
                                                                ))
                                                                .then(Commands.literal("spectator")
                                                                        .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                .executes(ctx -> duelStartSameOwnerWithSpectators(
                                                                                        ctx.getSource(),
                                                                                        DuelSelection.type(StringArgumentType.getString(ctx, "left")),
                                                                                        DuelSelection.all(),
                                                                                        StringArgumentType.getString(ctx, "players")
                                                                                )))))
                                                        .then(Commands.literal("group")
                                                                .then(Commands.argument("right", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "right"))
                                                                        ))
                                                                        .then(Commands.literal("spectator")
                                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                        .executes(ctx -> duelStartSameOwnerWithSpectators(
                                                                                                ctx.getSource(),
                                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "left")),
                                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "right")),
                                                                                                StringArgumentType.getString(ctx, "players")
                                                                                        ))))))
                                                        .then(Commands.literal("type")
                                                                .then(Commands.argument("right", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "right"))
                                                                        ))
                                                                        .then(Commands.literal("spectator")
                                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                        .executes(ctx -> duelStartSameOwnerWithSpectators(
                                                                                                ctx.getSource(),
                                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "left")),
                                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "right")),
                                                                                                StringArgumentType.getString(ctx, "players")
                                                                                        ))))))
                                                        .then(Commands.literal("name")
                                                                .then(Commands.argument("right", StringArgumentType.string())
                                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "right"))
                                                                        ))
                                                                        .then(Commands.literal("spectator")
                                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                        .executes(ctx -> duelStartSameOwnerWithSpectators(
                                                                                                ctx.getSource(),
                                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "left")),
                                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "right")),
                                                                                                StringArgumentType.getString(ctx, "players")
                                                                                        ))))))))
                                        .then(Commands.literal("name")
                                                .then(Commands.argument("left", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                        .then(Commands.literal("all")
                                                                .executes(ctx -> duelStartSameOwner(
                                                                        ctx.getSource(),
                                                                        DuelSelection.single(StringArgumentType.getString(ctx, "left")),
                                                                        DuelSelection.all()
                                                                ))
                                                                .then(Commands.literal("spectator")
                                                                        .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                .executes(ctx -> duelStartSameOwnerWithSpectators(
                                                                                        ctx.getSource(),
                                                                                        DuelSelection.single(StringArgumentType.getString(ctx, "left")),
                                                                                        DuelSelection.all(),
                                                                                        StringArgumentType.getString(ctx, "players")
                                                                                )))))
                                                        .then(Commands.literal("group")
                                                                .then(Commands.argument("right", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "right"))
                                                                        ))
                                                                        .then(Commands.literal("spectator")
                                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                        .executes(ctx -> duelStartSameOwnerWithSpectators(
                                                                                                ctx.getSource(),
                                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "left")),
                                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "right")),
                                                                                                StringArgumentType.getString(ctx, "players")
                                                                                        ))))))
                                                        .then(Commands.literal("type")
                                                                .then(Commands.argument("right", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "right"))
                                                                        ))
                                                                        .then(Commands.literal("spectator")
                                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                        .executes(ctx -> duelStartSameOwnerWithSpectators(
                                                                                                ctx.getSource(),
                                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "left")),
                                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "right")),
                                                                                                StringArgumentType.getString(ctx, "players")
                                                                                        ))))))
                                                        .then(Commands.literal("name")
                                                                .then(Commands.argument("right", StringArgumentType.string())
                                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                                        .executes(ctx -> duelStartSameOwner(
                                                                                ctx.getSource(),
                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "left")),
                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "right"))
                                                                        ))
                                                                        .then(Commands.literal("spectator")
                                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                        .executes(ctx -> duelStartSameOwnerWithSpectators(
                                                                                                ctx.getSource(),
                                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "left")),
                                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "right")),
                                                                                                StringArgumentType.getString(ctx, "players")
                                                                                        )))))))))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("group", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .executes(ctx -> duelAcceptQuick(
                                                        ctx.getSource(),
                                                        DuelSelection.group(StringArgumentType.getString(ctx, "group"))
                                                ))
                                                .then(Commands.literal("spectator")
                                                        .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                .executes(ctx -> duelAcceptQuickWithSpectators(
                                                                        ctx.getSource(),
                                                                        DuelSelection.group(StringArgumentType.getString(ctx, "group")),
                                                                        StringArgumentType.getString(ctx, "players")
                                                                ))))
                                                .then(Commands.argument("player", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                                        .executes(ctx -> duelInviteSelection(
                                                                ctx.getSource(),
                                                                DuelSelection.group(StringArgumentType.getString(ctx, "group")),
                                                                StringArgumentType.getString(ctx, "player")
                                                        ))
                                                        .then(Commands.literal("spectator")
                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                        .executes(ctx -> duelInviteSelectionWithSpectators(
                                                                                ctx.getSource(),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "group")),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "players")
                                                                        )))))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .executes(ctx -> duelAcceptQuick(
                                                        ctx.getSource(),
                                                        DuelSelection.type(StringArgumentType.getString(ctx, "type"))
                                                ))
                                                .then(Commands.literal("spectator")
                                                        .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                .executes(ctx -> duelAcceptQuickWithSpectators(
                                                                        ctx.getSource(),
                                                                        DuelSelection.type(StringArgumentType.getString(ctx, "type")),
                                                                        StringArgumentType.getString(ctx, "players")
                                                                ))))
                                                .then(Commands.argument("player", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                                        .executes(ctx -> duelInviteSelection(
                                                                ctx.getSource(),
                                                                DuelSelection.type(StringArgumentType.getString(ctx, "type")),
                                                                StringArgumentType.getString(ctx, "player")
                                                        ))
                                                        .then(Commands.literal("spectator")
                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                        .executes(ctx -> duelInviteSelectionWithSpectators(
                                                                                ctx.getSource(),
                                                                                DuelSelection.type(StringArgumentType.getString(ctx, "type")),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "players")
                                                                        )))))))
                                .then(Commands.literal("all")
                                        .executes(ctx -> duelAcceptQuick(ctx.getSource(), DuelSelection.all()))
                                        .then(Commands.literal("spectator")
                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                        .executes(ctx -> duelAcceptQuickWithSpectators(
                                                                ctx.getSource(),
                                                                DuelSelection.all(),
                                                                StringArgumentType.getString(ctx, "players")
                                                        ))))
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                                .executes(ctx -> duelInviteSelection(
                                                        ctx.getSource(),
                                                        DuelSelection.all(),
                                                        StringArgumentType.getString(ctx, "player")
                                                ))
                                                .then(Commands.literal("spectator")
                                                        .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                .executes(ctx -> duelInviteSelectionWithSpectators(
                                                                        ctx.getSource(),
                                                                        DuelSelection.all(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        StringArgumentType.getString(ctx, "players")
                                                                ))))))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                        .executes(ctx -> duelAcceptQuick(
                                                ctx.getSource(),
                                                DuelSelection.single(StringArgumentType.getString(ctx, "name"))
                                        ))
                                        .then(Commands.literal("spectator")
                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                        .executes(ctx -> duelAcceptQuickWithSpectators(
                                                                ctx.getSource(),
                                                                DuelSelection.single(StringArgumentType.getString(ctx, "name")),
                                                                StringArgumentType.getString(ctx, "players")
                                                        ))))
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                                .executes(ctx -> duelInviteSelection(
                                                        ctx.getSource(),
                                                        DuelSelection.single(StringArgumentType.getString(ctx, "name")),
                                                        StringArgumentType.getString(ctx, "player")
                                                ))
                                                .then(Commands.literal("spectator")
                                                        .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                .executes(ctx -> duelInviteSelectionWithSpectators(
                                                                        ctx.getSource(),
                                                                        DuelSelection.single(StringArgumentType.getString(ctx, "name")),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        StringArgumentType.getString(ctx, "players")
                                                                ))))))
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
                                        .then(Commands.argument("spec", StringArgumentType.greedyString())
                                                .executes(ctx -> duelAcceptCompact(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "spec")
                                                )))
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestIncomingDuelChallengers(ctx.getSource(), b))
                                                .then(Commands.literal("group")
                                                        .then(Commands.argument("group", StringArgumentType.word())
                                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                                .executes(ctx -> duelAcceptSelection(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        DuelSelection.group(StringArgumentType.getString(ctx, "group"))
                                                                ))
                                                                .then(Commands.literal("spectator")
                                                                        .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                .executes(ctx -> duelAcceptSelectionWithSpectators(
                                                                                        ctx.getSource(),
                                                                                        StringArgumentType.getString(ctx, "player"),
                                                                                        DuelSelection.group(StringArgumentType.getString(ctx, "group")),
                                                                                        StringArgumentType.getString(ctx, "players")
                                                                                ))))))
                                                .then(Commands.literal("type")
                                                        .then(Commands.argument("type", StringArgumentType.word())
                                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                                .executes(ctx -> duelAcceptSelection(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        DuelSelection.type(StringArgumentType.getString(ctx, "type"))
                                                                ))
                                                                .then(Commands.literal("spectator")
                                                                        .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                .executes(ctx -> duelAcceptSelectionWithSpectators(
                                                                                        ctx.getSource(),
                                                                                        StringArgumentType.getString(ctx, "player"),
                                                                                        DuelSelection.type(StringArgumentType.getString(ctx, "type")),
                                                                                        StringArgumentType.getString(ctx, "players")
                                                                                ))))))
                                                .then(Commands.literal("all")
                                                        .executes(ctx -> duelAcceptSelection(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "player"),
                                                                DuelSelection.all()
                                                        ))
                                                        .then(Commands.literal("spectator")
                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                        .executes(ctx -> duelAcceptSelectionWithSpectators(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                DuelSelection.all(),
                                                                                StringArgumentType.getString(ctx, "players")
                                                                        )))))
                                                .then(Commands.argument("name", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                        .executes(ctx -> duelAcceptSelection(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "player"),
                                                                DuelSelection.single(StringArgumentType.getString(ctx, "name"))
                                                        ))
                                                        .then(Commands.literal("spectator")
                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                        .executes(ctx -> duelAcceptSelectionWithSpectators(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                DuelSelection.single(StringArgumentType.getString(ctx, "name")),
                                                                                StringArgumentType.getString(ctx, "players")
                                                                        )))))
                                                .then(Commands.argument("group", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                        .executes(ctx -> duelAcceptSelection(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "player"),
                                                                DuelSelection.group(StringArgumentType.getString(ctx, "group"))
                                                        ))
                                                        .then(Commands.literal("spectator")
                                                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                        .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                        .executes(ctx -> duelAcceptSelectionWithSpectators(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                DuelSelection.group(StringArgumentType.getString(ctx, "group")),
                                                                                StringArgumentType.getString(ctx, "players")
                                                                        )))))))
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
                                        .executes(ctx -> duelInbox(ctx.getSource())))
                                .then(Commands.argument("spec", StringArgumentType.greedyString())
                                        .executes(ctx -> duelCompact(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "spec")
                                        ))))
                                .then(Commands.literal("legacyduelteam")
                                        .then(Commands.literal("invite")
                                                .then(Commands.argument("player", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOnlinePlayers(ctx.getSource(), b))
                                                        .then(Commands.argument("selection", StringArgumentType.greedyString())
                                                                .suggests((ctx, b) -> suggestTeamSelectionSpecs(ctx.getSource(), b))
                                                                .executes(ctx -> duelInviteTeam(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        StringArgumentType.getString(ctx, "selection")
                                                                ))
                                                                .then(Commands.literal("spectator")
                                                                        .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                .executes(ctx -> duelInviteTeamWithSpectators(
                                                                                        ctx.getSource(),
                                                                                        StringArgumentType.getString(ctx, "player"),
                                                                                        StringArgumentType.getString(ctx, "selection"),
                                                                                        StringArgumentType.getString(ctx, "players")
                                                                                )))))))
                                        .then(Commands.literal("accept")
                                                .then(Commands.argument("player", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestIncomingDuelChallengers(ctx.getSource(), b))
                                                        .then(Commands.argument("selection", StringArgumentType.greedyString())
                                                                .suggests((ctx, b) -> suggestTeamSelectionSpecs(ctx.getSource(), b))
                                                                .executes(ctx -> duelAcceptTeam(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        StringArgumentType.getString(ctx, "selection")
                                                                ))
                                                                .then(Commands.literal("spectator")
                                                                        .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                .executes(ctx -> duelAcceptTeamWithSpectators(
                                                                                        ctx.getSource(),
                                                                                        StringArgumentType.getString(ctx, "player"),
                                                                                        StringArgumentType.getString(ctx, "selection"),
                                                                                        StringArgumentType.getString(ctx, "players")
                                                                                )))))))
                                        .then(Commands.literal("quick")
                                                .then(Commands.argument("selection", StringArgumentType.greedyString())
                                                        .suggests((ctx, b) -> suggestTeamSelectionSpecs(ctx.getSource(), b))
                                                                .executes(ctx -> duelAcceptQuickTeam(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "selection")
                                                                ))
                                                                .then(Commands.literal("spectator")
                                                                        .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                .executes(ctx -> duelAcceptQuickTeamWithSpectators(
                                                                                        ctx.getSource(),
                                                                                        StringArgumentType.getString(ctx, "selection"),
                                                                                        StringArgumentType.getString(ctx, "players")
                                                                                ))))))
                                        .then(Commands.literal("vs")
                                                .then(Commands.argument("left", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestTeamSelectionSpecs(ctx.getSource(), b))
                                                        .then(Commands.argument("right", StringArgumentType.greedyString())
                                                                .suggests((ctx, b) -> suggestTeamSelectionSpecs(ctx.getSource(), b))
                                                                .executes(ctx -> duelStartSameOwnerTeam(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "left"),
                                                                        StringArgumentType.getString(ctx, "right")
                                                                ))
                                                                .then(Commands.literal("spectator")
                                                                        .then(Commands.argument("players", StringArgumentType.greedyString())
                                                                                .suggests((ctx, b) -> suggestDuelSpectators(ctx.getSource(), b))
                                                                                .executes(ctx -> duelStartSameOwnerTeamWithSpectators(
                                                                                        ctx.getSource(),
                                                                                        StringArgumentType.getString(ctx, "left"),
                                                                                        StringArgumentType.getString(ctx, "right"),
                                                                                        StringArgumentType.getString(ctx, "players")
                                                                                )))))))
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

                        .then(buildLegacyDuelCommand())
                        .then(Commands.literal("duel")
                                .then(Commands.literal("accept")
                                        .then(Commands.argument("spec", StringArgumentType.greedyString())
                                                .suggests((ctx, b) -> suggestCompactDuelAcceptSpec(ctx.getSource(), b))
                                                .executes(ctx -> duelAcceptCompact(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "spec")
                                                ))))
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
                                        .executes(ctx -> duelInbox(ctx.getSource())))
                                .then(Commands.argument("spec", StringArgumentType.greedyString())
                                        .suggests((ctx, b) -> suggestCompactDuelSpec(ctx.getSource(), b))
                                        .executes(ctx -> duelCompact(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "spec")
                                        ))))
                        .then(Commands.literal("info")
                                .executes(ctx -> infoOverview(ctx.getSource()))
                                .then(Commands.literal("tool")
                                        .then(Commands.literal("guardian")
                                                .executes(ctx -> infoDetail(ctx.getSource(), "tool guardian")))
                                        .then(Commands.literal("bone")
                                                .executes(ctx -> infoDetail(ctx.getSource(), "tool bone"))))
                                .then(Commands.literal("attribute")
                                        .executes(ctx -> infoDetail(ctx.getSource(), "attribute"))
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestAttributes(b))
                                                .executes(ctx -> infoAttribute(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name")
                                                ))
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> infoAttribute(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "name"),
                                                                IntegerArgumentType.getInteger(ctx, "level")
                                                        )))))
                                .then(Commands.literal("ability")
                                        .executes(ctx -> infoDetail(ctx.getSource(), "ability"))
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestAbilities(b))
                                                .executes(ctx -> infoAbility(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name")
                                                ))
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> infoAbility(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "name"),
                                                                IntegerArgumentType.getInteger(ctx, "level")
                                                        )))))
                                .then(Commands.literal("class")
                                        .executes(ctx -> infoDetail(ctx.getSource(), "class"))
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestClasses(b))
                                                .executes(ctx -> infoClass(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name")
                                                ))))
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .suggests((ctx, b) -> suggestInfoTopics(b))
                                        .executes(ctx -> infoDetail(ctx.getSource(), StringArgumentType.getString(ctx, "command")))))

                        .then(Commands.literal("leaderboard")
                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", true, null, null, 10))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", true, null, null, IntegerArgumentType.getInteger(ctx, "limit"))))
                                .then(Commands.literal("all")
                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", true, null, null, Integer.MAX_VALUE)))
                                .then(Commands.literal("everytame")
                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", true, null, null, Integer.MAX_VALUE)))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, StringArgumentType.getString(ctx, "name"), null, 10))
                                                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, StringArgumentType.getString(ctx, "name"), null, IntegerArgumentType.getInteger(ctx, "limit"))))
                                                .then(Commands.literal("all")
                                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, StringArgumentType.getString(ctx, "name"), null, Integer.MAX_VALUE)))
                                                .then(Commands.literal("everytame")
                                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, StringArgumentType.getString(ctx, "name"), null, Integer.MAX_VALUE)))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestLeaderboardTameTypes(b))
                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", true, null, StringArgumentType.getString(ctx, "name"), 10))
                                                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", true, null, StringArgumentType.getString(ctx, "name"), IntegerArgumentType.getInteger(ctx, "limit"))))
                                                .then(Commands.literal("all")
                                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", true, null, StringArgumentType.getString(ctx, "name"), Integer.MAX_VALUE)))
                                                .then(Commands.literal("everytame")
                                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", true, null, StringArgumentType.getString(ctx, "name"), Integer.MAX_VALUE)))
                                                .then(Commands.literal("owned")
                                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, StringArgumentType.getString(ctx, "name"), 10))
                                                        .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, StringArgumentType.getString(ctx, "name"), IntegerArgumentType.getInteger(ctx, "limit"))))
                                                        .then(Commands.literal("all")
                                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, StringArgumentType.getString(ctx, "name"), Integer.MAX_VALUE)))
                                                        .then(Commands.literal("everytame")
                                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, StringArgumentType.getString(ctx, "name"), Integer.MAX_VALUE))))))
                                .then(Commands.literal("owned")
                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, null, 10))
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, null, IntegerArgumentType.getInteger(ctx, "limit"))))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, null, Integer.MAX_VALUE)))
                                        .then(Commands.literal("everytame")
                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, null, Integer.MAX_VALUE)))
                                        .then(Commands.literal("type")
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                        .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, StringArgumentType.getString(ctx, "name"), 10))
                                                        .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, StringArgumentType.getString(ctx, "name"), IntegerArgumentType.getInteger(ctx, "limit"))))
                                                        .then(Commands.literal("all")
                                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, StringArgumentType.getString(ctx, "name"), Integer.MAX_VALUE)))
                                                        .then(Commands.literal("everytame")
                                                                .executes(ctx -> leaderboard(ctx.getSource(), "mix", false, null, StringArgumentType.getString(ctx, "name"), Integer.MAX_VALUE)))))
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestLeaderboardModes(b))
                                                .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), false, null, null, 10))
                                                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), false, null, null, IntegerArgumentType.getInteger(ctx, "limit"))))
                                                .then(Commands.literal("all")
                                                        .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), false, null, null, Integer.MAX_VALUE)))
                                                .then(Commands.literal("everytame")
                                                        .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), false, null, null, Integer.MAX_VALUE)))))
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((ctx, b) -> suggestLeaderboardModes(b))
                                        .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), true, null, null, 10))
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), true, null, null, IntegerArgumentType.getInteger(ctx, "limit"))))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), true, null, null, Integer.MAX_VALUE)))
                                        .then(Commands.literal("everytame")
                                                .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), true, null, null, Integer.MAX_VALUE)))
                                        .then(Commands.literal("owned")
                                                .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), false, null, null, 10))
                                                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), false, null, null, IntegerArgumentType.getInteger(ctx, "limit"))))
                                                .then(Commands.literal("all")
                                                        .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), false, null, null, Integer.MAX_VALUE)))
                                                .then(Commands.literal("everytame")
                                                        .executes(ctx -> leaderboard(ctx.getSource(), StringArgumentType.getString(ctx, "type"), false, null, null, Integer.MAX_VALUE))))))

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
                                        .executes(ctx -> setPetMovementState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.WANDER))
                                        .then(Commands.literal("lock")
                                                .executes(ctx -> setPetWanderLock(ctx.getSource(), StringArgumentType.getString(ctx, "name"), true))
                                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                        .executes(ctx -> setPetWanderLock(ctx.getSource(), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "enabled")))))
                                        .then(Commands.literal("unlock")
                                                .executes(ctx -> setPetWanderLock(ctx.getSource(), StringArgumentType.getString(ctx, "name"), false))))
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
                        .then(Commands.literal("guardian")
                                .then(Commands.literal("tool")
                                        .then(Commands.literal("confirm")
                                                .executes(ctx -> guardianToolConfirm(ctx.getSource())))
                                        .then(Commands.literal("changeOrder")
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("lvl", "name", "points", "type"), b))
                                                        .executes(ctx -> guardianToolChangeOrder(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))))
                                .then(Commands.literal("list")
                                        .executes(ctx -> guardianList(ctx.getSource())))
                                .then(Commands.literal("deployGroup")
                                        .then(Commands.argument("setName", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGuardianSetNames(ctx.getSource(), b))
                                                .executes(ctx -> guardianDeploySet(ctx.getSource(), StringArgumentType.getString(ctx, "setName")))))
                                .then(Commands.literal("deploy")
                                        .then(Commands.literal("all")
                                                .executes(ctx -> guardianDeployCurrentAll(ctx.getSource())))
                                        .then(Commands.literal("group")
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                        .executes(ctx -> guardianDeployCurrentGroup(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                        .then(Commands.literal("type")
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                        .executes(ctx -> guardianDeployCurrentType(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                        .then(Commands.literal("state")
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestMovementStates(b))
                                                        .executes(ctx -> guardianDeployCurrentState(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))))
                                .then(Commands.literal("capture")
                                        .then(Commands.argument("setName", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGuardianSetNames(ctx.getSource(), b))
                                                .then(Commands.literal("all")
                                                        .executes(ctx -> guardianCaptureCurrentAll(ctx.getSource(), StringArgumentType.getString(ctx, "setName"), false))
                                                        .then(Commands.argument("includeInactiveCurrent", BoolArgumentType.bool())
                                                                .executes(ctx -> guardianCaptureCurrentAll(ctx.getSource(), StringArgumentType.getString(ctx, "setName"), BoolArgumentType.getBool(ctx, "includeInactiveCurrent")))))
                                                .then(Commands.literal("group")
                                                        .then(Commands.argument("name", StringArgumentType.word())
                                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                                .executes(ctx -> guardianCaptureCurrentGroup(ctx.getSource(), StringArgumentType.getString(ctx, "setName"), StringArgumentType.getString(ctx, "name"), false))
                                                                .then(Commands.argument("includeInactiveCurrent", BoolArgumentType.bool())
                                                                        .executes(ctx -> guardianCaptureCurrentGroup(ctx.getSource(), StringArgumentType.getString(ctx, "setName"), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "includeInactiveCurrent"))))))
                                                .then(Commands.literal("type")
                                                        .then(Commands.argument("name", StringArgumentType.word())
                                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                                .executes(ctx -> guardianCaptureCurrentType(ctx.getSource(), StringArgumentType.getString(ctx, "setName"), StringArgumentType.getString(ctx, "name"), false))
                                                                .then(Commands.argument("includeInactiveCurrent", BoolArgumentType.bool())
                                                                        .executes(ctx -> guardianCaptureCurrentType(ctx.getSource(), StringArgumentType.getString(ctx, "setName"), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "includeInactiveCurrent"))))))
                                                .then(Commands.literal("state")
                                                        .then(Commands.argument("name", StringArgumentType.word())
                                                                .suggests((ctx, b) -> suggestMovementStates(b))
                                                                .executes(ctx -> guardianCaptureCurrentState(ctx.getSource(), StringArgumentType.getString(ctx, "setName"), StringArgumentType.getString(ctx, "name"), false))
                                                                .then(Commands.argument("includeInactiveCurrent", BoolArgumentType.bool())
                                                                        .executes(ctx -> guardianCaptureCurrentState(ctx.getSource(), StringArgumentType.getString(ctx, "setName"), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "includeInactiveCurrent"))))))))
                                .then(Commands.literal("info")
                                        .then(Commands.argument("setName", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGuardianSetNames(ctx.getSource(), b))
                                                .executes(ctx -> guardianSetInfo(ctx.getSource(), StringArgumentType.getString(ctx, "setName")))))
                                .then(Commands.literal("deploymentGroup")
                                        .then(Commands.literal("delete")
                                                .then(Commands.argument("setName", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedGuardianSetNames(ctx.getSource(), b))
                                                        .executes(ctx -> guardianDeleteSet(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "setName")
                                                        ))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("setName", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedGuardianSetNames(ctx.getSource(), b))
                                                        .then(Commands.literal("all")
                                                                .executes(ctx -> guardianRemoveSetAll(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "setName")
                                                                )))
                                                        .then(Commands.literal("group")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                                        .executes(ctx -> guardianRemoveSetGroup(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "setName"),
                                                                                StringArgumentType.getString(ctx, "name")
                                                                        ))))
                                                        .then(Commands.literal("type")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                                        .executes(ctx -> guardianRemoveSetType(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "setName"),
                                                                                StringArgumentType.getString(ctx, "name")
                                                                        ))))
                                                        .then(Commands.literal("state")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestMovementStates(b))
                                                                        .executes(ctx -> guardianRemoveSetState(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "setName"),
                                                                                StringArgumentType.getString(ctx, "name")
                                                                        ))))
                                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                                .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                                .executes(ctx -> guardianRemoveSetPet(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "setName"),
                                                                        StringArgumentType.getString(ctx, "pet")
                                                                )))))
                                        .then(Commands.argument("setName", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGuardianSetNames(ctx.getSource(), b))
                                                .then(Commands.argument("pet", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                        .executes(ctx -> guardianNamedSetPet(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "setName"),
                                                                StringArgumentType.getString(ctx, "pet")
                                                        )))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("setName", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGuardianSetNames(ctx.getSource(), b))
                                                .then(Commands.argument("pet", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                        .executes(ctx -> guardianNamedSetPet(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "setName"),
                                                                StringArgumentType.getString(ctx, "pet")
                                                        ))))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> setGuardianAllLoaded(ctx.getSource()))
                                                .then(Commands.literal("home")
                                                        .executes(ctx -> setGuardianAllLoadedHome(ctx.getSource())))
                                                .then(Commands.literal("previous")
                                                        .executes(ctx -> setGuardianAllLoadedPrevious(ctx.getSource()))))
                                        .then(Commands.literal("group")
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                        .executes(ctx -> guardianGroup(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                                                        .then(Commands.literal("home")
                                                                .executes(ctx -> guardianGroupHome(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))
                                                        .then(Commands.literal("previous")
                                                                .executes(ctx -> guardianGroupPrevious(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))))
                                        .then(Commands.literal("type")
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                        .executes(ctx -> guardianType(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                                                        .then(Commands.literal("home")
                                                                .executes(ctx -> guardianTypeHome(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))
                                                        .then(Commands.literal("previous")
                                                                .executes(ctx -> guardianTypePrevious(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))))
                                        .then(Commands.literal("state")
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestMovementStates(b))
                                                        .executes(ctx -> guardianState(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                                                        .then(Commands.literal("home")
                                                                .executes(ctx -> guardianStateHome(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))
                                                        .then(Commands.literal("previous")
                                                                .executes(ctx -> guardianStatePrevious(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))))
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                .executes(ctx -> guardianPet(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                                                .then(Commands.literal("home")
                                                        .executes(ctx -> guardianPetHome(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))
                                                .then(Commands.literal("previous")
                                                        .executes(ctx -> guardianPetPrevious(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                        .executes(ctx -> guardianPet(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                                        .then(Commands.literal("home")
                                                .executes(ctx -> guardianPetHome(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))
                                        .then(Commands.literal("previous")
                                                .executes(ctx -> guardianPetPrevious(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))))
                        .then(Commands.literal("movement")
                                .then(Commands.literal("all")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestMovementProfiles(b))
                                                .executes(ctx -> allMovementProfile(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                .then(Commands.argument("pet", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestMovementProfiles(b))
                                                .executes(ctx -> setMovementProfile(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "pet"),
                                                        StringArgumentType.getString(ctx, "name")))))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("group", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestMovementProfiles(b))
                                                        .executes(ctx -> groupMovementProfile(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "group"),
                                                                StringArgumentType.getString(ctx, "name"))))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestMovementProfiles(b))
                                                        .executes(ctx -> typeMovementProfile(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "type"),
                                                                StringArgumentType.getString(ctx, "name"))))))
                                .then(Commands.literal("state")
                                        .then(Commands.argument("state", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestMovementStates(b))
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestMovementProfiles(b))
                                                        .executes(ctx -> stateMovementProfile(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "state"),
                                                                StringArgumentType.getString(ctx, "name")))))))

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
                                        .executes(ctx -> teleportPet(ctx.getSource(), StringArgumentType.getString(ctx, "name"), null))
                                        .then(Commands.literal("follow")
                                                .executes(ctx -> teleportPet(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.FOLLOW)))
                                        .then(Commands.literal("sit")
                                                .executes(ctx -> teleportPet(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.SIT)))
                                        .then(Commands.literal("wander")
                                                .executes(ctx -> teleportPet(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.WANDER)))))
                        .then(Commands.literal("tphome")
                                .then(Commands.literal("all")
                                        .executes(ctx -> teleportAllHome(ctx.getSource())))
                                .then(Commands.literal("unloaded")
                                        .executes(ctx -> teleportUnloadedHome(ctx.getSource())))
                                .then(Commands.literal("follow")
                                        .executes(ctx -> teleportByMovementStateHome(ctx.getSource(), MovementOrder.FOLLOW)))
                                .then(Commands.literal("sit")
                                        .executes(ctx -> teleportByMovementStateHome(ctx.getSource(), MovementOrder.SIT)))
                                .then(Commands.literal("wander")
                                        .executes(ctx -> teleportByMovementStateHome(ctx.getSource(), MovementOrder.WANDER)))
                                .then(Commands.literal("state")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestMovementStates(b))
                                                .executes(ctx -> teleportByStateHome(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name")
                                                ))))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .executes(ctx -> groupTpHome(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .executes(ctx -> typeTpHome(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                        .executes(ctx -> teleportPetHome(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("respawn")
                                .then(Commands.literal("waitingList")
                                        .executes(ctx -> respawnWaitingList(ctx.getSource(), 10))
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                                .executes(ctx -> respawnWaitingList(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "limit")))))
                                .then(Commands.literal("order")
                                        .executes(ctx -> respawnOrderStatus(ctx.getSource()))
                                        .then(Commands.literal("info")
                                                .executes(ctx -> respawnOrderInfo(ctx.getSource())))
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("default", "level", "leaderboard"), b))
                                                .executes(ctx -> setRespawnOrder(ctx.getSource(), StringArgumentType.getString(ctx, "mode")))))
                                .then(Commands.literal("all")
                                        .executes(ctx -> respawnAll(ctx.getSource(), ReviveMode.RESPAWN)))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .executes(ctx -> respawnGroup(ctx.getSource(), StringArgumentType.getString(ctx, "name"), ReviveMode.RESPAWN))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .executes(ctx -> respawnType(ctx.getSource(), StringArgumentType.getString(ctx, "name"), ReviveMode.RESPAWN))))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedDeadPetNames(ctx.getSource(), b))
                                        .executes(ctx -> respawnPet(ctx.getSource(), StringArgumentType.getString(ctx, "name"), ReviveMode.RESPAWN))))
                        .then(Commands.literal("arise")
                                .then(Commands.literal("all")
                                        .executes(ctx -> respawnAll(ctx.getSource(), ReviveMode.ARISE)))
                                .then(Commands.literal("group")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                .executes(ctx -> respawnGroup(ctx.getSource(), StringArgumentType.getString(ctx, "name"), ReviveMode.ARISE))))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                .executes(ctx -> respawnType(ctx.getSource(), StringArgumentType.getString(ctx, "name"), ReviveMode.ARISE))))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggestOwnedDeadPetNames(ctx.getSource(), b))
                                        .executes(ctx -> respawnPet(ctx.getSource(), StringArgumentType.getString(ctx, "name"), ReviveMode.ARISE))))
                        .then(Commands.literal("group")
                                .executes(ctx -> groupOverview(ctx.getSource()))
                                .then(Commands.literal("create")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(ctx -> groupCreate(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                        .executes(ctx -> groupTames(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                                        .then(Commands.literal("clear")
                                                .executes(ctx -> groupDelete(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))
                                        .then(Commands.literal("delete")
                                                .executes(ctx -> groupDelete(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))
                                        .then(Commands.literal("add")
                                                .then(Commands.literal("all")
                                                        .executes(ctx -> groupAssignAll(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))
                                                .then(Commands.literal("group")
                                                        .then(Commands.argument("group", StringArgumentType.word())
                                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                                .executes(ctx -> groupAssignGroup(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "group")))))
                                                .then(Commands.literal("type")
                                                        .then(Commands.argument("type", StringArgumentType.word())
                                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                                .executes(ctx -> groupAssignType(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "type")))))
                                                .then(Commands.literal("follow")
                                                        .executes(ctx -> groupAssignState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.FOLLOW)))
                                                .then(Commands.literal("sit")
                                                        .executes(ctx -> groupAssignState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.SIT)))
                                                .then(Commands.literal("wander")
                                                        .executes(ctx -> groupAssignState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.WANDER)))
                                                .then(Commands.argument("pet", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                        .executes(ctx -> groupAssignPet(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "pet")))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.literal("all")
                                                        .executes(ctx -> groupRemoveSelectionAll(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))
                                                .then(Commands.literal("group")
                                                        .then(Commands.argument("group", StringArgumentType.word())
                                                                .suggests((ctx, b) -> suggestOwnedGroups(ctx.getSource(), b))
                                                                .executes(ctx -> groupRemoveSelectionGroup(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "group")))))
                                                .then(Commands.literal("type")
                                                        .then(Commands.argument("type", StringArgumentType.word())
                                                                .suggests((ctx, b) -> suggestOwnedTypes(ctx.getSource(), b))
                                                                .executes(ctx -> groupRemoveSelectionType(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "type")))))
                                                .then(Commands.literal("follow")
                                                        .executes(ctx -> groupRemoveSelectionState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.FOLLOW)))
                                                .then(Commands.literal("sit")
                                                        .executes(ctx -> groupRemoveSelectionState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.SIT)))
                                                .then(Commands.literal("wander")
                                                        .executes(ctx -> groupRemoveSelectionState(ctx.getSource(), StringArgumentType.getString(ctx, "name"), MovementOrder.WANDER)))
                                                .then(Commands.argument("pet", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                        .executes(ctx -> groupRemoveSelectionPet(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "pet")))))))

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
                                .then(Commands.literal("levelUp")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> setDebugLevelUp(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled"))))))

                        .then(Commands.literal("admin")
                                .requires(source -> source.hasPermission(2))

                                .then(Commands.literal("resetServerProgress")
                                        .executes(ctx -> adminResetServerProgress(ctx.getSource())))
                                .then(Commands.literal("reloadTames")
                                        .executes(ctx -> adminReloadTames(ctx.getSource())))
                                .then(Commands.literal("repair")
                                        .then(Commands.literal("loaded")
                                                .executes(ctx -> adminRepairLoadedTames(ctx.getSource()))))
                                .then(Commands.literal("grantMissingMilestoneAttributes")
                                        .executes(ctx -> adminGrantMissingMilestoneAttributes(ctx.getSource())))
                                .then(Commands.literal("reloadClassWeights")
                                        .executes(ctx -> adminReloadClassWeights(ctx.getSource())))
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
                                .then(Commands.literal("callOrder")
                                        .then(Commands.literal("info")
                                                .executes(ctx -> adminCallOrderInfo(ctx.getSource())))
                                        .then(Commands.literal("invert")
                                                .then(Commands.argument("typeId", StringArgumentType.word())
                                                        .executes(ctx -> adminToggleCallOrderInvert(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "typeId")
                                                        )))))
                                .then(Commands.literal("player")
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestKnownPlayerOwners(ctx.getSource(), b))
                                                .then(Commands.literal("respawn")
                                                        .then(Commands.literal("all")
                                                                .executes(ctx -> adminPlayerRespawnAll(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        ReviveMode.RESPAWN,
                                                                        false
                                                                )))
                                                        .then(Commands.literal("group")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .executes(ctx -> adminPlayerRespawnGroup(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name"),
                                                                                ReviveMode.RESPAWN,
                                                                                false
                                                                        ))))
                                                        .then(Commands.literal("type")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .executes(ctx -> adminPlayerRespawnType(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name"),
                                                                                ReviveMode.RESPAWN,
                                                                                false
                                                                        ))))
                                                        .then(Commands.argument("name", StringArgumentType.string())
                                                                .executes(ctx -> adminPlayerRespawnPet(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        StringArgumentType.getString(ctx, "name"),
                                                                        ReviveMode.RESPAWN,
                                                                        false
                                                                ))))
                                                .then(Commands.literal("arise")
                                                        .then(Commands.literal("all")
                                                                .executes(ctx -> adminPlayerRespawnAll(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        ReviveMode.ARISE,
                                                                        false
                                                                )))
                                                        .then(Commands.literal("group")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .executes(ctx -> adminPlayerRespawnGroup(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name"),
                                                                                ReviveMode.ARISE,
                                                                                false
                                                                        ))))
                                                        .then(Commands.literal("type")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .executes(ctx -> adminPlayerRespawnType(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name"),
                                                                                ReviveMode.ARISE,
                                                                                false
                                                                        ))))
                                                        .then(Commands.argument("name", StringArgumentType.string())
                                                                .executes(ctx -> adminPlayerRespawnPet(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        StringArgumentType.getString(ctx, "name"),
                                                                        ReviveMode.ARISE,
                                                                        false
                                                                ))))
                                                .then(Commands.literal("respawnReincarnated")
                                                        .then(Commands.literal("all")
                                                                .executes(ctx -> adminPlayerRespawnAll(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        ReviveMode.RESPAWN,
                                                                        true
                                                                )))
                                                        .then(Commands.literal("group")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .executes(ctx -> adminPlayerRespawnGroup(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name"),
                                                                                ReviveMode.RESPAWN,
                                                                                true
                                                                        ))))
                                                        .then(Commands.literal("type")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .executes(ctx -> adminPlayerRespawnType(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name"),
                                                                                ReviveMode.RESPAWN,
                                                                                true
                                                                        ))))
                                                        .then(Commands.argument("name", StringArgumentType.string())
                                                                .executes(ctx -> adminPlayerRespawnPet(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        StringArgumentType.getString(ctx, "name"),
                                                                        ReviveMode.RESPAWN,
                                                                        true
                                                                ))))
                                                .then(Commands.literal("tp")
                                                        .then(Commands.literal("all")
                                                                .executes(ctx -> adminPlayerTeleportAll(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player")
                                                                )))
                                                        .then(Commands.literal("unloaded")
                                                                .executes(ctx -> adminPlayerTeleportUnloaded(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player")
                                                                )))
                                                        .then(Commands.literal("follow")
                                                                .executes(ctx -> adminPlayerTeleportByMovementState(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        MovementOrder.FOLLOW
                                                                )))
                                                        .then(Commands.literal("sit")
                                                                .executes(ctx -> adminPlayerTeleportByMovementState(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        MovementOrder.SIT
                                                                )))
                                                        .then(Commands.literal("wander")
                                                                .executes(ctx -> adminPlayerTeleportByMovementState(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        MovementOrder.WANDER
                                                                )))
                                                        .then(Commands.literal("state")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestMovementStates(b))
                                                                        .executes(ctx -> adminPlayerTeleportByState(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name")
                                                                        ))))
                                                        .then(Commands.literal("group")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestPlayerOwnedGroups(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                b
                                                                        ))
                                                                        .executes(ctx -> adminPlayerTeleportGroup(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name")
                                                                        ))))
                                                        .then(Commands.literal("type")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .suggests((ctx, b) -> suggestPlayerOwnedTypes(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                b
                                                                        ))
                                                                        .executes(ctx -> adminPlayerTeleportType(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name")
                                                                        ))))
                                                        .then(Commands.argument("name", StringArgumentType.string())
                                                                .suggests((ctx, b) -> suggestPlayerOwnedPetNames(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        b
                                                                ))
                                                                .executes(ctx -> adminPlayerTeleportPet(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        StringArgumentType.getString(ctx, "name"),
                                                                        null
                                                                ))
                                                                .then(Commands.literal("follow")
                                                                        .executes(ctx -> adminPlayerTeleportPet(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name"),
                                                                                MovementOrder.FOLLOW
                                                                        )))
                                                                .then(Commands.literal("sit")
                                                                        .executes(ctx -> adminPlayerTeleportPet(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name"),
                                                                                MovementOrder.SIT
                                                                        )))
                                                                .then(Commands.literal("wander")
                                                                        .executes(ctx -> adminPlayerTeleportPet(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name"),
                                                                                MovementOrder.WANDER
                                                                        )))))
                                                .then(Commands.literal("reincarnate")
                                                        .then(Commands.argument("name", StringArgumentType.string())
                                                                .executes(ctx -> adminPlayerReincarnatePet(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player"),
                                                                        StringArgumentType.getString(ctx, "name")
                                                                ))))
                                                .then(Commands.literal("reincarnation")
                                                        .then(Commands.literal("all")
                                                                .executes(ctx -> adminPlayerReincarnateBatchAll(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "player")
                                                                )))
                                                        .then(Commands.literal("group")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .executes(ctx -> adminPlayerReincarnateBatchGroup(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name")
                                                                        ))))
                                                        .then(Commands.literal("type")
                                                                .then(Commands.argument("name", StringArgumentType.word())
                                                                        .executes(ctx -> adminPlayerReincarnateBatchType(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "name")
                                                                        )))))))
                                .then(Commands.literal("fixStats")
                                        .executes(ctx -> adminFixLoadedTameStatsAll(ctx.getSource()))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> adminFixLoadedTameStatsAll(ctx.getSource())))
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                .executes(ctx -> adminFixLoadedTameStats(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "pet")
                                                ))))
                                .then(Commands.literal("fixStale")
                                        .then(Commands.literal("all")
                                                .executes(ctx -> adminFixStaleAll(ctx.getSource()))))
                                .then(Commands.literal("setMode")
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                .then(Commands.argument("value", IntegerArgumentType.integer())
                                                        .executes(ctx -> adminInvokeTameIntSetter(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "pet"),
                                                                "setMode",
                                                                IntegerArgumentType.getInteger(ctx, "value")
                                                        )))))
                                .then(Commands.literal("setOrder")
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                .then(Commands.argument("value", IntegerArgumentType.integer())
                                                        .executes(ctx -> adminInvokeTameIntSetter(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "pet"),
                                                                "setOrder",
                                                                IntegerArgumentType.getInteger(ctx, "value")
                                                        )))))
                                .then(Commands.literal("setFollow")
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestOwnedPetNames(ctx.getSource(), b))
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(ctx -> adminInvokeTameBooleanSetter(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "pet"),
                                                                "setFollow",
                                                                BoolArgumentType.getBool(ctx, "value")
                                                        )))))
                                .then(Commands.literal("addMissingAbilities")
                                        .executes(ctx -> adminAddMissingAbilities(ctx.getSource())))
                                .then(Commands.literal("stat")
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                .then(Commands.literal("kills")
                                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                                .executes(ctx -> adminSetTameStat(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "pet"),
                                                                        "kills",
                                                                        IntegerArgumentType.getInteger(ctx, "value")
                                                                ))))
                                                .then(Commands.literal("assists")
                                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                                .executes(ctx -> adminSetTameStat(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "pet"),
                                                                        "assists",
                                                                        IntegerArgumentType.getInteger(ctx, "value")
                                                                ))))
                                                .then(Commands.literal("deaths")
                                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                                .executes(ctx -> adminSetTameStat(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "pet"),
                                                                        "deaths",
                                                                        IntegerArgumentType.getInteger(ctx, "value")
                                                                ))))))

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
                                .then(Commands.literal("changeClass")
                                        .then(Commands.argument("pet", StringArgumentType.string())
                                                .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                .then(Commands.argument("class", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestClasses(b))
                                                        .executes(ctx -> adminRebuildPet(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "pet"),
                                                                StringArgumentType.getString(ctx, "class")
                                                        )))))
                                .then(Commands.literal("rebuild")
                                        .then(Commands.argument("class", StringArgumentType.word())
                                                .suggests((ctx, b) -> suggestClasses(b))
                                                .then(Commands.argument("pet", StringArgumentType.string())
                                                        .suggests((ctx, b) -> suggestAllAliveTameNames(b))
                                                        .executes(ctx -> adminRebuildPet(
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
                                .then(Commands.literal("postTpStabilization")
                                        .executes(ctx -> adminPostTpStabilizationStatus(ctx.getSource()))
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> adminSetPostTpStabilization(
                                                        ctx.getSource(),
                                                        BoolArgumentType.getBool(ctx, "enabled")
                                                ))))
                                .then(Commands.literal("debug")
                                        .executes(ctx -> adminDebugStatus(ctx.getSource()))
                                        .then(Commands.literal("abilityUsed")
                                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                        .executes(ctx -> adminSetDebugAbilityUsed(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                                        .then(Commands.literal("attributeUsed")
                                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                        .executes(ctx -> setDebugAttributeUsed(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                                        .then(Commands.literal("perf")
                                                .executes(ctx -> adminPerfStatus(ctx.getSource()))
                                                .then(Commands.literal("start")
                                                        .executes(ctx -> adminPerfStart(ctx.getSource())))
                                                .then(Commands.literal("stop")
                                                        .executes(ctx -> adminPerfStop(ctx.getSource())))
                                                .then(Commands.literal("reset")
                                                        .executes(ctx -> adminPerfReset(ctx.getSource())))
                                                .then(Commands.literal("report")
                                                        .executes(ctx -> adminPerfReport(ctx.getSource()))))
                                        .then(Commands.literal("damage")
                                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                        .executes(ctx -> adminSetDebugDamage(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                                        .then(Commands.literal("teleport")
                                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                        .executes(ctx -> adminSetDebugTeleport(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
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
                                        .then(Commands.literal("all")
                                                .executes(ctx -> adminRespawnAll(ctx.getSource())))
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
                                                .executes(ctx -> adminApproveHeldItem(ctx.getSource())))
                                        .then(Commands.literal("list")
                                                .executes(ctx -> adminListApprovedItems(ctx.getSource())))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("item", StringArgumentType.word())
                                                        .suggests((ctx, b) -> suggestApprovedReincarnationItems(b))
                                                        .executes(ctx -> adminRemoveApprovedItem(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "item")
                                                        )))))
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
                                .then(Commands.literal("cleanupProjectiles")
                                        .executes(ctx -> adminCleanupProjectiles(ctx.getSource(), "all"))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> adminCleanupProjectiles(ctx.getSource(), "all")))
                                        .then(Commands.literal("dragonFireball")
                                                .executes(ctx -> adminCleanupProjectiles(ctx.getSource(), "dragon_fireball"))))
                                .then(Commands.literal("dragonfly")
                                        .then(Commands.literal("fixArmor")
                                                .executes(ctx -> fixDragonflyArmor(ctx.getSource())))
                                        .then(Commands.literal("fixStats")
                                                .executes(ctx -> fixDragonflyStats(ctx.getSource())))
                                        .then(Commands.literal("rebuildFromData")
                                                .executes(ctx -> rebuildDragonfliesFromData(ctx.getSource()))))

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
        TamePerformanceProfiler.run("system.pending_immediate_chunk_tp", () -> processPendingImmediateChunkTeleports(server));
        TamePerformanceProfiler.run("system.morning_registry_sweep", () -> processMorningRegistrySweep(server));
        TamePerformanceProfiler.run("system.pending_morning_lantern_recalls", () -> processPendingMorningLanternRecalls(server));
    }

    private static void processPendingImmediateChunkTeleports(MinecraftServer server) {
        if (server == null || PENDING_IMMEDIATE_CHUNK_TELEPORTS.isEmpty()) {
            return;
        }
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        long now = overworld == null ? 0L : overworld.getGameTime();
        List<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, PendingImmediateChunkTeleport> entry : PENDING_IMMEDIATE_CHUNK_TELEPORTS.entrySet()) {
            PendingImmediateChunkTeleport pending = entry.getValue();
            if (pending == null) {
                finished.add(entry.getKey());
                continue;
            }
            if (pending.nextAttemptTick > now) {
                continue;
            }
            ServerLevel sourceLevel = server.getLevel(pending.sourceDimension);
            if (sourceLevel == null || pending.target == null || pending.target.level == null || pending.target.pos == null) {
                finished.add(entry.getKey());
                continue;
            }
            ChunkPos sourceChunk = new ChunkPos(pending.sourcePos);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    sourceLevel.getChunk(sourceChunk.x + dx, sourceChunk.z + dz);
                }
            }
            boolean chunksReady = areImmediateTeleportChunksReady(sourceLevel, pending.sourcePos);
            if (!chunksReady) {
                if ((now - pending.createdTick) >= IMMEDIATE_CHUNK_TP_MAX_WAIT_TICKS) {
                    releaseImmediateChunkTeleport(sourceLevel, pending);
                    if (pending.liveEntityOnly) {
                        if (!pending.silent) notifyImmediateChunkTeleport(server, pending.ownerUuid, "Failed to retrieve unloaded " + pending.tameName + ". Live entity cross-dimension teleport timed out.", ChatFormatting.RED);
                        finished.add(entry.getKey());
                        continue;
                    }
                    TameData data = pending.tlId != null ? TameRegistry.getByTlId(pending.tlId) : TameRegistry.get(pending.tameUuid);
                    ServerPlayer owner = pending.ownerUuid == null ? null : server.getPlayerList().getPlayer(pending.ownerUuid);
                    if (owner != null && data != null) {
                        debugTeleport(owner, "unloaded chunk timeout rebuilding " + pending.tameName + " from snapshot");
                        RecoverResult recoverResult = recoverPetEntityAtLocation(owner, pending.target, data);
                        if (recoverResult.entity != null) {
                            if (!pending.silent) notifyImmediateChunkTeleport(server, pending.ownerUuid, "Rebuilt unloaded " + pending.tameName + " from snapshot after chunk load timeout.", ChatFormatting.YELLOW);
                        } else {
                            if (!pending.silent) notifyImmediateChunkTeleport(server, pending.ownerUuid, "Failed to retrieve unloaded " + pending.tameName + ". Chunk load timed out and snapshot rebuild failed: " + recoverResult.error, ChatFormatting.RED);
                        }
                    } else {
                        if (!pending.silent) notifyImmediateChunkTeleport(server, pending.ownerUuid, "Failed to retrieve unloaded " + pending.tameName + ". Chunk load timed out.", ChatFormatting.RED);
                    }
                    finished.add(entry.getKey());
                } else {
                    pending.nextAttemptTick = now + 5L;
                }
                continue;
            }
            if (pending.chunksReadyTick < 0L) {
                pending.chunksReadyTick = now;
            }
            TamableAnimal tame = findLoadedTameByIdentity(sourceLevel, pending.tameUuid, pending.tlId);
            if (tame != null && tame.isAlive()) {
                ServerPlayer owner = pending.ownerUuid == null ? null : server.getPlayerList().getPlayer(pending.ownerUuid);
                debugTeleport(owner, "unloaded chunk path found live entity " + pending.tameName + " in " + sourceLevel.dimension().location());
                teleportTameToLocation(tame, pending.target);
                releaseImmediateChunkTeleport(sourceLevel, pending);
                if (!pending.silent) notifyImmediateChunkTeleport(server, pending.ownerUuid, "Teleported unloaded " + pending.tameName + ".", ChatFormatting.GREEN);
                finished.add(entry.getKey());
                continue;
            }
            if (pending.chunksReadyTick >= 0L && (now - pending.chunksReadyTick) >= 10L) {
                releaseImmediateChunkTeleport(sourceLevel, pending);
                if (pending.liveEntityOnly) {
                    if (!pending.silent) notifyImmediateChunkTeleport(server, pending.ownerUuid, "Failed to retrieve unloaded " + pending.tameName + " after chunk load wait. Live entity not found.", ChatFormatting.RED);
                    finished.add(entry.getKey());
                    continue;
                }
                TameData data = pending.tlId != null ? TameRegistry.getByTlId(pending.tlId) : TameRegistry.get(pending.tameUuid);
                ServerPlayer owner = pending.ownerUuid == null ? null : server.getPlayerList().getPlayer(pending.ownerUuid);
                if (owner != null && data != null) {
                    debugTeleport(owner, "unloaded wait rebuilding " + pending.tameName + " from snapshot");
                    RecoverResult recoverResult = recoverPetEntityAtLocation(owner, pending.target, data);
                    if (recoverResult.entity != null) {
                        if (!pending.silent) notifyImmediateChunkTeleport(server, pending.ownerUuid, "Rebuilt unloaded " + pending.tameName + " from snapshot.", ChatFormatting.YELLOW);
                    } else {
                        String message = "Failed to retrieve unloaded " + pending.tameName + " after chunk load wait. Snapshot rebuild failed: " + recoverResult.error;
                        if ("stored type is not tamable".equalsIgnoreCase(recoverResult.error)) {
                            message = "Horses cannot be teleported.";
                        }
                        if (!pending.silent) notifyImmediateChunkTeleport(server, pending.ownerUuid, message, ChatFormatting.RED);
                    }
                } else {
                    if (!pending.silent) notifyImmediateChunkTeleport(server, pending.ownerUuid, "Failed to retrieve unloaded " + pending.tameName + " after chunk load wait.", ChatFormatting.RED);
                }
                finished.add(entry.getKey());
                continue;
            }
            pending.nextAttemptTick = now + 5L;
        }
        for (UUID id : finished) {
            PENDING_IMMEDIATE_CHUNK_TELEPORTS.remove(id);
        }
    }

    private static void releaseImmediateChunkTeleport(ServerLevel sourceLevel, PendingImmediateChunkTeleport pending) {
        if (sourceLevel == null || pending == null || pending.ticketId == null) {
            return;
        }
        loadChunksAround(sourceLevel, pending.ticketId, pending.sourcePos, false);
    }

    private static boolean areImmediateTeleportChunksReady(ServerLevel level, BlockPos center) {
        if (level == null || center == null) {
            return false;
        }
        ChunkPos centerChunk = new ChunkPos(center);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(centerChunk.x + dx, centerChunk.z + dz);
                if (chunk == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void notifyImmediateChunkTeleport(MinecraftServer server, UUID ownerUuid, String message, ChatFormatting color) {
        if (server == null || ownerUuid == null || message == null || message.isBlank()) {
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerUuid);
        if (owner != null) {
            owner.sendSystemMessage(Component.literal(message).withStyle(color));
        }
    }

    private static void processMorningRegistrySweep(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        long dayTime = overworld.getDayTime();
        long day = dayTime / 24000L;
        if (day <= lastMorningRegistrySweepDay) {
            return;
        }
        lastMorningRegistrySweepDay = day;

        incrementOwnerActiveSurvivalDays(server, day);

        DIWorldData worldData = DIWorldData.get(overworld);
        if (worldData != null) {
            worldData.clearAllRespawnRequests();
            worldData.clearLanternRequestsByMode(LanternRequest.MODE_LANTERN);
            worldData.clearLanternRequestsByMode(LanternRequest.MODE_PLAYER_TP);
            worldData.clearLanternRequestsByMode(LanternRequest.MODE_FIXED_TARGET_TP);
        }
        processMorningPetBedRespawns(server);
        processMorningGuardianWanderLocks(server);
        scheduleMorningWaywardLanternRecalls(server, overworld.getGameTime());
    }

    private static void incrementOwnerActiveSurvivalDays(MinecraftServer server, long day) {
        boolean changed = false;
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.dead || data.ownerUUID == null) {
                continue;
            }
            if (data.lastActiveSurvivalDay == day) {
                continue;
            }
            if (server.getPlayerList().getPlayer(data.ownerUUID) == null) {
                continue;
            }
            data.activeSurvivalDays = Math.max(0, data.activeSurvivalDays) + 1;
            data.lastActiveSurvivalDay = day;
            changed = true;
        }
        if (changed) {
            TameRegistry.markDirty();
        }
    }

    private static void processMorningPetBedRespawns(MinecraftServer server) {
        Set<UUID> owners = new LinkedHashSet<>();
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data != null && data.dead && data.ownerUUID != null) {
                owners.add(data.ownerUUID);
            }
        }
        for (UUID ownerUuid : owners) {
            for (TameData data : coloredMorningRespawnCandidates(server, ownerUuid)) {
                SpawnTarget target = resolveMorningRespawnTarget(server, data);
                if (target == null) {
                    continue;
                }
                RespawnResult result = respawnDeadTameAtServer(data, target.level, target.pos, target.yRot, target.xRot);
                if (!result.success) {
                    continue;
                }
                clearMatchingDiBedRespawnRequests(server, data);
                ServerPlayer owner = server.getPlayerList().getPlayer(ownerUuid);
                TamableAnimal respawned = findLoadedTameByUuid(server, data.uuid);
                if (owner != null && respawned != null) {
                    owner.displayClientMessage(
                            Component.translatable("message.domesticationinnovation.respawn", respawned.getName())
                                    .append(Component.literal(" " + respawnProgressSuffix(data))),
                            false
                    );
                }
            }
            for (TameData data : whiteMorningRespawnCandidates(server, ownerUuid)) {
                SpawnTarget target = resolveMorningRespawnTarget(server, data);
                if (target == null) {
                    continue;
                }
                RespawnResult result = respawnDeadTameAtServer(data, target.level, target.pos, target.yRot, target.xRot);
                if (!result.success) {
                    continue;
                }
                clearMatchingDiBedRespawnRequests(server, data);
                ServerPlayer owner = server.getPlayerList().getPlayer(ownerUuid);
                TamableAnimal respawned = findLoadedTameByUuid(server, data.uuid);
                if (owner != null && respawned != null) {
                    owner.displayClientMessage(
                            Component.translatable("message.domesticationinnovation.respawn", respawned.getName())
                                    .append(Component.literal(" " + respawnProgressSuffix(data))),
                            false
                    );
                }
                break;
            }
        }
    }

    private static void processMorningGuardianWanderLocks(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.uuid == null || data.dead || !data.hasHome || !data.wanderLock) {
                continue;
            }
            SpawnTarget target = spawnTargetFromGuardianHome(server, data);
            if (target == null) {
                continue;
            }
            TamableAnimal loaded = findLoadedTameByIdentity(server, data.uuid, data.tlId);
            if (loaded != null) {
                if (loaded.getTarget() != null && loaded.getTarget().isAlive()) {
                    continue;
                }
                teleportTameToLocation(loaded, target);
                continue;
            }
            ServerPlayer owner = data.ownerUUID == null ? null : server.getPlayerList().getPlayer(data.ownerUUID);
            if (owner == null) {
                continue;
            }
            tryImmediateChunkLoadTeleportSilent(owner.createCommandSourceStack(), owner, data, target);
        }
    }

    private static SpawnTarget spawnTargetFromGuardianHome(MinecraftServer server, TameData data) {
        if (server == null || data == null || !data.hasHome || data.homeDimension == null || data.homeDimension.isBlank()) {
            return null;
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(data.homeDimension));
        ServerLevel level = server.getLevel(key);
        if (level == null) {
            return null;
        }
        return new SpawnTarget(level, new Vec3(data.homeX + 0.5D, data.homeY, data.homeZ + 0.5D), 0.0F, 0.0F);
    }

    private static List<TameData> morningRespawnCandidates(MinecraftServer server, UUID ownerUuid) {
        return morningRespawnCandidates(server, ownerUuid, TameCommands::isWhitePetBedBlock);
    }

    private static List<TameData> whiteMorningRespawnCandidates(MinecraftServer server, UUID ownerUuid) {
        return morningRespawnCandidates(server, ownerUuid, TameCommands::isWhitePetBedBlock);
    }

    private static List<TameData> coloredMorningRespawnCandidates(MinecraftServer server, UUID ownerUuid) {
        return morningRespawnCandidates(server, ownerUuid, TameCommands::isColoredPetBedBlock);
    }

    private static List<TameData> morningRespawnCandidates(MinecraftServer server, UUID ownerUuid, Predicate<ResourceLocation> bedFilter) {
        List<TameData> dead = new ArrayList<>();
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || !data.dead || data.uuid == null) {
                continue;
            }
            if (ownerUuid != null && !ownerUuid.equals(data.ownerUUID)) {
                continue;
            }
            if (findLoadedTameByIdentity(server, data.uuid, data.tlId) != null) {
                continue;
            }
            if (!hasEligibleAutomaticRespawnBed(server, data, bedFilter)) {
                continue;
            }
            dead.add(data);
        }
        dead.sort(respawnQueueComparator(ownerUuid));
        return dead;
    }

    private static Comparator<TameData> respawnQueueComparator(UUID ownerUuid) {
        return switch (RespawnOrderMode.parse(TameRegistry.getRespawnOrder(ownerUuid))) {
            case LEVEL -> Comparator
                    .comparingInt((TameData data) -> data == null ? 0 : data.level)
                    .reversed()
                    .thenComparingLong(data -> data == null ? Long.MAX_VALUE : data.deadGameTime)
                    .thenComparing(data -> data == null || data.name == null ? "" : data.name.toLowerCase(Locale.ROOT));
            case LEADERBOARD -> Comparator
                    .comparingInt((TameData data) -> weightedCombatScore(data))
                    .reversed()
                    .thenComparingInt((TameData data) -> data == null ? 0 : data.level)
                    .reversed()
                    .thenComparingLong(data -> data == null ? Long.MAX_VALUE : data.deadGameTime)
                    .thenComparing(data -> data == null || data.name == null ? "" : data.name.toLowerCase(Locale.ROOT));
            case DEFAULT -> Comparator
                    .comparingLong((TameData data) -> data == null ? Long.MAX_VALUE : data.deadGameTime)
                    .thenComparingLong(data -> data == null ? Long.MAX_VALUE : data.deadUnixMillis)
                    .thenComparing(data -> data == null || data.name == null ? "" : data.name.toLowerCase(Locale.ROOT));
        };
    }

    private static String respawnOrderLabel(UUID ownerUuid) {
        return RespawnOrderMode.parse(TameRegistry.getRespawnOrder(ownerUuid)).id;
    }

    private static int reviveXpCost(TameData data, ReviveMode mode) {
        int baseCost = Math.max(0, LevelSystem.estimateInvestedXp(data));
        if (mode == ReviveMode.ARISE) {
            return Math.max(1, baseCost * 2);
        }
        return baseCost;
    }

    private static int reviveApprovedItemCost(TameData data, ReviveMode mode) {
        if (mode == ReviveMode.RESPAWN
                && data != null
                && data.hasPetBed
                && data.petBedDimension != null
                && !data.petBedDimension.isBlank()) {
            return 1;
        }
        int level = Math.max(1, data == null ? 1 : data.level);
        double divisor = mode == ReviveMode.ARISE ? 10.0D : 20.0D;
        return Math.max(1, (int) Math.ceil(level / divisor));
    }

    private static int highestRecordedLevel(TameData data) {
        if (data == null) {
            return 1;
        }
        return Math.max(Math.max(1, data.level), data.hasSavedProgress ? Math.max(1, data.savedLevel) : 1);
    }

    private static String respawnProgressSuffix(TameData data) {
        int reincarnationCost = data != null && data.hasSavedProgress ? LevelSystem.reincarnationXpCost(data) : 0;
        return "(" + reincarnationCost + " -> [" + highestRecordedLevel(data) + "])";
    }

    private static boolean isReincarnationEligible(TameData data) {
        return data != null && data.hasSavedProgress && data.level < highestRecordedLevel(data);
    }

    private static SpawnTarget resolveMorningRespawnTarget(MinecraftServer server, TameData data) {
        if (server == null || data == null) {
            return null;
        }
        return resolveAutomaticRespawnBedTarget(server, data);
    }

    private static boolean hasEligibleAutomaticRespawnBed(MinecraftServer server, TameData data) {
        return hasEligibleAutomaticRespawnBed(server, data, blockId -> blockId != null && blockId.getPath().startsWith("pet_bed_"));
    }

    private static boolean hasEligibleAutomaticRespawnBed(MinecraftServer server, TameData data, Predicate<ResourceLocation> bedFilter) {
        return resolveAutomaticRespawnBedTarget(server, data, bedFilter) != null;
    }

    private static SpawnTarget resolveAutomaticRespawnBedTarget(MinecraftServer server, TameData data) {
        return resolveAutomaticRespawnBedTarget(server, data, blockId -> blockId != null && blockId.getPath().startsWith("pet_bed_"));
    }

    private static SpawnTarget resolveAutomaticRespawnBedTarget(MinecraftServer server, TameData data, Predicate<ResourceLocation> bedFilter) {
        if (server == null || data == null || !data.hasPetBed || data.petBedDimension == null || data.petBedDimension.isBlank()) {
            return null;
        }
        ResourceLocation bedDimId = ResourceLocation.tryParse(data.petBedDimension);
        if (bedDimId == null) {
            return null;
        }
        ServerLevel bedLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, bedDimId));
        if (bedLevel == null) {
            return null;
        }
        BlockPos bedPos = new BlockPos(data.petBedX, data.petBedY, data.petBedZ);
        bedLevel.getChunk(bedPos);
        if (!(bedLevel.getBlockEntity(bedPos) instanceof com.github.alexthe668.domesticationinnovation.server.block.PetBedBlockEntity)) {
            return null;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(bedLevel.getBlockState(bedPos).getBlock());
        if (bedFilter == null || !bedFilter.test(blockId)) {
            return null;
        }
        Direction facing = bedLevel.getBlockState(bedPos).hasProperty(com.github.alexthe668.domesticationinnovation.server.block.PetBedBlock.FACING)
                ? bedLevel.getBlockState(bedPos).getValue(com.github.alexthe668.domesticationinnovation.server.block.PetBedBlock.FACING)
                : Direction.NORTH;
        return new SpawnTarget(bedLevel, Vec3.upFromBottomCenterOf(bedPos, 0.8F), yawFromDirection(facing), 0.0F);
    }

    private static boolean isColoredPetBedBlock(ResourceLocation blockId) {
        if (blockId == null || !"domesticationinnovation".equals(blockId.getNamespace())) {
            return false;
        }
        return blockId.getPath().startsWith("pet_bed_") && !"pet_bed_white".equals(blockId.getPath());
    }

    private static boolean isWhitePetBedBlock(ResourceLocation blockId) {
        return blockId != null
                && "domesticationinnovation".equals(blockId.getNamespace())
                && "pet_bed_white".equals(blockId.getPath());
    }

    private static boolean isBlackPetBedBlock(ResourceLocation blockId) {
        return blockId != null
                && "domesticationinnovation".equals(blockId.getNamespace())
                && "pet_bed_black".equals(blockId.getPath());
    }

    private static boolean hasBlackAutoReincarnationBed(MinecraftServer server, TameData data) {
        if (server == null || data == null || !data.hasPetBed || data.petBedDimension == null || data.petBedDimension.isBlank()) {
            return false;
        }
        ResourceLocation bedDimId = ResourceLocation.tryParse(data.petBedDimension);
        if (bedDimId == null) {
            return false;
        }
        ServerLevel bedLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, bedDimId));
        if (bedLevel == null) {
            return false;
        }
        BlockPos bedPos = new BlockPos(data.petBedX, data.petBedY, data.petBedZ);
        bedLevel.getChunk(bedPos);
        if (!(bedLevel.getBlockEntity(bedPos) instanceof com.github.alexthe668.domesticationinnovation.server.block.PetBedBlockEntity)) {
            return false;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(bedLevel.getBlockState(bedPos).getBlock());
        return isBlackPetBedBlock(blockId);
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
            if (request == null || (!request.isPlayerTeleportMode() && !request.isFixedTargetTeleportMode())) continue;

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
                owner.sendSystemMessage(Component.literal(successMessageForLanternRequest(request)).withStyle(ChatFormatting.GREEN));
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
                    owner.sendSystemMessage(Component.literal(timeoutRecoveryMessageForLanternRequest(request)).withStyle(ChatFormatting.YELLOW));
                } else {
                    String detail = recoverError == null || recoverError.isBlank() ? "" : " Recover failed: " + recoverError + ".";
                    owner.sendSystemMessage(Component.literal("Failed to tp unloaded " + request.getNametag() + " (entity load timeout)." + detail).withStyle(ChatFormatting.RED));
                }
                worldData.removeLanternRequest(request);
                loadChunksAround(sourceLevel, request.getPetUUID(), request.getChunkPosition(), false);
            }
        }
    }

    private static String successMessageForLanternRequest(LanternRequest request) {
        if (request != null && request.isFixedTargetTeleportMode()) {
            return "Teleported unloaded " + request.getNametag() + " to its respawn home.";
        }
        return "Teleported unloaded " + (request == null ? "unknown" : request.getNametag()) + " to your position.";
    }

    private static String timeoutRecoveryMessageForLanternRequest(LanternRequest request) {
        if (request != null && request.isFixedTargetTeleportMode()) {
            return "Recovered unloaded " + request.getNametag() + " at its respawn home after entity load timeout.";
        }
        return "Respawned unloaded " + (request == null ? "unknown" : request.getNametag()) + " at your position after entity load timeout.";
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
                    + inactiveSuffix(d);
            p.sendSystemMessage(Component.literal(line).withStyle(statusColor(d, isLoadedAnywhere(source.getServer(), d.uuid))));
        }
        return 1;
    }

    private static int strongest(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        TameData best = null;
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            if (isInactiveEntry(d.uuid)) continue;
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
        List<TameData> stored = new ArrayList<>();
        List<TameData> dead = new ArrayList<>();
        for (TameData d : tames) {
            if (d.stored) {
                stored.add(d);
            } else if (isDeadEntry(d.uuid)) {
                dead.add(d);
            } else if (isLoadedAnywhere(source.getServer(), d.uuid)) {
                loaded.add(d);
            } else {
                unloaded.add(d);
            }
        }

        loaded.sort(Comparator.comparing(d -> d.name.toLowerCase(Locale.ROOT)));
        unloaded.sort(Comparator.comparing(d -> d.name.toLowerCase(Locale.ROOT)));
        stored.sort(Comparator.comparing(d -> d.name.toLowerCase(Locale.ROOT)));
        dead.sort(Comparator.comparing(d -> d.name.toLowerCase(Locale.ROOT)));

        p.sendSystemMessage(Component.literal("---- Tame Load Status ----").withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.literal("Loaded (" + loaded.size() + "):").withStyle(ChatFormatting.GREEN));
        for (TameData d : loaded) {
            p.sendSystemMessage(Component.literal("- [" + d.level + "] " + d.name).withStyle(ChatFormatting.GREEN));
        }
        p.sendSystemMessage(Component.literal("Stored (" + stored.size() + "):").withStyle(ChatFormatting.LIGHT_PURPLE));
        for (TameData d : stored) {
            String where = (d.lastKnownDimension == null || d.lastKnownDimension.isBlank())
                    ? "unknown"
                    : (d.lastKnownDimension + " @ " + d.lastKnownX + " " + d.lastKnownY + " " + d.lastKnownZ);
            p.sendSystemMessage(Component.literal("- [" + d.level + "] " + d.name + " (" + where + ")").withStyle(ChatFormatting.LIGHT_PURPLE));
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
        p.sendSystemMessage(Component.literal("/tame is an alias for /tames").withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.literal("Use /tames info <topic> for the live mechanic page.").withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.literal("Topics: stat, inspect, search, leaderboard, group, mode, follow, sit, wander, guardian, guardian_arrow, call_stick, tool guardian, tool bone, movement, tp, tphome, bed, respawn, arise, graveyard, reincarnate, healthSiphon, enterPortalsByThemselves, duel, debug, attribute, ability, class").withStyle(ChatFormatting.GRAY));
        p.sendSystemMessage(Component.literal("Examples: /tames info guardian, /tames info tool guardian, /tames info ability arrow_shot 5, /tames info attribute tethered_teleport 1, /tames info class dps").withStyle(ChatFormatting.DARK_AQUA));
        p.sendSystemMessage(Component.literal("/tames berserk|passive"));
        return 1;
    }

    private static int infoDetail(CommandSourceStack source, String topic) {
        ServerPlayer p = source.getPlayer();
        String key = topic.trim().toLowerCase(Locale.ROOT);
        if (key.equals("leaderboard")) {
            sendInfoPage(p, "Leaderboard",
                    "/tames leaderboard [mix|kills|deaths|assists|lvl|days] [<number>|all|everytame]",
                    "/tames leaderboard type <typeName> [<number>|all|everytame]",
                    "/tames leaderboard owned [mix|kills|deaths|assists|lvl|days|type <typeName>] [<number>|all|everytame]",
                    "Modes sort by weighted combat score, kills, deaths, assists, level, or days since last death.",
                    "Leaderboard shows all owners by default, but only includes tames with invested XP above 0. Use 'owned' or 'type <typeName>' to filter it.",
                    "'days' means days since last death, or born day if the tame never died."
            );
        }
        else if (key.equals("deaths")) {
            sendInfoPage(p, "Deaths",
                    "/tames deaths <number>",
                    "Shows recent recorded death entries from the registry."
            );
        }
        else if (key.equals("loaded")) {
            sendInfoPage(p, "Loaded",
                    "/tames loaded",
                    "Shows your loaded, unloaded, and dead registry entries."
            );
        }
        else if (key.equals("stat") || key.equals("stats")) {
            sendInfoPage(p, "Stat",
                    "/tames stat <pet>",
                    "/tames stats <pet>",
                    "Shows the tame sheet: level, class, record, days, active survival days, bonuses, abilities, and attributes.",
                    "Bed info is shown as: BedType Dimension [x, y, z]."
            );
        }
        else if (key.equals("group")) {
            sendInfoPage(p, "Group",
                    "/tames group <name>",
                    "/tames group create <group>",
                    "/tames group <group> add <pet|group <name>|type <name>|follow|sit|wander|all>",
                    "/tames group <group> remove <pet|group <name>|type <name>|follow|sit|wander|all>",
                    "/tames group <group> clear",
                    "Groups are owner-local labels used by selectors in movement, guardian, tp, respawn, arise, duel, and leaderboard filters."
            );
        }
        else if (key.equals("mode")) {
            sendInfoPage(p, "Mode",
                    "/tames mode <pet> <mode>",
                    "/tames mode <all|group|type|state> ... <mode>",
                    "Current modes: default, default_plus, bodyguard, boss, monster_hunter, arena, aggressive, passive.",
                    "Modes control combat retargeting. They are separate from movement state and movement profile."
            );
        }
        else if (key.equals("follow") || key.equals("sit") || key.equals("wander")) {
            sendInfoPage(p, capitalizeAscii(key),
                    "/tames " + key + " [<name>|all|group <group>|type <type>|state <follow|wander|sit>]",
                    "These are the main movement-state selectors for batch commands.",
                    "Changing a tame to follow, sit, or wander clears its current guardian anchor.",
                    "Use /tames wander <name> lock [true|false] for modded tames that ignore normal follow/sit/wander state but should not auto-teleport back to the owner when unloaded."
            );
        }
        else if (key.equals("guardian")) {
            sendInfoPage(p, "Guardian",
                    "/tames guardian <name>",
                    "/tames guardian set <all|group <group>|type <type>|state <follow|wander|sit>|name>",
                    "/tames guardian deploymentGroup <name> <pet>",
                    "/tames guardian deployGroup <name>",
                    "/tames guardian deploy <all|group <group>|type <type>|state <follow|wander|sit>>",
                    "/tames guardian info <setName>",
                    "/tames guardian list",
                    "A guardian anchor is a return point. After combat, the tame paths back there.",
                    "If it still has not returned after about 60 seconds, it is teleported back.",
                    "'previous' restores the last guardian anchor. 'home' sets the current anchor without overwriting previous.",
                    "'deploy' activates the current guardian locations for the selected tames.",
                    "Deployment groups store per-tame guardian positions under a shared name and later redeploy those members with deployGroup."
            );
        }
        else if (key.equals("guardian_arrow") || key.equals("guardianarrow")) {
            sendInfoPage(p, "GuardianArrow",
                    "Rename an arrow to '<target>: <guardianGroup>'.",
                    "Examples: 'all: base', 'group wolves: north', 'type minecraft:wolf: west', 'Fluffy: tower'.",
                    "Right click block: set the next selected tame's guardian location in that guardian group one block above the clicked block.",
                    "Sneak right click block: deploy that guardian group.",
                    "Left click block: remove that guardian-group location at the clicked block from matching selected tames.",
                    "Sneak left click block for 3s: queue a confirm to clear that guardian group for the selected tames; use /tames guardian tool confirm within 60s.",
                    "Right click tame: add it to the selected group if the selector is 'group ...'. Left click tame: remove it from that group.",
                    "Sneak right click tame: store its current guardian anchor in the guardian group. Sneak left click tame: remove that guardian-group anchor from the tame."
            );
        }
        else if (key.equals("tool guardian") || key.equals("tool guardian_arrow") || key.equals("tool guardianarrow")) {
            sendToolInfoPage(p, "Guardian",
                    Component.literal("Item: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("renamed arrow").withStyle(ChatFormatting.GOLD)),
                    Component.literal("Format: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("<target>: <guardianGroup>").withStyle(ChatFormatting.AQUA)),
                    Component.literal("Set next anchor: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("right click block").withStyle(ChatFormatting.GREEN)),
                    Component.literal("Deploy group: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("sneak right click block").withStyle(ChatFormatting.GREEN)),
                    Component.literal("Remove anchor at block: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("left click block").withStyle(ChatFormatting.YELLOW)),
                    Component.literal("Clear whole guardian group: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("sneak left click block for 3s").withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" then ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("/tames guardian tool confirm").withStyle(ChatFormatting.GOLD)),
                    Component.literal("Edit groups with tame clicks: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("right click add").withStyle(ChatFormatting.GREEN))
                            .append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("left click remove").withStyle(ChatFormatting.YELLOW))
            );
        }
        else if (key.equals("call_stick") || key.equals("callstick")) {
            sendInfoPage(p, "CallStick",
                    "Rename a bone to a selector: all, exact tame name, 'group <group>', 'type <type>', close, nearby, follow, sit, or wander.",
                    "Right click air/block: apply the current movement command to all selected tames.",
                    "Left click air: cycle combat mode for the selected tames.",
                    "Left click block: clear current combat targets for the selected tames.",
                    "Left click block for 3s: set guardian there. Sneak left click block for 3s: move there and passive-sit.",
                    "Hit a mob: all selected tames target it.",
                    "Right click block for 5s: teleport selected tames there. Right click air for 5s: teleport them home.",
                    "Right click tame: add it to the selected group if the bone targets 'group ...'. Left click tame: remove it from that group.",
                    "The bone is consumed on use unless you are in creative, so renaming a stack lets you reuse the same selector many times."
            );
        }
        else if (key.equals("tool bone") || key.equals("tool call_stick") || key.equals("tool callstick")) {
            sendToolInfoPage(p, "Bone",
                    Component.literal("Item: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("renamed bone").withStyle(ChatFormatting.GOLD)),
                    Component.literal("Selectors: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("all, tame name, group <group>, type <type>, close, nearby, follow, sit, wander").withStyle(ChatFormatting.AQUA)),
                    Component.literal("Movement command: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("right click air/block").withStyle(ChatFormatting.GREEN)),
                    Component.literal("Cycle combat mode: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("left click air").withStyle(ChatFormatting.YELLOW)),
                    Component.literal("Clear targets: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("left click block").withStyle(ChatFormatting.YELLOW)),
                    Component.literal("Set guardian / passive sit: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("hold left click block 3s").withStyle(ChatFormatting.GREEN))
                            .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("sneak hold left click block 3s").withStyle(ChatFormatting.GREEN)),
                    Component.literal("Teleport selected tames: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("hold right click block 5s").withStyle(ChatFormatting.GREEN))
                            .append(Component.literal(" or ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("hold right click air 5s for home").withStyle(ChatFormatting.GREEN))
            );
        }
        else if (key.equals("movement")) {
            sendInfoPage(p, "Movement",
                    "/tames movement <pet|all|group|type|state> <default|skeleton|close>",
                    "default: no extra spacing behavior.",
                    "skeleton: if the target is too close, the tame backs off every 100 ticks.",
                    "close: while idle and not guarding, the tame repaths every 20 ticks to stay roughly 1.5 to 2.5 blocks from its owner."
            );
        }
        else if (key.equals("tp")) {
            sendInfoPage(p, "TP",
                    "/tames tp <name|all|follow|sit|wander|state <follow|wander|sit>|group <group>|type <type>>",
                    "Teleports tames to the player.",
                    "Cost is level XP points, doubled for cross-dimension teleports.",
                    "Loaded tames move immediately; unloaded ones use the lantern/recovery path when possible.",
                    "Teleporting clears the current guardian anchor."
            );
        }
        else if (key.equals("tphome")) {
            sendInfoPage(p, "TPHome",
                    "/tames tphome <name|all|follow|sit|wander|state <follow|wander|sit>|group <group>|type <type>>",
                    "Teleports tames to the same target the respawn system would use.",
                    "Target priority: tame bed, queued DI bed request, owner bed, then player/source position fallback.",
                    "TPHome is free.",
                    "Teleporting clears the current guardian anchor."
            );
        }
        else if (key.equals("bed")) {
            sendInfoPage(p, "Bed",
                    "/tames bed",
                    "/tames bed set <pet>",
                    "/tames bed remove <pet>",
                    "/tames bed remove all",
                    "/tames bed remove group <group>",
                    "/tames bed remove type <type>",
                    "Shows your tames with assigned beds.",
                    "Stand on a pet bed and run /tames bed set <pet> to assign that bed for the normal teleport XP cost.",
                    "Each row is formatted as: Tame: BedType, Dimension [x, y, z].",
                    "Removing a bed clears that tame's stored bed assignment."
            );
        }
        else if (key.equals("respawn")) {
            sendInfoPage(p, "Respawn",
                    "/tames respawn <name|all|group <name>|type <name>>",
                    "/tames respawn waitingList [limit]",
                    "/tames respawn order [default|level|leaderboard]",
                    "Respawn works only on dead tames and does not apply an extra death penalty.",
                    "Respawn target priority: tame bed, queued DI bed request, owner bed, then player/source position fallback.",
                    "Payment options: full invested XP, or 1 approved item if the tame has a bed, otherwise ceil(level/20) approved items, or 1 totem in main hand.",
                    "Morning auto-respawn is split by pet bed color.",
                    "All dead tames with colored pet beds respawn the next morning.",
                    "White pet beds use the TL graveyard/respawn-order queue, and only 1 white-bed tame per owner respawns each morning."
            );
        }
        else if (key.equals("arise")) {
            sendInfoPage(p, "Arise",
                    "/tames arise <name|all|group <name>|type <name>>",
                    "Arise respawns the tame at your current position.",
                    "Payment options: double invested XP, or ceil(level/10) approved items, or 1 totem in main hand."
            );
        }
        else if (key.equals("graveyard")) {
            sendInfoPage(p, "Graveyard",
                    "/tames graveyard [limit]",
                    "Shows your dead tames in the same order used for morning respawn.",
                    "Each row includes death time, active survival days, reincarnation cost, and the saved highest-level suffix."
            );
        }
        else if (key.equals("reincarnate") || key.equals("reincarnation")) {
            sendInfoPage(p, "Reincarnate",
                    "/tames reincarnate <pet>",
                    "/tames reincarnation",
                    "/tames reincarnation <all|group <name>|type <name>>",
                    "/tames reincarnation approvedItems",
                    "/tames reincarnation auto <true|false>",
                    "Reincarnation is command-only. The tame must already be alive and loaded.",
                    "It restores the saved highest progress snapshot for that tame.",
                    "Payment options: reincarnation XP cost, or 1 approved item per restored level, or 1 totem in main hand.",
                    "Auto reincarnation uses the player bed material check by default. Tames with a black pet bed auto reincarnate for free on respawn."
            );
        }
        else if (key.equals("healthsiphon") || key.equals("health_siphon")) {
            sendInfoPage(p, "HealthSiphon",
                    "/tames healthSiphon <true|false>",
                    "Toggles whether your tames may redirect incoming damage to you through the health_siphon attribute.",
                    "This is owner-local and persists for your tame registry."
            );
        }
        else if (key.equals("enterportalsbythemselves") || key.equals("enter_portals_by_themselves")) {
            sendInfoPage(p, "EnterPortalsByThemselves",
                    "/tames enterPortalsByThemselves <true|false>",
                    "Default is false.",
                    "False: your tames cannot enter portals on their own.",
                    "They only change dimension through owner-triggered tethered teleport follow when following and having tethered_teleport."
            );
        }
        else if (key.equals("inspect")) {
            sendInfoPage(p, "Inspect",
                    "/tames inspect <pet>",
                    "/tames inspect <pet> long",
                    "The base inspect command now shows the long inspect view directly.",
                    "Includes current base stats, class profile, exact known offensive ability numbers, support/heal mechanics, attributes, and recent level rewards."
            );
        }
        else if (key.equals("search")) {
            sendInfoPage(p, "Search",
                    "/tames search ability <id>",
                    "/tames search attribute <id>",
                    "/tames search class <id>",
                    "Searches your registered tames by ability, attribute, or tame class."
            );
        }
        else if (key.equals("duel")) {
            sendInfoPage(p, "Duel",
                    "/tames duel <left> vs <right>",
                    "/tames duel accept <player> vs <your selection>",
                    "Compact duel selectors support comma-separated mixes like: rex, type wolf, group gang, all, follow, or myself.",
                    "If no player names are in either side, the duel starts immediately as a same-owner team duel.",
                    "If player names are included, a staged duel is created and each invited player must accept with their own selection.",
                    "Accepted players may invite allies on their side during accept, but invited players are blocked if they already have a pending duel invite.",
                    "The duel starts only after every invited player has accepted."
            );
        }
        else if (key.equals("debug")) {
            sendInfoPage(p, "Debug",
                    "/tames debug enemyKilled|abilityUsed|attributeUsed|damage <true|false>",
                    "Toggles owner-local chat debug messages for combat and progression events."
            );
        }
        else if (key.equals("attribute")) {
            p.sendSystemMessage(Component.literal("Attribute docs (General):").withStyle(ChatFormatting.GOLD));
            sendDocLines(p, readDocSectionByHeading(
                    docPath("AttributesDocu.md"),
                    "## General",
                    "## "
            ));
            p.sendSystemMessage(Component.literal("Use /tames info attribute <name> for exact doc text and /tames info attribute <name> <level> for a live level preview.").withStyle(ChatFormatting.DARK_AQUA));
        }
        else if (key.equals("ability")) {
            p.sendSystemMessage(Component.literal("Ability docs (General):").withStyle(ChatFormatting.GOLD));
            sendDocLines(p, readDocSectionByHeading(
                    docPath("AbilitiesDocu.md"),
                    "## General",
                    "## "
            ));
            p.sendSystemMessage(Component.literal("Use /tames info ability <name> for exact doc text and /tames info ability <name> <level> for a live level preview.").withStyle(ChatFormatting.DARK_AQUA));
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
        else p.sendSystemMessage(Component.literal("Unknown topic. Use /tames info for the topic list."));
        return 1;
    }

    private static void sendInfoPage(ServerPlayer player, String title, String... lines) {
        if (player == null) {
            return;
        }
        player.sendSystemMessage(Component.literal(title + " Info").withStyle(ChatFormatting.GOLD));
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            ChatFormatting style = line.startsWith("/tames ") ? ChatFormatting.GOLD : ChatFormatting.GRAY;
            player.sendSystemMessage(Component.literal(line).withStyle(style));
        }
    }

    private static void sendToolInfoPage(ServerPlayer player, String title, net.minecraft.network.chat.MutableComponent... lines) {
        if (player == null) {
            return;
        }
        player.sendSystemMessage(Component.literal(title + " Tool Info").withStyle(ChatFormatting.GOLD));
        for (net.minecraft.network.chat.MutableComponent line : lines) {
            if (line != null) {
                player.sendSystemMessage(line);
            }
        }
    }

    private static String capitalizeAscii(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static int infoAbility(CommandSourceStack source, String abilityName) {
        return infoAbility(source, abilityName, null);
    }

    private static int infoAbility(CommandSourceStack source, String abilityName, Integer previewLevel) {
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
        sendAbilityRuntimeInfo(p, id);
        if (previewLevel != null) {
            sendAbilityLevelPreview(p, id, previewLevel);
        }
        sendDocLines(p, block);
        return 1;
    }

    private static void sendAbilityRuntimeInfo(ServerPlayer player, String abilityId) {
        if (player == null || abilityId == null || abilityId.isBlank()) {
            return;
        }
        if (!LevelSystem.isAttackAbility(abilityId)) {
            player.sendSystemMessage(Component.literal("Live runtime note: support/heal abilities are not affected by the attack-ability cooldown nerf.").withStyle(ChatFormatting.DARK_AQUA));
            return;
        }
        player.sendSystemMessage(Component.literal(
                "Live runtime scaling: cast damage = level-1 base * (1 + 0.20 * (level - 1) + 0.05 * bonusDamage * scaling + 0.15 * ability_power)."
        ).withStyle(ChatFormatting.DARK_AQUA));
        player.sendSystemMessage(Component.literal(
                "Live runtime cooldown nerf: attack cooldown x(1 + " + fmt(TLAdminRuntimeSettings.abilityCountCooldownNerfPercent()) + "% * log2(owned attack abilities))."
        ).withStyle(ChatFormatting.DARK_AQUA));
        player.sendSystemMessage(Component.literal("Use /tames inspect <pet> for exact current damage and cooldown on a specific tame.").withStyle(ChatFormatting.DARK_AQUA));
    }

    private static int infoAttribute(CommandSourceStack source, String attributeName) {
        return infoAttribute(source, attributeName, null);
    }

    private static int infoAttribute(CommandSourceStack source, String attributeName, Integer previewLevel) {
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
        if (previewLevel != null) {
            sendAttributeLevelPreview(p, id, previewLevel);
        }
        sendDocLines(p, block);
        return 1;
    }

    private static void sendAbilityLevelPreview(ServerPlayer player, String abilityId, int level) {
        if (player == null || abilityId == null || abilityId.isBlank()) {
            return;
        }
        int safeLevel = Math.max(1, level);
        TameData preview = previewTameData();
        preview.abilityLevels.put(abilityId, safeLevel);
        preview.abilities.add(abilityId);
        String line = buildAbilityInspectLine(null, preview, abilityId, safeLevel, true);
        if (line == null || line.isBlank()) {
            return;
        }
        player.sendSystemMessage(Component.literal("Preview L" + safeLevel + ": " + line).withStyle(ChatFormatting.DARK_AQUA));
        player.sendSystemMessage(Component.literal("Preview assumes no extra bonusDamage or synergistic attributes unless the line says otherwise.").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void sendAttributeLevelPreview(ServerPlayer player, String attributeId, int level) {
        if (player == null || attributeId == null || attributeId.isBlank()) {
            return;
        }
        int safeLevel = Math.max(1, level);
        TameData preview = previewTameData();
        preview.attributeLevels.put(attributeId, safeLevel);
        String line = buildAttributeInspectLine(preview, attributeId, safeLevel);
        if (line == null || line.isBlank()) {
            return;
        }
        player.sendSystemMessage(Component.literal("Preview L" + safeLevel + ": " + line).withStyle(ChatFormatting.DARK_AQUA));
        player.sendSystemMessage(Component.literal("Preview is mechanic-focused and not tied to a specific loaded tame.").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static TameData previewTameData() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", UUID.randomUUID());
        tag.putUUID("ownerUUID", UUID.randomUUID());
        tag.putString("name", "preview");
        tag.putString("type", "minecraft:wolf");
        tag.putInt("level", 1);
        tag.putInt("xpToNext", 100);
        return TameData.fromTag(tag);
    }

    private static int infoClass(CommandSourceStack source, String className) {
        ServerPlayer p = source.getPlayer();
        TameClass tameClass = TameClass.parse(className);
        if (tameClass == null) return error(p, "Unknown class: " + className);
        LevelSystem.ClassCategoryView category = LevelSystem.classCategoryWeights(tameClass);
        Map<String, Double> baseStats = LevelSystem.classBaseStatWeights(tameClass);
        Map<String, Double> attributes = LevelSystem.classAttributeWeights(tameClass);
        Map<String, Double> abilities = LevelSystem.classAbilityWeights(tameClass);

        p.sendSystemMessage(Component.literal("Class info: " + tameClass.name() + " (" + tameClass.rarity().name().toLowerCase(java.util.Locale.ROOT) + ")").withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.literal(
                "Category chances: base " + fmt(category.base()) + "%  attribute " + fmt(category.attribute()) + "%  ability " + fmt(category.ability()) + "%"
        ).withStyle(ChatFormatting.GRAY));
        p.sendSystemMessage(Component.literal("Preferred base stats: " + formatWeightMap(baseStats)).withStyle(ChatFormatting.GRAY));
        p.sendSystemMessage(Component.literal("Preferred attributes: " + formatWeightMap(attributes)).withStyle(ChatFormatting.GRAY));
        p.sendSystemMessage(Component.literal("Preferred abilities: " + formatWeightMap(abilities)).withStyle(ChatFormatting.GRAY));
        p.sendSystemMessage(Component.literal(
                "Global preferred multipliers: attributes x" + fmt(LevelSystem.preferredAttributeWeightMultiplier())
                        + "  abilities x" + fmt(LevelSystem.preferredAbilityWeightMultiplier())
        ).withStyle(ChatFormatting.DARK_AQUA));
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

    private static int reincarnatePet(CommandSourceStack source, String petName) {
        ServerPlayer player = source.getPlayer();
        TameData data = findOwnedTame(player.getUUID(), petName);
        if (data == null) {
            return error(player, "Loaded/alive tame not found.");
        }
        if (data.dead) {
            return error(player, "That tame is dead. Respawn it first.");
        }
        TamableAnimal tame = findLoadedTameByUuid(source, data.uuid);
        if (tame == null || !tame.isAlive()) {
            return error(player, "Reincarnation requires the tame to be loaded and alive.");
        }
        if (!data.hasSavedProgress) {
            return error(player, "That tame has no higher saved progress to restore.");
        }
        if (data.level >= data.savedLevel) {
            return error(player, "That tame is already at or above its highest saved level.");
        }
        int cost = LevelSystem.reincarnationXpCost(data);
        int restoredLevels = Math.max(0, data.savedLevel - data.level);
        PaymentResult preview = previewPayment(player, cost, Math.max(1, restoredLevels), true, "reincarnation");
        if (!preview.success) {
            return error(player, "Cannot afford reincarnation for " + tameDisplayName(data) + " (" + paymentPriceLabel(cost, Math.max(1, restoredLevels), true) + ").");
        }
        int fromLevel = data.level;
        int targetLevel = data.savedLevel;
        if (!LevelSystem.restoreHighestProgressWithoutXpCost(tame, data)) {
            return error(player, "Failed to restore saved progress.");
        }
        PaymentResult payment = tryConsumePayment(player, cost, Math.max(1, restoredLevels), true, "reincarnation");
        if (!payment.success) {
            return error(player, payment.error);
        }
        player.sendSystemMessage(Component.literal(
                "Reincarnated " + data.name + " from level " + fromLevel + " to level " + targetLevel + " for " + payment.label + "."
        ).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int requestRerollClass(CommandSourceStack source, String petName) {
        ServerPlayer player = source.getPlayer();
        TameData data = findOwnedTame(player.getUUID(), petName);
        if (data == null) {
            return error(player, "Alive tame not found.");
        }
        if (data.dead) {
            return error(player, tameDisplayName(data) + " is dead. Respawn it first.");
        }
        TamableAnimal tame = findLoadedTameByUuid(source, data.uuid);
        if (tame == null || !tame.isAlive()) {
            return error(player, "Class reroll requires the tame to be loaded and alive.");
        }
        if (data.classRerollsUsed >= MAX_CLASS_REROLLS) {
            return error(player, tameDisplayName(data) + " has already used all " + MAX_CLASS_REROLLS + " class rerolls.");
        }

        long now = source.getServer() != null && source.getServer().overworld() != null
                ? source.getServer().overworld().getGameTime()
                : 0L;
        PENDING_CLASS_REROLLS.put(player.getUUID(), new PendingClassReroll(
                data.uuid,
                data.tlId,
                tameDisplayName(data),
                now + CLASS_REROLL_CONFIRM_TICKS
        ));
        int rerollsLeftAfterUse = Math.max(0, MAX_CLASS_REROLLS - (data.classRerollsUsed + 1));
        player.sendSystemMessage(Component.literal(
                "Rerolling class will reset " + tameDisplayName(data) + " to level 1 with 0 XP and remove all abilities, attributes, bonus stats, saved progress, and level reward history."
        ).withStyle(ChatFormatting.RED));
        player.sendSystemMessage(Component.literal(
                "Kills, assists, deaths, and survival-day stats are kept. Run /tames rerollClass confirm within 30 seconds to continue. Rerolls left after this: " + rerollsLeftAfterUse + "."
        ).withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int cancelRerollClass(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        PendingClassReroll removed = PENDING_CLASS_REROLLS.remove(player.getUUID());
        if (removed == null) {
            return error(player, "No pending class reroll.");
        }
        player.sendSystemMessage(Component.literal("Cancelled class reroll for " + removed.tameName + ".").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int confirmRerollClass(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        PendingClassReroll pending = PENDING_CLASS_REROLLS.get(player.getUUID());
        if (pending == null) {
            return error(player, "No pending class reroll. Use /tames rerollClass <name> first.");
        }
        long now = source.getServer() != null && source.getServer().overworld() != null
                ? source.getServer().overworld().getGameTime()
                : 0L;
        if (now > pending.expiresAtGameTime) {
            PENDING_CLASS_REROLLS.remove(player.getUUID());
            return error(player, "The pending class reroll expired. Use /tames rerollClass <name> again.");
        }

        TameData data = pending.tameTlId != null ? TameRegistry.getByTlId(pending.tameTlId) : null;
        if (data == null && pending.tameUuid != null) {
            data = TameRegistry.get(pending.tameUuid);
        }
        if (data == null || !player.getUUID().equals(data.ownerUUID)) {
            PENDING_CLASS_REROLLS.remove(player.getUUID());
            return error(player, "The pending tame could not be found anymore.");
        }
        if (data.dead) {
            PENDING_CLASS_REROLLS.remove(player.getUUID());
            return error(player, tameDisplayName(data) + " is dead. Respawn it first.");
        }
        TamableAnimal tame = findLoadedTameByUuid(source, data.uuid);
        if (tame == null || !tame.isAlive()) {
            PENDING_CLASS_REROLLS.remove(player.getUUID());
            return error(player, "Class reroll requires the tame to still be loaded and alive.");
        }
        if (data.classRerollsUsed >= MAX_CLASS_REROLLS) {
            PENDING_CLASS_REROLLS.remove(player.getUUID());
            return error(player, tameDisplayName(data) + " has already used all " + MAX_CLASS_REROLLS + " class rerolls.");
        }

        TameClass previousClass = data.tameClass;
        TameClass rerolledClass = rerollToDifferentClass(previousClass);
        resetTameForClassReroll(tame, data, rerolledClass);
        PENDING_CLASS_REROLLS.remove(player.getUUID());
        player.sendSystemMessage(Component.literal(
                "Rerolled " + tameDisplayName(data) + " from " + (previousClass == null ? "unassigned" : previousClass.id()) + " to " + rerolledClass.id()
                        + ". It is now level 1 with 0 XP. Rerolls used: " + data.classRerollsUsed + "/" + MAX_CLASS_REROLLS + "."
        ).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static TameClass rerollToDifferentClass(TameClass previousClass) {
        TameClass[] values = TameClass.values();
        if (values.length <= 1) {
            return LevelSystem.rollClass();
        }
        TameClass rolled = LevelSystem.rollClass();
        for (int tries = 0; tries < 12 && Objects.equals(rolled, previousClass); tries++) {
            rolled = LevelSystem.rollClass();
        }
        if (Objects.equals(rolled, previousClass)) {
            for (TameClass candidate : values) {
                if (!Objects.equals(candidate, previousClass)) {
                    return candidate;
                }
            }
        }
        return rolled;
    }

    private static void resetTameForClassReroll(TamableAnimal tame, TameData data, TameClass rerolledClass) {
        if (tame == null || data == null) {
            return;
        }
        int keptKills = data.kills;
        int keptAssists = data.assists;
        int keptDeaths = data.deaths;
        int keptSurvivalDays = data.activeSurvivalDays;
        long keptLastSurvivalDay = data.lastActiveSurvivalDay;

        LevelSystem.resetProgress(tame, data);
        clearSavedProgress(data);
        data.levelRewardHistory.clear();
        data.kills = keptKills;
        data.assists = keptAssists;
        data.deaths = keptDeaths;
        data.activeSurvivalDays = keptSurvivalDays;
        data.lastActiveSurvivalDay = keptLastSurvivalDay;
        data.tameClass = rerolledClass;
        data.level = 1;
        data.xp = 0;
        data.xpToNext = LevelSystem.xpRequiredForLevel(1);
        data.cooldowns.clear();
        data.classRerollsUsed++;

        if (!applyTypeBasePlusBonus(tame, data)) {
            LevelSystem.updateTameName(tame, data);
        }
        tame.setTarget(null);
        tame.getNavigation().stop();
        tame.setHealth(tame.getMaxHealth());
        refreshRegistrySnapshotFor(tame);
        TameRegistry.markDirty();
    }

    private static int reincarnationOverview(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        List<TameData> eligible = eligibleReincarnationTames(player.getUUID());
        if (eligible.isEmpty()) {
            return error(player, "No reincarnation-eligible tames found.");
        }
        eligible.sort(Comparator
                .comparingInt((TameData data) -> highestRecordedLevel(data)).reversed()
                .thenComparing(data -> data.name == null ? "" : data.name.toLowerCase(Locale.ROOT)));
        player.sendSystemMessage(Component.literal("---- Reincarnation Eligible (" + eligible.size() + ") ----").withStyle(ChatFormatting.GOLD));
        for (TameData data : eligible) {
            player.sendSystemMessage(Component.literal(
                    data.name + "[" + data.level + "] -> " + highestRecordedLevel(data) + " (" + LevelSystem.reincarnationXpCost(data) + "xp)"
            ).withStyle(ChatFormatting.AQUA));
        }
        return 1;
    }

    private static int setAutoReincarnation(CommandSourceStack source, boolean enabled) {
        ServerPlayer player = source.getPlayer();
        if (enabled && player.getRespawnPosition() == null) {
            return error(player, "Auto reincarnation requires a valid player bed/spawn point.");
        }
        TameRegistry.setAutoReincarnation(player.getUUID(), enabled);
        player.sendSystemMessage(Component.literal("Auto reincarnation " + (enabled ? "enabled" : "disabled") + ".").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        return 1;
    }

    private static int listDoNotAttack(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        Set<String> blocked = new LinkedHashSet<>(TameRegistry.getDoNotAttackTypes(player.getUUID()));
        List<String> sorted = new ArrayList<>(blocked);
        sorted.sort(String::compareToIgnoreCase);
        boolean animals = TameRegistry.isDoNotAttackAnimals(player.getUUID());
        player.sendSystemMessage(Component.literal("---- Do Not Attack ----").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("Animals protected: " + animals).withStyle(animals ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        if (sorted.isEmpty()) {
            player.sendSystemMessage(Component.literal("Protected mob types: none").withStyle(ChatFormatting.GRAY));
        } else {
            player.sendSystemMessage(Component.literal("Protected mob types (" + sorted.size() + "):").withStyle(ChatFormatting.GREEN));
            for (String id : sorted) {
                player.sendSystemMessage(Component.literal("- " + id).withStyle(ChatFormatting.AQUA));
            }
        }
        return sorted.size();
    }

    private static int toggleDoNotAttackType(CommandSourceStack source, String rawMobType) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        ResourceLocation id = normalizeEntityTypeId(rawMobType);
        if (id == null) {
            return error(player, "Unknown mob type: " + rawMobType);
        }
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (entityType == null) {
            return error(player, "Unknown mob type: " + id);
        }
        boolean added = TameRegistry.toggleDoNotAttackType(player.getUUID(), id.toString());
        player.sendSystemMessage(Component.literal((added ? "Protected " : "Removed protection for ") + id + ".")
                .withStyle(added ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        return 1;
    }

    private static int removeDoNotAttackType(CommandSourceStack source, String rawMobType) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        ResourceLocation id = normalizeEntityTypeId(rawMobType);
        if (id == null) {
            return error(player, "Unknown mob type: " + rawMobType);
        }
        boolean removed = TameRegistry.removeDoNotAttackType(player.getUUID(), id.toString());
        if (!removed) {
            return error(player, "You do not have do-not-attack protection for " + id + ".");
        }
        player.sendSystemMessage(Component.literal("Removed protection for " + id + ".")
                .withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int setDoNotAttackAnimals(CommandSourceStack source, boolean enabled) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        TameRegistry.setDoNotAttackAnimals(player.getUUID(), enabled);
        player.sendSystemMessage(Component.literal("Do-not-attack animals " + (enabled ? "enabled" : "disabled") + ".")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        return 1;
    }

    private static int setHealthSiphonEnabled(CommandSourceStack source, boolean enabled) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        TameRegistry.setHealthSiphonEnabled(player.getUUID(), enabled);
        player.sendSystemMessage(Component.literal("Health siphon " + (enabled ? "enabled" : "disabled") + ".")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        return 1;
    }

    private static int setEnterPortalsByThemselves(CommandSourceStack source, boolean enabled) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        TameRegistry.setEnterPortalsByThemselves(player.getUUID(), enabled);
        player.sendSystemMessage(Component.literal("Tames entering portals by themselves " + (enabled ? "enabled" : "disabled") + ".")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        return 1;
    }

    private static int listOwnedBeds(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        List<TameData> withBeds = new ArrayList<>();
        List<TameData> withoutBeds = new ArrayList<>();
        for (TameData data : ownedTames(player.getUUID())) {
            if (data == null) {
                continue;
            }
            if (data.hasPetBed && data.petBedDimension != null && !data.petBedDimension.isBlank()) {
                withBeds.add(data);
            } else {
                withoutBeds.add(data);
            }
        }
        if (withBeds.isEmpty() && withoutBeds.isEmpty()) {
            return error(player, "You do not have any registered tames.");
        }
        if (!withBeds.isEmpty()) {
            withBeds.sort(Comparator.comparing(TameCommands::tameDisplayName, String.CASE_INSENSITIVE_ORDER));
            player.sendSystemMessage(Component.literal("---- Your Tames With Beds ----").withStyle(ChatFormatting.GOLD));
            for (TameData data : withBeds) {
                player.sendSystemMessage(Component.literal(tameDisplayName(data)).withStyle(bedEntryColor(source.getServer(), data)));
            }
        }
        if (!withoutBeds.isEmpty()) {
            withoutBeds.sort(Comparator.comparing(TameCommands::tameDisplayName, String.CASE_INSENSITIVE_ORDER));
            player.sendSystemMessage(Component.literal("---- Your Tames Without Beds ----").withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.literal(formatCompactTameNameList(withoutBeds)).withStyle(ChatFormatting.GRAY));
        }
        return withBeds.size() + withoutBeds.size();
    }

    private static int listOwnedBedsLong(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        List<TameData> withBeds = new ArrayList<>();
        List<TameData> withoutBeds = new ArrayList<>();
        for (TameData data : ownedTames(player.getUUID())) {
            if (data == null) {
                continue;
            }
            if (data.hasPetBed && data.petBedDimension != null && !data.petBedDimension.isBlank()) {
                withBeds.add(data);
            } else {
                withoutBeds.add(data);
            }
        }
        if (withBeds.isEmpty()) {
            if (withoutBeds.isEmpty()) {
                return error(player, "You do not have any registered tames.");
            }
            player.sendSystemMessage(Component.literal("None of your tames currently have a bed.").withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(Component.literal("No bed: " + formatCompactTameNameList(withoutBeds)).withStyle(ChatFormatting.GRAY));
            return withoutBeds.size();
        }
        withBeds.sort(Comparator.comparing(TameCommands::tameDisplayName, String.CASE_INSENSITIVE_ORDER));
        player.sendSystemMessage(Component.literal("---- Your Tame Beds ----").withStyle(ChatFormatting.GOLD));
        for (TameData data : withBeds) {
            player.sendSystemMessage(Component.literal(formatBedEntry(source.getServer(), data)).withStyle(bedEntryColor(source.getServer(), data)));
        }
        if (!withoutBeds.isEmpty()) {
            player.sendSystemMessage(Component.literal("No bed: " + formatCompactTameNameList(withoutBeds)).withStyle(ChatFormatting.GRAY));
        }
        return withBeds.size();
    }

    private static int listOwnedBedsWithoutBed(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        List<TameData> withoutBeds = new ArrayList<>();
        for (TameData data : ownedTames(player.getUUID())) {
            if (data != null && (!data.hasPetBed || data.petBedDimension == null || data.petBedDimension.isBlank())) {
                withoutBeds.add(data);
            }
        }
        if (withoutBeds.isEmpty()) {
            return error(player, "All of your tames currently have a bed.");
        }
        player.sendSystemMessage(Component.literal("---- Your Tames Without Beds ----").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(formatCompactTameNameList(withoutBeds)).withStyle(ChatFormatting.GRAY));
        return withoutBeds.size();
    }

    private static int setOwnedBed(CommandSourceStack source, String petName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        TameData data = findOwnedTame(player.getUUID(), petName);
        if (data == null) {
            return error(player, "Tame not found.");
        }
        BlockPos bedPos = currentPetBedUnderPlayer(player);
        if (bedPos == null) {
            return error(player, "Stand on a pet bed to set a tame bed location.");
        }
        String dimensionId = player.serverLevel().dimension().location().toString();
        TameData claimed = TameRegistry.getTameByPetBed(dimensionId, bedPos);
        if (claimed != null && claimed.uuid != null && !claimed.uuid.equals(data.uuid)) {
            return error(player, "That pet bed already belongs to " + tameDisplayName(claimed) + ".");
        }
        boolean crossDimension = isCrossDimension(data, player);
        int cost = teleportCostFor(data, crossDimension);
        if (!payTeleportXp(player, cost)) {
            return 0;
        }
        assignTameBed(player.getServer(), data, dimensionId, bedPos);
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Set bed for " + tameDisplayName(data) + " at [" + bedPos.getX() + ", " + bedPos.getY() + ", " + bedPos.getZ() + "] (-" + cost + " XP points" + (crossDimension ? ", cross-dimension" : "") + ").")
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int removeOwnedBed(CommandSourceStack source, String petName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        TameData data = findOwnedTame(player.getUUID(), petName);
        if (data == null) {
            return error(player, "Tame not found.");
        }
        if (!data.hasPetBed || data.petBedDimension == null || data.petBedDimension.isBlank()) {
            return error(player, tameDisplayName(data) + " does not have a bed assigned.");
        }
        clearTameBedAssignment(source.getServer(), data);
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Removed bed assignment for " + tameDisplayName(data) + ".").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int removeOwnedBedsAll(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        return removeOwnedBeds(source, player, ownedTames(player.getUUID()), "all your tames");
    }

    private static int removeOwnedBedsGroup(CommandSourceStack source, String group) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        return removeOwnedBeds(source, player, ownedGroup(player.getUUID(), group), "group '" + group + "'");
    }

    private static int removeOwnedBedsType(CommandSourceStack source, String typeFilter) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        return removeOwnedBeds(source, player, ownedType(player.getUUID(), typeFilter), "type '" + typeFilter + "'");
    }

    private static int removeOwnedBeds(CommandSourceStack source, ServerPlayer player, List<TameData> requested, String label) {
        if (player == null) {
            return 0;
        }
        if (requested == null || requested.isEmpty()) {
            return error(player, "No tames found for " + label + ".");
        }
        int removed = 0;
        for (TameData data : requested) {
            if (data == null || !data.hasPetBed || data.petBedDimension == null || data.petBedDimension.isBlank()) {
                continue;
            }
            clearTameBedAssignment(source.getServer(), data);
            removed++;
        }
        if (removed <= 0) {
            return error(player, "No bed assignments found for " + label + ".");
        }
        TameRegistry.markDirty();
        int finalRemoved = removed;
        player.sendSystemMessage(Component.literal("Removed " + finalRemoved + " bed assignment(s) for " + label + ".").withStyle(ChatFormatting.GREEN));
        return finalRemoved;
    }

    private static void clearTameBedAssignment(MinecraftServer server, TameData data) {
        if (data == null) {
            return;
        }
        data.hasPetBed = false;
        data.petBedDimension = "";
        data.petBedX = 0;
        data.petBedY = 0;
        data.petBedZ = 0;
        TamableAnimal loaded = server == null ? null : findLoadedTameByUuid(server, data.uuid);
        if (loaded != null) {
            TameableUtils.removePetBedPos(loaded);
            TameableUtils.setPetBedDimension(loaded, "");
        }
    }

    private static void assignTameBed(MinecraftServer server, TameData data, String dimensionId, BlockPos bedPos) {
        if (data == null || dimensionId == null || dimensionId.isBlank() || bedPos == null) {
            return;
        }
        data.hasPetBed = true;
        data.petBedDimension = dimensionId;
        data.petBedX = bedPos.getX();
        data.petBedY = bedPos.getY();
        data.petBedZ = bedPos.getZ();
        TamableAnimal loaded = server == null ? null : findLoadedTameByUuid(server, data.uuid);
        if (loaded != null) {
            TameableUtils.setPetBedPos(loaded, bedPos);
            TameableUtils.setPetBedDimension(loaded, dimensionId);
        }
    }

    private static BlockPos currentPetBedUnderPlayer(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        BlockPos[] candidates = new BlockPos[]{
                player.blockPosition(),
                player.blockPosition().below()
        };
        for (BlockPos pos : candidates) {
            if (pos == null) {
                continue;
            }
            BlockEntity blockEntity = player.serverLevel().getBlockEntity(pos);
            if (blockEntity instanceof com.github.alexthe668.domesticationinnovation.server.block.PetBedBlockEntity) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static String formatBedEntry(MinecraftServer server, TameData data) {
        if (data == null) {
            return "unknown";
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(data.petBedDimension);
        String dimensionName = shortDimensionName(dimensionId, data.petBedDimension);
        String bedType = shortBedType(petBedBlockId(server, data));
        return tameDisplayName(data) + ": " + bedType + ", " + dimensionName + " [" + data.petBedX + ", " + data.petBedY + ", " + data.petBedZ + "]";
    }

    private static String formatCompactTameNameList(List<TameData> tames) {
        if (tames == null || tames.isEmpty()) {
            return "-";
        }
        return tames.stream()
                .filter(Objects::nonNull)
                .map(TameCommands::tameDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
    }

    private static ChatFormatting bedEntryColor(MinecraftServer server, TameData data) {
        ResourceLocation blockId = petBedBlockId(server, data);
        if (blockId == null) {
            return ChatFormatting.GRAY;
        }
        return switch (blockId.getPath()) {
            case "pet_bed_black" -> ChatFormatting.DARK_GRAY;
            case "pet_bed_blue" -> ChatFormatting.BLUE;
            case "pet_bed_brown" -> ChatFormatting.GOLD;
            case "pet_bed_cyan" -> ChatFormatting.DARK_AQUA;
            case "pet_bed_gray" -> ChatFormatting.GRAY;
            case "pet_bed_green" -> ChatFormatting.DARK_GREEN;
            case "pet_bed_light_blue" -> ChatFormatting.AQUA;
            case "pet_bed_light_gray" -> ChatFormatting.WHITE;
            case "pet_bed_lime" -> ChatFormatting.GREEN;
            case "pet_bed_magenta" -> ChatFormatting.LIGHT_PURPLE;
            case "pet_bed_orange" -> ChatFormatting.GOLD;
            case "pet_bed_pink" -> ChatFormatting.LIGHT_PURPLE;
            case "pet_bed_purple" -> ChatFormatting.DARK_PURPLE;
            case "pet_bed_red" -> ChatFormatting.RED;
            case "pet_bed_white" -> ChatFormatting.WHITE;
            case "pet_bed_yellow" -> ChatFormatting.YELLOW;
            default -> ChatFormatting.GRAY;
        };
    }

    private static ResourceLocation petBedBlockId(MinecraftServer server, TameData data) {
        if (server == null || data == null || !data.hasPetBed || data.petBedDimension == null || data.petBedDimension.isBlank()) {
            return null;
        }
        ResourceLocation bedDimId = ResourceLocation.tryParse(data.petBedDimension);
        if (bedDimId == null) {
            return null;
        }
        ServerLevel bedLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, bedDimId));
        if (bedLevel == null) {
            return null;
        }
        BlockPos bedPos = new BlockPos(data.petBedX, data.petBedY, data.petBedZ);
        bedLevel.getChunk(bedPos);
        return ForgeRegistries.BLOCKS.getKey(bedLevel.getBlockState(bedPos).getBlock());
    }

    private static ResourceLocation normalizeEntityTypeId(String rawMobType) {
        if (rawMobType == null || rawMobType.isBlank()) {
            return null;
        }
        String normalized = rawMobType.trim().toLowerCase(Locale.ROOT);
        ResourceLocation parsed = ResourceLocation.tryParse(normalized);
        if (parsed != null && ForgeRegistries.ENTITY_TYPES.containsKey(parsed)) {
            return parsed;
        }
        ResourceLocation minecraftDefault = ResourceLocation.tryParse("minecraft:" + normalized);
        if (minecraftDefault != null && ForgeRegistries.ENTITY_TYPES.containsKey(minecraftDefault)) {
            return minecraftDefault;
        }
        return null;
    }

    private static CompletableFuture<Suggestions> suggestCurrentPlayerDoNotAttackTypes(CommandSourceStack source, SuggestionsBuilder builder) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return builder.buildFuture();
        }
        List<String> protectedTypes = new ArrayList<>(TameRegistry.getDoNotAttackTypes(player.getUUID()));
        protectedTypes.sort(String::compareToIgnoreCase);
        return SharedSuggestionProvider.suggest(protectedTypes, builder);
    }

    private static int reincarnateBatchAll(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return reincarnateBatch(source, eligibleLoadedReincarnationTames(source, player.getUUID(), ownedTames(player.getUUID())), "all");
    }

    private static int reincarnateBatchGroup(CommandSourceStack source, String group) {
        ServerPlayer player = source.getPlayer();
        return reincarnateBatch(source, eligibleLoadedReincarnationTames(source, player.getUUID(), ownedGroup(player.getUUID(), group)), "group " + group);
    }

    private static int reincarnateBatchType(CommandSourceStack source, String typeFilter) {
        ServerPlayer player = source.getPlayer();
        return reincarnateBatch(source, eligibleLoadedReincarnationTames(source, player.getUUID(), ownedType(player.getUUID(), typeFilter)), "type " + typeFilter);
    }

    private static int reincarnateBatch(CommandSourceStack source, List<TameData> requested, String label) {
        ServerPlayer player = source.getPlayer();
        if (requested.isEmpty()) {
            return error(player, "No loaded reincarnation-eligible tames found for " + label + ".");
        }
        int totalLevelsRestored = 0;
        int restored = 0;
        List<String> spentLabels = new ArrayList<>();
        List<String> unaffordable = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (TameData data : requested) {
            TamableAnimal tame = findLoadedTameByUuid(source, data.uuid);
            if (tame == null || !tame.isAlive()) {
                continue;
            }
            int xpCost = LevelSystem.reincarnationXpCost(data);
            int restoredLevels = Math.max(0, data.savedLevel - data.level);
            int approvedItemCost = Math.max(1, restoredLevels);
            PaymentResult preview = previewPayment(player, xpCost, approvedItemCost, true, "reincarnation");
            if (!preview.success) {
                unaffordable.add(pricedTameLabel(data, paymentPriceLabel(xpCost, approvedItemCost, true)));
                continue;
            }
            PaymentResult payment = tryConsumePayment(player, xpCost, approvedItemCost, true, "reincarnation");
            if (!payment.success) {
                unaffordable.add(pricedTameLabel(data, paymentPriceLabel(xpCost, approvedItemCost, true)));
                continue;
            }
            if (LevelSystem.restoreHighestProgressWithoutXpCost(tame, data)) {
                restored++;
                spentLabels.add(payment.label);
                totalLevelsRestored += restoredLevels;
            } else {
                failed.add(tameDisplayName(data) + " (failed to restore saved progress)");
            }
        }
        if (restored <= 0) {
            StringBuilder message = new StringBuilder("No tames were reincarnated.");
            if (!unaffordable.isEmpty()) {
                message.append(" Could not afford: ").append(String.join("; ", unaffordable)).append(".");
            }
            if (!failed.isEmpty()) {
                message.append(" Failures: ").append(String.join("; ", failed)).append(".");
            }
            return error(player, message.toString());
        }
        player.sendSystemMessage(Component.literal(
                "Reincarnated " + restored + " tame(s) for " + String.join(", ", spentLabels) + " (" + totalLevelsRestored + " total level(s) restored)."
        ).withStyle(ChatFormatting.GREEN));
        sendAffordabilityFailures(player, "Could not afford reincarnation for", unaffordable);
        if (!failed.isEmpty()) {
            player.sendSystemMessage(Component.literal("Reincarnation failures: " + String.join("; ", failed)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static List<TameData> eligibleReincarnationTames(UUID ownerUuid) {
        List<TameData> eligible = new ArrayList<>();
        for (TameData data : ownedTames(ownerUuid)) {
            if (data == null || data.dead) continue;
            if (!isReincarnationEligible(data)) continue;
            eligible.add(data);
        }
        return eligible;
    }

    private static List<TameData> eligibleLoadedReincarnationTames(CommandSourceStack source, UUID ownerUuid, List<TameData> requested) {
        List<TameData> eligible = new ArrayList<>();
        for (TameData data : requested) {
            if (data == null || data.dead) continue;
            if (!ownerUuid.equals(data.ownerUUID)) continue;
            if (!isReincarnationEligible(data)) continue;
            TamableAnimal tame = findLoadedTameByUuid(source, data.uuid);
            if (tame == null || !tame.isAlive()) continue;
            eligible.add(data);
        }
        return eligible;
    }

    private static PaymentResult tryConsumePayment(ServerPlayer player, int xpCost, int approvedItemCost, boolean allowXp, String purpose) {
        return evaluatePayment(player, xpCost, approvedItemCost, allowXp, purpose, true);
    }

    private static PaymentResult previewPayment(ServerPlayer player, int xpCost, int approvedItemCost, boolean allowXp, String purpose) {
        return evaluatePayment(player, xpCost, approvedItemCost, allowXp, purpose, false);
    }

    private static String tameDisplayName(TameData data) {
        if (data == null || data.name == null || data.name.isBlank()) {
            return "unknown";
        }
        return data.name;
    }

    private static String paymentPriceLabel(int xpCost, int approvedItemCost, boolean allowXp) {
        int normalizedXp = Math.max(0, xpCost);
        int normalizedItems = Math.max(1, approvedItemCost);
        List<String> parts = new ArrayList<>();
        if (allowXp) {
            parts.add(normalizedXp + " XP");
        }
        if (approvedItemCost > 0) {
            parts.add(normalizedItems + " approved item" + (normalizedItems == 1 ? "" : "s"));
        }
        parts.add("1 totem");
        return String.join(" or ", parts);
    }

    private static String teleportPriceLabel(int xpCost) {
        return Math.max(0, xpCost) + " XP points";
    }

    private static String pricedTameLabel(TameData data, String price) {
        return tameDisplayName(data) + " (" + price + ")";
    }

    private static void sendAffordabilityFailures(ServerPlayer player, String prefix, List<String> failures) {
        if (player == null || failures == null || failures.isEmpty()) {
            return;
        }
        player.sendSystemMessage(Component.literal(prefix + ": " + String.join("; ", failures)).withStyle(ChatFormatting.RED));
    }

    private static PaymentResult evaluatePayment(ServerPlayer player, int xpCost, int approvedItemCost, boolean allowXp, String purpose, boolean consume) {
        if (player == null) {
            return PaymentResult.fail("player unavailable");
        }
        ItemStack held = player.getMainHandItem();
        int normalizedXp = Math.max(0, xpCost);
        int normalizedItems = Math.max(1, approvedItemCost);
        if (isReincarnationTotem(held)) {
            if (consume) {
                held.shrink(1);
            }
            return PaymentResult.ok("1 totem");
        }
        if (isApprovedReincarnationItem(held) && held.getCount() >= normalizedItems) {
            if (consume) {
                held.shrink(normalizedItems);
            }
            return PaymentResult.ok(normalizedItems + " approved item" + (normalizedItems == 1 ? "" : "s"));
        }
        int currentXp = currentXpPoints(player);
        if (allowXp && currentXp >= normalizedXp) {
            if (consume && normalizedXp > 0) {
                player.giveExperiencePoints(-normalizedXp);
            }
            return PaymentResult.ok(normalizedXp + " XP points");
        }
        return PaymentResult.fail(describePaymentRequirement(player, normalizedXp, normalizedItems, allowXp, purpose, currentXp));
    }

    private static String describePaymentRequirement(ServerPlayer player, int xpCost, int approvedItemCost, boolean allowXp, String purpose, int currentXp) {
        StringBuilder builder = new StringBuilder();
        builder.append("Cannot afford ").append(purpose == null || purpose.isBlank() ? "this" : purpose).append(". Need ");
        boolean appended = false;
        if (allowXp) {
            builder.append(xpCost).append(" XP");
            appended = true;
        }
        if (approvedItemCost > 0) {
            if (appended) {
                builder.append(" or ");
            }
            builder.append(approvedItemCost).append(" approved item").append(approvedItemCost == 1 ? "" : "s");
            appended = true;
        }
        if (appended) {
            builder.append(" or 1 totem.");
        } else {
            builder.append("1 totem.");
        }
        return builder.toString();
    }

    private static String approvedItemNamesDisplay() {
        List<String> names = new ArrayList<>();
        for (String id : TameRegistry.APPROVED_REINCARNATE_ITEMS) {
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key == null) {
                continue;
            }
            var item = ForgeRegistries.ITEMS.getValue(key);
            if (item != null) {
                names.add(item.getDefaultInstance().getHoverName().getString());
            }
        }
        names.sort(String::compareToIgnoreCase);
        return String.join(", ", names);
    }

    private static boolean isApprovedReincarnationItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && TameRegistry.APPROVED_REINCARNATE_ITEMS.contains(key.toString().toLowerCase(Locale.ROOT));
    }

    private static boolean isReincarnationTotem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(Items.TOTEM_OF_UNDYING)) {
            return true;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) {
            return false;
        }
        String path = key.getPath().toLowerCase(Locale.ROOT);
        return path.contains("totem");
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
        return duelInviteSelection(source, selection, targetPlayerName, DuelSpectators.empty());
    }

    private static int duelInviteSelectionLegacy(CommandSourceStack source, DuelSelection selection, String targetPlayerName) {
        return duelInviteSelection(source, selection, targetPlayerName, DuelSpectators.empty(), DUEL_INVITES, "duelOld");
    }

    private static int duelInviteSelectionWithSpectators(CommandSourceStack source, DuelSelection selection, String targetPlayerName, String spectatorSpec) {
        DuelSpectatorParseResult parsed = parseDuelSpectators(source, spectatorSpec, source.getPlayer().getUUID());
        if (!parsed.error.isBlank()) {
            return error(source.getPlayer(), parsed.error);
        }
        return duelInviteSelection(source, selection, targetPlayerName, parsed.spectators);
    }

    private static int duelInviteSelection(CommandSourceStack source, DuelSelection selection, String targetPlayerName, DuelSpectators spectators) {
        return duelInviteSelection(source, selection, targetPlayerName, spectators, DUEL_INVITES, "duel");
    }

    private static int duelInviteSelection(CommandSourceStack source, DuelSelection selection, String targetPlayerName, DuelSpectators spectators, Map<UUID, Map<UUID, DuelInvite>> inviteStore, String commandLiteral) {
        ServerPlayer challenger = source.getPlayer();
        if (challenger.getName().getString().equalsIgnoreCase(targetPlayerName)) {
            return error(challenger, "You cannot duel yourself.");
        }
        ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(targetPlayerName);
        if (targetPlayer == null) {
            return error(challenger, "Target player is not online.");
        }

        cleanupExpiredDuelInviteStore(inviteStore);
        DuelSelectionResult challengerResult = resolveLoadedDuelSelection(source, challenger.getUUID(), selection);
        if (!challengerResult.error.isBlank()) return error(challenger, challengerResult.error);
        List<TamableAnimal> challengerGroup = challengerResult.tames;
        if (challengerGroup.isEmpty()) return error(challenger, "Your selected duel tames are not loaded/alive.");

        inviteStore.computeIfAbsent(targetPlayer.getUUID(), ignored -> new HashMap<>())
                .put(challenger.getUUID(), new DuelInvite(challenger.getUUID(), targetPlayer.getUUID(), TeamSelection.tameOnly(selection), spectators, System.currentTimeMillis()));

        String challengerSelectionText = duelSelectionLabel(selection);
        challenger.sendSystemMessage(Component.literal("Sent duel invite to " + targetPlayer.getName().getString() + " using " + challengerSelectionText + ".").withStyle(ChatFormatting.GREEN));
        targetPlayer.sendSystemMessage(Component.literal(challenger.getName().getString() + " invited you to a duel with " + challengerSelectionText + "." + duelSpectatorLabel(source.getServer(), spectators)).withStyle(ChatFormatting.GOLD));
        targetPlayer.sendSystemMessage(Component.literal("Accept: /tames " + commandLiteral + " accept " + challenger.getName().getString() + " <group|type|all|name>").withStyle(ChatFormatting.AQUA));
        targetPlayer.sendSystemMessage(Component.literal("Quick accept: /tames " + commandLiteral + " group <group>, /tames " + commandLiteral + " type <type>, /tames " + commandLiteral + " <tamename>, /tames " + commandLiteral + " all").withStyle(ChatFormatting.AQUA));
        targetPlayer.sendSystemMessage(Component.literal("Decline: /tames " + commandLiteral + " decline " + challenger.getName().getString()).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int duelInviteTeam(CommandSourceStack source, String targetPlayerName, String selectionSpec) {
        return duelInviteTeam(source, targetPlayerName, selectionSpec, DuelSpectators.empty());
    }

    private static int duelInviteTeamWithSpectators(CommandSourceStack source, String targetPlayerName, String selectionSpec, String spectatorSpec) {
        DuelSpectatorParseResult parsed = parseDuelSpectators(source, spectatorSpec, source.getPlayer().getUUID());
        if (!parsed.error.isBlank()) {
            return error(source.getPlayer(), parsed.error);
        }
        return duelInviteTeam(source, targetPlayerName, selectionSpec, parsed.spectators);
    }

    private static int duelInviteTeam(CommandSourceStack source, String targetPlayerName, String selectionSpec, DuelSpectators spectators) {
        ServerPlayer challenger = source.getPlayer();
        if (challenger.getName().getString().equalsIgnoreCase(targetPlayerName)) {
            return error(challenger, "You cannot duel yourself.");
        }
        ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(targetPlayerName);
        if (targetPlayer == null) {
            return error(challenger, "Target player is not online.");
        }

        TeamSelection selection = parseTeamSelectionSpec(selectionSpec);
        if (selection == null) {
            return error(challenger, invalidTeamSelectionMessage());
        }
        return duelInviteTeamSelection(source, targetPlayerName, selection, spectators);
    }

    private static int duelInviteTeamSelection(CommandSourceStack source, String targetPlayerName, TeamSelection selection, DuelSpectators spectators) {
        ServerPlayer challenger = source.getPlayer();
        if (challenger.getName().getString().equalsIgnoreCase(targetPlayerName)) {
            return error(challenger, "You cannot duel yourself.");
        }
        ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(targetPlayerName);
        if (targetPlayer == null) {
            return error(challenger, "Target player is not online.");
        }
        cleanupExpiredDuelInvites();
        TeamSelectionResult challengerResult = resolveLoadedTeamSelection(source, challenger, selection);
        if (!challengerResult.error.isBlank()) return error(challenger, challengerResult.error);
        if (challengerResult.members.isEmpty()) return error(challenger, "Your selected duel team has no loaded/alive members.");

        DUEL_INVITES.computeIfAbsent(targetPlayer.getUUID(), ignored -> new HashMap<>())
                .put(challenger.getUUID(), new DuelInvite(challenger.getUUID(), targetPlayer.getUUID(), selection, spectators, System.currentTimeMillis()));

        String challengerSelectionText = teamSelectionLabel(selection);
        challenger.sendSystemMessage(Component.literal("Sent duel invite to " + targetPlayer.getName().getString() + " using " + challengerSelectionText + ".").withStyle(ChatFormatting.GREEN));
        targetPlayer.sendSystemMessage(Component.literal(challenger.getName().getString() + " invited you to a duel with " + challengerSelectionText + "." + duelSpectatorLabel(source.getServer(), spectators)).withStyle(ChatFormatting.GOLD));
        targetPlayer.sendSystemMessage(Component.literal("Accept: /tames duel accept " + challenger.getName().getString() + " team <selection>").withStyle(ChatFormatting.AQUA));
        targetPlayer.sendSystemMessage(Component.literal("Quick accept: /tames duel team <selection>").withStyle(ChatFormatting.AQUA));
        targetPlayer.sendSystemMessage(Component.literal("Decline: /tames duel decline " + challenger.getName().getString()).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int duelStartSameOwner(CommandSourceStack source, DuelSelection leftSelection, DuelSelection rightSelection) {
        return duelStartSameOwner(source, leftSelection, rightSelection, DuelSpectators.empty());
    }

    private static int duelStartSameOwnerWithSpectators(CommandSourceStack source, DuelSelection leftSelection, DuelSelection rightSelection, String spectatorSpec) {
        DuelSpectatorParseResult parsed = parseDuelSpectators(source, spectatorSpec, source.getPlayer().getUUID());
        if (!parsed.error.isBlank()) {
            return error(source.getPlayer(), parsed.error);
        }
        return duelStartSameOwner(source, leftSelection, rightSelection, parsed.spectators);
    }

    private static int duelStartSameOwner(CommandSourceStack source, DuelSelection leftSelection, DuelSelection rightSelection, DuelSpectators spectators) {
        ServerPlayer owner = source.getPlayer();
        if (leftSelection != null && rightSelection != null
                && leftSelection.kind == DuelSelectionKind.ALL
                && rightSelection.kind == DuelSelectionKind.ALL) {
            return error(owner, "Both duel selections cannot be all.");
        }
        DuelSelectionResult leftResult = resolveLoadedDuelSelection(source, owner.getUUID(), leftSelection);
        if (!leftResult.error.isBlank()) return error(owner, leftResult.error);
        DuelSelectionResult rightResult = resolveLoadedDuelSelection(source, owner.getUUID(), rightSelection);
        if (!rightResult.error.isBlank()) return error(owner, rightResult.error);

        List<TamableAnimal> leftGroup = new ArrayList<>(leftResult.tames);
        List<TamableAnimal> rightGroup = new ArrayList<>(rightResult.tames);
        if (leftSelection != null && leftSelection.kind == DuelSelectionKind.ALL) {
            leftGroup = remainingDuelOpponents(source, owner.getUUID(), rightGroup);
        }
        if (rightSelection != null && rightSelection.kind == DuelSelectionKind.ALL) {
            rightGroup = remainingDuelOpponents(source, owner.getUUID(), leftGroup);
        }
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
        if (!leftIds.isEmpty()) {
            rightGroup.removeIf(tame -> tame != null && leftIds.contains(tame.getUUID()));
            rightIds.removeIf(leftIds::contains);
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

        TameDuelManager.startGroupDuel(source.getServer(), owner.getUUID(), leftIds, owner.getUUID(), rightIds, spectators.playerIds, spectators.broadcastToServer);
        notifyDuelSpectators(source.getServer(), owner.getUUID(), owner.getUUID(), spectators,
                Component.literal("Duel started: " + sameOwnerDuelSelectionLabel(leftSelection, rightSelection, true) + " vs " + sameOwnerDuelSelectionLabel(leftSelection, rightSelection, false) + ".").withStyle(ChatFormatting.RED));
        return 1;
    }

    private static int duelStartSameOwnerTeam(CommandSourceStack source, String leftSpec, String rightSpec) {
        return duelStartSameOwnerTeam(source, leftSpec, rightSpec, DuelSpectators.empty());
    }

    private static int duelStartSameOwnerTeamWithSpectators(CommandSourceStack source, String leftSpec, String rightSpec, String spectatorSpec) {
        DuelSpectatorParseResult parsed = parseDuelSpectators(source, spectatorSpec, source.getPlayer().getUUID());
        if (!parsed.error.isBlank()) {
            return error(source.getPlayer(), parsed.error);
        }
        return duelStartSameOwnerTeam(source, leftSpec, rightSpec, parsed.spectators);
    }

    private static int duelStartSameOwnerTeam(CommandSourceStack source, String leftSpec, String rightSpec, DuelSpectators spectators) {
        ServerPlayer owner = source.getPlayer();
        TeamSelection leftSelection = parseTeamSelectionSpec(leftSpec);
        TeamSelection rightSelection = parseTeamSelectionSpec(rightSpec);
        if (leftSelection == null || rightSelection == null) {
            return error(owner, invalidTeamSelectionMessage());
        }

        TeamSelectionResult leftResult = resolveLoadedTeamSelection(source, owner, leftSelection);
        if (!leftResult.error.isBlank()) return error(owner, leftResult.error);
        TeamSelectionResult rightResult = resolveLoadedTeamSelection(source, owner, rightSelection);
        if (!rightResult.error.isBlank()) return error(owner, rightResult.error);

        if (leftResult.members.isEmpty()) return error(owner, "Left duel team has no loaded/alive members.");
        if (rightResult.members.isEmpty()) return error(owner, "Right duel team has no loaded/alive members.");

        Set<UUID> leftIds = collectLivingEntityIds(leftResult.members);
        Set<UUID> rightIds = collectLivingEntityIds(rightResult.members);
        if (!leftIds.isEmpty()) {
            rightResult.members.removeIf(member -> member != null && leftIds.contains(member.getUUID()));
            rightResult.tames.removeIf(tame -> tame != null && leftIds.contains(tame.getUUID()));
            rightIds.removeIf(leftIds::contains);
        }
        if (rightIds.isEmpty()) {
            return error(owner, "Right duel team has no loaded/alive members after removing overlaps.");
        }

        prepareTeamForDuel(leftResult.tames);
        prepareTeamForDuel(rightResult.tames);
        assignInitialDuelTargets(leftResult.tames, rightResult.members);
        assignInitialDuelTargets(rightResult.tames, leftResult.members);

        TameDuelManager.startTeamDuel(source.getServer(), owner.getUUID(), leftIds, owner.getUUID(), rightIds, spectators.playerIds, spectators.broadcastToServer);
        notifyDuelSpectators(source.getServer(), owner.getUUID(), owner.getUUID(), spectators,
                Component.literal("Duel started: " + teamSelectionLabel(leftSelection) + " vs " + teamSelectionLabel(rightSelection) + ".").withStyle(ChatFormatting.RED));
        return 1;
    }

    private static int duelAcceptSelection(CommandSourceStack source, String challengerName, DuelSelection targetSelection) {
        return duelAcceptSelection(source, challengerName, targetSelection, DuelSpectators.empty());
    }

    private static int duelAcceptSelectionLegacy(CommandSourceStack source, String challengerName, DuelSelection targetSelection) {
        return duelAcceptSelection(source, challengerName, targetSelection, DuelSpectators.empty(), DUEL_INVITES);
    }

    private static int duelAcceptSelectionWithSpectators(CommandSourceStack source, String challengerName, DuelSelection targetSelection, String spectatorSpec) {
        DuelSpectatorParseResult parsed = parseDuelSpectators(source, spectatorSpec, source.getPlayer().getUUID());
        if (!parsed.error.isBlank()) {
            return error(source.getPlayer(), parsed.error);
        }
        return duelAcceptSelection(source, challengerName, targetSelection, parsed.spectators);
    }

    private static int duelAcceptSelection(CommandSourceStack source, String challengerName, DuelSelection targetSelection, DuelSpectators extraSpectators) {
        return duelAcceptSelection(source, challengerName, targetSelection, extraSpectators, DUEL_INVITES);
    }

    private static int duelAcceptSelection(CommandSourceStack source, String challengerName, DuelSelection targetSelection, DuelSpectators extraSpectators, Map<UUID, Map<UUID, DuelInvite>> inviteStore) {
        ServerPlayer targetPlayer = source.getPlayer();
        cleanupExpiredDuelInviteStore(inviteStore);
        ServerPlayer challenger = source.getServer().getPlayerList().getPlayerByName(challengerName);
        if (challenger == null) return error(targetPlayer, "Challenger is not online.");
        if (challenger.getUUID().equals(targetPlayer.getUUID())) return error(targetPlayer, "You cannot duel yourself.");

        DuelInvite invite = popDuelInvite(inviteStore, targetPlayer.getUUID(), challenger.getUUID());
        if (invite == null) return error(targetPlayer, "No pending duel invite from " + challengerName + ".");

        DuelSelectionResult challengerResult = resolveLoadedDuelSelection(source, challenger.getUUID(), firstTeamDuelSelection(invite.challengerSelection));
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
        DuelSpectators spectators = mergeDuelSpectators(invite.spectators, extraSpectators);
        TameDuelManager.startGroupDuel(source.getServer(), challenger.getUUID(), challengerIds, targetPlayer.getUUID(), targetIds, spectators.playerIds, spectators.broadcastToServer);

        String challengerSelectionText = teamSelectionLabel(invite.challengerSelection);
        String targetSelectionText = duelSelectionLabel(targetSelection);
        notifyDuelSpectators(source.getServer(), challenger.getUUID(), targetPlayer.getUUID(), spectators,
                Component.literal("Duel started: " + challengerSelectionText + " vs " + targetSelectionText + ".").withStyle(ChatFormatting.RED));
        return 1;
    }

    private static int duelAcceptTeam(CommandSourceStack source, String challengerName, String selectionSpec) {
        return duelAcceptTeam(source, challengerName, selectionSpec, DuelSpectators.empty());
    }

    private static int duelAcceptTeamWithSpectators(CommandSourceStack source, String challengerName, String selectionSpec, String spectatorSpec) {
        DuelSpectatorParseResult parsed = parseDuelSpectators(source, spectatorSpec, source.getPlayer().getUUID());
        if (!parsed.error.isBlank()) {
            return error(source.getPlayer(), parsed.error);
        }
        return duelAcceptTeam(source, challengerName, selectionSpec, parsed.spectators);
    }

    private static int duelAcceptTeam(CommandSourceStack source, String challengerName, String selectionSpec, DuelSpectators extraSpectators) {
        ServerPlayer targetPlayer = source.getPlayer();
        TeamSelection targetSelection = parseTeamSelectionSpec(selectionSpec);
        if (targetSelection == null) return error(targetPlayer, invalidTeamSelectionMessage());
        return duelAcceptTeamSelection(source, challengerName, targetSelection, extraSpectators);
    }

    private static int duelAcceptTeamSelection(CommandSourceStack source, String challengerName, TeamSelection targetSelection, DuelSpectators extraSpectators) {
        ServerPlayer targetPlayer = source.getPlayer();
        cleanupExpiredDuelInvites();
        ServerPlayer challenger = source.getServer().getPlayerList().getPlayerByName(challengerName);
        if (challenger == null) return error(targetPlayer, "Challenger is not online.");
        if (challenger.getUUID().equals(targetPlayer.getUUID())) return error(targetPlayer, "You cannot duel yourself.");

        DuelInvite invite = popDuelInvite(targetPlayer.getUUID(), challenger.getUUID());
        if (invite == null) return error(targetPlayer, "No pending duel invite from " + challengerName + ".");

        TeamSelectionResult challengerResult = resolveLoadedTeamSelection(source, challenger, invite.challengerSelection);
        if (!challengerResult.error.isBlank()) return error(targetPlayer, challengerResult.error);
        if (challengerResult.members.isEmpty()) return error(targetPlayer, "Challenger selected duel team has no loaded/alive members.");
        TeamSelectionResult targetResult = resolveLoadedTeamSelection(source, targetPlayer, targetSelection);
        if (!targetResult.error.isBlank()) return error(targetPlayer, targetResult.error);
        if (targetResult.members.isEmpty()) return error(targetPlayer, "Your selected duel team has no loaded/alive members.");

        Set<UUID> challengerIds = collectLivingEntityIds(challengerResult.members);
        Set<UUID> targetIds = collectLivingEntityIds(targetResult.members);
        prepareTeamForDuel(challengerResult.tames);
        prepareTeamForDuel(targetResult.tames);
        assignInitialDuelTargets(challengerResult.tames, targetResult.members);
        assignInitialDuelTargets(targetResult.tames, challengerResult.members);
        DuelSpectators spectators = mergeDuelSpectators(invite.spectators, extraSpectators);
        TameDuelManager.startTeamDuel(source.getServer(), challenger.getUUID(), challengerIds, targetPlayer.getUUID(), targetIds, spectators.playerIds, spectators.broadcastToServer);

        String challengerSelectionText = teamSelectionLabel(invite.challengerSelection);
        String targetSelectionText = teamSelectionLabel(targetSelection);
        notifyDuelSpectators(source.getServer(), challenger.getUUID(), targetPlayer.getUUID(), spectators,
                Component.literal("Duel started: " + challengerSelectionText + " vs " + targetSelectionText + ".").withStyle(ChatFormatting.RED));
        return 1;
    }

    private static int duelAcceptQuick(CommandSourceStack source, DuelSelection targetSelection) {
        return duelAcceptQuick(source, targetSelection, DuelSpectators.empty());
    }

    private static int duelAcceptQuickLegacy(CommandSourceStack source, DuelSelection targetSelection) {
        return duelAcceptQuick(source, targetSelection, DuelSpectators.empty(), DUEL_INVITES, "duelOld");
    }

    private static int duelAcceptQuickWithSpectators(CommandSourceStack source, DuelSelection targetSelection, String spectatorSpec) {
        DuelSpectatorParseResult parsed = parseDuelSpectators(source, spectatorSpec, source.getPlayer().getUUID());
        if (!parsed.error.isBlank()) {
            return error(source.getPlayer(), parsed.error);
        }
        return duelAcceptQuick(source, targetSelection, parsed.spectators);
    }

    private static int duelAcceptQuick(CommandSourceStack source, DuelSelection targetSelection, DuelSpectators extraSpectators) {
        return duelAcceptQuick(source, targetSelection, extraSpectators, DUEL_INVITES, "duel");
    }

    private static int duelAcceptQuick(CommandSourceStack source, DuelSelection targetSelection, DuelSpectators extraSpectators, Map<UUID, Map<UUID, DuelInvite>> inviteStore, String commandLiteral) {
        ServerPlayer targetPlayer = source.getPlayer();
        cleanupExpiredDuelInviteStore(inviteStore);
        Map<UUID, DuelInvite> incoming = inviteStore.get(targetPlayer.getUUID());
        if (incoming == null || incoming.isEmpty()) {
            return error(targetPlayer, "No pending duel invites.");
        }
        if (incoming.size() > 1) {
            return error(targetPlayer, "Multiple duel invites pending. Use /tames " + commandLiteral + " accept <player> <group|type|all|name>.");
        }

        DuelInvite invite = incoming.values().iterator().next();
        ServerPlayer challenger = source.getServer().getPlayerList().getPlayer(invite.challengerUuid);
        if (challenger == null) {
            popDuelInvite(inviteStore, targetPlayer.getUUID(), invite.challengerUuid);
            return error(targetPlayer, "Challenger is not online.");
        }
        if (challenger.getUUID().equals(targetPlayer.getUUID())) {
            return error(targetPlayer, "You cannot duel yourself.");
        }
        popDuelInvite(inviteStore, targetPlayer.getUUID(), invite.challengerUuid);

        DuelSelectionResult challengerResult = resolveLoadedDuelSelection(source, challenger.getUUID(), firstTeamDuelSelection(invite.challengerSelection));
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
        DuelSpectators spectators = mergeDuelSpectators(invite.spectators, extraSpectators);
        TameDuelManager.startGroupDuel(source.getServer(), challenger.getUUID(), challengerIds, targetPlayer.getUUID(), targetIds, spectators.playerIds, spectators.broadcastToServer);

        String challengerSelectionText = teamSelectionLabel(invite.challengerSelection);
        String targetSelectionText = duelSelectionLabel(targetSelection);
        notifyDuelSpectators(source.getServer(), challenger.getUUID(), targetPlayer.getUUID(), spectators,
                Component.literal("Duel started: " + challengerSelectionText + " vs " + targetSelectionText + ".").withStyle(ChatFormatting.RED));
        return 1;
    }

    private static int duelAcceptQuickTeam(CommandSourceStack source, String selectionSpec) {
        return duelAcceptQuickTeam(source, selectionSpec, DuelSpectators.empty());
    }

    private static int duelAcceptQuickTeamWithSpectators(CommandSourceStack source, String selectionSpec, String spectatorSpec) {
        DuelSpectatorParseResult parsed = parseDuelSpectators(source, spectatorSpec, source.getPlayer().getUUID());
        if (!parsed.error.isBlank()) {
            return error(source.getPlayer(), parsed.error);
        }
        return duelAcceptQuickTeam(source, selectionSpec, parsed.spectators);
    }

    private static int duelAcceptQuickTeam(CommandSourceStack source, String selectionSpec, DuelSpectators extraSpectators) {
        ServerPlayer targetPlayer = source.getPlayer();
        cleanupExpiredDuelInvites();
        Map<UUID, DuelInvite> incoming = DUEL_INVITES.get(targetPlayer.getUUID());
        if (incoming == null || incoming.isEmpty()) {
            return error(targetPlayer, "No pending duel invites.");
        }
        if (incoming.size() > 1) {
            return error(targetPlayer, "Multiple duel invites pending. Use /tames duel accept <player> team <selection>.");
        }

        TeamSelection targetSelection = parseTeamSelectionSpec(selectionSpec);
        if (targetSelection == null) return error(targetPlayer, invalidTeamSelectionMessage());

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

        TeamSelectionResult challengerResult = resolveLoadedTeamSelection(source, challenger, invite.challengerSelection);
        if (!challengerResult.error.isBlank()) return error(targetPlayer, challengerResult.error);
        if (challengerResult.members.isEmpty()) return error(targetPlayer, "Challenger selected duel team has no loaded/alive members.");
        TeamSelectionResult targetResult = resolveLoadedTeamSelection(source, targetPlayer, targetSelection);
        if (!targetResult.error.isBlank()) return error(targetPlayer, targetResult.error);
        if (targetResult.members.isEmpty()) return error(targetPlayer, "Your selected duel team has no loaded/alive members.");

        Set<UUID> challengerIds = collectLivingEntityIds(challengerResult.members);
        Set<UUID> targetIds = collectLivingEntityIds(targetResult.members);
        prepareTeamForDuel(challengerResult.tames);
        prepareTeamForDuel(targetResult.tames);
        assignInitialDuelTargets(challengerResult.tames, targetResult.members);
        assignInitialDuelTargets(targetResult.tames, challengerResult.members);
        DuelSpectators spectators = mergeDuelSpectators(invite.spectators, extraSpectators);
        TameDuelManager.startTeamDuel(source.getServer(), challenger.getUUID(), challengerIds, targetPlayer.getUUID(), targetIds, spectators.playerIds, spectators.broadcastToServer);

        String challengerSelectionText = teamSelectionLabel(invite.challengerSelection);
        String targetSelectionText = teamSelectionLabel(targetSelection);
        notifyDuelSpectators(source.getServer(), challenger.getUUID(), targetPlayer.getUUID(), spectators,
                Component.literal("Duel started: " + challengerSelectionText + " vs " + targetSelectionText + ".").withStyle(ChatFormatting.RED));
        return 1;
    }

    private static int duelDecline(CommandSourceStack source, String challengerName) {
        return duelDecline(source, challengerName, DUEL_INVITES, true);
    }

    private static int duelDeclineLegacy(CommandSourceStack source, String challengerName) {
        return duelDecline(source, challengerName, DUEL_INVITES, true);
    }

    private static int duelDecline(CommandSourceStack source, String challengerName, Map<UUID, Map<UUID, DuelInvite>> inviteStore, boolean includePendingMatches) {
        ServerPlayer targetPlayer = source.getPlayer();
        cleanupExpiredDuelInviteStore(inviteStore);
        ServerPlayer challenger = source.getServer().getPlayerList().getPlayerByName(challengerName);
        PendingDuelMatch pending = includePendingMatches ? findPendingDuelMatchForInvite(source, targetPlayer, challengerName) : null;
        if (includePendingMatches && pending != null) {
            notifyPendingDuelCancelled(source.getServer(), pending, targetPlayer.getUUID());
            removePendingDuelMatch(pending.matchId);
            targetPlayer.sendSystemMessage(Component.literal("Declined staged duel invite from " + challengerName + ".").withStyle(ChatFormatting.YELLOW));
            return 1;
        }
        if (challenger == null) return error(targetPlayer, "Challenger is not online.");
        DuelInvite invite = popDuelInvite(inviteStore, targetPlayer.getUUID(), challenger.getUUID());
        if (invite == null) return error(targetPlayer, "No pending duel invite from " + challengerName + ".");

        targetPlayer.sendSystemMessage(Component.literal("Declined duel invite from " + challengerName + ".").withStyle(ChatFormatting.YELLOW));
        challenger.sendSystemMessage(Component.literal(targetPlayer.getName().getString() + " declined your duel invite.").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int duelInbox(CommandSourceStack source) {
        return duelInbox(source, DUEL_INVITES, true);
    }

    private static int duelInboxLegacy(CommandSourceStack source) {
        return duelInbox(source, DUEL_INVITES, true);
    }

    private static int duelInbox(CommandSourceStack source, Map<UUID, Map<UUID, DuelInvite>> inviteStore, boolean includePendingMatches) {
        ServerPlayer targetPlayer = source.getPlayer();
        cleanupExpiredDuelInviteStore(inviteStore);
        Map<UUID, DuelInvite> incoming = inviteStore.get(targetPlayer.getUUID());
        PendingDuelMatch pending = null;
        if (includePendingMatches) {
            UUID pendingMatchId = PENDING_DUEL_MATCH_BY_PLAYER.get(targetPlayer.getUUID());
            if (pendingMatchId != null) {
                pending = PENDING_DUEL_MATCHES.get(pendingMatchId);
            }
        }
        if ((incoming == null || incoming.isEmpty()) && pending == null) {
            targetPlayer.sendSystemMessage(Component.literal("No pending duel invites.").withStyle(ChatFormatting.GRAY));
            return 1;
        }

        targetPlayer.sendSystemMessage(Component.literal("Pending duel invites:").withStyle(ChatFormatting.GOLD));
        if (incoming != null) {
            for (DuelInvite invite : incoming.values()) {
                ServerPlayer challenger = source.getServer().getPlayerList().getPlayer(invite.challengerUuid);
                String challengerName = challenger == null ? invite.challengerUuid.toString() : challenger.getName().getString();
                targetPlayer.sendSystemMessage(Component.literal("- " + challengerName + " using " + teamSelectionLabel(invite.challengerSelection) + duelSpectatorLabel(source.getServer(), invite.spectators)).withStyle(ChatFormatting.AQUA));
            }
        }
        if (pending != null) {
            String initiator = resolveKnownOwnerName(source.getServer(), pending.initiatorUuid, pending.initiatorUuid.toString());
            targetPlayer.sendSystemMessage(Component.literal("- staged duel from " + initiator + " | A: " + pendingDuelSideLabel(source.getServer(), pending, true) + " | B: " + pendingDuelSideLabel(source.getServer(), pending, false)).withStyle(ChatFormatting.LIGHT_PURPLE));
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
        return popDuelInvite(DUEL_INVITES, targetUuid, challengerUuid);
    }

    private static DuelInvite popDuelInvite(Map<UUID, Map<UUID, DuelInvite>> inviteStore, UUID targetUuid, UUID challengerUuid) {
        Map<UUID, DuelInvite> incoming = inviteStore.get(targetUuid);
        if (incoming == null) return null;
        DuelInvite invite = incoming.remove(challengerUuid);
        if (incoming.isEmpty()) {
            inviteStore.remove(targetUuid);
        }
        return invite;
    }

    private static void cleanupExpiredDuelInvites() {
        cleanupExpiredDuelInviteStore(DUEL_INVITES);
        long now = System.currentTimeMillis();
        List<UUID> expiredMatches = new ArrayList<>();
        for (PendingDuelMatch match : PENDING_DUEL_MATCHES.values()) {
            if ((now - match.createdAtMs) > DUEL_INVITE_TIMEOUT_MS) {
                expiredMatches.add(match.matchId);
            }
        }
        for (UUID matchId : expiredMatches) {
            removePendingDuelMatch(matchId);
        }
    }

    private static void cleanupExpiredDuelInviteStore(Map<UUID, Map<UUID, DuelInvite>> inviteStore) {
        long now = System.currentTimeMillis();
        List<UUID> emptyTargets = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, DuelInvite>> entry : inviteStore.entrySet()) {
            Map<UUID, DuelInvite> incoming = entry.getValue();
            incoming.entrySet().removeIf(e -> (now - e.getValue().createdAtMs) > DUEL_INVITE_TIMEOUT_MS);
            if (incoming.isEmpty()) {
                emptyTargets.add(entry.getKey());
            }
        }
        for (UUID target : emptyTargets) {
            inviteStore.remove(target);
        }
    }

    private static void sendTameStats(CommandSourceStack source, ServerPlayer receiver, TameData d, boolean detailed) {
        long days = daysAlive(source, d);
        receiver.sendSystemMessage(Component.literal("=== " + d.name + " ===").withStyle(ChatFormatting.GOLD));
        receiver.sendSystemMessage(Component.literal("Status " + statusLabel(d, source.getServer())).withStyle(statusColor(d, isLoadedAnywhere(source.getServer(), d.uuid))));
        receiver.sendSystemMessage(Component.literal("Lvl " + d.level + "  XP " + d.xp + "/" + d.xpToNext).withStyle(ChatFormatting.YELLOW));
        if (isReincarnationEligible(d)) {
            receiver.sendSystemMessage(Component.literal(
                    "Reincarnation: Level " + highestRecordedLevel(d) + "  cost: " + LevelSystem.reincarnationXpCost(d) + "xp"
            ).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            receiver.sendSystemMessage(Component.literal("Highest level " + highestRecordedLevel(d)).withStyle(ChatFormatting.DARK_GRAY));
        }
        receiver.sendSystemMessage(Component.literal("K " + d.kills + "  A " + d.assists + "  D " + d.deaths + "  Days " + days + "  ActiveDays " + Math.max(0, d.activeSurvivalDays)).withStyle(ChatFormatting.AQUA));
        receiver.sendSystemMessage(Component.literal("Mode " + TameMode.byId(d.mode).key() + "  Class " + (d.tameClass == null ? "-" : d.tameClass.id())).withStyle(ChatFormatting.GREEN));
        receiver.sendSystemMessage(Component.literal("Group " + groupLabel(d)).withStyle(ChatFormatting.DARK_GREEN));
        receiver.sendSystemMessage(Component.literal("Bed " + formatBedLocation(source.getServer(), d)).withStyle(ChatFormatting.DARK_AQUA));
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

    private static int inspectPet(CommandSourceStack source, String petName, boolean longForm) {
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

        double abilityPowerBonus = inspectAbilityPowerBaseBonus(data);
        int attackAbilityCount = inspectOwnedAttackAbilityCount(data);
        double cooldownMultiplier = inspectAttackCooldownMultiplier(attackAbilityCount);

        player.sendSystemMessage(Component.literal("=== Inspect " + data.name + (longForm ? " [Long]" : "") + " ===").withStyle(ChatFormatting.GOLD));
        if (longForm) {
            player.sendSystemMessage(Component.literal("Bonus DMG " + fmt(data.bonusDamage) + "  ability_power +" + fmt(abilityPowerBonus * 100.0D) + "% base"
                    + "  attack abilities " + attackAbilityCount + "  attack cooldown x" + fmt(cooldownMultiplier)).withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.literal("Days " + daysAlive(source, data) + "  ActiveDays " + Math.max(0, data.activeSurvivalDays)).withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.literal("Runtime multipliers: single x" + fmt(TLAdminRuntimeSettings.singleTargetAbilityDamageMultiplier())
                    + "  aoe x" + fmt(TLAdminRuntimeSettings.aoeAbilityDamageMultiplier())
                    + "  cooldown nerf " + fmt(TLAdminRuntimeSettings.abilityCountCooldownNerfPercent()) + "%").withStyle(ChatFormatting.GRAY));
            if (data.tameClass != null) {
                LevelSystem.ClassCategoryView category = LevelSystem.classCategoryWeights(data.tameClass);
                player.sendSystemMessage(Component.literal(
                        "Class " + data.tameClass.id() + " (" + data.tameClass.rarity().name().toLowerCase(java.util.Locale.ROOT) + "): base " + fmt(category.base()) + "%  attribute " + fmt(category.attribute()) + "%  ability " + fmt(category.ability()) + "%"
                ).withStyle(ChatFormatting.DARK_AQUA));
                player.sendSystemMessage(Component.literal(
                        "Class prefs: attr " + formatWeightMapCompact(LevelSystem.classAttributeWeights(data.tameClass), 6)
                                + "  ability " + formatWeightMapCompact(LevelSystem.classAbilityWeights(data.tameClass), 6)
                ).withStyle(ChatFormatting.DARK_AQUA));
            }
        }

        List<String> abilityLines = buildAbilityInspectLines(tame, data, longForm);
        if (abilityLines.isEmpty()) {
            player.sendSystemMessage(Component.literal("Abilities: none").withStyle(ChatFormatting.BLUE));
        } else {
            player.sendSystemMessage(Component.literal("Abilities").withStyle(ChatFormatting.BLUE));
            for (String line : abilityLines) {
                player.sendSystemMessage(Component.literal(line).withStyle(longForm ? ChatFormatting.GRAY : ChatFormatting.AQUA));
            }
        }

        if (longForm) {
            List<String> attributeLines = buildAttributeInspectLines(data);
            if (attributeLines.isEmpty()) {
                player.sendSystemMessage(Component.literal("Attributes: none").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                player.sendSystemMessage(Component.literal("Attributes").withStyle(ChatFormatting.LIGHT_PURPLE));
                for (String line : attributeLines) {
                    player.sendSystemMessage(Component.literal("- " + line).withStyle(ChatFormatting.GRAY));
                }
            }
            if (!data.levelRewardHistory.isEmpty()) {
                player.sendSystemMessage(Component.literal("Recent level rewards").withStyle(ChatFormatting.DARK_AQUA));
                int start = Math.max(0, data.levelRewardHistory.size() - 5);
                for (int i = start; i < data.levelRewardHistory.size(); i++) {
                    CompoundTag row = data.levelRewardHistory.get(i);
                    int level = row.getInt("level");
                    String reward = row.getString("reward");
                    player.sendSystemMessage(Component.literal("- L" + level + ": " + (reward == null || reward.isBlank() ? "-" : reward)).withStyle(ChatFormatting.GRAY));
                }
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
        TameClass tameClass = TameClass.parse(id);
        if (tameClass == null) {
            return error(player, "Unknown class: " + id);
        }
        return searchOwnedTames(source, "Class", tameClass.id(), data -> data.tameClass == tameClass);
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
                .comparingInt((TameData data) -> statusOrder(data, source.getServer()))
                .thenComparing((TameData data) -> data.name == null ? "" : data.name.toLowerCase(Locale.ROOT))
                .thenComparing((TameData data) -> -data.level));

        player.sendSystemMessage(Component.literal("=== Search " + category + ": " + query + " ===").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("Matches: " + matches.size()).withStyle(ChatFormatting.GRAY));
        for (TameData data : matches) {
            boolean loaded = isLoadedAnywhere(source.getServer(), data.uuid);
            String state = statusLabel(data, source.getServer());
            ChatFormatting color = statusColor(data, loaded);
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

    private static List<String> buildAbilityInspectLines(TamableAnimal tame, TameData data, boolean longForm) {
        List<String> lines = new ArrayList<>();
        for (String id : LevelSystem.knownAbilityIds()) {
            if (!LevelSystem.hasAbility(data, id)) continue;
            int level = Math.max(1, LevelSystem.getAbilityLevel(data, id));
            String line = buildAbilityInspectLine(tame, data, id, level, longForm);
            if (line != null && !line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static String buildAbilityInspectLine(TamableAnimal tame, TameData data, String id, int level, boolean longForm) {
        return switch (id) {
            case "arrow_shot", "creeper_explosion", "ghast_fireball", "blaze_attack", "guardian_beam",
                    "elder_guardian_beam", "trident", "crossbow", "fishing", "dash", "dragon_fireball",
                    "llama_spit", "snowball_shot", "lightning_strike", "warden_scream", "wither_skull",
                    "evoker_fangs", "shulker_bullet", "sky_launch" -> inspectReworkedAbilityLine(data, id, level, longForm);
            case "shadow_hands" -> longForm
                    ? id + "[" + level + "] -> situational {complex sustained runtime; exact closed-form DPS not reliable}"
                    : id + "[" + level + "] -> situational";
            case "battle_strength", "defensive_aura", "ender_pearl_jump", "berserker", "bloodlust", "retaliation_slow", "immunity_frame", "deflection", "defusal", "psychic_wall", "healing_aura", "healing_bottle", "guardian_repulse", "last_stand_fury", "shield_block",
                    "guardian_intercept", "emergency_shield", "body_block", "battlefield_medic", "triage_pulse", "revitalizing_presence", "cleanse_touch", "pack_guard", "life_gift" ->
                    (longForm ? inspectSupportAbilityLine(id, level) : id + "[" + level + "] -> utility");
            default -> longForm ? id + " L" + level + ": no inspect profile" : id + "[" + level + "] -> unknown";
        };
    }

    private static String formatWeightMap(Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            return "-";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            parts.add(entry.getKey() + " x" + fmt(entry.getValue()));
        }
        return String.join(", ", parts);
    }

    private static String formatWeightMapCompact(Map<String, Double> weights, int limit) {
        if (weights == null || weights.isEmpty()) {
            return "-";
        }
        List<String> parts = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            if (count >= limit) {
                parts.add("...");
                break;
            }
            parts.add(entry.getKey() + " x" + fmt(entry.getValue()));
            count++;
        }
        return String.join(", ", parts);
    }

    private static String inspectSupportAbilityLine(String id, int level) {
        return switch (id) {
            case "battle_strength" -> id + " L" + level + ": 10% proc on hurt; allies within 3 get Strength " + romanAmp(level - 1) + " for " + fmtSeconds(100L);
            case "defensive_aura" -> id + " L" + level + ": 10% proc on hurt; allies within 3 get Resistance " + romanAmp(defensiveAuraInfoAmplifier(level)) + " for " + fmtSeconds(100L) + "; caps at level 30";
            case "ender_pearl_jump" -> id + " L" + level + ": if target is >5 blocks away, teleports up to " + fmt(4.0D + level * 2.0D) + " blocks toward target; cooldown " + fmtSeconds(100L);
            case "berserker" -> id + " L" + level + ": at <=20% HP, gain Strength " + romanAmp(level - 1) + " and Resistance " + romanAmp(defensiveAuraInfoAmplifier(level)) + " for " + fmtSeconds(400L) + "; cooldown " + fmtSeconds(2000L);
            case "bloodlust" -> id + " L" + level + ": on kill, gain Strength " + romanAmp(level - 1) + " and Resistance " + romanAmp(defensiveAuraInfoAmplifier(level)) + " for " + fmtSeconds(200L) + "; no cooldown";
            case "retaliation_slow" -> id + " L" + level + ": on hurt, " + fmt(Math.min(0.85D, 0.20D + level * 0.04D) * 100.0D) + "% proc; radius " + fmt(2.0D + Math.max(0, level - (level / 3)) * 0.35D) + ", Slowness " + romanAmp(level / 3) + " for " + fmtSeconds(40L + level * 10L) + "; cooldown " + fmtSeconds(60L);
            case "immunity_frame" -> id + " L" + level + ": first hit is canceled and starts invulnerability for " + fmtSeconds(Math.min(100L, 5L + 5L * level)) + "; cooldown " + fmtSeconds(200L) + "; caps at 5.0s by level 19; while active, this tame cannot use abilities; passive/no active cast DPS";
            case "deflection" -> id + " L" + level + ": projectile deflect; reverses incoming projectile to 20% speed; passive reactive trigger";
            case "defusal" -> id + " L" + level + ": cancels nearby explosions; range " + fmt(10.0D + (level / 3) * 10.0D) + ", cooldown " + fmtSeconds(Math.max(0L, 100L - Math.max(0, level - 1) * 20L));
            case "psychic_wall" -> id + " L" + level + ": wall width " + (level + 1) + ", owner protect range " + fmt(5.0D + Math.max(0, level - 1) * 1.5D) + ", lifespan " + fmtSeconds(100L * level) + ", cooldown " + fmtSeconds(260L * level + 60L);
            case "healing_aura" -> id + " L" + level + ": regeneration pulse window " + fmtSeconds(200L) + " with Regeneration " + romanAmp(level / 4) + "; downtime 30-60s between cycles";
            case "healing_bottle" -> id + " L" + level + ": self-heal splash potion; instant heal " + (level >= 4 ? "II" : "I") + "; no regeneration; cooldown " + fmtSeconds(Math.max(60L, 220L - Math.max(0, Math.min(level, 4) - 1) * 15L));
            case "guardian_repulse" -> id + " L" + level + ": retarget aura radius " + fmt(6.0D + level * 0.6D) + ", " + fmt(Math.min(0.90D, 0.12D + level * 0.04D) * 100.0D) + "% chance per monster; cooldown " + fmtSeconds(60L);
            case "last_stand_fury" -> id + " L" + level + ": passive; as owner HP drops, tame gains scaling Strength/Speed buffs, refreshed every " + fmtSeconds(40L);
            case "shield_block" -> id + " L" + level + ": on hurt, reduces hit by " + fmt(Math.min(0.95D, 0.65D + (level - 1) * 0.03D) * 100.0D) + "%, sits for 1.0s, then restores previous order; cooldown " + fmtSeconds(Math.max(30L, 300L - Math.max(0, level - 1) * 20L));
            case "guardian_intercept" -> id + " L" + level + ": redirects " + fmt(Math.min(0.60D, 0.20D + 0.10D * level) * 100.0D) + "% of ally hit damage to supporter; cooldown " + fmtSeconds(Math.max(40L, 140L - level * 10L));
            case "emergency_shield" -> id + " L" + level + ": triggers if ally would fall below 35% HP; reduces triggering hit by " + fmt(Math.min(0.60D, 0.20D + level * 0.08D) * 100.0D) + "%, grants Absorption " + romanAmp((level - 1) / 2) + " for " + fmtSeconds(80L + level * 20L) + " and Resistance " + romanAmp(defensiveAuraInfoAmplifier(level)) + " for " + fmtSeconds(40L + level * 5L) + "; cooldown " + fmtSeconds(200L);
            case "body_block" -> id + " L" + level + ": projectile-only ally protection; prevents " + fmt(Math.min(0.90D, 0.45D + 0.10D * level) * 100.0D) + "% of hit; cooldown " + fmtSeconds(Math.max(40L, 180L - level * 15L));
            case "battlefield_medic" -> id + " L" + level + ": on kill, " + fmt(Math.min(1.0D, 0.30D + Math.max(0, level - 1) * 0.10D) * 100.0D) + "% chance to throw the same instant-heal bottle as healing_bottle";
            case "triage_pulse" -> id + " L" + level + ": heals lowest ally in 10 blocks for " + fmt(1.5D + 0.75D * level) + "; cooldown " + fmtSeconds(120L);
            case "revitalizing_presence" -> id + " L" + level + ": nearby allied tames within 2 blocks gain +" + level + " passive self-heal on each passive healing tick";
            case "cleanse_touch" -> id + " L" + level + ": removes 1 harmful effect from an ally within 10; cooldown " + fmtSeconds(Math.max(60L, 180L - Math.max(0, level - 1) * 15L));
            case "pack_guard" -> id + " L" + level + ": when ally is hurt by a monster, applies Weakness " + (level >= 4 ? "II" : "I") + " for " + fmtSeconds(60L + level * 20L) + " and retargets attacker; cooldown " + fmtSeconds(Math.max(40L, 140L - level * 10L));
            case "life_gift" -> id + " L" + level + ": lethal-save for allied tames; transfers up to " + fmt(2.0D + level) + " desired recovery HP from supporter while leaving supporter at >=5 HP; cooldown " + fmtSeconds(Math.max(2000L, 10000L - level * 200L));
            default -> id + " L" + level + ": utility/support ability, no fixed direct DPS";
        };
    }

    private static String inspectDamageAbilityLine(String id, int level, double castDamage, double attributeExtraDamage, String attributeNotes, long cooldownTicks, boolean longForm) {
        double cooldownSeconds = cooldownTicks / 20.0D;
        double totalCastDamage = castDamage + Math.max(0.0D, attributeExtraDamage);
        double dps = cooldownSeconds <= 0.0D ? 0.0D : totalCastDamage / cooldownSeconds;
        if (!longForm) {
            return id + "[" + level + "] -> " + fmt(dps) + "/s";
        }
        StringBuilder line = new StringBuilder(id)
                .append("[").append(level).append("]")
                .append(" -> ")
                .append(fmt(dps))
                .append("/s {(")
                .append(fmt(castDamage))
                .append(" ability+power+bonus");
        if (attributeExtraDamage > 0.0001D) {
            line.append(" + ").append(fmt(attributeExtraDamage)).append(" attr");
        }
        line.append(") / ").append(fmt(cooldownSeconds)).append("s");
        if (attributeNotes != null && !attributeNotes.isBlank()) {
            line.append(" [").append(attributeNotes).append("]");
        }
        line.append("}");
        return line.toString();
    }

    private static String inspectReworkedAbilityLine(TameData data, String id, int level, boolean longForm) {
        long budgetTicks = TameAbilityEvents.offensiveAbilityBudgetCooldownTicks(id, level);
        long effectiveTicks = inspectEffectiveCooldownTicks(data, id, budgetTicks);
        double castDamage = TameAbilityEvents.offensiveAbilityCastDamage(data, id, level);
        double attributeExtraDamage = inspectAbilityAttributeExtraDamage(data, id);
        String attributeNotes = inspectAbilityAttributeNotes(data);
        String aoe = inspectAbilityAoeDescriptor(id, level);
        String suffix = switch (id) {
            case "creeper_explosion", "dragon_fireball" -> " per target";
            case "ghast_fireball" -> " per target in blast";
            case "warden_scream" -> " per target in beam";
            case "elder_guardian_beam" -> " + mining fatigue";
            case "lightning_strike" -> " + visual lightning";
            case "fishing" -> " + pull only";
            case "dash" -> " per target hit in sweep";
            case "crossbow" -> " total cast damage assuming all " + Math.max(1, level) + " arrows hit";
            case "shulker_bullet" -> " + levitation utility";
            case "sky_launch" -> " + launch (reduced by target max HP)";
            default -> "";
        };
        String base = inspectDamageAbilityLine(id, level, castDamage, attributeExtraDamage, attributeNotes, effectiveTicks, longForm);
        if (longForm && !aoe.isBlank()) {
            base += " [" + aoe + "]";
        }
        return base + suffix;
    }

    private static String inspectAbilityAoeDescriptor(String id, int level) {
        int safeLevel = Math.max(1, level);
        return switch (id) {
            case "creeper_explosion" -> "AOE radius " + fmt(3.0D + (safeLevel - 1) * 0.3D);
            case "ghast_fireball" -> "blast AOE";
            case "guardian_beam" -> "single target beam";
            case "elder_guardian_beam" -> "single target beam";
            case "dash" -> "sweep box length " + fmt(Math.min(4.0D + safeLevel * 0.8D, 999.0D)) + ", width 2.2";
            case "evoker_fangs" -> (safeLevel >= 3 ? "line of 10 fangs" : "ring of 10 fangs, radius 3.0");
            case "dragon_fireball" -> "dragon cloud AOE";
            case "lightning_strike" -> "single target strike";
            case "warden_scream" -> "beam width " + fmt(1.1D);
            case "sky_launch" -> "single target launch; heavy targets resist lift";
            case "crossbow" -> "multi-shot x" + safeLevel;
            case "shulker_bullet" -> "single target homing";
            case "fishing" -> "single target pull; no damage";
            case "trident" -> "single target projectile";
            case "arrow_shot" -> "single target projectile";
            case "snowball_shot" -> "single target projectile";
            case "wither_skull" -> "single target projectile";
            case "blaze_attack" -> "single target projectile";
            case "llama_spit" -> "single target projectile";
            default -> "";
        };
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
            case "lightningfang" -> id + " L" + level + ": " + fmt(Math.min(0.35D, 0.10D + 0.05D * level) * 100.0D) + "% proc for " + fmt((2.0D + 2.0D * level) + (4.0D * 0.05D * Math.max(0.0D, data.bonusDamage))) + " bonus damage";
            case "firefang" -> id + " L" + level + ": burns target for " + (2 + level) + "s on hit" + (level >= 5 ? " and adds +2 damage" : level >= 3 ? " and adds +1 damage" : "");
            case "poison_fang" -> id + " L" + level + ": applies Poison " + (40 + level * 20) + " ticks on hit, amp " + (level >= 3 ? "II" : "I");
            case "witherfang" -> id + " L" + level + ": applies Wither " + (40 + level * 20) + " ticks on hit, amp " + (level >= 4 ? "II" : "I");
            case "frost_fang" -> id + " L" + level + ": " + fmt(Math.min(0.45D, 0.15D + Math.max(0, level - 1) * 0.075D) * 100.0D) + "% slow proc, amp " + (level >= 5 ? "III" : level >= 3 ? "II" : "I");
            case "chain_lightning" -> id + " L" + level + ": " + fmt(Math.min(0.28D, 0.12D + Math.max(0, level - 1) * 0.04D) * 100.0D) + "% proc; chains up to " + (1 + level) + " targets for " + fmt((2.0D + level) + (3.0D * 0.05D * 0.50D * Math.max(0.0D, data.bonusDamage))) + " aoe damage each";
            case "sweeping_edge" -> id + " L" + level + ": splash radius " + fmt(1.4D + Math.max(0, level - 1) * 0.20D) + ", splash scaling " + fmt(Math.min(0.50D, 0.20D + Math.max(0, level - 1) * 0.075D) * 100.0D) + "%";
            case "victim_siphon" -> id + " L" + level + ": on kill heals " + fmt(Math.min(0.35D, 0.04D + 0.04D * level) * 100.0D) + "% of victim max HP";
            case "killexploder" -> id + " L" + level + ": on kill/assist explodes for " + fmt((4.0D + Math.max(0, level - 1) * 1.5D) + (4.0D * 0.05D * 0.50D * Math.max(0.0D, data.bonusDamage))) + " aoe damage, radius " + fmt(2.0D + Math.max(0, level - 1) * 0.40D);
            case "positive_effect_steal" -> id + " L" + level + ": " + fmt(Math.min(0.38D, 0.08D + 0.06D * level) * 100.0D) + "% chance to steal one beneficial effect on hit";
            case "negative_effect_transfer" -> id + " L" + level + ": transfers harmful effects with x" + fmt(inspectAttributeLevelMultiplier(level)) + " duration";
            case "feather_falling" -> id + " L" + level + ": reduces fall damage by " + fmt(Math.min(0.70D, 0.20D + Math.max(0, level - 1) * 0.125D) * 100.0D) + "%";
            case "explosion_resistance" -> id + " L" + level + ": reduces explosion damage by " + fmt(Math.min(0.55D, 0.15D + Math.max(0, level - 1) * 0.10D) * 100.0D) + "%";
            case "regeneration" -> id + " L" + level + ": heals " + fmt(0.5D + 0.5D * level) + " every " + fmt((Math.max(20L, (level >= 5 ? 20L : level >= 3 ? 30L : 40L) + 40L)) / 20.0D) + "s while damaged";
            case "ability_power" -> id + " L" + level + ": +" + fmt(level * 15.0D) + "% level-1 ability damage";
            case "emergency_cooldown_reduction" -> id + " L" + level + ": at <=" + fmt((0.25D + Math.max(0, level - 1) * 0.025D) * 100.0D) + "% HP, " + fmt(Math.min(0.38D, 0.08D + 0.06D * level) * 100.0D) + "% chance to force next cooldown to 1s";
            case "totem" -> id + " L" + level + ": lethal save, cooldown " + fmt(Math.max(1L, 10L - Math.max(0, level - 1))) + "m";
            case "magnetic" -> id + " L" + level + ": pull utility; stronger target drag each level";
            case "speed" -> id + " L" + level + ": self-buff amplifier " + (level >= 5 ? "III" : level >= 3 ? "II" : "I");
            case "strength" -> id + " L" + level + ": self-buff amplifier " + (level >= 5 ? "III" : level >= 3 ? "II" : "I");
            case "jump_boost" -> id + " L" + level + ": self-buff amplifier " + (level >= 5 ? "III" : level >= 3 ? "II" : "I");
            case "resistance" -> id + " L" + level + ": self Resistance " + romanAmp(defensiveAuraInfoAmplifier(Math.min(level, 30))) + "; caps at level 30";
            case "fire_resistance" -> id + " L" + level + ": self Fire Resistance I; binary protection";
            case "poison_resistance" -> id + " L" + level + ": immediately clears Poison when applied; binary protection";
            case "comfort" -> id + " L" + level + ": when out of battle, heals " + fmt(level) + " every 5.0s";
            case "wall_climber" -> id + ": spider-style wall climbing while pressing into vertical surfaces";
            case "health_siphon" -> id + " L" + level + ": redirects incoming tame damage to owner while enabled by /tames healthSiphon; owner range " + fmt(Math.min(128.0D, 32.0D + 16.0D * Math.max(0, level - 1)));
            case "bubbling" -> id + " L" + level + ": vs enemies under the HP cap, " + fmt(Math.min(0.60D, 0.15D * level) * 100.0D) + "% proc to trap them in a giant bubble";
            case "herding" -> id + " L" + level + ": utility herding aura; range scales by level (roughly 8 at L1, 12 at L3, 16 at L5)";
            case "amphibious" -> id + " L" + level + ": cancels drown/dry-out damage and grants extra land speed while aquatic";
            case "void_cloud" -> id + " L" + level + ": negates fall and void-style fall damage";
            case "charisma" -> id + " L" + level + ": villager trade discount support; stronger discount each level";
            case "disc_jockey" -> id + " L" + level + ": enables jukebox-follow music utility; binary effect";
            case "warping_bite" -> id + " L" + level + ": base attacks can chorus-teleport enemy targets; level increases proc chance, attempts, and range";
            case "ore_scenting" -> id + " L" + level + ": ore-finding utility; binary effect";
            case "gluttonous" -> id + " L" + level + ": guaranteed milestone utility attribute unlocked at tame level 30";
            case "tethered_teleport" -> id + " L" + level + ": allows tethered owner-follow teleports and cross-dimension follow recovery";
            case "muffled" -> id + " L" + level + ": sound dampening utility; binary effect";
            case "blazing_protection" -> id + " L" + level + ": reactive flame shield; stored bars block hits, ignite attackers, and recover over time";
            case "rejuvenation" -> id + " L" + level + ": absorbs nearby XP orbs for the tame; utility attribute";
            case "linked_inventory" -> id + " L" + level + ": owner inventory-link utility; binary effect";
            default -> id + " L" + level + ": no inspect profile";
        };
    }

    private static double inspectAttributeLevelMultiplier(int level) {
        int safeLevel = Math.max(1, level);
        return 1.0D + (Math.max(0, safeLevel - 1) / 3.0D);
    }

    private static double inspectAbilityPowerBaseBonus(TameData data) {
        return LevelSystem.getAttributeLevel(data, "ability_power") * 0.15D;
    }

    private static double inspectAbilityAttributeExtraDamage(TameData data, String abilityId) {
        if (data == null || abilityId == null || abilityId.isBlank()) {
            return 0.0D;
        }
        double extra = 0.0D;

        int firefang = LevelSystem.getAttributeLevel(data, "firefang");
        if (firefang >= 5) {
            extra += 2.0D;
        } else if (firefang >= 3) {
            extra += 1.0D;
        }

        int lightningfang = LevelSystem.getAttributeLevel(data, "lightningfang");
        if (lightningfang > 0) {
            double chance = Math.min(0.35D, 0.10D + 0.05D * lightningfang);
            double procDamage = inspectSingleTargetDamage((2.0D + 2.0D * lightningfang) + (4.0D * 0.05D * Math.max(0.0D, data.bonusDamage)));
            extra += chance * procDamage;
        }

        int chainLightning = LevelSystem.getAttributeLevel(data, "chain_lightning");
        if (chainLightning > 0) {
            double chance = Math.min(0.28D, 0.12D + Math.max(0, chainLightning - 1) * 0.04D);
            double perChainDamage = inspectAoeDamage((2.0D + chainLightning) + (3.0D * 0.05D * 0.50D * Math.max(0.0D, data.bonusDamage)));
            extra += chance * Math.max(0, 1 + chainLightning) * perChainDamage;
        }

        return extra;
    }

    private static String inspectAbilityAttributeNotes(TameData data) {
        if (data == null) {
            return "";
        }
        List<String> notes = new ArrayList<>();
        if (LevelSystem.getAttributeLevel(data, "killer") > 0) {
            notes.add("killer conditional");
        }
        if (LevelSystem.getAttributeLevel(data, "pacifist") > 0) {
            notes.add("pacifist conditional");
        }
        if (LevelSystem.getAttributeLevel(data, "pierce") > 0) {
            notes.add("pierce vs armor");
        }
        if (LevelSystem.getAttributeLevel(data, "smite") > 0) {
            notes.add("smite vs undead");
        }
        if (LevelSystem.getAttributeLevel(data, "bane_of_arthropods") > 0) {
            notes.add("bane vs arthropods");
        }
        if (LevelSystem.getAttributeLevel(data, "sweeping_edge") > 0) {
            notes.add("sweep situational");
        }
        if (LevelSystem.getAttributeLevel(data, "poison_fang") > 0) {
            notes.add("poison DOT");
        }
        if (LevelSystem.getAttributeLevel(data, "witherfang") > 0) {
            notes.add("wither DOT");
        }
        return String.join(", ", notes);
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

    private static String formatBedLocation(MinecraftServer server, TameData data) {
        if (data == null || !data.hasPetBed || data.petBedDimension == null || data.petBedDimension.isBlank()) {
            return "-";
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(data.petBedDimension);
        String dimensionName = shortDimensionName(dimensionId, data.petBedDimension);
        String bedType = shortBedType(petBedBlockId(server, data));
        return bedType + " " + dimensionName + " [" + data.petBedX + ", " + data.petBedY + ", " + data.petBedZ + "]";
    }

    private static String shortDimensionName(ResourceLocation dimensionId, String fallback) {
        if (dimensionId == null) {
            return fallback == null || fallback.isBlank() ? "unknown" : fallback;
        }
        String path = dimensionId.getPath();
        if (path == null || path.isBlank()) {
            return dimensionId.toString();
        }
        return Arrays.stream(path.split("/"))
                .filter(part -> part != null && !part.isBlank())
                .map(TameCommands::capitalizeWords)
                .reduce((left, right) -> left + " / " + right)
                .orElse(path);
    }

    private static String shortBedType(ResourceLocation blockId) {
        if (blockId == null || !"domesticationinnovation".equals(blockId.getNamespace())) {
            return "Bed";
        }
        String path = blockId.getPath();
        if ("pet_bed_white".equals(path)) {
            return "White";
        }
        if ("pet_bed_black".equals(path)) {
            return "Black";
        }
        if (path != null && path.startsWith("pet_bed_")) {
            return "Colored";
        }
        return "Bed";
    }

    private static String capitalizeWords(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[] parts = raw.split("_");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            words.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
        }
        return String.join(" ", words);
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

    private static String fmtSeconds(long ticks) {
        return fmt(ticks / 20.0D) + "s";
    }

    private static String romanAmp(int amplifier) {
        int tier = Math.max(0, amplifier) + 1;
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(tier);
        };
    }

    private static int defensiveAuraInfoAmplifier(int levelValue) {
        int displayedResistance = 1;
        int threshold = 1;
        while (displayedResistance < 4) {
            int nextLevel = displayedResistance + 1;
            threshold += nextLevel * nextLevel;
            if (levelValue < threshold) {
                break;
            }
            displayedResistance = nextLevel;
        }
        return displayedResistance - 1;
    }

    private record ModeSpec(TameMode mode, Integer bodyguardRange) {
    }

    private static ModeSpec parseModeSpec(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        String[] tokens = trimmed.split("\\s+");
        TameMode mode = TameMode.tryByName(tokens[0]);
        if (mode == null) {
            return null;
        }
        Integer bodyguardRange = null;
        if (mode == TameMode.BODYGUARD && tokens.length >= 2) {
            try {
                bodyguardRange = Math.max(1, Integer.parseInt(tokens[1]));
            } catch (NumberFormatException ignored) {
                return null;
            }
        } else if (tokens.length >= 2) {
            return null;
        }
        return new ModeSpec(mode, bodyguardRange);
    }

    private static void applyModeSpec(TameData data, TameMode mode, Integer bodyguardRange) {
        data.mode = mode.id();
        if (mode == TameMode.BODYGUARD && bodyguardRange != null) {
            data.bodyguardRange = bodyguardRange;
        }
    }

    private static String modeLabel(TameMode mode, TameData data) {
        if (mode == TameMode.BODYGUARD) {
            int range = data == null ? 12 : Math.max(1, data.bodyguardRange);
            return mode.key() + " " + range;
        }
        return mode.key();
    }

    private static int setMode(CommandSourceStack source, String pet, String modeName) {
        ServerPlayer p = source.getPlayer();
        TameData d = findOwnedTame(p.getUUID(), pet);
        if (d == null) return error(p, "Pet not found.");
        ModeSpec spec = parseModeSpec(modeName);
        if (spec == null) return error(p, "Invalid mode.");
        TameMode mode = spec.mode();
        applyModeSpec(d, mode, spec.bodyguardRange());
        Entity e = p.serverLevel().getEntity(d.uuid);
        if (e instanceof TamableAnimal ta && mode != TameMode.PASSIVE) ta.setOrderedToSit(false);
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Mode set to " + modeLabel(mode, d) + " for " + d.name + "."));
        return 1;
    }

    private static int groupMode(CommandSourceStack source, String group, String modeName) {
        ServerPlayer p = source.getPlayer();
        ModeSpec spec = parseModeSpec(modeName);
        if (spec == null) return error(p, "Invalid mode.");
        TameMode mode = spec.mode();
        int count = 0;
        TameData sample = null;
        for (TameData d : ownedGroup(p.getUUID(), group)) {
            applyModeSpec(d, mode, spec.bodyguardRange());
            Entity e = p.serverLevel().getEntity(d.uuid);
            if (e instanceof TamableAnimal ta && mode != TameMode.PASSIVE) ta.setOrderedToSit(false);
            if (sample == null) {
                sample = d;
            }
            count++;
        }
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Set mode " + modeLabel(mode, sample) + " for " + count + " tames."));
        return 1;
    }

    private static int typeMode(CommandSourceStack source, String typeFilter, String modeName) {
        ServerPlayer p = source.getPlayer();
        ModeSpec spec = parseModeSpec(modeName);
        if (spec == null) return error(p, "Invalid mode.");
        TameMode mode = spec.mode();
        int count = 0;
        TameData sample = null;
        for (TameData d : ownedType(p.getUUID(), typeFilter)) {
            applyModeSpec(d, mode, spec.bodyguardRange());
            Entity e = p.serverLevel().getEntity(d.uuid);
            if (e instanceof TamableAnimal ta && mode != TameMode.PASSIVE) {
                applySitFollowOverride(ta, false);
            }
            if (sample == null) {
                sample = d;
            }
            count++;
        }
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Set mode " + modeLabel(mode, sample) + " for " + count + " tames of type '" + typeFilter + "'."));
        return 1;
    }

    private static int allMode(CommandSourceStack source, String modeName) {
        ServerPlayer p = source.getPlayer();
        ModeSpec spec = parseModeSpec(modeName);
        if (spec == null) return error(p, "Invalid mode.");
        TameMode mode = spec.mode();
        int count = 0;
        TameData sample = null;
        for (TameData d : ownedTames(p.getUUID())) {
            Entity e = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (!(e instanceof TamableAnimal ta) || !ta.isAlive()) continue;
            applyModeSpec(d, mode, spec.bodyguardRange());
            if (sample == null) {
                sample = d;
            }
            count++;
        }
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Set mode " + modeLabel(mode, sample) + " for " + count + " loaded tames."));
        return 1;
    }

    private static int stateMode(CommandSourceStack source, String stateName, String modeName) {
        ServerPlayer p = source.getPlayer();
        MovementOrder selectedState = parseMovementOrder(stateName);
        if (selectedState == null) return error(p, "Invalid state. Use follow, wander, or sit.");
        ModeSpec spec = parseModeSpec(modeName);
        if (spec == null) return error(p, "Invalid mode.");
        TameMode mode = spec.mode();

        int count = 0;
        TameData sample = null;
        for (TamableAnimal tame : loadedOwnedStateTames(source, p.getUUID(), selectedState)) {
            TameData d = TameRegistry.get(tame.getUUID());
            if (d == null) continue;
            applyModeSpec(d, mode, spec.bodyguardRange());
            if (mode != TameMode.PASSIVE) {
                applySitFollowOverride(tame, false);
            }
            if (sample == null) {
                sample = d;
            }
            count++;
        }
        TameRegistry.markDirty();
        p.sendSystemMessage(Component.literal("Set mode " + modeLabel(mode, sample) + " for " + count + " loaded " + movementLabel(selectedState) + " tames."));
        return 1;
    }

    private static int setMovementProfile(CommandSourceStack source, String pet, String profileName) {
        ServerPlayer player = source.getPlayer();
        TameData data = findOwnedTame(player.getUUID(), pet);
        if (data == null) return error(player, "Pet not found.");
        String profile = parseMovementProfile(profileName);
        if (profile == null) return error(player, "Invalid movement. Use default, skeleton, or close.");
        applyMovementProfile(data, profile);
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Movement set to " + movementProfileLabel(data) + " for " + data.name + "."));
        return 1;
    }

    private static int groupMovementProfile(CommandSourceStack source, String group, String profileName) {
        ServerPlayer player = source.getPlayer();
        String profile = parseMovementProfile(profileName);
        if (profile == null) return error(player, "Invalid movement. Use default, skeleton, or close.");
        int count = 0;
        for (TameData data : ownedGroup(player.getUUID(), group)) {
            applyMovementProfile(data, profile);
            count++;
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Set movement " + profile + " for " + count + " tames."));
        return 1;
    }

    private static int typeMovementProfile(CommandSourceStack source, String typeFilter, String profileName) {
        ServerPlayer player = source.getPlayer();
        String profile = parseMovementProfile(profileName);
        if (profile == null) return error(player, "Invalid movement. Use default, skeleton, or close.");
        int count = 0;
        for (TameData data : ownedType(player.getUUID(), typeFilter)) {
            applyMovementProfile(data, profile);
            count++;
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Set movement " + profile + " for " + count + " tames of type '" + typeFilter + "'."));
        return 1;
    }

    private static int allMovementProfile(CommandSourceStack source, String profileName) {
        ServerPlayer player = source.getPlayer();
        String profile = parseMovementProfile(profileName);
        if (profile == null) return error(player, "Invalid movement. Use default, skeleton, or close.");
        int count = 0;
        for (TameData data : ownedTames(player.getUUID())) {
            applyMovementProfile(data, profile);
            count++;
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Set movement " + profile + " for " + count + " tames."));
        return 1;
    }

    private static int stateMovementProfile(CommandSourceStack source, String stateName, String profileName) {
        ServerPlayer player = source.getPlayer();
        MovementOrder order = parseMovementOrder(stateName);
        if (order == null) return error(player, "Invalid state. Use follow, wander, or sit.");
        String profile = parseMovementProfile(profileName);
        if (profile == null) return error(player, "Invalid movement. Use default, skeleton, or close.");
        int count = 0;
        for (TamableAnimal tame : loadedOwnedStateTames(source, player.getUUID(), order)) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null) continue;
            applyMovementProfile(data, profile);
            count++;
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Set movement " + profile + " for " + count + " loaded " + movementLabel(order) + " tames."));
        return 1;
    }

    private static int guardianPet(CommandSourceStack source, String pet) {
        ServerPlayer player = source.getPlayer();
        TameData data = findOwnedTame(player.getUUID(), pet);
        if (data == null) return error(player, "Pet not found.");
        Entity entity = player.serverLevel().getEntity(data.uuid);
        if (!(entity instanceof TamableAnimal tame) || !tame.isAlive()) {
            return error(player, "Pet is not loaded.");
        }
        setGuardianAnchor(player, tame, data);
        player.sendSystemMessage(Component.literal("Set guardian anchor for " + data.name + " at your current location."));
        return 1;
    }

    private static int guardianPetHome(CommandSourceStack source, String pet) {
        ServerPlayer player = source.getPlayer();
        TameData data = findOwnedTame(player.getUUID(), pet);
        if (data == null) return error(player, "Pet not found.");
        Entity entity = player.serverLevel().getEntity(data.uuid);
        if (!(entity instanceof TamableAnimal tame) || !tame.isAlive()) {
            return error(player, "Pet is not loaded.");
        }
        setGuardianAnchorWithoutRemember(player, tame, data);
        player.sendSystemMessage(Component.literal("Set home guardian anchor for " + data.name + " at your current location."));
        return 1;
    }

    private static int guardianPetPrevious(CommandSourceStack source, String pet) {
        ServerPlayer player = source.getPlayer();
        TameData data = findOwnedTame(player.getUUID(), pet);
        if (data == null) return error(player, "Pet not found.");
        Entity entity = player.serverLevel().getEntity(data.uuid);
        if (!(entity instanceof TamableAnimal tame) || !tame.isAlive()) {
            return error(player, "Pet is not loaded.");
        }
        if (!restorePreviousGuardianAnchor(tame, data)) {
            return error(player, "No previous guardian anchor stored for " + data.name + ".");
        }
        player.sendSystemMessage(Component.literal("Restored previous guardian anchor for " + data.name + "."));
        return 1;
    }

    private static int guardianGroup(CommandSourceStack source, String group) {
        ServerPlayer player = source.getPlayer();
        int count = 0;
        for (TameData data : ownedGroup(player.getUUID(), group)) {
            Entity entity = player.serverLevel().getEntity(data.uuid);
            if (entity instanceof TamableAnimal tame && tame.isAlive()) {
                setGuardianAnchor(player, tame, data);
                count++;
            }
        }
        player.sendSystemMessage(Component.literal("Set guardian anchor for " + count + " tames in group '" + group + "'."));
        return 1;
    }

    private static int guardianGroupHome(CommandSourceStack source, String group) {
        ServerPlayer player = source.getPlayer();
        int count = 0;
        for (TameData data : ownedGroup(player.getUUID(), group)) {
            Entity entity = player.serverLevel().getEntity(data.uuid);
            if (entity instanceof TamableAnimal tame && tame.isAlive()) {
                setGuardianAnchorWithoutRemember(player, tame, data);
                count++;
            }
        }
        player.sendSystemMessage(Component.literal("Set home guardian anchor for " + count + " tames in group '" + group + "'."));
        return 1;
    }

    private static int guardianGroupPrevious(CommandSourceStack source, String group) {
        ServerPlayer player = source.getPlayer();
        int count = 0;
        for (TameData data : ownedGroup(player.getUUID(), group)) {
            Entity entity = player.serverLevel().getEntity(data.uuid);
            if (entity instanceof TamableAnimal tame && tame.isAlive() && restorePreviousGuardianAnchor(tame, data)) {
                count++;
            }
        }
        if (count <= 0) {
            return error(player, "No previous guardian anchors found in group '" + group + "'.");
        }
        player.sendSystemMessage(Component.literal("Restored previous guardian anchors for " + count + " tames in group '" + group + "'."));
        return 1;
    }

    private static int guardianType(CommandSourceStack source, String typeFilter) {
        ServerPlayer player = source.getPlayer();
        int count = 0;
        for (TameData data : ownedType(player.getUUID(), typeFilter)) {
            Entity entity = player.serverLevel().getEntity(data.uuid);
            if (entity instanceof TamableAnimal tame && tame.isAlive()) {
                setGuardianAnchor(player, tame, data);
                count++;
            }
        }
        player.sendSystemMessage(Component.literal("Set guardian anchor for " + count + " tames of type '" + typeFilter + "'."));
        return 1;
    }

    private static int guardianTypeHome(CommandSourceStack source, String typeFilter) {
        ServerPlayer player = source.getPlayer();
        int count = 0;
        for (TameData data : ownedType(player.getUUID(), typeFilter)) {
            Entity entity = player.serverLevel().getEntity(data.uuid);
            if (entity instanceof TamableAnimal tame && tame.isAlive()) {
                setGuardianAnchorWithoutRemember(player, tame, data);
                count++;
            }
        }
        player.sendSystemMessage(Component.literal("Set home guardian anchor for " + count + " tames of type '" + typeFilter + "'."));
        return 1;
    }

    private static int guardianTypePrevious(CommandSourceStack source, String typeFilter) {
        ServerPlayer player = source.getPlayer();
        int count = 0;
        for (TameData data : ownedType(player.getUUID(), typeFilter)) {
            Entity entity = player.serverLevel().getEntity(data.uuid);
            if (entity instanceof TamableAnimal tame && tame.isAlive() && restorePreviousGuardianAnchor(tame, data)) {
                count++;
            }
        }
        if (count <= 0) {
            return error(player, "No previous guardian anchors found for type '" + typeFilter + "'.");
        }
        player.sendSystemMessage(Component.literal("Restored previous guardian anchors for " + count + " tames of type '" + typeFilter + "'."));
        return 1;
    }

    private static int guardianState(CommandSourceStack source, String stateName) {
        ServerPlayer player = source.getPlayer();
        MovementOrder order = parseMovementOrder(stateName);
        if (order == null) return error(player, "Invalid state. Use follow, wander, or sit.");
        int count = 0;
        for (TamableAnimal tame : loadedOwnedStateTames(source, player.getUUID(), order)) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null) continue;
            setGuardianAnchor(player, tame, data);
            count++;
        }
        player.sendSystemMessage(Component.literal("Set guardian anchor for " + count + " loaded " + movementLabel(order) + " tames."));
        return 1;
    }

    private static int guardianStateHome(CommandSourceStack source, String stateName) {
        ServerPlayer player = source.getPlayer();
        MovementOrder order = parseMovementOrder(stateName);
        if (order == null) return error(player, "Invalid state. Use follow, wander, or sit.");
        int count = 0;
        for (TamableAnimal tame : loadedOwnedStateTames(source, player.getUUID(), order)) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null) continue;
            setGuardianAnchorWithoutRemember(player, tame, data);
            count++;
        }
        player.sendSystemMessage(Component.literal("Set home guardian anchor for " + count + " loaded " + movementLabel(order) + " tames."));
        return 1;
    }

    private static int guardianStatePrevious(CommandSourceStack source, String stateName) {
        ServerPlayer player = source.getPlayer();
        MovementOrder order = parseMovementOrder(stateName);
        if (order == null) return error(player, "Invalid state. Use follow, wander, or sit.");
        int count = 0;
        for (TamableAnimal tame : loadedOwnedStateTames(source, player.getUUID(), order)) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data != null && restorePreviousGuardianAnchor(tame, data)) {
                count++;
            }
        }
        if (count <= 0) {
            return error(player, "No previous guardian anchors found for loaded " + movementLabel(order) + " tames.");
        }
        player.sendSystemMessage(Component.literal("Restored previous guardian anchors for " + count + " loaded " + movementLabel(order) + " tames."));
        return 1;
    }

    private static int setGuardianAllLoaded(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        int count = 0;
        for (TamableAnimal tame : loadedOwnedAllTames(source, player.getUUID())) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null || !tame.isAlive()) continue;
            setGuardianAnchor(player, tame, data);
            count++;
        }
        player.sendSystemMessage(Component.literal("Set guardian anchor for " + count + " loaded tames."));
        return 1;
    }

    private static int setGuardianAllLoadedHome(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        int count = 0;
        for (TamableAnimal tame : loadedOwnedAllTames(source, player.getUUID())) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null || !tame.isAlive()) continue;
            setGuardianAnchorWithoutRemember(player, tame, data);
            count++;
        }
        player.sendSystemMessage(Component.literal("Set home guardian anchor for " + count + " loaded tames."));
        return 1;
    }

    private static int setGuardianAllLoadedPrevious(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        int count = 0;
        for (TamableAnimal tame : loadedOwnedAllTames(source, player.getUUID())) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data != null && tame.isAlive() && restorePreviousGuardianAnchor(tame, data)) {
                count++;
            }
        }
        if (count <= 0) {
            return error(player, "No previous guardian anchors found for your loaded tames.");
        }
        player.sendSystemMessage(Component.literal("Restored previous guardian anchors for " + count + " loaded tames."));
        return 1;
    }

    private static int guardianList(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        List<TameData> guardians = new ArrayList<>();
        for (TameData data : ownedTames(player.getUUID())) {
            if (data != null && data.hasHome) {
                guardians.add(data);
            }
        }
        if (guardians.isEmpty()) {
            return error(player, "You have no guardian tames.");
        }
        guardians.sort(Comparator.comparing(data -> data.name == null ? "" : data.name.toLowerCase(Locale.ROOT)));
        player.sendSystemMessage(Component.literal("---- Guardian Tames (" + guardians.size() + ") ----").withStyle(ChatFormatting.GOLD));
        for (TameData data : guardians) {
            String dimension = data.homeDimension == null || data.homeDimension.isBlank() ? "unknown" : data.homeDimension;
            player.sendSystemMessage(Component.literal(
                    "[" + data.level + "] " + data.name + " @ " + dimension + " " + data.homeX + " " + data.homeY + " " + data.homeZ
            ).withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static int guardianNamedSetPet(CommandSourceStack source, String setName, String pet) {
        ServerPlayer player = source.getPlayer();
        TameData data = findOwnedTame(player.getUUID(), pet);
        if (data == null) return error(player, "Pet not found.");
        String normalizedSet = normalizeGuardianSetName(setName);
        if (normalizedSet == null) return error(player, "Guardian set name cannot be blank.");
        putGuardianSetAnchor(data, normalizedSet, player.serverLevel().dimension().location().toString(), player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ());
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Saved deployment group '" + normalizedSet + "' for " + data.name + " at your current location.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int guardianRemoveSetPet(CommandSourceStack source, String setName, String pet) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        TameData data = findOwnedTame(player.getUUID(), pet);
        if (data == null) return error(player, "Pet not found.");
        String normalizedSet = normalizeGuardianSetName(setName);
        if (normalizedSet == null) return error(player, "Guardian set name cannot be blank.");
        if (data.guardianSetAnchors.remove(normalizedSet) == null) {
            return error(player, tameDisplayName(data) + " is not in deployment group '" + normalizedSet + "'.");
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Removed " + tameDisplayName(data) + " from deployment group '" + normalizedSet + "'.").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int guardianRemoveSetAll(CommandSourceStack source, String setName) {
        ServerPlayer player = source.getPlayer();
        return guardianRemoveSet(source, setName, ownedTames(player.getUUID()), "all owned tames");
    }

    private static int guardianRemoveSetGroup(CommandSourceStack source, String setName, String group) {
        ServerPlayer player = source.getPlayer();
        return guardianRemoveSet(source, setName, ownedGroup(player.getUUID(), group), "group '" + group + "'");
    }

    private static int guardianRemoveSetType(CommandSourceStack source, String setName, String typeFilter) {
        ServerPlayer player = source.getPlayer();
        return guardianRemoveSet(source, setName, ownedType(player.getUUID(), typeFilter), "type '" + typeFilter + "'");
    }

    private static int guardianRemoveSetState(CommandSourceStack source, String setName, String stateName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        MovementOrder order = parseMovementOrder(stateName);
        if (order == null) return error(player, "Invalid state. Use follow, wander, or sit.");
        return guardianRemoveSet(source, setName, ownedState(player.getUUID(), order), "state '" + movementLabel(order) + "'");
    }

    private static int guardianDeleteSet(CommandSourceStack source, String setName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        String normalizedSet = normalizeGuardianSetName(setName);
        if (normalizedSet == null) return error(player, "Guardian set name cannot be blank.");
        int removed = 0;
        for (TameData data : ownedTames(player.getUUID())) {
            if (data == null) {
                continue;
            }
            if (data.guardianSetAnchors.remove(normalizedSet) != null) {
                removed++;
            }
        }
        if (removed <= 0) {
            return error(player, "Deployment group '" + normalizedSet + "' does not exist on any of your tames.");
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Deleted deployment group '" + normalizedSet + "' from " + removed + " tame(s).").withStyle(ChatFormatting.YELLOW));
        return removed;
    }

    private static int guardianRemoveSet(CommandSourceStack source, String setName, List<TameData> selected, String label) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        String normalizedSet = normalizeGuardianSetName(setName);
        if (normalizedSet == null) return error(player, "Guardian set name cannot be blank.");
        if (selected == null || selected.isEmpty()) {
            return error(player, "No tames found for " + label + ".");
        }
        int removed = 0;
        for (TameData data : selected) {
            if (data == null) {
                continue;
            }
            if (data.guardianSetAnchors.remove(normalizedSet) != null) {
                removed++;
            }
        }
        if (removed <= 0) {
            return error(player, "No deployment-group entries removed for " + label + ".");
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Removed deployment group '" + normalizedSet + "' from " + removed + " tame(s) in " + label + ".").withStyle(ChatFormatting.YELLOW));
        return removed;
    }

    private static int guardianCaptureCurrentAll(CommandSourceStack source, String setName, boolean includeInactiveCurrent) {
        ServerPlayer player = source.getPlayer();
        return guardianCaptureCurrent(source, setName, ownedTames(player.getUUID()), includeInactiveCurrent, "all");
    }

    private static int guardianCaptureCurrentGroup(CommandSourceStack source, String setName, String group, boolean includeInactiveCurrent) {
        ServerPlayer player = source.getPlayer();
        return guardianCaptureCurrent(source, setName, ownedGroup(player.getUUID(), group), includeInactiveCurrent, "group '" + group + "'");
    }

    private static int guardianCaptureCurrentType(CommandSourceStack source, String setName, String typeFilter, boolean includeInactiveCurrent) {
        ServerPlayer player = source.getPlayer();
        return guardianCaptureCurrent(source, setName, ownedType(player.getUUID(), typeFilter), includeInactiveCurrent, "type '" + typeFilter + "'");
    }

    private static int guardianCaptureCurrentState(CommandSourceStack source, String setName, String stateName, boolean includeInactiveCurrent) {
        ServerPlayer player = source.getPlayer();
        MovementOrder order = parseMovementOrder(stateName);
        if (order == null) return error(player, "Invalid state. Use follow, wander, or sit.");
        List<TameData> selected = new ArrayList<>();
        for (TamableAnimal tame : loadedOwnedStateTames(source, player.getUUID(), order)) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data != null) {
                selected.add(data);
            }
        }
        return guardianCaptureCurrent(source, setName, selected, includeInactiveCurrent, "loaded " + movementLabel(order));
    }

    private static int guardianCaptureCurrent(CommandSourceStack source, String setName, List<TameData> selected, boolean includeInactiveCurrent, String label) {
        ServerPlayer player = source.getPlayer();
        String normalizedSet = normalizeGuardianSetName(setName);
        if (normalizedSet == null) return error(player, "Guardian set name cannot be blank.");
        if (selected == null || selected.isEmpty()) return error(player, "No tames found for " + label + ".");

        int captured = 0;
        int skippedNoAnchor = 0;
        int skippedInactive = 0;
        for (TameData data : selected) {
            if (data == null || data.dead || !data.hasHome) {
                skippedNoAnchor++;
                continue;
            }
            boolean loadedAlive = isTameLoadedAnywhere(source.getServer(), data);
            if (!includeInactiveCurrent && !loadedAlive) {
                skippedInactive++;
                continue;
            }
            putGuardianSetAnchor(data, normalizedSet, data.homeDimension, data.homeX, data.homeY, data.homeZ);
            captured++;
        }
        if (captured <= 0) {
            return error(player, "No current guardian anchors captured for " + label + ".");
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal(
                "Captured " + captured + " current guardian anchor(s) into set '" + normalizedSet + "' from " + label
                        + (includeInactiveCurrent ? " (including inactive current)." : ".")
                        + (skippedInactive > 0 ? " Skipped inactive current: " + skippedInactive + "." : "")
                        + (skippedNoAnchor > 0 ? " Skipped without current anchor: " + skippedNoAnchor + "." : "")
        ).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int guardianDeploySet(CommandSourceStack source, String setName) {
        ServerPlayer player = source.getPlayer();
        String normalizedSet = normalizeGuardianSetName(setName);
        if (normalizedSet == null) return error(player, "Guardian set name cannot be blank.");

        List<TameData> members = ownedGuardianSetMembers(player.getUUID(), normalizedSet);
        if (members.isEmpty()) {
            return error(player, "No tames are part of deployment group '" + normalizedSet + "'.");
        }

        int deployed = 0;
        int skippedDead = 0;
        int skippedUnloaded = 0;
        int skippedInvalid = 0;
        List<String> failedNames = new ArrayList<>();

        for (TameData data : members) {
            if (data.dead) {
                skippedDead++;
                continue;
            }
            TamableAnimal tame = findLoadedTameByIdentity(source.getServer(), data.uuid, data.tlId);
            if (tame == null || !tame.isAlive()) {
                skippedUnloaded++;
                continue;
            }
            CompoundTag anchor = getGuardianSetAnchor(data, normalizedSet);
            SpawnTarget target = spawnTargetFromGuardianSet(source, anchor);
            if (target == null) {
                skippedInvalid++;
                failedNames.add((data.name == null ? "unknown" : data.name) + " (invalid set location)");
                continue;
            }
            if (deployLoadedTameToGuardianSet(tame, data, target)) {
                deployed++;
            } else {
                skippedInvalid++;
                failedNames.add((data.name == null ? "unknown" : data.name) + " (teleport failed)");
            }
        }

        player.sendSystemMessage(Component.literal(
                    "Guardian deploy '" + normalizedSet + "': deployed " + deployed
                        + ", skipped unloaded " + skippedUnloaded
                        + ", dead " + skippedDead
                        + ", invalid " + skippedInvalid + "."
        ).withStyle(ChatFormatting.GOLD));
        if (!failedNames.isEmpty()) {
            player.sendSystemMessage(Component.literal("Guardian deploy failures: " + String.join("; ", failedNames)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int guardianDeployCurrentAll(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return guardianDeployCurrent(source, player, ownedTames(player.getUUID()), "all current guardian anchors");
    }

    private static int guardianDeployCurrentGroup(CommandSourceStack source, String group) {
        ServerPlayer player = source.getPlayer();
        return guardianDeployCurrent(source, player, ownedGroup(player.getUUID(), group), "group '" + group + "'");
    }

    private static int guardianDeployCurrentType(CommandSourceStack source, String typeFilter) {
        ServerPlayer player = source.getPlayer();
        return guardianDeployCurrent(source, player, ownedType(player.getUUID(), typeFilter), "type '" + typeFilter + "'");
    }

    private static int guardianDeployCurrentState(CommandSourceStack source, String stateName) {
        ServerPlayer player = source.getPlayer();
        MovementOrder order = parseMovementOrder(stateName);
        if (order == null) {
            return error(player, "State must be follow, sit, or wander.");
        }
        List<TameData> selected = new ArrayList<>();
        for (TamableAnimal tame : loadedOwnedStateTames(source, player.getUUID(), order)) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data != null) {
                selected.add(data);
            }
        }
        return guardianDeployCurrent(source, player, selected, "loaded " + movementLabel(order) + " tames");
    }

    private static int guardianDeployCurrent(CommandSourceStack source, ServerPlayer player, List<TameData> requested, String label) {
        if (requested == null || requested.isEmpty()) {
            return error(player, "No tames found for " + label + ".");
        }

        List<TamableAnimal> loadedTargets = new ArrayList<>();
        List<SpawnTarget> loadedDestinations = new ArrayList<>();
        List<TameData> unloadedTargets = new ArrayList<>();
        List<SpawnTarget> unloadedDestinations = new ArrayList<>();
        List<String> failedNames = new ArrayList<>();
        int deadSkipped = 0;
        int queueFailed = 0;
        int queued = 0;
        int cost = 0;
        int crossDimension = 0;

        for (TameData data : requested) {
            if (data == null || data.uuid == null) {
                continue;
            }
            if (data.dead) {
                deadSkipped++;
                continue;
            }
            SpawnTarget target = spawnTargetFromCurrentGuardian(source, data);
            if (target == null || target.level == null || target.pos == null) {
                queueFailed++;
                failedNames.add((data.name == null ? "unknown" : data.name) + " (missing current guardian location)");
                continue;
            }
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, player.getUUID(), data.uuid);
            if (tame == null) {
                String queueError = validateUnloadedHomeTeleport(source, player, data, target);
                if (queueError != null && (data.entitySnapshot == null || data.entitySnapshot.isEmpty())) {
                    queueFailed++;
                    failedNames.add((data.name == null ? "unknown" : data.name) + " (" + queueError + ")");
                    continue;
                }
                unloadedTargets.add(data);
                unloadedDestinations.add(target);
                boolean cross = data.lastKnownDimension != null
                        && !data.lastKnownDimension.isBlank()
                        && !target.level.dimension().location().toString().equals(data.lastKnownDimension);
                cost += teleportCostFor(data, cross);
                if (cross) {
                    crossDimension++;
                }
                continue;
            }
            boolean cross = !tame.level().dimension().equals(target.level.dimension());
            loadedTargets.add(tame);
            loadedDestinations.add(target);
            cost += teleportCostFor(data, cross);
            if (cross) {
                crossDimension++;
            }
        }

        if (loadedTargets.isEmpty() && unloadedTargets.isEmpty()) {
            return error(player, "No current guardian locations available for " + label + ".");
        }
        if (!payTeleportXp(player, cost)) {
            return 0;
        }
        for (int i = 0; i < loadedTargets.size(); i++) {
            deployLoadedTameToGuardianCurrent(loadedTargets.get(i), loadedDestinations.get(i));
        }
        for (int i = 0; i < unloadedTargets.size(); i++) {
            UnloadedTpResult unloaded = tpUnloadedHomeViaLanternOrRecover(source, player, unloadedTargets.get(i), unloadedDestinations.get(i));
            if (unloaded.success) {
                queued++;
            } else {
                queueFailed++;
                failedNames.add((unloadedTargets.get(i).name == null ? "unknown" : unloadedTargets.get(i).name) + " (" + unloaded.error + ")");
            }
        }

        sendTeleportSummary(player, "Guardian deploy " + label, loadedTargets.size(), queued, deadSkipped, queueFailed, cost, crossDimension);
        if (!failedNames.isEmpty()) {
            player.sendSystemMessage(Component.literal("Guardian deploy failures: " + String.join("; ", failedNames)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int guardianSetInfo(CommandSourceStack source, String setName) {
        ServerPlayer player = source.getPlayer();
        String normalizedSet = normalizeGuardianSetName(setName);
        if (normalizedSet == null) return error(player, "Guardian set name cannot be blank.");
        List<TameData> members = ownedGuardianSetMembers(player.getUUID(), normalizedSet);
        if (members.isEmpty()) {
            return error(player, "No tames are part of deployment group '" + normalizedSet + "'.");
        }
        members.sort(Comparator.comparing(data -> data.name == null ? "" : data.name.toLowerCase(Locale.ROOT)));
        player.sendSystemMessage(Component.literal("---- Deployment Group '" + normalizedSet + "' (" + members.size() + ") ----").withStyle(ChatFormatting.GOLD));
        for (TameData data : members) {
            CompoundTag anchor = getGuardianSetAnchor(data, normalizedSet);
            String dimension = anchor == null ? "unknown" : anchor.getString("dimension");
            int x = anchor == null ? 0 : anchor.getInt("x");
            int y = anchor == null ? 0 : anchor.getInt("y");
            int z = anchor == null ? 0 : anchor.getInt("z");
            boolean loaded = isTameLoadedAnywhere(source.getServer(), data);
            ChatFormatting color = statusColor(data, loaded);
            String state = statusLabel(data, source.getServer());
            player.sendSystemMessage(Component.literal(
                    "[" + data.level + "] " + data.name + " [" + state + "] @ " + dimension + " " + x + " " + y + " " + z
            ).withStyle(color));
        }
        return 1;
    }

    private static int groupCreate(CommandSourceStack source, String group) {
        ServerPlayer player = source.getPlayer();
        if (player == null || group == null || group.isBlank()) {
            return 0;
        }
        TameRegistry.rememberGroup(player.getUUID(), group);
        player.sendSystemMessage(Component.literal("Created group '" + group + "'.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int groupAssignPet(CommandSourceStack source, String group, String pet) {
        ServerPlayer player = source.getPlayer();
        TameData data = findOwnedTame(player.getUUID(), pet);
        if (data == null) return error(player, "Pet not found.");
        return groupAssignBatch(source, group, List.of(data), "'" + data.name + "'");
    }

    private static int groupAssignGroup(CommandSourceStack source, String group, String selectedGroup) {
        ServerPlayer player = source.getPlayer();
        return groupAssignBatch(source, group, ownedGroup(player.getUUID(), selectedGroup), "group '" + selectedGroup + "'");
    }

    private static int groupAssignType(CommandSourceStack source, String group, String type) {
        ServerPlayer player = source.getPlayer();
        return groupAssignBatch(source, group, ownedType(player.getUUID(), type), "type '" + type + "'");
    }

    private static int groupAssignState(CommandSourceStack source, String group, MovementOrder order) {
        ServerPlayer player = source.getPlayer();
        return groupAssignBatch(source, group, ownedState(player.getUUID(), order), movementLabel(order));
    }

    private static int groupAssignAll(CommandSourceStack source, String group) {
        ServerPlayer player = source.getPlayer();
        return groupAssignBatch(source, group, ownedTames(player.getUUID()), "all");
    }

    private static int groupRemoveSelectionPet(CommandSourceStack source, String group, String pet) {
        ServerPlayer player = source.getPlayer();
        TameData data = findOwnedTame(player.getUUID(), pet);
        if (data == null) return error(player, "Pet not found.");
        return groupRemoveBatch(source, group, List.of(data), "'" + data.name + "'");
    }

    private static int groupRemoveSelectionGroup(CommandSourceStack source, String group, String selectedGroup) {
        ServerPlayer player = source.getPlayer();
        return groupRemoveBatch(source, group, ownedGroup(player.getUUID(), selectedGroup), "group '" + selectedGroup + "'");
    }

    private static int groupRemoveSelectionType(CommandSourceStack source, String group, String type) {
        ServerPlayer player = source.getPlayer();
        return groupRemoveBatch(source, group, ownedType(player.getUUID(), type), "type '" + type + "'");
    }

    private static int groupRemoveSelectionState(CommandSourceStack source, String group, MovementOrder order) {
        ServerPlayer player = source.getPlayer();
        return groupRemoveBatch(source, group, ownedState(player.getUUID(), order), movementLabel(order));
    }

    private static int groupRemoveSelectionAll(CommandSourceStack source, String group) {
        ServerPlayer player = source.getPlayer();
        return groupRemoveBatch(source, group, ownedTames(player.getUUID()), "all");
    }

    private static int groupAssignBatch(CommandSourceStack source, String group, List<TameData> requested, String label) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        if (group == null || group.isBlank()) {
            return error(player, "Provide a group name.");
        }
        if (requested == null || requested.isEmpty()) {
            return error(player, "No tames found for " + label + ".");
        }
        int changed = 0;
        List<TameData> changedEntries = new ArrayList<>();
        for (TameData data : requested) {
            if (data == null) {
                continue;
            }
            if (isInGroup(data, group)) {
                continue;
            }
            if (addGroupMembership(data, group)) {
                changed++;
                changedEntries.add(data);
            }
        }
        TameRegistry.rememberGroup(player.getUUID(), group);
        if (changed <= 0) {
            return error(player, "No tames changed for " + label + ".");
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Group '" + group + "' add: " + changed + " tame(s) changed from " + label + ".").withStyle(ChatFormatting.GREEN));
        sendGroupSelectionPreview(player, group, changedEntries, ChatFormatting.GREEN);
        return changed;
    }

    private static int groupRemoveBatch(CommandSourceStack source, String group, List<TameData> requested, String label) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        if (group == null || group.isBlank()) {
            return error(player, "Provide a group name.");
        }
        if (requested == null || requested.isEmpty()) {
            return error(player, "No tames found for " + label + ".");
        }
        int changed = 0;
        List<TameData> changedEntries = new ArrayList<>();
        for (TameData data : requested) {
            if (data == null || !isInGroup(data, group)) {
                continue;
            }
            if (removeGroupMembership(data, group)) {
                changed++;
                changedEntries.add(data);
            }
        }
        if (changed <= 0) {
            return error(player, "No tames from " + label + " were in group '" + group + "'.");
        }
        if (ownedGroup(player.getUUID(), group).isEmpty()) {
            TameRegistry.forgetGroup(player.getUUID(), group);
        } else {
            TameRegistry.markDirty();
        }
        player.sendSystemMessage(Component.literal("Group '" + group + "' remove: " + changed + " tame(s) changed using " + label + ".").withStyle(ChatFormatting.YELLOW));
        sendGroupSelectionPreview(player, group, changedEntries, ChatFormatting.YELLOW);
        return changed;
    }

    private static void sendGroupSelectionPreview(ServerPlayer player, String group, List<TameData> changedEntries, ChatFormatting color) {
        if (player == null || changedEntries == null || changedEntries.isEmpty()) {
            return;
        }
        List<TameData> sorted = new ArrayList<>(changedEntries);
        sorted.sort(Comparator
                .comparingInt((TameData data) -> data.level).reversed()
                .thenComparing(data -> data.name == null ? "" : data.name.toLowerCase(Locale.ROOT)));
        player.sendSystemMessage(Component.literal("---- Group '" + group + "' changed tames (" + sorted.size() + ") ----").withStyle(ChatFormatting.GOLD));
        for (TameData data : sorted) {
            player.sendSystemMessage(Component.literal("- [" + data.level + "] " + tameDisplayName(data)).withStyle(color));
        }
    }

    private static int groupDelete(CommandSourceStack source, String group) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        List<TameData> members = ownedGroup(player.getUUID(), group);
        if (members.isEmpty()) {
            return error(player, "No tames in group '" + group + "'.");
        }
        int removed = 0;
        for (TameData data : members) {
            if (data == null || !isInGroup(data, group)) {
                continue;
            }
            if (removeGroupMembership(data, group)) {
                removed++;
            }
        }
        if (removed <= 0) {
            if (TameRegistry.getOwnerGroups(player.getUUID()).stream().noneMatch(existing -> existing.equalsIgnoreCase(group))) {
                return error(player, "No tames in group '" + group + "'.");
            }
        }
        TameRegistry.forgetGroup(player.getUUID(), group);
        if (removed > 0) {
            TameRegistry.markDirty();
        }
        player.sendSystemMessage(Component.literal("Deleted group '" + group + "' from " + removed + " tame(s).").withStyle(ChatFormatting.YELLOW));
        return Math.max(1, removed);
    }

    public static int drumIssueMovementCommand(ServerPlayer player, ItemStack drum, int command) {
        List<TamableAnimal> selected = drumSelectedLoadedTames(player, drum);
        return drumApplyMovementCommand(player, selected, command);
    }

    public static int drumIssueMovementCommand(ServerPlayer player, String selectorName, int command) {
        List<TamableAnimal> selected = drumSelectedLoadedTames(player, selectorName);
        return drumApplyMovementCommand(player, selected, command);
    }

    private static int drumApplyMovementCommand(ServerPlayer player, List<TamableAnimal> selected, int command) {
        MovementOrder order = switch (command) {
            case 1 -> MovementOrder.SIT;
            case 2 -> MovementOrder.FOLLOW;
            default -> MovementOrder.WANDER;
        };
        int count = 0;
        for (TamableAnimal tame : selected) {
            if (tame == null || !tame.isAlive()) continue;
            applyMovementOverride(tame, order);
            count++;
        }
        return count;
    }

    public static int drumCyclePlacedMode(ServerPlayer player, String selectorName) {
        List<TameData> selected = drumSelectedTameData(player, selectorName);
        if (selected.isEmpty()) {
            return 0;
        }
        int nextModeId = switch (TameMode.byId(selected.get(0).mode)) {
            case DEFAULT -> TameMode.BODYGUARD.id();
            case BODYGUARD -> TameMode.MONSTER_HUNTER.id();
            case MONSTER_HUNTER -> TameMode.ARENA.id();
            case ARENA -> TameMode.AGGRESSIVE.id();
            case AGGRESSIVE -> TameMode.PASSIVE.id();
            default -> TameMode.DEFAULT.id();
        };
        for (TameData data : selected) {
            data.mode = nextModeId;
        }
        TameRegistry.markDirty();
        player.displayClientMessage(Component.literal("Drum: mode -> " + TameMode.byId(nextModeId).key() + " (" + selected.size() + ")").withStyle(ChatFormatting.GREEN), true);
        return selected.size();
    }

    public static int drumCycleHeldMode(ServerPlayer player, ItemStack drum) {
        DrumSelector selector = parseDrumSelector(drum);
        String selectorName = switch (selector.kind) {
            case ALL -> "Drum";
            case SINGLE -> selector.value;
            case GROUP -> "Group " + selector.value;
            case TYPE -> "Type " + selector.value;
            case CLOSE -> "Close";
            case NEARBY -> "Nearby";
            case STATE -> selector.value;
        };
        return drumCyclePlacedMode(player, selectorName);
    }

    public static int drumClearGroupSelector(ServerPlayer player, String selectorName) {
        DrumSelector selector = parseDrumSelector(selectorName);
        if (selector.kind != DrumSelectorKind.GROUP) {
            return 0;
        }
        int count = 0;
        for (TameData data : ownedGroup(player.getUUID(), selector.value)) {
            if (removeGroupMembership(data, selector.value)) {
                count++;
            }
        }
        if (count > 0) {
            TameRegistry.markDirty();
            player.sendSystemMessage(Component.literal("Drum: removed " + count + " tame(s) from group " + selector.value + ".").withStyle(ChatFormatting.RED));
        }
        return count;
    }

    public static void drumReportSelector(ServerPlayer player, String selectorName) {
        List<TameData> selected = drumSelectedTameData(player, selectorName);
        player.sendSystemMessage(Component.literal("---- Drum Report: " + selectorName + " ----").withStyle(ChatFormatting.GOLD));
        if (selected.isEmpty()) {
            player.sendSystemMessage(Component.literal("No matching tames.").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (TameData data : selected) {
            String guard = data.hasHome
                    ? data.homeDimension + " " + data.homeX + " " + data.homeY + " " + data.homeZ
                    : "-";
            player.sendSystemMessage(Component.literal("[" + data.level + "] " + data.name
                    + "  mode:" + TameMode.byId(data.mode).key()
                    + "  group:" + groupLabel(data)
                    + "  guard:" + guard).withStyle(ChatFormatting.AQUA));
        }
    }

    public static int drumClearTargets(ServerPlayer player, ItemStack drum) {
        int count = 0;
        for (TamableAnimal tame : drumSelectedLoadedTames(player, drum)) {
            tame.setTarget(null);
            count++;
        }
        if (count > 0) {
            player.displayClientMessage(Component.literal("Drum: cleared targets for " + count + " tame(s).").withStyle(ChatFormatting.YELLOW), true);
        }
        return count;
    }

    public static int drumSetTargets(ServerPlayer player, ItemStack drum, LivingEntity target) {
        int count = 0;
        for (TamableAnimal tame : drumSelectedLoadedTames(player, drum)) {
            if (tame == target) continue;
            tame.setTarget(target);
            count++;
        }
        if (count > 0) {
            player.displayClientMessage(Component.literal("Drum: targeted " + target.getName().getString() + " with " + count + " tame(s).").withStyle(ChatFormatting.RED), true);
        }
        return count;
    }

    public static boolean drumAddGroupTarget(ServerPlayer player, ItemStack drum, TamableAnimal tame) {
        DrumSelector selector = parseDrumSelector(drum);
        if (selector.kind != DrumSelectorKind.GROUP) return false;
        if (!tame.isTame() || !player.getUUID().equals(tame.getOwnerUUID())) return false;
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) return false;
        if (!addGroupMembership(data, selector.value)) return false;
        TameRegistry.markDirty();
        player.displayClientMessage(Component.literal("Drum: added " + data.name + " to group " + selector.value + ".").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    public static boolean drumRemoveGroupTarget(ServerPlayer player, ItemStack drum, TamableAnimal tame) {
        DrumSelector selector = parseDrumSelector(drum);
        if (selector.kind != DrumSelectorKind.GROUP) return false;
        if (!tame.isTame() || !player.getUUID().equals(tame.getOwnerUUID())) return false;
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null || !removeGroupMembership(data, selector.value)) return false;
        TameRegistry.markDirty();
        player.displayClientMessage(Component.literal("Drum: removed " + data.name + " from group " + selector.value + ".").withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    public static String drumCommandLabel(int command) {
        return switch (command) {
            case 1 -> "Sit";
            case 2 -> "Follow";
            default -> "Wander";
        };
    }

    public static int drumSetGuardianAnchor(ServerPlayer player, ItemStack drum, BlockPos pos) {
        int count = 0;
        String dimensionId = player.serverLevel().dimension().location().toString();
        for (TamableAnimal tame : drumSelectedLoadedTames(player, drum)) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null) continue;
            rememberCurrentGuardianAnchor(data);
            applyGuardianAnchor(dimensionId, pos.getX(), pos.getY(), pos.getZ(), tame, data);
            count++;
        }
        if (count > 0) {
            player.displayClientMessage(Component.literal("Drum: set guard location for " + count + " tame(s).").withStyle(ChatFormatting.DARK_AQUA), true);
        }
        return count;
    }

    public static List<UUID> drumStartMoveAndSitPassive(ServerPlayer player, ItemStack drum, BlockPos pos) {
        List<UUID> moved = new ArrayList<>();
        for (TamableAnimal tame : drumSelectedLoadedTames(player, drum)) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null) continue;
            data.mode = TameMode.PASSIVE.id();
            tame.setOrderedToSit(false);
            tame.setTarget(null);
            tame.getNavigation().stop();
            clearGuardianAnchor(data);
            clearExternalWanderingState(tame, MovementOrder.FOLLOW);
            tame.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 1.15D);
            moved.add(tame.getUUID());
        }
        if (!moved.isEmpty()) {
            TameRegistry.markDirty();
            player.displayClientMessage(Component.literal("Drum: moving " + moved.size() + " tame(s) to sit there passively.").withStyle(ChatFormatting.AQUA), true);
        }
        return moved;
    }

    public static void drumFinalizePassiveSit(TamableAnimal tame) {
        if (tame == null) return;
        TameData data = TameRegistry.get(tame.getUUID());
        if (data != null) {
            data.mode = TameMode.PASSIVE.id();
        }
        applyMovementOverride(tame, MovementOrder.SIT);
        if (data != null) {
            TameRegistry.markDirty();
        }
    }

    public static int drumTeleportToBlock(ServerPlayer player, ItemStack drum, BlockPos pos) {
        SpawnTarget target = new SpawnTarget(player.serverLevel(), new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D), player.getYRot(), player.getXRot());
        return drumTeleportBatchToTarget(player, drumSelectedTameData(player, drum), target, "Drum TP");
    }

    public static int drumTeleportHome(ServerPlayer player, ItemStack drum) {
        return teleportHomeBatch(player.createCommandSourceStack(), player, drumSelectedTameData(player, drum), "Drum TPHome", 0);
    }

    private static int drumTeleportBatchToTarget(ServerPlayer player, List<TameData> requested, SpawnTarget target, String label) {
        CommandSourceStack source = player.createCommandSourceStack();
        List<TamableAnimal> loadedTargets = new ArrayList<>();
        List<SpawnTarget> loadedDestinations = new ArrayList<>();
        List<TameData> unloadedTargets = new ArrayList<>();
        List<SpawnTarget> unloadedDestinations = new ArrayList<>();
        int queued = 0;
        int queueFailed = 0;
        int cost = 0;
        int crossDimension = 0;
        List<String> failedNames = new ArrayList<>();

        for (TameData data : requested) {
            if (data == null || data.uuid == null || data.dead) continue;
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, player.getUUID(), data.uuid);
            if (tame == null) {
                String queueError = validateUnloadedHomeTeleport(source, player, data, target);
                if (queueError != null) {
                    queueFailed++;
                    failedNames.add((data.name == null ? "unknown" : data.name) + " (" + queueError + ")");
                    continue;
                }
                unloadedTargets.add(data);
                unloadedDestinations.add(target);
                boolean cross = data.lastKnownDimension != null
                        && !data.lastKnownDimension.isBlank()
                        && !target.level.dimension().location().toString().equals(data.lastKnownDimension);
                cost += teleportCostFor(data, cross);
                if (cross) crossDimension++;
                continue;
            }
            boolean cross = !tame.level().dimension().equals(target.level.dimension());
            loadedTargets.add(tame);
            loadedDestinations.add(target);
            cost += teleportCostFor(data, cross);
            if (cross) crossDimension++;
        }

        if (!payTeleportXp(player, cost)) return 0;
        for (int i = 0; i < loadedTargets.size(); i++) {
            teleportTameToLocation(loadedTargets.get(i), loadedDestinations.get(i));
        }
        for (int i = 0; i < unloadedTargets.size(); i++) {
            UnloadedTpResult unloaded = tpUnloadedHomeViaLanternOrRecover(source, player, unloadedTargets.get(i), unloadedDestinations.get(i));
            if (unloaded.success) {
                queued++;
            } else {
                queueFailed++;
                failedNames.add((unloadedTargets.get(i).name == null ? "unknown" : unloadedTargets.get(i).name) + " (" + unloaded.error + ")");
            }
        }
        sendTeleportSummary(player, label, loadedTargets.size(), queued, 0, queueFailed, cost, crossDimension);
        if (!failedNames.isEmpty()) {
            player.sendSystemMessage(Component.literal("Drum tp failures: " + String.join("; ", failedNames)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private enum DrumSelectorKind {
        ALL,
        SINGLE,
        GROUP,
        TYPE,
        CLOSE,
        NEARBY,
        STATE
    }

    private record DrumSelector(DrumSelectorKind kind, String value) {
    }

    private static DrumSelector parseDrumSelector(ItemStack drum) {
        if (drum == null || drum.isEmpty() || !drum.hasCustomHoverName()) {
            return new DrumSelector(DrumSelectorKind.ALL, "");
        }
        return parseDrumSelector(drum.getHoverName().getString());
    }

    private static DrumSelector parseDrumSelector(String rawText) {
        String raw = rawText == null ? "" : rawText.trim();
        if (raw.isBlank() || raw.equalsIgnoreCase("Drum") || raw.equalsIgnoreCase("all")) {
            return new DrumSelector(DrumSelectorKind.ALL, "");
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.equals("close")) {
            return new DrumSelector(DrumSelectorKind.CLOSE, "");
        }
        if (lower.equals("nearby")) {
            return new DrumSelector(DrumSelectorKind.NEARBY, "");
        }
        if (lower.equals("follow") || lower.equals("sit") || lower.equals("wander")) {
            return new DrumSelector(DrumSelectorKind.STATE, lower);
        }
        if (lower.startsWith("group ")) {
            String value = raw.substring("group ".length()).trim();
            return value.isBlank() ? new DrumSelector(DrumSelectorKind.ALL, "") : new DrumSelector(DrumSelectorKind.GROUP, value);
        }
        if (lower.startsWith("type ")) {
            String value = raw.substring("type ".length()).trim();
            return value.isBlank() ? new DrumSelector(DrumSelectorKind.ALL, "") : new DrumSelector(DrumSelectorKind.TYPE, value);
        }
        return new DrumSelector(DrumSelectorKind.SINGLE, raw);
    }

    private static List<TamableAnimal> drumSelectedLoadedTames(ServerPlayer player, ItemStack drum) {
        CommandSourceStack source = player.createCommandSourceStack();
        DrumSelector selector = parseDrumSelector(drum);
        return drumSelectedLoadedTames(player, source, selector);
    }

    private static List<TamableAnimal> drumSelectedLoadedTames(ServerPlayer player, String selectorName) {
        CommandSourceStack source = player.createCommandSourceStack();
        DrumSelector selector = parseDrumSelector(selectorName);
        return drumSelectedLoadedTames(player, source, selector);
    }

    private static List<TamableAnimal> drumSelectedLoadedTames(ServerPlayer player, CommandSourceStack source, DrumSelector selector) {
        return switch (selector.kind) {
            case ALL -> loadedOwnedAllTames(source, player.getUUID());
            case GROUP -> loadedOwnedGroupTames(source, player.getUUID(), selector.value);
            case CLOSE -> loadedOwnedCloseTames(source, player.getUUID(), player);
            case NEARBY -> loadedOwnedNearbyTames(source, player.getUUID(), player);
            case STATE -> {
                MovementOrder order = parseMovementOrder(selector.value);
                yield order == null ? List.of() : loadedOwnedStateTames(source, player.getUUID(), order);
            }
            case TYPE -> {
                List<TamableAnimal> list = new ArrayList<>();
                for (TameData data : ownedType(player.getUUID(), selector.value)) {
                    TamableAnimal tame = findLoadedOwnedTameByUuid(source, player.getUUID(), data.uuid);
                    if (tame != null && tame.isAlive()) {
                        list.add(tame);
                    }
                }
                yield list;
            }
            case SINGLE -> {
                TameData data = findOwnedTame(player.getUUID(), selector.value);
                if (data == null) {
                    yield List.of();
                }
                TamableAnimal tame = findLoadedOwnedTameByUuid(source, player.getUUID(), data.uuid);
                yield tame == null || !tame.isAlive() ? List.of() : List.of(tame);
            }
        };
    }

    private static List<TameData> drumSelectedTameData(ServerPlayer player, ItemStack drum) {
        DrumSelector selector = parseDrumSelector(drum);
        return drumSelectedTameData(player, selector);
    }

    private static List<TameData> drumSelectedTameData(ServerPlayer player, String selectorName) {
        DrumSelector selector = parseDrumSelector(selectorName);
        return drumSelectedTameData(player, selector);
    }

    private static List<TameData> drumSelectedTameData(ServerPlayer player, DrumSelector selector) {
        return switch (selector.kind) {
            case ALL -> ownedTames(player.getUUID());
            case GROUP -> ownedGroup(player.getUUID(), selector.value);
            case TYPE -> ownedType(player.getUUID(), selector.value);
            case CLOSE -> loadedSelectionData(player, loadedOwnedCloseTames(player.createCommandSourceStack(), player.getUUID(), player));
            case NEARBY -> loadedSelectionData(player, loadedOwnedNearbyTames(player.createCommandSourceStack(), player.getUUID(), player));
            case STATE -> {
                MovementOrder order = parseMovementOrder(selector.value);
                yield order == null ? List.of() : ownedState(player.getUUID(), order);
            }
            case SINGLE -> {
                TameData data = findOwnedTame(player.getUUID(), selector.value);
                yield data == null ? List.of() : List.of(data);
            }
        };
    }

    private static List<TameData> loadedSelectionData(ServerPlayer player, List<TamableAnimal> loaded) {
        List<TameData> out = new ArrayList<>();
        for (TamableAnimal tame : loaded) {
            if (tame == null || !tame.isTame() || !player.getUUID().equals(tame.getOwnerUUID())) continue;
            TameData data = TameRegistry.get(tame.getUUID());
            if (data != null) {
                out.add(data);
            }
        }
        return out;
    }

    private static List<TamableAnimal> loadedOwnedCloseTames(CommandSourceStack source, UUID owner, ServerPlayer player) {
        List<TamableAnimal> list = new ArrayList<>();
        for (TamableAnimal tame : loadedOwnedAllTames(source, owner)) {
            if (tame.distanceToSqr(player) <= 2.25D) {
                list.add(tame);
            }
        }
        return list;
    }

    private static List<TamableAnimal> loadedOwnedNearbyTames(CommandSourceStack source, UUID owner, ServerPlayer player) {
        List<TamableAnimal> list = new ArrayList<>();
        for (TamableAnimal tame : player.level().getEntitiesOfClass(TamableAnimal.class, player.getBoundingBox().inflate(32))) {
            if (!tame.isTame() || !owner.equals(tame.getOwnerUUID())) continue;
            list.add(tame);
        }
        return list;
    }

    private static List<TameData> ownedState(UUID owner, MovementOrder order) {
        List<TameData> list = new ArrayList<>();
        for (TameData data : ownedTames(owner)) {
            if (matchesMovementOrderSnapshot(data, order)) {
                list.add(data);
            }
        }
        return list;
    }

    private record GuardianToolTarget(DrumSelector selector, String selectorRaw, String setName) {
    }

    private static GuardianToolTarget parseGuardianToolTarget(ItemStack arrow) {
        if (arrow == null || arrow.isEmpty() || !arrow.hasCustomHoverName()) {
            return null;
        }
        String raw = arrow.getHoverName().getString().trim();
        if (raw.isBlank()) {
            return null;
        }
        int colon = raw.indexOf(':');
        String selectorRaw = colon >= 0 ? raw.substring(0, colon).trim() : raw;
        String setRaw = colon >= 0 ? raw.substring(colon + 1).trim() : "";
        if (selectorRaw.isBlank()) {
            selectorRaw = "all";
        }
        String lowerSelector = selectorRaw.toLowerCase(Locale.ROOT);
        DrumSelector selector = lowerSelector.equals("all") || lowerSelector.startsWith("all ")
                ? new DrumSelector(DrumSelectorKind.ALL, "")
                : parseDrumSelector(selectorRaw);
        String normalizedSet = normalizeGuardianSetName(setRaw);
        if (normalizedSet.isBlank()) {
            return null;
        }
        return new GuardianToolTarget(selector, selectorRaw, normalizedSet);
    }

    private static GuardianToolOrder currentGuardianToolOrder(ServerPlayer player) {
        return GuardianToolOrder.parse(player.getPersistentData().getString(TAG_GUARDIAN_TOOL_ORDER));
    }

    public static int guardianToolChangeOrder(CommandSourceStack source, String rawOrder) {
        ServerPlayer player = source.getPlayer();
        GuardianToolOrder order = GuardianToolOrder.parse(rawOrder);
        player.getPersistentData().putString(TAG_GUARDIAN_TOOL_ORDER, order.id);
        player.sendSystemMessage(Component.literal("Guardian tool order -> " + order.id + ".").withStyle(ChatFormatting.AQUA));
        return 1;
    }

    public static int guardianToolConfirm(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        PendingGuardianToolConfirm pending = PENDING_GUARDIAN_TOOL_CONFIRMS.remove(player.getUUID());
        if (pending == null) {
            return error(player, "No guardian tool confirmation is pending.");
        }
        if (player.serverLevel().getGameTime() > pending.expiresAtTick()) {
            return error(player, "Guardian tool confirmation expired.");
        }
        GuardianToolTarget target = new GuardianToolTarget(parseDrumSelector(pending.selectorRaw()), pending.selectorRaw(), pending.setName());
        return switch (pending.action()) {
            case CLEAR_ALL -> guardianToolClearAllAnchors(player, target, true);
        };
    }

    public static int guardianToolQueueClearConfirm(ServerPlayer player, ItemStack arrow) {
        GuardianToolTarget target = parseGuardianToolTarget(arrow);
        if (target == null) {
            return error(player, guardianToolRenameHelp());
        }
        PENDING_GUARDIAN_TOOL_CONFIRMS.put(player.getUUID(), new PendingGuardianToolConfirm(
                GuardianToolConfirmAction.CLEAR_ALL,
                target.selectorRaw(),
                target.setName(),
                player.serverLevel().dimension().location().toString(),
                0,
                0,
                0,
                player.serverLevel().getGameTime() + GUARDIAN_TOOL_CONFIRM_TICKS
        ));
        player.sendSystemMessage(Component.literal("Confirm clearing deployment group '" + target.setName() + "' with /tames guardian tool confirm within 60 seconds.")
                .withStyle(ChatFormatting.GOLD));
        return 1;
    }

    public static int guardianToolSetNextAnchor(ServerPlayer player, ItemStack arrow, BlockPos pos) {
        GuardianToolTarget target = parseGuardianToolTarget(arrow);
        if (target == null) {
            return error(player, guardianToolRenameHelp());
        }
        List<TameData> selected = guardianToolOrderedSelection(player, target.selector());
        if (selected.isEmpty()) {
            return error(player, "No matching tames for that guardian tool.");
        }
        TameData next = null;
        for (TameData data : selected) {
            if (!data.guardianSetAnchors.containsKey(target.setName())) {
                next = data;
                break;
            }
        }
        if (next == null) {
            return error(player, "All selected tames already have a guardian location in '" + target.setName() + "'.");
        }
        putGuardianSetAnchor(next, target.setName(), player.serverLevel().dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
        TameRegistry.markDirty();
        TameData upcoming = null;
        boolean seen = false;
        for (TameData data : selected) {
            if (!seen) {
                if (data.uuid != null && data.uuid.equals(next.uuid)) {
                    seen = true;
                }
                continue;
            }
            if (!data.guardianSetAnchors.containsKey(target.setName())) {
                upcoming = data;
                break;
            }
        }
        net.minecraft.network.chat.MutableComponent line = Component.literal("Set guardian '")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(target.setName()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("' for ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(tameDisplayName(next)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(". ").withStyle(ChatFormatting.WHITE));
        if (upcoming != null) {
            line = line.append(Component.literal("Next: ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(tameDisplayName(upcoming)).withStyle(ChatFormatting.BLUE))
                    .append(Component.literal(".").withStyle(ChatFormatting.WHITE));
        } else {
            line = line.append(Component.literal("No next tame pending.").withStyle(ChatFormatting.WHITE));
        }
        player.sendSystemMessage(line);
        return 1;
    }

    public static int guardianToolDeploySet(ServerPlayer player, ItemStack arrow) {
        GuardianToolTarget target = parseGuardianToolTarget(arrow);
        if (target == null) {
            return error(player, guardianToolRenameHelp());
        }
        return guardianDeploySet(player.createCommandSourceStack(), target.setName());
    }

    public static int guardianToolClearAnchorsAtBlock(ServerPlayer player, ItemStack arrow, BlockPos pos) {
        GuardianToolTarget target = parseGuardianToolTarget(arrow);
        if (target == null) {
            return error(player, guardianToolRenameHelp());
        }
        List<TameData> selected = guardianToolOrderedSelection(player, target.selector());
        String dimensionId = player.serverLevel().dimension().location().toString();
        int removed = 0;
        for (TameData data : selected) {
            CompoundTag anchor = getGuardianSetAnchor(data, target.setName());
            if (guardianToolMatchesAnchor(anchor, dimensionId, pos)) {
                data.guardianSetAnchors.remove(target.setName());
                removed++;
            }
        }
        if (removed <= 0) {
            return error(player, "No guardian locations removed at that block.");
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Removed guardian '" + target.setName() + "' from " + removed + " tame(s).")
                .withStyle(ChatFormatting.YELLOW));
        return removed;
    }

    public static boolean guardianToolAddGroupTarget(ServerPlayer player, ItemStack arrow, TamableAnimal tame) {
        GuardianToolTarget target = parseGuardianToolTarget(arrow);
        if (target == null || target.selector().kind != DrumSelectorKind.GROUP) return false;
        if (!tame.isTame() || !player.getUUID().equals(tame.getOwnerUUID())) return false;
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) return false;
        if (!addGroupMembership(data, target.selector().value)) return false;
        TameRegistry.markDirty();
        player.displayClientMessage(Component.literal("Guardian tool: added " + tameDisplayName(data) + " to group " + target.selector().value + ".")
                .withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    public static boolean guardianToolRemoveGroupTarget(ServerPlayer player, ItemStack arrow, TamableAnimal tame) {
        GuardianToolTarget target = parseGuardianToolTarget(arrow);
        if (target == null || target.selector().kind != DrumSelectorKind.GROUP) return false;
        if (!tame.isTame() || !player.getUUID().equals(tame.getOwnerUUID())) return false;
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null || !removeGroupMembership(data, target.selector().value)) return false;
        TameRegistry.markDirty();
        player.displayClientMessage(Component.literal("Guardian tool: removed " + tameDisplayName(data) + " from group " + target.selector().value + ".")
                .withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    public static boolean guardianToolAddCurrentAnchorToSet(ServerPlayer player, ItemStack arrow, TamableAnimal tame) {
        GuardianToolTarget target = parseGuardianToolTarget(arrow);
        if (target == null) return false;
        if (!tame.isTame() || !player.getUUID().equals(tame.getOwnerUUID())) return false;
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null || !data.hasHome || data.homeDimension == null || data.homeDimension.isBlank()) {
            error(player, "That tame has no current guardian location to store.");
            return false;
        }
        putGuardianSetAnchor(data, target.setName(), data.homeDimension, data.homeX, data.homeY, data.homeZ);
        TameRegistry.markDirty();
        player.displayClientMessage(Component.literal("Guardian tool: stored current guardian of " + tameDisplayName(data) + " in '" + target.setName() + "'.")
                .withStyle(ChatFormatting.DARK_AQUA), true);
        return true;
    }

    public static boolean guardianToolRemoveCurrentAnchorFromSet(ServerPlayer player, ItemStack arrow, TamableAnimal tame) {
        GuardianToolTarget target = parseGuardianToolTarget(arrow);
        if (target == null) return false;
        if (!tame.isTame() || !player.getUUID().equals(tame.getOwnerUUID())) return false;
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) return false;
        CompoundTag anchor = getGuardianSetAnchor(data, target.setName());
        if (anchor == null || anchor.isEmpty()) {
            return false;
        }
        if (data.hasHome && guardianToolMatchesAnchor(anchor, data.homeDimension, new BlockPos(data.homeX, data.homeY, data.homeZ))) {
            data.guardianSetAnchors.remove(target.setName());
        } else {
            data.guardianSetAnchors.remove(target.setName());
        }
        TameRegistry.markDirty();
        player.displayClientMessage(Component.literal("Guardian tool: removed guardian '" + target.setName() + "' from " + tameDisplayName(data) + ".")
                .withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    private static int guardianToolSetAllAnchors(ServerPlayer player, GuardianToolTarget target, String dimensionId, BlockPos pos, boolean fromConfirm) {
        List<TameData> selected = guardianToolOrderedSelection(player, target.selector());
        if (selected.isEmpty()) {
            return error(player, "No matching tames for that guardian tool.");
        }
        int count = 0;
        for (TameData data : selected) {
            putGuardianSetAnchor(data, target.setName(), dimensionId, pos.getX(), pos.getY(), pos.getZ());
            count++;
        }
        if (count <= 0) {
            return 0;
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal((fromConfirm ? "Confirmed" : "Set") + " guardian '" + target.setName() + "' for " + count + " tame(s).")
                .withStyle(ChatFormatting.GREEN));
        return count;
    }

    private static int guardianToolClearAllAnchors(ServerPlayer player, GuardianToolTarget target, boolean fromConfirm) {
        List<TameData> selected = guardianToolOrderedSelection(player, target.selector());
        if (selected.isEmpty()) {
            return error(player, "No matching tames for that guardian tool.");
        }
        int removed = 0;
        for (TameData data : selected) {
            if (data == null) {
                continue;
            }
            if (data.guardianSetAnchors.remove(target.setName()) != null) {
                removed++;
            }
        }
        if (removed <= 0) {
            return error(player, "No guardian locations were stored in '" + target.setName() + "' for that selection.");
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal((fromConfirm ? "Confirmed" : "Cleared") + " guardian group '" + target.setName() + "' for " + removed + " tame(s).")
                .withStyle(ChatFormatting.YELLOW));
        return removed;
    }

    private static List<TameData> guardianToolOrderedSelection(ServerPlayer player, DrumSelector selector) {
        List<TameData> selected = new ArrayList<>(drumSelectedTameData(player, selector));
        selected.removeIf(data -> data == null || data.dead || data.ownerUUID == null || !player.getUUID().equals(data.ownerUUID));
        Comparator<TameData> comparator = switch (currentGuardianToolOrder(player)) {
            case NAME -> Comparator
                    .comparing((TameData data) -> safeName(data).toLowerCase(Locale.ROOT))
                    .thenComparing(data -> data.level, Comparator.reverseOrder())
                    .thenComparing(data -> data.uuid == null ? "" : data.uuid.toString());
            case POINTS -> Comparator
                    .comparingInt(TameCommands::weightedCombatScore).reversed()
                    .thenComparing((TameData data) -> data.level, Comparator.reverseOrder())
                    .thenComparing(data -> safeName(data).toLowerCase(Locale.ROOT));
            case TYPE -> Comparator
                    .comparing((TameData data) -> data.type == null ? "" : data.type.toLowerCase(Locale.ROOT))
                    .thenComparing(data -> safeName(data).toLowerCase(Locale.ROOT))
                    .thenComparing(data -> data.level, Comparator.reverseOrder());
            case LEVEL -> Comparator
                    .comparingInt((TameData data) -> data.level).reversed()
                    .thenComparing(data -> safeName(data).toLowerCase(Locale.ROOT))
                    .thenComparing(data -> weightedCombatScore(data), Comparator.reverseOrder());
        };
        selected.sort(comparator);
        return selected;
    }

    private static String guardianToolRenameHelp() {
        return "Rename the arrow to '<target>: <guardianGroup>'. Examples: 'all: base', 'group wolves: north', 'type minecraft:wolf: west', 'Fluffy: tower'.";
    }

    private static boolean guardianToolMatchesAnchor(CompoundTag anchor, String dimensionId, BlockPos pos) {
        if (anchor == null || anchor.isEmpty() || dimensionId == null || pos == null) {
            return false;
        }
        return dimensionId.equals(anchor.getString("dimension"))
                && pos.getX() == anchor.getInt("x")
                && pos.getY() == anchor.getInt("y")
                && pos.getZ() == anchor.getInt("z");
    }

    private static String safeName(TameData data) {
        return data == null || data.name == null ? "" : data.name;
    }

    private static int groupTames(CommandSourceStack source, String group) {
        ServerPlayer p = source.getPlayer();
        List<TameData> t = ownedGroup(p.getUUID(), group);
        if (t.isEmpty()) {
            if (TameRegistry.getOwnerGroups(p.getUUID()).stream().noneMatch(existing -> existing.equalsIgnoreCase(group))) {
                return error(p, "No tames in group.");
            }
            p.sendSystemMessage(Component.literal(group + ": ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal("(empty)").withStyle(ChatFormatting.GRAY)));
            return 1;
        }
        for (TameData d : t) p.sendSystemMessage(Component.literal("- " + d.name + " [Lvl " + d.level + "]"));
        return 1;
    }

    private static int groupOverview(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        Map<String, List<TameData>> groups = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String group : TameRegistry.getOwnerGroups(p.getUUID())) {
            if (group == null || group.isBlank()) continue;
            groups.computeIfAbsent(group, ignored -> new ArrayList<>());
        }
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            if (isDeadEntry(d.uuid)) continue;
            for (String group : groupMemberships(d)) {
                groups.computeIfAbsent(group, ignored -> new ArrayList<>()).add(d);
            }
        }
        if (groups.isEmpty()) return error(p, "No groups found.");

        int index = 1;
        for (Map.Entry<String, List<TameData>> entry : groups.entrySet()) {
            List<String> names = new ArrayList<>();
            for (TameData d : entry.getValue()) {
                names.add(d.name);
            }
            names.sort(String::compareToIgnoreCase);
            String joined = names.isEmpty() ? "(empty)" : String.join(", ", names);
            p.sendSystemMessage(
                    Component.literal(index + ". ").withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(entry.getKey() + ": ").withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(joined).withStyle(names.isEmpty() ? ChatFormatting.GRAY : ChatFormatting.YELLOW))
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
        List<TameData> queuedTargets = new ArrayList<>();
        List<String> unaffordable = new ArrayList<>();
        int cost = 0;
        int crossDimension = 0;
        int xpBudget = currentXpPoints(p);
        for (TameData d : requested) {
            if (d == null || d.dead) {
                continue;
            }
            TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (ta == null) {
                queuedTargets.add(d);
                continue;
            }
            boolean cross = isCrossDimension(ta, p);
            int tameCost = teleportCostFor(d, cross);
            if (tameCost > xpBudget) {
                unaffordable.add(pricedTameLabel(d, teleportPriceLabel(tameCost)));
                continue;
            }
            targets.add(ta);
            xpBudget -= tameCost;
            cost += tameCost;
            if (cross) crossDimension++;
        }
        if (!payTeleportXp(p, cost)) return 0;
        for (TamableAnimal ta : targets) teleportTameToPlayer(ta, p);
        for (TameData d : queuedTargets) {
            UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, p, d);
            if (unloaded.success) {
                queued++;
            } else {
                queueFailed++;
                failedQueueNames.add(tameDisplayName(d) + " (" + unloaded.error + ")");
            }
        }
        sendTeleportSummary(p, "TP group " + group, targets.size(), queued, deadSkipped, queueFailed, cost, crossDimension);
        sendAffordabilityFailures(p, "Could not afford tp for", unaffordable);
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
        List<TameData> queuedTargets = new ArrayList<>();
        List<String> unaffordable = new ArrayList<>();
        int cost = 0;
        int crossDimension = 0;
        int xpBudget = currentXpPoints(p);
        for (TameData d : requested) {
            if (d == null || d.dead) {
                continue;
            }
            TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (ta == null) {
                queuedTargets.add(d);
                continue;
            }
            boolean cross = isCrossDimension(ta, p);
            int tameCost = teleportCostFor(d, cross);
            if (tameCost > xpBudget) {
                unaffordable.add(pricedTameLabel(d, teleportPriceLabel(tameCost)));
                continue;
            }
            targets.add(ta);
            xpBudget -= tameCost;
            cost += tameCost;
            if (cross) crossDimension++;
        }
        if (!payTeleportXp(p, cost)) return 0;
        for (TamableAnimal ta : targets) teleportTameToPlayer(ta, p);
        for (TameData d : queuedTargets) {
            UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, p, d);
            if (unloaded.success) {
                queued++;
            } else {
                queueFailed++;
                failedQueueNames.add(tameDisplayName(d) + " (" + unloaded.error + ")");
            }
        }
        sendTeleportSummary(p, "TP type " + typeFilter, targets.size(), queued, deadSkipped, queueFailed, cost, crossDimension);
        sendAffordabilityFailures(p, "Could not afford tp for", unaffordable);
        if (!failedQueueNames.isEmpty()) {
            p.sendSystemMessage(Component.literal("Unloaded tp failures: " + String.join("; ", failedQueueNames)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int groupTpHome(CommandSourceStack source, String group) {
        ServerPlayer player = source.getPlayer();
        List<TameData> requested = ownedGroup(player.getUUID(), group);
        if (requested.isEmpty()) return error(player, "No tames found in group '" + group + "'.");
        return teleportHomeBatch(source, player, requested, "TPHome group " + group, ownedDeadGroup(player.getUUID(), group).size());
    }

    private static int typeTpHome(CommandSourceStack source, String typeFilter) {
        ServerPlayer player = source.getPlayer();
        List<TameData> requested = ownedType(player.getUUID(), typeFilter);
        if (requested.isEmpty()) return error(player, "No tames found for type '" + typeFilter + "'.");
        return teleportHomeBatch(source, player, requested, "TPHome type " + typeFilter, ownedDeadType(player.getUUID(), typeFilter).size());
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

    private static int setPetWanderLock(CommandSourceStack source, String pet, boolean enabled) {
        ServerPlayer player = source.getPlayer();
        TameData data = findOwnedTame(player.getUUID(), pet);
        if (data == null) return error(player, "Pet not found.");
        data.wanderLock = enabled;
        TameRegistry.markDirty();
        String state = enabled ? "enabled" : "disabled";
        player.sendSystemMessage(Component.literal("Wander lock " + state + " for " + data.name + "."));
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

    private static int teleportPet(CommandSourceStack source, String pet, MovementOrder orderOverride) {
        ServerPlayer p = source.getPlayer();
        TameData d = findOwnedTameAny(p.getUUID(), pet);
        if (d == null) return error(p, "Pet not found.");
        if (d.stored) return error(p, tameDisplayName(d) + " is stored and can only be recovered with admin respawn.");
        if (d.dead || isDeadEntry(d.uuid)) return error(p, tameDisplayName(d) + " is dead and cannot be teleported.");
        TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
        if (ta == null) {
            UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, p, d);
            if (!unloaded.success) return error(p, "Failed to tp unloaded tame: " + unloaded.error);
            TamableAnimal loadedAfterTeleport = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (orderOverride != null && loadedAfterTeleport != null) {
                applyMovementOverride(loadedAfterTeleport, orderOverride);
            }
            p.sendSystemMessage(Component.literal("Teleported unloaded " + d.name + " to your position.").withStyle(ChatFormatting.GREEN));
            return 1;
        }
        boolean crossDimension = isCrossDimension(ta, p);
        int cost = teleportCostFor(d, crossDimension);
        if (!payTeleportXp(p, cost)) return 0;
        if (crossDimension) {
            if (orderOverride != null) {
                applyMovementOverride(ta, orderOverride);
            }
            UnloadedTpResult queued = queueImmediateChunkTeleport((ServerLevel) ta.level(), ta.blockPosition(), d, new SpawnTarget(p.serverLevel(), p.position(), p.getYRot(), p.getXRot()), true, false);
            if (!queued.success) return error(p, "Failed to queue cross-dimension tame teleport: " + queued.error);
            return 1;
        }
        teleportTameToPlayer(ta, p);
        TamableAnimal loadedAfterTeleport = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
        if (orderOverride != null && loadedAfterTeleport != null) {
            applyMovementOverride(loadedAfterTeleport, orderOverride);
        }
        p.sendSystemMessage(Component.literal("Teleported " + d.name + " (-" + cost + " XP points" + (crossDimension ? ", cross-dimension" : "") + ")."));
        return 1;
    }

    private static int teleportGuardianPet(CommandSourceStack source, String pet) {
        ServerPlayer player = source.getPlayer();
        TameData data = findOwnedTameAny(player.getUUID(), pet);
        if (data == null) return error(player, "Pet not found.");
        if (data.stored) return error(player, tameDisplayName(data) + " is stored and can only be recovered with admin respawn.");
        if (data.dead || isDeadEntry(data.uuid)) return error(player, tameDisplayName(data) + " is dead and cannot be teleported.");
        if (!data.hasHome) return error(player, data.name + " does not currently have a guardian location.");
        return teleportGuardianBatch(source, player, List.of(data), "TPGuardian " + data.name, 0);
    }

    private static int teleportPetHome(CommandSourceStack source, String pet) {
        ServerPlayer player = source.getPlayer();
        TameData data = findOwnedTameAny(player.getUUID(), pet);
        if (data == null) return error(player, "Pet not found.");
        if (data.stored) return error(player, tameDisplayName(data) + " is stored and can only be recovered with admin respawn.");
        if (data.dead || isDeadEntry(data.uuid)) return error(player, tameDisplayName(data) + " is dead and cannot be teleported.");
        return teleportHomeBatch(source, player, List.of(data), "TPHome " + data.name, ownedDeadTames(player.getUUID()).stream().anyMatch(d -> Objects.equals(d.uuid, data.uuid)) ? 1 : 0);
    }

    private static int recoverPet(CommandSourceStack source, String pet) {
        ServerPlayer p = source.getPlayer();
        TameData data = findOwnedTame(p.getUUID(), pet);
        if (data == null) return error(p, "Pet not found.");
        if (data.stored) return error(p, tameDisplayName(data) + " is stored and can only be recovered with admin respawn.");
        if (isEffectivelyLoaded(source, p, data)) {
            return error(p, "Pet is already loaded/carried. Recover is only for lost/unloaded tames.");
        }
        RecoverResult recovered = recoverPetEntity(source, p, data);
        if (recovered.entity == null) return error(p, "Failed to recover tame: " + recovered.error);
        p.sendSystemMessage(Component.literal("Recovered " + data.name + " (no XP cost).").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int respawnPet(CommandSourceStack source, String pet, ReviveMode mode) {
        ServerPlayer p = source.getPlayer();
        TameData data = findOwnedDeadTame(p.getUUID(), pet);
        if (data == null) return error(p, "No dead tame found with that name.");
        return respawnDeadBatch(source, p, List.of(data), mode, mode.label);
    }

    private static int respawnGroup(CommandSourceStack source, String group, ReviveMode mode) {
        ServerPlayer p = source.getPlayer();
        List<TameData> dead = ownedDeadGroup(p.getUUID(), group);
        if (dead.isEmpty()) return error(p, "No dead tames in group '" + group + "'.");
        return respawnDeadBatch(source, p, dead, mode, mode.label + " group '" + group + "'");
    }

    private static int respawnType(CommandSourceStack source, String typeFilter, ReviveMode mode) {
        ServerPlayer p = source.getPlayer();
        List<TameData> dead = ownedDeadType(p.getUUID(), typeFilter);
        if (dead.isEmpty()) return error(p, "No dead tames of type '" + typeFilter + "'.");
        return respawnDeadBatch(source, p, dead, mode, mode.label + " type '" + typeFilter + "'");
    }

    private static int respawnAll(CommandSourceStack source, ReviveMode mode) {
        ServerPlayer p = source.getPlayer();
        List<TameData> dead = ownedDeadTames(p.getUUID());
        if (dead.isEmpty()) return error(p, "You have no dead tames to respawn.");
        return respawnDeadBatch(source, p, dead, mode, mode.label + " all dead tames");
    }

    private static int adminPlayerTeleportPet(CommandSourceStack source, String playerName, String pet, MovementOrder orderOverride) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        String ownerName = resolveKnownOwnerName(source.getServer(), ownerId, playerName);
        ServerPlayer owner = source.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return adminError(source, ownerName + " must be online for admin tp.");
        }
        TameData data = findOwnedTameAny(ownerId, pet);
        if (data == null) {
            return adminError(source, "Pet not found for " + ownerName + ".");
        }
        if (data.stored) {
            return adminError(source, tameDisplayName(data) + " is stored and can only be recovered with admin respawn.");
        }
        if (data.dead || isDeadEntry(data.uuid)) {
            return adminError(source, tameDisplayName(data) + " is dead and cannot be teleported.");
        }
        TamableAnimal tame = findLoadedOwnedTameByUuid(source, ownerId, data.uuid);
        if (tame == null) {
            UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, owner, data);
            if (!unloaded.success) {
                return adminError(source, "Failed to tp unloaded tame for " + ownerName + ": " + unloaded.error);
            }
            TamableAnimal loadedAfterTeleport = findLoadedOwnedTameByUuid(source, ownerId, data.uuid);
            if (orderOverride != null && loadedAfterTeleport != null) {
                applyMovementOverride(loadedAfterTeleport, orderOverride);
            }
            source.sendSuccess(() -> Component.literal("Admin teleported unloaded " + tameDisplayName(data) + " to " + ownerName + "."), false);
            return 1;
        }
        boolean crossDimension = isCrossDimension(tame, owner);
        if (crossDimension) {
            if (orderOverride != null) {
                applyMovementOverride(tame, orderOverride);
            }
            UnloadedTpResult queued = queueImmediateChunkTeleport(
                    (ServerLevel) tame.level(),
                    tame.blockPosition(),
                    data,
                    new SpawnTarget(owner.serverLevel(), owner.position(), owner.getYRot(), owner.getXRot()),
                    true,
                    false
            );
            if (!queued.success) {
                return adminError(source, "Failed to queue cross-dimension tame teleport for " + ownerName + ": " + queued.error);
            }
            source.sendSuccess(() -> Component.literal("Admin queued cross-dimension teleport for " + tameDisplayName(data) + " to " + ownerName + "."), false);
            return 1;
        }
        teleportTameToPlayer(tame, owner);
        TamableAnimal loadedAfterTeleport = findLoadedOwnedTameByUuid(source, ownerId, data.uuid);
        if (orderOverride != null && loadedAfterTeleport != null) {
            applyMovementOverride(loadedAfterTeleport, orderOverride);
        }
        source.sendSuccess(() -> Component.literal("Admin teleported " + tameDisplayName(data) + " to " + ownerName + "."), false);
        return 1;
    }

    private static int adminPlayerTeleportAll(CommandSourceStack source, String playerName) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        return adminPlayerTeleportBatch(source, playerName, ownedDeadTames(ownerId).size(), "TP all", owner -> ownedTames(owner));
    }

    private static int adminPlayerTeleportUnloaded(CommandSourceStack source, String playerName) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        String ownerName = resolveKnownOwnerName(source.getServer(), ownerId, playerName);
        ServerPlayer owner = source.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return adminError(source, ownerName + " must be online for admin tp.");
        }

        List<TameData> requested = ownedTames(ownerId);
        int queued = 0;
        int failed = 0;
        List<String> failedQueueNames = new ArrayList<>();

        for (TameData data : requested) {
            if (data == null || data.dead) {
                continue;
            }
            if (isEffectivelyLoaded(source, owner, data)) {
                continue;
            }
            UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, owner, data);
            if (unloaded.success) {
                queued++;
            } else {
                failed++;
                failedQueueNames.add(tameDisplayName(data) + " (" + unloaded.error + ")");
            }
        }

        sendAdminTeleportSummary(source, "Admin TP " + ownerName + " unloaded", 0, queued, ownedDeadTames(ownerId).size(), failed, 0, 0);
        if (!failedQueueNames.isEmpty()) {
            source.sendFailure(Component.literal("Admin unloaded tp failures for " + ownerName + ": " + String.join("; ", failedQueueNames)).withStyle(ChatFormatting.RED));
        }
        return queued > 0 ? 1 : 0;
    }

    private static int adminPlayerTeleportGroup(CommandSourceStack source, String playerName, String group) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        if (ownedGroup(ownerId, group).isEmpty()) {
            return adminError(source, "No tames found in group '" + group + "' for " + resolveKnownOwnerName(source.getServer(), ownerId, playerName) + ".");
        }
        return adminPlayerTeleportBatch(source, playerName, ownedDeadGroup(ownerId, group).size(), "TP group " + group, owner -> ownedGroup(owner, group));
    }

    private static int adminPlayerTeleportType(CommandSourceStack source, String playerName, String typeFilter) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        if (ownedType(ownerId, typeFilter).isEmpty()) {
            return adminError(source, "No tames found for type '" + typeFilter + "' for " + resolveKnownOwnerName(source.getServer(), ownerId, playerName) + ".");
        }
        return adminPlayerTeleportBatch(source, playerName, ownedDeadType(ownerId, typeFilter).size(), "TP type " + typeFilter, owner -> ownedType(owner, typeFilter));
    }

    private static int adminPlayerTeleportByMovementState(CommandSourceStack source, String playerName, MovementOrder order) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        return adminPlayerTeleportBatch(source, playerName, 0, "TP " + movementLabel(order), owner -> {
            List<TameData> requested = ownedTames(owner);
            List<TameData> selected = new ArrayList<>();
            for (TameData data : requested) {
                if (data == null || data.dead) {
                    continue;
                }
                TamableAnimal tame = findLoadedOwnedTameByUuid(source, owner, data.uuid);
                if (tame == null) {
                    if (matchesMovementOrderSnapshot(data, order)) {
                        selected.add(data);
                    }
                    continue;
                }
                if (matchesMovementOrder(tame, order)) {
                    selected.add(data);
                }
            }
            return selected;
        });
    }

    private static int adminPlayerTeleportByState(CommandSourceStack source, String playerName, String stateName) {
        MovementOrder order = parseMovementOrder(stateName);
        if (order == null) {
            return adminError(source, "Invalid state. Use follow, wander, or sit.");
        }
        return adminPlayerTeleportByMovementState(source, playerName, order);
    }

    private interface AdminOwnerSelection {
        List<TameData> select(UUID ownerId);
    }

    private static int adminPlayerTeleportBatch(CommandSourceStack source, String playerName, int deadSkipped, String label, AdminOwnerSelection selection) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        String ownerName = resolveKnownOwnerName(source.getServer(), ownerId, playerName);
        ServerPlayer owner = source.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return adminError(source, ownerName + " must be online for admin tp.");
        }

        List<TameData> requested = selection.select(ownerId);
        if (requested.isEmpty()) {
            return adminError(source, ownerName + " has no matching tames for admin tp.");
        }

        List<TamableAnimal> loadedTargets = new ArrayList<>();
        List<TameData> unloadedTargets = new ArrayList<>();
        int queued = 0;
        int failed = 0;
        int crossDimension = 0;
        List<String> failedNames = new ArrayList<>();

        for (TameData data : requested) {
            if (data == null || data.dead) {
                continue;
            }
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, ownerId, data.uuid);
            if (tame == null) {
                String queueError = validateUnloadedTeleportForPlayer(source, owner, data);
                if (queueError != null) {
                    failed++;
                    failedNames.add(tameDisplayName(data) + " (" + queueError + ")");
                    continue;
                }
                unloadedTargets.add(data);
                if (isCrossDimension(data, owner)) {
                    crossDimension++;
                }
                continue;
            }
            loadedTargets.add(tame);
            if (isCrossDimension(tame, owner)) {
                crossDimension++;
            }
        }

        for (TamableAnimal tame : loadedTargets) {
            teleportTameToPlayer(tame, owner);
        }
        for (TameData data : unloadedTargets) {
            UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, owner, data);
            if (unloaded.success) {
                queued++;
            } else {
                failed++;
                failedNames.add(tameDisplayName(data) + " (" + unloaded.error + ")");
            }
        }

        sendAdminTeleportSummary(source, "Admin " + ownerName + " " + label, loadedTargets.size(), queued, deadSkipped, failed, 0, crossDimension);
        if (!failedNames.isEmpty()) {
            source.sendFailure(Component.literal("Admin tp failures for " + ownerName + ": " + String.join("; ", failedNames)).withStyle(ChatFormatting.RED));
        }
        return loadedTargets.size() + queued > 0 ? 1 : 0;
    }

    private static void sendAdminTeleportSummary(CommandSourceStack source, String label, int loaded, int unloaded, int deadSkipped, int failed, int xpCost, int crossDimension) {
        if (source == null) {
            return;
        }
        source.sendSuccess(() -> buildTeleportSummary(label, loaded, unloaded, deadSkipped, failed, xpCost, crossDimension), false);
    }

    private static int adminPlayerRespawnPet(CommandSourceStack source, String playerName, String pet, ReviveMode mode, boolean reincarnateAfter) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        TameData data = findOwnedDeadTame(ownerId, pet);
        if (data == null) {
            return adminError(source, "No dead tame found with that name for " + resolveKnownOwnerName(source.getServer(), ownerId, playerName) + ".");
        }
        return adminPlayerRespawnDeadBatch(source, ownerId, List.of(data), mode, adminPlayerReviveLabel(resolveKnownOwnerName(source.getServer(), ownerId, playerName), mode, pet), reincarnateAfter);
    }

    private static int adminPlayerRespawnGroup(CommandSourceStack source, String playerName, String group, ReviveMode mode, boolean reincarnateAfter) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        List<TameData> dead = ownedDeadGroup(ownerId, group);
        if (dead.isEmpty()) {
            return adminError(source, "No dead tames in group '" + group + "' for " + resolveKnownOwnerName(source.getServer(), ownerId, playerName) + ".");
        }
        return adminPlayerRespawnDeadBatch(source, ownerId, dead, mode, adminPlayerReviveLabel(resolveKnownOwnerName(source.getServer(), ownerId, playerName), mode, "group '" + group + "'"), reincarnateAfter);
    }

    private static int adminPlayerRespawnType(CommandSourceStack source, String playerName, String typeFilter, ReviveMode mode, boolean reincarnateAfter) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        List<TameData> dead = ownedDeadType(ownerId, typeFilter);
        if (dead.isEmpty()) {
            return adminError(source, "No dead tames of type '" + typeFilter + "' for " + resolveKnownOwnerName(source.getServer(), ownerId, playerName) + ".");
        }
        return adminPlayerRespawnDeadBatch(source, ownerId, dead, mode, adminPlayerReviveLabel(resolveKnownOwnerName(source.getServer(), ownerId, playerName), mode, "type '" + typeFilter + "'"), reincarnateAfter);
    }

    private static int adminPlayerRespawnAll(CommandSourceStack source, String playerName, ReviveMode mode, boolean reincarnateAfter) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        List<TameData> dead = ownedDeadTames(ownerId);
        if (dead.isEmpty()) {
            return adminError(source, resolveKnownOwnerName(source.getServer(), ownerId, playerName) + " has no dead tames to respawn.");
        }
        return adminPlayerRespawnDeadBatch(source, ownerId, dead, mode, adminPlayerReviveLabel(resolveKnownOwnerName(source.getServer(), ownerId, playerName), mode, "all dead tames"), reincarnateAfter);
    }

    private static int respawnWaitingList(CommandSourceStack source, int requestedLimit) {
        ServerPlayer player = source.getPlayer();
        List<TameData> queue = morningRespawnCandidates(source.getServer(), player.getUUID());
        if (queue.isEmpty()) {
            return error(player, "No dead white-bed tames are waiting in the morning respawn queue.");
        }
        int limit = Math.min(Math.max(1, requestedLimit), queue.size());
        player.sendSystemMessage(Component.literal(
                "---- White Bed Morning Respawn Queue | order: " + respawnOrderLabel(player.getUUID()) + " | showing " + limit + "/" + queue.size() + " ----"
        ).withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < limit; i++) {
            TameData data = queue.get(i);
            player.sendSystemMessage(Component.literal(
                    (i + 1) + ". [" + data.level + "] " + data.name + " K:" + data.kills + " A:" + data.assists + " D:" + data.deaths
                            + "  " + respawnProgressSuffix(data)
            ).withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static int adminPlayerReincarnatePet(CommandSourceStack source, String playerName, String petName) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        String ownerName = resolveKnownOwnerName(source.getServer(), ownerId, playerName);
        TameData data = findOwnedTame(ownerId, petName);
        if (data == null) {
            return adminError(source, "Loaded/alive tame not found for " + ownerName + ".");
        }
        if (data.dead) {
            return adminError(source, "That tame is dead. Respawn it first.");
        }
        TamableAnimal tame = findLoadedTameByUuid(source, data.uuid);
        if (tame == null || !tame.isAlive()) {
            return adminError(source, "Reincarnation requires the tame to be loaded and alive.");
        }
        if (!data.hasSavedProgress) {
            return adminError(source, "That tame has no higher saved progress to restore.");
        }
        if (data.level >= data.savedLevel) {
            return adminError(source, "That tame is already at or above its highest saved level.");
        }
        int fromLevel = data.level;
        int targetLevel = data.savedLevel;
        if (!LevelSystem.restoreHighestProgressWithoutXpCost(tame, data)) {
            return adminError(source, "Failed to restore saved progress.");
        }
        source.sendSuccess(() -> Component.literal(
                "Admin reincarnated " + tameDisplayName(data) + " for " + ownerName + " from level " + fromLevel + " to level " + targetLevel + " (no cost)."
        ).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int adminPlayerReincarnateBatchAll(CommandSourceStack source, String playerName) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        return adminPlayerReincarnateBatch(
                source,
                resolveKnownOwnerName(source.getServer(), ownerId, playerName),
                eligibleLoadedReincarnationTames(source, ownerId, ownedTames(ownerId)),
                "all"
        );
    }

    private static int adminPlayerReincarnateBatchGroup(CommandSourceStack source, String playerName, String group) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        return adminPlayerReincarnateBatch(
                source,
                resolveKnownOwnerName(source.getServer(), ownerId, playerName),
                eligibleLoadedReincarnationTames(source, ownerId, ownedGroup(ownerId, group)),
                "group " + group
        );
    }

    private static int adminPlayerReincarnateBatchType(CommandSourceStack source, String playerName, String typeFilter) {
        UUID ownerId = resolveKnownOwnerUuid(source.getServer(), playerName);
        if (ownerId == null) {
            return adminError(source, "Unknown player: " + playerName);
        }
        return adminPlayerReincarnateBatch(
                source,
                resolveKnownOwnerName(source.getServer(), ownerId, playerName),
                eligibleLoadedReincarnationTames(source, ownerId, ownedType(ownerId, typeFilter)),
                "type " + typeFilter
        );
    }

    private static int adminPlayerReincarnateBatch(CommandSourceStack source, String ownerName, List<TameData> requested, String label) {
        if (requested.isEmpty()) {
            return adminError(source, "No loaded reincarnation-eligible tames found for " + ownerName + " " + label + ".");
        }
        int totalLevelsRestored = 0;
        int restored = 0;
        List<String> failed = new ArrayList<>();
        for (TameData data : requested) {
            TamableAnimal tame = findLoadedTameByUuid(source, data.uuid);
            if (tame == null || !tame.isAlive()) {
                continue;
            }
            int restoredLevels = Math.max(0, data.savedLevel - data.level);
            if (LevelSystem.restoreHighestProgressWithoutXpCost(tame, data)) {
                restored++;
                totalLevelsRestored += restoredLevels;
            } else {
                failed.add(tameDisplayName(data) + " (failed to restore saved progress)");
            }
        }
        if (restored <= 0) {
            StringBuilder message = new StringBuilder("No tames were reincarnated for ").append(ownerName).append(".");
            if (!failed.isEmpty()) {
                message.append(" Failures: ").append(String.join("; ", failed)).append(".");
            }
            return adminError(source, message.toString());
        }
        final int restoredCount = restored;
        final int restoredLevelsTotal = totalLevelsRestored;
        source.sendSuccess(() -> Component.literal(
                "Admin reincarnated " + restoredCount + " tame(s) for " + ownerName + " (" + restoredLevelsTotal + " total level(s) restored, no cost)."
        ).withStyle(ChatFormatting.GREEN), true);
        if (!failed.isEmpty()) {
            source.sendFailure(Component.literal("Failures: " + String.join("; ", failed)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int graveyard(CommandSourceStack source, int requestedLimit) {
        ServerPlayer player = source.getPlayer();
        List<TameData> dead = ownedDeadTames(player.getUUID());
        if (dead.isEmpty()) {
            return error(player, "Your graveyard is empty.");
        }
        dead.sort(respawnQueueComparator(player.getUUID()));
        int limit = Math.min(Math.max(1, requestedLimit), dead.size());
        player.sendSystemMessage(Component.literal(
                "---- Graveyard | order: " + respawnOrderLabel(player.getUUID()) + " | showing " + limit + "/" + dead.size() + " ----"
        ).withStyle(ChatFormatting.DARK_GRAY));
        for (int i = 0; i < limit; i++) {
            TameData data = dead.get(i);
            int reincarnationCost = data.hasSavedProgress ? LevelSystem.reincarnationXpCost(data) : 0;
            player.sendSystemMessage(Component.literal(
                    (i + 1) + ". [" + data.level + "] " + data.name
                            + "  died " + formatDeathTime(data.deadUnixMillis)
                            + "  activeDays " + Math.max(0, data.activeSurvivalDays)
                            + "  reincarnateCost " + reincarnationCost
                            + "  " + respawnProgressSuffix(data)
            ).withStyle(ChatFormatting.GRAY));
            String deathMessage = latestDeathMessage(data);
            if (!deathMessage.isBlank()) {
                player.sendSystemMessage(Component.literal("    cause: " + deathMessage).withStyle(ChatFormatting.DARK_RED));
            }
        }
        return 1;
    }

    private static int adminPlayerRespawnDeadBatch(CommandSourceStack source, UUID ownerId, List<TameData> candidates, ReviveMode mode, String label, boolean reincarnateAfter) {
        if (source == null || ownerId == null || candidates == null || candidates.isEmpty()) {
            return 0;
        }
        String ownerName = resolveKnownOwnerName(source.getServer(), ownerId, ownerId.toString());
        ServerPlayer ownerPlayer = source.getServer() == null ? null : source.getServer().getPlayerList().getPlayer(ownerId);
        int success = 0;
        int failed = 0;
        int reincarnated = 0;
        List<String> spentLabels = new ArrayList<>();
        List<String> failReasons = new ArrayList<>();
        List<String> unaffordable = new ArrayList<>();

        for (TameData data : candidates) {
            if (data == null || data.uuid == null) {
                failed++;
                failReasons.add("unknown (invalid registry entry)");
                continue;
            }
            if (!isDeadEntry(data.uuid)) {
                continue;
            }
            if (findLoadedOwnedTameByUuid(source, ownerId, data.uuid) != null) {
                failed++;
                failReasons.add(data.name + " (already loaded)");
                continue;
            }
            int xpCost = 0;
            int approvedItemCost = 0;
            if (reincarnateAfter) {
                if (ownerPlayer == null) {
                    failed++;
                    failReasons.add(data.name + " (owner must be online for payment)");
                    continue;
                }
                xpCost = reviveXpCost(data, mode);
                approvedItemCost = reviveApprovedItemCost(data, mode);
                if (data.hasSavedProgress && data.level < data.savedLevel) {
                    xpCost += LevelSystem.reincarnationXpCost(data);
                    approvedItemCost += Math.max(1, data.savedLevel - data.level);
                }
                PaymentResult preview = previewPayment(ownerPlayer, xpCost, approvedItemCost, true, "respawn reincarnation");
                if (!preview.success) {
                    failed++;
                    unaffordable.add(pricedTameLabel(data, paymentPriceLabel(xpCost, approvedItemCost, true)));
                    continue;
                }
            }
            SpawnTarget target = resolveAdminPlayerRespawnTarget(source, ownerId, data, mode);
            if (target == null || target.level == null) {
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
            if (reincarnateAfter) {
                PaymentResult payment = tryConsumePayment(ownerPlayer, xpCost, approvedItemCost, true, "respawn reincarnation");
                if (!payment.success) {
                    failed++;
                    failReasons.add(data.name + " (" + payment.error + ")");
                    continue;
                }
                spentLabels.add(payment.label);
            }
            if (reincarnateAfter) {
                TamableAnimal respawned = findLoadedTameByUuid(source, data.uuid);
                if (respawned != null && respawned.isAlive() && data.hasSavedProgress && data.level < data.savedLevel
                        && LevelSystem.restoreHighestProgressWithoutXpCost(respawned, data)) {
                    reincarnated++;
                }
            }
            success++;
        }

        if (success <= 0) {
            StringBuilder message = new StringBuilder("No dead tames respawned for ").append(ownerName).append(".");
            if (!failReasons.isEmpty()) {
                message.append(" Reasons: ").append(String.join("; ", failReasons));
            }
            if (!unaffordable.isEmpty()) {
                message.append(" Could not afford: ").append(String.join("; ", unaffordable)).append(".");
            }
            return adminError(source, message.toString());
        }
        final int respawnedCount = success;
        final int reincarnatedCount = reincarnated;
        final String spentText = spentLabels.isEmpty() ? "no cost" : String.join(", ", spentLabels);
        source.sendSuccess(() -> Component.literal(
                label + ": " + respawnedCount + " tame(s) for " + ownerName
                        + (reincarnateAfter ? ", " + reincarnatedCount + " restored to saved progress" : "")
                        + ", paid " + spentText + "."
        ).withStyle(ChatFormatting.GREEN), true);
        if (!unaffordable.isEmpty() && ownerPlayer != null) {
            sendAffordabilityFailures(ownerPlayer, "Could not afford respawn reincarnation for", unaffordable);
        }
        if (failed > 0 && !failReasons.isEmpty()) {
            source.sendFailure(Component.literal("Respawn failed for " + failed + ": " + String.join("; ", failReasons)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int respawnDeadBatch(CommandSourceStack source, ServerPlayer player, List<TameData> candidates, ReviveMode mode, String label) {
        if (source == null || player == null || candidates == null || candidates.isEmpty()) return 0;
        int success = 0;
        int failed = 0;
        List<String> spentLabels = new ArrayList<>();
        List<String> failReasons = new ArrayList<>();
        List<String> unaffordable = new ArrayList<>();

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
            int xpCost = reviveXpCost(data, mode);
            int approvedItemCost = reviveApprovedItemCost(data, mode);
            PaymentResult preview = previewPayment(player, xpCost, approvedItemCost, true, mode == ReviveMode.ARISE ? "arise" : "respawn");
            if (!preview.success) {
                failed++;
                unaffordable.add(pricedTameLabel(data, paymentPriceLabel(xpCost, approvedItemCost, true)));
                continue;
            }
            SpawnTarget target = resolveRespawnTarget(source, player, data, mode.toMe);
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
            PaymentResult payment = tryConsumePayment(player, xpCost, approvedItemCost, true, mode == ReviveMode.ARISE ? "arise" : "respawn");
            if (!payment.success) {
                failed++;
                failReasons.add(data.name + " (" + payment.error + ")");
                continue;
            }
            spentLabels.add(payment.label);
            success++;
        }

        if (success <= 0) {
            StringBuilder message = new StringBuilder("No dead tames respawned.");
            if (!unaffordable.isEmpty()) {
                message.append(" Could not afford: ").append(String.join("; ", unaffordable)).append(".");
            }
            if (!failReasons.isEmpty()) {
                message.append(" Reasons: ").append(String.join("; ", failReasons));
            }
            return error(player, message.toString());
        }
        String spentText = spentLabels.isEmpty() ? "no cost" : String.join(", ", spentLabels);
        player.sendSystemMessage(Component.literal(label + ": " + success + " tame(s), paid " + spentText + ".").withStyle(ChatFormatting.GREEN));
        sendAffordabilityFailures(player, "Could not afford respawn for", unaffordable);
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

    private static SpawnTarget resolveAdminPlayerRespawnTarget(CommandSourceStack source, UUID ownerId, TameData data, ReviveMode mode) {
        if (source == null || source.getServer() == null) {
            return null;
        }
        if (mode == null) {
            mode = ReviveMode.RESPAWN;
        }
        ServerPlayer owner = ownerId == null ? null : source.getServer().getPlayerList().getPlayer(ownerId);
        if (mode.toMe) {
            if (owner == null) {
                return null;
            }
            return new SpawnTarget(owner.serverLevel(), owner.position(), owner.getYRot(), owner.getXRot());
        }
        if (data != null && data.hasPetBed && data.petBedDimension != null && !data.petBedDimension.isBlank()) {
            ResourceLocation bedId = ResourceLocation.tryParse(data.petBedDimension);
            if (bedId != null) {
                ServerLevel bedLevel = source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, bedId));
                if (bedLevel != null) {
                    return new SpawnTarget(bedLevel, new Vec3(data.petBedX + 0.5D, data.petBedY, data.petBedZ + 0.5D), 0.0F, 0.0F);
                }
            }
        }
        SpawnTarget diQueueTarget = resolveBedTargetFromDiQueue(source, data);
        if (diQueueTarget != null) {
            return diQueueTarget;
        }
        SpawnTarget ownerBedTarget = resolveOwnerBedTarget(source.getServer(), ownerId);
        if (ownerBedTarget != null) {
            return ownerBedTarget;
        }
        if (owner != null) {
            return new SpawnTarget(owner.serverLevel(), owner.position(), owner.getYRot(), owner.getXRot());
        }
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

    private record AdminRespawnEntry(TameData data, TameDeathRecord deadRecord) {
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
        prepareAutoReincarnationOnRespawn(level.getServer(), data);
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

    public static boolean restoreDuelParticipantSnapshot(MinecraftServer server, CompoundTag tameSnapshotTag) {
        if (server == null || tameSnapshotTag == null || tameSnapshotTag.isEmpty()) {
            return false;
        }
        TameData snapshot = TameData.fromTag(tameSnapshotTag.copy());
        if (snapshot.uuid == null) {
            return false;
        }
        TamableAnimal loaded = findLoadedTameByUuid(server, snapshot.uuid);
        if (loaded != null) {
            loaded.discard();
        }
        String typeId = recoverEntityTypeId(snapshot);
        if (typeId.isBlank()) {
            return false;
        }
        ResourceLocation entityId = ResourceLocation.tryParse(typeId);
        if (entityId == null) {
            return false;
        }
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (entityType == null) {
            return false;
        }
        SpawnTarget target = spawnTargetFromSnapshot(server, snapshot);
        if (target == null || target.level == null || target.pos == null) {
            return false;
        }
        Entity created = entityType.create(target.level);
        if (!(created instanceof TamableAnimal restored)) {
            return false;
        }
        CompoundTag entitySnapshot = snapshot.entitySnapshot == null ? new CompoundTag() : snapshot.entitySnapshot.copy();
        if (!entitySnapshot.isEmpty()) {
            restored.load(entitySnapshot);
        }
        restored.setUUID(snapshot.uuid);
        TameData.syncTlIdToEntity(restored, snapshot.tlId);
        restored.moveTo(target.pos.x, target.pos.y, target.pos.z, target.yRot, target.xRot);
        enforceTamedOwnerPreserveCollar(restored, snapshot.ownerUUID);
        if (!target.level.addFreshEntity(restored)) {
            return false;
        }
        TameGoalInstaller.installIfMissing(restored);
        TameRegistry.register(snapshot);
        TameRegistry.markDirty();
        return true;
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
        prepareAutoReincarnationOnRespawn(level.getServer(), data);
        finalizeRespawnState(respawned, data);
        return RespawnResult.ok();
    }

    private static SpawnTarget spawnTargetFromSnapshot(MinecraftServer server, TameData data) {
        if (server == null || data == null) {
            return null;
        }
        ServerLevel level = null;
        if (data.lastKnownDimension != null && !data.lastKnownDimension.isBlank()) {
            ResourceLocation dimId = ResourceLocation.tryParse(data.lastKnownDimension);
            if (dimId != null) {
                level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
            }
        }
        if (level == null) {
            level = server.overworld();
        }
        Vec3 pos = new Vec3(data.lastKnownX + 0.5D, data.lastKnownY, data.lastKnownZ + 0.5D);
        float yRot = 0.0F;
        float xRot = 0.0F;
        CompoundTag entitySnapshot = data.entitySnapshot;
        if (entitySnapshot != null && entitySnapshot.contains("Pos", Tag.TAG_LIST)) {
            ListTag posList = entitySnapshot.getList("Pos", Tag.TAG_DOUBLE);
            if (posList.size() >= 3) {
                pos = new Vec3(posList.getDouble(0), posList.getDouble(1), posList.getDouble(2));
            }
        }
        if (entitySnapshot != null && entitySnapshot.contains("Rotation", Tag.TAG_LIST)) {
            ListTag rotList = entitySnapshot.getList("Rotation", Tag.TAG_FLOAT);
            if (rotList.size() >= 2) {
                yRot = rotList.getFloat(0);
                xRot = rotList.getFloat(1);
            }
        }
        return new SpawnTarget(level, pos, yRot, xRot);
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
        tame.stopRiding();
        if (tame.isVehicle()) {
            tame.ejectPassengers();
        }
        tame.removeAllEffects();
        tame.setHealth(tame.getMaxHealth());
        tame.hurtTime = 0;
        tame.deathTime = 0;
        tame.invulnerableTime = 0;
        tame.setSecondsOnFire(0);
        tame.setRemainingFireTicks(0);
        tame.fallDistance = 0.0F;
        tame.setDeltaMovement(0.0D, 0.0D, 0.0D);
        tame.setTarget(null);
        tame.getNavigation().stop();
        tame.setNoGravity(false);
        tame.setNoAi(false);
        tame.setInSittingPose(false);
        TameGoalInstaller.installIfMissing(tame);
        if (data.hasHome) {
            // Respawns should preserve guardian mode when the tame died with a current guardian anchor.
            tame.setOrderedToSit(false);
            clearExternalWanderingState(tame, MovementOrder.GUARDIAN);
        }
        data.dead = false;
        data.deadGameTime = 0L;
        data.deadUnixMillis = 0L;
        data.deathDimension = "";
        data.deathX = 0;
        data.deathY = 0;
        data.deathZ = 0;
        data.lastActiveSurvivalDay = Long.MIN_VALUE;
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
        clearGuardianAnchor(data);
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

        boolean normalized = applyTypeBasePlusBonus(recovered, data);
        if (!normalized) {
            LevelSystem.updateTameName(recovered, data);
            recovered.setHealth(recovered.getMaxHealth());
        }
        finalizeRespawnState(recovered, data);

        data.ownerUUID = p.getUUID();
        TameRegistry.markDirty();
        return RecoverResult.ok(recovered);
    }

    private static RecoverResult recoverPetEntityAtLocation(ServerPlayer owner, SpawnTarget target, TameData data) {
        if (owner == null || target == null || target.level == null || target.pos == null || data == null) {
            return RecoverResult.fail("invalid context");
        }
        if (data.uuid == null) return RecoverResult.fail("missing tame UUID");
        clearGuardianAnchor(data);
        if (isDeadEntry(data.uuid)) return RecoverResult.fail("tame is marked dead");
        String logicalKey = logicalTameKey(data);
        if (!logicalKey.isBlank() && hasLoadedLogicalDuplicate(owner.getServer(), data, logicalKey)) {
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

        Entity spawned = entityType.create(target.level);
        if (!(spawned instanceof TamableAnimal recovered)) {
            return RecoverResult.fail("stored type is not tamable");
        }

        CompoundTag snapshot = data.entitySnapshot == null ? new CompoundTag() : data.entitySnapshot.copy();
        if (!snapshot.isEmpty()) {
            recovered.load(snapshot);
        }

        recovered.setUUID(data.uuid);
        recovered.moveTo(target.pos.x, target.pos.y, target.pos.z, target.yRot, target.xRot);
        recovered.setDeltaMovement(0.0D, 0.0D, 0.0D);
        enforceTamedOwnerPreserveCollar(recovered, owner.getUUID());

        if (!target.level.addFreshEntity(recovered)) {
            return RecoverResult.fail("spawn failed (UUID conflict or invalid state)");
        }

        boolean normalized = applyTypeBasePlusBonus(recovered, data);
        if (!normalized) {
            LevelSystem.updateTameName(recovered, data);
            recovered.setHealth(recovered.getMaxHealth());
        }
        finalizeRespawnState(recovered, data);

        data.ownerUUID = owner.getUUID();
        TameRegistry.markDirty();
        return RecoverResult.ok(recovered);
    }

    private static int teleportAll(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        List<TameData> requested = ownedTames(p.getUUID());
        List<TamableAnimal> targets = new ArrayList<>();
        List<TameData> queuedTargets = new ArrayList<>();
        int queued = 0;
        int queueFailed = 0;
        List<String> failedQueueNames = new ArrayList<>();
        List<String> unaffordable = new ArrayList<>();
        int cost = 0;
        int crossDimension = 0;
        int xpBudget = currentXpPoints(p);
        for (TameData d : requested) {
            if (d == null || d.dead) {
                continue;
            }
            TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (ta == null) {
                queuedTargets.add(d);
                continue;
            }
            boolean cross = isCrossDimension(ta, p);
            int tameCost = teleportCostFor(d, cross);
            if (tameCost > xpBudget) {
                unaffordable.add(pricedTameLabel(d, teleportPriceLabel(tameCost)));
                continue;
            }
            targets.add(ta);
            xpBudget -= tameCost;
            cost += tameCost;
            if (cross) crossDimension++;
        }
        if (!payTeleportXp(p, cost)) return 0;
        for (TamableAnimal ta : targets) teleportTameToPlayer(ta, p);
        for (TameData d : queuedTargets) {
            UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, p, d);
            if (unloaded.success) {
                queued++;
            } else {
                queueFailed++;
                failedQueueNames.add(tameDisplayName(d) + " (" + unloaded.error + ")");
            }
        }
        sendTeleportSummary(p, "TP all", targets.size(), queued, ownedDeadTames(p.getUUID()).size(), queueFailed, cost, crossDimension);
        sendAffordabilityFailures(p, "Could not afford tp for", unaffordable);
        if (!failedQueueNames.isEmpty()) {
            p.sendSystemMessage(Component.literal("Unloaded tp failures: " + String.join("; ", failedQueueNames)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int teleportGuardianAll(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        List<TameData> requested = ownedGuardianTames(player.getUUID());
        if (requested.isEmpty()) return error(player, "You have no guardian tames to teleport.");
        return teleportGuardianBatch(source, player, requested, "TPGuardian all", ownedDeadGuardianTames(player.getUUID()).size());
    }

    private static int teleportGuardianGroup(CommandSourceStack source, String group) {
        ServerPlayer player = source.getPlayer();
        List<TameData> requested = ownedGuardianGroup(player.getUUID(), group);
        if (requested.isEmpty()) return error(player, "No guardian tames found in group '" + group + "'.");
        return teleportGuardianBatch(source, player, requested, "TPGuardian group " + group, ownedDeadGuardianGroup(player.getUUID(), group).size());
    }

    private static int teleportGuardianType(CommandSourceStack source, String typeFilter) {
        ServerPlayer player = source.getPlayer();
        List<TameData> requested = ownedGuardianType(player.getUUID(), typeFilter);
        if (requested.isEmpty()) return error(player, "No guardian tames found for type '" + typeFilter + "'.");
        return teleportGuardianBatch(source, player, requested, "TPGuardian type " + typeFilter, ownedDeadGuardianType(player.getUUID(), typeFilter).size());
    }

    private static int teleportAllHome(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        List<TameData> requested = ownedTames(player.getUUID());
        if (requested.isEmpty()) return error(player, "You have no tames to teleport.");
        return teleportHomeBatch(source, player, requested, "TPHome all", ownedDeadTames(player.getUUID()).size());
    }

    private static int teleportAllFromDimension(CommandSourceStack source, ServerLevel fromDimension) {
        ServerPlayer p = source.getPlayer();
        if (fromDimension == null) return error(p, "Invalid dimension.");

        List<TameData> requested = ownedTames(p.getUUID());
        List<TamableAnimal> targets = new ArrayList<>();
        List<TameData> queuedTargets = new ArrayList<>();
        int queued = 0;
        int queueFailed = 0;
        List<String> failedQueueNames = new ArrayList<>();
        List<String> unaffordable = new ArrayList<>();
        int cost = 0;
        int crossDimension = 0;
        ResourceLocation dimensionId = fromDimension.dimension().location();
        int xpBudget = currentXpPoints(p);

        for (TameData d : requested) {
            if (d == null || d.dead) {
                continue;
            }
            TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (ta == null) {
                if (!matchesDimensionFilter(d, null, dimensionId)) {
                    continue;
                }
                queuedTargets.add(d);
                continue;
            }
            if (!matchesDimensionFilter(d, ta, dimensionId)) {
                continue;
            }
            boolean cross = isCrossDimension(ta, p);
            int tameCost = teleportCostFor(d, cross);
            if (tameCost > xpBudget) {
                unaffordable.add(pricedTameLabel(d, teleportPriceLabel(tameCost)));
                continue;
            }
            targets.add(ta);
            xpBudget -= tameCost;
            cost += tameCost;
            if (cross) crossDimension++;
        }

        if (!payTeleportXp(p, cost)) return 0;
        for (TamableAnimal ta : targets) teleportTameToPlayer(ta, p);
        for (TameData d : queuedTargets) {
            UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, p, d);
            if (unloaded.success) {
                queued++;
            } else {
                queueFailed++;
                failedQueueNames.add(tameDisplayName(d) + " (" + unloaded.error + ")");
            }
        }
        int deadSkipped = 0;
        for (TameData d : ownedDeadTames(p.getUUID())) {
            if (matchesDimensionFilter(d, null, dimensionId)) {
                deadSkipped++;
            }
        }
        sendTeleportSummary(p, "TP dim " + dimensionId, targets.size(), queued, deadSkipped, queueFailed, cost, crossDimension);
        sendAffordabilityFailures(p, "Could not afford tp for", unaffordable);
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
            if (d == null || d.dead) {
                continue;
            }
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

    private static int teleportUnloadedHome(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        List<TameData> requested = ownedTames(player.getUUID());
        int queued = 0;
        int failed = 0;
        List<String> failedNames = new ArrayList<>();

        for (TameData data : requested) {
            if (data == null || data.dead || isEffectivelyLoaded(source, player, data)) {
                continue;
            }
            SpawnTarget target = resolveRespawnTarget(source, player, data, false);
            if (target == null || target.level == null || target.pos == null) {
                failed++;
                failedNames.add((data.name == null ? "unknown" : data.name) + " (invalid respawn home)");
                continue;
            }
            UnloadedTpResult result = tpUnloadedHomeViaLanternOrRecover(source, player, data, target);
            if (!result.success) {
                failed++;
                failedNames.add((data.name == null ? "unknown" : data.name) + " (" + result.error + ")");
                continue;
            }
            queued++;
        }

        sendTeleportSummary(player, "TPHome unloaded", 0, queued, ownedDeadTames(player.getUUID()).size(), failed, 0, 0);
        if (!failedNames.isEmpty()) {
            player.sendSystemMessage(Component.literal("Unloaded tphome failures: " + String.join("; ", failedNames)).withStyle(ChatFormatting.RED));
        }
        return queued > 0 ? 1 : 0;
    }

    private static int teleportGuardianBatch(CommandSourceStack source, ServerPlayer player, List<TameData> requested, String label, int deadSkipped) {
        List<TamableAnimal> loadedTargets = new ArrayList<>();
        List<TameData> unloadedTargets = new ArrayList<>();
        int queued = 0;
        int queueFailed = 0;
        int cost = 0;
        int crossDimension = 0;
        List<String> failedNames = new ArrayList<>();
        List<String> unaffordable = new ArrayList<>();
        int xpBudget = currentXpPoints(player);

        for (TameData data : requested) {
            if (data == null || data.uuid == null || data.dead || !data.hasHome) {
                continue;
            }
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, player.getUUID(), data.uuid);
            if (tame == null) {
                String previewError = validateGuardianTeleport(source, player, data);
                if (previewError != null) {
                    queueFailed++;
                    failedNames.add((data.name == null ? "unknown" : data.name) + " (" + previewError + ")");
                    continue;
                }
                unloadedTargets.add(data);
                boolean cross = data.lastKnownDimension != null
                        && !data.lastKnownDimension.isBlank()
                        && !player.serverLevel().dimension().location().toString().equals(data.lastKnownDimension);
                int tameCost = teleportGuardianCostFor(data, cross);
                if (tameCost > xpBudget) {
                    unaffordable.add(pricedTameLabel(data, teleportPriceLabel(tameCost)));
                    unloadedTargets.remove(unloadedTargets.size() - 1);
                    continue;
                }
                xpBudget -= tameCost;
                cost += tameCost;
                if (cross) crossDimension++;
                continue;
            }
            boolean cross = isCrossDimension(tame, player);
            int tameCost = teleportGuardianCostFor(data, cross);
            if (tameCost > xpBudget) {
                unaffordable.add(pricedTameLabel(data, teleportPriceLabel(tameCost)));
                continue;
            }
            loadedTargets.add(tame);
            xpBudget -= tameCost;
            cost += tameCost;
            if (cross) crossDimension++;
        }

        if (loadedTargets.isEmpty() && unloadedTargets.isEmpty()) {
            return error(player, "No guardian tames could be teleported.");
        }
        if (!payTeleportXp(player, cost)) return 0;
        for (TamableAnimal tame : loadedTargets) {
            teleportTameToPlayer(tame, player);
        }
        for (TameData data : unloadedTargets) {
            UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, player, data);
            if (unloaded.success) {
                queued++;
            } else {
                queueFailed++;
                failedNames.add((data.name == null ? "unknown" : data.name) + " (" + unloaded.error + ")");
            }
        }
        sendTeleportSummary(player, label, loadedTargets.size(), queued, deadSkipped, queueFailed, cost, crossDimension);
        sendAffordabilityFailures(player, "Could not afford tpguardian for", unaffordable);
        if (!failedNames.isEmpty()) {
            player.sendSystemMessage(Component.literal("TPGuardian failures: " + String.join("; ", failedNames)).withStyle(ChatFormatting.RED));
        }
        return 1;
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
            UnloadedTpResult result = tpUnloadedViaLanternOrRecover(source, admin, data);
            if (result.success) {
                queued++;
            } else {
                queueFailed++;
                failedQueueNames.add((data.name == null ? "unknown" : data.name) + " (" + result.error + ")");
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

    private static int adminCleanupProjectiles(CommandSourceStack source, String mode) {
        MinecraftServer server = source.getServer();
        ServerPlayer admin = source.getPlayer();
        if (server == null || admin == null) {
            return 0;
        }
        String normalized = mode == null ? "all" : mode.trim().toLowerCase(Locale.ROOT);
        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> toDiscard = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity == null || entity.isRemoved()) {
                    continue;
                }
                if (matchesProjectileCleanup(entity, normalized)) {
                    toDiscard.add(entity);
                }
            }
            for (Entity entity : toDiscard) {
                entity.discard();
                removed++;
            }
        }
        admin.sendSystemMessage(Component.literal("Removed " + removed + " tame projectile entities (" + normalized + ").").withStyle(ChatFormatting.YELLOW));
        return removed > 0 ? 1 : 0;
    }

    private static boolean matchesProjectileCleanup(Entity entity, String mode) {
        if ("dragon_fireball".equals(mode)) {
            return entity instanceof TimedTameDragonFireball;
        }
        return entity instanceof TimedTameArrow
                || entity instanceof TimedTameSnowball
                || entity instanceof TimedTameThrownPotion
                || entity instanceof TimedTameWitherSkull
                || entity instanceof TimedTameSmallFireball
                || entity instanceof TimedTameTrident
                || entity instanceof TimedTameDragonFireball
                || entity instanceof TimedTameLlamaSpit
                || entity instanceof TimedTameShulkerBullet
                || entity instanceof NoGriefLargeFireball;
    }

    private static UnloadedTpResult tpUnloadedViaLanternOrRecover(CommandSourceStack source, ServerPlayer owner, TameData data) {
        if (owner == null) {
            return UnloadedTpResult.fail("owner unavailable");
        }
        if (data == null || data.dead || (data.uuid != null && isDeadEntry(data.uuid))) {
            return UnloadedTpResult.fail("tame is dead");
        }
        SpawnTarget target = new SpawnTarget(owner.serverLevel(), owner.position(), owner.getYRot(), owner.getXRot());
        boolean crossDimension = isCrossDimension(data, target.level);
        String validationError = validateUnloadedHomeTeleport(source, owner, data, target);
        if (validationError != null) {
            if (crossDimension) {
                return UnloadedTpResult.fail(validationError);
            }
            return tryRebuildSnapshotTeleport(owner, target, data, validationError);
        }
        return tryImmediateChunkLoadTeleport(source, data, target, crossDimension);
    }

    private static String validateUnloadedHomeTeleport(CommandSourceStack source, ServerPlayer owner, TameData data, SpawnTarget target) {
        if (source == null || owner == null || data == null || target == null || target.level == null || target.pos == null) {
            return "invalid context";
        }
        if (data.dead || (data.uuid != null && isDeadEntry(data.uuid))) {
            return "tame is dead";
        }
        if (data.uuid == null) {
            return "missing tame UUID";
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
        return null;
    }

    private static UnloadedTpResult tpUnloadedHomeViaLanternOrRecover(CommandSourceStack source, ServerPlayer owner, TameData data, SpawnTarget target) {
        if (data == null || data.dead || (data.uuid != null && isDeadEntry(data.uuid))) {
            return UnloadedTpResult.fail("tame is dead");
        }
        boolean crossDimension = isCrossDimension(data, target.level);
        String validationError = validateUnloadedHomeTeleport(source, owner, data, target);
        if (validationError != null) {
            if (crossDimension) {
                return UnloadedTpResult.fail(validationError);
            }
            return tryRebuildSnapshotTeleport(owner, target, data, validationError);
        }
        return tryImmediateChunkLoadTeleport(source, data, target, crossDimension);
    }

    private static UnloadedTpResult tryRebuildSnapshotTeleport(ServerPlayer owner, SpawnTarget target, TameData data, String reason) {
        if (owner == null || target == null || data == null) {
            return UnloadedTpResult.fail(reason);
        }
        RecoverResult recoverResult = recoverPetEntityAtLocation(owner, target, data);
        if (recoverResult.entity != null) {
            owner.sendSystemMessage(Component.literal("Rebuilt unloaded " + (data.name == null ? "unknown" : data.name) + " from snapshot (" + reason + ").").withStyle(ChatFormatting.YELLOW));
            return UnloadedTpResult.queued();
        }
        return UnloadedTpResult.fail(reason + "; snapshot rebuild failed: " + recoverResult.error);
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
        if (owner == null) {
            return "owner unavailable";
        }
        return tryQueueUnloadedTeleportToTarget(
                source,
                owner,
                data,
                new SpawnTarget(owner.serverLevel(), owner.position(), owner.getYRot(), owner.getXRot()),
                LanternRequest.MODE_PLAYER_TP
        );
    }

    private static String tryQueueUnloadedTeleportToTarget(CommandSourceStack source, ServerPlayer owner, TameData data, SpawnTarget target, String mode) {
        if (source == null || owner == null || data == null || data.uuid == null) {
            return "invalid context";
        }
        if (source.getServer() == null) {
            return "server unavailable";
        }
        if (target == null || target.level == null || target.pos == null) {
            return "invalid target";
        }
        if (isLoadedAnywhere(source.getServer(), data.uuid)) {
            return "already loaded";
        }
        if (data.lastKnownDimension == null || data.lastKnownDimension.isBlank()) {
            return "missing last known dimension";
        }
        clearGuardianAnchor(data);
        ResourceLocation lastKnown = ResourceLocation.tryParse(data.lastKnownDimension);
        if (lastKnown == null) {
            return "invalid last known dimension";
        }
        ResourceKey<Level> sourceDimension = ResourceKey.create(Registries.DIMENSION, lastKnown);
        ResourceKey<Level> targetDimension = target.level.dimension();
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
                mode,
                targetDimension.location().toString(),
                target.pos.x,
                target.pos.y,
                target.pos.z,
                target.yRot,
                target.xRot
        );
        worldData.addLanternRequest(request);
        loadChunksAround(sourceLevel, data.uuid, request.getChunkPosition(), true);
        return null;
    }

    private static UnloadedTpResult tryImmediateChunkLoadTeleport(CommandSourceStack source, TameData data, SpawnTarget target) {
        return tryImmediateChunkLoadTeleport(source, data, target, false);
    }

    private static UnloadedTpResult tryImmediateChunkLoadTeleport(CommandSourceStack source, TameData data, SpawnTarget target, boolean liveEntityOnly) {
        if (source == null || source.getServer() == null || data == null || data.uuid == null) {
            return UnloadedTpResult.fail("invalid context");
        }
        if (target == null || target.level == null || target.pos == null) {
            return UnloadedTpResult.fail("invalid target");
        }
        ResourceLocation lastKnown = ResourceLocation.tryParse(data.lastKnownDimension);
        if (lastKnown == null) {
            return UnloadedTpResult.fail("invalid last known dimension");
        }
        ServerLevel sourceLevel = source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, lastKnown));
        if (sourceLevel == null) {
            return UnloadedTpResult.fail("source level unavailable");
        }

        BlockPos sourcePos = new BlockPos(data.lastKnownX, data.lastKnownY, data.lastKnownZ);
        ChunkPos sourceChunk = new ChunkPos(sourcePos);
        UUID ticketId = data.tlId != null ? data.tlId : data.uuid;
        loadChunksAround(sourceLevel, ticketId, sourcePos, true);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                sourceLevel.getChunk(sourceChunk.x + dx, sourceChunk.z + dz);
            }
        }
        TamableAnimal tame = findLoadedTameByIdentity(sourceLevel, data.uuid, data.tlId);
        if (tame != null && tame.isAlive()) {
            try {
                teleportTameToLocation(tame, target);
                return UnloadedTpResult.queued();
            } finally {
                loadChunksAround(sourceLevel, ticketId, sourcePos, false);
            }
        }
        long now = source.getServer().overworld() == null ? 0L : source.getServer().overworld().getGameTime();
        PendingImmediateChunkTeleport pending = new PendingImmediateChunkTeleport(
                ticketId,
                data.uuid,
                data.tlId,
                data.ownerUUID,
                sourceLevel.dimension(),
                sourcePos,
                target,
                now,
                now + IMMEDIATE_CHUNK_TP_INITIAL_DELAY_TICKS,
                -1L,
                data.name,
                liveEntityOnly,
                false
        );
        PENDING_IMMEDIATE_CHUNK_TELEPORTS.put(ticketId, pending);
        return UnloadedTpResult.queued();
    }

    private static UnloadedTpResult queueImmediateChunkTeleport(ServerLevel sourceLevel, BlockPos sourcePos, TameData data, SpawnTarget target, boolean liveEntityOnly, boolean silent) {
        if (sourceLevel == null || sourcePos == null || data == null || data.uuid == null) {
            return UnloadedTpResult.fail("invalid context");
        }
        if (target == null || target.level == null || target.pos == null) {
            return UnloadedTpResult.fail("invalid target");
        }
        UUID ticketId = data.tlId != null ? data.tlId : data.uuid;
        loadChunksAround(sourceLevel, ticketId, sourcePos, true);
        long now = sourceLevel.getServer() != null && sourceLevel.getServer().overworld() != null
                ? sourceLevel.getServer().overworld().getGameTime()
                : sourceLevel.getGameTime();
        PendingImmediateChunkTeleport pending = new PendingImmediateChunkTeleport(
                ticketId,
                data.uuid,
                data.tlId,
                data.ownerUUID,
                sourceLevel.dimension(),
                sourcePos,
                target,
                now,
                now + IMMEDIATE_CHUNK_TP_INITIAL_DELAY_TICKS,
                -1L,
                data.name,
                liveEntityOnly,
                silent
        );
        PENDING_IMMEDIATE_CHUNK_TELEPORTS.put(ticketId, pending);
        return UnloadedTpResult.queued();
    }

    private static UnloadedTpResult tryImmediateChunkLoadTeleportSilent(CommandSourceStack source, ServerPlayer owner, TameData data, SpawnTarget target) {
        return tryImmediateChunkLoadTeleportSilent(source, owner, data, target, false);
    }

    private static UnloadedTpResult tryImmediateChunkLoadTeleportSilent(CommandSourceStack source, ServerPlayer owner, TameData data, SpawnTarget target, boolean liveEntityOnly) {
        if (source == null || source.getServer() == null || owner == null || data == null || data.uuid == null) {
            return UnloadedTpResult.fail("invalid context");
        }
        if (data.dead || isDeadEntry(data.uuid)) {
            return UnloadedTpResult.fail("tame is dead");
        }
        if (target == null || target.level == null || target.pos == null) {
            return UnloadedTpResult.fail("invalid target");
        }
        if (isLoadedAnywhere(source.getServer(), data.uuid)) {
            return UnloadedTpResult.fail("already loaded");
        }
        if (data.lastKnownDimension == null || data.lastKnownDimension.isBlank()) {
            if (liveEntityOnly) {
                return UnloadedTpResult.fail("missing last known dimension");
            }
            RecoverResult recoverResult = recoverPetEntityAtLocation(owner, target, data);
            return recoverResult.entity != null ? UnloadedTpResult.queued() : UnloadedTpResult.fail("missing last known dimension");
        }
        ResourceLocation lastKnown = ResourceLocation.tryParse(data.lastKnownDimension);
        if (lastKnown == null) {
            if (liveEntityOnly) {
                return UnloadedTpResult.fail("invalid last known dimension");
            }
            RecoverResult recoverResult = recoverPetEntityAtLocation(owner, target, data);
            return recoverResult.entity != null ? UnloadedTpResult.queued() : UnloadedTpResult.fail("invalid last known dimension");
        }
        ServerLevel sourceLevel = source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, lastKnown));
        if (sourceLevel == null) {
            if (liveEntityOnly) {
                return UnloadedTpResult.fail("source level unavailable");
            }
            RecoverResult recoverResult = recoverPetEntityAtLocation(owner, target, data);
            return recoverResult.entity != null ? UnloadedTpResult.queued() : UnloadedTpResult.fail("source level unavailable");
        }

        BlockPos sourcePos = new BlockPos(data.lastKnownX, data.lastKnownY, data.lastKnownZ);
        ChunkPos sourceChunk = new ChunkPos(sourcePos);
        UUID ticketId = data.tlId != null ? data.tlId : data.uuid;
        loadChunksAround(sourceLevel, ticketId, sourcePos, true);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                sourceLevel.getChunk(sourceChunk.x + dx, sourceChunk.z + dz);
            }
        }
        TamableAnimal tame = findLoadedTameByIdentity(sourceLevel, data.uuid, data.tlId);
        if (tame != null && tame.isAlive()) {
            try {
                teleportTameToLocation(tame, target);
                return UnloadedTpResult.queued();
            } finally {
                loadChunksAround(sourceLevel, ticketId, sourcePos, false);
            }
        }
        long now = source.getServer().overworld() == null ? 0L : source.getServer().overworld().getGameTime();
        PendingImmediateChunkTeleport pending = new PendingImmediateChunkTeleport(
                ticketId,
                data.uuid,
                data.tlId,
                data.ownerUUID,
                sourceLevel.dimension(),
                sourcePos,
                target,
                now,
                now + IMMEDIATE_CHUNK_TP_INITIAL_DELAY_TICKS,
                -1L,
                data.name,
                liveEntityOnly,
                true
        );
        PENDING_IMMEDIATE_CHUNK_TELEPORTS.put(ticketId, pending);
        return UnloadedTpResult.queued();
    }

    private static void clearPendingImmediateChunkTeleport(TameData data) {
        if (data == null) {
            return;
        }
        UUID key = data.tlId != null ? data.tlId : data.uuid;
        if (key != null) {
            PENDING_IMMEDIATE_CHUNK_TELEPORTS.remove(key);
        }
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
    private static int teleportHomeBatch(CommandSourceStack source, ServerPlayer player, List<TameData> requested, String label, int deadSkipped) {
        List<TamableAnimal> loadedTargets = new ArrayList<>();
        List<SpawnTarget> loadedDestinations = new ArrayList<>();
        List<TameData> unloadedTargets = new ArrayList<>();
        List<SpawnTarget> unloadedDestinations = new ArrayList<>();
        int queued = 0;
        int queueFailed = 0;
        List<String> failedNames = new ArrayList<>();

        for (TameData data : requested) {
            if (data == null || data.uuid == null) {
                continue;
            }
            if (data.dead) {
                continue;
            }
            SpawnTarget target = resolveRespawnTarget(source, player, data, false);
            if (target == null || target.level == null || target.pos == null) {
                queueFailed++;
                failedNames.add((data.name == null ? "unknown" : data.name) + " (invalid respawn home)");
                continue;
            }
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, player.getUUID(), data.uuid);
            if (tame == null) {
                String queueError = validateUnloadedHomeTeleport(source, player, data, target);
                if (queueError != null) {
                    queueFailed++;
                    failedNames.add((data.name == null ? "unknown" : data.name) + " (" + queueError + ")");
                    continue;
                }
                unloadedTargets.add(data);
                unloadedDestinations.add(target);
                continue;
            }
            loadedTargets.add(tame);
            loadedDestinations.add(target);
        }
        for (int i = 0; i < loadedTargets.size(); i++) {
            teleportTameToLocation(loadedTargets.get(i), loadedDestinations.get(i));
        }
        for (int i = 0; i < unloadedTargets.size(); i++) {
            UnloadedTpResult unloaded = tpUnloadedHomeViaLanternOrRecover(source, player, unloadedTargets.get(i), unloadedDestinations.get(i));
            if (unloaded.success) {
                queued++;
            } else {
                queueFailed++;
                failedNames.add((unloadedTargets.get(i).name == null ? "unknown" : unloadedTargets.get(i).name) + " (" + unloaded.error + ")");
            }
        }
        sendTeleportSummary(player, label, loadedTargets.size(), queued, deadSkipped, queueFailed, 0, 0);
        if (!failedNames.isEmpty()) {
            player.sendSystemMessage(Component.literal("TPHome failures: " + String.join("; ", failedNames)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    //tleeports
    //tp
    private static int teleportByMovementState(CommandSourceStack source, MovementOrder order) {
        ServerPlayer p = source.getPlayer();
        List<TameData> requested = ownedTames(p.getUUID());
        List<TamableAnimal> loadedTargets = new ArrayList<>();
        List<TameData> unloadedTargets = new ArrayList<>();
        int cost = 0;
        int failed = 0;
        int queued = 0;
        int crossDimension = 0;
        List<String> unaffordable = new ArrayList<>();
        List<String> failedNames = new ArrayList<>();
        int xpBudget = currentXpPoints(p);
        for (TameData d : requested) {
            if (d == null || d.dead) {
                continue;
            }
            TamableAnimal ta = findLoadedOwnedTameByUuid(source, p.getUUID(), d.uuid);
            if (ta == null) {
                if (!matchesMovementOrderSnapshot(d, order)) {
                    continue;
                }
                String queueError = validateUnloadedTeleportForPlayer(source, p, d);
                if (queueError != null) {
                    failed++;
                    failedNames.add(tameDisplayName(d) + " (" + queueError + ")");
                    continue;
                }
                boolean cross = isCrossDimension(d, p);
                int tameCost = teleportCostFor(d, cross);
                if (tameCost > xpBudget) {
                    unaffordable.add(pricedTameLabel(d, teleportPriceLabel(tameCost)));
                    continue;
                }
                unloadedTargets.add(d);
                xpBudget -= tameCost;
                cost += tameCost;
                if (cross) crossDimension++;
                continue;
            }
            if (!matchesMovementOrder(ta, order)) continue;
            boolean cross = isCrossDimension(ta, p);
            int tameCost = teleportCostFor(d, cross);
            if (tameCost > xpBudget) {
                unaffordable.add(pricedTameLabel(d, teleportPriceLabel(tameCost)));
                continue;
            }
            loadedTargets.add(ta);
            xpBudget -= tameCost;
            cost += tameCost;
            if (cross) crossDimension++;
        }
        if (!payTeleportXp(p, cost)) return 0;
        for (TamableAnimal ta : loadedTargets) teleportTameToPlayer(ta, p);
        for (TameData data : unloadedTargets) {
            UnloadedTpResult unloaded = tpUnloadedViaLanternOrRecover(source, p, data);
            if (unloaded.success) {
                queued++;
            } else {
                failed++;
                failedNames.add(tameDisplayName(data) + " (" + unloaded.error + ")");
            }
        }
        sendTeleportSummary(p, "TP " + movementLabel(order), loadedTargets.size(), queued, 0, failed, cost, crossDimension);
        sendAffordabilityFailures(p, "Could not afford tp for", unaffordable);
        if (!failedNames.isEmpty()) {
            p.sendSystemMessage(Component.literal("TP " + movementLabel(order) + " failures: " + String.join("; ", failedNames)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int teleportByMovementStateHome(CommandSourceStack source, MovementOrder order) {
        ServerPlayer player = source.getPlayer();
        List<TameData> requested = new ArrayList<>();
        int skipped = 0;
        for (TameData data : ownedTames(player.getUUID())) {
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, player.getUUID(), data.uuid);
            if (tame == null) {
                skipped++;
                continue;
            }
            if (matchesMovementOrder(tame, order)) {
                requested.add(data);
            }
        }
        int result = teleportHomeBatch(source, player, requested, "TPHome " + movementLabel(order), 0);
        if (skipped > 0) {
            player.sendSystemMessage(Component.literal("Skipped " + skipped + " unloaded tames for tphome " + movementLabel(order) + ".").withStyle(ChatFormatting.GRAY));
        }
        return result;
    }

    private static int teleportByState(CommandSourceStack source, String stateName) {
        ServerPlayer p = source.getPlayer();
        MovementOrder order = parseMovementOrder(stateName);
        if (order == null) return error(p, "Invalid state. Use follow, wander, or sit.");
        return teleportByMovementState(source, order);
    }

    private static int teleportByStateHome(CommandSourceStack source, String stateName) {
        ServerPlayer player = source.getPlayer();
        MovementOrder order = parseMovementOrder(stateName);
        if (order == null) return error(player, "Invalid state. Use follow, wander, or sit.");
        return teleportByMovementStateHome(source, order);
    }

    private static boolean payTeleportXp(ServerPlayer player, int cost) {
        cost = Math.max(0, cost);
        if (cost <= 0) return true;
        int currentXp = currentXpPoints(player);
        if (currentXp < cost) {
            player.sendSystemMessage(Component.literal("Not enough XP points. Required: " + cost + ", you have: " + currentXp + ".").withStyle(ChatFormatting.RED));
            return false;
        }
        player.giveExperiencePoints(-cost);
        return true;
    }

    private static int teleportCostFor(TameData data, boolean crossDimension) {
        int base = Math.max(1, data == null ? 1 : data.level);
        return crossDimension ? (base * 2) : base;
    }

    private static int teleportGuardianCostFor(TameData data, boolean crossDimension) {
        int normal = teleportCostFor(data, crossDimension);
        return Math.max(1, (int) Math.ceil(normal / 4.0D));
    }

    private static String validateGuardianTeleport(CommandSourceStack source, ServerPlayer owner, TameData data) {
        if (source == null || owner == null || data == null) {
            return "invalid context";
        }
        if (!data.hasHome) {
            return "no current guardian location";
        }
        String validationError = validateUnloadedHomeTeleport(
                source,
                owner,
                data,
                new SpawnTarget(owner.serverLevel(), owner.position(), owner.getYRot(), owner.getXRot())
        );
        if (validationError != null && (data.entitySnapshot == null || data.entitySnapshot.isEmpty())) {
            return validationError;
        }
        return null;
    }

    private static boolean isCrossDimension(TamableAnimal tame, ServerPlayer player) {
        return tame != null && player != null && !tame.level().dimension().equals(player.level().dimension());
    }

    private static boolean isCrossDimension(TameData data, ServerPlayer player) {
        if (data == null || player == null || data.lastKnownDimension == null || data.lastKnownDimension.isBlank()) {
            return false;
        }
        ResourceLocation lastKnown = ResourceLocation.tryParse(data.lastKnownDimension);
        return lastKnown != null && !player.level().dimension().location().equals(lastKnown);
    }

    private static boolean isCrossDimension(TameData data, ServerLevel level) {
        if (data == null || level == null || data.lastKnownDimension == null || data.lastKnownDimension.isBlank()) {
            return false;
        }
        ResourceLocation lastKnown = ResourceLocation.tryParse(data.lastKnownDimension);
        return lastKnown != null && !level.dimension().location().equals(lastKnown);
    }

    private static String validateUnloadedTeleportForPlayer(CommandSourceStack source, ServerPlayer owner, TameData data) {
        if (source == null || owner == null || data == null) {
            return "invalid context";
        }
        SpawnTarget target = new SpawnTarget(owner.serverLevel(), owner.position(), owner.getYRot(), owner.getXRot());
        String validationError = validateUnloadedHomeTeleport(source, owner, data, target);
        if (validationError != null && (data.entitySnapshot == null || data.entitySnapshot.isEmpty())) {
            return validationError;
        }
        return null;
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
        clearGuardianAnchor(data);
        TameTransferService.TransferResult result = TameTransferService.transferToPlayer(tame, player, data);
        if (!result.success()) {
            System.err.println("[TamesLevel] Command teleport failed for tame " + tame.getUUID() + ": " + result.error());
        }
    }

    public static boolean autoFollowTeleportLoadedToOwner(TamableAnimal tame, ServerPlayer player) {
        if (tame == null || player == null || !tame.isAlive()) {
            return false;
        }
        teleportTameToPlayer(tame, player);
        return true;
    }

    public static boolean autoFollowTeleportLoadedToLocation(TamableAnimal tame, ServerLevel level, Vec3 pos, float yRot, float xRot) {
        if (tame == null || level == null || pos == null || !tame.isAlive()) {
            return false;
        }
        teleportTameToLocation(tame, new SpawnTarget(level, pos, yRot, xRot));
        return true;
    }

    public static boolean autoFollowQueueCrossDimensionLiveTeleport(TamableAnimal tame, TameData data, ServerLevel level, Vec3 pos, float yRot, float xRot) {
        if (tame == null || data == null || level == null || pos == null || !tame.isAlive() || !(tame.level() instanceof ServerLevel sourceLevel)) {
            return false;
        }
        SpawnTarget target = new SpawnTarget(level, pos, yRot, xRot);
        UnloadedTpResult result = queueImmediateChunkTeleport(sourceLevel, tame.blockPosition(), data, target, true, true);
        return result.success;
    }

    private static void teleportTameToLocation(TamableAnimal tame, SpawnTarget target) {
        if (tame == null || target == null || target.level == null || target.pos == null) return;
        TameData data = TameRegistry.get(tame.getUUID());
        clearGuardianAnchor(data);
        TameTransferService.TransferResult result = TameTransferService.transferToLocation(
                tame,
                target.level,
                target.pos.x,
                target.pos.y,
                target.pos.z,
                target.yRot,
                target.xRot,
                data
        );
        if (!result.success()) {
            System.err.println("[TamesLevel] Command tphome failed for tame " + tame.getUUID() + ": " + result.error());
        }
    }

    public static boolean autoFollowTeleportUnloadedToOwner(ServerPlayer owner, TameData data) {
        if (owner == null || data == null || data.uuid == null || data.dead) {
            return false;
        }
        if (data.lastKnownDimension != null && !data.lastKnownDimension.isBlank()) {
            ResourceLocation lastKnown = ResourceLocation.tryParse(data.lastKnownDimension);
            if (lastKnown != null && !owner.serverLevel().dimension().location().equals(lastKnown)) {
                return false;
            }
        }
        SpawnTarget target = new SpawnTarget(owner.serverLevel(), owner.position(), owner.getYRot(), owner.getXRot());
        UnloadedTpResult result = tryImmediateChunkLoadTeleportSilent(owner.createCommandSourceStack(), owner, data, target);
        return result.success;
    }

    public static boolean autoFollowTeleportUnloadedToLocation(ServerPlayer owner, TameData data, ServerLevel level, Vec3 pos, float yRot, float xRot) {
        if (owner == null || data == null || data.uuid == null || data.dead || level == null || pos == null) {
            return false;
        }
        SpawnTarget target = new SpawnTarget(level, pos, yRot, xRot);
        boolean crossDimension = isCrossDimension(data, level);
        if (!crossDimension && data.lastKnownDimension != null && !data.lastKnownDimension.isBlank()) {
            ResourceLocation lastKnown = ResourceLocation.tryParse(data.lastKnownDimension);
            if (lastKnown != null && !level.dimension().location().equals(lastKnown)) {
                return false;
            }
        }
        UnloadedTpResult result = tryImmediateChunkLoadTeleportSilent(owner.createCommandSourceStack(), owner, data, target, crossDimension);
        return result.success;
    }

    public static boolean autoFollowTeleportViaTpPath(ServerPlayer owner, TameData data, ServerLevel level, Vec3 pos, float yRot, float xRot) {
        if (owner == null || data == null || data.uuid == null || data.dead || level == null || pos == null) {
            return false;
        }
        if (TameDuelManager.isTameInDuel(data.uuid)) {
            return false;
        }
        SpawnTarget target = new SpawnTarget(level, pos, yRot, xRot);
        TamableAnimal loaded = owner.getServer() == null ? null : findLoadedTameByIdentity(owner.getServer(), data.uuid, data.tlId);
        if (loaded != null && loaded.isAlive()) {
            teleportTameToLocation(loaded, target);
            return true;
        }
        UnloadedTpResult result = tpUnloadedHomeViaLanternOrRecover(owner.createCommandSourceStack(), owner, data, target);
        return result.success;
    }

    public static boolean hasPendingImmediateChunkTeleport(TameData data) {
        if (data == null) {
            return false;
        }
        UUID key = data.tlId != null ? data.tlId : data.uuid;
        return key != null && PENDING_IMMEDIATE_CHUNK_TELEPORTS.containsKey(key);
    }

    private static int leaderboard(CommandSourceStack source, String mode, boolean includeAll, String groupFilter, String typeFilter, int requestedLimit) {
        ServerPlayer p = source.getPlayer();
        String m = mode == null ? "mix" : mode.trim().toLowerCase(Locale.ROOT);
        List<TameData> entries = new ArrayList<>();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!includeAll && !p.getUUID().equals(d.ownerUUID)) continue;
            if (groupFilter != null && !isInGroup(d, groupFilter)) continue;
            if (typeFilter != null && !matchesTypeFilter(d, typeFilter)) continue;
            if (leaderboardInvestedXp(d) <= 0) continue;
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
        String typeText = (typeFilter == null || typeFilter.isBlank()) ? "" : (" | type: " + normalizeTypeFilter(typeFilter));
        p.sendSystemMessage(Component.literal("---- Leaderboard (" + m + ") | " + scope + groupText + typeText + " | showing " + limit + "/" + entries.size() + " ----").withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.literal("score / kills / assists / deaths / days").withStyle(ChatFormatting.DARK_GRAY));
        for (int i = 0; i < limit; i++) {
            TameData d = entries.get(i);
            int score = weightedCombatScore(d);
            p.sendSystemMessage(Component.literal((i + 1) + ". ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("(" + ownerInitials(source.getServer(), d.ownerUUID) + ") ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("[" + d.level + "] ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(d.name + " ").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("(").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(String.valueOf(score)).withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(String.valueOf(d.kills)).withStyle(ChatFormatting.RED))
                    .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(String.valueOf(d.assists)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(String.valueOf(d.deaths)).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(String.valueOf(daysAlive(source, d))).withStyle(ChatFormatting.DARK_AQUA))
                    .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY)));
        }
        return 1;
    }

    private static int leaderboardInvestedXp(TameData data) {
        return data == null ? 0 : Math.max(0, LevelSystem.estimateInvestedXp(data));
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

    private static int adminSetDebugTeleport(CommandSourceStack source, boolean enabled) {
        ServerPlayer p = source.getPlayer();
        PlayerDebugSettings.setTeleport(p.getUUID(), enabled);
        p.sendSystemMessage(Component.literal("Admin debug teleport set to " + enabled + ".").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static void debugTeleport(ServerPlayer player, String message) {
        if (player == null || !PlayerDebugSettings.teleport(player.getUUID())) {
            return;
        }
        player.sendSystemMessage(Component.literal("TPDBG " + message).withStyle(ChatFormatting.YELLOW));
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
        boolean teleport = PlayerDebugSettings.teleport(p.getUUID());
        boolean perf = TamePerformanceProfiler.isEnabled();
        p.sendSystemMessage(Component.literal("Admin Debug -> abilityUsed: " + ability + ", damageDealt: " + damage + ", teleport: " + teleport + ", perf: " + perf).withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int adminPerfStatus(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        p.sendSystemMessage(Component.literal("Perf profiler is " + (TamePerformanceProfiler.isEnabled() ? "running" : "stopped") + ".").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int adminPerfStart(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        TamePerformanceProfiler.start();
        p.sendSystemMessage(Component.literal("Started tame performance profiler.").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int adminPerfStop(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        TamePerformanceProfiler.stop();
        p.sendSystemMessage(Component.literal("Stopped tame performance profiler.").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int adminPerfReset(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        TamePerformanceProfiler.reset();
        p.sendSystemMessage(Component.literal("Reset tame performance profiler data.").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int adminPerfReport(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        try {
            java.nio.file.Path file = TamePerformanceProfiler.writeReport();
            p.sendSystemMessage(Component.literal("Wrote tame performance report to " + file + ".").withStyle(ChatFormatting.YELLOW));
            return 1;
        } catch (java.io.IOException e) {
            p.sendSystemMessage(Component.literal("Failed to write tame performance report: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
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
        TameClass tameClass = TameClass.parse(className);
        if (tameClass == null) {
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

        source.sendSuccess(() -> Component.literal("Set class of " + data.name + " to " + tameClass.id() + "."), true);
        return 1;
    }

    private static int adminRebuildPet(CommandSourceStack source, String petName, String className) {
        ServerPlayer player = source.getPlayer();
        TameClass tameClass = TameClass.parse(className);
        if (tameClass == null) {
            return error(player, "Unknown class. Use one of: " + String.join(", ", classNames()));
        }

        TameData data = resolveAdminAliveTame(player, petName);
        if (data == null) {
            return 0;
        }
        Entity entity = findLoadedTameByUuid(source, data.uuid);
        if (!(entity instanceof TamableAnimal tame) || !tame.isTame() || !tame.isAlive()) {
            return error(player, "Pet is not loaded.");
        }

        int previousLevel = Math.max(1, data.level);
        int previousKills = data.kills;
        int previousAssists = data.assists;
        int targetXp = totalXpToReachLevel(previousLevel);

        LevelSystem.resetProgress(tame, data);
        clearSavedProgress(data);
        data.levelRewardHistory.clear();
        data.tameClass = tameClass;
        data.kills = previousKills;
        data.assists = previousAssists;
        data.xp = 0;
        data.xpToNext = LevelSystem.xpRequiredForLevel(1);
        data.cooldowns.clear();
        TameRegistry.markDirty();

        if (targetXp > 0) {
            LevelSystem.grantXP(tame, data, targetXp);
        } else {
            LevelSystem.updateTameName(tame, data);
            tame.setHealth(tame.getMaxHealth());
            TameRegistry.markDirty();
        }

        int rebuiltLevel = data.level;
        source.sendSuccess(() -> Component.literal(
                "Rebuilt " + data.name + " as " + tameClass.id() + " and rerolled progression back to level " + rebuiltLevel + "."
        ), true);
        return 1;
    }

    private static int totalXpToReachLevel(int targetLevel) {
        int total = 0;
        for (int level = 1; level < Math.max(1, targetLevel); level++) {
            total += LevelSystem.xpRequiredForLevel(level);
        }
        return total;
    }

    private static void clearSavedProgress(TameData data) {
        if (data == null) {
            return;
        }
        data.hasSavedProgress = false;
        data.savedProgressCost = 0;
        data.savedLevel = 1;
        data.savedXp = 0;
        data.savedXpToNext = LevelSystem.xpRequiredForLevel(1);
        data.savedKills = 0;
        data.savedAssists = 0;
        data.savedBonusHealth = 0.0D;
        data.savedBonusDamage = 0.0D;
        data.savedBonusSpeed = 0.0D;
        data.savedBonusArmor = 0.0D;
        data.savedBonusArmorToughness = 0.0D;
        data.savedBonusKnockback = 0.0D;
        data.savedBonusKnockbackResist = 0.0D;
        data.savedAbilities.clear();
        data.savedAbilityLevels.clear();
        data.savedAttributeLevels.clear();
    }

    private static int adminMigrateProtectorToSupporter(CommandSourceStack source) {
        int affected = 0;
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.tameClass != TameClass.SUPPORTER) {
                continue;
            }
            data.tameClass = TameClass.SUPPORTER;
            affected++;
        }
        if (affected > 0) {
            TameRegistry.markDirty();
        }
        final int migrated = affected;
        source.sendSuccess(() -> Component.literal("Protector -> supporter migration applied to " + migrated + " tame entries."), true);
        return 1;
    }

    private static int adminReloadClassWeights(CommandSourceStack source) {
        try {
            LevelSystem.reloadClassWeightConfig();
            source.sendSuccess(() -> Component.literal(
                    "Reloaded class weights from " + LevelSystem.classWeightConfigResourcePath() + "."
            ), true);
            return 1;
        } catch (RuntimeException ex) {
            source.sendFailure(Component.literal("Failed to reload class weights: " + ex.getMessage()));
            return 0;
        }
    }

    private static int respawnOrderStatus(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        source.sendSuccess(() -> Component.literal(
                "Your morning respawn order is " + respawnOrderLabel(player.getUUID()) + "."
        ), false);
        return 1;
    }

    private static int respawnOrderInfo(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        player.sendSystemMessage(Component.literal("Respawn order modes:").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("default: first dead tame in your registry queue respawns first.").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("level: highest-level dead tame respawns first.").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("leaderboard: highest combat-score dead tame respawns first.").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int setRespawnOrder(CommandSourceStack source, String rawMode) {
        ServerPlayer player = source.getPlayer();
        RespawnOrderMode mode = RespawnOrderMode.parse(rawMode);
        TameRegistry.setRespawnOrder(player.getUUID(), mode.id);
        source.sendSuccess(() -> Component.literal(
                "Your morning respawn order is now " + mode.id + "."
        ), true);
        return 1;
    }

    private static int adminNormalizeXpCurve(CommandSourceStack source) {
        int changed = 0;
        for (TameData data : TameRegistry.TAMES.values()) {
            if (LevelSystem.normalizeXpForCurrentLevel(data)) {
                changed++;
            }
        }
        if (changed > 0) {
            TameRegistry.markDirty();
        }
        final int updated = changed;
        source.sendSuccess(() -> Component.literal(
                "Normalized XP progress to the current level curve for " + updated + " tame entries."
        ), true);
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
        String normalizedKeeperName = stripLevelPrefixes(keeper.name);
        if (normalizedKeeperName.isBlank()) {
            normalizedKeeperName = stripLevelPrefixes(duplicate.name);
        }
        if (!normalizedKeeperName.isBlank()) {
            keeper.name = normalizedKeeperName;
        }
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
                forceRemoveLoadedTame(loaded);
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

    private static int adminSetHealthSiphon(CommandSourceStack source, boolean enabled) {
        TLAdminRuntimeSettings.setHealthSiphonEnabled(enabled);
        source.sendSuccess(() -> Component.literal(
                "Temporary admin setting: health siphon is now " + (enabled ? "ENABLED" : "DISABLED") + "."
        ).withStyle(enabled ? ChatFormatting.YELLOW : ChatFormatting.GREEN), true);
        return 1;
    }

    private static int adminPostTpStabilizationStatus(CommandSourceStack source) {
        boolean enabled = TLAdminRuntimeSettings.postTpStabilizationEnabled();
        source.sendSuccess(() -> Component.literal(
                "Post-teleport stabilization is currently " + (enabled ? "ENABLED" : "DISABLED") + "."
        ).withStyle(enabled ? ChatFormatting.YELLOW : ChatFormatting.GREEN), false);
        return 1;
    }

    private static int adminSetPostTpStabilization(CommandSourceStack source, boolean enabled) {
        TLAdminRuntimeSettings.setPostTpStabilizationEnabled(enabled);
        source.sendSuccess(() -> Component.literal(
                "Temporary admin setting: post-teleport stabilization is now " + (enabled ? "ENABLED" : "DISABLED") + "."
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

    private static String adminPlayerReviveLabel(String ownerName, ReviveMode mode, String targetLabel) {
        String action = mode == ReviveMode.ARISE ? "Admin arose" : "Admin respawned";
        return action + " " + targetLabel + " for " + ownerName;
    }

    private static void forceRemoveLoadedTame(TamableAnimal loaded) {
        if (loaded == null) {
            return;
        }
        loaded.setTarget(null);
        loaded.getNavigation().stop();
        loaded.remove(Entity.RemovalReason.DISCARDED);
        if (!loaded.isRemoved()) {
            loaded.discard();
        }
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
            forceRemoveLoadedTame(loaded);
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
        return adminRespawnResolvedEntry(source, data, deadRecord);
    }

    private static int adminRespawnAll(CommandSourceStack source) {
        List<AdminRespawnEntry> entries = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.uuid == null || !isDeadEntry(data.uuid)) {
                continue;
            }
            if (data.entitySnapshot == null || data.entitySnapshot.isEmpty()) {
                continue;
            }
            entries.add(new AdminRespawnEntry(data, null));
            seen.add(data.uuid);
        }
        for (TameDeathRecord record : TameRegistry.LAST_DEATHS.values()) {
            if (record == null || record.uuid == null || seen.contains(record.uuid)) {
                continue;
            }
            if (record.snapshot == null || record.snapshot.isEmpty()) {
                continue;
            }
            TameData data = TameData.fromTag(record.snapshot.copy());
            if (data == null || data.uuid == null) {
                continue;
            }
            if (data.name == null || data.name.isBlank()) {
                data.name = record.name == null ? data.uuid.toString() : record.name;
            }
            entries.add(new AdminRespawnEntry(data, record));
            seen.add(data.uuid);
        }
        if (entries.isEmpty()) {
            return error(source.getPlayer(), "No dead tames found.");
        }
        int success = 0;
        List<String> failed = new ArrayList<>();
        for (AdminRespawnEntry entry : entries) {
            int result = adminRespawnResolvedEntry(source, entry.data(), entry.deadRecord(), false, failed);
            if (result > 0) {
                success++;
            }
        }
        if (success <= 0) {
            return error(source.getPlayer(), "No tames respawned. Reasons: " + String.join("; ", failed));
        }
        final int successCount = success;
        source.sendSuccess(() -> Component.literal("Respawned " + successCount + " dead tame(s) at your position.").withStyle(ChatFormatting.GREEN), true);
        if (!failed.isEmpty()) {
            source.sendFailure(Component.literal("Respawn failed for " + failed.size() + ": " + String.join("; ", failed)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int adminFixStaleAll(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        if (server == null) {
            return adminError(source, "Server not available.");
        }
        List<TamableAnimal> loaded = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal tame) || !tame.isTame() || !tame.isAlive()) {
                    continue;
                }
                if (!seen.add(tame.getUUID())) {
                    continue;
                }
                TameData data = TameRegistry.get(tame.getUUID());
                if (data == null || data.dead) {
                    continue;
                }
                loaded.add(tame);
            }
        }
        if (loaded.isEmpty()) {
            return adminError(source, "No loaded alive registered tames found.");
        }
        int success = 0;
        List<String> failed = new ArrayList<>();
        for (TamableAnimal tame : loaded) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null) {
                failed.add("unknown (missing registry data)");
                continue;
            }
            if (adminFixStaleLoadedTame(source, tame, data, failed)) {
                success++;
            }
        }
        if (success <= 0) {
            return adminError(source, "No loaded tames were rebuilt. Reasons: " + String.join("; ", failed));
        }
        final int successCount = success;
        source.sendSuccess(() -> Component.literal("Rebuilt " + successCount + " loaded tame(s) in place.").withStyle(ChatFormatting.GREEN), true);
        if (!failed.isEmpty()) {
            source.sendFailure(Component.literal("FixStale failed for " + failed.size() + ": " + String.join("; ", failed)).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static boolean adminFixStaleLoadedTame(CommandSourceStack source, TamableAnimal tame, TameData data, List<String> failures) {
        if (tame == null || data == null || data.uuid == null) {
            failures.add("unknown (invalid loaded tame)");
            return false;
        }
        String typeId = recoverEntityTypeId(data);
        if (typeId.isBlank()) {
            failures.add(tameDisplayName(data) + " (missing saved entity type)");
            return false;
        }
        ResourceLocation id = ResourceLocation.tryParse(typeId);
        if (id == null) {
            failures.add(tameDisplayName(data) + " (invalid entity type '" + typeId + "')");
            return false;
        }
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (entityType == null) {
            failures.add(tameDisplayName(data) + " (unknown entity type '" + typeId + "')");
            return false;
        }
        if (!rebuildLoadedTameFromSnapshot(tame, data, false)) {
            failures.add(tameDisplayName(data) + " (spawn failed after rebuild)");
            return false;
        }
        return true;
    }

    private static boolean rebuildLoadedTameFromSnapshot(TamableAnimal tame, TameData data, boolean markJoinSkip) {
        if (tame == null || data == null || data.uuid == null || !(tame.level() instanceof ServerLevel level)) {
            return false;
        }
        String typeId = recoverEntityTypeId(data);
        if (typeId.isBlank()) {
            return false;
        }
        ResourceLocation id = ResourceLocation.tryParse(typeId);
        if (id == null) {
            return false;
        }
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (entityType == null) {
            return false;
        }
        Vec3 pos = tame.position();
        float yRot = tame.getYRot();
        float xRot = tame.getXRot();
        CompoundTag snapshot = data.entitySnapshot == null ? new CompoundTag() : data.entitySnapshot.copy();
        Entity created = entityType.create(level);
        if (!(created instanceof TamableAnimal rebuilt)) {
            return false;
        }
        if (!snapshot.isEmpty()) {
            rebuilt.load(snapshot);
        }
        rebuilt.setUUID(data.uuid);
        rebuilt.moveTo(pos.x, pos.y, pos.z, yRot, xRot);
        rebuilt.setDeltaMovement(0.0D, 0.0D, 0.0D);
        enforceTamedOwnerPreserveCollar(rebuilt, data.ownerUUID);
        if (markJoinSkip) {
            rebuilt.getPersistentData().putBoolean(JOIN_FIX_STALE_SKIP_TAG, true);
        }
        tame.discard();
        if (!level.addFreshEntity(rebuilt)) {
            return false;
        }
        boolean normalized = applyTypeBasePlusBonus(rebuilt, data);
        if (!normalized) {
            LevelSystem.updateTameName(rebuilt, data);
            rebuilt.setHealth(rebuilt.getMaxHealth());
        }
        finalizeRespawnState(rebuilt, data);
        TameRegistry.markDirty();
        return true;
    }

    private static int adminRespawnResolvedEntry(CommandSourceStack source, TameData data, TameDeathRecord deadRecord) {
        return adminRespawnResolvedEntry(source, data, deadRecord, true, null);
    }

    private static int adminRespawnResolvedEntry(CommandSourceStack source, TameData data, TameDeathRecord deadRecord, boolean announceSingle, List<String> failures) {
        if (data == null || data.uuid == null) {
            if (failures != null) {
                failures.add("unknown (invalid registry entry)");
                return 0;
            }
            return error(source.getPlayer(), "Cannot respawn this tame: invalid registry entry.");
        }
        TamableAnimal loaded = findLoadedTameByUuid(source, data.uuid);
        if (loaded != null) {
            loaded.discard();
        }
        if (data.stored) {
            data.stored = false;
            TameRegistry.markDirty();
        }

        String typeId = recoverEntityTypeId(data);
        if (typeId.isBlank()) {
            if (failures != null) {
                failures.add(data.name + " (missing saved entity type)");
                return 0;
            }
            return error(source.getPlayer(), "Cannot respawn this tame: missing saved entity type.");
        }

        ResourceLocation id = ResourceLocation.tryParse(typeId);
        if (id == null) {
            if (failures != null) {
                failures.add(data.name + " (invalid entity type '" + typeId + "')");
                return 0;
            }
            return error(source.getPlayer(), "Cannot respawn this tame: invalid entity type '" + typeId + "'.");
        }

        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (entityType == null) {
            if (failures != null) {
                failures.add(data.name + " (unknown entity type '" + typeId + "')");
                return 0;
            }
            return error(source.getPlayer(), "Cannot respawn this tame: unknown entity type '" + typeId + "'.");
        }

        ServerLevel level = source.getLevel();
        Entity spawned = entityType.create(level);
        if (!(spawned instanceof TamableAnimal respawned)) {
            if (failures != null) {
                failures.add(data.name + " (stored type is not tamable)");
                return 0;
            }
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
            if (failures != null) {
                failures.add(data.name + " (UUID conflict or invalid state)");
                return 0;
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
        if (announceSingle) {
            if (normalized) {
                source.sendSuccess(() -> Component.literal("Respawned " + data.name + " at your position and normalized bonuses."), true);
            } else {
                source.sendSuccess(() -> Component.literal("Respawned " + data.name + " at your position (normalize failed: type template unavailable)."), true);
            }
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

    private static int listApprovedReincarnationItems(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        List<String> approved = new ArrayList<>(TameRegistry.APPROVED_REINCARNATE_ITEMS);
        approved.sort(String::compareToIgnoreCase);
        if (approved.isEmpty()) {
            return error(player, "No approved reincarnation items are configured.");
        }
        player.sendSystemMessage(Component.literal("Approved reincarnation items (" + approved.size() + "):").withStyle(ChatFormatting.GOLD));
        for (String id : approved) {
            String label = approvedItemDisplayLabel(id);
            player.sendSystemMessage(Component.literal("- " + label + " [" + id + "]").withStyle(ChatFormatting.GRAY));
        }
        return approved.size();
    }

    private static int adminListApprovedItems(CommandSourceStack source) {
        return listApprovedReincarnationItems(source);
    }

    private static int adminRemoveApprovedItem(CommandSourceStack source, String rawItemId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        String itemId = normalizeItemId(rawItemId);
        if (itemId.isBlank()) {
            return error(player, "Provide an item id to remove.");
        }
        boolean removed = TameRegistry.APPROVED_REINCARNATE_ITEMS.remove(itemId);
        if (!removed) {
            return error(player, "Item is not approved: " + itemId);
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Removed approved reincarnation item: " + itemId).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestApprovedReincarnationItems(SuggestionsBuilder builder) {
        List<String> approved = new ArrayList<>(TameRegistry.APPROVED_REINCARNATE_ITEMS);
        approved.sort(String::compareToIgnoreCase);
        return SharedSuggestionProvider.suggest(approved, builder);
    }

    private static String normalizeItemId(String rawItemId) {
        if (rawItemId == null) {
            return "";
        }
        return rawItemId.trim().toLowerCase(Locale.ROOT);
    }

    private static String approvedItemDisplayLabel(String itemId) {
        ResourceLocation key = ResourceLocation.tryParse(itemId);
        if (key == null) {
            return itemId;
        }
        var item = ForgeRegistries.ITEMS.getValue(key);
        if (item == null) {
            return itemId;
        }
        return item.getDefaultInstance().getHoverName().getString();
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

    public static void refreshRegistrySnapshotFor(TamableAnimal tame) {
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

    public static void fixStaleLoadedTameOnJoin(TamableAnimal tame) {
        if (tame == null || tame.level().isClientSide || !tame.isTame() || !tame.isAlive()) {
            return;
        }
        if (tame.getPersistentData().getBoolean(JOIN_FIX_STALE_SKIP_TAG)) {
            tame.getPersistentData().remove(JOIN_FIX_STALE_SKIP_TAG);
            return;
        }
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null && TameData.getTlId(tame) != null) {
            data = TameRegistry.getByTlId(TameData.getTlId(tame));
        }
        if (data == null || data.dead) {
            return;
        }
        rebuildLoadedTameFromSnapshot(tame, data, true);
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

    private static int adminRepairLoadedTames(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        if (server == null) {
            source.sendFailure(Component.literal("Server unavailable."));
            return 0;
        }

        int scanned = 0;
        int repaired = 0;
        int restored = 0;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal tame) || !tame.isTame() || !tame.isAlive()) {
                    continue;
                }
                scanned++;
                TameData data = TameRegistry.get(tame.getUUID());
                if (data == null && TameData.getTlId(tame) != null) {
                    data = TameRegistry.getByTlId(TameData.getTlId(tame));
                }
                if (data == null) {
                    data = TameSpawnEvents.registerOrRestoreTame(tame, false);
                    if (data != null) {
                        restored++;
                    }
                }
                if (data != null && repairLoadedTameState(tame, data)) {
                    repaired++;
                }
            }
        }

        if (repaired > 0 || restored > 0) {
            TameRegistry.markDirty();
        }
        final int totalScanned = scanned;
        final int totalRepaired = repaired;
        final int totalRestored = restored;
        source.sendSuccess(
                () -> Component.literal(
                        "Repair loaded: scanned " + totalScanned
                                + ", repaired " + totalRepaired
                                + ", restored registry " + totalRestored + "."
                ).withStyle(ChatFormatting.GREEN),
                true
        );
        return totalRepaired > 0 || totalRestored > 0 ? 1 : 0;
    }

    private static boolean repairLoadedTameState(TamableAnimal tame, TameData data) {
        if (tame == null || data == null) {
            return false;
        }
        CompoundTag beforeData = data.toTag();
        MovementOrder intendedOrder = resolveRepairMovementOrder(tame, data);
        CompoundTag before = new CompoundTag();
        tame.save(before);
        data.cooldowns.clear();
        tame.setNoAi(false);
        tame.setTarget(null);
        tame.getNavigation().stop();
        TameGoalInstaller.installIfMissing(tame);
        LevelSystem.ensureClassAssigned(tame, data, false);
        LevelSystem.reapplyTypeBasePlusBonuses(tame, data);
        applyMovementOverride(tame, intendedOrder);
        CompoundTag after = new CompoundTag();
        tame.save(after);
        CompoundTag afterData = data.toTag();
        return !after.equals(before) || !afterData.equals(beforeData);
    }

    private static MovementOrder resolveRepairMovementOrder(TamableAnimal tame, TameData data) {
        if (data != null) {
            CompoundTag snapshot = data.entitySnapshot;
            Integer command = findSnapshotCommand(snapshot);
            if (command != null) {
                return resolveMovementOrderFromSnapshotCommand(command, data.type, data.hasHome);
            }
            if (snapshot != null && !snapshot.isEmpty()) {
                if (snapshot.contains("Sitting", Tag.TAG_BYTE) && snapshot.getBoolean("Sitting")) {
                    return MovementOrder.SIT;
                }
                if (snapshot.contains("orderedToSit", Tag.TAG_BYTE) && snapshot.getBoolean("orderedToSit")) {
                    return MovementOrder.SIT;
                }
                if (snapshot.contains("OrderedToSit", Tag.TAG_BYTE) && snapshot.getBoolean("OrderedToSit")) {
                    return MovementOrder.SIT;
                }
            }
            if (data.hasHome) {
                return MovementOrder.GUARDIAN;
            }
        }
        if (tame != null && tame.isOrderedToSit()) {
            return MovementOrder.SIT;
        }
        return MovementOrder.FOLLOW;
    }

    private static int adminGrantMissingMilestoneAttributes(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        MinecraftServer server = source.getServer();
        if (player == null || server == null) {
            return 0;
        }

        int updatedEntries = 0;
        int refreshedLoaded = 0;
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null) {
                continue;
            }
            String granted = LevelSystem.grantMissingMilestoneAttributes(data);
            if (granted.isEmpty()) {
                continue;
            }
            updatedEntries++;
            TamableAnimal loaded = findLoadedTameByIdentity(server, data.uuid, data.tlId);
            if (loaded != null && loaded.isAlive()) {
                refreshRegistrySnapshotFor(loaded);
                refreshedLoaded++;
            }
        }

        if (updatedEntries <= 0) {
            player.sendSystemMessage(Component.literal("No tames were missing milestone attributes.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        TameRegistry.markDirty();
        player.sendSystemMessage(Component.literal("Granted missing milestone attributes to " + updatedEntries + " tame registry entries; refreshed " + refreshedLoaded + " loaded tames.").withStyle(ChatFormatting.GREEN));
        return 1;
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
        return adminRerollHalfDamageBonus(source, false);
    }

    private static int adminRerollHalfDamageBonusExcludeDps(CommandSourceStack source) {
        return adminRerollHalfDamageBonus(source, true);
    }

    private static int adminRerollHalfDamageBonus(CommandSourceStack source, boolean excludeDps) {
        ServerPlayer p = source.getPlayer();
        if (p == null || source.getServer() == null) {
            return 0;
        }

        int updatedEntries = 0;
        int rerolledDamagePoints = 0;
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null) continue;
            if (excludeDps && data.tameClass == TameClass.DPS) continue;
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
                if (excludeDps && data.tameClass == TameClass.DPS) continue;
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
            String scope = excludeDps ? " excluding DPS class" : "";
            p.sendSystemMessage(Component.literal("Rerolled " + rerolledDamagePoints + " damage bonus points across " + updatedEntries + " tame registry entries" + scope + "; refreshed " + appliedLoaded + " loaded tames.").withStyle(ChatFormatting.GREEN));
            return 1;
        }
        String emptyMessage = excludeDps
                ? "No non-DPS tame entries had enough damage bonus to reroll."
                : "No tame entries had enough damage bonus to reroll.";
        p.sendSystemMessage(Component.literal(emptyMessage).withStyle(ChatFormatting.YELLOW));
        return 0;
    }

    private static int adminConvertHealthToDamageExcludeDps(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        if (p == null || source.getServer() == null) {
            return 0;
        }

        int updatedEntries = 0;
        int movedHealthPoints = 0;
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.tameClass == TameClass.DPS) continue;
            int moved = LevelSystem.restoreHalfDamageBonusFromHealth(data);
            if (moved > 0) {
                updatedEntries++;
                movedHealthPoints += moved;
            }
        }

        int appliedLoaded = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal tame) || !tame.isTame()) continue;
                TameData data = TameRegistry.get(tame.getUUID());
                if (data == null || data.tameClass == TameClass.DPS) continue;
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
            p.sendSystemMessage(Component.literal("Restored " + movedHealthPoints + " damage bonus points by taking the same amount from bonus HP across " + updatedEntries + " non-DPS tame registry entries; refreshed " + appliedLoaded + " loaded tames. This is a compensation pass, not a perfect reroll undo.").withStyle(ChatFormatting.GREEN));
            return 1;
        }
        p.sendSystemMessage(Component.literal("No non-DPS tame entries had enough matching whole bonus HP and bonus damage to restore.").withStyle(ChatFormatting.YELLOW));
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

    private static int adminToggleCallOrderInvert(CommandSourceStack source, String typeId) {
        String normalized = typeId == null ? "" : typeId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            if (source.getPlayer() != null) {
                return error(source.getPlayer(), "Type id required.");
            }
            source.sendFailure(Component.literal("Type id required."));
            return 0;
        }
        boolean enabled = TameRegistry.toggleCallOrderInvertedType(normalized);
        Component line = Component.literal("Call-order invert override for " + normalized + " -> " + (enabled ? "enabled" : "disabled") + ".")
                .withStyle(enabled ? ChatFormatting.AQUA : ChatFormatting.YELLOW);
        if (source.getPlayer() != null) {
            source.getPlayer().sendSystemMessage(line);
        } else {
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int adminCallOrderInfo(CommandSourceStack source) {
        List<String> types = new ArrayList<>(TameRegistry.getCallOrderInvertedTypes());
        types.sort(String::compareToIgnoreCase);
        Component header = Component.literal("Call-order default: inverted generic mapping (0 wander, 1 follow, 2 sit).")
                .withStyle(ChatFormatting.AQUA);
        if (source.getPlayer() != null) {
            source.getPlayer().sendSystemMessage(header);
            if (types.isEmpty()) {
                source.getPlayer().sendSystemMessage(Component.literal("No per-type call-order overrides are currently enabled.")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                source.getPlayer().sendSystemMessage(Component.literal("Override types using old mapping (0 wander, 1 sit, 2 follow):")
                        .withStyle(ChatFormatting.GOLD));
                for (String type : types) {
                    source.getPlayer().sendSystemMessage(Component.literal("- " + type).withStyle(ChatFormatting.YELLOW));
                }
            }
        } else {
            source.sendSuccess(() -> header, false);
            if (types.isEmpty()) {
                source.sendSuccess(() -> Component.literal("No per-type call-order overrides are currently enabled."), false);
            } else {
                source.sendSuccess(() -> Component.literal("Override types using old mapping (0 wander, 1 sit, 2 follow): " + String.join(", ", types)), false);
            }
        }
        return 1;
    }

    private static int adminFixLoadedTameStatsAll(CommandSourceStack source) {
        int fixed = 0;
        int missingData = 0;
        int failed = 0;

        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal tame) || !tame.isTame()) {
                    continue;
                }
                TameData data = TameRegistry.get(tame.getUUID());
                if (data == null) {
                    data = TameRegistry.getByTlId(TameData.getTlId(tame));
                    if (data != null && !tame.getUUID().equals(data.uuid)) {
                        TameRegistry.rebindEntityUuid(data, tame.getUUID());
                    }
                }
                if (data == null) {
                    missingData++;
                    continue;
                }
                if (!fixLoadedTameStats(tame, data)) {
                    failed++;
                    continue;
                }
                fixed++;
            }
        }

        TameRegistry.markDirty();
        final int fixedCount = fixed;
        final int missingCount = missingData;
        final int failedCount = failed;
        source.sendSuccess(() -> Component.literal(
                "Fixed loaded tame stats: " + fixedCount + " fixed, " + missingCount + " missing registry data, " + failedCount + " failed."
        ), true);
        return fixedCount > 0 ? 1 : 0;
    }

    private static int adminFixLoadedTameStats(CommandSourceStack source, String petName) {
        List<TameData> matches = findAliveTamesByName(petName);
        if (matches.isEmpty()) {
            return error(source.getPlayer(), "No alive tame found with that name.");
        }
        if (matches.size() > 1) {
            return error(source.getPlayer(), "Ambiguous tame name (" + matches.size() + " matches). Rename duplicates first.");
        }

        TameData data = matches.get(0);
        TamableAnimal tame = findLoadedTameByUuid(source, data.uuid);
        if (tame == null && data.tlId != null) {
            for (ServerLevel level : source.getServer().getAllLevels()) {
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof TamableAnimal candidate && data.tlId.equals(TameData.getTlId(candidate))) {
                        tame = candidate;
                        if (!candidate.getUUID().equals(data.uuid)) {
                            TameRegistry.rebindEntityUuid(data, candidate.getUUID());
                        }
                        break;
                    }
                }
                if (tame != null) {
                    break;
                }
            }
        }
        if (tame == null) {
            return error(source.getPlayer(), "Target tame is not loaded.");
        }
        if (!fixLoadedTameStats(tame, data)) {
            return error(source.getPlayer(), "Could not fix loaded stats for this tame.");
        }

        TameRegistry.markDirty();
        source.sendSuccess(() -> Component.literal("Fixed loaded stats for " + data.name + "."), true);
        return 1;
    }

    private static int fixDragonflyArmor(CommandSourceStack source) {
        int touched = 0;
        int clearedStacks = 0;
        ResourceLocation dragonflyId = new ResourceLocation("crittersandcompanions", "dragonfly");
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                if (!dragonflyId.equals(typeId) || !(entity instanceof LivingEntity living)) {
                    continue;
                }
                int removedHere = 0;
                for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                    ItemStack stack = living.getItemBySlot(slot);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    living.setItemSlot(slot, ItemStack.EMPTY);
                    removedHere++;
                }
                if (removedHere > 0) {
                    touched++;
                    clearedStacks += removedHere;
                }
            }
        }
        final int fixedEntities = touched;
        final int fixedStacks = clearedStacks;
        source.sendSuccess(() -> Component.literal("Fixed dragonfly armor on " + fixedEntities + " loaded dragonflies and removed " + fixedStacks + " equipped armor stacks."), true);
        return fixedEntities > 0 ? fixedEntities : 1;
    }

    private static int rebuildDragonfliesFromData(CommandSourceStack source) {
        ResourceLocation dragonflyId = new ResourceLocation("crittersandcompanions", "dragonfly");
        EntityType<?> dragonflyType = ForgeRegistries.ENTITY_TYPES.getValue(dragonflyId);
        if (dragonflyType == null) {
            source.sendFailure(Component.literal("Dragonfly entity type not found: " + dragonflyId));
            return 0;
        }

        int rebuilt = 0;
        int skippedUnloaded = 0;
        int failed = 0;

        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.uuid == null || data.dead) {
                continue;
            }
            if (!dragonflyId.toString().equalsIgnoreCase(data.type)) {
                continue;
            }

            TamableAnimal current = findLoadedTameByUuid(source.getServer(), data.uuid);
            if (current == null) {
                skippedUnloaded++;
                continue;
            }
            if (!(current.level() instanceof ServerLevel level)) {
                failed++;
                continue;
            }

            Vec3 pos = current.position();
            float yRot = current.getYRot();
            float xRot = current.getXRot();
            boolean sitting = current.isOrderedToSit();
            UUID ownerId = current.getOwnerUUID() != null ? current.getOwnerUUID() : data.ownerUUID;

            current.getPersistentData().putBoolean(CommonProxy.SKIP_LANTERN_UNLOAD_ONCE_TAG, true);
            current.discard();

            Entity spawned = dragonflyType.create(level);
            if (!(spawned instanceof TamableAnimal rebuiltDragonfly)) {
                failed++;
                continue;
            }

            rebuiltDragonfly.setUUID(data.uuid);
            rebuiltDragonfly.moveTo(pos.x, pos.y, pos.z, yRot, xRot);
            rebuiltDragonfly.setDeltaMovement(0.0D, 0.0D, 0.0D);
            rebuiltDragonfly.setTame(true);
            if (ownerId != null) {
                rebuiltDragonfly.setOwnerUUID(ownerId);
            }
            rebuiltDragonfly.setTarget(null);
            rebuiltDragonfly.getNavigation().stop();
            rebuiltDragonfly.setOrderedToSit(sitting);

            if (!level.addFreshEntity(rebuiltDragonfly)) {
                failed++;
                continue;
            }

            TameGoalInstaller.installIfMissing(rebuiltDragonfly);
            TameRegistry.bindEntityToData(rebuiltDragonfly, data);
            LevelSystem.ensureClassAssigned(rebuiltDragonfly, data, false);
            LevelSystem.reapplyTypeBasePlusBonuses(rebuiltDragonfly, data);
            LevelSystem.updateTameName(rebuiltDragonfly, data);
            rebuiltDragonfly.setHealth(rebuiltDragonfly.getMaxHealth());

            data.ownerUUID = ownerId;
            data.lastKnownDimension = level.dimension().location().toString();
            data.lastKnownX = rebuiltDragonfly.blockPosition().getX();
            data.lastKnownY = rebuiltDragonfly.blockPosition().getY();
            data.lastKnownZ = rebuiltDragonfly.blockPosition().getZ();
            data.lastKnownGameTime = level.getGameTime();
            CompoundTag refreshedSnapshot = new CompoundTag();
            rebuiltDragonfly.save(refreshedSnapshot);
            data.entitySnapshot = refreshedSnapshot;
            rebuilt++;
        }

        TameRegistry.markDirty();
        final int rebuiltCount = rebuilt;
        final int skippedCount = skippedUnloaded;
        final int failedCount = failed;
        source.sendSuccess(() -> Component.literal(
                "Rebuilt " + rebuiltCount + " loaded dragonflies from registry data; skipped unloaded " + skippedCount + "; failed " + failedCount + "."
        ), true);
        return rebuiltCount > 0 ? rebuiltCount : 1;
    }

    private static int fixDragonflyStats(CommandSourceStack source) {
        ResourceLocation dragonflyId = new ResourceLocation("crittersandcompanions", "dragonfly");
        int fixed = 0;
        int skippedUntracked = 0;
        int failed = 0;

        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                if (!dragonflyId.equals(typeId) || !(entity instanceof TamableAnimal tame)) {
                    continue;
                }
                TameData data = TameRegistry.get(tame.getUUID());
                if (data == null) {
                    data = TameRegistry.getByTlId(TameData.getTlId(tame));
                }
                if (data == null) {
                    skippedUntracked++;
                    continue;
                }

                float oldHealth = tame.getHealth();
                float oldMaxHealth = Math.max(1.0F, (float) tame.getMaxHealth());
                double healthRatio = Mth.clamp(oldHealth / oldMaxHealth, 0.0F, 1.0F);

                TameGoalInstaller.installIfMissing(tame);
                TameRegistry.bindEntityToData(tame, data);
                LevelSystem.ensureClassAssigned(tame, data, false);
                if (!LevelSystem.reapplyTypeBasePlusBonuses(tame, data)) {
                    failed++;
                    continue;
                }
                LevelSystem.updateTameName(tame, data);
                tame.setHealth((float) Mth.clamp(tame.getMaxHealth() * healthRatio, 1.0D, tame.getMaxHealth()));
                TameSpawnEvents.queueDeferredStatRefresh(tame, data, 1200L);

                data.lastKnownDimension = level.dimension().location().toString();
                data.lastKnownX = tame.blockPosition().getX();
                data.lastKnownY = tame.blockPosition().getY();
                data.lastKnownZ = tame.blockPosition().getZ();
                data.lastKnownGameTime = level.getGameTime();
                CompoundTag refreshedSnapshot = new CompoundTag();
                tame.save(refreshedSnapshot);
                data.entitySnapshot = refreshedSnapshot;
                fixed++;
            }
        }

        TameRegistry.markDirty();
        final int fixedCount = fixed;
        final int skippedCount = skippedUntracked;
        final int failedCount = failed;
        source.sendSuccess(() -> Component.literal(
                "Fixed dragonfly stats on " + fixedCount + " loaded tracked dragonflies; skipped untracked " + skippedCount + "; failed " + failedCount + "."
        ), true);
        return fixedCount > 0 ? fixedCount : 1;
    }

    private static boolean fixLoadedTameStats(TamableAnimal tame, TameData data) {
        if (tame == null || data == null || !(tame.level() instanceof ServerLevel level)) {
            return false;
        }
        float oldHealth = tame.getHealth();
        float oldMaxHealth = Math.max(1.0F, (float) tame.getMaxHealth());
        double healthRatio = Mth.clamp(oldHealth / oldMaxHealth, 0.0F, 1.0F);

        TameGoalInstaller.installIfMissing(tame);
        TameRegistry.bindEntityToData(tame, data);
        LevelSystem.ensureClassAssigned(tame, data, false);
        if (!LevelSystem.reapplyTypeBasePlusBonuses(tame, data)) {
            return false;
        }
        LevelSystem.updateTameName(tame, data);
        tame.setHealth((float) Mth.clamp(tame.getMaxHealth() * healthRatio, 1.0D, tame.getMaxHealth()));
        TameSpawnEvents.queueDeferredStatRefresh(tame, data, 1200L);

        data.lastKnownDimension = level.dimension().location().toString();
        data.lastKnownX = tame.blockPosition().getX();
        data.lastKnownY = tame.blockPosition().getY();
        data.lastKnownZ = tame.blockPosition().getZ();
        data.lastKnownGameTime = level.getGameTime();
        CompoundTag refreshedSnapshot = new CompoundTag();
        tame.save(refreshedSnapshot);
        data.entitySnapshot = refreshedSnapshot;
        return true;
    }

    private static int adminSetTameStat(CommandSourceStack source, String petName, String stat, int value) {
        ServerPlayer player = source.getPlayer();
        TameData data = resolveAdminAliveTame(player, petName);
        if (data == null) {
            return 0;
        }
        String normalized = stat == null ? "" : stat.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "kills" -> data.kills = Math.max(0, value);
            case "assists" -> data.assists = Math.max(0, value);
            case "deaths" -> data.deaths = Math.max(0, value);
            default -> {
                return error(player, "Unknown stat: " + stat);
            }
        }
        TameRegistry.markDirty();
        source.sendSuccess(() -> Component.literal(
                "Set " + data.name + " " + normalized + " to " + Math.max(0, value) + "."
        ), true);
        return 1;
    }

    private static int adminInvokeTameIntSetter(CommandSourceStack source, String petName, String methodName, int value) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        TameData data = findOwnedTame(player.getUUID(), petName);
        if (data == null) {
            return error(player, "Pet not found: " + petName);
        }
        TamableAnimal tame = findLoadedTameByUuid(source, data.uuid);
        if (tame == null) {
            return error(player, data.name + " is not loaded.");
        }
        if (!player.getUUID().equals(tame.getOwnerUUID())) {
            return error(player, "That tame is not yours.");
        }
        if (!invokeExactIntSetter(tame, methodName, value)) {
            return error(player, tame.getName().getString() + " has no " + methodName + "(int).");
        }
        refreshRegistrySnapshotFor(tame);
        source.sendSuccess(() -> Component.literal(
                "Called " + methodName + "(" + value + ") on " + tame.getName().getString() + "."
        ), true);
        return 1;
    }

    private static int adminInvokeTameBooleanSetter(CommandSourceStack source, String petName, String methodName, boolean value) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        TameData data = findOwnedTame(player.getUUID(), petName);
        if (data == null) {
            return error(player, "Pet not found: " + petName);
        }
        TamableAnimal tame = findLoadedTameByUuid(source, data.uuid);
        if (tame == null) {
            return error(player, data.name + " is not loaded.");
        }
        if (!player.getUUID().equals(tame.getOwnerUUID())) {
            return error(player, "That tame is not yours.");
        }
        if (!invokeExactBooleanSetter(tame, methodName, value)) {
            return error(player, tame.getName().getString() + " has no " + methodName + "(boolean).");
        }
        refreshRegistrySnapshotFor(tame);
        source.sendSuccess(() -> Component.literal(
                "Called " + methodName + "(" + value + ") on " + tame.getName().getString() + "."
        ), true);
        return 1;
    }

    private static boolean invokeExactIntSetter(TamableAnimal tame, String methodName, int value) {
        try {
            Method m = tame.getClass().getMethod(methodName, int.class);
            m.setAccessible(true);
            m.invoke(tame, value);
            return true;
        } catch (Throwable ignored) {
        }
        try {
            Method m = tame.getClass().getMethod(methodName, Integer.class);
            m.setAccessible(true);
            m.invoke(tame, Integer.valueOf(value));
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean invokeExactBooleanSetter(TamableAnimal tame, String methodName, boolean value) {
        try {
            Method m = tame.getClass().getMethod(methodName, boolean.class);
            m.setAccessible(true);
            m.invoke(tame, value);
            return true;
        } catch (Throwable ignored) {
        }
        try {
            Method m = tame.getClass().getMethod(methodName, Boolean.class);
            m.setAccessible(true);
            m.invoke(tame, Boolean.valueOf(value));
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static int adminAddMissingAbilities(CommandSourceStack source) {
        int touchedTames = 0;
        int restoredAbilities = 0;
        int restoredLevels = 0;

        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.levelRewardHistory == null || data.levelRewardHistory.isEmpty()) {
                continue;
            }
            Map<String, Integer> missingByAbility = new HashMap<>();
            for (CompoundTag row : data.levelRewardHistory) {
                if (row == null) {
                    continue;
                }
                boolean active = !row.contains("active") || row.getBoolean("active");
                if (!active) {
                    continue;
                }
                if (!"ABILITY".equalsIgnoreCase(row.getString("rewardCategory"))) {
                    continue;
                }
                String rewardId = row.getString("rewardId");
                if (rewardId == null || rewardId.isBlank()) {
                    continue;
                }
                if (LevelSystem.getAbilityLevel(data, rewardId) > 0) {
                    continue;
                }
                int amount = Math.max(1, (int) Math.round(row.contains("rewardAmount", Tag.TAG_DOUBLE) ? row.getDouble("rewardAmount") : 1.0D));
                missingByAbility.merge(rewardId, amount, Integer::sum);
            }
            if (missingByAbility.isEmpty()) {
                continue;
            }

            int beforeSize = data.abilityLevels.size();
            int addedForTame = 0;
            int levelsForTame = 0;
            for (Map.Entry<String, Integer> entry : missingByAbility.entrySet()) {
                if (LevelSystem.addAbility(data, entry.getKey(), entry.getValue())) {
                    addedForTame++;
                    levelsForTame += Math.max(1, entry.getValue());
                }
            }
            if (addedForTame <= 0 && data.abilityLevels.size() == beforeSize) {
                continue;
            }
            touchedTames++;
            restoredAbilities += addedForTame;
            restoredLevels += levelsForTame;
        }

        if (restoredAbilities <= 0) {
            return error(source.getPlayer(), "No missing recorded abilities were found.");
        }
        TameRegistry.markDirty();
        final int finalTouchedTames = touchedTames;
        final int finalRestoredAbilities = restoredAbilities;
        final int finalRestoredLevels = restoredLevels;
        source.sendSuccess(() -> Component.literal(
                "Restored " + finalRestoredAbilities + " missing ability entries across " + finalTouchedTames
                        + " tame(s), totaling " + finalRestoredLevels + " recorded ability level(s)."
        ), true);
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
        return shouldLanternRecallFromSnapshot(data);
    }

    private static boolean shouldLanternRecallFromSnapshot(TameData data) {
        CompoundTag snapshot = data == null ? null : data.entitySnapshot;
        if (snapshot == null || snapshot.isEmpty()) {
            return true;
        }
        if (DomesticationMod.CONFIG.trinaryCommandSystem.get()) {
            Integer command = findSnapshotCommand(snapshot);
            if (command != null) {
                return matchesSnapshotCommand(command, MovementOrder.FOLLOW, data == null ? null : data.type);
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

    private static boolean matchesMovementOrderSnapshot(TameData data, MovementOrder order) {
        if (data == null) {
            return false;
        }
        CompoundTag snapshot = data.entitySnapshot;
        Integer command = findSnapshotCommand(snapshot);
        if (command != null) {
            return matchesSnapshotCommand(command, order, data.type);
        }
        boolean sitting = false;
        if (snapshot != null && !snapshot.isEmpty()) {
            if (snapshot.contains("Sitting", Tag.TAG_BYTE)) {
                sitting = snapshot.getBoolean("Sitting");
            } else if (snapshot.contains("orderedToSit", Tag.TAG_BYTE)) {
                sitting = snapshot.getBoolean("orderedToSit");
            } else if (snapshot.contains("OrderedToSit", Tag.TAG_BYTE)) {
                sitting = snapshot.getBoolean("OrderedToSit");
            }
        }
        return switch (order) {
            case FOLLOW -> !sitting;
            case SIT -> sitting;
            case WANDER, GUARDIAN -> false;
        };
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
        return LevelSystem.reapplyTypeBasePlusBonuses(tame, data);
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

    private static double resolveBaseValue(TameData data, TamableAnimal template, Attribute attribute, double trackedBonus) {
        Double snapshotBase = readBaseFromSnapshot(data == null ? null : data.entitySnapshot, attribute, trackedBonus);
        if (snapshotBase != null) {
            return snapshotBase;
        }
        return readBaseOrDefault(template, attribute);
    }

    private static Double readBaseFromSnapshot(CompoundTag snapshot, Attribute attribute, double trackedBonus) {
        if (snapshot == null || snapshot.isEmpty() || attribute == null) {
            return null;
        }
        if (!snapshot.contains("Attributes", Tag.TAG_LIST)) {
            return null;
        }
        ResourceLocation key = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (key == null) {
            return null;
        }
        ListTag attributes = snapshot.getList("Attributes", Tag.TAG_COMPOUND);
        for (int i = 0; i < attributes.size(); i++) {
            CompoundTag entry = attributes.getCompound(i);
            if (!entry.contains("Name", Tag.TAG_STRING) || !entry.contains("Base", Tag.TAG_DOUBLE)) {
                continue;
            }
            if (!key.toString().equals(entry.getString("Name"))) {
                continue;
            }
            return entry.getDouble("Base") - trackedBonus;
        }
        return null;
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
    private static int adminError(CommandSourceStack source, String message) {
        if (source != null) {
            source.sendFailure(Component.literal(message));
        }
        return 0;
    }
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
            case GUARDIAN -> "guardian";
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

    private static String parseMovementProfile(String raw) {
        if (raw == null) return null;
        String key = raw.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "default" -> "default";
            case "skeleton" -> "skeleton";
            case "close" -> "close";
            default -> null;
        };
    }

    private static String movementProfileLabel(TameData data) {
        if (data != null) {
            if (data.closeMovement) {
                return "close";
            }
            if (data.skeletonMovement) {
                return "skeleton";
            }
        }
        return "default";
    }

    private static void applyMovementProfile(TameData data, String profile) {
        if (data == null) {
            return;
        }
        data.skeletonMovement = "skeleton".equals(profile);
        data.closeMovement = "close".equals(profile);
    }

    private static boolean matchesMovementOrder(TamableAnimal tame, MovementOrder order) {
        TameData data = tame == null ? null : TameRegistry.get(tame.getUUID());
        return tame != null && currentLiveMovementOrder(tame, data) == order;
    }

    private static MovementOrder currentLiveMovementOrder(TamableAnimal tame, TameData data) {
        if (tame == null) {
            return MovementOrder.FOLLOW;
        }
        if (tame instanceof IComandableMob commandable) {
            return resolveMovementOrderFromSnapshotCommand(commandable.getCommand(), entityTypeId(tame), data != null && data.hasHome);
        }
        if (tame.isOrderedToSit()) {
            return MovementOrder.SIT;
        }
        if (data != null && data.hasHome && data.movementOrder == 3) {
            return MovementOrder.GUARDIAN;
        }
        return MovementOrder.FOLLOW;
    }

    private static boolean usesInvertedGenericCallOrder(TamableAnimal tame) {
        if (tame == null) {
            return true;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        String typeId = key == null ? tame.getType().toString() : key.toString();
        return usesInvertedGenericCallOrder(typeId);
    }

    private static boolean usesInvertedGenericCallOrder(String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return true;
        }
        String normalized = typeId.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("entity.")) {
            normalized = normalized.substring("entity.".length());
        }
        return !TameRegistry.isCallOrderInvertedType(normalized);
    }

    private static boolean matchesSnapshotCommand(int command, MovementOrder order, TamableAnimal tame) {
        return matchesSnapshotCommand(command, order, tame == null ? null : entityTypeId(tame));
    }

    private static boolean matchesSnapshotCommand(int command, MovementOrder order, String typeId) {
        if (TameRegistry.isFollowSitOnlyType(typeId)) {
            int sitCommand = twoStateSitCommand(typeId);
            return switch (order) {
                case FOLLOW -> command != sitCommand;
                case SIT -> command == sitCommand;
                case WANDER, GUARDIAN -> false;
            };
        }
        int expected = switch (order) {
            case WANDER, GUARDIAN -> 0;
            case FOLLOW -> usesInvertedGenericCallOrder(typeId) ? 1 : 2;
            case SIT -> usesInvertedGenericCallOrder(typeId) ? 2 : 1;
        };
        return command == expected;
    }

    private static MovementOrder resolveMovementOrderFromSnapshotCommand(int command, String typeId, boolean hasHome) {
        if (matchesSnapshotCommand(command, MovementOrder.FOLLOW, typeId)) {
            return MovementOrder.FOLLOW;
        }
        if (matchesSnapshotCommand(command, MovementOrder.SIT, typeId)) {
            return MovementOrder.SIT;
        }
        return hasHome ? MovementOrder.GUARDIAN : MovementOrder.WANDER;
    }

    private static int twoStateSitCommand(String typeId) {
        return usesInvertedGenericCallOrder(typeId) ? 2 : 1;
    }

    private static String entityTypeId(TamableAnimal tame) {
        if (tame == null) {
            return null;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        return key == null ? tame.getType().toString() : key.toString();
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

    public static int captureMovementOrderCode(TamableAnimal tame, TameData data) {
        MovementOrder order = resolveRepairMovementOrder(tame, data);
        return switch (order) {
            case FOLLOW -> 0;
            case SIT -> 1;
            case WANDER -> 2;
            case GUARDIAN -> 3;
        };
    }

    public static void applyMovementOrderCode(TamableAnimal tame, int orderCode) {
        applyMovementOverride(tame, switch (orderCode) {
            case 1 -> MovementOrder.SIT;
            case 2 -> MovementOrder.WANDER;
            case 3 -> MovementOrder.GUARDIAN;
            default -> MovementOrder.FOLLOW;
        });
    }

    private static void applyMovementOverride(TamableAnimal tame, MovementOrder order) {
        if (tame == null) return;
        TameData data = TameRegistry.get(tame.getUUID());
        if (order != MovementOrder.GUARDIAN) {
            clearGuardianAnchor(data);
        }
        if (data != null) {
            data.movementOrder = switch (order) {
                case FOLLOW -> 0;
                case SIT -> 1;
                case WANDER -> 2;
                case GUARDIAN -> 3;
            };
        }
        tame.setTarget(null);
        tame.getNavigation().stop();
        if (tame instanceof IComandableMob commandableMob) {
            syncCommandableMovementState(tame, commandableMob, order);
            MovementOrder liveOrder = currentLiveMovementOrder(tame, data);
            boolean sit = liveOrder == MovementOrder.SIT;
            tame.setOrderedToSit(sit);
            tame.setInSittingPose(sit);
            if (liveOrder != MovementOrder.FOLLOW) {
                tame.setTarget(null);
            }
        } else {
            boolean sit = order == MovementOrder.SIT;
            tame.setOrderedToSit(sit);
            tame.setInSittingPose(sit);
            if (sit || order == MovementOrder.WANDER) {
                tame.setTarget(null);
            }
            if (!usesMinimalMovementOverride(tame)) {
                // Best-effort compatibility with non-DI wandering/order state.
                clearExternalWanderingState(tame, order);
            }
        }
        if (order == MovementOrder.SIT) {
            tame.setTarget(null);
        }
        refreshRegistrySnapshotFor(tame);
    }

    public static boolean syncLiveMovementStateFor(TamableAnimal tame) {
        if (tame == null) {
            return false;
        }
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) {
            return false;
        }
        MovementOrder liveOrder = currentLiveMovementOrder(tame, data);
        int liveCode = switch (liveOrder) {
            case FOLLOW -> 0;
            case SIT -> 1;
            case WANDER -> 2;
            case GUARDIAN -> 3;
        };
        boolean changed = data.movementOrder != liveCode || !matchesMovementOrderSnapshot(data, liveOrder);
        if (!changed) {
            return false;
        }
        data.movementOrder = liveCode;
        refreshRegistrySnapshotFor(tame);
        return true;
    }

    private static boolean usesMinimalMovementOverride(TamableAnimal tame) {
        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        return isLegendaryMonstersType(typeKey == null ? null : typeKey.toString());
    }

    private static boolean isLegendaryMonstersType(String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return false;
        }
        String normalized = typeId.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("entity.")) {
            normalized = normalized.substring("entity.".length());
        }
        return normalized.startsWith("legendary_monsters:")
                || normalized.startsWith("legendary_monsters.");
    }

    private static void syncCommandableMovementState(TamableAnimal tame, IComandableMob commandableMob, MovementOrder order) {
        TameData data = TameRegistry.get(tame.getUUID());
        if (currentLiveMovementOrder(tame, data) == order) {
            return;
        }
        if (tame.getOwner() instanceof Player owner) {
            Animal animal = tame;
            int attempts = Math.max(3, commandCandidates(tame, order).length + 1);
            for (int i = 0; i < attempts && currentLiveMovementOrder(tame, data) != order; i++) {
                commandableMob.playerSetCommand(owner, animal);
            }
        }
        if (currentLiveMovementOrder(tame, data) == order) {
            return;
        }
        for (int candidate : commandCandidates(tame, order)) {
            commandableMob.setCommand(candidate);
            if (currentLiveMovementOrder(tame, data) == order) {
                return;
            }
        }
    }

    private static void clearExternalWanderingState(TamableAnimal tame, MovementOrder order) {
        boolean sit = order == MovementOrder.SIT;
        boolean follow = order == MovementOrder.FOLLOW;
        boolean wander = order == MovementOrder.WANDER || order == MovementOrder.GUARDIAN;
        tryInvokeBooleanSetter(tame, "setWandering", wander);
        tryInvokeBooleanSetter(tame, "setWander", wander);
        tryInvokeBooleanSetter(tame, "setDrumWandering", wander);
        tryInvokeBooleanSetter(tame, "setCommandWander", wander);
        tryInvokeBooleanSetter(tame, "setFollowing", follow);
        tryInvokeBooleanSetter(tame, "setFollow", follow);
        tryInvokeBooleanSetter(tame, "setSitting", sit);
        tryInvokeBooleanSetter(tame, "setSit", sit);
        // Common int-based command APIs used by tame mods.
        int genericCommand = preferredCommandInt(tame, order);
        tryInvokeIntSetter(tame, "setCommand", genericCommand);
        tryInvokeIntSetter(tame, "setPetCommand", genericCommand);
        tryInvokeIntSetter(tame, "setOrder", genericCommand);
        tryInvokeIntSetter(tame, "setMode", genericCommand);
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
            case GUARDIAN -> 0;
        };
    }

    private static int preferredCommandInt(TamableAnimal tame, MovementOrder order) {
        if (TameRegistry.isFollowSitOnlyType(entityTypeId(tame))) {
            return switch (order) {
                case SIT -> twoStateSitCommand(entityTypeId(tame));
                case FOLLOW, WANDER, GUARDIAN -> 0;
            };
        }
        if (usesInvertedGenericCallOrder(tame)) {
            return switch (order) {
                case WANDER -> 0;
                case FOLLOW -> 1;
                case SIT -> 2;
                case GUARDIAN -> 0;
            };
        }
        return switch (order) {
            case WANDER -> 0;
            case SIT -> 1;
            case FOLLOW -> 2;
            case GUARDIAN -> 0;
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
                    for (int candidate : commandCandidates(tame, order)) {
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
                case GUARDIAN -> new String[]{"wander", "roam", "free"};
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
        return commandCandidates(null, order);
    }

    private static int[] commandCandidates(TamableAnimal tame, MovementOrder order) {
        if (TameRegistry.isFollowSitOnlyType(entityTypeId(tame))) {
            int sitCommand = twoStateSitCommand(entityTypeId(tame));
            return switch (order) {
                case SIT -> sitCommand == 2 ? new int[]{2, 1, 0, 3} : new int[]{1, 2, 0, 3};
                case FOLLOW, WANDER, GUARDIAN -> new int[]{0, 2, 1, 3};
            };
        }
        if (usesInvertedGenericCallOrder(tame)) {
            return switch (order) {
                case WANDER -> new int[]{0, 1, 2, 3};
                case FOLLOW -> new int[]{1, 2, 0, 3};
                case SIT -> new int[]{2, 1, 0, 3};
                case GUARDIAN -> new int[]{0, 1, 2, 3};
            };
        }
        return switch (order) {
            case WANDER -> new int[]{0, 2, 1, 3};
            case SIT -> new int[]{1, 2, 0, 3};
            case FOLLOW -> new int[]{2, 1, 0, 3};
            case GUARDIAN -> new int[]{0, 2, 1, 3};
        };
    }

    private static void setGuardianAnchor(ServerPlayer player, TamableAnimal tame, TameData data) {
        if (player == null || tame == null || data == null) {
            return;
        }
        rememberCurrentGuardianAnchor(data);
        applyGuardianAnchor(player.serverLevel().dimension().location().toString(), player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ(), tame, data);
    }

    private static void setGuardianAnchorWithoutRemember(ServerPlayer player, TamableAnimal tame, TameData data) {
        if (player == null || tame == null || data == null) {
            return;
        }
        applyGuardianAnchor(player.serverLevel().dimension().location().toString(), player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ(), tame, data);
    }

    private static void applyGuardianAnchor(String dimensionId, int x, int y, int z, TamableAnimal tame, TameData data) {
        data.hasHome = true;
        data.homeDimension = dimensionId == null ? "" : dimensionId;
        data.homeX = x;
        data.homeY = y;
        data.homeZ = z;
        data.guardianReturnTicks = 0;
        data.guardianRelaxing = false;
        data.guardianNextPhaseTick = 0L;
        data.guardianTargetUuid = null;
        data.guardianTargetStuckTicks = 0;
        data.guardianTargetBestDistanceSq = 0.0D;
        applyMovementOverride(tame, MovementOrder.GUARDIAN);
        refreshRegistrySnapshotFor(tame);
        TameRegistry.markDirty();
    }

    private static boolean restorePreviousGuardianAnchor(TamableAnimal tame, TameData data) {
        if (tame == null || data == null || !data.hasPreviousHome) {
            return false;
        }
        boolean hadCurrent = data.hasHome;
        String currentDim = data.homeDimension;
        int currentX = data.homeX;
        int currentY = data.homeY;
        int currentZ = data.homeZ;

        data.hasHome = true;
        data.homeDimension = data.previousHomeDimension == null ? "" : data.previousHomeDimension;
        data.homeX = data.previousHomeX;
        data.homeY = data.previousHomeY;
        data.homeZ = data.previousHomeZ;
        data.guardianReturnTicks = 0;
        data.guardianRelaxing = false;
        data.guardianNextPhaseTick = 0L;
        data.guardianTargetUuid = null;
        data.guardianTargetStuckTicks = 0;
        data.guardianTargetBestDistanceSq = 0.0D;

        if (hadCurrent) {
            data.hasPreviousHome = true;
            data.previousHomeDimension = currentDim == null ? "" : currentDim;
            data.previousHomeX = currentX;
            data.previousHomeY = currentY;
            data.previousHomeZ = currentZ;
        } else {
            data.hasPreviousHome = false;
            data.previousHomeDimension = "";
            data.previousHomeX = 0;
            data.previousHomeY = 0;
            data.previousHomeZ = 0;
        }
        applyMovementOverride(tame, MovementOrder.GUARDIAN);
        refreshRegistrySnapshotFor(tame);
        TameRegistry.markDirty();
        return true;
    }

    private static void rememberCurrentGuardianAnchor(TameData data) {
        if (data == null || !data.hasHome) {
            return;
        }
        data.hasPreviousHome = true;
        data.previousHomeDimension = data.homeDimension == null ? "" : data.homeDimension;
        data.previousHomeX = data.homeX;
        data.previousHomeY = data.homeY;
        data.previousHomeZ = data.homeZ;
    }

    private static void clearGuardianAnchor(TameData data) {
        if (data == null || !data.hasHome) {
            return;
        }
        data.hasHome = false;
        data.homeDimension = "";
        data.homeX = 0;
        data.homeY = 0;
        data.homeZ = 0;
        data.guardianReturnTicks = 0;
        data.guardianRelaxing = false;
        data.guardianNextPhaseTick = 0L;
        data.guardianTargetUuid = null;
        data.guardianTargetStuckTicks = 0;
        data.guardianTargetBestDistanceSq = 0.0D;
        TameRegistry.markDirty();
    }

    private static String normalizeGuardianSetName(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private static void putGuardianSetAnchor(TameData data, String setName, String dimensionId, int x, int y, int z) {
        if (data == null || setName == null || setName.isBlank()) return;
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimensionId == null ? "" : dimensionId);
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);
        data.guardianSetAnchors.put(setName, tag);
    }

    private static CompoundTag getGuardianSetAnchor(TameData data, String setName) {
        if (data == null || setName == null || setName.isBlank()) return null;
        CompoundTag tag = data.guardianSetAnchors.get(setName);
        return tag == null || tag.isEmpty() ? null : tag;
    }

    private static List<TameData> ownedGuardianSetMembers(UUID owner, String setName) {
        List<TameData> list = new ArrayList<>();
        for (TameData data : ownedTames(owner)) {
            if (getGuardianSetAnchor(data, setName) != null) {
                list.add(data);
            }
        }
        return list;
    }

    private static SpawnTarget spawnTargetFromGuardianSet(CommandSourceStack source, CompoundTag anchor) {
        if (source == null || anchor == null || anchor.isEmpty()) return null;
        String dimensionId = anchor.getString("dimension");
        if (dimensionId == null || dimensionId.isBlank()) return null;
        ResourceLocation location = ResourceLocation.tryParse(dimensionId);
        if (location == null) return null;
        ServerLevel level = source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, location));
        if (level == null) return null;
        return new SpawnTarget(level, new Vec3(anchor.getInt("x") + 0.5D, anchor.getInt("y"), anchor.getInt("z") + 0.5D), 0.0F, 0.0F);
    }

    private static SpawnTarget spawnTargetFromCurrentGuardian(CommandSourceStack source, TameData data) {
        if (source == null || data == null || !data.hasHome) {
            return null;
        }
        CompoundTag anchor = new CompoundTag();
        anchor.putString("dimension", data.homeDimension == null ? "" : data.homeDimension);
        anchor.putInt("x", data.homeX);
        anchor.putInt("y", data.homeY);
        anchor.putInt("z", data.homeZ);
        return spawnTargetFromGuardianSet(source, anchor);
    }

    private static boolean deployLoadedTameToGuardianSet(TamableAnimal tame, TameData data, SpawnTarget target) {
        if (tame == null || data == null || target == null || target.level == null || target.pos == null) return false;
        TameTransferService.TransferResult result = TameTransferService.transferToLocation(
                tame,
                target.level,
                target.pos.x,
                target.pos.y,
                target.pos.z,
                target.yRot,
                target.xRot,
                data
        );
        if (!result.success()) {
            return false;
        }
        TamableAnimal moved = result.entity();
        if (moved == null) {
            return false;
        }
        rememberCurrentGuardianAnchor(data);
        applyGuardianAnchor(target.level.dimension().location().toString(), Mth.floor(target.pos.x), Mth.floor(target.pos.y), Mth.floor(target.pos.z), moved, data);
        return true;
    }

    private static boolean deployLoadedTameToGuardianCurrent(TamableAnimal tame, SpawnTarget target) {
        if (tame == null || target == null || target.level == null || target.pos == null) {
            return false;
        }
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) {
            return false;
        }
        TameTransferService.TransferResult result = TameTransferService.transferToLocation(
                tame,
                target.level,
                target.pos.x,
                target.pos.y,
                target.pos.z,
                target.yRot,
                target.xRot,
                data
        );
        if (!result.success()) {
            return false;
        }
        TamableAnimal moved = result.entity();
        if (moved == null) {
            return false;
        }
        applyMovementOverride(moved, MovementOrder.GUARDIAN);
        refreshRegistrySnapshotFor(moved);
        moved.getNavigation().stop();
        data.guardianReturnTicks = 0;
        data.guardianRelaxing = false;
        data.guardianNextPhaseTick = 0L;
        data.guardianTargetUuid = null;
        data.guardianTargetStuckTicks = 0;
        data.guardianTargetBestDistanceSq = 0.0D;
        TameRegistry.markDirty();
        return true;
    }

    private static boolean isTameLoadedAnywhere(MinecraftServer server, TameData data) {
        return server != null && data != null && findLoadedTameByIdentity(server, data.uuid, data.tlId) != null;
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

    private static List<TameData> ownedGuardianTames(UUID owner) {
        List<TameData> list = new ArrayList<>();
        for (TameData data : ownedTames(owner)) {
            if (data != null && data.hasHome) {
                list.add(data);
            }
        }
        return list;
    }

    private static List<TameData> ownedDeadGuardianTames(UUID owner) {
        List<TameData> list = new ArrayList<>();
        for (TameData data : ownedDeadTames(owner)) {
            if (data != null && data.hasHome) {
                list.add(data);
            }
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

    private static void prepareAutoReincarnationOnRespawn(MinecraftServer server, TameData data) {
        if (server == null || data == null || data.ownerUUID == null) {
            return;
        }
        boolean freeBlackBed = hasBlackAutoReincarnationBed(server, data);
        if (!freeBlackBed && !TameRegistry.isAutoReincarnationEnabled(data.ownerUUID)) {
            return;
        }
        if (!(data.hasSavedProgress && data.level < highestRecordedLevel(data))) {
            return;
        }
        TameDeathRecord record = latestAvailableDeathForTame(data.ownerUUID, data.uuid, data.tlId, data.name);
        if (record == null || record.reincarnated || record.snapshot == null || record.snapshot.isEmpty()) {
            return;
        }
        if (freeBlackBed) {
            record.autoReincarnateOnRespawn = true;
            TameRegistry.markDirty();
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(data.ownerUUID);
        if (owner == null) {
            return;
        }
        BlockPos bedPos = owner.getRespawnPosition();
        if (bedPos == null) {
            return;
        }
        ServerLevel bedLevel = server.getLevel(owner.getRespawnDimension());
        if (bedLevel == null) {
            return;
        }
        int restoredLevels = Math.max(1, highestRecordedLevel(data) - Math.max(1, data.level));
        if (consumeAutoReincarnationMaterials(bedLevel, bedPos, restoredLevels)) {
            record.autoReincarnateOnRespawn = true;
            TameRegistry.markDirty();
        }
    }

    private static boolean consumeAutoReincarnationMaterials(ServerLevel level, BlockPos bedPos, int approvedItemCost) {
        if (level == null || bedPos == null) {
            return false;
        }
        Set<BlockPos> bedAndNeighbors = reincarnationBedScanPositions(level, bedPos);
        List<InventoryAccess> inventories = new ArrayList<>();
        Set<Object> seenInventories = new HashSet<>();
        for (BlockPos scanPos : bedAndNeighbors) {
            for (Direction direction : Direction.values()) {
                BlockEntity blockEntity = level.getBlockEntity(scanPos.relative(direction));
                if (blockEntity == null) {
                    continue;
                }
                InventoryAccess containerAccess = inventoryAccessFromBlockEntity(blockEntity);
                if (containerAccess != null && seenInventories.add(containerAccess.identity())) {
                    inventories.add(containerAccess);
                }
            }
        }
        if (inventories.isEmpty()) {
            return false;
        }
        if (consumeFromInventories(inventories, 1, TameCommands::isReincarnationTotem)) {
            return true;
        }
        return consumeFromInventories(inventories, Math.max(1, approvedItemCost), TameCommands::isApprovedReincarnationItem);
    }

    private static InventoryAccess inventoryAccessFromBlockEntity(BlockEntity blockEntity) {
        if (blockEntity instanceof Container container) {
            return new ContainerInventoryAccess(container);
        }
        LazyOptional<IItemHandler> itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
        if (itemHandler.isPresent()) {
            IItemHandler handler = itemHandler.orElse(null);
            if (handler != null) {
                return new ItemHandlerInventoryAccess(blockEntity, handler);
            }
        }
        return null;
    }

    private static boolean consumeFromInventories(List<InventoryAccess> inventories, int required, java.util.function.Predicate<ItemStack> matcher) {
        if (inventories == null || inventories.isEmpty() || matcher == null || required <= 0) {
            return false;
        }
        int remaining = required;
        for (InventoryAccess inventory : inventories) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty() || !matcher.test(stack)) {
                    continue;
                }
                remaining -= stack.getCount();
                if (remaining <= 0) {
                    int toConsume = required;
                    for (InventoryAccess consumeInventory : inventories) {
                        for (int consumeSlot = 0; consumeSlot < consumeInventory.getContainerSize() && toConsume > 0; consumeSlot++) {
                            ItemStack consumeStack = consumeInventory.getItem(consumeSlot);
                            if (consumeStack.isEmpty() || !matcher.test(consumeStack)) {
                                continue;
                            }
                            int shrink = Math.min(toConsume, consumeStack.getCount());
                            ItemStack removed = consumeInventory.remove(consumeSlot, shrink);
                            toConsume -= Math.min(shrink, removed.getCount());
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<BlockPos> reincarnationBedScanPositions(ServerLevel level, BlockPos bedPos) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.add(bedPos);
        if (level == null || bedPos == null) {
            return positions;
        }
        BlockState state = level.getBlockState(bedPos);
        if (!(state.getBlock() instanceof BedBlock) || !state.hasProperty(BedBlock.FACING) || !state.hasProperty(BedBlock.PART)) {
            return positions;
        }
        Direction facing = state.getValue(BedBlock.FACING);
        BedPart part = state.getValue(BedBlock.PART);
        BlockPos otherHalf = part == BedPart.HEAD ? bedPos.relative(facing.getOpposite()) : bedPos.relative(facing);
        positions.add(otherHalf);
        return positions;
    }

    private interface InventoryAccess {
        int getContainerSize();

        ItemStack getItem(int slot);

        ItemStack remove(int slot, int amount);

        Object identity();
    }

    private static final class ContainerInventoryAccess implements InventoryAccess {
        private final Container container;

        private ContainerInventoryAccess(Container container) {
            this.container = container;
        }

        @Override
        public int getContainerSize() {
            return container.getContainerSize();
        }

        @Override
        public ItemStack getItem(int slot) {
            return container.getItem(slot);
        }

        @Override
        public ItemStack remove(int slot, int amount) {
            return container.removeItem(slot, amount);
        }

        @Override
        public Object identity() {
            return container;
        }
    }

    private static final class ItemHandlerInventoryAccess implements InventoryAccess {
        private final BlockEntity blockEntity;
        private final IItemHandler itemHandler;

        private ItemHandlerInventoryAccess(BlockEntity blockEntity, IItemHandler itemHandler) {
            this.blockEntity = blockEntity;
            this.itemHandler = itemHandler;
        }

        @Override
        public int getContainerSize() {
            return itemHandler.getSlots();
        }

        @Override
        public ItemStack getItem(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack remove(int slot, int amount) {
            ItemStack extracted = itemHandler.extractItem(slot, amount, false);
            if (!extracted.isEmpty()) {
                blockEntity.setChanged();
            }
            return extracted;
        }

        @Override
        public Object identity() {
            return blockEntity;
        }
    }

    private static boolean applyLatestDeathSnapshotIfAvailable(TamableAnimal tame, TameData current) {
        if (tame == null || current == null || current.ownerUUID == null) {
            return false;
        }
        TameDeathRecord record = latestAvailableDeathForTame(current.ownerUUID, current.uuid, current.tlId, current.name);
        if (record == null || !record.autoReincarnateOnRespawn || record.snapshot == null || record.snapshot.isEmpty()) {
            return false;
        }
        if (!LevelSystem.restoreHighestProgressWithoutXpCost(tame, current)) {
            return false;
        }
        record.reincarnated = true;
        record.autoReincarnateOnRespawn = false;
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
            if (isInGroup(d, group)) list.add(d);
        }
        return list;
    }

    private static Set<String> groupMemberships(TameData data) {
        Set<String> groups = new LinkedHashSet<>();
        if (data == null || data.group == null || data.group.isBlank()) {
            return groups;
        }
        for (String raw : data.group.split("\\|")) {
            if (raw == null) {
                continue;
            }
            String trimmed = raw.trim();
            if (!trimmed.isBlank()) {
                groups.add(trimmed);
            }
        }
        return groups;
    }

    private static boolean isInGroup(TameData data, String group) {
        if (data == null || group == null || group.isBlank()) {
            return false;
        }
        for (String existing : groupMemberships(data)) {
            if (existing.equalsIgnoreCase(group)) {
                return true;
            }
        }
        return false;
    }

    private static boolean addGroupMembership(TameData data, String group) {
        if (data == null || group == null || group.isBlank()) {
            return false;
        }
        Set<String> groups = groupMemberships(data);
        for (String existing : groups) {
            if (existing.equalsIgnoreCase(group)) {
                return false;
            }
        }
        groups.add(group.trim());
        data.group = String.join("|", groups);
        return true;
    }

    private static boolean removeGroupMembership(TameData data, String group) {
        if (data == null || group == null || group.isBlank()) {
            return false;
        }
        Set<String> groups = groupMemberships(data);
        boolean removed = groups.removeIf(existing -> existing.equalsIgnoreCase(group));
        if (removed) {
            data.group = String.join("|", groups);
        }
        return removed;
    }

    private static String groupLabel(TameData data) {
        Set<String> groups = groupMemberships(data);
        return groups.isEmpty() ? "-" : String.join(", ", groups);
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

    private static List<TameData> ownedGuardianGroup(UUID owner, String group) {
        List<TameData> list = new ArrayList<>();
        for (TameData data : ownedGroup(owner, group)) {
            if (data != null && data.hasHome && !isDeadEntry(data.uuid)) {
                list.add(data);
            }
        }
        return list;
    }

    private static List<TameData> ownedDeadGuardianGroup(UUID owner, String group) {
        List<TameData> list = new ArrayList<>();
        for (TameData data : ownedGroup(owner, group)) {
            if (data != null && data.hasHome && isDeadEntry(data.uuid)) {
                list.add(data);
            }
        }
        return list;
    }

    private static List<TamableAnimal> loadedOwnedGroupTames(CommandSourceStack source, UUID owner, String group) {
        List<TamableAnimal> list = new ArrayList<>();
        for (TameData data : ownedGroup(owner, group)) {
            if (isInactiveEntry(data.uuid)) continue;
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, owner, data.uuid);
            if (tame == null || !tame.isAlive()) continue;
            list.add(tame);
        }
        return list;
    }

    private static List<TamableAnimal> loadedOwnedAllTames(CommandSourceStack source, UUID owner) {
        List<TamableAnimal> list = new ArrayList<>();
        for (TameData data : ownedTames(owner)) {
            if (isInactiveEntry(data.uuid)) continue;
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
            case STATE -> {
                MovementOrder order = parseMovementOrder(selection.value);
                if (order == null) {
                    yield DuelSelectionResult.fail("Invalid movement state '" + selection.value + "'.");
                }
                List<TamableAnimal> loaded = loadedOwnedStateTames(source, owner, order);
                if (loaded.isEmpty()) {
                    yield DuelSelectionResult.fail("No loaded alive tames found for state '" + movementLabel(order) + "'.");
                }
                yield DuelSelectionResult.ok(loaded);
            }
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
                List<TamableAnimal> loaded = new ArrayList<>();
                for (TameData data : owned) {
                    TamableAnimal tame = findLoadedOwnedTameByUuid(source, owner, data.uuid);
                    if (tame != null && tame.isAlive()) {
                        loaded.add(tame);
                    }
                }
                if (loaded.isEmpty()) {
                    yield DuelSelectionResult.fail("No loaded alive tames found for duel.");
                }
                yield DuelSelectionResult.ok(loaded);
            }
        };
    }

    private static TeamSelectionResult resolveLoadedTeamSelection(CommandSourceStack source, ServerPlayer owner, TeamSelection selection) {
        if (owner == null) return TeamSelectionResult.fail("Owner is not online.");
        if (selection == null) return TeamSelectionResult.fail("Invalid duel team selection.");
        List<LivingEntity> members = new ArrayList<>();
        List<TamableAnimal> tames = new ArrayList<>();
        Set<UUID> seen = new LinkedHashSet<>();
        if (selection.includeSelf) {
            if (!owner.isAlive()) {
                return TeamSelectionResult.fail("You must be alive to duel as yourself.");
            }
            members.add(owner);
            seen.add(owner.getUUID());
        }
        if (selection.tameSelections != null && !selection.tameSelections.isEmpty()) {
            for (DuelSelection tameSelection : selection.tameSelections) {
                DuelSelectionResult tameResult = resolveLoadedDuelSelection(source, owner.getUUID(), tameSelection);
                if (!tameResult.error.isBlank()) {
                    return TeamSelectionResult.fail(tameResult.error);
                }
                for (TamableAnimal tame : tameResult.tames) {
                    if (tame == null || !tame.isAlive()) {
                        continue;
                    }
                    if (seen.add(tame.getUUID())) {
                        tames.add(tame);
                        members.add(tame);
                    }
                }
            }
        }
        if (members.isEmpty()) {
            return TeamSelectionResult.fail("Your duel team selection is empty.");
        }
        return TeamSelectionResult.ok(members, tames);
    }

    private static DuelSelectionResult resolveGroupDuelSelection(CommandSourceStack source, UUID owner, String group) {
        List<TameData> groupMembers = ownedGroup(owner, group);
        if (groupMembers.isEmpty()) {
            return DuelSelectionResult.fail("Group '" + group + "' has no tames.");
        }
        List<String> dead = new ArrayList<>();
        List<TamableAnimal> loaded = new ArrayList<>();
        for (TameData data : groupMembers) {
            String name = data.name == null || data.name.isBlank() ? "unknown" : data.name;
            if (isDeadEntry(data.uuid)) {
                dead.add(name);
                continue;
            }
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, owner, data.uuid);
            if (tame != null && tame.isAlive()) {
                loaded.add(tame);
            }
        }
        if (loaded.isEmpty()) {
            return DuelSelectionResult.fail(dead.isEmpty()
                    ? "Group '" + group + "' has no loaded alive tames."
                    : "Group '" + group + "' has no loaded alive tames. Dead skipped: " + String.join(", ", dead));
        }
        return DuelSelectionResult.ok(loaded);
    }

    private static DuelSelectionResult resolveTypeDuelSelection(CommandSourceStack source, UUID owner, String type) {
        List<TameData> typeMembers = ownedType(owner, type);
        if (typeMembers.isEmpty()) {
            return DuelSelectionResult.fail("Type '" + type + "' has no tames.");
        }
        List<String> dead = new ArrayList<>();
        List<TamableAnimal> loaded = new ArrayList<>();
        for (TameData data : typeMembers) {
            String name = data.name == null || data.name.isBlank() ? "unknown" : data.name;
            if (isDeadEntry(data.uuid)) {
                dead.add(name);
                continue;
            }
            TamableAnimal tame = findLoadedOwnedTameByUuid(source, owner, data.uuid);
            if (tame != null && tame.isAlive()) {
                loaded.add(tame);
            }
        }
        if (loaded.isEmpty()) {
            return DuelSelectionResult.fail(dead.isEmpty()
                    ? "Type '" + type + "' has no loaded alive tames."
                    : "Type '" + type + "' has no loaded alive tames. Dead skipped: " + String.join(", ", dead));
        }
        return DuelSelectionResult.ok(loaded);
    }

    private static List<TamableAnimal> remainingDuelOpponents(CommandSourceStack source, UUID owner, List<TamableAnimal> excluded) {
        Set<UUID> excludedIds = new HashSet<>();
        if (excluded != null) {
            for (TamableAnimal tame : excluded) {
                if (tame != null) {
                    excludedIds.add(tame.getUUID());
                }
            }
        }
        List<TamableAnimal> remaining = new ArrayList<>();
        for (TamableAnimal tame : loadedOwnedAllTames(source, owner)) {
            if (tame == null || !tame.isAlive()) continue;
            if (excludedIds.contains(tame.getUUID())) continue;
            remaining.add(tame);
        }
        return remaining;
    }

    private static TeamSelection parseTeamSelectionSpec(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isBlank()) return null;
        String[] tokens = trimmed.split("\\s+");
        int index = 0;
        boolean includeSelf = false;
        if (index < tokens.length && tokens[index].equalsIgnoreCase("myself")) {
            includeSelf = true;
            index++;
        }
        if (index >= tokens.length) {
            return includeSelf ? TeamSelection.selfOnly() : null;
        }

        String head = tokens[index].toLowerCase(Locale.ROOT);
        DuelSelection selection;
        switch (head) {
            case "all" -> {
                selection = DuelSelection.all();
                index++;
            }
            case "group" -> {
                if (index + 1 >= tokens.length) return null;
                selection = DuelSelection.group(joinSelectionTokens(tokens, index + 1));
                index = tokens.length;
            }
            case "type" -> {
                if (index + 1 >= tokens.length) return null;
                selection = DuelSelection.type(joinSelectionTokens(tokens, index + 1));
                index = tokens.length;
            }
            case "state" -> {
                if (index + 1 >= tokens.length) return null;
                selection = DuelSelection.state(tokens[index + 1]);
                index += 2;
            }
            case "follow", "sit", "wander" -> {
                selection = DuelSelection.state(head);
                index++;
            }
            case "name" -> {
                if (index + 1 >= tokens.length) return null;
                selection = DuelSelection.single(joinSelectionTokens(tokens, index + 1));
                index = tokens.length;
            }
            default -> {
                return null;
            }
        }
        if (index != tokens.length) {
            return null;
        }
        return TeamSelection.of(includeSelf, selection);
    }

    private static CompactDuelSideParseResult parseCompactDuelSide(CommandSourceStack source, ServerPlayer owner, String raw) {
        if (owner == null) {
            return CompactDuelSideParseResult.fail("Player required.");
        }
        if (raw == null || raw.trim().isBlank()) {
            return CompactDuelSideParseResult.fail("Empty duel selection.");
        }
        boolean includeSelf = false;
        LinkedHashSet<String> targetPlayerNames = new LinkedHashSet<>();
        List<DuelSelection> selections = new ArrayList<>();
        String[] terms = raw.split(",");
        for (String term : terms) {
            String trimmed = term == null ? "" : term.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.equalsIgnoreCase("myself") || trimmed.equalsIgnoreCase(owner.getGameProfile().getName())) {
                includeSelf = true;
                continue;
            }
            ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(trimmed);
            if (online != null && !online.getUUID().equals(owner.getUUID())) {
                targetPlayerNames.add(online.getGameProfile().getName());
                continue;
            }
            DuelSelection selection = parseCompactDuelSelector(trimmed);
            if (selection == null) {
                return CompactDuelSideParseResult.fail("Invalid duel selector: " + trimmed + ".");
            }
            selections.add(selection);
        }
        TeamSelection selection = TeamSelection.of(includeSelf, selections);
        if (!selection.includeSelf && selection.tameSelections.isEmpty() && targetPlayerNames.isEmpty()) {
            return CompactDuelSideParseResult.fail("Empty duel selection.");
        }
        return CompactDuelSideParseResult.ok(selection, new ArrayList<>(targetPlayerNames));
    }

    private static DuelSelection parseCompactDuelSelector(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.equals("all")) {
            return DuelSelection.all();
        }
        if (lower.equals("follow") || lower.equals("sit") || lower.equals("wander")) {
            return DuelSelection.state(lower);
        }
        if (lower.startsWith("group ")) {
            return DuelSelection.group(trimmed.substring(6).trim());
        }
        if (lower.startsWith("type ")) {
            return DuelSelection.type(trimmed.substring(5).trim());
        }
        if (lower.startsWith("state ")) {
            return DuelSelection.state(trimmed.substring(6).trim());
        }
        if (lower.startsWith("name ")) {
            return DuelSelection.single(trimmed.substring(5).trim());
        }
        return DuelSelection.single(trimmed);
    }

    private static int duelCompact(CommandSourceStack source, String spec) {
        ServerPlayer owner = source.getPlayer();
        String[] parts = splitCompactVs(spec);
        if (parts == null) {
            return error(owner, "Use /tames duel <left> vs <right>.");
        }
        CompactDuelSideParseResult left = parseCompactDuelSide(source, owner, parts[0]);
        if (!left.error.isBlank()) {
            return error(owner, left.error);
        }
        CompactDuelSideParseResult right = parseCompactDuelSide(source, owner, parts[1]);
        if (!right.error.isBlank()) {
            return error(owner, right.error);
        }
        if (!right.targetPlayerNames.isEmpty()
                && (right.selection.includeSelf || (right.selection.tameSelections != null && !right.selection.tameSelections.isEmpty()))) {
            return error(owner, "The inviting command cannot choose tames for the opposing side. Invite those players and let them accept with their own selection.");
        }
        if (left.targetPlayerNames.isEmpty() && right.targetPlayerNames.isEmpty()) {
            return duelStartCompactSameOwner(source, left.selection, right.selection);
        }
        return duelCreatePendingCompactMatch(source, left, right);
    }

    private static int duelAcceptCompact(CommandSourceStack source, String spec) {
        ServerPlayer targetPlayer = source.getPlayer();
        String[] parts = splitCompactVs(spec);
        if (parts == null) {
            return error(targetPlayer, "Use /tames duel accept <player> vs <your selection>.");
        }
        String challengerName = parts[0].trim();
        if (challengerName.isBlank()) {
            return error(targetPlayer, "Challenger player name required before vs.");
        }
        CompactDuelSideParseResult right = parseCompactDuelSide(source, targetPlayer, parts[1]);
        if (!right.error.isBlank()) {
            return error(targetPlayer, right.error);
        }
        return duelAcceptPendingCompactMatch(source, challengerName, right);
    }

    private static int duelStartCompactSameOwner(CommandSourceStack source, TeamSelection leftSelection, TeamSelection rightSelection) {
        ServerPlayer owner = source.getPlayer();
        TeamSelectionResult leftResult = resolveLoadedTeamSelection(source, owner, leftSelection);
        if (!leftResult.error.isBlank()) return error(owner, leftResult.error);
        TeamSelectionResult rightResult = resolveLoadedTeamSelection(source, owner, rightSelection);
        if (!rightResult.error.isBlank()) return error(owner, rightResult.error);
        if (leftResult.members.isEmpty()) return error(owner, "Left duel team has no loaded/alive members.");
        if (rightResult.members.isEmpty()) return error(owner, "Right duel team has no loaded/alive members.");

        Set<UUID> leftIds = collectLivingEntityIds(leftResult.members);
        Set<UUID> rightIds = collectLivingEntityIds(rightResult.members);
        if (!leftIds.isEmpty()) {
            rightResult.members.removeIf(member -> member != null && leftIds.contains(member.getUUID()));
            rightResult.tames.removeIf(tame -> tame != null && leftIds.contains(tame.getUUID()));
            rightIds.removeIf(leftIds::contains);
        }
        if (rightIds.isEmpty()) {
            return error(owner, "Right duel team has no loaded/alive members after removing overlaps.");
        }

        prepareTeamForDuel(leftResult.tames);
        prepareTeamForDuel(rightResult.tames);
        assignInitialDuelTargets(leftResult.tames, rightResult.members);
        assignInitialDuelTargets(rightResult.tames, leftResult.members);

        TameDuelManager.startTeamDuel(source.getServer(), owner.getUUID(), leftIds, owner.getUUID(), rightIds, Set.of(), false);
        notifyDuelSpectators(source.getServer(), owner.getUUID(), owner.getUUID(), DuelSpectators.empty(),
                Component.literal("Duel started: " + teamSelectionLabel(leftSelection) + " vs " + teamSelectionLabel(rightSelection) + ".").withStyle(ChatFormatting.RED));
        return 1;
    }

    private static int duelCreatePendingCompactMatch(CommandSourceStack source, CompactDuelSideParseResult left, CompactDuelSideParseResult right) {
        ServerPlayer owner = source.getPlayer();
        cleanupExpiredDuelInvites();
        if (playerHasAnyPendingDuel(owner.getUUID())) {
            return error(owner, "You already have a pending duel invitation or staged duel.");
        }
        PendingDuelMatch match = new PendingDuelMatch(UUID.randomUUID(), owner.getUUID(), DuelSpectators.empty(), System.currentTimeMillis());
        match.participants.put(owner.getUUID(), new PendingDuelParticipant(owner.getUUID(), true, owner.getUUID(), left.selection, true));
        PENDING_DUEL_MATCHES.put(match.matchId, match);
        PENDING_DUEL_MATCH_BY_PLAYER.put(owner.getUUID(), match.matchId);

        try {
            for (String playerName : left.targetPlayerNames) {
                ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(playerName);
                if (player == null) {
                    return cancelPendingMatch(source, match, "Player is not online: " + playerName + ".");
                }
                String conflict = addPendingDuelParticipant(source, match, player, true, owner.getUUID());
                if (!conflict.isBlank()) {
                    return cancelPendingMatch(source, match, conflict);
                }
            }
            for (String playerName : right.targetPlayerNames) {
                ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(playerName);
                if (player == null) {
                    return cancelPendingMatch(source, match, "Player is not online: " + playerName + ".");
                }
                String conflict = addPendingDuelParticipant(source, match, player, false, owner.getUUID());
                if (!conflict.isBlank()) {
                    return cancelPendingMatch(source, match, conflict);
                }
            }
        } catch (RuntimeException ex) {
            return cancelPendingMatch(source, match, ex.getMessage());
        }

        notifyPendingDuelInvites(source.getServer(), match, owner.getUUID());
        owner.sendSystemMessage(Component.literal("Created pending duel: " + teamSelectionLabel(left.selection) + " vs " + pendingDuelSideLabel(source.getServer(), match, false) + ".").withStyle(ChatFormatting.GREEN));
        return tryStartPendingDuel(source, match);
    }

    private static int duelAcceptPendingCompactMatch(CommandSourceStack source, String challengerName, CompactDuelSideParseResult acceptedSide) {
        ServerPlayer player = source.getPlayer();
        cleanupExpiredDuelInvites();
        PendingDuelMatch match = findPendingDuelMatchForInvite(source, player, challengerName);
        if (match == null) {
            return error(player, "No pending staged duel invite from " + challengerName + ".");
        }
        PendingDuelParticipant participant = match.participants.get(player.getUUID());
        if (participant == null) {
            return error(player, "You are not part of that pending duel.");
        }
        boolean sideA = participant.sideA;
        for (String playerName : acceptedSide.targetPlayerNames) {
            ServerPlayer ally = source.getServer().getPlayerList().getPlayerByName(playerName);
            if (ally == null) {
                return error(player, "Player is not online: " + playerName + ".");
            }
            PendingDuelParticipant existing = match.participants.get(ally.getUUID());
            if (existing != null) {
                if (existing.sideA != sideA) {
                    return error(player, ally.getName().getString() + " is already on the opposing side of this duel.");
                }
                continue;
            }
            String conflict = addPendingDuelParticipant(source, match, ally, sideA, player.getUUID());
            if (!conflict.isBlank()) {
                return error(player, conflict);
            }
        }
        participant.acceptedSelection = acceptedSide.selection;
        participant.accepted = true;
        player.sendSystemMessage(Component.literal("Accepted pending duel invite.").withStyle(ChatFormatting.GREEN));
        notifyPendingDuelInvites(source.getServer(), match, player.getUUID());
        return tryStartPendingDuel(source, match);
    }

    private static int tryStartPendingDuel(CommandSourceStack source, PendingDuelMatch match) {
        if (match == null) {
            return 0;
        }
        for (PendingDuelParticipant participant : match.participants.values()) {
            if (!participant.accepted || participant.acceptedSelection == null) {
                return 1;
            }
        }
        List<LivingEntity> sideAMembers = new ArrayList<>();
        List<LivingEntity> sideBMembers = new ArrayList<>();
        List<TamableAnimal> sideATames = new ArrayList<>();
        List<TamableAnimal> sideBTames = new ArrayList<>();
        Set<UUID> sideAOwners = new LinkedHashSet<>();
        Set<UUID> sideBOwners = new LinkedHashSet<>();
        for (PendingDuelParticipant participant : match.participants.values()) {
            ServerPlayer player = source.getServer().getPlayerList().getPlayer(participant.playerUuid);
            if (player == null) {
                return error(source.getPlayer(), "Pending duel cannot start because " + participant.playerUuid + " is offline.");
            }
            TeamSelectionResult resolved = resolveLoadedTeamSelection(source, player, participant.acceptedSelection);
            if (!resolved.error.isBlank()) {
                return error(source.getPlayer(), player.getName().getString() + ": " + resolved.error);
            }
            if (participant.sideA) {
                sideAMembers.addAll(resolved.members);
                sideATames.addAll(resolved.tames);
                sideAOwners.add(player.getUUID());
            } else {
                sideBMembers.addAll(resolved.members);
                sideBTames.addAll(resolved.tames);
                sideBOwners.add(player.getUUID());
            }
        }
        if (sideAMembers.isEmpty() || sideBMembers.isEmpty()) {
            return error(source.getPlayer(), "Pending duel cannot start with an empty side.");
        }
        Set<UUID> leftIds = collectLivingEntityIds(sideAMembers);
        Set<UUID> rightIds = collectLivingEntityIds(sideBMembers);
        if (!leftIds.isEmpty()) {
            sideBMembers.removeIf(member -> member != null && leftIds.contains(member.getUUID()));
            sideBTames.removeIf(tame -> tame != null && leftIds.contains(tame.getUUID()));
            rightIds.removeIf(leftIds::contains);
        }
        if (rightIds.isEmpty()) {
            return error(source.getPlayer(), "Pending duel cannot start with an empty side after removing overlaps.");
        }
        prepareTeamForDuel(sideATames);
        prepareTeamForDuel(sideBTames);
        assignInitialDuelTargets(sideATames, sideBMembers);
        assignInitialDuelTargets(sideBTames, sideAMembers);
        UUID ownerA = sideAOwners.iterator().next();
        UUID ownerB = sideBOwners.iterator().next();
        LinkedHashSet<UUID> runtimeSpectators = new LinkedHashSet<>(match.spectators.playerIds);
        for (UUID ownerId : sideAOwners) {
            if (!ownerId.equals(ownerA)) {
                runtimeSpectators.add(ownerId);
            }
        }
        for (UUID ownerId : sideBOwners) {
            if (!ownerId.equals(ownerB)) {
                runtimeSpectators.add(ownerId);
            }
        }
        TameDuelManager.startTeamDuel(source.getServer(), ownerA, leftIds, ownerB, rightIds, runtimeSpectators, match.spectators.broadcastToServer);
        notifyPendingDuelStart(source.getServer(), match);
        removePendingDuelMatch(match.matchId);
        return 1;
    }

    private static PendingDuelMatch findPendingDuelMatchForInvite(CommandSourceStack source, ServerPlayer player, String challengerName) {
        UUID matchId = PENDING_DUEL_MATCH_BY_PLAYER.get(player.getUUID());
        if (matchId == null) {
            return null;
        }
        PendingDuelMatch match = PENDING_DUEL_MATCHES.get(matchId);
        if (match == null) {
            PENDING_DUEL_MATCH_BY_PLAYER.remove(player.getUUID());
            return null;
        }
        ServerPlayer initiator = source.getServer().getPlayerList().getPlayer(match.initiatorUuid);
        String initiatorName = initiator == null ? resolveKnownOwnerName(source.getServer(), match.initiatorUuid, "") : initiator.getGameProfile().getName();
        if (initiatorName.equalsIgnoreCase(challengerName)) {
            return match;
        }
        for (PendingDuelParticipant participant : match.participants.values()) {
            String participantName = resolveKnownOwnerName(source.getServer(), participant.playerUuid, "");
            if (participantName.equalsIgnoreCase(challengerName)) {
                return match;
            }
            String invitedByName = resolveKnownOwnerName(source.getServer(), participant.invitedBy, "");
            if (invitedByName.equalsIgnoreCase(challengerName)) {
                return match;
            }
        }
        return null;
    }

    private static String addPendingDuelParticipant(CommandSourceStack source, PendingDuelMatch match, ServerPlayer player, boolean sideA, UUID invitedBy) {
        if (player == null || match == null) {
            return "Invalid pending duel participant.";
        }
        PendingDuelParticipant existing = match.participants.get(player.getUUID());
        if (existing != null) {
            return existing.sideA == sideA ? "" : player.getName().getString() + " is already on the opposing side.";
        }
        if (playerHasAnyPendingDuel(player.getUUID())) {
            return player.getName().getString() + " already has a pending duel invitation.";
        }
        match.participants.put(player.getUUID(), new PendingDuelParticipant(player.getUUID(), sideA, invitedBy, null, false));
        PENDING_DUEL_MATCH_BY_PLAYER.put(player.getUUID(), match.matchId);
        return "";
    }

    private static boolean playerHasAnyPendingDuel(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        if (PENDING_DUEL_MATCH_BY_PLAYER.containsKey(playerUuid)) {
            return true;
        }
        Map<UUID, DuelInvite> incoming = DUEL_INVITES.get(playerUuid);
        return incoming != null && !incoming.isEmpty();
    }

    private static int cancelPendingMatch(CommandSourceStack source, PendingDuelMatch match, String error) {
        removePendingDuelMatch(match == null ? null : match.matchId);
        return error(source.getPlayer(), error);
    }

    private static void removePendingDuelMatch(UUID matchId) {
        if (matchId == null) {
            return;
        }
        PendingDuelMatch removed = PENDING_DUEL_MATCHES.remove(matchId);
        if (removed == null) {
            return;
        }
        for (UUID participantId : removed.participants.keySet()) {
            UUID mapped = PENDING_DUEL_MATCH_BY_PLAYER.get(participantId);
            if (matchId.equals(mapped)) {
                PENDING_DUEL_MATCH_BY_PLAYER.remove(participantId);
            }
        }
    }

    private static void notifyPendingDuelInvites(MinecraftServer server, PendingDuelMatch match, UUID actorId) {
        if (server == null || match == null) {
            return;
        }
        String sideALabel = pendingDuelSideLabel(server, match, true);
        String sideBLabel = pendingDuelSideLabel(server, match, false);
        String initiatorName = resolveKnownOwnerName(server, match.initiatorUuid, "unknown");
        for (PendingDuelParticipant participant : match.participants.values()) {
            if (participant.accepted) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(participant.playerUuid);
            if (player == null) {
                continue;
            }
            String invitedByName = resolveKnownOwnerName(server, participant.invitedBy, initiatorName);
            player.sendSystemMessage(Component.literal(invitedByName + " invited you to a staged duel.").withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.literal("Side A: " + sideALabel).withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.literal("Side B: " + sideBLabel).withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("Accept: /tames duel accept " + initiatorName + " vs <your tames[, allyPlayer]>").withStyle(ChatFormatting.GREEN));
        }
        if (actorId != null) {
            ServerPlayer actor = server.getPlayerList().getPlayer(actorId);
            if (actor != null) {
                actor.sendSystemMessage(Component.literal("Pending duel roster updated. Waiting for all invited players to accept.").withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    private static String pendingDuelSideLabel(MinecraftServer server, PendingDuelMatch match, boolean sideA) {
        List<String> labels = new ArrayList<>();
        for (PendingDuelParticipant participant : match.participants.values()) {
            if (participant.sideA != sideA) {
                continue;
            }
            String playerName = resolveKnownOwnerName(server, participant.playerUuid, participant.playerUuid.toString());
            String status = participant.accepted && participant.acceptedSelection != null
                    ? teamSelectionLabel(participant.acceptedSelection)
                    : "pending";
            labels.add(playerName + " [" + status + "]");
        }
        return labels.isEmpty() ? "-" : String.join(", ", labels);
    }

    private static void notifyPendingDuelStart(MinecraftServer server, PendingDuelMatch match) {
        if (server == null || match == null) {
            return;
        }
        Component line = Component.literal("Duel started: " + pendingDuelSideLabel(server, match, true) + " vs " + pendingDuelSideLabel(server, match, false) + ".").withStyle(ChatFormatting.RED);
        for (UUID participantId : match.participants.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(participantId);
            if (player != null) {
                player.sendSystemMessage(line);
            }
        }
    }

    private static void notifyPendingDuelCancelled(MinecraftServer server, PendingDuelMatch match, UUID decliningPlayerId) {
        if (server == null || match == null) {
            return;
        }
        String decliningName = resolveKnownOwnerName(server, decliningPlayerId, "A player");
        Component line = Component.literal("Pending duel cancelled because " + decliningName + " declined the invitation.").withStyle(ChatFormatting.YELLOW);
        for (UUID participantId : match.participants.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(participantId);
            if (player != null) {
                player.sendSystemMessage(line);
            }
        }
    }

    private static String[] splitCompactVs(String spec) {
        if (spec == null) {
            return null;
        }
        String trimmed = spec.trim();
        int marker = trimmed.toLowerCase(Locale.ROOT).indexOf(" vs ");
        if (marker < 0) {
            return null;
        }
        String left = trimmed.substring(0, marker).trim();
        String right = trimmed.substring(marker + 4).trim();
        if (left.isBlank() || right.isBlank()) {
            return null;
        }
        return new String[]{left, right};
    }

    private static DuelSpectatorParseResult parseDuelSpectators(CommandSourceStack source, String raw, UUID... excludedIds) {
        if (raw == null || raw.trim().isBlank()) {
            return DuelSpectatorParseResult.ok(DuelSpectators.empty());
        }
        if (source == null || source.getServer() == null) {
            return DuelSpectatorParseResult.fail("Server unavailable.");
        }
        Set<UUID> excluded = new HashSet<>();
        if (excludedIds != null) {
            for (UUID excludedId : excludedIds) {
                if (excludedId != null) {
                    excluded.add(excludedId);
                }
            }
        }
        boolean broadcastToServer = false;
        LinkedHashSet<UUID> spectatorIds = new LinkedHashSet<>();
        String[] tokens = raw.trim().split("[,\\s]+");
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            if (token.equalsIgnoreCase("server")) {
                broadcastToServer = true;
                continue;
            }
            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(token);
            if (player == null) {
                return DuelSpectatorParseResult.fail("Spectator player is not online: " + token + ".");
            }
            if (excluded.contains(player.getUUID())) {
                continue;
            }
            spectatorIds.add(player.getUUID());
        }
        return DuelSpectatorParseResult.ok(new DuelSpectators(broadcastToServer, spectatorIds));
    }

    private static DuelSpectators mergeDuelSpectators(DuelSpectators first, DuelSpectators second) {
        if ((first == null || first.isEmpty()) && (second == null || second.isEmpty())) {
            return DuelSpectators.empty();
        }
        boolean broadcastToServer = (first != null && first.broadcastToServer) || (second != null && second.broadcastToServer);
        LinkedHashSet<UUID> merged = new LinkedHashSet<>();
        if (first != null) {
            merged.addAll(first.playerIds);
        }
        if (second != null) {
            merged.addAll(second.playerIds);
        }
        return new DuelSpectators(broadcastToServer, merged);
    }

    private static String duelSpectatorLabel(MinecraftServer server, DuelSpectators spectators) {
        if (spectators == null || spectators.isEmpty()) {
            return "";
        }
        List<String> labels = new ArrayList<>();
        if (spectators.broadcastToServer) {
            labels.add("server");
        }
        for (UUID spectatorId : spectators.playerIds) {
            String name = resolveKnownOwnerName(server, spectatorId, "");
            if (name.isBlank()) {
                ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(spectatorId);
                name = player == null ? spectatorId.toString() : player.getGameProfile().getName();
            }
            labels.add(name);
        }
        return labels.isEmpty() ? "" : " Spectators: " + String.join(", ", labels) + ".";
    }

    private static void notifyDuelSpectators(MinecraftServer server, UUID ownerA, UUID ownerB, DuelSpectators spectators, Component line) {
        if (server == null || line == null) {
            return;
        }
        LinkedHashSet<UUID> recipients = new LinkedHashSet<>();
        if (ownerA != null) {
            recipients.add(ownerA);
        }
        if (ownerB != null) {
            recipients.add(ownerB);
        }
        if (spectators != null) {
            if (spectators.broadcastToServer) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    recipients.add(player.getUUID());
                }
            }
            recipients.addAll(spectators.playerIds);
        }
        for (UUID recipientId : recipients) {
            ServerPlayer player = server.getPlayerList().getPlayer(recipientId);
            if (player != null) {
                player.sendSystemMessage(line);
            }
        }
    }

    private static String joinSelectionTokens(String[] tokens, int startIndex) {
        if (tokens == null || startIndex < 0 || startIndex >= tokens.length) return "";
        return String.join(" ", Arrays.copyOfRange(tokens, startIndex, tokens.length)).trim();
    }

    private static String invalidTeamSelectionMessage() {
        return "Invalid team selection. Use: myself, all, group <name>, type <name>, state <follow|sit|wander>, follow, sit, wander, name <pet>, or comma-separated mixes like 'type wolf, rex'.";
    }

    private static Set<UUID> collectLivingEntityIds(List<? extends LivingEntity> members) {
        Set<UUID> ids = new HashSet<>();
        if (members == null) return ids;
        for (LivingEntity member : members) {
            if (member == null || !member.isAlive()) continue;
            ids.add(member.getUUID());
        }
        return ids;
    }

    private static void prepareTeamForDuel(List<TamableAnimal> tames) {
        if (tames == null) return;
        for (TamableAnimal tame : tames) {
            applySitFollowOverride(tame, false);
        }
    }

    private static void assignInitialDuelTargets(List<TamableAnimal> actingTames, List<? extends LivingEntity> opponents) {
        if (actingTames == null || opponents == null) return;
        for (TamableAnimal tame : actingTames) {
            LivingEntity enemy = nearestLoadedLivingOpponent(tame, opponents);
            if (enemy != null) {
                tame.setTarget(enemy);
            }
        }
    }

    private static String duelSelectionLabel(DuelSelection selection) {
        if (selection == null) return "selected tames";
        return switch (selection.kind) {
            case GROUP -> "group " + selection.value;
            case TYPE -> "type " + selection.value;
            case STATE -> "state " + selection.value;
            case SINGLE -> "tame " + selection.value;
            case ALL -> "all loaded tames";
        };
    }

    private static String sameOwnerDuelSelectionLabel(DuelSelection leftSelection, DuelSelection rightSelection, boolean leftSide) {
        DuelSelection current = leftSide ? leftSelection : rightSelection;
        DuelSelection other = leftSide ? rightSelection : leftSelection;
        if (current != null && current.kind == DuelSelectionKind.ALL && other != null && other.kind != DuelSelectionKind.ALL) {
            return "rest";
        }
        return duelSelectionLabel(current);
    }

    private static DuelSelection firstTeamDuelSelection(TeamSelection selection) {
        if (selection == null || selection.tameSelections == null || selection.tameSelections.isEmpty()) {
            return null;
        }
        return selection.tameSelections.get(0);
    }

    private static String teamSelectionLabel(TeamSelection selection) {
        if (selection == null) return "selected team";
        if (selection.includeSelf && (selection.tameSelections == null || selection.tameSelections.isEmpty())) {
            return "myself";
        }
        List<String> parts = new ArrayList<>();
        if (selection.includeSelf) {
            parts.add("myself");
        }
        if (selection.tameSelections != null) {
            for (DuelSelection tameSelection : selection.tameSelections) {
                parts.add(duelSelectionLabel(tameSelection));
            }
        }
        return parts.isEmpty() ? "selected team" : String.join(", ", parts);
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

    private static List<TameData> ownedGuardianType(UUID owner, String typeFilter) {
        List<TameData> list = new ArrayList<>();
        for (TameData data : ownedType(owner, typeFilter)) {
            if (data != null && data.hasHome) {
                list.add(data);
            }
        }
        return list;
    }

    private static List<TameData> ownedDeadGuardianType(UUID owner, String typeFilter) {
        List<TameData> list = new ArrayList<>();
        for (TameData data : ownedDeadType(owner, typeFilter)) {
            if (data != null && data.hasHome) {
                list.add(data);
            }
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
            if (isInactiveEntry(d.uuid)) continue;
            if (!d.name.equalsIgnoreCase(name)) continue;
            if (best == null
                    || d.level > best.level
                    || (d.level == best.level && String.valueOf(d.uuid).compareTo(String.valueOf(best.uuid)) < 0)) {
                best = d;
            }
        }
        return best;
    }

    private static LivingEntity nearestLoadedLivingOpponent(TamableAnimal from, List<? extends LivingEntity> opponents) {
        if (from == null || opponents == null || opponents.isEmpty()) return null;
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity opponent : opponents) {
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

    private static TameData findOwnedTameAny(UUID owner, String name) {
        TameData alive = findOwnedTame(owner, name);
        if (alive != null) {
            return alive;
        }
        TameData stored = findOwnedStoredTame(owner, name);
        if (stored != null) {
            return stored;
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

    private static TameData findOwnedStoredTame(UUID owner, String name) {
        if (owner == null || name == null) return null;
        TameData best = null;
        for (TameData d : TameRegistry.TAMES.values()) {
            if (d == null || d.name == null) continue;
            if (!owner.equals(d.ownerUUID)) continue;
            if (!d.stored) continue;
            if (!d.name.equalsIgnoreCase(name)) continue;
            if (best == null
                    || d.level > best.level
                    || (d.level == best.level && String.valueOf(d.uuid).compareTo(String.valueOf(best.uuid)) < 0)) {
                best = d;
            }
        }
        return best;
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

    private static boolean isStoredEntry(UUID tameUuid) {
        if (tameUuid == null) return false;
        TameData data = TameRegistry.get(tameUuid);
        return data != null && data.stored;
    }

    private static boolean isInactiveEntry(UUID tameUuid) {
        if (tameUuid == null) return false;
        TameData data = TameRegistry.get(tameUuid);
        if (data != null) {
            return data.isInactive();
        }
        return TameRegistry.LAST_DEATHS.containsKey(tameUuid);
    }

    private static String inactiveSuffix(TameData data) {
        if (data == null) {
            return "";
        }
        if (data.stored) {
            return " [STORED]";
        }
        if (data.dead) {
            return " [DEAD]";
        }
        return "";
    }

    private static String statusLabel(TameData data, MinecraftServer server) {
        if (data == null) {
            return "unknown";
        }
        if (data.stored) {
            return "stored";
        }
        if (data.dead) {
            return "dead";
        }
        return isLoadedAnywhere(server, data.uuid) ? "loaded" : "unloaded";
    }

    private static ChatFormatting statusColor(TameData data, boolean loaded) {
        if (data == null) {
            return ChatFormatting.WHITE;
        }
        if (data.stored) {
            return ChatFormatting.LIGHT_PURPLE;
        }
        if (data.dead) {
            return ChatFormatting.GRAY;
        }
        return loaded ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
    }

    private static int statusOrder(TameData data, MinecraftServer server) {
        if (data == null) {
            return 4;
        }
        if (data.stored) {
            return 2;
        }
        if (data.dead) {
            return 3;
        }
        return isLoadedAnywhere(server, data.uuid) ? 0 : 1;
    }

    private static long daysAlive(CommandSourceStack source, TameData data) {
        if (data == null) return 0L;
        long now = source.getServer().overworld().getDayTime();
        long start = survivalStartDayTime(data);
        if (start < 0L) return Math.max(0, data.activeSurvivalDays);
        long ticks = Math.max(0L, now - start);
        long days = ticks / 24000L;
        return Math.max(days, Math.max(0, data.activeSurvivalDays));
    }

    private static long survivalStartDayTime(TameData data) {
        long latestDeath = -1L;
        if (data != null) {
            for (CompoundTag row : data.deathHistory) {
                if (row == null || row.isEmpty() || !row.contains("gameTime", Tag.TAG_LONG)) {
                    continue;
                }
                latestDeath = Math.max(latestDeath, row.getLong("gameTime"));
            }
            if (latestDeath >= 0L) {
                return latestDeath;
            }
            return data.bornDayTime;
        }
        return 0L;
    }

    private static String latestDeathMessage(TameData data) {
        if (data == null || data.deathHistory.isEmpty()) {
            return "";
        }
        CompoundTag latest = null;
        long latestTime = Long.MIN_VALUE;
        for (CompoundTag row : data.deathHistory) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            long time = row.contains("unixMillis", Tag.TAG_LONG) ? row.getLong("unixMillis")
                    : row.contains("gameTime", Tag.TAG_LONG) ? row.getLong("gameTime")
                    : Long.MIN_VALUE;
            if (latest == null || time >= latestTime) {
                latest = row;
                latestTime = time;
            }
        }
        if (latest == null || !latest.contains("message", Tag.TAG_STRING)) {
            return "";
        }
        return latest.getString("message");
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
            if (isInactiveEntry(d.uuid)) continue;
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
            if (isInactiveEntry(d.uuid)) continue;
            suggestCommandString(b, d.name);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestPlayerOwnedPetNames(CommandSourceStack source, String playerName, SuggestionsBuilder b) {
        UUID ownerId = resolveKnownOwnerUuid(source == null ? null : source.getServer(), playerName);
        if (ownerId == null) return b.buildFuture();
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.name == null || data.name.isBlank()) continue;
            if (!ownerId.equals(data.ownerUUID)) continue;
            if (isInactiveEntry(data.uuid)) continue;
            suggestCommandString(b, data.name);
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

    private static CompletableFuture<Suggestions> suggestPlayerOwnedGroups(CommandSourceStack source, String playerName, SuggestionsBuilder b) {
        UUID ownerId = resolveKnownOwnerUuid(source == null ? null : source.getServer(), playerName);
        if (ownerId == null) return b.buildFuture();
        Set<String> seen = new HashSet<>();
        for (String group : TameRegistry.getOwnerGroups(ownerId)) {
            if (group == null || group.isBlank()) continue;
            if (seen.add(group)) suggestCommandString(b, group);
        }
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || !ownerId.equals(data.ownerUUID)) continue;
            for (String group : groupMemberships(data)) {
                if (seen.add(group)) suggestCommandString(b, group);
            }
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestPlayerOwnedTypes(CommandSourceStack source, String playerName, SuggestionsBuilder b) {
        UUID ownerId = resolveKnownOwnerUuid(source == null ? null : source.getServer(), playerName);
        if (ownerId == null) return b.buildFuture();
        Set<String> seenPath = new HashSet<>();
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || !ownerId.equals(data.ownerUUID)) continue;
            if (isDeadEntry(data.uuid)) continue;
            String full = tameTypeId(data);
            if (full.isBlank()) continue;
            int sep = full.indexOf(':');
            String path = sep >= 0 ? full.substring(sep + 1) : full;
            if (!path.isBlank() && seenPath.add(path)) suggestCommandString(b, path);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOwnedPetNamesWithBeds(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return b.buildFuture();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            if (isInactiveEntry(d.uuid)) continue;
            if (!d.hasPetBed || d.petBedDimension == null || d.petBedDimension.isBlank()) continue;
            suggestCommandString(b, d.name);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOwnedUnloadedPetNames(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return b.buildFuture();
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            if (isInactiveEntry(d.uuid)) continue;
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
            suggestCommandString(b, player.getGameProfile().getName());
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestDuelSpectators(CommandSourceStack source, SuggestionsBuilder b) {
        suggestCommandString(b, "server");
        return suggestOnlinePlayers(source, b);
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
            suggestCommandString(b, name);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestIncomingDuelChallengers(CommandSourceStack source, SuggestionsBuilder b) {
        return suggestIncomingDuelChallengers(source, b, DUEL_INVITES);
    }

    private static CompletableFuture<Suggestions> suggestIncomingDuelChallengers(CommandSourceStack source, SuggestionsBuilder b, Map<UUID, Map<UUID, DuelInvite>> inviteStore) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return b.buildFuture();
        cleanupExpiredDuelInviteStore(inviteStore);
        Map<UUID, DuelInvite> incoming = inviteStore.get(p.getUUID());
        if (incoming == null || incoming.isEmpty()) return b.buildFuture();
        for (UUID challengerId : incoming.keySet()) {
            ServerPlayer challenger = source.getServer().getPlayerList().getPlayer(challengerId);
            if (challenger != null) {
                suggestCommandString(b, challenger.getGameProfile().getName());
            }
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOwnedGroups(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return b.buildFuture();
        Set<String> seen = new HashSet<>();
        for (String group : TameRegistry.getOwnerGroups(p.getUUID())) {
            if (group == null || group.isBlank()) continue;
            if (seen.add(group)) suggestCommandString(b, group);
        }
        for (TameData d : TameRegistry.TAMES.values()) {
            if (!p.getUUID().equals(d.ownerUUID)) continue;
            for (String group : groupMemberships(d)) {
                if (seen.add(group)) suggestCommandString(b, group);
            }
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
            if (!path.isBlank() && seenPath.add(path)) suggestCommandString(b, path);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestEntityTypes(SuggestionsBuilder b) {
        for (ResourceLocation id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            suggestCommandString(b, id.toString());
            if ("minecraft".equals(id.getNamespace())) {
                suggestCommandString(b, id.getPath());
            }
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOwnedGuardianSetNames(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return b.buildFuture();
        Set<String> seen = new HashSet<>();
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || !player.getUUID().equals(data.ownerUUID)) continue;
            for (String setName : data.guardianSetAnchors.keySet()) {
                if (setName != null && !setName.isBlank() && seen.add(setName)) {
                    suggestCommandString(b, setName);
                }
            }
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestTeamSelectionSpecs(CommandSourceStack source, SuggestionsBuilder b) {
        suggestCommandString(b, "myself");
        suggestCommandString(b, "myself all");
        suggestCommandString(b, "all");
        suggestCommandString(b, "type wolf, rex");
        suggestCommandString(b, "rex, roxi");
        suggestCommandString(b, "follow");
        suggestCommandString(b, "sit");
        suggestCommandString(b, "wander");
        suggestCommandString(b, "state follow");
        suggestCommandString(b, "state sit");
        suggestCommandString(b, "state wander");
        suggestCommandString(b, "group ");
        suggestCommandString(b, "type ");
        suggestCommandString(b, "name ");
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestCompactDuelSpec(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer owner = source.getPlayer();
        if (owner == null) {
            return b.buildFuture();
        }
        String remaining = b.getRemaining();
        int vsIndex = compactVsIndex(remaining);
        if (vsIndex < 0) {
            suggestCompactDuelSide(source, owner, b, remaining, true);
            return b.buildFuture();
        }
        SuggestionsBuilder rightBuilder = b.createOffset(b.getStart() + vsIndex + 4);
        suggestCompactDuelSide(source, owner, rightBuilder, remaining.substring(vsIndex + 4), false);
        return rightBuilder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestCompactDuelAcceptSpec(CommandSourceStack source, SuggestionsBuilder b) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return b.buildFuture();
        }
        String remaining = b.getRemaining();
        int vsIndex = compactVsIndex(remaining);
        if (vsIndex < 0) {
            suggestIncomingDuelChallengers(source, b);
            if (isExactIncomingDuelChallenger(source, remaining.trim())) {
                SuggestionsBuilder tail = b.createOffset(b.getStart() + remaining.length());
                tail.suggest(" vs ");
                return tail.buildFuture();
            }
            return b.buildFuture();
        }
        SuggestionsBuilder rightBuilder = b.createOffset(b.getStart() + vsIndex + 4);
        suggestCompactDuelSide(source, player, rightBuilder, remaining.substring(vsIndex + 4), false);
        return rightBuilder.buildFuture();
    }

    private static void suggestCompactDuelSide(CommandSourceStack source, ServerPlayer owner, SuggestionsBuilder builder, String rawSide, boolean allowVs) {
        if (owner == null || builder == null) {
            return;
        }
        String side = rawSide == null ? "" : rawSide;
        int commaIndex = side.lastIndexOf(',');
        int segmentStart = commaIndex >= 0 ? commaIndex + 1 : 0;
        while (segmentStart < side.length() && Character.isWhitespace(side.charAt(segmentStart))) {
            segmentStart++;
        }
        String term = side.substring(segmentStart);
        SuggestionsBuilder termBuilder = builder.createOffset(builder.getStart() + segmentStart);
        String trimmedTerm = term.trim();
        if (trimmedTerm.isBlank()) {
            suggestCompactDuelBaseTerms(source, owner, termBuilder);
            return;
        }
        String lower = trimmedTerm.toLowerCase(Locale.ROOT);
        if (lower.startsWith("group ")) {
            SuggestionsBuilder groupBuilder = termBuilder.createOffset(termBuilder.getStart() + trimmedTerm.indexOf(' ') + 1);
            suggestOwnedGroups(source, groupBuilder);
            return;
        }
        if (lower.startsWith("type ")) {
            SuggestionsBuilder typeBuilder = termBuilder.createOffset(termBuilder.getStart() + trimmedTerm.indexOf(' ') + 1);
            suggestOwnedTypes(source, typeBuilder);
            return;
        }
        if (lower.startsWith("name ")) {
            SuggestionsBuilder nameBuilder = termBuilder.createOffset(termBuilder.getStart() + trimmedTerm.indexOf(' ') + 1);
            suggestOwnedPetNamesAll(source, nameBuilder);
            return;
        }
        if (lower.startsWith("state ")) {
            SuggestionsBuilder stateBuilder = termBuilder.createOffset(termBuilder.getStart() + trimmedTerm.indexOf(' ') + 1);
            suggestMovementStates(stateBuilder);
            return;
        }
        if (lower.equals("group")) {
            SuggestionsBuilder tailBuilder = termBuilder.createOffset(termBuilder.getStart() + trimmedTerm.length());
            tailBuilder.suggest(" ");
            return;
        }
        if (lower.equals("type")) {
            SuggestionsBuilder tailBuilder = termBuilder.createOffset(termBuilder.getStart() + trimmedTerm.length());
            tailBuilder.suggest(" ");
            return;
        }
        if (lower.equals("name")) {
            SuggestionsBuilder tailBuilder = termBuilder.createOffset(termBuilder.getStart() + trimmedTerm.length());
            tailBuilder.suggest(" ");
            return;
        }
        if (lower.equals("state")) {
            SuggestionsBuilder tailBuilder = termBuilder.createOffset(termBuilder.getStart() + trimmedTerm.length());
            tailBuilder.suggest(" ");
            return;
        }

        suggestCompactDuelBaseTerms(source, owner, termBuilder);
        if (isValidCompactDuelSideTerm(source, owner, trimmedTerm)) {
            SuggestionsBuilder tailBuilder = builder.createOffset(builder.getStart() + side.length());
            tailBuilder.suggest(", ");
            if (allowVs) {
                tailBuilder.suggest(" vs ");
            }
        }
    }

    private static void suggestCompactDuelBaseTerms(CommandSourceStack source, ServerPlayer owner, SuggestionsBuilder builder) {
        suggestCommandString(builder, "group");
        suggestCommandString(builder, "type");
        suggestCommandString(builder, "name");
        suggestCommandString(builder, "state");
        suggestCommandString(builder, "all");
        suggestCommandString(builder, "follow");
        suggestCommandString(builder, "sit");
        suggestCommandString(builder, "wander");
        suggestCommandString(builder, owner.getGameProfile().getName());
        suggestOwnedPetNamesAll(source, builder);
        for (ServerPlayer other : source.getServer().getPlayerList().getPlayers()) {
            if (other != null && !other.getUUID().equals(owner.getUUID())) {
                suggestCommandString(builder, other.getGameProfile().getName());
            }
        }
    }

    private static boolean isValidCompactDuelSideTerm(CommandSourceStack source, ServerPlayer owner, String term) {
        if (owner == null || term == null || term.isBlank()) {
            return false;
        }
        if (term.equalsIgnoreCase(owner.getGameProfile().getName()) || term.equalsIgnoreCase("myself")) {
            return true;
        }
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(term);
        if (online != null && !online.getUUID().equals(owner.getUUID())) {
            return true;
        }
        DuelSelection selection = parseCompactDuelSelector(term);
        if (selection == null) {
            return false;
        }
        return switch (selection.kind) {
            case ALL -> true;
            case GROUP -> !selection.value.isBlank();
            case TYPE -> !selection.value.isBlank();
            case STATE -> !selection.value.isBlank();
            case SINGLE -> findOwnedTame(owner.getUUID(), selection.value) != null;
        };
    }

    private static boolean isExactIncomingDuelChallenger(CommandSourceStack source, String name) {
        return isExactIncomingDuelChallenger(source, name, DUEL_INVITES);
    }

    private static boolean isExactIncomingDuelChallenger(CommandSourceStack source, String name, Map<UUID, Map<UUID, DuelInvite>> inviteStore) {
        if (source == null || name == null || name.isBlank()) {
            return false;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return false;
        }
        cleanupExpiredDuelInviteStore(inviteStore);
        Map<UUID, DuelInvite> incoming = inviteStore.get(player.getUUID());
        if (incoming == null || incoming.isEmpty()) {
            return false;
        }
        for (UUID challengerId : incoming.keySet()) {
            ServerPlayer challenger = source.getServer().getPlayerList().getPlayer(challengerId);
            if (challenger != null && challenger.getGameProfile().getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static int compactVsIndex(String spec) {
        if (spec == null || spec.isBlank()) {
            return -1;
        }
        return spec.toLowerCase(Locale.ROOT).indexOf(" vs ");
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
        for (TameMode mode : TameMode.values()) suggestCommandString(b, mode.key());
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestMovementProfiles(SuggestionsBuilder b) {
        suggestCommandString(b, "default");
        suggestCommandString(b, "skeleton");
        suggestCommandString(b, "close");
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestMovementStates(SuggestionsBuilder b) {
        suggestCommandString(b, "follow");
        suggestCommandString(b, "wander");
        suggestCommandString(b, "sit");
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestInfoTopics(SuggestionsBuilder b) {
        suggestCommandString(b, "leaderboard");
        suggestCommandString(b, "deaths");
        suggestCommandString(b, "loaded");
        suggestCommandString(b, "stat");
        suggestCommandString(b, "group");
        suggestCommandString(b, "mode");
        suggestCommandString(b, "follow");
        suggestCommandString(b, "sit");
        suggestCommandString(b, "wander");
        suggestCommandString(b, "guardian");
        suggestCommandString(b, "tool guardian");
        suggestCommandString(b, "tool bone");
        suggestCommandString(b, "movement");
        suggestCommandString(b, "tp");
        suggestCommandString(b, "tphome");
        suggestCommandString(b, "respawn");
        suggestCommandString(b, "arise");
        suggestCommandString(b, "graveyard");
        suggestCommandString(b, "reincarnate");
        suggestCommandString(b, "inspect");
        suggestCommandString(b, "search");
        suggestCommandString(b, "duel");
        suggestCommandString(b, "debug");
        suggestCommandString(b, "attribute");
        suggestCommandString(b, "ability");
        suggestCommandString(b, "class");
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAbilities(SuggestionsBuilder b) {
        LevelSystem.knownAbilityIds().forEach(id -> suggestCommandString(b, id));
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAdminAbilities(SuggestionsBuilder b) {
        for (String id : LevelSystem.knownAbilityIds()) {
            if ("berserker".equalsIgnoreCase(id) || "passive".equalsIgnoreCase(id)) continue;
            suggestCommandString(b, id);
        }
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAttributes(SuggestionsBuilder b) {
        LevelSystem.knownAttributeIds().forEach(id -> suggestCommandString(b, id));
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestClasses(SuggestionsBuilder b) {
        classNames().forEach(name -> suggestCommandString(b, name));
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
        String remaining = b.getRemainingLowerCase();
        String candidate = value.toLowerCase(Locale.ROOT);
        if (!remaining.isBlank() && !candidate.startsWith(remaining)) {
            return;
        }
        b.suggest(StringArgumentType.escapeIfRequired(value));
    }

    private static List<String> classNames() {
        List<String> names = new ArrayList<>();
        for (TameClass value : TameClass.values()) {
            names.add(value.id());
        }
        return names;
    }

    private static CompletableFuture<Suggestions> suggestLeaderboardModes(SuggestionsBuilder b) {
        suggestCommandString(b, "mix");
        suggestCommandString(b, "kills");
        suggestCommandString(b, "deaths");
        suggestCommandString(b, "assists");
        suggestCommandString(b, "lvl");
        suggestCommandString(b, "days");
        return b.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestLeaderboardTameTypes(SuggestionsBuilder b) {
        Set<String> seenPath = new HashSet<>();
        for (TameData data : TameRegistry.TAMES.values()) {
            String full = tameTypeId(data);
            if (full.isBlank()) continue;
            int sep = full.indexOf(':');
            String path = sep >= 0 ? full.substring(sep + 1) : full;
            if (!path.isBlank() && seenPath.add(path)) suggestCommandString(b, path);
        }
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
