package com.example.farm;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns; // BU KÜTÜPHANEYİ EKLEDİK
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class Admingiris extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admingiris);

        ImageButton btnkapat = findViewById(R.id.btnkapat);
        EditText etUser = findViewById(R.id.etAdminUser);
        EditText etPass = findViewById(R.id.etAdminPass);
        Button btnGiris = findViewById(R.id.btnAdminLoginAction);

        btnGiris.setOnClickListener(v -> {
            String kullaniciAdi = etUser.getText().toString().trim();
            String sifre = etPass.getText().toString().trim();

            // 1. KONTROL: Boş alan var mı?
            if (kullaniciAdi.isEmpty() || sifre.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurunuz!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. KONTROL: Girilen şey gerçekten bir E-Posta mı?
            if (!Patterns.EMAIL_ADDRESS.matcher(kullaniciAdi).matches()) {
                etUser.setError("Geçersiz e-posta formatı!");
                etUser.requestFocus();
                return;
            }

            // 3. KONTROL: Admin bilgileri doğru mu?
            if (kullaniciAdi.equals("admin@farm.com") && sifre.equals("123456")) {
                Toast.makeText(this, "Giriş Başarılı! Hoş geldin Patron.", Toast.LENGTH_SHORT).show();

                // Admin Paneline Yönlendir
                startActivity(new Intent(Admingiris.this, Admin.class));
                finish();

            } else {
                Toast.makeText(this, "Hatalı Yönetici E-postası veya Şifre!", Toast.LENGTH_SHORT).show();
            }
        });

        btnkapat.setOnClickListener(view -> {
            finish();
        });
    }
}