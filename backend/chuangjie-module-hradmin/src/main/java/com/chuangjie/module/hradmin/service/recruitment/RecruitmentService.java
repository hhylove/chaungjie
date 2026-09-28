package com.chuangjie.module.hradmin.service.recruitment;

import com.chuangjie.module.hradmin.controller.admin.employee.vo.EmployeeSaveReqVO;
import com.chuangjie.module.hradmin.dal.dataobject.recruitment.CandidateDO;
import com.chuangjie.module.hradmin.dal.dataobject.recruitment.CandidateHistoryDO;
import com.chuangjie.module.hradmin.dal.dataobject.recruitment.HiringRequestDO;
import com.chuangjie.module.hradmin.dal.mysql.recruitment.CandidateHistoryMapper;
import com.chuangjie.module.hradmin.dal.mysql.recruitment.CandidateMapper;
import com.chuangjie.module.hradmin.dal.mysql.recruitment.HiringRequestMapper;
import com.chuangjie.module.hradmin.service.employee.EmployeeArchiveService;
import com.chuangjie.framework.security.core.service.SecurityFrameworkService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.chuangjie.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.chuangjie.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.chuangjie.framework.tenant.core.context.TenantContextHolder.getTenantId;
import static com.chuangjie.module.hradmin.enums.ErrorCodeConstants.*;

@Service
public class RecruitmentService {
    public static final List<String> STAGES = List.of("候选人", "面试", "评价", "录用审批", "待入职", "已转员工");
    @Resource private HiringRequestMapper requests;
    @Resource private CandidateMapper candidates;
    @Resource private CandidateHistoryMapper history;
    @Resource private EmployeeArchiveService employees;
    @Resource private SecurityFrameworkService security;

    public List<HiringRequestDO> requests() { return requests.selectList(); }
    public List<CandidateDO> candidates() { return candidates.selectList(); }
    public List<CandidateHistoryDO> history(Long candidateId) {
        return history.selectList(CandidateHistoryDO::getCandidateId, candidateId);
    }

    public Long submit(String job, Long deptId, Integer count, String reason) {
        if (job == null || job.isBlank() || reason == null || reason.isBlank() || deptId == null || count == null || count < 1) {
            throw exception(RECRUITMENT_STATE_INVALID);
        }
        HiringRequestDO row = new HiringRequestDO();
        row.setJob(job.trim()); row.setDeptId(deptId); row.setCount(count);
        row.setReason(reason.trim()); row.setStatus("待审批");
        requests.insert(row);
        return row.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(Long id, boolean approved, String note) {
        if (note == null || note.isBlank() || requests.reviewIfPending(id, getTenantId(),
                approved ? "招聘中" : "已驳回", note.trim()) != 1) throw exception(RECRUITMENT_STATE_INVALID);
    }

    public Long addCandidate(Long requestId, String name) {
        HiringRequestDO request = requests.selectById(requestId);
        if (request == null || !"招聘中".equals(request.getStatus()) || name == null || name.isBlank()) throw exception(RECRUITMENT_STATE_INVALID);
        CandidateDO row = new CandidateDO();
        row.setRequestId(requestId); row.setName(name.trim()); row.setStage(STAGES.get(0));
        candidates.insert(row);
        return row.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void advance(Long id, String expectedStage, String note) {
        CandidateDO row = candidates.selectById(id);
        int stage = row == null ? -1 : STAGES.indexOf(row.getStage());
        if (stage < 0 || stage >= 4 || !Objects.equals(expectedStage, row.getStage()) || note == null || note.isBlank()) {
            throw exception(RECRUITMENT_STATE_INVALID);
        }
        String permission = stage == 0 ? "hradmin:recruitment:candidate"
                : stage == 3 ? "hradmin:recruitment:approve" : "hradmin:recruitment:manager";
        if (!security.hasPermission(permission)) throw exception(RECRUITMENT_STATE_INVALID);
        CandidateHistoryDO event = new CandidateHistoryDO();
        event.setCandidateId(id); event.setStage(row.getStage()); event.setNote(note.trim()); event.setActorUserId(getLoginUserId());
        history.insert(event);
        if (candidates.advanceIfCurrent(id, getTenantId(), expectedStage, STAGES.get(stage + 1), note.trim()) != 1)
            throw exception(RECRUITMENT_STATE_INVALID);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long convert(Long id, EmployeeSaveReqVO archive) {
        CandidateDO candidate = candidates.selectById(id);
        if (candidate == null || !"待入职".equals(candidate.getStage()) || candidate.getEmployeeId() != null) throw exception(RECRUITMENT_STATE_INVALID);
        HiringRequestDO request = requests.lockById(candidate.getRequestId(), getTenantId());
        if (request == null || !"招聘中".equals(request.getStatus())) throw exception(RECRUITMENT_STATE_INVALID);
        long filled = candidates.selectList(CandidateDO::getRequestId, request.getId()).stream()
                .filter(row -> row.getEmployeeId() != null).count();
        if (filled >= request.getCount()) throw exception(RECRUITMENT_QUOTA_FULL);
        archive.setName(candidate.getName());
        archive.setDeptId(request.getDeptId());
        archive.setPositionName(request.getJob());
        archive.setUserId(null);
        archive.setEmploymentStatus(0);
        Long employeeId = employees.create(archive);
        if (candidates.markConverted(id, employeeId, getTenantId()) != 1) throw exception(RECRUITMENT_STATE_INVALID);
        return employeeId;
    }
}
