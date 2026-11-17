package edu.lab.entity;

/** 仪器设备使用登记实体。 */
public class EquipmentUse {
    private Long id;
    private String equipmentName;
    private String userName;
    private String useTime;
    private String purpose;
    private String returnStatus;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUseTime() { return useTime; }
    public void setUseTime(String useTime) { this.useTime = useTime; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getReturnStatus() { return returnStatus; }
    public void setReturnStatus(String returnStatus) { this.returnStatus = returnStatus; }
}
