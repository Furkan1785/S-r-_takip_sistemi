package com.example.farm;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.text.InputFilter;
import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HayvanEkle extends AppCompatActivity {

    private DatabaseReference databaseRef;
    private FirebaseAuth auth;

    private EditText etKupe, etAd, etTarih, etAgirlik, etNot;
    private RadioGroup rgCinsiyet, rgTip, rgDurum;
    private Spinner spIrk;
    private CheckBox cbHasta, cbAsi;
    private Button btnKaydet;
    private ImageButton btnFoto;
    private ImageView imgHayvan;

    private Uri resimUri = null;
    private androidx.cardview.widget.CardView layoutGebeTarih;
    private EditText etTohumlama;
    private final Calendar takvim = Calendar.getInstance();
    private String[] irklar = {"Holstein", "Simental", "Jersey", "Angus", "Yerli Kara", "Diğer"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hayvanekle);

        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        tanimla();

        etTarih.setOnClickListener(v -> tarihSec());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, irklar);
        spIrk.setAdapter(adapter);

        btnFoto.setOnClickListener(v -> secimKutusuGoster());

        // Eğer bu sayfa "Düzenle" butonuna basılarak açıldıysa
        if (getIntent().getBooleanExtra("duzenleModu", false)) {
            btnKaydet.setText("Güncellemeyi Kaydet");
            etKupe.setEnabled(false);
            formuDoldur();
        }


        btnKaydet.setOnClickListener(v -> {
            String kaydedilecekResim = "yok";

            // 1. Kullanıcı YENİ bir resim seçtiyse onu al
            if (resimUri != null) {
                try {
                    imgHayvan.setDrawingCacheEnabled(true);
                    imgHayvan.buildDrawingCache();
                    Bitmap bitmap = imgHayvan.getDrawingCache();
                    kaydedilecekResim = bitmapToString(bitmap);
                } catch (Exception e) {
                    kaydedilecekResim = "yok";
                }
            }
            //  Yeni resim seçmedi ama DÜZENLEME modundaysak ve eski resim varsa
            // (Eski resmi imgHayvan'ın içine 'Tag' olarak saklamıştık)
            else if (imgHayvan.getTag() != null) {
                kaydedilecekResim = imgHayvan.getTag().toString();
            }

            kontrolEtVeKaydet(kaydedilecekResim);
        });

        etTohumlama.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                c.set(y, m, d);
                etTohumlama.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        //  Radyo Buton Dinleyici (Gebe seçilirse aç)
        rgDurum.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioGebe) {
                layoutGebeTarih.setVisibility(View.VISIBLE); // Göster
                Toast.makeText(this, "Lütfen tohumlama tarihini giriniz.", Toast.LENGTH_SHORT).show();
            } else {
                layoutGebeTarih.setVisibility(View.GONE); // Gizle
                etTohumlama.setText(""); // İçini temizle
            }
        });
        rgCinsiyet.setOnCheckedChangeListener((group, checkedId) -> {

            // 1. Önce Yönetilecek Butonları Tanımlayalım
            android.widget.RadioButton rbGebe = findViewById(R.id.radioGebe);
            android.widget.RadioButton rbSagimda = findViewById(R.id.radioSagimda);
            android.widget.RadioButton rbDana = findViewById(R.id.radioDana);
            android.widget.RadioButton rbDuve = findViewById(R.id.radioDuve);
            android.widget.RadioButton rbInek = findViewById(R.id.radioInek);

            if (checkedId == R.id.radioErkek) {

                rbDuve.setEnabled(false);
                rbInek.setEnabled(false);

                rbDana.setEnabled(true);

                rbGebe.setEnabled(false);
                rbSagimda.setEnabled(false);

                // Eğer o an yasaklı bir "Tip" seçiliyse (Düve/İnek), temizle
                if (rgTip.getCheckedRadioButtonId() == R.id.radioDuve ||
                        rgTip.getCheckedRadioButtonId() == R.id.radioInek) {
                    rgTip.clearCheck();
                    Toast.makeText(this, "Erkek hayvan Düve veya İnek olamaz.", Toast.LENGTH_SHORT).show();
                }

                //  Eğer o an yasaklı bir "Durum" seçiliyse (Gebe/Sağımda), "Normal" yap
                if (rgDurum.getCheckedRadioButtonId() == R.id.radioGebe ||
                        rgDurum.getCheckedRadioButtonId() == R.id.radioSagimda) {
                    rgDurum.check(R.id.radioNormal);
                    Toast.makeText(this, "Erkek hayvan Gebe olamaz, durum 'Normal' yapıldı.", Toast.LENGTH_SHORT).show();
                }
            }

            else if (checkedId == R.id.radioDişi) {

                rbDana.setEnabled(false);


                rbDuve.setEnabled(true);
                rbInek.setEnabled(true);

                rbGebe.setEnabled(true);
                rbSagimda.setEnabled(true);

                if (rgTip.getCheckedRadioButtonId() == R.id.radioDana) {
                    rgTip.clearCheck();
                    Toast.makeText(this, "Dişi hayvan 'Dana' olamaz.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rgTip.setOnCheckedChangeListener((group, checkedId) -> {

            android.widget.RadioButton rbGebe = findViewById(R.id.radioGebe);
            android.widget.RadioButton rbSagimda = findViewById(R.id.radioSagimda);

            if (checkedId == R.id.radioBuzagi) {

                rbGebe.setEnabled(false);
                rbSagimda.setEnabled(false);

                if (rgDurum.getCheckedRadioButtonId() == R.id.radioGebe ||
                        rgDurum.getCheckedRadioButtonId() == R.id.radioSagimda) {
                    rgDurum.check(R.id.radioNormal);
                    Toast.makeText(this, "Buzağı Gebe veya Sağımda olamaz.", Toast.LENGTH_SHORT).show();
                }
            }
            //
            else {

                if (rgCinsiyet.getCheckedRadioButtonId() != R.id.radioErkek) {
                    rbGebe.setEnabled(true);
                    rbSagimda.setEnabled(true);
                }
            }
        });
    }

    private void formuDoldur() {
        Intent i = getIntent();

        etKupe.setText(i.getStringExtra("kupe"));
        etAd.setText(i.getStringExtra("ad"));
        etTarih.setText(i.getStringExtra("dogum"));
        etAgirlik.setText(i.getStringExtra("agirlik"));
        etNot.setText(i.getStringExtra("not"));

        cbHasta.setChecked(i.getBooleanExtra("hastaMi", false));
        cbAsi.setChecked(i.getBooleanExtra("asiliMi", false));

        // Cinsiyet Seçimi
        String cinsiyet = i.getStringExtra("cinsiyet");
        if (cinsiyet != null) {
            if (cinsiyet.equals("Erkek")) rgCinsiyet.check(R.id.radioErkek);
            else if (cinsiyet.equals("Dişi")) rgCinsiyet.check(R.id.radioDişi);
        }
        // formuDoldur içinde:
        String gelenTohumlama = i.getStringExtra("tohumlama");
        if (gelenTohumlama != null && !gelenTohumlama.isEmpty()) {
            etTohumlama.setText(gelenTohumlama);
            // Eğer durumu gebeyse kutuyu aç
            if (i.getStringExtra("durum").equals("Gebe")) {
                layoutGebeTarih.setVisibility(View.VISIBLE);
            }
        }

        // Tür Seçimi
        String tur = i.getStringExtra("tur");
        if (tur != null) {
            if (tur.equals("İnek")) rgTip.check(R.id.radioInek);
            else if (tur.equals("Düve")) rgTip.check(R.id.radioDuve);
            else if (tur.equals("Dana")) rgTip.check(R.id.radioDana);
            else if (tur.equals("Buzağı")) rgTip.check(R.id.radioBuzagi);
        }

        // Durum Seçimi
        String durum = i.getStringExtra("durum");
        if (durum != null) {
            if (durum.equals("Normal")) rgDurum.check(R.id.radioNormal);
            else if (durum.equals("Sağımda")) rgDurum.check(R.id.radioSagimda);
            else if (durum.equals("Kuruda")) rgDurum.check(R.id.radioKuruda);
            else if (durum.equals("Gebe")) rgDurum.check(R.id.radioGebe);
        }

        // Spinner (Irk) Seçimi
        String gelenIrk = i.getStringExtra("irk");
        if (gelenIrk != null) {
            for (int k = 0; k < spIrk.getCount(); k++) {
                if (spIrk.getItemAtPosition(k).toString().equals(gelenIrk)) {
                    spIrk.setSelection(k);
                    break;
                }
            }
        }

        // Resmi Geri Yükle
        String gelenResim = i.getStringExtra("url");
        if (gelenResim != null && gelenResim.length() > 20) {
            try {
                byte[] decodedString = Base64.decode(gelenResim, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imgHayvan.setImageBitmap(decodedByte);

                // Resmi değiştirmeden kaydederse diye, eski kodu buraya saklıyoruz
                imgHayvan.setTag(gelenResim);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    private void kontrolEtVeKaydet(String resimData) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String kupe = etKupe.getText().toString().trim();
        String agirlik = etAgirlik.getText().toString().trim();
        String dogumTarihi = etTarih.getText().toString();

        if (kupe.isEmpty() || agirlik.isEmpty() || dogumTarihi.isEmpty()) {
            Toast.makeText(this, "Lütfen Küpe No, Doğum Tarihi ve Ağırlığı giriniz!", Toast.LENGTH_LONG).show();
            return;
        }

        // GELECEK ZAMAN OLAMAZ
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date dateDogum = sdf.parse(dogumTarihi);
            Date dateBugun = new Date(); // Şu an

            if (dateDogum != null && dateDogum.after(dateBugun)) {
                Toast.makeText(this, "Hata: Doğum tarihi bugünden sonra olamaz!", Toast.LENGTH_LONG).show();
                return; // İşlemi durdur
            }
        } catch (Exception e) {
            e.printStackTrace();
        }



        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Hayvan kaydediliyor, lütfen bekleyiniz...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Veritabanı Yolu
        DatabaseReference hayvanRef = databaseRef.child("users").child(user.getUid()).child("animals").child(kupe);

        // Eğer "Düzenle Modu"ndaysak bu kontrolü yapma, çünkü kendi üstüne yazıyoruz.
        boolean duzenleModu = getIntent().getBooleanExtra("duzenleModu", false);

        if (!duzenleModu) {
            hayvanRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    // KÜPE NO ZATEN VAR!
                    progressDialog.dismiss(); // Yükleniyor yazısını kapat
                    Toast.makeText(HayvanEkle.this, "HATA: Bu Küpe Numarası (" + kupe + ") zaten kayıtlı!", Toast.LENGTH_LONG).show();
                } else {
                    // KÜPE YOK, KAYDETMEYE DEVAM ET
                    veritabaninaYaz(hayvanRef, user, kupe, agirlik, dogumTarihi, resimData, progressDialog);
                }
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(HayvanEkle.this, "Bağlantı hatası!", Toast.LENGTH_SHORT).show();
            });
        } else {
            // DÜZENLEME MODUYSA DİREKT KAYDET
            veritabaninaYaz(hayvanRef, user, kupe, agirlik, dogumTarihi, resimData, progressDialog);
        }
    }

    // KAYIT İŞLEMİNİ YAPAN YARDIMCI METOD
    private void veritabaninaYaz(DatabaseReference ref, FirebaseUser user, String kupe, String agirlik, String dogumTarihi, String resimData, android.app.ProgressDialog pd) {
        // Seçimleri Al
        String tip = "Bilinmiyor";
        if (rgTip.getCheckedRadioButtonId() == R.id.radioInek) tip = "İnek";
        else if (rgTip.getCheckedRadioButtonId() == R.id.radioDuve) tip = "Düve";
        else if (rgTip.getCheckedRadioButtonId() == R.id.radioDana) tip = "Dana";
        else if (rgTip.getCheckedRadioButtonId() == R.id.radioBuzagi) tip = "Buzağı";

        String durum = "Normal";
        if (rgDurum.getCheckedRadioButtonId() == R.id.radioGebe) durum = "Gebe";
        else if (rgDurum.getCheckedRadioButtonId() == R.id.radioSagimda) durum = "Sağımda";
        else if (rgDurum.getCheckedRadioButtonId() == R.id.radioKuruda) durum = "Kuruda";

        String cinsiyet = "Bilinmiyor";
        if (rgCinsiyet.getCheckedRadioButtonId() == R.id.radioErkek) cinsiyet = "Erkek";
        else if (rgCinsiyet.getCheckedRadioButtonId() == R.id.radioDişi) cinsiyet = "Dişi";

        String irk = spIrk.getSelectedItem().toString();
        String kayitTarihi = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String tohumlama = etTohumlama.getText().toString();

        HayvanVeriTabanı hayvan = new HayvanVeriTabanı(kupe, user.getUid(), kayitTarihi, etAd.getText().toString(),
                dogumTarihi, cinsiyet, irk, tip,
                Double.parseDouble(agirlik), durum, cbHasta.isChecked(),
                cbAsi.isChecked(), etNot.getText().toString(), resimData, tohumlama);

        ref.setValue(hayvan).addOnSuccessListener(aVoid -> {
            pd.dismiss(); // Yükleniyor'u kapat
            Toast.makeText(HayvanEkle.this, "✅ Hayvan Başarıyla Kaydedildi!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(HayvanEkle.this, "Kaydetme başarısız oldu.", Toast.LENGTH_SHORT).show();
        });
    }
    private Bitmap resmiKucult(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height, true);
    }
    private String bitmapToString(Bitmap bitmap) {
        Bitmap kucukResim = resmiKucult(bitmap, 500);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        kucukResim.compress(Bitmap.CompressFormat.JPEG, 70, baos);

        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }
    private void tanimla() {
        etKupe = findViewById(R.id.editTextKupeNo);
        etAd = findViewById(R.id.editTextTakmaAd);
        etAd.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });
        etTarih = findViewById(R.id.editTextDogumTarihi);
        etAgirlik = findViewById(R.id.editTextAgirlik);
        etNot = findViewById(R.id.editTextNotlar);
        rgCinsiyet = findViewById(R.id.radioGroupCinsiyet);
        rgTip = findViewById(R.id.radioGroupTip);
        rgDurum = findViewById(R.id.radioGroupDurum);
        spIrk = findViewById(R.id.spinnerIrkCinsi);
        cbHasta = findViewById(R.id.checkHastaGecmis);
        cbAsi = findViewById(R.id.checkAsiDurumu);
        btnKaydet = findViewById(R.id.buttonSaveAnimal);
        btnFoto = findViewById(R.id.btnFotoSec);
        imgHayvan = findViewById(R.id.imgHayvanEkle);
        layoutGebeTarih = findViewById(R.id.layoutGebeTarih);
        etTohumlama = findViewById(R.id.etTohumlamaTarihi);
    }

    private void secimKutusuGoster() {
        String[] secenekler = {"Kamera", "Galeri"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Fotoğraf Ekle");
        builder.setItems(secenekler, (dialog, which) -> {
            if (which == 0) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
                } else {
                    kamerayiAc();
                }
            } else {
                galeriyiAc();
            }
        });
        builder.show();
    }

    private void kamerayiAc() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        kameraBaslatici.launch(intent);
    }

    private void galeriyiAc() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galeriBaslatici.launch(intent);
    }

    ActivityResultLauncher<Intent> galeriBaslatici = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    resimUri = result.getData().getData();
                    imgHayvan.setImageURI(resimUri);
                }
            }
    );

    ActivityResultLauncher<Intent> kameraBaslatici = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap foto = (Bitmap) result.getData().getExtras().get("data");
                    imgHayvan.setImageBitmap(foto);
                    resimUri = resmiKaydetVeYolunuAl(foto);
                }
            }
    );

    private Uri resmiKaydetVeYolunuAl(Bitmap bitmap) {

        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "HayvanFoto", null);
        return Uri.parse(path);
    }

    private void tarihSec() {
        new DatePickerDialog(this, (view, y, m, d) -> {
            takvim.set(y, m, d);
            etTarih.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(takvim.getTime()));
        }, takvim.get(Calendar.YEAR), takvim.get(Calendar.MONTH), takvim.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            kamerayiAc();
        }
    }
}