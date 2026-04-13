package com.example.farm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class HayvanListe extends AppCompatActivity {

    RecyclerView recycler;
    HayvanAdapter adapter;
    ArrayList<HayvanVeriTabanı> liste;
    String filtre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hayvanliste);

        filtre = getIntent().getStringExtra("gelenFiltre");
        ((TextView)findViewById(R.id.tvListeBaslik)).setText(filtre + " Listesi");

        recycler = findViewById(R.id.recyclerView);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        liste = new ArrayList<>();
        androidx.appcompat.widget.SearchView searchView = findViewById(R.id.searchViewHayvan);

        // Adaptöre, bir hayvana tıklanınca ne yapması gerektiğini burada söylüyorum.
        adapter = new HayvanAdapter(this, liste, hayvan -> {
            Intent intent = new Intent(HayvanListe.this, HayvanDetayi.class);
            // Sadece kimlik numarasını (küpe) gönderiyorum, detay sayfası gerisini kendi çekecek.
            intent.putExtra("kupe", hayvan.kulakKupeNo);
            startActivity(intent);
        });
        recycler.setAdapter(adapter);

        // Arama çubuğu dinleyicisi
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Kullanıcı her harfe bastığında listeyi anlık süzüyorum.
                filtrele(newText);
                return true;
            }
        });

        verileriCek();
    }

    private void verileriCek() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid).child("animals");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                liste.clear(); // Listeyi temizlemezsem her güncellemede üstüne ekler, şişer.
                for (DataSnapshot d : snapshot.getChildren()) {
                    HayvanVeriTabanı h = d.getValue(HayvanVeriTabanı.class);
                    if (h != null) {

                        if (filtre.equals("Tümü")) {
                            liste.add(h);
                        }

                        else if (filtre.equals("Hasta")) {
                            if (h.hastaMi == true) {
                                liste.add(h);
                            }

                        }
                        else if (filtre.equals("Aşısız")) {
                            if (!h.asiliMi) {
                                liste.add(h);
                            }
                        }
                        else if (h.durum.contains(filtre) || h.tur.contains(filtre)) {
                            liste.add(h);
                        }
                    }
                }
                adapter.notifyDataSetChanged(); // Listeyi güncelledim diye adaptöre haber veriyorum.
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filtrele(String text) {
        ArrayList<HayvanVeriTabanı> filtrelenmisListe = new ArrayList<>();

        if (liste != null) {
            for (HayvanVeriTabanı hayvan : liste) {
                // Hem küpe numarasında hem de isimde arama yapıyorum.
                boolean kupeVar = hayvan.kulakKupeNo != null &&
                        hayvan.kulakKupeNo.toLowerCase().contains(text.toLowerCase());

                boolean isimVar = hayvan.takmaAd != null &&
                        hayvan.takmaAd.toLowerCase().contains(text.toLowerCase());

                if (kupeVar || isimVar) {
                    filtrelenmisListe.add(hayvan);
                }
            }
        }
        if (adapter != null) {
            adapter.filterList(filtrelenmisListe); // Süzülen listeyi ekrana bas.
        }
    }
}