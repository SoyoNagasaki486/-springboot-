package edu.lab.entity;

/** 实验室安全检查实体。 */
public class SafetyCheck {
    private Long id;
    private String labName;
    private String inspector;
    private String checkDate;
    private String result;
    private String issue;
    private String rectification;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLabName() { return labName; }
    public void setLabName(String labName) { this.labName = labName; }
    public String getInspector() { return inspector; }
    public void setInspector(String inspector) { this.inspector = inspector; }
    public String getCheckDate() { return checkDate; }
    public void setCheckDate(String checkDate) { this.checkDate = checkDate; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }
    public String getRectification() { return rectification; }
    public void setRectification(String rectification) { this.rectification = rectification; }
}
