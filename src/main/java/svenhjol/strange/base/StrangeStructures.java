package svenhjol.strange.base;

import net.minecraft.block.Blocks;
import svenhjol.charm.base.handler.ModuleHandler;
import svenhjol.charm.base.structure.DataBlockProcessor;
import svenhjol.strange.module.Excavation;
import svenhjol.strange.module.Runestones;
import svenhjol.strange.runestones.RunestoneHelper;

public class StrangeStructures {
    public static void init() {
        DataBlockProcessor.callbacks.put("rubble", processor -> {
            if (processor.random.nextFloat() < 0.9F)
                processor.state = Excavation.ANCIENT_RUBBLE.getDefaultState();
        });

        DataBlockProcessor.callbacks.put("runestone", processor -> {
            if (ModuleHandler.enabled("strange:runestones")
                && processor.random.nextFloat() < 0.75F) {
                processor.state = Runestones.RUNESTONE_BLOCKS.get(processor.random.nextInt(RunestoneHelper.NUMBER_OF_RUNES)).getDefaultState();
            } else {
                processor.state = Blocks.STONE.getDefaultState();
            }
        });
    }
}
