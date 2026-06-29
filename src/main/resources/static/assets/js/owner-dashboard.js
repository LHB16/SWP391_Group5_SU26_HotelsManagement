// src/main/resources/static/assets/js/owner-dashboard.js

document.addEventListener('DOMContentLoaded', function() {

    // Cập nhật thời gian
    updateDateTime();

    // Active nav
    setActiveNavItem();

    // Animation stat cards
    animateStatCards();
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
        el.textContent = now.toLocaleDateString('vi-VN', options);
    }

    update();
    setInterval(update, 1000);
}

function setActiveNavItem() {
    const currentPath = window.location.pathname;
    document.querySelectorAll('.sidebar .nav-link').forEach(function(link) {
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('active');
        }
    });
}

function animateStatCards() {
    const observer = new IntersectionObserver(function(entries) {
        entries.forEach(function(entry) {
            if (entry.isIntersecting) {
                entry.target.classList.add('fade-in');
            }
        });
    }, { threshold: 0.1 });

    document.querySelectorAll('.stat-card').forEach(function(card) {
        observer.observe(card);
    });
}