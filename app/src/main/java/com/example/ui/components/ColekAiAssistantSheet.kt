package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TaskRepository
import com.example.data.local.LocalTaskRepository
import com.example.model.Task
import com.example.model.TaskScope
import com.example.model.User
import com.example.model.toEntity
import com.example.ui.theme.EmeraldDone
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OrangeFlame
import com.example.ui.theme.PrincipalTeal
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ColekAiViewModel"

data class StructuredTaskDraft(
    val title: String,
    val category: String,
    val description: String,
    val politeNudgeTemplate: String,
    val coachingNote: String
)

sealed interface ColekAiUiState {
    data object Idle : ColekAiUiState
    data class Streaming(val partialText: String) : ColekAiUiState
    data class Success(val text: String) : ColekAiUiState
    data class DraftSuccess(val draft: StructuredTaskDraft) : ColekAiUiState
    data class Error(val message: String) : ColekAiUiState
}

class ColekAiViewModel : ViewModel() {

    // Lazy initialization so FirebaseApp and App Check are guaranteed to be configured first
    private val chatModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val structuredDraftModel by lazy {
        val draftSchema = Schema.obj(
            mapOf(
                "title" to Schema.string("Judul tugas atau program supervisi yang jelas dan spesifik"),
                "category" to Schema.string("Kategori atau mata pelajaran, misalnya Modul Ajar / RPP, Kurikulum, Matematika, atau Proyek Kelas"),
                "description" to Schema.string("Instruksi pengerjaan langkah demi langkah beserta kriteria keberhasilan"),
                "politeNudgeTemplate" to Schema.string("Kalimat pengingat Colek Santun yang sopan, apresiatif, dan memotivasi"),
                "coachingNote" to Schema.string("Tips pembinaan bagi kepala sekolah/guru agar tugas berjalan efektif")
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = draftSchema
            }
        )
    }

    private val _uiState = MutableStateFlow<ColekAiUiState>(ColekAiUiState.Idle)
    val uiState: StateFlow<ColekAiUiState> = _uiState.asStateFlow()

