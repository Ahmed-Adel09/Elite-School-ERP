package com.elite.erp.business;

import com.elite.erp.model.Parent;
import com.elite.erp.model.Student;
import com.elite.erp.util.Response;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.util.Properties;

/**
 * EmailService — Real SMTP email delivery via Gmail using Jakarta Mail.
 *
 * Extends BaseService (Inheritance + Polymorphism — course requirement).
 *
 * ─────────────────────────────────────────────────────────────────────
 * HOW TO CONFIGURE YOUR GMAIL APP PASSWORD
 * ─────────────────────────────────────────────────────────────────────
 * 1. Enable 2-Step Verification on your Gmail account.
 * 2. Go to: myaccount.google.com → Security → App Passwords
 * 3. Generate an App Password for "Mail".
 * 4. Replace SENDER_EMAIL and SENDER_APP_PASSWORD below with your values.
 * 5. That's it — the ERP will send real emails on Approve.
 * ─────────────────────────────────────────────────────────────────────
 *
 * Dual-activation: sendActivationEmails() sends BOTH student and parent
 * emails atomically so both parties are notified in one call.
 */
public class EmailService extends BaseService {

  // ── ⚠ FILL IN YOUR DETAILS HERE ────────────────────────────────────────
  private static final String SENDER_EMAIL = "aamansour1001@gmail.com";
  private static final String SENDER_APP_PASSWORD = "tcpdrcwfamzdbgag";
  // ────────────────────────────────────────────────────────────────────────

  private static final String SCHOOL_NAME = "Elite International School";
  private static final String SMTP_HOST = "smtp.gmail.com";
  private static final int SMTP_PORT = 587;

  public EmailService() {
    super();
  }

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Dual-activation: sends both the student and parent activation emails.
   * Called from the Admin Approve Task on a background thread.
   *
   * @param studentEmail the student's own Gmail (from Step 3)
   * @param parentEmail  the parent/guardian email (from Step 2)
   * @param studentId    the application / student ID shown in the email
   * @throws MessagingException if SMTP fails (caught by Task.setOnFailed)
   */
  public void sendActivationEmails(String studentEmail,
      String parentEmail,
      int studentId) throws MessagingException {

    logInfo("Sending activation emails — student: " + studentEmail
        + " | parent: " + parentEmail);

    Session session = buildSession();

    // Student activation email
    if (studentEmail != null && !studentEmail.isBlank()) {
      sendHtml(session,
          studentEmail,
          "Welcome to " + SCHOOL_NAME + " — Activate Your Account",
          buildStudentHtml(studentId));
      logInfo("✅ Student activation email sent to " + studentEmail);
    }

    // Parent enrollment email
    if (parentEmail != null && !parentEmail.isBlank()) {
      sendHtml(session,
          parentEmail,
          "Enrollment Confirmed — Set Up Your Parent Portal | " + SCHOOL_NAME,
          buildParentHtml(studentId));
      logInfo("✅ Parent enrollment email sent to " + parentEmail);
    }
  }

  // ── Legacy helpers (kept for backwards compatibility) ─────────────────────

  /** Wraps sendActivationEmails for callers that have model objects. */
  public Response<Boolean> sendStudentWelcomeEmail(Student student, int applicationId) {
    try {
      Session session = buildSession();
      String target = student.getStudentEmail();
      if (target == null || target.isBlank()) {
        logInfo("[SIMULATED] No student email — console only.");
        printConsoleEmail(student.getFullName(), target,
            "Welcome to " + SCHOOL_NAME,
            buildStudentHtml(applicationId));
        return Response.success(true, "Simulated (no email address)");
      }
      sendHtml(session, target,
          "Welcome to " + SCHOOL_NAME + " — Activate Your Account",
          buildStudentHtml(applicationId));
      return Response.success(true, "Email sent to " + target);
    } catch (MessagingException e) {
      logError("Student email failed", e);
      return Response.failure("Email failed: " + e.getMessage());
    }
  }

  /** Wraps sendActivationEmails for callers that have model objects. */
  public Response<Boolean> sendParentEnrollmentEmail(Parent parent, Student student) {
    if (isNullOrEmpty(parent.getEmail())) {
      return Response.failure("Parent email missing", "EMAIL_NO_ADDR");
    }
    try {
      Session session = buildSession();
      sendHtml(session, parent.getEmail(),
          "Enrollment Confirmed — Set Up Your Parent Portal | " + SCHOOL_NAME,
          buildParentHtml(student.getId() > 0 ? student.getId() : student.getApplicationId()));
      return Response.success(true, "Parent email sent to " + parent.getEmail());
    } catch (MessagingException e) {
      logError("Parent email failed", e);
      return Response.failure("Email failed: " + e.getMessage());
    }
  }

  // ── SMTP session ──────────────────────────────────────────────────────────

