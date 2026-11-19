import { store } from '../db/store.js';

export class MaxConversationService {
  getRecentHistory(limit = 50) {
    const list = store.get('conversations') || [];
    return list.slice(-limit);
  }

  addMessage({ senderDevice, role, content, language = 'ENGLISH' }) {
    const newMsg = {
      id: `msg_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
      senderDevice: senderDevice || 'system',
      role: role || 'USER',
      content,
      language,
      timestamp: Date.now()
    };

    store.update('conversations', (convos) => {
      const updated = [...(convos || []), newMsg];
      if (updated.length > 200) updated.shift();
      return updated;
    });

    return newMsg;
  }

  clearHistory() {
    store.set('conversations', []);
    return true;
  }
}

export const conversationService = new MaxConversationService();
