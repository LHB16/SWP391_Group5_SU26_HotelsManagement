package vn.edu.fpt.hotel_management.selenium;

import org.junit.jupiter.api.AfterEach;
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

        // Kiểm tra file excel input tồn tại
        File inputFile = new File(inputExcelPath);
        if (!inputFile.exists()) {
            System.out.println(">>> SKIP TEST: File dữ liệu test '" + inputExcelPath + "' không tồn tại.");
            return;
        }

        // Đọc dữ liệu từ file Excel
        List<Map<String, String>> testCases = ExcelHelper.readTestData(inputExcelPath);
        if (testCases == null || testCases.isEmpty()) {
            System.out.println(">>> SKIP TEST: Không tìm thấy dữ liệu kiểm thử nào trong file: " + inputExcelPath);
            return;
        }
        List<Map<String, String>> reportList = new ArrayList<>();

        System.out.println("Số lượng testcase đọc được từ Excel: " + testCases.size());

        for (Map<String, String> testCase : testCases) {
            String testCaseId = testCase.getOrDefault("TestCaseID", "TC_Unknown");
            String username = testCase.getOrDefault("Username", "");
            String password = testCase.getOrDefault("Password", "");
            String hotelName = testCase.getOrDefault("HotelName", "");
            String roomType = testCase.getOrDefault("RoomType", "");
            String expectedStatus = testCase.getOrDefault("ExpectedStatus", "SUCCESS");

            System.out.println("\n--------------------------------------------------");
            System.out.println("Đang chạy: " + testCaseId + " | User: " + username + " | Hotel: " + hotelName + " | Room: " + roomType);

            Map<String, String> reportRow = new HashMap<>(testCase);
            long startTime = System.currentTimeMillis();

            WebDriver driver = null;
            try {
                // 1. Dọn dẹp trạng thái wishlist của user trước khi chạy test để đảm bảo tính độc lập
                clearUserWishlist(username);

                // 2. Khởi tạo Selenium Driver (Chrome)
                ChromeOptions options = new ChromeOptions();
                // options.addArguments("--headless=new"); // Bỏ comment nếu muốn chạy ẩn không giao diện
                options.addArguments("--start-maximized");
                options.addArguments("--remote-allow-origins=*");
                driver = new ChromeDriver(options);
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

                // 3. Mở trang login
                driver.get(baseUrl + "/login");
                System.out.println("Đã truy cập trang login");

                // 4. Nhập form login
                WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
                WebElement passwordInput = driver.findElement(By.id("password"));
                WebElement loginBtn = driver.findElement(By.xpath("//button[@type='submit']"));

                usernameInput.sendKeys(username);
                passwordInput.sendKeys(password);
                loginBtn.click();
                System.out.println("Đã bấm submit form login");

                // Xác thực đăng nhập thành công (Thanh điều hướng có avatarBtn)
                WebElement avatarBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("avatarBtn")));
                System.out.println("Đăng nhập thành công với user: " + avatarBtn.getText());

                // 5. Vào trang hotel list
                driver.get(baseUrl + "/hotels");
                System.out.println("Đã truy cập trang danh sách khách sạn /hotels");

                // 6. Tìm khách sạn được chỉ định và click "View Rooms"
                String viewRoomsXPath = String.format(
                        "//div[contains(@class, 'hotel-card') and .//h5[contains(@class, 'card-title') and contains(text(), '%s')]]/descendant::a[contains(@class, 'custom-hl-view-btn')]",
                        hotelName
                );
                WebElement viewRoomsBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(viewRoomsXPath)));
                
                String parentWindow = driver.getWindowHandle();
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", viewRoomsBtn);
                System.out.println("Đã click View Rooms khách sạn: " + hotelName);

                // 7. Chuyển sang tab mới (do target="_blank")
                wait.until(d -> d.getWindowHandles().size() > 1);
                for (String handle : driver.getWindowHandles()) {
                    if (!handle.equals(parentWindow)) {
                        driver.switchTo().window(handle);
                        break;
                    }
                }
                System.out.println("Đã chuyển tab sang trang chi tiết phòng");

                // 8. Tìm phòng được chỉ định và click nút trái tim thêm vào wishlist
                String heartBtnXPath = String.format(
                        "//div[contains(@class, 'room-card') and .//div[contains(@class, 'room-type-title') and contains(text(), '%s')]]/descendant::a[contains(@class, 'wishlist-heart-btn')]",
                        roomType
                );
                WebElement heartBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(heartBtnXPath)));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", heartBtn);
                System.out.println("Đã click nút trái tim của phòng: " + roomType);

                // Chờ trang tải lại và nút trái tim được fill (bi-heart-fill)
                String filledHeartXPath = String.format(
                        "//div[contains(@class, 'room-card') and .//div[contains(@class, 'room-type-title') and contains(text(), '%s')]]/descendant::i[contains(@class, 'bi-heart-fill')]",
                        roomType
                );
                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(filledHeartXPath)));
                System.out.println("Đã xác nhận icon trái tim chuyển sang dạng fill (đỏ/hồng)");

                // 9. Vào trang wishlist bằng cách click qua giao diện (Avatar Dropdown -> Wishlist)
                avatarBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("avatarBtn")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", avatarBtn);
                System.out.println("Đã click mở menu avatar dropdown");

                WebElement wishlistLink = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//a[contains(@class, 'avatar-menu-item') and contains(@href, '/wishlist') and not(contains(@href, '/toggle'))]")
                ));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", wishlistLink);
                System.out.println("Đã click chọn menu 'Wishlist'");

                // 10. Xác nhận trang wishlist chứa phòng được chỉ định
                String wishlistRoomXPath = String.format(
                        "//div[contains(@class, 'wishlist-room-card') and .//span[contains(@class, 'wl-hotel-name') and contains(text(), '%s')] and .//div[contains(@class, 'room-type-title') and contains(text(), '%s')]]",
                        hotelName, roomType
                );
                WebElement savedRoom = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(wishlistRoomXPath)));
                
                assertTrue(savedRoom.isDisplayed(), "Không tìm thấy phòng đã lưu trong danh sách Wishlist!");
                System.out.println("THÀNH CÔNG: Tìm thấy phòng " + roomType + " của khách sạn " + hotelName + " trong Wishlist!");

                // Ghi nhận kết quả PASS
                reportRow.put("Status", "PASS");
                reportRow.put("Note", "Thêm phòng vào wishlist thành công thông qua UI.");

            } catch (Exception e) {
                System.err.println("Lỗi khi chạy testcase " + testCaseId + ": " + e.getMessage());
                reportRow.put("Status", "FAIL");
                reportRow.put("Note", "Lỗi: " + e.getMessage());
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                reportRow.put("ExecutionTime", String.valueOf(duration));
                reportList.add(reportRow);

                if (driver != null) {
                    driver.quit();
                    System.out.println("Đã đóng trình duyệt.");
                }
            }
        }

        // Xuất báo cáo ra file Excel
        System.out.println("\nĐang xuất báo cáo kết quả ra file Excel: " + outputExcelPath);
        try {
            ExcelHelper.writeTestReport(outputExcelPath, reportList);
            System.out.println("Báo cáo xuất thành công. Đường dẫn: " + new File(outputExcelPath).getAbsolutePath());
        } catch (IOException e) {
            System.err.println("\n>>> LỖI GHI FILE BÁO CÁO EXCEL: " + e.getMessage());
            System.err.println(">>> Vui lòng ĐÓNG file '" + outputExcelPath + "' nếu đang mở bằng Excel/WPS Office và chạy lại test.");
            
            // Tạo file backup dự phòng để không mất kết quả test
            String backupPath = "wishlist_test_report_" + System.currentTimeMillis() + ".xlsx";
            try {
                ExcelHelper.writeTestReport(backupPath, reportList);
                System.out.println(">>> Đã ghi báo cáo kết quả dự phòng thành công tại: " + new File(backupPath).getAbsolutePath());
            } catch (IOException ex) {
                System.err.println(">>> Không thể ghi file báo cáo dự phòng: " + ex.getMessage());
            }
        }
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
                        System.out.println("Đã clear wishlist của user: " + username + " trong database.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Không thể dọn dẹp database trước khi test: " + e.getMessage());
        }
    }
}
