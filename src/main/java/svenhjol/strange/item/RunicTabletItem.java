package svenhjol.strange.item;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import svenhjol.charm.Charm;
import svenhjol.charm.base.CharmModule;
import svenhjol.charm.base.helper.DimensionHelper;

public class RunicTabletItem extends TabletItem {
    public RunicTabletItem(CharmModule module, String name) {
        super(module, name, new Settings()
            .group(ItemGroup.MISC)
            .rarity(Rarity.UNCOMMON)
            .maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!(stack.getItem() instanceof TabletItem))
            return TypedActionResult.fail(stack);

        BlockPos pos = TabletItem.getPos(stack);
        if (pos == null)
            return TypedActionResult.fail(stack);

        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 1.0F, 1.15F);
        return TypedActionResult.success(stack);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        super.usageTick(world, user, stack, remainingUseTicks);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient)
            return;

        ServerWorld serverWorld = (ServerWorld)world;
        BlockPos pos = TabletItem.getPos(stack);
        Identifier dimension = TabletItem.getDimension(stack);
        RegistryKey<World> key = DimensionHelper.getDimension(dimension);

        if (key == null) {
            Charm.LOG.warn("Something bad happened with the dimension");
            return;
        }

        if (pos == null) {
            Charm.LOG.warn("Encoded position was null");
            return;
        }

        ServerWorld destinationWorld = serverWorld.getServer().getWorld(key);
        user.moveToWorld(destinationWorld);

        // TODO: move to helper, this is shared with Runestone code
        int surface = 0;

        for (int y = world.getHeight(); y >= 0; --y) {
            BlockPos n = new BlockPos(pos.getX(), y, pos.getZ());
            if (world.isAir(n) && !world.isAir(n.down())) {
                surface = y;
                break;
            }
        }

        if (surface <= 0) {
            Charm.LOG.warn("Failed to find a surface value to spawn the player");
            return;
        }

        user.teleport(pos.getX(), surface, pos.getZ());
        stack.damage(1, user, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 40000;
    }
}
