package net.lanportdefault;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The mod's only setting: the port pre-filled in the World Options screen. A plain properties
 * file rather than a config screen — one value does not need one.
 *
 * <p>Re-read on every call (screen open), so edits apply without restarting the game.
 */
public final class LanPortDefaultConfig {

    private static final String FILE_NAME = "lanport-default.properties";
    /** Written on first launch. 0 disables the pre-fill entirely. */
    private static final int DEFAULT_PORT = 25565;
    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;
    private static final String HEADER =
            "# LAN port pre-filled in the World Options screen (1024-65535).\n"
                    + "# 0 = disabled: vanilla picks a random free port on every publish.\n";

    private LanPortDefaultConfig() {
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    /** The configured port, or 0 when disabled or out of the range the port field accepts. */
    public static int port() {
        Path path = path();
        Properties props = new Properties();
        if (Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
            } catch (IOException e) {
                LanPortDefaultClient.LOGGER.warn("[LAN Port Default] Could not read {}: {}", path, e.toString());
                return 0;
            }
        } else if (!write(DEFAULT_PORT)) {
            return DEFAULT_PORT;
        }

        String value = props.getProperty("port", Integer.toString(DEFAULT_PORT));
        try {
            int parsed = Integer.parseInt(value.trim());
            return (parsed >= MIN_PORT && parsed <= MAX_PORT) ? parsed : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Makes {@code port} the new default. False when out of range or the file cannot be written. */
    public static boolean save(int port) {
        if (port < MIN_PORT || port > MAX_PORT) {
            return false;
        }
        return write(port);
    }

    private static boolean write(int port) {
        try {
            Path path = path();
            Files.createDirectories(path.getParent());
            Files.writeString(path, HEADER + "port=" + port + "\n");
            return true;
        } catch (IOException e) {
            LanPortDefaultClient.LOGGER.warn("[LAN Port Default] Could not write {}: {}", path(), e.toString());
            return false;
        }
    }
}
