package security;

public final class AppAccount {
    private final String username;
    private final String role;

    public AppAccount(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
