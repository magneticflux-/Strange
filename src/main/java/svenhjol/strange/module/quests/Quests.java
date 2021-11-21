package svenhjol.strange.module.quests;

import com.mojang.brigadier.CommandDispatcher;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DimensionDataStorage;
import svenhjol.charm.annotation.CommonModule;
import svenhjol.charm.helper.LogHelper;
import svenhjol.charm.helper.NetworkHelper;
import svenhjol.charm.loader.CharmModule;
import svenhjol.charm.loader.CommonLoader;
import svenhjol.strange.Strange;
import svenhjol.strange.module.journals.Journals;
import svenhjol.strange.module.knowledge.Knowledge.Tier;
import svenhjol.strange.module.quests.QuestToast.QuestToastType;

import javax.annotation.Nullable;
import java.util.*;

@CommonModule(mod = Strange.MOD_ID)
public class Quests extends CharmModule {
    public static final ResourceLocation MSG_SERVER_SYNC_PLAYER_QUESTS = new ResourceLocation(Strange.MOD_ID, "server_sync_player_quests");
    public static final ResourceLocation MSG_SERVER_ABANDON_QUEST = new ResourceLocation(Strange.MOD_ID, "server_abandon_quest");
    public static final ResourceLocation MSG_SERVER_PAUSE_QUEST = new ResourceLocation(Strange.MOD_ID, "server_pause_quest");
    public static final ResourceLocation MSG_CLIENT_SHOW_QUEST_TOAST = new ResourceLocation(Strange.MOD_ID, "client_show_quest_toast");
    public static final ResourceLocation MSG_CLIENT_SYNC_PLAYER_QUESTS = new ResourceLocation(Strange.MOD_ID, "client_sync_player_quests");

    public static final int NUM_TIERS = 6;
    public static final String DEFINITION_FOLDER = "quest_definitions";
    public static final Map<Integer, Map<String, QuestDefinition>> DEFINITIONS = new HashMap<>();
    public static final Map<Integer, String> TIER_NAMES;
    public static final Map<Integer, ScrollItem> SCROLLS = new HashMap<>();

    private static QuestData quests;

    @Override
    public void register() {
        for (int tier = 0; tier < NUM_TIERS; tier++) {
            SCROLLS.put(tier, new ScrollItem(this, tier));
        }

        ServerPlayNetworking.registerGlobalReceiver(MSG_SERVER_SYNC_PLAYER_QUESTS, this::handleSyncPlayerQuests);
        ServerPlayNetworking.registerGlobalReceiver(MSG_SERVER_ABANDON_QUEST, this::handleAbandonQuest);
        ServerPlayNetworking.registerGlobalReceiver(MSG_SERVER_PAUSE_QUEST, this::handlePauseQuest);

        CommandRegistrationCallback.EVENT.register(this::handleRegisterCommand);
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(this::handleKilledEntity);
    }

    @Override
    public void runWhenEnabled() {
        ServerWorldEvents.LOAD.register(this::handleWorldLoad);
    }

