package vn.edu.fpt.hotel_management.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.edu.fpt.hotel_management.entity.Booking;
import vn.edu.fpt.hotel_management.entity.Payment;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private String buildHtmlTemplate(String title, String heading, String bodyContent) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "  <meta charset='utf-8'>"
                + "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "  <title>" + title + "</title>"
                + "  <style>"
                + "    body { margin: 0; padding: 0; background-color: #f5f6f8; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased; }"
                + "    table { border-collapse: collapse; width: 100%; }"
                + "    .wrapper { width: 100%; table-layout: fixed; background-color: #f5f6f8; padding: 40px 0; }"
                + "    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(10, 22, 40, 0.08); border: 1px solid #e2e8f0; }"
                + "    .header { background-color: #0a1628; padding: 35px 20px; text-align: center; border-bottom: 4px solid #c9a96e; }"
                + "    .header h1 { margin: 0; color: #c9a96e; font-size: 24px; font-weight: bold; letter-spacing: 2px; text-transform: uppercase; font-family: 'Playfair Display', Georgia, serif; }"
                + "    .content { padding: 40px 30px; color: #1a1a2e; font-size: 16px; line-height: 1.6; }"
                + "    .content h2 { color: #0a1628; font-size: 20px; margin-top: 0; margin-bottom: 20px; font-weight: 600; }"
                + "    .otp-box { background-color: #faf8f4; border: 1px dashed #c9a96e; border-radius: 8px; padding: 20px; text-align: center; margin: 30px 0; }"
                + "    .otp-code { font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #0a1628; margin: 0; font-family: 'Courier New', Courier, monospace; }"
                + "    .info-table { margin: 25px 0; width: auto; }"
                + "    .info-cell { padding: 12px 24px; background-color: #faf8f4; border: 1px solid #e2e8f0; border-radius: 8px; color: #0a1628; font-size: 15px; }"
                + "    .divider { height: 1px; background-color: #e2e8f0; margin: 30px 0; }"
                + "    .footer { background-color: #faf8f4; padding: 25px 20px; text-align: center; font-size: 13px; color: #6c757d; border-top: 1px solid #e2e8f0; }"
                + "    .footer p { margin: 5px 0; }"
                + "    .footer a { color: #c9a96e; text-decoration: none; font-weight: 500; }"
                + "  </style>"
                + "</head>"
                + "<body>"
                + "  <div class='wrapper'>"
                + "    <div class='container'>"
                + "      <div class='header'>"
                + "        <h1>Booking Hotels</h1>"
                + "      </div>"
                + "      <div class='content'>"
                + "        <h2>" + heading + "</h2>"
                + "        " + bodyContent
                + "      </div>"
                + "      <div class='footer'>"
                + "        <p>&copy; 2026 Booking Hotels. All rights reserved.</p>"
                + "        <p>If you have any questions, contact us at <a href='mailto:hotelmanagementcantho@gmail.com'>hotelmanagementcantho@gmail.com</a>.</p>"
                + "      </div>"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }

    private void sendHtmlMessage(String toEmail, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    public void sendOtp(String toEmail, String otp) {
        String heading = "Verify Your Account";
        String bodyContent = "<p>Dear Customer,</p>"
                + "<p>We received a request to verify your account. Please use the following One-Time Password (OTP) to complete the verification process:</p>"
                + "<div class='otp-box'>"
                + "  <div class='otp-code'>" + otp + "</div>"
                + "</div>"
                + "<p style='color: #6c757d; font-size: 14px;'>Please note that this code is only valid for <strong>3 minutes</strong>. For security reasons, do not share this OTP with anyone.</p>";
        
        String htmlContent = buildHtmlTemplate("Booking Hotels - Verify OTP", heading, bodyContent);
        
        System.out.println(">>> [DEV BYPASS] OTP for " + toEmail + " is: " + otp);
        
        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, "Booking Hotels - Verify OTP", htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] OTP sent to: " + toEmail);
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send OTP to " + toEmail + ": " + e.getMessage());
            }
        });
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        String heading = "Reset Your Password";
        String bodyContent = "<p>Dear Customer,</p>"
                + "<p>We received a request to reset the password for your account. Please use the following One-Time Password (OTP) to proceed with your password reset:</p>"
                + "<div class='otp-box'>"
                + "  <div class='otp-code'>" + otp + "</div>"
                + "</div>"
                + "<p style='color: #6c757d; font-size: 14px;'>Please note that this code is only valid for <strong>3 minutes</strong>. If you did not request a password reset, please ignore this email or contact support if you suspect unauthorized access.</p>";

        String htmlContent = buildHtmlTemplate("Booking Hotels - Reset Password", heading, bodyContent);

        System.out.println(">>> [DEV BYPASS] Password Reset OTP for " + toEmail + " is: " + otp);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, "Booking Hotels - Reset Password", htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Reset OTP sent to: " + toEmail);
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send Reset OTP to " + toEmail + ": " + e.getMessage());
            }
        });
    }

    public void sendWelcome(String toEmail, String fullName, String username) {
        String heading = "Welcome to Booking Hotels!";
        String bodyContent = "<p>Hi <strong>" + fullName + "</strong>,</p>"
                + "<p>Your account has been created successfully! Welcome to the Booking Hotels community.</p>"
                + "<table class='info-table'>"
                + "  <tr>"
                + "    <td class='info-cell'>"
                + "      <strong>Username:</strong> <code style='font-size: 15px; color: #0a1628;'>" + username
                + "</code>"
                + "    </td>"
                + "  </tr>"
                + "</table>"
                + "<p>You can now log in to your account and explore our services, search for hotels, and manage your bookings effortlessly.</p>"
                + "<p>Thank you for choosing Booking Hotels!</p>";

        String htmlContent = buildHtmlTemplate("Booking Hotels - Welcome!", heading, bodyContent);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, "Booking Hotels - Welcome!", htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Welcome email sent to: " + toEmail);
            } catch (Exception e) {
                System.err.println(
                        ">>> [EMAIL ERROR] Failed to send welcome email to " + toEmail + ": " + e.getMessage());
            }
        });
    }

    public void sendPasswordResetSuccess(String toEmail, String fullName, String username) {
        String heading = "Password Reset Successfully";
        String bodyContent = "<p>Hi <strong>" + fullName + "</strong>,</p>"
                + "<p>Your password has been reset successfully. You can now log in with your username and your new password.</p>"
                + "<table class='info-table'>"
                + "  <tr>"
                + "    <td class='info-cell'>"
                + "      <strong>Username:</strong> <code style='font-size: 15px; color: #0a1628;'>" + username
                + "</code>"
                + "    </td>"
                + "  </tr>"
                + "</table>"
                + "<p style='color: #dc3545; font-size: 14px; font-weight: 500;'>If you did not request this password reset, please contact us immediately to secure your account.</p>";

        String htmlContent = buildHtmlTemplate("Booking Hotels - Password Reset Successfully", heading, bodyContent);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, "Booking Hotels - Password Reset Successfully", htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Password reset success email sent to: " + toEmail);
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send password reset success email to " + toEmail + ": "
                        + e.getMessage());
            }
        });
    }

    public void sendProfileUpdateOtp(String toEmail, String otp) {
        String heading = "Verify Profile Modification";
        String bodyContent = "<p>Dear Customer,</p>"
                + "<p>We received a request to update your profile email or details. Please use the following One-Time Password (OTP) to verify and confirm these modifications:</p>"
                + "<div class='otp-box'>"
                + "  <div class='otp-code'>" + otp + "</div>"
                + "</div>"
                + "<p style='color: #6c757d; font-size: 14px;'>Please note that this code is only valid for <strong>3 minutes</strong>. If you did not request this update, please ignore this email or contact support immediately.</p>";

        String htmlContent = buildHtmlTemplate("Booking Hotels - Verify Profile Update OTP", heading, bodyContent);

        System.out.println(">>> [DEV BYPASS] Profile Update OTP for " + toEmail + " is: " + otp);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, "Booking Hotels - Verify Profile Update OTP", htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Profile update OTP sent to: " + toEmail);
            } catch (Exception e) {
                System.err.println(
                        ">>> [EMAIL ERROR] Failed to send profile update OTP to " + toEmail + ": " + e.getMessage());
            }
        });
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 VND";
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
        return formatter.format(amount) + " VND";
    }

    public void sendBookingConfirmation(String toEmail, Booking booking, Payment payment) {
        String heading = "Booking Confirmed!";
        
        String hotelName = booking.getHotel() != null ? booking.getHotel().getName() : "Booking Hotels Partner";
        String hotelAddress = booking.getHotel() != null ? (booking.getHotel().getAddress() + ", " + booking.getHotel().getCity()) : "";
        String roomType = booking.getRoom() != null ? booking.getRoom().getType() : "Standard Room";
        int quantity = booking.getQuantity() != null ? booking.getQuantity() : 1;
        
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String checkIn = booking.getCheckInDate() != null ? booking.getCheckInDate().format(dateFormatter) : "N/A";
        String checkOut = booking.getCheckOutDate() != null ? booking.getCheckOutDate().format(dateFormatter) : "N/A";
        int nights = booking.getNumNights() != null ? booking.getNumNights() : 0;
        
        String amountStr = formatCurrency(booking.getTotalPrice());
        String methodStr = payment != null && payment.getMethod() != null ? payment.getMethod() : "N/A";
        String transactionIdStr = payment != null && payment.getTransactionId() != null ? payment.getTransactionId() : "N/A";
        
        String paidAtStr = "N/A";
        if (payment != null && payment.getPaidAt() != null) {
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            paidAtStr = payment.getPaidAt().format(dtf);
        }

        String bodyContent = "<p>Dear <strong>" + booking.getFullName() + "</strong>,</p>"
                + "<p>Thank you for choosing Booking Hotels! Your reservation has been confirmed and paid successfully. Here are your booking details and invoice:</p>"
                
                + "<h3>Reservation Details</h3>"
                + "<table style='width: 100%; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 25px; border-collapse: collapse;'>"
                + "  <tr style='background-color: #faf8f4; border-bottom: 1px solid #e2e8f0;'>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Detail</th>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Information</th>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Hotel</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + hotelName + "<br/><span style='font-size: 12px; color: #6c757d;'>" + hotelAddress + "</span></td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Room Type</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + roomType + " (" + quantity + " Room" + (quantity > 1 ? "s" : "") + ")</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Check-In Date</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + checkIn + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Check-Out Date</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + checkOut + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Duration</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + nights + " Night" + (nights > 1 ? "s" : "") + "</td>"
                + "  </tr>"
                + "</table>"
                
                + "<h3>Invoice</h3>"
                + "<table style='width: 100%; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 25px; border-collapse: collapse;'>"
                + "  <tr style='background-color: #faf8f4; border-bottom: 1px solid #e2e8f0;'>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Payment Field</th>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Value</th>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Invoice Status</td>"
                + "    <td style='padding: 12px; font-size: 14px; color: #198754; font-weight: 600;'>PAID</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Amount Paid</td>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600; color: #0a1628;'>" + amountStr + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Payment Method</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + methodStr + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Transaction ID</td>"
                + "    <td style='padding: 12px; font-size: 14px; font-family: monospace;'>" + transactionIdStr + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Paid Time</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + paidAtStr + "</td>"
                + "  </tr>"
                + "</table>"
                
                + "<p>We hope you enjoy your stay! If you need to make changes or have questions about your reservation, please do not hesitate to contact the hotel directly or email our support.</p>";

        String htmlContent = buildHtmlTemplate("Booking Hotels - Reservation Confirmed", heading, bodyContent);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, "Booking Hotels - Reservation Confirmed", htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Booking confirmation sent to: " + toEmail + " for Booking #" + booking.getId());
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send booking confirmation email to " + toEmail + ": " + e.getMessage());
            }
        });
    }
}
