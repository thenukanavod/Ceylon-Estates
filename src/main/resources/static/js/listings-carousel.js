// "Latest Listings" carousel with category filtering.
// Each listing card carries data-type="land|house|apartment". Clicking a tab
// shows only the matching cards; the row auto-rotates (4 visible at a time)
// only when there are enough matching cards to make that worthwhile.
document.addEventListener('DOMContentLoaded', function () {
    const track = document.getElementById('listingTrack');
    const tabs = document.querySelectorAll('.listing-tab');
    if (!track) {
        return;
    }

    const visibleCount = 4;

    // The "Explore more" button always points at the page for the selected tab
    // (Lands -> /lands, Houses -> /houses, Apartments -> /apartments).
    const exploreMore = document.getElementById('listingExploreMore');

    function updateExploreLink(tab) {
        if (exploreMore && tab && tab.dataset.href) {
            exploreMore.setAttribute('href', tab.dataset.href);
        }
    }

    // Keep the full set of listings in memory, detached from the track, so we
    // can freely rebuild the track's contents for whichever category is active.
    const allCards = Array.from(track.children);
    track.innerHTML = '';

    let rotateTimer = null;

    function stopRotating() {
        if (rotateTimer !== null) {
            window.clearInterval(rotateTimer);
            rotateTimer = null;
        }
    }

    function render(filterType) {
        stopRotating();
        track.style.transition = 'none';
        track.style.transform = 'translateX(0%)';
        track.innerHTML = '';

        const matches = allCards.filter(function (card) {
            return card.dataset.type === filterType;
        });

        matches.forEach(function (card) {
            const clone = card.cloneNode(true);
            clone.classList.remove('is-visible');
            track.appendChild(clone);
            if (window.ScrollReveal) {
                window.ScrollReveal.observe(clone);
            }
        });

        // Only loop/auto-rotate when there are more matching listings than
        // visible slots, otherwise everything already fits, so just show it.
        if (matches.length <= visibleCount) {
            return;
        }

        matches.slice(0, visibleCount - 1).forEach(function (card) {
            const clone = card.cloneNode(true);
            // These wrap-around clones are duplicates used purely for the
            // seamless loop, show them immediately rather than waiting for
            // a scroll trigger that may never fire for an off-screen clone.
            clone.classList.add('is-visible');
            track.appendChild(clone);
        });

        const totalOriginal = matches.length;
        let index = 0;

        function goToIndex(newIndex, animate) {
            track.style.transition = animate ? 'transform 0.6s ease' : 'none';
            track.style.transform = 'translateX(-' + (newIndex * (100 / visibleCount)) + '%)';
        }

        function advance() {
            index += 1;
            goToIndex(index, true);

            if (index >= totalOriginal) {
                window.setTimeout(function () {
                    index = 0;
                    goToIndex(index, false);
                }, 620);
            }
        }

        rotateTimer = window.setInterval(advance, 4000);
    }

    tabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            tabs.forEach(function (t) {
                t.classList.remove('active');
            });
            tab.classList.add('active');
            render(tab.dataset.filter);
            updateExploreLink(tab);
        });
    });

    // Start on whichever tab is marked active in the HTML (Lands, by default).
    const initialTab = document.querySelector('.listing-tab.active') || tabs[0];
    if (initialTab) {
        render(initialTab.dataset.filter);
        updateExploreLink(initialTab);
    }
});
