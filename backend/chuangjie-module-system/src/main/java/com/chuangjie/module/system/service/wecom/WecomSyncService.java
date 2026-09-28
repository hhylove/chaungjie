package com.chuangjie.module.system.service.wecom;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chuangjie.framework.common.enums.CommonStatusEnum;
import com.chuangjie.framework.common.enums.UserTypeEnum;
import com.chuangjie.framework.common.util.json.JsonUtils;
import com.chuangjie.module.system.dal.dataobject.social.SocialClientDO;
import com.chuangjie.module.system.dal.dataobject.dept.DeptDO;
import com.chuangjie.module.system.dal.dataobject.user.AdminUserDO;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomDeptDO;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomSyncRunDO;
import com.chuangjie.module.system.dal.dataobject.wecom.WecomUserDO;
import com.chuangjie.module.system.dal.mysql.social.SocialClientMapper;
import com.chuangjie.module.system.dal.mysql.user.AdminUserMapper;
import com.chuangjie.module.system.dal.mysql.wecom.WecomDeptMapper;
import com.chuangjie.module.system.dal.mysql.wecom.WecomSyncRunMapper;
import com.chuangjie.module.system.dal.mysql.wecom.WecomUserMapper;
import com.chuangjie.module.system.enums.social.SocialTypeEnum;
import com.chuangjie.module.system.service.permission.PermissionService;
import com.chuangjie.module.system.service.permission.RoleService;
import com.chuangjie.module.system.service.dept.DeptService;
import com.chuangjie.module.system.service.user.AdminUserService;
import com.chuangjie.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import com.chuangjie.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import com.chuangjie.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import com.chuangjie.framework.security.core.util.SecurityFrameworkUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.chuangjie.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.chuangjie.module.system.enums.ErrorCodeConstants.WECOM_SYNC_CONFIG_MISSING;
import static com.chuangjie.module.system.enums.ErrorCodeConstants.WECOM_SYNC_REMOTE_ERROR;
import static com.chuangjie.module.system.enums.ErrorCodeConstants.WECOM_SYNC_LINK_INVALID;

/**
 * 企业微信通讯录同步及管理员审核后的系统组织、账号开通。
 */
@Service
public class WecomSyncService {

    private static final String API = "https://qyapi.weixin.qq.com/cgi-bin/";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Resource private SocialClientMapper socialClientMapper;
    @Resource private WecomDeptMapper wecomDeptMapper;
    @Resource private WecomUserMapper wecomUserMapper;
    @Resource private AdminUserMapper adminUserMapper;
    @Resource private PermissionService permissionService;
    @Resource private RoleService roleService;
    @Resource private WecomSyncRunMapper wecomSyncRunMapper;
    @Resource private ObjectMapper objectMapper;
    @Resource private DeptService deptService;
    @Resource private AdminUserService adminUserService;
    private static final SecureRandom PASSWORD_RANDOM = new SecureRandom();

