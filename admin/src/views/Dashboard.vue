<template>
  <div class="dashboard">
    <div class="page-header">
      <h2 class="page-title">لوحة التحكم</h2>
    </div>
    
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6" v-for="stat in stats" :key="stat.key">
        <div class="stat-card" :style="{ borderLeftColor: stat.color }">
          <div class="stat-header">
            <div class="stat-icon" :style="{ backgroundColor: stat.bgColor }">
              <el-icon :size="24" :color="stat.color"><component :is="stat.icon" /></el-icon>
            </div>
            <el-tag :type="stat.trend > 0 ? 'success' : 'danger'" size="small" v-if="stat.trend !== undefined">
              {{ stat.trend > 0 ? '+' : '' }}{{ stat.trend }}%
            </el-tag>
          </div>
          <div class="stat-value">{{ formatNumber(stat.value) }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :xs="24" :lg="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>الاستماع خلال 14 يوماً</span>
            </div>
          </template>
          <div class="chart-container" ref="playsChart"></div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :lg="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>أكثر الأغاني استماعاً</span>
            </div>
          </template>
          <div class="top-songs">
            <div 
              v-for="(song, index) in topSongs" 
              :key="song.id" 
              class="top-song-item"
            >
              <span class="rank">{{ index + 1 }}</span>
              <div class="song-info">
                <div class="song-title">{{ song.title }}</div>
                <div class="song-artist">{{ song.artistName }}</div>
              </div>
              <div class="song-plays">
                <el-icon><Headset /></el-icon>
                {{ formatNumber(song.playCount) }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :xs="24" :lg="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>أكثر الفنانين استماعاً</span>
            </div>
          </template>
          <div class="top-artists">
            <div 
              v-for="(artist, index) in topArtists" 
              :key="artist.id" 
              class="top-artist-item"
            >
              <span class="rank">{{ index + 1 }}</span>
              <div class="artist-info">
                <el-avatar :size="40" :src="artist.imageUrl" shape="circle" />
                <div class="artist-details">
                  <div class="artist-name">{{ artist.name }}</div>
                  <div class="artist-plays">{{ formatNumber(artist.playCount) }} تشغيل</div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :lg="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>النشاط الأخير</span>
            </div>
          </template>
          <el-table :data="recentActivity" border stripe size="small">
            <el-table-column prop="title" label="الأغنية" min-width="180" />
            <el-table-column prop="artistName" label="الفنان" width="150" />
            <el-table-column prop="userEmail" label="المستخدم" width="180">
              <template #default="scope">
                <el-tag size="small" v-if="scope.row.userEmail">{{ scope.row.userEmail }}</el-tag>
                <span v-else style="color: #B0B0B0;">زائر</span>
              </template>
            </el-table-column>
            <el-table-column prop="at" label="الوقت" width="160">
              <template #default="scope">
                {{ formatDate(scope.row.at) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { 
  User, Collection, List, Music, Headset, TrendCharts, DataAnalysis 
} from '@element-plus/icons-vue'
import { ElRow, ElCol, ElCard, ElTag, ElAvatar, ElTable, ElTableColumn, ElMessage } from 'element-plus'
import { statsApi } from '@/services/api'
import { formatNumber, formatDate } from '@/utils/formatters'

const stats = ref([
  { key: 'users', label: 'المستخدمون', value: 0, icon: User, color: '#FF0000', bgColor: 'rgba(255,0,0,0.1)', trend: 12 },
  { key: 'songs', label: 'الأغاني', value: 0, icon: Music, color: '#4CAF50', bgColor: 'rgba(76,175,80,0.1)', trend: 8 },
  { key: 'artists', label: 'الفنانين', value: 0, icon: User, color: '#2196F3', bgColor: 'rgba(33,150,243,0.1)', trend: 5 },
  { key: 'plays', label: 'إجمالي الاستماع', value: 0, icon: Headset, color: '#FF9800', bgColor: 'rgba(255,152,0,0.1)', trend: 23 },
])

const playsChart = ref(null)
const chartInstance = ref(null)

const topSongs = ref([])
const topArtists = ref([])
const recentActivity = ref([])

const loadDashboard = async () => {
  try {
    const response = await statsApi.dashboard()
    const data = response.data
    
    stats.value[0].value = data.counts.users
    stats.value[1].value = data.counts.songs
    stats.value[2].value = data.counts.artists
    stats.value[3].value = data.totals.plays
    
    topSongs.value = data.topSongs
    topArtists.value = data.topArtists
    recentActivity.value = data.daily.map(d => ({
      title: `استماع يوم ${d.date}`,
      artistName: '',
      userEmail: '',
      at: d.date,
      plays: d.plays,
    })).reverse().slice(0, 10)
    
    initPlaysChart(data.daily)
  } catch (error) {
    console.error('Failed to load dashboard:', error)
  }
}

const initPlaysChart = (dailyData) => {
  nextTick(() => {
    if (!playsChart.value) return
    
    if (chartInstance.value) {
      chartInstance.value.dispose()
    }
    
    chartInstance.value = echarts.init(playsChart.value)
    
    const dates = dailyData.map(d => d.date.slice(5)).reverse()
    const plays = dailyData.map(d => d.plays).reverse()
    
    const option = {
      grid: { top: 20, right: 20, bottom: 40, left: 50 },
      xAxis: {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: '#333' } },
        axisLabel: { color: '#B0B0B0', fontSize: 11 },
      },
      yAxis: {
        type: 'value',
        axisLine: { lineStyle: { color: '#333' } },
        axisLabel: { color: '#B0B0B0', fontSize: 11 },
        splitLine: { lineStyle: { color: '#222' } },
      },
      series: [{
        data: plays,
        type: 'line',
        smooth: true,
        color: '#FF0000',
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(255,0,0,0.3)' },
              { offset: 1, color: 'rgba(255,0,0,0)' },
            ],
          },
        },
        lineStyle: { width: 2 },
        symbol: 'circle',
        symbolSize: 6,
      }],
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#1E1E1E',
        borderColor: '#333',
        textStyle: { color: '#FFF' },
        formatter: (params) => `${params[0].axisValue}<br/>${params[0].seriesName}: ${formatNumber(params[0].value)}`,
      },
    }
    
    chartInstance.value.setOption(option)
    
    window.addEventListener('resize', () => {
      chartInstance.value?.resize()
    })
  })
}

