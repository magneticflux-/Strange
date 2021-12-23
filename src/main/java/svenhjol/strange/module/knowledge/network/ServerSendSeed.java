package svenhjol.strange.module.knowledge.network;

import net.minecraft.server.level.ServerPlayer;
import svenhjol.strange.module.knowledge.Knowledge;
import svenhjol.charm.network.Id;
import svenhjol.charm.network.ServerSender;

@Id("strange:knowledge_seed")
public class ServerSendSeed extends ServerSender {
    @Override
    public void send(ServerPlayer player) {
        send(player, buf -> buf.writeLong(Knowledge.SEED));
    }
}
