# Sholat Tracker — Android App

Aplikasi tracker sholat harian untuk Android, dibuat dengan Kotlin + Material Design 3.

---

## Fitur
- ✅ Checkbox 5 waktu sholat harian
- 🔥 Streak tracker (hari berturut-turut sholat lengkap)
- 🔔 Notifikasi pengingat otomatis per waktu sholat
- 📋 Riwayat sholat harian
- 📄 Export PDF laporan harian
- 🟩 **Widget homescreen 2x2** — progress, dot 5 sholat, sholat berikutnya

---

## Cara Import ke Android Studio

### 1. Buka Project
- Buka **Android Studio**
- Pilih **File → Open**
- Arahkan ke folder `SholatTracker` ini → klik **OK**
- Tunggu Gradle sync selesai

### 2. Download Font (wajib)
Download font **Plus Jakarta Sans** dari Google Fonts dan taruh di `app/src/main/res/font/`:
- https://fonts.google.com/specimen/Plus+Jakarta+Sans
- Download `PlusJakartaSans-Regular.ttf` → rename jadi `plus_jakarta_sans_regular.ttf`
- Download `PlusJakartaSans-SemiBold.ttf` → rename jadi `plus_jakarta_sans_semibold.ttf`

Atau hapus referensi font di layout (ganti `@font/plus_jakarta_sans` dengan `sans-serif`)
agar bisa langsung build tanpa download font.

### 3. Launcher Icon
Untuk icon yang bagus, gunakan Android Studio:
- Klik kanan folder `res` → **New → Image Asset**
- Pilih icon yang kamu suka → Generate

### 4. Build & Install
- Sambungkan HP Android via USB (aktifkan Developer Mode & USB Debugging)
- Klik tombol **Run ▶** di Android Studio
- Atau: **Build → Build Bundle(s)/APK(s) → Build APK(s)**
- APK ada di: `app/build/outputs/apk/debug/app-debug.apk`

---

## Persyaratan
- Android 8.0 (API 26) ke atas
- Android Studio Hedgehog (2023.1) atau lebih baru
- JDK 8+

---

## Catatan Penting

### Notifikasi
- Saat pertama buka app, akan minta izin notifikasi → **Izinkan**
- Setelah itu ketuk ikon 🔔 di toolbar untuk mengatur jam
- Notifikasi akan tetap aktif meski app ditutup (pakai AlarmManager)
- Setelah HP restart, alarm otomatis dijadwal ulang (via BOOT_COMPLETED)

### Penyimpanan Data
- Data disimpan di `SharedPreferences` (internal storage HP)
- Tidak perlu internet
- Data aman selama app tidak di-uninstall

### PDF Export
- PDF digenerate di internal storage lalu langsung muncul dialog share
- Bisa share via WhatsApp, email, dll

---

## Cara Pasang Widget di Homescreen

Setelah app terinstall:
1. Long press area kosong di homescreen Android
2. Pilih **Widget**
3. Cari **Sholat Tracker**
4. Drag widget 2x2 ke homescreen
5. Widget langsung menampilkan progress & sholat berikutnya

Widget otomatis update saat kamu centang sholat di dalam app.

---

## Struktur Project
```
app/src/main/
├── java/com/sholattracker/app/
│   ├── ui/
│   │   ├── MainActivity.kt          ← Halaman utama
│   │   ├── SholatAdapter.kt         ← List sholat
│   │   ├── HistoryActivity.kt       ← Riwayat
│   │   └── NotificationSettingsActivity.kt ← Atur pengingat
│   ├── data/
│   │   └── SholatRepository.kt      ← Data & storage
│   ├── notification/
│   │   └── NotificationScheduler.kt ← Alarm & notifikasi
│   └── pdf/
│       └── PdfExporter.kt           ← Generate PDF
└── res/
    ├── layout/                      ← Semua layout XML
    ├── values/colors.xml            ← Tema warna gelap
    └── drawable/                    ← Icon & shapes
```
