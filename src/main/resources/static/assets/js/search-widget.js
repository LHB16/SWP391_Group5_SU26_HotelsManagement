document.addEventListener('DOMContentLoaded', function() {
    // 1. Initialize Lucide icons if available
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }

    // 2. Setup Flatpickr for Check-in & Check-out
    const checkinInput = document.getElementById('checkinInput');
    const checkoutInput = document.getElementById('checkoutInput');
    
    if (checkinInput && checkoutInput && typeof flatpickr === "function") {
        const today = new Date();
        const tomorrow = new Date();
        tomorrow.setDate(today.getDate() + 1);

        // Get initial dates from inputs if set, else default
        const initCheckin = checkinInput.value ? new Date(checkinInput.value) : today;
        const initCheckout = checkoutInput.value ? new Date(checkoutInput.value) : tomorrow;

        const checkinPicker = flatpickr(checkinInput, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            altInputClass: "search-pill-input",
            minDate: "today",
            defaultDate: initCheckin,
            onChange: function(selectedDates) {
                if (selectedDates[0]) {
                    const next = new Date(selectedDates[0]);
                    next.setDate(next.getDate() + 1);
                    checkoutPicker.set("minDate", next);
                    const cur = checkoutPicker.selectedDates[0];
                    if (!cur || cur <= selectedDates[0]) {
                        checkoutPicker.setDate(next);
                    }
                }
            }
        });

        const checkoutPicker = flatpickr(checkoutInput, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            altInputClass: "search-pill-input",
            minDate: initCheckout,
            defaultDate: initCheckout
        });
    }

    // 3. Guests Picker Popover Toggle
    const trigger = document.getElementById('guestsPickerTrigger');
    const dropdown = document.getElementById('guestsDropdown');
    const doneBtn = document.getElementById('btnGuestsDone');
    
    if (trigger && dropdown) {
        trigger.addEventListener('click', function(e) {
            e.stopPropagation();
            dropdown.classList.toggle('d-none');
        });
        
        dropdown.addEventListener('click', function(e) {
            e.stopPropagation();
        });
        
        document.addEventListener('click', function() {
            dropdown.classList.add('d-none');
        });
        
        if (doneBtn) {
            doneBtn.addEventListener('click', function() {
                dropdown.classList.add('d-none');
                const form = doneBtn.closest('form');
                if (form) {
                    form.submit();
                }
            });
        }
    }

    // 4. Counter Logic
    function setupCounter(minusId, plusId, displayId, min, max) {
        const minusBtn = document.getElementById(minusId);
        const plusBtn = document.getElementById(plusId);
        const display = document.getElementById(displayId);
        
        if (minusBtn && plusBtn && display) {
            minusBtn.addEventListener('click', function() {
                let val = parseInt(display.textContent);
                if (val > min) {
                    val--;
                    display.textContent = val;
                    updateGuestsDisplay();
                }
            });
            
            plusBtn.addEventListener('click', function() {
                let val = parseInt(display.textContent);
                if (val < max) {
                    val++;
                    display.textContent = val;
                    updateGuestsDisplay();
                }
            });
        }
    }
    
    setupCounter('btnPersonMinus', 'btnPersonPlus', 'personCountDisplay', 1, 10);
    setupCounter('btnRoomMinus', 'btnRoomPlus', 'roomCountDisplay', 1, 10);
    
    function updateGuestsDisplay() {
        const personDisplay = document.getElementById('personCountDisplay');
        const roomDisplay = document.getElementById('roomCountDisplay');
        
        if (personDisplay && roomDisplay) {
            const persons = personDisplay.textContent;
            const rooms = roomDisplay.textContent;
            
            const displayInput = document.getElementById('guestsInputDisplay');
            if (displayInput) {
                const personText = `${persons} Person${parseInt(persons) > 1 ? 's' : ''}`;
                const roomText = `${rooms} Room${parseInt(rooms) > 1 ? 's' : ''}`;
                displayInput.value = `${personText}, ${roomText}`;
            }

            // Update hidden inputs if present for form submission
            const hiddenPersons = document.getElementById('hiddenPersons');
            const hiddenRooms = document.getElementById('hiddenRooms');
            if (hiddenPersons) hiddenPersons.value = persons;
            if (hiddenRooms) hiddenRooms.value = rooms;
        }
    }

    // Call initial update to make sure inputs align
    updateGuestsDisplay();

    // 5. Destination Suggestions Autocomplete Dropdown
    const destInput = document.getElementById('destInput');
    const destDropdown = document.getElementById('destSuggestions');
    if (destInput && destDropdown) {
        destInput.addEventListener('focus', function() {
            destDropdown.classList.remove('d-none');
        });
        
        // Hide dropdown when clicking outside
        document.addEventListener('click', function(e) {
            if (!destInput.contains(e.target) && !destDropdown.contains(e.target)) {
                destDropdown.classList.add('d-none');
            }
        });
        
        // Handle suggestion click
        const items = destDropdown.querySelectorAll('.suggestion-item');
        items.forEach(item => {
            item.addEventListener('click', function() {
                destInput.value = this.getAttribute('data-val');
                destDropdown.classList.add('d-none');
            });
        });
    }
});
