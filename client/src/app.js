const form = document.getElementById('digest-form');
const urlInput = document.getElementById('youtubeUrl');
const statusBox = document.getElementById('status');
const resultBox = document.getElementById('result');
const summaryEl = document.getElementById('summary');
const tagsEl = document.getElementById('tags');
const reqidEl = document.getElementById('reqid');
const linkEl = document.getElementById('videoLink');
const resetBtn = document.getElementById('reset');

let pollTimer = null;

function setStatus(text) {
    statusBox.textContent = text;
    statusBox.classList.remove('hidden');
}
function clearStatus() {
    statusBox.classList.add('hidden');
    statusBox.textContent = '';
}

function renderResult(doc) {
    summaryEl.textContent = doc.summary || '(no summary)';
    tagsEl.innerHTML = '';
    (doc.tags || []).forEach(t => {
        const span = document.createElement('span');
        span.className = 'tag';
        span.textContent = t;
        tagsEl.appendChild(span);
    });
    reqidEl.textContent = doc.requestId;
    linkEl.href = doc.youtubeUrl;
    resultBox.classList.remove('hidden');
}

async function postIngestion(youtubeUrl) {
    const res = await fetch('/api/ingest', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ youtubeUrl })
    });
    if (!res.ok) throw new Error(`Ingestion failed: HTTP ${res.status}`);
    return res.json();
}

async function fetchByRequest(requestId) {
    const res = await fetch(`/api/result?requestId=${encodeURIComponent(requestId)}`);
    if (res.status === 404) return null;
    if (!res.ok) throw new Error(`Fetch failed: HTTP ${res.status}`);
    return res.json();
}

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const youtubeUrl = urlInput.value.trim();
    if (!youtubeUrl) return;

    resultBox.classList.add('hidden');
    clearStatus();
    form.querySelector('button').disabled = true;
    setStatus('Submitting…');

    try {
        const { requestId } = await postIngestion(youtubeUrl);
        setStatus(`Accepted. requestId=${requestId}. Polling for result…`);
        let attempts = 0;

        setTimeout(() => {
            pollTimer = setInterval(async () => {
                attempts++;
                try {
                    const doc = await fetchByRequest(requestId);
                    if (doc) {
                        clearInterval(pollTimer);
                        clearStatus();
                        renderResult(doc);
                        form.querySelector('button').disabled = false;
                    } else {
                        setStatus(`Waiting… (attempt ${attempts})`);
                    }
                } catch (err) {
                    clearInterval(pollTimer);
                    form.querySelector('button').disabled = false;
                    setStatus(`Error while polling: ${err.message}`);
                }
                if (attempts >= 120) {
                    clearInterval(pollTimer);
                    form.querySelector('button').disabled = false;
                    setStatus('Timed out waiting for result. Please try again later.');
                }
            }, 5000);
        }, 10000);
    } catch (err) {
        form.querySelector('button').disabled = false;
        setStatus(err.message);
    }
});

resetBtn.addEventListener('click', () => {
    resultBox.classList.add('hidden');
    clearStatus();
    urlInput.value = '';
    urlInput.focus();
    if (pollTimer) clearInterval(pollTimer);
});
