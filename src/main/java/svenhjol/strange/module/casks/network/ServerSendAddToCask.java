package svenhjol.strange.module.casks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import svenhjol.charm.network.Id;
import svenhjol.charm.network.ServerSender;

@Id("strange:add_to_cask")
public class ServerSendAddToCask extends ServerSender {
    public void send(ServerPlayer player, BlockPos pos) {
        super.send(player, buf -> buf.writeBlockPos(pos));
    }
}
