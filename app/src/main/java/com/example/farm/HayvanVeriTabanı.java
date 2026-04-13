package com.example.farm;


import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class HayvanVeriTabanı {

    public String kulakKupeNo;
    public String userId; // Kaydeden kişinin ID'si
    public String tarih;  // Ekleme tarihi

    // Hayvanın Özellikleri
    public String takmaAd;
    public String dogumTarihi;
    public String cinsiyet; // Erkek / Dişi
    public String irk;      // Holstein, Simental vb.
    public String tur;      // İnek, Düve, Dana vb.
    public double agirlik;

    // Sağlık ve Durum Bilgileri
    public String durum;    // Sağımda, Kuru, Gebe vb.
    public boolean hastaMi;
    public boolean asiliMi;
    public String notlar;
    public String tohumlamaTarihi;
    public String resimUrl;
    // Boş kurucu metot (Firebase için şart!)
    public HayvanVeriTabanı() {
    }

    // Veri eklerken kullanacağımız dolu kurucu metot
    public HayvanVeriTabanı(String kulakKupeNo, String userId, String tarih, String takmaAd, String dogumTarihi,
                            String cinsiyet, String irk, String tur, double agirlik, String durum,
                            boolean hastaMi, boolean asiliMi, String notlar, String resimUrl, String tohumlamaTarihi) {
        this.kulakKupeNo = kulakKupeNo;
        this.userId = userId;
        this.tarih = tarih;
        this.takmaAd = takmaAd;
        this.dogumTarihi = dogumTarihi;
        this.cinsiyet = cinsiyet;
        this.irk = irk;
        this.tur = tur;
        this.agirlik = agirlik;
        this.durum = durum;
        this.hastaMi = hastaMi;
        this.asiliMi = asiliMi;
        this.notlar = notlar;
        this.resimUrl = resimUrl;
        this.tohumlamaTarihi = tohumlamaTarihi;
    }
}