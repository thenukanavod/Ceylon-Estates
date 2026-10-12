// Hero advanced search widget: tabs, single-select chip groups, the
// dual-handle price slider, and the live city filter. Purely client-side
// for now — there's no backend search endpoint yet, so this wires up the
// interactions (and the Search button jumps to the Latest Listings
// section) without pretending to run a real query.
(function () {
    var card = document.querySelector('.hero-search-card');
    if (!card) {
        return;
    }

    // --- Buy / Sell tabs ---------------------------------------------
    // "Sell" is a real link to the Sell section (<a href="#sell">), not a
    // search mode, but it still gets the same active-tab visual feedback
    // as "Buy" on click.
    var tabs = card.querySelectorAll('.hero-tab');
    tabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            tabs.forEach(function (t) { t.classList.remove('active'); });
            tab.classList.add('active');
        });
    });

    // --- Sliders button: toggles the filter row, doesn't always show it ---
    var filtersToggleBtn = document.getElementById('heroFiltersToggleBtn');
    if (filtersToggleBtn) {
        filtersToggleBtn.addEventListener('click', function () {
            var isOpen = card.classList.toggle('filters-open');
            filtersToggleBtn.classList.toggle('active', isOpen);
            filtersToggleBtn.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
        });
    }

    // --- Property type list (single-select) -------------------------
    var typeItems = card.querySelectorAll('.hero-proptype-item');
    typeItems.forEach(function (item) {
        item.addEventListener('click', function () {
            typeItems.forEach(function (i) { i.classList.remove('active'); });
            item.classList.add('active');
            var toggleEl = item.closest('.dropdown').querySelector('.dropdown-toggle');
            if (toggleEl && window.bootstrap) {
                var instance = window.bootstrap.Dropdown.getInstance(toggleEl);
                if (instance) {
                    instance.hide();
                }
            }
        });
    });

    // --- Chip groups (beds, baths, radius) — single-select per group ---
    var chipGroups = {};
    card.querySelectorAll('[data-hero-chip-group]').forEach(function (group) {
        var name = group.getAttribute('data-hero-chip-group');
        chipGroups[name] = group.querySelectorAll('.hero-chip');
    });
    Object.keys(chipGroups).forEach(function (name) {
        var chips = chipGroups[name];
        chips.forEach(function (chip) {
            chip.addEventListener('click', function () {
                var alreadyActive = chip.classList.contains('active');
                chips.forEach(function (c) { c.classList.remove('active'); });
                if (!alreadyActive) {
                    chip.classList.add('active');
                }
            });
        });
    });

    // --- Price range (dual-handle slider) ---------------------------
    var minInput = document.getElementById('heroPriceMin');
    var maxInput = document.getElementById('heroPriceMax');
    var fill = document.getElementById('heroPriceFill');
    var minLabel = document.getElementById('heroPriceMinLabel');
    var maxLabel = document.getElementById('heroPriceMaxLabel');

    function formatPrice(valueInMillions) {
        if (valueInMillions >= 1000) {
            return 'Rs ' + (valueInMillions / 1000).toFixed(1) + ' Bn';
        }
        return 'Rs ' + valueInMillions + 'M';
    }

    function updatePriceSlider() {
        if (!minInput || !maxInput) {
            return;
        }
        var min = parseInt(minInput.value, 10);
        var max = parseInt(maxInput.value, 10);
        if (min > max) {
            // Keep the two handles from crossing over each other.
            var tmp = min;
            min = max;
            max = tmp;
        }
        var range = parseInt(minInput.max, 10) - parseInt(minInput.min, 10);
        var leftPct = ((min - minInput.min) / range) * 100;
        var rightPct = ((max - minInput.min) / range) * 100;
        if (fill) {
            fill.style.left = leftPct + '%';
            fill.style.width = (rightPct - leftPct) + '%';
        }
        if (minLabel) {
            minLabel.textContent = formatPrice(min);
        }
        if (maxLabel) {
            maxLabel.textContent = formatPrice(max);
        }
    }

    if (minInput && maxInput) {
        minInput.addEventListener('input', function () {
            if (parseInt(minInput.value, 10) > parseInt(maxInput.value, 10)) {
                minInput.value = maxInput.value;
            }
            updatePriceSlider();
        });
        maxInput.addEventListener('input', function () {
            if (parseInt(maxInput.value, 10) < parseInt(minInput.value, 10)) {
                maxInput.value = minInput.value;
            }
            updatePriceSlider();
        });
        updatePriceSlider();
    }

    // --- Location: live city filter + click-to-fill -----------------
    var citySearchInput = document.getElementById('heroCitySearch');
    var cityList = document.getElementById('heroCityList');
    var mainSearchInput = document.getElementById('heroSearchInput');

    if (citySearchInput && cityList) {
        var cityItems = cityList.querySelectorAll('.hero-city-item');
        citySearchInput.addEventListener('input', function () {
            var query = citySearchInput.value.trim().toLowerCase();
            cityItems.forEach(function (item) {
                var name = (item.getAttribute('data-name') || '').toLowerCase();
                var matches = name.indexOf(query) !== -1;
                item.classList.toggle('hero-item-hidden', !matches);
            });
        });

        cityItems.forEach(function (item) {
            item.addEventListener('click', function () {
                var name = item.getAttribute('data-name');
                if (mainSearchInput && name) {
                    mainSearchInput.value = name;
                }
                var dropdownToggleEl = item.closest('.dropdown').querySelector('.dropdown-toggle');
                if (dropdownToggleEl && window.bootstrap) {
                    var instance = window.bootstrap.Dropdown.getInstance(dropdownToggleEl);
                    if (instance) {
                        instance.hide();
                    }
                }
            });
        });
    }
})();
