// src/main/resources/static/assets/js/owner-dashboard.js

// ===== CSRF HELPER =====
function getCsrfHeaders() {
    var tokenMeta = document.querySelector('meta[name="_csrf"]');
    var headerMeta = document.querySelector('meta[name="_csrf_header"]');
    var headers = {};
    if (tokenMeta && headerMeta) {
        headers[headerMeta.getAttribute('content')] = tokenMeta.getAttribute('content');
    }
    return headers;
}

document.addEventListener('DOMContentLoaded', function() {

    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }

    updateDateTime();

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

    const urlParams = new URLSearchParams(window.location.search);
    const tabParam = urlParams.get('tab');
    if (tabParam) {
        const targetLink = document.querySelector(`.sidebar-link[data-tab="${tabParam}"]`);
        const targetPanel = document.getElementById(tabParam);
        if (targetLink && targetPanel) {
            sidebarLinks.forEach(l => l.classList.remove('active'));
            tabPanels.forEach(panel => panel.classList.remove('active'));
            targetLink.classList.add('active');
            targetPanel.classList.add('active');
        }
    }

    setupBookingDateFilters();
    setupBookingFilterEvents();

    window.openAddHotelModal = function() {
        const modal = document.getElementById('addHotelModal');
        if (modal) {
            const bsModal = new bootstrap.Modal(modal);
            bsModal.show();
        }
    };

    window.closeAddHotelModal = function() {
        const modal = document.getElementById('addHotelModal');
        if (modal) {
            const bsModal = bootstrap.Modal.getInstance(modal);
            if (bsModal) {
                bsModal.hide();
            }
        }
    };

    setTimeout(applyBookingFilters, 500);
    setTimeout(formatAllBookingDates, 600);
});

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

