package svenhjol.strange.module.journals2.screen.knowledge;

import svenhjol.strange.module.journals2.Journals2Client;
import svenhjol.strange.module.journals2.PageTracker;
import svenhjol.strange.module.journals2.paginator.BiomePaginator;
import svenhjol.strange.module.journals2.paginator.ResourcePaginator;

import java.util.List;

public class JournalBiomesScreen extends JournalResourcesScreen {
    public JournalBiomesScreen() {
        super(LEARNED_BIOMES);
    }

    @Override
    protected ResourcePaginator getPaginator() {
        var journal = Journals2Client.journal;
        return new BiomePaginator(journal != null ? journal.getLearnedBiomes() : List.of());
    }

    @Override
    protected void setViewedPage() {
        Journals2Client.tracker.setPage(PageTracker.Page.BIOMES, offset);
    }
}