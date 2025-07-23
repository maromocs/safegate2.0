# SafeGate - Web Application Security Gateway

SafeGate is a comprehensive web application security monitoring and reporting system designed to detect, track, and respond to various web security vulnerabilities and attacks.

## Overview

SafeGate serves as a security gateway that monitors web traffic, detects potential security threats, and provides a platform for reporting and managing security incidents. The system allows different types of users to report security issues, which can then be tracked and resolved by administrators and security professionals.

## Features

- **Web Application Firewall (WAF)**: Actively detects and blocks malicious requests using signature-based rules
- **Security Monitoring**: Monitors and logs all incoming HTTP requests
- **Attack Detection**: Identifies common attack patterns like SQL Injection, XSS, Command Injection, and Directory Traversal
- **Security Logging**: Records detailed information about blocked attacks for analysis
- **Performance Testing**: Provides tools to test and evaluate WAF performance
- **Vulnerability Reporting**: Allows users to report various types of web security vulnerabilities
- **Incident Management**: Tracks security incidents through their lifecycle from reporting to resolution
- **User Management**: Supports different user roles with varying levels of access and responsibilities
- **Gate Management**: Monitors and manages security gates/checkpoints

## Architecture

SafeGate is built as a multi-component system:

1. **Spring Boot Backend API**: Core application that handles business logic, data persistence, and API endpoints
2. **Web Application Firewall (WAF)**: Filters incoming requests and blocks malicious traffic based on signature rules
3. **Signature Rules Engine**: Manages and applies security rules to detect common attack patterns
4. **MySQL Database**: Stores user data, security reports, gate information, and blocked request logs
5. **Performance Testing Module**: Evaluates WAF effectiveness by tracking passed and blocked requests
6. **Analyzer Component** (Planned): Will provide AI/ML-based analysis of security data
7. **Dashboard** (Planned): Will provide a user-friendly interface for interacting with the system

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

### Security Monitoring and Reporting
SafeGate can detect and report on various types of web security vulnerabilities, including:

- SQL Injection
- Cross-Site Scripting (XSS)
- Cross-Site Request Forgery (CSRF)
- Brute Force Attacks
- Denial of Service (DoS)
- HTTP Response Splitting (HRS)
- Directory Traversal
- Local File Inclusion
- Remote File Inclusion
- XML External Entity (XXE)
- Command Injection

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

## API Endpoints

### Test Endpoints
- `/hello` - Test endpoint that returns "Hello, World!"

### Security Endpoints
- `/api/security/blocked` - Get all blocked requests ordered by timestamp
- `/api/security/blocked/count` - Get the total count of blocked requests

### Logs Endpoints
- `/api/logs` - Get all logs of blocked requests
- `/api/logs/recent` - Get recent logs (default: last 24 hours)
- `/api/logs/stats` - Get statistics about blocked requests (total count, last 24h count, attack patterns, top attacking IPs)
- `/api/logs` (DELETE) - Clear all logs

### Rules Endpoints
- `/api/rules` - Get all signature rules
- `/api/rules/{id}` - Get a rule by ID
- `/api/rules` (POST) - Add a new rule
- `/api/rules/{id}` (PUT) - Update a rule
- `/api/rules/{id}` (DELETE) - Delete a rule
- `/api/rules/{id}/toggle` - Toggle a rule's enabled status

### Testing Endpoints
- `/api/tests/start` - Start a WAF test run
- `/api/tests/stop` - Stop the current test run and save results
- `/api/tests/status` - Get the status of the current test run
- `/api/tests/results` - Get results of all test runs

## Security Logging System

SafeGate includes a comprehensive logging system for security incidents:

### Blocked Request Logging
When the WAF blocks a malicious request, it records detailed information:
- Timestamp of the attack
- Source IP address
- Attack pattern that was matched
- Raw payload that triggered the block
- Rule ID that was matched
- HTTP method (GET, POST, etc.)
- Request URI (path)
- User-Agent header

### Security Analytics
The system provides several analytics endpoints:
- Total number of blocked requests
- Requests blocked in the last 24 hours
- Distribution of attack patterns
- Top attacking IP addresses

### Log Management
Administrators can:
- View all blocked requests
- Filter logs by time period
- Clear logs when needed

## Web Interface

SafeGate provides a simple web interface with the following pages:
- Admin Console (index.html) - Main dashboard with links to other pages
- Blocked Requests Log (logs.html) - View logs of blocked requests
- WAF Signature Rules (rules.html) - Manage WAF signature rules
- Performance Testing Dashboard (testing.html) - Run and view WAF performance tests

## Future Development

- Implementation of the Analyzer component for AI/ML-based security analysis
- Development of a more comprehensive user-friendly dashboard interface
- Enhanced security monitoring and threat detection capabilities
- Integration with external security tools and services
- Expansion of signature rules to detect more attack patterns
- Real-time alerting for security incidents

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the [MIT License](LICENSE).

## Last Updated

2025-07-24