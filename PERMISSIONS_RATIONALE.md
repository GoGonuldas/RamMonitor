# Permissions Rationale - RAM Monitor

Bu doküman, RAM Monitor uygulamasının kullandığı izinlerin neden gerekli olduğunu açıklar.

## Amaç
RAM Monitor, cihaz RAM kullanımını, uygulama bazlı bellek/ağ istatistiklerini ve geçmiş kullanım grafiğini göstermek için geliştirilmiştir.

## Kullanılan İzinler ve Gerekçeleri

### 1) `android.permission.PACKAGE_USAGE_STATS`
**Neden gerekli?**  
Uygulama bazlı kullanım istatistiklerini (RAM/ağ) gösterebilmek için gereklidir.

**Nerede kullanılır?**
- Uygulamalar sekmesi (app bazlı RAM listesi)
- Ağ sekmesi (24 saatlik ve canlı kullanım istatistikleri)
- Geçmiş/analiz ekranları

**Not:**  
Bu izin normal runtime izin değildir; kullanıcı tarafından sistem ayarlarından özel olarak verilir.

---

### 2) `android.permission.FOREGROUND_SERVICE`
**Neden gerekli?**  
RAM izleme işlemini uygulama arka plandayken güvenilir şekilde sürdürebilmek için gereklidir.

**Nerede kullanılır?**
- Sürekli RAM izleme servisi
- Durum bildirimi ile birlikte çalışan izleme akışı

---

### 3) `android.permission.FOREGROUND_SERVICE_DATA_SYNC`
**Neden gerekli?**  
Foreground service tipini Android’in modern gereksinimlerine uygun şekilde belirtmek için kullanılır.

**Nerede kullanılır?**
- RAM izleme servis tanımında (`foregroundServiceType="dataSync"`)

---

### 4) `android.permission.POST_NOTIFICATIONS`
**Neden gerekli?**  
Kullanıcıya RAM izleme durumunu ve önemli eşik bildirimlerini göstermek için gereklidir.

**Nerede kullanılır?**
- İzleme aktif bildirimi
- Yüksek RAM kullanımı uyarıları

**Not:**  
Android 13+ cihazlarda kullanıcıdan runtime olarak izin istenir.

---

### 5) `android.permission.RECEIVE_BOOT_COMPLETED`
**Neden gerekli?**  
Cihaz yeniden başlatıldıktan sonra izleme servisinin devam edebilmesi için gereklidir.

**Nerede kullanılır?**
- Boot receiver üzerinden servis başlatma akışı

---

### 6) `android.permission.INTERNET`
**Neden gerekli?**  
Uygulamanın ağla ilgili Android bileşenleriyle uyumlu çalışması için tanımlanmıştır.

**Nerede kullanılır?**
- Ağ istatistikleri/izleme altyapısı ile genel uyumluluk

---

## Veri Gizliliği ve Güvenlik
- Uygulama kişisel veri toplamaz (ad, e-posta, telefon, konum vb.).
- Veriler cihaz üzerinde işlenir.
- Veri satışı veya üçüncü taraflarla reklam amaçlı paylaşım yapılmaz.

## Kullanıcı Kontrolü
- Kullanıcı, kullanım erişimi iznini sistem ayarlarından dilediği zaman kapatabilir.
- Bildirim izni kullanıcı tarafından yönetilebilir.
- Uygulama izleme fonksiyonları izinlere bağlı olarak çalışır.