    public static void sendToast(ServerPlayer player, QuestToastType type, String definitionId, int tier) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeEnum(type);
        buffer.writeUtf(definitionId);
        buffer.writeInt(tier);
        ServerPlayNetworking.send(player, Quests.MSG_CLIENT_SHOW_QUEST_TOAST, buffer);
    }

    public static Optional<QuestData> getQuestData() {
        return Optional.ofNullable(quests);
    }

    @Nullable
    public static QuestDefinition getDefinition(String id) {
        String[] split;
        String tierName;

        if (id.contains("/")) {
            id = id.replace("/", ".");
        }

        if (!id.contains(".")) {
            return null;
        }

        split = id.split("\\.");
        tierName = split[0];
        int tier = getTierByName(tierName);

        if (DEFINITIONS.containsKey(tier) && DEFINITIONS.get(tier).containsKey(id)) {
            return DEFINITIONS.get(tier).get(id);
        }

        return null;
    }

    public static int getTierByName(String name) {
        for (Map.Entry<Integer, String> tier : TIER_NAMES.entrySet()) {
            if (tier.getValue().equals(name)) {
                return tier.getKey();
            }
        }
        return 0;
    }

    private void handleKilledEntity(ServerLevel level, Entity attacker, LivingEntity target) {
        getQuestData().ifPresent(quests -> quests.eachQuest(q -> q.entityKilled(target, attacker)));
    }

    private void handleRegisterCommand(CommandDispatcher<CommandSourceStack> dispatcher, boolean dedicated) {
        QuestCommand.register(dispatcher);
    }

    private void handleWorldLoad(MinecraftServer server, Level level) {
        if (level.dimension() == Level.OVERWORLD) {
            // load all quest definitions
            ResourceManager manager = server.getResourceManager();
            Map<ResourceLocation, CharmModule> allModules = CommonLoader.getAllModules();

            for (int tier = 0; tier < NUM_TIERS; tier++) {
                Collection<ResourceLocation> definitions = manager.listResources(DEFINITION_FOLDER + "/" + TIER_NAMES.get(tier), file -> file.endsWith(".json"));
                for (ResourceLocation resource : definitions) {
                    try {
                        QuestDefinition definition = QuestDefinition.deserialize(manager.getResource(resource));

                        // check definition module requirements
                        List<String> requiredModules = definition.getModules();
                        if (!requiredModules.isEmpty()) {
                            boolean skip = false;
                            for (String module : requiredModules) {
                                ResourceLocation modres = new ResourceLocation(module);
                                if (!allModules.containsKey(modres)) {
                                    LogHelper.info(this.getClass(), "Quest definition " + definition.getId() + " requires module " + modres + ", skipping");
                                    skip = true;
                                    break;
                                }
                            }

                            if (skip) continue;
                        }

                        String id = resource.getPath()
                            .replace(DEFINITION_FOLDER, "")
                            .replace("/", ".")
                            .replace(".json", "");

                        if (id.startsWith(".")) {
                            id = id.substring(1);
                        }

                        definition.setId(id);
                        definition.setTier(tier);

                        DEFINITIONS.computeIfAbsent(tier, a -> new HashMap<>()).put(id, definition);
                        LogHelper.debug(this.getClass(), "Loaded quest definition " + resource + " for tier " + tier);
                    } catch (Exception e) {
                        LogHelper.warn(this.getClass(), "Could not load quest definition for " + resource.toString() + ": " + e.getMessage());
                    }
                }
            }

            // setup the data storage
            ServerLevel serverLevel = (ServerLevel)level;
            DimensionDataStorage storage = serverLevel.getDataStorage();

            quests = storage.computeIfAbsent(
                nbt -> QuestData.fromNbt(serverLevel, nbt),
                () -> new QuestData(serverLevel),
                QuestData.getFileId(serverLevel.getLevel().dimensionType()));

            LogHelper.info(this.getClass(), "Loaded quests saved data");
        }
    }

    private void handleSyncPlayerQuests(MinecraftServer server, ServerPlayer player, ServerGamePacketListener listener, FriendlyByteBuf buffer, PacketSender sender) {
        syncPlayerQuests(player);
    }

    private void handleAbandonQuest(MinecraftServer server, ServerPlayer player, ServerGamePacketListener listener, FriendlyByteBuf buffer, PacketSender sender) {
        String questId = buffer.readUtf();
        server.execute(() -> Quests.getQuestData().flatMap(quests -> quests.get(questId)).ifPresent(quest -> {
            quest.abandon(player);
            Journals.sendOpenJournal(player, Journals.Page.QUESTS);
        }));
    }

    private void handlePauseQuest(MinecraftServer server, ServerPlayer player, ServerGamePacketListener listener, FriendlyByteBuf buffer, PacketSender sender) {
        String questId = buffer.readUtf();
        server.execute(() -> Quests.getQuestData().flatMap(quests -> quests.get(questId)).ifPresent(quest -> {
            quest.pause(player);
            Journals.sendOpenJournal(player, Journals.Page.QUESTS);
        }));
    }

    private void syncPlayerQuests(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        quests.saveForPlayer(player, tag);
        NetworkHelper.sendPacketToClient(player, MSG_CLIENT_SYNC_PLAYER_QUESTS, buf -> buf.writeNbt(tag));
    }

    static {
        TIER_NAMES = new HashMap<>();
        TIER_NAMES.put(0, Tier.TEST.getSerializedName());
        TIER_NAMES.put(1, Tier.NOVICE.getSerializedName());
        TIER_NAMES.put(2, Tier.APPRENTICE.getSerializedName());
        TIER_NAMES.put(3, Tier.JOURNEYMAN.getSerializedName());
        TIER_NAMES.put(4, Tier.EXPERT.getSerializedName());
        TIER_NAMES.put(5, Tier.MASTER.getSerializedName());
    }
}
