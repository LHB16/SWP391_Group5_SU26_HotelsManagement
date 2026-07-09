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
});