const formatNumber = (num) => {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'م'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'ك'
  return num.toString()
}

const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('ar-SA', { 
    day: '2-digit', 
    month: '2-digit', 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.dashboard {
  height: 100%;
}

.stats-row {
  margin-bottom: 8px;
}

.stat-card {
  background: linear-gradient(135deg, #121212 0%, #1E1E1E 100%);
  border: 1px solid #333;
  border-radius: 16px;
  padding: 24px;
  border-left-width: 4px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(255, 0, 0, 0.15);
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #FFFFFF;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #B0B0B0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  font-size: 16px;
  font-weight: 600;
}

.top-songs, .top-artists {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.top-song-item, .top-artist-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.top-song-item:hover, .top-artist-item:hover {
  background-color: #1E1E1E;
}

.rank {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: linear-gradient(135deg, #FF0000, #FF3333);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.song-info, .artist-details {
  flex: 1;
  min-width: 0;
}

.song-title, .artist-name {
  font-weight: 600;
  color: #FFFFFF;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.song-artist {
  font-size: 12px;
  color: #B0B0B0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.artist-plays {
  font-size: 12px;
  color: #FF0000;
  font-weight: 500;
}

.song-plays {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #B0B0B0;
  font-size: 13px;
  font-weight: 500;
}

.chart-container {
  width: 100%;
  height: 300px;
}
</style>