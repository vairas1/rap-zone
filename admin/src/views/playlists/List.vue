<template>
  <div class="entity-list">
    <div class="page-header">
      <h2 class="page-title">قوائم التشغيل</h2>
      <div class="action-buttons">
        <el-button type="primary" @click="goToAdd"><el-icon><Plus /></el-icon> إنشاء قائمة</el-button>
      </div>
    </div>
    <el-card>
      <el-table :data="playlists" border stripe :loading="loading" row-key="id">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="imageUrl" label="الصورة" width="70"><template #default="s"><el-image :src="s.row.imageUrl" style="width:50px;height:50px;border-radius:8px" fit="cover"/></template></el-table-column>
        <el-table-column prop="name" label="الاسم" min-width="180"><template #default="s"><div style="font-weight:600">{{s.row.name}}</div><div v-if="s.row.nameEn" style="font-size:12px;color:#B0B0B0">{{s.row.nameEn}}</div></template></el-table-column>
        <el-table-column label="الحالة" width="120"><template #default="s"><el-tag :type="s.row.isPublished?'success':'danger'" size="small">{{s.row.isPublished?'منشور':'مسودة'}}</el-tag></template></el-table-column>
        <el-table-column label="الإجراءات" width="180" fixed="right"><template #default="s"><div class="table-actions"><el-button size="small" type="primary" @click="goToEdit(s.row.id)"><el-icon><Edit/></el-icon></el-button><el-button size="small" :type="s.row.isPublished?'warning':'success'" @click="togglePublish(s.row)"><el-icon><component :is="s.row.isPublished?'Delete':'Check'"/></el-icon></el-button><el-button size="small" type="danger" @click="deletePlaylist(s.row)"><el-icon><Delete/></el-icon></el-button></div></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
<script setup>
import {ref,onMounted} from 'vue'; import {useRouter} from 'vue-router'; import {Plus,Search,Edit,Delete,Check,List} from '@element-plus/icons-vue'; import {ElTable,ElTableColumn,ElButton,ElInput,ElSelect,ElOption,ElTag,ElCard,ElImage,ElMessage,ElMessageBox} from 'element-plus'; import {playlistsApi} from '@/services/api';
const router=useRouter(); const playlists=ref([]); const loading=ref(false); const searchQuery=ref('');
const fetchPlaylists=async()=>{loading.value=true;try{const r=await playlistsApi.list();playlists.value=r.data.items}finally{loading.value=false}};
const goToAdd=()=>router.push('/playlists/add'); const goToEdit=id=>router.push(`/playlists/edit/${id}`);
const togglePublish=async p=>{try{await playlistsApi.update(p.id,{isPublished:!p.isPublished});p.isPublished=!p.isPublished;ElMessage.success(p.isPublished?'تم النشر':'تم إخفاء القائمة')}catch{ElMessage.error('فشل')}};
const deletePlaylist=async p=>{try{await ElMessageBox.confirm(`حذف "${p.name}"؟`,'تأكيد',{type:'warning'});await playlistsApi.delete(p.id);ElMessage.success('تم الحذف');fetchPlaylists()}catch(e){if(e!=='cancel')ElMessage.error('فشل الحذف')}};
onMounted(fetchPlaylists);
</script>