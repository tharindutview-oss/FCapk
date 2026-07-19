package com.example.ui

object WebInterface {
    fun getHtml(hostIp: String, port: Int): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Chat Hub - Local Offline Chat</title>
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }

        body {
            background-color: #dadbd3;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            overflow: hidden;
        }

        /* Main Container */
        .app-container {
            width: 100%;
            height: 100%;
            max-width: 1200px;
            max-height: 900px;
            background-color: #f0f2f5;
            display: flex;
            flex-direction: column;
            box-shadow: 0 6px 18px rgba(0,0,0,0.15);
            border-radius: 12px;
            overflow: hidden;
            position: relative;
        }

        @media (max-width: 768px) {
            .app-container {
                border-radius: 0;
                max-height: 100%;
            }
        }

        /* Header */
        .app-header {
            background-color: #00a884;
            color: #ffffff;
            padding: 10px 16px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            height: 60px;
            z-index: 10;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }

        .header-info {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .header-avatar {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background-color: #ffffff;
            color: #00a884;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
            font-size: 18px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }

        .header-text h1 {
            font-size: 16px;
            font-weight: 600;
        }

        .header-text p {
            font-size: 12px;
            color: #e1f7f0;
        }

        .header-status {
            display: flex;
            align-items: center;
            gap: 6px;
            background-color: rgba(255, 255, 255, 0.2);
            padding: 4px 10px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 500;
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: #ff3b30;
        }

        .status-dot.connected {
            background-color: #25d366;
        }

        /* Chat Area */
        .chat-area {
            flex: 1;
            background-color: #efeae2;
            /* WhatsApp custom doodle background */
            background-image: radial-gradient(rgba(0, 0, 0, 0.05) 1px, transparent 1px),
                              radial-gradient(rgba(0, 0, 0, 0.05) 1px, transparent 1px);
            background-size: 24px 24px;
            background-position: 0 0, 12px 12px;
            overflow-y: auto;
            padding: 16px 24px;
            display: flex;
            flex-direction: column;
            gap: 12px;
            scroll-behavior: smooth;
        }

        /* Message Bubbles */
        .message-row {
            display: flex;
            width: 100%;
            margin-bottom: 2px;
        }

        .message-row.sent {
            justify-content: flex-end;
        }

        .message-row.received {
            justify-content: flex-start;
        }

        .message-bubble {
            max-width: 65%;
            padding: 6px 10px 8px 10px;
            border-radius: 8px;
            position: relative;
            box-shadow: 0 1px 1px rgba(0,0,0,0.1);
            font-size: 14.5px;
            line-height: 1.4;
            word-wrap: break-word;
        }

        .message-row.sent .message-bubble {
            background-color: #dcf8c6;
            color: #303030;
            border-top-right-radius: 0;
        }

        .message-row.received .message-bubble {
            background-color: #ffffff;
            color: #303030;
            border-top-left-radius: 0;
        }

        .message-sender {
            font-size: 11.5px;
            font-weight: 600;
            color: #128c7e;
            margin-bottom: 3px;
        }

        .message-row.sent .message-sender {
            color: #075e54;
            text-align: right;
        }

        .message-text {
            white-space: pre-wrap;
            margin-bottom: 4px;
        }

        /* Attachment Styles */
        .attachment-container {
            margin-top: 4px;
            margin-bottom: 6px;
            border-radius: 6px;
            overflow: hidden;
            background-color: rgba(0,0,0,0.03);
            border: 1px solid rgba(0,0,0,0.06);
        }

        .attachment-image {
            max-width: 100%;
            max-height: 250px;
            display: block;
            border-radius: 4px;
            cursor: pointer;
        }

        .attachment-video {
            max-width: 100%;
            max-height: 250px;
            display: block;
            border-radius: 4px;
        }

        .attachment-file-box {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 8px 12px;
            background-color: rgba(255,255,255,0.7);
        }

        .file-icon {
            color: #ff2d55;
            flex-shrink: 0;
        }

        .file-info {
            flex: 1;
            min-width: 0;
        }

        .file-name {
            font-size: 13px;
            font-weight: 500;
            color: #333;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .file-download-btn {
            font-size: 12px;
            font-weight: bold;
            color: #007aff;
            text-decoration: none;
            text-transform: uppercase;
        }

        /* Message Footer (Timestamp & Action Buttons) */
        .message-footer {
            display: flex;
            align-items: center;
            justify-content: flex-end;
            gap: 8px;
            margin-top: 2px;
        }

        .message-time {
            font-size: 10px;
            color: #7f8c8d;
        }

        .bubble-action-btn {
            background: none;
            border: none;
            color: #7f8c8d;
            cursor: pointer;
            padding: 2px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 4px;
            transition: background-color 0.2s, color 0.2s;
        }

        .bubble-action-btn:hover {
            background-color: rgba(0, 0, 0, 0.05);
            color: #333333;
        }

        .bubble-action-btn.delete-btn:hover {
            background-color: rgba(255, 59, 48, 0.1);
            color: #ff3b30;
        }

        /* Input Bar */
        .input-bar {
            background-color: #f0f2f5;
            padding: 8px 16px;
            display: flex;
            align-items: center;
            gap: 12px;
            min-height: 60px;
            z-index: 10;
            box-shadow: 0 -1px 3px rgba(0,0,0,0.05);
        }

        .input-action-btn {
            background: none;
            border: none;
            color: #54656f;
            cursor: pointer;
            padding: 6px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: background-color 0.2s;
        }

        .input-action-btn:hover {
            background-color: rgba(0, 0, 0, 0.05);
        }

        .message-input-container {
            flex: 1;
            position: relative;
        }

        .message-input {
            width: 100%;
            border: none;
            outline: none;
            background-color: #ffffff;
            border-radius: 8px;
            padding: 10px 14px;
            font-size: 14.5px;
            color: #333333;
            resize: none;
            max-height: 120px;
            min-height: 40px;
            line-height: 20px;
            box-shadow: inset 0 1px 1px rgba(0,0,0,0.05);
        }

        .message-input::placeholder {
            color: #8696a0;
        }

        .send-btn-circle {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background-color: #00a884;
            color: #ffffff;
            border: none;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            box-shadow: 0 1px 3px rgba(0,0,0,0.2);
            transition: transform 0.1s, background-color 0.2s;
            flex-shrink: 0;
        }

        .send-btn-circle:hover {
            background-color: #008f72;
        }

        .send-btn-circle:active {
            transform: scale(0.95);
        }

        /* Nickname Popup */
        .overlay {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-color: rgba(0, 0, 0, 0.5);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 100;
        }

        .overlay.hidden {
            display: none;
        }

        .dialog-box {
            background-color: #ffffff;
            padding: 24px;
            border-radius: 12px;
            width: 90%;
            max-width: 400px;
            box-shadow: 0 8px 24px rgba(0,0,0,0.25);
            text-align: center;
        }

        .dialog-box h2 {
            font-size: 18px;
            color: #075e54;
            margin-bottom: 12px;
        }

        .dialog-box p {
            font-size: 14px;
            color: #666;
            margin-bottom: 20px;
        }

        .nickname-input {
            width: 100%;
            padding: 10px 14px;
            border: 2px solid #e1e9e7;
            border-radius: 8px;
            font-size: 14px;
            outline: none;
            margin-bottom: 16px;
            transition: border-color 0.2s;
        }

        .nickname-input:focus {
            border-color: #00a884;
        }

        .dialog-btn {
            width: 100%;
            background-color: #00a884;
            color: #ffffff;
            border: none;
            padding: 12px;
            border-radius: 8px;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.2s;
        }

        .dialog-btn:hover {
            background-color: #008f72;
        }

        /* Fallback Copy Textarea */
        #fallback-copy-area {
            position: absolute;
            left: -9999px;
            top: 0;
        }

        /* Image modal */
        .image-modal {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-color: rgba(0,0,0,0.9);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 200;
            display: none;
        }

        .image-modal img {
            max-width: 95%;
            max-height: 90%;
            object-fit: contain;
            border-radius: 4px;
        }

        .image-modal-close {
            position: absolute;
            top: 20px;
            right: 20px;
            background: none;
            border: none;
            color: #ffffff;
            font-size: 30px;
            cursor: pointer;
        }
    </style>
