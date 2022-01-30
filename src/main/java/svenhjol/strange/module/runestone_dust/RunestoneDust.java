package svenhjol.strange.module.runestone_dust;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import svenhjol.charm.annotation.CommonModule;
import svenhjol.charm.loader.CharmModule;
import svenhjol.charm.registry.CommonRegistry;
import svenhjol.strange.Strange;

@CommonModule(mod = Strange.MOD_ID, description = "Runestone dust can be used to show the heading of the nearest stone circle.\n" +
    "It is obtainable by mining a runestone.")
public class RunestoneDust extends CharmModule {
    public static final ResourceLocation RUNESTONE_DUST_ID = new ResourceLocation(Strange.MOD_ID, "runestone_dust");
    public static RunestoneDustItem RUNESTONE_DUST;
    public static EntityType<RunestoneDustEntity> RUNESTONE_DUST_ENTITY;
    public static SoundEvent RUNESTONE_DUST_SOUND;

    @Override
    public void register() {
        // setup runestone dust item and entity
        RUNESTONE_DUST = new RunestoneDustItem(this);
        RUNESTONE_DUST_ENTITY = CommonRegistry.entity(RUNESTONE_DUST_ID, FabricEntityTypeBuilder
            .<RunestoneDustEntity>create(MobCategory.MISC, RunestoneDustEntity::new)
            .trackRangeBlocks(80)
            .trackedUpdateRate(10)
            .dimensions(EntityDimensions.fixed(2.0F, 2.0F)));

        RUNESTONE_DUST_SOUND = CommonRegistry.sound(new ResourceLocation(Strange.MOD_ID, "runestone_dust"));
    }
}