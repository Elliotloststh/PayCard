package yuan.paycard.controller;

import io.swagger.annotations.Api;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yuan.paycard.annotation.UserLoginToken;
import yuan.paycard.model.Account;
import yuan.paycard.service.AccountService;
import yuan.paycard.utils.AliyunUtils;
import yuan.paycard.utils.ApiResponse;
import yuan.paycard.utils.CommonUtils;

import javax.servlet.http.HttpServletRequest;
import javax.swing.*;

@RestController
@RequestMapping("/account")
@Api(value="账号Controller")
public class AccountController {
    @Autowired
    AccountService accountService;

    @GetMapping("/getAvatar/{phone}")
    public ApiResponse getAvatar(@PathVariable("phone") String phone) {
        Account query = new Account();
        query.setPhoneNumber(phone);
        query.setType(0);
        Account account = accountService.findOne(query);
        if(account == null) {
            ApiResponse response = ApiResponse.customerError();
            response.setMessage("手机号不存在");
            return response;
        }
        ApiResponse response = ApiResponse.ok();
        response.putDataValue("avatar", account.getAvatar());
        return response;
    }

    @PostMapping("/common/login")
    public ApiResponse commonLogin(@RequestBody LoginInput input) throws Exception {
        return login(input, 0);
    }

    @PostMapping("/family/login")
    public ApiResponse familyLogin(@RequestBody LoginInput input) throws Exception {
        return login(input, 1);
    }

    @PostMapping("/common/register")
    public ApiResponse commonRegister(@RequestBody RegisterInput input) throws Exception {
        return register(input, 0);
    }

    @PostMapping("/family/register")
    public ApiResponse familyRegister(@RequestBody RegisterInput input) throws Exception {
        return register(input, 1);
    }

    @UserLoginToken
    @PostMapping("/update")
    public ApiResponse update(@RequestBody Account account, HttpServletRequest request) throws Exception{
        long userId = CommonUtils.getUserId(request);
        account.setId(userId);
        if(account.getPassword()!=null) {
            account.setPassword(CommonUtils.md5(account.getPassword()));
        }
        if(accountService.update(account) == 1) {
            ApiResponse response = ApiResponse.ok();
            if(account.getPassword()!=null) {
                response.putDataValue("token", accountService.getToken(account));
            }
            return response;
        } else {
            return ApiResponse.serverInternalError();
        }
    }

    @PostMapping("/retrievePassword")
    public ApiResponse retrievePassword(@RequestBody retrievePasswordInput input) throws Exception {
        Account query = new Account();
        query.setPhoneNumber(input.getPhoneNumber());
        Account account = accountService.findOne(query);
        account.setPassword(CommonUtils.md5(input.getPassword()));
        if(accountService.update(account) == 1) {
            return ApiResponse.ok();
        } else {
            return ApiResponse.serverInternalError();
        }
    }

    @UserLoginToken
    @PostMapping("/changeAvatar")
    public ApiResponse changeAvatar(@RequestBody MultipartFile avatar, HttpServletRequest request) {
        String url;
        try {
            String name = AliyunUtils.uploadImg(avatar);
            url = AliyunUtils.getImgUrl(name,AliyunUtils.styleAvatar);
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse response = ApiResponse.serverInternalError();
            response.setMessage("头像上传失败，请重试~");
            return response;
        }
        long userId = CommonUtils.getUserId(request);
        Account account = accountService.selectById(userId);
        account.setAvatar(url);
        accountService.update(account);
        return ApiResponse.ok();
    }


    private ApiResponse login(LoginInput input, int type) throws Exception {
        String md5Password = CommonUtils.md5(input.getPassword());
        Account query = Account.builder()
                .phoneNumber(input.getPhone())
                .password(md5Password)
                .type(type)
                .build();
        Account account = accountService.findOne(query);
        if(account == null) {
            ApiResponse response = ApiResponse.customerError();
            response.setMessage("手机号或密码错误");
            return response;
        } else {
            String token = accountService.getToken(account);
            ApiResponse response = ApiResponse.ok();
            response.putDataValue("token", token);
            account.setPassword(null);
            response.putDataValue("userInfo", account);
            return response;
        }
    }

    private ApiResponse register(RegisterInput input, int type) throws Exception {
        Account account = new Account();
        account.setPhoneNumber(input.getPhone());

        if(accountService.findOne(account) != null) {
            ApiResponse response = ApiResponse.customerError();
            response.setMessage("手机号已注册");
            return response;
        }

        account.setPassword(CommonUtils.md5(input.getPassword()));
        account.setUsername(input.getUsername());
        account.setType(type);
        account.setStatus(0);
        account.setCreateTime(System.currentTimeMillis()/1000);
        if(accountService.register(account) == 1) {
            return ApiResponse.ok();
        } else {
            return ApiResponse.serverInternalError();
        }
    }


    @Data
    private static class LoginInput {
        String phone;
        String password;
    }

    @Data
    private static class RegisterInput {
        String phone;
        String username;
        String password;
    }

    @Data
    private static class retrievePasswordInput {
        String phoneNumber;
        String password;
    }
}


