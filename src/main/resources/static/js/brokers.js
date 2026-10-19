// Local mortgage brokers row on the homepage: slides along by itself and loops forever,
// the same way the latest-listings carousel does (no arrow button).
document.addEventListener('DOMContentLoaded', function () {
    const row = document.getElementById('brokerRow');
    const viewport = document.getElementById('brokerViewport');
    if (!row || !viewport) {
        return;
    }

    const originals = Array.from(row.children);
    let timer = null;
    let paused = false;

    function stop() {
        if (timer !== null) {
            window.clearInterval(timer);
            timer = null;
        }
    }

    function start() {
        stop();

        // Put the row back as it came from the page before measuring.
        Array.from(row.children).forEach(function (child) {
            if (child.dataset.clone) { child.remove(); }
        });
        row.style.transition = 'none';
        row.style.transform = 'translateX(0)';

        if (originals.length < 2) { return; }

        // One slide = card width + the gap between cards.
        const step = originals[1].offsetLeft - originals[0].offsetLeft;
        if (!step) { return; }
        const visibleCount = Math.ceil(viewport.clientWidth / step);

        // Everything already fits: nothing to loop.
        if (originals.length <= visibleCount) { return; }

        // Copies of the first cards go on the end so the loop has no visible jump.
        originals.slice(0, visibleCount).forEach(function (card) {
            const clone = card.cloneNode(true);
            clone.dataset.clone = '1';
            clone.classList.add('is-visible');
            row.appendChild(clone);
        });

        const total = originals.length;
        let index = 0;

        function goTo(i, animate) {
            row.style.transition = animate ? 'transform 0.6s ease' : 'none';
            row.style.transform = 'translateX(-' + (i * step) + 'px)';
        }

        timer = window.setInterval(function () {
            // Wait until the section has faded in on screen, and while the mouse is over the row.
            if (paused || !originals[0].classList.contains('is-visible')) { return; }

            // Cards further along have not been "scrolled to" yet, so show them as they slide in.
            originals.forEach(function (card) { card.classList.add('is-visible'); });

            index += 1;
            goTo(index, true);
            if (index >= total) {
                window.setTimeout(function () {
                    index = 0;
                    goTo(0, false);
                }, 620);
            }
        }, 4000);
    }

    viewport.addEventListener('mouseenter', function () { paused = true; });
    viewport.addEventListener('mouseleave', function () { paused = false; });

    let resizeTimer = null;
    window.addEventListener('resize', function () {
        window.clearTimeout(resizeTimer);
        resizeTimer = window.setTimeout(start, 250);
    });

    start();
});
