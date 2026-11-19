import { store } from '../db/store.js';

export class MaxTaskService {
  getAllTasks() {
    return store.get('tasks') || [];
  }

  createTask({ title, dueAt, originDevice, assignedDevice }) {
    const newTask = {
      id: `task_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
      title,
      completed: false,
      dueAt: dueAt || (Date.now() + 3600 * 1000),
      originDevice: originDevice || 'unknown',
      assignedDevice: assignedDevice || 'all',
      createdAt: Date.now()
    };

    store.update('tasks', (tasks) => [...(tasks || []), newTask]);
    return newTask;
  }

  toggleTaskCompletion(taskId) {
    let target = null;
    store.update('tasks', (tasks) =>
      (tasks || []).map(t => {
        if (t.id === taskId) {
          target = { ...t, completed: !t.completed };
          return target;
        }
        return t;
      })
    );
    return target;
  }

  deleteTask(taskId) {
    let deleted = false;
    store.update('tasks', (tasks) =>
      (tasks || []).filter(t => {
        if (t.id === taskId) {
          deleted = true;
          return false;
        }
        return true;
      })
    );
    return deleted;
  }
}

export const taskService = new MaxTaskService();
