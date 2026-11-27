<template>
  <div class="entity-list">
    <div class="page-header">
      <h2 class="page-title">الأغاني</h2>
      <div class="action-buttons">
        <el-button type="primary" @click="goToAdd">
          <el-icon><Plus /></el-icon>
          إضافة أغنية
        </el-button>
      </div>
    </div>
    
    <el-card>
      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="البحث عن أغنية..."
          prefix-icon="Search"
          clearable
          @keyup.enter="fetchSongs"
          style="width: 300px;"
        />
        <el-select v-model="artistFilter" placeholder="كل الفنانين" style="width: 180px;" class="ml-10">
          <el-option label="كل الفنانين" value="" />
          <el-option v-for="a in artists" :key="a.id" :label="a.name" :value="a.id" />
        </el-select>
        <el-select v-model="albumFilter" placeholder="كل الألبومات" style="width: 180px;" class="ml-10">
          <el-option label="كل الألبومات" value="" />
          <el-option v-for="al in albums" :key="al.id" :label="al.title" :value="al.id" />
        </el-select>
        <el-select v-model="sortBy" placeholder="الأحدث" style="width: 140px;" class="ml-10">
          <el-option label="الأحدث" value="latest" />
          <el-option label: "الأكثر استماعاً" value="popular" />
        </el-select>
        <el-button @click="fetchSongs" type="primary">
          <el-icon><Search /></el-icon>
          بحث
        </el-button>
      </div>
      
      <el-table
        :data="songs"
        border
        stripe
        :loading="loading"
        row-key="id"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="imageUrl" label="الغلاف" width="70">
          <template #default="scope">
            <el-image
              :src="scope.row.imageUrl"
              :preview-src-list="[scope.row.imageUrl]"
              style="width: 50px; height: 50px; border-radius: 8px;"
              fit="cover"
            />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="العنوان" min-width="180">
          <template #default="scope">
            <div style="font-weight: 600;">{{ scope.row.title }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="artistName" label="الفنان" width="150" />
        <el-table-column prop="albumTitle" label="الألبوم" width="150" />
        <el-table-column prop="durationSec" label="المدة" width="80">
          <template #default="scope">
            {{ formatDuration(scope.row.durationSec) }}
          </template>
        </el-table-column>
        <el-table-column prop="playCount" label="الاستماع" width="100">
          <template #default="scope">
            {{ formatNumber(scope.row.playCount) }}
          </template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="التحميل" width="100">
          <template #default="scope">
            {{ formatNumber(scope.row.downloadCount) }}
          </template>
        </el-table-column>
        <el-table-column label="الحالة" width="120">
          <template #default="scope">
            <el-tag
              :type="scope.row.isPublished ? 'success' : 'danger'"
              size="small"
            >
              {{ scope.row.isPublished ? 'منشور' : 'مسودة' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="الإجراءات" width="200" fixed="right">
          <template #default="scope">
            <div class="table-actions">
              <el-button
                size="small"
                type="primary"
                @click="goToEdit(scope.row.id)"
              >
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button
                size="small"
                type="success"
                @click="playPreview(scope.row)"
              >
                <el-icon><Monitor /></el-icon>
              </el-button>
              <el-button
                size="small"
                :type="scope.row.isPublished ? 'warning' : 'success'"
                @click="togglePublish(scope.row)"
              >
                <el-icon>
                  <component :is="scope.row.isPublished ? 'Delete' : 'Check'"/>
                </el-icon>
              </el-button>
              <el-button
                size="small"
                type="danger"
                @click="deleteSong(scope.row)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-if="total > pageSize"
        :page-size="pageSize"
        :current-page="currentPage"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Plus, Search, Edit, Delete, Check, Monitor, Music 
} from '@element-plus/icons-vue'
import { ElTable, ElTableColumn, ElPagination, ElButton, ElInput, ElSelect, ElOption, ElTag, ElCard, ElImage, ElMessage, ElMessageBox, ElAudio } from 'element-plus'
import { songsApi, artistsApi, albumsApi } from '@/services/api'
import { formatNumber, formatDuration } from '@/utils/formatters'

const router = useRouter()

const songs = ref([])
const artists = ref([])
const albums = ref([])
const loading = ref(false)
const searchQuery = ref('')
const artistFilter = ref('')
const albumFilter = ref('')
const sortBy = ref('latest')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const fetchSongs = async () => {
  loading.value = true
  try {
    const params = {
      q: searchQuery.value,
      artistId: artistFilter.value,
      albumId: albumFilter.value,
      limit: pageSize.value,
      offset: (currentPage.value - 1) * pageSize.value,
      sortBy: sortBy.value,
      includeUnpublished: true,
    }
    const response = await songsApi.list(params)
    songs.value = response.data.items
    total.value = response.data.total
  } catch (error) {
    ElMessage.error('فشل تحميل الأغاني')
  } finally {
    loading.value = false
  }
}

const fetchArtists = async () => {
  try {
    const response = await artistsApi.list({ includeUnpublished: true, limit: 1000 })
    artists.value = response.data.items
  } catch (error) {
    console.error('Failed to load artists')
  }
}

const fetchAlbums = async () => {
  try {
    const response = await albumsApi.list({ includeUnpublished: true, limit: 1000 })
    albums.value = response.data.items
  } catch (error) {
    console.error('Failed to load albums')
  }
}

const goToAdd = () => router.push('/songs/add')
const goToEdit = (id) => router.push(`/songs/edit/${id}`)

const togglePublish = async (song) => {
  try {
    await songsApi.update(song.id, { isPublished: !song.isPublished })
    song.isPublished = !song.isPublished
    ElMessage.success(song.isPublished ? 'تم النشر' : 'تم إخفاء الأغنية')
  } catch (error) {
    ElMessage.error('فشل تحديث الحالة')
  }
}

const playPreview = (song) => {
  ElMessage.info('معاينة: ' + song.title)
  // Could open a modal with audio player
}

const deleteSong = async (song) => {
  try {
    await ElMessageBox.confirm(
      `هل أنت متأكد من حذف "${song.title}"؟`,
      'تأكيد الحذف',
      { type: 'warning' }
    )
    await songsApi.delete(song.id)
    ElMessage.success('تم الحذف بنجاح')
    fetchSongs()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('فشل الحذف')
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchSongs()
}

onMounted(() => {
  fetchSongs()
  fetchArtists()
  fetchAlbums()
})
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.ml-10 {
  margin-left: 10px;
}

.table-actions {
  display: flex;
  gap: 6px;
}
</style>