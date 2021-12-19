package svenhjol.strange.module.quests.helper;

import net.minecraft.world.entity.player.Player;
import svenhjol.strange.module.quests.Quest;
import svenhjol.strange.module.quests.QuestData;
import svenhjol.strange.module.quests.Quests;
import svenhjol.strange.module.quests.exception.QuestException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class QuestHelper {
    public static final UUID ANY_UUID = UUID.fromString("0-0-0-0-0");
    public static final int MAX_QUESTS = 3;

    public static QuestException makeException(Quest quest, String message) {
        Quests.getQuestData().ifPresent(quests -> quests.remove(quest));
        return new QuestException(message);
    }

    public static Optional<Quest> getFirstSatisfiedQuest(Player player) {
        QuestData quests = Quests.getQuestData().orElseThrow();
        List<Quest> playerQuests = quests.all(player);
        return playerQuests.stream().filter(q -> q.isSatisfied(player)).findFirst();
    }
}
