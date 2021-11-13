package svenhjol.strange.module.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import svenhjol.charm.helper.DimensionHelper;
import svenhjol.charm.helper.LogHelper;
import svenhjol.charm.helper.WorldHelper;
import svenhjol.strange.init.StrangeSounds;

import java.util.Random;
import java.util.function.Consumer;

public class EntityTeleportTicket {
    private int ticks;
    private boolean valid;
    private boolean chunkLoaded;
    private boolean success;
    private final boolean exactPosition;
    private final boolean allowDimensionChange;
    private final Consumer<EntityTeleportTicket> onSuccess;
    private final Consumer<EntityTeleportTicket> onFail;
    private final LivingEntity entity;
    private final ServerLevel level;
    private final ResourceLocation dimension;
    private final BlockPos from;
    private final BlockPos to;

    public EntityTeleportTicket(LivingEntity entity, ResourceLocation dimension, BlockPos from, BlockPos to, boolean exactPosition, boolean allowDimensionChange) {
        this(entity, dimension, from, to, exactPosition, allowDimensionChange, t -> {}, t -> {});
    }

    public EntityTeleportTicket(LivingEntity entity, ResourceLocation dimension, BlockPos from, BlockPos to, boolean exactPosition, boolean allowDimensionChange, Consumer<EntityTeleportTicket> onSuccess, Consumer<EntityTeleportTicket> onFail) {
        this.entity = entity;
        this.level = (ServerLevel)entity.level;
        this.dimension = dimension;
        this.from = from;
        this.to = to;
        this.exactPosition = exactPosition;
        this.allowDimensionChange = allowDimensionChange;
        this.onSuccess = onSuccess;
        this.onFail = onFail;
        this.valid = true;
        this.chunkLoaded = false;
        this.success = false;
        this.ticks = 0;
    }

    public void tick() {
        boolean sameDimension = DimensionHelper.isDimension(level, dimension);

        if (!sameDimension) {
            if (allowDimensionChange) {
                // TODO: dimension change
            } else {
                valid = false;
            }
        }

        if (!valid) {
            return;
        }

        if (!chunkLoaded) {
            chunkLoaded = openChunk();

            if (!chunkLoaded) {
                LogHelper.warn(this.getClass(), "Could not load destination chunk, giving up");
                valid = false;
                return;
            }

            level.playSound(null, from, StrangeSounds.RUNESTONE_TRAVEL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        if (ticks++ == Teleport.TELEPORT_TICKS) {
            BlockPos found;
            BlockPos target = to;
            Random r = new Random();

            if (exactPosition) {
                found = target;
            } else {
                int tries = 0;
                int maxTries = 5;
                do {
                    if (level.dimensionType().hasCeiling()) {
                        found = WorldHelper.getSurfacePos(level, target, Math.min(level.getSeaLevel() + 40, level.getLogicalHeight() - 20));
                    } else {
                        found = WorldHelper.getSurfacePos(level, target);
                    }
                    if (tries > 0) {
                        int x = to.getX() + (r.nextInt(tries * 2) - tries * 2);
                        int z = to.getZ() + (r.nextInt(tries * 2) - tries * 2);
                        target = new BlockPos(x, 0, z);
                    }
                } while (found == null && tries++ < maxTries);
            }

            if (found == null) {
                closeChunk();
                valid = false;
                return;
            }

            entity.teleportToWithTicket(found.getX(), found.getY(), found.getZ());
            closeChunk();
            success = true;
        }
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isSuccess() {
        return success;
    }

    public void onSuccess() {
        this.onSuccess.accept(this);
    }

    public void onFail() {
        this.onFail.accept(this);
    }

    private boolean openChunk() {
        return WorldHelper.addForcedChunk(level, to);
    }

    private void closeChunk() {
        WorldHelper.removeForcedChunk(level, to);
    }
}
