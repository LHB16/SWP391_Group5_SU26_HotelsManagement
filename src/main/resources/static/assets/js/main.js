// File JavaScript xử lý sự kiện hiển thị/ẩn menu thả của Avatar

document.addEventListener('DOMContentLoaded', function() {
    // Tự động khởi tạo Lucide icons nếu thư viện có sẵn
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }

    const avatarBtn = document.getElementById('avatarBtn');
    const avatarMenu = document.getElementById('avatarMenu');

    if (avatarBtn && avatarMenu) {
        // Sự kiện click vào avatar để đóng/mở menu
        avatarBtn.addEventListener('click', function(e) {
            e.stopPropagation(); // Ngăn chặn sự kiện nổi bọt lên document
            avatarMenu.classList.toggle('show');
        });

        // Click bất kỳ đâu ngoài menu để đóng menu lại
        document.addEventListener('click', function(e) {
            if (!avatarMenu.contains(e.target) && e.target !== avatarBtn) {
                avatarMenu.classList.remove('show');
            }
        });
    }
});
