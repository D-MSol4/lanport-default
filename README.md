# LAN Port Default

A tiny client-side Fabric mod for **Minecraft 26.3**. It does one thing: the LAN port field in
the **World Options** screen comes pre-filled with a fixed, configurable port, so a world is
always published on the same address. A small **DEF** button next to the field changes that
default from inside the game.

No blocks, no items, no commands, no networking, no Fabric API dependency.

## Why

Vanilla leaves the port field empty and picks a random free port every time you publish. That
means the address other players saved in their server list — and any router or firewall rule
pointing at the port — changes every session. With a fixed port, a friend can keep `host:port`
forever.

## Configuration

Created on first launch at `config/lanport-default.properties`:

```properties
# LAN port pre-filled in the World Options screen (1024-65535).
# 0 = disabled: vanilla picks a random free port on every publish.
port=38472
```

The file is re-read every time the screen opens, so edits apply without restarting the game.
The **DEF** button writes it for you: it saves whatever port is currently in the field.

## Behaviour

- The field is only pre-filled when it is **empty**. If the world is already published, the
  field shows the live port and the running publication is left untouched.
- If the configured port is taken, vanilla marks the field in red and you can type another one.
- The mixin is declared `required: false` and the mod depends on `minecraft >=26.3 <26.4`: on
  any other version the mod simply does not load instead of crashing.

## Building

Requires JDK 25. Minecraft 26.1+ ships unobfuscated, so this uses the non-remapping Loom plugin
(no mappings, no remap step).

```bash
./gradlew build
```

The jar lands in `build/libs/` and goes straight into `mods/`.
