function getNextDayStr(dateStr) {
    if (!dateStr) return "";
    const parts = dateStr.split("-");
    if (parts.length === 3) {
        const year = parseInt(parts[0], 10);
        const month = parseInt(parts[1], 10) - 1;
        const day = parseInt(parts[2], 10);
        const d = new Date(year, month, day);
        d.setDate(d.getDate() + 1);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const dayStr = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${dayStr}`;
    }
    return "";
}

document.addEventListener("DOMContentLoaded", function() {
    const checkinInputs = document.querySelectorAll(".booking-checkin-picker");
    const checkoutInputs = document.querySelectorAll(".booking-checkout-picker");

    checkinInputs.forEach(function(checkinInput, index) {
        const checkoutInput = checkoutInputs[index];
        
        const ciVal = checkinInput.value;
        const coVal = checkoutInput ? checkoutInput.value : '';

        const ciPicker = flatpickr(checkinInput, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            altInputClass: "form-control form-control-sm bg-light",
            minDate: "today",
            allowInput: false,
            clickOpens: false,
            defaultDate: ciVal ? ciVal : null,
            onChange: function(selectedDates, dateStr) {
                if (selectedDates[0] && coPicker) {
                    const d = selectedDates[0];
                    const nextDay = new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1);
                    const y = nextDay.getFullYear();
                    const m = String(nextDay.getMonth() + 1).padStart(2, '0');
                    const dayStr = String(nextDay.getDate()).padStart(2, '0');
                    const nextDayStr = `${y}-${m}-${dayStr}`;
                    
                    const currentCheckout = coPicker.selectedDates[0];
                    if (!currentCheckout || currentCheckout <= d) {
                        coPicker.setDate(nextDayStr);
                    }
                    
                    coPicker.set("minDate", nextDayStr);
                }
            }
        });

        let coPicker = null;
        if (checkoutInput) {
            const minCoDate = ciVal ? getNextDayStr(ciVal) : "today";
            coPicker = flatpickr(checkoutInput, {
                dateFormat: "Y-m-d",
                altInput: true,
                altFormat: "d/m/Y",
                altInputClass: "form-control form-control-sm bg-light",
                minDate: minCoDate,
                allowInput: false,
                clickOpens: false,
                defaultDate: coVal ? coVal : null
            });
        }
    });
});
