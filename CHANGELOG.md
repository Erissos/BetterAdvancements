# Sürüm notları

## 1.1.1-SNAPSHOT — yayımlanmamış düzeltmeler

- Vault hizmeti arayüz üzerinden ve UUID hesabıyla bağlanır; reddedilen para ödülleri kalıcı bekler, belirsiz ödemeler incelemeye alınır.
- Eşya/para ödülü makbuzları eski profilin yeniden yüklenmesinde ikinci teslimatı önler; açık sıfırlama yeni ödül dönemi açar.
- MySQL şema kontrolü yalnız bağlı veritabanını inceler; hatalı görev ödülü/saat dilimi ve ilerleme taşması korunur, tamamlanmamış görev ilerlemesi de kaydedilir.
- Türkçe mesajlar doğru Türkçe harfleri kullanır; eski varsayılan mesajlar yalnız birebir eşleşirse bellekte güncellenir. Özel mesaj dosyaları değiştirilmez.
- `storage.mysql.parameters` JDBC/TLS seçeneklerini ayarlar; ilk caching_sha2_password bağlantısına uygun varsayılanlar eklenir.

- Oyuncu dili 11 paketin menü başlıkları, düğmeleri, açıklamaları ve paketle gelen görev içeriklerine uygulanır; açık menü dil değişiminde yenilenir.
- Türkçe görev başlıkları ve açıklamaları, Minecraft eşya/yaratık/biyom adları; sezon ve rotasyon bildirimleri oyuncunun diline göre çözülür.
- Normal yürüyüşte kesirli mesafe birikir; ışınlanma, dünya değişimi, uçuş ve taşıtta mesafe sıfırlanır.
- Genel, sezon ve oturum sıralamaları büyükten küçüğe; oturum sıralaması yalnızca o oturumda tamamlanan görevleri kullanır.
- Profil yükleme/kayıt/sıfırlama sıralanır; kayıt değişmez kopyadan yapılır; dil kaydı başarısızsa tercih geri alınır.
- İptal edilmiş olaylar ilerlemeye sayılmaz; üretim gerçek envanter ve imleç miktarı farkından hesaplanır.
- Dolu envantere sığmayan eşya ödülleri kalıcı kutuda bekler; `/ba claim` ile alınır. Belirsiz teslimat tekrar verilmez, yönetici incelemesine bırakılır.
- Tamamlanmamış günlük/haftalık ilerleme yeni dönemde sıfırlanır.
- Çıkışta başarıyla kaydedilen profiller önbellekten çıkarılır; sıralama sorgusu sınırlıdır ve yalnız değişen ilerleme satırları yazılır.
- Menü sürükleme ve alt envanter tıklama koruması; bozuk YAML, negatif hedef/ödül ve eksik/döngüsel görev bağımlılığı denetimi.

## 1.1.0 — 3 Ekim 2026

- Paper 1.20.6–26.2 desteği; Paper 1.20.6 API, Java 21 bytecode ve `api-version: '1.20.6'` korunur.
- Oyuncuya özel, kalıcı dil tercihi; başka bir oyuncunun tercihini değiştirmez.
- Ortak `menu`, `help`, `language [kod]`, `reload` komutları; `lang` ve `locale` takma adları.
- Dil kodu verilmeden mevcut dil ve seçenekleri görüntüleme; kısa/bölge/tireli kod desteği; geçersiz kodda tercihi koruma.
- Konsoldan help/reload ve yetkiye göre tab tamamlama.
- Mevcut özelleştirilmiş dil dosyalarını koruyarak eksik mesajları paket içindeki çevirilerden tamamlama.
- Eski ses adları ve registry ses anahtarlarını destekleyen sürüm uyumlu ses çözümü.
- Mevcut oyuncu profilindeki dil alanı korunur; yeni profiller sunucunun varsayılan dilini kullanır.
- SQLite erişimi tek havuz bağlantısına alınarak eş zamanlı kayıtlarda `SQLITE_BUSY` hatası giderilir. Profil listesi bağlantısı alt kayıtlar yüklenmeden bırakılır. MySQL havuz ayarları korunur.
- GUI parlaması kaldırılan enchantment sabitinden bağımsızdır; adı bilinmeyen oyuncu başlıkları Paper 1.20.6'da güvenle oluşturulur.

Altı gerçek Paper sürümünde üç eklentiyi birlikte kapsayan 43 kontrol/sürüm geçti. Ayrıntılar: [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).
