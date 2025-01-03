package com.example.bloodbank.handler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bloodbank.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.SiteViewHolder> {

    private List<DocumentSnapshot> siteList;
    private OnSiteSelectedListener listener;

    public SiteAdapter(List<DocumentSnapshot> siteList, OnSiteSelectedListener listener) {
        this.siteList = siteList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SiteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.site_item, parent, false);
        return new SiteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SiteViewHolder holder, int position) {
        DocumentSnapshot site = siteList.get(position);
        holder.bind(site, listener);
    }

    @Override
    public int getItemCount() {
        return siteList.size();
    }

    public static class SiteViewHolder extends RecyclerView.ViewHolder {
        private final TextView siteName;
        private final TextView siteAddress;
        private final TextView bloodDetails;
        private final TextView campaignDate;
        private final LinearLayout siteContainer;

        public SiteViewHolder(@NonNull View itemView) {
            super(itemView);
            siteName = itemView.findViewById(R.id.siteName);
            siteAddress = itemView.findViewById(R.id.siteAddress);
            bloodDetails = itemView.findViewById(R.id.siteDetails);
            campaignDate = itemView.findViewById(R.id.campaignDate);
            siteContainer = (LinearLayout) itemView;
        }

        public void bind(DocumentSnapshot site, OnSiteSelectedListener listener) {
            String shortName = site.getString("shortName");
            String address = site.getString("address");
            List<String> requiredBloodTypes = (List<String>) site.get("requiredBloodTypes");
            String eventDate = site.getString("eventDate");

            siteName.setText(shortName != null ? shortName : "Unknown Site");
            siteAddress.setText(address != null ? address : "No Address Available");
            bloodDetails.setText(requiredBloodTypes != null ? "Required Blood: " + String.join(", ", requiredBloodTypes) : "No Blood Requirements");
            campaignDate.setText(eventDate != null ? "Campaign Date: " + eventDate : "No Campaign Date Available");
            siteContainer.setOnClickListener(v -> listener.onSiteSelected(site));
        }
    }

    public interface OnSiteSelectedListener {
        void onSiteSelected(DocumentSnapshot site);
    }
}
