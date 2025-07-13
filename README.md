# SafeGate - Web Application Security Gateway

SafeGate is a comprehensive web application security monitoring and reporting system designed to detect, track, and respond to various web security vulnerabilities and attacks.

## Overview

SafeGate serves as a security gateway that monitors web traffic, detects potential security threats, and provides a platform for reporting and managing security incidents. The system allows different types of users to report security issues, which can then be tracked and resolved by administrators and security professionals.

## Features

- **Security Monitoring**: Monitors and logs all incoming HTTP requests
- **Vulnerability Reporting**: Allows users to report various types of web security vulnerabilities
- **Incident Management**: Tracks security incidents through their lifecycle from reporting to resolution
- **User Management**: Supports different user roles with varying levels of access and responsibilities
- **Gate Management**: Monitors and manages security gates/checkpoints

## Architecture

SafeGate is built as a multi-component system:

1. **Spring Boot Backend API**: Core application that handles business logic, data persistence, and API endpoints
2. **MySQL Database**: Stores user data, security reports, and gate information
3. **Analyzer Component** (Planned): Will provide AI/ML-based analysis of security data
4. **Dashboard** (Planned): Will provide a user-friendly interface for interacting with the system

## Technology Stack

- **Backend**: Java 21, Spring Boot
- **Database**: MySQL 8.0
- **Containerization**: Docker, Docker Compose
- **Build Tool**: Gradle

## Security Features

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

- `/hello` - Test endpoint that returns "Hello, World!"
- More endpoints will be added as development progresses

## Future Development

- Implementation of the Analyzer component for AI/ML-based security analysis
- Development of a user-friendly dashboard interface
- Enhanced security monitoring and threat detection capabilities
- Integration with external security tools and services

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the [MIT License](LICENSE).
