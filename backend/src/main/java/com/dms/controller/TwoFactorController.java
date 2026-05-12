package com.dms.controller;

import com.dms.dto.request.SendOtpRequest;
import com.dms.dto.request.VerifyOtpRequest;
import com.dms.dto.response.ApiResponse;
import com.dms.entity.OtpToken;
import com.dms.entity.TwoFactorSetting;
import com.dms.entity.User;
import com.dms.exception.BadRequestException;
import com.dms.service.impl.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Two-factor / one-time-password endpoints. Designed as additive endpoints
 * that work alongside the existing password-based login. Typical flows:
 *
 * <ol>
 *   <li>Authenticated user calls {@code POST /2fa/enable/start} which mails
 *       an OTP. They confirm with {@code POST /2fa/enable/confirm}.</li>
 *   <li>Once enabled, sensitive actions can require {@code POST /2fa/send-otp}
 *       followed by {@code POST /2fa/verify}.</li>
 *   <li>{@code POST /2fa/disable} turns 2FA off after OTP verification.</li>
 * </ol>
 */
@RestController
@RequestMapping("/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final OtpService otpService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal UserDetails ud) {
        User user = otpService.getUser(ud.getUsername());
        TwoFactorSetting s = otpService.getOrCreateSetting(user);
        Map<String, Object> body = new HashMap<>();
        body.put("enabled", Boolean.TRUE.equals(s.getEnabled()));
        body.put("enabledAt", s.getEnabledAt());
        body.put("lastVerifiedAt", s.getLastVerifiedAt());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse> sendOtp(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody(required = false) SendOtpRequest req) {
        User user = otpService.getUser(ud.getUsername());
        OtpToken.Purpose purpose = parsePurpose(req == null ? null : req.getPurpose(),
                OtpToken.Purpose.SENSITIVE_ACTION);
        otpService.generateAndSend(user, purpose);
        return ResponseEntity.ok(ApiResponse.ok("OTP sent to your registered email"));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verify(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody VerifyOtpRequest req) {
        User user = otpService.getUser(ud.getUsername());
        OtpToken.Purpose purpose = parsePurpose(req.getPurpose(),
                OtpToken.Purpose.SENSITIVE_ACTION);
        boolean ok = otpService.verify(user, req.getCode(), purpose);
        if (!ok) throw new BadRequestException("Invalid or expired OTP");
        return ResponseEntity.ok(ApiResponse.ok("OTP verified"));
    }

    @PostMapping("/enable/start")
    public ResponseEntity<ApiResponse> startEnable(@AuthenticationPrincipal UserDetails ud) {
        User user = otpService.getUser(ud.getUsername());
        if (otpService.isEnabled(user)) {
            throw new BadRequestException("2FA is already enabled");
        }
        otpService.generateAndSend(user, OtpToken.Purpose.ENABLE_2FA);
        return ResponseEntity.ok(ApiResponse.ok("Verification code sent to enable 2FA"));
    }

    @PostMapping("/enable/confirm")
    public ResponseEntity<ApiResponse> confirmEnable(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody VerifyOtpRequest req) {
        User user = otpService.getUser(ud.getUsername());
        boolean ok = otpService.verify(user, req.getCode(), OtpToken.Purpose.ENABLE_2FA);
        if (!ok) throw new BadRequestException("Invalid or expired OTP");
        otpService.setEnabled(user, true);
        return ResponseEntity.ok(ApiResponse.ok("2FA enabled"));
    }

    @PostMapping("/disable")
    public ResponseEntity<ApiResponse> disable(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody VerifyOtpRequest req) {
        User user = otpService.getUser(ud.getUsername());
        boolean ok = otpService.verify(user, req.getCode(), OtpToken.Purpose.SENSITIVE_ACTION);
        if (!ok) throw new BadRequestException("Invalid or expired OTP");
        otpService.setEnabled(user, false);
        return ResponseEntity.ok(ApiResponse.ok("2FA disabled"));
    }

    private OtpToken.Purpose parsePurpose(String s, OtpToken.Purpose fallback) {
        if (s == null || s.isBlank()) return fallback;
        try {
            return OtpToken.Purpose.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Invalid purpose. Allowed: LOGIN_2FA, ENABLE_2FA, SENSITIVE_ACTION");
        }
    }
}
