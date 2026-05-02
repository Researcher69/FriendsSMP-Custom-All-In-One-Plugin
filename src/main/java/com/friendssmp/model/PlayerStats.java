package com.friendssmp.model;

import java.util.UUID;

public final class PlayerStats {
    private final UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private long playtimeMinutes;

    public PlayerStats(UUID uuid, String name, int kills, int deaths, long playtimeMinutes) {
        this.uuid = uuid;
        this.name = name;
        this.kills = kills;
        this.deaths = deaths;
        this.playtimeMinutes = playtimeMinutes;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public int kills() {
        return kills;
    }

    public void addKill() {
        kills++;
    }

    public int deaths() {
        return deaths;
    }

    public void addDeath() {
        deaths++;
    }

    public long playtimeMinutes() {
        return playtimeMinutes;
    }

    public void addPlaytimeMinutes(long minutes) {
        playtimeMinutes += Math.max(0, minutes);
    }
}
