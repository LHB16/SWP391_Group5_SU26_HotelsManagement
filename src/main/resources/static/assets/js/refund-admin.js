// Biến toàn cục lưu thông tin yêu cầu đang xem chi tiết để chuyển sang modal xử lý nhanh
var activeRefundId = null;
var activeCustomer = null;
var activeAmount = null;
var activeBookingId = null;
var activeBankName = null;
var activeAccNum = null;
var activeAccHolder = null;

// Mở modal Chi tiết
function openDetailModal(btn) {
    activeRefundId = btn.getAttribute('data-refund-id');
    activeCustomer = btn.getAttribute('data-customer-name');
    activeAmount   = btn.getAttribute('data-amount');
    activeBookingId = btn.getAttribute('data-booking-id');
    activeBankName = btn.getAttribute('data-bank-name');
    activeAccNum   = btn.getAttribute('data-account-number');
    activeAccHolder = btn.getAttribute('data-account-holder');

    var bookingId  = btn.getAttribute('data-booking-id');
    var email      = btn.getAttribute('data-customer-email');
    var bankName   = btn.getAttribute('data-bank-name');
    var accNum     = btn.getAttribute('data-account-number');
    var accHolder  = btn.getAttribute('data-account-holder');
    var reqDate    = btn.getAttribute('data-date');
    var status     = btn.getAttribute('data-status');
    var reason     = btn.getAttribute('data-reason');
    var procDate   = btn.getAttribute('data-processed-at');
    var note       = btn.getAttribute('data-admin-note');

    // Gán dữ liệu vào modal
    document.getElementById('detailRefundId').textContent = '#' + activeRefundId;
    document.getElementById('detailBookingId').textContent = 'Booking #' + bookingId;
    document.getElementById('detailCustomerName').textContent = activeCustomer;
    document.getElementById('detailCustomerEmail').textContent = email;
    document.getElementById('detailAmount').textContent = activeAmount;
    document.getElementById('detailRequestedAt').textContent = reqDate;
    document.getElementById('detailBankName').textContent = bankName ? bankName : '-';
    document.getElementById('detailAccountNumber').textContent = accNum ? accNum : '-';
    document.getElementById('detailAccountHolder').textContent = accHolder ? accHolder : '-';
    document.getElementById('detailCancellationReason').textContent = reason ? reason : 'No reason provided.';

    // Trạng thái badge
    var statusBadge = document.getElementById('detailStatusBadge');
    statusBadge.textContent = status;
    statusBadge.className = 'badge';
    if (status === 'PENDING') {
        statusBadge.classList.add('bg-warning', 'text-dark');
        document.getElementById('detailProcessedSection').style.display = 'none';
        document.getElementById('detailActionButtons').setAttribute('style', 'display: flex !important;');
    } else if (status === 'PROCESSED') {
        statusBadge.classList.add('bg-success', 'text-white');
        document.getElementById('detailProcessedAt').textContent = procDate;
        document.getElementById('detailAdminNote').textContent = note ? note : 'No notes.';
        document.getElementById('detailProcessedSection').style.display = 'block';
        document.getElementById('detailActionButtons').setAttribute('style', 'display: none !important;');
    } else {
        statusBadge.classList.add('bg-danger', 'text-white');
        document.getElementById('detailProcessedAt').textContent = procDate;
        document.getElementById('detailAdminNote').textContent = note ? note : 'No notes.';
        document.getElementById('detailProcessedSection').style.display = 'block';
        document.getElementById('detailActionButtons').setAttribute('style', 'display: none !important;');
    }

    new bootstrap.Modal(document.getElementById('detailModal')).show();
}

function getVietQRBankCode(bankName) {
    if (!bankName) return "icb";
    var name = bankName.toLowerCase().replace(/[^a-z0-9]/g, "");
    if (name.includes("vietcom") || name.includes("vcb")) return "vietcombank";
    if (name.includes("mbbank") || name.includes("mb") || name.includes("quandoi")) return "mb";
    if (name.includes("techcom") || name.includes("tcb")) return "techcombank";
    if (name.includes("vietin") || name.includes("ctg")) return "vietinbank";
    if (name.includes("bidv") || name.includes("dautu")) return "bidv";
    if (name.includes("acb") || name.includes("achau")) return "acb";
    if (name.includes("vpbank") || name.includes("vp")) return "vpbank";
    if (name.includes("tpbank") || name.includes("tpb") || name.includes("tienphong")) return "tpbank";
    if (name.includes("sacom") || name.includes("stb")) return "sacombank";
    if (name.includes("agri") || name.includes("nongnghiep")) return "agribank";
    if (name.includes("shb")) return "shb";
    if (name.includes("vib")) return "vib";
    if (name.includes("scb")) return "scb";
    if (name.includes("hdbank") || name.includes("hd")) return "hdbank";
    if (name.includes("ocb") || name.includes("phuongdong")) return "ocb";
    if (name.includes("exim")) return "eximbank";
    if (name.includes("msb") || name.includes("hanghai")) return "msb";
    if (name.includes("seabank") || name.includes("sea")) return "seabank";
    if (name.includes("bacabank") || name.includes("baca")) return "bacabank";
    return name;
}

