// Browser audio for the Melova web app: raw PCM capture (scored takes), compressed recording
// (long talks), playback with pitch-preserving speed, and picking a file. Called from Kotlin/Wasm.
(function () {
  const MIC = { audio: { echoCancellation: false, noiseSuppression: false, autoGainControl: false, channelCount: 1 } };

  function micError(error) {
    if (error && (error.name === 'NotAllowedError' || error.name === 'SecurityError')) {
      return new Error('دسترسی میکروفون داده نشده است');
    }
    return new Error('میکروفون در دسترس نیست');
  }

  function wavBytes(chunks, length, sampleRate) {
    const buffer = new ArrayBuffer(44 + length * 2);
    const view = new DataView(buffer);
    const text = (offset, value) => { for (let i = 0; i < value.length; i++) view.setUint8(offset + i, value.charCodeAt(i)); };
    text(0, 'RIFF'); view.setUint32(4, 36 + length * 2, true); text(8, 'WAVE'); text(12, 'fmt ');
    view.setUint32(16, 16, true); view.setUint16(20, 1, true); view.setUint16(22, 1, true);
    view.setUint32(24, sampleRate, true); view.setUint32(28, sampleRate * 2, true);
    view.setUint16(32, 2, true); view.setUint16(34, 16, true); text(36, 'data'); view.setUint32(40, length * 2, true);
    let offset = 44;
    for (const chunk of chunks) {
      for (let i = 0; i < chunk.length; i++) {
        const s = Math.max(-1, Math.min(1, chunk[i]));
        view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7fff, true);
        offset += 2;
      }
    }
    return new Int8Array(buffer);
  }

  /** Mono PCM straight from the microphone, kept in memory and written as WAV at the end. */
  async function startCapture(preferredRate) {
    let stream;
    try { stream = await navigator.mediaDevices.getUserMedia(MIC); } catch (e) { throw micError(e); }
    let context;
    try { context = new AudioContext({ sampleRate: preferredRate }); } catch (e) { context = new AudioContext(); }
    await context.resume();
    const source = context.createMediaStreamSource(stream);
    const processor = context.createScriptProcessor(4096, 1, 1);
    const mute = context.createGain();
    mute.gain.value = 0;
    const chunks = [];
    let length = 0, level = 0, paused = false, error = null, closed = false;
    processor.onaudioprocess = (event) => {
      const input = event.inputBuffer.getChannelData(0);
      let peak = 0;
      for (let i = 0; i < input.length; i++) peak = Math.max(peak, Math.abs(input[i]));
      level = peak;
      if (paused || closed) return;
      chunks.push(new Float32Array(input));
      length += input.length;
    };
    stream.getAudioTracks().forEach((track) => { track.onended = () => { error = 'میکروفون قطع شد'; }; });
    source.connect(processor); processor.connect(mute); mute.connect(context.destination);
    const close = () => {
      if (closed) return;
      closed = true;
      processor.disconnect(); source.disconnect();
      stream.getTracks().forEach((track) => track.stop());
      context.close();
    };
    return {
      elapsedMs: () => length * 1000 / context.sampleRate,
      level: () => level,
      error: () => error,
      pause: () => { paused = true; },
      resume: () => { paused = false; },
      finish: async () => { close(); return { bytes: wavBytes(chunks, length, context.sampleRate), extension: 'wav' }; },
      cancel: () => { close(); chunks.length = 0; },
    };
  }

  /** The browser's own encoder and the file extension it writes; the server accepts m4a, ogg and webm for talk. */
  function compressedType() {
    const types = [['audio/mp4', 'm4a'], ['audio/ogg;codecs=opus', 'ogg'], ['audio/webm;codecs=opus', 'webm'], ['audio/webm', 'webm']];
    return types.find(([type]) => window.MediaRecorder && MediaRecorder.isTypeSupported(type)) || ['', 'webm'];
  }

  async function startCompressed() {
    let stream;
    try { stream = await navigator.mediaDevices.getUserMedia({ audio: true }); } catch (e) { throw micError(e); }
    const [mimeType, extension] = compressedType();
    const recorder = mimeType ? new MediaRecorder(stream, { mimeType, audioBitsPerSecond: 128000 }) : new MediaRecorder(stream);
    const parts = [];
    let error = null;
    recorder.ondataavailable = (event) => { if (event.data.size > 0) parts.push(event.data); };
    recorder.onerror = () => { error = 'ضبط با خطا متوقف شد'; };
    // A meter from the same stream.
    const context = new AudioContext();
    const analyser = context.createAnalyser();
    analyser.fftSize = 1024;
    context.createMediaStreamSource(stream).connect(analyser);
    const samples = new Float32Array(analyser.fftSize);
    let startedAt = performance.now(), pausedAt = 0, pausedTotal = 0;
    recorder.start(1000);
    const close = () => { stream.getTracks().forEach((track) => track.stop()); context.close(); };
    return {
      elapsedMs: () => (pausedAt || performance.now()) - startedAt - pausedTotal,
      level: () => {
        analyser.getFloatTimeDomainData(samples);
        let peak = 0;
        for (let i = 0; i < samples.length; i++) peak = Math.max(peak, Math.abs(samples[i]));
        return pausedAt ? 0 : peak;
      },
      error: () => error,
      pause: () => { if (!pausedAt) { recorder.pause(); pausedAt = performance.now(); } },
      resume: () => { if (pausedAt) { recorder.resume(); pausedTotal += performance.now() - pausedAt; pausedAt = 0; } },
      finish: () => new Promise((resolve) => {
        recorder.onstop = async () => {
          close();
          const blob = new Blob(parts, { type: recorder.mimeType || mimeType });
          resolve({ bytes: new Int8Array(await blob.arrayBuffer()), extension });
        };
        recorder.stop();
      }),
      cancel: () => { recorder.onstop = null; if (recorder.state !== 'inactive') recorder.stop(); close(); },
    };
  }

  function mimeFor(name) {
    const ext = (name.split('.').pop() || '').toLowerCase();
    return { wav: 'audio/wav', m4a: 'audio/mp4', aac: 'audio/mp4', mp3: 'audio/mpeg', ogg: 'audio/ogg', webm: 'audio/webm' }[ext] || '';
  }

  function createPlayer() {
    const audio = new Audio();
    audio.preload = 'auto';
    audio.preservesPitch = true;
    audio.webkitPreservesPitch = true; // Safari before 17
    let url = null, ended = false, onEnded = null, onReady = null;
    audio.addEventListener('ended', () => { if (!audio.loop) { ended = true; if (onEnded) onEnded(); } });
    audio.addEventListener('canplay', () => { const ready = onReady; onReady = null; if (ready) ready(); });
    audio.addEventListener('play', () => { ended = false; });
    const unload = () => { if (url) { URL.revokeObjectURL(url); url = null; } };
    return {
      load: (bytes, name, finished, ready) => {
        unload();
        ended = false; onEnded = finished; onReady = ready;
        url = URL.createObjectURL(new Blob([bytes], { type: mimeFor(name) }));
        audio.loop = false;
        audio.src = url;
        audio.load();
      },
      play: () => { if (ended) audio.currentTime = 0; ended = false; audio.play().catch(() => {}); },
      pause: () => audio.pause(),
      seek: (ms) => { audio.currentTime = Math.max(0, ms / 1000); ended = false; },
      positionMs: () => audio.currentTime * 1000,
      durationMs: () => (isFinite(audio.duration) ? audio.duration * 1000 : 0),
      isPlaying: () => !audio.paused && !audio.ended,
      hasEnded: () => ended,
      isReady: () => audio.readyState >= 2,
      // Each new rate restarts the browser's pitch-keeping stretcher, which cuts the sound,
      // so an unchanged rate is left alone. Loading a file resets playbackRate to
      // defaultPlaybackRate, so set both, or a speed set before the file loads is lost.
      setSpeed: (speed) => {
        const rate = Math.round(speed * 100) / 100;
        audio.defaultPlaybackRate = rate;
        if (Math.abs(audio.playbackRate - rate) > 0.001) audio.playbackRate = rate;
      },
      setLoop: (loop) => { audio.loop = loop; },
      stop: () => { onEnded = null; onReady = null; audio.pause(); audio.removeAttribute('src'); audio.load(); unload(); ended = false; },
    };
  }

  function pickFile(accept) {
    return new Promise((resolve) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = accept;
      input.onchange = async () => {
        const file = input.files && input.files[0];
        if (!file) { resolve(null); return; }
        resolve({ name: file.name, bytes: new Int8Array(await file.arrayBuffer()) });
      };
      input.oncancel = () => resolve(null);
      input.click();
    });
  }

  let googleScript;

  function loadGoogleScript() {
    googleScript = googleScript || new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.onload = resolve;
      script.onerror = () => { googleScript = null; reject(new Error('Google sign-in did not load')); };
      document.head.appendChild(script);
    });
    return googleScript;
  }

  // Google only lets its own button start a sign-in, and the app is one canvas, so the button
  // sits in a small dialog over it.
  async function googleSignIn(clientId) {
    await loadGoogleScript();
    return new Promise((resolve) => {
      const overlay = document.createElement('div');
      overlay.style.cssText = 'position:fixed;inset:0;z-index:20;display:flex;align-items:center;justify-content:center;background:rgba(0,0,0,.55)';
      const box = document.createElement('div');
      box.style.cssText = 'display:flex;flex-direction:column;align-items:center;gap:16px;padding:24px;border-radius:24px;background:#fff;font-family:sans-serif';
      const button = document.createElement('div');
      const cancel = document.createElement('button');
      cancel.textContent = 'انصراف';
      cancel.style.cssText = 'border:0;background:none;color:#666;font:inherit;padding:8px 16px;cursor:pointer';
      const finish = (answer) => { overlay.remove(); resolve(answer); };
      cancel.onclick = () => finish({ credential: null, cancelled: true });
      box.append(button, cancel);
      overlay.append(box);
      document.body.append(overlay);
      google.accounts.id.initialize({
        client_id: clientId,
        callback: (response) => finish({ credential: response.credential, cancelled: false }),
      });
      google.accounts.id.renderButton(button, { theme: 'outline', size: 'large', text: 'continue_with', locale: 'fa' });
    });
  }

  // Android shows a Chrome tab with Chrome's logo in the recent apps, whatever the page does; only
  // an installed app gets its own. Chrome offers that through this event alone, and it can come
  // before the app is up.
  let installOffer = null, onInstallChange = null;

  function setInstallOffer(offer) {
    installOffer = offer;
    if (onInstallChange) onInstallChange();
  }

  window.addEventListener('beforeinstallprompt', setInstallOffer);
  window.addEventListener('appinstalled', () => setInstallOffer(null));

  // An offer works once; Chrome makes a new one if this one is turned down.
  function install() {
    const offer = installOffer;
    setInstallOffer(null);
    if (offer) offer.prompt();
  }

  // Chat notifications. On iOS a browser tab can't have them, only the app added to the home screen.
  function needsInstall() {
    const isIos = /iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
    return isIos && !navigator.standalone;
  }

  function canPush() {
    return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;
  }

  async function pushSubscription() {
    if (!canPush()) return null;
    const registration = await navigator.serviceWorker.ready;
    return registration.pushManager.getSubscription();
  }

  function addressOf(subscription) {
    if (!subscription) return null;
    const { endpoint, keys } = subscription.toJSON();
    return JSON.stringify({ endpoint, p256dh: keys.p256dh, auth: keys.auth });
  }

  async function pushState() {
    if (needsInstall()) return 'needs-install';
    if (!canPush()) return 'unsupported';
    if (Notification.permission === 'denied') return 'blocked';
    const subscription = Notification.permission === 'granted' ? await pushSubscription() : null;
    return subscription ? 'on' : 'off';
  }

  async function pushSubscribe(key) {
    const text = atob(key.replace(/-/g, '+').replace(/_/g, '/').padEnd(key.length + (4 - key.length % 4) % 4, '='));
    const applicationServerKey = Uint8Array.from(text, (char) => char.charCodeAt(0));
    const registration = await navigator.serviceWorker.ready;
    return addressOf(await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey }));
  }

  async function pushUnsubscribe() {
    const subscription = await pushSubscription();
    if (subscription) await subscription.unsubscribe();
    return addressOf(subscription);
  }

  async function askNotifications() {
    return (await Notification.requestPermission()) === 'granted';
  }

  function onFirstTap(tapped) {
    const events = ['pointerup', 'touchend'];
    const once = () => {
      events.forEach((name) => document.removeEventListener(name, once, true));
      tapped();
    };
    events.forEach((name) => document.addEventListener(name, once, true));
  }

  // A tapped notification reaches an open page as a message, and a new page in its address.
  function watchChatLinks(opened) {
    const address = new URL(location.href);
    const chat = address.searchParams.get('chat');
    if (chat) {
      address.searchParams.delete('chat');
      history.replaceState(null, '', address);
      opened(chat);
    }
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.addEventListener('message', (event) => { if (event.data && event.data.chat) opened(event.data.chat); });
    }
  }

  // One extra history entry catches the browser's back, so it never leaves the app from an
  // inner page. The app has no addresses of its own, so one entry is enough for any depth.
  let backHeld = Boolean(history.state && history.state.melovaBack);
  let backWanted = false;
  let backDropping = false;

  function holdBack(wanted) {
    backWanted = wanted;
    if (backDropping) return;
    if (wanted && !backHeld) {
      history.pushState({ melovaBack: true }, '');
      backHeld = true;
    } else if (!wanted && backHeld) {
      backDropping = true;
      history.back();
    }
  }

  function watchBack(pressed) {
    window.addEventListener('popstate', (event) => {
      backHeld = Boolean(event.state && event.state.melovaBack);
      if (backHeld) return; // The forward button stepped onto the entry again.
      if (backDropping) {
        backDropping = false;
        holdBack(backWanted);
      } else {
        pressed();
      }
    });
  }

  // pickAudioFile is for the JS app, which a quick web build keeps from the last --legacy build.
  globalThis.MelovaAudio = { startCapture, startCompressed, compressedExtension: () => compressedType()[1], createPlayer, pickFile, googleSignIn,
    pickAudioFile: () => pickFile('audio/*'), apiBaseUrl: () => String(globalThis.MELOVA_API_BASE_URL || ''),
    canInstall: () => installOffer !== null, watchInstall: (changed) => { onInstallChange = changed; }, install,
    pushState, pushAddress: async () => addressOf(await pushSubscription()), pushSubscribe, pushUnsubscribe, askNotifications, watchChatLinks,
    pushAllowed: () => 'Notification' in window && Notification.permission === 'granted', onFirstTap, watchBack, holdBack };
})();
