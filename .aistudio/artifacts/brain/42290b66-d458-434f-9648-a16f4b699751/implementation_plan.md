# Colek: Pengingat Tugas Sekolah & Alarm Multi-Sekolah

Aplikasi pengingat tugas berbasis alarm yang terhubung secara multi-tenant antar-sekolah. Dilengkapi fitur saling mengingatkan ("Colek") antar-siswa, supervisi tugas guru oleh Kepala Sekolah, sistem alarm Android prioritas tinggi dengan snooze, streak kedisiplinan ala Duolingo, dan dashboard rekap analitik komprehensif.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Keputusan penting & revisi yang telah diintegrasikan:
> - **Nama Aplikasi Resmi:** **Colek** (slogan: *"Colek tugasmu sebelum alarm bunyi!"*).
> - **Peran Baru (Kepala Sekolah):** Kepala Sekolah memiliki wewenang khusus untuk mendistribusikan tugas administratif/akademik kepada para Guru (seperti Modul Ajar/RPP, Penginputan Nilai Raport, Dokumen Evaluasi), memantau kepatuhan guru, dan mengirimkan fitur **"Colek Guru"** yang santun dan berbobot profesional.
> - **Autentikasi:** Google Sign-In terintegrasi menggunakan Jetpack Credential Manager.
> - **Perilaku Alarm:** Notifikasi prioritas tinggi (`IMPORTANCE_HIGH` / `PRIORITY_MAX`) dengan nada dering kencang, tombol *Snooze* (tunda 10 menit), dan tombol cepat *Tandai Selesai* langsung dari status bar / lockscreen.
> - **Verifikasi Anggota Sekolah:** Anggota baru yang mendaftar dengan kode sekolah berstatus `PENDING` dan disetujui manual oleh Admin Sekolah / Kepala Sekolah.
> - **Basis Data:** Cloud Firebase Firestore dengan aturan isolasi multi-tenant ketat berbasis `schoolId` dan `userId`.

---

## 1. Overview & Core Concept

### Apa yang Dilakukan Aplikasi
Aplikasi **Colek** menyediakan ekosistem pengingat tugas berjenjang yang aman dan ceria di dalam satu sekolah:
1. **Kepala Sekolah ➔ Guru:** Mengingatkan administrasi sekolah, modul ajar, dan laporan evaluasi belajar.
2. **Guru ➔ Siswa:** Memberikan tugas mata pelajaran dengan batas waktu (deadline) presisi serta memantau tingkat penyelesaian.
3. **Antar Siswa (Peer-to-Peer):** Saling mencolek teman sekelas yang belum selesai mengerjakan tugas bersama dengan template santun & jenaka.
4. **Sistem Alarm Mandiri:** Alarm lokal Android yang berbunyi tepat waktu (H-1 hari, H-3 jam, H-30 menit, dan saat deadline) meskipun aplikasi sedang tertutup.

### Pengguna dan 5 Peran Hierarkis
1. **Super Admin:** Mendaftarkan institusi sekolah baru, mengaktifkan/menonaktifkan sekolah, serta membuat kode undangan awal (`school_code`).
2. **Kepala Sekolah (Principal):**
   - Membuat tugas administratif dan target kurikulum untuk seluruh dewan guru atau guru per rumpun mapel.
   - Memantau rekap ketepatan waktu pengumpulan dokumen guru.
   - Mengirimkan fitur *"Colek Guru"* secara terukur dan santun.
3. **Admin Sekolah:** Mengelola master data (daftar kelas, mata pelajaran), memverifikasi pendaftaran anggota baru (siswa/guru), dan audit log.
4. **Guru:**
   - Menerima & menyelesaikan tugas administratif dari Kepala Sekolah.
   - Membuat tugas untuk kelas & mata pelajaran yang diampu.
   - Melihat rekap ketepatan waktu siswa dan mencolek siswa yang belum selesai.
5. **Siswa:**
   - Menerima tugas kelas dan membuat catatan tugas pribadi.
   - Menyetel alarm pengingat, menjaga streak harian, dan mencolek teman sekelas.

---

## 2. User Experience & Visual Design

### Alur Pengguna & Fitur Utama (Key User Flows)

