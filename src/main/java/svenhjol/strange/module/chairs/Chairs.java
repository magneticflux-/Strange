package svenhjol.strange.module.chairs;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import svenhjol.charm.annotation.CommonModule;
import svenhjol.charm.annotation.Config;
import svenhjol.charm.helper.LogHelper;
import svenhjol.charm.loader.CharmModule;
import svenhjol.charm.registry.CommonRegistry;
import svenhjol.strange.Strange;

/**
 * Inspired by Quark's SitInStairs module.
 */
@CommonModule(mod = Strange.MOD_ID, description = "Experimental! While holding no items, right-click on any stairs block to sit down.")
public class Chairs extends CharmModule {
    public static EntityType<ChairEntity> CHAIR;

    @Config(name = "Restrict rotation", description = "If true, the seated player will sit facing the direction of the stairs and not be able to rotate.")
    public static boolean restrictRotation = true;

    @Override
    public void register() {
        CHAIR = CommonRegistry.entity(new ResourceLocation(Strange.MOD_ID, "chair"), FabricEntityTypeBuilder
            .<ChairEntity>create(MobCategory.MISC, ChairEntity::new)
            .trackedUpdateRate(1)
            .trackRangeBlocks(1)
            .dimensions(EntityDimensions.fixed(0.25F, 0.25F)));
    }

    @Override
    public void runWhenEnabled() {
        UseBlockCallback.EVENT.register(this::handleUseBlock);
    }

    private InteractionResult handleUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide
            && player.getOffhandItem().isEmpty()
            && player.getMainHandItem().isEmpty()
            && !player.isPassenger()
            && !player.isCrouching()
        ) {
            var pos = hitResult.getBlockPos();
            var state = level.getBlockState(pos);
            var stateAbove = level.getBlockState(pos.above());
            var block = state.getBlock();

            if (block instanceof StairBlock
                && state.getValue(StairBlock.HALF) == Half.BOTTOM
                && !stateAbove.isCollisionShapeFullBlock(level, pos.above())
            ) {
                var chair = new ChairEntity(level, pos);
                level.addFreshEntity(chair);
                LogHelper.debug(getClass(), "Added new chair entity");

                var result = player.startRiding(chair);
                LogHelper.debug(getClass(), "Player is now riding");
                if (result) {
                    player.moveTo(chair.getX(), chair.getY(), chair.getZ());
                    player.setPos(chair.getX(), chair.getY(), chair.getZ());
                    LogHelper.debug(getClass(), "Moved player to chair pos");
                }

                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }
}