function openApproveModalWithData(refundId, customer, amount, bankName, accNum, accHolder, bookingId) {
    document.getElementById('approveCustomerName').textContent = customer;
    document.getElementById('approveAmount').textContent = amount;
    document.getElementById('approveForm').action = '/admin/refunds/' + refundId + '/update';
    document.getElementById('approveNote').value = '';

    // Sinh QR Code VietQR chuyển khoản thụ hưởng của khách hàng
    if (bankName && accNum && accHolder) {
        var cleanBank = getVietQRBankCode(bankName);
        var cleanAcc = accNum.replace(/[^0-9]/g, "");
        var cleanAmount = amount.replace(/[^0-9]/g, "");
        var info = "Refund Booking " + bookingId;

        var qrUrl = "https://img.vietqr.io/image/" + cleanBank + "-" + cleanAcc + "-compact.png"
            + "?amount=" + cleanAmount
            + "&addInfo=" + encodeURIComponent(info)
            + "&accountName=" + encodeURIComponent(accHolder);

        document.getElementById('approveQrImg').src = qrUrl;
        document.getElementById('approveQrBankName').textContent = bankName;
        document.getElementById('approveQrAccNum').textContent = accNum;
        document.getElementById('approveQrAccHolder').textContent = accHolder;
        document.getElementById('approveQrSection').style.display = 'block';
    } else {
        document.getElementById('approveQrSection').style.display = 'none';
    }

    new bootstrap.Modal(document.getElementById('approveModal')).show();
}

function triggerApproveFromDetail() {
    // Đóng detail modal
    var detailModalEl = document.getElementById('detailModal');
    bootstrap.Modal.getInstance(detailModalEl).hide();

    setTimeout(function() {
        openApproveModalWithData(activeRefundId, activeCustomer, activeAmount, activeBankName, activeAccNum, activeAccHolder, activeBookingId);
    }, 350);
}

function triggerRejectFromDetail() {
    // Đóng detail modal
    var detailModalEl = document.getElementById('detailModal');
    bootstrap.Modal.getInstance(detailModalEl).hide();

    // Mở modal Reject với dữ liệu đã lưu
    document.getElementById('rejectForm').action = '/admin/refunds/' + activeRefundId + '/update';
    document.getElementById('rejectNote').value = '';

    setTimeout(function() {
        new bootstrap.Modal(document.getElementById('rejectModal')).show();
    }, 350);
}

// Mở modal Approve trực tiếp từ bảng chính
function openApproveModal(btn) {
    var refundId = btn.getAttribute('data-refund-id');
    var customer = btn.getAttribute('data-customer');
    var amount   = btn.getAttribute('data-amount');
    var bankName = btn.getAttribute('data-bank-name');
    var accNum   = btn.getAttribute('data-account-number');
    var accHolder = btn.getAttribute('data-account-holder');
    var bookingId = btn.getAttribute('data-booking-id');

    openApproveModalWithData(refundId, customer, amount, bankName, accNum, accHolder, bookingId);
}

function submitApprove() {
    document.getElementById('approveNoteHidden').value = document.getElementById('approveNote').value;
    document.getElementById('approveForm').submit();
}

// Mở modal Reject trực tiếp từ bảng chính
function openRejectModal(btn) {
    var refundId = btn.getAttribute('data-refund-id');
    document.getElementById('rejectForm').action = '/admin/refunds/' + refundId + '/update';
    document.getElementById('rejectNote').value = '';

    new bootstrap.Modal(document.getElementById('rejectModal')).show();
}

function submitReject() {
    var note = document.getElementById('rejectNote').value.trim();
    if (!note) {
        alert('Please provide a reason for rejection.');
        return;
    }
    document.getElementById('rejectNoteHidden').value = note;
    document.getElementById('rejectForm').submit();
}

document.addEventListener('DOMContentLoaded', function () {
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
});
