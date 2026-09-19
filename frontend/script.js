/**
 * Whisp — Real-time Chat Client
 * Pure Vanilla JavaScript Application Logic
 */

// --- Application State ---
const state = {
  currentUser: localStorage.getItem('whisp_username') || '',
  baseUrl: localStorage.getItem('whisp_base_url') || 'https://whisp-app.duckdns.org',
  stompClient: null,
  sockJS: null,
  messagesMap: new Map(), // UUID -> Message Object
  currentPage: 0,
  pageSize: 20,
  isLastPage: false,
  isLoadingMore: false,
  isConnected: false,
  reconnectTimer: null,
  reconnectDelayMs: 5000,
  editingMessageId: null,
};

// Color Palette for User Avatars & Names
const USER_COLORS = [
  { text: 'text-emerald', avatar: 'avatar-emerald' },
  { text: 'text-blue', avatar: 'avatar-blue' },
  { text: 'text-amber', avatar: 'avatar-amber' },
  { text: 'text-pink', avatar: 'avatar-pink' },
  { text: 'text-cyan', avatar: 'avatar-cyan' },
  { text: 'text-rose', avatar: 'avatar-rose' },
  { text: 'text-violet', avatar: 'avatar-violet' },
];

// --- DOM Elements ---
let joinScreen;
let chatScreen;
let usernameInput;
let currentUserNameEl;
let messageContainer;
let messageList;
let messageInput;
let emptyState;
let loadMoreContainer;
let loadMoreBtn;
let statusPill;
let statusDot;
let statusText;
let baseUrlInput;
let displayBaseUrl;
let serverModal;
let scrollBottomBtn;
let editModal;
let editContentInput;
let toastBanner;
let toastText;
let toastIcon;

// --- App Lifecycle Setup ---
window.addEventListener('DOMContentLoaded', () => {
  // Bind DOM Elements
  joinScreen = document.getElementById('joinScreen');
  chatScreen = document.getElementById('chatScreen');
  usernameInput = document.getElementById('usernameInput');
  currentUserNameEl = document.getElementById('currentUserName');
  messageContainer = document.getElementById('messageContainer');
  messageList = document.getElementById('messageList');
  messageInput = document.getElementById('messageInput');
  emptyState = document.getElementById('emptyState');
  loadMoreContainer = document.getElementById('loadMoreContainer');
  loadMoreBtn = document.getElementById('loadMoreBtn');
  statusPill = document.getElementById('statusPill');
  statusDot = document.getElementById('statusDot');
  statusText = document.getElementById('statusText');
  baseUrlInput = document.getElementById('baseUrlInput');
  displayBaseUrl = document.getElementById('displayBaseUrl');
  serverModal = document.getElementById('serverModal');
  scrollBottomBtn = document.getElementById('scrollBottomBtn');
  editModal = document.getElementById('editModal');
  editContentInput = document.getElementById('editContentInput');
  toastBanner = document.getElementById('toastBanner');
  toastText = document.getElementById('toastText');
  toastIcon = document.getElementById('toastIcon');

  baseUrlInput.value = state.baseUrl;
  displayBaseUrl.textContent = state.baseUrl;

  messageContainer.addEventListener('scroll', handleScrollVisibility);

  if (state.currentUser) {
    enterChatRoom();
  } else {
    showJoinScreen();
  }
});

// --- Screen Transitions ---
function showJoinScreen() {
  joinScreen.classList.remove('hidden');
  chatScreen.classList.add('hidden');
  disconnectWebSocket();
  setTimeout(() => usernameInput && usernameInput.focus(), 150);
}

function enterChatRoom() {
  joinScreen.classList.add('hidden');
  chatScreen.classList.remove('hidden');

  currentUserNameEl.textContent = state.currentUser;

  // Reset pagination and messages
  state.currentPage = 0;
  state.isLastPage = false;
  state.messagesMap.clear();
  messageList.innerHTML = '';

  // Connect WebSocket (STOMP) - history will be fetched automatically via WS upon connect
  connectWebSocket();

  setTimeout(() => messageInput && messageInput.focus(), 150);
}

function handleJoin(event) {
  event.preventDefault();
  const name = usernameInput.value.trim();
  if (!name) return;

  state.currentUser = name;
  localStorage.setItem('whisp_username', name);
  enterChatRoom();
}

function leaveChat() {
  state.currentUser = '';
  localStorage.removeItem('whisp_username');
  showJoinScreen();
}

