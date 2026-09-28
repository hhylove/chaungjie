package com.chuangjie.module.oa.controller.admin.discussion;

import com.chuangjie.framework.common.pojo.CommonResult;
import com.chuangjie.module.oa.controller.admin.discussion.vo.vote.OaDiscussionVoteReqVO;
import com.chuangjie.module.oa.service.discussion.OaDiscussionVoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.chuangjie.framework.common.pojo.CommonResult.success;
import static com.chuangjie.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - OA 讨论投票")
@RestController
@RequestMapping("/oa/discussion-vote")
@Validated
public class OaDiscussionVoteController {

    @Resource
    private OaDiscussionVoteService discussionVoteService;

    @PostMapping("/create")
    @Operation(summary = "参与讨论投票")
    public CommonResult<Boolean> voteDiscussion(@Valid @RequestBody OaDiscussionVoteReqVO voteReqVO) {
        discussionVoteService.voteDiscussion(voteReqVO.getDiscussionId(), voteReqVO.getOptionIds(), getLoginUserId());
        return success(true);
    }

}
