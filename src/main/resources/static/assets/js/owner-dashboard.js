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

    // 4. Add Hotel Modal - Open
    window.openAddHotelModal = function() {
        const modal = document.getElementById('addHotelModal');
        if (modal) {
            const bsModal = new bootstrap.Modal(modal);
            bsModal.show();
        }
    };

    // 5. Add Hotel Modal - Close
    window.closeAddHotelModal = function() {
        const modal = document.getElementById('addHotelModal');
        if (modal) {
            const bsModal = bootstrap.Modal.getInstance(modal);
            if (bsModal) {
                bsModal.hide();
            }
        }
    };

    // 6. Apply Booking Filters on page load
    setTimeout(applyBookingFilters, 500);

    // 7. Booking Filter Input Events
    const searchInput = document.getElementById('bookingSearchInput');
    const hotelFilter = document.getElementById('bookingHotelFilter');
    const statusFilter = document.getElementById('bookingStatusFilter');
    const checkinFilter = document.getElementById('bookingCheckinFilter');
    const checkoutFilter = document.getElementById('bookingCheckoutFilter');

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
    if (checkinFilter) {
        checkinFilter.addEventListener('change', function() {
            applyBookingFilters();
        });
    }
    if (checkoutFilter) {
        checkoutFilter.addEventListener('change', function() {
            applyBookingFilters();
        });
    }

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
// BOOKINGS FILTER FUNCTIONS - FIXED
// ============================================================

function applyBookingFilters() {
    console.log('🔍 applyBookingFilters called');

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

    console.log('📋 Filter values:', { keyword, hotelVal, statusVal, checkinVal, checkoutVal });

    const tbody = document.getElementById('bookingTableBody');
    if (!tbody) {
        console.warn('bookingTableBody not found');
        return;
    }

    // Lấy tất cả rows TRỪ row empty
    const allRows = tbody.querySelectorAll('tr');
    const rows = Array.from(allRows).filter(row => {
        return !row.querySelector('td[colspan]');
    });

    console.log('Total rows to filter:', rows.length);

    let visibleCount = 0;

    // Remove existing empty row
    const existingEmpty = tbody.querySelector('.booking-empty-row');
    if (existingEmpty) {
        existingEmpty.remove();
    }

    if (rows.length === 0) {
        console.log('ℹNo rows to filter');
        return;
    }

    rows.forEach((row, index) => {
        let show = true;

        const cells = row.querySelectorAll('td');
        if (cells.length < 9) {
            console.warn(`⚠️ Row ${index} has less than 9 cells:`, cells.length);
            return;
        }

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

    console.log(`Filter applied - Visible: ${visibleCount}/${rows.length}`);

    // Show empty state if no results (ĐÃ BỎ ICON)
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
    console.log(' clearBookingFilters called');

    const searchInput = document.getElementById('bookingSearchInput');
    const hotelFilter = document.getElementById('bookingHotelFilter');
    const statusFilter = document.getElementById('bookingStatusFilter');
    const checkinFilter = document.getElementById('bookingCheckinFilter');
    const checkoutFilter = document.getElementById('bookingCheckoutFilter');

    if (searchInput) searchInput.value = '';
    if (hotelFilter) hotelFilter.value = 'all';
    if (statusFilter) statusFilter.value = 'all';
    if (checkinFilter) checkinFilter.value = '';
    if (checkoutFilter) checkoutFilter.value = '';

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