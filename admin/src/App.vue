<template>
  <el-container>
    <el-aside :width="isCollapsed ? '64px' : '260px'">
      <div class="sidebar-header">
        <div v-if="!isCollapsed" class="logo">
          <el-icon :size="28" class="text-red"><Music /></el-icon>
          <span class="logo-text">Rap Zone</span>
        </div>
        <div v-else class="logo-collapsed">
          <el-icon :size="28" class="text-red"><Music /></el-icon>
        </div>
        <el-button
          v-if="!isCollapsed"
          class="collapse-btn"
          @click="isCollapsed = true"
          circle
          size="small"
        >
          <el-icon><Fold /></el-icon>
        </el-button>
        <el-button
          v-else
          class="collapse-btn"
          @click="isCollapsed = false"
          circle
          size="small"
        >
          <el-icon><Expand /></el-icon>
        </el-button>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        :unique-opened="true"
        :collapse="isCollapsed"
        router
        class="el-menu-vertical"
        background-color="#121212"
        text-color="#B0B0B0"
        active-text-color="#FF0000"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <template #title>لوحة التحكم</template>
        </el-menu-item>
        
        <el-sub-menu index="artists">
          <template #title>
            <el-icon><User /></el-icon>
            <span>الفنانين</span>
          </template>
          <el-menu-item index="/artists">قائمة الفنانين</el-menu-item>
          <el-menu-item index="/artists/add">إضافة فنان</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="albums">
          <template #title>
            <el-icon><Collection /></el-icon>
            <span>الألبومات</span>
          </template>
          <el-menu-item index="/albums">قائمة الألبومات</el-menu-item>
          <el-menu-item index="/albums/add">إضافة ألبوم</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="songs">
          <template #title>
            <el-icon><Music /></el-icon>
            <span>الأغاني</span>
          </template>
          <el-menu-item index="/songs">قائمة الأغاني</el-menu-item>
          <el-menu-item index="/songs/add">إضافة أغنية</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="playlists">
          <template #title>
            <el-icon><List /></el-icon>
            <span>قوائم التشغيل</span>
          </template>
          <el-menu-item index="/playlists">قوائم التشغيل</el-menu-item>
          <el-menu-item index="/playlists/add">إنشاء قائمة</el-menu-item>
        </el-sub-menu>
        
        <el-menu-item index="/users">
          <el-icon><UserFilled /></el-icon>
          <template #title>المستخدمين</template>
        </el-menu-item>
        
        <el-menu-item index="/stats">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>الإحصائيات</template>
        </el-menu-item>
        
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <template #title>الإعدادات</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header height="60px" style="display: flex; align-items: center; justify-content: space-between; padding: 0 24px;">
        <div style="display: flex; align-items: center; gap: 16px;">
          <h1 style="margin: 0; font-size: 20px; font-weight: 600;">{{ pageTitle }}</h1>
        </div>
        <div style="display: flex; align-items: center; gap: 16px;">
          <el-dropdown>
            <span class="el-dropdown-link" style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
              <el-avatar :size="32" :src="userAvatar" />
              <span v-if="!isCollapsed">{{ userName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <div style="padding: 8px 0;">
                    <div style="font-weight: 600;">{{ userName }}</div>
                    <div style="font-size: 12px; color: #B0B0B0;">{{ userEmail }}</div>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item @click.native="goToProfile">
                  <el-icon><User /></el-icon>
                  <span>الملف الشخصي</span>
                </el-dropdown-item>
                <el-dropdown-item @click.native="logout">
                  <el-icon><SwitchButton /></el-icon>
                  <span style="color: #F44336;">تسجيل الخروج</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main style="padding: 24px;">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Music, Monitor, User, Collection, List, UserFilled,
  DataAnalysis, Setting, Fold, Expand, ArrowDown, SwitchButton
} from '@element-plus/icons-vue'
import { ElContainer, ElAside, ElHeader, ElMain, ElMenu, ElSubMenu, ElMenuItem, ElButton, ElAvatar, ElDropdown, ElDropdownMenu, ElDropdownItem } from 'element-plus'

const router = useRouter()
const route = useRoute()

const isCollapsed = ref(false)
const activeMenu = ref(route.path)

const userName = ref('المشرف الرئيسي')
const userEmail = ref('admin@rapzone.app')
const userAvatar = ref('https://api.dicebear.com/7.x/avataaars/svg?seed=admin')

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    '/dashboard': 'لوحة التحكم',
    '/artists': 'قائمة الفنانين',
    '/artists/add': 'إضافة فنان جديد',
    '/albums': 'قائمة الألبومات',
    '/albums/add': 'إضافة ألبوم جديد',
    '/songs': 'قائمة الأغاني',
    '/songs/add': 'إضافة أغنية جديدة',
    '/playlists': 'قوائم التشغيل',
    '/playlists/add': 'إنشاء قائمة تشغيل',
    '/users': 'إدارة المستخدمين',
    '/stats': 'الإحصائيات والتحليلات',
    '/settings': 'الإعدادات',
  }
  return titles[route.path] || 'Rap Zone Admin'
})

const goToProfile = () => {
  router.push('/profile')
}

const logout = () => {
  localStorage.removeItem('admin_token')
  router.push('/login')
}

onMounted(() => {
  activeMenu.value = route.path
})
</script>

<style scoped>
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  height: 60px;
  border-bottom: 1px solid #333;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #FF0000;
}

.logo-collapsed {
  display: flex;
  justify-content: center;
}

.text-red {
  color: #FF0000;
}

.collapse-btn {
  color: #B0B0B0;
}

.collapse-btn:hover {
  background-color: #1E1E1E;
  color: #FFFFFF;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
</script>