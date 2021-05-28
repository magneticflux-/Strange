package svenhjol.strange.module.scrollkeepers;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestType;
import svenhjol.charm.Charm;
import svenhjol.charm.annotation.Config;
import svenhjol.charm.annotation.Module;
import svenhjol.charm.event.SetupStructureCallback;
import svenhjol.charm.handler.ModuleHandler;
import svenhjol.charm.helper.VillagerHelper;
import svenhjol.charm.helper.WorldHelper;
import svenhjol.charm.mixin.accessor.VillagerEntityAccessor;
import svenhjol.charm.module.CharmModule;
import svenhjol.strange.Strange;
import svenhjol.strange.module.scrolls.*;
import svenhjol.strange.module.scrolls.tag.Quest;

import java.util.Optional;
import java.util.UUID;

import static svenhjol.charm.event.SetupStructureCallback.addVillageHouse;

@Module(mod = Strange.MOD_ID, client = ScrollKeepersClient.class, description = "Scrollkeepers are villagers that sell scrolls and accept completed quests. [Requires Scrolls]", alwaysEnabled = true)
public class Scrollkeepers extends CharmModule {
    public static Identifier VILLAGER_ID = new Identifier(Strange.MOD_ID, "scrollkeeper");
    public static final int[] QUEST_XP = new int[]{1, 10, 16, 24, 35, 44};
    public static Identifier BLOCK_ID = new Identifier(Strange.MOD_ID, "writing_desk");
    public static WritingDeskBlock WRITING_DESK;

    public static final Identifier MSG_SERVER_GET_SCROLL_QUEST = new Identifier(Strange.MOD_ID, "server_quest_satisfied");
    public static final Identifier MSG_CLIENT_RECEIVE_SCROLL_QUEST = new Identifier(Strange.MOD_ID, "client_quest_satisfied");

    public static VillagerProfession SCROLLKEEPER;
    public static PointOfInterestType POIT;

    public static ScrollKeepersClient client;
    public static int interestRange = 16;

    @Config(name = "Bad Omen chance", description = "Chance (out of 1.0) of the player receiving Bad Omen when handing in a scroll.")
    public static double badOmenChance = 0.05D;

    @Override
    public void register() {
        WRITING_DESK = new WritingDeskBlock(this);
        POIT = WorldHelper.addPointOfInterestType(BLOCK_ID, WRITING_DESK, 1);
        SCROLLKEEPER = VillagerHelper.addProfession(VILLAGER_ID, POIT, SoundEvents.ENTITY_VILLAGER_WORK_LIBRARIAN);
    }

    @Override
    public boolean depends() {
        return ModuleHandler.enabled("strange:scrolls");
    }

    @Override
    public void init() {
        // listen for entity interaction events
        UseEntityCallback.EVENT.register(this::tryHandInScroll);

        // register scrollkeeper trades
        VillagerHelper.addTrade(SCROLLKEEPER, 1, new ScrollkeeperTradeOffers.ScrollForEmeralds(1));
        VillagerHelper.addTrade(SCROLLKEEPER, 2, new ScrollkeeperTradeOffers.ScrollForEmeralds(2));
        VillagerHelper.addTrade(SCROLLKEEPER, 3, new ScrollkeeperTradeOffers.ScrollForEmeralds(3));
        VillagerHelper.addTrade(SCROLLKEEPER, 4, new ScrollkeeperTradeOffers.ScrollForEmeralds(4));
        VillagerHelper.addTrade(SCROLLKEEPER, 5, new ScrollkeeperTradeOffers.ScrollForEmeralds(5));

        // register scrollkeeper structures
        addVillageHouse(SetupStructureCallback.VillageType.PLAINS, new Identifier("strange:village/plains/houses/plains_scrollkeeper"), 5);
        addVillageHouse(SetupStructureCallback.VillageType.SAVANNA, new Identifier("strange:village/savanna/houses/savanna_scrollkeeper"), 5);
        addVillageHouse(SetupStructureCallback.VillageType.SNOWY, new Identifier("strange:village/snowy/houses/snowy_scrollkeeper"), 5);
        addVillageHouse(SetupStructureCallback.VillageType.TAIGA, new Identifier("strange:village/taiga/houses/taiga_scrollkeeper"), 5);
        addVillageHouse(SetupStructureCallback.VillageType.DESERT, new Identifier("strange:village/desert/houses/desert_scrollkeeper"), 5);

        // listen for quest satisfied request coming from the client
        ServerPlayNetworking.registerGlobalReceiver(MSG_SERVER_GET_SCROLL_QUEST, this::handleGetScrollQuest);
    }

