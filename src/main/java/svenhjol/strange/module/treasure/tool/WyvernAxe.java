package svenhjol.strange.module.treasure.tool;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.DyeColor;
import svenhjol.strange.module.treasure.ITreasureTool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WyvernAxe implements ITreasureTool {
    @Override
    public DyeColor getColor() {
        return DyeColor.BLACK;
    }

    @Override
    public Map<Enchantment, Integer> getEnchantments() {
        HashMap<Enchantment, Integer> map = new HashMap<>();
        map.put(Enchantments.SHARPNESS, 3);
        map.put(Enchantments.SMITE, 3);
        map.put(Enchantments.BANE_OF_ARTHROPODS, 3);
        return map;
    }

    @Override
    public List<String> getValidEnchantments() {
        return null;
    }

    @Override
    public TranslatableText getName(ItemStack itemStack) {
        return new TranslatableText("item.strange.treasure.wyvern_axe");
    }

    @Override
    public ItemStack getItemStack() {
        return new ItemStack(Items.DIAMOND_AXE);
    }

    @Override
    public int getMaxAdditionalLevels() {
        return 0;
    }
}