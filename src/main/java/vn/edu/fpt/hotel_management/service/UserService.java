// src/main/java/vn/edu/fpt/hotel_management/service/UserService.java
package vn.edu.fpt.hotel_management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.UserRepository;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public void register(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã được sử dụng!");
        }
        User user = new User(username, password, email, "CUSTOMER");
        userRepository.save(user);
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Username or password incorrect!"));
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Username or password incorrect!");
        }
        return user;
    }
}