document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('hotelForm');

    if (form) {
        form.addEventListener('submit', function(e) {
            const name = document.getElementById('hotelName').value.trim();
            const address = document.getElementById('hotelAddress').value.trim();
            const city = document.getElementById('hotelCity').value.trim();

            if (!name || !address || !city) {
                e.preventDefault();
                alert('Please fill in all required fields (Name, Address, City)');
            }
        });
    }
});