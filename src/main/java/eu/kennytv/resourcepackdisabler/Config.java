package eu.kennytv.resourcepackdisabler;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.ServerConnection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public final class Config {

    private final Map<String, List<ProtocolMatcher>> serverMatchers;
    private final List<ProtocolMatcher> globalMatchers;

    private Config(final List<ProtocolMatcher> globalMatchers, final Map<String, List<ProtocolMatcher>> serverMatchers) {
        this.globalMatchers = globalMatchers;
        this.serverMatchers = new LinkedHashMap<>(serverMatchers);
    }

    public static Config load(final Path dataDirectory) throws IOException {
        Files.createDirectories(dataDirectory);
        final Path configPath = dataDirectory.resolve("config.yml");
        final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .nodeStyle(NodeStyle.BLOCK)
                .build();
        final CommentedConfigurationNode root = loader.load();
        final RawConfig rawConfig;
        if (root.empty()) {
            rawConfig = RawConfig.defaults();
            root.set(rawConfig);
            root.comment("""
                    You can set:
                    - exact version: 26.1
                    - inclusive range: 1.8-1.12.2
                    - comparators: <=1.20.1, >=1.21, <1.17, >1.19.4
                    """);
            loader.save(root);
        } else {
            rawConfig = root.get(RawConfig.class);
        }

        final List<ProtocolMatcher> globalMatchers = parseMatchers(rawConfig.global);
        final Map<String, List<ProtocolMatcher>> serverMatchers = new LinkedHashMap<>();
        for (final Map.Entry<String, List<String>> entry : rawConfig.servers.entrySet()) {
            final String serverName = entry.getKey().trim();
            if (serverName.isEmpty()) {
                throw new IllegalArgumentException("Server rule key must include a server name");
            }

            serverMatchers.put(normalizeServerName(serverName), parseMatchers(entry.getValue()));
        }

        return new Config(globalMatchers, serverMatchers);
    }

    public boolean shouldBlock(final ServerConnection serverConnection) {
        final ProtocolVersion protocolVersion = serverConnection.getPlayer().getProtocolVersion();
        if (matchesAny(this.globalMatchers, protocolVersion)) {
            return true;
        }

        final List<ProtocolMatcher> matchers = this.serverMatchers.get(normalizeServerName(serverConnection.getServerInfo().getName()));
        return matchesAny(matchers, protocolVersion);
    }

    private boolean matchesAny(final List<ProtocolMatcher> matchers, final ProtocolVersion protocolVersion) {
        if (matchers == null || matchers.isEmpty()) {
            return false;
        }

        for (final ProtocolMatcher matcher : matchers) {
            if (matcher.matches(protocolVersion)) {
                return true;
            }
        }
        return false;
    }

    private static List<ProtocolMatcher> parseMatchers(final List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        final List<ProtocolMatcher> matchers = new ArrayList<>();
        for (final String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }

            for (final String token : value.split(",")) {
                final String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    matchers.add(ProtocolMatcher.parse(trimmed));
                }
            }
        }
        return List.copyOf(matchers);
    }

    private static String normalizeServerName(final String serverName) {
        return serverName.toLowerCase(Locale.ROOT);
    }

    @ConfigSerializable
    private static final class RawConfig {

        List<String> global = List.of();
        Map<String, List<String>> servers = Map.of();

        static RawConfig defaults() {
            final RawConfig config = new RawConfig();
            config.global = List.of("<=1.8");
            config.servers = new LinkedHashMap<>();
            config.servers.put("a", List.of("1.9-1.12.2"));
            config.servers.put("b", List.of(">=26.1"));
            return config;
        }

        @Override
        public String toString() {
            return "RawConfig{" +
                    "global=" + global +
                    ", servers=" + servers +
                    '}';
        }
    }
}
