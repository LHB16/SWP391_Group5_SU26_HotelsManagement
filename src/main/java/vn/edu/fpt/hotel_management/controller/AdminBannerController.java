package vn.edu.fpt.hotel_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.hotel_management.entity.Banner;
import vn.edu.fpt.hotel_management.entity.User;
import vn.edu.fpt.hotel_management.repository.BannerRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Controller
public class AdminBannerController {

    @Autowired
    private BannerRepository bannerRepository;

    @PostMapping("/admin/banners/add")
    public String addBanner(
            @RequestParam(value = "bannerImage", required = false) MultipartFile file,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "linkUrl", required = false) String linkUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        String finalImageUrl = "";

        if (file != null && !file.isEmpty()) {
            try {
                String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                
                // Save to src/main/resources/static/assets/images/banners
                Path sourcePath = Paths.get("src/main/resources/static/assets/images/banners").toAbsolutePath().normalize();
                if (!Files.exists(sourcePath)) {
                    Files.createDirectories(sourcePath);
                }
                Path sourceTarget = sourcePath.resolve(fileName);
                Files.copy(file.getInputStream(), sourceTarget, StandardCopyOption.REPLACE_EXISTING);

                // Save to target/classes/static/assets/images/banners (for dynamic reload during development)
                Path classesPath = Paths.get("target/classes/static/assets/images/banners").toAbsolutePath().normalize();
                if (Files.exists(Paths.get("target/classes/static"))) {
                    if (!Files.exists(classesPath)) {
                        Files.createDirectories(classesPath);
                    }
                    Path classesTarget = classesPath.resolve(fileName);
                    Files.copy(file.getInputStream(), classesTarget, StandardCopyOption.REPLACE_EXISTING);
                }

                finalImageUrl = "/assets/images/banners/" + fileName;
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload banner image: " + e.getMessage());
                return "redirect:/admin/dashboard?tab=bannerPanel";
            }
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            finalImageUrl = imageUrl.trim();
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Please provide a banner image file or image URL.");
            return "redirect:/admin/dashboard?tab=bannerPanel";
        }

        Banner banner = new Banner();
        banner.setImageUrl(finalImageUrl);
        banner.setTitle(title != null ? title.trim() : "");
        banner.setLinkUrl(linkUrl != null ? linkUrl.trim() : "");
        banner.setActive(true);

        bannerRepository.save(banner);

        redirectAttributes.addFlashAttribute("successMessage", "Banner added successfully!");
        return "redirect:/admin/dashboard?tab=bannerPanel";
    }

    @PostMapping("/admin/banners/toggle")
    public String toggleBanner(
            @RequestParam("id") int id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        Banner banner = bannerRepository.findById(id).orElse(null);
        if (banner != null) {
            banner.setActive(!banner.isActive());
            bannerRepository.save(banner);
            redirectAttributes.addFlashAttribute("successMessage", "Banner visibility toggled successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Banner not found.");
        }

        return "redirect:/admin/dashboard?tab=bannerPanel";
    }

    @PostMapping("/admin/banners/delete")
    public String deleteBanner(
            @RequestParam("id") int id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        Banner banner = bannerRepository.findById(id).orElse(null);
        if (banner != null) {
            bannerRepository.delete(banner);
            redirectAttributes.addFlashAttribute("successMessage", "Banner deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Banner not found.");
        }

        return "redirect:/admin/dashboard?tab=bannerPanel";
    }
}
