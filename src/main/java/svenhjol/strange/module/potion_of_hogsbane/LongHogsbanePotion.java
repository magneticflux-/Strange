package svenhjol.strange.module.potion_of_hogsbane;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import svenhjol.charm.loader.CharmModule;
import svenhjol.charm.potion.CharmPotion;

public class LongHogsbanePotion extends CharmPotion {
    public LongHogsbanePotion(CharmModule module) {
        super(module, "long_hogsbane", "hogsbane", new MobEffectInstance(PotionOfHogsbane.HOGSBANE_EFFECT, 9600));
        registerRecipe(PotionOfHogsbane.HOGSPANE_POTION, Items.REDSTONE);
    }
}
