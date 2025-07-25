# SafeGate - Web Application Security Gateway

SafeGate is a comprehensive web application security monitoring and reporting system designed to detect, track, and respond to various web security vulnerabilities and attacks.

## Overview

SafeGate serves as a security gateway that monitors web traffic, detects potential security threats, and provides a platform for reporting and managing security incidents. The system allows different types of users to report security issues, which can then be tracked and resolved by administrators and security professionals.

## Key Features

- **Web Application Firewall (WAF)**: Actively detects and blocks malicious requests using signature-based rules
- **Security Monitoring**: Monitors and logs all incoming HTTP requests
- **Attack Detection**: Identifies common attack patterns like SQL Injection, XSS, Command Injection, and Directory Traversal
- **Security Logging**: Records detailed information about blocked attacks for analysis
- **Performance Testing**: Provides tools to test and evaluate WAF performance with dataset testing
- **Vulnerability Reporting**: Allows users to report various types of web security vulnerabilities
- **Incident Management**: Tracks security incidents through their lifecycle from reporting to resolution
- **User Management**: Supports different user roles with varying levels of access and responsibilities

## Architecture

SafeGate is built as a multi-component system:

1. **Spring Boot Backend API**: Core application that handles business logic, data persistence, and API endpoints
2. **Web Application Firewall (WAF)**: Filters incoming requests and blocks malicious traffic based on signature rules
3. **Signature Rules Engine**: Manages and applies security rules to detect common attack patterns
4. **MySQL Database**: Stores user data, security reports, gate information, and blocked request logs
5. **Performance Testing Module**: Evaluates WAF effectiveness by tracking passed and blocked requests
6. **Dataset Testing**: Tests WAF against various attack payload datasets in multiple formats

## Technology Stack

- **Backend**: Java 21, Spring Boot
- **Database**: MySQL 8.0
- **Containerization**: Docker, Docker Compose
- **Build Tool**: Gradle

## Security Features

### Web Application Firewall (WAF)

SafeGate includes a built-in Web Application Firewall that actively monitors incoming requests and blocks malicious traffic. The WAF:

- Intercepts all HTTP requests
- Normalizes request data (method, path, query parameters)
- Applies signature-based rules to detect attack patterns
- Blocks requests that match known attack signatures
- Logs detailed information about blocked requests

### Signature-Based Detection

The system uses a signature rules engine with regex patterns to detect common attack types:

- SQL Injection (SQLI-001) - Detects common SQL injection patterns
- Cross-Site Scripting (XSS-001) - Detects script tags and JavaScript code
- Command Injection (CMDI-001) - Detects attempts to execute system commands
- Directory Traversal (TRAV-001) - Detects path traversal attempts

### WAF Performance Testing

SafeGate includes a testing module that allows users to evaluate the performance of the WAF:

- Start and stop test runs to measure WAF effectiveness
- Track passed and blocked requests during test periods
- Record which rules blocked requests and how many times
- View detailed test results including start/end times and request counts
- Compare results across multiple test runs

### Dataset Testing

The system supports testing WAF rules against datasets of attack payloads:

- Upload datasets in various formats (CSV, JSON, XML, TXT, TSV)
- Automatic format detection
- Configurable sampling options (All, Random 100, Random 1,000, etc.)
- Detailed reporting on blocked and passed payloads
- Support for the CSIC 2010 dataset and other common WAF testing datasets

## User Roles

- **CITIZEN**: Regular users who can report security issues
- **ADMIN**: Administrators with full system access
- **MODERATOR**: Users who can moderate and manage reports
- **EMERGENCY_RESPONDER**: Users who respond to critical security incidents

## Report Management

Security reports in SafeGate have:

- **Types**: Various vulnerability types (SQL Injection, XSS, etc.)
- **Severity Levels**: INFO, LOW, MEDIUM, HIGH, CRITICAL
- **Statuses**: PENDING, INVESTIGATING, RESOLVED, CLOSED, REJECTED

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 21 (for local development)
- Gradle (for local development)

### Running with Docker

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/SafeGate.git
   cd SafeGate
   ```

2. Start the application using Docker Compose:
   ```
   docker-compose up -d
   ```

3. Access the application at http://localhost:8080

### Local Development

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/SafeGate.git
   cd SafeGate
   ```

2. Build the application:
   ```
   ./gradlew build
   ```

3. Run the application:
   ```
   ./gradlew bootRun
   ```

4. Access the application at http://localhost:8080

## API Documentation

For detailed API documentation, please refer to the [API Documentation](API_DOCUMENTATION.md) file.

## Dataset Testing Guide

For information on how to use the dataset testing feature, including supported formats and usage examples, please refer to the [Dataset Testing Guide](DATASET_GUIDE.md) file.

## Web Interface

SafeGate provides a simple web interface with the following pages:
- Admin Console (index.html) - Main dashboard with links to other pages
- Blocked Requests Log (logs.html) - View logs of blocked requests
- WAF Signature Rules (rules.html) - Manage WAF signature rules
- Performance Testing Dashboard (testing.html) - Run and view WAF performance tests

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the [MIT License](LICENSE).

## Last Updated

2025-07-25