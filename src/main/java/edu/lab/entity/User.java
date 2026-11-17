package edu.lab.entity;

/** 系统用户实体。 */
public class User {
    private Long id;
    private String username;
    private String realName;
    private String role;
    private boolean qualified;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isQualified() { return qualified; }
    public void setQualified(boolean qualified) { this.qualified = qualified; }
}
