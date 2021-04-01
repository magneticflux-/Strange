package svenhjol.strange;

import net.fabricmc.api.ModInitializer;
import svenhjol.charm.Charm;
import svenhjol.charm.base.CharmLoader;
import svenhjol.strange.base.*;
import svenhjol.strange.bronze.Bronze;
import svenhjol.strange.rubble.Rubble;
import svenhjol.strange.ruins.Ruins;
import svenhjol.strange.runestones.Runestones;
import svenhjol.strange.scrollkeepers.Scrollkeepers;
import svenhjol.strange.scrolls.Scrolls;
import svenhjol.strange.stonecircles.StoneCircles;
import svenhjol.strange.totems.TotemOfFlying;
import svenhjol.strange.totems.TotemOfPreserving;
import svenhjol.strange.totems.TotemOfWandering;
import svenhjol.strange.traveljournals.TravelJournals;
import svenhjol.strange.treasure.Treasure;

import java.util.Arrays;

public class Strange implements ModInitializer {
    public static final String MOD_ID = "strange";

    @Override
    public void onInitialize() {
        Charm.runFirst();

        new CharmLoader(MOD_ID, Arrays.asList(
            Bronze.class,
            Rubble.class,
            Ruins.class,
            Runestones.class,
            Scrollkeepers.class,
            Scrolls.class,
            StoneCircles.class,
            TravelJournals.class,
            TotemOfFlying.class,
            TotemOfPreserving.class,
            TotemOfWandering.class,
            Treasure.class
        ));

        StrangeStructures.init();
        StrangeLoot.init();
        StrangeCommands.init();
        StrangeSounds.init();
    }
}
