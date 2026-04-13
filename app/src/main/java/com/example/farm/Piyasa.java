package com.example.farm;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class Piyasa extends AppCompatActivity {

    private TableLayout tabloEt, tabloSut;
    private Button btnYenile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.piyasa);

        tabloEt = findViewById(R.id.tabloEt);
        tabloSut = findViewById(R.id.tabloSut);
        btnYenile = findViewById(R.id.btnYenile);


        // Verileri Çek
        verileriGuncelle();
        btnYenile.setOnClickListener(v -> verileriGuncelle());
    }

    private void verileriGuncelle() {
        btnYenile.setText("Yükleniyor...");
        btnYenile.setEnabled(false);

        // Tabloları temizle (Başlıklar hariç)
        temizle(tabloEt);
        temizle(tabloSut);

        new Thread(() -> {
            try {
                Document doc = Jsoup.connect("http://www.ukon.org.tr/fiyatlar.aspx")
                        .userAgent("Mozilla/5.0").timeout(10000).get();
                Elements rows = doc.select("tr");

                for (Element row : rows) {
                    Elements cols = row.select("td");
                    if (cols.size() >= 2) {
                        String bolge = cols.get(0).text().trim();
                        // "Bölgesi" kelimesi geçenleri al
                        if (bolge.contains("Bölgesi") && bolge.length() < 40) {
                            String fiyat = "";
                            for (int i = 1; i < cols.size(); i++) {
                                String txt = cols.get(i).text().trim();
                                if (txt.matches(".*\\d+.*") && txt.length() < 10) {
                                    fiyat = txt; break;
                                }
                            }
                            if (!fiyat.isEmpty()) {
                                String finalFiyat = fiyat;
                                runOnUiThread(() -> satirEkle(tabloEt, bolge, finalFiyat + " ₺", Color.parseColor("#D32F2F")));
                            }
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        new Thread(() -> {
            try {
                Document doc = Jsoup.connect("https://ulusalsutkonseyi.org.tr/2025-yili-cig-inek-sutu-tavsiye-fiyatlari-4523/")
                        .userAgent("Mozilla/5.0").timeout(10000).get();

                // USK sitesindeki tabloları bul
                Elements rows = doc.select("tr");

                for (Element row : rows) {
                    Elements cols = row.select("td");
                    if (cols.size() >= 2) {
                        String donem = cols.get(0).text().trim();
                        String fiyat = cols.get(1).text().trim();

                        //
                        if (donem.contains("202") && fiyat.matches(".*\\d+.*")) {
                            runOnUiThread(() -> satirEkle(tabloSut, donem, fiyat + " ₺", Color.parseColor("#1976D2")));
                        }
                    }
                }

                runOnUiThread(() -> {
                    btnYenile.setText("🔄 Listeyi Yenile");
                    btnYenile.setEnabled(true);
                    Toast.makeText(Piyasa.this, "Veriler Güncellendi", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnYenile.setText("Hata!");
                    btnYenile.setEnabled(true);
                });
            }
        }).start();
    }

    private void satirEkle(TableLayout tablo, String solYazi, String sagYazi, int renk) {
        TableRow row = new TableRow(this);
        row.setPadding(50, 40, 50, 40);

        TextView tvSol = new TextView(this);
        tvSol.setText(solYazi);
        tvSol.setTextColor(Color.DKGRAY);
        tvSol.setTextSize(14);
        tvSol.setMaxWidth(450);

        TextView tvSag = new TextView(this);
        tvSag.setText(sagYazi);
        tvSag.setTextColor(renk);
        tvSag.setTextSize(15);
        tvSag.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSag.setGravity(Gravity.END);

        row.addView(tvSol);
        row.addView(tvSag);
        tablo.addView(row);

        // Ara Çizgi
        android.view.View cizgi = new android.view.View(this);
        cizgi.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 1));
        cizgi.setBackgroundColor(Color.parseColor("#EEEEEE"));
        tablo.addView(cizgi);
    }

    private void temizle(TableLayout t) {
        if (t.getChildCount() > 1) {
            t.removeViews(1, t.getChildCount() - 1);
        }
    }
}