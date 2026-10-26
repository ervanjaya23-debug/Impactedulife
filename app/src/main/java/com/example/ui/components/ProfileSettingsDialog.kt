package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserRepository
import com.example.model.User
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PrincipalTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Renders a user's profile avatar from Base64 data URI, local URI, or fallback initials/icon
 * with memoized decoding to prevent UI flickering during recompositions.
 */
@Composable
fun UserAvatar(
    photoUrl: String,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.2f),
    contentColor: Color = Color.White,
    fallbackIcon: ImageVector? = null,
    showEditBadge: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val decodedBitmap: ImageBitmap? = remember(photoUrl) {
        decodePhotoUrlToImageBitmap(context, photoUrl)
    }

    val boxModifier = modifier
        .size(size)
        .then(
            if (onClick != null) {
                Modifier.clickable { onClick() }
            } else {
                Modifier
            }
        )

    Box(modifier = boxModifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(backgroundColor, shape = CircleShape)
                .border(1.5.dp, contentColor.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (decodedBitmap != null) {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = "Foto Profil $displayName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (displayName.isNotBlank()) {
                val initials = displayName
                    .trim()
                    .split("\\s+".toRegex())
                    .filter { it.isNotEmpty() && it[0].isLetter() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifEmpty { "U" }

                Text(
                    text = initials,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp
                )
            } else {
                Icon(
                    imageVector = fallbackIcon ?: Icons.Default.Person,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }

        if (showEditBadge) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Ubah Profil",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

/**
 * Dialog for Principal (Kepala Sekolah) and Teacher (Guru) to upload their profile photo,
 * edit their display name, and configure the school name shown in the dashboard header.
 */
@Composable
fun ProfileSettingsDialog(
    currentUser: User,
    userRepository: UserRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var displayName by remember(currentUser.id) { mutableStateOf(currentUser.displayName) }
    var schoolName by remember(currentUser.id) { mutableStateOf(currentUser.schoolName) }
    var managedSchoolsInput by remember(currentUser.id) {
        mutableStateOf(currentUser.effectiveSchools.joinToString(", "))
    }
    var subjectSpecialty by remember(currentUser.id) { mutableStateOf(currentUser.subjectSpecialty) }
    var managedClassesInput by remember(currentUser.id) {
        mutableStateOf(currentUser.effectiveClasses.joinToString(", "))
    }
    var photoDataUrl by remember(currentUser.id) { mutableStateOf(currentUser.photoUrl) }
    var isSaving by remember { mutableStateOf(false) }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    val accentColor = if (currentUser.isPrincipal) PrincipalTeal else IndigoPrimary

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingPhoto = true
            coroutineScope.launch {
                val encoded = withContext(Dispatchers.IO) {
                    encodeUriToBase64DataUrl(context, uri)
                }
                isProcessingPhoto = false
                if (encoded != null) {
                    photoDataUrl = encoded
                    Toast.makeText(context, "Foto profil berhasil dipilih", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal memuat gambar, silakan coba foto lain", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        modifier = Modifier.testTag("profile_settings_dialog"),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accentColor.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pengaturan Profil & Sekolah",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentUser.isPrincipal) "Profil Kepala Sekolah & Daftar Sekolah" else "Profil Guru, Sekolah & Kelas Ampuan",
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Preview + Upload Actions
                UserAvatar(
                    photoUrl = photoDataUrl,
                    displayName = displayName.ifBlank { currentUser.displayName },
                    size = 84.dp,
                    backgroundColor = accentColor.copy(alpha = 0.15f),
                    contentColor = accentColor,
                    showEditBadge = true,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isProcessingPhoto) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("upload_profile_photo_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unggah Foto", fontSize = 12.sp)
                        }

                        if (photoDataUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = { photoDataUrl = "" },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("remove_profile_photo_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Hapus Foto",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hapus", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nama Lengkap & Gelar
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nama Lengkap & Gelar") },
                    placeholder = { Text("Contoh: Drs. H. Budi Santoso, M.Pd.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Nama Sekolah Utama
                OutlinedTextField(
                    value = schoolName,
                    onValueChange = { schoolName = it },
                    label = { Text("Nama Sekolah Utama (Header Dasbor)") },
                    placeholder = { Text("Contoh: SMA Negeri 1 Teladan") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_school_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Daftar Multi-Sekolah (Dipisahkan Koma)
                OutlinedTextField(
                    value = managedSchoolsInput,
                    onValueChange = { managedSchoolsInput = it },
                    label = { Text("Daftar Sekolah Binaan / Mengajar (Pisahkan Koma)") },
                    placeholder = { Text("Contoh: SMA Negeri 1 Teladan, SMA Negeri 2 Harapan") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_managed_schools_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (currentUser.isTeacher) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = subjectSpecialty,
                        onValueChange = { subjectSpecialty = it },
                        label = { Text("Mata Pelajaran / Bidang Ampuan") },
                        placeholder = { Text("Contoh: Matematika & Fisika") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_subject_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = managedClassesInput,
                        onValueChange = { managedClassesInput = it.uppercase() },
                        label = { Text("Daftar Kelas yang Diampu (Pisahkan Koma)") },
                        placeholder = { Text("Contoh: 10-IPA-1, 10-IPA-2, 11-IPA-1, 12-IPS-1") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_managed_classes_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (displayName.isBlank() || schoolName.isBlank()) {
                        Toast.makeText(context, "Nama lengkap dan nama sekolah wajib diisi", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSaving = true
                    coroutineScope.launch {
                        try {
                            val parsedSchools = buildList {
                                add(schoolName.trim())
                                managedSchoolsInput.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .forEach { add(it) }
                            }.distinct()

                            val parsedClasses = if (currentUser.isTeacher) {
                                managedClassesInput.split(",")
                                    .map { it.trim().uppercase() }
                                    .filter { it.isNotEmpty() }
                                    .distinct()
                            } else null

                            userRepository.updateUserProfileAndSchool(
                                userId = currentUser.id,
                                schoolId = currentUser.schoolId,
                                displayName = displayName,
                                schoolName = schoolName,
                                photoUrl = photoDataUrl,
                                subjectSpecialty = subjectSpecialty,
                                managedSchools = parsedSchools,
                                managedClasses = parsedClasses
                            )
                            isSaving = false
                            Toast.makeText(context, "Profil, sekolah, dan kelas berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } catch (e: Exception) {
                            isSaving = false
                            Toast.makeText(context, "Gagal menyimpan perubahan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isSaving && !isProcessingPhoto,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_profile_settings_btn")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Simpan Perubahan")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
                modifier = Modifier.testTag("cancel_profile_settings_btn")
            ) {
                Text("Batal")
            }
        }
    )
}

private fun decodePhotoUrlToImageBitmap(context: Context, photoUrl: String): ImageBitmap? {
    if (photoUrl.isBlank()) return null
    return try {
        when {
            photoUrl.startsWith("data:image") -> {
                val base64Part = photoUrl.substringAfter("base64,", "")
                if (base64Part.isBlank()) return null
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
            photoUrl.startsWith("content://") || photoUrl.startsWith("file://") -> {
                val uri = Uri.parse(photoUrl)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun encodeUriToBase64DataUrl(context: Context, uri: Uri): String? {
    return try {
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        // Center crop to square
        val dimension = min(originalBitmap.width, originalBitmap.height)
        val xOffset = (originalBitmap.width - dimension) / 2
        val yOffset = (originalBitmap.height - dimension) / 2
        val cropped = Bitmap.createBitmap(originalBitmap, xOffset, yOffset, dimension, dimension)

        // Resize to compact 180x180 avatar
        val targetSize = 180
        val scaled = if (dimension > targetSize) {
            Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
        } else {
            cropped
        }

        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 78, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        "data:image/jpeg;base64,$base64String"
    } catch (_: Exception) {
        null
    }
}
