package svenhjol.strange.module.relics;

import net.fabricmc.fabric.api.loot.v1.FabricLootPoolBuilder;
import net.fabricmc.fabric.api.loot.v1.FabricLootSupplierBuilder;
import net.fabricmc.fabric.api.loot.v1.event.LootTableLoadingCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import svenhjol.charm.annotation.CommonModule;
import svenhjol.charm.annotation.Config;
import svenhjol.charm.helper.ClassHelper;
import svenhjol.charm.helper.LogHelper;
import svenhjol.charm.loader.CharmModule;
import svenhjol.charm.registry.CommonRegistry;
import svenhjol.strange.Strange;
import svenhjol.strange.module.relics.loot.ToolRelicLootFunction;
import svenhjol.strange.module.stone_ruins.StoneRuins;
import svenhjol.strange.module.stone_ruins.StoneRuinsLoot;
import svenhjol.strange.module.vaults.Vaults;
import svenhjol.strange.module.vaults.VaultsLoot;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommonModule(mod = Strange.MOD_ID)
public class Relics extends CharmModule {
    public static final String ITEM_NAMESPACE = "svenhjol.strange.module.relics.item";
    public static final Map<Type, List<IRelicItem>> RELICS = new HashMap<>();

    public static final List<ResourceLocation> TOOL_LOOT_TABLES = new ArrayList<>();
    public static final List<ResourceLocation> WEAPON_LOOT_TABLES = new ArrayList<>();
    public static final List<ResourceLocation> ARMOR_LOOT_TABLES = new ArrayList<>();
    public static final List<ResourceLocation> WEIRD_LOOT_TABLES = new ArrayList<>();

    public static final ResourceLocation TOOL_LOOT_ID = new ResourceLocation(Strange.MOD_ID, "tool_relic_loot");
    public static final ResourceLocation WEAPON_LOOT_ID = new ResourceLocation(Strange.MOD_ID, "weapon_relic_loot");
    public static final ResourceLocation ARMOR_LOOT_ID = new ResourceLocation(Strange.MOD_ID, "armor_relic_loot");
    public static final ResourceLocation WEIRD_LOOT_ID = new ResourceLocation(Strange.MOD_ID, "weird_relic_loot");

    public static LootItemFunctionType TOOL_LOOT_FUNCTION;
    public static LootItemFunctionType WEAPON_LOOT_FUNCTION;
    public static LootItemFunctionType ARMOR_LOOT_FUNCTION;
    public static LootItemFunctionType WEIRD_LOOT_FUNCTION;

    @Config(name = "Additional levels", description = "Number of levels above the enchantment maximum level that can be applied to relics.\n" +
        "Enchantment levels are capped at level 10.")
    public static int extraLevels = 5;

    @Config(name = "Additional loot tables", description = "List of additional loot tables that relics will be added to.")
    public static List<String> additionalLootTables = List.of();

    @Config(name = "Blacklist", description = "List of relic items that will not be loaded. See wiki for details.")
    public static List<String> configBlacklist = new ArrayList<>();

    @Override
    public void register() {
        TOOL_LOOT_FUNCTION = CommonRegistry.lootFunctionType(TOOL_LOOT_ID, new LootItemFunctionType(new ToolRelicLootFunction.Serializer()));
    }

    @Override
    public void runWhenEnabled() {
        LootTableLoadingCallback.EVENT.register(this::handleLootTables);

        if (Strange.LOADER.isEnabled(Vaults.class)) {

            // If vaults is enabled, add relics to the large room.
            TOOL_LOOT_TABLES.add(VaultsLoot.VAULTS_LARGE_ROOM);
            WEAPON_LOOT_TABLES.add(VaultsLoot.VAULTS_LARGE_ROOM);

        } else if (Strange.LOADER.isEnabled(StoneRuins.class)) {

            // If vaults not enabled and stone ruins are enabled, add relics to the ruin rooms.
            TOOL_LOOT_TABLES.add(StoneRuinsLoot.STONE_RUINS_ROOM);
            WEAPON_LOOT_TABLES.add(StoneRuinsLoot.STONE_RUINS_ROOM);

        } else {

            // Default to adding relics to woodland mansions.
            TOOL_LOOT_TABLES.add(BuiltInLootTables.WOODLAND_MANSION);
            WEAPON_LOOT_TABLES.add(StoneRuinsLoot.STONE_RUINS_ROOM);

        }

        try {
            List<String> classes = ClassHelper.getClassesInPackage(ITEM_NAMESPACE);
            for (var className : classes) {
                var simpleClassName = className.substring(className.lastIndexOf(".") + 1);
                try {
                    var clazz = Class.forName(className);
                    if (configBlacklist.contains(simpleClassName)) continue;

                    var item = (IRelicItem) clazz.getDeclaredConstructor().newInstance();
                    RELICS.computeIfAbsent(item.getType(), a -> new ArrayList<>()).add(item);

                    LogHelper.debug(Strange.MOD_ID, getClass(), "Loaded relic: " + simpleClassName);
                } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    LogHelper.warn(getClass(), "Relic `" + simpleClassName + "` failed to load: " + e.getMessage());
                }
            }
        } catch (IOException | URISyntaxException e) {
            LogHelper.info(Strange.MOD_ID, getClass(), "Failed to load classes from namespace: " + e.getMessage());
        }

        for (var lootTable : additionalLootTables) {
            var table = new ResourceLocation(lootTable);
            TOOL_LOOT_TABLES.add(table);
            WEAPON_LOOT_TABLES.add(table);
            ARMOR_LOOT_TABLES.add(table);
            WEIRD_LOOT_TABLES.add(table);
        }
    }

    public static void preserveHighestLevelEnchantment(Map<Enchantment, Integer> enchantments, ItemStack book, ItemStack output) {
        if (book.isEmpty() || output.isEmpty()) return;

        if (book.getItem() instanceof EnchantedBookItem) {
            Map<Enchantment, Integer> reset = new HashMap<>();
            var bookEnchants = EnchantmentHelper.getEnchantments(book);

            bookEnchants.forEach((e, l) -> {
                if (l > e.getMaxLevel()) {
                    reset.put(e, l);
                }
            });

            reset.forEach((e, l) -> {
                if (enchantments.containsKey(e)) {
                    enchantments.put(e, l);
                }
            });
        }

        EnchantmentHelper.setEnchantments(enchantments, output);
    }

    private void handleLootTables(ResourceManager resourceManager, LootTables lootManager, ResourceLocation id, FabricLootSupplierBuilder supplier, LootTableLoadingCallback.LootTableSetter setter) {
        if (TOOL_LOOT_TABLES.contains(id)) {
            FabricLootPoolBuilder builder = FabricLootPoolBuilder.builder()
                .rolls(ConstantValue.exactly(1))
                .with(LootItem.lootTableItem(Items.DIAMOND_PICKAXE)
                    .setWeight(1)
                    .apply(() -> new ToolRelicLootFunction(new LootItemCondition[0])));

            supplier.withPool(builder);
        }
    }

    public enum Type {
        TOOL,
        WEAPON,
        ARMOR,
        WEIRD
    }
}
