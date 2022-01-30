package svenhjol.strange.module.runestone_dust;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import svenhjol.charm.item.CharmItem;
import svenhjol.charm.loader.CharmModule;
import svenhjol.strange.Strange;
import svenhjol.strange.module.stone_circles.StoneCircles;

public class RunestoneDustItem extends CharmItem {
    public RunestoneDustItem(CharmModule module) {
        super(module, "runestone_dust", new FabricItemSettings()
            .tab(CreativeModeTab.TAB_MISC)
            .stacksTo(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!Strange.LOADER.isEnabled(StoneCircles.class))
            return InteractionResultHolder.fail(stack);

        if (!player.isCreative())
            stack.shrink(1);

        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();

        player.getCooldowns().addCooldown(this, 40);

        // client
        if (level.isClientSide) {
            player.swing(hand);
            level.playSound(player, x, y, z, RunestoneDust.RUNESTONE_DUST_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        // server
        if (!level.isClientSide) {
            ServerLevel serverWorld = (ServerLevel)level;

            BlockPos pos = serverWorld.findNearestMapFeature(StoneCircles.STONE_CIRCLE_FEATURE, player.blockPosition(), 1500, false);
            if (pos != null) {
                RunestoneDustEntity entity = new RunestoneDustEntity(level, pos.getX(), pos.getZ());
                Vec3 look = player.getLookAngle();

                entity.setPosRaw(x + look.x * 2, y + 0.5, z + look.z * 2);
                level.addFreshEntity(entity);
                return InteractionResultHolder.pass(stack);
            }
        }

        return InteractionResultHolder.success(stack);
    }
}