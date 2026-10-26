package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SchoolRepository
import com.example.data.UserRepository
import com.example.model.User
import com.example.model.UserRole
import com.example.model.UserStatus
import com.example.ui.components.GoogleSignInButton
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OrangeFlame
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    schoolRepository: SchoolRepository,
    userRepository: UserRepository,
    onAuthComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser

    // If user is not yet logged in with Google:
    if (currentUser == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(IndigoPrimary.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Logo Colek",
                    tint = IndigoPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Colek",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Text(
                text = "Colek tugasmu sebelum alarm bunyi! ⏰🔥",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            GoogleSignInButton(
                onAuthSuccess = {
                    Toast.makeText(context, "Berhasil masuk Google!", Toast.LENGTH_SHORT).show()
                },
                onAuthError = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Isolasi data ketat multi-sekolah terlindungi UU PDP",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        return
    }

    // Google Sign-In done, check if pre-added by email or do onboarding:
    var isCheckingPreAdded by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser.uid) {
        val email = currentUser.email
        if (!email.isNullOrBlank()) {
            try {
                val linked = userRepository.checkAndLinkPreAddedUser(
                    uid = currentUser.uid,
                    email = email,
                    displayName = currentUser.displayName
                )
                if (linked != null) {
                    Toast.makeText(
                        context,
                        "Selamat datang! Akun terhubung ke ${linked.schoolName} (${linked.role})",
                        Toast.LENGTH_LONG
                    ).show()
                    onAuthComplete()
                    return@LaunchedEffect
                }
            } catch (e: Exception) {
                // Ignore and proceed to manual join
            }
        }
        isCheckingPreAdded = false
    }

    if (isCheckingPreAdded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Memeriksa undangan sekolah...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    var tabIndex by remember { mutableIntStateOf(0) }
    var inviteCode by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var className by remember { mutableStateOf("10-IPA-1") }
    var subjectSpecialty by remember { mutableStateOf("Matematika") }

    // For creating new school:
    var newSchoolName by remember { mutableStateOf("") }
    var newInviteCode by remember { mutableStateOf("") }
    var newPrincipalName by remember { mutableStateOf(currentUser.displayName ?: "") }
    var newCity by remember { mutableStateOf("Jakarta") }

    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Selamat Datang, ${currentUser.displayName ?: "Pengguna"}!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Pilih sekolah untuk menghubungkan tugas dan alarm Anda.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        TabRow(selectedTabIndex = tabIndex) {
            Tab(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                text = { Text("Gabung Sekolah") },
                icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) }
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                text = { Text("Daftar Sekolah Baru") },
                icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (tabIndex == 0) {
            // Join School Form
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Kode Undangan Sekolah",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it.uppercase() },
                        placeholder = { Text("Contoh: SMAN1-BDG-2026") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("school_invite_code_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Peran Anda di Sekolah:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedRole == UserRole.STUDENT,
                            onClick = { selectedRole = UserRole.STUDENT },
                            label = { Text("Siswa") }
                        )
                        FilterChip(
                            selected = selectedRole == UserRole.TEACHER,
                            onClick = { selectedRole = UserRole.TEACHER },
                            label = { Text("Guru") }
                        )
                        FilterChip(
                            selected = selectedRole == UserRole.PRINCIPAL,
                            onClick = { selectedRole = UserRole.PRINCIPAL },
                            label = { Text("Kepala Sekolah") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (selectedRole) {
                        UserRole.STUDENT -> {
                            OutlinedTextField(
                                value = className,
                                onValueChange = { className = it },
                                label = { Text("Kelas (contoh: 10-IPA-1)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        UserRole.TEACHER -> {
                            OutlinedTextField(
                                value = subjectSpecialty,
                                onValueChange = { subjectSpecialty = it },
                                label = { Text("Mata Pelajaran yang Diampu") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        UserRole.PRINCIPAL -> {
                            Text(
                                text = "Kepala Sekolah dapat membuat tugas administrasi guru dan memantau kinerja sekolah.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (inviteCode.isBlank()) {
                                Toast.makeText(context, "Masukkan kode undangan sekolah", Toast.LENGTH_SHORT).show()
                            } else {
                                isLoading = true
                                coroutineScope.launch {
                                    val school = schoolRepository.findSchoolByInviteCode(inviteCode)
                                    if (school == null) {
                                        isLoading = false
                                        Toast.makeText(context, "Sekolah dengan kode tersebut tidak ditemukan!", Toast.LENGTH_LONG).show()
                                        return@launch
                                    }

                                    val user = User(
                                        id = currentUser.uid,
                                        email = currentUser.email ?: "",
                                        displayName = currentUser.displayName ?: "Pengguna",
                                        photoUrl = currentUser.photoUrl?.toString() ?: "",
                                        schoolId = school.id,
                                        schoolName = school.name,
                                        role = selectedRole.name,
                                        status = UserStatus.PENDING.name, // Requires approval by school admin/principal
                                        className = className,
                                        subjectSpecialty = subjectSpecialty,
                                        streakCount = 1L
                                    )
                                    userRepository.saveUserProfile(user)
                                    isLoading = false
                                    onAuthComplete()
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("join_school_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Ajukan Bergabung 🚀")
                        }
                    }
                }
            }
        } else {
            // Register New School Form (For Principal / Admin)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Daftarkan Sekolah Baru",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newSchoolName,
                        onValueChange = { newSchoolName = it },
                        label = { Text("Nama Sekolah (e.g. SMAN 1 Jakarta)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newInviteCode,
                        onValueChange = { newInviteCode = it.uppercase() },
                        label = { Text("Buat Kode Undangan Unik (e.g. SMAN1-JKT)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newPrincipalName,
                        onValueChange = { newPrincipalName = it },
                        label = { Text("Nama Kepala Sekolah") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newCity,
                        onValueChange = { newCity = it },
                        label = { Text("Kota / Kabupaten") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (newSchoolName.isBlank() || newInviteCode.isBlank()) {
                                Toast.makeText(context, "Lengkapi nama sekolah dan kode undangan", Toast.LENGTH_SHORT).show()
                            } else {
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val schoolId = schoolRepository.createSchool(
                                            name = newSchoolName,
                                            inviteCode = newInviteCode,
                                            principalName = newPrincipalName,
                                            city = newCity
                                        )
                                        // Pendaftar sekolah baru langsung aktif sebagai PRINCIPAL
                                        val user = User(
                                            id = currentUser.uid,
                                            email = currentUser.email ?: "",
                                            displayName = newPrincipalName.ifBlank { currentUser.displayName ?: "Kepala Sekolah" },
                                            photoUrl = currentUser.photoUrl?.toString() ?: "",
                                            schoolId = schoolId,
                                            schoolName = newSchoolName,
                                            role = UserRole.PRINCIPAL.name,
                                            status = UserStatus.ACTIVE.name, // Auto-active as school founder
                                            streakCount = 1L
                                        )
                                        userRepository.saveUserProfile(user)
                                        isLoading = false
                                        Toast.makeText(context, "Sekolah berhasil didaftarkan!", Toast.LENGTH_SHORT).show()
                                        onAuthComplete()
                                    } catch (e: Exception) {
                                        isLoading = false
                                        Toast.makeText(context, "Gagal membuat sekolah: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("create_school_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Daftarkan & Jadi Kepala Sekolah 🏫")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                Firebase.auth.signOut()
            }
        ) {
            Text("Ganti Akun Google", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