function setupBookingDateFilters() {
    const checkinFilter = document.getElementById('bookingCheckinFilter');
    const checkoutFilter = document.getElementById('bookingCheckoutFilter');

    let fpCheckout;

    // Helper to parse d/m/Y date string
    function parseDMY(dateStr) {
        if (!dateStr) return null;
        const parts = dateStr.split('/');
        if (parts.length === 3) {
            const day = parseInt(parts[0], 10);
            const month = parseInt(parts[1], 10) - 1;
            const year = parseInt(parts[2], 10);
            return new Date(year, month, day);
        }
        return null;
    }

    let initMinCheckout = null;
    if (checkinFilter && checkinFilter.value) {
        const checkinDate = parseDMY(checkinFilter.value);
        if (checkinDate) {
            initMinCheckout = new Date(checkinDate);
            initMinCheckout.setDate(initMinCheckout.getDate() + 1);
        }
    }

    if (checkoutFilter && typeof flatpickr === 'function') {
        fpCheckout = flatpickr(checkoutFilter, {
            dateFormat: "d/m/Y",
            altInput: true,
            altFormat: "d/m/Y",
            allowInput: false,
            minDate: initMinCheckout,
            defaultDate: checkoutFilter.value ? parseDMY(checkoutFilter.value) : null,
            onChange: function() {
                applyBookingFilters();
            }
        });
    }

    if (checkinFilter && typeof flatpickr === 'function') {
        flatpickr(checkinFilter, {
            dateFormat: "d/m/Y",
            altInput: true,
            altFormat: "d/m/Y",
            allowInput: false,
            defaultDate: checkinFilter.value ? parseDMY(checkinFilter.value) : null,
            onChange: function(selectedDates) {
                if (fpCheckout) {
                    if (selectedDates[0]) {
                        const minDate = new Date(selectedDates[0]);
                        minDate.setDate(minDate.getDate() + 1);
                        fpCheckout.set('minDate', minDate);
                    } else {
                        fpCheckout.set('minDate', null);
                    }
                }
                applyBookingFilters();
            }
        });
    }
}

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

    const allRows = tbody.querySelectorAll('tr');
    const rows = Array.from(allRows).filter(row => {
        return !row.querySelector('td[colspan]');
    });

    let visibleCount = 0;

    const existingEmpty = tbody.querySelector('.booking-empty-row');
    if (existingEmpty) {
        existingEmpty.remove();
    }

    if (rows.length === 0) return;

    rows.forEach((row) => {
        let show = true;

        const cells = row.querySelectorAll('td');
        if (cells.length < 8) return;

        const bookingId = cells[0] ? cells[0].textContent.trim().replace('#', '') : '';
        const customerName = cells[1] ? cells[1].textContent.trim().toLowerCase() : '';
        const hotelName = cells[2] ? cells[2].textContent.trim().toLowerCase() : '';
        const statusText = cells[3] ? cells[3].textContent.trim() : '';
        const checkinText = cells[4] ? cells[4].textContent.trim() : '';
        const checkoutText = cells[5] ? cells[5].textContent.trim() : '';

        const actionBtn = cells[7] ? cells[7].querySelector('.view-detail-btn') : null;
        const dataStatus = actionBtn ? (actionBtn.getAttribute('data-status') || '') : '';

        if (keyword) {
            const searchMatch = customerName.includes(keyword) ||
                bookingId.includes(keyword) ||
                hotelName.includes(keyword);
            if (!searchMatch) {
                show = false;
            }
        }

        if (show && hotelVal !== 'all') {
            const hotelOption = document.querySelector(`#bookingHotelFilter option[value="${hotelVal}"]`);
            const hotelNameFilter = hotelOption ? hotelOption.textContent.trim().toLowerCase() : '';
            if (!hotelName.includes(hotelNameFilter)) {
                show = false;
            }
        }

        if (show && statusVal !== 'all') {
            if (dataStatus !== statusVal) {
                show = false;
            }
        }

        // ============================================================
        // FILTER THEO KHOẢNG NGÀY: check-in (từ) -> check-out (đến)
        // Hiển thị booking nào có check-in VÀ check-out đều nằm
        // trong khoảng [checkinVal, checkoutVal] đã chọn
        // ============================================================
        if (show && (checkinVal || checkoutVal)) {
            const rangeStart = checkinVal ? parseDateDMY(checkinVal) : null;
            const rangeEnd = checkoutVal ? parseDateDMY(checkoutVal) : null;

            const bookingCheckin = parseDateDMY(checkinText);
            const bookingCheckout = parseDateDMY(checkoutText);

            if (rangeStart) rangeStart.setHours(0, 0, 0, 0);
            if (rangeEnd) rangeEnd.setHours(23, 59, 59, 999);

            if (rangeStart && bookingCheckin) {
                if (bookingCheckin < rangeStart) show = false;
            } else if (rangeStart) {
                // Không parse được ngày check-in của booking -> loại
                show = false;
            }

            if (show && rangeEnd && bookingCheckout) {
                if (bookingCheckout > rangeEnd) show = false;
            } else if (show && rangeEnd) {
                show = false;
            }
        }

        if (show) {
            row.style.display = '';
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });

    if (visibleCount === 0 && rows.length > 0) {
        const emptyRow = document.createElement('tr');
        emptyRow.className = 'booking-empty-row';
        emptyRow.innerHTML = `
            <td colspan="8" class="text-center text-muted py-5">
                No bookings found matching your filters.
            </td>
        `;
        tbody.appendChild(emptyRow);
    }
}

function parseDateDMY(dateStr) {
    if (!dateStr) return null;

    dateStr = dateStr.trim();

    if (dateStr.match(/^\d{2}\/\d{2}\/\d{4}$/)) {
        const parts = dateStr.split('/');
        if (parts.length === 3) {
            const day = parseInt(parts[0], 10);
            const month = parseInt(parts[1], 10) - 1;
            const year = parseInt(parts[2], 10);
            if (!isNaN(day) && !isNaN(month) && !isNaN(year)) {
                return new Date(year, month, day);
            }
        }
    }

    return null;
}

