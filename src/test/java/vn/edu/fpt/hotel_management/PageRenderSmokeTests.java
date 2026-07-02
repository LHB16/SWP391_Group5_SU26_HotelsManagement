package vn.edu.fpt.hotel_management;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PageRenderSmokeTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void publicPagesRender() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/hotels"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/hotels/1/rooms"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk());
    }

    @Test
    void adminPagesRender() throws Exception {
        MockHttpSession session = sessionFor("admin@gmail.com");

        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/dashboard").param("tab", "customerPanel").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/customer-detail").param("id", "1").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void profileAndOwnerPagesRender() throws Exception {
        mockMvc.perform(get("/profile").session(sessionFor("customer@gmail.com")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/owner/dashboard").session(sessionFor("owner@gmail.com")))
                .andExpect(status().isOk());
    }

    private MockHttpSession sessionFor(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Missing seed user " + email));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", user);
        return session;
    }
}
