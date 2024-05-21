# Akıllı Durak/Navigasyon ve Sesli Yönlendirme Sistemi

## Proje Açıklaması

Bu proje, görme engelli bireyler için bağımsız hareket etmelerini kolaylaştıran bir mobil uygulamadır. Uygulama, gerçek zamanlı haritalama, sesli komutlar ve otomatik navigasyon özelliklerini entegre ederek kullanıcı dostu ve erişilebilir bir deneyim sunar. Kullanıcılar, sesli komutlarla varış noktalarını belirleyebilir ve sesli yönlendirme ile hedeflerine ulaşabilirler.

## Özellikler

- **Gerçek Zamanlı Konum Takibi**: Kullanıcıların mevcut konumlarını gerçek zamanlı olarak takip eder.
- **Sesli Yönlendirme**: Sesli komutlarla belirli bir rotayı veya durağı yönlendirme.
- **Engel Tespiti ve Bilgilendirme**: Yolda karşılaşılan engeller hakkında kullanıcıyı bilgilendirir.
- **Rota Planlama**: Kullanıcıların belirli bir noktaya ulaşmaları için en uygun rotayı belirler.
- **Durak Bilgilendirme**: Toplu taşıma durakları hakkında bilgi sağlar.

## Kurulum

1. **Depoyu Kopyalama**:
    ```bash
    git clone https://github.com/kullaniciadi/proje-adi.git
    cd proje-adi
    ```

2. **Gerekli Bağımlılıkları Yükleme**:
    - Android Studio veya Xcode kullanarak projeyi açın.
    - Gerekli SDK ve bağımlılıkları indirin.

3. **API Anahtarlarının Ayarlanması**:
    - Google Maps ve Google Places API anahtarlarınızı `local.properties` dosyasına ekleyin:
    ```properties
    MAPS_API_KEY=your_google_maps_api_key
    PLACES_API_KEY=your_google_places_api_key
    ```

## Kullanım

1. **Uygulamayı Çalıştırma**:
    - Android Studio'da `Run` tuşuna basarak uygulamayı başlatın.
    - Cihazınızda veya emülatörde uygulamanın yüklenmesini bekleyin.

2. **Navigasyon Başlatma**:
    - Uygulama açıldığında, sesli komutlar ile varış noktasını belirleyin.
    - Rota oluşturulduktan sonra sesli yönlendirme ile hedefinize ulaşın.

3. **Sesli Komutlar**:
    - Ses açma tuşuna basarak sesli komut modunu başlatın.
    - Sesli komutlar ile varış noktasını belirtin.
    - Ses kısma tuşuna basarak navigasyonu durdurun.

## Uygulama İçerisinden Görseller

![1](https://github.com/AndacAkyuz/Akilli-Durak-Navigasyon-ve-Sesli-Yonlendirme-Sistemi/assets/91327557/6c9a947d-2e02-4a58-b2c0-460739d9f34b)
![2](https://github.com/AndacAkyuz/Akilli-Durak-Navigasyon-ve-Sesli-Yonlendirme-Sistemi/assets/91327557/a6f73b94-882e-4fca-84af-cb84e274b831)


## Katkıda Bulunma

1. Bu depoyu fork'layın (üst sağdaki Fork düğmesine tıklayın)
2. Kendi dalınızı oluşturun (`git checkout -b ozellik-adi`)
3. Değişikliklerinizi commit edin (`git commit -am 'Yeni özellik ekle'`)
4. Dalınıza push yapın (`git push origin ozellik-adi`)
5. Bir Pull Request oluşturun
