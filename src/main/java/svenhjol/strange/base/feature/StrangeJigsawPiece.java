package svenhjol.strange.base.feature;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Dynamic;
import net.minecraft.util.IDynamicDeserializer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.jigsaw.IJigsawDeserializer;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.jigsaw.SingleJigsawPiece;
import net.minecraft.world.gen.feature.template.*;

import java.util.List;
import java.util.Random;

public class StrangeJigsawPiece extends SingleJigsawPiece {
    protected final ResourceLocation location;
    protected final ImmutableList<StructureProcessor> processors;
    public static IJigsawDeserializer STRANGE_POOL_ELEMENT;

    public StrangeJigsawPiece(String location, List<StructureProcessor> processors) {
        super(location, processors, JigsawPattern.PlacementBehaviour.RIGID);
        this.location = new ResourceLocation(location);
        this.processors = ImmutableList.copyOf(processors);
    }

    public StrangeJigsawPiece(Dynamic<?> dyn) {
        super(dyn);
        this.location = new ResourceLocation(dyn.get("location").asString(""));
        this.processors = ImmutableList.copyOf(dyn.get("processors").asList((p_214858_0_)
            -> IDynamicDeserializer.func_214907_a(p_214858_0_, Registry.STRUCTURE_PROCESSOR, "processor_type", NopProcessor.INSTANCE)));
    }

    @Override
    protected PlacementSettings createPlacementSettings(Rotation rotationIn, MutableBoundingBox boundsIn) {
        PlacementSettings placementsettings = new PlacementSettings();
        placementsettings.setBoundingBox(boundsIn);
        placementsettings.setRotation(rotationIn);
        placementsettings.func_215223_c(true);
        placementsettings.setIgnoreEntities(false);
        placementsettings.addProcessor(JigsawReplacementStructureProcessor.INSTANCE);
        this.processors.forEach(placementsettings::addProcessor);
        this.getPlacementBehaviour().getStructureProcessors().forEach(placementsettings::addProcessor);
        return placementsettings;
    }

    public String toString() {
        return "Strange[" + this.location + "]";
    }

    @Override
    public IJigsawDeserializer getType() {
        return STRANGE_POOL_ELEMENT;
    }

    @Override
    public boolean func_225575_a_(TemplateManager templateManagerIn, IWorld worldIn, ChunkGenerator<?> gen, BlockPos pos, Rotation rotationIn, MutableBoundingBox boundsIn, Random rand) {
        // bodge metadata into the author tag so we can determine what type of "air" to use for template
        Template template = templateManagerIn.getTemplateDefaulted(this.location);
        String loc = this.location.toString();
        if (loc.contains("underwater")) {
            template.setAuthor(DecorationProcessor.WATER);
        } else if (loc.contains("underlava")) {
            template.setAuthor(DecorationProcessor.LAVA);
        }
        return super.func_225575_a_(templateManagerIn, worldIn, gen, pos, rotationIn, boundsIn, rand);
    }
}
