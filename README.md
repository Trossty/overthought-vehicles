# Car

MrCrayfish's Vehicle Mod tarzı sürülebilir araba modu. NeoForge, Minecraft 1.21.1
(`neoforge-1.21.1` dalı; `main` dalı Fabric 1.21.11 sürümü).

## Durum

Çalışan özellikler:

- **Sürülebilir araba** — gaz, fren, geri vites, hıza bağlı direksiyon kavraması
- **Spawn eşyası** — Araba eşyasını bir bloğa sağ tıklayarak araç oluştur (Araçlar sekmesinde)
- **Kapalı kabin** — iki koltuk yan yana, sürücü solda; oyuncular arabanın içinde oturur
- **Yer çekimi** — araba düşer, yere oturur, uçurumdan yuvarlanır
- **Dönen tekerlekler + kıvrılan ön tekerlekler** — gerçek hıza ve direksiyona bağlı
- **Motor sesi** — devre göre perdesi değişen (vites değişince düşen), aracı takip eden döngü (etraftakiler de duyar)
- **Yol hissi** — tekerin altındaki bloğa göre yol sesi (çakıl çıtırdar, asfalt/taş fısıldar) ve bozuk zeminde kamera sarsıntısı
- **Korna** — `H` tuşu, sunucu tarafında yetki kontrollü
- **Yakıt sistemi** — her aracın gerçek depo boyutu (Vespa 8 L, Atego 200 L); tüketim hıza, gaza, yokuşa ve yüke bağlı; göstergede menzil; azalınca uyarı, bitmek üzereyken motor öksürür; çarpışmada delinen ve yamalanan depo
- **MrCrayfish usulü dolum** — bidonu ya da pompanın tabancasını aracın **depo kapağına** tut, sağ tıkı **basılı tut**: yakıt akıyor, bırakınca ya da başka yere bakınca duruyor; boş bidonla aynı şekilde depodan çek
- **Analog gösterge paneli** — hızölçer (ölçeği aracın azami hızına göre), devir saati + vites, yakıt ve sıcaklık
- **Kalıcılık** — depodaki yakıt kayıt/yükleme arasında korunur
- **Araç tezgâhı** — arabayı ve bidonu malzemeden inşa ettiğin blok
- **Hayatta kalma modunda elde edilebilirlik** — tezgâhın tarifi var, geri kalanı tezgâhta
- **Boya** — 16 vanilla rengi; boya elde arabaya sağ tıkla, renk eşyaya da taşınır
- **Araç lifti** — parçaları araca çeviren, arabayı havaya kaldıran ve tamirin tek yapılabildiği yer
- **İç mekân ve animasyon** — direksiyon, vites, pedallar, salınan egzoz dumanı
- **Animasyonlar** — motorla birlikte yatan sürücü, direksiyondaki/gidondaki eller, dönen gidon ve direksiyon, motor sehpası, römork dili, kilit flaşörü, 3B sprey ve anahtar
- **Hasar ve tamir** — çarpma hasarı, pert olma, hurda düşürme, liftte onarım
- **Parçalar ve atölye** — 24 parça aşınıyor, sökme sırası var, servis ekranından teşhis ve değişim
- **Üretim hattı** — dünyada petrol yatakları, onları çeken pompa (pumpjack), çok bloklu ve bacası tüten rafineri, ham yakıtı temizleyen hidrojenle arıtıcı; organik maddeden ayrıca **biyoyakıt**
- **Üç jeneratör** — kömür, biyoyakıt ve dizel; borulu şasili, egzozu tüten taşınabilir setler
- **Test sekmesi** — yaratıcı modda "Araba: Tüm Eşyalar (test)": modun her eşyası ve bloğu, her türüyle (dolu bidonlar, parça çeşitleri, boyalar) ve makinelerin yediği vanilya eşyalar
- **Enerji kablosu** — jeneratörden makinelere hat; ağ yok, enerji tüketene doğru akıyor; başka modların kablolarıyla bağlanıyor
- **Yakıt pompası** — boş elle tabancayı al, hortum peşinden gelsin; 6 bloktan uzaklaşınca tabanca yerine döner
- **Yakıt borusu** — arıtıcıdan pompaya hat; ağ yok, her boru kendi seviyesine bakıyor; başka modların boruları/tanklarıyla bağlanıyor
- **Yakıt deposu** — hattı besleyen, komparatörle okunan, seviyesi dışarıdan görünen tank
- **Tanker** — herhangi bir sıvıyı (benzin, dizel, su, lav, süt, bal, başka modların sıvıları) sekiz kova taşıyan, camlı bölümünden yükü görünen, park edince hortumu kendi çalışan araç
- **Yakıt valfi** — tek yönlü, hızlı ve kızıltaşla kapanan: hatta "önce şurayı doldur" demenin yolu
- **Kamyonet** — kasası sandık olan, üstünden geçtiği eşyaları kendi toplayan araç
- **Araç İstasyonu** — listeden aracı seç, dönen 3B önizlemesine bak, rengini seç, malzemeyle üret
- **Anahtar** — boş anahtarı araca tıklayınca o araca kesilir; anahtarsız çalışmaz, eğilip tıklayınca kilitlenir
- **Sprey boya** — 16 boyadan karıştırılan her renk; kutu 10 sıkım, boyayla tezgâhta doldurulur
- **Drift** — hızda el freni + direksiyon arkayı kaydırır
- **Römork** — tek akslı kasalı römork; aracın arkasına bağlanır, 27 slotluk yük taşır, yük çekeni yavaşlatır
- **İki tekerliler** — Vespa scooter ve Honda CBR spor motor; virajda yatıyorlar
- **Gerçek araçlar** — Toyota Corolla, Tofaş Şahin, Jeep Wrangler, Ford Mustang (yarış arabası), Toyota Hilux, Mercedes-Benz Atego tanker, Vespa Primavera, Honda CBR600RR ve kasalı römork; eski deneme arabası yok
- **Araç Kasası ve İngiliz anahtarı** — Araç İstasyonu aracı tahta bir kasada verir; kasayı koy, İngiliz anahtarıyla dört kez sağ tıkla, kapak kalkar, yanlar düşer, araç çıkar
- **Helikopter** — Robinson R44; zıplama tuşuyla dikine kalkar, ileri/geri burnu eğer, sağ/sol kuyruk rotoruyla yerinde döner, bırakınca yavaşça iner; motor havada durursa otorotasyonla iner
- **Tarım** — Massey Ferguson 240 traktör, arkasına takılan ekim makinesi (yükündeki tohumu sürülmüş toprağa eker) ve gübre serpme makinesi (yükündeki kemik tozunu ekinlere serper); John Deere X350 çim biçme traktörü üstünden geçtiği otu ve çiçeği biçip çektiği römorka doldurur
- **Küçük araçlar** — Honda FourTrax Rancher ATV, Tony Kart Racer 401 go-kart, Meyers Manx plaj arabası, Club Car Precedent golf arabası (elektrikli) ve Yamaha WaveRunner VX jet ski; ATV'de ve jet skide direksiyon değil gidon var
- **3B istasyon blokları** — pompa, rafineri, arıtıcı, pumpjack, jeneratörler ve depo küp değil, kaideli/kolonlu/bacalı modeller
- **Makine ekranları** — jeneratör, arıtıcı, pompa kulesi, akü ve şarj direği: depo, enerji, akış ve çalışıyor/boşta lambası
- **Akü bankası ve şarj direği** — 200 000 E'lik depo (önünden verir, öbür yüzlerinden alır), yanındaki elektrikli aracı şarj eden direk
- **Elektrikli motor** — motor parçalarına dokuzuncu tür: depo yerine akü, kendine has ses, yakıt kapağı yok
- **Motor sesleri** — dört silindir, altı silindir, V8, dizel ve elektrik ayrı; makinelerin de kendi uğultusu
- **Kilometre ve teşhis** — kilometre arttıkça ve parça yıprandıkça aşınma hızlanıyor; OBD tableti arızaları kodlarıyla, en kötüsü önde gösteriyor
- **Yakıt kalitesi** — rafineriden çıkan ham yakıt bidona da dolar, ama motoru ve bujileri hızlı yer; arıtıcı ham bidonu kabul eder
- **Asfalt ve yol boyası** — kar gibi katman katman serilen asfalt; yol boyası kutusunun menüsünden seçilen şerit, ok, yaya geçidi, dur çizgisi, viraj, park işareti çizilir
- **OBD tableti** — kalın gövdeli, kauçuk köşeli 3B bir tablet; elde harita gibi tutulur, ekranında aracın arızaları, yakıtı, kaportası ve motor ısısı canlı görünür
- **Başarımlar, ayar dosyası, Jade** — petrolden asfalta başarım zinciri, `config/car.json`, Jade kuruluysa araca bakınca durum özeti

## Kontroller

| Tuş | İşlev |
|-----|-------|
| `W` | Gaz |
| `S` | Geri / fren |
| `A` / `D` | Direksiyon |
| `Space` | El freni (hızda direksiyonla birlikte: drift) |
| `H` | Korna |
| `Shift` | İn |

Yere konan araç bir daha eşyaya dönmez, geri alınamaz. Vurmak onu ezer,
yeterince vurursan hurda demire döner. Yaratıcı modda vurmak aracı hiçbir şey
vermeden kaldırır. Pikabın kasası ve römorkun yükü her iki durumda da yere
dökülür.

## Sürüş hissini ayarlama

Bütün ayar sabitleri tek yerde:
[`CarEntity.java`](src/main/java/me/trossty/car/entity/CarEntity.java) dosyasının başı.

| Sabit | Etkisi |
|-------|--------|
| `MAX_SPEED` | En yüksek ileri hız |
| `MAX_REVERSE_SPEED` | En yüksek geri hız |
| `ACCELERATION` | Kalkış gücü |
| `BRAKE_STRENGTH` | Fren gücü |
| `ENGINE_DRAG` | Gaz bırakınca yavaşlama |
| `ROLLING_FRICTION` | Yerde sürtünme |
| `MAX_TURN_RATE` | Dönüş keskinliği |
| `FULL_GRIP_SPEED` | Direksiyonun tam tuttuğu hız |
| `MAX_STEER_ANGLE` | Tekerleğin görsel kıvrılma açısı |

## Kabin ve oturma

Araba 2.1875 blok yüksekliğinde ve kabini oyuk — dört duvar ve bir tavan, içi boş.

### Oturma noktası ayağın değil, kalçanın yeri

`getPassengerAttachmentPos`'tan dönen nokta doğrudan oyuncunun konumu olmuyor.
`Entity.updatePassengerPosition` ondan oyuncunun kendi `VEHICLE` bağlanma
noktasını çıkarıyor ve oyuncuda bu sıfır değil:

```java
// PlayerLikeEntity
public static final Vec3d VEHICLE_ATTACHMENT = new Vec3d(0.0, 0.6, 0.0);

// Entity.updatePassengerPosition
konum = araç.konum + aracınOturmaNoktası - yolcununVehicleNoktası
```

`EntityAttachmentType.VEHICLE`'ın enum varsayılanı `ZERO`, ama oyuncu bunu
`EntityDimensions` üzerinden eziyor — yani varsayılana bakıp geçmek 0.6 bloktan
hata demek. Oturma noktası pratikte oyuncunun **kalçasının** gittiği yer.

Oturma noktasına göre oyuncunun ölçüleri (vanilla `BipedEntityModel` araçtaki
oyuncunun yalnızca bacaklarını döndürüyor, gövde ve kafa yerinde kalıyor):

| Parça | Oturma noktasına göre |
|---|---|
| Ayak | -0.60 |
| Kalça / bacaklar | +0.15 |
| Omuz | +0.90 |
| Göz | +1.02 |
| Kafa tepesi | +1.40 |

### Sayılar

`SEAT_HEIGHT = 0.6` seçildi, çünkü 0.6 oyuncunun kendi 0.6'sını tam götürüyor:

| | Dünya y |
|---|---|
| Tekerlekler | 0 – 0.375 |
| Oyuncunun ayağı | 0.00 (arabanın tabanı) |
| Gövde sacı / kabin tabanı | 0.375 – 0.75 |
| Oyuncunun kalçası ve bacakları | 0.75 (tam kabin tabanında) |
| Omuz | 1.50 |
| Göz | 1.62 (ayaktakiyle aynı) |
| Cam bandı | 1.25 – 2.00 |
| Kafa tepesi | 2.00 (camın üst kenarı) |
| Kabin tavanı (iç) | 2.125 |
| Tavan (dış) | 2.1875 |

Bacaklar kabin tabanına oturuyor, kafa tam camı dolduruyor, tavana 0.1 blok pay
kalıyor. Koltuğu buradan oynatırsan tavanı da birebir oynatman gerekiyor —
tek serbestlik derecesi var.

### Camlar

Camlar dokuda saydam bırakılmış delikler: model `entityCutoutNoCull` kullandığı
için alfası sıfır olan pikseller tamamen eleniyor. Böylece dışarıdan içerideki
sürücü görünüyor ve ayrı bir cam malzemesine gerek kalmıyor.

İki koltuk yan yana duruyor. Kabin içi 1.0 blok geniş, koltuklar `±0.22`'de;
oyuncuların gövdeleri sığıyor ama kolları kapılara biraz taşıyor — Minecraft'ta
araç içindeki oyuncularda bu normal, modelleri daraltmadan çözümü yok.

Çarpışma kutusu 1.5'ten 2.1875'e çıktı.

## İç mekân ve animasyon

Kabin oyuk olduğu için içi gerçekten görünüyor, o yüzden içi de var. Hepsi
`tools/GenVehicles.java` içindeki yardımcılarla kuruluyor:

- `frontSeats` / `rearBench`: koyu yanaklı, kumaş (`SEAT_CLOTH`) ortalı koltuklar;
  istenirse krom çubuklu baş dayamaları. Minder kalçanın altında kalacak kadar
  ince, oyuncuyu yutmuyor.
- `dash`: yumuşak siyah üst, kaplama bandı, altında halı kaplı ayak boşluğu;
  orta konsolda turkuaz ekranlı radyo ve krom menfezler, uçlarda menfezler,
  torpido gözü ve kolu; konsol tabana iner, koltuk arası tünelde vites kolu
  (krom çubuk, siyah topuz) ve el freni.
- `gauges`: sürücünün önünde siyah siperlik, iki krem kadran ve kırmızı ibreler.
- `carpet`: kabin tabanı halı.

Corolla, Wrangler, Hilux, Şahin, Mustang ve Atego'nun
kabinleri bunlarla döşeli. Fotoğraf sahnesi: `cabins` (sürücü gözü, aşağı bakış,
camdan).

**Direksiyon yuvarlak.** 16 parçalı bir jant (her parça 22.5° döndürülmüş ince
bir kutu), yatay ve aşağı inen iki kol, sekizgen göbek ve gümüş bir amblem;
kolonun üstünde koyu bir kılıf.

Direksiyon, kolonun eğimini taşıyan `steering_mount`'un **çocuğu**. Bu şart:
`ModelPart` roll'ü pitch'in dışında uyguluyor, yani hem eğilip hem dönen tek bir
parça kolonun ekseni etrafında değil kabinin ekseni etrafında dönerdi.

### Gövde grubu

Karosere bağlı her şey `chassis`'in altında, dört tekerlek değil. Sebebi bu:
viraja yatarken ya da gaza çökerken gövde kendi içindeki bir nokta etrafında
dönüyor, tekerlekler yolda düz kalıyor.

| Hareket | Kaynak | Büyüklük |
|---------|--------|----------|
| Direksiyon dönüşü | `steerAngle` | yol tekerleklerinin 4 katı (tam kilitte ~112°) |
| Viraja yatma | `steerAngle` × hız | tam kilit + tam gazda ~4° |
| Çökme / dalma | yumuşatılmış ivme | ±3.4° ile sınırlı |
| Rölanti titremesi | `age` | 0.16 birim, 0.08 blok/tick üstünde sönüyor |

İvme `CarEntity.animateWheels` içinde tutuluyor ve `lerp(0.3)` ile yumuşatılıyor;
ham tick farkı gövdeyi çökertmek yerine titretecek kadar gürültülü.

### Egzoz

Motor dönerken — yani biri araçtaysa ve depoda yakıt varsa — arkada solda duman
çıkıyor. Rölantide tembel, gazda sık; hız 0.25'i geçince kalın `SMOKE`'a
dönüyor. Park etmiş ama içinde biri olan bir arabanın çalıştığının tek dışarıdan
görünen işareti bu.

### Tampon ve lambalar

Tamponlar artık gövde renginde düz bir blok değil: `bumper()` gri bir tampon ve
dış yüzünde 0.15 taşan siyah bir sürtünme bandı çiziyor (14 tamponun hepsi).
`lamps()` her ince far ve stop kutusuna siyah bir çerçeve ve ortada parlak bir
iç kısım (`LAMP_GLOW` / `TAIL_GLOW`) ekliyor; farlar düz renk lekesi yerine
kasası olan lambalar gibi duruyor.

### Motosiklet: CBR

CBR600RR'ın deposu dizde geniş, üstte daralıyor; üstünde açık tonlu bir sırt,
beyaz şerit ve krom yakıt kapağı. Motorun önünde radyatör, altında koyu bir alt
kaporta ve iki havalandırma yarığı; camın arkasında kadranlı gösterge; kuyrukta
askılı plaka ve iki sinyal.

## Boya

Elinde boyayla arabaya sağ tıkla. 16 vanilla rengi de var. Araba zaten o renkteyse
tıklama normal biniş davranışına düşüyor, boya boşa gitmiyor.

Renk üç yerde birden korunuyor:

| Nerede | Nasıl |
|--------|-------|
| Dünyada duran arabada | `DataTracker` ile, yakındaki herkesin istemcisine gidiyor |
| Kayıt/yükleme arasında | `writeCustomData` içinde `Color` alanı |
| Araba kırılıp eşyaya döndüğünde | `car:color` veri bileşeni |

Son satır önemliydi: `VehicleEntity.killAndDropItem` düz bir eşya yığını üretiyor,
yani boya sessizce kaybolurdu. Onu geçersiz kıldım. Eşya ikonu da boyayı
gösteriyor: ikonun boyalı kutuları beyaz çiziliyor ve `car:paint` renk kaynağı
(`PaintTintSource`) onları eşyanın `car:color`'ıyla, boyanmamışsa aracın fabrika
rengiyle boyuyor. Araçtaki gibi üç ton var (`paint`, `paint_dark`,
`paint_light`). Vanilla bu renk kaynakları listesini dışarı açmadığı için
`TintSourceTypesAccessor` karışımıyla ekleniyor. İpucu balonunda da rengin adı
yazıyor.

### Nasıl çalışıyor

Model renk filtresiyle boyanmıyor — filtre tekerlekleri, camları ve farları da
boyardı. Onun yerine renk başına bir gövde dokusu üretiliyor ve renderer dokuyu
değiştiriyor. 16 doku da [`tools/GenCarTextures.java`](tools/GenCarTextures.java)
tarafından üretiliyor.

Açık ve koyu tonlar ana renkten çarpanla değil, beyaza ve siyaha karıştırılarak
türetiliyor. Çarpan olsaydı siyah arabada hiç kontrast kalmazdı.

## Blok tasarımları

Düz küp olan atölye blokları gerçek 3B modellere çevrildi (`tools/gen_shop_blocks.py`
ile üretildi; dokular `shop_steel`, `shop_red`, `shop_chrome`, `shop_rubber`,
`shop_hazard`, `shop_wood`):

- **Araç Tezgâhı:** ahşap tabla, kırmızı çelik ayaklar, alt raf, kırmızı
  çekmeceli dolap, köşede mengene, lokma kutusu ve üstünde bir anahtar.
- **Araç İş İstasyonu:** tekerlekli kırmızı takım dolabı, yan tutamaklar,
  klavye ve geriye eğik **arıza tespit ekranı** (eski animasyonlu ekran dokusu).
- **Araç Lifti:** makaslı lift. Platform 2 blok geniş, 3 blok uzun (model blok
  dışına taşıyor; çarpışma hâlâ tek blok). Koyarken baktığın yöne göre uzanıyor
  (`facing` özelliği), yani arabayı süreceğin yöne. Kalkınca zemin şasesi, iki
  çift makas kolu (eğim yüksekliğe göre hesaplanıyor), krom hidrolik piston ve
  kırmızı kirişler görünüyor; inikken düz bir tırtıllı plaka. Model:
  `tools/gen_pump_lift.py`.
