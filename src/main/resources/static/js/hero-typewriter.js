// Types the hero's cursive word out letter by letter, pauses, erases it,
// then retypes it, looping for continuous movement in the hero, similar to
// the reference site's animated headline.
document.addEventListener('DOMContentLoaded', function () {
    const el = document.getElementById('heroTypedWord');
    if (!el) {
        return;
    }

    const word = el.getAttribute('data-word') || '';
    if (!word) {
        return;
    }

    const typingSpeed = 160;
    const deletingSpeed = 90;
    const pauseAfterType = 1800;
    const pauseAfterDelete = 500;

    let i = 0;

    function type() {
        if (i <= word.length) {
            el.textContent = word.slice(0, i);
            i++;
            window.setTimeout(type, typingSpeed);
        } else {
            window.setTimeout(erase, pauseAfterType);
        }
    }

    function erase() {
        if (i > 0) {
            i--;
            el.textContent = word.slice(0, i);
            window.setTimeout(erase, deletingSpeed);
        } else {
            window.setTimeout(type, pauseAfterDelete);
        }
    }

    type();
});
