package com.example.farm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.provider.Settings;
import android.net.wifi.WifiManager;
import android.location.LocationManager;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Locale;

public class AnaSayfa extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference myRef;
    private BottomNavigationView altMenu;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView ToplamSayi, Hosgeldin;
    private CardView cardHasta, cardGebe, cardSagimda, cardKuru, cardInek, cardDuve, cardDana, cardBuzagi,cardAsisiz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.anasayfa);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            myRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("animals");

            Hosgeldin = findViewById(R.id.textViewWelcomeHome);
            String isim = user.getEmail().split("@")[0];
            Hosgeldin.setText("Hoş Geldin, " + isim.toUpperCase(Locale.getDefault()));
        }

        tanimla();
        tiklamalar();
        verileriGetir();

        ImageButton btnCikis = findViewById(R.id.btnCikisYap);
        btnCikis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(AnaSayfa.this)
                        .setTitle("Çıkış Yap")
                        .setMessage("Oturumu kapatmak istediğinize emin misiniz?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Evet, Çık", (dialog, which) -> {
                            FirebaseAuth.getInstance().signOut();
                            startActivity(new Intent(AnaSayfa.this, GirişEkrani.class));
                            finish();
                        })
                        .setNegativeButton("İptal", null)
                        .show();
            }
        });

        // ALT MENÜ YÖNLENDİRMELERİ
        altMenu.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_add_animal) {
                startActivity(new Intent(this, HayvanEkle.class));
                return true;
            } else if (id == R.id.nav_sut_genel) {
                startActivity(new Intent(AnaSayfa.this, Sutveri.class));
                return true;
            } else if (id == R.id.nav_piyasa) {
                startActivity(new Intent(AnaSayfa.this, Piyasa.class));
                return true;
            } else if (id == R.id.nav_location) {
                kontrolVeHaritaAc();
                return true;
            }
            return false;
        });
    }

    private void tanimla() {
        ToplamSayi = findViewById(R.id.textToplamHayvanSayisi);
        altMenu = findViewById(R.id.bottomNavigationView);
        cardHasta = findViewById(R.id.card_hasta);
        cardGebe = findViewById(R.id.card_gebe);
        cardSagimda = findViewById(R.id.card_sagimda);
        cardKuru = findViewById(R.id.card_kuruda);
        cardInek = findViewById(R.id.card_inek);
        cardDuve = findViewById(R.id.card_duve);
        cardDana = findViewById(R.id.card_dana);
        cardBuzagi = findViewById(R.id.card_buzagi);
        cardAsisiz = findViewById(R.id.card_asisiz);
    }

    private void tiklamalar() {
        View.OnClickListener listener = v -> {
            String filtre = "";
            int id = v.getId();

            // Hangi karta basıldıysa filtre kelimesini ona göre belirliyorum.
            if (id == R.id.card_hasta) filtre = "Hasta";
            else if (id == R.id.card_asisiz) filtre = "Aşısız";
            else if (id == R.id.card_gebe) filtre = "Gebe";
            else if (id == R.id.card_sagimda) filtre = "Sağımda";
            else if (id == R.id.card_kuruda) filtre = "Kuruda";
            else if (id == R.id.card_inek) filtre = "İnek";
            else if (id == R.id.card_duve) filtre = "Düve";
            else if (id == R.id.card_dana) filtre = "Dana";
            else if (id == R.id.card_buzagi) filtre = "Buzağı";
            else if (id == R.id.cardToplamHayvan) filtre = "Tümü";

            if (!filtre.isEmpty()) {
                Intent i = new Intent(AnaSayfa.this, HayvanListe.class);

                i.putExtra("gelenFiltre", filtre);
                startActivity(i);
            }
        };


        if(cardHasta != null) cardHasta.setOnClickListener(listener);
        if(cardGebe != null) cardGebe.setOnClickListener(listener);
        if(cardSagimda != null) cardSagimda.setOnClickListener(listener);
        if(cardKuru != null) cardKuru.setOnClickListener(listener);
        if(cardInek != null) cardInek.setOnClickListener(listener);
        if(cardDuve != null) cardDuve.setOnClickListener(listener);
        if(cardDana != null) cardDana.setOnClickListener(listener);
        if(cardBuzagi != null) cardBuzagi.setOnClickListener(listener);
        if(cardAsisiz != null) cardAsisiz.setOnClickListener(listener);
        findViewById(R.id.cardToplamHayvan).setOnClickListener(listener);
    }

    private void verileriGetir() {
        if (myRef == null) return;

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ToplamSayi.setText(String.valueOf(snapshot.getChildrenCount()));
                int hasta=0, gebe=0, sağmal=0, kuru=0, inek=0, duve=0, dana=0, buzaği=0,asisiz=0;

                for (DataSnapshot d : snapshot.getChildren()) {
                    String durum = d.child("durum").getValue(String.class);
                    String tur = d.child("tur").getValue(String.class);
                    Boolean isHasta = d.child("hastaMi").getValue(Boolean.class);
                    Boolean asiliMi = d.child("asiliMi").getValue(Boolean.class);

                    if (durum != null) {
                        if (isHasta != null && isHasta == true) hasta++;
                        if (asiliMi != null && !asiliMi) asisiz++;
                        if (durum.contains("Gebe")) gebe++;
                        if (durum.contains("Sağımda")) sağmal++;
                        if (durum.contains("Kuruda")) kuru++;
                    }
                    if (tur != null) {
                        if (tur.contains("İnek")) inek++;
                        if (tur.contains("Düve")) duve++;
                        if (tur.contains("Dana")) dana++;
                        if (tur.contains("Buzağı")) buzaği++;
                    }
                }
                kartGuncelle(cardHasta, hasta, " \uD83D\uDC2E\uD83E\uDD12 HASTA");
                kartGuncelle(cardGebe, gebe, "\uD83D\uDC2E\uD83E\uDD30  GEBE");
                kartGuncelle(cardSagimda, sağmal, "\uD83D\uDC2E\uD83E\uDD5B SAĞIMDA");
                kartGuncelle(cardKuru, kuru, "\uD83D\uDC2E\uD83D\uDCA4 KURUDA");
                kartGuncelle(cardInek, inek, "\uD83D\uDC2E İNEK");
                kartGuncelle(cardDuve, duve, "\uD83D\uDC04 DÜVE");
                kartGuncelle(cardDana, dana, "\uD83D\uDC02 DANA");
                kartGuncelle(cardBuzagi, buzaği, "\uD83D\uDC2E\uD83C\uDF7C BUZAĞI");
                if (cardAsisiz != null) {
                    TextView txtCount = cardAsisiz.findViewById(R.id.textCount);
                    if (txtCount != null) {
                        txtCount.setText(String.valueOf(asisiz));
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void kartGuncelle(CardView card, int sayi, String baslik) {
        if (card != null) {
            ((TextView) card.findViewById(R.id.textCount)).setText(String.valueOf(sayi));
            ((TextView) card.findViewById(R.id.textTitle)).setText(baslik);
        }
    }

    private void kontrolVeHaritaAc() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean wifiAcik = wifiManager != null && wifiManager.isWifiEnabled();

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsAcik = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!wifiAcik) {
            uyariGoster("WiFi Kapalı", "Konum doğruluğu için WiFi açık olmalıdır. Açmak ister misiniz?", Settings.ACTION_WIFI_SETTINGS);
        }
        else if (!gpsAcik) {
            uyariGoster("GPS Kapalı", "Konum servisleri kapalı. Ayarlardan açmak ister misiniz?", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        }
        else {
            haritaAc();
        }
    }

    private void uyariGoster(String baslik, String mesaj, String ayarEkrani) {
        new AlertDialog.Builder(this)
                .setTitle(baslik)
                .setMessage(mesaj)
                .setPositiveButton("Ayarlara Git", (dialog, which) -> {
                    startActivity(new Intent(ayarEkrani));
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void haritaAc() {
        Toast.makeText(this, "✅ WiFi ve GPS Aktif. Veteriner aranıyor...", Toast.LENGTH_SHORT).show();
        try {
            // Son konumu bulup etraftaki veterinerleri Google Maps'te aratıyorum.
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                Uri uri;
                if (location != null) {
                    uri = Uri.parse("geo:" + location.getLatitude() + "," + location.getLongitude() + "?q=veteriner");
                } else {
                    uri = Uri.parse("geo:0,0?q=veteriner");
                }
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
            });
        } catch (SecurityException e) { e.printStackTrace(); }
    }
}