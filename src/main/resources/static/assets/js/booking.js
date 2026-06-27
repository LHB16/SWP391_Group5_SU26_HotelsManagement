function calculateTotal() {
    let subtotal = 0;
    
    const checkinInput = document.getElementById('checkinDate');
    const checkoutInput = document.getElementById('checkoutDate');
    
    if (!checkinInput || !checkoutInput) return;

    let checkin = new Date(checkinInput.value);
    let checkout = new Date(checkoutInput.value);

    // Validate stay dates
    if (checkout <= checkin) {
        const nextDay = new Date(checkin);
        nextDay.setDate(nextDay.getDate() + 1);
        
        const yyyy = nextDay.getFullYear();
        const mm = String(nextDay.getMonth() + 1).padStart(2, '0');
        const dd = String(nextDay.getDate()).padStart(2, '0');
        checkoutInput.value = `${yyyy}-${mm}-${dd}`;
        checkout = nextDay;
    }

    // Calculate nights
    const diffTime = checkout - checkin;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    const nights = diffDays > 0 ? diffDays : 1;

    document.getElementById('nights-display').innerText = nights + ' Night(s)';

    // Sum prices of selected rooms
    const checkboxes = document.querySelectorAll('input[type="checkbox"]');
    checkboxes.forEach(checkbox => {
        if (checkbox.checked) {
            const price = parseFloat(checkbox.getAttribute('data-price'));
            subtotal += price * nights;
        }
    });

    // Calculate additional fees
    const serviceFee = subtotal > 0 ? 50000 : 0;
    const tax = Math.round(subtotal * 0.1);
    const grandTotal = subtotal + tax + serviceFee;

    // Update displays
    document.getElementById('subtotal-display').innerText = formatVND(subtotal);
    document.getElementById('tax-display').innerText = formatVND(tax);
    document.getElementById('service-display').innerText = formatVND(serviceFee);
    document.getElementById('total-display').innerText = formatVND(grandTotal);
}

function formatVND(amount) {
    return amount.toLocaleString('en-US') + ' VND';
}

document.addEventListener('DOMContentLoaded', function() {
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const formatDate = (date) => {
        const yyyy = date.getFullYear();
        const mm = String(date.getMonth() + 1).padStart(2, '0');
        const dd = String(date.getDate()).padStart(2, '0');
        return `${yyyy}-${mm}-${dd}`;
    };

    const checkinInput = document.getElementById('checkinDate');
    const checkoutInput = document.getElementById('checkoutDate');

    if (checkinInput && !checkinInput.value) {
        checkinInput.value = formatDate(today);
        checkinInput.min = formatDate(today);
    }
    if (checkoutInput && !checkoutInput.value) {
        checkoutInput.value = formatDate(tomorrow);
        checkoutInput.min = formatDate(tomorrow);
    }

    calculateTotal();
});
