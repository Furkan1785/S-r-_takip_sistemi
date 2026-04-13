package com.example.farm;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Kayıt extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText editTextRegisterEmail, editTextRegisterPassword;
    private Button buttonFinalSignUp;
    private TextView textBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.kayitekrani);

        mAuth = FirebaseAuth.getInstance();

        editTextRegisterEmail = findViewById(R.id.editTextRegisterEmail);
        editTextRegisterPassword = findViewById(R.id.editTextRegisterPassword);
        buttonFinalSignUp = findViewById(R.id.buttonFinalSignUp);
        textBackToLogin = findViewById(R.id.textBackToLogin);


        buttonFinalSignUp.setOnClickListener(v -> registerNewUser());

        textBackToLogin.setOnClickListener(v -> finish());
    }

    private void registerNewUser() {
        String email = editTextRegisterEmail.getText().toString().trim();
        String password = editTextRegisterPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || password.length() < 6 || password.length() > 15) {
            Toast.makeText(this, "E-posta ve şifre (6-15 karakter) zorunludur.", Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(this, "Kullanıcı oluşturulamadı!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String uid = user.getUid();

                        DatabaseReference userRef =
                                FirebaseDatabase.getInstance().getReference("users").child(uid);

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", email);
                        userData.put("id", uid);
                        userData.put("role", "user");

                        userRef.setValue(userData).addOnCompleteListener(dbTask -> {
                            if (dbTask.isSuccessful()) {
                                Toast.makeText(Kayıt.this, "Kayıt Başarılı!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(Kayıt.this, GirişEkrani.class));
                                finish();
                            } else {
                                Toast.makeText(Kayıt.this, "Veritabanı hatası!", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        try {
                            throw task.getException();
                        } catch (com.google.firebase.auth.FirebaseAuthUserCollisionException e) {
                            Toast.makeText(this, "Bu e-posta adresi zaten kayıtlı!", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Bir hata oluştu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}