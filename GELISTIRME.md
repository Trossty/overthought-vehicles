# Geliştirilebilecek noktalar

Kod, README ve fotoğraf testleri taranarak hazırlandı (2026-11-12). Liste önceliğe göre sıralı: önce oyuncunun hemen fark edeceği eksikler, sonra oynanış derinliği, en son altyapı.

Öncelik: **Y** yüksek · **O** orta · **D** düşük. Emek: **S** küçük (bir oturum) · **M** orta · **L** büyük.

---

## 1. Görsel pürüzler

| Öncelik | Emek | Konu | Ayrıntı | Durum |
|---|---|---|---|---|
| Y | S | Kablo plakası havada duruyor | Hidrotreater ve jeneratörlerin gövdesi blok yüzüne değmiyor, bu yüzden plaka 1–2 piksel boşlukta kalıyor. Ya plakanın derinliği cihazın gerçek gövdesine göre ayarlanmalı, ya da cihazlara blok yüzüne kadar uzanan bir "priz kutusu" çıkıntısı eklenmeli. İkincisi daha temiz ve tek yerden çözülür. | ✅ Jeneratörler ve akü bankasında priz kutusu çıkıntısı var. Hidrojenle arıtıcıda ayrıca bakılmadı. |
| O | M | Ölçek uyumsuzluğu | 4×4 rafineri büyüdü, ama pompa kulesi, hidrotreater ve jeneratörler yanında küçük kalıyor. Hidrotreater 2×2'lik çok bloklu yapıya çıkabilir; pompa kulesi bir blok uzayabilir. | ⏸ Ertelendi: arıtıcıyı 2×2 yapmak çok bloklu yapı demek, kayıt biçimi ve testler değişir. Ayrı bir iş olarak ele alınmalı. |
| O | S | Kabloda akış görünmüyor | Kablo enerji taşırken de boşken de aynı görünüyor. `powered` gibi bir blok durumu eklenip doluyken bakır çekirdek hafifçe parlatılabilir; 1–2 tick gecikmeyle güncellenirse blok güncellemesi pahalı olmaz. | ✅ 50 E/t üstünde bakır çekirdek parlıyor, 10 tick'te bir bakılıyor. |
| D | S | Makinelerde gösterge yok | Rafineride ekran var, ama hidrotreater, pompa kulesi ve jeneratörlerin durumu ancak sağ tıklayınca okunuyor. Modele küçük bir gösterge (yeşil/kırmızı lamba) eklenebilir: `LIT` zaten var, yalnızca doku gerekiyor. | ✅ Lambalar çalışırken karanlıkta da yanıyor (`light_emission`). |

## 2. Arayüz ve geri bildirim

| Öncelik | Emek | Konu | Ayrıntı | Durum |
|---|---|---|---|---|
| Y | M | Jeneratör ekranı | Kodda `There is no screen` yazıyor. Yakıt yuvası, depo çubuğu, enerji çubuğu ve yanma süresi olan basit bir ekran, oyuncunun makineyi anlamasını çok kolaylaştırır. Rafineri ekranı (`FuelRefineryScreen`) şablon olarak kullanılabilir. | ✅ |
| Y | M | Hidrotreater ve pompa kulesi ekranı | Giriş/çıkış depoları, enerji, rezervdeki kalan petrol (pompa kulesi için "90 → 87" gibi). Şu an bu bilgiler yalnızca sohbet satırına geliyor. | ✅ Akü ve şarj direği ekranı da eklendi. |
| O | M | Jade/WTHIT desteği | Bakılan bloğun enerji, depo ve ilerleme bilgisini ekranın üstünde gösterir. İsteğe bağlı bağımlılık (`modCompileOnly`) olarak eklenir; mod yoksa hiçbir şey değişmez. | ✅ Jade eklentisi derleniyor (araç bilgisi), ama Jade kurulu çalıştırılmadı. ⏸ WTHIT eklenmedi. |
| O | M | EMI/REI tarif gösterimi | Rafineri, biyo-rafineri ve hidrotreater dönüşümleri şu an hiçbir yerde listelenmiyor. EMI eklentisi, oyuncunun "ham petrol neye dönüşür" sorusunu oyundan çıkmadan yanıtlar. | ⏸ EMI'nin 1.21.11 sürümü yok. REI var, ama dönüşümler tarif değil makine işi olduğundan önce tarif türü gerekir. |
| D | S | Rehber / ilk adımlar | İlk kez oynayan biri petrol sızıntısını, pompa kulesini ve rafineriyi nasıl bağlayacağını bilmiyor. Yapılabilecekler: ilk eşyayı alınca verilen bir rehber kitabı ya da bir başarım (advancement) zinciri. Başarımlar hiç yok. | ✅ Petrolden asfalta başarım zinciri. |

