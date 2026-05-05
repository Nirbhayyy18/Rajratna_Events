package com.rajratna.events.ui.screens.backup

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.database.AppDatabase
import com.rajratna.events.data.repository.BackupData
import com.rajratna.events.ui.components.SectionHeader
import com.rajratna.events.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupState(
    val isWorking: Boolean = false,
    val lastBackupTime: Long? = null,
    val message: String? = null
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RajratnaApp).repository
    private val prefs = application.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(BackupState(lastBackupTime = prefs.getLong("last_backup", 0).takeIf { it > 0 }))
    val state: StateFlow<BackupState> = _state.asStateFlow()

    fun createBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isWorking = true, message = null)
            try {
                val data = repository.getAllDataForBackup()
                val json = Gson().toJson(data)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }
                val now = System.currentTimeMillis()
                prefs.edit().putLong("last_backup", now).apply()
                _state.value = _state.value.copy(isWorking = false, lastBackupTime = now, message = "Backup created successfully!")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isWorking = false, message = "Backup failed: ${e.message}")
            }
        }
    }

    fun restoreBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isWorking = true, message = null)
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                }
                val data = Gson().fromJson(json, BackupData::class.java)
                // Reset database and restore
                AppDatabase.resetInstance()
                val db = AppDatabase.getDatabase(context)
                db.clearAllTables()
                val newRepo = com.rajratna.events.data.repository.AppRepository(db.itemDao(), db.customerDao(), db.orderDao(), db.paymentDao())
                newRepo.restoreFromBackup(data)
                _state.value = _state.value.copy(isWorking = false, message = "Restore completed! Please restart the app.")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isWorking = false, message = "Restore failed: ${e.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onNavigateBack: () -> Unit, viewModel: BackupViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val createFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.createBackup(context, it) }
    }
    val openFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingRestoreUri = it; showRestoreConfirm = true }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Backup & Restore", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Last backup info
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Column {
                        Text("Last Backup", style = MaterialTheme.typography.labelMedium)
                        Text(if (state.lastBackupTime != null) DateUtils.formatDateTime(state.lastBackupTime!!) else "Never", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Message
            state.message?.let { msg ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (msg.contains("success") || msg.contains("completed")) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)) {
                    Text(msg, Modifier.padding(16.dp))
                }
            }

            // Create Backup
            SectionHeader("Create Backup")
            Button(onClick = { createFileLauncher.launch("rajratna_backup_${System.currentTimeMillis()}.json") },
                Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), enabled = !state.isWorking) {
                if (state.isWorking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else { Icon(Icons.Default.CloudUpload, null); Spacer(Modifier.width(8.dp)); Text("Create & Save Backup", fontWeight = FontWeight.SemiBold) }
            }
            Text("Save to Google Drive, WhatsApp, or phone storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            HorizontalDivider()

            // Restore Backup
            SectionHeader("Restore Backup")
            OutlinedButton(onClick = { openFileLauncher.launch(arrayOf("application/json")) },
                Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), enabled = !state.isWorking) {
                Icon(Icons.Default.CloudDownload, null); Spacer(Modifier.width(8.dp)); Text("Select Backup File", fontWeight = FontWeight.SemiBold)
            }
            Text("⚠️ Restoring will replace all current data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore Data?") },
            text = { Text("This will replace ALL current app data with the backup. This cannot be undone. Are you sure?") },
            confirmButton = { Button(onClick = { showRestoreConfirm = false; pendingRestoreUri?.let { viewModel.restoreBackup(context, it) } }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Yes, Restore") } },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") } }
        )
    }
}