1. **Onboarding & Multi-Tenant Login:**
   - Masuk cepat dengan Google Sign-In.
   - Masukkan kode sekolah unik (contoh: `SMAN1-BDG-2026`).
   - Pilih peran yang diajukan (Siswa + Kelas, Guru + Rumpun Mapel, atau Kepala Sekolah).
   - Layar tunggu persetujuan yang informatif dan ramah.

2. **Dashboard Kepala Sekolah (Supervisi & Colek Guru):**
   - Metrik Utama: Persentase kesiapan administrasi guru sekolah (% Modul Ajar terkumpul, % Nilai terinput).
   - Tombol Buat Tugas Guru: Judul (misal: "Batas Akhir Upload Nilai PTS"), Tenggat Waktu, Kategori (Kurikulum / Kesiswaan / Administrasi), dan Target Rumpun Guru.
   - Matriks Guru: Daftar guru dengan status penyelesaian tugas (Belum / Selesai / Terlambat).
   - Aksi "Colek Guru": Template pesan profesional & santun, contoh:
     - *"Bapak/Ibu Guru yang terhormat, pengingat ramah untuk pengumpulan Modul Ajar sebelum hari Jumat demi kelancaran akreditasi sekolah 🙏"*
     - *"Semangat Bapak/Ibu! Batas penginputan nilai PTS tersisa 2 hari lagi. Terima kasih atas dedikasinya ✨"*

3. **Dashboard Guru:**
   - Tab 1: **Tugas Mengajar (Kepada Siswa)** — Buat tugas kelas, pantau progres siswa, kirim pengingat kelas.
   - Tab 2: **Tugas Administrasi (Dari Kepala Sekolah)** — Cek deadline dokumen sekolah, centang selesai, lampirkan catatan.

4. **Dashboard Siswa (Playful & Berorientasi Alarm):**
   - Banner Streak Api Belajar 🔥 ala Duolingo dengan kata-kata penyemangat dinamis.
   - Daftar Tugas dengan filter: "Tenggat Dekat", "Tugas Kelas", "Tugas Pribadi", "Riwayat Selesai".
   - Kartu tugas interaktif: indikator prioritas (High, Med, Low), hitung mundur deadline, saklar alarm aktif, dan tombol "Colek Teman".
   - Notifikasi Alarm Android: Tampil sebagai Heads-Up Notification prioritas tinggi dengan bunyi kencang, tombol *Snooze 10 Menit*, dan tombol *Tandai Selesai*.

5. **Modal Colekan Santun (Anti-Spam & Anti-Bullying):**
   - Hanya menggunakan pilihan template kalimat ramah, suportif, dan jenaka (tidak ada input teks bebas sembarangan).
   - Pembatasan ketat: Maksimal 3 kali colekan per rekan per tugas per 24 jam.
   - Ada opsi "Mute Colekan" dan pengaturan jam tenang (Quiet Hours: 21.00 - 06.00).

### Identitas Visual & Desain UI
- **Nama Aplikasi:** **Colek**
- **Tema & Vibe:** Ceria (*Cheerful*), Modern, Bersahabat, dan Terstruktur rapi (*Clean Card Hierarchy*).
- **Palet Warna:**
  - *Primary:* Indigo & Electric Cobalt (`#4F46E5`, `#3B82F6`) — stabilitas & fokus belajar.
  - *Accent Flame:* Oranye Hangat & Kuning Keemasan (`#F97316`, `#F59E0B`) — simbol Streak dan Colekan.
  - *Success Emerald:* (`#10B981`) — tugas tuntas & ketepatan waktu.
  - *Executive Teal:* (`#0F766E`) — khusus aksen panel Kepala Sekolah untuk nuansa kepemimpinan yang berwibawa.

---

## 3. Key Product Decisions & Trade-Offs

### 1. Dua Jalur Penugasan dalam Satu Skema
- **Pendekatan:** Koleksi `tasks` mendukung tipe `scope`:
  - `STUDENT_TASK`: dibuat oleh Guru atau Siswa sendiri (target: siswa per kelas/pribadi).
  - `TEACHER_TASK`: dibuat oleh Kepala Sekolah (target: guru per mata pelajaran atau seluruh guru).
