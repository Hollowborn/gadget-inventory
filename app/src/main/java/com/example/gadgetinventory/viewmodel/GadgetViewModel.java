package com.example.gadgetinventory.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.gadgetinventory.data.entity.GadgetEntity;
import com.example.gadgetinventory.repository.GadgetRepository;
import java.util.List;

public class GadgetViewModel extends AndroidViewModel {
    private final GadgetRepository repository;
    private final LiveData<List<GadgetEntity>> allGadgets;

    public GadgetViewModel(Application application) {
        super(application);
        repository = new GadgetRepository(application);
        allGadgets = repository.getAllGadgets();
    }

    public LiveData<List<GadgetEntity>> getAllGadgets() {
        return allGadgets;
    }

    public LiveData<GadgetEntity> getGadgetById(long id) {
        return repository.getGadgetById(id);
    }

    public void insert(GadgetEntity gadget) {
        repository.insert(gadget);
    }

    public void update(GadgetEntity gadget) {
        repository.update(gadget);
    }

    public void delete(GadgetEntity gadget) {
        repository.delete(gadget);
    }

    public LiveData<List<GadgetEntity>> searchGadgets(String query) {
        return repository.searchGadgets(query);
    }

    public void deleteAll() {
        repository.deleteAll();
    }
} 