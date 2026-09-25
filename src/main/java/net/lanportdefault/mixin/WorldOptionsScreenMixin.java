package net.lanportdefault.mixin;

import net.lanportdefault.LanPortDefaultClient;
import net.lanportdefault.LanPortDefaultConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WorldOptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The whole mod: two tweaks to the World Options screen (26.3+), both about the LAN port field.
 *
 * <p><b>1. Pre-fill.</b> Vanilla leaves the field empty and picks a random free port on every
 * publish, so the address other players saved changes each session. This fills the field with
 * the configured port instead.
 *
 * <p>The fill is hooked at the TAIL of {@code updatePortControlsState()} rather than in the
 * constructor: that method — called from the constructor and again whenever the LAN toggle
 * flips — syncs the field with the wanted state, and while nothing is published its "desired"
 * value is the empty string. So it clears the field, and the field's own responder then
 * assigns a fresh random port. Filling right after that clear is the only spot where the value
 * survives the LAN toggle and reaches Apply. Only an EMPTY field is filled: while a world is
 * published the field holds the live port, and that running publication is left alone.
 *
 * <p><b>2. "Set default" button.</b> A small button to the right of the field saves whatever
 * port is in it as the new default, so the config file never has to be edited by hand. It is
 * carved out of the field's own width on every layout pass ({@code repositionElements}), which
 * keeps it inside the vanilla layout box and following resizes and scrolling.
 */
@Mixin(WorldOptionsScreen.class)
public abstract class WorldOptionsScreenMixin extends Screen {

    /** Width of the little button and the gap to the port field, in GUI units. */
    private static final int LPD_BUTTON_WIDTH = 30;
    private static final int LPD_GAP = 4;

    @Shadow
    private EditBox portEdit;

    /** Vanilla's own chat + narrator feedback path, reused for the "saved" messages. */
    @Shadow
    private void sendPublishMessage(Component message) {
        throw new AssertionError();
    }

    @Unique
    private Button lanportdefault$defaultButton;

    /**
     * The field's width as laid out by vanilla, captured on every layout pass. The button
     * placement always derives from this, never from the already-shrunk width, which makes it
     * idempotent across repeated layout passes.
     */
    @Unique
    private int lanportdefault$fullWidth = -1;

    protected WorldOptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "updatePortControlsState", at = @At("TAIL"))
    private void lanportdefault$prefillLanPort(CallbackInfo ci) {
        int port = LanPortDefaultConfig.port();
        if (port <= 0 || this.portEdit == null || !this.portEdit.getValue().isEmpty()) {
            return;
        }
        this.portEdit.setValue(Integer.toString(port));
        LanPortDefaultClient.LOGGER.info("[LAN Port Default] Pre-filled LAN port {}", port);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void lanportdefault$addDefaultButton(CallbackInfo ci) {
        if (this.lanportdefault$defaultButton == null) {
            this.lanportdefault$defaultButton = Button.builder(
                            Component.translatable("lanportdefault.button.set_default"),
                            button -> this.lanportdefault$saveFieldPortAsDefault())
                    .bounds(0, 0, LPD_BUTTON_WIDTH, 20)
                    .build();
            this.lanportdefault$defaultButton.setTooltip(
                    Tooltip.create(Component.translatable("lanportdefault.button.set_default.tooltip")));
        }
        this.addRenderableWidget(this.lanportdefault$defaultButton);
        this.lanportdefault$placeButton();
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void lanportdefault$afterLayout(CallbackInfo ci) {
        if (this.portEdit == null) {
            return;
        }
        this.lanportdefault$fullWidth = this.portEdit.getWidth();
        this.lanportdefault$placeButton();
    }

    /** Shrinks the port field and drops the button into the freed strip. */
    @Unique
    private void lanportdefault$placeButton() {
        if (this.portEdit == null || this.lanportdefault$fullWidth <= 0) {
            return;
        }
        int width = Math.max(40, this.lanportdefault$fullWidth - LPD_BUTTON_WIDTH - LPD_GAP);
        this.portEdit.setWidth(width);
        if (this.lanportdefault$defaultButton != null) {
            this.lanportdefault$defaultButton.setX(this.portEdit.getX() + width + LPD_GAP);
            this.lanportdefault$defaultButton.setY(this.portEdit.getY());
            this.lanportdefault$defaultButton.setHeight(this.portEdit.getHeight());
        }
    }

    /** Saves whatever port is in the field as the new default, with chat feedback either way. */
    @Unique
    private void lanportdefault$saveFieldPortAsDefault() {
        String raw = this.portEdit == null ? "" : this.portEdit.getValue().trim();
        int port = -1;
        try {
            port = Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            // Reported as invalid below.
        }
        if (LanPortDefaultConfig.save(port)) {
            LanPortDefaultClient.LOGGER.info("[LAN Port Default] Default LAN port set to {}", port);
            this.sendPublishMessage(Component.translatable("lanportdefault.saved", port));
        } else {
            this.sendPublishMessage(Component.translatable("lanportdefault.invalid", raw));
        }
    }
}
