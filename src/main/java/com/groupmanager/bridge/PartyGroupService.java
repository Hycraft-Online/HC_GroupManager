package com.groupmanager.bridge;

import com.groupmanager.api.GroupEventListener;
import com.groupmanager.api.GroupManagerAPI;
import com.groupmanager.api.GroupService;
import com.groupmanager.api.GroupType;
import com.hcparty.HC_PartyPlugin;
import com.hcparty.hud.PartyHud;
import com.hcparty.listeners.PartyEventListener;
import com.hcparty.managers.PartyHudManager;
import com.hcparty.managers.PartyManager;
import com.hcparty.models.Party;
import com.hcparty.models.PartyType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements GroupService by delegating to HC_Party's PartyManager.
 */
public class PartyGroupService implements GroupManagerAPI {

    private static final Logger LOG = Logger.getLogger(PartyGroupService.class.getName());

    // Maps GroupEventListener -> PartyEventListener adapter for unregister support
    private final Map<GroupEventListener, PartyEventListener> adapterMap = new ConcurrentHashMap<>();

    // Track paused HUDs
    private final Set<UUID> pausedHuds = ConcurrentHashMap.newKeySet();

    // ========== Inner interface implementations ==========

    private record GroupInfoImpl(
        UUID groupId,
        UUID leaderUuid,
        GroupType type,
        int size,
        List<UUID> memberUuids,
        int onlineMemberCount,
        Map<UUID, String> displayNames
    ) implements GroupInfo {}

    private record GroupResultImpl(
        boolean success,
        UUID groupId
    ) implements GroupResult {}

    // ========== Helpers ==========

    private PartyManager pm() {
        HC_PartyPlugin plugin = HC_PartyPlugin.getInstance();
        return plugin != null ? plugin.getPartyManager() : null;
    }

    private PartyHudManager hudManager() {
        HC_PartyPlugin plugin = HC_PartyPlugin.getInstance();
        return plugin != null ? plugin.getHudManager() : null;
    }

    private GroupType toGroupType(PartyType pt) {
        return pt == PartyType.TEMPORARY ? GroupType.TEMPORARY : GroupType.PERMANENT;
    }

    private GroupInfo toGroupInfo(Party party) {
        if (party == null) return null;
        PartyManager pm = pm();
        List<UUID> memberUuids = new ArrayList<>(Arrays.asList(party.getAllPartyMembers()));
        int onlineCount = pm != null ? pm.getOnlineMemberCount(party) : party.getTotalMemberCount();
        Map<UUID, String> displayNames = pm != null ? pm.getDisplayNames(party) : Map.of();

        return new GroupInfoImpl(
            party.getId(),
            party.getLeader(),
            toGroupType(party.getPartyType()),
            party.getTotalMemberCount(),
            memberUuids,
            onlineCount,
            displayNames
        );
    }

    private GroupResult result(boolean success, UUID groupId) {
        return new GroupResultImpl(success, groupId);
    }

    // ========== Core Lookups ==========

    @Override
    public Optional<GroupInfo> getPlayerGroup(UUID playerUuid) {
        PartyManager pm = pm();
        if (pm == null) return Optional.empty();
        Party party = pm.getPartyFromPlayer(playerUuid);
        return Optional.ofNullable(toGroupInfo(party));
    }

    @Override
    public Optional<GroupInfo> getGroup(UUID groupId) {
        PartyManager pm = pm();
        if (pm == null) return Optional.empty();
        Party party = pm.getPartyById(groupId);
        return Optional.ofNullable(toGroupInfo(party));
    }

    // ========== Group Creation ==========

    @Override
    public GroupResult createGroup(Player leader) {
        PartyManager pm = pm();
        if (pm == null) return result(false, null);
        try {
            PlayerRef playerRef = leader.getPlayerRef();
            if (playerRef == null) return result(false, null);
            Party party = pm.createParty(playerRef);
            return result(true, party.getId());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error creating group: " + e.getMessage(), e);
            return result(false, null);
        }
    }

    @Override
    public GroupResult createTemporaryGroup(Player leader, List<Player> members) {
        PartyManager pm = pm();
        if (pm == null) return result(false, null);
        try {
            UUID leaderUuid = leader.getUuid();
            List<UUID> memberUuids = new ArrayList<>();
            if (members != null) {
                for (Player p : members) {
                    if (p != null) memberUuids.add(p.getUuid());
                }
            }
            // Track names
            pm.setPlayerName(leaderUuid, leader.getDisplayName());
            for (Player p : members) {
                if (p != null) pm.setPlayerName(p.getUuid(), p.getDisplayName());
            }

            Party party = pm.createTemporaryGroup(leaderUuid, memberUuids);
            return result(true, party.getId());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error creating temporary group: " + e.getMessage(), e);
            return result(false, null);
        }
    }

    @Override
    public GroupResult dissolveTemporaryGroup(UUID groupId, Map<UUID, UUID> originalGroups) {
        PartyManager pm = pm();
        if (pm == null) return result(false, null);
        try {
            pm.dissolveTemporaryGroup(groupId, originalGroups);
            return result(true, groupId);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error dissolving temporary group: " + e.getMessage(), e);
            return result(false, groupId);
        }
    }

