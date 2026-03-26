package eu.kennytv.resourcepackdisabler;

import com.google.inject.Inject;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerResourcePackSendEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.nio.file.Path;

@Plugin(
        id = "resourcepackdisabler",
        name = "ResourcePack Disabler",
        version = "1.0.0-SNAPSHOT",
        description = "Blocks backend resource packs for configured protocol versions."
)
public class ResourcePackDisablerPlugin {

    private final ProxyServer server;
    private final Path dataDirectory;
    private Config config;

    @Inject
    public ResourcePackDisablerPlugin(final ProxyServer server, @DataDirectory final Path dataDirectory) {
        this.server = server;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(final ProxyInitializeEvent event) {
        try {
            this.config = Config.load(this.dataDirectory);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load plugin configuration", e);
        }
    }

    @Subscribe
    public void onServerResourcePackSend(final ServerResourcePackSendEvent event) {
        if (this.config.shouldBlock(event.getServerConnection())) {
            event.setResult(ResultedEvent.GenericResult.denied());
        }
    }
}
