package com.example.farm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class GirişEkrani extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText editTextEmail, editTextPassword;
    private CardView groupAuth;
    private LinearLayout groupHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.girisekrani);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
        }
        Button btnAdmin = findViewById(R.id.btnAdminPage);
        btnAdmin.setOnClickListener(v -> {
            startActivity(new Intent(GirişEkrani.this, Admingiris.class));
        });
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        Button buttonSignIn = findViewById(R.id.buttonSignIn);
        Button buttonSignUp = findViewById(R.id.buttonSignUp);
        groupAuth = findViewById(R.id.group_auth);

        buttonSignUp.setOnClickListener(v -> startActivity(new Intent(GirişEkrani.this, Kayıt.class)));
        buttonSignIn.setOnClickListener(v -> girisYap());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(GirişEkrani.this, AnaSayfa.class));
            finish();
        } else {
            groupAuth.setVisibility(View.VISIBLE);
        }
    }

    private void girisYap() {
        String email = editTextEmail.getText().toString().trim();
        String sifre = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || sifre.isEmpty()) {
            Toast.makeText(this, "Bilgileri giriniz.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, sifre)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        DatabaseReference banRef = FirebaseDatabase.getInstance().getReference("deleted_users");

                        // Kullanıcı "deleted_users" tablosunda var mı?
                        banRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    Toast.makeText(GirişEkrani.this, "Bu hesap yönetici tarafından silindi/yasaklandı!", Toast.LENGTH_LONG).show();
                                    mAuth.signOut(); // Çıkış yap
                                } else {
                                    startActivity(new Intent(GirişEkrani.this, AnaSayfa.class));
                                    finish();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(GirişEkrani.this, "Bağlantı hatası!", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        Toast.makeText(GirişEkrani.this, "Giriş Başarısız: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}