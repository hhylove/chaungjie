package com.chuangjie.module.hradmin.controller.admin.recruitment;

import com.chuangjie.framework.common.pojo.CommonResult;
import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeeSaveReqVO;
import com.chuangjie.module.hradmin.dal.dataobject.recruitment.CandidateDO;
import com.chuangjie.module.hradmin.dal.dataobject.recruitment.CandidateHistoryDO;
import com.chuangjie.module.hradmin.dal.dataobject.recruitment.HiringRequestDO;
import com.chuangjie.module.hradmin.service.recruitment.RecruitmentService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.chuangjie.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/hradmin/recruitment")
public class RecruitmentController {
    @Resource private RecruitmentService service;

    public record RequestInput(@NotBlank String job, @NotNull Long deptId, @NotNull @Min(1) Integer count, @NotBlank String reason) {}
    public record ReviewInput(@NotNull Long id, @NotNull Boolean approved, @NotBlank String note) {}
    public record CandidateInput(@NotNull Long requestId, @NotBlank String name) {}
    public record AdvanceInput(@NotNull Long id, @NotBlank String expectedStage, @NotBlank String note) {}
    public record ConvertInput(@NotNull Long id, @Valid @NotNull EmployeeSaveReqVO archive) {}

    @GetMapping("/requests")
    @PreAuthorize("@ss.hasPermission('hradmin:recruitment:query')")
    public CommonResult<List<HiringRequestDO>> requests() { return success(service.requests()); }

    @GetMapping("/candidates")
    @PreAuthorize("@ss.hasPermission('hradmin:recruitment:query')")
    public CommonResult<List<CandidateDO>> candidates() { return success(service.candidates()); }

    @GetMapping("/history")
    @PreAuthorize("@ss.hasPermission('hradmin:recruitment:query')")
    public CommonResult<List<CandidateHistoryDO>> history(@RequestParam @NotNull Long candidateId) {
        return success(service.history(candidateId));
    }

    @PostMapping("/submit")
    @PreAuthorize("@ss.hasPermission('hradmin:recruitment:submit')")
    public CommonResult<Long> submit(@Valid @RequestBody RequestInput input) {
        return success(service.submit(input.job(), input.deptId(), input.count(), input.reason()));
    }

    @PostMapping("/review")
    @PreAuthorize("@ss.hasPermission('hradmin:recruitment:review')")
    public CommonResult<Boolean> review(@Valid @RequestBody ReviewInput input) {
        service.review(input.id(), input.approved(), input.note()); return success(true);
    }

    @PostMapping("/candidate")
    @PreAuthorize("@ss.hasPermission('hradmin:recruitment:candidate')")
    public CommonResult<Long> candidate(@Valid @RequestBody CandidateInput input) {
        return success(service.addCandidate(input.requestId(), input.name()));
    }

    @PostMapping("/advance")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> advance(@Valid @RequestBody AdvanceInput input) {
        service.advance(input.id(), input.expectedStage(), input.note()); return success(true);
    }

    @PostMapping("/convert")
    @PreAuthorize("@ss.hasPermission('hradmin:recruitment:convert') and @ss.hasPermission('hradmin:employee:create') and @ss.hasPermission('hradmin:salary:write')")
    public CommonResult<Long> convert(@Valid @RequestBody ConvertInput input) {
        return success(service.convert(input.id(), input.archive()));
    }
}
