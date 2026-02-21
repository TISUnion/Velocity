/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.uuidrewrite;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItemPacket;
import com.velocitypowered.proxy.protocol.packet.RemovePlayerInfoPacket;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * [fallen's fork] player uuid rewrite - tab list entry: rewrite logic.
 */
@SuppressWarnings("ALL")
public class TabListUuidRewriter {

  private static final Logger logger = LogManager.getLogger(EntityPacketUuidRewriter.class);

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private static boolean shouldRewrite(VelocityServer server) {
    return UuidRewriteUtils.isUuidRewriteEnabled(server.getConfiguration());
  }

  /**
   * [fallen's fork] player uuid rewrite.
   * send the missing player tab-list removal packets to other players in the mc server
   * see bungeecord net.md_5.bungee.connection.UpstreamBridge#disconnected
   */
  public static void sendRewrittenTabListRemovalPackets(VelocityServer server, ConnectedPlayer player) {
    if (!shouldRewrite(server)) {
      return;
    }
    if (server.getConfiguration().isUuidRewriteDatabaseEnabled()) {
      // if the database is enabled, then no need for this bungee hack
      // cuz the mapping is always available
      return;
    }

    VelocityServerConnection connectedServer = player.getConnectedServer();
    if (connectedServer == null) {
      return;
    }

    var oldPacket = new LegacyPlayerListItemPacket(
            LegacyPlayerListItemPacket.REMOVE_PLAYER,
            Collections.singletonList(new LegacyPlayerListItemPacket.Item(player.getUniqueId()))
    );
    var newPacket = new RemovePlayerInfoPacket(
            Collections.singleton(player.getUniqueId())
    );

    for (Player otherPlayer : connectedServer.getServer().getPlayersConnected()) {
      if (otherPlayer != player && otherPlayer instanceof ConnectedPlayer) {
        var connection = ((ConnectedPlayer) otherPlayer).getConnection();
        MinecraftPacket packet;
        if (connection.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
          packet = newPacket;
        } else {
          packet = oldPacket;
        }
        connection.write(packet);
      }
    }
  }

  /**
   * Rewrite uuid for a LegacyPlayerListItem packet (S -> C).
   */
  public static void rewrite(VelocityServer server, LegacyPlayerListItemPacket packet) {
    if (!shouldRewrite(server)) {
      return;
    }

    var rewriter = UuidRewriter.create(server);
    packet.getItems().replaceAll(item -> {
      var clientSideUuid = rewriter.toClient(item.getUuid());
      var onlineProfile = rewriter.getOnlineProfile(clientSideUuid);
      if (UuidRewriter.DEBUG) {
        logger.info("TLUR for packet {}: S={} -> C={} ({})",
                packet.getClass().getSimpleName(), item.getUuid(), clientSideUuid, onlineProfile);
      }
      if (clientSideUuid != null && !clientSideUuid.equals(item.getUuid())) {
        var newItem = new LegacyPlayerListItemPacket.Item(clientSideUuid);

        newItem.setName(item.getName());
        newItem.setProperties(onlineProfile != null ? onlineProfile.getProperties() : item.getProperties());
        newItem.setGameMode(item.getGameMode());
        newItem.setLatency(item.getLatency());
        newItem.setDisplayName(item.getDisplayName());
        newItem.setPlayerKey(item.getPlayerKey());

        item = newItem;
      }

      return item;
    });
  }

  /**
   * Rewrite uuid for a UpsertPlayerInfo packet (S -> C).
   */
  public static void rewrite(VelocityServer server, UpsertPlayerInfoPacket packet) {
    if (!shouldRewrite(server)) {
      return;
    }

    var rewriter = UuidRewriter.create(server);
    packet.getEntries().replaceAll(entry -> {
      var clientSideUuid = rewriter.toClient(entry.getProfileId());
      var onlineProfile = rewriter.getOnlineProfile(clientSideUuid);
      if (UuidRewriter.DEBUG) {
        logger.info("TLUR for packet {}: S={} -> C={} ({})",
                packet.getClass().getSimpleName(), entry.getProfileId(), clientSideUuid, onlineProfile);
      }
      if (clientSideUuid != null && !clientSideUuid.equals(entry.getProfileId())) {
        var newEntry = new UpsertPlayerInfoPacket.Entry(clientSideUuid);

        newEntry.setProfile(onlineProfile != null ? onlineProfile : entry.getProfile());
        newEntry.setListed(entry.isListed());
        newEntry.setLatency(entry.getLatency());
        newEntry.setGameMode(entry.getGameMode());
        newEntry.setDisplayName(entry.getDisplayName());
        newEntry.setChatSession(entry.getChatSession());

        entry = newEntry;
      }

      return entry;
    });
  }

  /**
   * Rewrite uuid for a RemovePlayerInfo packet (S -> C).
   */
  public static void rewrite(VelocityServer server, RemovePlayerInfoPacket packet) {
    if (!shouldRewrite(server)) {
      return;
    }

    var rewriter = UuidRewriter.create(server);
    var newProfiles = packet.getProfilesToRemove().stream()
        .map(serverUuid -> {
          var newUuid = Optional.ofNullable(rewriter.toClient(serverUuid)).orElse(serverUuid);
          if (UuidRewriter.DEBUG) {
            logger.info("TLUR for packet {}: S={} -> C={}",
                    packet.getClass().getSimpleName(), serverUuid, newUuid);
          }
          return newUuid;
        })
        .collect(Collectors.toList());

    packet.setProfilesToRemove(newProfiles);
  }
}
