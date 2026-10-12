// Sell / edit listing form: show only the fields that fit the chosen property type,
// switch the price label (per perch / per month / total) and make land sale-only.
(function () {
    var form = document.getElementById('sellForm');
    if (!form) return;

    var typeRadios = form.querySelectorAll('input[type="radio"][name="type"]');
    var hiddenType = form.querySelector('input[type="hidden"][name="type"]');
    var saleRadio = document.getElementById('sale');
    var rentRadio = document.getElementById('rent');
    var priceInput = document.getElementById('price');
    var priceLabel = document.getElementById('priceLabel');
    var landNote = document.getElementById('landNote');

    function currentType() {
        if (hiddenType) return hiddenType.value;
        var checked = form.querySelector('input[type="radio"][name="type"]:checked');
        return checked ? checked.value : 'house';
    }

    function refresh() {
        var type = currentType();

        form.querySelectorAll('.type-fields').forEach(function (section) {
            var show = section.getAttribute('data-for').split(' ').indexOf(type) !== -1;
            section.style.display = show ? '' : 'none';
            // Hidden sections must not block the form or send stale values.
            section.querySelectorAll('input, select, textarea').forEach(function (el) { el.disabled = !show; });
        });

        if (type === 'land') {
            saleRadio.checked = true;
            rentRadio.disabled = true;
            landNote.style.display = '';
            priceLabel.textContent = priceInput.getAttribute('data-label-land');
        } else {
            rentRadio.disabled = false;
            landNote.style.display = 'none';
            priceLabel.textContent = rentRadio.checked
                ? priceInput.getAttribute('data-label-rent')
                : priceInput.getAttribute('data-label-sale');
        }
    }

    typeRadios.forEach(function (r) { r.addEventListener('change', refresh); });
    saleRadio.addEventListener('change', refresh);
    rentRadio.addEventListener('change', refresh);
    refresh();
})();
