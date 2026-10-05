# BetterAdvancements yapılandırması

Desperis ürünü · Geliştirici: **erisos** · [Resmî site](https://desperis.com) · Yapılandırma sürümü: **1.1.1-SNAPSHOT**.

Ana ayarlar [`config.yml`](../src/main/resources/config.yml) içinde Türkçe yorumlarla açıklanır. Paketlenmiş dosya yeni kurulumun örneğidir; çalışan sunucuda `plugins/BetterAdvancements/config.yml` düzenlenir. Dosyayı UTF-8 kaydedin ve girintide boşluk kullanın. YAML anahtarları, Material / EntityType / Sound kodları ve kalıcı kimlikler çevrilmez.

## Genel kurallar

- Her zaman değerin yanında yazılan birimi kullanın. `20 tick ≈ 1 saniye`, `1000 ms = 1 saniye`; sunucu gecikirse tick tabanlı süre de uzar.
- HEX renklerini tırnak içinde yazın; `#` tırnaksız olursa YAML yorumu başlayabilir.
- Düzenlemeden önce config ve önemli kalıcı verilerin yedeğini alın. Veritabanı türü, dosya adı, dünya veya içerik kimliği değişikliği kayıtlardaki veriyi otomatik taşımaz.
- Normal ayarlar için `/ba reload` kullanılır. Veritabanı / dünya altyapısı veya başka sağlayıcının kurulması gibi başlangıçla ilişkili değişikliklerden sonra kontrollü sunucu yeniden başlatması yapın.
- Mevcut dosya özelleştirmeleri korunur. Yeni varsayılanları görmek için paketlenmiş örneğe bakın; tüm configi silip yeniden oluşturmak kendi ayarlarınızı kaybettirir.

## Ürüne özgü ayarlar

`general.default-language` yeni oyuncu ve konsol için varsayılan dil kodudur; paketlenmiş `lang` dosyalarından birini kullanın. Oyuncunun kişisel dil kaydı ayrıca korunur. `general.cache-save-interval-seconds` ve `general.hidden-achievement-placeholder` eski uyumluluk alanlarıdır; bu sürümde ayrı bir kayıt zamanlayıcısı / gizli görünüm yolu bunları okumaz. Gizli başarı görünümü ilgili dil / GUI metinleri üzerinden gelir.

`prestige.bonus-points` prestij bonusunu, `prestige.reset-challenges` ise prestij sırasında meydan okuma ilerlemesinin sıfırlanıp sıfırlanmadığını belirler. Bunlar oyuncunun prestij işlemini değiştiren oyun kurallarıdır. `challenges.broadcast-rotation` günlük / haftalık görev değişimi duyurusunu yönetir.

`messages` içindeki duyurular MiniMessage biçimindedir. `{threshold}` gibi dinamik yer tutucuları silmeyin. `notifications` sohbet, başlık, aksiyon çubuğu, boss bar ve ses kanallarını ayrı ayrı açıp kapatır. Oyuncunun bildirim tercihi de ilgili kanalda dikkate alınabilir.

Veritabanı `database.yml`, menü düzeni `gui.yml`, başarı tanımları `achievements.yml`, görev tanımları `challenges.yml`, sezon tanımları `season.yml` içindedir. Bu dosyalar `config.yml` içinde yinelenmez. Başarı / görev ID değiştirmek yalnızca görünen metni değiştirmekten farklıdır; kayıtlı ilerlemenin hangi tanıma bağlandığını etkiler. Türkçe görünen metinleri `lang/tr.yml` içinde düzenleyin.

Ortak `integrations.checks.progress` yasaklı bölgelerdeki eylemlerden ilerleme / ödül kazanmayı denetleyen bağlantıyı kontrol eder. Blok, etkileşim ve savaş kaynaklı ilerleme ile menüdeki oyuncu tercihlerinin değiştirilmesi aynı işlem değildir. Ayrıntılı işlem eşlemesi entegrasyon rehberindedir.

## Koruma ve core entegrasyonları

`integrations` ortak bölümü Towny Advanced, GriefPrevention, WorldGuard, Lands, BentoBox ve SuperiorSkyblock2 için açma / kapatma, dünya listeleri ve işlem denetimleri içerir. Kurulu ve etkin olmayan sağlayıcı normalde atlanır. Örneğin `required-providers: [towny]` Townynin kurulu / etkin olmadığı kurulumda koruma gerektiren işlemlerin serbest geçmesini önler. Etkin bir sağlayıcının API hatası ise `fail-closed` kuralına tabidir. Birden fazla etkin koruma aynı konumu kapsıyorsa birinin reddi yeterlidir.

`fail-closed: true` etkin sağlayıcının API hatasında ilgili işlemi reddeder. `bypass-permission` boşken hiçbir permission entegrasyonu atlatmaz. Bir bypass izni tanımlamak oyuncunun işletme / hayvan / eşya sahibi olma şartını ortadan kaldırmaz. `wilderness` yalnızca sağlayıcının yönettiği dünyada alan dışı davranıştır; normal oyun dünyalarını bir SkyBlock eklentisi var diye ada saymaz.

`integrations.worlds.allowed: []` ortak katmanda tüm dünyalar, `blocked: []` ortak yasak yok anlamına gelir. Engelli dünya listesi izin listesinden önceliklidir. Ürünün kendi dünya kısıtlamaları ayrıca geçerlidir. İsimler kesin dünya adlarıdır; joker karakter eşlemesi yoktur.

`checks` sadece bu ürünün gerçekten yaptığı işlemlere uygulanır. Örneğin bir ticaret ürünü için blok kırma anahtarının var olması ürünün blok kırdığı anlamına gelmez. Bir kontrolü kapatmak sağlayıcının o işlemle ilgili denetimini bilerek kaldırır; eklentinin kendi sahiplik ve izin kuralları devam eder. Desteklenen API eşlemeleri ve doğrulama sınırları [entegrasyon rehberinde](INTEGRATIONS.md) bulunur.

## Dil, görünüm ve telemetri

Varsayılan dil oyuncunun kişisel tercihinden farklıdır. Dil / menü görünümü ayrı kaynak dosyalarında yönetilir; eşya kodlarını çevirmek yerine görünen metinleri düzenleyin. Bu üründe olmayan ayarları eklemek çalışma davranışını değiştirmez; paketlenmiş config ve ayrı dil / menü dosyalarındaki mevcut seçenekleri kullanın.
