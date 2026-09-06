package com.demo.qr;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class MainActivity extends AppCompatActivity {

    private LinearLayout tabHome, tabScan, tabGenerate, tabHistory;
    private ImageView iconHome, iconScan, iconGenerate, iconHistory;
    private TextView labelHome, labelScan, labelGenerate, labelHistory;

    private Fragment fragmentHome;
    private Fragment fragmentScan;
    private Fragment fragmentGenerate;
    private Fragment fragmentHistory;
    private Fragment activeFragment;

    private int currentTab = 0;
    private int previousTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View mainRoot = findViewById(R.id.mainRoot);
        if (mainRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                return windowInsets;
            });
        }

        // Nạp repository dùng chung từ SQLite
        HistoryRepository.getInstance().load(this);

        bindViews();
        setupFragments();
        setupTabListeners();
        selectTab(0);
    }

    private void bindViews() {
        tabHome = findViewById(R.id.tabHome);
        tabScan = findViewById(R.id.tabScan);
        tabGenerate = findViewById(R.id.tabGenerate);
        tabHistory = findViewById(R.id.tabHistory);

        iconHome = findViewById(R.id.iconHome);
        iconScan = findViewById(R.id.iconScan);
        iconGenerate = findViewById(R.id.iconGenerate);
        iconHistory = findViewById(R.id.iconHistory);

        labelHome = findViewById(R.id.labelHome);
        labelScan = findViewById(R.id.labelScan);
        labelGenerate = findViewById(R.id.labelGenerate);
        labelHistory = findViewById(R.id.labelHistory);
    }

    private void setupFragments() {
        fragmentHome = new HomeFragment();
        fragmentScan = new ScanFragment();
        fragmentGenerate = new GenerateFragment();
        fragmentHistory = new HistoryFragment();

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragmentHistory, "3").hide(fragmentHistory)
                .add(R.id.fragmentContainer, fragmentGenerate, "2").hide(fragmentGenerate)
                .add(R.id.fragmentContainer, fragmentScan, "1").hide(fragmentScan)
                .add(R.id.fragmentContainer, fragmentHome, "0")
                .commit();

        activeFragment = fragmentHome;
    }

    private void setupTabListeners() {
        tabHome.setOnClickListener(v -> selectTab(0));
        tabScan.setOnClickListener(v -> selectTab(1));
        tabGenerate.setOnClickListener(v -> selectTab(2));
        tabHistory.setOnClickListener(v -> selectTab(3));
    }

    public void selectTab(int index) {
        previousTab = currentTab;
        currentTab = index;

        Fragment targetFragment;
        switch (index) {
            case 1:
                targetFragment = fragmentScan;
                break;
            case 2:
                targetFragment = fragmentGenerate;
                break;
            case 3:
                targetFragment = fragmentHistory;
                break;
            default:
                targetFragment = fragmentHome;
                break;
        }

        if (targetFragment != activeFragment && targetFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .hide(activeFragment)
                    .show(targetFragment)
                    .commit();
            activeFragment = targetFragment;
        }

        updateTabUI();
    }

    private void updateTabUI() {
        int colorMint = ContextCompat.getColor(this, R.color.qr_mint);
        int colorMuted = ContextCompat.getColor(this, R.color.qr_muted);

        // Reset all to inactive
        iconHome.setColorFilter(colorMuted);
        iconScan.setColorFilter(colorMuted);
        iconGenerate.setColorFilter(colorMuted);
        iconHistory.setColorFilter(colorMuted);

        labelHome.setTextColor(colorMuted);
        labelScan.setTextColor(colorMuted);
        labelGenerate.setTextColor(colorMuted);
        labelHistory.setTextColor(colorMuted);

        // Highlight selected
        switch (currentTab) {
            case 0:
                iconHome.setColorFilter(colorMint);
                labelHome.setTextColor(colorMint);
                break;
            case 1:
                iconScan.setColorFilter(colorMint);
                labelScan.setTextColor(colorMint);
                break;
            case 2:
                iconGenerate.setColorFilter(colorMint);
                labelGenerate.setTextColor(colorMint);
                break;
            case 3:
                iconHistory.setColorFilter(colorMint);
                labelHistory.setTextColor(colorMint);
                break;
        }
    }

    public int getPreviousTab() {
        return previousTab;
    }
}