</head>
<body>

    <div class="app-container">
        <!-- Header -->
        <header class="app-header">
            <div class="header-info">
                <div class="header-avatar">CH</div>
                <div class="header-text">
                    <h1 id="client-display-name">Guest</h1>
                    <p>Connected to Chat Hub Server</p>
                </div>
            </div>
            <div class="header-status">
                <span id="status-text">Disconnected</span>
                <div id="status-indicator" class="status-dot"></div>
            </div>
        </header>

        <!-- Chat History -->
        <main class="chat-area" id="chat-messages-container">
            <!-- Messages dynamic content -->
        </main>

        <!-- Input Bar -->
        <footer class="input-bar">
            <!-- Emoji Smiley Icon (Static decoration as requested) -->
            <button class="input-action-btn" title="Emoji Placeholder" onclick="alert('Emoji panel placeholder! 😊')">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
                    <path d="M12 2c5.52 0 10 4.48 10 10s-4.48 10-10 10S2 17.52 2 12 6.48 2 12 2zm0 18c4.41 0 8-3.59 8-8s-3.59-8-8-8-8 3.59-8 8 3.59 8 8 8zm0-11c1.38 0 2.5 1.12 2.5 2.5s-1.12 2.5-2.5 2.5-2.5-1.12-2.5-2.5 1.12-2.5 2.5-2.5zm0 1c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5zm0 3c-1.82 0-3.41 1.05-4.2 2.6l1.34.68C9.7 15.05 10.77 14.5 12 14.5s2.3.55 2.86 1.78l1.34-.68C15.41 14.05 13.82 13 12 13z"/>
                </svg>
            </button>

            <!-- Working File Attachment Button (📎) -->
            <button class="input-action-btn" title="Attach File" onclick="document.getElementById('file-upload-input').click()">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
                    <path d="M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5c0-3.31 2.69-6 6-6s6 2.69 6 6v12.5c0 4.41-3.59 8-8 8s-8-3.59-8-8V6h2v11.5c0 3.31 2.69 6 6 6s6-2.69 6-6V5c0-2.21-1.79-4-4-4s-4 1.79-4 4v12.5c0 1.1.9 2 2 2s2-.9 2-2V6h2z"/>
                </svg>
            </button>
            <input type="file" id="file-upload-input" style="display: none;" onchange="handleFileUpload(this)">

            <!-- Message Box -->
            <div class="message-input-container">
                <textarea 
                    class="message-input" 
                    id="message-text-box" 
                    placeholder="Type a message" 
                    rows="1" 
                    oninput="adjustTextareaHeight(this)"
                    onkeydown="handleKeyDown(event)"></textarea>
            </div>

            <!-- Send button -->
            <button class="send-btn-circle" id="send-button-element" onclick="sendTextMessage()" title="Send">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                    <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
                </svg>
            </button>
        </footer>

        <!-- Nickname Prompt Overlay -->
        <div class="overlay" id="nickname-overlay">
            <div class="dialog-box">
                <h2>Welcome to Chat Hub</h2>
                <p>Choose a nickname before joining the local discussion:</p>
                <input type="text" class="nickname-input" id="nickname-field" placeholder="E.g., User 43.15" maxlength="20" onkeydown="if(event.key === 'Enter') saveNickname()">
                <button class="dialog-btn" onclick="saveNickname()">Start Chatting</button>
            </div>
        </div>
    </div>

    <!-- Fallback clipboard copy mechanism -->
    <textarea id="fallback-copy-area" readonly></textarea>

    <!-- Large Image modal viewer -->
    <div class="image-modal" id="image-viewer-modal" onclick="closeImageViewer()">
        <button class="image-modal-close" onclick="closeImageViewer()">&times;</button>
        <img id="image-viewer-content" src="" alt="View Large Attachment">
    </div>

    <script>
        let ws;
        let nickname = "";
        const hostIp = "${hostIp}";
        const port = ${port};
        const wsUrl = "ws://" + window.location.host + "/chat-ws";

        // Prompt nickname setup
        window.onload = function() {
            // Pre-fill nickname suggestions based on last octet of current connection
            const randomSuffix = Math.floor(Math.random() * 254) + 1;
            document.getElementById("nickname-field").value = "User " + randomSuffix;
            document.getElementById("nickname-field").focus();
        };

        function saveNickname() {
            const entered = document.getElementById("nickname-field").value.trim();
            if (!entered) return;
            nickname = entered;
            document.getElementById("client-display-name").textContent = nickname;
            document.getElementById("nickname-overlay").classList.add("hidden");
            connectWebSocket();
        }

        // Establish WS connection
        function connectWebSocket() {
            const statusIndicator = document.getElementById("status-indicator");
            const statusText = document.getElementById("status-text");

            statusText.textContent = "Connecting...";
            statusIndicator.className = "status-dot";

            ws = new WebSocket(wsUrl);

            ws.onopen = function() {
                statusText.textContent = "Connected";
                statusIndicator.className = "status-dot connected";
                console.log("WebSocket connected.");
            };

            ws.onmessage = function(event) {
                try {
                    const data = JSON.parse(event.data);
                    if (Array.isArray(data)) {
                        // Received initial full messages list
                        const container = document.getElementById("chat-messages-container");
                        container.innerHTML = "";
                        data.forEach(msg => appendMessageElement(msg));
                        scrollToBottom();
                    } else if (data.type === "CHAT") {
                        appendMessageElement(data);
                        scrollToBottom();
                    } else if (data.type === "DELETE") {
                        removeMessageElement(data.id);
                    }
                } catch (e) {
                    console.error("Error parsing message payload:", e);
                }
            };

            ws.onclose = function() {
                statusText.textContent = "Disconnected";
                statusIndicator.className = "status-dot";
                // Auto-reconnect in 3 seconds
                setTimeout(connectWebSocket, 3000);
            };

            ws.onerror = function(err) {
                console.error("WebSocket error:", err);
            };
        }

        // Key press bindings: Enter for new line, Shift + Enter to Send
        function handleKeyDown(event) {
            if (event.key === "Enter" && event.shiftKey) {
                event.preventDefault();
                sendTextMessage();
            }
        }

        // Send a text message
        function sendTextMessage() {
            const input = document.getElementById("message-text-box");
            const text = input.value.trim();
            if (!text) return;

            if (ws && ws.readyState === WebSocket.OPEN) {
                const payload = {
                    type: "CHAT",
                    sender: nickname,
                    text: text,
                    timestamp: Date.now()
                };
                ws.send(JSON.stringify(payload));
                input.value = "";
                adjustTextareaHeight(input);
            } else {
                alert("WebSocket is not connected. Reconnecting...");
            }
        }

        // Handle File upload attachment
        function handleFileUpload(inputElement) {
            const file = inputElement.files[0];
            if (!file) return;

            // Form data preparation
            const formData = new FormData();
            formData.append("file", file);
            formData.append("sender", nickname);
            formData.append("timestamp", Date.now().toString());

            // Determine file general type
            let fileType = "file";
            if (file.type.startsWith("image/")) {
                fileType = "image";
            } else if (file.type.startsWith("video/")) {
                fileType = "video";
            } else if (file.type === "application/pdf") {
                fileType = "pdf";
            }
            formData.append("fileType", fileType);

            // Temporarily update input UI feedback
            const textInput = document.getElementById("message-text-box");
            const oldPlaceholder = textInput.placeholder;
            textInput.placeholder = "Uploading " + file.name + "...";
            textInput.disabled = true;

            fetch("/upload", {
                method: "POST",
                body: formData
            })
            .then(response => {
                if (!response.ok) throw new Error("File upload failed.");
                return response.json();
            })
            .then(data => {
                // Upload successful, clear input state
                inputElement.value = "";
                textInput.placeholder = oldPlaceholder;
                textInput.disabled = false;
                textInput.focus();
            })
            .catch(error => {
                console.error("Upload error:", error);
                alert("Failed to upload attachment: " + error.message);
                textInput.placeholder = oldPlaceholder;
                textInput.disabled = false;
            });
        }

        // Append message UI component
        function appendMessageElement(msg) {
            const container = document.getElementById("chat-messages-container");
            
            // Check if element already exists to prevent duplicate renderings
            if (document.getElementById("msg-row-" + msg.id)) return;

            const isMyMessage = msg.sender === nickname;
            
            const row = document.createElement("div");
            row.id = "msg-row-" + msg.id;
            row.className = "message-row " + (isMyMessage ? "sent" : "received");

            const formattedTime = formatTimestamp(msg.timestamp);

            let contentHtml = "";
            contentHtml += '<div class="message-bubble">';
            
            // Sender badge
            contentHtml += '<div class="message-sender">' + escapeHtml(isMyMessage ? "You" : msg.sender) + '</div>';

            // Attachment container if exists
            if (msg.fileUrl) {
                contentHtml += '<div class="attachment-container">';
                if (msg.fileType === "image") {
                    contentHtml += '<img class="attachment-image" src="' + msg.fileUrl + '" onclick="viewLargeImage(\'' + msg.fileUrl + '\')" alt="Photo Attachment">';
                } else if (msg.fileType === "video") {
                    contentHtml += '<video class="attachment-video" controls src="' + msg.fileUrl + '"></video>';
                } else {
                    // PDF or standard document
                    contentHtml += '<div class="attachment-file-box">';
                    contentHtml += '<div class="file-icon">';
                    contentHtml += '<svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor">';
                    contentHtml += '<path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>';
                    contentHtml += '</svg>';
                    contentHtml += '</div>';
                    contentHtml += '<div class="file-info">';
                    contentHtml += '<div class="file-name" title="' + escapeHtml(msg.fileName) + '">' + escapeHtml(msg.fileName) + '</div>';
                    contentHtml += '</div>';
                    contentHtml += '<a class="file-download-btn" href="' + msg.fileUrl + '" target="_blank">Open</a>';
                    contentHtml += '</div>';
                }
                contentHtml += '</div>';
            }

            // Message text
            if (msg.text && msg.text.trim().length > 0) {
                contentHtml += '<div class="message-text">' + linkify(escapeHtml(msg.text)) + '</div>';
            }

            // Footer with copy and delete buttons
            contentHtml += '<div class="message-footer">';
            contentHtml += '<span class="message-time">' + formattedTime + '</span>';
            
            // Clipboard copy button (works offline over clean fallback)
            contentHtml += '<button class="bubble-action-btn" title="Copy Text" onclick="copyToClipboard(' + msg.id + ')">';
            contentHtml += '<svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">';
            contentHtml += '<path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/>';
            contentHtml += '</svg>';
            contentHtml += '</button>';

            // Delete button (Everyone can trigger delete for simplicity/hotspot utility)
            if (isMyMessage) {
                contentHtml += '<button class="bubble-action-btn delete-btn" title="Delete Message" onclick="deleteMessage(' + msg.id + ')">';
                contentHtml += '<svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">';
                contentHtml += '<path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>';
                contentHtml += '</svg>';
                contentHtml += '</button>';
            }

            contentHtml += '</div>'; // message-footer
            contentHtml += '</div>'; // message-bubble

            row.innerHTML = contentHtml;
            container.appendChild(row);
        }

        // Delete message
        function deleteMessage(msgId) {
            if (confirm("Are you sure you want to delete this message? This removes it for everyone on the server.")) {
                if (ws && ws.readyState === WebSocket.OPEN) {
                    const payload = {
                        type: "DELETE",
                        id: parseInt(msgId)
                    };
                    ws.send(JSON.stringify(payload));
                }
            }
        }

        // Remove message element from page
        function removeMessageElement(msgId) {
            const row = document.getElementById("msg-row-" + msgId);
            if (row) {
                row.style.opacity = "0";
                row.style.transform = "scale(0.9)";
                row.style.transition = "all 0.3s ease";
                setTimeout(() => {
                    row.remove();
                }, 300);
            }
        }

        // Utility: Auto-scroll
        function scrollToBottom() {
            const container = document.getElementById("chat-messages-container");
            container.scrollTop = container.scrollHeight;
        }

        // Utility: escape HTML helper
        function escapeHtml(text) {
            if (!text) return "";
            return text
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#039;");
        }

        // Helper: Convert URLs in text into clickable anchors
        function linkify(inputText) {
            var replacedText, replacePattern1, replacePattern2, replacePattern3;

            // URLs starting with http://, https://, or ftp://
            replacePattern1 = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gim;
            replacedText = inputText.replace(replacePattern1, '<a href="${'$'}1" target="_blank" style="color: #007aff; text-decoration: underline;">${'$'}1</a>');

            // URLs starting with "www." (without // before it, or it'd re-link)
            replacePattern2 = /(^|[^\/])(www\.[\S]+(\b|$))/gim;
            replacedText = replacedText.replace(replacePattern2, '${'$'}1<a href="http://${'$'}2" target="_blank" style="color: #007aff; text-decoration: underline;">${'$'}2</a>');

            return replacedText;
        }

        // Utility: Format date/time
        function formatTimestamp(timestamp) {
            const date = new Date(timestamp);
            let hours = date.getHours();
            let minutes = date.getMinutes();
            const ampm = hours >= 12 ? 'PM' : 'AM';
            hours = hours % 12;
            hours = hours ? hours : 12; // 12 instead of 0
            minutes = minutes < 10 ? '0'+minutes : minutes;
            return hours + ':' + minutes + ' ' + ampm;
        }

        // Textarea auto-sizing
        function adjustTextareaHeight(el) {
            el.style.height = "auto";
            el.style.height = el.scrollHeight + "px";
        }

        // Fallback robust Copy to Clipboard mechanism using hidden textarea
        function copyToClipboard(msgId) {
            const row = document.getElementById("msg-row-" + msgId);
            if (!row) return;
            const textElement = row.querySelector(".message-text");
            const copyText = textElement ? textElement.innerText : "";
            
            const textarea = document.getElementById("fallback-copy-area");
            textarea.value = copyText;
            textarea.select();
            textarea.setSelectionRange(0, 99999); // Mobile compatibility

            try {
                document.execCommand("copy");
                alert("Message text copied to clipboard! 📋");
            } catch (err) {
                console.error("Failed to copy text", err);
                alert("Could not copy text.");
            }
        }

        // Large Image Viewer Controls
        function viewLargeImage(src) {
            const modal = document.getElementById("image-viewer-modal");
            const content = document.getElementById("image-viewer-content");
            content.src = src;
            modal.style.display = "flex";
        }

        function closeImageViewer() {
            const modal = document.getElementById("image-viewer-modal");
            modal.style.display = "none";
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
