# BPM Transfer and Withdraw Optimization Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use executing-plans to implement this plan task-by-task.

**Goal:** Improve transfer and withdraw usability while preventing unsafe withdraw operations from corrupting complex workflow state.

**Architecture:** The backend remains the authority for withdraw eligibility and revalidates it inside the write transaction. The done-task response carries an eligibility flag and reason for UI rendering. Only a single direct serial next user task is supported; add-sign, multi-instance, branch and parallel paths fail closed.

**Tech Stack:** Java 17, Spring Boot, Flowable 8, Vue 3, TypeScript, Element Plus, JUnit 5, Mockito.

---

### Task 1: Centralize withdraw eligibility

**Files:**
- Create: `chuangjie-module-bpm/src/main/java/com/chuangjie/module/bpm/service/task/dto/BpmTaskWithdrawInfoDTO.java`
- Modify: `chuangjie-module-bpm/src/main/java/com/chuangjie/module/bpm/service/task/BpmTaskService.java`
- Modify: `chuangjie-module-bpm/src/main/java/com/chuangjie/module/bpm/service/task/BpmTaskServiceImpl.java`
- Modify: `chuangjie-module-bpm/src/main/java/com/chuangjie/module/bpm/enums/ErrorCodeConstants.java`

1. Add a read model containing `withdrawable` and `reason`.
2. Validate tenant, ownership, approved status, running process, model setting, direct serial topology, no multi-instance/add-sign, one active next task and no completed next task.
3. Reuse the same validation in `withdrawTask` so UI state cannot bypass backend authorization.
4. Add a batch eligibility method for a done-task page.

### Task 2: Return eligibility in done-task responses

**Files:**
- Modify: `chuangjie-module-bpm/src/main/java/com/chuangjie/module/bpm/controller/admin/task/vo/task/BpmTaskRespVO.java`
- Modify: `chuangjie-module-bpm/src/main/java/com/chuangjie/module/bpm/controller/admin/task/BpmTaskController.java`

1. Add documented `withdrawable` and `withdrawReason` response fields.
2. Enrich only the current user's done-task page.
3. Keep manager/history responses unchanged.

### Task 3: Improve transfer validation and confirmation

**Files:**
- Modify: `chuangjie-module-bpm/src/main/java/com/chuangjie/module/bpm/controller/admin/task/vo/task/BpmTaskTransferReqVO.java`
- Modify: `chuangjie-module-bpm/src/main/java/com/chuangjie/module/bpm/service/task/BpmTaskServiceImpl.java`
- Modify: `chuangjie-frontend/src/views/bpm/processInstance/detail/ProcessInstanceOperationButton.vue`

1. Limit transfer reason length and reject disabled target users.
2. Exclude the current handler from target choices.
3. Confirm the selected target and explain ownership change before submission.

### Task 4: Improve done-task UI

**Files:**
- Modify: `chuangjie-frontend/src/views/bpm/task/done/index.vue`

1. Show an enabled withdraw action only when the backend says it is safe.
2. Show a disabled action with the backend reason otherwise.
3. Add confirmation and per-row loading protection against duplicate submission.

### Task 5: Verification

1. Add focused backend unit tests for eligibility rules where practical.
2. Compile and run BPM focused tests.
3. Run frontend TypeScript checks.
4. Do not create branches or commits.
