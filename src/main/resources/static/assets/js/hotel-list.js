/**
 * hotel-list.js
 * JavaScript cho trang danh sách khách sạn (/hotels)
 * Xử lý: Multi-select button sao + Dual range slider khoảng giá
 */

// Chờ DOM load xong mới chạy
document.addEventListener('DOMContentLoaded', function () {

    // ═══════════════════════════════════════════════════
    // 1. XỬ LÝ MULTI-SELECT BUTTON CHỌN SỐ SAO
    // ═══════════════════════════════════════════════════

    // Lấy tất cả button sao và container chứa hidden inputs
    var starButtons      = document.querySelectorAll('.hotel-star-btn');
    var ratingsContainer = document.getElementById('ratingsContainer');

    /**
     * Cập nhật các hidden input <input name="ratings" value="...">
     * dựa trên các button đang active (trừ nút "All").
     * Khi form submit, chỉ gửi giá trị của những button active.
     */
    function updateRatingsInputs() {
        // Xóa hết hidden inputs cũ
        ratingsContainer.innerHTML = '';

        // Lấy tất cả button active KHÔNG phải "All" (data-value != 0)
        var activeButtons = document.querySelectorAll('.hotel-star-btn.active:not([data-value="0"])');

        // Tạo 1 hidden input cho mỗi button active
        activeButtons.forEach(function (btn) {
            var input   = document.createElement('input');
            input.type  = 'hidden';
            input.name  = 'ratings';
            input.value = btn.getAttribute('data-value');
            ratingsContainer.appendChild(input);
        });
    }

    // Gắn sự kiện click cho từng button sao
    starButtons.forEach(function (btn) {
        btn.addEventListener('click', function () {
            var value  = this.getAttribute('data-value');
            var allBtn = document.querySelector('.hotel-star-btn[data-value="0"]');

            if (value === '0') {
                // ── Click nút "All" ──
                // → Tắt hết các button sao cụ thể, bật "All"
                starButtons.forEach(function (b) { b.classList.remove('active'); });
                allBtn.classList.add('active');
            } else {
                // ── Click nút sao cụ thể (1★ - 5★) ──

                // Tắt nút "All" vì đang chọn sao cụ thể
                allBtn.classList.remove('active');

                // Toggle trạng thái active của button vừa click
                this.classList.toggle('active');

                // Kiểm tra: nếu không còn button sao nào active → bật lại "All"
                var anyActive = document.querySelector('.hotel-star-btn.active:not([data-value="0"])');
                if (!anyActive) {
                    allBtn.classList.add('active');
                }
            }

            // Cập nhật hidden inputs sau mỗi lần click
            updateRatingsInputs();
        });
    });

    // ═══════════════════════════════════════════════════
    // 2. XỬ LÝ DUAL RANGE SLIDER KHOẢNG GIÁ
    // ═══════════════════════════════════════════════════

    var minSlider      = document.getElementById('minPriceSlider');
    var maxSlider      = document.getElementById('maxPriceSlider');
    var minDisplay     = document.getElementById('minPriceDisplay');
    var maxDisplay     = document.getElementById('maxPriceDisplay');
    var minInput       = document.getElementById('minPriceInput');
    var maxInput       = document.getElementById('maxPriceInput');
    var rangeHighlight = document.getElementById('rangeHighlight');

    // Giá trị min/max cố định của slider
    var SLIDER_MIN = 0;
    var SLIDER_MAX = 50000000;

    /**
     * Định dạng số tiền VND (ví dụ: 350,000 VND)
     */
    function formatVND(value) {
        return parseInt(value).toLocaleString('en-US') + ' VND';
    }

    /**
     * Tính vị trí phần trăm (%) trên thanh slider
     */
    function getPercent(value) {
        return ((value - SLIDER_MIN) / (SLIDER_MAX - SLIDER_MIN)) * 100;
    }

    /**
     * Cập nhật giao diện slider:
     * - Thanh highlight giữa 2 con trỏ
     * - Label hiển thị giá trị
     * - Hidden inputs cho form
     */
    function updateRange() {
        var minVal = parseInt(minSlider.value);
        var maxVal = parseInt(maxSlider.value);

        // Đảm bảo min không vượt quá max
        if (minVal > maxVal) {
            var temp = minVal;
            minVal = maxVal;
            maxVal = temp;
            minSlider.value = minVal;
            maxSlider.value = maxVal;
        }

        // Cập nhật thanh highlight giữa 2 con trỏ
        var minPercent = getPercent(minVal);
        var maxPercent = getPercent(maxVal);
        rangeHighlight.style.left  = minPercent + '%';
        rangeHighlight.style.width = (maxPercent - minPercent) + '%';

        // Cập nhật text hiển thị phía trên
        minDisplay.textContent = formatVND(minVal);
        maxDisplay.textContent = formatVND(maxVal);

        // Cập nhật hidden inputs để gửi giá trị khi submit form
        minInput.value = minVal;
        maxInput.value = maxVal;
    }

    // Lắng nghe sự kiện kéo slider
    minSlider.addEventListener('input', updateRange);
    maxSlider.addEventListener('input', updateRange);

    // Gọi ngay khi trang tải để đồng bộ giao diện với giá trị từ server
    updateRange();

});
