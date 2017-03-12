package com.ps;

import io.vertx.core.json.JsonObject;

public class Whisky {

    private int id;

    private String name;

    private String origin;

    public Whisky() {}

    public Whisky(int id, String name, String origin) {
        this.id = id;
        this.name = name;
        this.origin = origin;
    }

    public Whisky(String id, String name, String origin) {
        this.id = Integer.valueOf(id);
        this.name = name;
        this.origin = origin;
    }

    public Whisky(String name, String origin) {
        this.name = name;
        this.origin = origin;
    }

    public Whisky(JsonObject entries) {
        this.id = entries.getInteger("ID");
        this.name = entries.getString("NAME");
        this.origin = entries.getString("ORIGIN");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }
}