- **Yakıt Pompası:** MrCrayfish'in pompası tarzında uzun, ince kırmızı kabin;
  önde beyaz çerçeve, koyu başlık. Üstteki siyah LCD canlı: yakıtın adını ve
  doluluk yüzdesini yeşil yazıyor (%15'in altında sarı), gece de parlıyor.
  Altta MrCrayfish'teki gibi kabine gömülü kare bir cam bölme var: beyaz
  çerçeve, köşelerinde parıltı çizgileri olan şeffaf cam ve arkasında
  seviyesine kadar dolu benzin kütlesi; yukarıdan bakınca yüzeyi görünüyor
  (`FuelPumpRenderer`). Tabanca önden bakınca sağ yanda asılı, siyah hortum
  yere inip kabine geri giriyor. Fotoğraf sahnesi: `gasPump`.
  Arkası da boş değil: beyaz köşe çıtaları, panjurlu ve kulplu servis kapağı,
  adadan çıkan besleme borusu; üst kabinde karşı şerit için ikinci bir LCD ve
  isim plakası.
- **Tank:** cıvatalı köşebent dikmeler ve kenarlar, her yanda tek parça cam,
  ortasında cıvatalı rögar kapağı, sarı dolum kapağı ve havalığı olan kabartmalı
  sac kapak, tahliye tapalı taban (bkz. Sıvı Tankı; model
  [`tools/blocks/fuel_tank.js`](tools/blocks/fuel_tank.js), cam ve kapak dokusu `tools/gen_tank.py`).
- **Yakıt Borusu ve Valfi:** her kol blok sınırında cıvatalı bir flanşla
  bitiyor, iki boru birleşince araya bir flanş çifti giriyor; bağlantı kutusu
  borudan sadece biraz kalın. Valf düz geçen bir boru, iki ucunda flanş, üstünde
  sürgü gövdesi, kapak, mil ve el çarkı; çark açıkken sarı, kapalıyken kırmızı
  (`tools/GenPartTextures.java`).
- **Şarj Direği:** artık bir buçuk blok boyunda, araba tavanı hizasında: kaide,
  yüzünde yeşil şerit olan ince beyaz gövde, ekranı çerçeveli başlık, sağda
  kılıfında duran fiş ve aşağıda halka yapıp kılıfa çıkan kablo. Eşya modeli
  ayrıca küçültülüyor (`models/item/charging_post.json`).
- **Akü Dolabı:** taşan kapak ve arkasında havalandırma ızgarası, kutup başları
  arasında bakır bara, kapıyı çerçeveleyen köşe dikmeleri, menteşeler, sarı
  kulp ve iki yanda uyarı plakası.

Jeneratörlerin dumanı ufak `SMOKE` (biyoyakıtta `WHITE_SMOKE`); dizel iri duman
yerine iki küçük puf çıkarıyor. Fotoğraf sahnesi: `blockGallery`.

## Araç Tezgâhı

**Parçaları** burada yapıyorsun — arabayı değil. Tezgâh gövde, motor, şanzıman,
süspansiyon, lastik ve tanker varili üretiyor; bunları bir araca çeviren tek yer
[araç lifti](#araç-lifti). Tezgâhın kendisi normal çalışma tezgâhında yapılıyor:

```
I I I      I = demir külçe
# C #      C = çalışma tezgâhı
#   #      # = herhangi bir kalas
```

Tezgâha sağ tıkla, soldaki listeden ne inşa edeceğini seç, malzemeleri 3x3
ızgaraya koy ve **İnşa Et**'e bas.

| Ürün | Malzeme |
|------|---------|
| Araba Gövdesi | 6 demir külçe, 2 cam |
| Motor | 4 demir külçe, 2 bakır külçe, 1 kızıltaş bloğu |
| Şanzıman | 3 demir külçe, 2 bakır külçe, 2 kızıltaş tozu |
| Süspansiyon | 3 demir külçe, 1 bakır külçe, 2 balçık topu |
| Lastik | 3 siyah yün, 1 demir külçe |
| Benzin Bidonu | 3 demir külçe, 1 bakır külçe |
| Tanker Varili | 6 demir külçe, 2 bakır külçe |
| Kamyonet Kasası | 6 meşe kalası, 3 demir külçe |

Sağdaki malzeme listesinde eksik olanların sayısı kırmızı görünüyor, **İnşa Et**
düğmesi de malzeme tamamlanana kadar pasif kalıyor.

Izgara depolama değil, tezgâhın üstü: pencereyi kapattığında içindekiler sana
geri dönüyor — tıpkı vanilla çalışma tezgâhı gibi. Bu yüzden tezgâhın blok
varlığı yok, dolayısıyla kaybolacak bir envanteri de yok.

Tarifler veri paketi değil, kodda ([`ModVehicleRecipes`](src/main/java/me/trossty/car/workbench/ModVehicleRecipes.java)):
tezgâh ızgaraya göre eşleştirme yapmıyor, listeden seçtiğini kuruyor. Yeni bir
araç eklemek o listeye bir satır eklemek demek.

## Araç Lifti

Arabayı havaya kaldıran ve arabanın tek servis noktası olan blok. **Penceresi
yok:** sağ tık mandalı çeviriyor (yukarı/aşağı). Eskiden burada bir montaj ve
yenileme penceresi vardı; araçlar Araç İstasyonu'nda, yenileme Araç Tezgâhı'nda
yapılıyor.

```
i i i      i = demir külçe
I P I      I = demir bloğu
I I I      P = piston
```

### Ve artık gerçekten kaldırıyor

Lift uzun süre bir kapıydı: oyun "bu arabanın altında lift var mı" diye sorardı,
kıpırdayan bir şey yoktu. Kaldırmayan lift masadır.

Şimdi rampa **dört çeyrek blok** çıkıyor — tam bir blok, çünkü tek bloğun modeli
kendi bloğunun en fazla 16 birim üstüne uzanabiliyor. Piston kolu çıkıyor,
tablası arabanın altına giriyor ve araba tablayla birlikte yükseliyor,
tekerlekler boşta kalıyor.

| Kontrol | Ne yapıyor |
|---------|------------|
| Sağ tık | Mandalı çeviriyor: yukarı/aşağı |
| Kızıltaş | Enerji verildiği sürece yukarıda tutuyor |

Kızıltaş mandalın üstüne biniyor, yerine geçmiyor: elle yukarı bırakılmış bir
lift, kol kapanınca yukarıda kalıyor.

Tabla yerden yüksekteyken üstündeki aracı **o sahipleniyor**: araba tablayı
birebir takip ediyor (inerken de), motor hızı sıfırda tutuluyor çünkü
tekerlekler havada. Bu aynı zamanda arabanın iki karış yukarıdaki tabladan
yuvarlanıp düşmesini engelleyen şey. Seviye 0'a inince lift elini tamamen
çekiyor — inmiş liftin üstünden geçmek metal bir plakanın üstünden geçmek.

### Tamir artık havada yapılıyor

**Tamir yalnızca burada, üstelik yalnızca araba kalkmışken.** Hırpalanmış bir
arabaya demir külçeyle tıklamak, araba bir liftin üstünde değilse hiçbir şey
yapmıyor; tıklama binmeye düşüyor, yani yolda kalmıyorsun — arabayı sürüp garaja
götürüyorsun. **İnmiş lift de saymıyor:** tekerleklerinin üstünde duran arabanın
altına kimse giremez, liftin bütün varlık sebebi bu
([`VehicleLiftBlock.isServicing`](src/main/java/me/trossty/car/block/VehicleLiftBlock.java)).
Lift, arabanın blok konumunun ±2 blok yatay, 3 blok aşağı kutusunda aranıyor,
çünkü araba 2.5 blok uzunluğunda, kimse bloğuna göre park etmiyor ve artık
kalkmış araba liftin bir blok üstünde duruyor.

### Ayarlar

| Yer | Sabit | Etkisi |
|-----|-------|--------|
| `VehicleLiftBlockEntity` | `MAX_LEVEL` | Kaç çeyrek blok çıktığı (model sınırı 4) |
| `VehicleLiftBlockEntity` | `RAISE_PER_LEVEL` | Bir seviyenin kaç blok kaldırdığı |
| `VehicleLiftBlockEntity` | `TICKS_PER_STEP` | Bir seviyenin süresi |
| `VehicleLiftBlockEntity` | `HOLD_REACH` | Tablanın sahiplendiği yarıçap |
| `VehicleLiftBlock` | `REACH` | Tamir için lift arama kutusu |

## Parçalar ve atölye

Demir külçeyle tamir, gövdenin göçüklerini düzeltiyor. Altındaki parçalara
hiçbir şey yapmıyor — bu da buranın doldurduğu boşluk: hiç çarpmamış bir araba
bile yoruluyor ve o zaman ihtiyacı olan şey çekiç değil, **teşhis**.

Arabada aşınan, sökülen ve takılan **24 parça** var. Dördü bu sistemi bir aşınma
çubuğu listesinden fazlası yapıyor.

**Hiçbir parçaya bedava ulaşılmıyor.** Her parça, üstünde duran parçayı
biliyor. Balata tekerleğin arkasında, disk balatanın arkasında, debriyaj
şanzımanın arkasında. Bir parçaya dokunabilmek için üstündeki her şeyin sökülmüş
olması gerekiyor — yani araba üzerinde çalışmak bir menü değil, bir **sıra**.

**Parça yok da olabiliyor.** Durum 0 "bitmiş" değil, "takılı değil" demek;
aşınma 1'de duruyor. Sökme işlemini mümkün kılan şey bu ve `essential`
bayrağının sebebi de bu: şaftı sökülmüş araba yavaşlamıyor, hiç gitmiyor.

**Her biri kendi işini yapıyor.** Hiçbiri "biraz daha aşınma" değil.

**Her parçanın bir sınıfı var.** Aynı parça ekonomik, standart veya performans
olarak yapılabiliyor; yaptığı iş aynı, dayanma süresi farklı. Ayrıntısı
[aşağıda](#parça-sınıfı-ekonomik-standart-performans).

### Sistemler ve sökme sırası

Girinti, neyin neyin altında olduğunu gösteriyor.

| Sistem | Parça | Adet | Ne yapıyor | Şart mı |
|--------|-------|------|------------|---------|
| Motor Bölümü | Hava filtresi | 1 | Hız ve yakıt tüketimi | — |
| | ↳ Ateşleme bobinleri | silindir başına 1 | Kıvılcım; yorgunu yalnızca yük altında tekletir | **evet** (benzinli) |
| | &nbsp;&nbsp;↳ Bujiler | silindir başına 1 (4/6/8) | Gaz tepkisi, tekleme | **evet** |
| | &nbsp;&nbsp;&nbsp;&nbsp;↳ Motor | 1 | Azami hız | **evet** |
| | ↳ Enjektörler | silindir başına 1 | Yakıt tüketimi; takılanı silindirini tekletir, zengin yakar | **evet** |
| | Yağ filtresi | 1 | Motor ve şanzıman ömrü | — |
| | Radyatör | 1 | Motor sıcaklığı | — |
| | Devirdaim pompası | 1 | Su dolaşımı; yorulunca sızdırır, yoksa motor kaynar | — |
| | Termostat | 1 | Isınma; yorulunca açık ya da kapalı takılır | — |
| Elektrik | Akü | 1 | Çalıştırma ve korna | **evet** |
| | ↳ Alternatör | 1 | Akü şarjı | — |
| Aktarma | Şaft | 1 | Güç aktarımı | **evet** |
| | ↳ Şanzıman | 1 | Hızlanma ve geri vites | **evet** |
| | &nbsp;&nbsp;↳ Debriyaj | 1 | Kavrama, hızlanma | **evet** |
| Yürüyen Aksam | Lastikler | 4 | Yol tutuşu | **evet** |
| | ↳ Fren kaliperleri | 4 | Fren; sıkışanı sürter ve aracı o yana çeker | — |
| | &nbsp;&nbsp;↳ Fren balataları | 4 | Fren gücü | — |
| | &nbsp;&nbsp;&nbsp;&nbsp;↳ Fren diskleri | 4 | Fren gücü | — |
| | ↳ Süspansiyon | 4 | Darbe emilimi | — |
| | ↳ Rotiller | 4 | Direksiyon; aşınınca tıkırdar, araç yolda gezinir | — |
| | Direksiyon kutusu | 1 | Direksiyon tepkisi | **evet** |
| Gövde | Egzoz | 1 | Ses ve bir miktar güç | — |
| | Susturucu | 1 | Ses; çürüdükçe yükselir (OBD görmez) | — |
| | Yakıt pompası (depo içi) | 1 | Yakıt beslemesi; yorulunca tam gazda öksürür | **evet** |

Elektrikli arabada bobin, enjektör, yakıt pompası, termostat ve susturucu
yok; dizelde bobin yok (enjektörler var). Bir aracın hiç olmayan parçası
kimsenin yolunu kapatmıyor: dizelde bujiler bobinin arkasında sayılmıyor.

Motora ulaşmak için önce hava filtresi, sonra bobinler, sonra bujiler çıkıyor.
Balataya ulaşmak için teker ve kaliper. Debriyaja
ulaşmak için şaft ve şanzıman. Oyun hangisinin yolu kapattığını söylüyor ve
**en dıştakini** söylüyor — şaft hâlâ takılıyken "debriyajı sök" demenin anlamı
yok.

### Motor vinçle çıkıyor, sehpaya oturuyor

Kaputlu bir arabanın motoru elle sökülmüyor, elle takılmıyor, lift de onu
serbest bırakmıyor: üstten, **motor vinciyle** çıkıyor.

1. Kaputu aç, hava filtresini, bobinleri ve bujileri sök.
2. Vinci arabanın burnuna koy. Koyarken arabaya bakıyorsan bom kaputun üstüne
   gelir; kanca bölmenin ortasından en çok 1,5 blok uzakta olmalı.
3. Vince **boş elle** tıkla (ya da motora, ya da servis ekranındaki **Sök**'e):
   motor kancaya çıkıyor. Cebe hiç girmiyor. Turbo varsa motorla birlikte
   sökülüp sana geliyor.
4. **Çömelip** tıklamak bomu saat yönünde çeyrek tur döndürüyor; motor kancada
   onunla dönüyor.
5. Kanca bir **motor sehpasının** üstüne gelince tıkla: motor sehpaya oturuyor.
   Oradan anahtarla açılıyor, iç parçaları tek tek çıkıyor.

Geri takmak aynı yolun tersi: toplanmış motoru sehpadan kancaya al (açık
motor kaldırılmıyor), bomu bölmeye çevir, tıkla. Motor yalnızca **boş
bölmeye** iniyor; içeride motor varsa önce o çıkıyor. Dükkândan alınmış bir
motor elde vince tıklanınca boş kancaya asılıyor. Vinç kırılırsa kancadaki
motor yere düşüyor.

Kaputu olmayan araçlarda (motosiklet, tekne, uçak) motor eskisi gibi liftten
elle sökülüyor.

### Motorun içi

Motorun tek bir yüzdesi yok; o yüzde içindekilerin ortalaması:

| İç parça | Ne aşındırır | Bitince |
|----------|--------------|---------|
| Segmanlar (silindir başına) | Kilometre, kirli yağ, kirli yakıt | O silindirin kompresyonu düşer; en kötüsü yağ yakar (mavi duman) |
| Supaplar (silindir başına) | Kirli yakıt; kopan triger kayışı **eğer** | Eğik supaplı silindir neredeyse hiç sıkıştırmaz, teklemeyle güç kaybı |
| Silindir kapak contası | Hararet (motor kaynarken çok hızlı) | Conta yanar: iki komşu silindir düşer, su eksilir, beyaz duman |
| Krank yatakları | Az ya da kirli yağ | Vuruntu, yağ dolu olsa bile |

Silindirler birbirinin aynısı aşınmıyor; her aracın kendine göre zayıf bir
silindiri olur. Kopan kayış her zaman ilk silindirin ve aşağı yukarı yarısının
supaplarını eğer; yeni kayış motoru çalıştırır ama eğik supaplar eğik kalır,
o silindirler güç vermez. Kompresyon ölçer her silindiri kendi segmanından,
supabından ve contasından okur.

Motor söküldüğünde içindekiler **motor eşyasında** kalır. Sehpada anahtarla
açınca dört parça çıkar: **pistonlar**, **supaplar**, **conta** ve **krank mili**.
Piston ve supap takımı her silindirin değerini taşır; ipucunda
`Silindir silindir: 1: %90 2: %30 …` diye görünür, eğik supap %0'dır. Aynı
parçaları geri takmak aynı motoru verir; kötüsünü yenisiyle değiştirmek yalnızca
o kısmı düzeltir. Supaplar demirden yapılır:

```
N N
N N      N = demir parçası, I = demir külçe
I I
```

### Servis ekranı

Arabaya **çömelip sağ tıklamak** (ya da **Teşhis Panosu** ile tıklamak) servis
penceresini açıyor: bütün araba tek sayfada.

- Sol taraf arabanın sökülme sırası. Her satır bir parça, girintisi üstündeki
  parçanın altında olduğunu gösteriyor, altındaki çubuk durumu. Yeşil → sarı →
  kırmızı; takılı olmayan parça gri ve `—`.
- Sağ taraf seçili parça: ne işe yaradığı, durumu, yolunu kapatan şey, elindeki
  yedeğin hâli.
- **Sök** ve **Tak** düğmeleri. İkisi de arabanın kendi `removePart` /
  `fitPart` metotlarından geçiyor — yani lift kuralı ve sökme sırası tek yerde
  yazılı, her yerde aynı.
- Altta motor sıcaklığı ve arabanın neden gitmediği.

Pencere parça durumlarını ağ üzerinden hiç taşımıyor: durumlar arabanın
*tracked data*'sında, yani arabayı gören her istemcide zaten var. Taşınan tek
şey hangi araba olduğu.

### Tıklamak hem soru hem iş

Elinde parçayla arabaya sağ tıklamak, ekranı açmadan hızlı yol:

- **Lift yoksa / inmişse** → takılı parçanın durumunu söylüyor. Hiçbir şey
  harcanmıyor.
- **Üstü kapalıysa** → "önce hava filtresi sökülmeli" diyor.
- **Kalkmış liftteyse** → parça değişiyor. Yenisi takılıyor, eskisi **ne hâldeyse
  o hâlde** yere düşüyor; saklayıp sonra geri takabilirsin, çünkü düşen eşya
  durumunu `part_condition` bileşeninde taşıyor.
- **Boş yuvaya** takmak takma, dolu yuvaya takmak değiştirme — boş yuvada geri
  verilecek bir şey yok.

### Belirtiler

Arabanın derdini ölçmeden önce **hissediyorsun**:

| Belirti | Sebebi |
|---------|--------|
| Motor tekliyor, hız bir an düşüyor | Yorgun bir buji, bobin ya da enjektör, ya da kompresyonu kaçıran silindir |
| Yalnızca yük altında tekliyor, rölantide temiz | Yorgun ateşleme bobini |
| Tekliyor, siyah duman, yakıt çabuk gidiyor | Takılan enjektör |
| Tam gazda öksürüyor, son hız düşük; yavaşta normal | Yorgun yakıt pompası |
| Motor hiç ısınmıyor, yakıt fazla gidiyor | Açık takılmış termostat (ya da hiç yok) |
| Radyatör sağlam ama hararet | Kapalı takılmış termostat ya da devirdaim pompası yok |
| Motorun önünden su damlıyor, antifriz azalıyor | Devirdaim pompasının salmastrası |
| Fren basmadan bir yana çekiyor, son hız düşük | O köşede sıkışan kaliper |
| Yolda sağa sola gezinip tıkırdıyor | Aşınmış rotil (direksiyon kutusu hep aynı yana çeker) |
| Egzoz gürültülü | Çürümüş susturucu — okuyucu görmez, kulakla bulunur |
| Frenlerken araç bir yana çekiyor | O köşenin balatası/diski diğer taraftan zayıf |
| Düz yolda bir yana çekiyor | Kot ayarı ya da yorgun bir amortisör |
| Kaputtan buhar, güç %55'e düşüyor | Radyatör bitmiş ya da yok |
| Fren tutmuyor | Balata, disk, lastik |
| Yakıt çabuk bitiyor | Tıkalı hava filtresi |
| Araba hiç çalışmıyor | Şart parçalardan biri yok ya da akü bitik |

Aşırı ısınma dumanı göçük dumanından **farklı parçacık** kullanıyor (bulut,
duman değil): iki arıza farklı parça istiyor, sürücü koltuğundan aynı
görünmemeli.

### Ucuz parça pahalıyı yiyor

Sistemin öğretmek için kurulduğu tek ders: **değiştirmediğin ucuz parça,
arkasındaki pahalıyı mahvediyor.**

- Bitmiş **balata** arkasındaki **diski** 4 katı hızda yiyor.
- Kirli **yağ filtresi** motoru ve şanzımanı 2.2 katı hızda yiyor.
- Bitmiş **alternatör** aküyü boşaltıyor; boşalan akü arabayı durduruyor.

Üçü de ucuz parçalar. Üçü de ihmal edilince pahalı fatura kesiyor.

### Parça sınıfı: ekonomik, standart, performans

Car Mechanic Simulator'daki "ucuz marka / pahalı marka" ayrımı. Aynı şanzıman
üç ayrı sınıfta yapılabiliyor; sınıf hem **ne kadar dayandığını** hem de
**arabanın nasıl gittiğini** değiştiriyor:

| Sınıf | Malzeme | Aşınma | Performans |
|-------|---------|--------|------------|
| Ekonomik | ×1 | ×1.70 | ×0.94 |
| Standart | ×2 | ×1.00 | ×1.00 |
| Performans | ×3 | ×0.55 | ×1.07 |

Performans çarpanı parçanın kendi işine uygulanıyor (motor ve ateşleme parçaları
çekişe, fren parçaları frene, direksiyon kutusu dönüşe…) ve parçalar üst üste
biniyor. Gerçek istemcide, aynı 40 tick'lik gazla ölçülen yol:

| Araba | Yol (blok) |
|-------|-----------|
| hepsi ekonomik | 12,6 |
| hepsi standart | 15,1 |
| hepsi performans | 18,3 |
| V8 | 20,1 |
| V8 + turbo | 23,8 |
| karda yol lastiği / kar lastiği | 9,0 / 15,1 |
| toprakta spor lastik / arazi lastiği | 9,8 / 15,1 |

Ekonomik sınıf bilerek **tezgâhın eskiden o fiyata yaptığı parçanın kendisi**:
hiçbir şey pahalanmadı, sadece artık daha iyisini alma seçeneği var. Tezgâhta
malzeme panelinin altında üç düğme var (Eko / Std / Per) ve seçilen sınıf
malzeme sayılarını yerinde çarpıyor — düğmeye bastığında panelde yazan sayılar
değişiyor, yani neye ne ödediğini görüyorsun.

Aşınma tarafı tek bir yerde hissediliyor: `CarParts.wear()`. Yukarıdaki bütün
kurallar aşınmayı bu metoda veriyor, dolayısıyla ucuz parça yaptığı her işte
daha hızlı bitiyor ve o kuralların hiçbirinin sınıfı bilmesi gerekmiyor.

Nerede görünüyor:

- **Eşya ipucunda**, standart parçada bile — karşılaştıramadığın üç sınıf, tek
  sınıf kadar işe yarar.
- **Servis ekranının** sağ panelinde, takılı parçanın durumunun altında. "Bu
  neden yine bitti" sorusunun yarısı orada.
- **Arabanın kendisinde**: 24 parçanın sınıfı iki bit ile tek bir `long`'a
  paketli (24 × 2 = 48 bit; eski kayıtlardaki `int` aynen okunur). Araç eşyasında bu sayı yazılıysa
  araç yere o sınıflarla konur.

Sınıf iki kuralı da değiştirdi:

- **Sağlam parça da yükseltilebiliyor.** "Bu zaten yeni" reddi artık yalnızca
  gelen parça *daha iyi değilse* geçerli. Olmasa, sağlam arabaya iyi şanzıman
  takmanın tek yolu standart olanı önce bitirmek olurdu.
- **Servis ekranı sınıfa göre seçiyor.** Envanterdeki en iyi yedek artık önce
  sınıfa, sonra duruma bakıyor: biraz yol yapmış performans parçası, kutusundan
  çıkmamış ucuz parçadan iyidir — ucuz olan sen daha çıkmadan yine bitecek.

Lift de buna uyuyor: bir arabayı ucuz lastiklerle kurduysan **ucuz lastiklerle**
yola çıkıyor. `consume()` ızgaradan aldığı parçaların en kötü sınıfını geri
bildiriyor (üç iyi bir ucuz lastik takılı arabanın bir ucuz lastiği var), lift
de bunu arabanın sınıf alanına basıyor.

### Yenileme: tamir mi, yenisi mi

Yorgun parçanın ikinci bir çıkışı var. **Araç Tezgâhı penceresinde** her parça için bir
*yenileme* tarifi duruyor: yorgun parçanın kendisi + yenisinin **yarı
malzemesi** → aynı parça **%60**'ta geri.

Üç kural:

- **%60 bilerek yeniden eksik.** Yenileme, bir daha hiç parça almamanın yolu
  değil; yorgun parçadan ucuza kurtulmanın yolu. Yenilemelerle idare edilen
  araba, %60'ta bir araba.
- **Zaten %60'ın üstündeki parça yenilenemiyor.** `countWornInput` yalnızca
  eşiğin altındakileri sayıyor; sağlam bir parçayı yenilemeye vermek, malzeme
  ödeyip onu kötüleştirmek olurdu.
- **Sınıf değişmiyor.** Yenilenen ucuz şanzıman hâlâ ucuz şanzıman — yenileme
  ona zaman kazandırıyor, karakter değiştirmiyor. Sınıf sisteminin yarattığı
  asıl karar bu: *bu parçayı yamayayım mı, yoksa iyisini mi alayım?*

Yenilemenin ek malzemeleri elle yazılmıyor; tezgâhın o parça için zaten var olan
tarifinden **yarıya bölünerek** türetiliyor (`VehicleRecipe.refurbish`), yani
tarif değişirse yenileme de kendiliğinden onunla değişiyor.

Yenileme tarifleri tezgâh listesinin sonunda duruyor ve düğme seçili tarife göre
"Build" ile "Refurbish" arasında geçiyor. (Bir dönem lifte ait ayrı bir penceredeydi;
liftin işi arabayı kaldırmak, pencere kaldırıldı.)

### Fren aşınması sunucuda ölçülüyor

Fren tuşu yalnızca sürücünün istemcisinde var. Bu yüzden frenler, sunucunun
arabanın **gerçekten kaybettiği hızdan** ücretlendiriliyor: `wearBrakes` iki tik
arasındaki hız farkını alıyor. Araba hâlâ yuvarlanıyorsa bu frendir; sıfıra
düştüyse bu çarpmadır ve frene yazılmıyor.

### Sıcaklık

Sıcaklık, yükün ve radyatörün söylediği yere doğru gidiyor:

```
hedef = min(1, (0.25 + 0.40 × yük) / soğutma)
```

Sağlam radyatörde soğutma 1, yani tam gazda bile hedef 0.65 — kaynama eşiği
0.85'in altında. Bitmiş radyatörde soğutma 0.55 → hedef 1.18, yani sürekli tam
gaz kaynatıyor. Radyatör yoksa 0.15 → anında kaynıyor. Kaynayan motor azami
hızın **%55**'ini veriyor.

### 3B servis: parçayı menüde değil, arabanın üstünde değiştirmek

Lifte kaldırılmış bir arabanın yanına geldiğinde, **o an elinin değdiği her
parça arabanın üzerinde kendi yerinde işaretleniyor**: radyatör burunda, şanzıman
tabanın altında, balatalar ön tekerleğin içinde. Her işaretin üstünde adı ve
durumu havada duruyor, nişan aldığın parça beyaz yanıyor, **sağ tık** onu söküyor
ya da yerine yenisini takıyor. Servis penceresi duruyor — arabanın tamamını tek
bakışta okumak için hâlâ en iyi yer — ama anahtar işi artık arabanın üstünde
yapılıyor.

Yalnızca **erişilebilir** parçalar işaretleniyor; bu da söküm sırasının gözle
görünür hâli. Motor; hava filtresi, bobinler ve bujiler sökülene kadar nişan alınacak bir
şey değil, çünkü orada henüz bir işaret yok. Bir parçayı söktüğünde onun boş
yuvası kırmızı işaretle duruyor (yerine takabilmen için) ve altındaki parçanın
işareti beliriyor.

| İşaret | Anlamı |
|--------|--------|
| Açık gri çerçeve, yeşil yazı | Takılı ve sağlam |
| Sarı / kırmızı yazı | Takılı ama yıpranmış |
| Kırmızı çerçeve, "yok" | Yuva boş, elindeki yedeği takabilirsin |
| Kalın beyaz çerçeve + yandaki kutu | Nişan aldığın parça — sağ tık buna işliyor |

**Görünüş Create modundan alındı**, çünkü bu modun yanında duracağı mod o.
Renkler ve ölçüler Create'in kendi kaynağından: işaretler dolgu değil çerçeve
(Create'in "value box"ı gibi), etiketler arkasında kutu olmadan koyu konturlu
açık yazı (`0xEDEDED` üstüne `0x333333`), nişan alınan çerçeve yerine snap
etmek yerine üstüne oturuyor (Create'in "chasing outline"ı). Nişan aldığın
parça için imlecin 20 piksel yanında **mühendis gözlüğü ipucu kutusu** açılıyor:
başında parçanın kendisi, Create altını (`0xF1DD79`) başlık, altında durum,
sınıf ve tıklamanın ne yapacağı — 24 tikte son 8 pikseli kayarak yerine geliyor.

**Nişan alma işaretlerin kendisine bakıyor.** Arabanın çarpışma kutusu ortasında
duran ~2 bloklık bir kare; 5 blokluk bir arabanın burnu, ön tekerlekleri ve
bagajı onun dışında kalıyor. Bu yüzden işaretler oyunun imlecinden bağımsız,
gözden ilk katı bloğa kadar uzanan ışınla seçiliyor; tıklama da araca, lifte,
zemine ya da boşluğa düşse yakalanıyor (`UseEntity/UseBlock/UseItemCallback`).

**Sökmek ve takmak ayrı tıklamalar.** Takılı bir parçaya boş elle (ya da başka
bir şeyle) tıklamak onu söküyor; sökülen parça envanterine gidiyor ama **çalıştığın
ele değil** — boş ele düşseydi bir sonraki tık onu geri takar, ondan sonraki yine
sökerdi. Boş yuvaya ancak **yedeği elinde tutarak** tıklarsan takılıyor; dolu
yuvaya yedekle tıklamak değiş tokuş, eskisi cebine gidiyor. Yaratıcı modda da
boş yuva artık kendiliğinden dolmuyor.

**Sökülen parça modelden de kalkıyor.** Lastikler sökülünce tekerlek yerine
çıplak göbek görünüyor; diskler de sökülünce göbek de gidiyor.

**Her aracın kendi parça haritası var**
([`PartLayout`](src/main/java/me/trossty/car/entity/PartLayout.java)). Kutular
araç tanımından hesaplanıyor: lastik kutusu aracın kendi ön tekerleğinde,
balata/disk/amortisör onun iç tarafında, motor bölmesi ön aks ile burun
arasında ve kaput yüksekliğine göre, şanzıman/debriyaj/şaft iki aks arasında
tabanın altında, egzoz kendi kuyruğunda. Motosikletin ayrı bir haritası var
(motor tekerlerin arasında). Corolla ile Atego'nun motoru aynı yerde değil;
bunu ve "hiçbir kutu başka kutunun içine gömülü değil, hepsi aracın üstünde"
kuralını `partBoxesFitEachVehicle` testi her araç için kontrol ediyor.

**Plakalar dünyaya yazılıyor.** İsimler aracın kendi çiziminde, kameraya dönük
yazı olarak çiziliyor (`submitText`, `SEE_THROUGH`), yani kamera sallansa da
görüş açısı değişse de parçanın üstünde sabit duruyor ve kaportanın arkasında
kalmıyor. Sağlam takılı parçalara plaka yazılmıyor — yeşil çerçeve yeterli —
yalnızca yıpranmış ve eksik olanlar yazılıyor; nişan aldığın parçanınki de
imlecin yanındaki kutuya geçiyor. Birbirine yarım genişliklerinden yakın plakalar
**aracın üstündeki konumlarına göre** üst üste diziliyor, ekrana göre değil;
bu yüzden etrafında yürürken yer değiştirmiyorlar.

**Aracın kendisine dair üç satır tavanın üstünde:** gövde ve sürüş değerleri
ön yarının, yakıt arka yarının üstünde. Liftte olmasa da araca baktığında
görünüyorlar; sürerken görünmüyorlar.

| Nerede | Ne yazıyor | Ne zaman |
|--------|-----------|----------|
| Tavan, ön | `Gövde · %74`, en yüksek hız / çekiş / tüketim | Arabaya baktığında |
| Tavan, arka | `Yakıt · %31` ya da aşırı ısınma uyarısı | Arabaya baktığında |
| Parça noktaları | Eksik ya da yıpranmış parçanın adı ve durumu | Araba liftte kalkıkken |

> **Tuzak — `/tp ... facing` ayaklardan nişan alıyor.** İstemci testinde kamerayı
> bir parçaya çevirirken bu bir metre altından nişan almaya yol açıyor; gözden
> hesaplanmış bir hedef noktası vermek gerekiyor. Bir de oyuncu 20 tik beklerken
> nişandan kayıyor, bu yüzden çekimden hemen önce yeniden nişan alınıyor.

### Tezgâh pencereyle çalışıyor, liftin penceresi yok

Bir dönem tezgâhın ve boş liftin üstünde 3B düğmeler vardı; kaldırıldı. Tezgâha
sağ tıklayınca penceresi açılıyor; liftin penceresi yok.

Listede uzun araç adları kesilmiyor: sığmayan ad 0.7'ye kadar küçültülüyor,
ancak bu da yetmezse sonu kırpılıyor.

### Create ile uyum

Mod NeoForge 1.21.1'e taşındı, yani Create ile aynı oyuna yüklenebiliyor.
Create'e karşı oyun içinde henüz denenmedi; bağlantı NeoForge'un ortak
kapılarından (capability) geçiyor, Create'in boruları da tam bunu soruyor:

- **Benzin artık gerçek bir akışkan** (`car:petrol`). Dünyaya dökülmüyor, kovası
  yok, akmıyor — Create'in kendi çayı ve iksirleri gibi yalnızca depolarda
  tutulup sayılıyor.
- **Yakıt deposu bir akışkan deposu olarak görünüyor** (NeoForge
  `Capabilities.FluidHandler.BLOCK`). Create'in boruları ve pompaları yanındaki bloğa
  tam olarak bunu soruyor; cevap veren depo, iki mod birbirini hiç duymadan
  tesisata bağlanıyor. Bu, deponun aynı sayısına açılan ikinci bir kapı — modun
  kendi borusu, pompası ve rafinerisi eskisi gibi çalışıyor.
- **Ölçü:** 1 birim benzin = 1 mB. Araba deposu 1 kova, yakıt deposu 16 kova.
- **Deneme aktarımı iz bırakmıyor.** Öbür modlar önce `SIMULATE` ile sorar;
  depo o zaman hiçbir şeyi değiştirmeden yalnızca ne kadar alacağını söyler.

Bu kapı artık istasyonun her yerinde var (`fluid/PetrolSlot`):

| Blok | Başka modun borusu ne yapabilir |
|------|------|
| Yakıt deposu | benzin koyar, alır |
| Yakıt borusu, valf | benzin koyar, alır — başka modun borusu bu modun borusunun yerine geçebilir |
| Pompa (iki yarısı da) | benzin koyar |
| Pumpjack (her parçası) | ham petrol alır |
| Rafineri (her parçası) | ham petrol koyar; ham benzin / ham dizel / biyoyakıt alır |
| Hidrojenle arıtıcı | ham benzin / ham dizel koyar; benzin / dizel alır |
| Biyoyakıt ve dizel jeneratörü | kendi yakıtını koyar |

Ters yön de çalışıyor (`fluid/ForeignFluids`): bu modun borusu yanındaki
**herhangi bir** NeoForge sıvı deposuna — başka modun borusu, tankı, makinesi —
kol uzatıyor ve ona karşı yerli depo gibi yarı dolu oturuyor; valf de tek yönlü
olarak oraya basıyor ya da oradan çekiyor.

### Parçaların nereden geldiği

Lift tarifi değişmedi: bir araba gövde, motor, şanzıman, 4 süspansiyon ve 4
lastikten çıkıyor ve **içindeki 24 parçanın hepsi yeni** oluyor. Geri kalan 20
parça ayrı ayrı tezgâhta yapılıyor ve yalnızca **bakım** için gerekiyor — teşhis
seni tezgâha tek bir şey için gönderiyor. Atölye döngüsünün tamamı bu.

Tezgâh listesi 21 tarife çıktığı için kaydırılabilir bir ızgaraya dönüştü (4
sütun, 3 satır, tekerlekle kayıyor); eskiden tek sütundu ve 8. tariften sonra
envanterin üstüne taşıyordu.

> **Izgara kataloğu malzeme gözlerinin üstüne oturmuş.** Malzeme ızgarası
> 44, 18'de 3×3 duruyordu — yani katalog ızgaraya dönüştüğü andan beri tam
> onun altında. Gözler duruyordu ve çalışıyordu, ama hiçbir şey çizilmiyordu
> ve oraya yapılan tıklama malzeme koymak yerine tarif seçiyordu; tezgâhı
> doldurmanın tek yolu shift-tıklamaktı. Hiçbir test bunu yakalayamazdı,
> **istemci testinin ekran görüntüsü yakaladı**. Artık kataloğun altında, tek
> sıra dokuz göz; pencere de o kadar uzadı (166 → 190).

### Çalışmayan yaklaşımlar

- **Aşınmanın 0'a inmesi.** Sökülmüş parçayla bitmiş parça ayırt edilemez
  oluyordu. Aşınma artık 1'de duruyor, 0 yalnızca "takılı değil" demek.
- **"Yolu kapatan parça" olarak en içtekini söylemek.** `blockedBy()` zinciri
  dıştan içe sıralıyor; döngü son eşleşeni tutunca oyun "bujileri sök" diyordu,
  oysa üstünde hâlâ hava filtresi vardı. İlk eşleşende dönüyor artık.
- **Aşınmanın tamamını `isEngineRunning()` içine koymak.** İtilen arabanın
  lastikleri aşınmıyordu. Dönen her şey döndüğü için aşınıyor artık; yalnızca
  motor bölümü motorun çalışmasına bağlı.

### Ayarlar

| Yer | Sabit | Etkisi |
|-----|-------|--------|
| `CarPart` | `wearPerTick` | Parçanın tam gazdaki aşınma hızı |
| `CarPart` | `count` | Bir takımın kaç eşya ettiği |
| `CarPart` | `parentKey` | Üstünde hangi parçanın durduğu |
| `CarPart` | `essential` | Yokken araba gidiyor mu |
| `CarPart` | `WORN_OUT` | Aşınmanın durduğu taban |
| `CarPart.Quality` | `costFactor` | Sınıfın malzeme çarpanı |
| `CarPart.Quality` | `wearFactor` | Sınıfın aşınma çarpanı |
| `CarPart.Quality` | `DEFAULT` | Sınıfı yazılmamış parçanın sayıldığı sınıf |
| `CarEntity` | `WORN_ENGINE_SPEED` | Bitmiş motorda kalan azami hız |
| `CarEntity` | `WORN_PLUGS_THROTTLE` | Bitmiş bujide kalan gaz tepkisi |
| `CarParts` | `MISFIRE_CHANCE` | Bitmiş bujide tik başına tekleme olasılığı |
| `CarParts` | `WORN_OIL_WEAR` | Kirli yağın aşınma çarpanı |
| `CarEntity` | `WORN_RADIATOR_COOLING` | Bitmiş radyatörde kalan soğutma |
| `CarEntity` | `OVERHEAT_AT` | Kaynama eşiği |
| `CarEntity` | `OVERHEATING_POWER` | Kaynarken kalan güç |
| `CarParts` | `BATTERY_DRAIN` | Şarj gelmezken akünün boşalma hızı |
| `CarEntity` | `FLAT_BATTERY` | Akünün "bitik" sayıldığı eşik |
| `CarParts` | `PADS_EAT_DISCS` | Bitmiş balatanın diski yeme katsayısı |
| `CarEntity` | `MISSING_PUMP_COOLING` | Devirdaim pompası yokken kalan soğutma |
| `CarEntity` | `OPEN_THERMOSTAT_TEMP`, `SHUT_THERMOSTAT_HEAT` | Açık takılı termostatın sıcaklık çarpanı, kapalı takılının eklediği ısı |
| `CarEntity` | `OPEN_THERMOSTAT_THIRST`, `WORN_INJECTOR_THIRST` | Soğuk motorun ve takılan enjektörün fazla yakıtı |
| `CarEntity` | `WORN_PUMP_SPEED`, `CALIPER_DRAG_SPEED` | Yorgun yakıt pompasında kalan son hız, sıkışan kaliperin aldığı |
| `CarEntity` | `MISSING_CALIPER_BRAKE`, `MISSING_JOINTS_STEER` | Kaliper yokken fren, rotil yokken direksiyon |
| `CarParts` | `WORN_PUMP_FEED` | Bitmiş yakıt pompasının yetiştirdiği gaz |
| `CarParts` | `WANDER` | Bitmiş rotilin aracı gezdirme payı (derece) |
| `Upkeep` | `PUMP_WEEP_LOSS` | Salmastrası giden devirdaimin antifriz kaybı çarpanı |
| `CarEntity` | `WORN_TYRE_GRIP` | Kel lastikte kalan tutuş |
| `BodyDamage` | `WORN_SPRING_HARSHNESS` | Bitmiş süspansiyonun hasar çarpanı |
| `BodyDamage` | `SUSPENSION_WEAR_PER_DAMAGE` | Yönsüz hasarda (düşme) dört köşenin süspansiyon aşınması |
| `CarEntity` | `LOW_FUEL_FRACTION`, `STARVING_FRACTION` | Çanın çaldığı ve motorun öksürmeye başladığı yakıt oranı |
| `CarEntity` | `COAST_MARGIN`, `CLIMB_LOAD`, `TOWING_THIRST` | Ayak gazdan çekildi sayılan yavaşlama, yokuş yükü, römork çarpanı |
| `CarEntity`, `BodyDamage` | `LEAK_PER_TICK`, `PUNCTURE_MIN_DAMAGE`, `PUNCTURE_SPREAD` | Delik deponun sızıntısı ve delinme olasılığı |
| `JerryCans` | `CAPACITY`, `POUR`, `LITRES_PER_UNIT` | Bidonun birimi, bir dökümü, birimin litresi |
| `CarEntity` | `CAN_FLOW`, `NOZZLE_FLOW`, `FILLER_RADIUS` | Bidonun ve tabancanın tik başına akışı, kapağa nişan toleransı |
| `FuelPumpBlockEntity` | `HOSE_LENGTH` | Hortumun pompadan ele uzanabildiği mesafe |

## Yakıt

Araç dolu depoyla doğuyor (montajdan çıkan araç dolu geliyor). Her aracın
deposu kendi tanımında, gerçeğindeki gibi litre olarak yazıyor (`"tank": 50`);
yazmayan araç 50 L alıyor. İçeride 1 birim = 0,5 L, yani 50 L'lik aile
arabasının deposu eskisi gibi 100 birim, bir kova benzin, bir dolu bidon.

| Araç | Depo | Menzil (dolu depo, azami hızın %60'ı) |
|------|------|--------------------------------------|
| Toyota Corolla | 50 L | 6.3 kb |
| Tofaş Şahin | 50 L | 5.6 kb |
| Jeep Wrangler | 66 L | 4.1 kb |
| Ford Mustang | 61 L | 2.9 kb |
| Toyota Hilux | 80 L | 9.4 kb |
| Mercedes Atego | 200 L | 10.0 kb |
| Vespa Primavera | 8 L | 2.0 kb |
| Honda CBR600RR | 18 L | 2.8 kb |

Tüketim oranları (`fuel_use`) gerçek araçların 100 km'de yaktığına göre
ayarlı: Vespa ~2.8, Corolla ~6, Mustang ~16, Atego ~15 L/100 km karşılığı.
Menzil eşyanın ipucunda da yazıyor ("Depo 50 L · menzil yaklaşık 6.3 km").

Corolla'nın dayanma süreleri:

| Durum | Dayanma süresi |
|-------|----------------|
| Tam gaz | ~9 dakika |
| Rölantide (motor açık, duruyor) | ~65 dakika |
| Park hâlinde (kimse yok) | sonsuz — boş araba yakmaz |

### Ne kadar yaktığı

Gaz pedalı yalnızca sürücünün istemcisinde var, depo ise sunucuda; sunucu ne
istendiğini aracın kendisinden okuyor:

- **Hız** — eskisi gibi: ne kadar hızlıysa o kadar.
- **Ayak gazdan çekilince** — yavaşlayan araç (fren, boşta kayma) yalnızca
  rölanti yakıyor. Test: gazdayken 0.0089, bırakınca 0.0013 birim/tick.
- **Yokuş** — tırmanış yükü artırıyor (yumuşatılmış; bir basamak bir anlık
  sıçrama sayılmıyor). 1/4 eğimde tam hız ~1.5 kat.
- **Yük** — römork takılıysa ×1.3; kamyonetin kasası dolduğunca ×1.3'e kadar;
  tankerin varili dolduğunca ×1.3'e kadar.

### Gösterge, uyarı, öksürme

- Yakıt saatinin göbeğinin altında **menzil** yazıyor ("6.4 km", "680 m").
  Durunca azami hızın %60'ıyla gidiyormuş gibi hesaplıyor, yani "burada ne
  kadar oturabilirim" değil "nereye kadar giderim" sorusunun cevabı. Gaz
  dalgalanmasında yumuşak kayıyor, depo dolunca ya da delinince anında
  değişiyor.
- Yakıt %10'un altına inince bir kere **çan** çalıyor ve aksiyon çubuğunda
  "Yakıt azaldı — yaklaşık 690 m menzil kaldı" çıkıyor; pompa ikonu ve
  menzil turuncuya dönüyor. %15'in üstüne dolunca uyarı yeniden kuruluyor.
- %4'ün altında pompa hava emiyor: motor arada bir **öksürüyor** — araç
  bir anlık hız kaybediyor (×0.75), egzozdan siyah duman çıkıyor, söner gibi
  bir ses geliyor.

Yakıt bitince gaz yanıt vermiyor ama araç yuvarlanmaya ve direksiyona uymaya
devam ediyor, yani yol kenarına çekebiliyorsun. Motor sesi de susuyor.

### Delinen depo

Sert bir çarpışma (en az 4 hasar) depoyu delebiliyor: 4'te ~%10, 9'da %60
(tavan). Bu arkadan çarpmanın olasılığı; yandan yarısı, önden hiç (arada motor
var). Delik depo sürücü olsun olmasın **sızdırıyor**: tick başına 0.03 birim,
dolu bir Corolla deposu ~3 dakikada boşalıyor. Sol marşpiyelden, arka tekerin
önünden damla damla yakıt akıyor ve yerde ıslaklık bırakıyor; göstergede
menzilin yerine yanıp sönen kırmızı **SIZINTI** yazıyor, servis etiketinde
"Depo sızdırıyor" çıkıyor, sürücüye aksiyon çubuğunda söyleniyor.

Yaması Minecraft'ça: delik depoya **balçık topu** ya da **bal peteği** ile sağ
tıkla, biri harcanır, delik kapanır. Durum kayıtta korunuyor (`Leaking`).

### Benzin bidonu

Bidonun dayanıklılığı yakıt ölçeri, tersten ve artık **birim** cinsinden:
hasar 0 dolu (100 birim = 50 L), hasar 100 boş. İpucunda "98 Kurşunsuz · 37.5 /
50 L" yazıyor.

- **Doldurma (MrCrayfish usulü):** bidonu aracın **depo kapağına** tut ve sağ
  tıkı **basılı tut**. Tik başına 1 birim (saniyede 10 L) akıyor; bırakınca,
  başka yere bakınca, depo dolunca ya da bidon bitince duruyor. Artanı bidonda
  kalıyor. Arabanın başka bir yerine bidonla tıklamak artık bir şey dökmüyor,
  normal tık (binmek) oluyor.
- **Çekme:** **boş** bidonla kapağa basılı tutunca akış ters dönüyor, depodaki
  bidona çekiliyor. Yön tuşa basıldığı an belirleniyor; döküp boşalttığın bidon
  geri çekmeye başlamıyor. Yanlış yakıtı geri almanın ve yolda kalan bir araca
  yakıt taşımanın yolu bu. Bidon çektiği yakıtın türünü hatırlıyor.
- Pompa, yakıt deposu ve tanker bidonu bir seferde **sığdığı kadar** dolduruyor;
  dolu bidonu da sığdığı kadar alıyor (eskiden tamamını alıp fazlasını
  yutuyordu). Rafineri hâlâ döküm döküm dolduruyor, son dökümde bidona
  sığan kadarını veriyor. Jeneratör bir döküm yakıyor; son kalıntıyı kendi
  payı kadar süre yakıyor.

Bidon **tükenmiyor**: döküm `damage()` değil `setDamage()` çağırıyor, çünkü boş
bidon çöp değil, rafineriye geri götürülecek bir şey.

> Eski dünyalardaki bidonlar 0–4 hasarla kayıtlı; yeni ölçekte bunlar 96–100
> birim okunuyor, yani eski boş bidon neredeyse dolu görünür.

### Yakıt varili

MrCrayfish'in varili gibi tam blok boyunda, 14 piksel çapında sekizgen çelik
varil (`car:fuel_drum`): iki yuvarlanmış çember, kenarın yarım piksel içine
gömülü kapak, üstünde dolum tıpası ve havalandırma, önünde sarı tehlike
etiketi. Konulduğu yöne döner, etiketi sana bakar.

- **Rengi içindeki sıvının rengi**: boşken çıplak çelik grisi, benzinle
  kehribar, dizelle koyu sarı, suyla mavi, lavla turuncu, sütle beyaz, ham
  petrolle siyah. Başka modların sıvıları da kendi dokularının rengini alır.
  Kapak, tıpa ve etiket boyanmaz. Elde/envanterdeki dolu varil de aynı renkte
  görünür.
- **8 kova** tek bir sıvı tutar: benzin, dizel, ham yakıt, su, lav, başka
  modların sıvıları. Tank hücreleri gibi komşularıyla birleşmez.
- **Bidon, kova, şişe** ile sağ tıklayıp dökersin ya da çekersin; tank ile
  aynı kod (`LiquidStores`). Başka sıvı dolu varile yakıt dökülmez. Boş el
  içindekini okur, karşılaştırıcı doluluğu okur.
- **Kırınca içindekiyle düşer**: sıvı eşyada `car:stored_fluid` bileşeninde
  taşınır, geri koyunca varilde yine aynı sıvı vardır (shulker kutusu gibi).
- Transfer API'ye açık: borular ve başka modların boruları benzini çeker ya
  da doldurur.
- Tarif: üstte demir–kırmızı boya–demir, ortada demir–boş–demir, altta üç
  demir.

### Depo kapağı

MrCrayfish'teki gibi dolum tüm arabaya değil **kapağa** yapılıyor. Kapağın yeri
her araç dosyasına tek tek yazılmadı, gövdeden hesaplanıyor
(`CarEntity.fillerOffset`):

- **Araba, pikap:** sol arka çamurluk, arka tekerin hemen üstü ve arkası.
  Yükseklik tekerin üstünden 0.2 blok yukarıda; yan yüzey o noktadaki gövde
  kutularının en dışı. Corolla'da yerel (0.96, 1.01, −1.53).
- **Kamyon (tanker):** arka tekerlerin önündeki yan depo, aks hizasında.
- **Motosiklet:** depo üstü, sürücünün önünde.
- **Römork:** deposu olmadığı için kapağı da yok.

Nişan, AABB'ye değil kapağın kendisine bakıyor: bakış ışını kapaktan **0.5
blok** yakınından geçiyorsa ve erişim mesafesindeyse kapak "görülüyor". Araba
dönük olsa da kutusu eksene hizalı kaldığı için hit noktasına güvenmek yerine
böyle yapıldı. Elinde bidon ya da tabanca varken yakındaki araçların kapağında
küçük bir çerçeve çıkıyor, nişan aldığında turuncu yanıyor.

Dolum sırasında kapaktan ince bir amber damla akışı iniyor, birkaç tikte bir
"lık lık" sesi (çekerken dolma sesi) çıkıyor, aksiyon çubuğu depoyu gösteriyor.
Depo dolunca küçük bir zil ve "Depo doldu: 50 / 50 L".

Basılı tutma, eşyanın kendi kullanımıyla yapılıyor (`Item.use` +
`usageTick`, 72000 tik). Araca tıklama kapağa bakıyorsa `PASS` dönüyor ki tık
eşyaya geçsin. Gösterge her tik değiştiği için el sallanmasın diye
`allowComponentsUpdateAnimation` kapalı.

### Tanker kendi yükünden

Tanker deposu 20 birimin altına inince varildeki yükten kendini dolduruyor,
ama yalnızca yük motorunun yaktığı yakıtsa: dizel Atego dizel taşırken kendi
yükünü içer, benzin, süt ya da lav taşırken herkes gibi pompada doldurur.
Varilden yalnızca deposunun aldığı kadar düşüyor.

### Üretim hattı

Yakıt artık tek bloktan çıkmıyor; iki yolu var ve ikisi de **elektrik** istiyor.

```
fosil:  petrol yatağı → pumpjack → ham petrol → rafineri → ham benzin / ham dizel → arıtıcı → benzin / dizel
bio:    organik madde → rafineri (kazan) → biyokütle → rafineri (kolon) → biyoyakıt
```

#### Petrol yatakları

Overworld'ün her yerinde, kabaca 14 chunk'ta bir, yerin altında (en derin
katmanla y=48 arası) **petrol yatağı** taşından bir mercek var: birkaç blok
genişliğinde, iki üç blok kalınlığında. Hemen üstünde, yüzeyde siyah, ara ara
tüten bir **petrol sızıntısı** onu ele veriyor. Yatak bloğu kazılınca kendini
düşürüyor ama asıl işi pumpjack'in altında durmak.

#### Pumpjack (petrol pompası)

Kuyu başı, ortada Samson direği ve üstünde sallanan kol (at başıyla), arkada
motor ve karşı ağırlıklı krank: 1 × 3 × 3 blokluk bir makine. Kuyu başı
konduğun blok; sızıntının üstüne doğrudan konabiliyor. Çalışırken kol iner
kalkar, krank döner (blok varlık çizicisi), motor hafif tüter.

- Kuyu başının altına, iki blok yana kadar bakıp en sığ yatak bloğunu buluyor.
- Her yatak bloğu **1000 mB** ham petrol veriyor, sonra eski taşına (y<0'da
  derin kayağa) dönüyor: yatak biter, pumpjack taşınır.
- **8 E/tick** ile **10 mB/tick** çekiyor; 8000 mB'lık kendi tankı var.
- Ham petrolü **dokunduğu her şeye** kendisi itiyor — yanına konan rafineriye,
  başka modun borusuna — ve tankerin hortumu da ondan dolduruyor.
- Boş elle sağ tık: tank, aşağıda kalan tahmini rezerv, enerji.

```
I I I      I = demir külçe,  P = piston,  C = bakır külçe
  P C      S = düz taş,      B = demir blok
S B S
```

#### Rafineri

Artık tek blok değil, **4 × 4 blokluk, 6 blok yüksekliğinde** bir tesis; hepsi
tek beton kaidenin üstünde:

- önde solda iki bölmeli **kazan dairesi**, sağda pencereli ve kapısı lambalı
  **kontrol odası**;
- arkalarında bantlı, beyaz **ham petrol tankı**;
- arkada **6 blok boyunda damıtma kolonu**: altında ateş kapısı yanan reboiler,
  iki çalışma platformu ve kazan dairesinin çatısından çıkan merdiven;
- yanında boru rafına yaslanmış daha ince **sıyırma kolonu**;
- arka köşede incelerek yükselen, bantlı **tuğla baca**;
- hepsini birbirine ve bacaya bağlayan bakır hatlar.

Çalışırken baca kamp ateşi dumanı gibi tüter, kolonun tepesi buhar salar,
kazan dairesinin önü, reboilerin ve bacanın ateş kapıları, kontrol odası
lambası yanar. Herhangi bir parçasına tıklamak pencereyi açar; herhangi bir
parçası kırılırsa bütün tesis yıkılır ve tek eşya geri düşer. Koymak için
önünde ve sağında **4 × 6 × 4** boş yer lazım (koyduğun blok sol ön köşe olur).

Pencerede başlık satırının sağında bir durum lambası var: yeşil "Çalışıyor" ya
da kırmızı "Duruyor". Depoların sağ kenarında çeyrek işaretleri, doluluğu bir
bakışta okutuyor.

Tek tesiste iki aşama var:

| Aşama | Girdi | Güç | Çıktı | Süre |
|-------|-------|-----|-------|------|
| Kazan | 1 organik madde (`#car:refinery_organic`) | 8 E/tick | 100 biyokütle | 120 tick |
| Kolon | 200 besleme + 1 katalizör | 16 E/tick, kolon ≥ 300° | 200 ürün | 100 tick |
| Doldurma | 250 biyoyakıt | — | bidonda 1 döküm | anında |

Besleme tankı ya **biyokütle** ya **ham petrol** tutar, ikisi birden değil.
Ürünü besleme ve katalizör belirler:

| Besleme | Katalizör | Ürün |
|---------|-----------|------|
| Biyokütle | herhangi biri | **Biyoyakıt** — hazır, doğrudan bidona |
| Ham petrol | kızıltaş / blaze tozu / ışıltaş tozu | **Ham benzin** — arıtıcıya gider |
| Ham petrol | kömür / odun kömürü | **Ham dizel** — arıtıcıya gider |

Ham yakıtı hiçbir motor almıyor. Son yuvadaki bidon yalnızca biyoyakıtla
dolar; ham yakıt ürün kapısından çıkar: yanındaki arıtıcıya, tanker hortumuna,
başka modun borusuna.

- `#car:refinery_organic`: deniz yosunu, şeker kamışı, bambu, kaktüs, buğday,
  karpuz dilimi, çürük et, mantarlar, tatlı meyve, pancar
- `#car:refinery_catalyst`: kızıltaş, blaze tozu, ışıltaş tozu
- `#car:refinery_diesel_catalyst`: kömür, odun kömürü

#### Hidrojenle arıtıcı

Fosil yolun son adımı: iki blok boyunda reaktör, yanında hidrojen tüpü,
önünde çalışırken yanan lamba ve tepesinde buhar salan emniyet valfi. Ham
benzini **benzine**, ham dizeli **dizele** çeviriyor.

- Değdiği rafineriden ham yakıtı **kendisi** çekiyor (10 mB/tick); boruyla ya
  da tankerle de beslenebiliyor.
- **12 E/tick** ile **4 mB/tick** temizliyor; giriş ve çıkış tankı 4000'er.
- Temiz yakıt çıkışı: elde bidonla sağ tık (benzin ya da dizel), istasyonun
  **pompası, borusu, deposu ve valfi** (yalnızca benzin), tanker, başka modun
  borusu. Boş elle sağ tık tankları ve enerjiyi söyler.

```
I C I      G = cam,  B = kazan
G B G      R = kızıltaş
I R I
```

#### Elektrik ve kolon sıcaklığı

Rafinerinin 10 000 E'lik bir tamponu var (pencerenin sağındaki ölçek), tick
başına en çok 200 E alıyor. Enerji arayüzü NeoForge'un ortak **Forge Energy** kapısı
(`Capabilities.EnergyStorage.BLOCK`) — Mekanism, Powah, Create eklentileri gibi
modların kullandığı — olduğu
için o modların kablosu tesisin **herhangi bir parçasına** doğrudan bağlanıyor;
pumpjack ve arıtıcı için de aynısı.

Kolonun bir sıcaklığı var (ortadaki termometre, 0–1000°):

- çalışırken tick başına 2° ısınıyor, boştayken 1° soğuyor;
- **300°'nin altında** kolon çalışmıyor, makine önce 12 E/tick harcayıp ısınıyor;
- **800°'nin üstünde** kraking iki kat hızlı gidiyor.

#### Jeneratörler

Başka bir teknoloji modu yoksa elektrik buradan geliyor. Üçü de şantiye tipi
taşınabilir jeneratör: yuvarlatılmış boru şasi ve lastik ayaklar, üstte yakıt
deposu, önde turuncu çerçeveli siyah kontrol paneli (mavi prizler, voltmetre,
şalter, çalışma lambası), solda ipli marşıyla motor ve uçta susturucu. Tek
blok, 3B; 20 000 E saklıyor, dokunduğu her komşu
makineye tick başına 200 E'ye kadar itiyor ve deposu doluyken yakıt
tutuşturmuyor. Çalışırken susturucunun çıkışından tütüyor.

| Jeneratör | Görünüş | Yakıt | Güç | Verim |
|-----------|---------|-------|-----|-------|
| Kömür | kırmızı; depo yerine açık kömür haznesi, marş yerine yanan ateş kapısı | fırın yakıtları (kömür, odun, lav kovası…) | 20 E/tick | kömür başına 32 000 E |
| Biyoyakıt | yeşil; depo kapağı ve yakıt göstergesi | biyoyakıt | 30 E/tick | mB başına 90 E |
| Dizel | sarı; hava soğutmalı, kanatçıklı silindir kapağı, büyük depo kapağı | dizel | 40 E/tick | mB başına 160 E |

Kömürlü olan yakıtla sağ tıkla beslenir, eğilip boş elle yakıt geri alınır.
Sıvılı olanlar 2000 mB'lık tanka **doğru bidonla** sağ tıkla, boruyla ya da
tanker hortumuyla dolar; yanlış yakıt bidonu geri çevrilir. Boş elle sağ tık
enerjiyi ve yakıtı söyler.

```
Kömür:      I C I    Biyoyakıt:  I C I    Dizel:  I C I
            C R C                G R G            Y R Y
            I F I                I B I            I P I
```
(R = kızıltaş bloğu, F = fırın, G = yeşil boya, B = kova, Y = sarı boya, P = piston)

#### Enerji kablosu

Jeneratörü makineye bitiştirmek zorunda değilsin: aradaki yolu **enerji
kablosu** ile döşe. İnce, kauçuk kaplı bir hat; döndüğü, dallandığı ve bittiği
yerde bakır bir bağlantı kutusu var. Bir makineye değdiği yüzde de cıvatalı,
düz bir **bağlantı plakası** ve bakır rakorla bitiyor; kablo makineye takılı
görünüyor. Kendi kablosuna ve o yüzünde enerji kapısı
olan her şeye uzanıyor — jeneratörler, rafinerinin herhangi bir parçası,
pumpjack, arıtıcı, başka modların kabloları, bataryaları ve makineleri.

- Boru gibi **ağ yok**: her parça 1000 E'lik küçük bir tampon. Her tick önce
  değdiği ve güç alan makinelere veriyor, sonra yanındaki kablolarla seviyesini
  eşitliyor. Tüketen uç seviyeyi düşürüyor, enerji ona doğru akıyor; dallanma,
  döngü ve ortadan sökülen parça kendiliğinden hallediliyor.
- Parça başına tick'te en çok **250 E** geçiyor (bir jeneratörün 200'ünden
  fazla).
- Jeneratör kablodan geri enerji **almıyor**, yani hat ters akmıyor.
- Boş elle sağ tık o parçadaki enerjiyi söylüyor; kıyaslayıcı da okuyor.
- Tarif: `K C K` (kurutulmuş yosun, bakır külçe, kurutulmuş yosun) → 6 kablo.

#### Başka modların petrolü

`#car:crude_oil` sıvı etiketindeki her sıvı (artık mod'un kendi ham petrolü de
dahil) rafinerinin besleme tankına **1 mB = 1 birim** giriyor: boruyla, ya da
kovasını pencerenin ham petrol yuvasına koyarak. Etiket isteğe bağlı olarak
`#c:oil`, `#c:crude_oil`, Tech Reborn, Modern Industrialization, Ad Astra,
Create Diesel Generators, Immersive Engineering, PneumaticCraft ve Thermal'in
ham petrolünü sayıyor.

### Yakıt Pompası

İki blok yüksekliğinde — tek blok olsa otomat gibi görünürdü.

```
I L I      I = demir külçe,  L = kol
I C I      C = benzin bidonu
I B I      B = demir bloğu
```

**Penceresi yok.** Yaptığı her şey bir sağ tık, hangisi olduğunu elindeki
belirliyor:

| Elinde | Ne oluyor |
|--------|-----------|
| Dolu bidon | Bidon pompaya boşalıyor |
| Boş(ça) bidon | Pompadan sığdığı kadar alıyor |
| Boş el | **Tabancayı** askısından alıyor |
| Tabanca | Askısına geri asıyor |

Hepsi tankın durumunu eylem çubuğunda yazıyor; pompanın tüm arayüzü bu ve
fazlasına gerek yok. Komparatör çıkışı da tank doluluğuyla orantılı.

Pompa **kendisine değen her arıtıcıdan** (hidrojenle arıtıcı) otomatik benzin çekiyor (tick başına 4
birim, altı yönden). Yani arıtıcıyı pompanın yanına koyduğunda ortaya elle
taşıma gerektirmeyen bir benzinlik çıkıyor — bidon artık istasyondan uzağa yakıt
taşımak için, yakıtı taşımanın tek yolu değil.

#### Tabanca ve hortum (MrCrayfish usulü)

Pompa artık yanındaki arabayı kendiliğinden doldurmuyor. Boş elle pompaya sağ
tıkla, **Yakıt Tabancası** eline geçiyor, pompanın modelinden tabanca ve hortum
kıvrımı kayboluyor (`nozzle=false` blok durumu), pompadan eline siyah bir
**hortum** sarkıyor. Tabancayı aracın depo kapağına tutup sağ tıkı basılı tut:
tik başına 2 birim (saniyede 20 L), benzin pompanın kendi tankından düşüyor.
Test: 41 birimdeki Corolla 30 tikte doldu, pompa 2000 → 1410.

Tabanca pompaya ait, taşınmıyor. Yerine dönüyor:
- pompaya tabancayla tekrar sağ tıklayınca (askıya asma sesi),
- **6 bloktan** uzaklaşınca (hortum kopma sesi),
- elinden çıkınca: başka slota geçince, yere atınca, ölünce.

Bunu pompa her tik kontrol ediyor (`watchNozzle`). Tabancayı kimin aldığı
kaydedilmiyor: yeniden yüklenen dünyada kimse bir şey tutmuyor, envanterde kalan
öksüz tabanca kendini siliyor (`inventoryTick`), tabanca da pompaya dönüyor.
Aynı anda bir pompanın tek tabancası var; başkası tutarken "Bu pompanın
tabancası başkasında" çıkıyor. Kapak hortumun ulaşabileceğinden uzaktaysa
"Hortum oraya yetişmiyor" diyor. Dizel dolu depoya pompa benzin koymuyor.

Hortum dünya çiziminde çiziliyor (`FuelHoseRenderer`). Pompanın önündeki
çıkıştan ele sarkan bir eğri; ortası boyuyla orantılı sarkıyor, zemine
gömülmüyor, kalınlığı uzaklığa göre ayarlanıyor. Diğer oyuncuların elindeki
tabancaların hortumları da çiziliyor.

Tank 2000 birim ve kur mod genelinde tek: **10 benzin = arabanın deposunda 1
birim**, çünkü rafinerinin bir dökümü 250 benzin ve bir döküm 25 birim depo.

Pompanın iki yarısı birbirine bağlı: biri gidince diğeri komşu güncellemesinde
peşinden geliyor — kapıların yaptığının aynısı, ve aynı sebeple: blok dünyadan
yalnızca kazılarak çıkmıyor. Ganimet tablosu sadece `half=lower` için düştüğü
için hangi ucundan vurursan vur tam bir pompa düşüyor.

> **Not — çalışmayan yaklaşım:** Önce `onBreak` içinde diğer yarıyı siliyordum.
> Testte üst yarı kırılınca alt yarı ayakta kaldı; komşu güncellemesi hem daha
> az kod hem de her yolu yakalıyor.

Tüketim sunucu tarafında, arabanın gerçekten katettiği mesafeden hesaplanıyor —
sürüş sırasında gaz yalnızca sürücünün istemcisinde var, o yüzden sunucu hıza
bakarak yakıyor.

### Yakıt Borusu

Pompanın rafineriye değmesi şart değil artık: aradaki mesafeyi boru çekiyor.

```
C I C      C = bakır külçe,  I = demir külçe      → 4 boru
```

Boruda **ağ yok**. Ne grafik, ne kaynak–hedef listesi, ne biri ortadan
kazıldığında yeniden kurulacak bir yapı: her boru minik bir tank ve her tick
sadece altı komşusuna bakıp üç şey yapıyor.

1. Değdiği arıtıcıdan içiyor,
2. Değdiği pompaya boşalıyor,
3. Yanındaki boruyla seviyesini eşitliyor.

Yakıtı yürüten üçüncü kural. Boşalan pompa yanındaki boruyu düşürüyor, o boru
kendi komşusunu düşürüyor, çukur hat boyunca arıtıcıya kadar geri gidiyor —
yakıt kendisini kullanan şeye doğru yokuş aşağı akıyor. Hattın uçlarının nerede
olduğunu kimsenin bilmesi gerekmiyor, dallanma ve döngü bedava, ortadan bir boru
çekilince eğim orada bitiyor.

Bedeli iki şey. Hat **priming** için birkaç tick istiyor (bir eklemden tick
başına en çok 8 benzin, hem de gördüğü farkın yarısı kadarı) ve her boru **son
bir damlayı** tutuyor: bir birimlik farkın yarısı sıfır, damlanın tamamını
göndermek de onu bir sonraki tick geri savururdu. 1 benzin = arabanın deposunun
onda biri, yani hat tam kuru kalmak yerine içinde bir film tutuyor — gerçeğinin
yaptığı gibi.

Boru; boruya, rafineriye, pompanın **iki yarısına da** ve başka modların sıvı
tutan bloklarına kol uzatıyor, taşa uzatmıyor: duvarın içinden geçen boru duvara yapışmış gibi değil, borunun
duvardan geçtiği gibi görünsün diye. Penceresi yok; sağ tıklayınca o parçanın
içinde ne kadar benzin durduğunu eylem çubuğunda yazıyor, komparatör çıkışı da
aynı bilgiyi veriyor.

Görünüş: düz hat **tek, kesintisiz bir tüp** (kollar bloğun ortasında
buluşuyor, gölgelendirme borunun boyunca aynı). Çelik **rakor** yalnızca bir şey
olan yerde var — dönüş, T, uç, makineye giriş (`fitting` blok durumu). Eskiden
her bloğun ortasında bir göbek ve dokunun boruya ters oturması hattı boğum boğum
gösteriyordu.

> **Not — çalışmayan yaklaşım:** Kolların yönünü üreten blockstate'te `x` dönüşünü
> ters yazmıştım; dikey borular yere doğru bakıyordu. Doğrusu: kuzeye bakan bir
> kolu **`x:270` yukarı**, **`x:90` aşağı** çeviriyor. Hiçbir test bunu
> yakalamadı, istemci test ekran görüntüsü yakaladı.

### Sıvı Tankı

Eskiden sadece benzin tutan "yakıt deposu" artık düz bir **tank**: çelik çerçeve,
cam duvarlar ve içinde **her türlü sıvı** — benzin, mazot, ham petrol, süt, bal,
su, lav ya da başka bir modun sıvısı. Kayıt adı hâlâ `fuel_tank`, eski
dünyalardaki benzin kaybolmuyor (`Petrol` NBT'si okunup benzine çevriliyor).

```
I C I      I = demir külçe,  C = bakır külçe
I   I
I I I
```

**Birleşiyor.** Yüz yüze değen tank hücreleri tek bir tank: üç kat kule de olur,
dört-dörtlük taban da. Her hücre 16 kova; tank toplamı hücre sayısı × 16.

- Sıvı **alttan yukarı** doluyor: önce en alt kat, katın içinde eşit pay. Her
  hücre kendi içindeki payı saklıyor ve çiziyor, yani kırılan hücre sadece kendi
  içindekini götürüyor.
- Her hücrenin her yüzü **bütün tankın** kapısı (NeoForge sıvı kapısı): Create
  borusu nereye bağlanırsa bağlansın aynı sıvıyı görüyor.
- Bir tankta tek sıvı olur. İki farklı sıvıyı birleştirecek yere hücre
  konulamıyor; suyla dolu tanka lav girmiyor.
- Model multipart: her yüzde "öbür tarafta tank var mı" özelliği var. Çerçeve
  ve cam sadece dış yüzlerde çiziliyor, aradaki duvarlar kayboluyor; birleşik
  tank tek bir kap gibi görünüyor. Sıvıyı `FuelTankRenderer` camın arkasına
  çiziyor (sıvının kendi dokusu ve rengiyle; lav parlıyor).

| Komşusu | Ne oluyor |
|---------|-----------|
| Rafineri | Tank boşsa ya da benzin tutuyorsa tick başına 8 benzin çekiyor |
| Pompa | Tank benzin tutuyorsa tick başına 16 benzin veriyor |
| Boru / valf | Benzini kendi kuralıyla taşıyor (aşağıya bak) |
| Başka modun borusu | Her sıvıyı her yüzden alıp veriyor |

Borunun kuralı şu: bir boru deponun yanında **yarı dolu** oturuyor. Altındaysa
depodan içiyor, üstündeyse fazlasını depoya geri döküyor.

Penceresi yok: **kova, şişe, süt kovası, bal şişesi** ya da sıvı taşıyan başka
her eşya sağ tıkla döküyor/çekiyor; **bidon** benzin, mazot, biyoyakıt ve ham
yakıtı alıp veriyor; boş elle tıklayınca eylem çubuğunda "sıvı · miktar /
kapasite · hücre sayısı" yazıyor. Komparatör bütün tankın doluluğunu okuyor.

Testler: `tanksMergeAndFillFromTheBottom` (üç hücreye 40 kova su: alt kat 16+16,
üst 8; lav reddediliyor) ve `brokenTankCellSplitsTheTank`.

### Tanker

Boru sıvıyı bir yarddan öbür ucuna taşıyor. Tanker dünyanın öbür ucuna taşıyor,
ve artık yalnızca yakıt değil **herhangi bir sıvıyı**: benzin, dizel, su, lav,
süt, bal ya da başka bir modun boruladığı her şey. Varil aynı anda **tek bir
sıvı** tutuyor; boşken her şey girer, doluyken yalnızca aynısı.

Arabadan **%22 daha yavaş**, **8000 mB** (sekiz kova; sekiz dolu araba deposu)
taşıyor ve modelde arabanın her şeyini aynen kullanıyor — kabin, koltuklar,
direksiyon, oturma matematiği. Bir `CarEntity` alt sınıfı, ayrı bir araç değil.

**"Herhangi bir sıvı" bir liste değil.** Varil, NeoForge'da bir
sıvı deposu (`IFluidHandler`). Yani bu kapının
taşıyabildiği her şeyi alıyor: kova, şişe, kazan, başka bir modun tankı ya da
kabı. Minecraft'ta sıvı olarak bulunmayan süt ve bal için mod kendi sıvılarını
kaydediyor (`car:milk`, `car:honey`, yanında `car:diesel`); süt kovası ve bal
şişesi bu sıvıların kapları oluyor (bal şişesi çeyrek kova, çünkü dört şişe bir
bal bloğu).

**Varilin kendisi yuvarlak.** Merdiven basamağı gibi görünen kare kesit
yerine 15°'den 75°'e beş kutu, 14 yarıçaplı bir dairenin etrafında
(`w = 14·cos`, `h = 14·sin`); her katta tank / cam / tank / yük sırası ve
beş yerde biraz taşan bantlar. Uçlar yuvarlak kapak gibi görünüyor.

**Camlı bölüm.** Varilin orta bantları arası camdan, ve içindeki yük kendi
renginde, kendi seviyesinde görünüyor: su mavi, lav parlıyor (karanlıkta da),
bal altın, benzin kehribar, süt beyaz. Doku ve renk sıvının kendisinden geliyor
(`FluidVariantRendering`), yani başka bir modun sıvısı o modun çizdiği gibi
görünüyor. Yükün nerede çizileceği araç tanımında: rengi `"cargo"` olan şekil
kutuları varilin içi; gövde onları çizmiyor, tanker onları yükle dolduruyor.
Seviye bütün kutularda ortak, sıvıda olacağı gibi. Gövde hızlanırken öne
eğildiğinde sıvı da onunla eğiliyor, camdan dışarı taşmıyor.

**Hortum.** Sürücü yokken tanker 3 blok çevresinde sıvı tutan en yakın şeye
bakıyor ve hortumun ayarına göre davranıyor. Ayar, çömelip **boş elle**
tıklayarak değişiyor (otomatik → doldur → boşalt → kapalı):

| Ayar | Ne oluyor |
|------|-----------|
| Otomatik | Pompa, pumpjack, rafineri ya da arıtıcıdan **doldurur**; yakıt deposuna ya da başka her şeye **boşaltır** |
| Doldur | En yakın şeyden çeker (kazan, tank, başka modun deposu) |
| Boşalt | En yakın şeye boşaltır |
| Kapalı | Hiçbir şey |

En yakın olan kazanıyor, yani iki tankın arasına park etmiş bir tanker sıvıyı
daire çizdirmiyor. Otomatik ayar yabancı bir şeyden hiç çekmiyor, bu yüzden az
önce boşalttığı şeyden geri dolamaz. Yalnızca tam kovayla çalışan şeyler
(kazan gibi) için hortum küçük partiler yerine bir kerede bir kova taşıyor.
Yardın boruları ve valfleri hedef sayılmıyor: onlar sıvıyı yalnızca geçiriyor.

**Çömelmek yükü yönetiyor.** Düz sağ tık araç. Çömelerek sağ tık yük:

| Elde | Ne oluyor |
|------|-----------|
| Dolu kova / şişe / kap | Varile boşalır |
| Boş kova / şişe / kap | Varilden dolar |
| Bidon | Varildeki kendi yakıtından dolar ya da varile boşalır |
| Boş el | Hortum ayarı değişir |

Varilde başka bir sıvı varsa (sütün suya girmesi gibi) geri çevriliyor ve ne
taşıdığını söylüyor. Her tıklamada gösterge eylem çubuğunda: sıvı, miktar,
hortum.

Cepleyince yük de geliyor: miktar `cargo`, sıvı `liquid` bileşeni olarak. Sıvısı
yazmayan eski bir tanker eşyası benzin taşıyor sayılıyor.

### Yakıt Valfi

Boru eğime bakıyor, yani bir çatal iki pompayı yaklaşık eşit besliyor. Çoğu
zaman doğru olan bu; "önce şunu doldur" demek istediğinde değil.

Valf bunu bir ağ kurmadan çözüyor. Borunun yapamadığı iki şeyi yapıyor:

- **Pompalıyor.** Arkasındakinden çekip önündekine **32 birim** basıyor,
  seviyelere hiç bakmadan. Borunun bir ekten geçirdiği miktarın **4 katı** —
  yani çatalda valfli kol önce doyuyor, öbür kol artanı alıyor.
- **Tek yönlü.** Valfi geçen yakıt geri sızamıyor. Dolmuş bir pompa, yardın
  geri kalanı kurusa bile dolu kalıyor.

| Sabit | Değer | Ne demek |
|-------|-------|----------|
| `FuelValveBlockEntity.PUMP_RATE` | 32 | Tik başına ittiği miktar |
| `FuelPipeBlockEntity.TRANSFER_RATE` | 8 | Borunun eğimle taşıdığı miktar |
| `FuelValveBlockEntity.CAPACITY` | 100 | İçinde tuttuğu — valf depo değil |

**Kızıltaş kapatıyor:** `powered` olan valf durur. Bir depoya takılan
komparatörle bu tam bir yönlendirme kuralı oluyor — "bu depo dokuzu görene
kadar burayı doldur, sonra gerisini öbür tarafa yolla" — ayar ekranı değil,
blokla kurulmuş bir kural.

**Nişan alması hunininki gibi:** hangi bloğa tıklayarak koyduysan ona bakıyor.
Yanlış tutturduysan boş sağ tık altı yöne sırayla çeviriyor (K→D→G→B→yukarı→
aşağı), kolu çekiyormuş gibi ses veriyor ve eylem çubuğunda hangi yöne baktığını
— kızıltaş kapatmışsa onu da — yazıyor.

Tarif: bakır külçe + yakıt borusu + kızıltaş, yan yana (`CPR`).

Arkasından ve önünden anladığı şeyler aynı listede: boru, başka bir valf, depo,
pompa, rafineri. Yani valfi hattın herhangi bir yerine, hatta doğrudan
rafineriyle pompanın arasına koyabiliyorsun.

### Ayarlar

| Yer | Sabit | Etkisi |
|-----|-------|--------|
| `CarEntity` | `MAX_FUEL` | Depo hacmi |
| `CarEntity` | `FUEL_BURN_IDLE` | Rölanti tüketimi |
| `CarEntity` | `FUEL_BURN_DRIVING` | Tam gazdaki ek tüketim |
| `CarEntity` | `JERRY_CAN_UNITS` | Bir dökümün verdiği yakıt |
| `ModItems` | `JERRY_CAN_POURS` | Bidondaki döküm sayısı |
| `FuelRefineryBlockEntity` | `DISTILL_TIME` | Bir organik maddenin pişme süresi |
| `FuelRefineryBlockEntity` | `BIOMASS_PER_ITEM` | Organik madde başına biyokütle |
| `FuelRefineryBlockEntity` | `BIOMASS_PER_REFINE` / `PETROL_PER_REFINE` | Rafine oranı |
| `FuelRefineryBlockEntity` | `PETROL_PER_POUR` | Bir dökümün maliyeti |
| `PumpjackBlockEntity` | `RATE` / `ENERGY_PER_TICK` | Çekme hızı ve gücü |
| `PumpjackBlockEntity` | `CRUDE_PER_DEPOSIT` / `REACH` | Yatak bloğunun değeri, yana bakış |
| `HydrotreaterBlockEntity` | `RATE` / `ENERGY_PER_TICK` | Arıtma hızı ve gücü |
| `GeneratorBlockEntity.Kind` | `output` / `ticksPerUnit` | Jeneratör türlerinin gücü ve tüketimi |
| `EnergyCableBlockEntity` | `CAPACITY` / `RATE` | Kablo parçasının tamponu ve geçirme hızı |
| `CarSetup.Fuel.BIOFUEL` | `thirst` | Biyoyakıtın fazla tüketimi (1.40) |
| `data/car/worldgen/placed_feature/oil_deposit.json` | `chance` | Yatakların seyrekliği |
| `FuelPumpBlockEntity` | `CAPACITY` | Pompa tankı |
| `FuelPumpBlockEntity` | `PETROL_PER_FUEL_UNIT` | Benzin → depo kuru |
| `FuelPumpBlock` | `HOSE_REACH` | Pompanın arabaya uzanma mesafesi |
| `FuelPipeBlockEntity` | `CAPACITY` | Bir boru parçasının tankı |
| `FuelPipeBlockEntity` | `TRANSFER_RATE` | Bir eklemden tick başına geçen benzin |
| `FuelTankBlockEntity` | `CAPACITY` | Depo hacmi |
| `FuelTankBlock` | `MAX_LEVEL` | Seviye camındaki kademe sayısı |
| `TankerEntity` | `CARGO_CAPACITY` | Tankerin varili (mB) |
| `TankerEntity` | `HOSE_REACH` | Hortumun uzandığı mesafe |
| `TankerEntity` | `TOP_SPEED_FACTOR` | Tankerin hız cezası |
| `TankerEntity` | `SELF_REFUEL_BELOW` | Kendi deposunu doldurma eşiği |

## Motorlar, yakıtlar, lastikler ve modifiye

Bir arabanın *ne tür* bir araba olduğu artık parçalarından gelir. Üç parça
türlere ayrılır (motor, lastik, hava filtresi), bir de motora cıvatalanan turbo
vardır. Hepsi `CarSetup` içinde tek bir sayıya paketlenir; parçanın türü eşyada
`car:part_variant`, araç eşyasında `car:setup` bileşeninde taşınır. Türsüz
bir parça düz olandır — eski kayıtlardaki her araba benzinli sıralı 4, asfalt
lastiği, kâğıt filtredir.

### Motorlar

| Motor | Azami hız | Çekiş | Tüketim | Isı | Tezgâhta metal |
|---|---|---|---|---|---|
| Sıralı 4 | ×1.00 | ×1.00 | ×1.00 | ×1.00 | ×1 |
| Sıralı 6 | ×1.12 | ×1.10 | ×1.25 | ×1.08 | ×2 |
| V6 | ×1.15 | ×1.18 | ×1.35 | ×1.12 | ×2 |
| V8 | ×1.30 | ×1.35 | ×1.70 | ×1.25 | ×3 |

Her biri **benzinli** ya da **dizel** yapılır (sekiz motor). Dizel: hız ×0.92,
çekiş ×1.15, tüketim ×0.72 ve **buji istemez** — bujisiz çalışır, tekleme
yapmaz, buji aşındırmaz. Tarifte kızıltaş bloğu yerine kömür bloğu ve 2 demir
fazlası gider.

### Yakıt

Üç yakıt var: **98 Kurşunsuz Benzin**, **Dizel** ve **Biyoyakıt**. Benzin ve
dizel petrolden gelir ve arıtıcıdan geçer (yukarıdaki üretim hattı); biyoyakıt
organik maddeden, doğrudan rafineriden çıkar. Bidon içindekini hatırlar
(dizel bidonu sarı, biyoyakıt bidonu yeşil; ipucunda yazar).

- **Biyoyakıt her motorda yanar** — benzinli de dizel de — ama **%40 daha çok**
  yakılır: aynı depo daha kısa sürer. Hız ve çekiş biraz düşer (%3 / %4).
  Petrol bulunmayan bir dünyada araba yine yürür, sadece daha sık durur.

- Depo da tek yakıt tutar: boş depoya her şey girer, doluya yalnızca aynısı.
- Depodaki yakıt motorun yaktığı değilse araba **çalışmaz** ve sürücüye söyler.
  **Boş bir bidonla** arabaya tıklamak yanlış yakıtı bidon bidon geri çeker.
- İstasyon altyapısı (boru, tank, pompa) **yalnızca benzin** taşır; dizel
  arıtıcıdan, biyoyakıt rafineriden bidonla çıkar. Dizel bidonu istasyonda geri çevrilir. Tanker
  ise dizeli de taşır: dizel bidonu varile boşalır.

### Turbo ve açık hava filtresi

- **Turbo** ayrı bir eşyadır, yuva değil: liftteki arabaya, motor takılıyken
  sağ tıklanır. Hız ×1.12, çekiş ×1.25, tüketim ×1.20, ısı ×1.20, motor
  aşınması ×1.30. Motor sökülünce turbo da onunla birlikte ele gelir.
- **Açık hava filtresi** hava filtresinin bir türüdür: hız ×1.04, çekiş ×1.06,
  tüketim ×1.05; kendisi iki kat hızlı tıkanır, motoru ×1.15 yıpratır.

Farklı türde bir parça, sağlam olanın üstüne de takılır (aynı tür ve aynı
kalite sağlam parçanın üstüne takılmaz — yanlış tık motor yakmasın diye).

### Lastikler ve zemin

Arabanın altındaki blok zemini belirler: kar/buz → **kar**; toprak, kum, çakıl,
kil, tarla, patika → **toprak**; gerisi **asfalt**. Yola serilmiş kar katmanı
da kardır.

| Lastik | Asfalt | Toprak | Kar | Aşınma |
|---|---|---|---|---|
| Asfalt | 1.00 | 0.75 | 0.55 | ×1.0 |
| Toprak | 0.88 | 1.00 | 0.75 | ×1.1 |
| Kar | 0.85 | 0.85 | 1.00 | ×1.2 |
| Spor | 1.15 | 0.60 | 0.40 | ×1.5 |

Tutuş frenleri ve direksiyonu doğrudan çarpar. Çekişi en çok 1'e kadar çarpar
(yapışkan lastik güç eklemez), azami hızı da `0.7 + 0.3 × tutuş` ile: patinaj
yapan teker motorun verebileceği hıza çıkamaz.

### Yolcular

Arabada iki koltuk var: ilk binen sürer, ikinci oyuncu sağ tıklayıp yanına
oturur. **Kamyonet** kasasında iki kişi daha taşır (dört kişi). Sürücüden
sonraki her yolcu çekişi ×0.95 düşürür.

## Veri paketiyle araç eklemek

Yeni bir araç Java değil, **bir JSON dosyası**. `data/<ad_alanı>/car/vehicle/<isim>.json`
koyan her veri paketi (ya da mod) bir araç eklemiş olur; model dosyası, doku
dosyası, kayıt kodu gerekmiyor. Gövde dosyadaki kutulardan oyun açılırken
kuruluyor, boyası araç boyandıkça yeniden üretiliyor.

```json
{
  "chassis": "car",
  "width": 1.9, "height": 1.8,
  "color": "white",
  "performance": { "speed": 1.05, "acceleration": 1.0, "fuel_use": 0.85,
                   "braking": 1.05, "steering": 1.05, "offroad": 0.0 },
  "factory": { "engine": "inline4", "fuel": "petrol", "tyres": "road" },
  "seats":  [ [-6.4, 4.4, 3], [6.4, 4.4, 3] ],
  "wheels": [ { "at": [-13, 5.5, 24], "radius": 5.5, "width": 4, "steers": true } ],
  "shape":  [ { "from": [-15, 4, 30.5], "to": [15, 13, 38.5], "color": "paint" },
              { "from": [-13, 14, 12], "to": [13, 28, 13.3], "color": "paint", "tilt": 43 } ]
}
```

- Ölçüler bloğun on altıda biri; **x** enine, **y** yukarı, **z** öne.
- `chassis`: `car`, `pickup` (sandıklı kasa) ya da `tanker`; aracın altındaki
  makine ve eşyası bu.
- `width` / `height`: çarpışma kutusu, blok cinsinden.
- `performance`: altındaki makinenin değerlerine **çarpan**. `offroad` 0–1
  arası; toprakta, kumda, karda lastiğin kaybettiği tutuşun ne kadarını geri
  verdiği.
- `factory`: fabrikadan hangi motor, yakıt, lastik ve turboyla çıktığı.
- `seats`: binenin **kalçasının** yeri; ilk koltuk sürücü. En çok 16 koltuk.
- `wheels`: yuvarlak (16 kenarlı) teker ve jant kendiliğinden çiziliyor;
  `steers` olanlar direksiyonla dönüyor.
- `steering_wheel_rise` (isteğe bağlı): direksiyonun ortası kalçadan kaç
  piksel yukarıda. Yazılmazsa torpidoya göre kendisi bulur (en az 9). Çim
  biçme traktöründe 6: orada direksiyon karın hizasında.
- `steering_wheel_rake` (isteğe bağlı): direksiyonun dikten kaç derece
  geriye yattığı; kolonu da aynı açıyla öne iner. Yazılmazsa arabanınki
  gibi 28. Go-kartta 55: simit iyice yatık, kolon şasinin önüne dalıyor.
- `shape`: gövde kutuları (en çok 512). `color` ya `paint` / `paint_dark` /
  `paint_light` (aracın boyasını izler), sabit `#RRGGBB` ya da yarı saydam
  `#AARRGGBB` (cam böyle yapılıyor: içerisi arkasından görünür). `tilt` kutuyu
  kendi ortası etrafında yatırır — ön cam, kaput, arka cam böyle eğiliyor.
- İsim: `vehicle.<ad_alanı>.<isim>` çeviri anahtarı; yoksa dosya adından türer.

Araç tanımları dinamik kayıt olduğu için `/reload` yetmez, **dünyayı yeniden
açmak** gerekir; çok oyunculuda sunucudan istemciye kendiliğinden gider.

### Mod'la gelen araçlar

Görünüş oyuncak araba tarzında: kalın lastikli tekerler. Teker katman
katman kutulardan yapılıyor, o yüzden derinliği var: lastik yüzü, dışarı şişkin
lastik yanağı, sırt ve ortada yanaktan biraz dışarı çıkan, tekerin yarısından
büyük, düz gri bir jant; kenarında 24 küçük, ince diş var. Doku sade (16×16 piksel düzeyinde); tekerler büyüdüğü kadar gövde de
tekerin tepesiyle birlikte inip kalkıyor, kemerler tekeri hep sarıyor. Tampon, çamurluk
kaşları ve Wrangler'ın tavanı açık krem renkte; camlar açık mavi ve yarı saydam.
Üretici aracı (`tools/GenVehicles.java`) bunu her araca aynı biçimde uyguluyor:
`grow` tekerin yazılan boyuna oranı, `widen` genişliğine oranı (1.1). Tekerler
gerçek ölçüden biraz iri, yarıçapları 6.25 ile 7.5 piksel arasında: Corolla
1.18, Şahin 1.25, Hilux 1.08, Wrangler 1.04, Mustang 1.08, Atego 1.0. (Eskiden tekerin boyu
model her karede sıfırlandığı için hiç uygulanmıyordu, bütün tekerler aynı iri
boyda çiziliyordu; artık `setAngles` her karede yeniden uyguluyor.)
Gövde boyuna esnetilerek çiziliyor: her teker kemerinin çevresi yazıldığı gibi
kalıyor, aks arası `between` ve aks dışındaki çıkıntılar `overhang` oranında
uzatılıp kısaltılıyor, böylece kemerler tekerleri sarmaya devam ediyor. Eskiden
araçlar oyuncak gibi kısaltılıyordu (0.78 ve 0.55; Atego 0.88 ve 0.75), ama
tıknaz hâlleriyle hepsi yüksekte duran birer hatchback gibi görünüyordu. Artık
otomobillerin ve kamyonun hepsi aynı oranla uzatılıyor (`LONG_BETWEEN` 1.08,
`LONG_OVERHANG` 1.12). Yükseklik ve tekerler aynı kaldığı için yerden yükseklik
değişmedi. Römork ve iki tekerliler eski ölçüsünde.

| Araç | Önce (blok) | Şimdi (blok) | Aks arası |
|---|---|---|---|
| Şahin | 3.9 | 4.9 | 3.0 |
| Corolla | 4.1 | 5.2 | 3.1 |
| Mustang | 4.2 | 5.3 | 3.2 |
| Wrangler | 4.3 | 5.4 | 3.2 |
| Hilux | 4.8 | 6.3 | 3.7 |
| Atego | 5.8 | 6.8 | 4.1 |

Çeki demiri (`hitchPoint`) ve depo kapağı gövdenin arka ucundan ve arka akstan
hesaplandığı için kendiliğinden yeni yerlerine geçiyor.

Hepsi yukarıdaki yoldan, sıradan veri olarak geliyor (`tools/GenVehicles.java`
çizip JSON'u yazıyor) ve gerçek ölçü oranlarına göre çizildi. Direksiyon solda.
İlk eklenen deneme arabası yok: her iskeletin varsayılanı gerçek bir araç.

| Araç | Altındaki | Varsayılan mı | Fabrika çıkışı |
|------|-----------|----------------|----------------|
| Toyota Corolla | araba | araba eşyasının varsayılanı | sıralı 4, benzin, yol lastiği |
| Tofaş Şahin | araba | | sıralı 4, benzin |
| Jeep Wrangler | araba | | V6, benzin, arazi lastiği |
| Ford Mustang | araba | | V8, benzin, spor lastik |
| Toyota Hilux | kamyonet | kamyonetin varsayılanı | sıralı 4 turbo dizel |
| Mercedes-Benz Atego | tanker | tankerin varsayılanı | dizel kamyon |
| Honda CBR600RR | motosiklet | motosikletin varsayılanı | sıralı 4, benzin, spor lastik |
| Vespa Primavera | motosiklet | | sıralı 4 (küçük), benzin, yol lastiği |
| Kasalı römork | römork | römorkun varsayılanı | motoru yok |

Yaratıcı sekmede hepsi ayrı duruyor; hayatta kalmada hepsi
[Araç İstasyonu](#araç-i̇stasyonu)'nda yapılıyor.

Mustang, 1967 fastback çizgisinde: uzun kaput, tavandan kuyruğa tek çizgide
inen arka cam, kalkık kuyruk, her yanda üç stop lambası ve kaputtan kuyruğa iki
beyaz şerit. Yarış arabası: en hızlısı ve en çok yakanı.

Tekerlekler takılı lastiğe göre değişiyor (yol: gümüş jant, arazi ve kış:
tırtıklı kara lastik, spor: ince kollu jant) ve Minecraft piksel dokulu.


## Araç İstasyonu

Bitmiş aracı **burada** yapıyorsun. Eskiden her araç için ayrı şekilsiz tarif
vardı; artık hepsi tek bir blokta, MrCrayfish'in iş istasyonu gibi:

```
P M P      P = kâğıt, M = mavi boya
I C I      I = demir külçe, C = çalışma tezgâhı
K D K      K = kırmızı boya, D = demir blok
```

Pencerede solda araç listesi (fare tekerleğiyle kayar), sağda seçili aracın
yavaşça dönen 3B önizlemesi, altında renk paleti (ilk kutu fabrika rengi, sonra
16 boya) ve en altta malzemeler. Eksik malzemenin sayısı kırmızı. Motor ve
lastik sarı yazılıyorsa isteğe bağlı demek: yoksa araç onlarsız çıkar, liftte
sonradan takarsın. Renk seçersen bir boya da harcanıyor.

Malzeme = iskeletin parçaları (gövde, motor, şanzıman, süspansiyon, lastik;
kamyonete kasa, tankere varil) + aracın tanımındaki `materials` listesi.
Kullandığın parçaların sınıfı (ekonomik/standart/performans) ve motor tipi
araca geçiyor, tıpkı liftte olduğu gibi.

| Araç | İskelet parçalarına ek |
|------|------------------------|
| Tofaş Şahin | 2 demir külçe, 1 demir parmaklık |
| Jeep Wrangler | 2 demir blok, 2 demir parmaklık, 2 deri |
| Ford Mustang | 1 demir blok, 2 demir külçe, 1 kızıltaş bloğu |
| Vespa | motor, şanzıman, 2 süspansiyon, 2 lastik + 2 demir külçe, 1 deri |
| Honda CBR | motor, şanzıman, 2 süspansiyon, 2 lastik + 3 demir külçe, 2 kızıltaş, 1 deri |
| Kasalı römork | 2 süspansiyon + 1 sandık, 3 demir külçe, 3 meşe basamak |

Araç Tezgâhı parçaları yapıyor ve yeniliyor; lift yalnızca kaldırıyor ve tamirin yapıldığı yer.

## Anahtar

`Araba Anahtarı` (siyah boya + kızıltaş + demir parça) boş çıkar. Boş anahtarla
araca sağ tıkla: araç daha anahtarlı değilse o araca kesilir; anahtarlıysa ve
sende onun anahtarı varsa kopyası çıkar. Anahtarı üstünde olmayan sürücünün
aracı çalışmaz (yaratıcı mod hariç). Anahtarla eğilip tıklamak kilitler/açar;
kilitli araca binilmez, parçasına dokunulmaz.

## Sprey boya

Sprey kutusu (`N/I/I`: demir parça + 2 demir külçe) 10 sıkımlık. Kutuyu boyalarla
çalışma tezgâhına koy: deri zırh boyamasıyla aynı karışım, yani 16 boyanın her
karışımı. Dolu kutuyla araca sağ tıkla, araç tam o rengi alır; boyadan farklı
olarak ara tonlar da çıkar. Sprey rengi araç eşyasında saklanıyor, ikon da o
renkte.

## Sürüş: drift

- **Drift**: hızlıyken `Space` + direksiyon. Arka kayar, araç daha keskin döner,
  arka tekerlerden duman ve lastik sesi çıkar.
- Hız pedi ve rampa kaldırıldı: arcade parçalarıydı, bir tamirci/sürüş moduna
  uymuyorlardı.

## Römork

Kasalı römorku aracın arkasına koy ve boş elle tıkla: en yakın aracın arka
tamponuna bağlanır. Tekrar tıklayınca çözülür. Eğilip tıklayınca 27 slotluk yük
açılır. Fizikle değil, gerçek tek akslı römork gibi çekiliyor: kaplin her tik
aracın çeki topuna konuyor ve römork ona dönüyor, virajda geniş döner, düzlükte
arkaya hizalanır. Hem sunucu hem istemci hesaplıyor, sürücü römorkun bir tik
geriden geldiğini görmüyor. Boş römork çekenin azami hızını %92'ye, dolu olan
%78'e düşürür. Araç kaybolursa veya römork 6 bloktan fazla geride kalırsa
bağlantı kopar. Kayıt/yüklemede bağlantı korunuyor.

## İki tekerliler

Motosiklet iskeleti arabanın aynısı: motor, yakıt, parçalar, anahtar, boya.
Farkı dar gövde, sürücü + arkada yolcu ve virajda yatması (hızla ve direksiyonla
artıyor, en fazla ~30°). Motor sesi daha tiz.

- **Vespa Primavera**: küçük tekerli, bacak kalkanlı, iki kişilik selesi olan
  İtalyan scooter. Yavaş ama çok az yakar, çok çevik döner.
- **Honda CBR600RR**: tam kaportalı spor motor. Arabalardan hızlı, en çabuk
  hızlanan araç.

Denizde/havada giden araçlar daha sonra.

## Animasyonlar ve 3B eşyalar

**3B eşyalar.** Sprey boya ve araba anahtarı artık düz resim değil, kutulardan
yapılmış model: sekizgen gövdeli, etiketi ve kapağı boyanın rengini alan bir
kutu; plastik başlı, kilit/açma tuşlu, dişli bir anahtar ve halkası. Elde, yerde
ve envanterde ayrı açılarla duruyorlar. Dokular `tools/GenItems3D.java` ile
üretiliyor.

**Sürücü araçla birlikte.**

- Motosiklet virajda yattığında **üstündeki oyuncu da aynı eksende yatıyor**
  (oyuncu çizimine bir mixin: `LivingEntityRendererMixin`). Eğim, motorun
  çizildiği formülle aynı (`CarEntityRenderer.leanDegrees`), dönme ekseni de
  aynı (sele hizası). Bu yüzden sürücü selede kalıyor.
- Sürücünün elleri kumandalarda (`BipedEntityModelMixin`). Arabada eller
  direksiyonun üstünde. Sağa kırınca sol el kalkıyor, sağ el iniyor. Motorda
  eller gidonda ve gidon dönünce biri ileri itiyor, öbürü geri çekiyor. Dizler
  de içe toplanıyor. Bir şey kullanırken ya da vururken eller serbest kalıyor.
- Tanımlı her kabinli araçta (araba, kamyonet, tanker) sürücü koltuğunun önünde
  **dönen bir direksiyon** var. Simidi, kolları, göbeği ve kolonu var; ön
  tekerden 2.5 kat fazla dönüyor. Torpido sürücüye normal uzanma mesafesinden
  yakınsa direksiyon torpidonun hemen önüne çekiliyor, içine gömülmüyor.
- **Sürücü artık camın arkasından görünüyor.** Önceden cam oyuncudan önce
  çiziliyor ve onu derinlikte siliyordu. Şimdi tanımlı araçların camı ayrı bir
  model ve sonraki bir çizim grubunda geliyor (`getBatchingQueue(1)`). Kaporta
  `entityCutoutNoCull` ile katı çiziliyor.

**İki tekerlilerde gidon.** Araç tanımındaki kutulara isteğe bağlı
`"steers": true` eklendi. Böyle işaretlenen kutular (çatal, ön çamurluk, far
kafası, gidon, aynalar) ön tekerle birlikte dönüyor. Dönme ekseni ön göbekten en
yüksek dönen kutuya çekilen çizgi, yani çatalın kendi eğimi (rake). Bu sayede
gidon yerinde dönüyor, kaymıyor.

**Park pozları.** Bunlar istemci tarafında yumuşakça oturuyor:

- Kimse binmeyen motor yan sehpasına yatıyor (9°, tekerin yere değdiği çizgide).
- Bağlı olmayan römork burnunu jokey tekerine indiriyor (6.5°, aksın etrafında).
- Takınca ikisi de doğruluyor.

**Kilit flaşörü.** Anahtarla kilitleyince sinyaller bir kez, açınca iki kez
yanıp sönüyor. Kıvılcım tasarımdaki her turuncu lambadan çıkıyor; turuncu lambası
olmayan araçta dört köşeden. Sunucu bunu bir entity status ile bildiriyor
(`STATUS_LOCKED` / `STATUS_UNLOCKED`).

**Hareketli bloklar.**
Araç İstasyonu'nun üstündeki planın üzerinden bir ışık geçiyor (16 kare).

**İstasyon ekranı.**

- Standdaki araç kendi kendine dönüyor. **Fareyle sürükleyerek** de
  çevrilebiliyor.
- Yeni bir araç seçince standa küçük bir sekmeyle oturuyor.
- İnşa edince araç bir an hızla dönüyor, ışık parlayıp sönüyor ve kıvılcımlar
  saçılıyor.

## Kamyonet

Tanker "yakıtı dünyanın öbür ucuna nasıl taşırım"ın cevabıydı. Kamyonet aynı
sorunun diğer yarısı: **geri kalan her şeyi**.

Kabin arabanın kabini — aynı koltuklar, aynı direksiyon, aynı oturma
matematiği. Arkası yeni: tabanı, dört duvarı ve mandalı olan açık bir kasa, onu
taşıyan şasi kızağı ve kasanın altına kaymış arka aks.

### Kasa bir sandıktır

Çömelerek sağ tıkla **27 gözlü** sandık ekranı açılıyor — vanilla'nın kendi
ekranı, çünkü yirmi yedi göz için kendi ızgarasını uyduran mod hiçbir şey
uydurmamış olur. Düz sağ tık hâlâ araç: biniyorsun, bidon döküyorsun,
boyuyorsun, tamir ediyorsun.

### Üstünden geçtiğini kendi topluyor

Kamyonetin değdiği yerdeki eşyalar kasaya giriyor. Madenden dönerken onu
sürmenin bütün sebebi bu: durmuyorsun, bir şey açmıyorsun, düşenler arkada
birikiyor.

Hiçbir şey kaybolmuyor: sığmayan yerde kalıyor, dolu bir kamyonet kendi
ganimetinin üstünden geçip gidiyor.

### Yük hızı düşürüyor

| Kasa | Azami hız |
|------|-----------|
| Boş | arabanın **%94**'ü |
| Dolu | arabanın **%74**'ü |

Arası düz bir çizgi, yani yükü bir yerde sayı okumadan hissediyorsun.

Ceplendiğinde ya da pert olduğunda kasa olduğu yere boşalıyor — kırılan bir
sandık gibi. Yük kasada duruyor, eşyada değil.

Tarif (araç liftinde): 1 gövde, 1 motor, 1 şanzıman, 1 süspansiyon, 4 lastik ve
tezgâhta yapılan 1 kasa (6 meşe kalası + 3 demir külçe).

### Model Blockbench'te çizildi

Bu araç [web.blockbench.net](https://web.blockbench.net/) üzerinde **Modded
Entity** formatında, 256×256 dokuyla kuruldu; proje dosyası
[`tools/pickup.bbmodel`](tools/pickup.bbmodel) olarak duruyor, açıp
kurcalayabilirsin.

Blockbench ile buradaki Java arasındaki tek çeviri Y ekseni:
`addBox(x, jy, z, sx, sy, sz)` ile Blockbench kutusu arasında
`from.y = -(jy + sy)`, `to.y = -jy`; x ve z aynen geçiyor. Bu, arabanın hâlihazırda
doğru olan gövdesi Blockbench'e taşınıp geri dışa aktarılarak doğrulandı —
aynı sayılar geri geldi.

**Çalışmayan yaklaşım:** kasanın altındaki kızak önce tankerinki gibi
şasinin hizasına (y −10…−6) kondu. Kasa tabanı −12'de bittiği için arada iki
birimlik boşluk kaldı ve oyunda araç, aynı yere park etmiş *iki* araç gibi
göründü. Hiçbir test bunu yakalayamazdı; ekran görüntüsü yakaladı. Kızak
tabana dayandı (y −12…−8) ve gövde rengine boyandı.

## Gösterge paneli

Arabaya binince eşya çubuğunun iki yanında, ekranın altında tek sıra hâlinde
analog kadranlar beliriyor: solda hızölçer ve onun dışında **devir saati**,
sağda yakıt ve onun dışında küçük sıcaklık kadranı.

**Devir saati:** otomatik şanzıman (`Gearbox.tick/getEngineRpm`); vites
sayısı ve oranları aracın tanımından gelir (`performance.gears`, her vitesin
kırmızı çizgide ulaştığı azami hız payı, sonuncusu 1): verilmezse 5 ileri,
Mustang/Jeep/Hilux/CBR 6, Atego 8, Vespa tek oranlı (CVT). Kısa alt vitesler,
uzun üst vitesler; devir kırmızı çizginin %90'ına gelince vites
büyür, %36'nın altına düşünce küçülür. Her geçiş 6 tik sürer: çekiş kısa bir an
kesilir, devir yeni vitese iner, motor sesi alçalıp kısılır, sonra yeniden çeker.
Ortadaki pencerede vites (`R`, `1`–`8`).

Her vitesin itişi beşlinin kendi değerlerinden okunur (aradakiler logaritmik
ara değer), sonra **motorun tork eğrisiyle** çarpılır (`CarSetup.torque`):
sıralı dört yüksek devirde (%70), sıralı altı ve V6 ortada, V8 aşağıda (%50),
dizel erkenden (%40) tepe yapar; elektrik motoru kalkışta tam iter, %45'ten
sonra gücü tükenir. Eğri vites aralığında ortalama 1'dir: araç toplamda aynı
yolu alır, yalnızca farklı hisseder.

| | Hızölçer | Yakıt | Sıcaklık |
|---|---|---|---|
| Ölçek | 0'dan aracın azami hızının biraz üstüne, 30'un katına yuvarlanmış | E – ¼ – ½ – ¾ – F | rakamsız |
| İbre | mutlak hız (geri viteste de ileri gösterir) | depodaki yakıt | motor sıcaklığı |
| Uyarı | %35'in altında anahtar ikonu kırmızı yanar | %10'un altında pompa ikonu kehribar yanar | kaynarken termometre kırmızı yanar |

Sıcaklık kadranında rakam yok, çünkü gerçek bir araba da sana soğutma suyunun
kaç derece olduğunu söylemiyor — ibrenin kırmızıda olup olmadığını söylüyor.
Kırmızı bölge, motorun gerçekten güç kaybetmeye başladığı orana (`OVERHEAT_AT`)
baskılı: ibrenin bandın içine girmesiyle gücün düşmesi aynı olay.

Üçüncü kadran büyük yüzün küçültülmüşü değil, **kendi boyutunda** (48 piksel)
baskılanıyor: yeniden örneklenmiş bir çerçeve, yanındaki iki keskin çerçevenin
arasında hemen belli olurdu. Sağ tarafta duruyor çünkü sol taraf sohbetin yeri.

Hız, arabanın gerçek sürat değerinden geliyor ve dünyanın kendi biriminde,
**blok/saniye** okunuyor: bir saniye yirmi tik, yani `blok/tik × 20`. Kadranın
sonu sabit değil: `getRatedTopSpeed()` — aracın gövdesi, motoru, yakıtı,
turbosu ve parça sınıfları, aşınma hariç — ×1,12 ve 3'ün katına yuvarlanmış.
Fabrika Corolla 15'te biten bir kadran görüyor; turbolu, performans motorlu bir
V8 daha uzununu. Kadranın altındaki yazılar (sayaç, menzil) kadrana sığmazsa
küçülüyor.

İbreler ani zıplamıyor: her kare hedefe doğru `NEEDLE_RESPONSE` kadar
yaklaşıyorlar, kare süresi hesaba katıldığı için hız salınımı FPS'ten bağımsız.
Araçtan inince ibreler sıfırlanıyor, böylece bir sonraki binişte gerçek
göstergeler gibi sıfırdan süpürerek yerine oturuyorlar.

Kadranın değişmeyen kısmı — çerçeve, kadran yüzü, kademe çizgileri, kırmızı
bölgeler ve sönük ikonlar — PNG'ye baskılı (devir saatinin çizgileri ve kırmızı
bölgesi motora göre değiştiği için her karede çiziliyor). Yalnızca ibre, rakamlar
ve yanan uyarı ikonu her karede çiziliyor; aksi hâlde sabit bir resim için her
karede birkaç yüz çizim çağrısı harcanırdı.

Kadranlar hiçbir zaman üst üste dizilmiyor; küçük pencerede hepsi birlikte
küçülüyor. Ölçek iki sınırın küçüğü: sıra eşya çubuğunun iki yanına sığmalı ve
ekran yüksekliğinin en fazla %22'sini kaplamalı (`MAX_HEIGHT_SHARE`). Büyük
ekranda tam boyda kalıyorlar. Ölçek 0,45'in altına inecekse rakamlar
okunmayacağı için kadranlar hiç çizilmiyor.

### Ayarlar

| Yer | Sabit | Etkisi |
|-----|-------|--------|
| `DashboardHud` | `DIAL_STEP_KMH`, `DIAL_HEADROOM` | Hız kadranının yuvarlama adımı ve azami hızın üstündeki pay |
| `vehicle/*.json` | `performance.gears` | Aracın vites sayısı ve oranları |
| `CarSetup` | `UPSHIFT`, `DOWNSHIFT`, `torque` | Vites atılan devir oranları ve tork eğrisi |
| `DashboardHud` | `NEEDLE_RESPONSE` | İbre yumuşatması (büyük = daha çevik) |
| `DashboardHud` | `WARNING_FRACTION` | Pompa ikonunun yandığı yakıt oranı |
| `CarEntity` | `DAMAGE_WARNING_FRACTION` | Anahtar ikonunun yandığı hasar oranı |
| `DashboardHud` | `HOTBAR_GAP` | Kadranla eşya çubuğu arasındaki boşluk |
| `GenGaugeTextures` | `RADIUS`, `SWEEP` | Kadran boyutu ve yay açıklığı |

Geometri sabitleri `DashboardHud` ile `tools/GenGaugeTextures.java` arasında
aynı adlarla tekrarlanıyor — birini değiştirince dokuları yeniden üret:

```bash
java tools/GenGaugeTextures.java src/main/resources/assets/car/textures/gui
```

## Hasar ve tamir

Arabanın 20 puanlık bir durumu var ve bu durum eşyaya da yazılıyor — hırpalanmış
bir arabayı toplayıp başka yere kurduğunda hırpalanmış hâlde kuruluyor. Eşyanın
ipucu balonunda `Durum: %74` gibi bir satır çıkıyor, %35'in altında kırmızı.

| Olay | Hasar |
|------|-------|
| Çarpışma | `(engelin yuttuğu yol − 0.15) × 18` |
| Düşme | 3 bloktan sonrası için blok başına 1.5 |
| Patlama, yangın, öfkeli yaratıklar | vanilla hasar değeri kadar |
| Oyuncunun vurması | hasar değil — arabayı toplar |

Tamir bir [araç lifti](#araç-lifti) istiyor: demir külçe yolda işe yaramıyor.

| Çarpma | Hasar |
|--------|-------|
| Tam gazla kafa kafaya | ~8.5 puan |
| Yarım gazla kafa kafaya | ~2.9 puan |
| Tam gazla 45° sıyırma | ~0.5 puan |
| Tam gazla 30° sıyırma | yok |
| 10 bloktan düşme | 10.5 puan |

Yani tam gazla üç kere duvara girmek arabayı pert ediyor. Çarpmada araba yerinde
duruyor, bir de yarım saniye (10 tik) hasar muafiyeti var — aksi hâlde gaza
basılı tutan bir oyuncu aynı duvardan her tikte fatura yerdi.

**Çarpışma, arabayı o an simüle eden tarafta yakalanıyor.** Biri sürerken fizik
o oyuncunun istemcisinde işliyor ve sunucuya yalnızca sonuçtaki konum geliyor;
yani çarpmayı yalnızca o istemci görüyor. Dolayısıyla çarpışmayı istemci
yakalayıp `car:crash` paketiyle durumu tutan sunucuya bildiriyor. Kimse
sürmüyorsa (yamaçtan kendi kendine yuvarlanan araba) simüle eden taraf sunucunun
kendisi oluyor ve hasarı doğrudan uyguluyor. Şiddet değeri istemciden geldiği
için sunucu onu `MAX_HEALTH` ile sınırlıyor ve yalnızca arabanın kendi
sürücüsünden kabul ediyor — en kötü hâlde bir oyuncu bindiği arabayı pert eder,
ki duvara girerek de yapabileceği bir şey.

Şiddeti `horizontalCollision` ile birlikte **engelin yuttuğu yol** veriyor:
`move()`'dan istenen yatay mesafeyle gerçekten katedilen mesafe arasındaki fark.
Bu, çarpmayı kendiliğinden derecelendiriyor — kafa kafaya çarpışma her şeyi
yutuyor, duvar boyunca sıyırma neredeyse hiçbir şey.

> **Not — çalışmayan yaklaşım.** İlk hâli sunucuda bir tikte kaybolan hızdan
> çarpışma çıkarıyordu. Bu çalışmıyor: araç konum paketleri sunucu tikiyle
> birebir hizalı gelmiyor, bir tike hiç paket düşmediğinde ölçüm sıfır çıkıyor
> ve dümdüz boş yolda giden araba pert oluyordu. Çarpışma bayrağı gerçek
> sinyal; konum farkı değil.

Hırpalanan araba:

- **Yavaşlıyor.** Üst hız durumla birlikte düşüyor, boş durumda `MAX_SPEED`'in
  yarısına kadar — ama oraya varmadan dağılıyor. %35'te ~8 blok/sn.
- **Tütüyor.** %35'in altında en çok ezilen tarafın üstünden duman çıkıyor
  (kafa kafaya sonrası kaputtan, geri geri vurunca bagajdan), %15'in altında
  duman kalınlaşıyor.
- **Uyarı veriyor.** Hızölçerdeki anahtar ikonu kırmızı yanıyor.

Durum sıfıra inince araba **dağılıyor**: örs kırılma sesi, bir bulut duman ve
yerde 2 demir külçe. Araba eşyası geri gelmiyor — yani arabayı pert etmek bir
bedele mâl oluyor. Elle toplamak ise arabayı ezikleriyle birlikte sapasağlam
geri veriyor.

**Tamir:** elinde demir külçeyle arabaya sağ tıkla, külçe başına 5 puan. Boş
durumdan tama dört külçe gidiyor. Araba sapasağlamsa tıklama normal biniş
davranışına düşüyor, külçe boşa harcanmıyor.

### Nereden çarptığın önemli

Gövde tek bir sayı değil, dört taraf: **ön, arka, sol, sağ**
([`BodyZone`](src/main/java/me/trossty/car/entity/BodyZone.java)). Her çarpma
tek bir tarafa iniyor ve o tarafın arkasındaki parçaları yoruyor:

| Taraf | Arkasında yorulan |
|-------|-------------------|
| Ön | **radyatör**, ön amortisörler; direksiyon ayarı kayar. Depo hiç delinmez (arada motor var). |
| Arka | **egzoz**, arka amortisörler; depo delinebilir. Arka tekerler dönmediği için ayar bozulmaz. |
| Sol / Sağ | o taraftaki **lastikler** ve amortisörler; araba vurulan tarafa çeker. Depoya yarı olasılıkla ulaşır. |

- **Tarafı ne belirliyor?** Sürerken çarptığın yön. Vurulunca vuruşun geldiği
  yer: yumrukta oyuncu, okta ok, patlamada patlamanın merkezi. Bir nokta ön tekerin yanındaysa ön değil yan
  sayılıyor: araba eninin iki katı uzunluğunda kabul ediliyor
  (`LENGTH_TO_WIDTH`). Düşme gibi yönü olmayan hasar dört tarafa birden iniyor,
  yani pert olma süresi eskisiyle aynı.
- **Pert olmak için bir tarafın bitmesi yetiyor.** Önü 3'e düşmüş arabaya
  arkadan 4 vurmak bir şey yapmaz; önden 4 vurmak dağıtır. Servis etiketi ve
  Jade satırı en kötü tarafı adıyla yazıyor ("Gövde · %15 (ön)").
- **Çekiç durduğun tarafa iniyor.** Külçeyle tıklayınca önce yanında
  durduğun taraf, o sağlamsa en kötü taraf düzeltiliyor; aksiyon çubuğu hangi
  tarafın kaç olduğunu söylüyor.
- **OBD tarafı söylüyor.** %35'in altındaki her taraf ayrı kod: B1001 ön,
  B1002 arka, B1003 sol yan, B1004 sağ yan.
- Eski kayıtlardaki tek `Health` değeri yüklenirken dört tarafa birden
  yazılıyor; yeni kayıt `Body` dizisi.

### Ayarlar

| Yer | Sabit | Etkisi |
|-----|-------|--------|
| `CarEntity` | `MAX_HEALTH` | Durum puanı |
| `BodyDamage` | `MIN_CRASH_SPEED` | Çarpışma sayılması için engelin yutması gereken yol |
| `BodyDamage` | `CRASH_COOLDOWN` | Çarpma sonrası hasar muafiyeti (tik) |
| `BodyDamage` | `CRASH_DAMAGE_PER_SPEED` | Çarpışmanın sertliği |
| `CarEntity` | `SAFE_FALL_BLOCKS` | Hasarsız düşme yüksekliği |
| `CarEntity` | `WRECKED_SPEED_FACTOR` | Boş durumdaki üst hız çarpanı |
| `BodyDamage` | `REPAIR_PER_INGOT` | Bir külçenin tamir ettiği puan |
| `BodyZone` | `LENGTH_TO_WIDTH` | Bir noktanın uca mı yana mı sayılacağını belirleyen boy/en oranı |
| `BodyDamage` | `STRUCK_SPRING_WEAR` | Vurulan taraftaki amortisörlerin hasar başına aşınması |
| `BodyDamage` | `RADIATOR_PER_DAMAGE` | Önden çarpmada radyatörün hasar başına aşınması |
| `BodyDamage` | `EXHAUST_PER_DAMAGE` | Arkadan çarpmada egzozun hasar başına aşınması |
| `BodyDamage` | `KERBED_TYRE_WEAR` | Yandan çarpmada o taraftaki lastiklerin aşınması |
| `BodyDamage` | `SIDE_TANK_EXPOSURE` | Yandan çarpmanın depoya ulaşma payı (arkadan 1, önden 0) |
| `BodyDamage` | `WRECK_SCRAP` | Dağılan arabadan düşen hurda |

## Sesler

Motor ve korna sesleri modun kendi sentezlenmiş `.ogg` dosyaları — vanilla sesi
veya yer tutucu kullanılmıyor.

| Dosya | İçerik |
|-------|--------|
| `assets/car/sounds/engine.ogg` | 70 Hz temel frekans + 8 harmonik, kusursuz 1.000 sn döngü |
| `assets/car/sounds/horn.ogg` | 440 Hz + 554.37 Hz iki sesli akor, testere dişi harmonikler, 0.650 sn |

**Yol sesi** ayrı bir dosya değil: tekerin altındaki bloğun kendi adım sesi,
araba döndükçe çalınıyor (`client/sound/RoadSurface`). Ses grubuna bakıldığı
için başka modun blokları da kendi seslerini veriyor. Çakıl en sık ve en yüksek,
kum/toprak/çamur, sonra çim ve kar, tahta; taş (asfaltın Minecraft'taki
karşılığı) neredeyse sessiz. Aynı pürüzlülük değeri, hızla çarpılıp sürücünün
kamerasını hafifçe sarsıyor (`mixin/client/CameraMixin`).

İkisi de **mono**, 44.1 kHz, 16-bit. Mono olmaları şart: Minecraft yalnızca tek
kanallı sesi konuma göre sağa/sola yayar ve uzaklığa göre kısar. Stereo bir dosya
her yerde tam sesle çalar — yani araba sesi haritanın öbür ucundan duyulurdu.

### Yeniden üretmek / değiştirmek

Kaynak WAV'lar ve onları üreten kod `sounds-src/` içinde:

```bash
cd sounds-src && javac GenSounds.java && java GenSounds
```

Sonra tekrar `.ogg`'ye çevir:

```bash
oggenc -q 6 -o ../src/main/resources/assets/car/sounds/engine.ogg engine.wav
```

`oggenc` yoksa: `brew install vorbis-tools`.

### Perde ayarı

| Yer | Sabit | Etkisi |
|-----|-------|--------|
| `CarEngineSoundInstance` | `IDLE_PITCH` = 1.0 | Rölanti notası (1.0 = sentezlendiği hâli, 70 Hz) |
| `CarEngineSoundInstance` | `MAX_PITCH` = 2.0 | Tam gazdaki perde (2.0 Minecraft'ın üst sınırı) |
| `CarEngineSoundInstance` | `IDLE_VOLUME` / `MAX_VOLUME` | Ses yüksekliği aralığı |
| `CarEngineSoundInstance` | `SMOOTHING` | Perdenin hıza ne kadar çabuk uyduğu |
| `CarEntity.honk()` | `1.4F, 1.0F` | Korna sesi ve perdesi |

## Elektrik, teşhis ve yol

[GELISTIRME.md](GELISTIRME.md)'deki listenin uygulanan kısmı. Her madde bir
gametest ile korunuyor (`./gradlew runGameTestServer`, şu an 39 test).

### Akü bankası ve şarj direği

- **Akü bankası** 200 000 E tutar, tick başına 500 alır/verir. Yalnızca
  **önünden** verir, diğer beş yüzünden yalnızca alır: arkasına jeneratör
  hattı, önüne tüketici hattı bağlanır, enerji geri sızmaz. Önündeki dört
  çubuk dolulukla yanar; kırınca şarjı eşyada kalır (ipucunda yazar),
  komparatör doluluğu okur.
- **Şarj direği** kablodan beslenir ve 3,5 blok içindeki en yakın elektrikli
  aracı tick başına 200 E ile doldurur; şarj ederken ekranı ve lambası yanar.
  Benzinli araca dokunmaz.

### Elektrikli araç

Motor tezgâhında / serviste motorun dokuzuncu türü **elektrik motoru**. Takılınca
depo aküye döner (yakıt kapağı yok, bidon "dolum ağzı yok" der), buji, hava
filtresi, yağ filtresi ve egzoz teşhiste aranmaz, ses elektrik vınlamasına
geçer. Devir sınırı 4500.

### Kilometre, aşınma ve OBD tableti

- Araç sürüldükçe **sayaç** blok blok artar **kiloblok** cinsinden (1 kb = 1000 blok,
  iki ondalıklı: 57 blok `0.06 kb`), kayıtta saklanır. Yalnızca tekerlekler yerdeyken sayar. 50 000 bloğa kadar her parça %50'ye varan oranda daha hızlı aşınır;
  yarı ömrünü bitirmiş bir parça kendi başına %25 daha hızlı gider. Servisi
  ertelemek servisin kendisinden pahalıya gelir.
- **OBD tableti** gerçek bir atölye tableti gibi **3B**: kalın gövde, turuncu
  kauçuk köşe tamponları, arkasında kamera, tutma bandı ve teşhis kablosunun girişi. Envanterde,
  yerde ve çerçevede katı bir model; üçüncü şahısta ekranı oyuncuya dönük
  tutulur.
- Tablet sohbete yazmaz: elde **harita gibi** tutulur (öbür el boşsa
  iki elle, aşağı bakınca kalkar). Ekranı aracı canlı okur: model adı,
  kilometre, yakıt, yakıt/şarj, kaporta ve motor ısısı çubukları, altında
  arıza kodları en kötüsü önde — kritik kırmızı, uyarı turuncu, tavsiye sarı;
  sığmayanlar "…ve N tane daha".
- Tablet bir araca tutulunca o araçla **eşleşir** ve 32 blok içinde onu okur;
  araca binince her zaman bindiğin aracı okur. Eşleşme yoksa ya da araç
  uzaktaysa ekranda "Eşleşmiş araç yok" / "Sinyal yok" yazar. Ekran
  karanlıkta da parlar.
- Kodlar gerçekçi: P0420 egzoz, C0040 fren balatası, P0217 hararet, P0172
  kirli yakıt, P2181 radyatör, P2600 devirdaim, P0128 termostat, P0087 yakıt
  pompası, P0201–P0208 enjektör, P0351–P0358 bobin, C0070 kaliper, C0545
  rotil, B1001–B1004 kaporta (ön, arka, sol, sağ)… Okuyucu yüzde söylemez; bir sensörün sınır
  dışına çıktığını bilir, kaç milim balata kaldığını değil.

#### Köşe ve silindir başına aşınma

Lastik, kaliper, balata, disk, amortisör ve rotil her tekerlekte; buji, bobin
ve enjektör her silindirde ayrı aşınır (`PartPositions`). Servis ekranı ve işaret takımı setin ortalamasını
gösterir, nişan ipucu tek tek köşeleri (`SÖ 82  SğÖ 80 …`, bujide `1 90  2 88`).

- Önden frenleme işin çoğunu yapar: ön balatalar ~1.4, arkalar ~0.6 payla
  aşınır. Her kaliper biraz farklı tutar (araca özgü ±%12).
- Kotu bozuk aracın ön lastikleri, inik lastik kendisi, conta yanığının iki
  silindirindeki bujiler daha hızlı gider.
- Bir köşesi zayıf fren, frenlerken aracı **güçlü tarafa çeker**; yorgun bir
  amortisör düz yolda kendi tarafına.
- Lastik takarken yeni lastik **kendi köşesine** gider, diğerleriyle ortalanmaz.
- V6/sıralı altı 6, V8 8 buji ister.

#### Birden çok olası sebep

Bazı kodlar parçayı değil belirtiyi gösterir; tablet altına "Olası: …" yazar:

| Kod | Anlamı | Olası |
|-----|--------|-------|
| P0301–P0308 | O silindirde tekleme | Buji, bobin, enjektör, motor (kompresyon); dizelde enjektör, motor |
| P0300 | 3+ silindirde rastgele tekleme | Buji, bobin, enjektör, motor |
| P0730 | Vites oranı hatalı (kaçırma) | Şanzıman, debriyaj |
| C0045 | Fren titremesi (köşe) | Disk, balata |

C0040/C0750/C0690 en kötü köşeyi adıyla söyler. Ayırmak oyuncunun işi:
bobin ve enjektör kendi devre kodunu da (P035N, P020N) yanına düşürür;
bujileri sökünce kompresyon ölçer contanın iki silindirini, eğik supaplı ya da
segmanı bitmiş silindiri gösterir; kompresyonu tamam ama teklerse bujidir.

#### Sökmeden görünmeyen aşınma

Bir parçanın üstüne başka parça takılıysa aşınması **görünmez**: servis
ekranında `?`, işaret plakasında `?`, ipucunda "Görünmüyor: önce X sökülmeli".
Debriyaj şanzıman ve şaft çıkmadan, motor bujiler çıkmadan görülmez. Balata
ve disk için bir tekerleği sökmek yeter (disk balatanın arkasından görünür).

### Yakıt kalitesi

Rafineri, ürün deposunda ham benzin ya da ham dizel varken yuvasındaki bidonu
da doldurur; bidon **ham** diye işaretlenir (ipucunda kırmızı). Araç ham
yakıtla çalışır ama depo karışımı hacimce izler: tamamen ham bir depoda motor
3 kat, bujiler 4 kat hızlı aşınır. Pompa ham bidonu kabul etmez; hidrojenle
arıtıcıya tutulan ham bidon onun giriş deposuna boşalır. Depo boşalınca
kirlilik de sıfırlanır.

### Asfalt ve yol boyası

Rafineri ham petrolü her kırışında **katran** bırakır (ekranında bidonun
altındaki yuva; huniyla çekilebilir). Tarif: ortada katran, köşelerde çakıl,
kenarlarda kum → 16 asfalt.

- **Kar gibi serilir.** Her asfalt 1/8 blok yüksekliğinde bir katman;
  asfaltın üstüne tekrar koyunca kalınlaşır, 8 katman tam blok olur. Böylece
  yokuş, aracın tırmanabileceği basamaklarla yapılır. Kırınca kaç katmansa o
  kadar asfalt düşer.
- **Yol boyası** dört renkte gelir: beyaz, sarı, kırmızı, mavi (demir külçesi
  + 2 boya). Asfalta sağ tıklayınca seçili işareti, baktığın yöne doğru çizer.
  Aynı işarete tekrar tıklamak onu 90° döndürür (virajlar ve oklar için).
  **Havaya sağ tıklamak** (ya da eğilip herhangi bir yere) **işaret menüsünü**
  açar: her işaret, kutunun renginde boyanmış küçük bir asfalt karesi olarak
  görünür. Tıkla, ya da 1–9 tuşları / oklar + Enter ile seç; seçili olan
  kutunun renginde çerçevelenir, altında kalan boya yazar.
- İşaretler: kesik çizgi, düz çizgi, çift çizgi, kenar çizgisi, viraj, dur
  çizgisi, yaya geçidi, ileri ok, sola ve sağa dönüş okları, park (P),
  tarama. İlk seçenek **"Sil"**: işareti kazır ve boya harcamaz.
- Bir kutu 128 işaret çizer; döndürmek ve silmek boya harcamaz. Boyalı bir
  bloğa katman eklenince işaret yeni yüzeye çıkar.
- Asfalt üstünde lastik ve direksiyon kutusu %40 daha yavaş aşınır.

### Ayar dosyası

`config/car.json` ilk açılışta yazılır:

| Anahtar | Varsayılan | Ne yapar |
|---|---|---|
| `partWear` | 1.0 | Bütün parça aşınmasının çarpanı |
| `fuelUse` | 1.0 | Yakıt tüketimi çarpanı |
| `generatorOutput` | 1.0 | Jeneratör çıkışı çarpanı |
| `cableRate` | 250 | Kablonun tick başına taşıdığı E |

### Uyumluluk

- **Jade** kuruluysa araca bakınca yakıt/şarj, kaporta, en kötü parça ve
  kilometre görünür. Bağımlılık yalnızca derleme için (`compileOnly`);
  Jade yoksa eklenti hiç yüklenmez.
- Yakıtlar `c:` ortak etiketlerinde (`c:crude_oil`, `c:diesel`,
  `c:gasoline`, `c:fuels/…`): başka modların boruları tanır.
- EMI/REI: rafineri/arıtıcı tarif değil
  makine olduğu için şimdilik eklenmedi.

### Kablo ışığı ve lambalar

Kablo, tick başına 50 E'den fazla taşırken bakır çekirdeğinde ince bir
çizgi parlar, durunca söner (10 tick'te bir bakılır, komşular önbellekte).
Jeneratör, arıtıcı, pompa kulesi ve şarj direğinin lambaları çalışırken
karanlıkta da yanar.

## Derleme

ModDevGradle 2.0 ve NeoForge 21.1; derleme hedefi Java 21. Bu makinede:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-26.jdk/Contents/Home ./gradlew build
```

Dev istemcisini çalıştırmak:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-26.jdk/Contents/Home ./gradlew runClient
```

## Testler

İki tür test var:

- **Sunucu gametest'leri** — [`CarMachineTests`](src/gametest/java/me/trossty/car/test/CarMachineTests.java),
  39 gerçek iddia: pompa kulesi petrol çeker, rafineri 1:1 kırar ve katran
  bırakır, arıtıcı her birimi temizler, kablo enerji taşır ve parlar, akü
  yalnız önünden verir, şarj direği elektrikli aracı doldurur, OBD en kötüyü
  öne koyar, asfalt kar gibi katmanlanır, boya çizer/döndürür/siler, tablet
  eşleşir ve menzil dışında sinyali keser, ham yakıt hacimce karışır…
  Hepsi modun boş şablonu `car:empty` üzerinde koşuyor.

  ```bash
  ./gradlew runGameTestServer
  ```

- **Fotoğraf testi** (`CarPhotoTest`) gerçek istemciyi açar, düz bir yaratıcı
  dünyada sahneleri kurar, her birinin fotoğrafını
  `build/run/photo/screenshots/` altına çeker ve kapanır. NeoForge'da Fabric'in
  istemci test API'si olmadığından sahneleri `PhotoHarness` sürüyor. Daha önce
  bakılmış sahneler atlanır; hepsi ya da yalnız seçilenler:

  ```bash
  ./gradlew runPhotoClient -Pcar.photo.all=true
  ./gradlew runPhotoClient -Pcar.photo.scenes=pumpAndLift,blockGallery
  ```

## 3B eşyalar

Parçalar, bidon, teşhis panosu ve üç araç artık düz resim değil, küplerden
kurulmuş model: envanterde, elde, yerde ve çerçevede 3B görünüyorlar. 24 modelin
hepsini [`tools/GenItemModels.java`](tools/GenItemModels.java) yazıyor:

```
java tools/GenItemModels.java [onizleme.png]
```

- Bütün modeller tek dokuyu paylaşıyor: `item/materials.png`, her malzemeye
  16×16'lık bir kare. Her yüz, kendi boyu kadar texel alıyor; yani bir texel
  burada da bloğun on altıda biri.
- Silindirler (teker, disk, filtre) ortadan geçen ve her biri biraz daha
  döndürülmüş dilimlerden oluşuyor; 8 ya da 16 kenar.
- Araba eşyası tek model; rengi `car:color` bileşenine göre eşya tanımındaki
  `tints` veriyor. Renk başına ayrı model ve doku yok.
- Modeller kuzeye bakacak şekilde yazılıyor: ön yüz −z, envanterde sağda görünen
  yüz o.
- Elde tutuş vanilla blok açısında (`[75, 45, 0]`): araçlar elde tekerleri
  aşağıda, dik duruyor.

Kalan düz sprite'lar da 3B oldu ([`tools/gen_paint_tar.py`](tools/gen_paint_tar.py)):
dört yol boyası tel saplı sekizgen teneke kutu, katran parlak bir yığın, petrol
sızıntısı eşyası üstünde bir damla duran düzensiz bir birikinti; asfalt eşyası
ince bir dilim yerine yarım yükseklikte bir plaka gösteriyor. Sızıntının blok
modeli artık arası boş düzensiz bir birikinti (cutout katmanı), siyah kareler
değil.

## Dokular

Araba ve eşya dokuları kodla üretiliyor, böylece renk değiştirmek kolay:

- [`tools/GenCarTextures.java`](tools/GenCarTextures.java) — 256×256 varlık dokusu (oyuk kabin daha çok UV alanı istiyor)
- [`tools/GenJerryCan.java`](tools/GenJerryCan.java) — benzin bidonu eşya dokusu
- [`tools/GenWorkbenchTextures.java`](tools/GenWorkbenchTextures.java) — tezgâh blok dokuları
- [`tools/GenItemModels.java`](tools/GenItemModels.java) — bütün eşyaların 3B modelleri, ortak malzeme dokusu (`item/materials.png`) ve eşya tanımları; dosya adı verilirse envanter görünümünün önizlemesini de çizer
- [`tools/GenGaugeTextures.java`](tools/GenGaugeTextures.java) — gösterge kadranı yüzleri
- [`tools/GenPartTextures.java`](tools/GenPartTextures.java) — lift, rafineri, pompa, depo ve boru blok dokuları, boru, valf ve lift modelleri.
- [`tools/gen_paint_tar.py`](tools/gen_paint_tar.py) — boya kutuları, katran, petrol sızıntısı (blok ve eşya), asfalt eşyası
- [`tools/gen_pump_lift.py`](tools/gen_pump_lift.py) — yakıt pompası ve lift modelleri
- [`tools/gen_tank.py`](tools/gen_tank.py) — sıvı tankının cam ve kapak dokuları
- [`tools/GenPlantModels.java`](tools/GenPlantModels.java) — rafineri, jeneratörler, akü dolabı, şarj direği, pumpjack

Her dosyanın tek bir sahibi var: hangi üreticiyi çalıştırırsan çalıştır, başka bir üreticinin dosyasını değiştirmez.
- [`tools/GenWheelTextures.java`](tools/GenWheelTextures.java) — lastik tipine göre teker dokuları
- [`tools/pickup.bbmodel`](tools/pickup.bbmodel) — kamyonetin Blockbench projesi (Modded Entity, 256×256)
- [`tools/GenEvItems.java`](tools/GenEvItems.java) — elektrik motorunun 3B modeli
- [`tools/GenAsphalt.java`](tools/GenAsphalt.java) — katran, katmanlı asfalt, 12 yol işareti, 4 boya kutusu: dokular, modeller, 392 parçalı blockstate, tarifler, ganimet tablosu
- [`tools/GenTablet.java`](tools/GenTablet.java) — OBD tabletinin elde çizilen çerçeve, arka yüz ve dolgu dokuları ile eşya modelindeki ekran dokusu (modelin kendisi `tools/items/tools.js`'te)
- Blockbench kiti ([`tools/blocks/kit.js`](tools/blocks/kit.js)) ile çizilenler: [`tools/items/fine_parts.js`](tools/items/fine_parts.js) — devirdaim pompası, termostat, yakıt pompası, enjektörler, ateşleme bobinleri, fren kaliperi, rotil ve susturucu; [`tools/blocks/vehicle_crate.js`](tools/blocks/vehicle_crate.js) — araç kasası; [`tools/blocks/fuel_tank.js`](tools/blocks/fuel_tank.js) — sıvı tankı. Her betik Blockbench'te çalışır, modelleri yazar ve yanına bir `.bbmodel` bırakır.
- [`tools/LangCheck.java`](tools/LangCheck.java) — iki dil dosyasının anahtar kümesini, yer tutucu sayılarını ve koddaki `translatable` anahtarlarını karşılaştırır (`java tools/LangCheck.java`)

## MrCrayfish seti

MrCrayfish'in araç modundaki kasa, helikopter, tarım araçları ve küçük
araçlar. Minibüs bilerek eklenmedi.

### Araç Kasası

Araç İstasyonu'nda üretilen araç artık eşya olarak değil, içinde o araç
olan bir **Araç Kasası** olarak çıkar. Kasanın ipucu içindekini söyler
("Honda FourTrax Rancher (Kasada)"). Yere koyulan kasa ancak **İngiliz
anahtarıyla** açılır (atölyenin rot ayarı ve motor sehpası için kullandığı
aynı anahtar; dört demir külçeden, çalışma masasında yapılır).
Kasa gerçek bir ihracat sandığı gibi: üç kızaklı, forklift oyuklu bir palet;
çıtalarla çerçevelenmiş, çapraz destekli, köşeleri çivili çelik plakalı dört
yan; üstünde iki çıtalı tahta kapak. Açmak için:

- her sağ tık kapağı biraz daha kaldırır, dördüncüsünde kapak gider;
- yanlar bir buçuk saniyede yere yatar ve araç, kasanın yerinde durur;
- aracın çıkacağı yer doluysa kasa daha ilk tıkta "yer yok" der, açılmaz.

Kasayı kırınca içindeki araçla birlikte düşer, araç kaybolmaz.

### Helikopter

Robinson R44'ün kendi biçimi var: burundan tavana tek eğimde çıkan, ortadaki
dikmede sivri birleşen iki parça ön camlı balon kabin, kabinin arkasında
rotor direğinin yuvarlak kaportası ve oradan daralıp ince bir kuyruk
konisine inen gövde. Kuyruk rotoru solda. Kabin, oturan oyuncunun kafası
tavana değmeyecek kadar yüksek.

Kızakları üstünde durur, yerde gitmez. **Zıplama** kolektifi
çeker ve dikine kalkar; havada **ileri/geri** rotoru eğer, o yöne uçar,
**sağ/sol** kuyruk rotoruyla olduğu yerde döner. Bırakınca yavaşça alçalır,
altındaki neyse üstüne konacak kadar yumuşak. Hızlanınca burnu düşer,
dönüşe yatar; bunu her taraf aynı görsün diye sürat ve dönüşten hesaplar.

Motor havada durursa rotor alttan gelen rüzgârla döner: daha hızlı ama
kontrollü iner, düz yere otorotasyon sadece kızakları biraz büker. Çok sert
ya da hızlı inmek her araçtaki gibi kazadır.

### Tarım

Tarım aracının ne yaptığı tanımındaki `implement` alanından gelir
(`seeder`, `fertilizer`, `mower`). Araç her yeni bloğa geçtiğinde o bloğun
sırasında, aracın genişliği kadar blok boyunca iş yapar:

| Araç | Ne yapar | Şartı |
|---|---|---|
| Ekim makinesi | Yükündeki ilk uygun tohumu (buğday, havuç, patates, pancar, kabak/karpuz çekirdeği...) boş sürülmüş toprağa eker | Bir traktöre takılı olmalı |
| Gübre serpme makinesi | Yükündeki kemik tozunu büyüyen ekinlere serper; kendinden bir blok geniş serper | Bir traktöre takılı olmalı |
| Çim biçme traktörü | Ot, eğrelti, çiçek, kuru çalı ve iki bloklu bitkileri biçer, düşeni çektiği römorkun yüküne koyar (yer yoksa yere bırakır) | Motoru çalışıyor olmalı |

Ekim makinesi ve gübre serpme makinesi römork gibi takılır: traktörün
arkasına koy ve boş elle tıkla. Yüklerini doldurmak için eğilip tıkla.

### Gidonlu araçlar

ATV ve jet ski tanımlarında `handlebars: true` var. Bunlarda kendiliğinden
direksiyon çizilmez, sürücü motorsikletteki gibi elleri gidonda, dizleri
içeride oturur, gidon da ön tekerlerin ortası (jet skide kendi ortası)
etrafında döner.

### Oturanlar araca bakar

Bir araçta oturanın gövdesi artık hep aracın baktığı yöne bakar, kafası
iki yana 105°'ye kadar döner (vanilya teknesindeki gibi). Önceden gövde,
binerken nereye bakıyorsan orada kalıyordu; dar araçlarda (çim biçme
traktörü, go-kart) bacak kaportanın yanından dışarı çıkıyordu.

## Sırada ne var

Henüz yapılmadı, MrCrayfish'te olan katmanlar:

- **Daha fazla araç tipi** — yarış arabası (Mustang)
  eklendi; yeni bir araç artık `tools/GenVehicles.java`'da bir yöntem ve bir
  tarif dosyası demek
- **MrCrayfish'ten kalanlar** — hız rampaları (boost pad); minibüs bilerek
  yok
- **Ertelenenler** ([GELISTIRME.md](GELISTIRME.md)'de nedenleriyle):
  arıtıcının 2×2 yapılması, iki istemcili çok oyunculu test, EMI/REI tarif
  gösterimi, WTHIT ve Create uyumunun oyun içinde denenmesi
