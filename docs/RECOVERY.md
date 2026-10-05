# Ödül teslim kutusu

Para ödülleri de aynı kutuda kalıcı saklanır. Vault sağlayıcısı yoksa veya ödemeyi açıkça reddederse kayıt `PENDING` kalır; sağlayıcı düzeldikten sonra girişte ya da `/ba claim` ile tekrar denenir. Sağlayıcı istisnası sonucu belirsiz bırakırsa kayıt `REVIEW` olur. `applied`/`retry` kararını verirken ekonomi sağlayıcısının hesap kayıtlarını kontrol edin.

Paket görevlerinin eşya/para ödülleri görev kimliği, ödül sırası, prestij ve tekrar dönemiyle tanımlanır. Tamamlanan teslimatın `DONE` makbuzu korunur; eski SQL ilerlemesinin yeniden yüklenmesi aynı ödülü tekrar vermez. Bu kayıtları silmeyin. `/ba reset` yeni bir ödül dönemi açar; önceki bekleyen ödülleri silmez. Özel COMMAND/XP ödülleri bu eşya/para makbuzunun kapsamı değildir.

Dolu envantere sığmayan eşya ödülü `rewards/<oyuncu UUID>.yml` içinde `PENDING` olarak saklanır. Girişte veya `/ba claim` ile yalnızca eşyanın tamamı sığdığında teslim edilir. Envanter değişimi öncesi kalıcı rezervasyon yazılır; aynı kutu ikinci kez alınmaz.

Sunucu teslimat ile kayıt silme arasında kapanırsa eşyanın oyuncuya geçip geçmediği yalnız bu dosyadan kesin olarak anlaşılamaz. Başlangıçta bu kayıt `REVIEW` durumuna alınır; otomatik tekrar verilmez.

- `/ba rewards <oyuncu UUID>`: inceleme bekleyen ödül kimliklerini listeler. Konsol kullanılabilir, `ba.admin` gerekir.
- `/ba rewards <oyuncu UUID> <ödül UUID> applied`: eşyanın verildiği doğrulandıktan sonra kaydı kapatır.
- `/ba rewards <oyuncu UUID> <ödül UUID> retry`: eşyanın verilmediği doğrulandıktan sonra tekrar alınabilir yapar.

Oyuncunun envanterini, sunucu yedeğini ve teslim kutusunu karşılaştırıp sonra çözümleyin; yeniden deneme kararı otomatik bir tahmin değildir. Bu güvence eşya teslim kutusuna aittir; yöneticiye ait özel konsol komutları ve üçüncü taraf ödülleri için dış sistemin kendi kayıtları geçerlidir.

Profil SQL işlemleri tek kuyrukta ilerler; değişiklikler ana iş parçacığında alınan kopyadan yazılır. Sıfırlama eski kaydı bekler, silme ve yeniden yükleme sıralıdır. Normal çıkış sonrası kaydı başarıyla biten profil önbellekten çıkarılır. Sıralama sınırı `general.leaderboard-limit` (varsayılan 100, 10–1000) ile ayarlanır.
