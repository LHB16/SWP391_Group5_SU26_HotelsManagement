// ============================================================
// GLOBAL VARIABLES
// ============================================================
let allPromotionsData = [];
let allHotelsData = [];
let isLoading = false;

// ============================================================
// HELPER: Format Date YYYY-MM-DD to DD/MM/YYYY
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
// LOAD PROMOTIONS
// ============================================================
function loadPromotions() {
    if (isLoading) {
        return;
    }

    isLoading = true;

    const tbody = document.getElementById('promotionTableBody');
    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-muted py-4">
                    <span class="spinner-border spinner-border-sm me-2" role="status"></span>
                    Loading promotions...
                </td>
            </tr>
        `;
    }

    fetch('/owner/promotions/list')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            if (data.error) {
                showPromotionError(data.error);
                isLoading = false;
                return;
            }

            allPromotionsData = [];
            allHotelsData = data.hotels || [];

            if (data.promotionsByHotel) {
                Object.values(data.promotionsByHotel).forEach(promos => {
                    if (Array.isArray(promos)) {
                        allPromotionsData = allPromotionsData.concat(promos);
                    }
                });
            }

            const countEl = document.getElementById('totalPromotionsCount');
            if (countEl) {
                countEl.textContent = allPromotionsData.length;
            }

            renderPromotions(allPromotionsData);
            isLoading = false;
        })
        .catch(error => {
            showPromotionError('Failed to load promotions. Please refresh the page.');
            isLoading = false;
        });
}

// ============================================================
// SHOW ERROR
// ============================================================
function showPromotionError(message) {
    const tbody = document.getElementById('promotionTableBody');
    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-danger py-4">
                    <i data-lucide="alert-circle" class="d-block mx-auto mb-2" style="width: 32px; height: 32px;"></i>
                    ${message}
                </td>
            </tr>
        `;
        if (typeof lucide !== 'undefined') lucide.createIcons();
    }
}

