package com.example.farm;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class HayvanAdapter extends RecyclerView.Adapter<HayvanAdapter.SatirTutucu> {

    Context context;
    ArrayList<HayvanVeriTabanı> liste;
    OnItemClickListener listener;

    // Tıklanma olayını yönetmek için bir arayüz (Interface) tanımladım.
    public interface OnItemClickListener {
        void onItemClick(HayvanVeriTabanı hayvan);
    }

    public HayvanAdapter(Context context, ArrayList<HayvanVeriTabanı> liste, OnItemClickListener listener) {
        this.context = context;
        this.liste = liste;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SatirTutucu onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Tasarım dosyasını (hayvanlistgorunum.xml) bağladım.
        View v = LayoutInflater.from(context).inflate(R.layout.hayvanlistgorunum, parent, false);
        return new SatirTutucu(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SatirTutucu holder, int position) {
        HayvanVeriTabanı h = liste.get(position);
        holder.Kupe.setText("Küpe: " + h.kulakKupeNo);
        holder.Tip.setText(h.tur + " - " + h.irk);
        holder.Durum.setText(h.durum);

        if (h.resimUrl != null && !h.resimUrl.equals("yok") && h.resimUrl.length() > 20) {

            byte[] decodedString = android.util.Base64.decode(h.resimUrl, android.util.Base64.DEFAULT);
            android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.img.setImageBitmap(decodedByte);

        } else {
            holder.img.setImageResource(R.drawable.inekresmi);
        }

        // Listeden bir elemana tıklanınca listener'ı tetikliyorum.
        holder.itemView.setOnClickListener(v -> listener.onItemClick(h));
    }

    @Override
    public int getItemCount() {
        return liste.size();
    }

    // Arama yapıldığında listeyi güncellemek için.
    public void filterList(ArrayList<HayvanVeriTabanı> filteredList) {
        this.liste = filteredList;
        notifyDataSetChanged();
    }

    public static class SatirTutucu extends RecyclerView.ViewHolder {
        TextView Kupe, Tip, Durum;
        ImageView img;
        public SatirTutucu(@NonNull View itemView) {
            super(itemView);
            Kupe = itemView.findViewById(R.id.tvListeKupe);
            Tip = itemView.findViewById(R.id.tvListeTip);
            Durum = itemView.findViewById(R.id.tvListeDurum);
            img = itemView.findViewById(R.id.imgListeIcon);
        }
    }
}