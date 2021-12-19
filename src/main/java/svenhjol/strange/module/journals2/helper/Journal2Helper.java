package svenhjol.strange.module.journals2.helper;

import svenhjol.strange.module.journals2.Journal2Data;
import svenhjol.strange.module.journals2.Journals2Client;
import svenhjol.strange.module.runes.RuneBranch;
import svenhjol.strange.module.runes.Tier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class Journal2Helper {
    public static List<Integer> getLearnedRunes() {
        if (Journals2Client.journal == null) return List.of();
        return Journals2Client.journal.getLearnedRunes();
    }

    public static int nextLearnableRune(Tier currentTier, Journal2Data journal) {
        var learnedRunes = journal.getLearnedRunes();

        for (int t = 1; t <= currentTier.ordinal(); t++) {
            var tier = Tier.byOrdinal(t);
            if (tier == null) continue;
            var chars = tier.getChars();

            for (char c : chars) {
                int intval = (int) c - 97;
                if (!learnedRunes.contains(intval)) {
                    return intval;
                }
            }
        }

        return Integer.MIN_VALUE;
    }

    public static <T> boolean learnFromBranch(RuneBranch<?, T> branch, List<T> knowledge, Function<T, Boolean> onLearn) {
        // If the current knowledge is less than the knowledge in the branch then there's something to be learned.
        if (knowledge.size() < branch.size()) {
            var list = new ArrayList<T>(branch.values());
            Collections.shuffle(list, new Random());

            for (T item : list) {
                if (!knowledge.contains(item)) {
                    return onLearn.apply(item);
                }
            }
        }

        // Did not learn anything.
        return false;
    }
}
