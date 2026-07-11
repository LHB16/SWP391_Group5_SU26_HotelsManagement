/**
 * payout-admin.js
 * Xử lý tương tác AJAX cho Tab Payout to Owner trong Admin Dashboard.
 * Chức năng:
 *   - openPayoutModal(bookingId): Gọi API GET để lấy thông tin ngân hàng Owner, hiển thị modal xác nhận
 *   - recalculatePayout(): Tính lại số tiền khi Admin điều chỉnh % phí sàn
 *   - submitPayout(): Gọi API POST để xác nhận đã chuyển khoản, snapshot ngân hàng
 *   - openPayoutDetailModal(...): Hiển thị modal xem chi tiết payout đã xử lý
 */

'use strict';

// Formatter tiền Việt Nam
const VND_FORMATTER = new Intl.NumberFormat('vi-VN', {
    style: 'decimal',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
});

function formatVND(amount) {
    if (amount == null || isNaN(amount)) return '0 VND';
    return VND_FORMATTER.format(amount) + ' VND';
}

/**
 * Mở modal xác nhận Payout cho booking chưa được chuyển khoản.
 * @param {number} bookingId - ID của booking
 */
function openPayoutModal(bookingId) {
    const modal = new bootstrap.Modal(document.getElementById('payoutConfirmModal'));

    // Reset modal về trạng thái loading
    document.getElementById('payoutModalLoading').style.display = 'block';
    document.getElementById('payoutModalContent').style.display = 'none';
    document.getElementById('payoutConfirmBtn').disabled = true;
    document.getElementById('pm-bookingIdHidden').value = bookingId;

    modal.show();

    // Lấy thông tin ngân hàng Owner từ server
    const feePercent = parseFloat(document.getElementById('pm-feePercent')?.value || '10');
    fetch(`/admin/payout/get-bank-info?bookingId=${bookingId}&feePercent=${feePercent}`)
        .then(res => {
            if (!res.ok) {
                return res.text().then(msg => { throw new Error(msg); });
            }
            return res.json();
        })
        .then(data => {
            // Điền thông tin vào modal
            document.getElementById('pm-bookingId').textContent = '#' + data.bookingId;
            document.getElementById('pm-hotelName').textContent = data.hotelName || 'N/A';
            document.getElementById('pm-ownerName').textContent = data.ownerName || 'N/A';
            document.getElementById('pm-bankName').value = data.bankName || 'Chưa cập nhật';
            document.getElementById('pm-bankAccountNumber').value = data.bankAccountNumber || 'Chưa cập nhật';
            document.getElementById('pm-bankAccountHolder').value = data.bankAccountHolder || 'Chưa cập nhật';
            document.getElementById('pm-feePercent').value = data.platformFeePercent || 10;
            document.getElementById('pm-totalPriceRaw').value = data.totalPrice || 0;

            // Hiển thị số tiền
            document.getElementById('pm-totalPrice').textContent = formatVND(data.totalPrice);
            document.getElementById('pm-feeAmount').textContent = formatVND(data.platformFeeAmount);
            document.getElementById('pm-payoutAmount').textContent = formatVND(data.ownerPayoutAmount);

            // Hiện nội dung và bật nút Confirm
            document.getElementById('payoutModalLoading').style.display = 'none';
            document.getElementById('payoutModalContent').style.display = 'block';

            // Chỉ cho phép xác nhận nếu Owner đã có STK
            const hasBankInfo = data.bankAccountNumber && data.bankAccountNumber.trim().length > 0;
            const confirmBtn = document.getElementById('payoutConfirmBtn');
            confirmBtn.disabled = !hasBankInfo;
            if (!hasBankInfo) {
                confirmBtn.title = 'Owner has not set up bank account information yet';
            }
        })
        .catch(err => {
            document.getElementById('payoutModalLoading').innerHTML =
                `<div class="alert alert-danger m-0" role="alert">
                    <i data-lucide="alert-circle" style="width:16px; height:16px;"></i>
                    Error: ${err.message}
                </div>`;
            lucide.createIcons();
        });
}

/**
 * Tính lại số tiền payout khi Admin thay đổi % phí sàn.
 */
