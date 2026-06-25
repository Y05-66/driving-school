// 自毁式 Service Worker - 加载后立即注销自身并清除所有缓存
self.addEventListener('install', () => {
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then(names => {
      return Promise.all(names.map(name => caches.delete(name)))
    }).then(() => self.registration.unregister())
  )
})

self.addEventListener('fetch', (event) => {
  // 不拦截任何请求，直接放行
  event.respondWith(fetch(event.request))
})
