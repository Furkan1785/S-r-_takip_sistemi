package com.example.farm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class HayvanDetayi extends AppCompatActivity {

    private String kupeNo;
    private HayvanVeriTabanı mevcutHayvan;
    private ImageView imgResim;
    // --- GÜNCELLEME: 'Kilo' değişkeni eklendi ---
    private TextView Ad, Kupe, Irk, Cinsiyet, Dogum, Durum, Notlar, Aşı, Kilo;
    private CardView cardGebelik;
    private TextView GebelikBilgi, TahminiDogum;
    private ProgressBar progressBarGebelik;
    private Button btnSutEkle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hayvandetayi);

        imgResim = findViewById(R.id.imgDetayResim);
        Ad = findViewById(R.id.tvDetayAd);
        Kupe = findViewById(R.id.tvDetayKupe);
        Irk = findViewById(R.id.tvDetayIrk);
        Cinsiyet = findViewById(R.id.tvDetayCinsiyet);
        Dogum = findViewById(R.id.tvDetayDogum);
        Durum = findViewById(R.id.tvDetayDurum);

        // --- GÜNCELLEME: Kilo TextView bağlandı ---
        Kilo = findViewById(R.id.tvDetayKilo);

        Notlar = findViewById(R.id.tvDetayNotlar);
        cardGebelik = findViewById(R.id.cardGebelik);
        GebelikBilgi = findViewById(R.id.tvGebelikBilgi);
        TahminiDogum = findViewById(R.id.tvTahminiDogum);
        progressBarGebelik = findViewById(R.id.progressBarGebelik);
        Aşı=findViewById(R.id.tvDetayAsi);

        Button btnSil = findViewById(R.id.btnSil);
        Button btnDuzenle = findViewById(R.id.btnDuzenle);
        btnSutEkle = findViewById(R.id.btnSutEkle);

        kupeNo = getIntent().getStringExtra("kupe");

        verileriGetir();

        btnSil.setOnClickListener(v -> silmeOnayiGoster());
        btnSutEkle.setOnClickListener(v -> sutPenceresiniAc());

        btnDuzenle.setOnClickListener(v -> {
            if (mevcutHayvan != null) {
                // Düzenleme sayfasına giderken elimdeki tüm verileri gönderiyorum ki kullanıcı tekrar girmek zorunda kalmasın.
                Intent intent = new Intent(HayvanDetayi.this, HayvanEkle.class);
                intent.putExtra("duzenleModu", true);
                intent.putExtra("kupe", mevcutHayvan.kulakKupeNo);
                intent.putExtra("ad", mevcutHayvan.takmaAd);
                intent.putExtra("dogum", mevcutHayvan.dogumTarihi);
                intent.putExtra("cinsiyet", mevcutHayvan.cinsiyet);
                intent.putExtra("irk", mevcutHayvan.irk);
                intent.putExtra("tur", mevcutHayvan.tur);
                intent.putExtra("agirlik", String.valueOf(mevcutHayvan.agirlik));
                intent.putExtra("durum", mevcutHayvan.durum);
                intent.putExtra("not", mevcutHayvan.notlar);
                intent.putExtra("url", mevcutHayvan.resimUrl);
                intent.putExtra("hastaMi", mevcutHayvan.hastaMi);
                intent.putExtra("asiliMi", mevcutHayvan.asiliMi);
                intent.putExtra("tohumlama", mevcutHayvan.tohumlamaTarihi);
                startActivity(intent);
                finish();
            }
        });
    }

    private void verileriGetir() {
        if (kupeNo == null) return;
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                .child(uid).child("animals").child(kupeNo);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    mevcutHayvan = snapshot.getValue(HayvanVeriTabanı.class);
                    if (mevcutHayvan != null) {
                        bilgileriEkranaYaz(mevcutHayvan);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HayvanDetayi.this, "Veri alınamadı!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bilgileriEkranaYaz(HayvanVeriTabanı h) {
        Ad.setText(h.takmaAd != null && !h.takmaAd.isEmpty() ? h.takmaAd : "İsimsiz");
        Kupe.setText(h.kulakKupeNo);
        Irk.setText(h.irk);
        Cinsiyet.setText(h.cinsiyet);
        Dogum.setText(h.dogumTarihi);
        Durum.setText(h.durum);

        // --- GÜNCELLEME: Kilo verisi yazdırılıyor ---
        if (h.agirlik > 0) {
            Kilo.setText(h.agirlik + " kg");
        } else {
            Kilo.setText("-");
        }

        Notlar.setText(h.notlar != null && !h.notlar.isEmpty() ? h.notlar : "Eklenmiş not yok.");


        if (h.durum != null && ( h.durum.equals("Sağımda"))) {
            btnSutEkle.setVisibility(View.VISIBLE);
        } else {
            btnSutEkle.setVisibility(View.GONE);
        }

        // Resmi Base64 formatından Bitmap'e çevirip gösteriyorum.
        if (h.resimUrl != null && h.resimUrl.length() > 20) {
            try {
                byte[] decodedString = Base64.decode(h.resimUrl, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imgResim.setImageBitmap(decodedByte);
            } catch (Exception e) {
                imgResim.setImageResource(R.drawable.inekresmi);
            }
        } else {
            imgResim.setImageResource(R.drawable.inekresmi);
        }


        if (h.asiliMi) {
            Aşı.setText("Aşı Durumu: AŞILI ✅");
            Aşı.setTextColor(Color.parseColor("#2E7D32")); // Yeşil Renk
        } else {
            Aşı.setText("Aşı Durumu: AŞISIZ ❌");
            Aşı.setTextColor(Color.RED); // Kırmızı Renk
        }

        // GEBELİK HESAPLAMA MANTIĞI:
        if (h.durum != null && h.durum.equals("Gebe") && h.tohumlamaTarihi != null && !h.tohumlamaTarihi.isEmpty()) {
            cardGebelik.setVisibility(View.VISIBLE);
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date tohumlama = sdf.parse(h.tohumlamaTarihi);
                Date bugun = new Date();

                long fark = bugun.getTime() - tohumlama.getTime();
                long gecenGun = TimeUnit.DAYS.convert(fark, TimeUnit.MILLISECONDS);
                long kalanGun = 280 - gecenGun;

                Calendar cal = Calendar.getInstance();
                cal.setTime(tohumlama);
                cal.add(Calendar.DAY_OF_YEAR, 280);
                String dogumTarihi = sdf.format(cal.getTime());

                // İlerlemeyi Progress Bar ile gösteriyorum.
                if (kalanGun > 0) {
                    GebelikBilgi.setText("Gebeliğin " + gecenGun + ". günü (" + kalanGun + " gün kaldı)");
                    progressBarGebelik.setProgress((int) gecenGun);
                } else {
                    GebelikBilgi.setText("Doğum vakti gelmiş! (" + Math.abs(kalanGun) + " gün geçti)");
                    progressBarGebelik.setProgress(280);
                }
                TahminiDogum.setText("Tahmini Doğum: " + dogumTarihi);

            } catch (Exception e) { e.printStackTrace(); }
        } else {
            cardGebelik.setVisibility(View.GONE);
        }
    }

    // Süt verisi eklemek için küçük bir pencere (Dialog) açıyorum.
    private void sutPenceresiniAc() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.sutekle, null);
        builder.setView(view);

        EditText etTarih = view.findViewById(R.id.etSutTarih);
        EditText etMiktar = view.findViewById(R.id.etSutMiktar);

        // Tarih seçici (DatePicker) açan kod
        etTarih.setOnClickListener(v -> {
            Calendar takvim = Calendar.getInstance();
            new android.app.DatePickerDialog(this, (dp, y, m, d) -> {
                takvim.set(y, m, d);
                etTarih.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(takvim.getTime()));
            }, takvim.get(Calendar.YEAR), takvim.get(Calendar.MONTH), takvim.get(Calendar.DAY_OF_MONTH)).show();
        });

        etTarih.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date()));

        builder.setTitle("Günlük Süt")
                .setPositiveButton("Kaydet", (dialog, which) -> {
                    String tarih = etTarih.getText().toString();
                    String miktarStr = etMiktar.getText().toString();
                    if (!miktarStr.isEmpty()) {
                        firebaseSutKaydet(tarih, Double.parseDouble(miktarStr));
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void firebaseSutKaydet(String tarih, double miktar) {
        if (kupeNo == null) return;
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                .child(uid).child("animals").child(kupeNo).child("sutKayitlari");

        String yeniId = ref.push().getKey();
        Map<String, Object> veri = new HashMap<>();
        veri.put("tarih", tarih);
        veri.put("miktar", miktar);

        ref.child(yeniId).setValue(veri).addOnSuccessListener(aVoid ->
                Toast.makeText(this, "Süt kaydedildi!", Toast.LENGTH_SHORT).show()
        );
    }

    private void silmeOnayiGoster() {
        new AlertDialog.Builder(this)
                .setTitle("Hayvanı Sil")
                .setMessage("Bu kaydı silmek istediğinize emin misiniz?")
                .setPositiveButton("Evet, Sil", (dialog, which) -> hayvanSil())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void hayvanSil() {
        if (kupeNo == null) return;
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        // Hayvanın bulunduğu düğümü tamamen siler.
        FirebaseDatabase.getInstance().getReference("users")
                .child(uid).child("animals").child(kupeNo)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Silindi.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}