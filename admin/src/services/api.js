import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_role')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// Auth
export const authApi = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  register: (data) => api.post('/auth/register', data),
  profile: () => api.get('/auth/profile'),
  changePassword: (data) => api.post('/auth/change-password', data),
}

// Artists
export const artistsApi = {
  list: (params) => api.get('/artists', { params }),
  get: (id) => api.get(`/artists/${id}`),
  create: (data) => api.post('/artists', data),
  update: (id, data) => api.patch(`/artists/${id}`, data),
  delete: (id) => api.delete(`/artists/${id}`),
}

// Albums
export const albumsApi = {
  list: (params) => api.get('/albums', { params }),
  get: (id) => api.get(`/albums/${id}`),
  create: (data) => api.post('/albums', data),
  update: (id, data) => api.patch(`/albums/${id}`, data),
  delete: (id) => api.delete(`/albums/${id}`),
}

// Songs
export const songsApi = {
  list: (params) => api.get('/songs', { params }),
  get: (id) => api.get(`/songs/${id}`),
  create: (data) => api.post('/songs', data),
  update: (id, data) => api.patch(`/songs/${id}`, data),
  delete: (id) => api.delete(`/songs/${id}`),
  popular: (limit) => api.get('/songs/popular', { params: { limit } }),
  latest: (limit) => api.get('/songs/latest', { params: { limit } }),
  recordPlay: (id, data) => api.post(`/songs/${id}/play`, data),
  recordDownload: (id) => api.post(`/songs/${id}/download`),
}

// Playlists
export const playlistsApi = {
  list: () => api.get('/playlists'),
  get: (id) => api.get(`/playlists/${id}`),
  create: (data) => api.post('/playlists', data),
  update: (id, data) => api.patch(`/playlists/${id}`, data),
  delete: (id) => api.delete(`/playlists/${id}`),
  addSong: (id, songId) => api.post(`/playlists/${id}/songs`, { songId }),
  removeSong: (id, songId) => api.delete(`/playlists/${id}/songs/${songId}`),
}

// Favorites
export const favoritesApi = {
  list: () => api.get('/favorites'),
  add: (songId) => api.post(`/favorites/${songId}`),
  remove: (songId) => api.delete(`/favorites/${songId}`),
}

// User Playlists
export const userPlaylistsApi = {
  list: () => api.get('/me/playlists'),
  create: (data) => api.post('/me/playlists', data),
  update: (id, data) => api.patch(`/me/playlists/${id}`, data),
  delete: (id) => api.delete(`/me/playlists/${id}`),
  addSong: (id, songId) => api.post(`/me/playlists/${id}/songs`, { songId }),
  removeSong: (id, songId) => api.delete(`/me/playlists/${id}/songs/${songId}`),
}

// Stats
export const statsApi = {
  dashboard: () => api.get('/stats/dashboard'),
  history: (params) => api.get('/stats/history', { params }),
  users: () => api.get('/stats/users'),
  createUser: (data) => api.post('/stats/users', data),
  updateUser: (id, data) => api.patch(`/stats/users/${id}`, data),
  deleteUser: (id) => api.delete(`/stats/users/${id}`),
}

// Upload
export const uploadApi = {
  audio: (file, onProgress) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/upload/audio', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress,
    })
  },
  image: (file, onProgress) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress,
    })
  },
}

export default api