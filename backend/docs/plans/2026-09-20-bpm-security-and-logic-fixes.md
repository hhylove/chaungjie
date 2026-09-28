# BPM Security and Logic Fixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use executing-plans to implement this plan task-by-task.

**Goal:** Close the confirmed BPM authorization, variable-tampering, timeout, pagination, tenant callback, and form-field validation defects without changing unrelated modules.

**Architecture:** Add one BPM process-access service as the single object-authorization boundary, and invoke it from every read/comment endpoint. Keep system automation distinct from user task operations, filter submitted variables against server-side field permissions and reserved names, then fix isolated Flowable query/listener defects.

**Tech Stack:** Java 17, Spring Boot, Flowable 8, MyBatis-Plus, JUnit 5, Mockito.

---

### Task 1: Process and task authorization boundary

**Files:**
- Create: `chuangjie-module-bpm/src/main/java/com/chuangjie/module/bpm/service/task/BpmProcessInstanceAccessService.java`
- Modify: BPM process/task/comment controllers and services
- Test: `chuangjie-module-bpm/src/test/java/com/chuangjie/module/bpm/service/task/BpmProcessInstanceAccessServiceTest.java`

1. Add tenant-aware participant checks for starter, assignee, owner, copied user, and manager permissions.
2. Apply the check to process details, task history, comments, model view, printing, and child-task endpoints.
3. Make user task validation reject unassigned tasks while preserving explicit system automation with `userId == null`.
4. Require parent-task ownership for sign deletion and current task ownership for copying.

### Task 2: Protect process variables

**Files:**
- Modify: `BpmTaskServiceImpl.java`, `FlowableUtils.java`
- Test: `FlowableUtilsTest.java`, task-service focused unit tests where practical

1. Define all BPM reserved variable names and prefixes.
2. Remove reserved variables from create/update payloads and API form responses.
3. On approval, accept only fields marked writable for the current task; keep legacy definitions compatible by still blocking reserved variables when field metadata is absent.

### Task 3: Correct deterministic logic defects

**Files:**
- Modify: `BpmTaskEventListener.java`, `BpmTaskServiceImpl.java`, `BpmHttpRequestUtils.java`, `BpmFormServiceImpl.java`
- Test: relevant focused unit tests

1. Use the parsed timer element id.
2. Push date and tenant filters into Flowable queries and stop filtering after pagination.
3. Propagate the process tenant in callbacks and avoid null request-variable dereferences.
4. Restore recursive duplicate form-field validation for both current and legacy form schemas.

### Task 4: Verification

1. Run BPM module tests with the project Java 17/Maven toolchain when available.
2. At minimum compile the BPM module and run focused tests.
3. Re-scan affected endpoints for missing object checks and review the final diff.

