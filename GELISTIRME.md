# Geliştirilebilecek noktalar

Yalnızca açık işler. Bitenler git geçmişinde ve [geliştirici notlarında](https://github.com/Trossty/overthought-vehicles/wiki/Developer-Notes).

Öncelik: **Y** yüksek · **O** orta · **D** düşük.

## Atölye ve teşhis

| Öncelik | Konu | Ayrıntı |
|---|---|---|
| Y | Motor tamirinin devamı | Yağlı kompresyon testi (segman mı supap mı), sıralı söküm ve tek kullanımlık conta, mm ölçümleri, tezgâhta işleme, alıştırma, yağ pompası ve servis geçmişi. |
| O | Canlı veri ve donmuş kare | OBD tabletinde canlı sensör değerleri ve arıza anının kaydı (freeze frame). |
| O | Elektrik: far ampulü, sigorta kutusu, kablo demeti | Bugün elektrikte yalnızca akü, alternatör ve marş var; farlar ve sinyaller hiç bozulmuyor. Yanan sigorta bir devreyi (far, sinyal, marş) susturur, OBD yalnızca devreyi söyler. Önden çarpma farı kırabilir. |
| D | Eksik parçalar | Rot başı ve manifold yok. Susturucu ve yakıt pompası aracın altında çizilmiyor, yalnızca nişan kutuları var. |

## Görsel

| Öncelik | Konu | Ayrıntı |
|---|---|---|
| O | Ölçek uyumsuzluğu | Pompa kulesi, hidrojenle arıtıcı ve jeneratörler 4×4 rafinerinin yanında küçük kalıyor. Arıtıcıyı 2×2 yapmak çok bloklu yapı demek: kayıt biçimi ve testler değişir. |
| D | Arıtıcının kablo plakası | Jeneratörlerde ve akü bankasında priz kutusu var; hidrojenle arıtıcıda plakanın boşlukta kalıp kalmadığına bakılmadı. |

## Uyumluluk

| Öncelik | Konu | Ayrıntı |
|---|---|---|
| O | Tarif gösterimi (EMI/JEI) | Rafineri, biyo-rafineri ve arıtıcı dönüşümleri hiçbir yerde listelenmiyor. Dönüşümler tarif değil makine işi olduğundan önce bir tarif türü gerekir. |
| D | Create | Benzin gerçek bir sıvı, Create boruları taşıyabilmeli, ama Create kurulu hiç denenmedi. Sonra: kinetik → elektrik dönüştürücü. |

Bilerek dışarıda bırakılanlar: dükkân, müşteri ve para sistemleri.
