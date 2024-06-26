package io.lettuce.test.condition;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.models.command.CommandDetail;
import io.lettuce.core.models.command.CommandDetailParser;

/**
 * Collection of utility methods to test conditions during test execution.
 *
 * @author Mark Paluch
 */
public class RedisConditions {

    private final Map<String, Integer> commands;

    private final Version version;

    private RedisConditions(RedisClusterCommands<String, String> commands) {

        List<CommandDetail> result = CommandDetailParser.parse(commands.command());

        this.commands = result.stream()
                .collect(Collectors.toMap(commandDetail -> commandDetail.getName().toUpperCase(), CommandDetail::getArity));

        String info = commands.info("server");

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(info.getBytes());
            Properties p = new Properties();
            p.load(inputStream);

            version = Version.parse(p.getProperty("redis_version"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Create {@link RedisCommands} given {@link StatefulRedisConnection}.
     *
     * @param connection must not be {@code null}.
     * @return
     */
    public static RedisConditions of(StatefulRedisConnection<String, String> connection) {
        return new RedisConditions(connection.sync());
    }

    /**
     * Create {@link RedisCommands} given {@link StatefulRedisClusterConnection}.
     *
     * @param connection must not be {@code null}.
     * @return
     */
    public static RedisConditions of(StatefulRedisClusterConnection<String, String> connection) {
        return new RedisConditions(connection.sync());
    }

    /**
     * Create {@link RedisConditions} given {@link RedisCommands}.
     *
     * @param commands must not be {@code null}.
     * @return
     */
    public static RedisConditions of(RedisClusterCommands<String, String> commands) {
        return new RedisConditions(commands);
    }

    /**
     * @return the Redis {@link Version}.
     */
    public Version getRedisVersion() {
        return version;
    }

    /**
     * @param command
     * @return {@code true} if the command is present.
     */
    public boolean hasCommand(String command) {
        return commands.containsKey(command.toUpperCase());
    }

    /**
     * @param command command name.
     * @param arity expected arity.
     * @return {@code true} if the command is present with the given arity.
     */
    public boolean hasCommandArity(String command, int arity) {

        if (!hasCommand(command)) {
            throw new IllegalStateException("Unknown command: " + command + " in " + commands);
        }

        return commands.get(command.toUpperCase()) == arity;
    }

    /**
     * @param versionNumber
     * @return {@code true} if the version number is met.
     */
    public boolean hasVersionGreaterOrEqualsTo(String versionNumber) {
        return version.isGreaterThanOrEqualTo(Version.parse(versionNumber));
    }

    /**
     * Value object to represent a Version consisting of major, minor and bugfix part.
     */
    public static class Version implements Comparable<Version> {

        private static final String VERSION_PARSE_ERROR = "Invalid version string! Could not parse segment %s within %s.";

        private final int major;

        private final int minor;

        private final int bugfix;

        private final int build;

        /**
         * Creates a new {@link Version} from the given integer values. At least one value has to be given but a maximum of 4.
         *
         * @param parts must not be {@code null} or empty.
         */
        Version(int... parts) {

            LettuceAssert.notNull(parts, "Parts must not be null!");
            LettuceAssert.isTrue(parts.length > 0 && parts.length < 5,
                    String.format("Invalid parts length. 0 < %s < 5", parts.length));

            this.major = parts[0];
            this.minor = parts.length > 1 ? parts[1] : 0;
            this.bugfix = parts.length > 2 ? parts[2] : 0;
            this.build = parts.length > 3 ? parts[3] : 0;

            LettuceAssert.isTrue(major >= 0, "Major version must be greater or equal zero!");
            LettuceAssert.isTrue(minor >= 0, "Minor version must be greater or equal zero!");
            LettuceAssert.isTrue(bugfix >= 0, "Bugfix version must be greater or equal zero!");
            LettuceAssert.isTrue(build >= 0, "Build version must be greater or equal zero!");
        }

        /**
         * Parses the given string representation of a version into a {@link Version} object.
         *
         * @param version must not be {@code null} or empty.
         * @return
         */
        public static Version parse(String version) {

            LettuceAssert.notEmpty(version, "Version must not be null o empty!");

            String[] parts = version.trim().split("\\.");
            int[] intParts = new int[parts.length];

            for (int i = 0; i < parts.length; i++) {

                String input = i == parts.length - 1 ? parts[i].replaceAll("\\D.*", "") : parts[i];

                if (LettuceStrings.isNotEmpty(input)) {
                    try {
                        intParts[i] = Integer.parseInt(input);
                    } catch (IllegalArgumentException o_O) {
                        throw new IllegalArgumentException(String.format(VERSION_PARSE_ERROR, input, version), o_O);
                    }
                }
            }

            return new Version(intParts);
        }

        /**
         * Returns whether the current {@link Version} is greater (newer) than the given one.
         *
         * @param version
         * @return
         */
        public boolean isGreaterThan(Version version) {
            return compareTo(version) > 0;
        }

        /**
         * Returns whether the current {@link Version} is greater (newer) or the same as the given one.
         *
         * @param version
         * @return
         */
        boolean isGreaterThanOrEqualTo(Version version) {
            return compareTo(version) >= 0;
        }

        /**
         * Returns whether the current {@link Version} is the same as the given one.
         *
         * @param version
         * @return
         */
        public boolean is(Version version) {
            return equals(version);
        }

        /**
         * Returns whether the current {@link Version} is less (older) than the given one.
         *
         * @param version
         * @return
         */
        public boolean isLessThan(Version version) {
            return compareTo(version) < 0;
        }

        /**
         * Returns whether the current {@link Version} is less (older) or equal to the current one.
         *
         * @param version
         * @return
         */
        public boolean isLessThanOrEqualTo(Version version) {
            return compareTo(version) <= 0;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Version that) {

            if (that == null) {
                return 1;
            }

            if (major != that.major) {
                return major - that.major;
            }

            if (minor != that.minor) {
                return minor - that.minor;
            }

            if (bugfix != that.bugfix) {
                return bugfix - that.bugfix;
            }

            if (build != that.build) {
                return build - that.build;
            }

            return 0;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {

            if (this == obj) {
                return true;
            }

            if (!(obj instanceof Version)) {
                return false;
            }

            Version that = (Version) obj;

            return this.major == that.major && this.minor == that.minor && this.bugfix == that.bugfix
                    && this.build == that.build;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {

            int result = 17;
            result += 31 * major;
            result += 31 * minor;
            result += 31 * bugfix;
            result += 31 * build;
            return result;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {

            List<Integer> digits = new ArrayList<>();
            digits.add(major);
            digits.add(minor);

            if (build != 0 || bugfix != 0) {
                digits.add(bugfix);
            }

            if (build != 0) {
                digits.add(build);
            }

            return digits.stream().map(Object::toString).collect(Collectors.joining("."));
        }

    }

}
