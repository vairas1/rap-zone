<template>
  <div class="entity-list">
    <div class="page-header">
      <h2 class="page-title">الفنانين</h2>
      <div class="action-buttons">
        <el-button type="primary" @click="goToAdd">
          <el-icon><Plus /></el-icon>
          إضافة فنان
        </el-button>
      </div>
    </div>
    
    <el-card>
      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="البحث عن فنان..."
          prefix-icon="Search"
          clearable
          @keyup.enter="fetchArtists"
          style="width: 300px;"
        />
        <el-select v-model="statusFilter" placeholder="الكل" style="width: 140px;" class="ml-10">
          <el-option label="الكل" value="" />
          <el-option label="منشور" value="published" />
          <el-option label="غير منشور" value="unpublished" />
        </el-select>
        <el-button @click="fetchArtists" type="primary">
          <el-icon><Search /></el-icon>
          بحث
        </el-button>
      </div>
      
      <el-table
        :data="artists"
        border
        stripe
        :loading="loading"
        row-key="id"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="imageUrl" label="الصورة" width="70">
          <template #default="scope">
            <el-image
              :src="scope.row.imageUrl"
              :preview-src-list="[scope.row.imageUrl]"
              style="width: 50px; height: 50px; border-radius: 8px;"
              fit="cover"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="الاسم" min-width="180">
          <template #default="scope">
            <div>
              <div style="font-weight: 600;">{{ scope.row.name }}</div>
              <div v-if="scope.row.nameEn" style="font-size: 12px; color: #B0B0B0;">{{ scope.row.nameEn }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="playCount" label="الاستماع" width="120">
          <template #default="scope">
            {{ formatNumber(scope.row.playCount) }}
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
                @click="deleteArtist(scope.row)"
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
  Plus, Search, Edit, Delete, Check, User 
} from '@element-plus/icons-vue'
import { ElTable, ElTableColumn, ElPagination, ElButton, ElInput, ElSelect, ElOption, ElTag, ElCard, ElImage, ElMessage, ElMessageBox } from 'element-plus'
import { artistsApi } from '@/services/api'
import { formatNumber } from '@/utils/formatters'

const router = useRouter()

const artists = ref([])
const loading = ref(false)
const searchQuery = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const fetchArtists = async () => {
  loading.value = true
  try {
    const params = {
      q: searchQuery.value,
      limit: pageSize.value,
      offset: (currentPage.value - 1) * pageSize.value,
      includeUnpublished: true,
    }
    if (statusFilter.value === 'published') params.includeUnpublished = false
    if (statusFilter.value === 'unpublished') params.includeUnpublished = 'only'
    
    const response = await artistsApi.list(params)
    artists.value = response.data.items
    total.value = response.data.total
  } catch (error) {
    ElMessage.error('فشل تحميل الفنانين')
  } finally {
    loading.value = false
  }
}

const goToAdd = () => router.push('/artists/add')
const goToEdit = (id) => router.push(`/artists/edit/${id}`)

const togglePublish = async (artist) => {
  try {
    await artistsApi.update(artist.id, { isPublished: !artist.isPublished })
    artist.isPublished = !artist.isPublished
    ElMessage.success(artist.isPublished ? 'تم النشر' : 'تم إخفاء الفنان')
  } catch (error) {
    ElMessage.error('فشل تحديث الحالة')
  }
}

const deleteArtist = async (artist) => {
  try {
    await ElMessageBox.confirm(
      `هل أنت متأكد من حذف "${artist.name}"؟`,
      'تأكيد الحذف',
      { type: 'warning' }
    )
    await artistsApi.delete(artist.id)
    ElMessage.success('تم الحذف بنجاح')
    fetchArtists()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('فشل الحذف')
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchArtists()
}

onMounted(() => {
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

.ml-10 {
  margin-left: 10px;
}
</style>