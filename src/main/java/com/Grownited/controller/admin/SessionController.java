package com.Grownited.controller.admin;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.Grownited.entity.UserDetailEntity;
import com.Grownited.entity.UserEntity;
import com.Grownited.entity.UserTypeEntity;
import com.Grownited.repository.UserDetailRepository;
import com.Grownited.repository.UserRepository;
import com.Grownited.repository.UserTypeRepository;
import com.Grownited.service.MailerService;
import com.cloudinary.Cloudinary;

import jakarta.servlet.http.HttpSession;

@Controller
public class SessionController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserTypeRepository userTypeRepository;

    @Autowired
    UserDetailRepository userDetailRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    MailerService mailerService;

    @Autowired
    Cloudinary cloudinary;

    @GetMapping("/signup")
    public String openSignupPage(Model model) {
        List<UserTypeEntity> allUserType = userTypeRepository.findAll();
        model.addAttribute("allUserType", allUserType);
        return "admin/Signup";
    }

    @GetMapping("/login")
    public String openLoginPage() {
        return "admin/Login";
    }

    // ✅ LOGIN
    @PostMapping("/authenticate")
    public String authenticate(String email, String password, Model model, HttpSession session) {

        Optional<UserEntity> op = userRepository.findByEmail(email);

        if (op.isPresent()) {
            UserEntity dbUser = op.get();

            if (passwordEncoder.matches(password, dbUser.getPassword())) {

                session.setAttribute("user", dbUser);

                if (dbUser.getRole().equals("ADMIN")) {
                    return "redirect:/admin-dashboard";
                } else if (dbUser.getRole().equals("PARTICIPANT")) {
                    return "redirect:/participant/home";
                } else if (dbUser.getRole().equals("JUDGE")) {
                    return "redirect:/judge-dashboard";
                }
            }
        }

        model.addAttribute("error", "Invalid Credentials");
        return "admin/Login";
    }

    @GetMapping("/forgetpassword")
    public String showForgotPasswordForm() {
        return "admin/ForgetPassword";
    }

    // ✅ FORGOT PASSWORD
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, Model model) {

        Optional<UserEntity> op = userRepository.findByEmail(email);

        if (op.isEmpty()) {
            model.addAttribute("error", "Email not found");
            return "admin/ForgetPassword";
        }

        UserEntity user = op.get();

        String otp = mailerService.sendForgotPasswordOtp(email);

        user.setOtp(otp);
        userRepository.save(user);

        model.addAttribute("email", email);

        return "admin/ResetPassword";
    }

    // ✅ RESET PASSWORD
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email,
                                @RequestParam String otp,
                                @RequestParam String newPassword,
                                Model model) {

        Optional<UserEntity> op = userRepository.findByEmail(email);

        if (op.isEmpty()) {
            model.addAttribute("error", "User not found");
            return "admin/ResetPassword";
        }

        UserEntity user = op.get();

        if (user.getOtp() == null || !user.getOtp().equals(otp)) {
            model.addAttribute("error", "Invalid OTP");
            model.addAttribute("email", email);
            return "admin/ResetPassword";
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        user.setOtp(null);

        userRepository.save(user);

        return "admin/Login";
    }

    // ✅ REGISTER
    @PostMapping("/register")
    public String register(UserEntity userEntity, UserDetailEntity userDetailEntity, MultipartFile profilePic) {

        userEntity.setRole("PARTICIPANT");
        userEntity.setActive(true);
        userEntity.setCreatedAt(LocalDate.now());

        String encodedPassword = passwordEncoder.encode(userEntity.getPassword());
        userEntity.setPassword(encodedPassword);

        try {
            if (profilePic != null && !profilePic.isEmpty()) {

                Map uploadResult = cloudinary.uploader().upload(profilePic.getBytes(), Map.of());
                String profilePicURL = uploadResult.get("secure_url").toString();

                userEntity.setProfilePicURL(profilePicURL);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        userRepository.save(userEntity);

        userDetailEntity.setUserId(userEntity.getUserId());
        userDetailRepository.save(userDetailEntity);

        mailerService.sendWelcomeMail(userEntity);

        return "admin/Login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "admin/Login";
    }
}