# Software Testing Course Project

[ English | [中文](./README.zh-CN.md) ]

## Project Introduction

This repository is a fork of the project: [OnlineExaminationSystem](https://github.com/liulang123xx/OnlineExaminationSystem).

The original project is a JavaEE-based online examination system. It provides common online exam features such as user management, question bank management, exam paper management, online examination, score management, proctoring, and anti-cheating functions.

This fork is used as the testing subject for the **Software Testing & Quality Assurance** course. The main purpose of this repository is to record testing design, testing execution, testing results, and quality analysis.

## Original System Overview

The original system mainly includes the following modules:

- User authentication and role management
- Question bank management
- Exam paper management
- Online examination
- Score management and score query
- Online proctoring and anti-cheating
- Statistical analysis and data export

This repository focuses on software testing and quality assurance work rather than new feature development.

## Testing Scope

The testing work in this repository mainly covers:

- Score management and score query testing
- Proctoring system and anti-cheating testing
- Unit testing for score, marking, proctoring, LiveKit, and anti-cheating services
- API testing for score and proctoring-related interfaces
- Security testing
- Final testing conclusion and quality evaluation

## Testing Documentation

### 1. Score Management and Score Query Testing

#### Module Overview

> TODO: Briefly describe the functions of the score management and score query module, including automatic grading, manual marking, score query, score statistics, wrong-answer analysis, cheating mark management, and score export.

#### Testing Objectives

> TODO: Describe the testing goals of this module, such as verifying grading correctness, score query accuracy, data consistency, permission control, export reliability, and exception handling.

#### Detailed Testing TODO List

- [ ] Objective question auto-grading test  
  TODO: Add test cases and results for automatic grading of objective questions, including single-choice, multiple-choice, true/false questions, and scoring rules.

- [ ] Subjective question manual marking test  
  TODO: Add test cases and results for teacher manual marking of subjective questions.

- [ ] Paper-based marking test  
  TODO: Verify the marking process based on exam papers or students.

- [ ] Question-based marking test  
  TODO: Verify the marking process based on individual questions.

- [ ] Marking progress query test  
  TODO: Verify whether teachers can correctly view marking progress.

- [ ] Score list query test  
  TODO: Verify score list filtering, pagination, sorting, and data accuracy.

- [ ] Score detail view test  
  TODO: Verify whether detailed score information is displayed correctly.

- [ ] Student score query test  
  TODO: Verify whether students can query their own scores correctly and cannot access unauthorized score data.

- [ ] Class score statistics test  
  TODO: Verify class-level statistics, such as average score, highest score, lowest score, and score distribution.

- [ ] Wrong-answer analysis test  
  TODO: Verify wrong-answer analysis results and AI-generated score analysis if applicable.

- [ ] Abnormal paper review test  
  TODO: Verify whether abnormal exam papers can be viewed and reviewed correctly.

- [ ] Cheating mark management test  
  TODO: Verify cheating mark creation, query, update, and management logic.

- [ ] Score export test  
  TODO: Verify whether score data can be exported correctly, including file format, field completeness, and data consistency.

- [ ] Test case design  
  TODO: Add test case tables, including test case ID, test item, preconditions, test steps, expected result, actual result, and status.

- [ ] Test execution results  
  TODO: Add execution records, logs, passed cases, failed cases, defect descriptions, and retest results.

- [ ] Test conclusion  
  TODO: Summarize whether the score management and score query module meets the expected functional and quality requirements.

### 2. Proctoring System and Anti-Cheating Testing

#### Module Overview

> TODO: Briefly describe the proctoring and anti-cheating module, including LiveKit room creation, token generation, real-time audio/video transmission, proctoring dashboard, multi-stream video monitoring, abnormal behavior recording, forced submission, dual-camera proctoring, screen switching detection, face recognition, multiple-person detection, and copy-paste prevention.

#### Testing Objectives

> TODO: Describe the testing goals of this module, such as verifying real-time communication stability, proctoring data accuracy, anti-cheating effectiveness, abnormal behavior recording, and exception handling.

#### Detailed Testing TODO List

- [ ] LiveKit room creation test  
  TODO: Verify whether LiveKit rooms can be created correctly for exams.

- [ ] LiveKit token generation and verification test  
  TODO: Verify token generation, token validity, permissions, and expiration behavior.

- [ ] Real-time audio and video transmission test  
  TODO: Verify audio/video connection, latency, stability, and reconnection behavior.

- [ ] Proctoring dashboard test  
  TODO: Verify whether the teacher-side proctoring dashboard displays exam status and video streams correctly.

- [ ] Multi-stream video monitoring test  
  TODO: Verify whether multiple students' video streams can be displayed and monitored simultaneously.

- [ ] Student status view test  
  TODO: Verify whether student exam status, connection status, and abnormal status are shown correctly.

- [ ] Abnormal behavior marking test  
  TODO: Verify abnormal behavior marking and recording logic.

- [ ] Cheating record test  
  TODO: Verify cheating record creation, query, and display.

- [ ] Forced submission test  
  TODO: Verify whether teachers can force students to submit papers under abnormal conditions.

- [ ] Dual-camera proctoring test  
  TODO: Verify front camera and mobile/secondary camera monitoring.

- [ ] Mobile QR code access test  
  TODO: Verify whether students can access mobile proctoring through QR code scanning.

- [ ] Screen switching detection test  
  TODO: Verify whether screen switching or tab switching can be detected and recorded.

- [ ] Page leaving record test  
  TODO: Verify whether leaving the exam page is correctly recorded.

- [ ] Face recognition verification test  
  TODO: Verify face recognition and identity verification behavior.

- [ ] Multiple-person detection test  
  TODO: Verify whether the system can detect multiple persons appearing in the camera.

- [ ] Copy-paste prevention test  
  TODO: Verify whether copy, paste, and related shortcut operations are blocked or recorded.

- [ ] Test case design  
  TODO: Add test case tables, including test case ID, test item, preconditions, test steps, expected result, actual result, and status.

- [ ] Test execution results  
  TODO: Add execution records, logs, passed cases, failed cases, defect descriptions, and retest results.

- [ ] Test conclusion  
  TODO: Summarize the reliability, stability, and effectiveness of the proctoring and anti-cheating module.

### 3. Unit Testing

The following unit tests are planned for score, marking, proctoring, LiveKit, and anti-cheating related services.

#### Unit Testing TODO List

- [ ] ScoreServiceTest  
  TODO: Add unit tests for score calculation, score query, score statistics, and score export logic.

- [ ] MarkingServiceTest  
  TODO: Add unit tests for automatic grading, manual marking, marking progress, and marking status updates.

- [ ] ProctorServiceTest  
  TODO: Add unit tests for proctoring status management, abnormal behavior recording, and forced submission logic.

- [ ] LiveKitServiceTest  
  TODO: Add unit tests for LiveKit room creation, token generation, token validation, and related service methods.

- [ ] AntiCheatingServiceTest  
  TODO: Add unit tests for screen switching detection, page leaving records, copy-paste prevention, face recognition records, and cheating mark management.

### 4. API Testing

#### API Testing TODO List

- [ ] Score query API test  
  TODO: Add request URL, request method, request parameters, response data, expected result, actual result, and status.

- [ ] Marking API test  
  TODO: Test APIs related to automatic grading, manual marking, marking submission, and marking progress query.

- [ ] LiveKit token API test  
  TODO: Test LiveKit token generation interface, token permission, and invalid token behavior.

- [ ] Abnormal behavior record API test  
  TODO: Test APIs for recording screen switching, page leaving, cheating marks, and other abnormal behaviors.

- [ ] Forced submission API test  
  TODO: Test the forced submission interface and verify whether exam status and score records are updated correctly.

### 5. Security Testing

#### Security Testing TODO List

- [ ] SQL injection test  
  TODO: Test whether score query, login, marking, and search interfaces are protected against SQL injection.

- [ ] XSS attack test  
  TODO: Test whether input fields, answers, rich text content, and displayed user-generated content are protected against stored and reflected XSS attacks.

- [ ] CSRF protection test  
  TODO: Test whether important operations such as marking, forced submission, and cheating mark update are protected against CSRF attacks.

- [ ] Sensitive information protection test  
  TODO: Check whether sensitive information such as passwords, tokens, face images, student identity data, and exam records are properly protected.

### 6. Final Testing Conclusion

> TODO: Summarize the overall testing process, test coverage, major defects found, fixed issues, remaining risks, and final quality evaluation of the tested modules.

Suggested content:

- Overall testing scope
- Number of designed and executed test cases
- Number of passed and failed test cases
- Major defects found during testing
- Defect fixes and retest results
- Remaining risks
- Final evaluation of system quality

## Academic and Attribution Notice

This repository is a fork of the original project and is used only for course learning, software testing experiments, and quality analysis.

Original project: [OnlineExaminationSystem](https://github.com/liulang123xx/OnlineExaminationSystem)

The original system implementation belongs to the original author. This repository mainly records my testing work, testing documentation, and quality assurance analysis.
