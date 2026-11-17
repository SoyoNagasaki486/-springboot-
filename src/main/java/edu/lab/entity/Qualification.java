package edu.lab.entity;

/** 实验室准入资格实体。 */
public class Qualification {
    private Long id;
    private String userName;
    private String labName;
    private Integer score;
    private String status;
    private String validUntil;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getLabName() { return labName; }
    public void setLabName(String labName) { this.labName = labName; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getValidUntil() { return validUntil; }
    public void setValidUntil(String validUntil) { this.validUntil = validUntil; }
}
