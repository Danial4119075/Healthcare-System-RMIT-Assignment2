

```markdown
# Healthcare Management System

**COSC1295 Advanced Programming - Assignment 2**  
**Student:** Danial Ansari  
**Student ID: S4119075 
**Semester:** 2, 2025

---

## Project Overview

A JavaFX-based healthcare management system for managing patient records, staff scheduling, prescriptions, and bed allocation in a care home environment. Implements role-based access control, real-time roster checking, and database archiving for compliance.

---

## Features

- Patient admission, movement, and discharge with database archiving
- Staff management with role-based permissions (Doctors, Nurses, Managers)
- Shift scheduling with compliance verification
- Prescription creation and medication administration tracking
- Gender-segregated room allocation with isolation support
- Color-coded bed visualization (blue=male, pink=female, white=vacant)
- Comprehensive audit logging
- Real-time shift status display

---

## Technology Stack

| Component | Version |
|-----------|---------|
| Java | 11+ |
| JavaFX | 11+ |
| Gradle | 7.0+ |
| SQLite | 3.x |
| JDBC Driver | 3.42.0.0 |

### Required Dependencies (build.gradle)

```

dependencies {
implementation 'org.openjfx:javafx-controls:11.0.2'
implementation 'org.openjfx:javafx-fxml:11.0.2'
implementation 'org.xerial:sqlite-jdbc:3.42.0.0'
}

```

---

## Prerequisites

- Java JDK 11 or higher
- Gradle 7.0 or above
- Git for version control

---

## How to Run

### Using Gradle Wrapper (Recommended)

**macOS/Linux:**
```

./gradlew clean build
./gradlew run

```

**Windows:**
```

gradlew.bat clean build
gradlew.bat run

```

### Using IDE

1. Import project as Gradle project
2. Wait for sync to complete
3. Run MainApplication.java

---

## Default Login Credentials

| Role | Username | Password |
|------|----------|----------|
| Manager | admin | admin123 |
| Doctor | doctor1 | doc123 |
| Nurse | nurse1 | nur123 |

---

## Project Structure

All source code is located in `src/main/java/healthcare/` with the following packages:

- **model:** Core business entities (Patient, Staff, Ward, Bed, etc.)
- **gui:** JavaFX controllers and dialogs
- **database:** DatabaseManager for SQLite operations
- **exceptions:** Custom exceptions for business rule enforcement
- **utils:** AuditLogger and ValidationUtils

---

## Key Design Decisions

### Singleton Pattern
Applied to CareHome, DatabaseManager, and AuditLogger to ensure single instances manage global system state.

### MVC Architecture
Clear separation between business logic (model), UI layout (FXML), and event handling (controllers).

### Custom Exceptions
StaffNotAuthorizedException, StaffNotRosteredException, and BedOccupiedException provide specific error contexts.

### Dual Persistence Strategy
- Active data: Java serialization (carehome_data.ser) for fast access
- Archived data: SQLite database (healthcare.db) for regulatory compliance

### Real-time Roster Validation
JavaFX Timeline updates shift status every second to enforce on-duty requirements.

---

## Development Challenges

### JavaFX Threading
Initially used Thread.sleep() which froze UI. Switched to JavaFX Timeline for non-blocking updates.

### Gender-Segregated Rooms
Implemented nested validation to check existing patient genders before bed assignment.

### Database Management
Simplified from connection pooling to direct JDBC connections with proper cleanup.

### Shift Compliance
Used LocalDateTime and LocalTime API for parsing shift strings and time comparison.

---

## Refactoring Performed

### Extracted ShiftScheduler
Moved shift-related operations from CareHome into separate utility class.

### Centralized Validation
Created ValidationUtils class to eliminate duplicated email and phone validation.

### Replaced Magic Numbers
Defined constants for hardcoded values like bed dimensions and spacing.

### Method Extraction
Split long methods into smaller, focused helper methods.

### Custom Exceptions
Replaced generic Exception with specific custom exception types.

---

## Testing

Manual testing performed for all core functionality including patient management, staff authorization, shift scheduling, and edge cases. All functionality passed with proper error handling.

---

## Known Limitations

- No automated unit tests
- No text-based patient search
- No PDF export for reports
- No email notifications

---

## Development Resources

- Oracle JavaFX Documentation
- SQLite JDBC Driver documentation
- Java SE 11 API documentation
- RMIT COSC1295 course materials
- Stack Overflow for specific implementation questions
- AI coding assistants (ChatGPT, Perplexity) for learning unfamiliar technologies

---

## AI Tool Usage

During development, I used AI-powered coding assistants as learning tools to accelerate understanding of JavaFX and SQLite. All AI-generated code was thoroughly reviewed, tested, and modified to fit project requirements. Detailed information is provided in DESIGN_AND_REFACTORING.md.

---

## Academic Integrity Statement

This project represents my own work for COSC1295 Assignment 2. All code has been written, reviewed, tested, and understood by me. External resources have been consulted and properly acknowledged. I can explain and defend every design decision and implementation detail.

This submission complies with RMIT's academic integrity policies.

---

## Version Control Note

This project was developed locally with regular commits between October 15-18, 2025. Due to GitHub authentication issues, the codebase was migrated to this repository on October 18, 2025. Local commit history exists and can be provided upon request.

---

## License

Academic use only - RMIT University COSC1295 Assignment 2, Semester 2, 2025
```


