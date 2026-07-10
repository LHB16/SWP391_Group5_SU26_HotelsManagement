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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import vn.edu.fpt.hotel_management.entity.Customer;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.entity.Wishlist;
import vn.edu.fpt.hotel_management.repository.CustomerRepository;
import vn.edu.fpt.hotel_management.repository.UserRepository;
import vn.edu.fpt.hotel_management.repository.WishlistRepository;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WishlistSeleniumTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    private String baseUrl;
    private final String inputExcelPath = "wishlist_test_data.xlsx";
    private final String outputExcelPath = "wishlist_test_report.xlsx";

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    public void runWishlistAutomationTest() throws IOException {
        System.out.println("=== STARTING WISHLIST SELENIUM E2E TEST ===");

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
            String scenario = testCase.getOrDefault("Scenario", "ADD").toUpperCase();

            System.out.println("\n--------------------------------------------------");
            System.out.println("Running: " + testCaseId + " | User: " + username + " | Hotel: " + hotelName + " | Room: " + roomType + " | Scenario: " + scenario);

            Map<String, String> reportRow = new HashMap<>(testCase);
            long startTime = System.currentTimeMillis();

            WebDriver driver = null;
            try {
                // 1. Clean up database (except for unauthorized test where user does not log in)
                if (!"UNAUTHORIZED".equals(scenario)) {
                    clearUserWishlist(username);
                }

                // 2. Initialize Driver
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--start-maximized");
                options.addArguments("--remote-allow-origins=*");
                driver = new ChromeDriver(options);
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

                switch (scenario) {
                    case "ADD":
                        // Login -> Go to rooms -> Add -> Verify in Wishlist page
                        login(driver, wait, username, password);
                        goToHotelRooms(driver, wait, hotelName);
                        addRoomToWishlist(driver, wait, roomType);
                        verifyRoomInWishlistPage(driver, wait, hotelName, roomType);
                        reportRow.put("Status", "PASS");
                        reportRow.put("Note", "Successfully added room to wishlist and verified on Wishlist page.");
                        break;

                    case "TOGGLE_REMOVE":
                        // Login -> Go to rooms -> Add -> Click heart again -> Verify wishlist empty
                        login(driver, wait, username, password);
                        goToHotelRooms(driver, wait, hotelName);
                        addRoomToWishlist(driver, wait, roomType);
                        
                        // Click heart again to toggle remove
                        clickHeartButton(driver, wait, roomType);
                        verifyHeartIconIsOutline(driver, wait, roomType);

                        goToWishlistPage(driver, wait);
                        verifyWishlistIsEmpty(driver, wait);
                        reportRow.put("Status", "PASS");
                        reportRow.put("Note", "Successfully removed room from wishlist via toggle on Rooms page.");
                        break;

                    case "REMOVE_PAGE":
                        // Login -> Go to rooms -> Add -> Go to wishlist -> Click red heart -> Verify empty
                        login(driver, wait, username, password);
                        goToHotelRooms(driver, wait, hotelName);
                        addRoomToWishlist(driver, wait, roomType);

                        goToWishlistPage(driver, wait);
                        clickRemoveHeartOnWishlistPage(driver, wait, roomType);
                        verifyWishlistIsEmpty(driver, wait);
                        reportRow.put("Status", "PASS");
                        reportRow.put("Note", "Successfully removed room from wishlist via red heart button on Wishlist page.");
                        break;

                    case "UNAUTHORIZED":
                        // Directly access /hotels -> View rooms -> Verify no heart button rendered
                        driver.get(baseUrl + "/hotels");
                        System.out.println("Accessed hotels list page (unauthorized)");
                        goToHotelRoomsDirect(driver, wait, hotelName);

                        verifyNoHeartButtonDisplayed(driver, wait, roomType);
                        reportRow.put("Status", "PASS");
                        reportRow.put("Note", "Successfully verified that unauthorized users cannot see the wishlist heart button.");
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown scenario type: " + scenario);
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
                    System.out.println("Closed the browser.");
                }
            }
        }

        // Export report to Excel
        System.out.println("\nExporting test report to Excel file: " + outputExcelPath);
        try {
            ExcelHelper.writeTestReport(outputExcelPath, reportList);
            System.out.println("Report exported successfully. Path: " + new File(outputExcelPath).getAbsolutePath());
        } catch (IOException e) {
            System.err.println("\n>>> ERROR WRITING EXCEL REPORT FILE: " + e.getMessage());
            String backupPath = "wishlist_test_report_" + System.currentTimeMillis() + ".xlsx";
            ExcelHelper.writeTestReport(backupPath, reportList);
            System.out.println(">>> Backup test report written to: " + new File(backupPath).getAbsolutePath());
        }
    }

    // Helper methods
    private void login(WebDriver driver, WebDriverWait wait, String username, String password) {
        driver.get(baseUrl + "/login");
        WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement loginBtn = driver.findElement(By.xpath("//button[@type='submit']"));

        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);
        loginBtn.click();
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("avatarBtn")));
        System.out.println("Logged in successfully as user: " + username);
    }

    private void goToHotelRooms(WebDriver driver, WebDriverWait wait, String hotelName) {
        driver.get(baseUrl + "/hotels");
        goToHotelRoomsDirect(driver, wait, hotelName);
    }

    private void goToHotelRoomsDirect(WebDriver driver, WebDriverWait wait, String hotelName) {
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
        System.out.println("Accessed rooms list page for hotel: " + hotelName);
    }

    private void clickHeartButton(WebDriver driver, WebDriverWait wait, String roomType) {
        String heartBtnXPath = String.format(
                "//div[contains(@class, 'room-card') and .//div[contains(@class, 'room-type-title') and contains(text(), '%s')]]/descendant::a[contains(@class, 'wishlist-heart-btn')]",
                roomType
        );
        WebElement heartBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(heartBtnXPath)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", heartBtn);
    }

    private void addRoomToWishlist(WebDriver driver, WebDriverWait wait, String roomType) {
        clickHeartButton(driver, wait, roomType);
        System.out.println("Clicked heart button for room: " + roomType);

        String filledHeartXPath = String.format(
                "//div[contains(@class, 'room-card') and .//div[contains(@class, 'room-type-title') and contains(text(), '%s')]]/descendant::i[contains(@class, 'bi-heart-fill')]",
                roomType
        );
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(filledHeartXPath)));
        System.out.println("Heart icon verified as filled.");
    }

    private void verifyHeartIconIsOutline(WebDriver driver, WebDriverWait wait, String roomType) {
        String outlineHeartXPath = String.format(
                "//div[contains(@class, 'room-card') and .//div[contains(@class, 'room-type-title') and contains(text(), '%s')]]/descendant::i[contains(@class, 'bi-heart') and not(contains(@class, 'bi-heart-fill'))]",
                roomType
        );
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(outlineHeartXPath)));
        System.out.println("Verified heart icon toggled back to outline.");
    }

    private void goToWishlistPage(WebDriver driver, WebDriverWait wait) {
        WebElement avatarBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("avatarBtn")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", avatarBtn);

        WebElement wishlistLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@class, 'avatar-menu-item') and contains(@href, '/wishlist') and not(contains(@href, '/toggle'))]")
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", wishlistLink);
        System.out.println("Navigated to Wishlist page.");
    }

    private void verifyRoomInWishlistPage(WebDriver driver, WebDriverWait wait, String hotelName, String roomType) {
        goToWishlistPage(driver, wait);
        String wishlistRoomXPath = String.format(
                "//div[contains(@class, 'wishlist-room-card') and .//span[contains(@class, 'wl-hotel-name') and contains(text(), '%s')] and .//div[contains(@class, 'room-type-title') and contains(text(), '%s')]]",
                hotelName, roomType
            );
        WebElement savedRoom = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(wishlistRoomXPath)));
        assertTrue(savedRoom.isDisplayed(), "Saved room not found on Wishlist page!");
        System.out.println("Verified room is listed on Wishlist page.");
    }

    private void verifyWishlistIsEmpty(WebDriver driver, WebDriverWait wait) {
        WebElement emptyState = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("wishlist-empty-state")));
        assertTrue(emptyState.isDisplayed(), "Wishlist is not empty!");
        System.out.println("Verified Wishlist page is empty.");
    }

    private void clickRemoveHeartOnWishlistPage(WebDriver driver, WebDriverWait wait, String roomType) {
        String wishlistHeartXPath = String.format(
                "//div[contains(@class, 'wishlist-room-card') and .//div[contains(@class, 'room-type-title') and contains(text(), '%s')]]/descendant::a[contains(@class, 'custom-wishlist-heart-btn')]",
                roomType
        );
        WebElement wishlistHeartBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(wishlistHeartXPath)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", wishlistHeartBtn);
        System.out.println("Clicked red heart button on Wishlist page to remove room: " + roomType);
    }

    private void verifyNoHeartButtonDisplayed(WebDriver driver, WebDriverWait wait, String roomType) {
        String heartBtnXPath = String.format(
                "//div[contains(@class, 'room-card') and .//div[contains(@class, 'room-type-title') and contains(text(), '%s')]]/descendant::a[contains(@class, 'wishlist-heart-btn')]",
                roomType
        );
        List<WebElement> heartButtons = driver.findElements(By.xpath(heartBtnXPath));
        assertTrue(heartButtons.isEmpty(), "Wishlist heart button should NOT be displayed for unauthorized users!");
        System.out.println("Verified that wishlist heart button is NOT displayed for unauthorized user.");
    }

    private void clearUserWishlist(String username) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                Optional<Customer> customerOpt = customerRepository.findByUserAccount(userOpt.get());
                if (customerOpt.isPresent()) {
                    List<Wishlist> list = wishlistRepository.findByCustomerIdOrderByAddedAtDesc(customerOpt.get().getId());
                    if (!list.isEmpty()) {
                        wishlistRepository.deleteAll(list);
                        System.out.println("Cleared wishlist of user: " + username + " in database.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Cannot clean up database before test: " + e.getMessage());
        }
    }
}
