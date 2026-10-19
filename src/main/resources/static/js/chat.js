/**
 * Grand Voyager Concierge - Enhanced Classic Chat Client
 * Features: Multi-line TextArea, Dark/Light Mode, Speech Synthesis, IME-Safe Submission
 */
(function () {
    'use strict';

    // DOM Elements
    const chatForm = document.getElementById('chat-form');
    const chatInput = document.getElementById('chat-input');
    const sendButton = document.getElementById('send-button');
    const messagesContainer = document.getElementById('messages-container');
    const messagesList = document.getElementById('messages-list');
    const typingIndicator = document.getElementById('typing-indicator');
    const typingStatusText = document.getElementById('typing-status-text');
    const clearChatBtn = document.getElementById('clear-chat-btn');
    const sidebarToggleBtn = document.getElementById('sidebar-toggle-btn');
    const sidebar = document.getElementById('sidebar');
    const themeToggleBtn = document.getElementById('theme-toggle-btn');
    const themeLabel = document.getElementById('theme-label');
    const charCounter = document.getElementById('char-counter');
    const clearInputBtn = document.getElementById('clear-input-btn');
    const expandTextareaBtn = document.getElementById('expand-textarea-btn');
    const scrollBottomBtn = document.getElementById('scroll-bottom-btn');

    let isSubmitting = false;
    let currentlySpeakingBtn = null;
    const conversationStorageKey = 'grand-voyager-conversation-id';
    let conversationId = getOrCreateConversationId();

    function getOrCreateConversationId() {
        try {
            const existingId = sessionStorage.getItem(conversationStorageKey);
            if (existingId) return existingId;

            const newId = window.crypto?.randomUUID
                ? window.crypto.randomUUID()
                : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
            sessionStorage.setItem(conversationStorageKey, newId);
            return newId;
        } catch (error) {
            return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
        }
    }

    // Initialize application
    function init() {
        initTheme();
        setupEventListeners();
        updateCharCounter();
        autoResizeTextarea();
        chatInput.focus();
    }

    /**
     * Dark / Light Mode Feature
     */
    function initTheme() {
        const storedTheme = localStorage.getItem('color-scheme') || 'dark';
        applyTheme(storedTheme);

        if (themeToggleBtn) {
            themeToggleBtn.addEventListener('click', toggleTheme);
        }

        // Listen for system theme changes if no override is set
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
            if (!localStorage.getItem('color-scheme')) {
                applyTheme(e.matches ? 'dark' : 'light');
            }
        });
    }

    function toggleTheme() {
        const currentTheme = document.documentElement.getAttribute('data-theme') || 'dark';
        const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
        applyTheme(nextTheme);
        try {
            localStorage.setItem('color-scheme', nextTheme);
        } catch (e) {
            console.warn('LocalStorage unavailable for theme storage.');
        }
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        const metaTheme = document.querySelector('meta[name="color-scheme"]');
        if (metaTheme) {
            metaTheme.content = theme;
        }

        if (themeLabel) {
            themeLabel.textContent = theme === 'dark' ? 'Dark' : 'Light';
        }
    }

    /**
     * Setup Event Listeners
     */
    function setupEventListeners() {
        // Form submission
        chatForm.addEventListener('submit', handleFormSubmit);

        // IME-safe Enter and shortcuts
        chatInput.addEventListener('keydown', handleKeydown);

        // Textarea input event for resizing and tool state
        chatInput.addEventListener('input', () => {
            autoResizeTextarea();
            updateCharCounter();
            updateSendButtonState();
        });

        // Clear textarea button
        if (clearInputBtn) {
            clearInputBtn.addEventListener('click', () => {
                chatInput.value = '';
                autoResizeTextarea();
                updateCharCounter();
                updateSendButtonState();
                chatInput.focus();
            });
        }

        // Expand / minimize textarea editor
        if (expandTextareaBtn) {
            expandTextareaBtn.addEventListener('click', () => {
                chatForm.classList.toggle('expanded');
                expandTextareaBtn.title = chatForm.classList.contains('expanded') ? 'Shrink editor' : 'Expand editor';
                autoResizeTextarea();
                chatInput.focus();
            });
        }

        // Quick prompt chips click delegation
        document.addEventListener('click', (event) => {
            const promptTarget = event.target.closest('[data-prompt]');
            if (promptTarget) {
                const promptText = promptTarget.getAttribute('data-prompt');
                if (promptText) {
                    sendDirectPrompt(promptText);
                }
            }
        });

        // Clear chat history button
        if (clearChatBtn) {
            clearChatBtn.addEventListener('click', handleClearChat);
        }

        // Scroll to bottom button
        if (scrollBottomBtn) {
            scrollBottomBtn.addEventListener('click', () => {
                scrollToBottom(true);
            });

            messagesContainer.addEventListener('scroll', () => {
                const distanceToBottom = messagesContainer.scrollHeight - messagesContainer.scrollTop - messagesContainer.clientHeight;
                if (distanceToBottom > 140) {
                    scrollBottomBtn.classList.add('visible');
                } else {
                    scrollBottomBtn.classList.remove('visible');
                }
            });
        }

        // Mobile sidebar toggle
        if (sidebarToggleBtn && sidebar) {
            sidebarToggleBtn.addEventListener('click', () => {
                sidebar.classList.toggle('open');
            });

            document.addEventListener('click', (e) => {
                if (sidebar.classList.contains('open') &&
                    !sidebar.contains(e.target) &&
                    !sidebarToggleBtn.contains(e.target)) {
                    sidebar.classList.remove('open');
                }
            });
        }
    }

    /**
     * IME-safe Enter & Cmd/Ctrl+Enter submission
     */
    function handleKeydown(event) {
        const isEnter = event.key === 'Enter';
        const isCmdOrCtrl = event.metaKey || event.ctrlKey;

        // Cmd + Enter or Ctrl + Enter always submits
        if (isEnter && isCmdOrCtrl) {
            event.preventDefault();
            if (!isSubmitting && chatInput.value.trim().length > 0) {
                chatForm.requestSubmit();
            }
            return;
        }

        // Enter without Shift submits (safeguarded against IME composition)
        if (isEnter && !event.shiftKey) {
            event.preventDefault();

            // Block submission during active IME composition (e.g. Japanese, Chinese, Korean)
            if (event.isComposing || event.keyCode === 229) {
                return;
            }

            if (!isSubmitting && chatInput.value.trim().length > 0) {
                chatForm.requestSubmit();
            }
        }
    }

    /**
     * Form submission handler
     */
    async function handleFormSubmit(event) {
        event.preventDefault();

        const messageText = chatInput.value.trim();
        if (!messageText || isSubmitting) {
            return;
        }

        // Reset input
        chatInput.value = '';
        autoResizeTextarea();
        updateCharCounter();
        updateSendButtonState();

        if (sidebar && sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
        }

        await sendMessage(messageText);
    }

    /**
     * Quick direct prompt dispatch
     */
    async function sendDirectPrompt(promptText) {
        if (isSubmitting) return;

        chatInput.value = '';
        autoResizeTextarea();
        updateCharCounter();
        updateSendButtonState();

        if (sidebar && sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
        }

        await sendMessage(promptText);
    }

    /**
     * Send user inquiry to /chat API
     */
    async function sendMessage(messageText) {
        // 1. Append User Message
        appendMessage('user', messageText);

        // 2. Set UI Loading
        setLoadingState(true);

        // Dynamic status phrases
        updateTypingStatusText(messageText);

        try {
            // 3. POST request to /chat API
            const response = await fetch('/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'text/plain; charset=UTF-8',
                    'Conversation-Id': conversationId
                },
                body: messageText
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || `Server returned status ${response.status}`);
            }

            const aiReply = await response.text();

            // 4. Append Assistant Response
            appendMessage('assistant', aiReply);

        } catch (error) {
            console.error('Chat API Error:', error);
            appendErrorMessage(error.message, messageText);
        } finally {
            setLoadingState(false);
            chatInput.focus();
        }
    }

    /**
     * Update typing status phrase based on topic
     */
    function updateTypingStatusText(prompt) {
        if (!typingStatusText) return;
        const lower = prompt.toLowerCase();
        if (lower.includes('flight')) {
            typingStatusText.textContent = 'Scanning airline manifests & flight schedules...';
        } else if (lower.includes('hotel') || lower.includes('stay')) {
            typingStatusText.textContent = 'Searching boutique accommodations & luxury suites...';
        } else if (lower.includes('weather') || lower.includes('temperature')) {
            typingStatusText.textContent = 'Retrieving real-time meteorology & city forecasts...';
        } else if (lower.includes('order') || lower.includes('booking')) {
            typingStatusText.textContent = 'Looking up reservation status in booking ledger...';
        } else {
            typingStatusText.textContent = 'Consulting Grand Voyager concierge knowledge...';
        }
    }

    /**
     * UI Loading State
     */
    function setLoadingState(loading) {
        isSubmitting = loading;
        sendButton.disabled = loading || chatInput.value.trim().length === 0;
        chatInput.disabled = loading;

        if (loading) {
            typingIndicator.classList.add('active');
            scrollToBottom();
        } else {
            typingIndicator.classList.remove('active');
        }
    }

    /**
     * Update char counter and tool buttons
     */
    function updateCharCounter() {
        const count = chatInput.value.length;
        if (charCounter) {
            charCounter.textContent = `${count} / 2000`;
            charCounter.style.color = count > 1800 ? 'var(--color-accent-gold)' : '';
        }
        if (clearInputBtn) {
            clearInputBtn.style.display = count > 0 ? 'inline-block' : 'none';
        }
    }

    /**
     * Update send button disabled state
     */
    function updateSendButtonState() {
        const hasText = chatInput.value.trim().length > 0;
        sendButton.disabled = !hasText || isSubmitting;
    }

    /**
     * Auto-resize multi-line textarea smoothly
     */
    function autoResizeTextarea() {
        chatInput.style.height = 'auto';
        const isExpanded = chatForm.classList.contains('expanded');
        const maxHeight = isExpanded ? 300 : 180;
        const minHeight = isExpanded ? 140 : 48;
        const targetHeight = Math.max(minHeight, Math.min(chatInput.scrollHeight, maxHeight));
        chatInput.style.height = `${targetHeight}px`;
    }

    /**
     * Append a message row to conversation
     */
    function appendMessage(sender, rawContent) {
        const row = document.createElement('div');
        row.className = `message-row ${sender}`;

        const avatar = document.createElement('div');
        avatar.className = 'message-avatar';
        avatar.textContent = sender === 'assistant' ? 'GV' : 'You';

        const bubbleWrapper = document.createElement('div');
        bubbleWrapper.className = 'message-bubble-wrapper';

        const bubble = document.createElement('div');
        bubble.className = 'message-bubble';

        if (sender === 'assistant') {
            bubble.innerHTML = renderMarkdown(rawContent);
        } else {
            bubble.textContent = rawContent;
        }

        const meta = document.createElement('div');
        meta.className = 'message-meta';

        const timeSpan = document.createElement('span');
        timeSpan.textContent = formatTime(new Date());
        meta.appendChild(timeSpan);

        if (sender === 'assistant') {
            // Copy Response Action
            const copyBtn = document.createElement('button');
            copyBtn.type = 'button';
            copyBtn.className = 'msg-action-btn';
            copyBtn.innerHTML = '📋 Copy';
            copyBtn.title = 'Copy response';
            copyBtn.addEventListener('click', () => {
                copyToClipboard(rawContent, copyBtn);
            });
            meta.appendChild(copyBtn);

            // Read Aloud / Speak Action (Web Speech API)
            if ('speechSynthesis' in window) {
                const speakBtn = document.createElement('button');
                speakBtn.type = 'button';
                speakBtn.className = 'msg-action-btn';
                speakBtn.innerHTML = '🔊 Speak';
                speakBtn.title = 'Listen to response';
                speakBtn.addEventListener('click', () => {
                    handleSpeech(rawContent, speakBtn);
                });
                meta.appendChild(speakBtn);
            }
        }

        bubbleWrapper.appendChild(bubble);
        bubbleWrapper.appendChild(meta);

        row.appendChild(avatar);
        row.appendChild(bubbleWrapper);

        messagesList.appendChild(row);
        scrollToBottom();
    }

    /**
     * Web Speech API: Read aloud assistant message
     */
    function handleSpeech(text, button) {
        if (!('speechSynthesis' in window)) return;

        if (window.speechSynthesis.speaking) {
            window.speechSynthesis.cancel();
            if (currentlySpeakingBtn) {
                currentlySpeakingBtn.innerHTML = '🔊 Speak';
            }
            if (currentlySpeakingBtn === button) {
                currentlySpeakingBtn = null;
                return;
            }
        }

        const cleanText = text.replace(/[*_#`]/g, '');
        const utterance = new SpeechSynthesisUtterance(cleanText);
        utterance.rate = 1.0;
        utterance.pitch = 1.0;

        utterance.onstart = () => {
            currentlySpeakingBtn = button;
            button.innerHTML = '⏹ Stop';
        };

        utterance.onend = utterance.onerror = () => {
            button.innerHTML = '🔊 Speak';
            currentlySpeakingBtn = null;
        };

        window.speechSynthesis.speak(utterance);
    }

    /**
     * Append Error banner with Retry
     */
    function appendErrorMessage(errorMsg, failedPrompt) {
        const row = document.createElement('div');
        row.className = 'message-row assistant';

        const avatar = document.createElement('div');
        avatar.className = 'message-avatar';
        avatar.textContent = 'GV';

        const bubbleWrapper = document.createElement('div');
        bubbleWrapper.className = 'message-bubble-wrapper';

        const bubble = document.createElement('div');
        bubble.className = 'message-bubble';
        bubble.style.borderLeftColor = 'var(--color-error)';
        bubble.style.backgroundColor = 'rgba(239, 68, 68, 0.08)';

        bubble.innerHTML = `
            <p style="color: var(--color-error); font-weight: 600; margin-bottom: 6px;">
                ⚠️ Connection Notice
            </p>
            <p style="color: var(--color-text-muted); font-size: 0.88rem; margin-bottom: 10px;">
                Our concierge service encountered a temporary interruption: <em>${escapeHtml(errorMsg || 'Unable to connect to server.')}</em>
            </p>
        `;

        const retryBtn = document.createElement('button');
        retryBtn.type = 'button';
        retryBtn.className = 'classic-btn-outline';
        retryBtn.style.padding = '4px 10px';
        retryBtn.style.fontSize = '0.78rem';
        retryBtn.textContent = '🔄 Retry Inquiry';
        retryBtn.addEventListener('click', () => {
            row.remove();
            sendMessage(failedPrompt);
        });
        bubble.appendChild(retryBtn);

        bubbleWrapper.appendChild(bubble);
        row.appendChild(avatar);
        row.appendChild(bubbleWrapper);

        messagesList.appendChild(row);
        scrollToBottom();
    }

    /**
     * Copy to clipboard helper
     */
    function copyToClipboard(text, button) {
        navigator.clipboard.writeText(text).then(() => {
            const original = button.innerHTML;
            button.innerHTML = '✓ Copied';
            button.style.color = 'var(--color-accent-gold)';
            setTimeout(() => {
                button.innerHTML = original;
                button.style.color = '';
            }, 2000);
        }).catch(err => {
            console.error('Clipboard copy failed:', err);
        });
    }

    /**
     * Clear chat history
     */
    function handleClearChat() {
        if (!confirm('Clear all conversation history?')) {
            return;
        }

        if (window.speechSynthesis && window.speechSynthesis.speaking) {
            window.speechSynthesis.cancel();
        }

        const messageRows = messagesList.querySelectorAll('.message-row');
        messageRows.forEach(row => row.remove());
        conversationId = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
        try {
            sessionStorage.setItem(conversationStorageKey, conversationId);
        } catch (error) {
            // The new ID remains valid for this page even if session storage is unavailable.
        }
        chatInput.value = '';
        autoResizeTextarea();
        updateCharCounter();
        updateSendButtonState();
        chatInput.focus();
    }

    /**
     * Smooth scroll to bottom of chat
     */
    function scrollToBottom(smooth = false) {
        setTimeout(() => {
            messagesContainer.scrollTo({
                top: messagesContainer.scrollHeight,
                behavior: smooth ? 'smooth' : 'auto'
            });
        }, 50);
    }

    /**
     * Format time HH:MM
     */
    function formatTime(date) {
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    /**
     * Escape HTML
     */
    function escapeHtml(text) {
        if (!text) return '';
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    /**
     * Lightweight Markdown parser
     */
    function renderMarkdown(rawText) {
        if (!rawText) return '';

        let text = escapeHtml(rawText);

        // Code blocks: ```code```
        text = text.replace(/```([\s\S]*?)```/g, (match, code) => {
            return `<pre><code>${code.trim()}</code></pre>`;
        });

        // Inline code: `code`
        text = text.replace(/`([^`]+)`/g, '<code>$1</code>');

        // Bold: **text** or __text__
        text = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
        text = text.replace(/__([^_]+)__/g, '<strong>$1</strong>');

        // Italic: *text* or _text_
        text = text.replace(/\*([^*]+)\*/g, '<em>$1</em>');
        text = text.replace(/_([^_]+)_/g, '<em>$1</em>');

        // Headings: ### Title, ## Title
        text = text.replace(/^### (.*$)/gim, '<h4 style="font-size: 1rem; font-weight: 600; margin: 10px 0 4px; color: var(--color-accent-gold);">$1</h4>');
        text = text.replace(/^## (.*$)/gim, '<h3 style="font-size: 1.1rem; font-weight: 700; margin: 12px 0 6px; color: var(--color-accent-gold);">$1</h3>');

        // Lists
        const lines = text.split('\n');
        let inList = false;
        let listType = 'ul';
        let processedLines = [];

        for (let i = 0; i < lines.length; i++) {
            let line = lines[i].trim();

            if (line.startsWith('* ') || line.startsWith('- ')) {
                if (!inList || listType !== 'ul') {
                    if (inList) processedLines.push(`</${listType}>`);
                    processedLines.push('<ul>');
                    inList = true;
                    listType = 'ul';
                }
                processedLines.push(`<li>${line.substring(2)}</li>`);
            } else if (/^\d+\.\s/.test(line)) {
                if (!inList || listType !== 'ol') {
                    if (inList) processedLines.push(`</${listType}>`);
                    processedLines.push('<ol>');
                    inList = true;
                    listType = 'ol';
                }
                const itemContent = line.replace(/^\d+\.\s/, '');
                processedLines.push(`<li>${itemContent}</li>`);
            } else {
                if (inList) {
                    processedLines.push(`</${listType}>`);
                    inList = false;
                }
                if (line.length > 0) {
                    processedLines.push(`<p>${line}</p>`);
                }
            }
        }

        if (inList) {
            processedLines.push(`</${listType}>`);
        }

        return processedLines.join('');
    }

    // Start on DOMContentLoaded
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
