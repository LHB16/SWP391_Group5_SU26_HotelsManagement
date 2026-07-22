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
            if (['regUsername', 'regPassword', 'regConfirmPassword'].indexOf(input.id) !== -1) return;
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
                } else if (username.classList.contains('is-invalid')) {
                    isValid = false;
                    errorMsg += 'Username is already taken or invalid.\n';
                }
            }

            if (password) {
                var val = password.value;
                if (val.length < 8) {
                    isValid = false;
                    errorMsg += 'Password must be at least 8 characters long.\n';
                    password.classList.add('is-invalid');
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
            if (['ownerUsername', 'ownerPassword', 'ownerConfirmPassword'].indexOf(input.id) !== -1) return;
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

    function setupUsernameAjaxCheck(inputElement, errorId) {
        if (!inputElement) return;
        var timer = null;

        inputElement.addEventListener('input', function() {
            var input = this;
            var val = input.value.trim();
            var container = input.parentElement;
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

            if (timer) {
                clearTimeout(timer);
            }

            if (val.length === 0) {
                errorEl.remove();
                input.classList.remove('is-invalid', 'is-valid');
                return;
            }

            if (val.length < 8) {
                errorEl.textContent = 'Username must be at least 8 characters.';
                errorEl.className = 'text-danger small mt-1';
                input.classList.add('is-invalid');
                input.classList.remove('is-valid');
                return;
            }
            if (val.length > 30) {
                errorEl.textContent = 'Username must not exceed 30 characters.';
                errorEl.className = 'text-danger small mt-1';
                input.classList.add('is-invalid');
                input.classList.remove('is-valid');
                return;
            }
            if (!/^[A-Za-z0-9_]+$/.test(val)) {
                errorEl.textContent = 'Only letters, numbers, and underscores allowed.';
                errorEl.className = 'text-danger small mt-1';
                input.classList.add('is-invalid');
                input.classList.remove('is-valid');
                return;
            }

            errorEl.textContent = 'Checking availability...';
            errorEl.className = 'text-muted small mt-1';
            input.classList.remove('is-invalid', 'is-valid');

            timer = setTimeout(function() {
                fetch('/register/check-username?username=' + encodeURIComponent(val))
                    .then(function(res) {
                        return res.json();
                    })
                    .then(function(data) {
                        if (input.value.trim() !== val) return;

                        if (data.valid) {
                            errorEl.textContent = data.message || 'Username is available.';
                            errorEl.className = 'text-success small mt-1';
                            input.classList.remove('is-invalid');
                            input.classList.add('is-valid');
                        } else {
                            errorEl.textContent = data.message || 'Username is already taken!';
                            errorEl.className = 'text-danger small mt-1';
                            input.classList.remove('is-valid');
                            input.classList.add('is-invalid');
                        }
                    })
                    .catch(function(err) {
                        console.error('Error checking username:', err);
                        if (input.value.trim() !== val) return;
                        errorEl.textContent = 'Unable to verify username availability.';
                        errorEl.className = 'text-warning small mt-1';
                    });
            }, 400);
        });
    }

    function setupPasswordMatchCheck(passInput, confirmInput, passErrorId, confirmErrorId) {
        if (!passInput) return;

        function getErrorElement(input, errorId) {
            var container = input.parentElement;
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
            return errorEl;
        }

        function validatePassword() {
            var val = passInput.value;
            var errorEl = getErrorElement(passInput, passErrorId);

            if (val.length === 0) {
                errorEl.remove();
                passInput.classList.remove('is-invalid', 'is-valid');
            } else if (val.length < 8) {
                errorEl.textContent = 'Password must be at least 8 characters.';
                errorEl.className = 'text-danger small mt-1';
                passInput.classList.add('is-invalid');
                passInput.classList.remove('is-valid');
            } else {
                errorEl.textContent = 'Valid password.';
                errorEl.className = 'text-success small mt-1';
                passInput.classList.remove('is-invalid');
                passInput.classList.add('is-valid');
            }

            if (confirmInput && confirmInput.value.length > 0) {
                validateConfirmPassword();
            }
        }

        function validateConfirmPassword() {
            if (!confirmInput) return;
            var passVal = passInput.value;
            var confirmVal = confirmInput.value;
            var errorEl = getErrorElement(confirmInput, confirmErrorId);

            if (confirmVal.length === 0) {
                errorEl.remove();
                confirmInput.classList.remove('is-invalid', 'is-valid');
            } else if (passVal.length < 8) {
                errorEl.textContent = 'Please enter a valid password first.';
                errorEl.className = 'text-danger small mt-1';
                confirmInput.classList.add('is-invalid');
                confirmInput.classList.remove('is-valid');
            } else if (confirmVal !== passVal) {
                errorEl.textContent = 'Passwords do not match!';
                errorEl.className = 'text-danger small mt-1';
                confirmInput.classList.add('is-invalid');
                confirmInput.classList.remove('is-valid');
            } else {
                errorEl.textContent = 'Passwords match.';
                errorEl.className = 'text-success small mt-1';
                confirmInput.classList.remove('is-invalid');
                confirmInput.classList.add('is-valid');
            }
        }

        passInput.addEventListener('input', validatePassword);
        if (confirmInput) {
            confirmInput.addEventListener('input', validateConfirmPassword);
        }
    }

    var customerUsername = document.getElementById('regUsername');
    if (customerUsername) {
        setupUsernameAjaxCheck(customerUsername, 'regUsernameError');
    }

    var ownerUsername = document.getElementById('ownerUsername');
    if (ownerUsername) {
        setupUsernameAjaxCheck(ownerUsername, 'ownerUsernameError');
    }

    var regPassword = document.getElementById('regPassword');
    var regConfirmPassword = document.getElementById('regConfirmPassword');
    if (regPassword) {
        setupPasswordMatchCheck(regPassword, regConfirmPassword, 'regPasswordError', 'regConfirmPasswordError');
    }

    var ownerPassword = document.getElementById('ownerPassword');
    var ownerConfirmPassword = document.getElementById('ownerConfirmPassword');
    if (ownerPassword) {
        setupPasswordMatchCheck(ownerPassword, ownerConfirmPassword, 'ownerPasswordError', 'ownerConfirmPasswordError');
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