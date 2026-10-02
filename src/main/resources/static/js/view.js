// "Copy for Apple Notes" on the viewer page.
//
// The service usually runs over plain http on the local network, where the async Clipboard API is not
// available (it needs a secure context). So: use the Clipboard API when we can, otherwise select the
// rendered document and copy the selection, which Safari puts on the clipboard as rich text with images.
(function () {
    const button = document.getElementById('copy-notes');
    const article = document.getElementById('document');
    const toast = document.getElementById('toast');

    function show(message) {
        toast.textContent = message;
        toast.hidden = false;
        clearTimeout(show.timer);
        show.timer = setTimeout(() => { toast.hidden = true; }, 3500);
    }

    async function copyWithClipboardApi() {
        const html = fetch(button.dataset.notesUrl).then(r => r.blob()).then(b => new Blob([b], {type: 'text/html'}));
        const text = new Blob([article.innerText], {type: 'text/plain'});
        await navigator.clipboard.write([new ClipboardItem({'text/html': html, 'text/plain': text})]);
    }

    function copyWithSelection() {
        const selection = window.getSelection();
        const range = document.createRange();
        range.selectNodeContents(article);
        selection.removeAllRanges();
        selection.addRange(range);
        const copied = document.execCommand('copy');
        selection.removeAllRanges();
        return copied;
    }

    button.addEventListener('click', async () => {
        try {
            if (window.isSecureContext && navigator.clipboard && window.ClipboardItem) {
                await copyWithClipboardApi();
            } else if (!copyWithSelection()) {
                throw new Error('copy command was rejected');
            }
            show('Copied! Paste it into Apple Notes.');
        } catch (e) {
            show('Could not copy automatically. Select the document and copy it manually.');
        }
    });
})();
