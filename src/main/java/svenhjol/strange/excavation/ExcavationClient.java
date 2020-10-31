package svenhjol.strange.excavation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import svenhjol.charm.base.CharmModule;
import svenhjol.strange.module.Excavation;

@Environment(EnvType.CLIENT)
public class ExcavationClient {
    public ExcavationClient(CharmModule module) {
        BlockEntityRendererRegistry.INSTANCE.register(Excavation.BLOCK_ENTITY, AncientRubbleBlockEntityRenderer::new);
    }
}