- **Alasan:** Menyatukan arsitektur alarm, pengingat, dan status penyelesaian tanpa perlu menduplikasi kode modul penugasan.

### 2. Nada Colekan Berjenjang (Role-Based Nudge Templates)
- **Colekan Siswa ➔ Siswa:** Lucu, kasual, gaya teman sebaya (*"Tugas Matematika kita nangis nih kalau belum disentuh 🥺"*).
- **Colekan Guru ➔ Siswa:** Mendidik, menyemangati (*"Jangan lupa selesaikan tugas Sejarah sebelum jam 5 sore ya Nak 📚"*).
- **Colekan Kepala Sekolah ➔ Guru:** Hormat, santun, dan fokus pada target sekolah (*"Bapak/Ibu, salam semangat. Mohon kesediaannya melengkapi administrasi RPP sebelum tenggat waktu 🙏"*).

### 3. Alarm Prioritas Tinggi (Exact Alarm Manager)
- Menggunakan `AlarmManager.setExactAndAllowWhileIdle()` untuk menjamin alarm tetap berdering saat ponsel tidur (Doze Mode).
- Integrasi `NotificationCompat.Action` langsung di status bar untuk *Snooze* dan *Mark Done*.

---

## 4. Technical Architecture & Data Strategy

### Diagram Arsitektur Berjenjang

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Aplikasi "Colek" (Compose UI)                   │
│  ┌──────────────┐  ┌─────────────────┐  ┌───────────────────────────┐  │
│  │ AuthScreen   │  │ StudentHomeScreen│ │ PrincipalDashboard        │  │
│  │ (Google SSO) │  │ (Tasks, Colekan)│ │ (Supervisi & Colek Guru)  │  │
│  └──────┬───────┘  └────────┬────────┘  └─────────────┬─────────────┘  │
│         │                   │                         │                │
│         │          ┌────────▼────────┐                │                │
│         │          │ TeacherDashboard│◄───────────────┘                │
│         │          │ (Tugas & Rekap) │                                 │
│         │          └────────┬────────┘                                 │
└─────────┼───────────────────┼──────────────────────────────────────────┘
          │                   │
┌─────────▼───────────────────▼──────────────────────────────────────────┐
│                           ViewModels                                   │
│    AuthViewModel  •  TaskViewModel  •  NudgeViewModel  •  SchoolViewModel│
└─────────┬───────────────────┬──────────────────────────────────────────┘
          │                   │
┌─────────▼───────────────────▼──────────────────────────────────────────┐
│                        Repository Layer                                │
│    SchoolRepository  •  TaskRepository  •  UserRepository  • NudgeRepo  │
└─────────┬───────────────────────────────┬──────────────────────────────┘
          │                               │
