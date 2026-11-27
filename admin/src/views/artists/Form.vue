<template>
  <div class="entity-form">
    <div class="page-header">
      <h2 class="page-title">{{ isEdit ? 'تعديل الفنان' : 'إضافة فنان جديد' }}</h2>
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
            <el-form-item label="الاسم *" prop="name">
              <el-input v-model="form.name" placeholder="اسم الفنان بالعربي" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="الاسم (إنجليزي)" prop="nameEn">
              <el-input v-model="form.nameEn" placeholder="Artist name in English" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="السيرة الذاتية" prop="bio">
          <el-input
            v-model="form.bio"
            type="textarea"
            :rows="4"
            placeholder="نبذة عن الفنان..."
          />
        </el-form-item>
        
        <el-form-item label="صورة الفنان" prop="imageUrl">
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
              <div v-if="!form.imageUrl" class="upload-placeholder">
                <el-icon class="upload-icon"><Picture /></el-icon>
                <div class="upload-text">انقر أو اسحب الصورة هنا</div>
                <div class="upload-hint">JPG, PNG, WEBP - حتى 10 ميجابايت</div>
              </div>
              <div v-else class="image-preview-container">
                <el-image
                  :src="form.imageUrl"
                  :preview-src-list="[form.imageUrl]"
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
          <el-col :xs="24" :md="12">
            <el-form-item label="حالة الترخيص" prop="licenseStatus">
              <el-select v-model="form.licenseStatus" placeholder="اختر" style="width: 100%;">
                <el-option label="مملوك" value="owned" />
                <el-option label="مرخص" value="licensed" />
                <el-option label="قيد المراجعة" value="pending" />
              </el-select>
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
            {{ isEdit ? 'تحديث الفنان' : 'إنشاء الفنان' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  ArrowLeft, Check, Picture, Upload 
} from '@element-plus/icons-vue'
import { ElForm, ElFormItem, ElInput, ElButton, ElCard, ElSwitch, ElSelect, ElOption, ElUpload, ElMessage, ElMessageBox } from 'element-plus'
import { artistsApi, uploadApi } from '@/services/api'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const submitting = ref(false)
const isEdit = ref(false)
const artistId = ref(null)
const imageFile = ref(null)

const form = ref({
  name: '',
  nameEn: '',
  bio: '',
  imageUrl: '',
  isPublished: true,
  licenseStatus: 'owned',
  licenseNote: '',
})

const rules = {
  name: [
    { required: true, message: 'الاسم مطلوب', trigger: 'blur' },
    { min: 2, max: 120, message: 'الاسم يجب أن يكون بين 2 و 120 حرف', trigger: 'blur' },
  ],
  nameEn: [
    { max: 120, message: 'الاسم الإنجليزي طويل جداً', trigger: 'blur' },
  ],
  imageUrl: [
    { required: true, message: 'صورة الفنان مطلوبة', trigger: 'change' },
  ],
}

const isEditMode = computed(() => route.params.id != null)

onMounted(() => {
  if (isEditMode.value) {
    isEdit.value = true
    artistId.value = route.params.id
    loadArtist()
  }
})

const loadArtist = async () => {
  try {
    const response = await artistsApi.get(artistId.value)
    const artist = response.data
    form.value = {
      name: artist.name,
      nameEn: artist.nameEn || '',
      bio: artist.bio || '',
      imageUrl: artist.imageUrl || '',
      isPublished: artist.isPublished,
      licenseStatus: artist.licenseStatus || 'owned',
      licenseNote: artist.licenseNote || '',
    }
  } catch (error) {
    ElMessage.error('فشل تحميل بيانات الفنان')
    router.push('/artists')
  }
}

const goBack = () => router.push('/artists')

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
    form.value.imageUrl = response.data.url
    ElMessage.success('تم رفع الصورة بنجاح')
  }
}

const handleImageError = () => {
  ElMessage.error('فشل رفع الصورة')
}

const removeImage = () => {
  form.value.imageUrl = ''
  imageFile.value = null
}

const submit = async () => {
  if (!formRef.value) return
  
  formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      let imageUrl = form.value.imageUrl
      
      // Upload new image if changed
      if (imageFile.value) {
        const uploadResponse = await uploadApi.audio(imageFile.value)
        imageUrl = uploadResponse.data.url
      }
      
      const payload = {
        ...form.value,
        imageUrl,
      }
      
      if (isEdit.value) {
        await artistsApi.update(artistId.value, payload)
        ElMessage.success('تم تحديث الفنان بنجاح')
      } else {
        await artistsApi.create(payload)
        ElMessage.success('تم إنشاء الفنان بنجاح')
      }
      
      router.push('/artists')
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