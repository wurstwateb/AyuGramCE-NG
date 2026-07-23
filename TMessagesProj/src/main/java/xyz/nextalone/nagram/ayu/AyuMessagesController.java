package xyz.nextalone.nagram.ayu;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Keeps a local copy of messages that were deleted, so they stay readable
 * after the server drops them.
 *
 * Saving happens off the main thread; reading is done by the UI on demand.
 */
public class AyuMessagesController {

    private static final AyuMessagesController[] instances = new AyuMessagesController[UserConfig.MAX_ACCOUNT_COUNT];

    private final int currentAccount;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private AyuMessagesController(int account) {
        this.currentAccount = account;
    }

    public static AyuMessagesController getInstance(int account) {
        AyuMessagesController local = instances[account];
        if (local == null) {
            synchronized (AyuMessagesController.class) {
                local = instances[account];
                if (local == null) {
                    local = new AyuMessagesController(account);
                    instances[account] = local;
                }
            }
        }
        return local;
    }

    private DeletedMessageDao dao() {
        return AyuDatabase.getInstance(ApplicationLoader.applicationContext).deletedMessageDao();
    }

    /**
     * Called right before Telegram removes a message locally.
     * Must be cheap on the calling thread: the actual write is queued.
     */
    public void onMessageDeleted(TLRPC.Message message, long dialogId, long topicId) {
        if (message == null || dialogId == 0) {
            return;
        }
        // Outgoing-but-not-yet-sent and service messages carry nothing worth keeping.
        if (message.id == 0) {
            return;
        }
        final TLRPC.Message copy = message;
        final long finalDialogId = dialogId;
        final long finalTopicId = topicId;
        executor.execute(() -> save(copy, finalDialogId, finalTopicId));
    }

    private void save(TLRPC.Message message, long dialogId, long topicId) {
        try {
            long userId = UserConfig.getInstance(currentAccount).getClientUserId();
            DeletedMessageDao dao = dao();
            if (dao.count(userId, dialogId, message.id) > 0) {
                return; // already stored
            }

            byte[] blob;
            NativeByteBuffer buffer = new NativeByteBuffer(message.getObjectSize());
            try {
                message.serializeToStream(buffer);
                buffer.rewind();
                blob = new byte[buffer.remaining()];
                buffer.readBytes(blob, false);
            } finally {
                buffer.reuse();
            }

            DeletedMessage entity = new DeletedMessage();
            entity.userId = userId;
            entity.dialogId = dialogId;
            entity.topicId = topicId;
            entity.messageId = message.id;
            entity.date = message.date;
            entity.deletedAt = (int) (System.currentTimeMillis() / 1000);
            entity.text = message.message;
            entity.data = blob;
            dao.insert(entity);
        } catch (Exception e) {
            FileLog.e("AyuMessagesController: failed to save deleted message", e);
        }
    }

    /** Restores stored messages for a dialog, oldest first. */
    public List<DeletedMessage> getDeletedMessages(long dialogId) {
        long userId = UserConfig.getInstance(currentAccount).getClientUserId();
        return dao().getForDialog(userId, dialogId);
    }

    /** Deserializes a stored row back into a usable message. */
    public static TLRPC.Message toMessage(DeletedMessage stored) {
        if (stored == null || stored.data == null) {
            return null;
        }
        NativeByteBuffer buffer = null;
        try {
            buffer = new NativeByteBuffer(stored.data.length);
            buffer.writeBytes(stored.data);
            buffer.rewind();
            return TLRPC.Message.TLdeserialize(
                    buffer, buffer.readInt32(false), false);
        } catch (Exception e) {
            FileLog.e("AyuMessagesController: failed to restore message", e);
            return null;
        } finally {
            if (buffer != null) {
                buffer.reuse();
            }
        }
    }

    public void clearDialog(long dialogId) {
        long userId = UserConfig.getInstance(currentAccount).getClientUserId();
        executor.execute(() -> dao().clearDialog(userId, dialogId));
    }

    public void clearAll() {
        long userId = UserConfig.getInstance(currentAccount).getClientUserId();
        executor.execute(() -> dao().clearAll(userId));
    }
}