## 3. Oynanış derinliği

| Öncelik | Emek | Konu | Ayrıntı | Durum |
|---|---|---|---|---|
| Y | M | Akü / enerji deposu bloğu | Jeneratörler yalnızca kendi 20 000'lik tamponlarını tutuyor; kablo 1000. Gece ya da yakıt bitince hat duruyor. Kablo ağına bağlanan bir akü bloğu (ör. 200 000) hem eksik bir halkayı tamamlar hem de elektrikli araç şarjına zemin hazırlar. | ✅ 200 000 E, yalnızca önünden verir. |
| Y | M | Elektrikli araç / şarj istasyonu | Enerji hattı şu an yalnızca makineleri besliyor. Bir şarj istasyonu ve akülü bir araç (ya da mevcut araçlara elektrikli motor seçeneği) enerji sistemini araç tarafına bağlar. "Araba modu" kimliğiyle en uyumlu genişleme bu. | ✅ Elektrik motoru (motorun 9. türü) ve şarj direği. |
| O | M | Mekanik tarafı (CMS) | Car Mechanic Simulator hedefi açısından: parça aşınması zamanla artıyor mu, teşhis aracı (OBD okuyucu benzeri bir eşya) var mı? Hasar ve tamir sistemi duruyor; parçaların kilometreye bağlı yıpranması ve "hangi parça bozuk" sorusunun bir aletle bulunması döngüyü derinleştirir. | ✅ Kilometreye bağlı aşınma ve OBD okuyucu. |
| O | S | Hurda / terk edilmiş araç | Dünyada bozuk parçalarla doğan terk edilmiş araçlar (köy kenarında, yol başında) tamir döngüsüne doğal bir başlangıç verir. Para ya da müşteri olmadan, Minecraft keşif hissine uyar. | ✅ Yeni chunk'ların yaklaşık 1/350'sinde. |
| D | M | Rafineri yan ürünleri | Ağır fraksiyondan asfalt/yol bloğu üretilebilir. Mod zaten yol bloğu içeriyor; petrolden yola giden zincir hattı anlamlı kılar. | ✅ Katran → asfalt, şeritli asfalt, yarım asfalt. |
| D | S | Yakıt kalitesi | Araç kirli dizel ile çalışınca motor parçası daha hızlı yıpranabilir; bu da hidrotreater'ı zorunlu değil ama değerli kılar. | ✅ Ham bidon, kirli depo, P0172 kodu. |

## 4. Ses

| Öncelik | Emek | Konu | Ayrıntı | Durum |
|---|---|---|---|---|
| O | M | Araç başına motor sesi | Şu an tek bir sentetik `engine.ogg` var. Motosiklet, kamyonet ve tanker aynı sesi kullanıyor. En az üç farklı motor sesi (küçük, V8, dizel) ve devire göre perde değişimi araçları ayırt edilebilir kılar. | ✅ Dört silindir, altı silindir, V8, dizel ve elektrik. |
| O | S | Makine sesleri | Pompa kulesi, rafineri ve jeneratörler sessiz. Kısa döngülü bir uğultu/pıt-pıt sesi, çalışıp çalışmadığını bakmadan anlatır. | ✅ |
| D | S | Kablo/boru yerleştirme sesi | Kablo `WOOL` sesini kullanıyor; kabul edilebilir, ama boru için metal bir ses daha uygun olabilir. | ✅ Boruda bakır sesi var. Kablo yün sesinde kaldı. |

## 5. Altyapı ve teknik borç

