package edu.lab.entity;

/** 安全准入考试成绩实体。 */
public class ExamRecord {
    private Long id;
    private String userName;
    private Integer score;
    private Boolean passed;
    private String examTime;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public String getExamTime() { return examTime; }
    public void setExamTime(String examTime) { this.examTime = examTime; }
}