    fun askAssistantStreaming(
        userPrompt: String,
        user: User,
        schoolContextSummary: String = ""
    ) {
        if (userPrompt.isBlank()) return
        viewModelScope.launch {
            _uiState.value = ColekAiUiState.Streaming("")
            val systemContext = buildString {
                appendLine("Kamu adalah 'Asisten AI Colek', asisten pendidikan cerdas berbahasa Indonesia untuk aplikasi manajemen tugas dan supervisi sekolah 'Colek'.")
                appendLine("Nama pengguna: ${user.displayName.ifBlank { "Pendidik" }}")
                appendLine("Peran pengguna: ${user.role}")
                appendLine("Nama sekolah: ${user.schoolName.ifBlank { "Sekolah" }}")
                if (schoolContextSummary.isNotBlank()) {
                    appendLine("Data ringkasan sekolah saat ini: $schoolContextSummary")
                }
                appendLine("Berikan jawaban yang terstruktur, praktis, sopan, dan langsung dapat diterapkan di lingkungan sekolah Indonesia (Kurikulum Merdeka).")
                appendLine()
                appendLine("Pertanyaan / Permintaan Pengguna:")
                appendLine(userPrompt.trim())
            }

            try {
                var accumulated = ""
                withTimeout(60_000L) {
                    chatModel.generateContentStream(systemContext).collect { chunk ->
                        val part = chunk.text.orEmpty()
                        accumulated += part
                        _uiState.value = ColekAiUiState.Streaming(accumulated)
                    }
                }
                if (accumulated.isBlank()) {
                    _uiState.value = ColekAiUiState.Error("Respons kosong dari Gemini AI. Silakan coba lagi.")
                } else {
                    _uiState.value = ColekAiUiState.Success(accumulated.trim())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini streaming request failed", e)
                _uiState.value = ColekAiUiState.Error(
                    e.localizedMessage ?: "Gagal menghubungi Gemini AI. Periksa koneksi internet Anda."
                )
            }
        }
    }

    fun generateStructuredTaskDraft(
        topicPrompt: String,
        user: User
    ) {
        if (topicPrompt.isBlank()) return
        viewModelScope.launch {
            _uiState.value = ColekAiUiState.Streaming("Menyusun draf tugas & template Colek Santun terstruktur...")
            val prompt = buildString {
                appendLine("Buatkan draf tugas sekolah beserta template pesan pengingat 'Colek Santun' dalam Bahasa Indonesia untuk sekolah '${user.schoolName.ifBlank { "Sekolah" }}'.")
                appendLine("Peran pembuat: ${user.role} (${user.displayName})")
                if (user.isPrincipal) {
                    appendLine("Target tugas: Dewan Guru (misalnya penyusunan Modul Ajar, RPP, asesmen diagnostik, atau administrasi supervisi).")
                } else {
                    appendLine("Target tugas: Siswa kelas (mata pelajaran: ${user.subjectSpecialty.ifBlank { "Umum" }}).")
                }
                appendLine("Topik atau kebutuhan spesifik: ${topicPrompt.trim()}")
            }

            try {
                val response = withTimeout(60_000L) {
                    structuredDraftModel.generateContent(prompt)
                }
                val rawJson = response.text?.trim().orEmpty()
                if (rawJson.isBlank()) {
                    _uiState.value = ColekAiUiState.Error("Respons draf kosong dari Gemini AI.")
                    return@launch
                }
                val json = JSONObject(rawJson)
                val draft = StructuredTaskDraft(
                    title = json.optString("title", "Tugas Baru"),
                    category = json.optString(
                        "category",
                        if (user.isPrincipal) "Modul Ajar / RPP" else user.subjectSpecialty.ifBlank { " Akademik" }
                    ),
                    description = json.optString("description", ""),
                    politeNudgeTemplate = json.optString("politeNudgeTemplate", ""),
                    coachingNote = json.optString("coachingNote", "")
                )
                _uiState.value = ColekAiUiState.DraftSuccess(draft)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini structured draft generation failed", e)
                _uiState.value = ColekAiUiState.Error(
                    e.localizedMessage ?: "Gagal menyusun draf tugas dengan Gemini AI."
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = ColekAiUiState.Idle
    }
}

@Composable
fun ColekAiAssistantDialog(
    currentUser: User,
    taskRepository: TaskRepository? = null,
    localTaskRepository: LocalTaskRepository? = null,
    schoolContextSummary: String = "",
    initialPrompt: String = "",
    onDismiss: () -> Unit,
    aiViewModel: ColekAiViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by aiViewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var promptText by remember(initialPrompt) { mutableStateOf(initialPrompt) }
    var isSavingTask by remember { mutableStateOf(false) }
    var taskSavedSuccess by remember { mutableStateOf(false) }

    val accentColor = if (currentUser.isPrincipal) PrincipalTeal else IndigoPrimary

    val quickSuggestions = remember(currentUser.role) {
        when {
            currentUser.isPrincipal -> listOf(
                "Analisis data supervisi guru saat ini dan berikan 3 langkah pembinaan",
                "Susun rubrik & kriteria supervisi Modul Ajar Kurikulum Merdeka",
                "Buat 3 contoh pesan Colek Santun yang memotivasi guru untuk tepat waktu"
            )
            currentUser.isTeacher -> listOf(
                "Buatkan ide tugas proyek kolaboratif siswa beserta kriteria penilaiannya",
                "Susun pesan Colek Santun untuk mengingatkan siswa yang belum mengumpulkan tugas",
                "Buat kerangka singkat Modul Ajar 1 pertemuan yang interaktif"
            )
            else -> listOf(
                "Bantu saya menyusun jadwal belajar harian agar semua PR selesai tepat waktu",
                "Cara mengatasi rasa malas dan menunda tugas sekolah",
                "Tips merangkum materi pelajaran dengan metode Cornell Notes"
            )
        }
    }

    val draftSuggestions = remember(currentUser.role) {
        if (currentUser.isPrincipal) {
            listOf(
                "Pengumpulan Modul Ajar Semester Baru",
                "Laporan Hasil Asesmen Diagnostik Siswa",
                "Evaluasi & Refleksi Pembelajaran Bulanan"
            )
        } else {
            listOf(
                "Tugas Proyek Kelompok & Presentasi Kelas",
                "Latihan Soal Evaluasi Bab Mingguan",
                "Portofolio Ringkasan Materi & Refleksi"
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .testTag("colek_ai_assistant_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Asisten AI Colek (Gemini)",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Didukung Gemini 3.8 Flash • ${currentUser.schoolName.ifBlank { "Sekolah" }}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_ai_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Asisten AI"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Tabs (Available for Principal/Teacher: Chat + Structured Task Generator; Student: Chat)
                if (currentUser.isPrincipal || currentUser.isTeacher) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        contentColor = accentColor,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                taskSavedSuccess = false
                            },
                            text = {
                                Text(
                                    text = if (currentUser.isPrincipal) "Konsultasi Supervisi" else "Tanya AI Guru",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("ai_tab_chat")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                taskSavedSuccess = false
                            },
                            text = {
                                Text(
                                    text = "Generator Draf Tugas",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("ai_tab_generator")
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Quick Suggestion Chips
                Text(
                    text = if (selectedTab == 0) "Ide Pertanyaan Cepat:" else "Pilih Topik Draf Tugas:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                val activeSuggestions = if (selectedTab == 0) quickSuggestions else draftSuggestions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeSuggestions.forEachIndexed { idx, suggestion ->
                        FilterChip(
                            selected = promptText == suggestion,
                            onClick = { promptText = suggestion },
                            label = {
                                Text(
                                    text = suggestion,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.16f),
                                selectedLabelColor = accentColor
                            ),
                            modifier = Modifier.testTag("ai_suggestion_chip_$idx")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input Field
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    label = {
                        Text(
                            if (selectedTab == 0) {
                                "Tulis pertanyaan atau instruksi untuk Gemini AI..."
                            } else {
                                "Topik tugas yang ingin dibuatkan drafnya..."
                            }
                        )
                    },
                    placeholder = {
                        Text(
                            if (selectedTab == 0) {
                                "Contoh: Berikan analisis singkat ketepatan waktu guru dan saran pembinaan..."
                            } else {
                                "Contoh: Penyusunan Modul Ajar Kurikulum Merdeka Kelas 10..."
                            }
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_prompt_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                val isGenerating = uiState is ColekAiUiState.Streaming

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState !is ColekAiUiState.Idle) {
                        OutlinedButton(
                            onClick = {
                                aiViewModel.resetState()
                                taskSavedSuccess = false
                            },
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            taskSavedSuccess = false
                            if (selectedTab == 0) {
                                aiViewModel.askAssistantStreaming(
                                    userPrompt = promptText,
                                    user = currentUser,
                                    schoolContextSummary = schoolContextSummary
                                )
                            } else {
                                aiViewModel.generateStructuredTaskDraft(
                                    topicPrompt = promptText,
                                    user = currentUser
                                )
                            }
                        },
                        enabled = promptText.isNotBlank() && !isGenerating,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_generate_btn")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Memproses dengan Gemini...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedTab == 0) "Tanya Gemini AI" else "Buat Draf Terstruktur",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Output / Result Section
                when (val state = uiState) {
                    is ColekAiUiState.Idle -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Siap Membantu Produktivitas Sekolah",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (currentUser.isPrincipal) {
                                        "Gunakan Gemini AI untuk menganalisis capaian supervisi guru, menyusun rekomendasi pembinaan yang santun, atau membuat draf tugas Modul Ajar otomatis."
                                    } else if (currentUser.isTeacher) {
                                        "Gunakan Gemini AI untuk menyusun instruksi tugas siswa, rubrik penilaian, hingga kalimat pengingat Colek Santun."
                                    } else {
                                        "Tanyakan strategi belajar, cara membagi waktu pengerjaan tugas, atau rangkuman materi kepada Gemini AI."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is ColekAiUiState.Streaming -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = accentColor.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_streaming_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = accentColor,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Gemini sedang menyusun jawaban...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                                if (state.partialText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = state.partialText,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    is ColekAiUiState.Success -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_response_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Hasil Analisis & Saran Gemini AI",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            copyToClipboard(context, "Hasil Gemini AI", state.text)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Salin Teks",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = state.text,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    is ColekAiUiState.DraftSuccess -> {
                        val draft = state.draft
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = accentColor.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_structured_draft_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Draf Tugas Terstruktur (JSON Schema)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                    IconButton(
                                        onClick = {
                                            val summaryText = "${draft.title}\nKategori: ${draft.category}\n\n${draft.description}\n\nColek Santun: ${draft.politeNudgeTemplate}"
                                            copyToClipboard(context, "Draf Tugas AI", summaryText)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Salin Draf",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = draft.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Kategori: ${draft.category}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Deskripsi & Langkah Pengerjaan:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = draft.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (draft.politeNudgeTemplate.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Template Pesan Colek Santun:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangeFlame
                                    )
                                    Text(
                                        text = "\"${draft.politeNudgeTemplate}\"",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (draft.coachingNote.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Catatan Pembinaan / Tips:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDone
                                    )
                                    Text(
                                        text = draft.coachingNote,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (taskRepository != null && (currentUser.isPrincipal || currentUser.isTeacher)) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    if (taskSavedSuccess) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldDone,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Tugas berhasil diterbitkan ke dasbor sekolah!",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldDone
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                isSavingTask = true
                                                coroutineScope.launch {
                                                    try {
                                                        val calendar = java.util.Calendar.getInstance().apply {
                                                            add(java.util.Calendar.DAY_OF_YEAR, 3)
                                                        }
                                                        val newTask = Task(
                                                            schoolId = currentUser.schoolId,
                                                            createdBy = currentUser.id,
                                                            creatorRole = currentUser.role,
                                                            creatorName = currentUser.displayName,
                                                            scope = if (currentUser.isPrincipal) {
                                                                TaskScope.TEACHER_TASK.name
                                                            } else {
                                                                TaskScope.STUDENT_TASK.name
                                                            },
                                                            title = draft.title,
                                                            subjectOrCategory = draft.category,
                                                            description = draft.description,
                                                            targetFilter = if (currentUser.isPrincipal) {
                                                                "SEMUA GURU"
                                                            } else {
                                                                "SEMUA KELAS"
                                                            },
                                                            priority = com.example.model.TaskPriority.HIGH.name,
                                                            dueDate = com.google.firebase.Timestamp(calendar.time)
                                                        )
                                                        val docId = taskRepository.createTask(newTask)
                                                        if (docId.isNotBlank()) {
                                                            localTaskRepository?.saveTask(
                                                                newTask.copy(id = docId).toEntity()
                                                            )
                                                            taskSavedSuccess = true
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Gagal menyimpan tugas ke server",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(
                                                            context,
                                                            "Gagal menyimpan tugas: ${e.localizedMessage}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } finally {
                                                        isSavingTask = false
                                                    }
                                                }
                                            },
                                            enabled = !isSavingTask,
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDone),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("ai_publish_draft_task_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlaylistAddCheck,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isSavingTask) {
                                                    "Menyimpan Tugas..."
                                                } else if (currentUser.isPrincipal) {
                                                    "Terbitkan sebagai Tugas Guru"
                                                } else {
                                                    "Terbitkan sebagai Tugas Siswa"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is ColekAiUiState.Error -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_error_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Gagal Memuat Respons Gemini AI",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = state.message,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Disalin ke papan klip", Toast.LENGTH_SHORT).show()
}
