/**
 * ODA Login Theme - login.js
 *
 * Password visibility toggle. Works with any element marked with
 * [data-password-toggle] that controls an input referenced by
 * aria-controls. Icons are swapped via the data-icon-show / data-icon-hide
 * attributes and labels via data-label-show / data-label-hide.
 */
(function () {
  'use strict';

  function togglePassword(button) {
    const input = document.getElementById(button.getAttribute('aria-controls'));
    if (!input) {
      return;
    }

    const isHidden = input.type === 'password';
    input.type = isHidden ? 'text' : 'password';

    const icon = button.querySelector('i, .oda-icon');
    if (icon) {
      icon.className = isHidden
        ? button.getAttribute('data-icon-hide')
        : button.getAttribute('data-icon-show');
    }

    button.setAttribute('aria-label', isHidden
      ? button.getAttribute('data-label-hide')
      : button.getAttribute('data-label-show'));
  }

  function init() {
    document.querySelectorAll('[data-password-toggle]').forEach(function (button) {
      button.addEventListener('click', function () {
        togglePassword(button);
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();