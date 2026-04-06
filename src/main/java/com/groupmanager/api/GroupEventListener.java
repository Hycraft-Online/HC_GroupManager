package com.groupmanager.api;

import java.util.UUID;

/**
 * Listener interface for group lifecycle events.
 * All methods have default no-op implementations.
 */
public interface GroupEventListener {

    default void onMemberJoined(UUID groupId, UUID playerUuid) {}

    default void onMemberLeft(UUID groupId, UUID playerUuid) {}

    default void onMemberKicked(UUID groupId, UUID kickedUuid, UUID kickerUuid) {}

    default void onGroupDissolved(UUID groupId) {}

    default void onLeadershipTransferred(UUID groupId, UUID oldLeader, UUID newLeader) {}
}
