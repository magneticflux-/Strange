package svenhjol.strange.module.mobs;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.IllusionerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import svenhjol.charm.annotation.Config;
import svenhjol.charm.annotation.Module;
import svenhjol.charm.event.EntityDropItemsCallback;
import svenhjol.charm.module.CharmModule;
import svenhjol.strange.Strange;

@Module(mod = Strange.MOD_ID, description = "Additional configuration options for mobs that spawn in Strange.")
public class Mobs extends CharmModule {
    @Config(name = "Allow Illusioners", description = "If true, Illusioners will be allowed to spawn at stone circles.  Like Evokers, they drop a Totem of Undying when killed.")
    public static boolean illusioners = true;

    @Override
    public void init() {
        EntityDropItemsCallback.AFTER.register(this::tryDropTotemFromIllusioner);
    }

    private ActionResult tryDropTotemFromIllusioner(LivingEntity entity, DamageSource source, int lootingLevel) {
        if (illusioners && !entity.world.isClient && entity instanceof IllusionerEntity) {
            World world = entity.getEntityWorld();
            BlockPos pos = entity.getBlockPos();

            world.spawnEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.TOTEM_OF_UNDYING)));
        }

        return ActionResult.PASS;
    }
}