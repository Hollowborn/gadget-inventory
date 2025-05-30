package com.example.gadgetinventory.ui.add;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.gadgetinventory.R;
import com.example.gadgetinventory.data.entity.GadgetEntity;
import com.example.gadgetinventory.viewmodel.GadgetViewModel;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.graphics.Bitmap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddGadgetFragment extends Fragment {
    private GadgetViewModel viewModel;
    private Uri imageUri;
    private ImageView imageView;
    private TextInputEditText nameInput, modelInput, valueInput, dateInput;
    private AutoCompleteTextView conditionInput;
    private MaterialButton saveButton;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private Date selectedDate;
    private GadgetEntity gadgetToEdit;
    private boolean isEditMode = false;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    loadImage();
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success) {
                    loadImage();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_gadget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(GadgetViewModel.class);

        // Initialize views
        imageView = view.findViewById(R.id.gadgetImage);
        nameInput = view.findViewById(R.id.nameInput);
        modelInput = view.findViewById(R.id.modelInput);
        conditionInput = view.findViewById(R.id.conditionInput);
        valueInput = view.findViewById(R.id.valueInput);
        dateInput = view.findViewById(R.id.dateInput);
        saveButton = view.findViewById(R.id.saveButton);

        // Set up condition dropdown
        String[] conditions = {
                getString(R.string.condition_good),
                getString(R.string.condition_fair),
                getString(R.string.condition_poor)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, conditions);
        conditionInput.setAdapter(adapter);

        // Set up date picker
        dateInput.setOnClickListener(v -> showDatePicker());

        // Set up image selection
        view.findViewById(R.id.takePhotoButton).setOnClickListener(v -> takePhoto());
        view.findViewById(R.id.choosePhotoButton).setOnClickListener(v -> chooseFromGallery());

        // Set up save button
        saveButton.setOnClickListener(v -> saveGadget());

        // Check if we're in edit mode
        if (getArguments() != null) {
            long gadgetId = getArguments().getLong("gadgetId", -1);
            if (gadgetId != -1) {
                isEditMode = true;
                loadGadgetForEditing(gadgetId);
                saveButton.setText(R.string.update);
            } else {
                // Handle detection data if available
                String detectedImageUri = getArguments().getString("detected_gadget_image");
                String detectedModel = getArguments().getString("detected_gadget_model");

                if (detectedImageUri != null) {
                    imageUri = Uri.parse(detectedImageUri);
                    loadImage();
                }

                if (detectedModel != null) {
                    modelInput.setText(detectedModel);
                }
            }
        }
    }

    private void loadGadgetForEditing(long gadgetId) {
        viewModel.getGadgetById(gadgetId).observe(getViewLifecycleOwner(), gadget -> {
            if (gadget != null) {
                gadgetToEdit = gadget;
                
                // Fill in the form with existing data
                nameInput.setText(gadget.getName());
                modelInput.setText(gadget.getModel());
                conditionInput.setText(gadget.getCondition(), false);
                valueInput.setText(String.valueOf(gadget.getEstimatedValue()));
                selectedDate = gadget.getPurchaseDate();
                dateInput.setText(dateFormat.format(selectedDate));
                
                if (gadget.getImageUri() != null && !gadget.getImageUri().isEmpty()) {
                    imageUri = Uri.parse(gadget.getImageUri());
                    loadImage();
                }
            }
        });
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select purchase date")
                .setSelection(selectedDate != null ? selectedDate.getTime() : MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            selectedDate = new Date(selection);
            dateInput.setText(dateFormat.format(selectedDate));
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void takePhoto() {
        File photoFile = new File(requireContext().getExternalFilesDir("Pictures"),
                "gadget_" + System.currentTimeMillis() + ".jpg");
        imageUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", photoFile);
        cameraLauncher.launch(imageUri);
    }

    private void chooseFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void loadImage() {
        if (imageUri != null) {
            Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.ic_gadget_placeholder)
                    .error(R.drawable.ic_gadget_placeholder)
                    .centerCrop()
                    .into(imageView);
        }
    }

    private void saveGadget() {
        String name = nameInput.getText().toString().trim();
        String model = modelInput.getText().toString().trim();
        String condition = conditionInput.getText().toString().trim();
        String valueStr = valueInput.getText().toString().trim();

        // Validate inputs
        if (name.isEmpty() || model.isEmpty() || condition.isEmpty() || valueStr.isEmpty() || selectedDate == null) {
            Snackbar.make(requireView(), R.string.required_field, Snackbar.LENGTH_SHORT).show();
            return;
        }

        double value;
        try {
            value = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            ((TextInputLayout) valueInput.getParent().getParent()).setError("Invalid value");
            return;
        }

        if (isEditMode && gadgetToEdit != null) {
            // Update existing gadget
            gadgetToEdit.setName(name);
            gadgetToEdit.setModel(model);
            gadgetToEdit.setCondition(condition);
            gadgetToEdit.setPurchaseDate(selectedDate);
            gadgetToEdit.setEstimatedValue(value);
            gadgetToEdit.setImageUri(imageUri != null ? imageUri.toString() : "");
            
            viewModel.update(gadgetToEdit);
            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    R.string.success_update, Snackbar.LENGTH_SHORT).show();
        } else {
            // Create new gadget
            GadgetEntity gadget = new GadgetEntity(name, model, condition, selectedDate, value,
                    imageUri != null ? imageUri.toString() : "");
            viewModel.insert(gadget);
            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    R.string.success_save, Snackbar.LENGTH_SHORT).show();
        }

        // Navigate back
        Navigation.findNavController(requireView()).navigateUp();
    }
} 