function parseDateRange(dateStr) {
    if (!dateStr) return null;

    dateStr = dateStr.trim();

    // Nếu có dấu gạch ngang -> khoảng ngày (vd: 01/01/2026 - 15/01/2026)
    if (dateStr.includes(' - ') || dateStr.includes('–') || dateStr.includes('—')) {
        var separator = ' - ';
        if (dateStr.includes('–')) separator = ' – ';
        if (dateStr.includes('—')) separator = ' — ';

        var parts = dateStr.split(separator);
        if (parts.length === 2) {
            var startDate = parseDateDMY(parts[0].trim());
            var endDate = parseDateDMY(parts[1].trim());
            if (startDate && endDate) {
                return { start: startDate, end: endDate };
            }
        }
    }

    // Nếu chỉ có 1 ngày, trả về cùng 1 ngày cho start và end
    var singleDate = parseDateDMY(dateStr);
    if (singleDate) {
        return { start: singleDate, end: singleDate };
    }

    return null;
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
        if (fp) {
            fp.clear();
            fp.set('minDate', null);
        }
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

function formatDateDisplay(dateStr) {
    if (!dateStr) return '';
    if (dateStr.match(/^\d{2}\/\d{2}\/\d{4}$/)) {
        return dateStr;
    }
    const parts = dateStr.split('-');
    if (parts.length === 3) {
        return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    return dateStr;
}

function formatAllBookingDates() {
    const rows = document.querySelectorAll('#bookingTableBody tr');
    rows.forEach(row => {
        const cells = row.querySelectorAll('td');
        if (cells.length >= 6) {
            const checkinCell = cells[4];
            if (checkinCell) {
                const dateStr = checkinCell.textContent.trim();
                if (dateStr) {
                    const formatted = formatDateDisplay(dateStr);
                    if (formatted) checkinCell.textContent = formatted;
                }
            }
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

window.openOwnerPayoutDetail = function(bookingId, bankName, accountNumber, accountHolder, payoutAmount, feePercent, payoutAt) {
    const elBankName = document.getElementById('opd-bankName');
    const elAccountNumber = document.getElementById('opd-accountNumber');
    const elAccountHolder = document.getElementById('opd-accountHolder');
    const elFeePercent = document.getElementById('opd-feePercent');
    const elPayoutAmount = document.getElementById('opd-payoutAmount');
    const elPayoutAt = document.getElementById('opd-payoutAt');

    if (elBankName) elBankName.textContent = bankName || 'N/A';
    if (elAccountNumber) elAccountNumber.textContent = accountNumber || 'N/A';
    if (elAccountHolder) elAccountHolder.textContent = accountHolder || 'N/A';
    if (elFeePercent) elFeePercent.textContent = (feePercent || 10) + '%';

    const fmt = new Intl.NumberFormat('vi-VN', { style: 'decimal', minimumFractionDigits: 0 });
    if (elPayoutAmount) {
        elPayoutAmount.textContent = (payoutAmount ? fmt.format(payoutAmount) : '0') + ' VND';
    }
    if (elPayoutAt) {
        elPayoutAt.textContent = payoutAt ? payoutAt.replace('T', ' ').substring(0, 19) : 'N/A';
    }

    const modalEl = document.getElementById('ownerPayoutDetailModal');
    if (modalEl) {
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
    }
};

function openBookingDetail(btn) {
    var bookingData = {
        bookingId: btn.getAttribute('data-booking-id') || '?',
        customerName: btn.getAttribute('data-customer-name') || 'N/A',
        hotelName: btn.getAttribute('data-hotel-name') || 'N/A',
        roomType: btn.getAttribute('data-room-type') || 'N/A',
        checkInDate: btn.getAttribute('data-checkin') || null,
        checkOutDate: btn.getAttribute('data-checkout') || null,
        totalPrice: parseFloat(btn.getAttribute('data-total')) || 0,
        bookingStatus: btn.getAttribute('data-status') || 'PENDING',
        paymentStatus: btn.getAttribute('data-payment') || 'PENDING',
        payoutStatus: btn.getAttribute('data-payout') || '',
        ownerPayoutAmount: parseFloat(btn.getAttribute('data-payout-amount')) || 0,
        platformFeePercent: parseFloat(btn.getAttribute('data-platform-fee')) || 10,
        payoutBankName: btn.getAttribute('data-payout-bank') || '',
        payoutBankAccountNumber: btn.getAttribute('data-payout-account') || '',
        payoutBankAccountHolder: btn.getAttribute('data-payout-holder') || '',
        checkInStatus: btn.getAttribute('data-checkin-status') === 'true',
        checkOutStatus: btn.getAttribute('data-checkout-status') === 'true',
        specialNotes: btn.getAttribute('data-notes') || ''
    };

    renderBookingDetail(bookingData);

    var modalEl = document.getElementById('bookingDetailModal');
    if (!modalEl) {
        console.error('bookingDetailModal not found in DOM');
        return;
    }
    var modal = new bootstrap.Modal(modalEl);
    modal.show();

    if (bookingData.bookingId && bookingData.bookingId !== '?') {
        fetch('/owner/booking/detail-data?bookingId=' + bookingData.bookingId)
            .then(function (response) {
                return response.ok ? response.json() : null;
            })
            .then(function (data) {
                if (data) renderBookingDetail(data);
            })
            .catch(function () {
                console.log('API not available, using existing data from table row');
            });
    }
}

function renderBookingDetail(data) {
    if (!data) return;

    function setText(id, value) {
        var el = document.getElementById(id);
        if (el) el.textContent = value;
    }

    setText('bd-bookingId', '#' + (data.bookingId || '?'));
    setText('bd-customerName', data.customerName || 'N/A');
    setText('bd-hotelName', data.hotelName || 'N/A');
    setText('bd-roomType', data.roomType || 'N/A');

    var checkinDisplay = formatDateDisplay(data.checkInDate);
    var checkoutDisplay = formatDateDisplay(data.checkOutDate);
    setText('bd-checkin', checkinDisplay);
    setText('bd-checkout', checkoutDisplay);
    setText('bd-checkin-display', checkinDisplay);
    setText('bd-checkout-display', checkoutDisplay);

    setText('bd-total', formatVND(parseFloat(data.totalPrice) || 0));

    var status = data.bookingStatus || 'PENDING';
    var statusBadge = document.getElementById('bd-status-badge');
    if (statusBadge) {
        statusBadge.textContent = status;
        statusBadge.className = 'booking-status-badge ' + statusColorClasses(status);
    }

    var payment = data.paymentStatus || 'PENDING';
    var paymentBadge = document.getElementById('bd-payment-badge');
    if (paymentBadge) {
        paymentBadge.textContent = payment;
        paymentBadge.className = 'payment-status-badge ' + paymentColorClasses(payment);
    }

    var payout = data.payoutStatus || '';
    var payoutBadge = document.getElementById('bd-payout-badge');
    var payoutDetail = document.getElementById('bd-payout-detail');

    if (payoutBadge && payoutDetail) {
        if (status === 'COMPLETED' && payout === 'PAID') {
            payoutBadge.textContent = 'PAID';
            payoutBadge.className = 'badge bg-success';
            payoutDetail.style.display = 'block';
            setText('bd-payout-bank', data.payoutBankName || '-');
            setText('bd-payout-account', data.payoutBankAccountNumber || '-');
            setText('bd-payout-holder', data.payoutBankAccountHolder || '-');
            setText('bd-payout-amount', formatVND(parseFloat(data.ownerPayoutAmount) || 0));
            setText('bd-platform-fee', (data.platformFeePercent || 10) + '%');
        } else if (status === 'COMPLETED' && payout === 'PENDING') {
            payoutBadge.textContent = 'PENDING';
            payoutBadge.className = 'badge bg-warning text-dark';
            payoutDetail.style.display = 'none';
        } else {
            payoutBadge.textContent = '-';
            payoutBadge.className = 'badge bg-light text-muted';
            payoutDetail.style.display = 'none';
        }
    }

    var isEditable = status === 'CONFIRMED' || status === 'COMPLETED';

    var checkinBtn = document.getElementById('bd-checkin-btn');
    var uncheckinBtn = document.getElementById('bd-uncheckin-btn');
    var checkinLabel = document.getElementById('bd-checkin-label');
    if (checkinBtn && uncheckinBtn && checkinLabel) {
        if (!isEditable) {
            checkinBtn.style.display = 'none';
            uncheckinBtn.style.display = 'none';
        } else {
            if (data.checkInStatus === true) {
                checkinBtn.style.display = 'none';
                uncheckinBtn.style.display = 'inline-block';
                checkinLabel.textContent = 'Checked In';
                checkinLabel.className = 'small text-success fw-semibold';
            } else {
                checkinBtn.style.display = 'inline-block';
                uncheckinBtn.style.display = 'none';
                checkinLabel.textContent = 'Not Checked In';
                checkinLabel.className = 'small text-secondary';
            }
        }
        updateActualTimeDisplay('bd-checkin-actual', data.checkedInAt);
    }

    var checkoutBtn = document.getElementById('bd-checkout-btn');
    var uncheckoutBtn = document.getElementById('bd-uncheckout-btn');
    var checkoutLabel = document.getElementById('bd-checkout-label');
    if (checkoutBtn && uncheckoutBtn && checkoutLabel) {
        if (!isEditable) {
            checkoutBtn.style.display = 'none';
            uncheckoutBtn.style.display = 'none';
        } else {
            if (data.checkOutStatus === true) {
                checkoutBtn.style.display = 'none';
                uncheckoutBtn.style.display = 'inline-block';
                checkoutLabel.textContent = 'Checked Out';
                checkoutLabel.className = 'small text-success fw-semibold';
            } else {
                checkoutBtn.style.display = 'inline-block';
                uncheckoutBtn.style.display = 'none';
                checkoutLabel.textContent = 'Not Checked Out';
                checkoutLabel.className = 'small text-secondary';
                if (data.checkInStatus !== true) {
                    checkoutBtn.disabled = true;
                    checkoutBtn.title = "Guest must check in first";
                } else {
                    checkoutBtn.disabled = false;
                    checkoutBtn.title = "";
                }
            }
        }
        updateActualTimeDisplay('bd-checkout-actual', data.checkedOutAt);
    }

    var notes = (data.specialNotes || '').trim();
    var notesContainer = document.getElementById('bd-special-notes');
    var notesContent = document.getElementById('bd-notes-content');
    if (notesContainer && notesContent) {
        if (notes !== '') {
            notesContainer.style.display = 'block';
            notesContent.textContent = notes;
        } else {
            notesContainer.style.display = 'none';
        }
    }

    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
}

function statusColorClasses(status) {
    switch (status) {
        case 'CONFIRMED': return 'bg-success-light text-success border-success';
        case 'COMPLETED': return 'bg-info-light text-info border-info';
        case 'PENDING': return 'bg-warning-light text-warning border-warning';
        case 'CANCELLED': return 'bg-danger-light text-danger border-danger';
        default: return 'bg-light-secondary text-secondary border-secondary';
    }
}

function paymentColorClasses(payment) {
    switch (payment) {
        case 'PAID': return 'bg-success-light text-success border-success';
        case 'PENDING': return 'bg-warning-light text-warning border-warning';
        case 'FAILED': return 'bg-danger-light text-danger border-danger';
        case 'REFUNDED': return 'bg-info-light text-info border-info';
        default: return 'bg-light-secondary text-secondary border-secondary';
    }
}

function formatActualDateTime(isoString) {
    if (!isoString) return '';
    var d = new Date(isoString);
    if (isNaN(d.getTime())) return '';
    var day = String(d.getDate()).padStart(2, '0');
    var month = String(d.getMonth() + 1).padStart(2, '0');
    var year = d.getFullYear();
    var hours = String(d.getHours()).padStart(2, '0');
    var minutes = String(d.getMinutes()).padStart(2, '0');
    return day + '/' + month + '/' + year + ' ' + hours + ':' + minutes;
}

function updateActualTimeDisplay(elementId, isoString) {
    var el = document.getElementById(elementId);
    if (!el) return;
    if (isoString) {
        el.textContent = 'At: ' + formatActualDateTime(isoString);
        el.style.display = 'block';
    } else {
        el.textContent = '';
        el.style.display = 'none';
    }
}

function formatVND(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'decimal', minimumFractionDigits: 0 }).format(amount) + ' VND';
}

function triggerCheckInConfirm() {
    var bookingId = (document.getElementById('bd-bookingId').textContent || '').replace('#', '');
    if (!bookingId || bookingId === '?') {
        showToast('Invalid booking ID', 'error');
        return;
    }

    showActionConfirmModal(
        'Confirm Check-in',
        'Are you sure you want to perform <strong>Check-in</strong> for this booking?',
        'btn-gold',
        'Check-in',
        function() {
            executeCheckInUpdate(bookingId, true);
        }
    );
}

function triggerUnCheckInConfirm() {
    var bookingId = (document.getElementById('bd-bookingId').textContent || '').replace('#', '');
    if (!bookingId || bookingId === '?') {
        showToast('Invalid booking ID', 'error');
        return;
    }

    var checkoutBtn = document.getElementById('bd-checkout-btn');
    if (checkoutBtn && checkoutBtn.style.display === 'none') {
        showToast('Cannot undo check-in because the guest has already checked out.', 'error');
        return;
    }

    showActionConfirmModal(
        'Confirm Undo Check-in',
        'Are you sure you want to <strong>Undo Check-in</strong> for this booking?',
        'btn-secondary',
        'Undo',
        function() {
            executeCheckInUpdate(bookingId, false);
        }
    );
}

function executeCheckInUpdate(bookingId, checked) {
    var checkinBtn = document.getElementById('bd-checkin-btn');
    var uncheckinBtn = document.getElementById('bd-uncheckin-btn');
    var checkinLabel = document.getElementById('bd-checkin-label');

    if (checkinBtn) checkinBtn.disabled = true;
    if (uncheckinBtn) uncheckinBtn.disabled = true;

    fetch('/owner/booking/update-checkin', {
        method: 'POST',
        headers: Object.assign(
            { 'Content-Type': 'application/x-www-form-urlencoded' },
            getCsrfHeaders()
        ),
        body: 'bookingId=' + bookingId + '&checkedIn=' + checked
    })
        .then(function (response) { return response.json(); })
        .then(function (data) {
            if (checkinBtn) checkinBtn.disabled = false;
            if (uncheckinBtn) uncheckinBtn.disabled = false;

            if (data.success) {
                if (checked) {
                    if (checkinBtn) checkinBtn.style.display = 'none';
                    if (uncheckinBtn) uncheckinBtn.style.display = 'inline-block';
                    if (checkinLabel) {
                        checkinLabel.textContent = 'Checked In';
                        checkinLabel.className = 'small text-success fw-semibold';
                    }
                } else {
                    if (checkinBtn) checkinBtn.style.display = 'inline-block';
                    if (uncheckinBtn) uncheckinBtn.style.display = 'none';
                    if (checkinLabel) {
                        checkinLabel.textContent = 'Not Checked In';
                        checkinLabel.className = 'small text-secondary';
                    }
                }

                updateActualTimeDisplay('bd-checkin-actual', data.checkedInAt);
                showToast('Check-in status updated successfully!', 'success');

                setTimeout(function() {
                    window.location.reload();
                }, 1000);
            } else {
                showToast(data.message || 'Failed to update check-in status.', 'error');
            }
        })
        .catch(function (err) {
            if (checkinBtn) checkinBtn.disabled = false;
            if (uncheckinBtn) uncheckinBtn.disabled = false;
            showToast('Error updating check-in status.', 'error');
            console.error('Error:', err);
        });
}

function triggerCheckOutConfirm() {
    var bookingId = (document.getElementById('bd-bookingId').textContent || '').replace('#', '');
    if (!bookingId || bookingId === '?') {
        showToast('Invalid booking ID', 'error');
        return;
    }

    showActionConfirmModal(
        'Confirm Check-out',
        'Are you sure you want to perform <strong>Check-out</strong> for this booking? This will change the booking status to <strong>COMPLETED</strong>.',
        'btn-danger',
        'Check-out',
        function() {
            executeCheckOutUpdate(bookingId, true);
        }
    );
}

function triggerUnCheckOutConfirm() {
    var bookingId = (document.getElementById('bd-bookingId').textContent || '').replace('#', '');
    if (!bookingId || bookingId === '?') {
        showToast('Invalid booking ID', 'error');
        return;
    }

    showActionConfirmModal(
        'Confirm Undo Check-out',
        'Are you sure you want to <strong>Undo Check-out</strong> for this booking? This will change the booking status back to <strong>CONFIRMED</strong>.',
        'btn-secondary',
        'Undo',
        function() {
            executeCheckOutUpdate(bookingId, false);
        }
    );
}

function showActionConfirmModal(title, body, btnClass, btnText, onConfirm) {
    var modalEl = document.getElementById('confirmActionModal');

    if (!modalEl) {
        modalEl = document.createElement('div');
        modalEl.className = 'modal fade';
        modalEl.id = 'confirmActionModal';
        modalEl.tabIndex = '-1';
        modalEl.setAttribute('aria-hidden', 'true');
        modalEl.innerHTML = `
            <div class="modal-dialog modal-dialog-centered" style="max-width: 400px;">
                <div class="modal-content border-0 shadow-lg">
                    <div class="modal-header border-0 pb-0">
                        <h5 class="fw-bold mb-0" id="confirmActionTitle">Confirm Action</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body py-3" id="confirmActionBody">
                        Are you sure you want to perform this action?
                    </div>
                    <div class="modal-footer border-0 pt-0">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                        <button type="button" class="btn" id="confirmActionSubmitBtn">Confirm</button>
                    </div>
                </div>
            </div>
        `;
        document.body.appendChild(modalEl);
    }

    var titleEl = document.getElementById('confirmActionTitle');
    var bodyEl = document.getElementById('confirmActionBody');
    var submitBtn = document.getElementById('confirmActionSubmitBtn');

    if (titleEl) titleEl.textContent = title;
    if (bodyEl) bodyEl.innerHTML = body;

    if (submitBtn) {
        submitBtn.textContent = btnText || 'Confirm';
        submitBtn.className = 'btn ' + (btnClass || 'btn-gold');
    }

    var existingModal = bootstrap.Modal.getInstance(modalEl);
    if (existingModal) {
        existingModal.dispose();
    }

    var bsModal = new bootstrap.Modal(modalEl, {
        backdrop: 'static',
        keyboard: false
    });

    var newSubmitBtn = submitBtn.cloneNode(true);
    if (submitBtn) {
        submitBtn.parentNode.replaceChild(newSubmitBtn, submitBtn);
    }

    newSubmitBtn.onclick = function() {
        bsModal.hide();
        setTimeout(function() {
            if (typeof onConfirm === 'function') {
                onConfirm();
            }
        }, 300);
    };

    document.querySelectorAll('.modal-backdrop').forEach(function(el) {
        el.remove();
    });
    document.body.classList.remove('modal-open');
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';

    bsModal.show();
}

function executeCheckOutUpdate(bookingId, checked) {
    var checkoutBtn = document.getElementById('bd-checkout-btn');
    var uncheckoutBtn = document.getElementById('bd-uncheckout-btn');
    var checkoutLabel = document.getElementById('bd-checkout-label');

    if (checkoutBtn) checkoutBtn.disabled = true;
    if (uncheckoutBtn) uncheckoutBtn.disabled = true;

    fetch('/owner/booking/update-checkout', {
        method: 'POST',
        headers: Object.assign(
            { 'Content-Type': 'application/x-www-form-urlencoded' },
            getCsrfHeaders()
        ),
        body: 'bookingId=' + bookingId + '&checkedOut=' + checked
    })
        .then(function (response) { return response.json(); })
        .then(function (data) {
            if (checkoutBtn) checkoutBtn.disabled = false;
            if (uncheckoutBtn) uncheckoutBtn.disabled = false;

            if (data.success) {
                if (checked) {
                    if (checkoutBtn) checkoutBtn.style.display = 'none';
                    if (uncheckoutBtn) uncheckoutBtn.style.display = 'inline-block';
                    if (checkoutLabel) {
                        checkoutLabel.textContent = 'Checked Out';
                        checkoutLabel.className = 'small text-success fw-semibold';
                    }
                } else {
                    if (checkoutBtn) checkoutBtn.style.display = 'inline-block';
                    if (uncheckoutBtn) uncheckoutBtn.style.display = 'none';
                    if (checkoutLabel) {
                        checkoutLabel.textContent = 'Not Checked Out';
                        checkoutLabel.className = 'small text-secondary';
                    }
                }

                var statusBadge = document.getElementById('bd-status-badge');
                if (statusBadge && data.bookingStatus) {
                    statusBadge.textContent = data.bookingStatus;
                    statusBadge.className = 'booking-status-badge ' + statusColorClasses(data.bookingStatus);
                }

                updateActualTimeDisplay('bd-checkout-actual', data.checkedOutAt);
                showToast('Check-out status updated successfully!', 'success');

                setTimeout(function() {
                    window.location.reload();
                }, 1000);
            } else {
                showToast(data.message || 'Failed to update check-out status.', 'error');
            }
        })
        .catch(function (err) {
            if (checkoutBtn) checkoutBtn.disabled = false;
            if (uncheckoutBtn) uncheckoutBtn.disabled = false;
            showToast('Error updating check-out status.', 'error');
            console.error('Error:', err);
        });
}

function showToast(message, type) {
    var oldToasts = document.querySelectorAll('.custom-toast-card');
    oldToasts.forEach(function(el) {
        el.remove();
    });

    var toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toastContainer';
        toastContainer.style.cssText = 'position:fixed; top:20px; right:20px; z-index:9999; display:flex; flex-direction:column; gap:8px;';
        document.body.appendChild(toastContainer);
    }

    var toast = document.createElement('div');
    var isSuccess = type === 'success';
    toast.className = 'custom-toast-card ' + (isSuccess ? 'toast-success' : 'toast-error');
    toast.style.cssText = 'pointer-events:auto; min-width:320px; max-width:450px; background:#ffffff; border-radius:10px; padding:14px 20px; box-shadow:0 10px 25px rgba(0,0,0,0.12); border-left:5px solid ' + (isSuccess ? '#10b981' : '#ef4444') + '; transform:translateX(120%); animation:toast-slide-in 0.35s forwards cubic-bezier(0.16, 1, 0.3, 1);';

    toast.innerHTML = `
        <div class="d-flex align-items-center gap-3">
            <div style="flex-shrink:0; width:32px; height:32px; border-radius:50%; display:flex; align-items:center; justify-content:center; background:${isSuccess ? '#dcfce7' : '#fee2e2'};">
                <i data-lucide="${isSuccess ? 'check-circle' : 'alert-circle'}" style="width:18px; height:18px; color:${isSuccess ? '#10b981' : '#ef4444'};"></i>
            </div>
            <div style="flex:1;">
                <p style="margin:0; font-size:0.88rem; font-weight:600; color:#1e293b;">${message}</p>
            </div>
            <button onclick="this.parentElement.parentElement.remove()" style="background:none; border:none; color:#94a3b8; cursor:pointer; font-size:1.1rem;">&times;</button>
        </div>
    `;

    toastContainer.appendChild(toast);

    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }

    setTimeout(function() {
        if (toast.parentNode) {
            toast.style.transform = 'translateX(120%)';
            toast.style.opacity = '0';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(function() {
                if (toast.parentNode) toast.remove();
            }, 350);
        }
    }, 3500);
}