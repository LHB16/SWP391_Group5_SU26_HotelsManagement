package vn.edu.fpt.hotel_management;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HotelManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(HotelManagementApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				jdbcTemplate.execute("ALTER TABLE payments DROP CONSTRAINT IF EXISTS CK_payments_method");
			} catch (Exception e) {
				// Ignore
			}
			try {
				jdbcTemplate.execute(
						"ALTER TABLE payments ADD CONSTRAINT CK_payments_method CHECK (method IS NULL OR method IN (N'QR_CODE', N'CASH', N'BANK_TRANSFER', N'MOMO', N'VNPAY', N'PAYPAL'))");
			} catch (Exception e) {
				System.err.println(
						">>> [DATABASE SETUP] Error configuring CK_payments_method constraint: " + e.getMessage());
			}
		};
	}
}