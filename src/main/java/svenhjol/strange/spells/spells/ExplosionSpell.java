package svenhjol.strange.spells.spells;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ExplosionSpell extends Spell
{
    public ExplosionSpell()
    {
        super("explosion");
        this.element = Element.FIRE;
        this.affect = Affect.TARGET;
        this.applyCost = 5;
        this.duration = 3.0F;
        this.castCost = 20;
    }

    @Override
    public void cast(PlayerEntity player, ItemStack staff, Consumer<Boolean> didCast)
    {
        List<RayTraceResult.Type> respondTo = new ArrayList<>(Arrays.asList(
            RayTraceResult.Type.BLOCK,
            RayTraceResult.Type.ENTITY
        ));

        this.castTarget(player, respondTo, (result, beam) -> {
            BlockPos pos = null;

            if (result.getType() == RayTraceResult.Type.BLOCK) {
                pos = ((BlockRayTraceResult) result).getPos();
            } else if (result.getType() == RayTraceResult.Type.ENTITY && !((EntityRayTraceResult)result).getEntity().isEntityEqual(player)) {
                pos = ((EntityRayTraceResult) result).getEntity().getPosition();
            }

            if (pos != null) {
                beam.remove();
                player.world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), 4.0F, Explosion.Mode.BREAK);
                didCast.accept(true);
                return;
            }

            didCast.accept(false);
        });
    }
}
