// Caches the app so it opens offline once installed. Bump VERSION when you update index.html.
const VERSION = 'loaf-v46';
const FILES = ['./', './index.html', './basic.html', './manifest.webmanifest', './icon-192.png', './icon-512.png', './icon-maskable-512.png'];
const OPTIONAL = ['./lumi-messages.js'];   // missing is fine
self.addEventListener('install', (e) => { e.waitUntil(caches.open(VERSION).then((c) => c.addAll(FILES).then(() => Promise.all(OPTIONAL.map((u) => c.add(u).catch(() => {}))))).then(() => self.skipWaiting())); });
self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k !== VERSION).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET') return;
  if (new URL(req.url).origin !== self.location.origin) return;   // only ever touch this app's own files
  // network first (so updates show up), fall back to the cache when offline
  e.respondWith(fetch(req).then((r) => { if (r.ok) { const copy = r.clone(); caches.open(VERSION).then((c) => c.put(req, copy)); } return r; })
    .catch(() => caches.match(req).then((m) => m || (req.mode === 'navigate' ? caches.match('./index.html') : Response.error()))));
});
// tapping a pop-up notice brings LOAF to the front
self.addEventListener('notificationclick', (e) => {
  e.notification.close();
  e.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((cs) => {
    for (const c of cs) if ('focus' in c) return c.focus();
    return self.clients.openWindow ? self.clients.openWindow('./') : null;
  }));
});
