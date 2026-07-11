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
            altInputClass: "form-control form-control-sm filter-input w-100",
            minDate: checkinMinDate,
            allowInput: false,
            defaultDate: checkinVal ? checkinVal : null,
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

        const checkoutPicker = flatpickr(checkoutInput, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            altInputClass: "form-control form-control-sm filter-input w-100",
            minDate: "today",
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
                } else {
                    let queryParams = [];
                    let firstHotelId = null;

                    document.querySelectorAll('.wl-add-btn.active').forEach(function(btn) {
                        const rId = btn.getAttribute('data-roomid');
                        queryParams.push('roomIds=' + rId);
                        if (!firstHotelId) {
                            firstHotelId = btn.getAttribute('data-hotelid');
                        }
                        
                        const card = btn.closest('.wishlist-room-card');
                        let ci = btn.getAttribute('data-checkin');
                        let co = btn.getAttribute('data-checkout');
                        if (card) {
                            const ciInput = card.querySelector('.wl-checkin-picker');
                            const coInput = card.querySelector('.wl-checkout-picker');
                            if (ciInput && ciInput.value) {
                                ci = ciInput.value;
                            }
                            if (coInput && coInput.value) {
                                co = coInput.value;
                            }
                        }
                        
                        queryParams.push('checkins=' + (ci ? ci : ''));
                        queryParams.push('checkouts=' + (co ? co : ''));
                        queryParams.push('quantities=1');
                    });

                    let url = '/booking';
                    if (firstHotelId) {
                        url += '?hotelId=' + firstHotelId;
                    }
                    if (queryParams.length > 0) {
                        url += (url.includes('?') ? '&' : '?') + queryParams.join('&');
                    }

                    bookBtn.setAttribute('href', url);
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



    // ==========================================
    // 6. ROOM CREATE VALIDATION
    // ==========================================
    const roomForm = document.getElementById("roomForm");
    if (roomForm) {
        roomForm.addEventListener("submit", function(event) {
            let isValid = true;
            let firstErrorElement = null;

            // 1. Kiểm tra HTML5 validation mặc định
            const inputs = roomForm.querySelectorAll("input[required], textarea[required]");
            inputs.forEach(input => {
                input.classList.remove("is-invalid");
                if (!input.checkValidity()) {
                    isValid = false;
                    input.classList.add("is-invalid");
                    if (!firstErrorElement) {
                        firstErrorElement = input;
                    }
                }
            });

            // 2. Kiểm tra ít nhất 1 Bathroom Amenity được chọn
            const bathroomCheckboxes = document.querySelectorAll('#bathroomAmenitiesSection input[type="checkbox"]');
            const isBathroomSelected = Array.from(bathroomCheckboxes).some(cb => cb.checked);
            const bathroomTitle = document.querySelector('#bathroomAmenitiesSection .facility-section-title');
            
            if (bathroomTitle) {
                bathroomTitle.style.color = "";
                bathroomTitle.style.borderBottom = "";
                const oldBathErr = document.getElementById("bath-err-msg");
                if (oldBathErr) oldBathErr.remove();

                if (!isBathroomSelected) {
                    isValid = false;
                    bathroomTitle.style.color = "var(--danger, #dc3545)";
                    bathroomTitle.style.borderBottom = "2px solid var(--danger, #dc3545)";
                    
                    const errMsg = document.createElement("div");
                    errMsg.id = "bath-err-msg";
                    errMsg.className = "text-danger small mt-2 fw-semibold";
                    errMsg.innerText = "Please select at least 1 bathroom amenity.";
                    bathroomTitle.parentNode.appendChild(errMsg);

                    if (!firstErrorElement) {
                        firstErrorElement = bathroomTitle;
                    }
                }
            }

            // 3. Kiểm tra ít nhất 1 Room Amenity được chọn
            const roomCheckboxes = document.querySelectorAll('#roomAmenitiesSection input[type="checkbox"]');
            const isRoomSelected = Array.from(roomCheckboxes).some(cb => cb.checked);
            const roomTitle = document.querySelector('#roomAmenitiesSection .facility-section-title');
            
            if (roomTitle) {
                roomTitle.style.color = "";
                roomTitle.style.borderBottom = "";
                const oldRoomErr = document.getElementById("room-err-msg");
                if (oldRoomErr) oldRoomErr.remove();

                if (!isRoomSelected) {
                    isValid = false;
                    roomTitle.style.color = "var(--danger, #dc3545)";
                    roomTitle.style.borderBottom = "2px solid var(--danger, #dc3545)";
                    
                    const errMsg = document.createElement("div");
                    errMsg.id = "room-err-msg";
                    errMsg.className = "text-danger small mt-2 fw-semibold";
                    errMsg.innerText = "Please select at least 1 room amenity.";
                    roomTitle.parentNode.appendChild(errMsg);

                    if (!firstErrorElement) {
                        firstErrorElement = roomTitle;
                    }
                }
            }

            // Nếu form không hợp lệ, chặn submit, tự động scroll đến phần tử lỗi đầu tiên và hiển thị thông báo
            if (!isValid) {
                event.preventDefault();
                if (firstErrorElement) {
                    firstErrorElement.scrollIntoView({ behavior: "smooth", block: "center" });
                    
                    setTimeout(() => {
                        if (typeof firstErrorElement.focus === "function") {
                            firstErrorElement.focus();
                        }
                        if (typeof firstErrorElement.reportValidity === "function") {
                            firstErrorElement.reportValidity();
                        }
                    }, 400); // Đợi scroll chạy xong rồi hiển thị tooltip lỗi
                }
            }
        });
    }

    // Auto expand if any hidden checkbox is already checked (loaded from filter session/URL)
    ['hotelFacilitiesContainer', 'hotelViewsContainer', 'roomFacilitiesContainer'].forEach(id => {
        const container = document.getElementById(id);
        if (container) {
            const hiddenChecked = container.querySelectorAll('.hidden-filter-item input[type="checkbox"]:checked');
            if (hiddenChecked.length > 0) {
                const btn = container.nextElementSibling;
                if (btn && btn.classList.contains('show-more-btn')) {
                    // Gọi hàm qua scope window để bảo đảm tương thích
                    window.toggleFilterGroup(btn, id);
                }
            }
        }
    });

});

// ==========================================
// 7. TOGGLE FILTER GROUP (Show more / Show less)
// ==========================================
window.toggleFilterGroup = function(button, containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;
    const hiddenItems = container.querySelectorAll('.hidden-filter-item');
    const isExpanded = button.getAttribute('data-expanded') === 'true';
    
    if (isExpanded) {
        hiddenItems.forEach(item => item.style.setProperty('display', 'none', 'important'));
        button.setAttribute('data-expanded', 'false');
        const remainingCount = hiddenItems.length;
        button.innerHTML = `Show all (+${remainingCount}) <i class="bi bi-chevron-down ms-1"></i>`;
    } else {
        hiddenItems.forEach(item => item.style.setProperty('display', 'flex', 'important'));
        button.setAttribute('data-expanded', 'true');
        button.innerHTML = `Show less <i class="bi bi-chevron-up ms-1"></i>`;
    }
};

