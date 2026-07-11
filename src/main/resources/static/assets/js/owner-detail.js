// Initialize Lucide icons
if (typeof lucide !== 'undefined') { lucide.createIcons(); }

// Handle AJAX hotel active/inactive toggle
document.addEventListener('DOMContentLoaded', function() {
    // Find all forms that post to set-active
    const forms = document.querySelectorAll('form[action*="/admin/owner-hotel/set-active"]');
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            e.preventDefault(); // Stop full page reload
            
            const formData = new FormData(this);
            const hotelId = formData.get('hotelId');
            const active = formData.get('active') === 'true';
            const url = this.action;

            // Send asynchronous POST request
            fetch(url, {
                method: 'POST',
                body: formData
            })
            .then(response => {
                if (response.ok) {
                    // 1. Update active status badge
                    const badge = document.getElementById('hotel-active-badge-' + hotelId);
                    if (badge) {
                        if (active) {
                            badge.className = 'badge-status badge-status-success';
                            badge.textContent = 'Active';
                        } else {
                            badge.className = 'badge-status badge-status-neutral';
                            badge.textContent = 'Inactive';
                        }
                    }

                    // 2. Toggle buttons visibility
                    const btnContainer = document.getElementById('hotel-action-container-' + hotelId);
                    if (btnContainer) {
                        const activeBtn = btnContainer.querySelector('.hotel-active-btn');
                        const inactiveBtn = btnContainer.querySelector('.hotel-inactive-btn');
                        if (active) {
                            if (activeBtn) activeBtn.style.setProperty('display', 'none', 'important');
                            if (inactiveBtn) inactiveBtn.style.setProperty('display', 'flex', 'important');
                        } else {
                            if (activeBtn) activeBtn.style.setProperty('display', 'flex', 'important');
                            if (inactiveBtn) inactiveBtn.style.setProperty('display', 'none', 'important');
                        }
                    }

                    // 3. Hide the open bootstrap modal
                    const modalId = active ? 'activeHotelModal-' + hotelId : 'inactiveHotelModal-' + hotelId;
                    const modalEl = document.getElementById(modalId);
                    if (modalEl) {
                        const modalInstance = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
                        if (modalInstance) {
                            modalInstance.hide();
                        }
                    }
                    
                    // 4. Force clean any lingering backdrops
                    document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
                    document.body.style.overflow = '';
                    document.body.style.paddingRight = '';
                } else {
                    alert('Failed to update hotel status.');
                }
            })
            .catch(error => {
                console.error('AJAX error:', error);
                alert('An error occurred. Please try again.');
            });
        });
    });
});
