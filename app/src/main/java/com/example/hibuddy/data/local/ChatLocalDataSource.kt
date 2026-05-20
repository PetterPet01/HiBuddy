package com.example.hibuddy.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class CachedChatMessage(
    val id: String,
    val matchId: String,
    val chatId: String?,
    val senderId: String,
    val senderName: String?,
    val content: String,
    val isRead: Boolean,
    val createdAt: String,
    val clientMessageId: String?,
    val deliveryState: String,
    val localSortTime: Long,
)

class ChatLocalDataSource(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_MESSAGES (
                id TEXT PRIMARY KEY NOT NULL,
                match_id TEXT NOT NULL,
                chat_id TEXT,
                sender_id TEXT NOT NULL,
                sender_name TEXT,
                content TEXT NOT NULL,
                is_read INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                client_message_id TEXT,
                delivery_state TEXT NOT NULL DEFAULT 'SENT',
                local_sort_time INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_messages_match ON $TABLE_MESSAGES(match_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_messages_match_sort ON $TABLE_MESSAGES(match_id, local_sort_time)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_messages_client_id ON $TABLE_MESSAGES(client_message_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    @Synchronized
    fun getMessages(matchId: String, limit: Int = DEFAULT_LIMIT): List<CachedChatMessage> {
        val rows = mutableListOf<CachedChatMessage>()
        val cursor = readableDatabase.rawQuery(
            """
            SELECT * FROM (
                SELECT * FROM $TABLE_MESSAGES
                WHERE match_id = ?
                ORDER BY local_sort_time DESC, created_at DESC
                LIMIT ?
            )
            ORDER BY local_sort_time ASC, created_at ASC
            """.trimIndent(),
            arrayOf(matchId, limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                rows.add(it.toCachedMessage())
            }
        }
        return rows
    }

    @Synchronized
    fun upsertMessages(messages: List<CachedChatMessage>) {
        if (messages.isEmpty()) return
        writableDatabase.runInTransaction {
            messages.forEach { upsertMessageInternal(it) }
        }
    }

    @Synchronized
    fun upsertMessage(message: CachedChatMessage) {
        writableDatabase.runInTransaction {
            upsertMessageInternal(message)
        }
    }

    @Synchronized
    fun deletePendingByClientId(matchId: String, clientMessageId: String) {
        writableDatabase.delete(
            TABLE_MESSAGES,
            "match_id = ? AND client_message_id = ? AND delivery_state IN (?, ?)",
            arrayOf(matchId, clientMessageId, DeliveryStateSending, DeliveryStateFailed)
        )
    }

    @Synchronized
    fun markMessageFailed(messageId: String) {
        val values = ContentValues().apply {
            put("delivery_state", DeliveryStateFailed)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update(TABLE_MESSAGES, values, "id = ?", arrayOf(messageId))
    }

    @Synchronized
    fun markMessageSending(messageId: String, clientMessageId: String) {
        val values = ContentValues().apply {
            put("client_message_id", clientMessageId)
            put("delivery_state", DeliveryStateSending)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update(TABLE_MESSAGES, values, "id = ?", arrayOf(messageId))
    }

    @Synchronized
    fun markOutgoingRead(matchId: String, currentUserId: String) {
        val values = ContentValues().apply {
            put("is_read", 1)
            put("delivery_state", DeliveryStateRead)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update(
            TABLE_MESSAGES,
            values,
            "match_id = ? AND sender_id = ? AND delivery_state != ?",
            arrayOf(matchId, currentUserId, DeliveryStateFailed)
        )
    }

    private fun SQLiteDatabase.runInTransaction(block: SQLiteDatabase.() -> Unit) {
        beginTransaction()
        try {
            block()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private fun SQLiteDatabase.upsertMessageInternal(message: CachedChatMessage) {
        insertWithOnConflict(
            TABLE_MESSAGES,
            null,
            message.toContentValues(),
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun CachedChatMessage.toContentValues(): ContentValues {
        return ContentValues().apply {
            put("id", id)
            put("match_id", matchId)
            put("chat_id", chatId)
            put("sender_id", senderId)
            put("sender_name", senderName)
            put("content", content)
            put("is_read", if (isRead) 1 else 0)
            put("created_at", createdAt)
            put("client_message_id", clientMessageId)
            put("delivery_state", deliveryState)
            put("local_sort_time", localSortTime)
            put("updated_at", System.currentTimeMillis())
        }
    }

    private fun Cursor.toCachedMessage(): CachedChatMessage {
        return CachedChatMessage(
            id = getString(getColumnIndexOrThrow("id")),
            matchId = getString(getColumnIndexOrThrow("match_id")),
            chatId = getNullableString("chat_id"),
            senderId = getString(getColumnIndexOrThrow("sender_id")),
            senderName = getNullableString("sender_name"),
            content = getString(getColumnIndexOrThrow("content")),
            isRead = getInt(getColumnIndexOrThrow("is_read")) == 1,
            createdAt = getString(getColumnIndexOrThrow("created_at")),
            clientMessageId = getNullableString("client_message_id"),
            deliveryState = getString(getColumnIndexOrThrow("delivery_state")),
            localSortTime = getLong(getColumnIndexOrThrow("local_sort_time")),
        )
    }

    private fun Cursor.getNullableString(columnName: String): String? {
        val index = getColumnIndexOrThrow(columnName)
        return if (isNull(index)) null else getString(index)
    }

    companion object {
        private const val DATABASE_NAME = "hibuddy_chat.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_MESSAGES = "chat_messages"
        private const val DEFAULT_LIMIT = 100

        const val DeliveryStateSending = "SENDING"
        const val DeliveryStateSent = "SENT"
        const val DeliveryStateRead = "READ"
        const val DeliveryStateFailed = "FAILED"
    }
}
