// Admin Dashboard Logic - English Version
document.addEventListener("DOMContentLoaded", function () {
    // 1. Initialize Lucide Icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }

    // 2. Sidebar Tab Switching is handled server-side to optimize database queries.
    // Each tab link reloads the page with the corresponding 'tab' parameter.

    // 3. Load Data from window globals (rendered by Thymeleaf inline JS)
    const bookingsData = window.bookingsData || [];

    // 4. Formatting Helper
    function formatVND(amount) {
        return new Intl.NumberFormat('en-US', { minimumFractionDigits: 0 }).format(amount) + " VND";
    }

    // ==========================================
    // TAB 1: REVENUE OVERVIEW LOGIC (CSS Chart & Date filters)
    // ==========================================
    const revenuePanel = document.getElementById("revenuePanel");
    const isRevenueTab = revenuePanel && revenuePanel.classList.contains("active");

    const filterStartDate = document.getElementById("filterStartDate");
    const filterEndDate = document.getElementById("filterEndDate");

    const totalRevenueEl = document.getElementById("totalRevenue");
    const totalBookingsEl = document.getElementById("totalBookings");
    const totalHotelsEl = document.getElementById("totalHotels");
    
    const cssChartContainer = document.getElementById("cssChartContainer");
    const revenueTableBody = document.getElementById("revenueTableBody");

    function applyRevenueFilters() {
        if (!filterStartDate) return;

        const startDateVal = filterStartDate.value;
        const endDateVal = filterEndDate.value;

        // Constraint: Start Date must be before or equal to End Date
        if (startDateVal && endDateVal && startDateVal > endDateVal) {
            alert("Start Date must be before or equal to End Date!");
            filterStartDate.value = "";
            filterEndDate.removeAttribute("min");
            filterStartDate.removeAttribute("max");
            applyRevenueFilters();
            return;
        }

        let totalRevenue = 0;
        let totalPlatformFee = 0;
        let totalBookings = 0;

        bookingsData.forEach(b => {
            const isValidStatus = ['CONFIRMED', 'COMPLETED'].includes(b.bookingStatus);
            if (!isValidStatus) return;

            const bookingDateStr = b.checkInDate; // YYYY-MM-DD
            if (startDateVal && bookingDateStr < startDateVal) return;
            if (endDateVal && bookingDateStr > endDateVal) return;

            const amount = parseFloat(b.totalPrice) || 0;
            // Sum platformFeeAmount from database, skip if null
            if (b.platformFeeAmount !== null && b.platformFeeAmount !== undefined) {
                totalPlatformFee += parseFloat(b.platformFeeAmount) || 0;
            }

            totalRevenue += amount;
            totalBookings += 1;
        });
        
        let totalRefunded = 0;
        if (window.refundsData) {
            window.refundsData.forEach(r => {
                const refundDateStr = r.processedAt ? r.processedAt.split('T')[0] : null;
                if (refundDateStr) {
                    if (startDateVal && refundDateStr < startDateVal) return;
                    if (endDateVal && refundDateStr > endDateVal) return;
                }
                totalRefunded += parseFloat(r.refundAmount) || 0;
            });
        }

        // 1. Update summary cards
        totalRevenueEl.textContent = formatVND(totalRevenue);
        const totalPlatformFeeEl = document.getElementById("totalPlatformFee");
        if (totalPlatformFeeEl) totalPlatformFeeEl.textContent = formatVND(totalPlatformFee);
        const totalRefundedAmountEl = document.getElementById("totalRefundedAmount");
        if (totalRefundedAmountEl) totalRefundedAmountEl.textContent = formatVND(totalRefunded);
        totalBookingsEl.textContent = totalBookings.toLocaleString('en-US');
    }

    if (filterStartDate && isRevenueTab) {
        // Clear Filter Button
        const btnClearDateFilter = document.getElementById("btnClearDateFilter");
        if (btnClearDateFilter) {
            btnClearDateFilter.addEventListener("click", function() {
                filterStartDate.value = "";
                filterEndDate.value = "";
                filterEndDate.removeAttribute("min");
                filterStartDate.removeAttribute("max");
                applyRevenueFilters();
            });
        }

        filterStartDate.addEventListener("change", function() {
            if (this.value) {
                filterEndDate.min = this.value;
            } else {
                filterEndDate.removeAttribute("min");
            }
            applyRevenueFilters();
        });
        filterEndDate.addEventListener("change", function() {
            if (this.value) {
                filterStartDate.max = this.value;
            } else {
                filterStartDate.removeAttribute("max");
            }
            applyRevenueFilters();
        });

        // Load initial state
        applyRevenueFilters();
    }
});
document.addEventListener('DOMContentLoaded', function() {

    // Preview ảnh Hotel
    const hotelImageInput = document.querySelector('input[name="imageFile"]');
    const hotelImagePreview = document.getElementById('hotelImagePreview');
    if (hotelImageInput && hotelImagePreview) {
        hotelImageInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(ev) {
                    hotelImagePreview.innerHTML = `
                        <div class="d-inline-block position-relative">
                            <img src="${ev.target.result}" class="img-thumbnail" style="max-height: 120px; border-radius: 8px; border: 2px solid #c9a96e;" />
                            <span class="badge bg-success position-absolute top-0 start-0 m-1">Selected</span>
                        </div>
                        <span class="ms-2 small text-success">${file.name}</span>
                    `;
                };
                reader.readAsDataURL(file);
            } else {
                hotelImagePreview.innerHTML = '';
            }
        });
    }

    // Preview Business Registration
    const businessInput = document.querySelector('input[name="businessRegistrationDoc"]');
    const businessPreview = document.getElementById('businessRegistrationPreview');
    if (businessInput && businessPreview) {
        businessInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(ev) {
                    const isImage = file.type.startsWith('image/');
                    if (isImage) {
                        businessPreview.innerHTML = `
                            <div class="d-inline-block position-relative">
                                <img src="${ev.target.result}" class="img-thumbnail" style="max-height: 100px; border-radius: 8px; border: 2px solid #10b981;" />
                                <span class="badge bg-success position-absolute top-0 start-0 m-1">Selected</span>
                            </div>
                            <span class="ms-2 small text-success">${file.name}</span>
                        `;
                    } else {
                        businessPreview.innerHTML = `
                            <span class="badge bg-info"><i class="bi bi-file-pdf"></i> ${file.name}</span>
                        `;
                    }
                };
                reader.readAsDataURL(file);
            } else {
                businessPreview.innerHTML = '';
            }
        });
    }

    // Preview Land Certificate
    const landInput = document.querySelector('input[name="landCertificateDoc"]');
    const landPreview = document.getElementById('landCertificatePreview');
    if (landInput && landPreview) {
        landInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(ev) {
                    const isImage = file.type.startsWith('image/');
                    if (isImage) {
                        landPreview.innerHTML = `
                            <div class="d-inline-block position-relative">
                                <img src="${ev.target.result}" class="img-thumbnail" style="max-height: 100px; border-radius: 8px; border: 2px solid #10b981;" />
                                <span class="badge bg-success position-absolute top-0 start-0 m-1">Selected</span>
                            </div>
                            <span class="ms-2 small text-success">${file.name}</span>
                        `;
                    } else {
                        landPreview.innerHTML = `
                            <span class="badge bg-info"><i class="bi bi-file-pdf"></i> ${file.name}</span>
                        `;
                    }
                };
                reader.readAsDataURL(file);
            } else {
                landPreview.innerHTML = '';
            }
        });
    }

    // Preview Rental Contract
    const rentalInput = document.querySelector('input[name="rentalContractDoc"]');
    const rentalPreview = document.getElementById('rentalContractPreview');
    if (rentalInput && rentalPreview) {
        rentalInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(ev) {
                    const isImage = file.type.startsWith('image/');
                    if (isImage) {
                        rentalPreview.innerHTML = `
                            <div class="d-inline-block position-relative">
                                <img src="${ev.target.result}" class="img-thumbnail" style="max-height: 100px; border-radius: 8px; border: 2px solid #f59e0b;" />
                                <span class="badge bg-warning position-absolute top-0 start-0 m-1">Selected</span>
                            </div>
                            <span class="ms-2 small text-muted">${file.name}</span>
                        `;
                    } else {
                        rentalPreview.innerHTML = `
                            <span class="badge bg-info"><i class="bi bi-file-pdf"></i> ${file.name}</span>
                        `;
                    }
                };
                reader.readAsDataURL(file);
            } else {
                rentalPreview.innerHTML = '';
            }
        });
    }
});