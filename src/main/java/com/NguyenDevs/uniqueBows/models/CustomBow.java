package com.NguyenDevs.uniqueBows.models;

import java.util.List;

public class CustomBow {

    private final String id;
    private final String name;
    private final List<String> lore;
    private final boolean craftable;
    private final boolean unbreakable;
    private final int delay;

    public CustomBow(String id, String name, List<String> lore, boolean craftable, boolean unbreakable, int delay) {
        this.id = id;
        this.name = name;
        this.lore = lore;
        this.craftable = craftable;
        this.unbreakable = unbreakable;
        this.delay = delay;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public boolean isCraftable() {
        return craftable;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public int getDelay() {
        return delay;
    }
}