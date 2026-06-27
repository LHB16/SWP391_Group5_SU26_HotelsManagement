// Admin Dashboard Logic - English Version
document.addEventListener("DOMContentLoaded", function () {
    // 1. Initialize Lucide Icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }

    // 2. Sidebar Tab Switching
    const sidebarLinks = document.querySelectorAll(".sidebar-link");
    const tabPanels = document.querySelectorAll(".tab-panel");

    sidebarLinks.forEach(link => {
        link.addEventListener("click", function () {
            // Remove active class from all links
            sidebarLinks.forEach(l => l.classList.remove("active"));
            // Add active class to clicked link
            this.classList.add("active");

            // Hide all tab panels
            tabPanels.forEach(panel => panel.classList.remove("active"));
            // Show target tab panel
            const targetTab = this.getAttribute("data-tab");
            const targetPanel = document.getElementById(targetTab);
            if (targetPanel) {
                targetPanel.classList.add("active");
            }
        });
    });

    // 3. Load Data from window globals (rendered by Thymeleaf inline JS)
    const bookingsData = window.bookingsData || [];

    // 4. Formatting Helper
    function formatUSD(amount) {
        return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'VND', minimumFractionDigits: 0 }).format(amount);
    }

    // ==========================================
    // TAB 1: REVENUE OVERVIEW LOGIC (CSS Chart & Date filters)
    // ==========================================
    const filterStartDate = document.getElementById("filterStartDate");
    const filterEndDate = document.getElementById("filterEndDate");
    const hotelCheckboxes = document.querySelectorAll(".hotel-filter-checkbox");

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

        // Get list of checked hotel names
        const selectedHotelNames = [];
        hotelCheckboxes.forEach(cb => {
            if (cb.checked) {
                selectedHotelNames.push(cb.value);
            }
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
        totalRevenueEl.textContent = formatUSD(totalRevenue);
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
                const rowDiv = document.createElement("div");
                rowDiv.className = "chart-row";
                rowDiv.innerHTML = `
                    <div class="chart-hotel-name" title="${h.hotelName}">${h.hotelName}</div>
                    <div class="chart-bar-wrapper">
                        <div class="chart-bar" style="width: 0%;">
                            <span class="chart-bar-value">${formatUSD(h.revenue)}</span>
                        </div>
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
                    <td class="revenue-amount text-success">${formatUSD(h.revenue)}</td>
                    <td>${h.bookingCount} successful bookings</td>
                    <td><span class="badge bg-success-subtle text-success px-2 py-1">Active</span></td>
                `;
                revenueTableBody.appendChild(tr);
            });
        }
    }

    if (filterStartDate) {
        // Search filter for hotels in checkbox list
        const searchHotelInput = document.getElementById("searchHotelInput");
        if (searchHotelInput) {
            searchHotelInput.addEventListener("input", function() {
                const keyword = this.value.trim().toLowerCase();
                const items = document.querySelectorAll(".hotel-checkbox-item");
                items.forEach(item => {
                    const hotelName = item.querySelector("span").textContent.toLowerCase();
                    if (hotelName.includes(keyword)) {
                        item.style.setProperty("display", "flex", "important");
                    } else {
                        item.style.setProperty("display", "none", "important");
                    }
                });
            });
        }

        // Select All / Deselect All buttons
        const btnSelectAllHotels = document.getElementById("btnSelectAllHotels");
        const btnDeselectAllHotels = document.getElementById("btnDeselectAllHotels");
        
        if (btnSelectAllHotels && btnDeselectAllHotels) {
            btnSelectAllHotels.addEventListener("click", function() {
                hotelCheckboxes.forEach(cb => {
                    const parentItem = cb.closest(".hotel-checkbox-item");
                    if (parentItem && parentItem.style.display !== "none") {
                        cb.checked = true;
                    }
                });
                applyRevenueFilters();
            });
            btnDeselectAllHotels.addEventListener("click", function() {
                hotelCheckboxes.forEach(cb => {
                    const parentItem = cb.closest(".hotel-checkbox-item");
                    if (parentItem && parentItem.style.display !== "none") {
                        cb.checked = false;
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
        hotelCheckboxes.forEach(cb => cb.addEventListener("change", applyRevenueFilters));
        
        // Load initial state
        applyRevenueFilters();
    }
});
