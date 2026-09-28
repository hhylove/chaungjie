package com.chuangjie.module.oa.controller.admin.vehicle;

import com.chuangjie.framework.common.pojo.CommonResult;
import com.chuangjie.framework.common.pojo.PageResult;
import com.chuangjie.framework.common.util.object.BeanUtils;
import com.chuangjie.module.oa.controller.admin.vehicle.vo.*;
import com.chuangjie.module.oa.dal.dataobject.vehicle.OaVehicleDO;
import com.chuangjie.module.oa.service.vehicle.OaVehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import com.chuangjie.module.system.api.dept.DeptApi;
import com.chuangjie.module.system.api.dept.dto.DeptRespDTO;
import cn.hutool.core.collection.CollUtil;
import com.chuangjie.framework.common.util.collection.MapUtils;
import java.util.*;
import static com.chuangjie.framework.common.util.collection.CollectionUtils.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.chuangjie.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 车辆")
@RestController
@RequestMapping("/oa/vehicle")
@Validated
public class OaVehicleController {

    @Resource
    private DeptApi deptApi;

    @Resource
    private OaVehicleService vehicleService;

    @PostMapping("/create")
    @Operation(summary = "创建车辆")
    @PreAuthorize("@ss.hasPermission('oa:vehicle:create')")
    public CommonResult<Long> createVehicle(@Valid @RequestBody OaVehicleSaveReqVO createReqVO) {
        return success(vehicleService.createVehicle(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新车辆")
    @PreAuthorize("@ss.hasPermission('oa:vehicle:update')")
    public CommonResult<Boolean> updateVehicle(@Valid @RequestBody OaVehicleSaveReqVO updateReqVO) {
        vehicleService.updateVehicle(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除车辆")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('oa:vehicle:delete')")
    public CommonResult<Boolean> deleteVehicle(@RequestParam("id") Long id) {
        vehicleService.deleteVehicle(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得车辆")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('oa:vehicle:query')")
    public CommonResult<OaVehicleRespVO> getVehicle(@RequestParam("id") Long id) {
        OaVehicleDO vehicle = vehicleService.validateVehicleExists(id);
        return success(buildVehicleRespVO(vehicle));
    }

    @GetMapping("/page")
    @Operation(summary = "获得车辆分页")
    @PreAuthorize("@ss.hasPermission('oa:vehicle:query')")
    public CommonResult<PageResult<OaVehicleRespVO>> getVehiclePage(@Valid OaVehiclePageReqVO pageReqVO) {
        PageResult<OaVehicleDO> pageResult = vehicleService.getVehiclePage(pageReqVO);
        return success(new PageResult<>(buildVehicleRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    // ==================== 拼接 VO ====================

    /**
     * 拼接车辆详情
     *
     * @param vehicle 车辆
     * @return 车辆响应
     */
    private OaVehicleRespVO buildVehicleRespVO(OaVehicleDO vehicle) {
        if (vehicle == null) {
            return null;
        }
        return CollUtil.getFirst(buildVehicleRespVOList(Collections.singletonList(vehicle)));
    }

    /**
     * 构建车辆响应列表，批量拼接所属部门名称
     *
     * @param vehicles 车辆列表
     * @return 车辆响应列表
     */
    private List<OaVehicleRespVO> buildVehicleRespVOList(List<OaVehicleDO> vehicles) {
        if (CollUtil.isEmpty(vehicles)) {
            return Collections.emptyList();
        }
        // 1. 批量查询所属部门
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(vehicles, OaVehicleDO::getDeptId));
        // 2. 转换并拼接部门名称
        return convertList(vehicles, vehicle -> {
            OaVehicleRespVO respVO = BeanUtils.toBean(vehicle, OaVehicleRespVO.class);
            MapUtils.findAndThen(deptMap, vehicle.getDeptId(), dept -> respVO.setDeptName(dept.getName()));
            return respVO;
        });
    }

}
