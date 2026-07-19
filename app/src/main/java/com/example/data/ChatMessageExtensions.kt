package com.example.data

fun ChatMessage.toJsonString(): String {
    val escapedText = text.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
    val escapedSender = sender.replace("\\", "\\\\")
        .replace("\"", "\\\"")
    val escapedFileName = fileName?.replace("\\", "\\\\")?.replace("\"", "\\\"") ?: ""
    val escapedFileUrl = fileUrl?.replace("\\", "\\\\")?.replace("\"", "\\\"") ?: ""
    val escapedFileType = fileType ?: ""
    
    val fileNamePart = if (fileName != null) "\"$escapedFileName\"" else "null"
    val fileUrlPart = if (fileUrl != null) "\"$escapedFileUrl\"" else "null"
    val fileTypePart = if (fileType != null) "\"$escapedFileType\"" else "null"

    return """{"type":"CHAT","id":$id,"sender":"$escapedSender","text":"$escapedText","timestamp":$timestamp,"fileName":$fileNamePart,"fileUrl":$fileUrlPart,"fileType":$fileTypePart}"""
}

fun List<ChatMessage>.toJsonString(): String {
    return joinToString(separator = ",", prefix = "[", postfix = "]") { it.toJsonString() }
}
