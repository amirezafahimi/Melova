// Lets the web app be installed to the home screen. The page and the app's files are checked with
// the server when online and the last ones are kept, so a slow or dropped connection still opens
// the app; files whose name carries their content hash are served straight from the copy here, so
// opening the app a second time does not wait for megabytes again; API calls always go to the server.
const CACHE = 'melova-web-v3';
// A build names its big wasm files by their content, so a kept copy under that name is never stale.
const HASHED = /[0-9a-f]{20}\.[a-z]+$/;
// What the page needs before the app's own files begin to load.
const SHELL = ['./', 'melova.js', 'melova-audio.js'];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE)
      .then((cache) => Promise.all(SHELL.map((path) => keep(cache, path))))
      .catch(() => {})
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  const url = new URL(request.url);
  if (request.method !== 'GET' || url.origin !== self.location.origin || url.pathname.startsWith('/api/')) return;
  event.respondWith(request.mode === 'navigate' ? page(event) : file(event));
});

/**
 * The page itself: the server's, with the last one kept for a connection that is down. Opened from
 * the home screen there is no address bar and no error page, so a page this worker fails to answer
 * is a white screen that stays white.
 */
async function page(event) {
  const cache = await caches.open(CACHE);
  try {
    const response = await fetch(event.request);
    if (response.ok) event.waitUntil(keep(cache, './', response.clone()));
    return unredirected(response);
  } catch (error) {
    const cached = await cache.match('./');
    if (cached) return cached;
    throw error;
  }
}

/**
 * The app's own files. A hashed one is served from the copy here. Any other is checked with the
 * server first: its name stays the same from build to build, so serving the kept copy showed the
 * old app on the first open after every deploy. An unchanged file costs only a "not modified"
 * answer, and with no connection the kept copy still opens the app.
 */
async function file(event) {
  const cache = await caches.open(CACHE);
  const request = event.request;
  if (HASHED.test(new URL(request.url).pathname)) {
    const cached = await cache.match(request);
    if (cached) return cached;
  }
  try {
    const response = await fetch(request, { cache: 'no-cache' });
    if (response.ok) event.waitUntil(keep(cache, request, response.clone()));
    return response;
  } catch (error) {
    const cached = await cache.match(request);
    if (cached) return cached;
    throw error;
  }
}

/** Keeps [response], or what [key] answers, for the next time the app opens. */
async function keep(cache, key, response) {
  try {
    const saved = await unredirected(response || await fetch(key));
    if (saved.ok) await cache.put(key, saved);
  } catch (error) {
    // A file that cannot be kept is fetched again next time; nothing to do here.
  }
}

/**
 * Safari refuses a page the worker answers with a response that followed a redirect, and the app
 * opens blank. The same page, without the redirect behind it, is accepted.
 */
async function unredirected(response) {
  if (!response.redirected) return response;
  return new Response(await response.blob(), {
    status: response.status,
    statusText: response.statusText,
    headers: response.headers,
  });
}

self.addEventListener('push', (event) => {
  const message = event.data ? event.data.json() : {};
  event.waitUntil(self.registration.showNotification(message.title || 'ملووا', {
    body: message.body || '',
    icon: 'icons/icon-192.png',
    lang: 'fa',
    dir: 'rtl',
    tag: message.chat_id,
    renotify: Boolean(message.chat_id),
    data: { chat: message.chat_id },
  }));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const chat = event.notification.data && event.notification.data.chat;
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windows) => {
      const open = windows[0];
      if (!open) return self.clients.openWindow(chat ? './?chat=' + encodeURIComponent(chat) : './');
      if (chat) open.postMessage({ chat });
      return open.focus();
    })
  );
});