| Öncelik | Emek | Konu | Ayrıntı | Durum |
|---|---|---|---|---|
| Y | M | Otomatik doğrulama testleri | Tek test dosyası (`CarPhotoTest`, ~1200 satır) ekran görüntüsü alıyor; sayısal kontroller günlüğe yazılıyor ama hiçbiri testi düşürmüyor. Fabric gametest ile "pompa kulesi 200 tick'te petrol üretir", "kablo enerjiyi 10 blok taşır", "rafineri 1:1 dönüştürür" gibi gerçek assertion'lar eklenmeli. Böylece fotoğraf testini yeniden çalıştırmadan gerilemeler yakalanır. | ✅ `CarMachineTests` içinde 26 sunucu gametest'i, hepsi geçiyor. |
| O | L | `CarEntity` çok büyük | 3109 satır. Sürüş fiziği, hasar, parçalar, ses ve ağ senkronu tek sınıfta. `VehiclePhysics`, `VehicleDamage`, `VehicleParts` gibi bileşenlere bölmek, yeni araç türü eklemeyi ve hata ayıklamayı kolaylaştırır. | ⏸ Ertelendi: büyük bir yeniden düzenleme. Davranış değişmeden yapılmalı ve ayrı bir iş olarak ele alınmalı. |
| O | M | Ayar dosyası yok | Jeneratör çıktısı, kablo kapasitesi, rezerv boyutu, yakıt tüketimi kodda sabit. Basit bir JSON ayar dosyası (ya da gamerule) sunucu sahiplerinin dengeyi değiştirmesine izin verir. | ✅ `config/car.json` |
| O | M | Kablo ağı performansı | Her kablo her tick komşularına bakıyor (`EnergyStorage.SIDED.find` her yön için). Uzun hatlarda ve çok oyunculu sunucularda maliyet artar. Seçenekler: komşu aramasını önbelleğe almak (blok güncellemesinde yenilemek) ya da kabloları her 2–4 tick'te bir çalıştırmak. Gerçek bir ağ (graph) yapısı en iyisi, ama emek L. | ✅ Komşular önbellekte tutuluyor ve blok güncellemesinde yenileniyor. |
| D | S | Çok oyunculu test | Testler tek oyunculu. İki istemcili bir senaryo (biri sürerken diğerinin izlemesi, tanker hortumu paylaşımı) senkron hatalarını ortaya çıkarabilir. | ✅ `runMpServer`, ardından `runMpAlice` ve `runMpBob`: gerçek bir sunucuda Alice sürer, Bob yolcu olur. Kaza ikisine de hasar veriyor, Bob arabayı aynı hızda görüyor, farlar gece ikisinde de yanıyor. Sunucu işini bitirince kapanıyor. Bu test sunucuda zıplayan hız ölçümünü ve duvarın arkasına sızan far ışığını yakaladı. |
| D | S | Yerelleştirme | Yalnızca `en_us` ve `tr_tr` var. Eksik anahtarları yakalayan bir betik (iki dosyanın anahtar kümesini karşılaştırır) yeni dil eklemeden önce faydalı. | ✅ `tools/LangCheck.java` |

## 6. Uyumluluk

| Öncelik | Emek | Konu | Ayrıntı | Durum |
|---|---|---|---|---|
| O | S | Etiketler | Yakıtlar için `c:` ortak etiketleri (`c:fuels/diesel`, `c:oil`) eklenirse diğer modların boruları ve tankları sıvıyı tanır. Enerji zaten Team Reborn Energy ile konuşuyor. | ✅ `c:` sıvı etiketleri |
| D | — | Create | Create'in Fabric 1.21.11 sürümü yok; çıktığında kinetik → elektrik dönüştürücü düşünülebilir. Şimdilik beklemede. | ⏸ Beklemede: Create'in Fabric 1.21.11 sürümü hâlâ yok. |

---

## Durum özeti (2026-11-12)

26 maddeden 21'i yapıldı ve testlerle korunuyor. Ertelenenler: ölçek uyumsuzluğu, `CarEntity` bölünmesi, çok oyunculu test, EMI/REI, WTHIT ve Create. Nedenleri tabloda.

## Önerilen sıra (ilk hâli)

1. **Kablo plakası boşluğu** (S): görünür ve hızlı.
2. **Otomatik gametest assertion'ları** (M): sonraki her değişikliği güvenli kılar.
3. **Jeneratör + hidrotreater ekranları** (M): oyuncunun en çok takılacağı yer.
4. **Akü bloğu** (M), ardından **elektrikli araç / şarj istasyonu** (M): enerji hattını araçlara bağlar.
5. **Araç başına motor sesi + makine sesleri** (M).
6. **CMS tarafı**: kilometreye bağlı yıpranma ve teşhis aleti (M).
7. Geri kalanlar ihtiyaç oldukça.

Bilerek dışarıda bırakılanlar: dükkân, müşteri ve para sistemleri; tekne ve uçaklar (istenene kadar).
