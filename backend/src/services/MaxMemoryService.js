import { store } from '../db/store.js';

export class MaxMemoryService {
  getAllMemories() {
    return store.get('memories') || [];
  }

  saveMemory(key, content, sourceDevice = 'system') {
    let result = null;
    store.update('memories', (mems) => {
      const existingIdx = (mems || []).findIndex(m => m.key.toLowerCase() === key.toLowerCase());
      if (existingIdx >= 0) {
        const updated = {
          ...mems[existingIdx],
          content,
          sourceDevice,
          updatedAt: Date.now()
        };
        mems[existingIdx] = updated;
        result = updated;
        return [...mems];
      } else {
        const newMem = {
          id: `mem_${Date.now().toString(36)}`,
          key,
          content,
          sourceDevice,
          updatedAt: Date.now()
        };
        result = newMem;
        return [...(mems || []), newMem];
      }
    });
    return result;
  }

  searchMemories(query) {
    const q = query.toLowerCase();
    const all = this.getAllMemories();
    return all.filter(m => m.key.toLowerCase().includes(q) || m.content.toLowerCase().includes(q));
  }

  deleteMemory(idOrKey) {
    let deleted = false;
    store.update('memories', (mems) =>
      (mems || []).filter(m => {
        if (m.id === idOrKey || m.key.toLowerCase() === idOrKey.toLowerCase()) {
          deleted = true;
          return false;
        }
        return true;
      })
    );
    return deleted;
  }
}

export const memoryService = new MaxMemoryService();
