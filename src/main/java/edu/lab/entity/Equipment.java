package edu.lab.entity;

/** 仪器设备台账实体。 */
public class Equipment {
    private Long id;
    private String code;
    private String name;
    private String labName;
    private String model;
    private String status;
    private String purchaseDate;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLabName() { return labName; }
    public void setLabName(String labName) { this.labName = labName; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
}
