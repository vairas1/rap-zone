package com.rapzone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rapzone.app.di.AppContainer
import com.rapzone.app.network.AppConfig
import com.rapzone.app.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Settings: appearance (dark mode), playback/download prefs, account, API.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onDarkModeChange: ((Boolean) -> Unit)? = null) {
    val context = LocalContext.current
    val container = AppContainer.instance()!!
    val scope = rememberCoroutineScope()

    var dark by remember { mutableStateOf(AppConfig.isDarkTheme(context)) }
    var wifiOnly by remember { mutableStateOf(true) }
    var highQuality by remember { mutableStateOf(false) }
    var bgPlayback by remember { mutableStateOf(true) }

    var apiUrl by remember { mutableStateOf(AppConfig.apiUrl(context).trimEnd('/')) }
    var apiMsg by remember { mutableStateOf<String?>(null) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var authMsg by remember { mutableStateOf<String?>(null) }
    val logged = container.repository.isLogged()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("الإعدادات") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            // Appearance
            SectionHeader("المظهر")
            SettingsRow("الوضع الداكن", checked = dark) { v ->
                dark = v
                scope.launch(Dispatchers.IO) { AppConfig.setDarkTheme(context, v) }
                onDarkModeChange?.invoke(v)
            }

            // Playback
            SectionHeader("التشغيل والتحميل")
            SettingsRow("التحميل عبر WiFi فقط", checked = wifiOnly) { wifiOnly = it }
            SettingsRow("الجودة العالية", checked = highQuality) { highQuality = it }
            SettingsRow("التشغيل في الخلفية", checked = bgPlayback) { bgPlayback = it }

            // API server
            SectionHeader("رابط الخادم (API)")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = { Text("مثال: http://192.168.1.5:4000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            AppConfig.setApiUrl(context, apiUrl.trim())
                            apiMsg = "تم حفظ رابط الخادم وتطبيقه"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حفظ وتطبيق")
                }
                apiMsg?.let { m ->
                    Text(
                        m,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Account
            SectionHeader("الحساب")
            if (logged) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("مسجل الدخول", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        scope.launch(Dispatchers.IO) { container.repository.logout() }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "تسجيل الخروج")
                    }
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row {
                        OutlinedButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        container.repository.login(email, password)
                                        authMsg = "تم تسجيل الدخول بنجاح"
                                    } catch (e: Exception) {
                                        authMsg = "فشل الدخول: ${e.message}"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("دخول")
                        }
                    }
                    authMsg?.let { m ->
                        Text(
                            m,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // About
            SectionHeader("حول")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "Rap Zone v1.0.0",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}