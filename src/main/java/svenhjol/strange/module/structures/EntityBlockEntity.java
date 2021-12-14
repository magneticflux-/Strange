package svenhjol.strange.module.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import svenhjol.charm.block.CharmSyncedBlockEntity;
import svenhjol.charm.helper.LogHelper;
import svenhjol.charm.helper.LootHelper;
import svenhjol.strange.module.structures.legacy.LegacyDataResolver;

import java.util.*;

public class EntityBlockEntity extends CharmSyncedBlockEntity {
    public static final String ENTITY_TAG = "entity";
    public static final String PERSISTENT_TAG = "persist";
    public static final String HEALTH_TAG = "health";
    public static final String ARMOR_TAG = "armor";
    public static final String EFFECTS_TAG = "effects";
    public static final String META_TAG = "meta";
    public static final String COUNT_TAG = "count";
    public static final String ROTATION_TAG = "rotation";
    public static final String PRIMED_TAG = "primed";

    private ResourceLocation entity = new ResourceLocation("minecraft:sheep");
    private Rotation rotation = Rotation.NONE;
    private boolean persistent = true;
    private boolean primed = false;
    private double health = 20;
    private int count = 1;
    private String effects = "";
    private String armor = "";
    private String meta = "";

    public float rotateTicks = 0F;

