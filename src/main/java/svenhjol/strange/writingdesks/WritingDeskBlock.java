package svenhjol.strange.writingdesks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import svenhjol.charm.base.CharmModule;
import svenhjol.charm.base.block.CharmBlock;

import javax.annotation.Nullable;
import java.util.Random;

public class WritingDeskBlock extends CharmBlock {
    public static final IntProperty VARIANT = IntProperty.of("variant", 0, 3);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final VoxelShape TOP;
    public static final VoxelShape LEGS;
    public static final VoxelShape COLLISION_SHAPE;
    public static final VoxelShape OUTLINE_SHAPE;
    public static final int NUM_VARIANTS = 4;

    public WritingDeskBlock(CharmModule module) {
        super(module, WritingDesks.BLOCK_ID.getPath(), Settings.copy(Blocks.CARTOGRAPHY_TABLE));
        this.setDefaultState(getDefaultState().with(VARIANT, 0));
    }

    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return COLLISION_SHAPE;
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        Random r = new Random(pos.asLong());
        world.setBlockState(pos, state.with(VARIANT, r.nextInt(NUM_VARIANTS)));
    }

    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public ItemGroup getItemGroup() {
        return ItemGroup.DECORATIONS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, VARIANT);
    }

    static {
        TOP = Block.createCuboidShape(0.0D, 13.0D, 0.0D, 16.0D, 13.0D, 16.0D);
        LEGS = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 12.0D, 15.0D);
        COLLISION_SHAPE = VoxelShapes.union(TOP, LEGS);
        OUTLINE_SHAPE = VoxelShapes.union(TOP, LEGS);
    }
}
