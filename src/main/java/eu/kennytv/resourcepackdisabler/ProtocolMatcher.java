package eu.kennytv.resourcepackdisabler;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ProtocolMatcher {

    private final ProtocolVersion min;
    private final ProtocolVersion max;
    private final boolean alwaysFalse;

    private ProtocolMatcher(final ProtocolVersion min, final ProtocolVersion max) {
        this(min, max, false);
    }

    private ProtocolMatcher(final ProtocolVersion min, final ProtocolVersion max, final boolean alwaysFalse) {
        this.min = min;
        this.max = max;
        this.alwaysFalse = alwaysFalse;
    }

    public static ProtocolMatcher parse(final String input) {
        if (input.startsWith("<=")) {
            return atMost(parseVersion(input.substring(2), input));
        } else if (input.startsWith(">=")) {
            return atLeast(parseVersion(input.substring(2), input));
        } else if (input.startsWith("<")) {
            return lessThan(parseVersion(input.substring(1), input));
        } else if (input.startsWith(">")) {
            return greaterThan(parseVersion(input.substring(1), input));
        }

        final int separator = input.indexOf('-');
        if (separator > 0) {
            final ProtocolVersion start = parseVersion(input.substring(0, separator), input);
            final ProtocolVersion end = parseVersion(input.substring(separator + 1), input);
            if (start.compareTo(end) > 0) {
                throw new IllegalArgumentException("Invalid protocol range '" + input);
            }
            return new ProtocolMatcher(start, end);
        }

        final ProtocolVersion version = parseVersion(input, input);
        return new ProtocolMatcher(version, version);
    }

    public boolean matches(final ProtocolVersion version) {
        if (this.alwaysFalse) {
            return false;
        }
        return version.compareTo(this.min) >= 0 && version.compareTo(this.max) <= 0;
    }

    private static ProtocolMatcher none() {
        return new ProtocolMatcher(ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MINIMUM_VERSION, true);
    }

    private static ProtocolMatcher atMost(final ProtocolVersion version) {
        return new ProtocolMatcher(ProtocolVersion.MINIMUM_VERSION, version);
    }

    private static ProtocolMatcher atLeast(final ProtocolVersion version) {
        return new ProtocolMatcher(version, ProtocolVersion.MAXIMUM_VERSION);
    }

    private static ProtocolMatcher lessThan(final ProtocolVersion version) {
        final ProtocolVersion previous = previousSupportedVersion(version);
        return previous == null ? none() : atMost(previous);
    }

    private static ProtocolMatcher greaterThan(final ProtocolVersion version) {
        final ProtocolVersion next = nextSupportedVersion(version);
        return next == null ? none() : atLeast(next);
    }

    private static @Nullable ProtocolVersion previousSupportedVersion(final ProtocolVersion version) {
        final ProtocolVersion[] versions = ProtocolVersion.values();
        final int index = version.ordinal() - 1;
        if (index < 0 || !versions[index].isSupported()) {
            return null;
        }
        return versions[index];
    }

    private static @Nullable ProtocolVersion nextSupportedVersion(final ProtocolVersion version) {
        final ProtocolVersion[] versions = ProtocolVersion.values();
        final int index = version.ordinal() + 1;
        if (index >= versions.length || !versions[index].isSupported()) {
            return null;
        }
        return versions[index];
    }

    private static ProtocolVersion parseVersion(final String rawVersion, final String originalInput) {
        final String version = rawVersion.trim();
        if (version.isEmpty()) {
            throw new IllegalArgumentException("Missing protocol version in '" + originalInput);
        }
        return Arrays.stream(ProtocolVersion.values())
                .filter(candidate -> candidate.isSupported())
                .filter(candidate -> candidate.getVersionsSupportedBy().stream().anyMatch(supported -> supported.equalsIgnoreCase(version)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown protocol version '" + version));
    }
}
