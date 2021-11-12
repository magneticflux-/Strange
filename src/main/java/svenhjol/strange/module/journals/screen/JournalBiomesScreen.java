package svenhjol.strange.module.journals.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import svenhjol.charm.helper.ClientHelper;
import svenhjol.strange.module.journals.JournalData;

import java.util.List;
import java.util.function.Supplier;

public class JournalBiomesScreen extends JournalResourcesScreen {
    public JournalBiomesScreen() {
        super(LEARNED_BIOMES);
    }

    @Override
    protected List<ResourceLocation> getResources(JournalData journal) {
        return journal.getLearnedBiomes();
    }

    @Override
    protected Supplier<Component> getLabelForNoItem() {
        return () -> NO_BIOMES;
    }

    @Override
    protected void select(ResourceLocation biome) {
        ClientHelper.getClient().ifPresent(client -> client.setScreen(new JournalBiomeScreen(biome)));
    }
}