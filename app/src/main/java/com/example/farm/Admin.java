package com.example.farm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Admin extends AppCompatActivity {

    private ListView listView;
    private DatabaseReference usersRef;

    // Ekranda E-postaları göstereceğiz
    private ArrayList<String> gosterilenListe = new ArrayList<>();
    // Arka planda silmek için ID'leri tutacağız
    private ArrayList<String> gizliIdListesi = new ArrayList<>();

    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin);

        listView = findViewById(R.id.listViewAdmin);

        // Kullanıcıların olduğu tablo
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Listeyi bağla
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, gosterilenListe);
        listView.setAdapter(adapter);

        // Verileri Getir ve Listele
        kullanicilariListele();

        // Listeye Uzun Basınca Silme İşlemi
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            // Tıklanan sıradaki ID ve Email'i al
            String secilenUid = gizliIdListesi.get(position);
            String secilenEmail = gosterilenListe.get(position);

            kullaniciSilOnayi(secilenUid, secilenEmail);
            return true;
        });
    }

    private void kullanicilariListele() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                gosterilenListe.clear();
                gizliIdListesi.clear();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String uid = userSnapshot.getKey(); // Kullanıcının ID'si


                    String email = userSnapshot.child("email").getValue(String.class);

                    // Eğer eski bir kullanıcıysa ve emaili kaydedilmemişse:
                    if (email == null || email.isEmpty()) {
                        email = "E-posta Yok (Eski Kayıt)";
                    }

                    gosterilenListe.add(email);
                    gizliIdListesi.add(uid);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Admin.this, "Hata: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void kullaniciSilOnayi(String uid, String email) {
        new AlertDialog.Builder(this)
                .setTitle("Kullanıcıyı Yasakla")
                .setMessage(email + " hesabını silmek ve yasaklamak istiyor musunuz?")
                .setPositiveButton("Evet, Sil", (dialog, which) -> {


                    DatabaseReference banRef = FirebaseDatabase.getInstance().getReference("deleted_users");

                    banRef.child(uid).setValue(true).addOnSuccessListener(aVoid -> {


                        // Böylece Admin panelinden kaybolur.
                        usersRef.child(uid).removeValue()
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, "Kullanıcı yasaklandı ve listeden silindi.", Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Listeden silinemedi!", Toast.LENGTH_SHORT).show());

                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Yasaklama işlemi başarısız oldu!", Toast.LENGTH_SHORT).show();
                    });

                })
                .setNegativeButton("İptal", null)
                .show();
    }
}