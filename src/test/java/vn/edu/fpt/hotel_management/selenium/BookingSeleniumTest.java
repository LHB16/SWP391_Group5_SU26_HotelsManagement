package vn.edu.fpt.hotel_management.selenium;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BookingSeleniumTest {

    @LocalServerPort
    private int port;

    private String baseUrl;
    private final String inputExcelPath = "booking_test_data.xlsx";
    private final String outputExcelPath = "booking_test_report.xlsx";

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    public void runBookingAutomationTest() throws IOException {
        System.out.println("=== STARTING COMPREHENSIVE BOOKING E2E TEST ===");

        // Check if input excel file exists
        File inputFile = new File(inputExcelPath);
        if (!inputFile.exists()) {
            System.out.println(">>> SKIP TEST: Test data file '" + inputExcelPath + "' does not exist.");
            return;
        }

        // Read test data from Excel file
        List<Map<String, String>> testCases = ExcelHelper.readTestData(inputExcelPath);
        if (testCases == null || testCases.isEmpty()) {
            System.out.println(">>> SKIP TEST: No test data found in file: " + inputExcelPath);
            return;
        }
        List<Map<String, String>> reportList = new ArrayList<>();

        System.out.println("Number of test cases read from Excel: " + testCases.size());

        for (Map<String, String> testCase : testCases) {
            String testCaseId = testCase.getOrDefault("TestCaseID", "TC_Unknown");
            String username = testCase.getOrDefault("Username", "");
            String password = testCase.getOrDefault("Password", "");
            String hotelName = testCase.getOrDefault("HotelName", "");
            String roomType = testCase.getOrDefault("RoomType", "");
            String fullName = testCase.getOrDefault("FullName", "");
            String phone = testCase.getOrDefault("Phone", "");
            String scenario = testCase.getOrDefault("Scenario", "SUCCESS");
            String description = testCase.getOrDefault("Description", "");

            System.out.println("\n--------------------------------------------------");
            System.out.println("Running: " + testCaseId + " | Scenario: " + scenario + " | User: " + username);

            Map<String, String> reportRow = new HashMap<>(testCase);
            long startTime = System.currentTimeMillis();

            WebDriver driver = null;
            try {
                // Initialize Selenium Driver (Chrome)
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--start-maximized");
                options.addArguments("--remote-allow-origins=*");
                driver = new ChromeDriver(options);
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(8));
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));

                switch (scenario.toUpperCase()) {
                    case "SUCCESS":
                        // 1. Login
                        performLogin(driver, wait, username, password);

                        // 2. Choose Hotel and click View Rooms
                        selectHotel(driver, wait, hotelName);

                        // 3. Choose Room and click Booking
                        selectRoomAndBook(driver, wait, roomType);

                        // 4. Fill in customer information
                        WebElement nameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("fullName")));
                        WebElement phoneInput = driver.findElement(By.id("phone"));
                        nameInput.clear();
                        nameInput.sendKeys(fullName);
                        phoneInput.clear();
                        phoneInput.sendKeys(phone);

                        // 5. Submit Booking
                        WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//button[@type='submit' and (contains(., 'Confirm') or contains(., 'Pay'))]")
                        ));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);

                        // 6. Extract bookingId from payment-data div attribute and call Bypass
                        WebElement paymentData = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("payment-data")));
                        String bookingId = paymentData.getAttribute("data-booking-id");
                        System.out.println("Extracted bookingId from paymentData attribute: " + bookingId);

                        driver.get(baseUrl + "/booking/payment-bypass?bookingId=" + bookingId);

                        // 7. Verify in history page (Fixing XPath to search in all descendant text nodes using '.')
                        wait.until(ExpectedConditions.urlContains("/booking/history"));
                        String bookingCardXPath = String.format(
                                "//div[contains(@class, 'bh-card') and .//div[contains(@class, 'bh-card-room-type') and contains(., '%s')] and .//div[contains(@class, 'bh-card-hotel-name') and contains(., '%s')]]",
                                roomType, hotelName
                        );
                        WebElement bookingCard = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(bookingCardXPath)));
                        WebElement statusSpan = bookingCard.findElement(By.xpath(".//span[contains(@class, 'bd-status-confirmed') or contains(text(), 'Confirmed')]"));
                        assertTrue(statusSpan.isDisplayed(), "Booking status label is not Confirmed!");

                        reportRow.put("Status", "PASS");
                        reportRow.put("Note", "Happy Path PASS: Booking created and bypass payment confirmed status successfully.");
                        break;

                    case "UNAUTHORIZED":
                        // Access booking checkout directly without login
                        driver.get(baseUrl + "/booking/create?hotelId=1&roomId=1");
                        System.out.println("Accessed checkout directly without login");

                        // Expect to redirect to login page
                        wait.until(ExpectedConditions.urlContains("/login"));
                        assertTrue(driver.getCurrentUrl().contains("/login"), "Unauthenticated user was not redirected to login page!");
                        System.out.println("Unauthenticated user successfully redirected to login page");

                        reportRow.put("Status", "PASS");
                        reportRow.put("Note", "Security Path PASS: Unauthenticated user successfully redirected to login page.");
                        break;

                    case "MISSING_INFO":
                        // 1. Login
                        performLogin(driver, wait, username, password);

                        // 2. Select Hotel
                        selectHotel(driver, wait, hotelName);

                        // 3. Select Room
                        selectRoomAndBook(driver, wait, roomType);

                        // 4. Fill in info but leave FullName empty
                        WebElement nameInputMissing = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("fullName")));
                        WebElement phoneInputMissing = driver.findElement(By.id("phone"));
                        nameInputMissing.clear(); // Leave empty
                        phoneInputMissing.clear();
                        phoneInputMissing.sendKeys(phone);

                        // 5. Click submit (Using Javascript click to avoid element click intercepted while keeping HTML5 Validation)
                        WebElement submitBtnMissing = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//button[@type='submit' and (contains(., 'Confirm') or contains(., 'Pay'))]")
                        ));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtnMissing);
                        System.out.println("Clicked submit with empty FullName");

                        // Wait a short time and verify the URL is still checkout page (not redirected to payment)
                        Thread.sleep(2500);
                        assertTrue(driver.getCurrentUrl().contains("/booking") && !driver.getCurrentUrl().contains("/booking/qr-payment"),
                                "Form submitted even when required fields were missing!");
                        System.out.println("Form submission prevented due to missing information.");

                        reportRow.put("Status", "PASS");
                        reportRow.put("Note", "Validation Path PASS: Form submission prevented due to missing required info.");
                        break;

                    case "INVALID_ROLE":
                        // 1. Login as Owner
                        performLogin(driver, wait, username, password);

                        // 2. Attempt to access booking page
                        driver.get(baseUrl + "/booking/create?hotelId=1&roomId=1");
                        System.out.println("Attempted to access checkout page as Owner");

                        // 3. Expect to be redirected to home page
                        wait.until(ExpectedConditions.urlContains("/home"));
                        assertTrue(driver.getCurrentUrl().contains("/home"), "Owner was not redirected to home page!");
                        System.out.println("Owner successfully blocked and redirected to home page");

                        reportRow.put("Status", "PASS");
                        reportRow.put("Note", "Role Validation Path PASS: Owner redirected to Home page.");
                        break;

                    case "INACTIVE_HOTEL":
                        // 1. Login as Customer
                        performLogin(driver, wait, username, password);

                        // 2. Try to access booking for invalid hotelId = 9999
                        driver.get(baseUrl + "/booking/create?hotelId=9999&roomId=9999");
                        System.out.println("Accessing booking checkout for non-existent hotelId=9999");

                        // 3. Expect to redirect to hotels list
                        wait.until(ExpectedConditions.urlContains("/hotels"));
                        assertTrue(driver.getCurrentUrl().contains("/hotels"), "Accessing inactive hotel did not redirect to hotels page!");
                        System.out.println("User successfully redirected to hotels page");

                        reportRow.put("Status", "PASS");
                        reportRow.put("Note", "Business Rule Path PASS: Non-existent/Inactive hotel booking redirected to hotels page.");
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown scenario: " + scenario);
                }

            } catch (Exception e) {
                System.err.println("Error running testcase " + testCaseId + ": " + e.getMessage());
                reportRow.put("Status", "FAIL");
                reportRow.put("Note", "Error: " + e.getMessage());
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                reportRow.put("ExecutionTime", String.valueOf(duration));
                reportList.add(reportRow);

                if (driver != null) {
                    driver.quit();
                    System.out.println("Closed browser.");
                }
            }
        }

        // Export report
        System.out.println("\nExporting test report to Excel: " + outputExcelPath);
        try {
            ExcelHelper.writeTestReport(outputExcelPath, reportList);
            System.out.println("Report exported successfully.");
        } catch (IOException e) {
            System.err.println("Error writing Excel report: " + e.getMessage());
        }
    }

    private void performLogin(WebDriver driver, WebDriverWait wait, String username, String password) {
        driver.get(baseUrl + "/login");
        WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement loginBtn = driver.findElement(By.xpath("//button[@type='submit']"));

        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginBtn);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("avatarBtn")));
        System.out.println("Logged in as user: " + username);
    }

    private void selectHotel(WebDriver driver, WebDriverWait wait, String hotelName) {
        driver.get(baseUrl + "/hotels");
        String viewRoomsXPath = String.format(
                "//div[contains(@class, 'hotel-card') and .//h5[contains(@class, 'card-title') and contains(text(), '%s')]]/descendant::a[contains(@class, 'custom-hl-view-btn')]",
                hotelName
        );
        WebElement viewRoomsBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(viewRoomsXPath)));
        
        String parentWindow = driver.getWindowHandle();
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", viewRoomsBtn);

        wait.until(d -> d.getWindowHandles().size() > 1);
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(parentWindow)) {
                driver.switchTo().window(handle);
                break;
            }
        }
        System.out.println("Selected hotel: " + hotelName);
    }

    private void selectRoomAndBook(WebDriver driver, WebDriverWait wait, String roomType) {
        String bookingBtnXPath = String.format(
                "//div[contains(@class, 'room-card') and .//div[contains(@class, 'room-type-title') and contains(text(), '%s')]]/descendant::a[contains(text(), 'Booking') or contains(@id, 'booking-btn')]",
                roomType
        );
        WebElement bookingBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(bookingBtnXPath)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", bookingBtn);
        System.out.println("Booked room: " + roomType);
    }
}
