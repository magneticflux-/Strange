package svenhjol.strange.module.dimensions.mirror.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import svenhjol.charm.network.ClientReceiver;
import svenhjol.charm.network.Id;
import svenhjol.strange.module.dimensions.mirror.MirrorDimension;
import svenhjol.strange.module.dimensions.mirror.MirrorDimensionClient;

@Id("strange:mirror_weather_change")
public class ClientReceiveWeatherChange extends ClientReceiver {
    @Override
    public void handle(Minecraft client, FriendlyByteBuf buffer) {
        var weather = buffer.readEnum(MirrorDimension.WeatherPhase.class);
        client.execute(() -> MirrorDimensionClient.handleWeatherChange(weather));
    }
}
