// Listing photo picker: choosing one or more photos opens the same
// drag-to-move / zoom-slider crop dialog used for the profile picture and
// broker photo, one photo at a time, cropped to a 4:3 rectangle (matching
// the listing card thumbnail shape) instead of a circle.
//
// Crops accumulate across picks (choosing more photos later adds to what
// was already cropped, it doesn't replace it) and are written back into the
// "images" file input as plain JPEGs, so the server side needs no changes -
// it still just receives a normal multipart file list.
(function () {
    var input = document.getElementById('imagesInput');
    var modalEl = document.getElementById('listingPhotoCropModal');
    var canvas = document.getElementById('listingPhotoCropCanvas');
    var previewEl = document.getElementById('imagesPreview');
    if (!input || !modalEl || !canvas || !previewEl) { return; }

    var zoom = document.getElementById('listingPhotoCropZoom');
    var saveBtn = document.getElementById('listingPhotoCropSave');
    var skipBtn = document.getElementById('listingPhotoCropSkip');
    var counter = document.getElementById('listingPhotoCropCounter');
    var ctx = canvas.getContext('2d');
    var modal = new bootstrap.Modal(modalEl);
    var OUT_W = 1024, OUT_H = 768;

    var allCropped = [];   // File[] - every photo cropped so far, this visit
    var queue = [];        // File[] - the batch just picked, waiting to be cropped
    var qIndex = 0;
    var img = null, scale = 1, minScale = 1, offX = 0, offY = 0;
    var dragging = false, lastX = 0, lastY = 0;

    input.addEventListener('change', function () {
        var files = Array.prototype.slice.call(input.files || []);
        if (!files.length) { return; }
        queue = files;
        qIndex = 0;
        loadNext();
    });

    function loadNext() {
        if (qIndex >= queue.length) {
            queue = [];
            modal.hide();
            commit();
            return;
        }
        counter.textContent = 'Photo ' + (qIndex + 1) + ' of ' + queue.length;
        var file = queue[qIndex];
        var url = URL.createObjectURL(file);
        var image = new Image();
        image.onload = function () {
            img = image;
            minScale = Math.max(canvas.width / img.width, canvas.height / img.height);
            zoom.value = 1;
            scale = minScale;
            offX = (canvas.width - img.width * scale) / 2;
            offY = (canvas.height - img.height * scale) / 2;
            draw();
            modal.show();
        };
        image.onerror = function () { qIndex++; loadNext(); }; // unreadable file: skip it
        image.src = url;
    }

    function clamp() {
        var w = img.width * scale, h = img.height * scale;
        offX = Math.min(0, Math.max(canvas.width - w, offX));
        offY = Math.min(0, Math.max(canvas.height - h, offY));
    }

    function draw() {
        if (!img) { return; }
        clamp();
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.drawImage(img, offX, offY, img.width * scale, img.height * scale);
    }

    zoom.addEventListener('input', function () {
        var cx = canvas.width / 2, cy = canvas.height / 2;
        var ix = (cx - offX) / scale, iy = (cy - offY) / scale;
        scale = minScale * parseFloat(zoom.value);
        offX = cx - ix * scale;
        offY = cy - iy * scale;
        draw();
    });

    function ratio() { return canvas.width / canvas.getBoundingClientRect().width; }

    canvas.addEventListener('pointerdown', function (e) {
        dragging = true; lastX = e.clientX; lastY = e.clientY;
        canvas.setPointerCapture(e.pointerId);
        canvas.style.cursor = 'grabbing';
    });
    canvas.addEventListener('pointermove', function (e) {
        if (!dragging) { return; }
        var r = ratio();
        offX += (e.clientX - lastX) * r;
        offY += (e.clientY - lastY) * r;
        lastX = e.clientX; lastY = e.clientY;
        draw();
    });
    function stopDrag() { dragging = false; canvas.style.cursor = 'grab'; }
    canvas.addEventListener('pointerup', stopDrag);
    canvas.addEventListener('pointercancel', stopDrag);

    skipBtn.addEventListener('click', function () {
        qIndex++;
        loadNext();
    });

    saveBtn.addEventListener('click', function () {
        if (!img) { return; }
        var out = document.createElement('canvas');
        out.width = OUT_W; out.height = OUT_H;
        var k = OUT_W / canvas.width;
        var octx = out.getContext('2d');
        octx.fillStyle = '#fff';
        octx.fillRect(0, 0, OUT_W, OUT_H);
        octx.drawImage(img, offX * k, offY * k, img.width * scale * k, img.height * scale * k);
        out.toBlob(function (blob) {
            if (!blob) { return; }
            allCropped.push(new File([blob], 'listing-photo-' + Date.now() + '-' + qIndex + '.jpg', { type: 'image/jpeg' }));
            qIndex++;
            loadNext();
        }, 'image/jpeg', 0.88);
    });

    // Closing the dialog early (X / Esc) stops the rest of the batch, but
    // keeps whatever was already cropped and saved.
    modalEl.addEventListener('hidden.bs.modal', function () {
        queue = [];
        commit();
    });

    function commit() {
        var dt = new DataTransfer();
        allCropped.forEach(function (f) { dt.items.add(f); });
        input.files = dt.files;
        renderPreview();
    }

    function renderPreview() {
        previewEl.innerHTML = '';
        allCropped.forEach(function (file, i) {
            var url = URL.createObjectURL(file);
            var wrap = document.createElement('div');
            wrap.className = 'photo-edit';
            var thumb = document.createElement('div');
            thumb.className = 'photo-thumb';
            thumb.style.backgroundImage = 'url(' + url + ')';
            wrap.appendChild(thumb);
            var rm = document.createElement('button');
            rm.type = 'button';
            rm.className = 'btn btn-sm btn-outline-danger mt-1 w-100';
            rm.textContent = 'Remove';
            rm.addEventListener('click', function () {
                allCropped.splice(i, 1);
                commit();
            });
            wrap.appendChild(rm);
            previewEl.appendChild(wrap);
        });
    }
})();
