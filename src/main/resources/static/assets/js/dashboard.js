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

    function saveRevenueFilterState() {
        if (!filterStartDate || !isRevenueTab) return;
        const activeHotelNames = [];
        document.querySelectorAll(".hotel-pill-btn.active").forEach(btn => {
            activeHotelNames.push(btn.getAttribute("data-hotel-name"));
        });
        const state = {
            startDate: filterStartDate.value,
            endDate: filterEndDate.value,
            selectedHotels: activeHotelNames
        };
        localStorage.setItem("adminRevenueFilters", JSON.stringify(state));
    }

    function restoreRevenueFilterState() {
        if (!isRevenueTab) return;
        const savedStateStr = localStorage.getItem("adminRevenueFilters");
        if (!savedStateStr) return;
        try {
            const state = JSON.parse(savedStateStr);
            
            // 1. Restore dates
            if (state.startDate) {
                filterStartDate.value = state.startDate;
                filterEndDate.min = state.startDate;
            }
            if (state.endDate) {
                filterEndDate.value = state.endDate;
                filterStartDate.max = state.endDate;
            }
            
            // 2. Restore active hotels
            if (state.selectedHotels && Array.isArray(state.selectedHotels)) {
                const hotelPillButtons = document.querySelectorAll(".hotel-pill-btn");
                hotelPillButtons.forEach(btn => {
                    const hotelName = btn.getAttribute("data-hotel-name");
                    if (state.selectedHotels.includes(hotelName)) {
                        btn.classList.add("active");
                    } else {
                        btn.classList.remove("active");
                    }
                });
            }
        } catch (e) {
            console.error("Failed to restore revenue filters", e);
        }
    }

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

        // Get list of selected hotel names from active buttons
        const selectedHotelNames = [];
        document.querySelectorAll(".hotel-pill-btn.active").forEach(btn => {
            selectedHotelNames.push(btn.getAttribute("data-hotel-name"));
        });

        // Object to accumulate revenue per hotel
        const hotelRevenueMap = {};
        let totalRevenue = 0;
        let totalBookings = 0;

        bookingsData.forEach(b => {
            // Only count valid booking statuses
            const isValidStatus = ['CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT'].includes(b.bookingStatus);
            if (!isValidStatus) return;

            // Date Range Filter (compare against checkInDate or booking createdAt)
            const bookingDateStr = b.checkInDate; // YYYY-MM-DD
            if (startDateVal && bookingDateStr < startDateVal) return;
            if (endDateVal && bookingDateStr > endDateVal) return;

            // Hotel Checkbox Filter
            if (!selectedHotelNames.includes(b.hotelName)) return;

            const amount = parseFloat(b.totalPrice) || 0;
            totalRevenue += amount;
            totalBookings += 1;

            if (!hotelRevenueMap[b.hotelName]) {
                hotelRevenueMap[b.hotelName] = {
                    hotelName: b.hotelName,
                    revenue: 0,
                    bookingCount: 0
                };
            }
            hotelRevenueMap[b.hotelName].revenue += amount;
            hotelRevenueMap[b.hotelName].bookingCount += 1;
        });

        // 1. Update summary cards
        totalRevenueEl.textContent = formatVND(totalRevenue);
        totalBookingsEl.textContent = totalBookings.toLocaleString('en-US');
        totalHotelsEl.textContent = Object.keys(hotelRevenueMap).length;

        // 2. Render Custom CSS Chart (Bars)
        cssChartContainer.innerHTML = "";
        const hotelRevenueList = Object.values(hotelRevenueMap);
        
        if (hotelRevenueList.length === 0) {
            cssChartContainer.innerHTML = `<div class="text-center text-muted py-4">No revenue data matching the filters.</div>`;
        } else {
            // Find max revenue to scale chart bars to 100% width
            const maxRevenue = Math.max(...hotelRevenueList.map(h => h.revenue));

            hotelRevenueList.forEach(h => {
                const percentage = maxRevenue > 0 ? (h.revenue / maxRevenue) * 100 : 0;
                const isShortBar = percentage < 28;
                const rowDiv = document.createElement("div");
                rowDiv.className = "chart-row";
                rowDiv.innerHTML = `
                    <div class="chart-hotel-name" title="${h.hotelName}">${h.hotelName}</div>
                    <div class="chart-bar-wrapper">
                        <div class="chart-bar" style="width: 0%;">
                            ${!isShortBar ? `<span class="chart-bar-value" style="color:#fff;">${formatVND(h.revenue)}</span>` : ''}
                        </div>
                        ${isShortBar ? `<span class="chart-bar-value chart-bar-value--outside">${formatVND(h.revenue)}</span>` : ''}
                    </div>
                `;
                cssChartContainer.appendChild(rowDiv);

                // Set width directly (No animation)
                const bar = rowDiv.querySelector(".chart-bar");
                if (bar) bar.style.width = percentage + "%";
            });
        }

        // 3. Render Table
        revenueTableBody.innerHTML = "";
        if (hotelRevenueList.length === 0) {
            revenueTableBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">No records found</td></tr>`;
        } else {
            hotelRevenueList.forEach(h => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${h.hotelName}</strong></td>
                    <td class="revenue-amount text-success">${formatVND(h.revenue)}</td>
                    <td>${h.bookingCount} successful bookings</td>
                    <td><span class="badge bg-success-subtle text-success px-2 py-1">Active</span></td>
                `;
                revenueTableBody.appendChild(tr);
            });
        }

        // Save current filter state to localStorage
        saveRevenueFilterState();
    }

    if (filterStartDate && isRevenueTab) {
        // Search filter for hotels in pill buttons list
        const searchHotelInput = document.getElementById("searchHotelInput");
        if (searchHotelInput) {
            searchHotelInput.addEventListener("input", function() {
                const keyword = this.value.trim().toLowerCase();
                const hotelPillButtons = document.querySelectorAll(".hotel-pill-btn");
                hotelPillButtons.forEach(btn => {
                    const hotelName = btn.getAttribute("data-hotel-name").toLowerCase();
                    if (hotelName.includes(keyword)) {
                        btn.style.setProperty("display", "inline-flex", "important");
                    } else {
                        btn.style.setProperty("display", "none", "important");
                    }
                });
            });
        }

        // Select All / Deselect All buttons
        const btnSelectAllHotels = document.getElementById("btnSelectAllHotels");
        const btnDeselectAllHotels = document.getElementById("btnDeselectAllHotels");
        
        if (btnSelectAllHotels && btnDeselectAllHotels) {
            btnSelectAllHotels.addEventListener("click", function() {
                const hotelPillButtons = document.querySelectorAll(".hotel-pill-btn");
                hotelPillButtons.forEach(btn => {
                    if (btn.style.display !== "none") {
                        btn.classList.add("active");
                    }
                });
                applyRevenueFilters();
            });
            btnDeselectAllHotels.addEventListener("click", function() {
                const hotelPillButtons = document.querySelectorAll(".hotel-pill-btn");
                hotelPillButtons.forEach(btn => {
                    if (btn.style.display !== "none") {
                        btn.classList.remove("active");
                    }
                });
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

        // Add click events to pill buttons
        const hotelPillButtons = document.querySelectorAll(".hotel-pill-btn");
        hotelPillButtons.forEach(btn => {
            btn.addEventListener("click", function() {
                this.classList.toggle("active");
                applyRevenueFilters();
            });
        });
        
        // Restore filters state from localStorage
        restoreRevenueFilterState();
        
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