```markdown
# Design Decisions and Refactoring Report

**COSC1295 Advanced Programming - Assignment 2**  
**Student:** Danial Ansari  
**Date:** October 18, 2025

---

## 1. System Architecture

The Healthcare Management System follows Model-View-Controller (MVC) architecture with clear separation between business logic (model package), UI layout (FXML files), and user interaction handling (gui package). This promotes independent testing, easier maintenance, and flexible UI redesign.

Package structure organized as: model, gui, database, exceptions, utils. This promotes high cohesion within packages and low coupling between them.

---

## 2. Design Patterns

### Singleton Pattern

Applied to CareHome, DatabaseManager, and AuditLogger. These classes manage global system state and resources requiring exactly one instance. CareHome serves as central repository preventing data inconsistency. DatabaseManager controls connections preventing conflicts. AuditLogger maintains sequential trail preventing fragmented logging.

Alternative considered: Dependency Injection framework. Rejected due to unnecessary complexity for desktop application scope.

### Factory Method Pattern

Applied to staff creation in DashboardController. Encapsulates logic for instantiating different Staff subclasses (Doctor, Nurse, Manager) based on user selection at runtime. Allows easy addition of new staff types.

### Template Method Pattern

Applied to Staff class hierarchy. Common behavior for all staff (login, permissions) implemented in base class with role-specific specialization through abstract methods in subclasses. Promotes code reuse and consistent interface.

---

## 3. Database Design

Schema includes four tables: discharged_patients, discharged_patient_prescriptions, discharged_patient_medications, and audit_log. Normalized to Third Normal Form to eliminate redundancy while maintaining queryability.

Intentional denormalization: Patient data duplicated in archived table rather than referenced because archived records must be immutable and independent.

### Hybrid Persistence Strategy

Active data uses Java serialization (carehome_data.ser) for fast access and preservation of object relationships. Archived data uses SQLite database (healthcare.db) for external accessibility and SQL queryability. This optimizes for different use cases: active data needs speed, archived data needs compliance access.

---

## 4. Class Hierarchy

Person serves as abstract base class preventing instantiation of generic person objects. Staff extends Person adding credentials, permissions, and shifts. Doctor, Nurse, and Manager extend Staff implementing role-specific canPerformAction methods. Patient extends Person with medical-specific attributes.

Ward structure uses composition: CareHome contains Wards, which contain Rooms, which contain Beds. This models real-world containment and enforces navigation rules preventing orphaned objects.

---

## 5. Refactoring Process

### Extracted ShiftScheduler

Original CareHome class exceeded 2000 lines with mixed concerns. Extracted ShiftScheduler utility class for shift operations improving maintainability and Single Responsibility Principle adherence.

### Centralized Validation

Email and phone validation was duplicated across Patient and Staff classes. Created ValidationUtils utility class with static methods achieving DRY principle and consistent validation.

### Replaced Magic Numbers

Hardcoded values for bed dimensions and spacing scattered throughout code. Defined constants at class level with descriptive names enabling easier configuration and self-documenting code.

### Method Extraction

Long methods in DashboardController split into focused helper methods improving readability and testability.

### Custom Exceptions

Replaced generic Exception with custom classes (StaffNotAuthorizedException, StaffNotRosteredException, BedOccupiedException) enabling specific catch blocks and better error messages.

---

## 6. AI Tool Usage Acknowledgment

### Overview

In accordance with RMIT's academic integrity policies, I acknowledge using AI-powered coding assistants during development. This provides transparent disclosure of how, where, and why AI tools were utilized.

### AI Tools Used

ChatGPT (OpenAI GPT-4) for code generation, debugging, and technical explanations. Perplexity AI for research and documentation references.

### Development Approach

As a student encountering JavaFX and SQLite for the first time, I used AI tools as supplementary learning resources. My approach: attempt implementation based on course materials, consult AI for specific challenges, review and understand all AI-generated code, test and modify to fit requirements, ensure ability to explain every implementation.

### Specific Areas of AI Assistance

#### Database Integration

Challenge: SQLite and JDBC were new technologies not covered in depth during lectures.

AI assisted with: JDBC connection string format, PreparedStatement usage, schema design best practices, transaction management patterns.

My contribution: Designed specific schema for archiving patient records with prescriptions and medications based on assignment requirements. Implemented dischargePatient method logic. Added comprehensive error handling and rollback. Tested extensively for data integrity. Modified suggestions to fit dual-persistence strategy.

#### JavaFX GUI Components

Challenge: First time implementing complex JavaFX interfaces with event handling and real-time updates.

AI assisted with: FXML structure and controller binding, dialog creation with custom result converters, Timeline API for clock updates, cell factory usage for ComboBox.

My contribution: Designed bed visualization layout with color-coding by gender. Implemented two-click patient movement workflow. Created custom cell factories for dropdowns. Developed real-time shift status display with tooltips. Customized all workflows to match assignment specifications.

#### Exception Handling

Challenge: Designing meaningful custom exceptions for business rules.

AI assisted with: Template structure for custom exception classes, best practices for hierarchies, guidelines on checked vs unchecked.

My contribution: Identified specific exception types from assignment requirements. Designed exception hierarchy. Implemented context-specific constructors and error messages. Integrated exceptions into business logic validation.

#### Shift Compliance Logic

Challenge: Implementing roster checking with real-time validation.

AI assisted with: LocalDateTime and DayOfWeek API examples, time comparison techniques, string parsing strategies.

My contribution: Designed shift storage structure in Staff class. Implemented isRosteredNow method with time comparison. Created compliance report generation algorithm. Added validation preventing duplicate assignments. Integrated checking into all nurse actions.

#### Debugging

Challenge: Encountered compilation errors, runtime exceptions, and logic bugs.

AI assisted with: Interpreting error messages, debugging strategies for NullPointerException and ClassCastException, suggestions for JavaFX threading issues.

My contribution: Tested all solutions within existing context. Modified suggestions to align with requirements. Ensured fixes maintained business logic integrity. Added defensive programming checks.

### What I Did NOT Use AI For

System architecture: Overall class structure and entity relationships designed by me based on assignment specifications, OOP principles from lectures, and real-world analysis.

Business logic: All business rules implemented by me including gender-segregated room allocation, shift compliance checking, role-based permissions, and discharge workflow.

Testing: All testing scenarios, edge case identification, and validation performed by me.

Design decisions: All architectural and pattern choices made by me based on course material and requirement analysis.

### Learning Outcomes

Through strategic AI use as learning tool, I achieved accelerated understanding of JavaFX and SQLite, exposure to industry patterns, improved debugging skills, and awareness of code quality opportunities.

Critical point: I can explain, modify, and extend every line of code. AI served as educational resource, not substitute for understanding.

### Why This Approach is Academically Acceptable

Transparency: Full disclosure in this document. Understanding: I can defend all decisions in interview. Customization: All code adapted to specific requirements. Learning focus: AI used to understand concepts not bypass learning. Comparable to consulting textbooks or Stack Overflow.

All assignment learning objectives achieved through hands-on implementation and adaptation.

---

## 7. Design Trade-offs

### Serialization vs Database

Decision: Use serialization for active data, SQLite for archived data. Active data needs fast read/write for UI responsiveness. Archived data prioritizes external accessibility for auditing. Hybrid approach optimizes for both.

### Singleton vs Dependency Injection

Decision: Use Singleton for core classes. For desktop application with single-user access, Singleton's simplicity outweighs dependency injection's testability benefits.

### Custom vs Standard Exceptions

Decision: Implement custom exceptions. Provides meaningful error messages and allows specific handling. Worth the additional code volume.

### Manual vs Automated Testing

Decision: Manual testing due to time constraints. Ensured functionality verification. Recognized as technical debt for future improvement.

---

## Conclusion

This system demonstrates application of object-oriented design principles, design patterns, and modern Java technologies. Architecture balances simplicity with extensibility.

Key achievements: Clean MVC architecture, appropriate design patterns, robust business logic with custom exceptions, dual persistence optimized for different use cases, successful JavaFX and SQLite integration.

Areas for enhancement: Implement automated test suite, refactor to dependency injection, add sophisticated reporting, enhance search and filtering.

AI tools accelerated technology adoption while ensuring deep understanding through hands-on customization and testing.

---

**End of Report**
```
