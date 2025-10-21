package io.github.thwisse.kentinsesi.data.model

// Firestore'dan veri okurken/yazarken alan adlarının eşleşmesi önemlidir.
// Boş constructor, Firestore'un veriyi otomatik olarak bu sınıfa dönüştürebilmesi için gereklidir.
data class User(
    val uid: String = "", // Firebase Auth ID'si (Boş bırakılamaz)
    val fullName: String = "", // Ad Soyad
    val email: String = "", // E-posta
    val role: String = "citizen", // Varsayılan rol: vatandaş
    val points: Long = 0, // Katkı puanı (Firestore sayıları Long olarak tutar)
    val title: String = "Yeni Kullanıcı" // Puana göre unvan
) {
    // Firestore'un data class'a çevrim yapabilmesi için boş constructor GEREKLİDİR.
    // Eğer tüm alanların varsayılan değeri varsa (yukarıdaki gibi), buna gerek kalmaz.
    // constructor() : this("", "", "", "citizen", 0, "Yeni Kullanıcı")
}