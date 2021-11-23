package svenhjol.strange.module.potion_of_spelunking;

import net.minecraft.world.effect.MobEffectCategory;
import svenhjol.charm.loader.CharmModule;
import svenhjol.charm.potion.CharmStatusEffect;

public class SpelunkingEffect extends CharmStatusEffect {
    public SpelunkingEffect(CharmModule module) {
        super(module, "spelunking", MobEffectCategory.NEUTRAL, 0xC09FDD);
    }
}
