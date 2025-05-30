package com.example.gadgetinventory.ui.details;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.gadgetinventory.R;
import com.example.gadgetinventory.data.entity.GadgetEntity;
import com.example.gadgetinventory.databinding.FragmentGadgetDetailsBinding;
import com.example.gadgetinventory.viewmodel.GadgetViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class GadgetDetailsFragment extends Fragment {
    private FragmentGadgetDetailsBinding binding;
    private GadgetViewModel gadgetViewModel;
    private GadgetEntity currentGadget;
    private static final double DEFAULT_DEPRECIATION_RATE = 15.0;
    private long gadgetId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGadgetDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gadgetViewModel = new ViewModelProvider(requireActivity()).get(GadgetViewModel.class);

        if (getArguments() != null) {
            gadgetId = getArguments().getLong("gadgetId");
            loadGadgetDetails(gadgetId);
        }

        setupDepreciationCalculator();
        setupEditButton();
        setupDeleteButton();
    }

    private void loadGadgetDetails(long gadgetId) {
        gadgetViewModel.getGadgetById(gadgetId).observe(getViewLifecycleOwner(), gadget -> {
            if (gadget != null) {
                currentGadget = gadget;
                updateUI(gadget);
                calculateDepreciation(DEFAULT_DEPRECIATION_RATE);
            }
        });
    }

    private void updateUI(GadgetEntity gadget) {
        binding.gadgetName.setText(gadget.getName());
        binding.gadgetModel.setText(String.format("Model: %s", gadget.getModel()));
        binding.condition.setText(String.format("Condition: %s", gadget.getCondition()));

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        binding.purchaseDate.setText(String.format("Purchase Date: %s",
                dateFormat.format(gadget.getPurchaseDate())));

        binding.initialValue.setText(String.format("Initial Value: ₱%.2f",
                gadget.getEstimatedValue()));

        if (gadget.getImageUri() != null) {
            Glide.with(this)
                    .load(gadget.getImageUri())
                    .placeholder(R.drawable.ic_gadget_placeholder)
                    .into(binding.gadgetImage);
        }
    }

    private void setupDepreciationCalculator() {
        binding.depreciationRateInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    double rate = s.length() > 0 ? Double.parseDouble(s.toString()) : DEFAULT_DEPRECIATION_RATE;
                    calculateDepreciation(rate);
                } catch (NumberFormatException e) {
                    binding.depreciationRateInput.setError("Please enter a valid number");
                }
            }
        });
    }

    private void calculateDepreciation(double depreciationRate) {
        if (currentGadget == null) return;

        double initialValue = currentGadget.getEstimatedValue();
        Date purchaseDate = currentGadget.getPurchaseDate();
        Date currentDate = new Date();

        // Calculate years since purchase
        Calendar purchase = Calendar.getInstance();
        purchase.setTime(purchaseDate);
        Calendar now = Calendar.getInstance();
        now.setTime(currentDate);

        int yearsSincePurchase = now.get(Calendar.YEAR) - purchase.get(Calendar.YEAR);

        // Calculate depreciation
        double depreciationPerYear = initialValue * (depreciationRate / 100);
        double currentValue = initialValue - (yearsSincePurchase * depreciationPerYear);

        // Ensure value doesn't go below 10% of initial value
        currentValue = Math.max(currentValue, initialValue * 0.1);

        // Update UI
        binding.currentValue.setText(String.format("Current Value: ₱%.2f", currentValue));
        binding.yearlyDepreciation.setText(String.format("Yearly Depreciation: ₱%.2f", depreciationPerYear));
    }

    private void setupEditButton() {
        binding.editButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("gadgetId", gadgetId);
            Navigation.findNavController(requireView())
                    .navigate(R.id.navigation_add, args);
        });
    }

    private void setupDeleteButton() {
        binding.deleteButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Gadget")
                .setMessage("Are you sure you want to delete this gadget? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (currentGadget != null) {
                        gadgetViewModel.delete(currentGadget);
                        Toast.makeText(requireContext(), "Gadget deleted", Toast.LENGTH_SHORT).show();
                        // Navigate back
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}