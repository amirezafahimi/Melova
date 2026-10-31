// Lets the web app be installed to the home screen. App files are fetched fresh when online
// and kept as a fallback, so a slow or dropped connection still opens the app; API calls
// always go to the server.
const CACHE = 'melova-web-v1';

self.addEventListener('install', () => self.skipWaiting());

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
  event.respondWith(
    fetch(request)
      .then((response) => {
        if (response.ok) {
          const copy = response.clone();
          caches.open(CACHE).then((cache) => cache.put(request, copy));
        }
        return response;
      })
      .catch(() => caches.match(request).then((cached) => cached || Promise.reject(new Error('offline'))))
  );
});

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
