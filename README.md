A custom fork of [Velocity](https://github.com/PaperMC/Velocity)

A list of small tweaks I made:

- Added proxy setting for authenticating player with `sessionserver.mojang.com`
  - The proxy setting is in the `auth-proxy` section in `velocity.toml`, of course you know how to fill it
  - Supported proxy types: `socks4`, `socks5`, `http`
  - If enabled, velocity will firstly try authenticating with the given proxy, if failed it will try again without the proxy
- Implement player UUID rewrite, like what bungeecord does. 
  Make setup with online velocity + offline Minecraft server work correctly
  (`online-mode=true` on velocity + `online-mode=false` on backend mc servers + `player-info-forwarding-mode=none`)
  - Packets to rewrite:
    - TabList packets
      - Affects `LegacyPlayerListItemPacket`, `UpsertPlayerInfoPacket`, `RemovePlayerInfoPacket` packets
      - Rewrites player UUIDs inside those packets to their UUIDs in the velocity server
    - Entity packets
      - Rewrites player UUIDs inside player creation packets and spectator teleport packets, to their UUIDs in the velocity server
  - All related configs are under section `uuid-rewrite` in `velocity.toml`
  - Optional external uuid mapping sqlite database support
    - Enabled with `databaseEnabled = true`, database path configurable with `databasePath`
    - Mapping between online / offline uuid will be updated on player connected
    - The sqlite database file can be shared between multiple velocity instances
  - UUID rewrite can be disabled by setting `enabled = false`
- Added advanced proxy setting to forward client virtual host
  - setting to overwrite handshake packet host and port between virtual host or backend server

# Velocity

[![Build Status](https://img.shields.io/github/actions/workflow/status/PaperMC/Velocity/gradle.yml)](https://papermc.io/downloads/velocity)
[![Join our Discord](https://img.shields.io/discord/289587909051416579.svg?logo=discord&label=)](https://discord.gg/papermc)

A Minecraft server proxy with unparalleled server support, scalability,
and flexibility.

Velocity is licensed under the GPLv3 license.

## Goals

* A codebase that is easy to dive into and consistently follows best practices
  for Java projects as much as reasonably possible.
* High performance: handle thousands of players on one proxy.
* A new, refreshing API built from the ground up to be flexible and powerful
  whilst avoiding design mistakes and suboptimal designs from other proxies.
* First-class support for Paper, Sponge, Fabric and Forge. (Other implementations
  may work, but we make every endeavor to support these server implementations
  specifically.)
  
## Building

Velocity is built with [Gradle](https://gradle.org). We recommend using the
wrapper script (`./gradlew`) as our CI builds using it.

It is sufficient to run `./gradlew build` to run the full build cycle.

## Running

Once you've built Velocity, you can copy and run the `-all` JAR from
`proxy/build/libs`. Velocity will generate a default configuration file
and you can configure it from there.

Alternatively, you can get the proxy JAR from the [downloads](https://papermc.io/downloads/velocity)
page.

# Localisation

Translations are handled using [Crowdin](https://papermc-io.crowdin.com/velocity).
If you want to translate a language not available on Crowdin,
you might want to ask in the [Discord](https://discord.gg/papermc) about it.
