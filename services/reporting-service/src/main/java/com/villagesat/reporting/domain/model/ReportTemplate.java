package com.villagesat.reporting.domain.model;

public class ReportTemplate {

    private ReportType type;
    private String name;
    private String description;

    public ReportTemplate() {}

    public ReportTemplate(ReportType type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public ReportType getType() { return type; }
    public void setType(ReportType type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
