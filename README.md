# Better Advancements

Better Advancements, Minecraft 1.20 API tabanını hedefleyen; görev zincirleri, puan sistemi, GUI tabanlı ilerleme ekranları, günlük/haftalık challenge yapısı, liderlik tabloları ve çoklu dil desteği sunan veri odaklı bir progression eklentisidir.

Bu README, sadece kısa bir tanıtım değil; kurulumdan özelleştirmeye, YAML şemasından geliştirici API kullanımına kadar ayrıntılı bir operasyon ve yapılandırma kılavuzudur.

## İçindekiler

- [Genel Bakış](#genel-bakış)
- [Öne Çıkan Özellikler](#öne-çıkan-özellikler)
- [Mimari Özeti](#mimari-özeti)
- [Uyumluluk ve Gereksinimler](#uyumluluk-ve-gereksinimler)
- [Kurulum](#kurulum)
- [İlk Açılışta Ne Olur](#ilk-açılışta-ne-olur)
- [Komutlar](#komutlar)
- [Yetkiler](#yetkiler)
- [Dosya ve Klasör Yapısı](#dosya-ve-klasör-yapısı)
- [Yapılandırma Dosyaları](#yapılandırma-dosyaları)
- [Advancement Sistemi](#advancement-sistemi)
- [Challenge Sistemi](#challenge-sistemi)
- [Tetikleyici Türleri](#tetikleyici-türleri)
- [Ödül Türleri](#ödül-türleri)
- [GUI Yapısı ve Özelleştirme](#gui-yapısı-ve-özelleştirme)
- [Çoklu Dil Sistemi](#çoklu-dil-sistemi)
- [Veri Saklama ve Veritabanı](#veri-saklama-ve-veritabanı)
- [Liderlik Tabloları](#liderlik-tabloları)
- [Vault ve PlaceholderAPI Entegrasyonu](#vault-ve-placeholderapi-entegrasyonu)
- [Geliştirici API'si](#geliştirici-apisi)
- [Build Alma](#build-alma)
- [Operasyon Notları ve Dikkat Edilmesi Gerekenler](#operasyon-notları-ve-dikkat-edilmesi-gerekenler)
- [Sorun Giderme](#sorun-giderme)

## Genel Bakış

Eklenti, oyuncu ilerlemesini sadece vanilla advancement mantığıyla sınırlamaz. Onun yerine sunucuya özel bir görev ağı oluşturur.

Varsayılan paket ile gelen içerik:

- 65 adet advancement
- 5 aktif tier: Novice, Skilled, Expert, Master, Mythic
- 6 adet challenge: 3 günlük, 3 haftalık
- 11 dil paketi: EN, TR, RU, DE, ES, IT, FR, SK, CS, ZH, RO
- Oyuncu başına puan, advancement ilerlemesi, challenge ilerlemesi ve dil tercihi saklama

Sistem YAML merkezlidir. Yani görevlerin başlıkları, açıklamaları, ikonları, bağımlılıkları, hedefleri, ödülleri ve GUI yerleşimleri doğrudan yapılandırma dosyalarından yönetilir.

## Öne Çıkan Özellikler

- Veri odaklı advancement sistemi
- Bağımlılık zinciri kullanan görev grafiği
- Gizli görev desteği
- Oyuncuya özel puan ekonomisi
- Günlük ve haftalık challenge rotasyonu
- Ana menü, tier görünümü, istatistik ekranı ve leaderboard GUI'leri
- SQLite ve MySQL desteği
- HikariCP ile bağlantı havuzu kullanımı
- Vault ile para ödülü entegrasyonu
- PlaceholderAPI ile oyuncuya gösterilen metinlerde placeholder çözümü
- Bukkit ServicesManager üzerinden erişilebilir halka açık API
- MiniMessage tabanlı çoklu dil mesaj sistemi

## Mimari Özeti

Çekirdek bileşenler şu akışla çalışır:

1. Eklenti açıldığında varsayılan kaynak dosyaları plugin klasörüne kopyalanır.
2. config.yml, database.yml, gui.yml, achievements.yml, challenges.yml ve dil paketleri yüklenir.
3. Tier tanımları gui.yml içinden okunur.
4. Veri katmanı başlatılır ve JDBC tabanlı storage adapter hazırlanır.
5. Dil yöneticisi, advancement yöneticisi, challenge yöneticisi, leaderboard yöneticisi ve bildirim yöneticisi ayağa kalkar.
6. Event listener'lar oyundaki hareketleri trigger olarak advancement/challenge sistemine yollar.
7. BetterAdvancementsAPI, Bukkit servis katmanına kayıt edilir.

Başlıca sınıf sorumlulukları:

- BetterAdvancementsPlugin: eklenti yaşam döngüsü ve servis kayıtları
- ConfigManager: tüm YAML dosyalarının bootstrap ve reload yönetimi
- PlayerDataManager: profil cache, async load/save ve storage erişimi
- JdbcStorageAdapter: SQLite/MySQL bağlantısı ve tablo işlemleri
- AchievementManager: advancement registry, ilerleme, tamamlanma ve ödüller
- ChallengeManager: aktif daily/weekly challenge rotasyonu ve ödüller
- LeaderboardManager: global ve session sıralama hesaplama
- GUIManager: tüm envanter menülerinin çizimi ve tıklama akışları
- LanguageManager: dil seçimi ve MiniMessage çözümleme
- NotificationManager: chat, title, action bar, boss bar ve ses bildirimleri

## Uyumluluk ve Gereksinimler

- Java 17
- Minecraft API sürümü: 1.20
- Derleme hedefi: Paper 1.20.4 API
- Vault: opsiyonel
- PlaceholderAPI: opsiyonel

Pratikte bu proje Paper odaklı derlenmiştir. Spigot uyumluluğu hedeflenmiş olsa da üretim ortamında önce test etmeniz önerilir.

## Kurulum

### Hazır JAR kullanımı

1. Derlenmiş BetterAdvancements JAR dosyasını alın.
2. Dosyayı sunucunuzun plugins klasörüne yerleştirin.
3. İsteğe bağlı olarak Vault ve PlaceholderAPI'yi de plugins klasörüne koyun.
4. Sunucuyu başlatın.
5. İlk açılışta sunucu, plugin.yml içinde tanımlı JDBC kütüphanelerini Maven Central üzerinden otomatik indirir.
6. İlk açılıştan sonra oluşan BetterAdvancements klasöründeki yapılandırmaları düzenleyin.
7. Gerekirse sunucuyu yeniden başlatın veya /ba reload kullanın.

### Kaynaktan çalıştırma

1. Depoyu klonlayın.
2. Java 17 kurulu olduğundan emin olun.
3. Gradle wrapper ile build alın.
4. Oluşan JAR dosyasını plugins klasörüne kopyalayın.
5. İlk sunucu açılışında HikariCP, SQLite JDBC ve MySQL Connector/J otomatik indirilir; bu nedenle build çıktısı fat jar yerine ince bir plugin jar olarak üretilir.

## İlk Açılışta Ne Olur

İlk çalıştırmada eklenti kendi kaynak dosyalarını plugin veri klasörüne kopyalar. Kopyalanan ana dosyalar:

- config.yml
- database.yml
- gui.yml
- achievements.yml
- challenges.yml
- lang klasörü altındaki tüm dil dosyaları

Bu davranış önemlidir çünkü depo içindeki src/main/resources dosyalarını değiştirmeniz canlı sunucuyu etkilemez. Canlı davranışı değiştiren dosyalar, sunucu çalışma klasöründe oluşan plugin veri klasöründeki kopyalardır.

## Komutlar

Ana komut kökü:

```text
/ba
```

Alt komutlar:

| Komut | Açıklama |
| --- | --- |
| /ba | Varsayılan olarak ana menüyü açar |
| /ba menu | Ana progression menüsünü açar |
| /ba stats | Oyuncunun istatistik ekranını açar |
| /ba leaderboard | Global leaderboard ekranını açar |
| /ba leaderboard session | Session leaderboard ekranını açar |
| /ba leaderboard global | Global leaderboard ekranını açar |
| /ba language <kod> | Oyuncunun arayüz dilini değiştirir |
| /ba reload | YAML yapılandırmalarını yeniden yükler |
| /ba give <oyuncu> <advancementId> | Belirtilen advancement'i zorla verir |
| /ba reset <oyuncu> | Oyuncunun Better Advancements verisini sıfırlar |

Önemli davranış notları:

- Menü, stats ve leaderboard komutları fiilen oyuncu için tasarlanmıştır.
- reload, give ve reset akışlarında ba.admin kontrolü uygulanır.
- language değişikliği oyuncu profiline kalıcı olarak kaydedilir.

## Yetkiler

plugin.yml içinde tanımlı yetkiler:

| Yetki | Varsayılan | Açıklama |
| --- | --- | --- |
| ba.use | true | Genel kullanım düğümü |
| ba.admin | op | Reload, give ve reset gibi yönetici işlemleri |

Not: Çekirdek komut kontrolünde aktif permission denetimi esas olarak ba.admin üzerinde uygulanır.

## Dosya ve Klasör Yapısı

Sunucu tarafında pratik olarak ilgileneceğiniz dosyalar şunlardır:

```text
plugins/
	BetterAdvancements/
		config.yml
		database.yml
		gui.yml
		achievements.yml
		challenges.yml
		lang/
			en.yml
			tr.yml
			...
```

Geliştirici tarafında kaynak yapısı:

- src/main/java: Java kaynak kodu
- src/main/resources: varsayılan gömülü konfigürasyonlar
- build/libs: derlenmiş çıktı

## Yapılandırma Dosyaları

### config.yml

Bu dosya genel davranış ve bildirim sistemini kontrol eder.

Varsayılan ana anahtarlar:

```yml
general:
	default-language: en
	cache-save-interval-seconds: 60
	hidden-achievement-placeholder: "<dark_gray>???</dark_gray>"

notifications:
	chat: true
	title: true
	action-bar: true
	boss-bar: true
	sound: UI_TOAST_CHALLENGE_COMPLETE
```

Alan açıklamaları:

- general.default-language: yeni oyuncu profilleri için başlangıç dili
- notifications.chat: chat mesajı gösterimi
- notifications.title: ekranın ortasında title gösterimi
- notifications.action-bar: action bar puan mesajı
- notifications.boss-bar: kısa süreli boss bar bildirimi
- notifications.sound: tamamlanmada çalınacak Bukkit sound enum adı

Operasyon notu:

- cache-save-interval-seconds alanı yapılandırmada bulunuyor, ancak mevcut kod akışında periyodik kayıt zamanlayıcısını doğrudan bu değere bağlayan bir kullanım yok.
- hidden-achievement-placeholder alanı da mevcut paketle geliyor; fakat gizli görevlerin GUI metni fiilen gui.yml içindeki tier.node.hidden-title ve tier.node.hidden-description üzerinden belirleniyor.

### database.yml

Oyuncu verisinin SQLite veya MySQL üzerinde saklanmasını belirler.

Varsayılan yapı:

```yml
storage:
	type: sqlite
	sqlite:
		file: better-advancements.db
	mysql:
		host: localhost
		port: 3306
		database: better_advancements
		username: root
		password: password

pool:
	maximum-size: 8
	minimum-idle: 2
```

Kullanım:

- SQLite için storage.type değerini sqlite bırakın.
- MySQL için storage.type değerini mysql yapın ve bağlantı alanlarını doldurun.

Örnek MySQL yapılandırması:

```yml
storage:
	type: mysql
	sqlite:
		file: better-advancements.db
	mysql:
		host: 127.0.0.1
		port: 3306
		database: better_advancements
		username: minecraft
		password: super-secret

pool:
	maximum-size: 10
	minimum-idle: 2
```

Önemli not:

- Veritabanı tipini değiştiriyorsanız sadece /ba reload yeterli değildir. Çünkü reload akışı storage adapter'ı yeniden başlatmaz. SQLite ile MySQL arasında geçişte tam sunucu yeniden başlatması yapın.

### gui.yml

Tüm GUI ekranlarının görünüm, slot, materyal, başlık ve lore ayarları bu dosyada bulunur.

Ana bölümler:

- main: ana hub menüsü
- tier: tek bir tier içindeki görev zinciri ekranı
- stats: oyuncu istatistik ekranı
- leaderboard: sıralama ekranı

Bu dosyada şunları özelleştirebilirsiniz:

- Menü başlıkları
- Kenarlık ve arka plan item'ları
- Slot yerleşimleri
- Tier ikonları ve tier sıralaması
- Bağımlılık yollarını temsil eden connector item'ları
- İstatistik kartları
- Leaderboard podium ve liste slotları

Tier yapılandırması için önemli kural:

- main.tiers.order içindeki her anahtarın karşılık gelen bir main.tiers.<anahtar> bloğu olmalıdır.
- Kod, order listesinde olup yapılandırması bulunmayan tier'ları otomatik oluşturmaz.

### achievements.yml

Bu dosya eklentinin kalbidir. Tüm advancement tanımları burada yer alır.

Bir advancement kaydı genel olarak şu alanları içerir:

- title
- description
- tier
- category
- icon
- hidden
- points
- rarity
- trigger.type
- trigger.target
- trigger.conditions
- dependencies
- rewards
- gui.slot
- gui.page

Örnek şablon:

```yml
achievements:
	sample_advancement:
		title: "Stone Worker"
		description: "Break 128 stone blocks."
		tier: skilled
		category: builder
		icon: STONE_PICKAXE
		hidden: false
		points: 15
		rarity: uncommon
		trigger:
			type: BLOCK_BREAK
			target: 128
			conditions:
				material: STONE
		dependencies:
			- t1_stone_age
		rewards:
			reward-1:
				type: XP
				amount: 250
			reward-2:
				type: ITEM
				value: IRON_PICKAXE
				amount: 1
		gui:
			slot: 21
			page: 0
```

Alanların anlamı:

- tier: görev hangi tier ekranında gösterilecek
- category: stats ekranında kategori bazlı dağılım için kullanılır
- icon: GUI üzerinde gösterilecek materyal
- hidden: tamamlanana kadar görevin gizli görünmesini sağlar
- points: görev tamamlandığında profile eklenecek temel puan
- rarity: görsel sunum ve istatistik anlamlandırması için kullanılır
- dependencies: bu listedeki tüm advancement'ler tamamlanmadan görev açılmaz
- gui.slot: tier envanteri içindeki konum

Varsayılan paket 65 advancement içerir.

### challenges.yml

Challenge sistemi advancement sisteminden bağımsız ama benzer bir trigger mantığı kullanır.

Challenge alanları:

- type: daily veya weekly
- title
- description
- trigger.type
- trigger.target
- trigger.conditions
- points-reward
- rewards

Örnek:

```yml
challenges:
	weekly_builder:
		type: weekly
		title: "Master Mason"
		description: "Craft 200 stone bricks this week."
		trigger:
			type: ITEM_CRAFT
			target: 200
			conditions:
				item: STONE_BRICKS
		points-reward: 50
		rewards:
			reward-1:
				type: XP
				amount: 300
```

Varsayılan paket:

- 3 daily challenge
- 3 weekly challenge

Rotasyon mantığı:

- Günlük challenge, günün sıra numarasına göre belirlenir.
- Haftalık challenge, ISO hafta numarasına göre belirlenir.
- Her anda bir günlük ve bir haftalık challenge aktif olabilir.

## Advancement Sistemi

Advancement sistemi event tabanlıdır. Oyuncu bir eylem yaptığında listener bunu uygun TriggerType ile AdvancementManager'a iletir.

Bir advancement'in tamamlanma akışı:

1. Trigger tipi eşleşir.
2. Trigger koşulları eşleşir.
3. Bağımlılıklar tamamlanmışsa görev açılmış kabul edilir.
4. Progress artırılır.
5. Hedef değere ulaşınca görev tamamlanır.
6. Temel puan profile eklenir.
7. Reward listesi uygulanır.
8. Bildirimler gösterilir.
9. Profil kaydedilir.

Bağımlılık mantığı:

- dependencies listesi boşsa görev doğrudan açılabilir.
- dependencies listesi doluysa listedeki tüm görevlerin tamamlanmış olması gerekir.
- Tier menüsündeki connector çizgileri bu bağımlılık yollarını görselleştirir.

Gizli görev mantığı:

- hidden: true ise görev tamamlanana kadar gerçek başlığı ve açıklaması yerine gizli metinler gösterilir.
- Tamamlandıktan sonra normal görünüm açılır.

## Challenge Sistemi

Challenge sistemi aktif günlük ve haftalık görevlerden oluşur.

Davranış özeti:

- Yalnızca aktif challenge'lar trigger alır.
- Challenge tamamlandığında points-reward profile eklenir.
- Ardından reward listesi uygulanır.
- Challenge ilerlemesi de oyuncu profiline kaydedilir.

Önemli fark:

- Advancement ödüllerinde MONEY tipi Vault üzerinden işlenir.
- Challenge ödüllerinde MONEY tipi için mevcut kodda aktif ödeme uygulaması bulunmaz.

Yani challenge dosyasında MONEY tanımlasanız bile mevcut sürümde bunun otomatik ekonomi ödemesi üretmesini beklememelisiniz.

## Tetikleyici Türleri

Kodda desteklenen trigger türleri şunlardır:

| TriggerType | Kaynak olay | Örnek koşul anahtarları |
| --- | --- | --- |
| JOIN | Oyuncu giriş yaptı | amount |
| BLOCK_BREAK | Blok kırıldı | material, amount |
| ITEM_CRAFT | Item craft edildi | item, amount |
| MOB_KILL | Oyuncu bir mob öldürdü | entity, amount |
| PLAYER_KILL | Oyuncu başka oyuncu öldürdü | entity, amount |
| EXPLORE_BIOME | Oyuncu yeni biome'a geçti | biome, amount |
| DISTANCE_WALK | Oyuncu yürüdü | amount |
| FISH | Oyuncu bir şey yakaladı | amount |
| ENCHANT | Eşya enchant edildi | amount |
| SMELT | Furnace çıktısı alındı | item, amount |
| BREED | Canlı üretildi | entity, amount |
| TAME | Canlı evcilleştirildi | entity, amount |
| PLAYTIME | Dakikalık periyot | amount |
| ITEM_CONSUME | Eşya tüketildi | item, amount |
| COMMAND | Oyuncu komut kullandı | command, amount |
| CUSTOM | Özel akışlar için ayrılmış tür | uygulamaya göre |

Notlar:

- PLAYTIME trigger'ı her 60 saniyede bir amount=1 olarak ilerletilir.
- DISTANCE_WALK, oyuncunun iki konumu arasındaki blok mesafesi kadar artış alır.
- EXPLORE_BIOME yalnızca biome değişiminde tetiklenir.
- COMMAND trigger'ında komut adı baştaki / işareti olmadan ve büyük harfli olarak işlenir.

## Ödül Türleri

Desteklenen reward tipleri:

| Tür | Açıklama |
| --- | --- |
| COMMAND | Konsoldan komut çalıştırır |
| MONEY | Vault üzerinden para yatırır |
| ITEM | Oyuncuya item verir |
| XP | Oyuncuya deneyim verir |
| POINTS | Better Advancements puanı ekler |

Örnek:

```yml
rewards:
	reward-1:
		type: COMMAND
		value: "crate give {player} vote 1"
	reward-2:
		type: MONEY
		amount: 500
	reward-3:
		type: ITEM
		value: DIAMOND
		amount: 3
	reward-4:
		type: XP
		amount: 200
	reward-5:
		type: POINTS
		amount: 15
```

Detaylar:

- COMMAND tipinde {player} placeholder'ı oyuncu adıyla değiştirilir.
- ITEM tipinde value alanı Bukkit Material adı olmalıdır.
- MONEY için Vault ve bir ekonomi sağlayıcısı gerekir.
- POINTS, oyuncunun Better Advancements puanını artırır; sunucu ekonomisine dokunmaz.

## GUI Yapısı ve Özelleştirme

GUI tamamen gui.yml tarafından yönetilir.

### Main menu

Ana menüde şu kartlar bulunur:

- Oyuncu profil kartı
- Sonraki hedef kartı
- Challenge board kartı
- Tier girişleri
- Statistics girişi
- Leaderboard girişi
- Dil bilgisi kartı
- Genel overview kartı

Next target mantığı:

- Sistem, oyuncunun açılmış ama tamamlanmamış bir sonraki hedefini bulur.
- Bu karta tıklanınca ilgili tier ekranına yönlendirme yapılır.

### Tier menu

Tier ekranı bir görev zinciri paneli gibi çalışır.

Gösterilen öğeler:

- Tier overview kartı
- Her advancement için bir node
- Node'lar arası dependency path connector'ları
- Previous tier ve next tier okları
- Legend item'ları
- Ana menüye dönüş düğmesi
- Route focus kartı

Gizli node davranışı:

- hidden görevler tamamlanana kadar özel gizli başlık/açıklama kullanır.

### Stats menu

Stats ekranı oyuncunun ilerlemesini farklı açılardan gösterir:

- Toplam tamamlanma ve puan
- Tier bazlı ilerleme
- Kategori dağılımı
- Rare completion sayısı
- Session completion bilgisi
- Mevcut objective özeti

Kategori sistemiyle ilgili önemli not:

- Kodun varsayılan içgörü hesaplaması combat, exploration, builder, farming ve magic kategorilerine odaklıdır.
- achievements.yml içine yeni kategori eklemek mümkündür; ancak stats ekranında görünmesi için gui.yml altında ilgili kategori bloklarını sizin de eklemeniz gerekir.

### Leaderboard menu

Leaderboard ekranı iki moda sahiptir:

- Global
- Session

Arayüz bileşenleri:

- Header kartı
- İlk üç oyuncu için podium kartları
- Diğer oyuncular için liste kartları
- Global ve session arasında geçiş düğmeleri
- Geri dönüş düğmesi

## Çoklu Dil Sistemi

Dil paketleri lang klasörü altında YML dosyaları olarak tutulur.

Varsayılan diller:

- en
- tr
- ru
- de
- es
- it
- fr
- sk
- cs
- zh
- ro

Dil seçimi akışı:

1. Yeni oyuncu, config.yml içindeki default-language değerini alır.
2. Oyuncu /ba language <kod> ile dili değiştirirse profil kalıcı olarak güncellenir.
3. Bildirimler ve komut mesajları bu dile göre çözülür.

Dil mesajları MiniMessage kullandığı için şunları rahatça yapabilirsiniz:

- Renk tanımı
- Gradient kullanımı
- Kalın, italik vb. biçimlendirme
- Placeholder yerleştirme

## Veri Saklama ve Veritabanı

Eklenti JDBC tabanlı bir storage adapter kullanır ve HikariCP ile bağlantı havuzu açar.

Oluşturulan tablolar:

- ba_players
- ba_advancement_progress
- ba_challenge_progress

Saklanan veri türleri:

- Oyuncu UUID
- Son bilinen oyuncu adı
- Seçilen dil
- Toplam puan
- Session join zamanı
- Son görülme zamanı
- Her advancement için progress, completion durumu ve tamamlanma zamanı
- Her challenge için progress ve completion durumu

Profil yaşam döngüsü:

- Join olduğunda profil async yüklenir.
- Quit olduğunda profil kaydedilir.
- Advancement ilerlemesi değişince profil kaydedilir.
- Sunucu kapanırken tüm cache yazılır.

## Liderlik Tabloları

İki ayrı leaderboard mantığı vardır.

### Global leaderboard

- Tüm kayıtlı profiller storage üzerinden yüklenir.
- Tamamlanmış advancement sayısı, yüzdesel ilerleme ve puan birlikte sıralama ölçütüdür.

### Session leaderboard

- Aktif cache içindeki oyuncu profilleri üzerinden hesaplanır.
- Bu nedenle pratikte mevcut oturum veya bellekte tutulan oyunculara odaklanır.

Yenilenme sıklığı:

- Leaderboard başlangıçta bir kez hesaplanır.
- Sonrasında her 60 saniyede bir yenilenir.
- /ba reload sonrasında ayrıca refresh çağrılır.

## Vault ve PlaceholderAPI Entegrasyonu

### Vault

Vault bulunduğunda ekonomi sağlayıcısı reflection ile çözülür.

Kullanım alanı:

- Advancement ödüllerindeki MONEY tipi

Vault yoksa:

- Eklenti çalışmaya devam eder.
- MONEY ödülleri sessiz şekilde etkisiz kalır.

### PlaceholderAPI

PlaceholderAPI yüklüyse oyuncuya gösterilen metinlerde placeholder çözümü yapılabilir.

Kullanım alanı:

- Dil dosyalarındaki oyuncu odaklı mesajlar
- MiniMessage metinleri içinde PlaceholderAPI ile uyumlu çıktı üretimi

PlaceholderAPI yoksa:

- Eklenti çalışmaya devam eder.
- Placeholder çözümü yapılmadan ham metin kullanılır.

## Geliştirici API'si

Eklenti, BetterAdvancementsAPI arayüzünü Bukkit ServicesManager üzerinden servis olarak kaydeder.

Sağlanan metodlar:

- getAdvancements()
- getAdvancement(String id)
- getProfile(UUID uniqueId)
- grant(UUID uniqueId, String advancementId)
- getGlobalLeaderboard(int limit)
- getSessionLeaderboard(int limit)

Servisi alma örneği:

```java
import dev.erissos.betteradvancements.api.BetterAdvancementsAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

RegisteredServiceProvider<BetterAdvancementsAPI> registration =
				Bukkit.getServicesManager().getRegistration(BetterAdvancementsAPI.class);

if (registration != null) {
		BetterAdvancementsAPI api = registration.getProvider();
		api.getAdvancement("t1_first_log").ifPresent(adv -> {
				Bukkit.getLogger().info("Found advancement: " + adv.title());
		});
}
```

Örnek kullanım senaryoları:

- Başka bir eklentiden oyuncuya advancement vermek
- Özel GUI içinde progression özeti göstermek
- Sunucuya özel bir leaderboard paneli üretmek
- Oyuncu profiline bağlı ek gameplay sistemleri kurmak

## Build Alma

Bu proje Java 17 hedefler ve Gradle wrapper ile derlenir.

Komut:

```powershell
.\gradlew.bat build
```

Çıktı tipik olarak build/libs klasöründe oluşur.

Projede gömülü gelen bağımlılıklar:

- HikariCP
- sqlite-jdbc
- mysql-connector-j

Opsiyonel compileOnly bağımlılıklar:

- Paper API
- PlaceholderAPI

## Operasyon Notları ve Dikkat Edilmesi Gerekenler

### 1. Reload ile restart aynı şey değildir

/ba reload şu bileşenleri yeniden yükler:

- tüm YAML dosyaları
- tier registry
- dil paketleri
- advancement registry
- challenge registry
- GUI konfigürasyonu
- leaderboard hesaplaması

Ancak şu durumlarda tam restart tercih edilmelidir:

- SQLite ile MySQL arasında geçiş
- JDBC bağlantı problemleri sonrası temiz yeniden başlatma
- Büyük çaplı plugin ekosistemi değişiklikleri

### 2. Tier order dikkat ister

gui.yml içindeki main.tiers.order listesinde yazdığınız her tier için detay bloğu tanımlayın. Aksi halde eksik tier'lar doğru render edilmeyebilir.

### 3. Kategori genişletirken stats ekranını da güncelleyin

Sadece achievements.yml içine yeni kategori eklemek yeterli değildir. stats ekranında kart görmek istiyorsanız gui.yml altındaki stats.categories bölümünü de genişletin.

### 4. MONEY ödülleri için Vault gerekir

Vault ve çalışan bir ekonomi eklentisi yoksa para ödülleri uygulanmaz.

### 5. Challenge tarafında MONEY desteği tamamlanmış değil

Advancement ödüllerinde MONEY çalışır. Challenge ödüllerinde ise mevcut kod akışında MONEY case'i aktif ödeme yapmaz.

## Sorun Giderme

### Eklenti açılıyor ama menü boş görünüyor

Kontrol edin:

- gui.yml bozulmuş mu
- achievements.yml içinde tier isimleri ile gui.yml tier tanımları uyumlu mu
- Slot çakışması var mı

### MySQL'e geçtim ama veri hâlâ SQLite gibi davranıyor

Muhtemel sebep:

- Sadece /ba reload yaptınız

Çözüm:

- Sunucuyu tamamen kapatıp yeniden başlatın

### Para ödülleri çalışmıyor

Kontrol edin:

- Vault kurulu mu
- Bir ekonomi sağlayıcısı kurulu mu
- Reward type MONEY doğru yazılmış mı
- Challenge ödülü mü test ediyorsunuz, advancement ödülü mü

### Dil değişmiyor

Kontrol edin:

- İstenen dil kodu lang klasöründe mevcut mu
- /ba language <kod> komutu doğru kod ile verildi mi
- Dil dosyası YAML hatası içeriyor mu

### Leaderboard beklediğim oyuncuları göstermiyor

Bilmeniz gerekenler:

- Global leaderboard storage verisini okur
- Session leaderboard yalnızca cache veya aktif oturum kapsamındaki profillere odaklanır
- Refresh periyodu 60 saniyedir

## Son Söz

Better Advancements, sabit kodlanmış bir görev sistemi olmaktan çok, sunucu sahibinin kendi progression tasarımını inşa edebileceği bir iskele sunar. Bu projeyi verimli kullanmanın anahtarı, achievements.yml ve gui.yml dosyalarını birlikte düşünmek, trigger mantığını doğru kurgulamak ve reload ile restart farkını operasyonel olarak doğru yönetmektir.

Sunucu konseptiniz ister survival odaklı olsun, ister RPG/quest temalı, mevcut yapı; zincirli görevler, gizli hedefler, puan ekonomisi ve GUI tabanlı takip için güçlü bir temel sağlar.