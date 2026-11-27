<template>
  <div class="entity-list">
    <div class="page-header">
      <h2 class="page-title">الألبومات</h2>
      <div class="action-buttons">
        <el-button type="primary" @click="goToAdd">
          <el-icon><Plus /></el-icon>
          إضافة ألبوم
        </el-button>
      </div>
    </div>
    
    <el-card>
      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="البحث عن ألبوم..."
          prefix-icon="Search"
          clearable
          @keyup.enter="fetchAlbums"
          style="width: 300px;"
        />
        <el-select v-model="artistFilter" placeholder="كل الفنانين" style="width: 200px;" class="ml-10">
          <el-option label="كل الفنانين" value="" />
          <el-option
            v-for="artist in artists"
            :key="artist.id"
            :label="artist.name"
            :value="artist.id"
          />
        </el-select>
        <el-button @click="fetchAlbums" type="primary">
          <el-icon><Search /></el-icon>
          بحث
        </el-button>
      </div>
      
      <el-table
        :data="albums"
        border
        stripe
        :loading="loading"
        row-key="id"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="coverUrl" label="الغلاف" width="70">
          <template #default="scope">
            <el-image
              :src="scope.row.coverUrl"
              :preview-src-list="[scope.row.coverUrl]"
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
        <el-table-column prop="releaseYear" label="السنة" width="80" />
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
        <el-table-column label="الإجراءات" width="180" fixed="right">
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
                @click="deleteAlbum(scope.row)"
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
  Plus, Search, Edit, Delete, Check, Collection 
} from '@element-plus/icons-vue'
import { ElTable, ElTableColumn, ElPagination, ElButton, ElInput, ElSelect, ElOption, ElTag, ElCard, ElImage, ElMessage, ElMessageBox } from 'element-plus'
import { albumsApi, artistsApi } from '@/services/api'
import { formatNumber } from '@/utils/formatters'

const router = useRouter()

const albums = ref([])
const artists = ref([])
const loading = ref(false)
const searchQuery = ref('')
const artistFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const fetchAlbums = async () => {
  loading.value = true
  try {
    const params = {
      q: searchQuery.value,
      artistId: artistFilter.value,
      limit: pageSize.value,
      offset: (currentPage.value - 1) * pageSize.value,
      includeUnpublished: true,
    }
    const response = await albumsApi.list(params)
    albums.value = response.data.items
    total.value = response.data.total
  } catch (error) {
    ElMessage.error('فشل تحميل الألبومات')
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

const goToAdd = () => router.push('/albums/add')
const goToEdit = (id) => router.push(`/albums/edit/${id}`)

const togglePublish = async (album) => {
  try {
    await albumsApi.update(album.id, { isPublished: !album.isPublished })
    album.isPublished = !album.isPublished
    ElMessage.success(album.isPublished ? 'تم النشر' : 'تم إخفاء الألبوم')
  } catch (error) {
    ElMessage.error('فشل تحديث الحالة')
  }
}

const deleteAlbum = async (album) => {
  try {
    await ElMessageBox.confirm(
      `هل أنت متأكد من حذف "${album.title}"؟`,
      'تأكيد الحذف',
      { type: 'warning' }
    )
    await albumsApi.delete(album.id)
    ElMessage.success('تم الحذف بنجاح')
    fetchAlbums()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('فشل الحذف')
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchAlbums()
}

onMounted(() => {
  fetchAlbums()
  fetchArtists()
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