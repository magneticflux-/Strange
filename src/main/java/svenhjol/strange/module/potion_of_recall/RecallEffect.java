package svenhjol.strange.module.potion_of_recall;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import svenhjol.charm.loader.CharmModule;
import svenhjol.charm.potion.CharmStatusEffect;
import svenhjol.strange.module.teleport.Teleport;
import svenhjol.strange.module.teleport.ticket.TeleportTicket;

public class RecallEffect extends CharmStatusEffect {
    protected RecallEffect(CharmModule module) {
        super(module, "recall", MobEffectCategory.NEUTRAL, 0xE0FF33);
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    @Override
    public boolean isDurationEffectTick(int i, int j) {
        return i >= 1;
    }

    @Override
    public void applyInstantenousEffect(@Nullable Entity attacker1, @Nullable Entity attacker2, LivingEntity livingEntity, int amplifier, double d) {
        if (!livingEntity.level.isClientSide) {
            var serverLevel = (ServerLevel) livingEntity.level;
            var overworld = ServerLevel.OVERWORLD.location();

            // remove recall effect so it doesn't try and teleport multiple times
            livingEntity.removeEffect(this);

            // generate a teleportation ticket for the player to the spawn point
            var ticket = new TeleportTicket(livingEntity, overworld, livingEntity.blockPosition(), serverLevel.getSharedSpawnPos());
            ticket.useExactPosition(false);
            ticket.allowDimensionChange(true);
            Teleport.addTeleportTicket(ticket);
        } else {
            super.applyInstantenousEffect(attacker1, attacker2, livingEntity, amplifier, d);
        }
    }
}
