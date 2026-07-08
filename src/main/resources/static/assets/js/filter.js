/**
 * Unified filter.js for managing Flatpickr date pickers and Dual Range Slider
 * without writing inline Javascript in HTML files.
 */
document.addEventListener("DOMContentLoaded", function() {
    
    // ==========================================
    // 1. DUAL RANGE SLIDER (Only for hotel-list)
    // ==========================================
    const sliderOne = document.getElementById("slider-1");
    const sliderTwo = document.getElementById("slider-2");
    
    if (sliderOne && sliderTwo) {
        const minPriceInput = document.getElementById("minPriceInput");
        const maxPriceInput = document.getElementById("maxPriceInput");
        const displayValOne = document.getElementById("range1");
        const displayValTwo = document.getElementById("range2");
        const minGap = 200000; // 200,000 VND
        const sliderTrack = document.querySelector(".slider-track");

        function formatCurrency(value) {
            return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value);
        }

        function fillColor() {
            let val1 = parseInt(sliderOne.value);
            let val2 = parseInt(sliderTwo.value);
            let percent1 = ((val1 - 100000) / (50000000 - 100000)) * 100;
            let percent2 = ((val2 - 100000) / (50000000 - 100000)) * 100;
            if (sliderTrack) {
                sliderTrack.style.background = `linear-gradient(to right, #ced4da ${percent1}% , var(--gold) ${percent1}% , var(--gold) ${percent2}%, #ced4da ${percent2}%)`;
            }
        }

        function slideOne() {
            let val1 = parseInt(sliderOne.value);
            let val2 = parseInt(sliderTwo.value);
            if (val2 - val1 <= minGap) {
                sliderOne.value = val2 - minGap;
            }
            if (minPriceInput) minPriceInput.value = sliderOne.value;
            if (displayValOne) displayValOne.textContent = formatCurrency(sliderOne.value);
            fillColor();
            sliderOne.style.zIndex = "10";
            sliderTwo.style.zIndex = "0";
        }

        function slideTwo() {
            let val1 = parseInt(sliderOne.value);
            let val2 = parseInt(sliderTwo.value);
            if (val2 - val1 <= minGap) {
                sliderTwo.value = val1 + minGap;
            }
            if (maxPriceInput) maxPriceInput.value = sliderTwo.value;
            if (displayValTwo) displayValTwo.textContent = formatCurrency(sliderTwo.value);
            fillColor();
            sliderTwo.style.zIndex = "10";
            sliderOne.style.zIndex = "0";
        }

        sliderOne.addEventListener("input", slideOne);
        sliderTwo.addEventListener("input", slideTwo);

        // Initial call
        slideOne();
        slideTwo();
    }

    // ==========================================
    // 2. FLATPICKR DATE PICKER FOR SEARCH SIDEBAR
    // ==========================================
    const checkinInput = document.getElementById("filterCheckin");
    const checkoutInput = document.getElementById("filterCheckout");

    if (checkinInput && checkoutInput && typeof flatpickr === "function") {
        const checkinVal = checkinInput.value;
        const checkoutVal = checkoutInput.value;
        const checkinMinDate = checkinInput.getAttribute("data-min-date") || "today";
        const checkoutMinDate = checkoutInput.getAttribute("data-min-date") || "today";

        const checkinPicker = flatpickr(checkinInput, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            altInputClass: checkinInput.classList.contains("form-control-sm")
                ? "form-control form-control-sm filter-input w-100"
                : "filter-input w-100",
            minDate: checkinMinDate,
            allowInput: false,
            defaultDate: checkinVal ? checkinVal : null,
            onChange: function(selectedDates, dateStr) {
                if (selectedDates[0]) {
                    const nextDay = new Date(selectedDates[0]);
                    nextDay.setDate(nextDay.getDate() + 1);
                    if (checkoutPicker) {
                        checkoutPicker.set("minDate", nextDay);
                        const currentCheckout = checkoutPicker.selectedDates[0];
                        if (currentCheckout && currentCheckout <= selectedDates[0]) {
                            checkoutPicker.setDate(nextDay);
                        }
                    }
                }
            }
        });

        const checkoutPicker = flatpickr(checkoutInput, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            altInputClass: checkoutInput.classList.contains("form-control-sm")
                ? "form-control form-control-sm filter-input w-100"
                : "filter-input w-100",
            minDate: checkoutMinDate,
            allowInput: false,
            defaultDate: checkoutVal ? checkoutVal : null
        });
    }

    // ==========================================
    // 3. FLATPICKR DATE PICKER FOR WISHLIST CARDS
    // ==========================================
    const wlForms = document.querySelectorAll('.wishlist-card-filter-form');
    if (wlForms.length > 0 && typeof flatpickr === "function") {
        wlForms.forEach(form => {
            const checkinInp  = form.querySelector('input[name="checkin"]');
            const checkoutInp = form.querySelector('input[name="checkout"]');

            if (checkinInp && checkoutInp) {

                // Hàm parse ngày local (tránh UTC midnight lệch múi giờ)
                function parseLocal(str) {
                    if (!str) return null;
                    const p = str.split('-');
                    if (p.length !== 3) return null;
                    return new Date(+p[0], +p[1] - 1, +p[2]);
                }

                // Giá trị checkin/checkout từ HTML (đã được Thymeleaf fill sẵn)
                const ciVal  = checkinInp.value  || '';
                const coVal  = checkoutInp.value || '';

                // Tính ngày mặc định
                const checkinDate  = parseLocal(ciVal)  || new Date();
                const tomorrowDate = new Date(checkinDate);
                tomorrowDate.setDate(tomorrowDate.getDate() + 1);
                const checkoutDate = parseLocal(coVal) || tomorrowDate;

                const checkinPicker = flatpickr(checkinInp, {
                    dateFormat: "Y-m-d",
                    altInput: true,
                    altFormat: "d/m/Y",
                    altInputClass: "form-control form-control-sm w-100",
                    minDate: "today",
                    allowInput: false,
                    defaultDate: checkinDate,
                    onChange: function(selectedDates) {
                        if (selectedDates[0]) {
                            const next = new Date(selectedDates[0]);
                            next.setDate(next.getDate() + 1);
                            checkoutPicker.set("minDate", next);
                            const cur = checkoutPicker.selectedDates[0];
                            if (!cur || cur <= selectedDates[0]) {
                                checkoutPicker.setDate(next);
                            }
                        }
                    }
                });

                const checkoutPicker = flatpickr(checkoutInp, {
                    dateFormat: "Y-m-d",
                    altInput: true,
                    altFormat: "d/m/Y",
                    altInputClass: "form-control form-control-sm w-100",
                    minDate: tomorrowDate,
                    allowInput: false,
                    defaultDate: checkoutDate
                });
            }
        });


    }

    // ==========================================
    // 4. WISHLIST — Estimated Total (Tính tổng giá)
    //    Dùng Bootstrap data-bs-toggle="button"
    //    Khi nút active → cộng giá; inactive → trừ giá
    // ==========================================
    const wlContainer = document.getElementById('wishlistContainer');
    if (wlContainer) {
        const totalEl  = document.getElementById('wlTotalAmount');
        const countEl  = document.getElementById('wlSelectedCount');
        const bookBtn  = document.getElementById('wlBookNowBtn');
        const GOLD     = getComputedStyle(document.documentElement).getPropertyValue('--gold').trim() || '#b45309';

        function formatVND(n) {
            return Math.round(n).toLocaleString('vi-VN');
        }

        function recalcTotal() {
            let total = 0, count = 0, lastBtn = null;
            document.querySelectorAll('.wl-add-btn.active').forEach(function(btn) {
                total += parseFloat(btn.getAttribute('data-price')) || 0;
                count++;
                lastBtn = btn;
            });

            if (totalEl) totalEl.textContent = formatVND(total);
            if (countEl) countEl.textContent = '(' + count + ' room' + (count !== 1 ? 's' : '') + ' selected)';

            if (bookBtn) {
                if (count === 0) {
                    bookBtn.setAttribute('href', '#');
                    bookBtn.classList.add('wl-book-disabled');
                } else if (count === 1 && lastBtn) {
                    const url = '/booking?hotelId=' + lastBtn.getAttribute('data-hotelid')
                        + '&roomId='   + lastBtn.getAttribute('data-roomid')
                        + (lastBtn.getAttribute('data-checkin')  ? '&checkin='  + lastBtn.getAttribute('data-checkin')  : '')
                        + (lastBtn.getAttribute('data-checkout') ? '&checkout=' + lastBtn.getAttribute('data-checkout') : '');
                    bookBtn.setAttribute('href', url);
                    bookBtn.classList.remove('wl-book-disabled');
                } else {
                    bookBtn.setAttribute('href', '#');
                    bookBtn.classList.remove('wl-book-disabled');
                }
            }
        }

        // Gắn sự kiện cho từng nút Add (Bootstrap toggle button phát ra click)
        document.querySelectorAll('.wl-add-btn').forEach(function(btn) {
            btn.addEventListener('click', function() {
                // Bootstrap toggle button cập nhật class 'active' SAU click
                // Dùng setTimeout 0 để đọc sau khi Bootstrap xử lý xong
                setTimeout(recalcTotal, 0);
            });
        });

        recalcTotal();
    }

});
