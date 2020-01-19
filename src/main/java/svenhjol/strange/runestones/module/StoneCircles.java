package svenhjol.strange.runestones.module;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.placement.IPlacementConfig;
import net.minecraft.world.gen.placement.Placement;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import svenhjol.meson.MesonModule;
import svenhjol.meson.handler.RegistryHandler;
import svenhjol.meson.helper.BiomeHelper;
import svenhjol.meson.iface.Config;
import svenhjol.meson.iface.Module;
import svenhjol.strange.Strange;
import svenhjol.strange.base.StrangeCategories;
import svenhjol.strange.base.helper.StructureHelper.RegisterJigsawPieces;
import svenhjol.strange.ruins.module.UndergroundRuins;
import svenhjol.strange.runestones.structure.StoneCirclePiece;
import svenhjol.strange.runestones.structure.StoneCircleStructure;
import svenhjol.strange.runestones.structure.VaultPiece;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Module(mod = Strange.MOD_ID, category = StrangeCategories.RUNESTONES, configureEnabled = false)
public class StoneCircles extends MesonModule
{
    public static final String NAME = "stone_circle";
    public static final String RESNAME = "strange:stone_circle";
    public static final String VAULTS_DIR = "vaults";
    public static Structure<NoFeatureConfig> structure;

    @Config(name = "Vault generation chance", description = "Chance (out of 1.0) of vaults generating beneath a stone circle.")
    public static double vaultChance = 0.66D;

    @Config(name = "Vault generation size", description = "Maximum number of rooms generated in any vault corridor.")
    public static int vaultSize = 6;

    @Config(name = "Outer stone circles only", description = "If true, vaults will only generate under stone circles in 'outer lands'.\n" +
        "This has no effect if the Outerlands module is disabled.")
    public static boolean outerOnly = true;

    @Config(name = "Allowed biomes", description = "Biomes that stone circles may generate in.")
    public static List<String> biomesConfig = new ArrayList<>(Arrays.asList(
        BiomeHelper.getBiomeName(Biomes.PLAINS),
        BiomeHelper.getBiomeName(Biomes.BADLANDS),
        BiomeHelper.getBiomeName(Biomes.MUSHROOM_FIELDS),
        BiomeHelper.getBiomeName(Biomes.SUNFLOWER_PLAINS),
        BiomeHelper.getBiomeName(Biomes.DESERT),
        BiomeHelper.getBiomeName(Biomes.DESERT_LAKES),
        BiomeHelper.getBiomeName(Biomes.BEACH),
        BiomeHelper.getBiomeName(Biomes.SAVANNA),
        BiomeHelper.getBiomeName(Biomes.SNOWY_TUNDRA),
        BiomeHelper.getBiomeName(Biomes.SNOWY_BEACH),
        BiomeHelper.getBiomeName(Biomes.SWAMP),
        BiomeHelper.getBiomeName(Biomes.END_BARRENS),
        BiomeHelper.getBiomeName(Biomes.END_HIGHLANDS),
        BiomeHelper.getBiomeName(Biomes.END_MIDLANDS),
        BiomeHelper.getBiomeName(Biomes.NETHER)
    ));

    public static List<Biome> validBiomes = new ArrayList<>();

    @Override
    public boolean isEnabled()
    {
        return super.isEnabled() && Strange.hasModule(Runestones.class);
    }

    @Override
    public void init()
    {
        structure = new StoneCircleStructure();

        RegistryHandler.registerStructure(structure, new ResourceLocation(Strange.MOD_ID, NAME));
        RegistryHandler.registerStructurePiece(StoneCirclePiece.PIECE, new ResourceLocation(Strange.MOD_ID, "scp"));
        RegistryHandler.registerStructurePiece(VaultPiece.PIECE, new ResourceLocation(Strange.MOD_ID, "vp"));

        biomesConfig.forEach(biomeName -> {
            Biome biome = Registry.BIOME.getOrDefault(new ResourceLocation(biomeName));
            if (!validBiomes.contains(biome)) validBiomes.add(biome);
        });

        validBiomes.forEach(biome -> {
            biome.addFeature(
                GenerationStage.Decoration.UNDERGROUND_STRUCTURES,
                Biome.createDecoratedFeature(structure, IFeatureConfig.NO_FEATURE_CONFIG, Placement.NOPE, IPlacementConfig.NO_PLACEMENT_CONFIG));

            biome.addStructure(structure, IFeatureConfig.NO_FEATURE_CONFIG);
        });

        UndergroundRuins.blacklist.add(structure);
    }

    @Override
    public void serverStarted(FMLServerStartedEvent event)
    {
        new RegisterJigsawPieces(event.getServer().getResourceManager(), VAULTS_DIR);
    }
}
