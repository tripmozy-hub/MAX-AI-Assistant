import { store } from '../db/store.js';

export class MaxNotificationService {
  getAllNotifications() {
    return store.get('notifications') || [];
  }

  sendNotification({ title, body, sourceDevice = 'system', targetDevice = 'all' }) {
    const notif = {
      id: `notif_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
      title,
      body,
      sourceDevice,
      targetDevice,
      read: false,
      timestamp: Date.now()
    };

    store.update('notifications', (list) => {
      const updated = [notif, ...(list || [])];
      return updated.slice(0, 100);
    });

    return notif;
  }

  markAsRead(notificationId) {
    store.update('notifications', (list) =>
      (list || []).map(n => (n.id === notificationId ? { ...n, read: true } : n))
    );
  }
}

export const notificationService = new MaxNotificationService();
