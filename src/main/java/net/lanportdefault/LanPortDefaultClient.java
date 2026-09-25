package net.lanportdefault;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint. The mod is mixin-only, so this class does nothing but make the resolved
 * port and the config path show up in the log — and create the config file on first launch, so
 * it is easy to find.
 */
public final class LanPortDefaultClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("lanportdefault");

    @Override
    public void onInitializeClient() {
        int port = LanPortDefaultConfig.port();
        LOGGER.info("[LAN Port Default] config {} — LAN port {}",
                LanPortDefaultConfig.path(),
                port > 0 ? Integer.toString(port) : "disabled (vanilla random)");
    }
}
