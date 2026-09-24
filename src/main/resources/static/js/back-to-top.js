// Back-to-top button: appears once the page has scrolled down a bit, and
// jumps straight to the top with no animation when clicked. This is
// intentionally instant rather than smooth (unlike the footer's logo/nav
// anchors), so it's passed behavior: 'auto' explicitly — that overrides
// the page-wide `scroll-behavior: smooth` set in style.css for this one
// call only.
(function () {
    var btn = document.getElementById('backToTopBtn');
    if (!btn) {
        return;
    }

    var SHOW_AFTER_PX = 400;

    function toggleVisibility() {
        if (window.scrollY > SHOW_AFTER_PX) {
            btn.classList.add('show');
        } else {
            btn.classList.remove('show');
        }
    }

    window.addEventListener('scroll', toggleVisibility, { passive: true });
    toggleVisibility();

    btn.addEventListener('click', function () {
        window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
    });
})();