// ============================================================
// RENDER PROMOTIONS
// ============================================================
function renderPromotions(promotions) {
    const tbody = document.getElementById('promotionTableBody');
    if (!tbody) return;

    if (!promotions || promotions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-muted py-4">
                    <i data-lucide="inbox" class="d-block mx-auto mb-2" style="width: 32px; height: 32px; opacity: 0.4;"></i>
                    No promotions found.
                </td>
            </tr>
        `;
        if (typeof lucide !== 'undefined') lucide.createIcons();
        return;
    }

    let html = '';
    promotions.forEach(promo => {
        const statusClass = promo.status === 'ACTIVE' ? 'promo-status-active' :
            promo.status === 'EXPIRED' ? 'promo-status-expired' :
                'promo-status-inactive';

        let hotelName = '';
        if (allHotelsData) {
            const hotel = allHotelsData.find(h => h.id === promo.hotelId);
            if (hotel) hotelName = hotel.name;
        }

        // Format date từ YYYY-MM-DD sang DD/MM/YYYY
        const startDate = formatDateDisplay(promo.startDate);
        const endDate = formatDateDisplay(promo.endDate);

        html += `
            <tr class="promo-table-row" data-promo-id="${promo.id}">
                <td>
                    <strong class="text-dark">${escapeHtml(promo.title)}</strong>
                    <div class="text-muted small">${escapeHtml(hotelName)}</div>
                </td>
                <td style="text-align: center;"><span class="discount-badge">-${promo.discountPercent}%</span></td>
                <td style="text-align: center;">${startDate || '-'}</td>
                <td style="text-align: center;">${endDate || '-'}</td>
                <td style="text-align: center;">
                    <span class="promo-status-badge ${statusClass}">${promo.status}</span>
                </td>
                <td style="text-align: center;">
                    <div class="d-flex gap-2 justify-content-center">
                        <button type="button" class="promo-action-btn promo-action-btn-edit" 
                                onclick="openEditPromotionModal(this)" 
                                data-promo-id="${promo.id}"
                                data-hotel-id="${promo.hotelId}"
                                data-title="${escapeHtml(promo.title)}"
                                data-description="${escapeHtml(promo.description || '')}"
                                data-discount="${promo.discountPercent}"
                                data-start="${promo.startDate || ''}"
                                data-end="${promo.endDate || ''}"
                                data-status="${promo.status}">
                            <i data-lucide="pencil" style="width: 14px; height: 14px;"></i>
                            Edit
                        </button>
                        <button type="button" class="promo-action-btn promo-action-btn-delete" 
                                data-bs-toggle="modal" 
                                data-bs-target="#deleteModalPromo-${promo.id}">
                            <i data-lucide="trash-2" style="width: 14px; height: 14px;"></i>
                            Delete
                        </button>
                    </div>
                </td>
            </tr>
        `;
    });

    tbody.innerHTML = html;

    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
}

// ============================================================
// HELPER: Escape HTML
// ============================================================
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ============================================================
// SETUP PROMOTION DATE FILTERS WITH FLATPICKR
// ============================================================
function setupPromotionDateFilters() {
    const startDateFilter = document.getElementById('promoStartDateFilter');
    const endDateFilter = document.getElementById('promoEndDateFilter');

    if (startDateFilter && typeof flatpickr === 'function') {
        flatpickr(startDateFilter, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            allowInput: false,
            onChange: function () {
                applyPromotionFilters();
            }
        });
    }

    if (endDateFilter && typeof flatpickr === 'function') {
        flatpickr(endDateFilter, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            allowInput: false,
            onChange: function () {
                applyPromotionFilters();
            }
        });
    }
}

// ============================================================
// FILTER PROMOTIONS
// ============================================================
function applyPromotionFilters() {
    const searchInput = document.getElementById('promotionSearchInput');
    const hotelFilter = document.getElementById('hotelFilterSelect');
    const statusFilter = document.getElementById('promotionStatusFilter');
    const startDateFilter = document.getElementById('promoStartDateFilter');
    const endDateFilter = document.getElementById('promoEndDateFilter');

    const searchTerm = searchInput ? searchInput.value.toLowerCase().trim() : '';
    const hotelId = hotelFilter ? hotelFilter.value : 'all';
    const status = statusFilter ? statusFilter.value : 'all';
    const startDate = startDateFilter ? startDateFilter.value : '';
    const endDate = endDateFilter ? endDateFilter.value : '';

    let filtered = allPromotionsData.filter(promo => {
        if (searchTerm && !promo.title.toLowerCase().includes(searchTerm)) {
            return false;
        }
        if (hotelId !== 'all' && promo.hotelId != hotelId) {
            return false;
        }
        if (status !== 'all' && promo.status !== status) {
            return false;
        }
        if (startDate && promo.startDate && promo.startDate < startDate) {
            return false;
        }
        if (endDate && promo.endDate && promo.endDate > endDate) {
            return false;
        }
        return true;
    });

    renderPromotions(filtered);
}

// ============================================================
// CLEAR PROMOTION FILTERS
// ============================================================
function clearPromotionFilters() {
    const searchInput = document.getElementById('promotionSearchInput');
    const hotelFilter = document.getElementById('hotelFilterSelect');
    const statusFilter = document.getElementById('promotionStatusFilter');
    const startDateFilter = document.getElementById('promoStartDateFilter');
    const endDateFilter = document.getElementById('promoEndDateFilter');

    if (searchInput) searchInput.value = '';
    if (hotelFilter) hotelFilter.value = 'all';
    if (statusFilter) statusFilter.value = 'all';

    if (startDateFilter) {
        const fp = startDateFilter._flatpickr;
        if (fp) fp.clear();
        startDateFilter.value = '';
    }
    if (endDateFilter) {
        const fp = endDateFilter._flatpickr;
        if (fp) fp.clear();
        endDateFilter.value = '';
    }

    renderPromotions(allPromotionsData);
}

// ============================================================
// OPEN EDIT PROMOTION MODAL
// ============================================================
function openEditPromotionModal(button) {
    const promoId = button.getAttribute('data-promo-id');
    const hotelId = button.getAttribute('data-hotel-id');
    const title = button.getAttribute('data-title');
    const description = button.getAttribute('data-description');
    const discount = button.getAttribute('data-discount');
    const start = button.getAttribute('data-start');
    const end = button.getAttribute('data-end');
    const status = button.getAttribute('data-status');

    document.getElementById('editPromotionId').value = promoId;
    document.getElementById('editHotelId').value = hotelId;
    document.getElementById('editTitle').value = title;
    document.getElementById('editDescription').value = description;
    document.getElementById('editDiscountPercent').value = discount;

    // Gán ngày cho Flatpickr
    const startFp = document.getElementById('editStartDate')._flatpickr;
    if (startFp) {
        startFp.setDate(start);
    } else {
        document.getElementById('editStartDate').value = start;
    }

    const endFp = document.getElementById('editEndDate')._flatpickr;
    if (endFp) {
        endFp.setDate(end);
    } else {
        document.getElementById('editEndDate').value = end;
    }

    document.getElementById('editStatus').value = status || 'ACTIVE';

    const modal = new bootstrap.Modal(document.getElementById('editPromotionModal'));
    modal.show();
}

// ============================================================
// OPEN ADD PROMOTION MODAL
// ============================================================
function openAddPromotionModal() {
    // Clear dữ liệu cũ trong modal add trước khi mở
    const addStart = document.getElementById('addStartDate');
    const addEnd = document.getElementById('addEndDate');
    if (addStart && addStart._flatpickr) addStart._flatpickr.clear();
    if (addEnd && addEnd._flatpickr) addEnd._flatpickr.clear();

    const modal = new bootstrap.Modal(document.getElementById('addPromotionModal'));
    modal.show();
}

// ============================================================
// SETUP PROMOTION FORM DATE PICKERS (ADD/EDIT) WITH FLATPICKR
// ============================================================
function setupPromotionFormDatePickers() {
    const addStart = document.getElementById('addStartDate');
    const addEnd = document.getElementById('addEndDate');
    const editStart = document.getElementById('editStartDate');
    const editEnd = document.getElementById('editEndDate');

    const config = {
        dateFormat: "Y-m-d",
        altInput: true,
        altFormat: "d/m/Y",
        allowInput: false
    };

    if (addStart && typeof flatpickr === 'function') {
        flatpickr(addStart, config);
    }
    if (addEnd && typeof flatpickr === 'function') {
        flatpickr(addEnd, config);
    }
    if (editStart && typeof flatpickr === 'function') {
        flatpickr(editStart, config);
    }
    if (editEnd && typeof flatpickr === 'function') {
        flatpickr(editEnd, config);
    }
}

// ============================================================
// SETUP PROMOTION EVENTS
// ============================================================
function setupPromotionEvents() {
    const searchInput = document.getElementById('promotionSearchInput');
    const hotelFilter = document.getElementById('hotelFilterSelect');
    const statusFilter = document.getElementById('promotionStatusFilter');

    if (searchInput) {
        searchInput.addEventListener('input', applyPromotionFilters);
    }
    if (hotelFilter) {
        hotelFilter.addEventListener('change', applyPromotionFilters);
    }
    if (statusFilter) {
        statusFilter.addEventListener('change', applyPromotionFilters);
    }
}

// ============================================================
// AUTO-LOAD ON PAGE READY
// ============================================================
document.addEventListener('DOMContentLoaded', function () {
    // Setup events
    setupPromotionEvents();

    // Setup date filters with Flatpickr
    setupPromotionDateFilters();

    // Setup add/edit form date pickers with Flatpickr
    setupPromotionFormDatePickers();

    // Check if promotions tab is active
    const activeTab = document.querySelector('.tab-panel.active');
    if (activeTab && activeTab.id === 'promotions') {
        loadPromotions();
    }

    // Listen for tab changes
    const sidebarLinks = document.querySelectorAll('.sidebar-link');
    sidebarLinks.forEach(link => {
        link.addEventListener('click', function () {
            const tab = this.getAttribute('data-tab');
            if (tab === 'promotions') {
                loadPromotions();
            }
        });
    });
});