package svenhjol.strange.module.runestones;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import svenhjol.charm.helper.LogHelper;
import svenhjol.strange.module.knowledge.Knowledge;
import svenhjol.strange.module.knowledge.KnowledgeData;
import svenhjol.strange.module.knowledge.KnowledgeHelper;
import svenhjol.strange.module.knowledge.types.Discovery;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class RunestoneHelper {
    public static Item DEFAULT_ITEM = Items.ENDER_PEARL;

//    public static Item getItem(ResourceLocation dimension, float difficulty, Random random) {
//        if (!Runestones.ITEMS.containsKey(dimension)) {
//            return DEFAULT_ITEM;
//        }
//
//        Map<Integer, List<Item>> items = Runestones.ITEMS.get(dimension);
//        int tier = Math.round(Runestones.TIERS * difficulty);
//
//        if (items.containsKey(tier) && !items.get(tier).isEmpty()) {
//            List<Item> tierItems = items.get(tier);
//
//            tierItems.sort((i1, i2) -> {
//                if (i1.hashCode() == i2.hashCode()) return 0;
//                return i1.hashCode() < i2.hashCode() ? -1 : 1;
//            });
//
//            return tierItems.get(random.nextInt(tierItems.size()));
//        } else {
//            return DEFAULT_ITEM;
//        }
//    }
//
//    public static List<Item> getItems(ResourceLocation dimension, float difficulty, Random random) {
//        if (!Runestones.ITEMS.containsKey(dimension)) {
//            return List.of(Items.ENDER_PEARL);
//        }
//
//        Map<Integer, List<Item>> items = Runestones.ITEMS.get(dimension);
//        int tier = Math.round(Runestones.TIERS * difficulty);
//
//        if (items.containsKey(tier) && !items.get(tier).isEmpty()) {
//            List<Item> tierItems = new LinkedList<>(items.get(tier));
//            Collections.shuffle(tierItems, random);
//            return tierItems.subList(0, Math.min(tierItems.size(), Runestones.MAX_ITEMS)).stream().distinct().collect(Collectors.toList());
//        } else {
//            return List.of(Items.ENDER_PEARL);
//        }
//    }

    public static List<Item> getItems(ResourceLocation dimension, String runes) {
        if (!Runestones.ITEMS.containsKey(dimension)) {
            return List.of(DEFAULT_ITEM);
        }

        long seed = KnowledgeHelper.generateSeedFromString(runes);
        Random random = new Random(seed);
        Map<Integer, List<Item>> items = Runestones.ITEMS.get(dimension);

        int len = Math.min(Knowledge.NUM_RUNES, runes.length());
        int tier = Math.round(Runestones.TIERS * (len / (float)Knowledge.NUM_RUNES));

        if (items.containsKey(tier) && !items.get(tier).isEmpty()) {
            List<Item> tierItems = new LinkedList<>(items.get(tier));
            Collections.shuffle(tierItems, random);
            return tierItems.subList(0, Math.min(tierItems.size(), Runestones.MAX_ITEMS)).stream().distinct().collect(Collectors.toList());
        } else {
            return List.of(DEFAULT_ITEM);
        }
    }

    public static Item getItem(ResourceLocation dimension, String runes) {
        List<Item> items = getItems(dimension, runes);
        return items.get(0);
    }

    public static String getClue(ResourceLocation location, Random random) {
        if (Runestones.CLUES.containsKey(location) && !Runestones.CLUES.get(location).isEmpty()) {
            List<String> clues = Runestones.CLUES.get(location);
            return clues.get(random.nextInt(clues.size()));
        }

        return RunestoneLocations.UNKNOWN_CLUE;
    }

    public static Optional<ResourceLocation> tryFetchDestinationByDifficulty(ResourceLocation dimension, Random random, float difficulty) {
        // fetch a location using the difficulty
        if (!Runestones.DESTINATIONS.containsKey(dimension)) {
            LogHelper.debug(RunestoneHelper.class, "No locations available for this dimension, giving up");
            return Optional.empty();
        }

        List<ResourceLocation> dimensionDestinations = new ArrayList<>(Runestones.DESTINATIONS.get(dimension));
        int index = Math.min(dimensionDestinations.size() - 1, (int)Math.ceil(dimensionDestinations.size() * difficulty));
        if (index == 1 && random.nextFloat() < 0.5F) {
            index = 0;
        }
        return Optional.of(dimensionDestinations.get(index));
    }

    public static Optional<Discovery> getOrCreateDestination(ResourceLocation dimension, Random random, float difficulty, float decay, @Nullable ResourceLocation id) {
        KnowledgeData knowledge = Knowledge.getKnowledgeData().orElseThrow();

        if (id != null) {
            if (id.equals(RunestoneLocations.SPAWN)) {
                // does it exist?
                Optional<Discovery> optSpawn = knowledge.specials.values().stream().filter(d -> d.getId().equals(RunestoneLocations.SPAWN)).findFirst();
                if (optSpawn.isPresent()) {
                    // generate items for it
                    Discovery spawn = optSpawn.get();
                    return Optional.of(spawn);
                }

                // generate a new set of runes for it, check if they already exist
                String runes = KnowledgeHelper.tryGenerateUniqueId(knowledge.specials, random, difficulty, 1, 1).orElseThrow();

                Discovery discovery = new Discovery(runes, RunestoneLocations.SPAWN);

                knowledge.specials.add(runes, discovery);
                knowledge.setDirty();
                return Optional.of(discovery);
            }
        } else {
            Optional<ResourceLocation> optLocation = tryFetchDestinationByDifficulty(dimension, random, difficulty);
            if (optLocation.isEmpty()) {
                return Optional.empty();
            }

            id = optLocation.get();
        }

        String runes = KnowledgeHelper.tryGenerateUniqueId(knowledge.discoveries, random, difficulty, Knowledge.MIN_LENGTH, Knowledge.MAX_LENGTH).orElseThrow();

        Discovery discovery = new Discovery(runes, id);
        discovery.setDifficulty(difficulty);
        discovery.setDecay(decay);
        discovery.setDimension(dimension);

        LogHelper.debug(RunestoneHelper.class, "Adding discovery to server knowledge.  Runes = " + runes + ", Location = " + id);
        knowledge.discoveries.add(runes, discovery);
        knowledge.setDirty();

        return knowledge.discoveries.get(runes);
    }
}
