package svenhjol.strange.module.writing_desks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import svenhjol.strange.module.journals.JournalHelper;
import svenhjol.strange.module.journals.Journals;
import svenhjol.strange.module.knowledge.Knowledge;
import svenhjol.strange.module.knowledge.KnowledgeHelper;
import svenhjol.strange.module.runic_tomes.RunicTomeItem;

import java.util.UUID;

public class WritingDeskMenu extends AbstractContainerMenu {
    public static final int DELETE = -1;

    private final Player player;
    private final ContainerLevelAccess access;
    private final Container inputSlots = new SimpleContainer(2) {

    };
    private final ResultContainer resultSlots = new ResultContainer();

    public WritingDeskMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, ContainerLevelAccess.NULL);
    }

    public WritingDeskMenu(int syncId, Inventory playerInventory, ContainerLevelAccess access) {
        super(WritingDesks.WRITING_DESK_MENU, syncId);

        this.access = access;
        this.player = playerInventory.player;

        // book slot
        this.addSlot(new Slot(inputSlots, 0, 134, 35) {
            @Override
            public void setChanged() {
                super.setChanged();
                Player player = WritingDeskMenu.this.player;

                if (!player.level.isClientSide) {
                    clearWrittenRunes(player);
                    clearResult();
                }
                slotsChanged(inputSlots);
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == Items.BOOK;
            }
        });

        // ink slot
        this.addSlot(new Slot(inputSlots, 1, 134, 57) {
            @Override
            public void setChanged() {
                super.setChanged();
                Player player = WritingDeskMenu.this.player;

                if (!player.level.isClientSide) {
                    clearWrittenRunes(player);
                    clearResult();
                }

                slotsChanged(inputSlots);
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == Items.INK_SAC;
            }
        });

        // output tome slot
        this.addSlot(new Slot(resultSlots, 2, 275, 45) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return true;
            }

            @Override
            public void onTake(Player player, ItemStack itemStack) {
                WritingDeskMenu.this.onTake(player, itemStack);
            }
        });

        // TODO: abstract this
        int k;
        for(k = 0; k < 3; ++k) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + k * 9 + 9, 132 + j * 18, 139 + k * 18));
            }
        }

        for(k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 132 + k * 18, 197));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> {
            if (!this.isValidBlock(level.getBlockState(pos))) return false;
            return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
        }, true);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> {
            this.clearContainer(player, this.inputSlots);
        });
    }

    private void onTake(Player player, ItemStack stack) {
        boolean isCreative = player.getAbilities().instabuild;

        if (!isCreative) {
            ItemStack books = this.inputSlots.getItem(0);
            ItemStack ink = this.inputSlots.getItem(1);

            books.shrink(1);
            ink.shrink(1);

            this.inputSlots.setItem(0, books);
            this.inputSlots.setItem(1, ink);
        }

        clearWrittenRunes(player);

        this.access.execute((level, pos) -> {
            // TODO: this needs to be a custom sound effect
            level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
        });
    }

    private void clearResult() {
        resultSlots.setItem(0, ItemStack.EMPTY);
    }

    private boolean isValidBlock(BlockState state) {
        return state.getBlock() == WritingDesks.WRITING_DESK;
    }

    /**
     * A single int is passed from the screen to the menu.
     * The int represents the clicked/typed rune (0-25).
     * -1 represents the "delete rune" click or backspace.
     */
    @Override
    public boolean clickMenuButton(Player player, int i) {
        if (player.level.isClientSide) {
            return false;
        }

        ServerPlayer serverPlayer = (ServerPlayer)player;
        UUID uuid = serverPlayer.getUUID();
        String runes = WritingDesks.writtenRunes.computeIfAbsent(uuid, s -> "");

        // don't allow writing if there is no book or ink
        if (inputSlots.getItem(0).isEmpty() || inputSlots.getItem(1).isEmpty()) {
            clearWrittenRunes(serverPlayer);
            return false;
        }

        if (i == DELETE) {
            runes = runes.substring(0, runes.length() - 1);
        } else if (runes.length() <= Knowledge.MAX_LENGTH) {
            runes += String.valueOf((char)(i + Knowledge.ALPHABET_START));
        }

        WritingDesks.writtenRunes.put(uuid, runes);
        checkAndUpdateResult(serverPlayer);

        return true;
    }

    public void checkAndUpdateResult(ServerPlayer player) {
        String runes = WritingDesks.writtenRunes.get(player.getUUID());
        boolean validRuneString = KnowledgeHelper.isValidRuneString(runes);
        boolean hasBook = !inputSlots.getItem(0).isEmpty();
        boolean hasInk = !inputSlots.getItem(1).isEmpty();

        if (hasBook && hasInk && validRuneString) {

            // It's possible that the player found out these runes through other means.
            // If it's a valid string, then add this to the player's journal so
            // that it's available to them next time they want to write a tome.
            Journals.getJournalData(player).ifPresent(journal -> {
                JournalHelper.tryLearnPhrase(runes, journal);
                Journals.sendSyncJournal(player);
            });

            ItemStack tome = RunicTomeItem.create(runes, player);
            resultSlots.setItem(0, tome);
        } else {
            clearResult();
        }
    }

    public void clearWrittenRunes(Player player) {
        WritingDesks.writtenRunes.remove(player.getUUID());
    }

    public ItemStack quickMoveStack(Player player, int index) {
        // copypasta. Can this be abstracted?
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            stack = stackInSlot.copy();
            if (index < this.inputSlots.getContainerSize()) {
                if (!this.moveItemStackTo(stackInSlot, this.inputSlots.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, this.inputSlots.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return stack;
    }
}
