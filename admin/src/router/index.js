import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/artists',
    name: 'Artists',
    component: () => import('@/views/artists/List.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/artists/add',
    name: 'AddArtist',
    component: () => import('@/views/artists/Form.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/artists/edit/:id',
    name: 'EditArtist',
    component: () => import('@/views/artists/Form.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/albums',
    name: 'Albums',
    component: () => import('@/views/albums/List.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/albums/add',
    name: 'AddAlbum',
    component: () => import('@/views/albums/Form.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/albums/edit/:id',
    name: 'EditAlbum',
    component: () => import('@/views/albums/Form.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/songs',
    name: 'Songs',
    component: () => import('@/views/songs/List.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/songs/add',
    name: 'AddSong',
    component: () => import('@/views/songs/Form.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/songs/edit/:id',
    name: 'EditSong',
    component: () => import('@/views/songs/Form.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/playlists',
    name: 'Playlists',
    component: () => import('@/views/playlists/List.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/playlists/add',
    name: 'AddPlaylist',
    component: () => import('@/views/playlists/Form.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/playlists/edit/:id',
    name: 'EditPlaylist',
    component: () => import('@/views/playlists/Form.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/users',
    name: 'Users',
    component: () => import('@/views/users/List.vue'),
    meta: { requiresAuth: true, adminOnly: true }
  },
  {
    path: '/stats',
    name: 'Stats',
    component: () => import('@/views/Stats.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/Settings.vue'),
    meta: { requiresAuth: true, adminOnly: true }
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Auth guard
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin_token')
  const isAdmin = localStorage.getItem('admin_role') === 'admin'
  
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.adminOnly && !isAdmin) {
    next('/dashboard')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router