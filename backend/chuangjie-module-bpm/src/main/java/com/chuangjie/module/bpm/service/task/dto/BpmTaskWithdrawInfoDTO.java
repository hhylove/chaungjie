package com.chuangjie.module.bpm.service.task.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * BPM 任务撤回资格 DTO。
 */
@Data
@AllArgsConstructor(staticName = "of")
public class BpmTaskWithdrawInfoDTO {

    /** 是否允许撤回 */
    private Boolean withdrawable;

    /** 不允许撤回的原因；允许撤回时为空 */
    private String reason;

    public static BpmTaskWithdrawInfoDTO allowed() {
        return of(true, null);
    }

    public static BpmTaskWithdrawInfoDTO denied(String reason) {
        return of(false, reason);
    }

}
