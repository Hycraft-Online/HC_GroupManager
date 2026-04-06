package com.groupmanager.api;

/**
 * Abstract base class for GroupService implementations.
 * ArenaPvP's bytecode expects GroupManagerProvider.get() to return GroupManagerAPI,
 * which is then stored in a GroupService-typed field (so it must extend GroupService).
 */
public interface GroupManagerAPI extends GroupService {
}