    public static void sendScrollQuestPacket(ServerPlayerEntity player, Quest quest) {
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        buffer.writeNbt(quest.toTag());
        ServerPlayNetworking.send(player, MSG_CLIENT_RECEIVE_SCROLL_QUEST, buffer);
    }

    private ActionResult tryHandInScroll(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (entity instanceof VillagerEntity) {
            ItemStack heldStack = player.getStackInHand(hand);
            VillagerEntity villager = (VillagerEntity)entity;

            if (villager.getVillagerData().getProfession() != SCROLLKEEPER)
                return ActionResult.PASS;

            if (!(heldStack.getItem() instanceof ScrollItem))
                return ActionResult.PASS;

            if (!world.isClient) {
                Optional<QuestManager> optionalQuestManager = Scrolls.getQuestManager();
                if (optionalQuestManager.isEmpty())
                    return ActionResult.PASS;

                QuestManager questManager = optionalQuestManager.get();

                String questId = ScrollItem.getScrollQuest(heldStack);
                if (questId == null)
                    return ActionResult.PASS;

                Optional<Quest> optionalQuest = questManager.getQuest(questId);
                if (optionalQuest.isEmpty()) {
                    ((VillagerEntityAccessor)villager).invokeSayNo();
                    return ActionResult.FAIL;
                }

                Quest quest = optionalQuest.get();

                // must be the owner of the scroll
                UUID owner = ScrollItem.getScrollOwner(heldStack);
                if (owner != null && !player.getUuid().equals(owner)) {
                    ((VillagerEntityAccessor)villager).invokeSayNo();
                    return ActionResult.FAIL;
                }

                // quest conditions haven't been satisfied yet
                if (!quest.isSatisfied(player)) {
                    ((VillagerEntityAccessor)villager).invokeSayNo();
                    return ActionResult.FAIL;
                }

                // must be the merchant you bought the scroll from, or a scroll you found
                if (!villager.getUuid().equals(quest.getMerchant())
                    && !quest.getMerchant().equals(ScrollsHelper.ANY_UUID)) {
                    ((VillagerEntityAccessor)villager).invokeSayNo();
                    return ActionResult.FAIL;
                }

                // success, tidy up the quest, give rewards etc.
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.PLAYERS, 1.0F, 1.0F);
                quest.complete(player, villager);
                questManager.sendToast((ServerPlayerEntity) player, quest, QuestToastType.Success, "event.strange.quests.completed");
                Scrolls.triggerCompletedScroll((ServerPlayerEntity) player);
                heldStack.decrement(1);

                // handle villager xp increase and level-up
                VillagerData villagerData = villager.getVillagerData();
                int villagerXp = villager.getExperience();
                int villagerLevel = villagerData.getLevel();
                int questTier = quest.getTier();
                int questRarity = quest.getRarity();

                if (questTier >= villagerLevel) {
                    int tierXp = QUEST_XP[Math.min(Scrolls.TIERS, questTier) - 1];
                    if (questTier > villagerLevel)
                        tierXp /= 2;

                    int rareXp = tierXp * questRarity;
                    int newVillagerXp = villagerXp + Math.max(tierXp, rareXp);
                    villager.setCurrentCustomer(null);
                    villager.setExperience(newVillagerXp);

                    if (((VillagerEntityAccessor)villager).invokeCanLevelUp())
                        ((VillagerEntityAccessor)villager).invokeLevelUp();
                }

                // handle bad omen penalty
                if (badOmenChance > 0 && villagerLevel >= 3 && world.random.nextFloat() < (Math.min(badOmenChance, 1.0D) * (villagerLevel - 2))) {
                    int amplifier = Math.max(0, villagerLevel - 2);
                    StatusEffectInstance badOmen = new StatusEffectInstance(StatusEffects.BAD_OMEN, 120000, amplifier, false, false, true);
                    player.addStatusEffect(badOmen);
                    Charm.LOG.debug("Applying bad omen of amplifier: " + amplifier);
                }
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    private void handleGetScrollQuest(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf data, PacketSender sender) {
        String questId = data.readString(4);

        server.execute(() -> {
            if (player == null)
                return;

            if (!Scrolls.getQuestManager().isPresent())
                return;

            QuestManager questManager = Scrolls.getQuestManager().get();
            if (!questManager.getQuest(questId).isPresent())
                return;

            Quest quest = questManager.getQuest(questId).get();
            Scrollkeepers.sendScrollQuestPacket(player, quest);
        });
    }
}