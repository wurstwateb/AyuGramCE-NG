package xyz.nextalone.nagram.ayu;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A single message that was deleted (or edited away) and kept locally.
 *
 * The message body is stored as the raw serialized TLRPC.Message blob, exactly
 * as Telegram itself serializes it, so it can be restored without depending on
 * any particular TL layer field layout.
 */
@Entity(
        tableName = "deleted_messages",
        indices = {@Index(value = {"userId", "dialogId", "messageId"}, unique = true)}
)
public class DeletedMessage {

    @PrimaryKey(autoGenerate = true)
    public long fakeId;

    /** Owner account id, so multiple logged-in accounts don't mix. */
    @ColumnInfo(name = "userId")
    public long userId;

    @ColumnInfo(name = "dialogId")
    public long dialogId;

    @ColumnInfo(name = "topicId")
    public long topicId;

    @ColumnInfo(name = "messageId")
    public int messageId;

    /** Unix time when we noticed the deletion. */
    @ColumnInfo(name = "deletedAt")
    public int deletedAt;

    /** Original message date, kept for display ordering. */
    @ColumnInfo(name = "date")
    public int date;

    /** Plain text, duplicated for cheap searching/preview. */
    @ColumnInfo(name = "text")
    public String text;

    /** Serialized TLRPC.Message; the authoritative copy. */
    @ColumnInfo(name = "data", typeAffinity = ColumnInfo.BLOB)
    public byte[] data;
}
