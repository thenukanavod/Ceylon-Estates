// Completed Projects page: filter the grid by property type using the chips.
(function () {
    var chips = document.querySelectorAll('#projectChips .chip');
    var cards = document.querySelectorAll('#projectGrid [data-type]');
    chips.forEach(function (chip) {
        chip.addEventListener('click', function () {
            chips.forEach(function (c) { c.classList.remove('active'); });
            chip.classList.add('active');
            var f = chip.getAttribute('data-filter');
            cards.forEach(function (card) {
                card.style.display = (f === 'all' || card.getAttribute('data-type') === f) ? '' : 'none';
            });
        });
    });
})();
