<template>
  <div class="entity-form">
    <div class="page-header">
      <h2 class="page-title">{{ isEdit ? 'تعديل الألبوم' : 'إضافة ألبوم جديد' }}</h2>
      <div class="action-buttons">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          رجوع
        </el-button>
        <el-button type="primary" @click="submit" :loading="submitting">
          <el-icon><Check /></el-icon>
          {{ isEdit ? 'تحديث' : 'إنشاء' }}
        </el-button>
      </div>
    </div>
    
    <el-card>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        label-position="right"
      >
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="العنوان *" prop="title">
              <el-input v-model="form.title" placeholder="عنوان الألبوم" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="الفنان *" prop="artistId">
              <el-select v-model="form.artistId" placeholder="اختر الفنان" style="width: 100%;" filterable>
                <el-option
                  v-for="artist in artists"
                  :key="artist.id"
                  :label="artist.name"
                  :value="artist.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="سنة الإصدار" prop="releaseYear">
              <el-input-number
                v-model="form.releaseYear"
                :min="1900"
                :max="new Date().getFullYear() + 1"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="الحالة" prop="isPublished">
              <el-switch
                v-model="form.isPublished"
                active-text="منشور"
                inactive-text="مسودة"
                active-color="#4CAF50"
                inactive-color="#F44336"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="الوصف" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="وصف الألبوم..."
          />
        </el-form-item>
        
        <el-form-item label="صورة الغلاف" prop="coverUrl">
          <div class="image-upload">
            <el-upload
              class="upload-area"
              action="#"
              :auto-upload="false"
              :on-change="handleImageChange"
              :on-success="handleImageSuccess"
              :on-error="handleImageError"
              :show-file-list="false"
              :before-upload="beforeImageUpload"
            >
              <div v-if="!form.coverUrl" class="upload-placeholder">
                <el-icon class="upload-icon"><Picture /></el-icon>
                <div class="upload-text">انقر أو اسحب صورة الغلاف</div>
                <div class="upload-hint">JPG, PNG, WEBP - حتى 10 ميجابايت</div>
              </div>
              <div v-else class="image-preview-container">
                <el-image
                  :src="form.coverUrl"
                  :preview-src-list="[form.coverUrl]"
                  fit="cover"
                  style="width: 100%; max-height: 250px; border-radius: 8px;"
                />
                <el-button
                  size="small"
                  type="danger"
                  class="remove-image-btn"
                  @click="removeImage"
                >
                  <el-icon><Delete /></el-icon>
                  إزالة
                </el-button>
              </div>
            </el-upload>
          </div>
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="حالة الترخيص" prop="licenseStatus">
              <el-select v-model="form.licenseStatus" placeholder="اختر" style="width: 100%;">
                <el-option label="مملوك" value="owned" />
                <el-option label="مرخص" value="licensed" />
                <el-option label="قيد المراجعة" value="pending" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="الغلاف منشور" prop="coverPublished">
              <el-switch
                v-model="form.coverPublished"
                active-text="نعم"
                inactive-text="لا"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="ملاحظة الترخيص" prop="licenseNote">
          <el-input
            v-model="form.licenseNote"
            type="textarea"
            :rows="2"
            placeholder="ملاحظات حول حقوق النشر..."
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="submit" :loading="submitting" style="width: 100%;">
            <el-icon><Check /></el-icon>
            {{ isEdit ? 'تحديث الألبوم' : 'إنشاء الألبوم' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  ArrowLeft, Check, Picture 
} from '@element-plus/icons-vue'
import { ElForm, ElFormItem, ElInput, ElInputNumber, ElButton, ElCard, ElSwitch, ElSelect, ElOption, ElUpload, ElMessage, ElMessageBox } from 'element-plus'
import { albumsApi, artistsApi, uploadApi } from '@/services/api'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const submitting = ref(false)
const isEdit = ref(false)
const albumId = ref(null)
const imageFile = ref(null)

const form = ref({
  title: '',
  artistId: '',
  releaseYear: new Date().getFullYear(),
  description: '',
  coverUrl: '',
  isPublished: true,
  licenseStatus: 'owned',
  licenseNote: '',
  coverPublished: true,
})

const rules = {
  title: [
    { required: true, message: 'العنوان مطلوب', trigger: 'blur' },
  ],
  artistId: [
    { required: true, message: 'اختر فنان', trigger: 'change' },
  ],
}

const isEditMode = computed(() => route.params.id != null)

onMounted(() => {
  fetchArtists()
  if (isEditMode.value) {
    isEdit.value = true
    albumId.value = route.params.id
    loadAlbum()
  } else {
    form.value.releaseYear = new Date().getFullYear()
  }
})

const fetchArtists = async () => {
  try {
    const response = await artistsApi.list({ includeUnpublished: true, limit: 1000 })
    artists.value = response.data.items
  } catch (error) {
    console.error('Failed to load artists')
  }
}

const loadAlbum = async () => {
  try {
    const response = await albumsApi.get(albumId.value)
    const album = response.data
    form.value = {
      title: album.title,
      artistId: album.artistId,
      releaseYear: album.releaseYear,
      description: album.description || '',
      coverUrl: album.coverUrl || '',
      isPublished: album.isPublished,
      licenseStatus: album.licenseStatus || 'owned',
      licenseNote: album.licenseNote || '',
      coverPublished: true,
    }
  } catch (error) {
    ElMessage.error('فشل تحميل بيانات الألبوم')
    router.push('/albums')
  }
}

const artists = ref([])

const goBack = () => router.push('/albums')

const handleImageChange = (file) => {
  imageFile.value = file.raw
}

const beforeImageUpload = (file) => {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('نوع الملف غير مدعوم. استخدم JPG, PNG, أو WEBP')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('حجم الملف يجب أن يكون أقل من 10 ميجابايت')
    return false
  }
  return true
}

const handleImageSuccess = (response, file, fileList) => {
  if (response.data?.url) {
    form.value.coverUrl = response.data.url
    ElMessage.success('تم رفع صورة الغلاف بنجاح')
  }
}

const handleImageError = () => {
  ElMessage.error('فشل رفع صورة الغلاف')
}

const removeImage = () => {
  form.value.coverUrl = ''
  imageFile.value = null
}

const submit = async () => {
  if (!formRef.value) return
  
  formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      let coverUrl = form.value.coverUrl
      
      if (imageFile.value) {
        const uploadResponse = await uploadApi.image(imageFile.value)
        coverUrl = uploadResponse.data.url
      }
      
      const payload = {
        ...form.value,
        coverUrl,
      }
      
      if (isEdit.value) {
        await albumsApi.update(albumId.value, payload)
        ElMessage.success('تم تحديث الألبوم بنجاح')
      } else {
        await albumsApi.create(payload)
        ElMessage.success('تم إنشاء الألبوم بنجاح')
      }
      
      router.push('/albums')
    } catch (error) {
      ElMessage.error(error.response?.data?.message || 'فشل الحفظ')
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped>
.image-upload :deep(.el-upload) {
  width: 100%;
}

.upload-area {
  border: 2px dashed #333;
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-area:hover {
  border-color: #FF0000;
  background-color: rgba(255, 0, 0, 0.05);
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.upload-icon {
  font-size: 48px;
  color: #FF0000;
}

.upload-text {
  font-size: 16px;
  color: #B0B0B0;
}

.upload-hint {
  font-size: 12px;
  color: #666;
}

.image-preview-container {
  position: relative;
  display: inline-block;
  border-radius: 8px;
  overflow: hidden;
}

.remove-image-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
  background: rgba(0, 0, 0, 0.7);
}
</style>