    /** 使用当前租户已启用的企微管理端应用配置，同步其可见范围内的部门和成员。 */
    @Transactional(rollbackFor = Exception.class)
    public WecomSyncRunDO sync() {
        SocialClientDO client = socialClientMapper.selectBySocialTypeAndUserType(
                SocialTypeEnum.WECHAT_ENTERPRISE.getType(), UserTypeEnum.ADMIN.getValue());
        if (client == null || !Objects.equals(client.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
            throw exception(WECOM_SYNC_CONFIG_MISSING);
        }
        JsonNode tokenResponse = get("gettoken?corpid=" + encode(client.getClientId())
                + "&corpsecret=" + encode(client.getClientSecret()));
        String token = tokenResponse.path("access_token").asText();
        if (token.isBlank()) {
            throw exception(WECOM_SYNC_REMOTE_ERROR, "企微未返回访问令牌");
        }

        JsonNode departments = get("department/list?access_token=" + encode(token)).path("department");
        if (!departments.isArray() || departments.isEmpty()) {
            throw exception(WECOM_SYNC_REMOTE_ERROR, "应用可见范围内没有部门");
        }
        // 同一员工可能在多个部门，按企微 userid 去重，并保存完整部门归属。
        Map<String, Member> members = new LinkedHashMap<>();
        for (JsonNode department : departments) {
            long deptId = department.path("id").asLong();
            if (deptId <= 0) {
                throw exception(WECOM_SYNC_REMOTE_ERROR, "部门 ID 无效");
            }
            JsonNode userList = get("user/list?department_id=" + deptId
                    + "&fetch_child=0&access_token=" + encode(token)).path("userlist");
            if (!userList.isArray()) {
                throw exception(WECOM_SYNC_REMOTE_ERROR, "企微未返回成员列表");
            }
            for (JsonNode user : userList) {
                String userId = user.path("userid").asText();
                if (userId.isBlank()) {
                    throw exception(WECOM_SYNC_REMOTE_ERROR, "成员 userid 为空");
                }
                Member member = members.computeIfAbsent(userId,
                        key -> new Member(userId, user.path("name").asText(userId)));
                if (member.mobile == null && user.hasNonNull("mobile") && !user.get("mobile").asText().isBlank()) {
                    member.mobile = user.get("mobile").asText();
                }
                if (member.email == null && user.hasNonNull("email") && !user.get("email").asText().isBlank()) {
                    member.email = user.get("email").asText();
                }
                int gender = user.path("gender").asInt();
                if (member.sex == null && (gender == 1 || gender == 2)) member.sex = gender;
                JsonNode userDepartments = user.path("department");
                if (userDepartments.isArray()) {
                    userDepartments.forEach(id -> member.departmentIds.add(id.asLong()));
                } else {
                    member.departmentIds.add(deptId);
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (JsonNode department : departments) {
            Long wecomId = department.path("id").asLong();
            WecomDeptDO row = wecomDeptMapper.selectByWecomDeptId(wecomId);
            boolean creating = row == null;
            if (creating) row = new WecomDeptDO().setWecomDeptId(wecomId);
            row.setParentWecomDeptId(department.path("parentid").asLong());
            row.setName(department.path("name").asText());
            row.setLeaderUserIds(department.path("department_leader").isArray()
                    ? department.path("department_leader").toString() : "[]");
            row.setSort(department.path("order").asInt());
            row.setLastSeenAt(now);
            if (creating) wecomDeptMapper.insert(row); else wecomDeptMapper.updateById(row);
        }
        for (Member member : members.values()) {
            WecomUserDO row = wecomUserMapper.selectByWecomUserId(member.userId);
            boolean creating = row == null;
            if (creating) row = new WecomUserDO().setWecomUserId(member.userId);
            row.setName(member.name);
            row.setDepartmentIds(JsonUtils.toJsonString(new ArrayList<>(member.departmentIds)));
            row.setMobile(member.mobile);
            row.setEmail(member.email);
            row.setSex(member.sex);
            row.setLastSeenAt(now);
            // 已核对的 systemUserId 不受通讯录更新影响。
            if (creating) {
                wecomUserMapper.insert(row);
            } else {
                // 显式清空本次接口未返回的敏感字段，避免使用过期镜像资料。
                wecomUserMapper.update(null, new LambdaUpdateWrapper<WecomUserDO>()
                        .eq(WecomUserDO::getId, row.getId())
                        .set(WecomUserDO::getName, row.getName())
                        .set(WecomUserDO::getDepartmentIds, row.getDepartmentIds())
                        .set(WecomUserDO::getMobile, member.mobile)
                        .set(WecomUserDO::getEmail, member.email)
                        .set(WecomUserDO::getSex, member.sex)
                        .set(WecomUserDO::getLastSeenAt, now));
            }
        }
        WecomSyncRunDO run = new WecomSyncRunDO().setDepartmentCount(departments.size())
                .setUserCount(members.size());
        wecomSyncRunMapper.insert(run);
        return run;
    }

    /** 管理员核对后关联现有账号；不更改该账号的角色、部门或启用状态。 */
    @Transactional(rollbackFor = Exception.class)
    public void linkUser(String wecomUserId, Long systemUserId) {
        WecomUserDO member = wecomUserMapper.selectByWecomUserId(wecomUserId);
        AdminUserDO account = adminUserMapper.selectById(systemUserId);
        if (member == null || account == null) {
            throw exception(WECOM_SYNC_LINK_INVALID);
        }
        // 只有超级管理员可以将企微身份关联到超级管理员账号，避免越权接管。
        boolean targetIsSuperAdmin = roleService.hasAnySuperAdmin(
                permissionService.getUserRoleIdListByUserId(systemUserId));
        Long actorId = SecurityFrameworkUtils.getLoginUserId();
        boolean actorIsSuperAdmin = actorId != null && roleService.hasAnySuperAdmin(
                permissionService.getUserRoleIdListByUserId(actorId));
        if (targetIsSuperAdmin && !actorIsSuperAdmin) {
            throw exception(WECOM_SYNC_LINK_INVALID);
        }
        WecomUserDO existing = wecomUserMapper.selectBySystemUserId(systemUserId);
        if (existing != null && !Objects.equals(existing.getId(), member.getId())) {
            throw exception(WECOM_SYNC_LINK_INVALID);
        }
        wecomUserMapper.updateById(new WecomUserDO().setId(member.getId()).setSystemUserId(systemUserId));
    }

    /** 管理员确认企微部门与现有系统部门的对应关系；后续应用按该映射更新。 */
    @Transactional(rollbackFor = Exception.class)
    public void linkDepartment(Long wecomDeptId, Long systemDeptId) {
        WecomDeptDO source = wecomDeptMapper.selectByWecomDeptId(wecomDeptId);
        DeptDO target = deptService.getDept(systemDeptId);
        if (source == null || target == null || source.getSystemDeptId() != null) {
            throw exception(WECOM_SYNC_LINK_INVALID);
        }
        WecomDeptDO existing = wecomDeptMapper.selectBySystemDeptId(systemDeptId);
        if (existing != null) throw exception(WECOM_SYNC_LINK_INVALID);
        wecomDeptMapper.updateById(new WecomDeptDO().setId(source.getId()).setSystemDeptId(systemDeptId));
    }

    /** 按企微 ID 映射创建或更新系统组织和员工；未关联员工创建禁用账号，不授予角色。 */
    @Transactional(rollbackFor = Exception.class)
    public ApplyResult apply() {
        WecomSyncRunDO latest = wecomSyncRunMapper.selectLatest();
        if (latest == null) throw exception(WECOM_SYNC_LINK_INVALID);
        List<WecomDeptDO> allDepartments = wecomDeptMapper.selectList();
        LocalDateTime lastSeen = allDepartments.stream().map(WecomDeptDO::getLastSeenAt)
                .max(Comparator.naturalOrder()).orElseThrow(() -> exception(WECOM_SYNC_LINK_INVALID));
        Map<Long, WecomDeptDO> departments = new HashMap<>();
        for (WecomDeptDO row : allDepartments) {
            if (lastSeen.equals(row.getLastSeenAt())) departments.put(row.getWecomDeptId(), row);
        }
        if (departments.size() != latest.getDepartmentCount()) throw exception(WECOM_SYNC_LINK_INVALID);
        Set<Long> existingDeptIds = new HashSet<>();
        for (WecomDeptDO row : departments.values()) {
            if (row.getSystemDeptId() != null) existingDeptIds.add(row.getWecomDeptId());
        }
        int existingDepartments = existingDeptIds.size();
        Set<Long> visiting = new HashSet<>();
        for (WecomDeptDO row : departments.values()) {
            if (row.getSystemDeptId() == null) {
                applyDepartment(row, departments, visiting);
            }
        }
        Map<String, WecomUserDO> currentMembers = new HashMap<>();
        for (WecomUserDO member : wecomUserMapper.selectList()) {
            if (lastSeen.equals(member.getLastSeenAt())) currentMembers.put(member.getWecomUserId(), member);
        }
        int createdUsers = 0;
        int updatedUsers = 0;
        for (WecomUserDO member : currentMembers.values()) {
            Long deptId = null;
            try {
                for (JsonNode id : objectMapper.readTree(member.getDepartmentIds())) {
                    WecomDeptDO department = departments.get(id.asLong());
                    if (department != null) { deptId = department.getSystemDeptId(); break; }
                }
            } catch (IOException e) {
                throw exception(WECOM_SYNC_LINK_INVALID);
            }
            boolean newlyCreated = member.getSystemUserId() == null;
            if (member.getSystemUserId() == null) {
                String username = "wx" + UUID.nameUUIDFromBytes(member.getWecomUserId().getBytes(StandardCharsets.UTF_8))
                        .toString().replace("-", "").substring(0, 28);
                // 绝不依据姓名或手机号猜测绑定，也不接管同名系统账号。
                if (adminUserService.getUserByUsername(username) != null) throw exception(WECOM_SYNC_LINK_INVALID);
                byte[] passwordBytes = new byte[12];
                PASSWORD_RANDOM.nextBytes(passwordBytes);
                UserSaveReqVO account = new UserSaveReqVO();
                account.setUsername(username);
                account.setNickname(member.getName().substring(0, Math.min(30, member.getName().length())));
                account.setDeptId(deptId);
                account.setPassword(Base64.getUrlEncoder().withoutPadding().encodeToString(passwordBytes));
                Long accountId = adminUserService.createUser(account);
                adminUserService.updateUserStatus(accountId, CommonStatusEnum.DISABLE.getStatus());
                wecomUserMapper.updateById(new WecomUserDO().setId(member.getId()).setSystemUserId(accountId));
                member.setSystemUserId(accountId);
                createdUsers++;
            }
            AdminUserDO current = adminUserService.getUser(member.getSystemUserId());
            if (current == null) throw exception(WECOM_SYNC_LINK_INVALID);
            // 超级管理员账号可能承载跨部门权限和找回密码资料，保持人工维护。
            if (roleService.hasAnySuperAdmin(permissionService.getUserRoleIdListByUserId(current.getId()))) continue;
            boolean changed = false;
            if (deptId != null && !Objects.equals(current.getDeptId(), deptId)) {
                deptService.validateDeptList(List.of(deptId));
                adminUserMapper.updateById(new AdminUserDO().setId(current.getId()).setDeptId(deptId));
                changed = true;
            }
            UserProfileUpdateReqVO profile = new UserProfileUpdateReqVO();
            String nickname = member.getName();
            if (nickname != null && !nickname.isBlank() && !nickname.equals(member.getWecomUserId())) {
                nickname = nickname.substring(0, Math.min(30, nickname.length()));
                if (!nickname.equals(current.getNickname())) profile.setNickname(nickname);
            }
            if (member.getMobile() != null && member.getMobile().matches("1[3-9]\\d{9}")
                    && !member.getMobile().equals(current.getMobile())) profile.setMobile(member.getMobile());
            if (member.getEmail() != null && member.getEmail().length() <= 50
                    && member.getEmail().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
                    && !member.getEmail().equals(current.getEmail())) profile.setEmail(member.getEmail());
            if (member.getSex() != null && (member.getSex() == 1 || member.getSex() == 2)
                    && !member.getSex().equals(current.getSex())) profile.setSex(member.getSex());
            if (profile.getNickname() != null || profile.getMobile() != null || profile.getEmail() != null
                    || profile.getSex() != null) {
                adminUserService.updateUserProfile(member.getSystemUserId(), profile);
                changed = true;
            }
            if (changed && !newlyCreated) updatedUsers++;
        }
        int updatedDepartments = 0;
        for (WecomDeptDO row : departments.values()) {
            DeptDO current = deptService.getDept(row.getSystemDeptId());
            if (current == null) throw exception(WECOM_SYNC_LINK_INVALID);
            WecomDeptDO parent = departments.get(row.getParentWecomDeptId());
            Long parentId = parent == null ? DeptDO.PARENT_ID_ROOT : parent.getSystemDeptId();
            Long leaderId = current.getLeaderUserId();
            try {
                if (row.getLeaderUserIds() != null) {
                    for (JsonNode id : objectMapper.readTree(row.getLeaderUserIds())) {
                        WecomUserDO leader = currentMembers.get(id.asText());
                        if (leader != null && leader.getSystemUserId() != null) {
                            leaderId = leader.getSystemUserId();
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                throw exception(WECOM_SYNC_LINK_INVALID);
            }
            if (Objects.equals(current.getName(), row.getName()) && Objects.equals(current.getParentId(), parentId)
                    && Objects.equals(current.getSort(), row.getSort()) && Objects.equals(current.getLeaderUserId(), leaderId)) continue;
            DeptSaveReqVO update = new DeptSaveReqVO();
            update.setId(current.getId());
            update.setName(row.getName());
            update.setParentId(parentId);
            update.setSort(row.getSort());
            update.setStatus(current.getStatus());
            update.setLeaderUserId(leaderId);
            update.setPhone(current.getPhone());
            update.setEmail(current.getEmail());
            deptService.updateDept(update);
            if (existingDeptIds.contains(row.getWecomDeptId())) updatedDepartments++;
        }
        return new ApplyResult(departments.size() - existingDepartments, updatedDepartments, createdUsers, updatedUsers);
    }

    private Long applyDepartment(WecomDeptDO row, Map<Long, WecomDeptDO> departments, Set<Long> visiting) {
        if (row.getSystemDeptId() != null) return row.getSystemDeptId();
        if (!visiting.add(row.getWecomDeptId())) throw exception(WECOM_SYNC_LINK_INVALID);
        WecomDeptDO parent = departments.get(row.getParentWecomDeptId());
        // 应用可见范围可能不含上级部门；此时部门先挂在系统根节点。
        Long parentId = parent == null ? DeptDO.PARENT_ID_ROOT : applyDepartment(parent, departments, visiting);
        DeptSaveReqVO department = new DeptSaveReqVO();
        department.setName(row.getName());
        department.setParentId(parentId);
        department.setSort(row.getSort());
        department.setStatus(CommonStatusEnum.ENABLE.getStatus());
        Long id = deptService.createDept(department);
        wecomDeptMapper.updateById(new WecomDeptDO().setId(row.getId()).setSystemDeptId(id));
        row.setSystemDeptId(id);
        visiting.remove(row.getWecomDeptId());
        return id;
    }

    public record ApplyResult(int createdDepartments, int updatedDepartments, int createdUsers, int updatedUsers) {}
    public List<WecomDeptDO> getDepartments() { return wecomDeptMapper.selectList(); }
    public List<WecomUserDO> getUsers() { return wecomUserMapper.selectList(); }
    public WecomSyncRunDO getLatestRun() { return wecomSyncRunMapper.selectLatest(); }

    private JsonNode get(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(API + path))
                    .timeout(Duration.ofSeconds(20)).GET().build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = objectMapper.readTree(response.body());
            if (response.statusCode() != 200 || body.path("errcode").asInt(-1) != 0) {
                throw exception(WECOM_SYNC_REMOTE_ERROR, body.path("errcode").asText("HTTP " + response.statusCode()));
            }
            return body;
        } catch (IOException e) {
            throw exception(WECOM_SYNC_REMOTE_ERROR, "网络请求失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw exception(WECOM_SYNC_REMOTE_ERROR, "请求被中断");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static class Member {
        private final String userId;
        private final String name;
        private final Set<Long> departmentIds = new LinkedHashSet<>();
        private String mobile;
        private String email;
        private Integer sex;
        private Member(String userId, String name) { this.userId = userId; this.name = name; }
    }
}


