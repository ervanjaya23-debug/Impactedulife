package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SchoolSupervisionOverview
import com.example.model.TeacherSupervisionSummary
import com.example.model.User
import com.example.ui.theme.EmeraldDone
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PrincipalTeal
import com.example.util.TeacherPerformancePdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ExportPdfDialog(
    currentUser: User,
    schoolOverview: SchoolSupervisionOverview,
    allSummaries: List<TeacherSupervisionSummary>,
    filteredSummaries: List<TeacherSupervisionSummary>,
    activeFilterLabel: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var exportAll by remember { mutableStateOf(true) }
    var principalName by remember { mutableStateOf(currentUser.displayName.ifBlank { "Kepala Sekolah" }) }
    var principalNip by remember { mutableStateOf("") }
    var supervisionNotes by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val targetSummaries = if (exportAll) allSummaries else filteredSummaries
    val targetLabel = if (exportAll) "Semua Dewan Guru (${allSummaries.size} guru)" else "$activeFilterLabel (${filteredSummaries.size} guru)"

    AlertDialog(
        onDismissRequest = {
            if (!isGenerating) onDismiss()
        },
        modifier = Modifier.testTag("export_pdf_dialog"),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrincipalTeal.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = PrincipalTeal,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Ekspor Laporan PDF",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dokumen Evaluasi Kinerja Dewan Guru",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Info preview card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = PrincipalTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Format Standar Cetak A4",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Laporan memuat kop sekolah, KPI rata-rata ketepatan waktu, status pengumpulan modul ajar, dan tabel rinci guru beserta predikat kinerja.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scope Selection
                Text(
                    text = "Pilih Data Guru Yang Dimasukkan:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exportAll = true }
                        .padding(vertical = 4.dp)
                        .testTag("export_pdf_scope_all")
                ) {
                    RadioButton(
                        selected = exportAll,
                        onClick = { exportAll = true }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Semua Guru (${allSummaries.size} guru)",
                        fontSize = 13.sp,
                        fontWeight = if (exportAll) FontWeight.Medium else FontWeight.Normal
                    )
                }

                if (filteredSummaries.size != allSummaries.size) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { exportAll = false }
                            .padding(vertical = 4.dp)
                            .testTag("export_pdf_scope_filtered")
                    ) {
                        RadioButton(
                            selected = !exportAll,
                            onClick = { exportAll = false }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hasil Filter Aktif: $activeFilterLabel (${filteredSummaries.size} guru)",
                            fontSize = 13.sp,
                            fontWeight = if (!exportAll) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Principal Signee Name
                OutlinedTextField(
                    value = principalName,
                    onValueChange = { principalName = it },
                    label = { Text("Nama Kepala Sekolah (Penanda Tangan)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_pdf_principal_name"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Principal NIP
                OutlinedTextField(
                    value = principalNip,
                    onValueChange = { principalNip = it },
                    label = { Text("NIP Kepala Sekolah (Opsional)") },
                    placeholder = { Text("Contoh: 19780512 200501 1 008") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_pdf_principal_nip"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notes / Recommendation
                OutlinedTextField(
                    value = supervisionNotes,
                    onValueChange = { supervisionNotes = it },
                    label = { Text("Catatan / Rekomendasi Supervisi (Opsional)") },
                    placeholder = { Text("Contoh: Secara umum kepatuhan penyusunan modul ajar meningkat, namun guru mapel perlu penguatan ketepatan batas waktu...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_pdf_notes"),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                if (isGenerating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = PrincipalTeal
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sedang menyusun dokumen PDF...",
                            fontSize = 12.sp,
                            color = PrincipalTeal,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button: Buka Langsung / Cetak
                    FilledTonalButton(
                        onClick = {
                            if (targetSummaries.isEmpty()) {
                                Toast.makeText(context, "Tidak ada data guru untuk diekspor", Toast.LENGTH_SHORT).show()
                                return@FilledTonalButton
                            }
                            isGenerating = true
                            coroutineScope.launch {
                                val generatedFile = withContext(Dispatchers.IO) {
                                    TeacherPerformancePdfExporter.generatePdf(
                                        context = context,
                                        options = TeacherPerformancePdfExporter.ExportOptions(
                                            schoolName = currentUser.schoolName,
                                            principalName = principalName,
                                            principalNip = principalNip,
                                            notes = supervisionNotes,
                                            filterLabel = targetLabel
                                        ),
                                        schoolOverview = schoolOverview,
                                        summaries = targetSummaries
                                    )
                                }
                                isGenerating = false
                                Toast.makeText(context, "Laporan PDF berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                TeacherPerformancePdfExporter.viewPdf(context, generatedFile)
                                onDismiss()
                            }
                        },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_pdf_view_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lihat / Cetak", fontSize = 12.sp, maxLines = 1)
                    }

                    // Button: Bagikan PDF (WhatsApp, Gmail, dll)
                    Button(
                        onClick = {
                            if (targetSummaries.isEmpty()) {
                                Toast.makeText(context, "Tidak ada data guru untuk diekspor", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isGenerating = true
                            coroutineScope.launch {
                                val generatedFile = withContext(Dispatchers.IO) {
                                    TeacherPerformancePdfExporter.generatePdf(
                                        context = context,
                                        options = TeacherPerformancePdfExporter.ExportOptions(
                                            schoolName = currentUser.schoolName,
                                            principalName = principalName,
                                            principalNip = principalNip,
                                            notes = supervisionNotes,
                                            filterLabel = targetLabel
                                        ),
                                        schoolOverview = schoolOverview,
                                        summaries = targetSummaries
                                    )
                                }
                                isGenerating = false
                                Toast.makeText(context, "Laporan PDF siap dibagikan!", Toast.LENGTH_SHORT).show()
                                TeacherPerformancePdfExporter.sharePdf(context, generatedFile, currentUser.schoolName)
                                onDismiss()
                            }
                        },
                        enabled = !isGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = PrincipalTeal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_pdf_share_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bagikan PDF", fontSize = 12.sp, maxLines = 1)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isGenerating,
                        modifier = Modifier.testTag("export_pdf_cancel_btn")
                    ) {
                        Text("Tutup", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        dismissButton = null
    )
}
