package edu.lab.entity;

/** 实验室基础信息实体。 */
public class Lab {
    private Long id;
    private String name;
    private String location;
    private String level;
    private Integer capacity;
    private String status;
    private String openTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }
}
