// src/main/resources/static/assets/js/owner-dashboard.js

document.addEventListener('DOMContentLoaded', function() {

    // 1. Initialize Lucide Icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }

    // 2. Update Date/Time
    updateDateTime();

    // 3. Sidebar Tab Switching
    const sidebarLinks = document.querySelectorAll('.sidebar-link[data-tab]');
    const tabPanels = document.querySelectorAll('.tab-panel');

    sidebarLinks.forEach(link => {
        link.addEventListener('click', function() {
            sidebarLinks.forEach(l => l.classList.remove('active'));
            this.classList.add('active');

            tabPanels.forEach(panel => panel.classList.remove('active'));
            const targetTab = this.getAttribute('data-tab');
            const targetPanel = document.getElementById(targetTab);
            if (targetPanel) {
                targetPanel.classList.add('active');
            }
        });
    });

    // Tự động switch sang tab tương ứng nếu URL chứa query parameter ?tab=xxx
    const urlParams = new URLSearchParams(window.location.search);
    const tabParam = urlParams.get('tab');
    if (tabParam) {
        const targetLink = document.querySelector(`.sidebar-link[data-tab="${tabParam}"]`);
        const targetPanel = document.getElementById(tabParam);
        if (targetLink && targetPanel) {
            // Xóa active ở các tab mặc định
            sidebarLinks.forEach(l => l.classList.remove('active'));
            tabPanels.forEach(panel => panel.classList.remove('active'));

            // Kích hoạt active cho tab mới
            targetLink.classList.add('active');
            targetPanel.classList.add('active');
        }
    }

    // 4. Setup Flatpickr for Booking Date Filters
    setupBookingDateFilters();

    // 5. Setup Booking Filter Events
    setupBookingFilterEvents();

    // 6. Add Hotel Modal - Open
    window.openAddHotelModal = function() {
        const modal = document.getElementById('addHotelModal');
        if (modal) {
            const bsModal = new bootstrap.Modal(modal);
            bsModal.show();
        }
    };

    // 7. Add Hotel Modal - Close
    window.closeAddHotelModal = function() {
        const modal = document.getElementById('addHotelModal');
        if (modal) {
            const bsModal = bootstrap.Modal.getInstance(modal);
            if (bsModal) {
                bsModal.hide();
            }
        }
    };

    // 8. Apply Booking Filters on page load
    setTimeout(applyBookingFilters, 500);

    // 9. Format booking dates after load
    setTimeout(formatAllBookingDates, 600);

});

// ============================================================
// UPDATE DATE/TIME
// ============================================================
function updateDateTime() {
    const el = document.getElementById('currentDateTime');
    if (!el) return;

    function update() {
        const now = new Date();
        const options = {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        };
        el.textContent = now.toLocaleDateString('en-US', options);
    }

    update();
    setInterval(update, 1000);
}

// ============================================================
// SETUP BOOKING DATE FILTERS WITH FLATPICKR
// ============================================================
function setupBookingDateFilters() {
    const checkinFilter = document.getElementById('bookingCheckinFilter');
    const checkoutFilter = document.getElementById('bookingCheckoutFilter');

    if (checkinFilter && typeof flatpickr === 'function') {
        flatpickr(checkinFilter, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            allowInput: false,
            onChange: function() {
                applyBookingFilters();
            }
        });
    }

    if (checkoutFilter && typeof flatpickr === 'function') {
        flatpickr(checkoutFilter, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            allowInput: false,
            onChange: function() {
                applyBookingFilters();
            }
        });
    }
}

// ============================================================
// SETUP BOOKING FILTER EVENTS
// ============================================================
function setupBookingFilterEvents() {
    const searchInput = document.getElementById('bookingSearchInput');
    const hotelFilter = document.getElementById('bookingHotelFilter');
    const statusFilter = document.getElementById('bookingStatusFilter');

    if (searchInput) {
        searchInput.addEventListener('input', function() {
            applyBookingFilters();
        });
    }
    if (hotelFilter) {
        hotelFilter.addEventListener('change', function() {
            applyBookingFilters();
        });
    }
    if (statusFilter) {
        statusFilter.addEventListener('change', function() {
            applyBookingFilters();
        });
    }
}

