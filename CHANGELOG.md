# Sürüm notları

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