// --- WebSocket Connection & Subscriptions ---
function connectWebSocket() {
  if (state.stompClient && state.stompClient.connected) {
    return;
  }

  updateStatusUI('connecting');
  const wsEndpoint = `${state.baseUrl}/ws`;

  try {
    state.sockJS = new SockJS(wsEndpoint);
    state.stompClient = Stomp.over(state.sockJS);
    state.stompClient.debug = null; // Disable verbose console debug logs

    state.stompClient.connect({}, onStompConnected, onStompError);
  } catch (e) {
    console.error('SockJS initialization failed:', e);
    onStompError('SockJS init failure');
  }
}

function onStompConnected() {
  updateStatusUI('connected');
  showToast('Connected to Whisp real-time server', 'success');

  if (state.reconnectTimer) {
    clearTimeout(state.reconnectTimer);
    state.reconnectTimer = null;
  }

  // 1. Subscribe to new chat messages
  state.stompClient.subscribe('/topic/messages', (messagePayload) => {
    try {
      const newMsg = JSON.parse(messagePayload.body);
      if (state.messagesMap.has(newMsg.id)) {
        updateMessageElement(newMsg);
      } else {
        state.messagesMap.set(newMsg.id, newMsg);
        renderMessage(newMsg, 'append');
        updateEmptyState();

        const isSelf = newMsg.sender === state.currentUser;
        const isUserNearBottom = messageContainer.scrollHeight - messageContainer.scrollTop - messageContainer.clientHeight < 150;
        if (isSelf || isUserNearBottom) {
          scrollToBottom(true);
        }
      }
    } catch (e) {
      console.error('Failed to parse message payload:', e);
    }
  });

  // 2. Subscribe to message history/pagination
  state.stompClient.subscribe('/topic/history', (payload) => {
    try {
      const data = JSON.parse(payload.body);
      handleHistoryPayload(data);
    } catch (e) {
      console.error('Failed to parse history payload:', e);
    }
  });

  // 3. Subscribe to real-time edits
  state.stompClient.subscribe('/topic/edit', (payload) => {
    try {
      const updatedMsg = JSON.parse(payload.body);
      state.messagesMap.set(updatedMsg.id, updatedMsg);
      updateMessageElement(updatedMsg);
    } catch (e) {
      console.error('Failed to parse edit payload:', e);
    }
  });

  // 4. Subscribe to real-time deletions
  state.stompClient.subscribe('/topic/delete', (payload) => {
    try {
      let deletedId = payload.body;
      if (typeof deletedId === 'string' && deletedId.startsWith('"') && deletedId.endsWith('"')) {
        deletedId = JSON.parse(deletedId);
      }
      removeMessageElement(deletedId);
    } catch (e) {
      console.error('Failed to parse delete payload:', e);
    }
  });

  // Request initial history page via WebSocket
  requestHistoryViaWebSocket(0);
}

function onStompError(error) {
  console.warn('STOMP Connection error:', error);
  updateStatusUI('reconnecting');
  scheduleReconnect();
}

function scheduleReconnect() {
  if (state.reconnectTimer) return;
  state.reconnectTimer = setTimeout(() => {
    state.reconnectTimer = null;
    if (state.currentUser) {
      connectWebSocket();
    }
  }, state.reconnectDelayMs);
}

function disconnectWebSocket() {
  if (state.reconnectTimer) {
    clearTimeout(state.reconnectTimer);
    state.reconnectTimer = null;
  }
  if (state.stompClient && state.stompClient.connected) {
    state.stompClient.disconnect(() => {
      updateStatusUI('disconnected');
    });
  }
}

// --- WebSocket Pagination / History Handling ---
function requestHistoryViaWebSocket(page = 0) {
  if (!state.stompClient || !state.stompClient.connected) return;

  state.isLoadingMore = true;
  if (page > 0) {
    loadMoreBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i><span>Loading...</span>';
  }

  state.stompClient.send('/app/history', {}, JSON.stringify({
    page: page,
    size: state.pageSize
  }));
}

function handleHistoryPayload(data) {
  const messages = data.content || (Array.isArray(data) ? data : []);
  const pageNumber = data.number !== undefined ? data.number : 0;
  state.isLastPage = data.last !== undefined ? data.last : messages.length < state.pageSize;
  state.currentPage = pageNumber;

  updateLoadMoreVisibility();

  if (messages.length > 0) {
    if (pageNumber === 0) {
      // First page: reverse so oldest is on top, newest at bottom
      [...messages].reverse().forEach(msg => {
        if (!state.messagesMap.has(msg.id)) {
          state.messagesMap.set(msg.id, msg);
          renderMessage(msg, 'append');
        }
      });
      scrollToBottom();
    } else {
      // Older pages: render in order as prepend, preserve scroll
      const prevScrollHeight = messageContainer.scrollHeight;
      const prevScrollTop = messageContainer.scrollTop;

      messages.forEach(msg => {
        if (!state.messagesMap.has(msg.id)) {
          state.messagesMap.set(msg.id, msg);
          renderMessage(msg, 'prepend');
        }
      });

      const newScrollHeight = messageContainer.scrollHeight;
      messageContainer.scrollTop = prevScrollTop + (newScrollHeight - prevScrollHeight);
    }
  }

  state.isLoadingMore = false;
  loadMoreBtn.innerHTML = '<i class="fa-solid fa-clock-rotate-left"></i><span>Load older messages</span>';
  updateEmptyState();
}