// ============================================================
// BOOKINGS FILTER FUNCTIONS
// ============================================================
function applyBookingFilters() {
    const searchInput = document.getElementById('bookingSearchInput');
    const hotelFilter = document.getElementById('bookingHotelFilter');
    const statusFilter = document.getElementById('bookingStatusFilter');
    const checkinFilter = document.getElementById('bookingCheckinFilter');
    const checkoutFilter = document.getElementById('bookingCheckoutFilter');

    const keyword = searchInput ? searchInput.value.trim().toLowerCase() : '';
    const hotelVal = hotelFilter ? hotelFilter.value : 'all';
    const statusVal = statusFilter ? statusFilter.value : 'all';
    const checkinVal = checkinFilter ? checkinFilter.value : '';
    const checkoutVal = checkoutFilter ? checkoutFilter.value : '';

    const tbody = document.getElementById('bookingTableBody');
    if (!tbody) return;

    // Get all rows EXCEPT empty row
    const allRows = tbody.querySelectorAll('tr');
    const rows = Array.from(allRows).filter(row => {
        return !row.querySelector('td[colspan]');
    });

    let visibleCount = 0;

    // Remove existing empty row
    const existingEmpty = tbody.querySelector('.booking-empty-row');
    if (existingEmpty) {
        existingEmpty.remove();
    }

    if (rows.length === 0) return;

    rows.forEach((row) => {
        let show = true;

        const cells = row.querySelectorAll('td');
        if (cells.length < 9) return;

        const bookingId = cells[0] ? cells[0].textContent.trim().replace('#', '') : '';
        const customerName = cells[1] ? cells[1].textContent.trim().toLowerCase() : '';
        const hotelName = cells[2] ? cells[2].textContent.trim().toLowerCase() : '';
        const roomType = cells[3] ? cells[3].textContent.trim().toLowerCase() : '';
        const checkinText = cells[4] ? cells[4].textContent.trim() : '';
        const checkoutText = cells[5] ? cells[5].textContent.trim() : '';
        const statusText = cells[7] ? cells[7].textContent.trim() : '';

        // 1. Filter by Keyword
        if (keyword) {
            const searchMatch = customerName.includes(keyword) ||
                bookingId.includes(keyword) ||
                hotelName.includes(keyword) ||
                roomType.includes(keyword);
            if (!searchMatch) {
                show = false;
            }
        }

        // 2. Filter by Hotel
        if (show && hotelVal !== 'all') {
            const hotelOption = document.querySelector(`#bookingHotelFilter option[value="${hotelVal}"]`);
            const hotelNameFilter = hotelOption ? hotelOption.textContent.trim().toLowerCase() : '';
            if (!hotelName.includes(hotelNameFilter)) {
                show = false;
            }
        }

        // 3. Filter by Status
        if (show && statusVal !== 'all' && statusText !== statusVal) {
            show = false;
        }

        // 4. Filter by Check-in Date
        if (show && checkinVal && checkinText !== checkinVal) {
            show = false;
        }

        // 5. Filter by Check-out Date
        if (show && checkoutVal && checkoutText !== checkoutVal) {
            show = false;
        }

        if (show) {
            row.style.display = '';
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });

    // Show empty state if no results
    if (visibleCount === 0 && rows.length > 0) {
        const emptyRow = document.createElement('tr');
        emptyRow.className = 'booking-empty-row';
        emptyRow.innerHTML = `
            <td colspan="9" class="text-center text-muted py-5">
                No bookings found matching your filters.
            </td>
        `;
        tbody.appendChild(emptyRow);
    }
}

function clearBookingFilters() {
    const searchInput = document.getElementById('bookingSearchInput');
    const hotelFilter = document.getElementById('bookingHotelFilter');
    const statusFilter = document.getElementById('bookingStatusFilter');
    const checkinFilter = document.getElementById('bookingCheckinFilter');
    const checkoutFilter = document.getElementById('bookingCheckoutFilter');

    if (searchInput) searchInput.value = '';
    if (hotelFilter) hotelFilter.value = 'all';
    if (statusFilter) statusFilter.value = 'all';
    if (checkinFilter) {
        const fp = checkinFilter._flatpickr;
        if (fp) fp.clear();
        checkinFilter.value = '';
    }
    if (checkoutFilter) {
        const fp = checkoutFilter._flatpickr;
        if (fp) fp.clear();
        checkoutFilter.value = '';
    }

    applyBookingFilters();
}

function openAddHotelModal() {
    var modal = new bootstrap.Modal(document.getElementById('addHotelModal'));
    modal.show();
}

function openAddPromotionModal() {
    var modal = new bootstrap.Modal(document.getElementById('addPromotionModal'));
    modal.show();
}

// ============================================================
// FORMAT DATE YYYY-MM-DD TO DD/MM/YYYY
// ============================================================
function formatDateDisplay(dateStr) {
    if (!dateStr) return '';
    // Nếu đã có định dạng DD/MM/YYYY thì giữ nguyên
    if (dateStr.match(/^\d{2}\/\d{2}\/\d{4}$/)) {
        return dateStr;
    }
    // Chuyển từ YYYY-MM-DD sang DD/MM/YYYY
    const parts = dateStr.split('-');
    if (parts.length === 3) {
        return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    return dateStr;
}

// ============================================================
// FORMAT ALL BOOKING DATES
// ============================================================
function formatAllBookingDates() {
    const rows = document.querySelectorAll('#bookingTableBody tr');
    rows.forEach(row => {
        const cells = row.querySelectorAll('td');
        if (cells.length >= 6) {
            // Check-in date (cell index 4)
            const checkinCell = cells[4];
            if (checkinCell) {
                const dateStr = checkinCell.textContent.trim();
                if (dateStr) {
                    const formatted = formatDateDisplay(dateStr);
                    if (formatted) checkinCell.textContent = formatted;
                }
            }
            // Check-out date (cell index 5)
            const checkoutCell = cells[5];
            if (checkoutCell) {
                const dateStr = checkoutCell.textContent.trim();
                if (dateStr) {
                    const formatted = formatDateDisplay(dateStr);
                    if (formatted) checkoutCell.textContent = formatted;
                }
            }
        }
    });
}