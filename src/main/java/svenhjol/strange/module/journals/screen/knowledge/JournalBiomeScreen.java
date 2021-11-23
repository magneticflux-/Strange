package svenhjol.strange.module.journals.screen.knowledge;

import net.minecraft.resources.ResourceLocation;
import svenhjol.strange.helper.GuiHelper;
import svenhjol.strange.module.journals.JournalViewer;
import svenhjol.strange.module.journals.Journals;
import svenhjol.strange.module.knowledge.KnowledgeBranch;
import svenhjol.strange.module.knowledge.KnowledgeData;

public class JournalBiomeScreen extends JournalResourceScreen {
    public JournalBiomeScreen(ResourceLocation biome) {
        super(biome);

        // add a back button at the bottom
        this.bottomButtons.add(0, new GuiHelper.ButtonDefinition(b -> biomes(), GO_BACK));
    }

    @Override
    public KnowledgeBranch<?, ResourceLocation> getBranch(KnowledgeData knowledge) {
        return knowledge.biomes;
    }

    @Override
    protected void setViewedPage() {
        JournalViewer.viewedResource(Journals.Page.BIOME, resource);
    }
}