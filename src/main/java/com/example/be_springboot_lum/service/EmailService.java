package com.example.be_springboot_lum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[LỤM.vn] Đặt lại mật khẩu");

            String resetLink = frontendUrl + "/dat-lai-mat-khau?token=" + token;
            String html = buildPasswordResetEmailHtml(fullName, resetLink);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Đã gửi email đặt lại mật khẩu tới: {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email đặt lại mật khẩu tới {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[LỤM] Chào mừng bạn đến với LỤM!");

            String html = buildWelcomeEmailHtml(fullName);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Đã gửi email chào mừng tới: {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email chào mừng tới {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildPasswordResetEmailHtml(String fullName, String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                  <div style="background:#059669; padding:20px; text-align:center;">
                    <h1 style="color:#FFBA00; margin:0;">LỤM.vn</h1>
                  </div>
                  <div style="padding:32px 24px;">
                    <h2>Xin chào %s,</h2>
                    <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                    <p>Nhấn vào nút bên dưới để đặt lại mật khẩu. Link này sẽ hết hạn sau <strong>30 phút</strong>.</p>
                    <div style="text-align:center; margin: 32px 0;">
                      <a href="%s"
                         style="background:#059669; color:#FFBA00; padding:14px 28px;
                                text-decoration:none; border-radius:8px; font-weight:bold; font-size:16px;">
                        Đặt lại mật khẩu
                      </a>
                    </div>
                    <p style="color:#888; font-size:13px;">
                      Nếu bạn không yêu cầu điều này, hãy bỏ qua email này. Tài khoản của bạn vẫn an toàn.
                    </p>
                    <p style="color:#888; font-size:13px;">Hoặc copy link sau vào trình duyệt:<br>%s</p>
                  </div>
                  <div style="background:#f5f5f5; padding:16px; text-align:center; font-size:12px; color:#888;">
                    © 2025 LỤM.vn - Sàn giao dịch đồ cũ sinh viên
                  </div>
                </body>
                </html>
                """.formatted(fullName, resetLink, resetLink);
    }

    private String buildWelcomeEmailHtml(String fullName) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                  <div style="background:#059669; padding:20px; text-align:center;">
                    <h1 style="color:#FFBA00; margin:0;">LỤM.vn</h1>
                  </div>
                  <div style="padding:32px 24px;">
                    <h2>Chào mừng %s!</h2>
                    <p>Cảm ơn bạn đã đăng ký tài khoản tại LỤM.vn - sàn giao dịch đồ cũ dành cho sinh viên.</p>
                    <p>Bạn có thể bắt đầu mua bán, trao đổi đồ cũ với cộng đồng sinh viên ngay bây giờ!</p>
                  </div>
                  <div style="background:#f5f5f5; padding:16px; text-align:center; font-size:12px; color:#888;">
                    © 2025 LỤM.vn - Sàn giao dịch đồ cũ sinh viên
                  </div>
                </body>
                </html>
                """.formatted(fullName);
    }
}