┌─────────▼───────────────┐     ┌─────────▼──────────────────────────────┐
│  Cloud Firestore (DB)   │     │        Android Native Services         │
│  - schools (tenants)    │     │  ┌──────────────────────────────────┐  │
│  - users (5 roles)      │     │  │ AlarmManager (Exact Alarm)       │  │
│  - tasks (scope & type) │     │  └──────────────────┬───────────────┘  │
│  - task_submissions     │     │                     │                  │
│  - nudges (role-scoped) │     │  ┌──────────────────▼───────────────┐  │
│  - audit_logs           │     │  │ ColekAlarmReceiver & NotifMgr    │  │
│                         │     │  │ (High-Priority + Snooze/Done)    │  │
└─────────────────────────┘     └────────────────────────────────────────┘
```

### Skema Entitas Firestore (Multi-Tenant & Role Aware)

1. **`schools`**
   - `id`: String (contoh: `SCH-SMAN1`)
   - `name`: String
   - `inviteCode`: String (unik)
   - `principalName`: String
   - `isActive`: Boolean
   - `createdAt`: Timestamp

2. **`users`**
   - `id`: String (`auth.uid`)
   - `schoolId`: String (kunci isolasi tenant)
   - `email`: String
   - `displayName`: String
   - `role`: String (`"STUDENT"`, `"TEACHER"`, `"PRINCIPAL"`, `"SCHOOL_ADMIN"`, `"SUPER_ADMIN"`)
   - `status`: String (`"PENDING"`, `"ACTIVE"`, `"SUSPENDED"`)
   - `className`: String? (untuk siswa, misal `"10-IPA-1"`)
   - `subjectSpecialty`: String? (untuk guru, misal `"Matematika"`)
   - `streakCount`: Long
   - `lastActiveDate`: String (`"YYYY-MM-DD"`)
   - `createdAt`: Timestamp

3. **`tasks`**
   - `id`: String
   - `schoolId`: String (kunci isolasi tenant)
   - `createdBy`: String (`userId` pembuat)
   - `creatorRole`: String (`"PRINCIPAL"`, `"TEACHER"`, `"STUDENT"`)
   - `scope`: String (`"TEACHER_TASK"`, `"STUDENT_TASK"`, `"PERSONAL_TASK"`)
   - `title`: String
   - `description`: String
   - `subjectOrCategory`: String (misal: `"Modul Ajar"`, `"Matematika"`)
   - `targetFilter`: String (contoh: `"ALL_TEACHERS"`, `"10-IPA-1"`, `"SELF"`)
   - `priority`: String (`"LOW"`, `"MEDIUM"`, `"HIGH"`)
   - `dueDate`: Timestamp
   - `reminderOffsets`: List<Long> (menit: `[1440, 180, 30, 0]`)
   - `createdAt`: Timestamp

4. **`task_submissions`**
   - `id`: String (`${taskId}_${userId}`)
   - `taskId`: String
   - `userId`: String
   - `schoolId`: String
   - `userRole`: String (`"TEACHER"`, `"STUDENT"`)
   - `status`: String (`"IN_PROGRESS"`, `"COMPLETED"`, `"LATE"`)
   - `completedAt`: Timestamp?
   - `isOntime`: Boolean

5. **`nudges` (Colekan Berjenjang)**
   - `id`: String
   - `schoolId`: String
   - `fromUserId`: String
   - `fromUserName`: String
   - `fromRole`: String
   - `toUserId`: String
   - `toUserName`: String
   - `toRole`: String
   - `nudgeType`: String (`"PEER_STUDENT"`, `"TEACHER_TO_STUDENT"`, `"PRINCIPAL_TO_TEACHER"`)
   - `taskId`: String
   - `taskTitle`: String
   - `templateText`: String
   - `sentAt`: Timestamp
   - `dateKey`: String (`"YYYY-MM-DD"`)

---

## 5. Rencana Tahapan Eksekusi (Roadmap)

- **Tahap 1: Setup Backend & Aturan Keamanan Multi-Tenant**
  - Setup Firebase Firestore & Auth Google Sign-In.
  - Penulisan `firebase-blueprint.json` & `firestore.rules` mencakup aturan akses Kepala Sekolah, Guru, Siswa, dan Admin.
  - Verifikasi keamanan aturan dengan pengujian JavaScript & Robolectric.
- **Tahap 2: Manajemen Peran & Bergabung ke Sekolah**
  - Alur masuk Google SSO + pendaftaran kode sekolah dengan pemilihan peran (Siswa / Guru / Kepala Sekolah).
  - Layar persetujuan anggota oleh Admin / Kepala Sekolah.
- **Tahap 3: Mesin Penugasan & Alarm Android Presisi**
  - Pembuatan tugas untuk Siswa (oleh Guru) dan tugas administrasi Guru (oleh Kepala Sekolah).
  - Integrasi `ColekAlarmScheduler` menggunakan `AlarmManager.setExactAndAllowWhileIdle()` dengan opsi notifikasi *Snooze 10 Menit* & *Tandai Selesai*.
- **Tahap 4: Sistem Colek Berjenjang & Streak Kedisiplinan**
  - Dialog "Colek Teman" (antar siswa) dan "Colek Guru" (oleh Kepala Sekolah) dengan template sopan & batas kuota harian.
  - Penghitung streak dan dorongan motivasi harian.
- **Tahap 5: Dashboard Supervisi Kepala Sekolah & Rekap Guru**
  - Panel kontrol Kepala Sekolah untuk memantau kelengkapan administrasi guru.
  - Panel analitik guru untuk melihat kepatuhan pengumpulan tugas kelas.
  - Verifikasi isolasi data antar-sekolah secara menyeluruh.
