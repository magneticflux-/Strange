package svenhjol.strange.module.scrolls.tag;

import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import svenhjol.charm.helper.PlayerHelper;
import svenhjol.charm.helper.PosHelper;
import svenhjol.strange.module.scrolls.populator.ExplorePopulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Explore implements ISerializable {
    public static final String STRUCTURE = "structure";
    public static final String DIMENSION = "dimension";
    public static final String CHEST_START = "chest_start";
    public static final String CHEST_RANGE = "chest_range";
    public static final String CHESTS = "chests";
    public static final String ITEMS = "items";
    public static final String QUEST = "quest";

    private Quest quest;
    private BlockPos structure;
    private List<BlockPos> chests;
    private Identifier dimension;
    private int chestStart;
    private int chestRange;
    private List<ItemStack> items = new ArrayList<>();
    private final Map<Item, Boolean> satisfied = new HashMap<>(); // this is dynamically generated, not stored in nbt

    public Explore(Quest quest) {
        this.quest = quest;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound outTag = new NbtCompound();

        if (!items.isEmpty()) {
            NbtList itemDataTag = new NbtList();
            for (ItemStack stack : items) {
                NbtCompound itemTag = new NbtCompound();
                stack.writeNbt(itemTag);
                itemDataTag.add(itemTag);
            }

            outTag.put(ITEMS, itemDataTag);
        }


        if (structure != null)
            outTag.putLong(STRUCTURE, structure.asLong());

        if (chests != null && !chests.isEmpty()) {
            List<Long> chestPositions = chests.stream()
                .map(BlockPos::toImmutable)
                .map(BlockPos::asLong)
                .collect(Collectors.toList());

            outTag.putLongArray(CHESTS, chestPositions);
        }

        if (dimension != null)
            outTag.putString(DIMENSION, dimension.toString());

        outTag.putInt(CHEST_START, chestStart);
        outTag.putInt(CHEST_RANGE, chestRange);

        return outTag;
    }

    @Override
    public void fromTag(NbtCompound tag) {
        chestRange = tag.getInt(CHEST_RANGE);
        chestStart = tag.getInt(CHEST_START);
        structure = tag.contains(STRUCTURE) ? BlockPos.fromLong(tag.getLong(STRUCTURE)) : null;
        dimension = Identifier.tryParse(tag.getString(DIMENSION));

        if (dimension == null)
            dimension = new Identifier("minecraft", "overworld"); // probably not good

        if (tag.contains(CHESTS)) {
            chests = new ArrayList<>();
            long[] chestPositions = tag.getLongArray(CHESTS);
            for (long pos : chestPositions) {
                chests.add(BlockPos.fromLong(pos));
            }
        }

        items = new ArrayList<>();

        NbtList itemDataTag = (NbtList)tag.get(ITEMS);
        if (itemDataTag != null && itemDataTag.size() > 0) {
            for (NbtElement itemTag : itemDataTag) {
                ItemStack stack = ItemStack.fromNbt((NbtCompound)itemTag);
                items.add(stack);
            }
        }
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public BlockPos getStructure() {
        return structure;
    }

    public int getChestStart() {
        return chestStart;
    }

    public int getChestRange() {
        return chestRange;
    }

    public Map<Item, Boolean> getSatisfied() {
        return satisfied;
    }

    public void setStructure(BlockPos structure) {
        this.structure = structure;
    }

    public void setDimension(Identifier dimension) {
        this.dimension = dimension;
    }

    public void setItems(List<ItemStack> items) {
        this.items = items;
    }

    public void setChestStart(int chestStart) {
        this.chestStart = chestStart;
    }

    public void setChestRange(int chestRange) {
        this.chestRange = chestRange;
    }

    public boolean isSatisfied() {
        if (items.isEmpty())
            return true;

        return satisfied.size() == items.size() && getSatisfied().values().stream().allMatch(r -> r);
    }

    public void playerTick(PlayerEntity player) {
        if (player.world.isClient || structure == null || (chests != null && !chests.isEmpty()))
            return;

        double dist = PosHelper.getDistanceSquared(player.getBlockPos(), structure);
        if (dist < 1200) {
            List<BlockPos> chestPositions = ExplorePopulator.addScrollItemsToChests(player, this);

            if (chestPositions.isEmpty()) {
                quest.abandon(player);
                return;
            }

            quest.setDirty(true);
            chests = chestPositions;
        }
    }

    public void complete(PlayerEntity player, MerchantEntity merchant) {
        if (items.isEmpty())
            return;

        items.forEach(stack -> {
            for (ItemStack invStack : PlayerHelper.getInventory(player).main) {
                if (ItemStack.areEqual(stack, invStack)
                    && stack.getOrCreateTag().getString(QUEST).equals(quest.getId())
                ) {
                    invStack.decrement(1);
                }
            }
        });
    }

    public void update(PlayerEntity player) {
        satisfied.clear();

        items.forEach(itemStack -> {
            ItemStack stack = itemStack.copy();
            Item item = stack.getItem();
            satisfied.put(item, false);

            PlayerHelper.getInventory(player).main.forEach(invStack -> {
                // skip if already added
                if (satisfied.containsKey(item) && satisfied.get(item))
                    return;

                // compare stacks and add the item if found
                if (!stack.isEmpty()
                    && stack.isItemEqualIgnoreDamage(invStack)
                    && invStack.getOrCreateTag().getString(QUEST).equals(quest.getId())
                ) {
                    satisfied.put(item, true);
                }
            });
        });
    }
}