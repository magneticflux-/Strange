package svenhjol.strange.runeportals;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.HashMap;
import java.util.Map;

public class RunePortalManager extends PersistentState {
    public static final String POSITIONS_NBT = "Positions";
    public static final String COLORS_NBT = "Colors";
    
    private final World world;
    private Map<BlockPos, BlockPos> positions = new HashMap<>();
    private Map<BlockPos, DyeColor> colors = new HashMap<>();

    public RunePortalManager(ServerWorld world) {
        this.world = world;
        markDirty();
    }
    
    public static RunePortalManager fromNbt(ServerWorld world, NbtCompound nbt) {
        RunePortalManager manager = new RunePortalManager(world);
        NbtCompound positions = (NbtCompound)nbt.get(POSITIONS_NBT);
        NbtCompound colors = (NbtCompound)nbt.get(COLORS_NBT);

        manager.positions = new HashMap<>();
        manager.colors = new HashMap<>();

        if (positions != null && !positions.isEmpty()) {
            positions.getKeys().forEach(s -> {
                BlockPos source = BlockPos.fromLong(Long.parseLong(s));
                BlockPos dest = BlockPos.fromLong(positions.getLong(s));
                manager.positions.put(source, dest);
            });
        }

        if (colors != null && !colors.isEmpty()) {
            colors.getKeys().forEach(s -> {
                BlockPos source = BlockPos.fromLong(Long.parseLong(s));
                DyeColor color = DyeColor.byId(colors.getInt(s));
                manager.colors.put(source, color);
            });
        }

        return manager;
    }
    
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound positionsNbt = new NbtCompound();
        NbtCompound colorNbt = new NbtCompound();

        positions.forEach((source, dest) ->
            positionsNbt.putLong(String.valueOf(source.asLong()), dest.asLong()));

        colors.forEach((source, dye) ->
            colorNbt.putInt(String.valueOf(source.asLong()), dye.getId()));

        nbt.put(POSITIONS_NBT, positionsNbt);
        nbt.put(COLORS_NBT, colorNbt);
        return nbt;
    }

    public static String nameFor(DimensionType dimensionType) {
        return "runeportals" + dimensionType.getSuffix();
    }

}