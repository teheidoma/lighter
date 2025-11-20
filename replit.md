# Lighter Application

## Overview
Lighter is a Spring Boot application that aggregates schedule information from multiple sources including web scraping and Telegram channels. The application uses AI (Ollama) to parse and extract schedule data from unstructured text.

## Project Information
- **Framework**: Spring Boot 3.5.7
- **Language**: Java 19 (GraalVM CE 22.3.1)
- **Build Tool**: Gradle 9.2.0
- **Database**: H2 (in-memory)
- **AI Integration**: Spring AI with Ollama (llama3.1:8b model)

## Architecture

### Core Components
1. **Provider System**: Pluggable providers for different data sources
   - `Provider` interface: Base interface for all data providers
   - `KiroeProvider`: Scrapes schedule data from kiroe.com.ua website
   - `TelegramProvider`: Abstract base for Telegram channel scraping with AI parsing

2. **REST API Endpoints**:
   - `GET /list` - Returns list of available providers
   - `GET /{id}` - Returns schedule data from specific provider
   - `GET /remaining/{id}/{group}` - Returns remaining schedule for a specific group
   - `GET /actuator` - Spring Boot Actuator endpoints
   - `GET /h2-console` - H2 Database console

3. **Data Model**:
   - `Schedule`: Main schedule record containing city, update time, and queues
   - `Queue`: Individual queue with intervals

### Key Dependencies
- Spring Boot Web (REST API)
- Spring Boot WebFlux (Reactive web client)
- Spring Data JPA (Database access)
- Spring AI Ollama (AI integration)
- Jsoup (HTML parsing)
- GraalVM Polyglot (Python script execution)
- H2 Database (In-memory storage)
- Lombok (Code generation)

## Replit Configuration

### Server Settings
- **Port**: 5000 (configured in `application.yaml`)
- **Host**: 0.0.0.0 (allows Replit proxy to work correctly)
- **Development Mode**: Spring DevTools enabled with LiveReload

### Workflow
- **Name**: Run Application
- **Command**: `./gradlew bootRun`
- **Output**: Webview on port 5000

### Deployment
- **Type**: Autoscale (stateless web application)
- **Build Command**: `./gradlew build -x test`
- **Run Command**: `./gradlew bootRun`

## Project Structure
```
├── gradle/               # Gradle wrapper files
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/teheidom/lighter/
│   │   │       ├── provider/         # Provider implementations
│   │   │       ├── LighterApplication.java
│   │   │       ├── MainController.java
│   │   │       └── Schedule.java
│   │   └── resources/
│   │       └── application.yaml      # Spring configuration
│   └── test/                         # Test files
├── build.gradle                      # Gradle build configuration
└── .gitignore                        # Git ignore patterns
```

## Recent Changes (2025-11-20)
1. Fixed Java version compatibility - changed from Java 25 to Java 19 to match available GraalVM
2. Fixed syntax error in MainController.java - corrected lambda expression in getRemaining method
3. Configured server to run on port 5000 with host 0.0.0.0 for Replit environment
4. Added comprehensive .gitignore for Java/Gradle projects
5. Configured deployment settings for Replit autoscale deployment
6. Successfully built project and started Spring Boot application

## Development Notes
- The application uses GraalVM's Polyglot feature to execute Python scripts for parsing Telegram messages
- Ollama AI model (llama3.1:8b) is used for intelligent text extraction
- H2 database console is accessible at `/h2-console` for debugging
- Spring Boot DevTools provides hot-reload during development
- No concrete TelegramProvider implementations are instantiated yet (abstract class only)

## Known Issues
- Application requires Ollama service to be running for AI-based parsing (not available in Replit by default)
- TelegramProvider is abstract and needs concrete implementations to function
- The `/remaining/{id}/{group}` endpoint implementation may need refinement based on actual use cases
