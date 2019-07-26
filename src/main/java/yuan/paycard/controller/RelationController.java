package yuan.paycard.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import yuan.paycard.annotation.UserLoginToken;
import yuan.paycard.model.Account;
import yuan.paycard.model.FamilyRelation;
import yuan.paycard.service.AccountService;
import yuan.paycard.service.FamilyRelationService;
import yuan.paycard.service.PushService;
import yuan.paycard.utils.ApiResponse;
import yuan.paycard.utils.CommonUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/relation")
@Api(value="绑定关系Controller")
public class RelationController {
    @Autowired
    FamilyRelationService familyRelationService;

    @Autowired
    AccountService accountService;

    @Autowired
    PushService pushService;

    @UserLoginToken
    @GetMapping("/getAll")
    public ApiResponse getRelations(HttpServletRequest request) {
        List<FamilyRelation> relations = familyRelationService.getAllRelation(CommonUtils.getUserId(request));
        ApiResponse response = ApiResponse.ok();
        response.putDataValue("size", relations.size());
        response.putDataValue("relations", relations);
        return response;
    }

    @UserLoginToken
    @PostMapping("/sendInvitation")
    public ApiResponse createRelation(@RequestBody createInput input, HttpServletRequest request) {
        long userId = CommonUtils.getUserId(request);
        Account account = accountService.selectById(userId);
        if(accountService.selectById(input.getFamilyId()) == null) {
            ApiResponse response = ApiResponse.customerError();
            response.setMessage("此账号不存在");
            return response;
        }

        FamilyRelation relation = familyRelationService.findRelation(input.getFamilyId());
        if(relation != null) {
            ApiResponse response = ApiResponse.customerError();
            if(relation.getGuardianId() == userId) {
                response.setMessage("您已经与该账号绑定");
            } else {
                response.setMessage("此账号已经有关联亲情账号，无法进行绑定");
            }
            return response;
        }

        JSONObject contentJson = new JSONObject();
        contentJson.put("familyId", userId);
        contentJson.put("relationName", input.getRelationName());
        contentJson.put("monthLimit", input.getMonthLimit());
        contentJson.put("singleLimit", input.getSingleLimit());
        contentJson.put("message", "用户"+account.getUsername()+"向您发来了亲情账号绑定请求");
        pushService.sendMessageToAlias("Invite",contentJson.toJSONString(),String.valueOf(input.getFamilyId()));

        return ApiResponse.ok();
    }

    @UserLoginToken
    @PostMapping("create")
    public ApiResponse confirmRelation(@RequestBody createInput input, HttpServletRequest request) {
        long userId = CommonUtils.getUserId(request);
        FamilyRelation newRelation = new FamilyRelation();
        newRelation.setGuardianId(input.getFamilyId());
        newRelation.setFamilyId(userId);
        newRelation.setRelationName(input.getRelationName());
        newRelation.setMonthLimit(input.getMonthLimit());
        newRelation.setSingleLimit(input.getSingleLimit());
        newRelation.setStatus(0);
        newRelation.setCreateTime(System.currentTimeMillis()/1000);
        newRelation.setUpdateTime(System.currentTimeMillis()/1000);
        familyRelationService.insert(newRelation);
        return ApiResponse.ok();
    }

    @UserLoginToken
    @PostMapping("/update")
    public ApiResponse update(@RequestBody FamilyRelation input, HttpServletRequest request) {
        FamilyRelation relation = familyRelationService.selectById(input.getId());
        long userId = CommonUtils.getUserId(request);
        if(relation.getGuardianId() != userId) {
            return ApiResponse.unauthorized();
        }
        input.setUpdateTime(System.currentTimeMillis()/1000);
        familyRelationService.updateSelective(input);
        return ApiResponse.ok();
    }

    @Data
    static class createInput {
        Long familyId;
        String relationName;
        Double monthLimit;
        Double singleLimit;
    }
}
