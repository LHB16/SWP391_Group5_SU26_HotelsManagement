document.addEventListener("DOMContentLoaded", function () {
    const hotelId = document.body.getAttribute("data-hotel-id") || 0;

    // 1. Vote Review Logic (Upvote / Downvote)
    document.querySelectorAll(".vote-btn").forEach(button => {
        button.addEventListener("click", function (e) {
            e.preventDefault();
            const reviewId = this.getAttribute("data-review-id");
            const voteType = this.getAttribute("data-vote-type");
            const currentButton = this;

            const parentDiv = this.parentElement;
            const otherButton = parentDiv.querySelector(`.vote-btn[data-vote-type="${voteType === 'UPVOTE' ? 'DOWNVOTE' : 'UPVOTE'}"]`);

            const formData = new FormData();
            formData.append("type", voteType);

            fetch(`/hotels/${hotelId}/reviews/${reviewId}/vote`, {
                method: "POST",
                body: formData
            })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        // Cập nhật số hiển thị
                        parentDiv.querySelector('.vote-btn[data-vote-type="UPVOTE"] .vote-count').textContent = data.upvotes;
                        parentDiv.querySelector('.vote-btn[data-vote-type="DOWNVOTE"] .vote-count').textContent = data.downvotes;

                        // Cập nhật trạng thái active
                        if (data.userVote === "UPVOTE") {
                            currentButton.classList.add("active-vote");
                            if (otherButton) otherButton.classList.remove("active-vote");
                        } else if (data.userVote === "DOWNVOTE") {
                            currentButton.classList.add("active-vote");
                            if (otherButton) otherButton.classList.remove("active-vote");
                        } else {
                            currentButton.classList.remove("active-vote");
                            if (otherButton) otherButton.classList.remove("active-vote");
                        }
                    } else {
                        if (typeof window.showCustomToast === 'function') {
                            window.showCustomToast(data.message || "An error occurred.", "error");
                        } else {
                            alert(data.message || "An error occurred.");
                        }
                    }
                })
                .catch(err => {
                    console.error("Error voting:", err);
                });
        });
    });

    // 2. Toggle Status Review Logic (Hide / Show - Admin/Owner Only)
    document.querySelectorAll(".toggle-status-btn").forEach(button => {
        button.addEventListener("click", function (e) {
            e.preventDefault();
            const reviewId = this.getAttribute("data-review-id");
            const currentStatus = this.getAttribute("data-status");
            const targetStatus = currentStatus === "VISIBLE" ? "HIDDEN" : "VISIBLE";
            const currentButton = this;

            const formData = new FormData();
            formData.append("status", targetStatus);

            fetch(`/hotels/${hotelId}/reviews/${reviewId}/status`, {
                method: "POST",
                body: formData
            })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        currentButton.setAttribute("data-status", data.status);
                        currentButton.textContent = data.status === "VISIBLE" ? "Hide" : "Show";

                        const badgeContainer = document.querySelector(`.status-badge-container[data-review-id="${reviewId}"]`);
                        if (badgeContainer) {
                            if (data.status === "HIDDEN") {
                                badgeContainer.innerHTML = '<span class="badge bg-secondary text-white" style="font-size: 0.7rem; border-radius: 4px; padding: 2px 6px;">HIDDEN</span>';
                            } else {
                                badgeContainer.innerHTML = '';
                            }
                        }
                    } else {
                        if (typeof window.showCustomToast === 'function') {
                            window.showCustomToast(data.message || "An error occurred.", "error");
                        } else {
                            alert(data.message || "An error occurred.");
                        }
                    }
                })
                .catch(err => {
                    console.error("Error updating review status:", err);
                });
        });
    });

    // 3. Wishlist AJAX Toggle Logic (Favorite / Unfavorite Room)
    document.querySelectorAll(".wishlist-heart-btn").forEach(btn => {
        btn.addEventListener("click", function(e) {
            e.preventDefault();
            const roomId = this.getAttribute("data-room-id");
            const checkin = this.getAttribute("data-checkin") || "";
            const checkout = this.getAttribute("data-checkout") || "";
            const icon = this.querySelector("i");

            fetch(`/api/wishlist/toggle?roomId=${roomId}&checkin=${checkin}&checkout=${checkout}`)
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        if (data.active) {
                            icon.className = "bi bi-heart-fill pink-heart";
                            if (typeof window.showCustomToast === 'function') {
                                window.showCustomToast("Added to wishlist!", "success");
                            }
                        } else {
                            icon.className = "bi bi-heart pink-heart";
                            if (typeof window.showCustomToast === 'function') {
                                window.showCustomToast("Removed from wishlist.", "success");
                            }
                        }
                    } else {
                        if (typeof window.showCustomToast === 'function') {
                            window.showCustomToast(data.message || "An error occurred.", "error");
                        } else {
                            alert(data.message || "An error occurred.");
                        }
                    }
                })
                .catch(err => {
                    console.error("Error toggling wishlist:", err);
                });
        });
    });

    // 4. Auto-scroll & Highlight Review Logic
    if (window.location.hash && window.location.hash.startsWith('#review-')) {
        const performScroll = function() {
            setTimeout(function() {
                const target = document.querySelector(window.location.hash);
                if (target) {
                    target.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    // Highlight review đó lên trong 3 giây
                    target.style.transition = 'background-color 0.5s';
                    target.style.backgroundColor = 'rgba(201, 169, 110, 0.15)';
                    setTimeout(function() {
                        target.style.backgroundColor = '';
                    }, 3000);
                }
            }, 500);
        };

        if (document.readyState === 'complete') {
            performScroll();
        } else {
            window.addEventListener('load', performScroll);
        }
    }
});
