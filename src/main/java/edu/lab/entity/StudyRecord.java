package edu.lab.entity;

/** 用户课程学习记录实体。 */
public class StudyRecord {
    private Long id;
    private String userName;
    private String courseTitle;
    private Integer progress;
    private Boolean completed;
    private String studyTime;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public String getStudyTime() { return studyTime; }
    public void setStudyTime(String studyTime) { this.studyTime = studyTime; }
}
