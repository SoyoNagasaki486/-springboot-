package edu.lab.entity;

/** 系统操作日志实体，用于审计和责任追溯。 */
public class OperationLog {
    private Long id;
    private String operatorName;
    private String action;
    private String module;
    private String operateTime;
    private String detail;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getOperateTime() { return operateTime; }
    public void setOperateTime(String operateTime) { this.operateTime = operateTime; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
