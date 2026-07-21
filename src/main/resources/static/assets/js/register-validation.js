document.addEventListener('DOMContentLoaded', function() {

    var customerForm = document.querySelector('form[action="/register"]');
    if (customerForm) {
        customerForm.addEventListener('submit', function(e) {
            var username = document.getElementById('regUsername');
            var password = document.getElementById('regPassword');
            var isValid = true;
            var errorMsg = '';

            if (username) {
                var val = username.value.trim();
                if (val.length < 8) {
                    isValid = false;
                    errorMsg += 'Username must be at least 8 characters long.\n';
                    username.classList.add('is-invalid');
                } else if (val.length > 30) {
                    isValid = false;
                    errorMsg += 'Username must not exceed 30 characters.\n';
                    username.classList.add('is-invalid');
                } else if (!/^[A-Za-z0-9_]+$/.test(val)) {
                    isValid = false;
                    errorMsg += 'Username can only contain letters, numbers, and underscores.\n';
                    username.classList.add('is-invalid');
                } else {
                    username.classList.remove('is-invalid');
                    username.classList.add('is-valid');
                }
            }

            if (password) {
                var val = password.value;
                if (val.length < 8) {
                    isValid = false;
                    errorMsg += 'Password must be at least 8 characters long.\n';
                    password.classList.add('is-invalid');
                } else {
                    password.classList.remove('is-invalid');
                    password.classList.add('is-valid');
                }
            }

            var confirmPassword = document.getElementById('regConfirmPassword');
            if (password && confirmPassword) {
                if (password.value !== confirmPassword.value) {
                    isValid = false;
                    errorMsg += 'Passwords do not match.\n';
                    confirmPassword.classList.add('is-invalid');
                    var matchErr = document.getElementById('regMatchError');
                    if (matchErr) matchErr.style.display = 'block';
                } else {
                    confirmPassword.classList.remove('is-invalid');
                    confirmPassword.classList.add('is-valid');
                    var matchErr = document.getElementById('regMatchError');
                    if (matchErr) matchErr.style.display = 'none';
                }
            }

            if (!isValid) {
                e.preventDefault();
                alert(errorMsg);
                var firstInvalid = customerForm.querySelector('.is-invalid');
                if (firstInvalid) {
                    firstInvalid.focus();
                }
            }
        });

        customerForm.querySelectorAll('input').forEach(function(input) {
            input.addEventListener('input', function() {
                this.classList.remove('is-invalid', 'is-valid');
                var container = this.parentElement;
                if (container.classList.contains('input-group')) {
                    container = container.parentElement;
                }
                var errorDiv = container.querySelector('.text-danger, .text-success');
                if (errorDiv) {
                    errorDiv.remove();
                }
            });
        });
    }

    var ownerForm = document.querySelector('form[action="/register-owner"]');
    if (ownerForm) {
        ownerForm.addEventListener('submit', function(e) {
            var username = document.getElementById('ownerUsername');
            var password = document.getElementById('ownerPassword');
            var isValid = true;
            var errorMsg = '';

            if (username) {
                var val = username.value.trim();
                if (val.length < 8) {
                    isValid = false;
                    errorMsg += 'Username must be at least 8 characters long.\n';
                    username.classList.add('is-invalid');
                } else if (val.length > 30) {
                    isValid = false;
                    errorMsg += 'Username must not exceed 30 characters.\n';
                    username.classList.add('is-invalid');
                } else if (!/^[A-Za-z0-9_]+$/.test(val)) {
                    isValid = false;
                    errorMsg += 'Username can only contain letters, numbers, and underscores.\n';
                    username.classList.add('is-invalid');
                } else {
                    username.classList.remove('is-invalid');
                    username.classList.add('is-valid');
                }
            }

            if (password) {
                var val = password.value;
                if (val.length < 8) {
                    isValid = false;
                    errorMsg += 'Password must be at least 8 characters long.\n';
                    password.classList.add('is-invalid');
                } else {
                    password.classList.remove('is-invalid');
                    password.classList.add('is-valid');
                }
            }

            var confirmPassword = document.getElementById('ownerConfirmPassword');
            if (password && confirmPassword) {
                if (password.value !== confirmPassword.value) {
                    isValid = false;
                    errorMsg += 'Passwords do not match.\n';
                    confirmPassword.classList.add('is-invalid');
                    var matchErr = document.getElementById('ownerMatchError');
                    if (matchErr) matchErr.style.display = 'block';
                } else {
                    confirmPassword.classList.remove('is-invalid');
                    confirmPassword.classList.add('is-valid');
                    var matchErr = document.getElementById('ownerMatchError');
                    if (matchErr) matchErr.style.display = 'none';
                }
            }

            if (!isValid) {
                e.preventDefault();
                alert(errorMsg);
                var firstInvalid = ownerForm.querySelector('.is-invalid');
                if (firstInvalid) {
                    firstInvalid.focus();
                }
            }
        });

        ownerForm.querySelectorAll('input').forEach(function(input) {
            input.addEventListener('input', function() {
                this.classList.remove('is-invalid', 'is-valid');
                var container = this.parentElement;
                if (container.classList.contains('input-group')) {
                    container = container.parentElement;
                }
                var errorDiv = container.querySelector('.text-danger, .text-success');
                if (errorDiv) {
                    errorDiv.remove();
                }
            });
        });
    }

    var customerUsername = document.getElementById('regUsername');
    if (customerUsername) {
        customerUsername.addEventListener('input', function() {
            var val = this.value.trim();
            var errorId = 'regUsernameError';
            var container = this.parentElement;
            if (container.classList.contains('input-group')) {
                container = container.parentElement;
            }
            var errorEl = container.querySelector('#' + errorId);

            if (!errorEl) {
                errorEl = document.createElement('div');
                errorEl.id = errorId;
                errorEl.className = 'text-danger small mt-1';
                container.appendChild(errorEl);
            }

            if (val.length > 0) {
                if (val.length < 8) {
                    errorEl.textContent = 'Username must be at least 8 characters.';
                    errorEl.className = 'text-danger small mt-1';
                    this.classList.add('is-invalid');
                    this.classList.remove('is-valid');
                } else if (val.length > 30) {
                    errorEl.textContent = 'Username must not exceed 30 characters.';
                    errorEl.className = 'text-danger small mt-1';
                    this.classList.add('is-invalid');
                    this.classList.remove('is-valid');
                } else if (!/^[A-Za-z0-9_]+$/.test(val)) {
                    errorEl.textContent = 'Only letters, numbers, and underscores allowed.';
                    errorEl.className = 'text-danger small mt-1';
                    this.classList.add('is-invalid');
                    this.classList.remove('is-valid');
                } else {
                    if (errorEl) {
                        errorEl.textContent = 'Valid username.';
                        errorEl.className = 'text-success small mt-1';
                    }
                    this.classList.remove('is-invalid');
                    this.classList.add('is-valid');
                }
            } else {
                if (errorEl) errorEl.remove();
                this.classList.remove('is-invalid', 'is-valid');
            }
        });
    }

    var customerPassword = document.getElementById('regPassword');
    if (customerPassword) {
        customerPassword.addEventListener('input', function() {
            var val = this.value;
            var errorId = 'regPasswordError';
            var container = this.parentElement;
            if (container.classList.contains('input-group')) {
                container = container.parentElement;
            }
            var errorEl = container.querySelector('#' + errorId);

            if (!errorEl) {
                errorEl = document.createElement('div');
                errorEl.id = errorId;
                errorEl.className = 'text-danger small mt-1';
                container.appendChild(errorEl);
            }

            if (val.length > 0) {
                if (val.length < 8) {
                    errorEl.textContent = 'Password must be at least 8 characters.';
                    errorEl.className = 'text-danger small mt-1';
                    this.classList.add('is-invalid');
                    this.classList.remove('is-valid');
                } else {
                    if (errorEl) {
                        errorEl.textContent = 'Valid password.';
                        errorEl.className = 'text-success small mt-1';
                    }
                    this.classList.remove('is-invalid');
                    this.classList.add('is-valid');
                }
            } else {
                if (errorEl) errorEl.remove();
                this.classList.remove('is-invalid', 'is-valid');
            }
        });
    }

    var ownerUsername = document.getElementById('ownerUsername');
    if (ownerUsername) {
        ownerUsername.addEventListener('input', function() {
            var val = this.value.trim();
            var errorId = 'ownerUsernameError';
            var container = this.parentElement;
            if (container.classList.contains('input-group')) {
                container = container.parentElement;
            }
            var errorEl = container.querySelector('#' + errorId);

            if (!errorEl) {
                errorEl = document.createElement('div');
                errorEl.id = errorId;
                errorEl.className = 'text-danger small mt-1';
                container.appendChild(errorEl);
            }

            if (val.length > 0) {
                if (val.length < 8) {
                    errorEl.textContent = 'Username must be at least 8 characters.';
                    errorEl.className = 'text-danger small mt-1';
                    this.classList.add('is-invalid');
                    this.classList.remove('is-valid');
                } else if (val.length > 30) {
                    errorEl.textContent = 'Username must not exceed 30 characters.';
                    errorEl.className = 'text-danger small mt-1';
                    this.classList.add('is-invalid');
                    this.classList.remove('is-valid');
                } else if (!/^[A-Za-z0-9_]+$/.test(val)) {
                    errorEl.textContent = 'Only letters, numbers, and underscores allowed.';
                    errorEl.className = 'text-danger small mt-1';
                    this.classList.add('is-invalid');
                    this.classList.remove('is-valid');
                } else {
                    if (errorEl) {
                        errorEl.textContent = 'Valid username.';
                        errorEl.className = 'text-success small mt-1';
                    }
                    this.classList.remove('is-invalid');
                    this.classList.add('is-valid');
                }
            } else {
                if (errorEl) errorEl.remove();
                this.classList.remove('is-invalid', 'is-valid');
            }
        });
    }

    var ownerPassword = document.getElementById('ownerPassword');
    if (ownerPassword) {
        ownerPassword.addEventListener('input', function() {
            var val = this.value;
            var errorId = 'ownerPasswordError';
            var container = this.parentElement;
            if (container.classList.contains('input-group')) {
                container = container.parentElement;
            }
            var errorEl = container.querySelector('#' + errorId);

            if (!errorEl) {
                errorEl = document.createElement('div');
                errorEl.id = errorId;
                errorEl.className = 'text-danger small mt-1';
                container.appendChild(errorEl);
            }

            if (val.length > 0) {
                if (val.length < 8) {
                    errorEl.textContent = 'Password must be at least 8 characters.';
                    errorEl.className = 'text-danger small mt-1';
                    this.classList.add('is-invalid');
                    this.classList.remove('is-valid');
                } else {
                    if (errorEl) {
                        errorEl.textContent = 'Valid password.';
                        errorEl.className = 'text-success small mt-1';
                    }
                    this.classList.remove('is-invalid');
                    this.classList.add('is-valid');
                }
            } else {
                if (errorEl) errorEl.remove();
                this.classList.remove('is-invalid', 'is-valid');
            }
        });
    }

});
document.addEventListener("DOMContentLoaded", function() {
    var phoneInput = document.getElementById("phone");
    var phoneError = document.getElementById("phone-error");
    if (phoneInput) {
        var iti = window.intlTelInput(phoneInput, {
            initialCountry: "vn",
            preferredCountries: ["vn"],
            separateDialCode: true,
            nationalMode: false,
            utilsScript: "https://cdnjs.cloudflare.com/ajax/libs/intl-tel-input/17.0.19/js/utils.js"
        });

        var form = phoneInput.closest("form");
        if (form) {
            form.addEventListener("submit", function(e) {
                if (phoneInput.value.trim()) {
                    if (!iti.isValidNumber()) {
                        e.preventDefault();
                        if (phoneError) {
                            phoneError.textContent = "Invalid phone number format for the selected country.";
                            phoneError.style.display = "block";
                        }
                        phoneInput.classList.add("is-invalid");
                    } else {
                        if (phoneError) {
                            phoneError.style.display = "none";
                        }
                        phoneInput.classList.remove("is-invalid");
                        phoneInput.value = iti.getNumber();
                    }
                }
            });
        }

        phoneInput.addEventListener("input", function() {
            if (phoneError) {
                phoneError.style.display = "none";
            }
            phoneInput.classList.remove("is-invalid");
        });
    }
});