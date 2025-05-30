package com.example.gadgetinventory.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.gadgetinventory.data.dao.GadgetDao;
import com.example.gadgetinventory.data.database.GadgetDatabase;
import com.example.gadgetinventory.data.entity.GadgetEntity;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GadgetRepository {
    private final GadgetDao gadgetDao;
    private final ExecutorService executorService;

    public GadgetRepository(Application application) {
        GadgetDatabase database = GadgetDatabase.getDatabase(application);
        gadgetDao = database.gadgetDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(GadgetEntity gadget) {
        executorService.execute(() -> gadgetDao.insert(gadget));
    }

    public void update(GadgetEntity gadget) {
        executorService.execute(() -> gadgetDao.update(gadget));
    }

    public void delete(GadgetEntity gadget) {
        executorService.execute(() -> gadgetDao.delete(gadget));
    }

    public LiveData<List<GadgetEntity>> getAllGadgets() {
        return gadgetDao.getAllGadgets();
    }

    public LiveData<GadgetEntity> getGadgetById(long id) {
        return gadgetDao.getGadgetById(id);
    }

    public LiveData<List<GadgetEntity>> searchGadgets(String query) {
        return gadgetDao.searchGadgets("%" + query + "%");
    }

    public void deleteAll() {
        executorService.execute(gadgetDao::deleteAll);
    }
} 