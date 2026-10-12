// Property detail page: clicking a thumbnail swaps the main photo.
(function () {
    var main = document.getElementById('galleryMain');
    if (!main) return;
    document.querySelectorAll('.gallery-thumb').forEach(function (t) {
        t.addEventListener('click', function () {
            main.style.backgroundImage = t.getAttribute('data-bg');
            document.querySelectorAll('.gallery-thumb').forEach(function (x) { x.classList.remove('active'); });
            t.classList.add('active');
        });
    });
})();
