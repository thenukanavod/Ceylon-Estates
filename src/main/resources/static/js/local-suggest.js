// Search-as-you-type suggestions for the "Get Local Info" box and the main search bar.
// Any input with class .local-info-search-input or a data-suggest-url attribute gets a dropdown that is filled
// from /local-info/suggest as soon as the first letter is typed.
(function () {
    document.querySelectorAll('.local-info-search-input, [data-suggest-url]').forEach(function (el) {
        if (!el.dataset.suggestReady) { el.dataset.suggestReady = '1'; setup(el); }
    });

    function setup(input) {
        var form = input.form;
        if (!form) { return; }
        var url = input.getAttribute('data-suggest-url') || (form.action.replace(/\/+$/, '') + '/suggest');
        var list = document.createElement('ul');
        list.className = 'suggest-list';
        list.setAttribute('role', 'listbox');
        list.hidden = true;
        // The navbar search keeps its input outside the (empty) form, so the list
        // is attached to the search wrapper there; elsewhere it goes inside the form.
        var host = input.closest('.nav-search-wrap') || form;
        if (!host.style.position && getComputedStyle(host).position === 'static') {
            host.style.position = 'relative';
        }
        host.appendChild(list);

        var items = [], active = -1, timer = null, seq = 0;

        function hide() { list.hidden = true; active = -1; }

        function render(data) {
            items = data;
            list.innerHTML = '';
            if (!data.length) { hide(); return; }
            data.forEach(function (d, i) {
                var li = document.createElement('li');
                li.setAttribute('role', 'option');
                var name = document.createElement('span');
                name.className = 'suggest-name';
                name.textContent = d.text;
                var hint = document.createElement('span');
                hint.className = 'suggest-hint';
                hint.textContent = d.hint;
                li.appendChild(name);
                li.appendChild(hint);
                li.addEventListener('mousedown', function (e) {
                    e.preventDefault();
                    choose(i);
                });
                list.appendChild(li);
            });
            active = -1;
            list.hidden = false;
        }

        function choose(i) {
            input.value = items[i].text;
            hide();
            form.submit();
        }

        function highlight() {
            Array.prototype.forEach.call(list.children, function (li, i) {
                li.classList.toggle('active', i === active);
            });
        }

        input.addEventListener('input', function () {
            var q = input.value.trim();
            clearTimeout(timer);
            if (!q) { hide(); return; }
            timer = setTimeout(function () {
                var mine = ++seq;
                fetch(url + '?q=' + encodeURIComponent(q), { headers: { 'Accept': 'application/json' } })
                    .then(function (r) { return r.ok ? r.json() : []; })
                    .then(function (data) { if (mine === seq) { render(data); } })
                    .catch(function () { hide(); });
            }, 120);
        });

        input.addEventListener('keydown', function (e) {
            if (list.hidden || !items.length) { return; }
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                active = (active + 1) % items.length;
                highlight();
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                active = (active - 1 + items.length) % items.length;
                highlight();
            } else if (e.key === 'Enter' && active >= 0) {
                e.preventDefault();
                choose(active);
            } else if (e.key === 'Escape') {
                hide();
            }
        });

        input.addEventListener('blur', function () { setTimeout(hide, 100); });
        input.addEventListener('focus', function () { if (items.length && input.value.trim()) { list.hidden = false; } });
    }
})();
