package info.dsandrade.postgrescache.service;

public class UserProfile {

    private String id;
    private String name;

    public UserProfile() {
    }

    public UserProfile(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public UserProfile setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public UserProfile setName(String name) {
        this.name = name;
        return this;
    }
}
