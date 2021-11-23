package svenhjol.strange.module.potion_of_hogsbane;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import svenhjol.charm.loader.CharmModule;
import svenhjol.charm.potion.CharmPotion;

public class HogsbanePotion extends CharmPotion {
    public HogsbanePotion(CharmModule module) {
        super(module, "hogsbane", new MobEffectInstance(PotionOfHogsbane.HOGSBANE_EFFECT, 3600));
        registerRecipe(Potions.AWKWARD, Items.WARPED_FUNGUS);
    }
}
