/**
 * hotel-create.js
 * JS riêng cho trang thêm mới khách sạn (hotel/hotel-create.html)
 */

document.addEventListener('DOMContentLoaded', function () {

    // ── Star Rating Picker ──────────────────────────────────────────
    const starBtns    = document.querySelectorAll('.star-btn');
    const ratingInput = document.getElementById('ratingInput');

    starBtns.forEach(function (btn) {
        btn.addEventListener('click', function () {
            starBtns.forEach(b => b.classList.remove('selected'));
            this.classList.add('selected');
            ratingInput.value = this.dataset.val;
        });
    });

    // ── Active Toggle ───────────────────────────────────────────────
    const activeToggle = document.getElementById('activeToggle');
    const activeInput  = document.getElementById('activeInput');
    const activeStatus = document.getElementById('activeStatus');

    if (activeToggle) {
        activeToggle.addEventListener('click', function () {
            const isOn = this.classList.toggle('on');
            activeInput.value        = isOn ? 'true' : 'false';
            activeStatus.textContent = isOn ? 'Active' : 'Inactive';
        });
    }

    // ── Image Upload Preview ────────────────────────────────────────
    const imageFile   = document.getElementById('imageFile');
    const previewWrap = document.getElementById('imagePreviewWrap');
    const previewImg  = document.getElementById('imagePreview');
    const removeBtn   = document.getElementById('removeImage');
    const uploadZone  = document.getElementById('uploadZone');

    if (imageFile) {
        imageFile.addEventListener('change', function () {
            const file = this.files[0];
            if (!file) return;

            // Kiểm tra dung lượng tối đa 10MB
            if (file.size > 10 * 1024 * 1024) {
                alert('File size exceeds 10MB limit. Please choose a smaller file.');
                this.value = '';
                return;
            }

            // Hiển thị preview
            const reader = new FileReader();
            reader.onload = function (e) {
                previewImg.src                  = e.target.result;
                previewWrap.style.display       = 'block';
                uploadZone.style.borderStyle    = 'solid';
                uploadZone.style.borderColor    = '#c9a96e';
            };
            reader.readAsDataURL(file);
        });
    }

    // Xoá ảnh đã chọn
    if (removeBtn) {
        removeBtn.addEventListener('click', function () {
            imageFile.value                 = '';
            previewImg.src                  = '#';
            previewWrap.style.display       = 'none';
            uploadZone.style.borderStyle    = 'dashed';
            uploadZone.style.borderColor    = '#d1d5db';
        });
    }

    // Drag & Drop
    if (uploadZone) {
        uploadZone.addEventListener('dragover', function (e) {
            e.preventDefault();
            this.classList.add('dragover');
        });

        uploadZone.addEventListener('dragleave', function () {
            this.classList.remove('dragover');
        });

        uploadZone.addEventListener('drop', function (e) {
            e.preventDefault();
            this.classList.remove('dragover');
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                imageFile.files = files;
                imageFile.dispatchEvent(new Event('change'));
            }
        });
    }

    // ── Submit button loading state ─────────────────────────────────
    const hotelForm = document.getElementById('hotelCreateForm');
    if (hotelForm) {
        hotelForm.addEventListener('submit', function () {
            const btn = document.getElementById('submitBtn');
            if (btn) {
                btn.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14"
                     viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"
                     style="animation: spin 1s linear infinite;">
                    <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                </svg> Saving...`;
                btn.disabled = true;
            }
        });
    }

});
