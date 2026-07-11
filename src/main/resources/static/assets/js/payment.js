(function () {
    // Lấy thông tin booking từ HTML data-attributes
    const paymentDataEl = document.getElementById('payment-data');
    if (!paymentDataEl) return;

    let secs = parseInt(paymentDataEl.getAttribute('data-remaining-seconds')) || 0;
    const bookingId = parseInt(paymentDataEl.getAttribute('data-booking-id')) || 0;
    
    // ── 1. ĐỒNG HỒ ĐẾM NGƯỢC ──
    const el = document.getElementById('qr-countdown');
    if (el && secs > 0) {
        function fmt(s) {
            const m = Math.floor(s / 60);
            const ss = s % 60;
            return (m < 10 ? '0' : '') + m + ':' + (ss < 10 ? '0' : '') + ss;
        }
        
        function showExpiredOverlay() {
            var modalEl = document.getElementById('paymentExpiredModal');
            if (modalEl && typeof bootstrap !== 'undefined') {
                var modal = new bootstrap.Modal(modalEl);
                modal.show();
            }
        }

        el.textContent = fmt(secs);
        let timerInterval = null;
        
        function startTimer() {
            if (timerInterval) return;
            timerInterval = setInterval(function () {
                secs--;
                if (secs <= 0) { 
                    el.textContent = '00:00'; 
                    clearInterval(timerInterval); 
                    timerInterval = null;
                    showExpiredOverlay();   // ← Show expired modal immediately
                    return; 
                }
                el.textContent = fmt(secs);
            }, 1000);
        }
        
        function pauseTimer() {
            if (timerInterval) {
                clearInterval(timerInterval);
                timerInterval = null;
            }
        }
        
        const tabVietQR = document.getElementById('tab-vietqr');
        const tabPaypal = document.getElementById('tab-paypal');
        
        if ((tabVietQR && tabVietQR.checked) || (tabPaypal && tabPaypal.checked)) {
            startTimer();
        }
        
        if (tabVietQR && tabPaypal) {
            tabVietQR.addEventListener('change', function() {
                if (this.checked) startTimer();
            });
            tabPaypal.addEventListener('change', function() {
                if (this.checked) startTimer();
            });
        }
    }

    // ── 2. TỰ ĐỘNG XÁC NHẬN THANH TOÁN (AJAX) ──
    if (bookingId > 0) {
        const checkTimer = setInterval(function () {
            fetch('/booking/check-status?bookingId=' + bookingId)
                .then(function (response) {
                    return response.text();
                })
                .then(function (status) {
                    if (status === 'CONFIRMED') {
                        clearInterval(checkTimer);
                        window.location.href = '/booking/qr-payment-status?bookingId=' + bookingId;
                    }
                })
                .catch(function (err) {
                    console.error('Error checking payment status:', err);
                });
        }, 3000);
    }
})();
