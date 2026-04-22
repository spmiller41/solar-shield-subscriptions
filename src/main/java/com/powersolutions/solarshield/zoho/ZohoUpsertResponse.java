package com.powersolutions.solarshield.zoho;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents the subset of a Zoho upsert response needed by this app.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZohoUpsertResponse {

    private List<Result> data;

    public List<Result> getData() { return data; }

    public void setData(List<Result> data) { this.data = data; }

    public String getFirstRecordId() {
        if (data == null || data.isEmpty()) {
            return null;
        }

        Result firstResult = data.get(0);
        if (firstResult == null || firstResult.getDetails() == null) {
            return null;
        }

        return firstResult.getDetails().getId();
    }

    public boolean hasSuccessfulFirstResult() {
        if (data == null || data.isEmpty() || data.get(0) == null) {
            return false;
        }

        return "success".equalsIgnoreCase(data.get(0).getStatus());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {

        private String code;
        private String action;
        private String message;
        private String status;
        private Details details;

        public String getCode() { return code; }

        public void setCode(String code) { this.code = code; }

        public String getAction() { return action; }

        public void setAction(String action) { this.action = action; }

        public String getMessage() { return message; }

        public void setMessage(String message) { this.message = message; }

        public String getStatus() { return status; }

        public void setStatus(String status) { this.status = status; }

        public Details getDetails() { return details; }

        public void setDetails(Details details) { this.details = details; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Details {

        private String id;

        public String getId() { return id; }

        public void setId(String id) { this.id = id; }
    }

}
