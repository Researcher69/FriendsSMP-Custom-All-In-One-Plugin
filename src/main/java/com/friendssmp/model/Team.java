package com.friendssmp.model;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Team {
    private final int id;
    private final String name;
    private UUID ownerUuid;
    private final Set<UUID> members = new HashSet<>();
    private Location home;

    public Team(int id, String name, UUID ownerUuid) {
        this.id = id;
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.members.add(ownerUuid);
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public void ownerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public Set<UUID> members() {
        return members;
    }

    public Location home() {
        return home == null ? null : home.clone();
    }

    public void home(Location home) {
        this.home = home == null ? null : home.clone();
    }
}
