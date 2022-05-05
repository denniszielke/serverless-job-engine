package org.acme;

public class JobRequest {
    public String message;
    public String guid;

    public JobRequest() {
    }

    public JobRequest(String message, String guid) {
        this.message = message;
        this.guid = message;
    }
}
