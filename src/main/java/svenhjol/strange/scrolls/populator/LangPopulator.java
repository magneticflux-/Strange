package svenhjol.strange.scrolls.populator;

import net.minecraft.server.network.ServerPlayerEntity;
import svenhjol.strange.scrolls.Scrolls;
import svenhjol.strange.scrolls.JsonDefinition;
import svenhjol.strange.scrolls.tag.Quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LangPopulator extends Populator {
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";

    public LangPopulator(ServerPlayerEntity player, Quest quest, JsonDefinition definition) {
        super(player, quest, definition);
    }

    @Override
    public void populate() {
        Map<String, Map<String, String>> def = definition.getLang();
        if (def.isEmpty())
            return;

        if (def.containsKey(Scrolls.language)) {
            Map<String, String> strings = def.get(Scrolls.language);
            List<String> keys = new ArrayList<>(strings.keySet());

            if (keys.contains(TITLE))
                quest.setTitle(strings.get(TITLE));

            if (keys.contains(DESCRIPTION))
                quest.setDescription(strings.get(DESCRIPTION));
        }
    }
}
