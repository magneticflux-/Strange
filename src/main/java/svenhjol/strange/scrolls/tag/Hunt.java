package svenhjol.strange.scrolls.tag;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import svenhjol.strange.scrolls.ScrollsHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Hunt implements ISerializable {
    public static final String ENTITY_DATA = "entity_data";
    public static final String ENTITY_COUNT = "entity_count";
    public static final String ENTITY_KILLED = "entity_killed";

    private Quest quest;
    private Map<Identifier, Integer> entities = new HashMap<>();
    private Map<Identifier, Integer> killed = new HashMap<>();

    // these are dynamically generated, not stored in nbt
    private Map<Identifier, Boolean> satisfied = new HashMap<>();
    private Map<Identifier, String> names = new HashMap<>();

    public Hunt(Quest quest) {
        this.quest = quest;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag outTag = new CompoundTag();
        CompoundTag dataTag = new CompoundTag();
        CompoundTag countTag = new CompoundTag();
        CompoundTag killedTag = new CompoundTag();

        if (!entities.isEmpty()) {
            int index = 0;
            for (Identifier entityId : entities.keySet()) {
                String entityIndex = Integer.toString(index);
                int entityCount = entities.get(entityId);
                int entityKilled = killed.getOrDefault(entityId, 0);

                // write the data to the tags at the specified index
                dataTag.putString(entityIndex, entityId.toString());
                countTag.putInt(entityIndex, entityCount);
                killedTag.putInt(entityIndex, entityKilled);

                index++;
            }
        }

        outTag.put(ENTITY_DATA, dataTag);
        outTag.put(ENTITY_COUNT, countTag);
        outTag.put(ENTITY_KILLED, killedTag);

        return outTag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        CompoundTag dataTag = (CompoundTag)tag.get(ENTITY_DATA);
        CompoundTag countTag = (CompoundTag)tag.get(ENTITY_COUNT);
        CompoundTag killedTag = (CompoundTag)tag.get(ENTITY_KILLED);

        entities = new HashMap<>();
        killed = new HashMap<>();

        if (dataTag != null && dataTag.getSize() > 0 && countTag != null) {
            for (int i = 0; i < dataTag.getSize(); i++) {
                // read the data from the tags at the specified index
                String tagIndex = String.valueOf(i);
                Identifier entityId = Identifier.tryParse(dataTag.getString(tagIndex));
                if (entityId == null)
                    continue;

                int entityCount = countTag.getInt(tagIndex);
                int entityKilled = killedTag != null ? killedTag.getInt(tagIndex) : 0;

                entities.put(entityId, entityCount);
                killed.put(entityId, entityKilled);
            }
        }
    }

    public void addEntity(Identifier entity, int count) {
        entities.put(entity, count);
    }

    public Map<Identifier, Integer> getEntities() {
        return entities;
    }

    public Map<Identifier, Integer> getKilled() {
        return killed;
    }

    public Map<Identifier, Boolean> getSatisfied() {
        return satisfied;
    }

    public Map<Identifier, String> getNames() {
        return names;
    }

    public boolean isSatisfied() {
        if (entities.isEmpty())
            return true;

        return satisfied.size() == entities.size() && getSatisfied().values().stream().allMatch(r -> r);
    }

    public void update(PlayerEntity player) {
        satisfied.clear();

        entities.forEach((id, count) -> {
            int countKilled = killed.getOrDefault(id, 0);
            satisfied.put(id, countKilled >= count);

            Optional<EntityType<?>> optionalEntityType = Registry.ENTITY_TYPE.getOrEmpty(id);
            optionalEntityType.ifPresent(entityType -> names.put(id, entityType.getName().getString()));
        });
    }

    public void entityKilled(LivingEntity entity, Entity attacker) {
        if (!(attacker instanceof ServerPlayerEntity))
            return;

        ServerPlayerEntity player = (ServerPlayerEntity) attacker;

        // must be the player who owns the quest
        if (quest.getOwner().equals(player.getUuid()) || quest.getOwner().equals(ScrollsHelper.ANY_UUID)) {
            Identifier id = Registry.ENTITY_TYPE.getId(entity.getType());

            if (entities.containsKey(id)) {
                Integer count = killed.getOrDefault(id, 0);
                killed.put(id, count + 1);

                quest.setDirty(true);
                quest.update(player);
            }
        }
    }
}
