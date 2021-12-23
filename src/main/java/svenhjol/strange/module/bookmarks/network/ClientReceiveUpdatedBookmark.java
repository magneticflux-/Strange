package svenhjol.strange.module.bookmarks.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import svenhjol.charm.helper.LogHelper;
import svenhjol.strange.module.bookmarks.Bookmark;
import svenhjol.strange.module.bookmarks.BookmarksClient;
import svenhjol.strange.module.journals.screen.bookmark.JournalBookmarksScreen;
import svenhjol.strange.network.ClientReceiver;
import svenhjol.strange.network.Id;

/**
 * Client receives an updated bookmark.
 * Update the local copy of the bookmark with the message state.
 */
@Id("strange:updated_bookmark")
public class ClientReceiveUpdatedBookmark extends ClientReceiver {
    @Override
    public void handle(Minecraft client, FriendlyByteBuf buffer) {
        var tag = getCompoundTag(buffer).orElseThrow();
        var branch = BookmarksClient.getBranch().orElseThrow();

        client.execute(() -> {
            var bookmark = Bookmark.load(tag);
            branch.add(bookmark.getRunes(), bookmark);
            LogHelper.debug(getClass(), "Updated bookmark name is  `" + bookmark.getName() + "`.");

            // If the current player is the one who modified the bookmark then open the journal's bookmarks page.
            if (client.player == null) return;
            if (client.player.getUUID().equals(bookmark.getUuid())) {
                client.setScreen(new JournalBookmarksScreen());
            }
        });
    }
}
