package svenhjol.strange.module.journals.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.Arrays;
import java.util.List;

public class JournalKnowledgeScreen extends BaseJournalScreen {
    protected boolean hasRenderedKnowledgeButtons = false;
    protected List<ButtonDefinition> knowledgeButtons;

    protected JournalKnowledgeScreen() {
        this(new TranslatableComponent("gui.strange.journal.knowledge"));
    }

    public JournalKnowledgeScreen(Component component) {
        super(component);

        knowledgeButtons = Arrays.asList(
            new ButtonDefinition(b -> onClose(),
                new TranslatableComponent("gui.strange.journal.learned_runes"),
                new TranslatableComponent("gui.strange.journal.learned_runes")),

            new ButtonDefinition(b -> onClose(),
                new TranslatableComponent("gui.strange.journal.learned_biomes")),

            new ButtonDefinition(b -> onClose(),
                new TranslatableComponent("gui.strange.journal.learned_structures")),

            new ButtonDefinition(b -> onClose(),
                new TranslatableComponent("gui.strange.journal.learned_dimensions")),

            new ButtonDefinition(b -> onClose(),
                new TranslatableComponent("gui.strange.journal.learned_players"))
        );
    }

    @Override
    protected void init() {
        super.init();
        hasRenderedKnowledgeButtons = false;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        super.render(poseStack, mouseX, mouseY, delta);
        renderButtons(poseStack);
    }

    public void renderButtons(PoseStack poseStack) {
        if (!hasRenderedKnowledgeButtons) {
            int buttonWidth = 100;
            int buttonHeight = 20;
            int x = (width / 2) + 5;
            int y = 24;
            int yOffset = 24;

            renderButtons(knowledgeButtons, x, y, 0, yOffset, buttonWidth, buttonHeight);
            hasRenderedKnowledgeButtons = true;
        }
    }
}
