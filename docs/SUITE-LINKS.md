# BetterAdvancements canlı suite hedefleri

Ortak ayarlar [SUITE-INTEGRATIONS.md](SUITE-INTEGRATIONS.md) içinde açıklanır. Kaynak/olay eşleşmeleri normal AchievementManager ve ChallengeManager yollarını kullanır; yönetici grant çağrılmaz. Bağımlılıklar, hedef sınırları, kalıcı ödül teslimi ve aktif görev döngüleri korunur. Tarihi kayıtlar taranmaz ve offline başarı telafisi yapılmaz.

| `suite-integrations.features` anahtarı | Varsayılan | Davranış |
| --- | --- | --- |
| `betteradvancements-milestones` | `true` | Kaynak+olay koşulu taşıyan CUSTOM başarımları ve aktif görevleri ilerletir. |
| `betteradvancements-ignore-synthetic-progress` | `true` | NPC işçinin gerçek blok kırmadan yaptığı koruma sorgusunu BLOCK_BREAK saymaz. |
| `betteradvancements-count-managed-mob-kills` | `false` | Gerçek suite boss/zindan/hayvan ölümlerinin normal MOB_KILL hedeflerine de sayılmasını açar. |

NPC işçiler ve `NPC` metadata'sı taşıyan varlıklar her durumda hariçtir. Katil ve oyuncu hedefi Bukkit'e bağlı gerçek oyuncu olarak doğrulanır; sahte Player sayılmaz. İptal edilen son hasar veya sonradan iptal edilen/resurrection ölüm olayı ilerleme vermez. Ölümün son durumu sonraki tick'te doğrulanır.

## CUSTOM koşulları

```yaml
trigger:
  type: CUSTOM
  target: 64
  conditions:
    source: SmartNPCWorkers
    event: worker_produced
    material: COBBLESTONE
```

Hem `source` hem `event` zorunludur. Genel/koşulsuz eski CUSTOM hedefleri suite bildirimiyle kendiliğinden ilerlemez. `amount` tamamlanmış işlemin miktarıdır; örnekte 64 kırık taş üretimi ister, 64 koruma sorgusu istemez. Ek düz alanlar yalnızca bildirimde mevcut ve eşleşiyorsa kabul edilir. Kaynak/olay adları ortak rehberde; her ürünün ayrıntı alanları kendi SUITE-LINKS rehberindedir.

## İsteğe bağlı örnek katalog

`suite-achievements.yml` on iki örnekle gelir ve **`enabled: false`** varsayılanını kullanır. Mevcut dosyanın üzerine yazılmaz; `achievements.yml` otomatik değiştirilmez. Açılan örnekler yalnızca bellekte mevcut katalogla birleştirilir. Başlık/açıklamalar mevcut on bir dilde yerelleştirilmiştir; değiştirilmiş özel metinler korunur.

Örnekler açıldığında normal oyuncu ilerlemesi ve prestige için ek hedefler olur. Tekrar kapatıldığında kayıtlı ilerleme ve kazanılmış puanlar silinmez. Yüzde, menü sayıları, prestige ve gösterilen tamamlanma değerleri yalnızca aktif tanım kimliklerini sayar. Oturum geçmişi geçmiş başarı olarak korunur. Liderlik tablosu SQL adaylarında da aktif kimliklerle filtreleme **LIMIT öncesinde** uygulanır; kapalı tanımlar başka oyuncuyu aday listesinden dışlayamaz. Reload eski katalog sonuçlarını temizler ve eski asenkron sorguyu yeni katalog üzerine yayımlamaz.

Etkin örneklerde mevcut kimlik, aynı tier'in dolu slotu, kenarlık/gezinme alanı, bilinmeyen tier, eksik/döngüsel bağımlılık, yanlış kaynak/olay, hedef ve ödül kontrol edilir. Tier envanteri 54 gerçek slotluk tek sayfadır; `gui.page` yalnızca `0` olabilir. Hazır yerleşimler beş varsayılan tier'in boş slotlarını kullanır; özelleştirilmiş layout için boş slot seçilir. Hatalı birleşim aktif katalog/dil/ana ayar snapshot'ı değişmeden reload'u reddeder.

## Odaklı kontroller

Beş JUnit kontrolü bağlam kaynak/miktar sınırlarını, hazır örneklerin gerçek hazır kataloğa sığmasını, kapalı örneklerin sahip kataloğunu değiştirmemesini, kapalı ilerlemenin saklanmasını ve hatalı slot/ödül/bağımlılığın reddini kapsar. Bir ek SQLite JUnit kontrolü iki oyunculu geçici veritabanında LIMIT öncesi filtrelemeyi, boş katalog/sezon koşullarını ve kazanılmış puan/eski ilerlemenin korunmasını denetler. Ayrı sunucu veya sağlayıcı sistemi kurulmaz. Çalıştırılan kanıt ve kapsam ortak doğrulama raporundadır.
