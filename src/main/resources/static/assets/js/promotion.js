// ============================================================
// GLOBAL VARIABLES
// ============================================================
let allPromotionsData = [];
let allHotelsData = [];
let isLoading = false;

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

function showPromotionError(message) {
    const tbody = document.getElementById('promotionTableBody');
    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-danger py-4">
                    ${message}
                </td>
            </tr>
        `;
    }
}

function renderPromotions(promotions) {
    const tbody = document.getElementById('promotionTableBody');
    if (!tbody) return;

    if (!promotions || promotions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-muted py-4">
                    No promotions found.
                </td>
            </tr>
        `;
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
                            Edit
                        </button>
                        <button type="button" class="promo-action-btn promo-action-btn-delete" 
                                data-bs-toggle="modal" 
                                data-bs-target="#deleteModalPromo-${promo.id}">
                            Delete
                        </button>
                    </div>
                </td>
            </tr>
        `;
    });

    tbody.innerHTML = html;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function setupPromotionDateFilters() {
    const startDateFilter = document.getElementById('promoStartDateFilter');
    const endDateFilter = document.getElementById('promoEndDateFilter');

    let startPicker, endPicker;

    let initMinEnd = null;
    if (startDateFilter && startDateFilter.value) {
        const startD = new Date(startDateFilter.value);
        if (!isNaN(startD.getTime())) {
            initMinEnd = new Date(startD);
            initMinEnd.setDate(initMinEnd.getDate() + 1);
        }
    }

    if (startDateFilter && endDateFilter && typeof flatpickr === 'function') {
        startPicker = flatpickr(startDateFilter, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            allowInput: false,
            defaultDate: startDateFilter.value ? new Date(startDateFilter.value) : null,
            onChange: function (selectedDates) {
                if (selectedDates[0]) {
                    const next = new Date(selectedDates[0]);
                    next.setDate(next.getDate() + 1);
                    endPicker.set("minDate", next);
                } else {
                    endPicker.set("minDate", null);
                }
                applyPromotionFilters();
            }
        });

        endPicker = flatpickr(endDateFilter, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            allowInput: false,
            minDate: initMinEnd,
            defaultDate: endDateFilter.value ? new Date(endDateFilter.value) : null,
            onChange: function (selectedDates) {
                if (selectedDates[0] && startPicker.selectedDates[0]) {
                    if (selectedDates[0] <= startPicker.selectedDates[0]) {
                        endPicker.clear();
                    }
                }
                applyPromotionFilters();
            }
        });
    } else {
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
}

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

function openEditPromotionModal(button) {
    const promoId = button.getAttribute('data-promo-id');
    const hotelId = button.getAttribute('data-hotel-id');
    const title = button.getAttribute('data-title');
    const description = button.getAttribute('data-description');
    const discount = button.getAttribute('data-discount');
    const start = button.getAttribute('data-start');
    const end = button.getAttribute('data-end');
    const status = button.getAttribute('data-status');

    const errorEl = document.getElementById('editDiscountError');
    if (errorEl) {
        errorEl.style.display = 'none';
    }
    const input = document.getElementById('editDiscountPercent');
    if (input) {
        input.classList.remove('is-invalid', 'is-valid');
    }

    document.getElementById('editPromotionId').value = promoId;
    document.getElementById('editHotelId').value = hotelId;
    document.getElementById('editTitle').value = title;
    document.getElementById('editDescription').value = description;
    document.getElementById('editDiscountPercent').value = discount;

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

function openAddPromotionModal() {
    const errorEl = document.getElementById('addDiscountError');
    if (errorEl) {
        errorEl.style.display = 'none';
    }
    const input = document.getElementById('addDiscountPercent');
    if (input) {
        input.classList.remove('is-invalid', 'is-valid');
        input.value = '';
    }

    const addStart = document.getElementById('addStartDate');
    const addEnd = document.getElementById('addEndDate');
    if (addStart && addStart._flatpickr) addStart._flatpickr.clear();
    if (addEnd && addEnd._flatpickr) addEnd._flatpickr.clear();

    const modal = new bootstrap.Modal(document.getElementById('addPromotionModal'));
    modal.show();
}

function setupPromotionFormDatePickers() {
    const addStart = document.getElementById('addStartDate');
    const addEnd = document.getElementById('addEndDate');
    const editStart = document.getElementById('editStartDate');
    const editEnd = document.getElementById('editEndDate');

    if (addStart && addEnd && typeof flatpickr === 'function') {
        let addStartPicker, addEndPicker;
        
        addStartPicker = flatpickr(addStart, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            allowInput: false,
            minDate: "today",
            onChange: function(selectedDates) {
                if (selectedDates[0]) {
                    const next = new Date(selectedDates[0]);
                    next.setDate(next.getDate() + 1);
                    addEndPicker.set("minDate", next);
                    const curEnd = addEndPicker.selectedDates[0];
                    if (!curEnd || curEnd <= selectedDates[0]) {
                        addEndPicker.setDate(next);
                    }
                } else {
                    addEndPicker.set("minDate", "today");
                }
            }
        });

        addEndPicker = flatpickr(addEnd, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            allowInput: false,
            minDate: "today",
            onChange: function(selectedDates) {
                if (selectedDates[0] && addStartPicker.selectedDates[0]) {
                    if (selectedDates[0] <= addStartPicker.selectedDates[0]) {
                        addEndPicker.clear();
                    }
                }
            }
        });
    }

    if (editStart && editEnd && typeof flatpickr === 'function') {
        let editStartPicker, editEndPicker;

        editStartPicker = flatpickr(editStart, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            allowInput: false,
            onChange: function(selectedDates) {
                if (selectedDates[0]) {
                    const next = new Date(selectedDates[0]);
                    next.setDate(next.getDate() + 1);
                    editEndPicker.set("minDate", next);
                    const curEnd = editEndPicker.selectedDates[0];
                    if (!curEnd || curEnd <= selectedDates[0]) {
                        editEndPicker.setDate(next);
                    }
                } else {
                    editEndPicker.set("minDate", null);
                }
            }
        });

        editEndPicker = flatpickr(editEnd, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            allowInput: false,
            onChange: function(selectedDates) {
                if (selectedDates[0] && editStartPicker.selectedDates[0]) {
                    if (selectedDates[0] <= editStartPicker.selectedDates[0]) {
                        editEndPicker.clear();
                    }
                }
            }
        });
    }
}

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

function validateDiscountInteger(input) {
    if (!input) return;

    const errorId = input.id === 'addDiscountPercent' ? 'addDiscountError' :
        input.id === 'editDiscountPercent' ? 'editDiscountError' : null;
    const errorEl = document.getElementById(errorId);
    const val = input.value;

    input.classList.remove('is-valid', 'is-invalid');

    if (val && val.length > 0) {
        const numVal = parseFloat(val);

        if (val.includes('.')) {
            if (errorEl) {
                errorEl.style.display = 'block';
                errorEl.textContent = 'Decimals are not allowed. Please enter a whole number.';
            }
            input.classList.add('is-invalid');
            const intVal = Math.floor(numVal);
            if (intVal >= 1 && intVal <= 100) {
                input.value = intVal;
                setTimeout(function() {
                    validateDiscountInteger(input);
                }, 100);
            }
            return;
        }

        if (numVal < 1) {
            if (errorEl) {
                errorEl.style.display = 'block';
                errorEl.textContent = 'Discount must be at least 1%.';
            }
            input.classList.add('is-invalid');
            return;
        }

        if (numVal > 100) {
            if (errorEl) {
                errorEl.style.display = 'block';
                errorEl.textContent = 'Discount cannot exceed 100%.';
            }
            input.classList.add('is-invalid');
            return;
        }

        if (errorEl) {
            errorEl.style.display = 'none';
        }
        input.classList.add('is-valid');
    } else {
        if (errorEl) {
            errorEl.style.display = 'none';
        }
    }
}

document.addEventListener('DOMContentLoaded', function () {
    setupPromotionEvents();
    setupPromotionDateFilters();
    setupPromotionFormDatePickers();

    const activeTab = document.querySelector('.tab-panel.active');
    if (activeTab && activeTab.id === 'promotions') {
        loadPromotions();
    }

    const sidebarLinks = document.querySelectorAll('.sidebar-link');
    sidebarLinks.forEach(link => {
        link.addEventListener('click', function () {
            const tab = this.getAttribute('data-tab');
            if (tab === 'promotions') {
                loadPromotions();
            }
        });
    });

    const addForm = document.getElementById('addPromotionForm');
    if (addForm) {
        addForm.addEventListener('submit', function(e) {
            const discountInput = document.getElementById('addDiscountPercent');
            if (!discountInput) return;

            const val = discountInput.value.trim();
            const errorEl = document.getElementById('addDiscountError');

            if (!val) {
                e.preventDefault();
                if (errorEl) {
                    errorEl.style.display = 'block';
                    errorEl.textContent = 'Discount percent is required.';
                }
                discountInput.classList.add('is-invalid');
                discountInput.focus();
                return false;
            }

            if (val.includes('.')) {
                e.preventDefault();
                if (errorEl) {
                    errorEl.style.display = 'block';
                    errorEl.textContent = 'Decimals are not allowed. Please enter a whole number (e.g., 10, 15, 20).';
                }
                discountInput.classList.add('is-invalid');
                discountInput.focus();
                return false;
            }

            const numVal = parseFloat(val);

            if (numVal < 1 || numVal > 100) {
                e.preventDefault();
                if (errorEl) {
                    errorEl.style.display = 'block';
                    errorEl.textContent = 'Discount must be between 1% and 100%.';
                }
                discountInput.classList.add('is-invalid');
                discountInput.focus();
                return false;
            }

            if (errorEl) {
                errorEl.style.display = 'none';
            }
            discountInput.classList.remove('is-invalid');
            discountInput.classList.add('is-valid');
            return true;
        });
    }

    const editForm = document.getElementById('editPromotionForm');
    if (editForm) {
        editForm.addEventListener('submit', function(e) {
            const discountInput = document.getElementById('editDiscountPercent');
            if (!discountInput) return;

            const val = discountInput.value.trim();
            const errorEl = document.getElementById('editDiscountError');

            if (!val) {
                e.preventDefault();
                if (errorEl) {
                    errorEl.style.display = 'block';
                    errorEl.textContent = 'Discount percent is required.';
                }
                discountInput.classList.add('is-invalid');
                discountInput.focus();
                return false;
            }

            if (val.includes('.')) {
                e.preventDefault();
                if (errorEl) {
                    errorEl.style.display = 'block';
                    errorEl.textContent = 'Decimals are not allowed. Please enter a whole number (e.g., 10, 15, 20).';
                }
                discountInput.classList.add('is-invalid');
                discountInput.focus();
                return false;
            }

            const numVal = parseFloat(val);

            if (numVal < 1 || numVal > 100) {
                e.preventDefault();
                if (errorEl) {
                    errorEl.style.display = 'block';
                    errorEl.textContent = 'Discount must be between 1% and 100%.';
                }
                discountInput.classList.add('is-invalid');
                discountInput.focus();
                return false;
            }

            if (errorEl) {
                errorEl.style.display = 'none';
            }
            discountInput.classList.remove('is-invalid');
            discountInput.classList.add('is-valid');
            return true;
        });
    }

    const addInput = document.getElementById('addDiscountPercent');
    if (addInput) {
        addInput.addEventListener('input', function() {
            validateDiscountInteger(this);
        });
        addInput.addEventListener('blur', function() {
            validateDiscountInteger(this);
        });
    }

    const editInput = document.getElementById('editDiscountPercent');
    if (editInput) {
        editInput.addEventListener('input', function() {
            validateDiscountInteger(this);
        });
        editInput.addEventListener('blur', function() {
            validateDiscountInteger(this);
        });
    }

    const addModal = document.getElementById('addPromotionModal');
    if (addModal) {
        addModal.addEventListener('shown.bs.modal', function() {
            if (typeof lucide !== 'undefined') {
                lucide.createIcons();
            }
        });
    }

    const editModal = document.getElementById('editPromotionModal');
    if (editModal) {
        editModal.addEventListener('shown.bs.modal', function() {
            if (typeof lucide !== 'undefined') {
                lucide.createIcons();
            }
        });
    }
});