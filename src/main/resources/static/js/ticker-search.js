/*
 * Add-ticker autocomplete.
 *
 * As the user types into the add-ticker box, fetches /api/symbols?q=... (a keyless bundled
 * catalog) and shows a dropdown of symbol/name suggestions. Click or press Enter on a suggestion
 * to fill the input and submit the add form. Arrow keys navigate; Escape closes. No dependencies.
 */
(function () {
    "use strict";

    var form = document.querySelector(".add-ticker");
    if (!form) return;
    var input = form.querySelector('input[name="ticker"]');
    var list = document.getElementById("ticker-suggestions");
    if (!input || !list) return;

    var items = [];       // current suggestion data
    var active = -1;      // highlighted index
    var debounceTimer;

    function close() {
        list.hidden = true;
        list.innerHTML = "";
        items = [];
        active = -1;
        input.setAttribute("aria-expanded", "false");
    }

    function submitWith(symbol) {
        input.value = symbol;
        close();
        if (form.requestSubmit) {
            form.requestSubmit();
        } else {
            form.submit();
        }
    }

    function render() {
        list.innerHTML = "";
        items.forEach(function (m, i) {
            var li = document.createElement("li");
            li.className = "suggestion" + (i === active ? " active" : "");
            li.setAttribute("role", "option");
            li.setAttribute("aria-selected", i === active ? "true" : "false");
            var sym = document.createElement("span");
            sym.className = "s-symbol";
            sym.textContent = m.symbol;
            var name = document.createElement("span");
            name.className = "s-name";
            name.textContent = m.name;
            li.appendChild(sym);
            li.appendChild(name);
            li.addEventListener("mousedown", function (e) {
                // mousedown (not click) so it fires before the input blur closes the list
                e.preventDefault();
                submitWith(m.symbol);
            });
            list.appendChild(li);
        });
        list.hidden = items.length === 0;
        input.setAttribute("aria-expanded", items.length > 0 ? "true" : "false");
    }

    function fetchSuggestions(q) {
        fetch("/api/symbols?q=" + encodeURIComponent(q))
            .then(function (r) { return r.ok ? r.json() : []; })
            .then(function (data) {
                items = data || [];
                active = -1;
                render();
            })
            .catch(close);
    }

    input.setAttribute("role", "combobox");
    input.setAttribute("aria-autocomplete", "list");
    input.setAttribute("aria-expanded", "false");
    input.setAttribute("autocomplete", "off");

    input.addEventListener("input", function () {
        var q = input.value.trim();
        clearTimeout(debounceTimer);
        if (q.length === 0) {
            close();
            return;
        }
        debounceTimer = setTimeout(function () { fetchSuggestions(q); }, 120);
    });

    input.addEventListener("keydown", function (e) {
        if (list.hidden || items.length === 0) return;
        if (e.key === "ArrowDown") {
            e.preventDefault();
            active = (active + 1) % items.length;
            render();
        } else if (e.key === "ArrowUp") {
            e.preventDefault();
            active = (active - 1 + items.length) % items.length;
            render();
        } else if (e.key === "Enter") {
            if (active >= 0 && active < items.length) {
                e.preventDefault();
                submitWith(items[active].symbol);
            }
        } else if (e.key === "Escape") {
            close();
        }
    });

    input.addEventListener("blur", function () {
        // Delay so a mousedown on a suggestion can register first.
        setTimeout(close, 100);
    });
})();
