package com.example.gadgetinventory.ui.inventory;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.gadgetinventory.R;
import com.example.gadgetinventory.data.entity.GadgetEntity;
import com.example.gadgetinventory.databinding.FragmentInventoryBinding;
import com.example.gadgetinventory.viewmodel.GadgetViewModel;
import java.util.List;

public class InventoryFragment extends Fragment implements GadgetAdapter.OnGadgetClickListener {
    private FragmentInventoryBinding binding;
    private GadgetViewModel gadgetViewModel;
    private GadgetAdapter gadgetAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentInventoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gadgetViewModel = new ViewModelProvider(requireActivity()).get(GadgetViewModel.class);
        setupRecyclerView();
        observeGadgets();
    }

    private void setupRecyclerView() {
        gadgetAdapter = new GadgetAdapter(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(gadgetAdapter);
    }

    private void observeGadgets() {
        gadgetViewModel.getAllGadgets().observe(getViewLifecycleOwner(), gadgets -> {
            gadgetAdapter.submitList(gadgets);
            updateTotalValue(gadgets);
        });
    }

    private void updateTotalValue(List<GadgetEntity> gadgets) {
        double totalValue = 0;
        for (GadgetEntity gadget : gadgets) {
            totalValue += gadget.getEstimatedValue();
        }
        binding.totalValueText.setText(String.format("Total Inventory Value: ₱%.2f", totalValue));
    }

    @Override
    public void onGadgetClick(GadgetEntity gadget) {
        Bundle args = new Bundle();
        args.putLong("gadgetId", gadget.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_inventory_to_details, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}