  private Session buildSession() {
    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true"); // TLS on port 587
    props.put("mail.smtp.host", SMTP_HOST);
    props.put("mail.smtp.port", SMTP_PORT);
    props.put("mail.smtp.ssl.protocols", "TLSv1.2");
    props.put("mail.debug", "false"); // set true to debug SMTP

    return Session.getInstance(props, new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(SENDER_EMAIL, SENDER_APP_PASSWORD);
      }
    });
  }

  /** Sends a rich HTML email via SMTP. */
  private void sendHtml(Session session, String toEmail,
      String subject, String htmlBody) throws MessagingException {
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(SENDER_EMAIL));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
    message.setSubject(subject, "UTF-8");

    // Multipart: HTML + plain-text fallback
    MimeMultipart multipart = new MimeMultipart("alternative");

    MimeBodyPart plainPart = new MimeBodyPart();
    plainPart.setText(stripHtml(htmlBody), "UTF-8");

    MimeBodyPart htmlPart = new MimeBodyPart();
    htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

    multipart.addBodyPart(plainPart);
    multipart.addBodyPart(htmlPart);

    message.setContent(multipart);
    Transport.send(message);
  }

  // ── HTML email bodies ─────────────────────────────────────────────────────

  /**
   * Student activation email — professional HTML.
   * Shows Student ID and portal login link.
   */
  private String buildStudentHtml(int studentId) {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width"/>
        <title>Welcome to Elite International School</title>
        <style>
          body { margin:0; padding:0; background:#f0f4f8; font-family:'Segoe UI',Arial,sans-serif; }
          .wrapper { max-width:620px; margin:32px auto; background:#ffffff;
                     border-radius:12px; overflow:hidden;
                     box-shadow:0 4px 24px rgba(0,0,0,0.10); }
          .header { background:linear-gradient(135deg,#0A1628,#1A3560);
                    padding:36px 40px; text-align:center; }
          .header h1 { margin:0; color:#C9A84C; font-size:26px; letter-spacing:1px; }
          .header p  { margin:6px 0 0; color:rgba(255,255,255,0.65); font-size:13px; }
          .body { padding:36px 40px; color:#1a2a3a; }
          .body h2 { color:#0A1628; font-size:22px; margin-top:0; }
          .id-badge { display:inline-block; background:#0A1628; color:#C9A84C;
                      font-size:20px; font-weight:bold; padding:12px 28px;
                      border-radius:8px; letter-spacing:3px; margin:18px 0; }
          .steps { background:#f8fafc; border-left:4px solid #C9A84C;
                   border-radius:0 8px 8px 0; padding:16px 20px; margin:20px 0; }
          .steps li { margin:8px 0; color:#2d3e50; }
          .btn { display:inline-block; background:linear-gradient(90deg,#C9A84C,#E8C96A);
                 color:#0A1628 !important; text-decoration:none; font-weight:bold;
                 padding:14px 36px; border-radius:8px; font-size:15px; margin-top:10px; }
          .footer { background:#f8fafc; border-top:1px solid #e0e7ef;
                    padding:20px 40px; text-align:center;
                    color:#8a9bb0; font-size:12px; }
        </style>
        </head>
        <body>
        <div class="wrapper">
          <div class="header">
            <h1>⭐ Elite International School</h1>
            <p>Excellence · Integrity · Innovation</p>
          </div>
          <div class="body">
            <h2>🎓 Congratulations — You're Admitted!</h2>
            <p>Your application has been <strong>reviewed and approved</strong> by our admissions team.
               Welcome to the Elite family!</p>

            <p><strong>Your Student ID:</strong></p>
            <div class="id-badge">EIS-%06d</div>

            <p>Please use your email address to log in and <strong>set your password</strong> on the ERP application.</p>

            <ol class="steps">
              <li>Open the <strong>Elite ERP Desktop Application</strong></li>
              <li>Click <em>"Set Password"</em> on the login screen</li>
              <li>Enter your email address and choose a secure password</li>
              <li>Log in to access your timetable, assignments and resources</li>
            </ol>

            <a href="#" class="btn" style="pointer-events: none; opacity: 0.8;">
              🔑 Open Desktop App
            </a>

            <p style="margin-top:28px;color:#5a6a7a;font-size:13px;">
              If you did not expect this email, please contact admissions@eliteschool.edu immediately.
            </p>
          </div>
          <div class="footer">
            &copy; 2025 Elite International School &nbsp;|&nbsp; admissions@eliteschool.edu<br>
            This is an automated message — please do not reply directly.
          </div>
        </div>
        </body>
        </html>
        """.formatted(studentId, studentId);
  }

  /**
   * Parent enrollment email — professional HTML.
   * Prompts parent to set up their portal account.
   */
  private String buildParentHtml(int studentId) {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width"/>
        <title>Enrollment Finalised — Elite International School</title>
        <style>
          body { margin:0; padding:0; background:#f0f4f8; font-family:'Segoe UI',Arial,sans-serif; }
          .wrapper { max-width:620px; margin:32px auto; background:#ffffff;
                     border-radius:12px; overflow:hidden;
                     box-shadow:0 4px 24px rgba(0,0,0,0.10); }
          .header { background:linear-gradient(135deg,#0A1628,#1A3560);
                    padding:36px 40px; text-align:center; }
          .header h1 { margin:0; color:#C9A84C; font-size:26px; letter-spacing:1px; }
          .header p  { margin:6px 0 0; color:rgba(255,255,255,0.65); font-size:13px; }
          .body { padding:36px 40px; color:#1a2a3a; }
          .body h2 { color:#0A1628; font-size:22px; margin-top:0; }
          .ref { font-size:13px; color:#8a9bb0; margin:4px 0 20px; }
          .features { display:table; width:100%%; margin:20px 0; }
          .feat { background:#f8fafc; border-radius:8px; padding:14px 16px;
                  margin:6px 0; border-left:3px solid #C9A84C; }
          .feat strong { color:#0A1628; }
          .btn { display:inline-block; background:linear-gradient(90deg,#C9A84C,#E8C96A);
                 color:#0A1628 !important; text-decoration:none; font-weight:bold;
                 padding:14px 36px; border-radius:8px; font-size:15px; margin-top:10px; }
          .footer { background:#f8fafc; border-top:1px solid #e0e7ef;
                    padding:20px 40px; text-align:center;
                    color:#8a9bb0; font-size:12px; }
        </style>
        </head>
        <body>
        <div class="wrapper">
          <div class="header">
            <h1>⭐ Elite International School</h1>
            <p>Excellence · Integrity · Innovation</p>
          </div>
          <div class="body">
            <h2>✅ Enrollment Complete!</h2>
            <p class="ref">Student Reference: <strong>EIS-%06d</strong></p>

            <p>We are delighted to confirm that your child's enrollment at
               <strong>Elite International School</strong> is now <strong>finalised</strong>.
               Your Parent Portal account is ready to activate.</p>

            <div class="features">
              <div class="feat"><strong>📅 Timetables</strong> — View your child's weekly schedule</div>
              <div class="feat"><strong>💰 Fee Management</strong> — Pay tuition and view receipts</div>
              <div class="feat"><strong>🏥 Health Records</strong> — Access medical and attendance logs</div>
              <div class="feat"><strong>💬 Teacher Messaging</strong> — Communicate with staff directly</div>
            </div>

            <p style="margin-top:20px; font-weight:bold;">
               Please open the Elite ERP Desktop Application, click "Set Password" on the login screen, and enter your email address to set your password.
            </p>

            <a href="#" class="btn" style="pointer-events: none; opacity: 0.8;">
              🔑 Open Desktop App
            </a>

            <p style="margin-top:28px;color:#5a6a7a;font-size:13px;">
              Need help? Contact us at admissions@eliteschool.edu or call +20 2 1234 5678.
            </p>
          </div>
          <div class="footer">
            &copy; 2025 Elite International School &nbsp;|&nbsp; admissions@eliteschool.edu<br>
            This is an automated message — please do not reply directly.
          </div>
        </div>
        </body>
        </html>
        """.formatted(studentId);
  }

  // ── Teacher Welcome Email ──────────────────────────────────────────────────

  public void sendTeacherWelcomeEmail(String toEmail, String name) throws Exception {
      String html = """
      <!DOCTYPE html>
      <html>
        <body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>
          <div style='max-width:600px; margin:0 auto; background:white; padding:30px;
                      border-radius:8px; box-shadow:0 4px 10px rgba(0,0,0,0.1);
                      border-top:5px solid #0A1628;'>
            <h2 style='color:#0A1628; text-align:center;'>Welcome to Elite International School!</h2>
            <p style='font-size:16px; color:#333;'>Dear <strong>%s</strong>,</p>
            <p style='font-size:16px; color:#555; line-height:1.6;'>
              We are thrilled to officially welcome you to the faculty of Elite International School.
              Your application has been reviewed and approved by HR.
            </p>
            <p style='font-size:16px; color:#555; line-height:1.6;'>
              Please open the Elite ERP Desktop Application, click <em>"Set Password"</em>
              on the login screen, and enter your email address to activate your account.
            </p>
            <p style='font-size:14px; color:#888; text-align:center;'>Elite International School — HR Department</p>
          </div>
        </body>
      </html>
      """.formatted(name);

      Session session = buildSession();
      sendHtml(session, toEmail, "Welcome to Elite International School Faculty!", html);
  }

  // ── Utility ───────────────────────────────────────────────────────────────

  /** Strip HTML tags for plain-text fallback part */
  private String stripHtml(String html) {
    return html.replaceAll("<[^>]+>", "").replaceAll("\\s{2,}", " ").trim();
  }

  /** Console fallback when no SMTP credentials are set */
  private void printConsoleEmail(String toName, String toEmail,
      String subject, String body) {
    System.out.println("╔══════════════════════════════════════╗");
    System.out.println("║    [EMAIL — CONSOLE FALLBACK MODE]   ║");
    System.out.println("╠══════════════════════════════════════╣");
    System.out.println("║ TO     : " + toName + " <" + toEmail + ">");
    System.out.println("║ SUBJECT: " + subject);
    System.out.println("╠══════════════════════════════════════╣");
    System.out.println(stripHtml(body));
    System.out.println("╚══════════════════════════════════════╝");
  }
}
