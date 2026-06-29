// src/main/resources/static/assets/js/owner-dashboard.js

document.addEventListener('DOMContentLoaded', function() {
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
    updateDateTime();
});

function updateDateTime() {
    const el = document.getElementById('currentDateTime');
    if (!el) return;

    function update() {
        const now = new Date();
        const options = {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        };
        el.textContent = now.toLocaleDateString('en-US', options);
    }

    update();
    setInterval(update, 1000);
}