    // ========== Broadcasting ==========

    @Override
    public void broadcastToGroup(UUID groupId, String message) {
        PartyManager pm = pm();
        if (pm != null) {
            pm.broadcastToParty(groupId, message);
        }
    }

    @Override
    public void broadcastToGroupExcept(UUID groupId, UUID excludeUuid, String message) {
        PartyManager pm = pm();
        if (pm != null) {
            pm.broadcastToPartyExcept(groupId, excludeUuid, message);
        }
    }

    // ========== Event Listeners ==========

    @Override
    public void registerEventListener(GroupEventListener listener) {
        PartyManager pm = pm();
        if (pm == null || listener == null) return;

        PartyEventListener adapter = new PartyEventListener() {
            @Override
            public void onMemberJoined(UUID groupId, UUID playerUuid) {
                listener.onMemberJoined(groupId, playerUuid);
            }

            @Override
            public void onMemberLeft(UUID groupId, UUID playerUuid) {
                listener.onMemberLeft(groupId, playerUuid);
            }

            @Override
            public void onMemberKicked(UUID groupId, UUID kickedUuid, UUID kickerUuid) {
                listener.onMemberKicked(groupId, kickedUuid, kickerUuid);
            }

            @Override
            public void onGroupDissolved(UUID groupId) {
                listener.onGroupDissolved(groupId);
            }

            @Override
            public void onLeadershipTransferred(UUID groupId, UUID oldLeader, UUID newLeader) {
                listener.onLeadershipTransferred(groupId, oldLeader, newLeader);
            }
        };

        adapterMap.put(listener, adapter);
        pm.registerEventListener(adapter);
    }

    @Override
    public void unregisterEventListener(GroupEventListener listener) {
        PartyManager pm = pm();
        if (pm == null || listener == null) return;

        PartyEventListener adapter = adapterMap.remove(listener);
        if (adapter != null) {
            pm.unregisterEventListener(adapter);
        }
    }

    // ========== Utility ==========

    @Override
    public boolean areInSameGroup(UUID playerA, UUID playerB) {
        PartyManager pm = pm();
        if (pm == null || playerA == null || playerB == null) return false;
        Party partyA = pm.getPartyFromPlayer(playerA);
        if (partyA == null) return false;
        return partyA.isLeaderOrMember(playerB);
    }

    @Override
    public boolean isGroupLeader(UUID playerUuid) {
        PartyManager pm = pm();
        return pm != null && pm.isGroupLeader(playerUuid);
    }

    @Override
    public void disconnectPlayer(Player player) {
        PartyManager pm = pm();
        if (pm != null && player != null) {
            pm.handlePlayerDisconnect(player.getUuid());
        }
    }

    @Override
    public void restorePermanentGroupOnDisconnect(UUID playerUuid, UUID permanentGroupId) {
        PartyManager pm = pm();
        if (pm != null) {
            Party permanent = pm.getPartyById(permanentGroupId);
            if (permanent != null && permanent.isLeaderOrMember(playerUuid)) {
                permanent.markMemberOnline(playerUuid);
            }
        }
    }

    @Override
    public void restorePlayerFromCoalition(Player player, UUID permanentGroupId, List<UUID> coalitionGroups) {
        PartyManager pm = pm();
        if (pm == null) return;

        if (coalitionGroups != null) {
            for (UUID coalId : coalitionGroups) {
                Party coal = pm.getPartyById(coalId);
                if (coal != null && coal.isTemporary() && coal.isLeaderOrMember(player.getUuid())) {
                    coal.removeMember(player.getUuid());
                    if (coal.getTotalMemberCount() <= 1) {
                        pm.dissolveTemporaryGroup(coalId, Map.of());
                    }
                }
            }
        }
    }

    @Override
    public boolean rejoinCoalitionGroup(Player player, List<Player> teamMembers,
                                         Map<UUID, UUID> originalPermanentGroups, List<UUID> coalitionGroups) {
        PartyManager pm = pm();
        if (pm == null || coalitionGroups == null) return false;

        for (UUID coalId : coalitionGroups) {
            Party coal = pm.getPartyById(coalId);
            if (coal == null || !coal.isTemporary()) continue;

            for (Player teammate : teamMembers) {
                if (teammate != null && coal.isLeaderOrMember(teammate.getUuid())) {
                    if (!coal.isLeaderOrMember(player.getUuid())) {
                        coal.addMember(player.getUuid());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    // ========== HUD Control ==========

    @Override
    public void pauseHud(UUID playerUuid) {
        pausedHuds.add(playerUuid);
        PartyHudManager hm = hudManager();
        if (hm != null) {
            PartyHud hud = hm.getHud(playerUuid);
            if (hud != null) {
                hud.stopAutoUpdate();
            }
        }
    }

    @Override
    public void resumeHud(UUID playerUuid) {
        pausedHuds.remove(playerUuid);
        PartyHudManager hm = hudManager();
        if (hm != null) {
            PartyHud hud = hm.getHud(playerUuid);
            if (hud != null) {
                hud.startAutoUpdate();
                hud.refreshHud();
            }
        }
    }
}
