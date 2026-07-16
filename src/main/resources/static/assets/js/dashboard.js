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
    const filterMonth = document.getElementById("filterMonth");
    const filterQuarter = document.getElementById("filterQuarter");
    const filterYear = document.getElementById("filterYear");

    const totalRevenueEl = document.getElementById("totalRevenue");
    const totalBookingsEl = document.getElementById("totalBookings");
    
    let revenueChartInstance = null;

    function updateRevenueChart(labels, revenueData, feeData, refundData) {
        const ctx = document.getElementById('revenueChart');
        if (!ctx) return;

        if (revenueChartInstance) {
            revenueChartInstance.destroy();
        }

        revenueChartInstance = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: 'Total Revenue',
                        data: revenueData,
                        backgroundColor: '#3b82f6', // sleek blue
                        borderRadius: 6,
                        maxBarThickness: 35
                    },
                    {
                        label: 'Total Platform Fee',
                        data: feeData,
                        backgroundColor: '#10b981', // emerald green
                        borderRadius: 6,
                        maxBarThickness: 35
                    },
                    {
                        label: 'Total Refunded Amount',
                        data: refundData,
                        backgroundColor: '#ef4444', // elegant red
                        borderRadius: 6,
                        maxBarThickness: 35
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                        labels: {
                            font: {
                                family: 'Inter, sans-serif',
                                size: 12,
                                weight: '500'
                            },
                            color: '#64748b'
                        }
                    },
                    tooltip: {
                        backgroundColor: '#0f172a',
                        titleFont: { family: 'Inter, sans-serif', size: 13, weight: 'bold' },
                        bodyFont: { family: 'Inter, sans-serif', size: 12 },
                        padding: 12,
                        cornerRadius: 8,
                        callbacks: {
                            label: function(context) {
                                let label = context.dataset.label || '';
                                if (label) {
                                    label += ': ';
                                }
                                if (context.raw !== null) {
                                    label += new Intl.NumberFormat('en-US').format(context.raw) + " VND";
                                }
                                return label;
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: {
                            display: false
                        },
                        ticks: {
                            font: {
                                family: 'Inter, sans-serif',
                                size: 11,
                                weight: '500'
                            },
                            color: '#64748b'
                        }
                    },
                    y: {
                        grid: {
                            color: '#f1f5f9'
                        },
                        ticks: {
                            font: {
                                family: 'Inter, sans-serif',
                                size: 11
                            },
                            color: '#64748b',
                            callback: function(value) {
                                if (value >= 1e6) {
                                    return (value / 1e6).toFixed(1) + 'M';
                                }
                                return value.toLocaleString('en-US');
                            }
                        }
                    }
                }
            }
        });
    }

    function getMonthsInRange(startYM, endYM) {
        let months = [];
        let startYear = parseInt(startYM.substring(0, 4), 10);
        let startMonth = parseInt(startYM.substring(5, 7), 10);
        let endYear = parseInt(endYM.substring(0, 4), 10);
        let endMonth = parseInt(endYM.substring(5, 7), 10);

        let currYear = startYear;
        let currMonth = startMonth;

        while (currYear < endYear || (currYear === endYear && currMonth <= endMonth)) {
            months.push(`${currYear}-${String(currMonth).padStart(2, '0')}`);
            currMonth++;
            if (currMonth > 12) {
                currMonth = 1;
                currYear++;
            }
        }
        return months;
    }

    function applyRevenueFilters() {
        if (!filterStartDate) return;

        const startDateVal = filterStartDate.value;
        const endDateVal = filterEndDate.value;
        const monthVal = filterMonth ? filterMonth.value : "";
        const quarterVal = filterQuarter ? filterQuarter.value : "";
        const yearVal = filterYear ? filterYear.value : "";

        // Constraint: Start Date must be before or equal to End Date
        if (startDateVal && endDateVal && startDateVal > endDateVal) {
            alert("Start Date must be before or equal to End Date!");
            filterStartDate.value = "";
            applyRevenueFilters();
            return;
        }

        let totalRevenue = 0;
        let totalPlatformFee = 0;
        let totalBookings = 0;

        let filteredBookings = [];
        let filteredRefunds = [];

        bookingsData.forEach(b => {
            const isValidStatus = ['CONFIRMED', 'COMPLETED'].includes(b.bookingStatus);
            if (!isValidStatus) return;

            const bookingDateStr = b.checkInDate; // YYYY-MM-DD
            if (!bookingDateStr) return;

            if (startDateVal && bookingDateStr < startDateVal) return;
            if (endDateVal && bookingDateStr > endDateVal) return;

            const bYear = bookingDateStr.substring(0, 4);
            const bMonth = bookingDateStr.substring(5, 7);

            if (yearVal && bYear !== yearVal) return;
            if (monthVal && bMonth !== monthVal) return;
            if (quarterVal) {
                const monthInt = parseInt(bMonth, 10);
                const belongsToQuarter = (quarterVal === "1" && monthInt >= 1 && monthInt <= 3) ||
                                         (quarterVal === "2" && monthInt >= 4 && monthInt <= 6) ||
                                         (quarterVal === "3" && monthInt >= 7 && monthInt <= 9) ||
                                         (quarterVal === "4" && monthInt >= 10 && monthInt <= 12);
                if (!belongsToQuarter) return;
            }

            const amount = parseFloat(b.totalPrice) || 0;
            if (b.platformFeeAmount !== null && b.platformFeeAmount !== undefined) {
                totalPlatformFee += parseFloat(b.platformFeeAmount) || 0;
            }

            totalRevenue += amount;
            totalBookings += 1;
            filteredBookings.push(b);
        });
        
        let totalRefunded = 0;
        if (window.refundsData) {
            window.refundsData.forEach(r => {
                const refundDateStr = r.processedAt ? r.processedAt.split('T')[0] : null;
                if (!refundDateStr) return;

                if (startDateVal && refundDateStr < startDateVal) return;
                if (endDateVal && refundDateStr > endDateVal) return;

                const rYear = refundDateStr.substring(0, 4);
                const rMonth = refundDateStr.substring(5, 7);

                if (yearVal && rYear !== yearVal) return;
                if (monthVal && rMonth !== monthVal) return;
                if (quarterVal) {
                    const monthInt = parseInt(rMonth, 10);
                    const belongsToQuarter = (quarterVal === "1" && monthInt >= 1 && monthInt <= 3) ||
                                             (quarterVal === "2" && monthInt >= 4 && monthInt <= 6) ||
                                             (quarterVal === "3" && monthInt >= 7 && monthInt <= 9) ||
                                             (quarterVal === "4" && monthInt >= 10 && monthInt <= 12);
                    if (!belongsToQuarter) return;
                }

                totalRefunded += parseFloat(r.refundAmount) || 0;
                filteredRefunds.push(r);
            });
        }

        // 1. Update summary cards
        totalRevenueEl.textContent = formatVND(totalRevenue);
        const totalPlatformFeeEl = document.getElementById("totalPlatformFee");
        if (totalPlatformFeeEl) totalPlatformFeeEl.textContent = formatVND(totalPlatformFee);
        const totalRefundedAmountEl = document.getElementById("totalRefundedAmount");
        if (totalRefundedAmountEl) totalRefundedAmountEl.textContent = formatVND(totalRefunded);
        totalBookingsEl.textContent = totalBookings.toLocaleString('en-US');

        // 2. Prepare X-axis Months based on filters
        let monthsToDisplay = [];
        
        if (yearVal && quarterVal) {
            const q = parseInt(quarterVal, 10);
            const mList = q === 1 ? ["01", "02", "03"] :
                          q === 2 ? ["04", "05", "06"] :
                          q === 3 ? ["07", "08", "09"] : ["10", "11", "12"];
            monthsToDisplay = mList.map(m => `${yearVal}-${m}`);
        } else if (yearVal && monthVal) {
            monthsToDisplay = [`${yearVal}-${monthVal}`];
        } else if (yearVal) {
            for (let i = 1; i <= 12; i++) {
                monthsToDisplay.push(`${yearVal}-${String(i).padStart(2, '0')}`);
            }
        } else if (startDateVal && endDateVal) {
            const startYM = startDateVal.substring(0, 7);
            const endYM = endDateVal.substring(0, 7);
            monthsToDisplay = getMonthsInRange(startYM, endYM);
        } else if (monthVal) {
            const defaultYear = new Date().getFullYear().toString();
            monthsToDisplay = [`${defaultYear}-${monthVal}`];
        } else if (quarterVal) {
            const defaultYear = new Date().getFullYear().toString();
            const q = parseInt(quarterVal, 10);
            const mList = q === 1 ? ["01", "02", "03"] :
                          q === 2 ? ["04", "05", "06"] :
                          q === 3 ? ["07", "08", "09"] : ["10", "11", "12"];
            monthsToDisplay = mList.map(m => `${defaultYear}-${m}`);
        } else {
            // All Time - find min and max month in existing data
            let allYM = new Set();
            bookingsData.forEach(b => {
                if (b.checkInDate && ['CONFIRMED', 'COMPLETED'].includes(b.bookingStatus)) {
                    allYM.add(b.checkInDate.substring(0, 7));
                }
            });
            if (window.refundsData) {
                window.refundsData.forEach(r => {
                    if (r.processedAt) {
                        allYM.add(r.processedAt.substring(0, 7));
                    }
                });
            }
            if (allYM.size > 0) {
                const sortedYM = Array.from(allYM).sort();
                monthsToDisplay = getMonthsInRange(sortedYM[0], sortedYM[sortedYM.length - 1]);
            } else {
                const currentY = new Date().getFullYear().toString();
                for (let i = 1; i <= 12; i++) {
                    monthsToDisplay.push(`${currentY}-${String(i).padStart(2, '0')}`);
                }
            }
        }

        // Initialize empty chart data for each month
        let chartDataMap = {};
        monthsToDisplay.forEach(ym => {
            chartDataMap[ym] = { revenue: 0, fee: 0, refund: 0 };
        });

        // Group data
        filteredBookings.forEach(b => {
            const ym = b.checkInDate.substring(0, 7);
            if (chartDataMap[ym] !== undefined) {
                chartDataMap[ym].revenue += parseFloat(b.totalPrice) || 0;
                if (b.platformFeeAmount !== null && b.platformFeeAmount !== undefined) {
                    chartDataMap[ym].fee += parseFloat(b.platformFeeAmount) || 0;
                }
            }
        });

        filteredRefunds.forEach(r => {
            if (r.processedAt) {
                const ym = r.processedAt.substring(0, 7);
                if (chartDataMap[ym] !== undefined) {
                    chartDataMap[ym].refund += parseFloat(r.refundAmount) || 0;
                }
            }
        });

        // Generate final labels and dataset values
        let labels = [];
        let rData = [];
        let fData = [];
        let refData = [];

        const monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

        monthsToDisplay.forEach(ym => {
            const y = ym.substring(0, 4);
            const m = parseInt(ym.substring(5, 7), 10);
            labels.push(`${monthNames[m - 1]} ${y}`);

            rData.push(chartDataMap[ym].revenue);
            fData.push(chartDataMap[ym].fee);
            refData.push(chartDataMap[ym].refund);
        });

        updateRevenueChart(labels, rData, fData, refData);
    }

    if (filterStartDate && isRevenueTab) {
        const btnClearDateFilter = document.getElementById("btnClearDateFilter");
        
        let startPicker = null;
        let endPicker = null;

        function resetQuickFilters() {
            if (filterMonth) filterMonth.value = "";
            if (filterQuarter) filterQuarter.value = "";
            if (filterYear) filterYear.value = "";
        }

        function populateYearFilter() {
            if (!filterYear) return;

            let years = new Set();
            years.add(new Date().getFullYear()); // Fallback current year

            if (window.bookingsData) {
                window.bookingsData.forEach(b => {
                    if (b.checkInDate) {
                        const y = parseInt(b.checkInDate.substring(0, 4), 10);
                        if (y) years.add(y);
                    }
                });
            }

            if (window.refundsData) {
                window.refundsData.forEach(r => {
                    if (r.processedAt) {
                        const y = parseInt(r.processedAt.substring(0, 4), 10);
                        if (y) years.add(y);
                    }
                });
            }

            const sortedYears = Array.from(years).sort((a, b) => b - a);
            const currentValue = filterYear.value;

            filterYear.innerHTML = '<option value="">All</option>';
            sortedYears.forEach(y => {
                const opt = document.createElement("option");
                opt.value = y.toString();
                opt.textContent = y.toString();
                if (y.toString() === currentValue) {
                    opt.selected = true;
                }
                filterYear.appendChild(opt);
            });
        }

        if (typeof flatpickr === "function") {
            startPicker = flatpickr(filterStartDate, {
                dateFormat: "Y-m-d",
                altInput: true,
                altFormat: "d/m/Y",
                altInputClass: "search-pill-input",
                onChange: function(selectedDates, dateStr) {
                    if (selectedDates[0]) {
                        endPicker.set("minDate", dateStr);
                        resetQuickFilters();
                    } else {
                        endPicker.set("minDate", null);
                    }
                    applyRevenueFilters();
                }
            });

            endPicker = flatpickr(filterEndDate, {
                dateFormat: "Y-m-d",
                altInput: true,
                altFormat: "d/m/Y",
                altInputClass: "search-pill-input",
                onChange: function(selectedDates, dateStr) {
                    if (selectedDates[0]) {
                        startPicker.set("maxDate", dateStr);
                        resetQuickFilters();
                    } else {
                        startPicker.set("maxDate", null);
                    }
                    applyRevenueFilters();
                }
            });

            if (btnClearDateFilter) {
                btnClearDateFilter.addEventListener("click", function() {
                    startPicker.clear();
                    endPicker.clear();
                    resetQuickFilters();
                    applyRevenueFilters();
                });
            }
        } else {
            // Fallback for native date inputs if flatpickr not loaded
            if (btnClearDateFilter) {
                btnClearDateFilter.addEventListener("click", function() {
                    filterStartDate.value = "";
                    filterEndDate.value = "";
                    filterEndDate.removeAttribute("min");
                    filterStartDate.removeAttribute("max");
                    resetQuickFilters();
                    applyRevenueFilters();
                });
            }

            filterStartDate.addEventListener("change", function() {
                if (this.value) {
                    filterEndDate.min = this.value;
                    resetQuickFilters();
                } else {
                    filterEndDate.removeAttribute("min");
                }
                applyRevenueFilters();
            });
            filterEndDate.addEventListener("change", function() {
                if (this.value) {
                    filterStartDate.max = this.value;
                    resetQuickFilters();
                } else {
                    filterStartDate.removeAttribute("max");
                }
                applyRevenueFilters();
            });
        }

        // Quick Selector Event Listeners
        if (filterMonth) {
            filterMonth.addEventListener("change", function() {
                if (this.value) {
                    if (startPicker) startPicker.clear();
                    if (endPicker) endPicker.clear();
                    filterStartDate.value = "";
                    filterEndDate.value = "";
                    if (filterQuarter) filterQuarter.value = "";
                }
                applyRevenueFilters();
            });
        }

        if (filterQuarter) {
            filterQuarter.addEventListener("change", function() {
                if (this.value) {
                    if (startPicker) startPicker.clear();
                    if (endPicker) endPicker.clear();
                    filterStartDate.value = "";
                    filterEndDate.value = "";
                    if (filterMonth) filterMonth.value = "";
                }
                applyRevenueFilters();
            });
        }

        if (filterYear) {
            filterYear.addEventListener("change", function() {
                if (this.value) {
                    if (startPicker) startPicker.clear();
                    if (endPicker) endPicker.clear();
                    filterStartDate.value = "";
                    filterEndDate.value = "";
                }
                applyRevenueFilters();
            });
        }

        // Populate dynamic years from data
        populateYearFilter();

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