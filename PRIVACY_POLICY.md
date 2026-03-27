# Gizlilik Politikası / Privacy Policy — RAM Monitor

**Yürürlük tarihi / Effective date:** 27 Mart 2026 / March 27, 2026

---

## TR — Türkçe

### Genel Bakış
RAM Monitor, cihaz belleği ve ağ kullanım istatistiklerini kullanıcıya göstermek amacıyla geliştirilmiş bir izleme uygulamasıdır.

### Toplanan Veriler
RAM Monitor aşağıdaki kişisel verileri **toplamaz**:
- Ad, soyad, e-posta, telefon numarası
- Konum bilgisi
- Fotoğraf, kamera veya mikrofon verisi

Uygulama yalnızca aşağıdaki teknik verilere erişir:
- Cihaz RAM kullanım değerleri
- Yüklü uygulamaların kullanım istatistikleri (Usage Stats API)
- Uygulama bazlı ağ kullanım miktarları

### Verilerin Kullanımı
Teknik veriler yalnızca:
- Uygulama ekranlarında istatistik göstermek,
- Geçmiş grafikleri oluşturmak,
- İzleme deneyimini sağlamak

amacıyla kullanılır.

### Veri Paylaşımı
RAM Monitor hiçbir veriyi üçüncü taraflarla paylaşmaz, satmaz veya reklam/izleme amacıyla kullanmaz.

### Veri Depolama
Tüm veriler yalnızca cihaz üzerinde yerel olarak saklanır. Uygulama herhangi bir sunucuya veri göndermez.

### Kullanılan İzinler
| İzin | Amaç |
|---|---|
| `PACKAGE_USAGE_STATS` | Uygulama bazlı istatistikleri göstermek için |
| `FOREGROUND_SERVICE` | Arka planda kesintisiz izleme için |
| `POST_NOTIFICATIONS` | İzleme bildirimleri göstermek için |
| `RECEIVE_BOOT_COMPLETED` | Yeniden başlatma sonrası izlemeyi sürdürmek için |
| `INTERNET` | Genel Android ağ uyumluluğu için |

### İletişim
Gizlilik politikasıyla ilgili sorularınız için: **your-email@example.com**

---

## EN — English

### Overview
RAM Monitor is a monitoring app designed to display device memory and network usage statistics to the user.

### Data Collection
RAM Monitor does **not** collect the following personal data:
- Name, email, phone number
- Location data
- Photos, camera, or microphone data

The app accesses only the following technical data:
- Device RAM usage values
- App usage statistics (Usage Stats API)
- Per-app network usage totals

### How Data Is Used
Technical data is used only to:
- Display statistics in app screens,
- Generate usage history charts,
- Provide monitoring functionality.

### Data Sharing
RAM Monitor does not sell or share usage data with third parties for advertising or tracking.

### Data Storage
All data is stored locally on-device only. The app does not send data to any remote server.

### Permissions Used
| Permission | Purpose |
|---|---|
| `PACKAGE_USAGE_STATS` | To show per-app usage statistics |
| `FOREGROUND_SERVICE` | For continuous background monitoring |
| `POST_NOTIFICATIONS` | To display monitoring notifications |
| `RECEIVE_BOOT_COMPLETED` | To resume monitoring after device restart |
| `INTERNET` | General Android network compatibility |

### Contact
For privacy-related questions: **your-email@example.com**
