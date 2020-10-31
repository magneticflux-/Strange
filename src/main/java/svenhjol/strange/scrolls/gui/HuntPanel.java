package svenhjol.strange.scrolls.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import svenhjol.strange.base.StrangeIcons;
import svenhjol.strange.scrolls.tag.Hunt;
import svenhjol.strange.scrolls.tag.Quest;

import java.util.ArrayList;
import java.util.Map;

public class HuntPanel extends Panel {
    public static HuntPanel INSTANCE = new HuntPanel();

    @Override
    public void render(Screen screen, MatrixStack matrices, Quest quest, int mid, int width, int top, int mouseX, int mouseY) {
        Hunt hunt = quest.getHunt();

        Map<Identifier, Integer> entities = hunt.getEntities();
        Map<Identifier, Integer> killed = hunt.getKilled();
        Map<Identifier, Boolean> satisfied = hunt.getSatisfied();
        Map<Identifier, String> names = hunt.getNames();

        if (entities.isEmpty())
            return;

        // panel title
        drawCenteredTitle(matrices, new TranslatableText("gui.strange.scrolls.hunt"), mid, top, titleColor);

        top += rowHeight;

        // render out entities with name and quantity
        ArrayList<Identifier> entityIds = new ArrayList<>(entities.keySet());

        // names
        int baseTop = top;
        for (Identifier entityId : entityIds) {
            // remaining is count - killed
            int remaining = Math.max(0, entities.get(entityId) - killed.getOrDefault(entityId, 0));
            String name = names.get(entityId);
            TranslatableText text = new TranslatableText("gui.strange.scrolls.hunt_entity", name, remaining);
            drawTextWithShadow(matrices, getTextRenderer(), text, mid - 36, baseTop, textColor);

            // if all of this type is killed, show a tick next to it
            if (satisfied.get(entityId))
                renderIcon(matrices, StrangeIcons.ICON_TICK, mid - 50, baseTop - 1);

            baseTop += rowHeight;
        }
    }
}