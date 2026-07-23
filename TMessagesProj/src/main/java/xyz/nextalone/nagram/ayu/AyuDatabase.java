package xyz.nextalone.nagram.ayu;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {DeletedMessage.class}, version = 1, exportSchema = false)
public abstract class AyuDatabase extends RoomDatabase {

    private static volatile AyuDatabase instance;

    public abstract DeletedMessageDao deletedMessageDao();

    public static AyuDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AyuDatabase.class) {
                if (instance == null) {
                    instance = Room
                            .databaseBuilder(
                                    context.getApplicationContext(),
                                    AyuDatabase.class,
                                    "ayu_messages.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
