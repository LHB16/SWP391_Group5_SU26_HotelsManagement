document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('hotelForm');

    if (form) {
        form.addEventListener('submit', function(e) {
            const name = document.getElementById('hotelName').value.trim();
            const address = document.getElementById('hotelAddress').value.trim();
            const city = document.getElementById('hotelCity').value.trim();
            const description = document.getElementById('hotelDescription').value.trim();
            const imageFile = document.getElementById('imageFile');

            if (!name || !address || !city) {
                e.preventDefault();
                alert('Please fill in all required fields (Name, Address, City)');
                return;
            }

            if (!description) {
                e.preventDefault();
                alert('Hotel description is required!');
                return;
            }

            if (!imageFile || !imageFile.files || imageFile.files.length === 0) {
                e.preventDefault();
                alert('Hotel image is required!');
                return;
            }

            const file = imageFile.files[0];
            const fileType = file.type;
            if (!fileType.startsWith('image/')) {
                e.preventDefault();
                alert('Hotel image must be an image file (JPG, PNG, WEBP, etc.)!');
                return;
            }

            const docInputs = [
                { id: 'businessRegistrationDoc', name: 'Business Registration' },
                { id: 'landCertificateDoc', name: 'Land Certificate' },
                { id: 'rentalContractDoc', name: 'Rental Contract' }
            ];

            for (const doc of docInputs) {
                const input = document.getElementById(doc.id);
                if (input && input.files && input.files.length > 0) {
                    const docFile = input.files[0];
                    if (docFile.type !== 'application/pdf') {
                        e.preventDefault();
                        alert(doc.name + ' must be a PDF file!');
                        return;
                    }
                }
            }
        });
    }
        document.addEventListener('DOMContentLoaded', function() {

        // Preview ảnh Hotel
        const hotelImageInput = document.getElementById('imageFile');
        const hotelImagePreview = document.getElementById('hotelImagePreview');
        if (hotelImageInput) {
        hotelImageInput.addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (file) {
        const reader = new FileReader();
        reader.onload = function(ev) {
        hotelImagePreview.innerHTML = `
                        <div class="d-inline-block position-relative">
                            <img src="${ev.target.result}" class="img-thumbnail" style="max-height: 150px; border-radius: 8px; border: 2px solid var(--gold);" />
                            <span class="badge bg-success position-absolute top-0 start-0 m-1">Selected</span>
                        </div>
                        <span class="ms-2 small text-success"><i class="bi bi-check-circle-fill"></i> ${file.name}</span>
                    `;
    };
        reader.readAsDataURL(file);
    } else {
        hotelImagePreview.innerHTML = '';
    }
    });
    }

        // Preview Business Registration
        const businessInput = document.getElementById('businessRegistrationDoc');
        const businessPreview = document.getElementById('businessRegistrationPreview');
        if (businessInput) {
        businessInput.addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (file) {
        if (file.type === 'application/pdf') {
        businessPreview.innerHTML = `
                            <span class="badge bg-info"><i class="bi bi-file-pdf"></i> ${file.name}</span>
                            <span class="ms-2 small text-success"><i class="bi bi-check-circle-fill"></i> PDF file selected</span>
                        `;
    } else {
        businessPreview.innerHTML = `
                            <span class="badge bg-danger"><i class="bi bi-x-circle"></i> Invalid file type. Please select PDF.</span>
                        `;
        businessInput.value = '';
    }
    } else {
        businessPreview.innerHTML = '';
    }
    });
    }

        // Preview Land Certificate
        const landInput = document.getElementById('landCertificateDoc');
        const landPreview = document.getElementById('landCertificatePreview');
        if (landInput) {
        landInput.addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (file) {
        if (file.type === 'application/pdf') {
        landPreview.innerHTML = `
                            <span class="badge bg-info"><i class="bi bi-file-pdf"></i> ${file.name}</span>
                            <span class="ms-2 small text-success"><i class="bi bi-check-circle-fill"></i> PDF file selected</span>
                        `;
    } else {
        landPreview.innerHTML = `
                            <span class="badge bg-danger"><i class="bi bi-x-circle"></i> Invalid file type. Please select PDF.</span>
                        `;
        landInput.value = '';
    }
    } else {
        landPreview.innerHTML = '';
    }
    });
    }

        // Preview Rental Contract
        const rentalInput = document.getElementById('rentalContractDoc');
        const rentalPreview = document.getElementById('rentalContractPreview');
        if (rentalInput) {
        rentalInput.addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (file) {
        if (file.type === 'application/pdf') {
        rentalPreview.innerHTML = `
                            <span class="badge bg-info"><i class="bi bi-file-pdf"></i> ${file.name}</span>
                            <span class="ms-2 small text-muted"><i class="bi bi-check-circle"></i> PDF file selected</span>
                        `;
    } else {
        rentalPreview.innerHTML = `
                            <span class="badge bg-danger"><i class="bi bi-x-circle"></i> Invalid file type. Please select PDF.</span>
                        `;
        rentalInput.value = '';
    }
    } else {
        rentalPreview.innerHTML = '';
    }
    });
    }
    });

});
