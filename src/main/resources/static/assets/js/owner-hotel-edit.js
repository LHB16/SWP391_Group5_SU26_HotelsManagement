document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('hotelForm');

    if (form) {
        form.addEventListener('submit', function(e) {
            const name = document.getElementById('hotelName').value.trim();
            const address = document.getElementById('hotelAddress').value.trim();
            const city = document.getElementById('hotelCity').value.trim();
            const description = document.getElementById('hotelDescription').value.trim();

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
        });
    }
});