// Settings page, two-factor card:
//  - the Turn on / Turn off button asks before turning 2FA off
//  - the freshly made backup codes can be downloaded as a .txt file
//    (they only exist in the page right now: the server keeps just hashes)
(function () {
    var form = document.getElementById('twoFaToggleForm');
    if (form) {
        form.addEventListener('submit', function (e) {
            // Only turning it OFF needs confirming (there is no hidden "on" field then).
            var turningOn = form.querySelector('input[name="twoFaEnabled"]');
            if (!turningOn && !confirm('Turn off two-factor authentication? Your backup codes will be deleted.')) {
                e.preventDefault();
            }
        });
    }

    var btn = document.getElementById('downloadCodesBtn');
    var box = document.getElementById('backupCodesBox');
    if (btn && box) {
        btn.addEventListener('click', function () {
            var codes = Array.prototype.map.call(box.querySelectorAll('.backup-code'), function (el) {
                return el.textContent.trim();
            });
            var lines = [
                'Ceylon Estates - two-factor backup codes',
                'Account: ' + (box.getAttribute('data-user') || ''),
                'Created: ' + new Date().toLocaleString(),
                '',
                'Each code works ONCE. Use one on the login verification page',
                'if the emailed code does not arrive.',
                'Keep this file somewhere safe and private.',
                ''
            ];
            codes.forEach(function (c, i) { lines.push((i + 1) + '. ' + c); });
            lines.push('');
            var blob = new Blob([lines.join('\r\n')], { type: 'text/plain' });
            var url = URL.createObjectURL(blob);
            var a = document.createElement('a');
            a.href = url;
            a.download = 'ceylon-estates-backup-codes.txt';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
        });
    }
})();
