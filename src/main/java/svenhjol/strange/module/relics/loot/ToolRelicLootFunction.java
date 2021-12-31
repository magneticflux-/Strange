package svenhjol.strange.module.relics.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import svenhjol.strange.module.relics.Relics;
import svenhjol.strange.module.relics.helper.RelicHelper;

public class ToolRelicLootFunction extends LootItemConditionalFunction {
    public ToolRelicLootFunction(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        var random = context.getRandom();
        var relic = RelicHelper.getRandomItem(Relics.Type.TOOL, random);
        if (relic == null) return stack;

        return RelicHelper.getStackWithDamage(relic, random);
    }

    @Override
    public LootItemFunctionType getType() {
        return Relics.TOOL_LOOT_FUNCTION;
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<ToolRelicLootFunction> {

        @Override
        public ToolRelicLootFunction deserialize(JsonObject json, JsonDeserializationContext context, LootItemCondition[] conditions) {
            return new ToolRelicLootFunction(conditions);
        }
    }
}