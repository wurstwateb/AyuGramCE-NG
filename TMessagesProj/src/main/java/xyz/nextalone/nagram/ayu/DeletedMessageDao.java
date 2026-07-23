package xyz.nextalone.nagram.ayu;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DeletedMessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(DeletedMessage message);

    @Query("SELECT COUNT(*) FROM deleted_messages "
            + "WHERE userId = :userId AND dialogId = :dialogId AND messageId = :messageId")
    int count(long userId, long dialogId, int messageId);

    @Query("SELECT * FROM deleted_messages "
            + "WHERE userId = :userId AND dialogId = :dialogId "
            + "ORDER BY date ASC")
    List<DeletedMessage> getForDialog(long userId, long dialogId);

    @Query("SELECT * FROM deleted_messages WHERE userId = :userId ORDER BY deletedAt DESC")
    List<DeletedMessage> getAll(long userId);

    @Query("DELETE FROM deleted_messages WHERE userId = :userId AND dialogId = :dialogId")
    void clearDialog(long userId, long dialogId);

    @Query("DELETE FROM deleted_messages WHERE userId = :userId")
    void clearAll(long userId);
}