function recalculatePayout() {
    const totalPrice = parseFloat(document.getElementById('pm-totalPriceRaw').value) || 0;
    const feePercent = parseFloat(document.getElementById('pm-feePercent').value) || 10;

    const platformFee = totalPrice * (feePercent / 100);
    const ownerPayout = totalPrice - platformFee;

    document.getElementById('pm-feeAmount').textContent = formatVND(Math.round(platformFee));
    document.getElementById('pm-payoutAmount').textContent = formatVND(Math.round(ownerPayout));
}

/**
 * Gửi yêu cầu xác nhận chuyển tiền cho Owner.
 */
function submitPayout() {
    const bookingId = document.getElementById('pm-bookingIdHidden').value;
    const feePercent = parseFloat(document.getElementById('pm-feePercent').value) || 10;

    if (!bookingId) {
        alert('Error: Booking ID is missing.');
        return;
    }

    const confirmBtn = document.getElementById('payoutConfirmBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Processing...';

    const formData = new FormData();
    formData.append('bookingId', bookingId);
    formData.append('feePercent', feePercent);

    fetch('/admin/payout/process', {
        method: 'POST',
        body: formData
    })
        .then(res => {
            if (!res.ok) {
                return res.text().then(msg => { throw new Error(msg); });
            }
            return res.json();
        })
        .then(data => {
            // Đóng modal và reload trang để cập nhật danh sách
            const modal = bootstrap.Modal.getInstance(document.getElementById('payoutConfirmModal'));
            if (modal) modal.hide();

            // Hiện thông báo thành công
            showPayoutSuccessToast(data);

            // Reload sau 1.5s
            setTimeout(() => { window.location.reload(); }, 1500);
        })
        .catch(err => {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i data-lucide="send" style="stroke-width:1.5; width:15px; height:15px;"></i> Confirm Transfer';
            lucide.createIcons();
            alert('Error processing payout: ' + err.message);
        });
}

/**
 * Hiển thị modal xem chi tiết payout đã xử lý.
 */
function openPayoutDetailModal(bookingId, bankName, accountNumber, accountHolder, payoutAmount, feePercent) {
    document.getElementById('pd-bankName').textContent = bankName || 'N/A';
    document.getElementById('pd-accountNumber').textContent = accountNumber || 'N/A';
    document.getElementById('pd-accountHolder').textContent = accountHolder || 'N/A';
    document.getElementById('pd-feePercent').textContent = (feePercent || 10) + '%';
    document.getElementById('pd-payoutAmount').textContent = formatVND(payoutAmount);

    const modal = new bootstrap.Modal(document.getElementById('payoutDetailModal'));
    modal.show();
}

/**
 * Hiện toast thông báo thành công sau khi xử lý payout.
 */
function showPayoutSuccessToast(data) {
    // Tạo toast container nếu chưa có
    let toastContainer = document.getElementById('payoutToastContainer');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'payoutToastContainer';
        toastContainer.style.cssText = 'position:fixed; top:20px; right:20px; z-index:9999;';
        document.body.appendChild(toastContainer);
    }

    const toastEl = document.createElement('div');
    toastEl.className = 'toast align-items-center text-white bg-success border-0 show';
    toastEl.setAttribute('role', 'alert');
    toastEl.style.cssText = 'min-width:300px; border-radius:10px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);';
    toastEl.innerHTML = `
        <div class="d-flex align-items-center p-3">
            <div class="flex-grow-1">
                <div class="fw-bold mb-1">✅ Payout Successful!</div>
                <div class="small">Transferred ${formatVND(data.ownerPayoutAmount)} to ${data.payoutBankAccountHolder || 'Owner'}</div>
                <div class="small opacity-75">${data.payoutBankName || ''} - ${data.payoutBankAccountNumber || ''}</div>
            </div>
            <button type="button" class="btn-close btn-close-white ms-3" onclick="this.closest('.toast').remove()"></button>
        </div>
    `;
    toastContainer.appendChild(toastEl);

    // Tự xóa sau 4s
    setTimeout(() => toastEl.remove(), 4000);
}