function loadOlderMessages() {
  if (state.isLoadingMore || state.isLastPage) return;
  requestHistoryViaWebSocket(state.currentPage + 1);
}

function updateLoadMoreVisibility() {
  if (state.isLastPage || state.messagesMap.size === 0) {
    loadMoreContainer.classList.add('hidden');
  } else {
    loadMoreContainer.classList.remove('hidden');
  }
}

// --- WebSocket: Send Message ---
function sendMessage(event) {
  event.preventDefault();
  const content = messageInput.value.trim();
  if (!content) return;

  if (!state.stompClient || !state.stompClient.connected) {
    showToast('Cannot send: WebSocket disconnected', 'error');
    return;
  }

  const payload = {
    sender: state.currentUser,
    content: content
  };

  try {
    state.stompClient.send('/app/chat', {}, JSON.stringify(payload));
    messageInput.value = '';
    messageInput.focus();
  } catch (err) {
    console.error('Error sending message:', err);
    showToast('Failed to deliver message', 'error');
  }
}

// --- WebSocket: Delete Message ---
function deleteMessage(id) {
  if (!id) return;
  if (!confirm('Are you sure you want to delete this message?')) return;

  if (!state.stompClient || !state.stompClient.connected) {
    showToast('Cannot delete: WebSocket disconnected', 'error');
    return;
  }

  const payload = {
    id: id,
    sender: state.currentUser
  };

  try {
    state.stompClient.send('/app/delete', {}, JSON.stringify(payload));
  } catch (err) {
    console.error('Delete WS error:', err);
    showToast('Failed to send delete request', 'error');
  }
}

// --- WebSocket: Edit Message ---
function openEditModal(id) {
  const msg = state.messagesMap.get(id);
  if (!msg) return;

  if (isMessageOlderThanOneMinute(msg.timestamp)) {
    showToast('Cannot edit: Message is older than 1 minute', 'error');
    return;
  }

  state.editingMessageId = id;
  editContentInput.value = msg.content;
  editModal.classList.remove('hidden');
  setTimeout(() => editContentInput.focus(), 100);
}

function closeEditModal() {
  editModal.classList.add('hidden');
  state.editingMessageId = null;
  editContentInput.value = '';
}

function handleEditSubmit(event) {
  event.preventDefault();
  const newContent = editContentInput.value.trim();
  const id = state.editingMessageId;
  if (!id || !newContent) return;

  if (!state.stompClient || !state.stompClient.connected) {
    showToast('Cannot edit: WebSocket disconnected', 'error');
    return;
  }

  const payload = {
    id: id,
    sender: state.currentUser,
    content: newContent
  };

  try {
    state.stompClient.send('/app/edit', {}, JSON.stringify(payload));
    closeEditModal();
    showToast('Edit sent', 'success');
  } catch (err) {
    console.error('Edit WS error:', err);
    showToast('Failed to send edit request', 'error');
  }
}

// --- Render & DOM Manipulation ---
function renderMessage(msg, position = 'append') {
  if (!msg || !msg.content) return;

  const isSelf = msg.sender === state.currentUser;
  const formattedTime = formatTimestamp(msg.timestamp);
  const colorScheme = getUserColorScheme(msg.sender);

  const msgNode = document.createElement('div');
  msgNode.id = `msg-${msg.id}`;
  msgNode.className = `msg-row ${isSelf ? 'self' : 'other'} animate-fade-in-up`;

  let innerHTML = '';

  if (isSelf) {
    innerHTML = `
      <div class="msg-wrapper-self">
        <div class="msg-meta-self">
          <div class="msg-actions">
            ${msg.id ? `
              <button onclick="openEditModal('${msg.id}')" title="Edit message" class="msg-action-btn edit">
                <i class="fa-solid fa-pen"></i>
              </button>
              <button onclick="deleteMessage('${msg.id}')" title="Delete message" class="msg-action-btn delete">
                <i class="fa-solid fa-trash"></i>
              </button>
            ` : ''}
          </div>
          <span class="msg-sender-self-label">You</span>
        </div>
        <div class="msg-bubble-self msg-content">${escapeHtml(msg.content)}</div>
        <span class="msg-time self">${formattedTime}</span>
      </div>
    `;
  } else {
    innerHTML = `
      <div class="msg-wrapper-other">
        <div class="msg-avatar ${colorScheme.avatar}">
          ${escapeHtml(msg.sender.charAt(0))}
        </div>
        <div class="msg-bubble-wrap-other">
          <span class="msg-sender-name ${colorScheme.text}">${escapeHtml(msg.sender)}</span>
          <div class="msg-bubble-other msg-content">${escapeHtml(msg.content)}</div>
          <span class="msg-time other">${formattedTime}</span>
        </div>
      </div>
    `;
  }

  msgNode.innerHTML = innerHTML;

  if (position === 'prepend') {
    messageList.prepend(msgNode);
  } else {
    messageList.appendChild(msgNode);
  }
}

