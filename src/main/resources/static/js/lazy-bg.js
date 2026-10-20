// Loads the big homepage pictures only when their section is about to scroll into view.
// Pictures that are set as CSS backgrounds (gallery tiles, broker photos, the Local Info and Sell photos)
// are fetched by the browser straight away, even when they are far down the page, and that slowed the
// whole page down. Those elements now carry the address in a data-bg attribute instead; this script
// turns it into a real background shortly before the section becomes visible.
(function () {
    var pending = document.querySelectorAll('[data-bg]');
    if (!pending.length) { return; }

    function loadWithin(root) {
        var targets = root.matches && root.matches('[data-bg]') ? [root] : [];
        root.querySelectorAll('[data-bg]').forEach(function (el) { targets.push(el); });
        targets.forEach(function (el) {
            var url = el.getAttribute('data-bg');
            if (!url) { return; }
            el.removeAttribute('data-bg');
            var base = el.style.backgroundImage;      // e.g. the dark gradient on the gallery tiles
            var image = 'url("' + url + '")';
            el.style.backgroundImage = base && base !== 'none' ? base + ', ' + image : image;
        });
    }

    if (!('IntersectionObserver' in window)) {
        loadWithin(document);
        return;
    }

    var observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting) {
                loadWithin(entry.target);
                observer.unobserve(entry.target);
            }
        });
    }, { rootMargin: '700px 0px' });

    // Watch the whole section a picture belongs to, so a section's pictures load together.
    var watched = new Set();
    pending.forEach(function (el) {
        var holder = el.closest('section') || el;
        if (!watched.has(holder)) {
            watched.add(holder);
            observer.observe(holder);
        }
    });
})();
