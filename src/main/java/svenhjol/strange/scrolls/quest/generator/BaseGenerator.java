package svenhjol.strange.scrolls.quest.generator;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SuspiciousStewItem;
import net.minecraft.potion.*;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;
import svenhjol.charm.Charm;
import svenhjol.meson.Meson;
import svenhjol.meson.helper.PotionHelper;
import svenhjol.strange.Strange;
import svenhjol.strange.base.helper.ItemHelper;
import svenhjol.strange.base.helper.LocationHelper;
import svenhjol.strange.scrolls.module.Scrolls;
import svenhjol.strange.scrolls.quest.Condition;
import svenhjol.strange.scrolls.quest.Definition;
import svenhjol.strange.scrolls.quest.iface.IQuest;
import svenhjol.strange.stonecircles.module.StoneCircles;
import svenhjol.strange.traveljournal.module.TravelJournal;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public abstract class BaseGenerator {
    public static final float EPIC_CHANCE_BASE = 0.02F;
    public static final float RARE_CHANCE_BASE = 0.15F;
    public static final float UNCOMMON_CHANCE_BASE = 0.45F;
    public static final float COMMON_CHANCE_BASE = 0.75F;

    public static final String ENCHANTED_LABEL = "[enchanted]";
    public static final String EPIC_LABEL = "[epic]";
    public static final String RARE_LABEL = "[rare]";
    public static final String COMMON_LABEL = "[common]";
    public static final String UNCOMMON_LABEL = "[uncommon]";

    // special item tags
    public static final String ANCIENT_TOME = "AncientTome";
    public static final String ENCHANTED_BOOK = "EnchantedBook";
    public static final String POTION = "Potion";
    public static final String RARE_ENCHANTED_BOOK = "RareEnchantedBook";
    public static final String SCROLL_TIER1 = "ScrollTier1";
    public static final String SCROLL_TIER2 = "ScrollTier2";
    public static final String SCROLL_TIER3 = "ScrollTier3";
    public static final String SCROLL_TIER4 = "ScrollTier4";
    public static final String SCROLL_TIER5 = "ScrollTier5";
    public static final String STONE_CIRCLE_MAP = "StoneCircleMap";
    public static final String STONE_CIRCLE_TOTEM = "StoneCircleTotem";
    public static final String SUSPICIOUS_STEW = "SuspiciousStew";
    public static final String TRAVEL_JOURNAL = "TravelJournal";

    public static final ArrayList<String> SPECIAL_ITEMS = new ArrayList<>(Arrays.asList(
        STONE_CIRCLE_TOTEM, ENCHANTED_BOOK, RARE_ENCHANTED_BOOK, TRAVEL_JOURNAL, ANCIENT_TOME, STONE_CIRCLE_MAP,
        SCROLL_TIER1, SCROLL_TIER2, SCROLL_TIER3, SCROLL_TIER4, SCROLL_TIER5
    ));

    protected final World world;
    protected final BlockPos pos;
    protected final IQuest quest;
    protected final Definition definition;

    public BaseGenerator(World world, BlockPos pos, IQuest quest, Definition definition) {
        this.world = world;
        this.pos = pos;
        this.quest = quest;
        this.definition = definition;
    }

    @SuppressWarnings("rawtypes")
    public void addCondition(Condition condition) {
        quest.getCriteria().addCondition(condition);
    }

    public int multiplyValue(int original) {
        return (int) (original * quest.getValue());
    }

    public String splitOptionalRandomly(String key) {
        if (key.contains("|")) {
            String[] split = key.split("\\|");
            key = split[world.rand.nextInt(split.length)];
        }

        key = key.trim();
        return key;
    }

    @Nullable
    public Block getBlockFromKey(String key) {
        key = splitOptionalRandomly(key);
        ResourceLocation res = ResourceLocation.tryCreate(key);
        if (res == null)
            return null;

        return ForgeRegistries.BLOCKS.getValue(res);
    }

    @Nullable
    public ItemStack getItemFromKey(String key) {
        ItemStack stack;
        key = splitOptionalRandomly(key);

        boolean doEnchant = key.contains(ENCHANTED_LABEL);
        boolean isRare = key.contains(RARE_LABEL) || key.contains(EPIC_LABEL);

        if (key.contains(ENCHANTED_LABEL))
            key = key.replace(ENCHANTED_LABEL, "");

        key = filterRarity(key);
        if (key == null)
            return null;

        if (isSpecialItem(key)) {
            // some items are keywords and have special initialization
            stack = createSpecialItem(key);
        } else {
            ResourceLocation res = ResourceLocation.tryCreate(key);
            if (res == null)
                return null;

            Item item = ForgeRegistries.ITEMS.getValue(res);
            if (item == null)
                return null;

            stack = new ItemStack(item);

            if (doEnchant && item.isEnchantable(stack))
                EnchantmentHelper.addRandomEnchantment(world.rand, stack, isRare ? 15 : 5, isRare);
        }

        return stack;
    }

    @Nullable
    public ResourceLocation getEntityResFromKey(String key) {
        key = splitOptionalRandomly(key);

        key = filterRarity(key);
        if (key == null)
            return null;

        return ResourceLocation.tryCreate(key);
    }

    @Nullable
    public String filterRarity(String key) {
        if (key.contains(EPIC_LABEL)) {
            // epic items only have a small chance to pass, boosted by quest value
            if (world.rand.nextFloat() > (EPIC_CHANCE_BASE + (0.02F * quest.getValue()))) return null;
            return key.replace(EPIC_LABEL, "");

        } else if (key.contains(RARE_LABEL)) {
            // rare items only have a small chance to pass, boosted by quest value
            if (world.rand.nextFloat() > (RARE_CHANCE_BASE + (0.02F * quest.getValue()))) return null;
            return key.replace(RARE_LABEL, "");

        } else if (key.contains(UNCOMMON_LABEL)) {
            // uncommon items pass sometimes
            if (world.rand.nextFloat() > (UNCOMMON_CHANCE_BASE + (0.05F * quest.getValue()))) return null;
            return key.replace(UNCOMMON_LABEL, "");

        } else if (key.contains(COMMON_LABEL)) {
            // common items pass often
            if (world.rand.nextFloat() > (COMMON_CHANCE_BASE + (0.05F * quest.getValue()))) return null;
            return key.replace(COMMON_LABEL, "");
        }

        return key;
    }

    public int getCountFromValue(String value, boolean scale) {
        int count;

        if (value.contains("!")) {
            return Integer.parseInt(value.replace("!", ""));
        }

        if (value.contains("-")) {
            String[] split = value.split("-");
            count = world.rand.nextInt(Integer.parseInt(split[1])) + Integer.parseInt(split[0]);
        } else {
            count = Integer.parseInt(value);
        }

        // amount increases based on distance
        return scale ? multiplyValue(count) : count;
    }

    public boolean isSpecialItem(String item) {
        return SPECIAL_ITEMS.contains(item);
    }

    @Nullable
    private ItemStack createSpecialItem(String item) {
        if (world == null) return null;

        Random rand = world.rand;
        ItemStack out = null;
        boolean rare;

        switch (item) {
            case STONE_CIRCLE_TOTEM:
                out = ItemHelper.getDistantTotem(world);
                break;

            case STONE_CIRCLE_MAP:
                if (!world.isRemote) {
                    List<Direction> directions = new ArrayList<>(Arrays.asList(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));

                    out = LocationHelper.createMap(
                        (ServerWorld) world,
                        pos.offset(directions.get(rand.nextInt(directions.size())), rand.nextInt(2000)),
                        new ResourceLocation(Strange.MOD_ID, StoneCircles.NAME));
                }

                break;

            case ENCHANTED_BOOK:
            case RARE_ENCHANTED_BOOK:
                out = new ItemStack(Items.BOOK);
                rare = item.equals(RARE_ENCHANTED_BOOK);
                out = EnchantmentHelper.addRandomEnchantment(rand, out, rand.nextInt(15) + (rare ? 15 : 1), rare);
                break;

            case TRAVEL_JOURNAL:
                if (Meson.isModuleEnabled("strange:travel_journal")) {
                    out = new ItemStack(TravelJournal.item);
                }
                break;

            case ANCIENT_TOME:
                if (Charm.quarkCompat != null
                    && Meson.isModuleEnabled("quark:ancient_tomes")
                ) {
                    out = Charm.quarkCompat.getRandomAncientTome(rand);
                }
                break;

            case POTION:
                ItemStack bottle = PotionHelper.getFilledWaterBottle();
                List<Potion> potions = Arrays.asList(
                    Potions.LONG_FIRE_RESISTANCE,
                    Potions.LONG_INVISIBILITY,
                    Potions.LONG_LEAPING,
                    Potions.LONG_REGENERATION,
                    Potions.LONG_SLOW_FALLING,
                    Potions.LONG_STRENGTH,
                    Potions.LONG_SWIFTNESS,
                    Potions.LONG_TURTLE_MASTER,
                    Potions.LONG_WATER_BREATHING
                );

                out = PotionUtils.addPotionToItemStack(bottle, potions.get(rand.nextInt(potions.size())));
                break;

            case SUSPICIOUS_STEW:
                out = new ItemStack(Items.SUSPICIOUS_STEW);
                List<Effect> effects = Arrays.asList(
                    Effects.FIRE_RESISTANCE, Effects.HEALTH_BOOST, Effects.SPEED, Effects.STRENGTH,
                    Effects.REGENERATION, Effects.ABSORPTION, Effects.BLINDNESS, Effects.MINING_FATIGUE
                );
                SuspiciousStewItem.addEffect(out, effects.get(rand.nextInt(effects.size())), 120);
                break;

            case SCROLL_TIER1:
                out = Scrolls.createScroll(1, 0.25F);
                break;

            case SCROLL_TIER2:
                out = Scrolls.createScroll(2, 0.35F);
                break;

            case SCROLL_TIER3:
                out = Scrolls.createScroll(3, 0.45F);
                break;

            case SCROLL_TIER4:
                out = Scrolls.createScroll(4, 0.5F);
                break;

            case SCROLL_TIER5:
                out = Scrolls.createScroll(5, 0.55F);
                break;

            default:
                break;
        }

        return out;
    }

    public abstract void generate();
}
