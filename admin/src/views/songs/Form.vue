<template>
  <div class="entity-form">
    <div class="page-header">
      <h2 class="page-title">{{ isEdit ? 'تعديل الأغنية' : 'إضافة أغنية جديدة' }}</h2>
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
              <el-input v-model="form.title" placeholder="عنوان الأغنية" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="الألبوم *" prop="albumId">
              <el-select v-model="form.albumId" placeholder="اختر الألبوم" style="width: 100%;" filterable>
                <el-option v-for="al in albums" :key="al.id" :label="al.title" :value="al.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :xs="24" :md="6">
            <el-form-item label="المدة (ثواني) *" prop="durationSec">
              <el-input-number v-model="form.durationSec" :min="1" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="6">
            <el-form-item label="الترتيب في الألبوم" prop="position">
              <el-input-number v-model="form.position" :min="1" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="6">
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
          <el-col :xs="24" :md="6">
            <el-form-item label="حالة الترخيص" prop="licenseStatus">
              <el-select v-model="form.licenseStatus" placeholder="اختر" style="width: 100%;">
                <el-option label="مملوك" value="owned" />
                <el-option label="مرخص" value="licensed" />
                <el-option label="قيد المراجعة" value="pending" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="الكلمات" prop="lyrics">
          <el-input
            v-model="form.lyrics"
            type="textarea"
            :rows="6"
            placeholder="كلمات الأغنية..."
          />
        </el-form-item>
        
        <el-form-item label="ملف الصوت (MP3) *" prop="audioUrl">
          <div class="file-upload">
            <el-upload
              class="upload-area"
              action="#"
              :auto-upload="false"
              :on-change="handleAudioChange"
              :on-success="handleAudioSuccess"
              :on-error="handleAudioError"
              :show-file-list="false"
              :before-upload="beforeAudioUpload"
            >
              <div v-if="!form.audioUrl" class="upload-placeholder">
                <el-icon class="upload-icon"><Music /></el-icon>
                <div class="upload-text">انقر أو اسحب ملف MP3</div>
                <div class="upload-hint">MP3 فقط - حتى 60 ميجابايت</div>
              </div>
              <div v-else class="audio-preview-container">
                <el-audio :src="form.audioUrl" controls />
                <el-button size="small" type="danger" @click="removeAudio">
                  <el-icon><Delete /></el-icon>
                  إزالة
                </el-button>
              </div>
            </el-upload>
          </div>
        </el-form-item>
        
        <el-form-item label="صورة الأغنية" prop="imageUrl">
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
                <div class="upload-text">صورة اختيارية للأغنية</div>
              </div>
              <div v-else class="image-preview-container">
                <el-image :src="form.imageUrl" :preview-src-list="[form.imageUrl]" fit="cover" style="width: 100%; max-height: 200px; border-radius: 8px;" />
                <el-button size="small" type="danger" class="remove-image-btn" @click="removeImage">
                  <el-icon><Delete /></el-icon> إزالة
                </el-button>
              </div>
            </el-upload>
          </div>
        </el-form-item>
        
        <el-form-item label="ملاحظة الترخيص" prop="licenseNote">
          <el-input v-model="form.licenseNote" type="textarea" :rows="2" placeholder="ملاحظات حول حقوق النشر..." />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="submit" :loading="submitting" style="width: 100%;">
            <el-icon><Check /></el-icon>
            {{ isEdit ? 'تحديث الأغنية' : 'إنشاء الأغنية' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, Check, Music, Picture, Upload } from '@element-plus/icons-vue'
import { ElForm, ElFormItem, ElInput, ElInputNumber, ElButton, ElCard, ElSwitch, ElSelect, ElOption, ElUpload, ElMessage, ElMessageBox } from 'element-plus'
import { songsApi, artistsApi, albumsApi, uploadApi } from '@/services/api'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const submitting = ref(false)
const isEdit = ref(false)
const songId = ref(null)
const audioFile = ref(null)
const imageFile = ref(null)

const form = ref({
  title: '',
  albumId: '',
  durationSec: 180,
  position: 1,
  lyrics: '',
  audioUrl: '',
  imageUrl: '',
  isPublished: true,
  licenseStatus: 'owned',
  licenseNote: '',
})

const rules = {
  title: [{ required: true, message: 'العنوان مطلوب', trigger: 'blur' }],
  albumId: [{ required: true, message: 'اختر الألبوم', trigger: 'change' }],
  durationSec: [{ required: true, message: 'المدة مطلوبة', trigger: 'blur' }, { min: 1, message: 'المدة يجب أن تكون أكبر من صفر', trigger: 'blur' }],
}

const isEditMode = computed(() => route.params.id != null)

onMounted(() => {
  fetchAlbums()
  if (isEditMode.value) {
    isEdit.value = true
    songId.value = route.params.id
    loadSong()
  }
})

const fetchAlbums = async () => {
  try {
    const response = await albumsApi.list({ includeUnpublished: true, limit: 1000 })
    albums.value = response.data.items
  } catch (error) {
    console.error('Failed to load albums')
  }
}

const loadSong = async () => {
  try {
    const response = await songsApi.get(songId.value)
    const song = response.data
    form.value = {
      title: song.title,
      albumId: song.albumId,
      durationSec: song.durationSec,
      position: song.position,
      lyrics: song.lyrics || '',
      audioUrl: song.audioUrl || '',
      imageUrl: song.imageUrl || '',
      isPublished: song.isPublished,
      licenseStatus: song.licenseStatus || 'owned',
      licenseNote: song.licenseNote || '',
    }
  } catch (error) {
    ElMessage.error('فشل تحميل بيانات الأغنية')
    router.push('/songs')
  }
}

const albums = ref([])

const goBack = () => router.push('/songs')

const handleAudioChange = (file) => { audioFile.value = file.raw }
const beforeAudioUpload = (file) => {
  if (file.type !== 'audio/mpeg' && !file.name.endsWith('.mp3')) { ElMessage.error('نوع الملف غير مدعوم. استخدم MP3 فقط'); return false }
  if (file.size > 60 * 1024 * 1024) { ElMessage.error('حجم الملف يجب أن يكون أقل من 60 ميجابايت'); return false }
  return true
}
const handleAudioSuccess = (response) => { if (response.data?.url) { form.value.audioUrl = response.data.url; ElMessage.success('تم رفع ملف الصوت بنجاح') } }
const handleAudioError = () => ElMessage.error('فشل رفع ملف الصوت')
const removeAudio = () => { form.value.audioUrl = ''; audioFile.value = null }

const handleImageChange = (file) => { imageFile.value = file.raw }
const beforeImageUpload = (file) => {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowedTypes.includes(file.type)) { ElMessage.error('نوع الملف غير مدعوم. استخدم JPG, PNG, أو WEBP'); return false }
  if (file.size > 10 * 1024 * 1024) { ElMessage.error('حجم الملف يجب أن يكون أقل من 10 ميجابايت'); return false }
  return true
}
const handleImageSuccess = (response) => { if (response.data?.url) { form.value.imageUrl = response.data.url; ElMessage.success('تم رفع الصورة بنجاح') } }
const handleImageError = () => ElMessage.error('فشل رفع الصورة')
const removeImage = () => { form.value.imageUrl = ''; imageFile.value = null }

