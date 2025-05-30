package com.example.gadgetinventory.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.gadgetinventory.data.entity.GadgetEntity;
import java.util.List;

@Dao
public interface GadgetDao {
    @Insert
    long insert(GadgetEntity gadget);

    @Update
    void update(GadgetEntity gadget);

    @Delete
    void delete(GadgetEntity gadget);

    @Query("SELECT * FROM gadgets ORDER BY name ASC")
    LiveData<List<GadgetEntity>> getAllGadgets();

    @Query("SELECT * FROM gadgets WHERE id = :id")
    LiveData<GadgetEntity> getGadgetById(long id);

    @Query("SELECT * FROM gadgets WHERE name LIKE :searchQuery OR model LIKE :searchQuery")
    LiveData<List<GadgetEntity>> searchGadgets(String searchQuery);

    @Query("DELETE FROM gadgets")
    void deleteAll();
} 