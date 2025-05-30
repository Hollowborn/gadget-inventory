package com.example.gadgetinventory.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.gadgetinventory.R;
import com.example.gadgetinventory.viewmodel.GadgetViewModel;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

public class SettingsFragment extends Fragment {
    private SharedPreferences preferences;
    private GadgetViewModel viewModel;
    private MaterialSwitch lightThemeSwitch;
    private MaterialSwitch darkThemeSwitch;
    private MaterialSwitch systemThemeSwitch;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(GadgetViewModel.class);

        // Initialize SharedPreferences
        preferences = requireContext().getSharedPreferences("settings", 0);

        // Initialize theme switches
        lightThemeSwitch = view.findViewById(R.id.lightThemeSwitch);
        darkThemeSwitch = view.findViewById(R.id.darkThemeSwitch);
        systemThemeSwitch = view.findViewById(R.id.systemThemeSwitch);

        // Set up theme switches
        setupThemeSwitches();

        // Set up reset database button
        view.findViewById(R.id.resetDatabaseButton).setOnClickListener(v -> showResetDatabaseDialog());
    }

    private void setupThemeSwitches() {
        // Get current theme mode
        int currentMode = preferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Set initial switch states
        lightThemeSwitch.setChecked(currentMode == AppCompatDelegate.MODE_NIGHT_NO);
        darkThemeSwitch.setChecked(currentMode == AppCompatDelegate.MODE_NIGHT_YES);
        systemThemeSwitch.setChecked(currentMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Set up click listeners
        lightThemeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                updateTheme(AppCompatDelegate.MODE_NIGHT_NO);
                darkThemeSwitch.setChecked(false);
                systemThemeSwitch.setChecked(false);
            }
        });

        darkThemeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                updateTheme(AppCompatDelegate.MODE_NIGHT_YES);
                lightThemeSwitch.setChecked(false);
                systemThemeSwitch.setChecked(false);
            }
        });

        systemThemeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                updateTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                lightThemeSwitch.setChecked(false);
                darkThemeSwitch.setChecked(false);
            }
        });
    }

    private void updateTheme(int mode) {
        preferences.edit().putInt("theme_mode", mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private void showResetDatabaseDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.reset_database)
                .setMessage(R.string.confirm_reset)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    viewModel.deleteAll();
                    Snackbar.make(requireView(), "Database reset successfully",
                            Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}