# Tes

| Key | Value |
|---|---|
| Package | `com.ccompile.lite` |
| Version | `1.0` |
| Min SDK | API 28 |
| Language | Kotlin |

## Library yang Di-include
- RecyclerView + `MyAdapter` + `item_list.xml`
- Retrofit 2 + `ApiClient` + `ApiService`
- Room Database + `User` + `UserDao` + `AppDatabase`
- ViewModel + LiveData (`MainViewModel`)
- Glide (image loading)
- Bottom Navigation + Nav Component (Home/Dashboard/Profile)
- ProGuard (release build)

## Cara Buka di Android Studio

1. Ekstrak ZIP ini
2. Buka **Android Studio** → `File > Open` → pilih folder `Tes/`
3. Tunggu Gradle sync selesai
4. Edit `app/src/main/java/com.ccompile.lite/MainActivity.kt`
5. Edit layout di `app/src/main/res/layout/activity_main.xml`
6. Run di emulator atau HP

## Build via GitHub Actions (No PC)

1. Buat repo baru di github.com
2. Upload/push isi folder `Tes/` ke branch `main`
3. GitHub Actions akan otomatis build APK
4. Download APK di tab **Actions → Artifacts**

> **Debug APK**: `Tes-debug`  
> **Release APK**: `Tes-release-unsigned`

## ⚠️ Catatan Room Database

`fallbackToDestructiveMigration()` akan **menghapus semua data**
saat schema database berubah. Ganti dengan migrasi yang tepat
sebelum deploy ke production.

## Struktur File

```
Tes/
├── app/src/main/
│   ├── java/com.ccompile.lite/
│   │   ├── MainActivity.kt
│   │   ├── MyAdapter.kt
│   │   ├── ApiClient.kt + ApiService.kt
│   │   ├── User.kt + UserDao.kt + AppDatabase.kt
│   │   ├── MainViewModel.kt
│   │   ├── HomeFragment.kt + DashboardFragment + ProfileFragment
│   └── res/layout/activity_main.xml
├── .github/workflows/build.yml
├── .gitignore
└── app/build.gradle
```
