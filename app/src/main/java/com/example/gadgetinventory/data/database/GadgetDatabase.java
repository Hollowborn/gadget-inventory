package com.example.gadgetinventory.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.example.gadgetinventory.data.converter.DateConverter;
import com.example.gadgetinventory.data.dao.GadgetDao;
import com.example.gadgetinventory.data.entity.GadgetEntity;

@Database(entities = {GadgetEntity.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class GadgetDatabase extends RoomDatabase {
    private static volatile GadgetDatabase INSTANCE;
    
    public abstract GadgetDao gadgetDao();
    
    public static GadgetDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (GadgetDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            GadgetDatabase.class,
                            "gadget_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
} 