function updateMessageElement(msg) {
  const el = document.getElementById(`msg-${msg.id}`);
  if (el) {
    const contentEl = el.querySelector('.msg-content');
    if (contentEl) {
      contentEl.textContent = msg.content;
    }
  }
}

function removeMessageElement(id) {
  state.messagesMap.delete(id);
  const el = document.getElementById(`msg-${id}`);
  if (el) {
    el.remove();
  }
  updateEmptyState();
}

function updateEmptyState() {
  if (state.messagesMap.size === 0) {
    emptyState.style.display = 'flex';
  } else {
    emptyState.style.display = 'none';
  }
}

// --- UI Status, Badges & Toasts ---
function updateStatusUI(status) {
  if (!statusPill) return;

  statusPill.className = `status-pill status-${status}`;

  if (status === 'connected') {
    state.isConnected = true;
    statusText.textContent = 'Live';
  } else if (status === 'connecting') {
    state.isConnected = false;
    statusText.textContent = 'Connecting...';
  } else if (status === 'reconnecting') {
    state.isConnected = false;
    statusText.textContent = 'Reconnecting in 5s';
  } else {
    state.isConnected = false;
    statusText.textContent = 'Disconnected';
  }
}

let toastTimeout = null;
function showToast(message, type = 'info') {
  if (!toastBanner || !toastText || !toastIcon) return;

  toastText.textContent = message;

  if (type === 'error') {
    toastIcon.className = 'fa-solid fa-circle-exclamation';
    toastIcon.style.color = 'var(--status-disconnected-text)';
  } else if (type === 'success') {
    toastIcon.className = 'fa-solid fa-circle-check';
    toastIcon.style.color = 'var(--status-connected-text)';
  } else {
    toastIcon.className = 'fa-solid fa-circle-info';
    toastIcon.style.color = 'var(--whisp-400)';
  }

  toastBanner.classList.add('show');

  if (toastTimeout) clearTimeout(toastTimeout);
  toastTimeout = setTimeout(hideToast, 4000);
}

function hideToast() {
  if (toastBanner) {
    toastBanner.classList.remove('show');
  }
}

// --- Helpers ---
function scrollToBottom(smooth = false) {
  if (!messageContainer) return;
  messageContainer.scrollTo({
    top: messageContainer.scrollHeight,
    behavior: smooth ? 'smooth' : 'auto'
  });
}

function handleScrollVisibility() {
  if (!messageContainer || !scrollBottomBtn) return;
  const distanceToBottom = messageContainer.scrollHeight - messageContainer.scrollTop - messageContainer.clientHeight;
  if (distanceToBottom > 150) {
    scrollBottomBtn.classList.add('show');
  } else {
    scrollBottomBtn.classList.remove('show');
  }
}

function isMessageOlderThanOneMinute(timestampStr) {
  if (!timestampStr) return false;
  try {
    const msgTime = new Date(timestampStr).getTime();
    const now = new Date().getTime();
    return (now - msgTime) > 60 * 1000;
  } catch (e) {
    return false;
  }
}

function getUserColorScheme(username) {
  if (!username) return USER_COLORS[0];
  let hash = 0;
  for (let i = 0; i < username.length; i++) {
    hash = username.charCodeAt(i) + ((hash << 5) - hash);
  }
  return USER_COLORS[Math.abs(hash) % USER_COLORS.length];
}

function formatTimestamp(isoString) {
  if (!isoString) return new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) {
      return isoString;
    }
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } catch (e) {
    return '';
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function toggleServerModal() {
  if (serverModal) {
    serverModal.classList.toggle('hidden');
  }
}

function saveServerConfig() {
  let url = baseUrlInput.value.trim();
  if (url.endsWith('/')) {
    url = url.slice(0, -1);
  }
  state.baseUrl = url;
  localStorage.setItem('whisp_base_url', url);
  displayBaseUrl.textContent = url;
  toggleServerModal();

  disconnectWebSocket();
  enterChatRoom();
}
