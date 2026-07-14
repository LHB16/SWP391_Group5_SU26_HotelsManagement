document.addEventListener("DOMContentLoaded", function () {
    // 0. Hàm helper định dạng ngày tháng sang dd/MM/yyyy
    function formatDateToDMY(dateStr) {
        if (!dateStr) return "N/A";
        // Nếu đã có dạng dd/MM/yyyy sẵn (chứa dấu /)
        if (dateStr.includes("/")) {
            const parts = dateStr.split("/");
            if (parts.length === 3) {
                let day = parts[0].padStart(2, '0');
                let month = parts[1].padStart(2, '0');
                let year = parts[2];
                if (parts[0].length === 4) { // yyyy/MM/dd
                    year = parts[0];
                    day = parts[2].padStart(2, '0');
                }
                return `${day}/${month}/${year}`;
            }
        }
        // Nếu có dạng yyyy-MM-dd
        if (dateStr.includes("-")) {
            const parts = dateStr.split("-");
            if (parts.length === 3) {
                let day = parts[2].padStart(2, '0');
                let month = parts[1].padStart(2, '0');
                let year = parts[0];
                if (parts[0].length === 2 || parts[0].length === 1) { // dd-MM-yyyy
                    day = parts[0].padStart(2, '0');
                    year = parts[2];
                }
                return `${day}/${month}/${year}`;
            }
        }
        return dateStr;
    }

    // 1. Logic Modal Special Requests cho từng phòng
    const requestBtns = document.querySelectorAll(".booking-special-request-btn");
    const modalEl = document.getElementById('specialRequestsModal');
    let modal = null;

    function getSpecialRequestsModal() {
        if (!modal && window.bootstrap && window.bootstrap.Modal) {
            modal = new bootstrap.Modal(modalEl);
        }
        return modal;
    }

    const modalTitle = document.querySelector(".room-target-title");
    const modalTextarea = document.getElementById("modalSpecialRequestsTextarea");
    const modalSubmitBtn = document.getElementById("modalSubmitBtn");
    const modalResetBtn = document.getElementById("modalResetBtn");

    let activeRoomId = null;
    let activeBtn = null;

    requestBtns.forEach(btn => {
        btn.addEventListener("click", function (e) {
            e.preventDefault();
            activeRoomId = this.getAttribute("data-room-id");
            const roomName = this.getAttribute("data-room-name");
            activeBtn = this;

            if (modalTitle) modalTitle.textContent = "Room: " + roomName;

            const hiddenInput = document.getElementById("room-req-" + activeRoomId);
            if (modalTextarea) modalTextarea.value = hiddenInput ? hiddenInput.value : "";

            const m = getSpecialRequestsModal();
            if (m) m.show();
        });
    });

    if (modalSubmitBtn) {
        modalSubmitBtn.addEventListener("click", function () {
            if (activeRoomId) {
                const hiddenInput = document.getElementById("room-req-" + activeRoomId);
                const reqValue = modalTextarea ? modalTextarea.value.trim() : "";

                if (hiddenInput) {
                    hiddenInput.value = reqValue;
                }

                if (activeBtn) {
                    if (reqValue !== "") {
                        activeBtn.classList.remove("bg-navy");
                        activeBtn.style.backgroundColor = "#c9a96e"; // Đổi sang màu gold
                        activeBtn.style.borderColor = "#c9a96e";
                        activeBtn.style.color = "#ffffff";
                        activeBtn.innerHTML = '<i class="bi bi-chat-left-text-fill me-1"></i><span>Note added</span>';
                    } else {
                        activeBtn.classList.add("bg-navy");
                        activeBtn.style.backgroundColor = ""; // Về mặc định
                        activeBtn.style.borderColor = "";
                        activeBtn.style.color = "";
                        activeBtn.innerHTML = '<i class="bi bi-chat-left-text me-1"></i><span>Special requests</span>';
                    }
                }

                const m = getSpecialRequestsModal();
                if (m) m.hide();
            }
        });
    }

    if (modalResetBtn) {
        modalResetBtn.addEventListener("click", function () {
            if (activeRoomId) {
                const hiddenInput = document.getElementById("room-req-" + activeRoomId);
                if (hiddenInput) {
                    hiddenInput.value = "";
                }
                if (modalTextarea) modalTextarea.value = "";

                if (activeBtn) {
                    activeBtn.classList.add("bg-navy");
                    activeBtn.style.backgroundColor = "";
                    activeBtn.style.borderColor = "";
                    activeBtn.style.color = "";
                    activeBtn.innerHTML = '<i class="bi bi-chat-left-text me-1"></i><span>Special requests</span>';
                }

                const m = getSpecialRequestsModal();
                if (m) m.hide();
            }
        });
    }

    // 2. Định dạng tiền VND
    function formatVND(amount) {
        return new Intl.NumberFormat('en-US').format(Math.round(amount));
    }

    // 3. Hàm tính toán lại hóa đơn (Invoice Summary)
    function recalculateGrandTotal() {
        const roomPrices = document.querySelectorAll(".booking-price-amount");
        let originalSubtotal = 0;
        let discountTotal = 0;
        const appliedDiscounts = [];

        roomPrices.forEach(span => {
            const roomId = span.id.replace("room-price-", "");
            const basePrice = parseFloat(span.getAttribute("data-base-price"));
            const promoInput = document.getElementById("room-promo-" + roomId);
            const promoId = promoInput ? parseInt(promoInput.value) : 0;

            let roomPrice = basePrice;
            let roomDiscount = 0;
            let promoTitle = "";
            if (promoId > 0 && typeof hotelPromotions !== "undefined") {
                const promo = hotelPromotions.find(p => p.id === promoId);
                if (promo) {
                    const discountRate = parseFloat(promo.discountPercent) / 100;
                    roomPrice = basePrice * (1 - discountRate);
                    roomDiscount = basePrice * discountRate;
                    promoTitle = promo.title;
                }
            }

            // Cập nhật giá hiển thị của phòng
            span.textContent = formatVND(roomPrice);
            originalSubtotal += basePrice;
            discountTotal += roomDiscount;

            if (roomDiscount > 0) {
                appliedDiscounts.push({
                    title: promoTitle || "Discount Voucher",
                    amount: roomDiscount
                });
            }
        });

        const actualSubtotal = originalSubtotal - discountTotal;
        const tax = Math.round(actualSubtotal * 0.1);
        const serviceFee = originalSubtotal > 0 ? 50000 : 0;
        const grandTotal = actualSubtotal + tax + serviceFee;

        // Cập nhật lên UI
        const subtotalEl = document.getElementById("invoiceSubtotal");
        const discountsContainer = document.getElementById("invoiceDiscountsContainer");
        const taxEl = document.getElementById("invoiceTax");
        const serviceFeeEl = document.getElementById("invoiceServiceFee");
        const grandTotalEl = document.getElementById("invoiceGrandTotal");

        if (subtotalEl) subtotalEl.textContent = formatVND(originalSubtotal) + " VND";
        
        if (discountsContainer) {
            discountsContainer.innerHTML = "";
            if (appliedDiscounts.length <= 1) {
                appliedDiscounts.forEach(item => {
                    const row = document.createElement("div");
                    row.className = "d-flex justify-content-between mb-2";
                    row.innerHTML = `
                        <span class="text-muted">Discount (${item.title})</span>
                        <span class="fw-semibold text-danger">-${formatVND(item.amount)} VND</span>
                    `;
                    discountsContainer.appendChild(row);
                });
            } else {
                // Tạo dòng tổng quan
                const summaryRow = document.createElement("div");
                summaryRow.className = "d-flex justify-content-between mb-2 align-items-center";
                summaryRow.style.cursor = "pointer";
                summaryRow.innerHTML = `
                    <span class="text-muted d-flex align-items-center">
                        Discount (Total)
                        <i class="bi bi-chevron-down ms-1" id="invoiceDiscountChevron" style="transition: transform 0.2s;"></i>
                    </span>
                    <span class="fw-semibold text-danger">-${formatVND(discountTotal)} VND</span>
                `;
                discountsContainer.appendChild(summaryRow);

                // Tạo container chi tiết ẩn
                const detailsContainer = document.createElement("div");
                detailsContainer.className = "d-none ps-3 border-start mb-2";
                detailsContainer.style.fontSize = "0.9rem";
                
                appliedDiscounts.forEach(item => {
                    const row = document.createElement("div");
                    row.className = "d-flex justify-content-between mb-1 text-muted";
                    row.innerHTML = `
                        <span>${item.title}</span>
                        <span class="text-danger">-${formatVND(item.amount)} VND</span>
                    `;
                    detailsContainer.appendChild(row);
                });
                discountsContainer.appendChild(detailsContainer);

                // Sự kiện click toggle
                summaryRow.addEventListener("click", function() {
                    const isCollapsed = detailsContainer.classList.contains("d-none");
                    const chevron = document.getElementById("invoiceDiscountChevron");
                    if (isCollapsed) {
                        detailsContainer.classList.remove("d-none");
                        if (chevron) chevron.style.transform = "rotate(180deg)";
                    } else {
                        detailsContainer.classList.add("d-none");
                        if (chevron) chevron.style.transform = "rotate(0deg)";
                    }
                });
            }
        }

        if (taxEl) taxEl.textContent = formatVND(tax) + " VND";
        if (serviceFeeEl) serviceFeeEl.textContent = formatVND(serviceFee) + " VND";
        if (grandTotalEl) grandTotalEl.textContent = formatVND(grandTotal) + " VND";
    }

    // 4. Logic Promotion Modal
    const selectPromoBtns = document.querySelectorAll(".booking-select-promo-btn");
    const promoModalEl = document.getElementById("promotionModal");
    let promoModal = null;

    function getPromoModal() {
        if (!promoModal && window.bootstrap && window.bootstrap.Modal) {
            promoModal = new bootstrap.Modal(promoModalEl);
        }
        return promoModal;
    }

    const promoListContainer = document.getElementById("promoListContainer");

    let activePromoRoomId = null;
    let activePromoBtn = null;
    let activePromoHotelId = null;

    function renderPromotionsList() {
        if (!promoListContainer || typeof hotelPromotions === "undefined") return;

        promoListContainer.innerHTML = "";

        // Lấy danh sách các promotion IDs đang được áp dụng ở các phòng KHÁC
        const promoInputs = document.querySelectorAll(".room-promotion-input");
        const appliedPromoIds = [];
        let currentAppliedPromoId = 0;

        promoInputs.forEach(input => {
            const rId = input.id.replace("room-promo-", "");
            const val = parseInt(input.value);
            if (val > 0) {
                if (rId === activePromoRoomId) {
                    currentAppliedPromoId = val;
                } else {
                    appliedPromoIds.push(val);
                }
            }
        });

        // Lọc danh sách promotions: status = 'ACTIVE', không nằm trong danh sách đã dùng ở phòng khác, và khớp hotelId
        let filteredPromos = hotelPromotions.filter(p => {
            const isStatusActive = p.status && p.status.toUpperCase() === "ACTIVE";
            const isNotAppliedElsewhere = !appliedPromoIds.includes(p.id);
            const isHotelMatch = !p.hotelId || p.hotelId === activePromoHotelId;
            return isStatusActive && isNotAppliedElsewhere && isHotelMatch;
        });

        // Sắp xếp theo discountPercent giảm dần
        filteredPromos.sort((a, b) => parseFloat(b.discountPercent) - parseFloat(a.discountPercent));

        if (filteredPromos.length === 0) {
            promoListContainer.innerHTML = `<div class="text-center text-muted py-3">No active promotions available.</div>`;
            return;
        }

        filteredPromos.forEach(p => {
            const isCurrent = p.id === currentAppliedPromoId;
            const discountText = parseFloat(p.discountPercent) % 1 === 0 ? parseInt(p.discountPercent) + "%" : parseFloat(p.discountPercent) + "%";

            // Format End Date chuẩn dd/MM/yyyy
            const formattedEndDate = formatDateToDMY(p.endDate);

            const card = document.createElement("div");
            card.className = "promo-item-card";
            card.innerHTML = `
                <div class="promo-item-details">
                    <p class="promo-item-title">${p.title || "Discount Voucher"}</p>
                    <span class="promo-item-discount">${discountText} OFF</span>
                    <span class="promo-item-date">Expires: ${formattedEndDate}</span>
                </div>
                <div>
                    ${isCurrent ?
                    `<button type="button" class="promo-item-btn-remove" data-id="${p.id}">Remove</button>` :
                    `<button type="button" class="promo-item-btn-apply" data-id="${p.id}">Apply</button>`
                }
                </div>
            `;

            // Bắt sự kiện Apply
            const applyBtn = card.querySelector(".promo-item-btn-apply");
            if (applyBtn) {
                applyBtn.addEventListener("click", function () {
                    const id = parseInt(this.getAttribute("data-id"));
                    applyPromotionToRoom(id);
                });
            }

            // Bắt sự kiện Remove
            const removeBtn = card.querySelector(".promo-item-btn-remove");
            if (removeBtn) {
                removeBtn.addEventListener("click", function () {
                    removePromotionFromRoom();
                });
            }

            promoListContainer.appendChild(card);
        });
    }

    function applyPromotionToRoom(promoId) {
        if (!activePromoRoomId) return;

        const promoInput = document.getElementById("room-promo-" + activePromoRoomId);
        if (promoInput) {
            promoInput.value = promoId;
        }

        if (activePromoBtn && typeof hotelPromotions !== "undefined") {
            const promo = hotelPromotions.find(p => p.id === promoId);
            if (promo) {
                activePromoBtn.classList.add("applied");
                activePromoBtn.innerHTML = `<i class="bi bi-check-circle-fill me-1"></i><span>${promo.title} Applied</span>`;
            }
        }

        recalculateGrandTotal();
        const pm = getPromoModal();
        if (pm) pm.hide();
    }

    function removePromotionFromRoom() {
        if (!activePromoRoomId) return;

        const promoInput = document.getElementById("room-promo-" + activePromoRoomId);
        if (promoInput) {
            promoInput.value = "0";
        }

        if (activePromoBtn) {
            activePromoBtn.classList.remove("applied");
            activePromoBtn.innerHTML = `<i class="bi bi-tags me-1"></i><span>Apply Promotion</span>`;
        }

        recalculateGrandTotal();
        const pm = getPromoModal();
        if (pm) pm.hide();
    }

    // Gắn sự kiện click mở modal chọn promotion
    selectPromoBtns.forEach(btn => {
        btn.addEventListener("click", function (e) {
            e.preventDefault();
            activePromoRoomId = this.getAttribute("data-room-id");
            activePromoBtn = this;
            activePromoHotelId = parseInt(this.getAttribute("data-hotel-id"));

            // Gọi API dọn dẹp và lấy promotion mới nhất trước khi render
            fetch(`/booking/api/clean-and-get-promotions?hotelId=${activePromoHotelId}`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error("HTTP error " + response.status);
                    }
                    return response.json();
                })
                .then(data => {
                    // Cập nhật lại biến toàn cục hotelPromotions ở client
                    window.hotelPromotions = data;
                    renderPromotionsList();
                    const pm = getPromoModal();
                    if (pm) pm.show();
                })
                .catch(err => {
                    console.error("Error fetching updated promotions:", err);
                    // Fallback trong trường hợp lỗi mạng: dùng dữ liệu cũ có sẵn
                    renderPromotionsList();
                    const pm = getPromoModal();
                    if (pm) pm.show();
                });
        });
    });

    // 5. Phone Number chỉ cho phép nhập số
    const phoneInput = document.getElementById("phone");
    if (phoneInput) {
        phoneInput.addEventListener("input", function (e) {
            // Loại bỏ bất kỳ ký tự nào không phải là số từ 0-9
            this.value = this.value.replace(/[^0-9]/g, '');
        });
    }

    // 6. Logic Validation & Gộp Special Requests khi submit form
    let iti = null;
    if (phoneInput && window.intlTelInput) {
        iti = window.intlTelInput(phoneInput, {
            initialCountry: "vn",
            preferredCountries: ["vn"],
            separateDialCode: false,
            nationalMode: false,
            utilsScript: "https://cdnjs.cloudflare.com/ajax/libs/intl-tel-input/17.0.19/js/utils.js"
        });
    }

    const form = document.getElementById("bookingCheckoutForm");
    if (form) {
        form.addEventListener("submit", function (event) {
            // a. Kiểm tra định dạng Email
            const emailInput = document.getElementById("email");
            if (emailInput) {
                const emailVal = emailInput.value.trim();
                const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (!emailVal) {
                    alert("Please fill in your Email.");
                    emailInput.focus();
                    event.preventDefault();
                    return false;
                }
                if (!emailRegex.test(emailVal)) {
                    alert("Please enter a valid email address.");
                    emailInput.focus();
                    event.preventDefault();
                    return false;
                }
            }

            // b. Kiểm tra tất cả các phòng phải điền Check-in & Check-out
            const checkinPickers = document.querySelectorAll(".booking-checkin-picker");
            const checkoutPickers = document.querySelectorAll(".booking-checkout-picker");

            for (let i = 0; i < checkinPickers.length; i++) {
                const ciVal = checkinPickers[i].value.trim();
                const coVal = checkoutPickers[i] ? checkoutPickers[i].value.trim() : "";

                if (!ciVal || !coVal) {
                    alert("Please enter Check-in and Check-out dates for all selected rooms before proceeding.");
                    event.preventDefault();
                    return false;
                }
            }

            // c. Gom toàn bộ Special requests của các phòng
            const roomInputs = document.querySelectorAll(".room-special-request-input");
            let combinedReqs = [];
            roomInputs.forEach(input => {
                const val = input.value.trim();
                if (val) {
                    const rId = input.id.replace("room-req-", "");
                    const btn = document.querySelector(`.booking-special-request-btn[data-room-id="${rId}"]`);
                    const roomName = btn ? btn.getAttribute("data-room-name") : "Room";
                    combinedReqs.push(`[${roomName}]: ${val}`);
                }
            });

            const finalInput = document.getElementById("finalSpecialRequests");
            if (finalInput) {
                finalInput.value = combinedReqs.join(" | ");
            }

            // Format phone number
            if (phoneInput && phoneInput.value.trim() && iti) {
                phoneInput.value = iti.getNumber();
            }
        });
    }

    // Auto-submit form when select quantities changes
    const qtySelects = document.querySelectorAll("select[name='quantities']");
    qtySelects.forEach(select => {
        select.addEventListener("change", function () {
            const checkoutForm = document.getElementById("bookingCheckoutForm");
            if (checkoutForm) {
                checkoutForm.submit();
            }
        });
    });
});
