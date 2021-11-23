package svenhjol.strange.module.glowballs;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.EntityType;
import svenhjol.charm.annotation.ClientModule;
import svenhjol.charm.event.ClientSpawnEntityCallback;
import svenhjol.charm.loader.CharmModule;

@ClientModule(module = Glowballs.class)
public class GlowballsClient extends CharmModule {

    @Override
    public void register() {
        EntityRendererRegistry.register(Glowballs.GLOWBALL, ThrownItemRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlock(Glowballs.GLOWBALL_BLOCK, RenderType.translucent());
    }

    @Override
    public void runWhenEnabled() {
        ClientSpawnEntityCallback.EVENT.register(this::handleClientSpawnEntity);
    }

    private void handleClientSpawnEntity(ClientboundAddEntityPacket packet, EntityType<?> entityType, ClientLevel level, double x, double y, double z) {
        if (entityType == Glowballs.GLOWBALL) {
            ClientSpawnEntityCallback.addEntity(packet, level, new GlowballEntity(level, x, y, z));
        }
    }
}
