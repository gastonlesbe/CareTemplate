package com.gastonlesbegueris.caretemplate.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class GriscalDashboardActivity extends AppCompatActivity {

    private SubjectDao subjectDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_griscal_dashboard);

        subjectDao = AppDb.get(this).subjectDao();

        // Show user email
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            TextView tvEmail = findViewById(R.id.tv_user_email);
            tvEmail.setText(user.getEmail());
        }

        // Category card click listeners
        MaterialCardView cardPets = findViewById(R.id.card_pets);
        MaterialCardView cardFamily = findViewById(R.id.card_family);
        MaterialCardView cardCars = findViewById(R.id.card_cars);
        MaterialCardView cardHouses = findViewById(R.id.card_houses);

        cardPets.setOnClickListener(v -> openCategory("pets", "Pets"));
        cardFamily.setOnClickListener(v -> openCategory("family", "Family"));
        cardCars.setOnClickListener(v -> openCategory("cars", "Vehicles"));
        cardHouses.setOnClickListener(v -> openCategory("house", "Houses"));

        // Logout
        findViewById(R.id.btn_logout).setOnClickListener(v -> confirmLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCounts();
    }

    private void refreshCounts() {
        new Thread(() -> {
            int pets   = subjectDao.countForApp("pets");
            int family = subjectDao.countForApp("family");
            int cars   = subjectDao.countForApp("cars");
            int house  = subjectDao.countForApp("house");

            runOnUiThread(() -> {
                ((TextView) findViewById(R.id.tv_count_pets)).setText(itemLabel(pets));
                ((TextView) findViewById(R.id.tv_count_family)).setText(itemLabel(family));
                ((TextView) findViewById(R.id.tv_count_cars)).setText(itemLabel(cars));
                ((TextView) findViewById(R.id.tv_count_houses)).setText(itemLabel(house));
            });
        }).start();
    }

    private String itemLabel(int count) {
        return count == 1 ? "1 item" : count + " items";
    }

    private void openCategory(String appType, String title) {
        Intent intent = new Intent(this, SubjectListActivity.class);
        intent.putExtra("extra_app_type", appType);
        intent.putExtra("extra_section_title", title);
        startActivity(intent);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sign out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign out", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
