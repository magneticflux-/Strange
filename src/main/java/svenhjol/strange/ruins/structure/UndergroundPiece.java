package svenhjol.strange.ruins.structure;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.jigsaw.JigsawPiece;
import net.minecraft.world.gen.feature.structure.AbstractVillagePiece;
import net.minecraft.world.gen.feature.structure.IStructurePieceType;
import net.minecraft.world.gen.feature.template.TemplateManager;
import svenhjol.strange.Strange;

import java.util.Random;

public class UndergroundPiece extends AbstractVillagePiece {
    public static final IStructurePieceType PIECE = UndergroundPiece::new;
    protected final TemplateManager templates;

    public UndergroundPiece(TemplateManager templates, JigsawPiece piece, BlockPos pos, int groundLevelDelta, Rotation rotation, MutableBoundingBox bounds) {
        super(PIECE, templates, piece, pos, groundLevelDelta, rotation, bounds);
        this.templates = templates;
    }

    public UndergroundPiece(TemplateManager templates, CompoundNBT nbt) {
        super(templates, nbt, PIECE);
        this.templates = templates;
    }

    @Override
    public boolean func_225577_a_(IWorld world, ChunkGenerator<?> gen, Random rand, MutableBoundingBox structureBox, ChunkPos chunk) {
        boolean result;

        try {
            result = super.func_225577_a_(world, gen, rand, structureBox, chunk);
        } catch (NullPointerException e) {
            Strange.LOG.warn("NullPointer when generating piece, FIXME: " + e);
            result = false;
        }

        return result;
    }
}
