package com.SafeGate.model;

/**
 * Data class to hold HTTP request information extracted from dataset files.
 * This class is used to pass data between the DatasetParsingService and the DatasetTestRunnerService.
 */
public class HttpRequestData {
    private String method;
    private String payload;

    /**
     * Constructor for HttpRequestData
     * @param method The HTTP method (GET, POST, etc.)
     * @param payload The payload data
     */
    public HttpRequestData(String method, String payload) {
        this.method = method;
        this.payload = payload;
    }

    /**
     * Get the HTTP method
     * @return The HTTP method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Set the HTTP method
     * @param method The HTTP method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Get the payload data
     * @return The payload data
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Set the payload data
     * @param payload The payload data
     */
    public void setPayload(String payload) {
        this.payload = payload;
    }

    /**
     * Returns a string representation of the HttpRequestData
     * @return A string representation of the HttpRequestData
     */
    @Override
    public String toString() {
        return "HttpRequestData{" +
                "method='" + method + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}