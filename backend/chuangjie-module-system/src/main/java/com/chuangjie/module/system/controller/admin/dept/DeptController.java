package com.chuangjie.module.system.controller.admin.dept;

import com.chuangjie.framework.common.enums.CommonStatusEnum;
import com.chuangjie.framework.common.pojo.CommonResult;
import com.chuangjie.framework.common.util.object.BeanUtils;
import com.chuangjie.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import com.chuangjie.module.system.controller.admin.dept.vo.dept.DeptRespVO;
import com.chuangjie.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import com.chuangjie.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import com.chuangjie.module.system.dal.dataobject.dept.DeptDO;
import com.chuangjie.module.system.dal.dataobject.user.AdminUserDO;
import com.chuangjie.module.system.service.dept.DeptService;
import com.chuangjie.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.chuangjie.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 部门")
@RestController
@RequestMapping("/system/dept")
@Validated
public class DeptController {

    @Resource
    private DeptService deptService;

    @Resource
    private AdminUserService adminUserService;

    @PostMapping("create")
    @Operation(summary = "创建部门")
    @PreAuthorize("@ss.hasPermission('system:dept:create')")
    public CommonResult<Long> createDept(@Valid @RequestBody DeptSaveReqVO createReqVO) {
        Long deptId = deptService.createDept(createReqVO);
        return success(deptId);
    }

    @PutMapping("update")
    @Operation(summary = "更新部门")
    @PreAuthorize("@ss.hasPermission('system:dept:update')")
    public CommonResult<Boolean> updateDept(@Valid @RequestBody DeptSaveReqVO updateReqVO) {
        deptService.updateDept(updateReqVO);
        return success(true);
    }

    @DeleteMapping("delete")
    @Operation(summary = "删除部门")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> deleteDept(@RequestParam("id") Long id) {
        deptService.deleteDept(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除部门")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> deleteDeptList(@RequestParam("ids") List<Long> ids) {
        deptService.deleteDeptList(ids);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获取部门列表")
    @PreAuthorize("@ss.hasPermission('system:dept:query')")
    public CommonResult<List<DeptRespVO>> getDeptList(DeptListReqVO reqVO) {
        List<DeptDO> list = deptService.getDeptList(reqVO);
        List<DeptRespVO> result = BeanUtils.toBean(list, DeptRespVO.class);
        Map<Long, AdminUserDO> leaders = adminUserService.getUserMap(list.stream()
                .map(DeptDO::getLeaderUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        result.forEach(dept -> {
            AdminUserDO leader = leaders.get(dept.getLeaderUserId());
            if (leader != null) dept.setLeaderName(leader.getNickname());
        });
        return success(result);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取部门精简信息列表", description = "只包含被开启的部门，主要用于前端的下拉选项")
    public CommonResult<List<DeptSimpleRespVO>> getSimpleDeptList() {
        List<DeptDO> list = deptService.getDeptList(
                new DeptListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus()));
        return success(BeanUtils.toBean(list, DeptSimpleRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得部门信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dept:query')")
    public CommonResult<DeptRespVO> getDept(@RequestParam("id") Long id) {
        DeptDO dept = deptService.getDept(id);
        DeptRespVO result = BeanUtils.toBean(dept, DeptRespVO.class);
        if (result != null && result.getLeaderUserId() != null) {
            AdminUserDO leader = adminUserService.getUser(result.getLeaderUserId());
            if (leader != null) result.setLeaderName(leader.getNickname());
        }
        return success(result);
    }

}
