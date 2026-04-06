package com.groupmanager.api;

import com.hypixel.hytale.server.core.entity.entities.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for group/party management.
 * ArenaPvP accesses this via GroupManagerProvider.get().
 *
 * GroupInfo and GroupResult are inner interfaces (ArenaPvP's bytecode uses
 * invokeinterface to call their methods).
 */
public interface GroupService {

    // ========== Inner Types ==========

    interface GroupInfo {
        UUID groupId();
        UUID leaderUuid();
        GroupType type();
        int size();
        List<UUID> memberUuids();
        int onlineMemberCount();
        Map<UUID, String> displayNames();
    }

    interface GroupResult {
        boolean success();
        UUID groupId();
    }

    // ========== Core Lookups ==========

    /**
     * Get the group a player belongs to (prefers TEMPORARY over PERMANENT).
     */
    Optional<GroupInfo> getPlayerGroup(UUID playerUuid);

    /**
     * Get a group by its ID.
     */
    Optional<GroupInfo> getGroup(UUID groupId);

    // ========== Group Creation ==========

    /**
     * Create a new permanent group with the given player as leader.
     */
    GroupResult createGroup(Player leader);

    /**
     * Create a temporary coalition group with a leader and members.
     */
    GroupResult createTemporaryGroup(Player leader, List<Player> members);

    /**
     * Dissolve a temporary group. Permanent groups are unaffected.
     */
    GroupResult dissolveTemporaryGroup(UUID groupId, Map<UUID, UUID> originalGroups);

    // ========== Broadcasting ==========

    /**
     * Broadcast a message to all members of a group.
     */
    void broadcastToGroup(UUID groupId, String message);

    /**
     * Broadcast a message to all members of a group except one.
     */
    void broadcastToGroupExcept(UUID groupId, UUID excludeUuid, String message);

    // ========== Event Listeners ==========

    void registerEventListener(GroupEventListener listener);

    void unregisterEventListener(GroupEventListener listener);

    // ========== Utility ==========

    /**
     * Check if two players are in the same group.
     * Required by ArenaPvP's damage event system.
     */
    boolean areInSameGroup(UUID playerA, UUID playerB);

    /**
     * Check if a player is the leader of their current group.
     */
    boolean isGroupLeader(UUID playerUuid);

    /**
     * Handle a player disconnecting from a group (mark offline).
     */
    void disconnectPlayer(Player player);

    /**
     * Restore a player's permanent group tracking after disconnect from a coalition.
     */
    void restorePermanentGroupOnDisconnect(UUID playerUuid, UUID permanentGroupId);

    /**
     * Restore a player from a coalition back to their permanent group.
     */
    void restorePlayerFromCoalition(Player player, UUID permanentGroupId, List<UUID> coalitionGroups);

    /**
     * Rejoin a coalition group (for reconnecting players during arena match).
     */
    boolean rejoinCoalitionGroup(Player player, List<Player> teamMembers,
                                  Map<UUID, UUID> originalPermanentGroups, List<UUID> coalitionGroups);

    // ========== HUD Control ==========

    /**
     * Pause the party HUD for a player (e.g., during arena).
     */
    void pauseHud(UUID playerUuid);

    /**
     * Resume the party HUD for a player (e.g., after arena).
     */
    void resumeHud(UUID playerUuid);
}
