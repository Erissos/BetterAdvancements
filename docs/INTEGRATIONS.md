# Bölge ve ada entegrasyonları

BetterAdvancements; Towny, GriefPrevention, WorldGuard, Lands, BentoBox ve SuperiorSkyblock2 kurulu ve etkin olduğunda bu eklentilerin yerel bölge / ada izinlerini kullanır. Hepsi isteğe bağlıdır. `config.yml` içindeki `integrations.providers` ve `integrations.checks` seçenekleri denetimleri ayrı ayrı yönetir. Birden fazla koruma sağlayıcısı aynı noktayı kapsıyorsa bütün etkin sağlayıcıların izin vermesi gerekir. API hataları varsayılan olarak o işlemin ilerlemesini engeller; bulunmayan sağlayıcı denetlenmez.

Her sağlayıcı `integrations.providers.<id>.enabled` ile açılır: `towny`, `griefprevention`, `worldguard`, `lands`, `bentobox`, `superiorskyblock`. `required-providers` listesine yazılan sağlayıcının yokluğu reddetmeye neden olur. Ortak `fail-closed`, `worlds.allowed`, `worlds.blocked`, `wilderness` ve isteğe bağlı `bypass-permission` seçenekleri yorumlu config dosyasında açıklanır. `wilderness` yalnızca sağlayıcının yönettiği dünyada claim / ada dışındaki alanı kapsar.

`integrations.checks.progress` başarı ve meydan okuma ilerlemesini oyuncunun bulunduğu bölgeye göre sınırlar. Başarı ekranını açmak, sıralamayı görmek veya önceden kazanılmış ödülü teslim almak bu denetimin kapsamına girmez. İlerleme reddedildiğinde her adım veya tick için sohbet uyarısı gönderilmez.

| İlerleme kaynağı | Denetlenen yer |
| --- | --- |
| Blok kırma | Gerçek blok konumu / malzemesi için kırma izni ve bölgedeki ilerleme izni |
| Üretim | Üretim masasının konumu varsa masa, yoksa oyuncu; gerçek envanter farkı sonraki tick'te sayılır |
| Yürüme / biyom keşfi | Hareketin başlangıcı ve sonu; engellenen bölgede mesafe birikimi temizlenir |
| Balık tutma | Yakalanan varlığın / eşyanın konumu |
| Büyüleme / fırın | Büyüleme masası / fırının gerçek konumu |
| Üretme / evcilleştirme | Varlığın gerçek konumu |
| Varlık öldürme | Ölen varlığın bölgesi ve oyuncunun bulunduğu yer |
| Oyuncu öldürme | Yukarıdaki ilerleme kontrolüne ek olarak iki oyuncunun PvP izinleri |
| Tüketim, komut, katılım ve oyun süresi | Oyuncunun bulunduğu bölge |

Blok kırma, üretim, hareket, balık tutma, büyüleme, yetiştirme, evcilleştirme, tüketim ve komut dinleyicileri `MONITOR` önceliğinde iptal edilmiş olayları dikkate almaz. Böylece core eklentinin engellediği faaliyet için başarı ilerlemesi verilmez. `progress: false` örneğin kamusal doğuş alanında yürüme / oyun süresi gibi başarıların sayılmasına izin verir; diğer koruma eklentisinin iptal ettiği bir kırma olayını yeniden geçerli yapmaz. İlerleme yöneticileri de yapılandırılmış bölge kontrolünü uygulayarak dinleyici dışından gelen normal tetiklemelerde aynı kuralı korur.

Yetkili `/ba give` veya API'nin açık `grant` işlemi yönetim amaçlı zorunlu başarı vermedir; olağan etkinlik tetiklemesinden ayrıdır. Mevcut ödül teslimatları ve mali uzlaştırma kayıtları yeni bölge kontrolüyle tutulmaz. Vault para ödülleri ve PlaceholderAPI değişkenleri mevcut işlevleriyle çalışmaya devam eder.

Kaydettikten sonra `/ba reload` kullanın. Entegrasyon seçeneğinin tipi / değeri hatalıysa etkin yapılandırma değiştirilmeden yeniden yükleme reddedilir. Ada veya claim verileri bu eklenti tarafından değiştirilmez. Üçüncü taraf eklentilerin her sürümünü kapsayan bir sertifikasyon iddiası yoktur; uygulanmış kontrollerin kapsamı çalışma alanındaki `verification/integrations` kanıtlarında yer alır.
