package com.SafeGate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for parsing dataset files in various formats.
 * This service extracts payloads from uploaded files for WAF testing.
 */
@Service
public class DatasetParsingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatasetParsingService.class);

    /**
     * Parses the uploaded file based on its specified type and extracts HTTP request data.
     *
     * @param file        The uploaded dataset file.
     * @param datasetType The type of the dataset (e.g., "CSV", "TXT", "AUTO").
     * @return A list of HttpRequestData objects containing method and payload information.
     * @throws Exception if parsing fails.
     */
    public List<com.SafeGate.model.HttpRequestData> getPayloadsFromFile(MultipartFile file, String datasetType) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty.");
        }

        // If datasetType is AUTO or null, detect the format
        if (datasetType == null || datasetType.trim().isEmpty() || "AUTO".equalsIgnoreCase(datasetType)) {
            datasetType = detectFileFormat(file);
            logger.info("Auto-detected file format: {}", datasetType);
        }

        return switch (datasetType.toUpperCase()) {
            case "CSV" -> parseCsvFile(file);
            case "TXT" -> convertToHttpRequestData(parseTxtFile(file));
            case "JSON" -> convertToHttpRequestData(parseJsonFile(file));
            case "XML" -> convertToHttpRequestData(parseXmlFile(file));
            case "TSV" -> convertToHttpRequestData(parseTsvFile(file));
            default -> throw new IllegalArgumentException("Unsupported dataset type: " + datasetType);
        };
    }

    /**
     * Converts a list of payload strings to a list of HttpRequestData objects.
     * For non-CSV formats, we default to GET method since we don't have method information.
     *
     * @param payloads The list of payload strings.
     * @return A list of HttpRequestData objects.
     */
    private List<com.SafeGate.model.HttpRequestData> convertToHttpRequestData(List<String> payloads) {
        return payloads.stream()
                .map(payload -> new com.SafeGate.model.HttpRequestData("GET", payload))
                .collect(Collectors.toList());
    }

    /**
     * Parses a text file with one payload per line.
     */
    private List<String> parseTxtFile(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.trim().isEmpty())
                    .collect(Collectors.toList());
        }
    }

    /**
     * Parses a CSV file, looking for columns named "method" and "payload".
     */
    private List<com.SafeGate.model.HttpRequestData> parseCsvFile(MultipartFile file) throws Exception {
        List<com.SafeGate.model.HttpRequestData> requestDataList = new ArrayList<>();
    
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            // Read the header line
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty or has no headers.");
            }

            // Find the indices of the "method" and "payload" columns
            String[] headers;
            int methodColumnIndex = -1;
            int payloadColumnIndex = -1;
        
            // Check if the header has quoted values
            if (headerLine.contains("\"")) {
                // Use regex to split by commas outside of quotes
                headers = headerLine.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
            
                // Clean up the quoted values
                for (int i = 0; i < headers.length; i++) {
                    String header = headers[i].trim();
                    if (header.startsWith("\"") && header.endsWith("\"")) {
                        headers[i] = header.substring(1, header.length() - 1);
                    }
                }
            } else {
                headers = headerLine.split(",");
            }
        
            if (headers.length == 0) {
                throw new IllegalArgumentException("CSV file has no columns.");
            }
        
            // Log all headers for debugging
            logger.debug("CSV headers found ({}): {}", headers.length, String.join(", ", headers));
        
            // Look for columns named "method" and "payload" (case insensitive)
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].trim();
                if (header.equalsIgnoreCase("method")) {
                    methodColumnIndex = i;
                    logger.info("Found method column at index {}", methodColumnIndex);
                } else if (header.equalsIgnoreCase("payload")) {
                    payloadColumnIndex = i;
                    logger.info("Found payload column at index {}", payloadColumnIndex);
                }
            }
        
            // If no method column was found, default to "GET"
            if (methodColumnIndex == -1) {
                logger.info("No column named 'method' found. Defaulting to GET method for all requests.");
            }
        
            // If no payload column was found, use the last column
            if (payloadColumnIndex == -1) {
                payloadColumnIndex = headers.length - 1;
                logger.info("No column named 'payload' found. Using column '{}' as payload column", 
                        headers[payloadColumnIndex]);
            }
        
            // Read data rows and extract method and payload
            String line;
            int rowCount = 0;
            while ((line = reader.readLine()) != null) {
                rowCount++;
                if (!line.trim().isEmpty()) {
                    try {
                        // Split the line, being careful about quoted values that might contain commas
                        String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                    
                        if (parts.length > payloadColumnIndex) {
                            // Extract method (default to GET if not found)
                            String method = "GET";
                            if (methodColumnIndex != -1 && parts.length > methodColumnIndex) {
                                method = parts[methodColumnIndex].trim();
                                // Remove quotes if present
                                if (method.startsWith("\"") && method.endsWith("\"")) {
                                    method = method.substring(1, method.length() - 1);
                                }
                            }
                        
                            // Extract payload
                            String payload = parts[payloadColumnIndex].trim();
                            // Remove quotes if present
                            if (payload.startsWith("\"") && payload.endsWith("\"")) {
                                payload = payload.substring(1, payload.length() - 1);
                            }
                        
                            if (!payload.isEmpty()) {
                                requestDataList.add(new com.SafeGate.model.HttpRequestData(method, payload));
                            }
                        } else {
                            logger.warn("Row {} has fewer columns ({}) than the payload column index ({})", 
                                    rowCount, parts.length, payloadColumnIndex);
                        }
                    } catch (Exception e) {
                        logger.warn("Error parsing CSV row {}: {}", rowCount, e.getMessage());
                        // Continue processing other rows
                    }
                }
            }
        
            if (requestDataList.isEmpty()) {
                throw new IllegalArgumentException("No request data could be extracted from the CSV file.");
            }
        
            logger.info("Extracted {} request data entries from CSV file with {} data rows", requestDataList.size(), rowCount);
        }
    
        return requestDataList;
    }

    /**
     * Parses a TSV (tab-separated values) file.
     */
    private List<String> parseTsvFile(MultipartFile file) throws Exception {
        List<String> payloads = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            // Read the header line
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new IllegalArgumentException("TSV file is empty or has no headers.");
            }

            // Find the index of the "payload" column
            String[] headers = headerLine.split("\t");
            int payloadColumnIndex = -1;
            
            // Look for a column named "payload" (case insensitive)
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase("payload")) {
                    payloadColumnIndex = i;
                    break;
                }
            }
            
            // If no payload column was found, use the last column
            if (payloadColumnIndex == -1) {
                payloadColumnIndex = headers.length - 1;
            }
            
            // Read data rows and extract payloads
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split("\t");
                    if (parts.length > payloadColumnIndex) {
                        String payload = parts[payloadColumnIndex].trim();
                        if (!payload.isEmpty()) {
                            payloads.add(payload);
                        }
                    }
                }
            }
        }
        
        return payloads;
    }

    /**
     * Parses a JSON file, looking for payloads in various formats.
     */
    private List<String> parseJsonFile(MultipartFile file) throws Exception {
        List<String> payloads = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            
            String json = jsonContent.toString().trim();
            if (json.isEmpty()) {
                throw new IllegalArgumentException("JSON file is empty.");
            }
            
            // Simple JSON parsing - this is a basic implementation
            // For production use, consider using a proper JSON parser like Jackson or Gson
            
            if (json.startsWith("[") && json.endsWith("]")) {
                // It's an array
                json = json.substring(1, json.length() - 1);
                
                // Handle empty array
                if (json.trim().isEmpty()) {
                    return payloads;
                }
                
                // Split by commas, but be careful about nested objects
                List<String> items = new ArrayList<>();
                int depth = 0;
                int startPos = 0;
                
                for (int i = 0; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (c == '{' || c == '[') depth++;
                    else if (c == '}' || c == ']') depth--;
                    else if (c == ',' && depth == 0) {
                        items.add(json.substring(startPos, i).trim());
                        startPos = i + 1;
                    }
                }
                
                // Add the last item
                if (startPos < json.length()) {
                    items.add(json.substring(startPos).trim());
                }
                
                for (String item : items) {
                    if (item.startsWith("\"") && item.endsWith("\"")) {
                        // It's a string
                        payloads.add(item.substring(1, item.length() - 1));
                    } else if (item.contains("\"payload\"")) {
                        // It's an object with a payload field
                        int start = item.indexOf("\"payload\"");
                        int valueStart = item.indexOf(":", start) + 1;
                        if (valueStart > 0) {
                            int valueEnd = item.indexOf(",", valueStart);
                            if (valueEnd == -1) valueEnd = item.indexOf("}", valueStart);
                            if (valueEnd != -1) {
                                String value = item.substring(valueStart, valueEnd).trim();
                                if (value.startsWith("\"") && value.endsWith("\"")) {
                                    payloads.add(value.substring(1, value.length() - 1));
                                }
                            }
                        }
                    }
                }
            } else if (json.startsWith("{") && json.endsWith("}")) {
                // It's a single object, look for payload field
                if (json.contains("\"payload\"")) {
                    int start = json.indexOf("\"payload\"");
                    int valueStart = json.indexOf(":", start) + 1;
                    if (valueStart > 0) {
                        int valueEnd = json.indexOf(",", valueStart);
                        if (valueEnd == -1) valueEnd = json.indexOf("}", valueStart);
                        if (valueEnd != -1) {
                            String value = json.substring(valueStart, valueEnd).trim();
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                payloads.add(value.substring(1, value.length() - 1));
                            }
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("Invalid JSON format. Expected an array or object.");
            }
        }
        
        return payloads;
    }

    /**
     * Parses an XML file, looking for payload tags or attributes.
     */
    private List<String> parseXmlFile(MultipartFile file) throws Exception {
        List<String> payloads = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder xmlContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                xmlContent.append(line);
            }
            
            String xml = xmlContent.toString().trim();
            if (xml.isEmpty()) {
                throw new IllegalArgumentException("XML file is empty.");
            }
            
            // Simple XML parsing - this is a basic implementation
            // For production use, consider using a proper XML parser
            
            // Look for payload tags (case insensitive)
            List<String> payloadTags = Arrays.asList("<payload>", "<PAYLOAD>", "<Payload>");
            List<String> payloadEndTags = Arrays.asList("</payload>", "</PAYLOAD>", "</Payload>");
            
            int index = 0;
            int payloadCount = 0;
            
            while (index < xml.length()) {
                // Find the next payload tag
                int tagStart = -1;
                String matchedTag = null;
                String matchedEndTag = null;
                
                for (int i = 0; i < payloadTags.size(); i++) {
                    String tag = payloadTags.get(i);
                    int pos = xml.indexOf(tag, index);
                    if (pos != -1 && (tagStart == -1 || pos < tagStart)) {
                        tagStart = pos;
                        matchedTag = tag;
                        matchedEndTag = payloadEndTags.get(i);
                    }
                }
                
                if (tagStart == -1 || matchedTag == null) {
                    // No more payload tags found
                    break;
                }
                
                // Find the end tag
                int contentStart = tagStart + matchedTag.length();
                int tagEnd = xml.indexOf(matchedEndTag, contentStart);
                
                if (tagEnd == -1) {
                    logger.warn("Found opening <payload> tag at position {} but no matching closing tag", tagStart);
                    // Move past this tag to avoid infinite loop
                    index = contentStart;
                    continue;
                }
                
                // Extract the payload content
                String payload = xml.substring(contentStart, tagEnd).trim();
                if (!payload.isEmpty()) {
                    payloads.add(payload);
                    payloadCount++;
                }
                
                // Move past this tag
                index = tagEnd + matchedEndTag.length();
            }
            
            // If no payload tags were found, try looking for a 'payload' attribute
            if (payloadCount == 0) {
                // Use a simple regex to find payload attributes
                // For production use, consider using a proper XML parser
                int attrIndex = 0;
                while ((attrIndex = xml.indexOf("payload=\"", attrIndex)) != -1) {
                    int valueStart = attrIndex + 9; // length of 'payload="'
                    int valueEnd = xml.indexOf("\"", valueStart);
                    if (valueEnd != -1) {
                        String payload = xml.substring(valueStart, valueEnd).trim();
                        if (!payload.isEmpty()) {
                            payloads.add(payload);
                            payloadCount++;
                        }
                    }
                    attrIndex = valueEnd + 1;
                }
            }
        }
        
        return payloads;
    }

    /**
     * Attempts to detect the format of a file based on its content and extension.
     * 
     * @param file The file to analyze
     * @return The detected format (CSV, TXT, JSON, XML, TSV)
     * @throws Exception if the file cannot be read or the format cannot be detected
     */
    private String detectFileFormat(MultipartFile file) throws Exception {
        // Check file extension first as a hint
        String fileName = file.getOriginalFilename();
        String fileExtension = "";
        if (fileName != null && fileName.contains(".")) {
            fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        
        // Read the first few lines to analyze content
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 10) {
                content.append(line).append("\n");
                lineCount++;
            }
            
            String sample = content.toString().trim();
            if (sample.isEmpty()) {
                throw new IllegalArgumentException("File is empty.");
            }
            
            // Check for CSV format with quoted values
            if (sample.contains(",") && (sample.contains("\"") || sample.contains("'"))) {
                String[] lines = sample.split("\n");
                
                // Check if first line might be a header
                if (lines.length > 1) {
                    String firstLine = lines[0];
                    // Headers often contain words, not just numbers
                    if (firstLine.matches(".*[a-zA-Z].*") && firstLine.contains(",")) {
                        // Look for a payload column in the header
                        String[] headers;
                        
                        // Check if the header has quoted values
                        if (firstLine.contains("\"")) {
                            // Use regex to split by commas outside of quotes
                            headers = firstLine.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                            
                            // Clean up the quoted values
                            for (int i = 0; i < headers.length; i++) {
                                String header = headers[i].trim();
                                if (header.startsWith("\"") && header.endsWith("\"")) {
                                    headers[i] = header.substring(1, header.length() - 1);
                                }
                            }
                        } else {
                            headers = firstLine.split(",");
                        }
                        
                        for (String header : headers) {
                            if (header.trim().equalsIgnoreCase("payload")) {
                                logger.info("Found 'payload' column in CSV header");
                                return "CSV";
                            }
                        }
                    }
                }
                
                // Even without a payload column, if it looks like CSV, return CSV
                return "CSV";
            }
            
            // Check for JSON format
            if (sample.startsWith("{") && sample.endsWith("}") && sample.contains("\":")) {
                return "JSON";
            } else if (sample.startsWith("[") && sample.endsWith("]")) {
                if (sample.contains("{\"") || sample.contains("\":")) {
                    return "JSON";
                }
            }
            
            // Check for XML format
            if (sample.startsWith("<") && sample.contains("</") && sample.contains(">")) {
                return "XML";
            }
            
            // Check for TSV format (tab-separated values)
            if (sample.contains("\t") && lineCount > 1) {
                String[] lines = sample.split("\n");
                // Check if most lines have tabs
                int linesWithTabs = 0;
                for (String l : lines) {
                    if (l.contains("\t")) {
                        linesWithTabs++;
                    }
                }
                if (linesWithTabs > lines.length / 2) {
                    return "TSV";
                }
            }
            
            // Check for CSV format without quotes
            if (sample.contains(",")) {
                String[] lines = sample.split("\n");
                
                // Count lines with commas
                int linesWithCommas = 0;
                for (String l : lines) {
                    if (l.contains(",")) {
                        linesWithCommas++;
                    }
                }
                
                // If most lines have commas, it's likely CSV
                if (linesWithCommas > lines.length / 2) {
                    return "CSV";
                }
            }
            
            // Use file extension as a fallback
            if (fileExtension.equals("json")) {
                return "JSON";
            } else if (fileExtension.equals("xml")) {
                return "XML";
            } else if (fileExtension.equals("csv")) {
                return "CSV";
            } else if (fileExtension.equals("tsv")) {
                return "TSV";
            }
            
            // Default to TXT
            return "TXT";
        }
    }
}