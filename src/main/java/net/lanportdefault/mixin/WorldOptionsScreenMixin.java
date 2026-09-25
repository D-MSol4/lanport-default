package net.lanportdefault.mixin;

import net.lanportdefault.LanPortDefaultClient;
import net.lanportdefault.LanPortDefaultConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.WorldOptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
 * <p><b>2. "Set default" button.</b> A small button next to the field saves whatever port is
 * in it as the new default, so the config file never has to be edited by hand.
 *
 * <p>The button is NOT added as a screen-level widget: the screen wraps its content in a
 * {@code ScrollableLayout}, whose container is earlier in the screen's child list and swallows
 * clicks aimed at widgets drawn on top of it. Instead the field's own {@code addChild} call is
 * redirected to put the field and the button side by side in a horizontal layout inside the
 * same grid cell. The vanilla layout then positions, renders and routes clicks for both — it
 * scrolls and resizes with the rest of the screen for free.
 */
@Mixin(WorldOptionsScreen.class)
public abstract class WorldOptionsScreenMixin {

    /** Button width, the gap to the field, and the field width that leaves room for both. */
    private static final int LPD_BUTTON_WIDTH = 30;
    private static final int LPD_GAP = 4;
    /** The multiplayer grid's right column is 308 wide (its game-mode buttons); 308 - 30 - 4. */
    private static final int LPD_FIELD_WIDTH = 274;

    @Shadow
    private EditBox portEdit;

    /** Vanilla's own chat + narrator feedback path, reused for the "saved" messages. */
    @Shadow
    private void sendPublishMessage(Component message) {
        throw new AssertionError();
    }

    @Unique
    private Button lanportdefault$defaultButton;

    @Inject(method = "updatePortControlsState", at = @At("TAIL"))
    private void lanportdefault$prefillLanPort(CallbackInfo ci) {
        int port = LanPortDefaultConfig.port();
        if (port <= 0 || this.portEdit == null || !this.portEdit.getValue().isEmpty()) {
            return;
        }
        this.portEdit.setValue(Integer.toString(port));
        LanPortDefaultClient.LOGGER.info("[LAN Port Default] Pre-filled LAN port {}", port);
    }

    /**
     * Replaces the port field's own grid cell content with a row holding the field plus the
     * button. {@code ordinal = 1} targets the second one-argument {@code addChild} call in
     * {@code multiplayerOptions} — the first is the LAN toggle, this one is the port field.
     */
    @Redirect(
            method = "multiplayerOptions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 1))
    private LayoutElement lanportdefault$addButtonNextToPortField(GridLayout.RowHelper helper, LayoutElement widget) {
        if (widget instanceof AbstractWidget field) {
            field.setWidth(LPD_FIELD_WIDTH);
        }
        LinearLayout row = LinearLayout.horizontal().spacing(LPD_GAP);
        row.addChild(widget);
        row.addChild(this.lanportdefault$createDefaultButton());
        LanPortDefaultClient.LOGGER.info("[LAN Port Default] Button attached next to {}", widget.getClass().getSimpleName());
        return helper.addChild(row);
    }

    @Unique
    private Button lanportdefault$createDefaultButton() {
        if (this.lanportdefault$defaultButton == null) {
            this.lanportdefault$defaultButton = Button.builder(
                            Component.translatable("lanportdefault.button.set_default"),
                            button -> this.lanportdefault$saveFieldPortAsDefault())
                    .bounds(0, 0, LPD_BUTTON_WIDTH, 20)
                    .build();
            this.lanportdefault$defaultButton.setTooltip(
                    Tooltip.create(Component.translatable("lanportdefault.button.set_default.tooltip")));
        }
        return this.lanportdefault$defaultButton;
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