const submit = async () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      let audioUrl = form.value.audioUrl, imageUrl = form.value.imageUrl
      if (audioFile.value) { const r = await uploadApi.audio(audioFile.value); audioUrl = r.data.url }
      if (imageFile.value) { const r = await uploadApi.image(imageFile.value); imageUrl = r.data.url }
      const payload = { ...form.value, audioUrl, imageUrl }
      if (isEdit.value) { await songsApi.update(songId.value, payload); ElMessage.success('تم تحديث الأغنية') }
      else { await songsApi.create(payload); ElMessage.success('تم إنشاء الأغنية') }
      router.push('/songs')
    } catch (error) { ElMessage.error(error.response?.data?.message || 'فشل الحفظ') }
    finally { submitting.value = false }
  })
}
</script>

<style scoped>
.upload-area { border: 2px dashed #333; border-radius: 12px; padding: 40px 20px; text-align: center; cursor: pointer; transition: all 0.2s; }
.upload-area:hover { border-color: #FF0000; background-color: rgba(255, 0, 0, 0.05); }
.upload-placeholder { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.upload-icon { font-size: 48px; color: #FF0000; }
.upload-text { font-size: 16px; color: #B0B0B0; }
.upload-hint { font-size: 12px; color: #666; }
.image-preview-container, .audio-preview-container { position: relative; display: inline-block; width: 100%; }
.remove-image-btn, .remove-audio-btn { position: absolute; top: 8px; right: 8px; z-index: 10; background: rgba(0, 0, 0, 0.7); }
</style>