package com.malllease.model;

import java.time.LocalDateTime;

public class ShowingRequestView {
    private int showingId;
    private int clientId;
    private String companyName;
    private String contactPerson;
    private int managerUserId;
    private String managerName;
    private LocalDateTime shownAt;
    private String result;
    private String comment;
    private String pointCodes;
    private String pointLocations;

    public int getShowingId() {
        return showingId;
    }

    public void setShowingId(int showingId) {
        this.showingId = showingId;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public int getManagerUserId() {
        return managerUserId;
    }

    public void setManagerUserId(int managerUserId) {
        this.managerUserId = managerUserId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public LocalDateTime getShownAt() {
        return shownAt;
    }

    public void setShownAt(LocalDateTime shownAt) {
        this.shownAt = shownAt;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPointCodes() {
        return pointCodes;
    }

    public void setPointCodes(String pointCodes) {
        this.pointCodes = pointCodes;
    }

    public String getPointLocations() {
        return pointLocations;
    }

    public void setPointLocations(String pointLocations) {
        this.pointLocations = pointLocations;
    }
}
