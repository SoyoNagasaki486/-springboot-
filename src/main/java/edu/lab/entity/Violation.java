package edu.lab.entity;

/** 实验室违规行为实体。 */
public class Violation {
    private Long id;
    private String userName;
    private String type;
    private String occurTime;
    private String handling;
    private String status;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getOccurTime() { return occurTime; }
    public void setOccurTime(String occurTime) { this.occurTime = occurTime; }
    public String getHandling() { return handling; }
    public void setHandling(String handling) { this.handling = handling; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
