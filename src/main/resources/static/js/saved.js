/*
 * Heart buttons on listing cards: save / unsave without reloading the page,
 * so the visitor stays exactly where they are (same scroll position, same carousel slide).
 * If the visitor is not signed in the server redirects to the login page, and we follow it.
 * On the Saved page itself the normal form post is kept, so a removed card disappears.
 */
document.addEventListener('submit', async function (event) {
    var form = event.target;
    if (!form.classList || !form.classList.contains('heart-form')) {
        return;
    }
    if (/\/saved\/?$/.test(window.location.pathname)) {
        return; // normal submit on the Saved page
    }
    event.preventDefault();

    var button = form.querySelector('.heart-btn');
    var icon = form.querySelector('i');
    button.disabled = true;
    try {
        var response = await fetch(form.action, {
            method: 'POST',
            body: new FormData(form), // includes the CSRF token
            headers: { 'X-Requested-With': 'fetch' },
            credentials: 'same-origin'
        });
        if (response.redirected && /\/login/.test(response.url)) {
            window.location.href = response.url; // not signed in
            return;
        }
        if (!response.ok) {
            throw new Error('save failed');
        }
        var saved = (await response.text()).trim() === 'saved';
        button.classList.toggle('on', saved);
        icon.className = saved ? 'bi bi-heart-fill' : 'bi bi-heart';
    } catch (error) {
        form.submit(); // fall back to the normal page reload
    } finally {
        button.disabled = false;
    }
});
