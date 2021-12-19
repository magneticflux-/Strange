package svenhjol.strange.module.quests.definition;

import com.google.gson.Gson;
import net.minecraft.server.packs.resources.Resource;
import svenhjol.strange.module.runes.Tier;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestDefinition {
    private int tier;
    private String id;
    private String pack;
    private List<String> modules = new ArrayList<>();
    private List<String> dimensions = new ArrayList<>();
    private Map<String, Map<String, String>> lang = new HashMap<>();

    // TODO: why are you this
    private final Map<String, String> hunt = new HashMap<>();
    private final Map<String, Map<String, Map<String, String>>> gather = new HashMap<>();
    private final Map<String, Map<String, Map<String, String>>> explore = new HashMap<>();
    private final Map<String, Map<String, Map<String, String>>> boss = new HashMap<>();
    private final Map<String, Map<String, Map<String, String>>> reward = new HashMap<>();

    public static QuestDefinition deserialize(Resource resource) {
        Reader reader = new InputStreamReader(resource.getInputStream());
        return new Gson().fromJson(reader, QuestDefinition.class);
    }

    public String getId() {
        return id;
    }

    public Tier getTier() {
        return Tier.byOrdinal(tier);
    }

    public String getPack() {
        return pack;
    }

    public List<String> getModules() {
        return modules;
    }

    public List<String> getDimensions() {
        return dimensions;
    }

    public Map<String, Map<String, String>> getLang() {
        return lang;
    }

    public Map<String, String> getHunt() {
        return hunt;
    }

    public Map<String, Map<String, Map<String, String>>> getGather() {
        return gather;
    }

    public Map<String, Map<String, Map<String, String>>> getExplore() {
        return explore;
    }

    public Map<String, Map<String, Map<String, String>>> getBoss() {
        return boss;
    }

    public Map<String, Map<String, Map<String, String>>> getReward() {
        return reward;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTier(Tier tier) {
        this.tier = tier.ordinal();
    }
}
