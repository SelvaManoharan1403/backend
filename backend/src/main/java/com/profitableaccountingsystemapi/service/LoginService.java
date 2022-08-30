package com.profitableaccountingsystemapi.service;

import com.profitableaccountingsystemapi.common.APIResponse;
import com.profitableaccountingsystemapi.dto.*;
import com.profitableaccountingsystemapi.entity.UserModel;
import com.profitableaccountingsystemapi.repo.UserRepository;
import com.profitableaccountingsystemapi.util.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoginService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public APIResponse signUp(SignUpRequestDTO signUpRequestDTO) {
        APIResponse apiResponse = new APIResponse();

        UserModel existUser = userRepository.findOneByEmailId(signUpRequestDTO.getEmailId());

        if(existUser != null) {
            apiResponse.setStatus(401);
            apiResponse.setData("This Email is already registered.");
            return apiResponse;
        }

        // dto to entity
        UserModel userModel = new UserModel();
        userModel.setName(signUpRequestDTO.getName());
        userModel.setEmailId(signUpRequestDTO.getEmailId());
        userModel.setActive(Boolean.TRUE);
        userModel.setGender(signUpRequestDTO.getGender());
        userModel.setPhoneNumber(signUpRequestDTO.getPhoneNumber());
        userModel.setPassword(passwordEncoder.encode(signUpRequestDTO.getPassword()));

        userModel.setResetToken(passwordEncoder.encode(userModel.getEmailId()).replaceAll("[^A-Za-z]",""));

        //store entity

        userModel = userRepository.save(userModel);

        // return
        apiResponse.setData(userModel);
        return apiResponse;
    }

    public APIResponse login(LoginRequestDTO loginRequestDTO) throws Exception {
        APIResponse apiResponse = new APIResponse();

        UserModel existUser = userRepository.findOneByEmailId(loginRequestDTO.getEmailId());

        if (existUser == null) {
            apiResponse.setStatus(401);
            apiResponse.setData("Invalid Email ID");
            return apiResponse;
        }
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), existUser.getPassword())) {
            apiResponse.setStatus(401);
            apiResponse.setData("Password is incorrect");
            return apiResponse;
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequestDTO.getEmailId(), existUser.getPassword()));
        } catch(Exception e) {
            apiResponse.setStatus(401);
            apiResponse.setData("Authentication Failed");
            return apiResponse;
        }

        UserDetails loadedUser = userService.loadUserByUsername(loginRequestDTO.getEmailId());

        // generate JWT
        //String token = jwtUtils.generateJwt(user);
        String token = jwtUtils.generateToken(loadedUser);
        String tokenRefresh = jwtUtils.generateRefreshToken(existUser);

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", token);
        data.put("refreshToken", tokenRefresh);

        apiResponse.setData(data);

        return apiResponse;
    }

    public APIResponse refreshToken(TokenDTO tokenDTO) throws Exception {
        APIResponse apiResponse = new APIResponse();

        //User user = userRepository.findByRefreshToken();
        Claims claims = jwtUtils.verify(tokenDTO.getRefreshToken());

        tokenDTO.setId(claims.getIssuer());

        UserModel userModel = userRepository.findOne(tokenDTO.getId());

        UserDetails loadedUser = userService.loadUserByUsername(userModel.getEmailId());

        // response

        // generate Refresh JWT
        String tokenAccess = jwtUtils.generateToken(loadedUser);
        String tokenRefresh = jwtUtils.generateRefreshToken(userModel);

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", tokenAccess);
        data.put("refreshToken", tokenRefresh);

        apiResponse.setData(data);

        return apiResponse;
    }

    public APIResponse resetTokenVerify(ResetTokenDTO resetTokenDTO) throws Exception {
        APIResponse apiResponse = new APIResponse();
        UserModel userModel = userRepository.findByResetToken(resetTokenDTO.getResetToken());
        if(userModel == null) {
            apiResponse.setStatus(401);
            apiResponse.setData("Token not Valid");
            return apiResponse;
        }

        // response
        apiResponse.setData("Success");

        return apiResponse;
    }

    public APIResponse updatePassword(UpdatePasswordDTO updatePasswordDTO) {
        APIResponse apiResponse = new APIResponse();

        UserModel userModel = userRepository.findByResetToken(updatePasswordDTO.getResetToken());

        if (passwordEncoder.matches(updatePasswordDTO.getPassword(), userModel.getPassword())) {
            apiResponse.setStatus(401);
            apiResponse.setData("Old Password & New Password are same.");
            return apiResponse;
        }


        userModel.setPassword(passwordEncoder.encode(updatePasswordDTO.getPassword()));

        userModel.setResetToken(passwordEncoder.encode(userModel.getEmailId()).replaceAll("[^A-Za-z]",""));

        userRepository.save(userModel);

        // return
        apiResponse.setData(userModel);
        return apiResponse;
    }

    public APIResponse forgetPasswordService(ForgetPasswordDTO forgetPasswordDTO) {
        APIResponse apiResponse = new APIResponse();

        UserModel userModel = userRepository.findOneByEmailId(forgetPasswordDTO.getEmailId());
        if(userModel == null) {
            apiResponse.setStatus(401);
            apiResponse.setData("This Email is not register.");
            return apiResponse;
        }

        String appUrl = httpServletRequest.getScheme() + "://" + httpServletRequest.getServerName() + ":3000";

        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom("Aru");
        simpleMailMessage.setTo(forgetPasswordDTO.getEmailId());
        simpleMailMessage.setSubject("Profitable Accounting System Api\n");
        simpleMailMessage.setText("Hi..\n\nTo reset your password, click the link below:\n\n" + appUrl
                + "/changePassword/" + userModel.getResetToken());

        javaMailSender.send(simpleMailMessage);

        // return
        apiResponse.setData("Successfully Done...");
        return apiResponse;
    }

}
