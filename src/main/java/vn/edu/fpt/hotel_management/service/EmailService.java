package vn.edu.fpt.hotel_management.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.edu.fpt.hotel_management.entity.Booking;
import vn.edu.fpt.hotel_management.entity.Hotel;
import vn.edu.fpt.hotel_management.entity.HotelOwner;
import vn.edu.fpt.hotel_management.entity.Payment;
import vn.edu.fpt.hotel_management.entity.Refund;
import vn.edu.fpt.hotel_management.entity.User;

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

    // =====================================================
    // REFUND EMAIL NOTIFICATIONS
    // =====================================================

    public void sendRefundSubmitted(Refund refund) {
        if (refund == null || refund.getBooking() == null) return;
        Booking booking = refund.getBooking();
        String toEmail = (booking.getCustomer() != null && booking.getCustomer().getUserAccount() != null)
                ? booking.getCustomer().getUserAccount().getEmail()
                : null;
        if (toEmail == null || toEmail.isBlank()) return;

        String customerName = booking.getCustomer() != null ? booking.getCustomer().getFullName() : "Valued Customer";
        String hotelName = booking.getHotel() != null ? booking.getHotel().getName()
                : (booking.getRoom() != null ? "Booking Hotels Partner" : "Booking Hotels Partner");
        String refundAmountStr = formatCurrency(refund.getRefundAmount());
        
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String requestedTimeStr = refund.getRequestedAt() != null ? refund.getRequestedAt().format(dtf) : "N/A";

        String heading = "Refund Request Received";
        String bodyContent = "<p>Dear <strong>" + customerName + "</strong>,</p>"
                + "<p>We have received your refund request for <strong>" + hotelName + "</strong>. Our administration team is currently reviewing your request.</p>"
                
                + "<div style='margin: 20px 0; text-align: center;'>"
                + "  <span style='background-color: #fff3cd; color: #856404; border: 1px solid #ffeeba; padding: 8px 18px; border-radius: 20px; font-weight: bold; font-size: 14px; display: inline-block;'>PENDING REVIEW</span>"
                + "</div>"

                + "<h3>Refund Request Details</h3>"
                + "<table style='width: 100%; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 25px; border-collapse: collapse;'>"
                + "  <tr style='background-color: #faf8f4; border-bottom: 1px solid #e2e8f0;'>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Field</th>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Details</th>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Hotel</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + hotelName + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Refund Amount</td>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600; color: #c9a96e;'>" + refundAmountStr + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Bank Name</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + (refund.getBankName() != null ? refund.getBankName() : "N/A") + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Account Number</td>"
                + "    <td style='padding: 12px; font-size: 14px; font-family: monospace;'>" + (refund.getAccountNumber() != null ? refund.getAccountNumber() : "N/A") + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Account Holder</td>"
                + "    <td style='padding: 12px; font-size: 14px; text-transform: uppercase;'>" + (refund.getAccountHolder() != null ? refund.getAccountHolder() : "N/A") + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Requested Time</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + requestedTimeStr + "</td>"
                + "  </tr>"
                + "</table>"
                + "<p>Refund processing usually takes 1 to 3 business days. You will receive an email notification once your request has been processed.</p>";

        String subject = "Booking Hotels - Refund Request Submitted";
        String htmlContent = buildHtmlTemplate(subject, heading, bodyContent);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, subject, htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Refund request submitted email sent to: " + toEmail + " for Refund #" + refund.getId());
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send refund request submitted email to " + toEmail + ": " + e.getMessage());
            }
        });
    }

    public void sendRefundProcessed(Refund refund) {
        if (refund == null || refund.getBooking() == null) return;
        Booking booking = refund.getBooking();
        String toEmail = (booking.getCustomer() != null && booking.getCustomer().getUserAccount() != null)
                ? booking.getCustomer().getUserAccount().getEmail()
                : null;
        if (toEmail == null || toEmail.isBlank()) return;

        String customerName = booking.getCustomer() != null ? booking.getCustomer().getFullName() : "Valued Customer";
        String hotelName = booking.getHotel() != null ? booking.getHotel().getName() : "Booking Hotels Partner";
        String refundAmountStr = formatCurrency(refund.getRefundAmount());
        
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String processedTimeStr = refund.getProcessedAt() != null ? refund.getProcessedAt().format(dtf) : "N/A";
        String noteStr = (refund.getNote() != null && !refund.getNote().isBlank()) ? refund.getNote() : "Refund transferred to customer bank account successfully.";

        String heading = "Refund Approved & Processed";
        String bodyContent = "<p>Dear <strong>" + customerName + "</strong>,</p>"
                + "<p>Great news! Your refund request for <strong>" + hotelName + "</strong> has been approved and successfully processed.</p>"
                
                + "<div style='margin: 20px 0; text-align: center;'>"
                + "  <span style='background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; padding: 8px 18px; border-radius: 20px; font-weight: bold; font-size: 14px; display: inline-block;'>PROCESSED / REFUNDED</span>"
                + "</div>"

                + "<h3>Refund Transfer Details</h3>"
                + "<table style='width: 100%; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 25px; border-collapse: collapse;'>"
                + "  <tr style='background-color: #faf8f4; border-bottom: 1px solid #e2e8f0;'>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Field</th>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Details</th>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Amount Refunded</td>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600; color: #198754;'>" + refundAmountStr + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Bank Name</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + (refund.getBankName() != null ? refund.getBankName() : "N/A") + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Account Number</td>"
                + "    <td style='padding: 12px; font-size: 14px; font-family: monospace;'>" + (refund.getAccountNumber() != null ? refund.getAccountNumber() : "N/A") + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Account Holder</td>"
                + "    <td style='padding: 12px; font-size: 14px; text-transform: uppercase;'>" + (refund.getAccountHolder() != null ? refund.getAccountHolder() : "N/A") + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Processed Date</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + processedTimeStr + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Admin Note</td>"
                + "    <td style='padding: 12px; font-size: 14px; color: #495057;'>" + noteStr + "</td>"
                + "  </tr>"
                + "</table>"
                + "<p>The funds should be available in your bank account shortly. Thank you for choosing Booking Hotels!</p>";

        String subject = "Booking Hotels - Refund Approved & Processed";
        String htmlContent = buildHtmlTemplate(subject, heading, bodyContent);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, subject, htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Refund processed email sent to: " + toEmail + " for Refund #" + refund.getId());
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send refund processed email to " + toEmail + ": " + e.getMessage());
            }
        });
    }

    public void sendRefundRejected(Refund refund) {
        if (refund == null || refund.getBooking() == null) return;
        Booking booking = refund.getBooking();
        String toEmail = (booking.getCustomer() != null && booking.getCustomer().getUserAccount() != null)
                ? booking.getCustomer().getUserAccount().getEmail()
                : null;
        if (toEmail == null || toEmail.isBlank()) return;

        String customerName = booking.getCustomer() != null ? booking.getCustomer().getFullName() : "Valued Customer";
        String hotelName = booking.getHotel() != null ? booking.getHotel().getName() : "Booking Hotels Partner";
        String refundAmountStr = formatCurrency(refund.getRefundAmount());
        String reasonStr = (refund.getNote() != null && !refund.getNote().isBlank())
                ? refund.getNote()
                : "The request does not meet the required terms and conditions for cancellation/refund.";

        String heading = "Refund Request Update";
        String bodyContent = "<p>Dear <strong>" + customerName + "</strong>,</p>"
                + "<p>We regret to inform you that your refund request for <strong>" + hotelName + "</strong> could not be approved at this time.</p>"
                
                + "<div style='margin: 20px 0; text-align: center;'>"
                + "  <span style='background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; padding: 8px 18px; border-radius: 20px; font-weight: bold; font-size: 14px; display: inline-block;'>REQUEST REJECTED</span>"
                + "</div>"

                + "<h3>Details & Rejection Reason</h3>"
                + "<table style='width: 100%; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 25px; border-collapse: collapse;'>"
                + "  <tr style='background-color: #faf8f4; border-bottom: 1px solid #e2e8f0;'>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Field</th>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Details</th>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Requested Refund</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + refundAmountStr + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600; color: #dc3545;'>Rejection Reason</td>"
                + "    <td style='padding: 12px; font-size: 14px; color: #dc3545; font-weight: 500;'>" + reasonStr + "</td>"
                + "  </tr>"
                + "</table>";

        String subject = "Booking Hotels - Update on Refund Request";
        String htmlContent = buildHtmlTemplate(subject, heading, bodyContent);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, subject, htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Refund rejected email sent to: " + toEmail + " for Refund #" + refund.getId());
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send refund rejected email to " + toEmail + ": " + e.getMessage());
            }
        });
    }

    // =====================================================
    // HOTEL OWNER VERIFICATION NOTIFICATIONS
    // =====================================================

    public void sendOwnerApproved(HotelOwner owner) {
        if (owner == null || owner.getUserAccount() == null) return;
        String toEmail = owner.getUserAccount().getEmail();
        if (toEmail == null || toEmail.isBlank()) return;

        String heading = "Hotel Owner Partner Account Approved!";
        String bodyContent = "<p>Dear <strong>" + owner.getFullName() + "</strong>,</p>"
                + "<p>Congratulations! Your Hotel Owner partner account verification has been <strong>APPROVED</strong> by the Booking Hotels Administration.</p>"
                
                + "<div style='margin: 20px 0; text-align: center;'>"
                + "  <span style='background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; padding: 8px 18px; border-radius: 20px; font-weight: bold; font-size: 14px; display: inline-block;'>VERIFIED HOTEL OWNER</span>"
                + "</div>"

                + "<h3>Partner Information</h3>"
                + "<table style='width: 100%; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 25px; border-collapse: collapse;'>"
                + "  <tr style='background-color: #faf8f4; border-bottom: 1px solid #e2e8f0;'>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Detail</th>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Value</th>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Owner Name</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + owner.getFullName() + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Username</td>"
                + "    <td style='padding: 12px; font-size: 14px;'><code style='color: #0a1628;'>" + owner.getUserAccount().getUsername() + "</code></td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Phone Number</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + (owner.getPhone() != null ? owner.getPhone() : "N/A") + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Tax ID</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + (owner.getTaxId() != null ? owner.getTaxId() : "N/A") + "</td>"
                + "  </tr>"
                + "</table>"
                + "<p>You can now log in to your Hotel Owner Dashboard to list your properties, configure rooms, and manage reservations.</p>"
                + "<p>Welcome to the Booking Hotels partner ecosystem!</p>";

        String subject = "Booking Hotels - Owner Verification Approved";
        String htmlContent = buildHtmlTemplate(subject, heading, bodyContent);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, subject, htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Owner approval email sent to: " + toEmail + " (Owner #" + owner.getId() + ")");
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send owner approval email to " + toEmail + ": " + e.getMessage());
            }
        });
    }

    public void sendOwnerRejected(HotelOwner owner, String reason) {
        if (owner == null || owner.getUserAccount() == null) return;
        String toEmail = owner.getUserAccount().getEmail();
        if (toEmail == null || toEmail.isBlank()) return;

        String rejectionReason = (reason != null && !reason.isBlank()) ? reason
                : (owner.getRejectionReason() != null ? owner.getRejectionReason() : "Verification documents did not meet requirements.");

        String heading = "Hotel Owner Verification Update";
        String bodyContent = "<p>Dear <strong>" + owner.getFullName() + "</strong>,</p>"
                + "<p>Thank you for submitting your verification details to Booking Hotels. After careful review, your Hotel Owner account application could not be approved at this time.</p>"
                
                + "<div style='margin: 20px 0; text-align: center;'>"
                + "  <span style='background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; padding: 8px 18px; border-radius: 20px; font-weight: bold; font-size: 14px; display: inline-block;'>VERIFICATION REJECTED</span>"
                + "</div>"

                + "<h3>Reason & Next Steps</h3>"
                + "<table style='width: 100%; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 25px; border-collapse: collapse;'>"
                + "  <tr style='background-color: #faf8f4; border-bottom: 1px solid #e2e8f0;'>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Field</th>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Information</th>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Applicant Name</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + owner.getFullName() + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600; color: #dc3545;'>Rejection Reason</td>"
                + "    <td style='padding: 12px; font-size: 14px; color: #dc3545; font-weight: 500;'>" + rejectionReason + "</td>"
                + "  </tr>"
                + "</table>"
                + "<p>Please log in to update your profile or re-upload your verification document(s) in accordance with the guidance provided.</p>";

        String subject = "Booking Hotels - Owner Verification Update";
        String htmlContent = buildHtmlTemplate(subject, heading, bodyContent);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, subject, htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Owner rejection email sent to: " + toEmail + " (Owner #" + owner.getId() + ")");
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send owner rejection email to " + toEmail + ": " + e.getMessage());
            }
        });
    }

    // =====================================================
    // HOTEL LISTING VERIFICATION NOTIFICATIONS
    // =====================================================

    public void sendHotelApproved(Hotel hotel) {
        if (hotel == null || hotel.getOwner() == null || hotel.getOwner().getUserAccount() == null) return;
        String toEmail = hotel.getOwner().getUserAccount().getEmail();
        if (toEmail == null || toEmail.isBlank()) return;

        String ownerName = hotel.getOwner().getFullName();
        String hotelAddress = (hotel.getAddress() != null ? hotel.getAddress() : "")
                + (hotel.getCity() != null ? ", " + hotel.getCity() : "");

        String heading = "Hotel Listing Approved!";
        String bodyContent = "<p>Dear <strong>" + ownerName + "</strong>,</p>"
                + "<p>We are delighted to inform you that your hotel listing <strong>" + hotel.getName() + "</strong> has been reviewed and <strong>APPROVED</strong> by our administration team!</p>"
                
                + "<div style='margin: 20px 0; text-align: center;'>"
                + "  <span style='background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; padding: 8px 18px; border-radius: 20px; font-weight: bold; font-size: 14px; display: inline-block;'>LISTING APPROVED & ACTIVE</span>"
                + "</div>"

                + "<h3>Hotel Details</h3>"
                + "<table style='width: 100%; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 25px; border-collapse: collapse;'>"
                + "  <tr style='background-color: #faf8f4; border-bottom: 1px solid #e2e8f0;'>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Field</th>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Information</th>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Hotel Name</td>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600; color: #0a1628;'>" + hotel.getName() + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Address</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + hotelAddress + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Status</td>"
                + "    <td style='padding: 12px; font-size: 14px; color: #198754; font-weight: 600;'>Active & Bookable</td>"
                + "  </tr>"
                + "</table>"
                + "<p>Your hotel property is now active and visible to travelers on the Booking Hotels platform. You can manage your room availability, pricing, and view guest reviews anytime via your dashboard.</p>";

        String subject = "Booking Hotels - Hotel Listing Approved: " + hotel.getName();
        String htmlContent = buildHtmlTemplate(subject, heading, bodyContent);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, subject, htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Hotel approval email sent to: " + toEmail + " for Hotel #" + hotel.getId());
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send hotel approval email to " + toEmail + ": " + e.getMessage());
            }
        });
    }

    public void sendHotelRejected(Hotel hotel, String reason) {
        if (hotel == null || hotel.getOwner() == null || hotel.getOwner().getUserAccount() == null) return;
        String toEmail = hotel.getOwner().getUserAccount().getEmail();
        if (toEmail == null || toEmail.isBlank()) return;

        String ownerName = hotel.getOwner().getFullName();
        String rejectionReason = (reason != null && !reason.isBlank()) ? reason
                : (hotel.getRejectionReason() != null ? hotel.getRejectionReason() : "Submitted documents or details did not meet policy standards.");

        String heading = "Hotel Listing Verification Update";
        String bodyContent = "<p>Dear <strong>" + ownerName + "</strong>,</p>"
                + "<p>We reviewed the submitted documents and details for your hotel listing <strong>" + hotel.getName() + "</strong>. Unfortunately, your hotel listing application could not be approved at this time.</p>"
                
                + "<div style='margin: 20px 0; text-align: center;'>"
                + "  <span style='background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; padding: 8px 18px; border-radius: 20px; font-weight: bold; font-size: 14px; display: inline-block;'>LISTING REJECTED</span>"
                + "</div>"

                + "<h3>Reason & Details</h3>"
                + "<table style='width: 100%; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 25px; border-collapse: collapse;'>"
                + "  <tr style='background-color: #faf8f4; border-bottom: 1px solid #e2e8f0;'>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Field</th>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Information</th>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Hotel Name</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + hotel.getName() + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600; color: #dc3545;'>Rejection Reason</td>"
                + "    <td style='padding: 12px; font-size: 14px; color: #dc3545; font-weight: 500;'>" + rejectionReason + "</td>"
                + "  </tr>"
                + "</table>"
                + "<p>Please log in to your Hotel Owner Dashboard to review the required documents and update your hotel details or re-upload clear documentation for re-verification.</p>";

        String subject = "Booking Hotels - Hotel Listing Update: " + hotel.getName();
        String htmlContent = buildHtmlTemplate(subject, heading, bodyContent);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, subject, htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Hotel rejection email sent to: " + toEmail + " for Hotel #" + hotel.getId());
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send hotel rejection email to " + toEmail + ": " + e.getMessage());
            }
        });
    }

    // =====================================================
    // ACCOUNT STATUS (ENABLE / DISABLE) NOTIFICATIONS
    // =====================================================

    public void sendAccountStatusUpdate(User user, String fullName, String accountType, boolean enabled) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;
        String toEmail = user.getEmail();
        String name = (fullName != null && !fullName.isBlank()) ? fullName : user.getUsername();
        String roleStr = (accountType != null && !accountType.isBlank()) ? accountType : "User Account";

        String heading = enabled ? "Account Activated" : "Account Suspended";
        String subject = enabled ? "Booking Hotels - Account Activated" : "Booking Hotels - Account Suspended";

        String badgeHtml = enabled
                ? "<div style='margin: 20px 0; text-align: center;'><span style='background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; padding: 8px 18px; border-radius: 20px; font-weight: bold; font-size: 14px; display: inline-block;'>ACCOUNT ACTIVE</span></div>"
                : "<div style='margin: 20px 0; text-align: center;'><span style='background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; padding: 8px 18px; border-radius: 20px; font-weight: bold; font-size: 14px; display: inline-block;'>ACCOUNT SUSPENDED</span></div>";

        String bodyMessage = enabled
                ? "<p>Great news! Your Booking Hotels account has been <strong>ACTIVATED</strong> by our administration team. You can now log in and access all features and services.</p>"
                : "<p>We are writing to inform you that your Booking Hotels account has been <strong>SUSPENDED</strong> by our administration team. You will not be able to log in to your account until it is reactivated.</p>";

        String statusStr = enabled ? "ACTIVE" : "SUSPENDED";
        String statusColor = enabled ? "#198754" : "#dc3545";

        String bodyContent = "<p>Dear <strong>" + name + "</strong>,</p>"
                + bodyMessage
                + badgeHtml
                + "<h3>Account Information</h3>"
                + "<table style='width: 100%; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 25px; border-collapse: collapse;'>"
                + "  <tr style='background-color: #faf8f4; border-bottom: 1px solid #e2e8f0;'>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Field</th>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Value</th>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Username</td>"
                + "    <td style='padding: 12px; font-size: 14px;'><code style='color: #0a1628;'>" + user.getUsername() + "</code></td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Account Type</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + roleStr + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Current Status</td>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600; color: " + statusColor + ";'>" + statusStr + "</td>"
                + "  </tr>"
                + "</table>";

        String htmlContent = buildHtmlTemplate(subject, heading, bodyContent);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, subject, htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Account status notification email sent to: " + toEmail + " (Enabled: " + enabled + ")");
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send account status email to " + toEmail + ": " + e.getMessage());
            }
        });
    }

    // =====================================================
    // HOTEL STATUS (ACTIVE / INACTIVE) NOTIFICATIONS
    // =====================================================

    public void sendHotelStatusUpdate(Hotel hotel, boolean active) {
        if (hotel == null || hotel.getOwner() == null || hotel.getOwner().getUserAccount() == null) return;
        String toEmail = hotel.getOwner().getUserAccount().getEmail();
        if (toEmail == null || toEmail.isBlank()) return;

        String ownerName = hotel.getOwner().getFullName();
        String hotelAddress = (hotel.getAddress() != null ? hotel.getAddress() : "")
                + (hotel.getCity() != null ? ", " + hotel.getCity() : "");

        String heading = active ? "Hotel Property Activated" : "Hotel Property Deactivated";
        String subject = active ? "Booking Hotels - Hotel Listing Activated: " + hotel.getName()
                                : "Booking Hotels - Hotel Listing Deactivated: " + hotel.getName();

        String badgeHtml = active
                ? "<div style='margin: 20px 0; text-align: center;'><span style='background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; padding: 8px 18px; border-radius: 20px; font-weight: bold; font-size: 14px; display: inline-block;'>HOTEL ACTIVE</span></div>"
                : "<div style='margin: 20px 0; text-align: center;'><span style='background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; padding: 8px 18px; border-radius: 20px; font-weight: bold; font-size: 14px; display: inline-block;'>HOTEL INACTIVE / DEACTIVATED</span></div>";

        String bodyMessage = active
                ? "<p>Great news! Your hotel property <strong>" + hotel.getName() + "</strong> has been set to <strong>ACTIVE</strong> by our administration team. Guests can now search, view, and book rooms at your property.</p>"
                : "<p>We are writing to inform you that your hotel property <strong>" + hotel.getName() + "</strong> has been set to <strong>INACTIVE / DEACTIVATED</strong> by our administration team. Your hotel listing is temporarily hidden from search results.</p>";

        String statusStr = active ? "ACTIVE & BOOKABLE" : "INACTIVE / HIDDEN";
        String statusColor = active ? "#198754" : "#dc3545";

        String bodyContent = "<p>Dear <strong>" + ownerName + "</strong>,</p>"
                + bodyMessage
                + badgeHtml
                + "<h3>Hotel Information</h3>"
                + "<table style='width: 100%; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 25px; border-collapse: collapse;'>"
                + "  <tr style='background-color: #faf8f4; border-bottom: 1px solid #e2e8f0;'>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Field</th>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Information</th>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Hotel Name</td>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600; color: #0a1628;'>" + hotel.getName() + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Address</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + hotelAddress + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Status</td>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600; color: " + statusColor + ";'>" + statusStr + "</td>"
                + "  </tr>"
                + "</table>";

        String htmlContent = buildHtmlTemplate(subject, heading, bodyContent);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, subject, htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Hotel status notification email sent to: " + toEmail + " for Hotel #" + hotel.getId() + " (Active: " + active + ")");
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send hotel status email to " + toEmail + ": " + e.getMessage());
            }
        });
    }

    // =====================================================
    // PAYOUT DISBURSEMENT NOTIFICATIONS
    // =====================================================

    public void sendPayoutProcessed(Booking booking) {
        if (booking == null || booking.getHotel() == null || booking.getHotel().getOwner() == null) return;
        HotelOwner owner = booking.getHotel().getOwner();
        if (owner.getUserAccount() == null) return;
        String toEmail = owner.getUserAccount().getEmail();
        if (toEmail == null || toEmail.isBlank()) return;

        String ownerName = owner.getFullName();
        String hotelName = booking.getHotel().getName();
        String roomType = booking.getRoom() != null ? booking.getRoom().getRoomType() : "Standard Room";
        
        String totalPriceStr = formatCurrency(booking.getTotalPrice());
        String platformFeeStr = formatCurrency(booking.getPlatformFeeAmount());
        String ownerPayoutStr = formatCurrency(booking.getOwnerPayoutAmount());
        String feePercentStr = (booking.getPlatformFeePercent() != null ? booking.getPlatformFeePercent().stripTrailingZeros().toPlainString() : "10") + "%";

        String bankInfo = (booking.getPayoutBankName() != null ? booking.getPayoutBankName() : "Bank Account")
                + " - " + (booking.getPayoutBankAccountNumber() != null ? booking.getPayoutBankAccountNumber() : "N/A")
                + " (" + (booking.getPayoutBankAccountHolder() != null ? booking.getPayoutBankAccountHolder() : ownerName) + ")";

        String checkoutStr = booking.getCheckOutDate() != null ? booking.getCheckOutDate().toString() : "N/A";

        String heading = "Payout Processed Successfully";
        String subject = "Booking Hotels - Payout Disbursed: " + hotelName;

        String badgeHtml = "<div style='margin: 20px 0; text-align: center;'><span style='background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; padding: 8px 18px; border-radius: 20px; font-weight: bold; font-size: 14px; display: inline-block;'>PAYOUT DISBURSED</span></div>";

        String bodyContent = "<p>Dear <strong>" + ownerName + "</strong>,</p>"
                + "<p>We are pleased to inform you that the payout for a completed reservation at <strong>" + hotelName + "</strong> has been processed and transferred to your bank account.</p>"
                + badgeHtml
                + "<h3>Payout Breakdown</h3>"
                + "<table style='width: 100%; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 25px; border-collapse: collapse;'>"
                + "  <tr style='background-color: #faf8f4; border-bottom: 1px solid #e2e8f0;'>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Description</th>"
                + "    <th style='text-align: left; padding: 12px; font-size: 14px; color: #0a1628;'>Details</th>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Hotel Property</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + hotelName + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Room Type</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + roomType + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Check-out Date</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + checkoutStr + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Total Reservation Revenue</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + totalPriceStr + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Platform Service Fee (" + feePercentStr + ")</td>"
                + "    <td style='padding: 12px; font-size: 14px; color: #dc3545;'>- " + platformFeeStr + "</td>"
                + "  </tr>"
                + "  <tr style='border-bottom: 1px solid #e2e8f0; background-color: #f8fafc;'>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 700; color: #0a1628;'>Net Disbursed Amount</td>"
                + "    <td style='padding: 12px; font-size: 16px; font-weight: 700; color: #198754;'>" + ownerPayoutStr + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding: 12px; font-size: 14px; font-weight: 600;'>Transferred To</td>"
                + "    <td style='padding: 12px; font-size: 14px;'>" + bankInfo + "</td>"
                + "  </tr>"
                + "</table>";

        String htmlContent = buildHtmlTemplate(subject, heading, bodyContent);

        CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMessage(toEmail, subject, htmlContent);
                System.out.println(">>> [EMAIL SUCCESS] Payout notification email sent to: " + toEmail + " for Hotel " + hotelName);
            } catch (Exception e) {
                System.err.println(">>> [EMAIL ERROR] Failed to send payout email to " + toEmail + ": " + e.getMessage());
            }
        });
    }
}
