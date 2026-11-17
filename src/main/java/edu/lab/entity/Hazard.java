package edu.lab.entity;

/** 实验室危险源实体。 */
public class Hazard {
    private Long id;
    private String name;
    private String labName;
    private String level;
    private String protection;
    private String emergency;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLabName() { return labName; }
    public void setLabName(String labName) { this.labName = labName; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getProtection() { return protection; }
    public void setProtection(String protection) { this.protection = protection; }
    public String getEmergency() { return emergency; }
    public void setEmergency(String emergency) { this.emergency = emergency; }
}
