/**
 * room-list.js
 * JS riêng cho trang danh sách phòng khách sạn (hotel/rooms.html)
 * Palette: Deep Navy (#0a1628) + Champagne Gold (#c9a96e)
 */

document.addEventListener('DOMContentLoaded', function () {

    // ==================== DUAL RANGE SLIDER ====================
    const rlMin       = document.getElementById('rlMin');
    const rlMax       = document.getElementById('rlMax');
    const rlHighlight = document.getElementById('rlHighlight');
    const minDisplay  = document.getElementById('minPriceDisplay');
    const maxDisplay  = document.getElementById('maxPriceDisplay');
    const minInput    = document.getElementById('minPriceInput');
    const maxInput    = document.getElementById('maxPriceInput');

    function formatVND(n) {
        return Number(n).toLocaleString('vi-VN') + ' VND';
    }

    function updateSlider() {
        const max    = 50000000;
        const minVal = parseFloat(rlMin.value);
        const maxVal = parseFloat(rlMax.value);
        if (minVal > maxVal) rlMin.value = maxVal;
        if (maxVal < minVal) rlMax.value = minVal;
        const minPct = (parseFloat(rlMin.value) / max) * 100;
        const maxPct = (parseFloat(rlMax.value) / max) * 100;
        rlHighlight.style.left  = minPct + '%';
        rlHighlight.style.width = (maxPct - minPct) + '%';
        minDisplay.textContent  = formatVND(rlMin.value);
        maxDisplay.textContent  = formatVND(rlMax.value);
        minInput.value = rlMin.value;
        maxInput.value = rlMax.value;
    }

    if (rlMin && rlMax) {
        rlMin.addEventListener('input', updateSlider);
        rlMax.addEventListener('input', updateSlider);
        updateSlider();
    }

    // ==================== STAR RATING – Create form ====================
    const starBtns    = document.querySelectorAll('.star-rating-input .star-btn');
    const ratingInput = document.getElementById('reviewRatingInput');

    if (starBtns && ratingInput) {
        starBtns.forEach(btn => {
            btn.addEventListener('click', function () {
                const val = parseInt(this.getAttribute('data-value'));
                ratingInput.value = val;
                updateStarRatingDisplay(val, starBtns, ratingInput);
            });
        });
    }

    // ==================== STAR RATING – Edit modal ====================
    const editStarBtns    = document.querySelectorAll('#editStarContainer .star-btn');
    const editRatingInput = document.getElementById('editRatingInput');

    if (editStarBtns && editRatingInput) {
        editStarBtns.forEach(btn => {
            btn.addEventListener('click', function () {
                const val = parseInt(this.getAttribute('data-value'));
                editRatingInput.value = val;
                updateStarRatingDisplay(val, editStarBtns, editRatingInput);
            });
        });
    }

    function updateStarRatingDisplay(rating, btns, input) {
        btns.forEach(btn => {
            const btnVal = parseInt(btn.getAttribute('data-value'));
            btn.classList.toggle('active', btnVal <= rating);
        });
        if (input) input.value = rating;
    }

    // Expose globally for inline onclick
    window._updateEditStars = function (rating) {
        updateStarRatingDisplay(rating, editStarBtns, editRatingInput);
    };

    // ==================== EDIT REVIEW MODAL ====================
    const editReviewModal   = document.getElementById('editReviewModal');
    const editReviewForm    = document.getElementById('editReviewForm');
    const editCommentInput  = document.getElementById('editCommentInput');

    // Lấy hotelId từ data attribute trên body thay vì Thymeleaf inline
    const hotelId = document.body.getAttribute('data-hotel-id') || 0;

    // Wire up tất cả nút Edit
    document.querySelectorAll('.btn-review-edit').forEach(btn => {
        btn.addEventListener('click', function () {
            const reviewId = this.getAttribute('data-review-id');
            const rating   = parseInt(this.getAttribute('data-review-rating')) || 5;
            const comment  = this.getAttribute('data-review-comment') || '';

            editReviewForm.action = '/hotels/' + hotelId + '/reviews/' + reviewId + '/edit';
            editCommentInput.value = comment;
            updateStarRatingDisplay(rating, editStarBtns, editRatingInput);

            editReviewModal.style.display = 'flex';
            document.body.style.overflow  = 'hidden';
        });
    });

    // closeEditModal – global
    window.closeEditModal = function () {
        editReviewModal.style.display = 'none';
        document.body.style.overflow  = '';
    };

    // Đóng khi click nền
    if (editReviewModal) {
        editReviewModal.addEventListener('click', function (e) {
            if (e.target === editReviewModal) window.closeEditModal();
        });
    }

    // ==================== ROOM DETAIL MODAL ====================
    const roomDetailModal = document.getElementById('roomDetailModal');

    window.openRoomDetailModal = function (btn) {
        const roomType  = btn.getAttribute('data-room-type')    || 'Room';
        const desc      = btn.getAttribute('data-room-desc')    || 'A luxurious room equipped with modern amenities.';
        const price     = btn.getAttribute('data-room-price')   || '0';
        const acreage   = btn.getAttribute('data-room-acreage') || '?';
        const person    = btn.getAttribute('data-room-person')  || '?';
        const bed       = btn.getAttribute('data-room-bed')     || '?';
        const window_   = btn.getAttribute('data-room-window')  || '?';
        const imgUrl    = btn.getAttribute('data-room-img')     || '';
        const hotelName = btn.getAttribute('data-hotel-name')   || '';

        // Điền nội dung
        document.getElementById('rdmTitle').textContent     = roomType;
        document.getElementById('rdmHotelName').textContent = hotelName;
        document.getElementById('rdmPrice').textContent     = price;
        document.getElementById('rdmAcreage').textContent   = acreage + ' m²';
        document.getElementById('rdmBed').textContent       = bed + ' bed(s)';
        document.getElementById('rdmPerson').textContent    = person + ' guest(s)';
        document.getElementById('rdmWindow').textContent    = window_ + ' window(s)';
        document.getElementById('rdmDesc').textContent      = desc;
        document.getElementById('rdmTypeBadge').textContent = roomType;

        // Ảnh
        const rdmImg         = document.getElementById('rdmImg');
        const rdmPlaceholder = document.getElementById('rdmImgPlaceholder');
        if (imgUrl && imgUrl.trim() !== '') {
            rdmImg.src                   = imgUrl;
            rdmImg.style.display         = 'block';
            rdmPlaceholder.style.display = 'none';
        } else {
            rdmImg.style.display         = 'none';
            rdmPlaceholder.style.display = 'flex';
        }

        // Hiển thị modal
        roomDetailModal.style.display = 'flex';
        document.body.style.overflow  = 'hidden';
    };

    window.closeRoomDetailModal = function () {
        roomDetailModal.style.display = 'none';
        document.body.style.overflow  = '';
    };

    // Đóng khi click nền
    if (roomDetailModal) {
        roomDetailModal.addEventListener('click', function (e) {
            if (e.target === roomDetailModal) closeRoomDetailModal();
        });
    }

    // ==================== ESC KEY – đóng cả 2 modal ====================
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            closeRoomDetailModal();
            window.closeEditModal();
        }
    });
});