    public EntityBlockEntity(BlockPos pos, BlockState state) {
        super(Structures.ENTITY_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        ResourceLocation entity = ResourceLocation.tryParse(tag.getString(ENTITY_TAG));
        this.entity = entity != null ? entity : new ResourceLocation("minecraft:sheep");

        this.persistent = tag.getBoolean(PERSISTENT_TAG);
        this.health = tag.getDouble(HEALTH_TAG);
        this.count = tag.getInt(COUNT_TAG);
        this.effects = tag.getString(EFFECTS_TAG);
        this.armor = tag.getString(ARMOR_TAG);
        this.meta = tag.getString(META_TAG);
        this.primed = tag.getBoolean(PRIMED_TAG);

        String rot = tag.getString(ROTATION_TAG);
        this.rotation = rot.isEmpty() ? Rotation.NONE : Rotation.valueOf(rot);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putString(ENTITY_TAG, entity.toString());
        tag.putString(ROTATION_TAG, rotation.name());
        tag.putBoolean(PERSISTENT_TAG, persistent);
        tag.putBoolean(PRIMED_TAG, primed);
        tag.putDouble(HEALTH_TAG, health);
        tag.putInt(COUNT_TAG, count);
        tag.putString(EFFECTS_TAG, effects);
        tag.putString(ARMOR_TAG, armor);
        tag.putString(META_TAG, meta);
    }

    public ResourceLocation getEntity() {
        return entity;
    }

    public void setEntity(ResourceLocation entity) {
        this.entity = entity;
    }

    public void setPrimed(boolean primed) {
        this.primed = primed;
    }

    public void setPersistent(boolean persist) {
        this.persistent = persist;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public void setEffects(String effects) {
        this.effects = effects;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setArmor(String armor) {
        this.armor = armor;
    }

    public boolean isPrimed() {
        return primed;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public double getHealth() {
        return health;
    }

    public int getCount() {
        return count;
    }

    public String getEffects() {
        return effects;
    }

    public String getArmor() {
        return armor;
    }

    public static <T extends EntityBlockEntity> void tick(Level world, BlockPos pos, BlockState state, T spawner) {
        if (world == null || world.getGameTime() % 10 == 0 || world.getDifficulty() == Difficulty.PEACEFUL || !spawner.isPrimed()) return;

        List<Player> players = world.getEntitiesOfClass(Player.class, new AABB(pos).inflate(Structures.entityTriggerDistance));
        if (players.size() == 0) return;

        // remove the spawner, create the entity
        world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        boolean result = trySpawn(world, spawner.worldPosition, spawner);

        if (result) {
            LogHelper.debug(EntityBlockEntity.class, "EntitySpawner spawned entity " + spawner.getEntity().toString() + " at pos: " + pos);
        } else {
            LogHelper.debug(EntityBlockEntity.class, "EntitySpawner failed to spawn entity " + spawner.getEntity().toString() + " at pos: " + pos);
        }
    }

    public static boolean trySpawn(Level world, BlockPos pos, EntityBlockEntity spawner) {
        Entity spawned;
        if (world == null)
            return false;

        Optional<EntityType<?>> optionalEntityType = Registry.ENTITY_TYPE.getOptional(spawner.entity);
        if (optionalEntityType.isEmpty())
            return false;

        EntityType<?> type = optionalEntityType.get();

        if (type == EntityType.MINECART || type == EntityType.CHEST_MINECART)
            return tryCreateMinecart(world, pos, type, spawner);

        if (type == EntityType.ARMOR_STAND)
            return tryCreateArmorStand(world, pos, spawner);

        for (int i = 0; i < spawner.count; i++) {
            spawned = type.create(world);
            if (spawned == null)
                return false;

            spawned.moveTo(pos, 0.0F, 0.0F);

            if (spawned instanceof Mob mob) {
                if (spawner.persistent) mob.setPersistenceRequired();

                // set the mob health if specified (values greater than zero)
                if (spawner.health > 0) {
                    // need to override this attribute on the entity to allow health values greater than maxhealth
                    AttributeInstance healthAttribute = mob.getAttribute(Attributes.MAX_HEALTH);
                    if (healthAttribute != null)
                        healthAttribute.setBaseValue(spawner.health);

                    mob.setHealth((float) spawner.health);
                }

                // add armor to the mob
                if (!spawner.armor.isEmpty()) {
                    Random random = world.random;
                    tryEquip(mob, spawner.armor, random);
                }

                // apply status effects to the mob
                // TODO: make this a helper so that Strange can use it too
                final List<String> effectsList = new ArrayList<>();
                if (spawner.effects.length() > 0) {
                    if (spawner.effects.contains(",")) {
                        effectsList.addAll(Arrays.asList(spawner.effects.split(",")));
                    } else {
                        effectsList.add(spawner.effects);
                    }
                    if (effectsList.size() > 0) {
                        effectsList.forEach(effectName -> {
                            MobEffect effect = Registry.MOB_EFFECT.get(new ResourceLocation(effectName));
                            if (effect != null) {
                                mob.addEffect(new MobEffectInstance(effect, 999999, 1)); // TODO: duration lol
                            }
                        });
                    }
                }

                mob.finalizeSpawn((ServerLevelAccessor)world, world.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null, null);
            }

            world.addFreshEntity(spawned);
        }
        return true;
    }

    public static boolean tryCreateMinecart(Level world, BlockPos pos, EntityType<?> type, EntityBlockEntity spawner) {
        AbstractMinecart minecart = null;
        if (world == null) return false;

        if (type == EntityType.CHEST_MINECART) {
            minecart = new MinecartChest(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);

            String loot = LegacyDataResolver.getValue("loot", spawner.meta, "");
            ResourceLocation lootTable = LootHelper.getLootTable(loot, BuiltInLootTables.ABANDONED_MINESHAFT);
            ((MinecartChest)minecart).setLootTable(lootTable, world.random.nextLong());
        } else if (type == EntityType.MINECART) {
            minecart = new Minecart(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        }

        if (minecart == null)
            return false;

        world.addFreshEntity(minecart);

        return true;
    }

    public static boolean tryCreateArmorStand(Level world, BlockPos pos, EntityBlockEntity spawner) {
        if (world == null)
            return false;

        Random random = world.random;
        ArmorStand stand = EntityType.ARMOR_STAND.create(world);
        if (stand == null)
            return false;

        Direction face = LegacyDataResolver.getFacing(LegacyDataResolver.getValue("facing", spawner.meta, "north"));
        Direction facing = spawner.rotation.rotate(face);
        String type = LegacyDataResolver.getValue("type", spawner.meta, "");

        tryEquip(stand, type, random);

        float yaw = facing.get2DDataValue();
        stand.moveTo(pos, yaw, 0.0F);
        world.addFreshEntity(stand);

        return true;
    }

    private static void tryEquip(LivingEntity entity, String type, Random random) {
        List<Item> ironHeld = new ArrayList<>(Arrays.asList(
            Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE
        ));

        List<Item> goldHeld = new ArrayList<>(Arrays.asList(
            Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE
        ));

        List<Item> diamondHeld = new ArrayList<>(Arrays.asList(
            Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL
        ));

        if (type.equals("leather")) {
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ironHeld.get(random.nextInt(ironHeld.size()))));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        }
        if (type.equals("chain")) {
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ironHeld.get(random.nextInt(ironHeld.size()))));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        }
        if (type.equals("iron")) {
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ironHeld.get(random.nextInt(ironHeld.size()))));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        }
        if (type.equals("gold")) {
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(goldHeld.get(random.nextInt(goldHeld.size()))));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
        }
        if (type.equals("diamond")) {
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(diamondHeld.get(random.nextInt(diamondHeld.size()))));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            if (random.nextFloat() < 0.25F)
                entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        }
    }
}