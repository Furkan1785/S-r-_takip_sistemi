package com.example.farm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Sutveri extends AppCompatActivity {

    private LineChart grafik;
    private TextView tvSonuc;
    private Spinner spinner;

    private ArrayList<String> hayvanAdlari = new ArrayList<>();
    private ArrayList<String> hayvanIdleri = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    private DatabaseReference veritabaniRef;
    private String kullaniciId;

    private RecyclerView recyclerAylik;
    private AylikAdapter listeAdapter;
    private ArrayList<AylikVeri> aylikVeriListesi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sutveri);

        grafik = findViewById(R.id.lineChartGenel);
        tvSonuc = findViewById(R.id.tvToplamOzet);
        spinner = findViewById(R.id.spinnerHayvanSec);

        recyclerAylik = findViewById(R.id.recyclerAylikSut);
        recyclerAylik.setLayoutManager(new LinearLayoutManager(this));
        aylikVeriListesi = new ArrayList<>();

        // Başlangıçta boş adaptör atayalım ki hata vermesin
        listeAdapter = new AylikAdapter(aylikVeriListesi);
        recyclerAylik.setAdapter(listeAdapter);

        // Kullanıcı kontrolü (Crash önlemek için)
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            kullaniciId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            veritabaniRef = FirebaseDatabase.getInstance().getReference("users").child(kullaniciId).child("animals");

            grafikAyarlari();
            hayvanlariListele();
        }
    }

    private void hayvanlariListele() {
        hayvanAdlari.clear();
        hayvanIdleri.clear();
        hayvanAdlari.add("TÜM ÇİFTLİK (GENEL)");
        hayvanIdleri.add("GENEL");

        veritabaniRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hayvanAdlari.clear();
                hayvanIdleri.clear();
                hayvanAdlari.add("TÜM ÇİFTLİK (GENEL)");
                hayvanIdleri.add("GENEL");

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String durum = ds.child("durum").getValue(String.class);
                    // Null kontrolü ve string güvenliği
                    if (durum != null && (durum.contains("Sağımda") || durum.contains("Sağmal"))) {
                        String kupe = ds.child("kulakKupeNo").getValue(String.class);
                        String ad = ds.child("takmaAd").getValue(String.class);
                        String gorunenIsim = kupe;
                        if (ad != null && !ad.isEmpty()) gorunenIsim += " (" + ad + ")";

                        hayvanAdlari.add(gorunenIsim);
                        hayvanIdleri.add(ds.getKey());
                    }
                }
                spinnerAdapter = new ArrayAdapter<>(Sutveri.this, android.R.layout.simple_spinner_dropdown_item, hayvanAdlari);
                spinner.setAdapter(spinnerAdapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String secilenId = hayvanIdleri.get(position);
                recyclerAylik.setVisibility(View.VISIBLE); // Listeyi her zaman görünür yap

                if (secilenId.equals("GENEL")) {
                    hesaplaGenel();
                } else {
                    hesaplaTekil(secilenId);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void hesaplaTekil(String kupeNo) {
        veritabaniRef.child(kupeNo).child("sutKayitlari").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                TreeMap<String, Double> gunlukVeriler = new TreeMap<>();
                HashMap<String, Double> aylikGruplama = new HashMap<>(); // Aylık liste için
                double toplamLitre = 0;

                for (DataSnapshot d : snapshot.getChildren()) {
                    String tarih = d.child("tarih").getValue(String.class);
                    // Sayısal değeri güvenli çekme
                    Double miktar = 0.0;
                    Object miktarObj = d.child("miktar").getValue();
                    if (miktarObj instanceof Double) miktar = (Double) miktarObj;
                    else if (miktarObj instanceof String) miktar = Double.parseDouble((String) miktarObj);
                    else if (miktarObj instanceof Long) miktar = ((Long) miktarObj).doubleValue();

                    if (tarih != null && miktar > 0) {
                        gunlukVeriler.put(tarih, miktar);
                        toplamLitre += miktar;

                        // --- AYLIK HESAPLAMA (TEKİL İÇİN) ---
                        parseVeEkle(tarih, miktar, aylikGruplama);
                    }
                }
                cizGrafigi(gunlukVeriler, "Seçilen Hayvan", Color.BLUE);
                tvSonuc.setText("Bu Hayvanın Toplamı: " + String.format("%.1f", toplamLitre) + " Litre");

                // Listeyi Güncelle
                listeyiGuncelle(aylikGruplama);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- TÜM ÇİFTLİK İÇİN ---
    private void hesaplaGenel() {
        veritabaniRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                TreeMap<String, Double> gunlukVeriler = new TreeMap<>();
                HashMap<String, Double> aylikGruplama = new HashMap<>();
                double genelToplam = 0;

                for (DataSnapshot hayvan : snapshot.getChildren()) {
                    for (DataSnapshot sut : hayvan.child("sutKayitlari").getChildren()) {
                        String tarih = sut.child("tarih").getValue(String.class);

                        Double miktar = 0.0;
                        Object miktarObj = sut.child("miktar").getValue();
                        if (miktarObj instanceof Double) miktar = (Double) miktarObj;
                        else if (miktarObj instanceof String) miktar = Double.parseDouble((String) miktarObj);
                        else if (miktarObj instanceof Long) miktar = ((Long) miktarObj).doubleValue();

                        if (tarih != null && miktar > 0) {
                            // Günlük (Grafik)
                            if (gunlukVeriler.containsKey(tarih)) {
                                gunlukVeriler.put(tarih, gunlukVeriler.get(tarih) + miktar);
                            } else {
                                gunlukVeriler.put(tarih, miktar);
                            }
                            genelToplam += miktar;

                            // Aylık (Liste)
                            parseVeEkle(tarih, miktar, aylikGruplama);
                        }
                    }
                }
                cizGrafigi(gunlukVeriler, "Çiftlik Ortalaması", Color.GREEN);
                tvSonuc.setText("Çiftlik Toplam Üretim: " + String.format("%.1f", genelToplam) + " Litre");

                // Listeyi Güncelle
                listeyiGuncelle(aylikGruplama);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void parseVeEkle(String tarih, Double miktar, HashMap<String, Double> map) {
        try {
            // 🔥 DÜZELTME: Hem / hem . hem - işaretlerini kabul et
            String[] parcalar = tarih.split("[/.-]");

            if (parcalar.length >= 3) {
                String ay = parcalar[1];
                String yil = parcalar[2];
                String ayKey = ayIsmiGetir(ay) + " " + yil;

                if (map.containsKey(ayKey)) {
                    map.put(ayKey, map.get(ayKey) + miktar);
                } else {
                    map.put(ayKey, miktar);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void listeyiGuncelle(HashMap<String, Double> mapData) {
        aylikVeriListesi.clear();
        for (String key : mapData.keySet()) {
            aylikVeriListesi.add(new AylikVeri(key, mapData.get(key)));
        }
        listeAdapter.notifyDataSetChanged();
    }

    private void cizGrafigi(TreeMap<String, Double> veriler, String baslik, int renk) {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> tarihler = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Double> satir : veriler.entrySet()) {
            entries.add(new Entry(i, satir.getValue().floatValue()));
            tarihler.add(satir.getKey());
            i++;
        }

        if (entries.isEmpty()) {
            grafik.clear();
            grafik.setNoDataText("Henüz süt kaydı girilmemiş.");
            return;
        }

        LineDataSet set = new LineDataSet(entries, baslik);
        set.setColor(renk);
        set.setLineWidth(3f);
        set.setCircleColor(renk);
        set.setCircleRadius(5f);
        set.setDrawCircleHole(true);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillColor(renk);
        set.setFillAlpha(40);
        set.setValueTextSize(12f);
        set.setDrawValues(true);

        LineData data = new LineData(set);
        grafik.setData(data);
        grafik.getXAxis().setValueFormatter(new IndexAxisValueFormatter(tarihler));

        if (entries.size() > 7) {
            grafik.setVisibleXRangeMaximum(7);
            grafik.moveViewToX(entries.size() - 1);
        } else {
            grafik.fitScreen();
        }
        grafik.invalidate();
    }

    private void grafikAyarlari() {
        grafik.getDescription().setEnabled(false);
        grafik.getAxisRight().setEnabled(false);
        grafik.setNoDataText("Veri bekleniyor...");
        grafik.setTouchEnabled(true);
        grafik.setDragEnabled(true);
        grafik.setScaleEnabled(true);
        grafik.setPinchZoom(true);
        XAxis xAxis = grafik.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setDrawGridLines(false);
        grafik.animateX(1500);
    }

    private String ayIsmiGetir(String ayNo) {
        switch (ayNo) {
            case "01": case "1": return "Ocak";
            case "02": case "2": return "Şubat";
            case "03": case "3": return "Mart";
            case "04": case "4": return "Nisan";
            case "05": case "5": return "Mayıs";
            case "06": case "6": return "Haziran";
            case "07": case "7": return "Temmuz";
            case "08": case "8": return "Ağustos";
            case "09": case "9": return "Eylül";
            case "10": return "Ekim";
            case "11": return "Kasım";
            case "12": return "Aralık";
            default: return "Ay";
        }
    }

    class AylikAdapter extends RecyclerView.Adapter<AylikAdapter.ViewHolder> {
        private ArrayList<AylikVeri> liste;
        public AylikAdapter(ArrayList<AylikVeri> liste) { this.liste = liste; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // DİKKAT: Buradaki layout isminin 'sutaylik.xml' olduğundan emin ol!
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.sutaylik, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AylikVeri veri = liste.get(position);
            holder.tvAy.setText(veri.ayAdi);
            holder.tvMiktar.setText(String.format("%.1f Lt", veri.toplamMiktar));
        }

        @Override
        public int getItemCount() { return liste.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvAy, tvMiktar;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvAy = itemView.findViewById(R.id.tvAyAdi);
                tvMiktar = itemView.findViewById(R.id.tvAyMiktar);
            }
        }
    }

    class AylikVeri {
        String ayAdi;
        Double toplamMiktar;
        public AylikVeri(String ayAdi, Double toplamMiktar) {
            this.ayAdi = ayAdi;
            this.toplamMiktar = toplamMiktar;
        }
    }
}