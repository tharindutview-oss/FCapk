package com.example.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatMessageDao: ChatMessageDao) {

    val allMessagesFlow: Flow<List<ChatMessage>> = chatMessageDao.getAllMessagesFlow()

    suspend fun getAllMessages(): List<ChatMessage> {
        return chatMessageDao.getAllMessages()
    }

    suspend fun insertMessage(message: ChatMessage): ChatMessage {
        val id = chatMessageDao.insertMessage(message)
        return message.copy(id = id.toInt())
    }

    suspend fun deleteMessage(id: Int) {
        chatMessageDao.deleteMessageById(id)
    }

    suspend fun clearAll() {
        chatMessageDao.clearAllMessages()
    }
}
