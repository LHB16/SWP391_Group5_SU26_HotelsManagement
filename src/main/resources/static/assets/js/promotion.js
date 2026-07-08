(function() {
    'use strict';

    let currentFilter = 'all';
    let currentHotelId = 'all';
    let promotionsCache = {};
    let hotelsCache = [];

    document.addEventListener('DOMContentLoaded', function() {
        if (typeof lucide !== 'undefined') {
            lucide.createIcons();
        }
        loadPromotionsData();
        setupDatePickers();
    });

    function loadPromotionsData() {
        const tbody = document.getElementById('promotionTableBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-muted py-4">
                        <span class="spinner-border spinner-border-sm me-2" role="status"></span>
                        Loading promotions...
                    </td>
                </tr>
            `;
        }

        fetch('/owner/promotions/list')
            .then(response => {
                if (!response.ok) throw new Error('Failed to fetch');
                return response.json();
            })
            .then(data => {
                if (data.error) {
                    console.error('Error:', data.error);
                    showEmptyState();
                    return;
                }
                hotelsCache = data.hotels || [];
                promotionsCache = data.promotionsByHotel || {};
                renderAllPromotions();
            })
            .catch(error => {
                console.error('Error loading promotions:', error);
                showEmptyState();
            });
    }

    function showEmptyState() {
        const tbody = document.getElementById('promotionTableBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-muted py-4">
                        <i data-lucide="percent" class="d-block mx-auto mb-2" style="width: 32px; height: 32px; opacity: 0.3;"></i>
                        No promotions found
                    </td>
                </tr>
            `;
            if (typeof lucide !== 'undefined') lucide.createIcons();
        }
    }

    function renderAllPromotions() {
        const tbody = document.getElementById('promotionTableBody');
        if (!tbody) return;

        let allPromotions = [];
        for (const [hotelId, promotions] of Object.entries(promotionsCache)) {
            promotions.forEach(p => {
                allPromotions.push({
                    ...p,
                    hotelId: parseInt(hotelId)
                });
            });
        }

        if (currentHotelId !== 'all') {
            allPromotions = allPromotions.filter(p => p.hotelId === parseInt(currentHotelId));
        }

        if (currentFilter !== 'all') {
            allPromotions = allPromotions.filter(p => p.status === currentFilter);
        }

        renderPromotionRows(tbody, allPromotions);
    }

    function renderPromotionRows(tbody, promotions) {
        if (!promotions || promotions.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-muted py-4">
                        <span class="small">No promotions available</span>
                    </td>
                </tr>
            `;
            return;
        }

        let html = '';
        promotions.forEach(promo => {
            const statusClass = getStatusClass(promo.status);
            const statusLabel = promo.status || 'UNKNOWN';
            const hotelId = promo.hotelId || '';

            html += `
                <tr class="promo-table-row" data-promo-id="${promo.id}" data-hotel-id="${hotelId}">
                    <td style="padding: 10px 16px;">
                        <div style="font-weight: 600; color: #0a1628;">${escapeHtml(promo.title || 'Untitled')}</div>
                        ${promo.description ? `<div style="font-size: 0.78rem; color: #64748b; margin-top: 2px;">${escapeHtml(promo.description.substring(0, 60))}${promo.description.length > 60 ? '...' : ''}</div>` : ''}
                    </td>
                    <td style="padding: 10px 16px;">
                        <span class="promo-discount-badge">-${promo.discountPercent || 0}%</span>
                    </td>
                    <td style="padding: 10px 16px; font-size: 0.82rem; color: #475569;">
                        ${formatDate(promo.startDate)} → ${formatDate(promo.endDate)}
                    </td>
                    <td style="padding: 10px 16px;">
                        <span class="promo-status-badge ${statusClass}">${statusLabel}</span>
                    </td>
                    <td style="padding: 10px 16px; text-align: center;">
                        <button class="promo-action-btn promo-action-btn-edit" 
                                onclick="editPromotion(${promo.id}, ${hotelId})"
                                title="Edit Promotion">
                            <i data-lucide="pencil" style="width: 14px; height: 14px;"></i>
                        </button>
                        <form th:action="@{/owner/promotions/delete}" method="post" style="display:inline;" 
                              onsubmit="return confirm('Are you sure you want to delete this promotion?')">
                            <input type="hidden" name="promotionId" value="${promo.id}" />
                            <input type="hidden" name="hotelId" value="${hotelId}" />
                            <button type="submit" class="promo-action-btn promo-action-btn-delete" title="Delete Promotion">
                                <i data-lucide="trash-2" style="width: 14px; height: 14px;"></i>
                            </button>
                        </form>
                    </td>
                </tr>
            `;
        });

        tbody.innerHTML = html;
        if (typeof lucide !== 'undefined') lucide.createIcons();
    }

    function getStatusClass(status) {
        switch (status) {
            case 'ACTIVE': return 'promo-status-active';
            case 'EXPIRED': return 'promo-status-expired';
            case 'INACTIVE': return 'promo-status-inactive';
            default: return 'promo-status-expired';
        }
    }

    function formatDate(dateStr) {
        if (!dateStr) return '-';
        try {
            const parts = dateStr.split('-');
            return `${parts[2]}/${parts[1]}/${parts[0]}`;
        } catch (e) {
            return dateStr;
        }
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    window.filterPromotionsByHotel = function(hotelId) {
        currentHotelId = hotelId;
        renderAllPromotions();
    };

    window.filterPromotions = function(btn, filter) {
        const parent = btn.closest('.btn-group');
        if (parent) {
            parent.querySelectorAll('.promo-filter-btn').forEach(b => b.classList.remove('active'));
        }
        btn.classList.add('active');
        currentFilter = filter;
        renderAllPromotions();
    };

    window.openAddPromotionModal = function(hotelId) {
        const modal = document.getElementById('addPromotionModal');
        if (!modal) return;
        const form = modal.querySelector('form');
        if (form) form.reset();
        if (hotelId) {
            const select = form.querySelector('select[name="hotelId"]');
            if (select) select.value = hotelId;
        }
        const bsModal = new bootstrap.Modal(modal);
        bsModal.show();
    };

    window.editPromotion = function(promoId, hotelId) {
        fetch(`/owner/promotions/${promoId}`)
            .then(response => {
                if (!response.ok) throw new Error('Failed to fetch');
                return response.json();
            })
            .then(data => {
                if (data.error) {
                    console.error('Error:', data.error);
                    return;
                }
                document.getElementById('editPromotionId').value = data.id;
                document.getElementById('editHotelId').value = data.hotelId;
                document.getElementById('editTitle').value = data.title || '';
                document.getElementById('editDescription').value = data.description || '';
                document.getElementById('editDiscountPercent').value = data.discountPercent || '';
                document.getElementById('editStartDate').value = data.startDate || '';
                document.getElementById('editEndDate').value = data.endDate || '';
                document.getElementById('editStatus').value = data.status || 'ACTIVE';

                const modal = document.getElementById('editPromotionModal');
                if (modal) {
                    const bsModal = new bootstrap.Modal(modal);
                    bsModal.show();
                }
            })
            .catch(error => {
                console.error('Error fetching promotion:', error);
            });
    };

    function setupDatePickers() {
        if (typeof flatpickr !== 'undefined') {
            const addStartDate = document.querySelector('#addPromotionModal input[name="startDate"]');
            const addEndDate = document.querySelector('#addPromotionModal input[name="endDate"]');
            if (addStartDate && addEndDate) {
                flatpickr(addStartDate, {
                    dateFormat: "Y-m-d",
                    minDate: "today",
                    onChange: function(selectedDates) {
                        if (selectedDates[0]) {
                            addEndDate._flatpickr.set('minDate', selectedDates[0]);
                        }
                    }
                });
                flatpickr(addEndDate, { dateFormat: "Y-m-d", minDate: "today" });
            }

            const editStartDate = document.querySelector('#editPromotionModal input[name="startDate"]');
            const editEndDate = document.querySelector('#editPromotionModal input[name="endDate"]');
            if (editStartDate && editEndDate) {
                flatpickr(editStartDate, {
                    dateFormat: "Y-m-d",
                    onChange: function(selectedDates) {
                        if (selectedDates[0]) {
                            editEndDate._flatpickr.set('minDate', selectedDates[0]);
                        }
                    }
                });
                flatpickr(editEndDate, { dateFormat: "Y-m-d" });
            }
        }
    }

})();