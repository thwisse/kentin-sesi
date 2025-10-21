package io.github.thwisse.kentinsesi.util

// Ağ veya veritabanı işlemlerinin durumunu sarmalamak için genel amaçlı sınıf.
// T: Başarı durumunda döndürülecek verinin tipi (Örn: List<Post>, AuthResult, Unit).
sealed class Resource<T>(
    val data: T? = null, // Başarı durumundaki veri (null olabilir)
    val message: String? = null // Hata durumundaki mesaj (null olabilir)
) {
    // Başarı durumu: Veri içerir.
    class Success<T>(data: T?) : Resource<T>(data)

    // Hata durumu: Hata mesajı ve isteğe bağlı veri içerir.
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    // Yükleniyor durumu: Veri henüz gelmedi.
    class Loading<T> : Resource<T>()
}