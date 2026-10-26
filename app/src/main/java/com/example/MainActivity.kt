package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.NudgeRepository
import com.example.data.SchoolRepository
import com.example.data.TaskRepository
import com.example.data.UserRepository
import com.example.data.local.AppRoomDatabase
import com.example.data.local.LocalTaskRepository
import com.example.model.UserRole
import com.example.model.UserStatus
import com.example.ui.components.ProfileSettingsDialog
import com.example.ui.components.UserAvatar
import com.example.ui.screens.AdminApprovalScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.PendingApprovalScreen
import com.example.ui.screens.PrincipalDashboardScreen
import com.example.ui.screens.StudentHomeScreen
import com.example.ui.screens.TeacherDashboardScreen
import com.example.ui.theme.ColekTheme
import com.example.ui.theme.OrangeFlame
import com.example.ui.theme.PrincipalTeal
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.ui.components.ColekAiAssistantDialog
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            ColekTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val context = LocalContext.current

    // Request POST_NOTIFICATIONS for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val schoolRepository = remember { SchoolRepository(context) }
    val userRepository = remember { UserRepository(context) }
    val taskRepository = remember { TaskRepository(context) }
    val nudgeRepository = remember { NudgeRepository(context) }
    val localTaskRepository = remember {
        LocalTaskRepository(AppRoomDatabase.getDatabase(context).taskDao())
    }

    var authUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val nextUser = auth.currentUser
            if (authUser?.uid != nextUser?.uid) {
                authUser = nextUser
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val currentUid = authUser?.uid

    if (currentUid == null) {
        AuthScreen(
            schoolRepository = schoolRepository,
            userRepository = userRepository,
            onAuthComplete = { authUser = Firebase.auth.currentUser }
        )
        return
    }

    val profileFlow = remember(currentUid, userRepository) {
        userRepository.observeUserProfileState(currentUid)
    }
    val profileState by profileFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.ProfileState.Loading
    )

    val user = when (val state = profileState) {
        is com.example.data.ProfileState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
            return
        }
        is com.example.data.ProfileState.Success -> {
            val u = state.user
            if (u == null || u.schoolId.isBlank()) {
                AuthScreen(
                    schoolRepository = schoolRepository,
                    userRepository = userRepository,
                    onAuthComplete = { /* state will update from Firestore */ }
                )
                return
            }
            u
        }
    }

    // Pending approval screen
    if (user.status == UserStatus.PENDING.name) {
        PendingApprovalScreen(
            user = user,
            onRefreshStatus = { /* Flow automatically updates */ },
            onSignOut = { Firebase.auth.signOut() }
        )
        return
    }

    // Authenticated & Approved member screen!
    var selectedNavTab by remember { mutableIntStateOf(0) }
    var showProfileSettingsDialog by remember { mutableStateOf(false) }
    var showAiAssistantDialog by remember { mutableStateOf(false) }

    val hasAdminTabs = user.isPrincipal || user.isAdmin

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            photoUrl = user.photoUrl,
                            displayName = user.displayName,
                            size = 38.dp,
                            backgroundColor = if (user.isPrincipal) PrincipalTeal else MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            fallbackIcon = Icons.Default.School,
                            showEditBadge = user.isPrincipal || user.isTeacher,
                            onClick = { showProfileSettingsDialog = true },
                            modifier = Modifier.testTag("top_bar_avatar")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = user.schoolName.ifBlank { "Colek Sekolah" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "${user.displayName.ifBlank { "Pengguna" }} • ${user.role}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiAssistantDialog = true },
                        modifier = Modifier.testTag("top_bar_ai_assistant_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Asisten AI Gemini",
                            tint = if (user.isPrincipal) PrincipalTeal else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (user.isPrincipal || user.isTeacher) {
                        IconButton(
                            onClick = { showProfileSettingsDialog = true },
                            modifier = Modifier.testTag("top_bar_profile_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan Profil & Sekolah",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { Firebase.auth.signOut() }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Keluar Akun",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (hasAdminTabs) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedNavTab == 0,
                        onClick = { selectedNavTab = 0 },
                        icon = { Icon(Icons.Default.SupervisedUserCircle, contentDescription = null) },
                        label = { Text("Supervisi Guru") }
                    )
                    NavigationBarItem(
                        selected = selectedNavTab == 1,
                        onClick = { selectedNavTab = 1 },
                        icon = { Icon(Icons.Default.Group, contentDescription = null) },
                        label = { Text("Persetujuan") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                user.isStudent -> {
                    StudentHomeScreen(
                        currentUser = user,
                        taskRepository = taskRepository,
                        nudgeRepository = nudgeRepository,
                        userRepository = userRepository,
                        localTaskRepository = localTaskRepository
                    )
                }
                user.isTeacher -> {
                    TeacherDashboardScreen(
                        currentUser = user,
                        taskRepository = taskRepository,
                        nudgeRepository = nudgeRepository,
                        userRepository = userRepository,
                        localTaskRepository = localTaskRepository
                    )
                }
                user.isPrincipal || user.isAdmin -> {
                    if (selectedNavTab == 0) {
                        PrincipalDashboardScreen(
                            currentUser = user,
                            taskRepository = taskRepository,
                            userRepository = userRepository,
                            nudgeRepository = nudgeRepository,
                            localTaskRepository = localTaskRepository
                        )
                    } else {
                        AdminApprovalScreen(
                            currentUser = user,
                            userRepository = userRepository
                        )
                    }
                }
                else -> {
                    StudentHomeScreen(
                        currentUser = user,
                        taskRepository = taskRepository,
                        nudgeRepository = nudgeRepository,
                        userRepository = userRepository,
                        localTaskRepository = localTaskRepository
                    )
                }
            }
        }
    }

    if (showProfileSettingsDialog) {
        ProfileSettingsDialog(
            currentUser = user,
            userRepository = userRepository,
            onDismiss = { showProfileSettingsDialog = false }
        )
    }

    if (showAiAssistantDialog) {
        ColekAiAssistantDialog(
            currentUser = user,
            taskRepository = taskRepository,
            localTaskRepository = localTaskRepository,
            onDismiss = { showAiAssistantDialog = false }
        )
    }
}
