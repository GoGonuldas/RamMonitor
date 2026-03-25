# 📱 RAM Monitor — Android Uygulaması

Anlık RAM izleme, uygulama bazlı bellek listesi, geçmiş grafik ve yüksek RAM uyarısı içeren tam özellikli Android uygulaması.

---

## 🚀 Kurulum

### Gereksinimler
- Android Studio Hedgehog (2023.1) veya üzeri
- Android SDK 26+ (Android 8.0)
- Kotlin 1.9+
- JDK 17

### Adımlar

1. **Projeyi açın**
   ```
   Android Studio → File → Open → RamMonitor klasörünü seçin
   ```

2. **Gradle sync edin**
   - Android Studio otomatik olarak sync önerir, "Sync Now" tıklayın
   - Veya: File → Sync Project with Gradle Files

3. **Telefonu bağlayın**
   - USB Hata Ayıklama açık olmalı (Geliştirici Seçenekleri)
   - `adb devices` ile tanındığını doğrulayın

4. **Çalıştırın**
   - Yeşil ▶ Run butonuna basın
   - Hedef cihazı seçin

---

## 🎯 Özellikler

| Özellik | Açıklama |
|---------|----------|
| 📊 Anlık RAM | Her 3 saniyede güncellenen canlı % göstergesi ve çizgi grafik |
| 📱 Uygulama Listesi | Çalışan tüm uygulamaların PSS/USS bellek kullanımı |
| 📈 Geçmiş | Son 100 ölçümün bar grafiği, min/max/ortalama istatistikleri |
| 🔔 Bildirim | RAM %85 üzerine çıkınca uyarı bildirimi |
| 🔄 Arka plan | Foreground Service ile ekran kapalıyken de çalışır |
| 💾 Veritabanı | Room DB ile 24 saatlik geçmiş saklanır |

---

## 📂 Proje Yapısı

```
app/src/main/java/com/rammonitor/
├── data/
│   ├── RamInfo.kt          — Veri modelleri
│   ├── AppDatabase.kt      — Room veritabanı
│   └── RamRepository.kt    — RAM okuma & veri erişim katmanı
├── service/
│   ├── RamMonitorService.kt — Foreground service (3s interval)
│   └── BootReceiver.kt      — Telefon açılışında otomatik başlatma
└── ui/
    ├── MainActivity.kt      — Ana ekran (TabLayout + ViewPager2)
    ├── MainViewModel.kt     — UI veri yönetimi
    ├── DashboardFragment.kt — Canlı RAM ekranı
    ├── AppListFragment.kt   — Uygulama RAM listesi
    └── HistoryFragment.kt   — Geçmiş istatistikleri
```

---

## ⚠️ İzinler

Uygulama aşağıdaki izinlere ihtiyaç duyar (tümü otomatik verilir, kullanıcı onayı gerektirmez):

- `FOREGROUND_SERVICE` — Arka planda çalışma
- `POST_NOTIFICATIONS` — Android 13+ bildirim izni (açılışta sorulur)
- `RECEIVE_BOOT_COMPLETED` — Telefon açılışında otomatik başlama

---

## 🎨 Tema

Koyu lacivert arka plan (#0D0D1A) üzerine neon cyan (#00E5FF) vurgulu, terminal/sistem monitörü estetiği.

---

## 🔧 Ayarlar

`RamMonitorService.kt` içindeki sabitleri değiştirebilirsiniz:

```kotlin
const val ALERT_THRESHOLD = 85f   // Uyarı eşiği (%)
```

`RamRepository.kt` içinde:
```kotlin
delay(3000) // Güncelleme aralığı (ms)
```
