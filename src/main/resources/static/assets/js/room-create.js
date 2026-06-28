/**
 * room-create.js
 * JS riêng cho trang thêm phòng mới (hotel/room-create.html)
 */

document.addEventListener('DOMContentLoaded', function () {

    // ── Stepper buttons (person, bed, window) ──────────────────────
    document.querySelectorAll('.stepper-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const input = document.getElementById(this.dataset.target);
            const op    = this.dataset.op;
            const min   = parseInt(input.min) || 0;
            const max   = parseInt(input.max) || 99;
            let   val   = parseInt(input.value) || 0;

            if (op === 'plus'  && val < max) input.value = val + 1;
            if (op === 'minus' && val > min) input.value = val - 1;
        });
    });

    // ── Image Upload Preview ────────────────────────────────────────
    const imageFile   = document.getElementById('imageFile');
    const previewWrap = document.getElementById('previewWrap');
    const previewImg  = document.getElementById('previewImg');
    const removeBtn   = document.getElementById('removeImg');
    const uploadZone  = document.getElementById('uploadZone');

    if (imageFile) {
        imageFile.addEventListener('change', function () {
            const file = this.files[0];
            if (!file) return;

            if (file.size > 10 * 1024 * 1024) {
                alert('File size exceeds 10MB. Please choose a smaller file.');
                this.value = '';
                return;
            }

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

    // ── Submit loading state ────────────────────────────────────────
    const roomForm = document.getElementById('roomForm');
    if (roomForm) {
        roomForm.addEventListener('submit', function () {
            const btn = document.getElementById('submitBtn');
            if (btn) {
                btn.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14"
                    viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                    stroke-linecap="round" stroke-linejoin="round"
                    style="animation:spin 1s linear infinite;">
                    <path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg> Saving...`;
                btn.disabled = true;
            }
        });
    }

});
