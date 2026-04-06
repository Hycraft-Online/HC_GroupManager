package com.groupmanager.api;

/**
 * Static accessor for the GroupManagerAPI singleton.
 * ArenaPvP calls GroupManagerProvider.get() to obtain the service.
 */
public class GroupManagerProvider {

    private static volatile GroupManagerAPI instance;

    private GroupManagerProvider() {}

    public static GroupManagerAPI get() {
        return instance;
    }

    public static void set(GroupManagerAPI service) {
        instance = service;
    }
}
