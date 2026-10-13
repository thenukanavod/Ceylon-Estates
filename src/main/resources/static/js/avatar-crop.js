// Profile picture picker: choosing an image opens a round crop dialog
// (drag to move, slider to zoom). The cropped square (400x400 JPEG) replaces
// the file in the form and is uploaded to /profile/avatar as before.
(function () {
    var input = document.getElementById('avatarInput');
    var pickBtn = document.getElementById('avatarPickBtn');
    var modalEl = document.getElementById('avatarCropModal');
    var canvas = document.getElementById('avatarCropCanvas');
    if (!input || !pickBtn || !modalEl || !canvas) { return; }

    var form = input.form;
    var zoom = document.getElementById('avatarCropZoom');
    var saveBtn = document.getElementById('avatarCropSave');
    var ctx = canvas.getContext('2d');
    var modal = new bootstrap.Modal(modalEl);
    var OUT = 400;

    var img = null, scale = 1, minScale = 1, offX = 0, offY = 0;
    var dragging = false, lastX = 0, lastY = 0;

    pickBtn.addEventListener('click', function () { input.click(); });

    input.addEventListener('change', function () {
        var file = input.files && input.files[0];
        if (!file) { return; }
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
        image.onerror = function () { input.value = ''; };
        image.src = url;
    });

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
        // zoom around the centre of the frame
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
    function stop() { dragging = false; canvas.style.cursor = 'grab'; }
    canvas.addEventListener('pointerup', stop);
    canvas.addEventListener('pointercancel', stop);

    // Cancel / close: forget the chosen file so nothing is uploaded
    modalEl.addEventListener('hidden.bs.modal', function () {
        if (!saveBtn.dataset.saving) { input.value = ''; }
    });

    saveBtn.addEventListener('click', function () {
        if (!img) { return; }
        var out = document.createElement('canvas');
        out.width = OUT; out.height = OUT;
        var k = OUT / canvas.width;
        var octx = out.getContext('2d');
        octx.fillStyle = '#fff';
        octx.fillRect(0, 0, OUT, OUT);
        octx.drawImage(img, offX * k, offY * k, img.width * scale * k, img.height * scale * k);
        out.toBlob(function (blob) {
            if (!blob) { return; }
            var file = new File([blob], 'avatar.jpg', { type: 'image/jpeg' });
            var dt = new DataTransfer();
            dt.items.add(file);
            input.files = dt.files;
            saveBtn.dataset.saving = '1';
            form.submit();
        }, 'image/jpeg', 0.9);
    });
})();
