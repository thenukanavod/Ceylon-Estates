// Broker photo picker (admin form): choosing an image opens the same
// round crop dialog as the profile picture (drag to move, slider to zoom).
// The cropped square (400x400 JPEG) replaces the chosen file in the
// "photo" input, ready to go up with the rest of the form when the admin
// clicks Save - no change to the server side, it still just receives a file.
(function () {
    var input = document.getElementById('photo');
    var modalEl = document.getElementById('photoCropModal');
    var canvas = document.getElementById('photoCropCanvas');
    if (!input || !modalEl || !canvas) { return; }

    var zoom = document.getElementById('photoCropZoom');
    var saveBtn = document.getElementById('photoCropSave');
    var ctx = canvas.getContext('2d');
    var modal = new bootstrap.Modal(modalEl);
    var OUT = 400;

    var img = null, scale = 1, minScale = 1, offX = 0, offY = 0;
    var dragging = false, lastX = 0, lastY = 0;

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

    // Cancel / close without saving: forget the chosen file so nothing odd gets uploaded
    modalEl.addEventListener('hidden.bs.modal', function () {
        if (!saveBtn.dataset.saved) { input.value = ''; }
        saveBtn.dataset.saved = '';
    });

    // Unlike the profile picture, this form has other fields too - cropping
    // just swaps the file ready for upload, it does not submit the form.
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
            var file = new File([blob], 'broker-photo.jpg', { type: 'image/jpeg' });
            var dt = new DataTransfer();
            dt.items.add(file);
            input.files = dt.files;
            saveBtn.dataset.saved = '1';
            modal.hide();
        }, 'image/jpeg', 0.9);
    });
})();
