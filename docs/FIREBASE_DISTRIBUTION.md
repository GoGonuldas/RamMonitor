# Firebase App Distribution — Test Sürümü Gönderme

RamMonitor APK'yı testçilere göndermek için kullanılan akış. Firebase **SDK entegrasyonu gerektirmez** — sadece Firebase CLI ile App ID üzerinden çalışır.

## Önkoşullar

- **Firebase CLI**
  ```bash
  npm install -g firebase-tools
  # veya
  curl -sL https://firebase.tools | bash
  ```
- Firebase hesabına giriş:
  ```bash
  firebase login
  firebase login:list   # doğrulamak için
  ```
- Firebase projesinde **App Distribution** etkin olmalı:
  https://console.firebase.google.com/project/rammonitor-3f12034f/appdistribution

## Sabit Bilgiler

| Anahtar | Değer |
|---|---|
| Firebase Project | `rammonitor-3f12034f` |
| Android App ID | `1:1048044952881:android:58997e2e39339614e9a410` |
| Package | `com.rammonitor` |
| Keystore | `rammonitor.keystore` (alias: `rammonitor`) |

## Tek Komutla Gönderim

```bash
cd /Users/gorkangonuldas/workspace/RamMonitor

# 1) Release APK üret (imzalı)
./gradlew assembleRelease

# 2) Firebase App Distribution'a yükle + testçilere dağıt
firebase appdistribution:distribute \
  app/build/outputs/apk/release/app-release.apk \
  --app 1:1048044952881:android:58997e2e39339614e9a410 \
  --release-notes-file release_notes.txt \
  --testers "gorkan.gonuldas@gmail.com"
```

### Birden fazla testçiye gönderme
```bash
--testers "kisi1@gmail.com,kisi2@gmail.com,kisi3@gmail.com"
```

### Gruplara gönderme (Firebase Console > App Distribution > Testers and Groups)
```bash
--groups "internal-testers,beta"
```

### Release notes'u inline geçme
```bash
--release-notes "Quick bugfix build"
```

## Tek Adımlık Helper Script

`scripts/distribute.sh` (örnek):

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

APP_ID="1:1048044952881:android:58997e2e39339614e9a410"
TESTERS="${TESTERS:-grkngnlds@gmail.com}"

./gradlew assembleRelease

firebase appdistribution:distribute \
  app/build/outputs/apk/release/app-release.apk \
  --app "$APP_ID" \
  --release-notes-file release_notes.txt \
  --testers "$TESTERS"
```

Kullanım:
```bash
chmod +x scripts/distribute.sh
./scripts/distribute.sh
# veya farklı testçilerle:
TESTERS="ali@x.com,veli@x.com" ./scripts/distribute.sh
```

## Testçi Telefonunda Kurulum

1. Testçi e-postasına gelen **Firebase App Distribution** davetinde "Get started" butonuna bas.
2. Telefon tarayıcısı **Firebase App Tester** APK'sını indirmeyi önerir → kur ve aç.
3. Davet alınan **Google hesabı** ile App Tester'a giriş yap.
4. App Tester ana ekranında **RamMonitor** uygulamasını ve son sürümü gör → **Download → Install**.
5. "Unknown apps install" izni isterse onayla (sadece App Tester için, tek seferlik).

## Release Notes Dosyası

`release_notes.txt` her gönderimden önce güncellenmelidir. Şablon:

```
RAM Monitor vX.Y.Z (build N) - Test sürümü

Yenilikler:
- ...
- ...

Test edilecekler:
1. ...
2. ...
```

## Sürüm Numarası

Her yeni dağıtım için `app/build.gradle` içinde **versionCode** mutlaka artırılmalıdır;
aynı versionCode ile aynı App ID'ye ikinci yükleme **reddedilir**.

```groovy
defaultConfig {
    versionCode 10        // her gönderimde +1
    versionName "1.2.6"
}
```

## Faydalı Linkler

- Console: https://console.firebase.google.com/project/rammonitor-3f12034f/appdistribution
- Testçi paylaşım sayfası (her release için): komut çıktısındaki
  `https://appdistribution.firebase.google.com/testerapps/<APP_ID>/releases/<RELEASE_ID>`
- Firebase CLI dokümanı: https://firebase.google.com/docs/app-distribution/android/distribute-cli

## Sorun Giderme

| Sorun | Çözüm |
|---|---|
| `Failed to fetch app` | App ID yanlış veya hesabın proje yetkisi yok. `firebase projects:list` ile kontrol et. |
| `Tester not authorized` | Testçi e-posta App Distribution > Testers listesine eklenmemiş. Console'dan ekle veya `--testers` ile gönder. |
| Aynı versionCode hatası | `versionCode`'u artır ve `./gradlew assembleRelease` ile yeniden build et. |
| `unauthorized_client` | `firebase logout && firebase login` ile yeniden giriş yap. |
| APK boyutu büyük | Release build'de `minifyEnabled true` ve `shrinkResources true` zaten açık. |

## Üretim (Huawei AppGallery) ile Farkı

Firebase App Distribution **yalnızca dahili test** içindir.
Huawei AppGallery'ye yüklerken **AAB** kullanılır:

```bash
./gradlew bundleRelease
# app/build/outputs/bundle/release/app-release.aab
```

ProGuard/R8 sembol dosyası (mapping) Huawei yüklemesinde ayrıca eklenmelidir:
```
app/build/outputs/mapping/release/mapping.txt
```

