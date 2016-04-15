package tk.zielony.taskqueue;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;

public class TaskQueue implements ViewTreeObserver.OnPreDrawListener {
    private static final long IDLE_MS = 500;

    public AverageValue averageTime = new AverageValue();

    private View view;

    private long prevFrameTime;
    private Handler handler = new Handler(Looper.getMainLooper());

    final List<Runnable> queue = new ArrayList<>();
    List<Runnable> frameTasks = new ArrayList<>();

    boolean idle = false, running = false;

    Thread thread = new Thread() {
        public void run() {
            while (running) {
                try {
                    synchronized (TaskQueue.this) {
                        while (queue.isEmpty() || !idle)
                            TaskQueue.this.wait();

                        queue.remove(0).run();
                    }
                } catch (InterruptedException e) {
                }
            }
        }
    };

    Runnable idleRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (TaskQueue.this) {
                idle = true;
                TaskQueue.this.notify();
            }
        }
    };

    public void queueTask(Runnable task) {
        synchronized (this) {
            queue.add(task);
            notify();
        }
    }

    public void unqueueTask(Runnable task) {
        synchronized (this) {
            queue.remove(task);
        }
    }

    public void clearTasks() {
        synchronized (this) {
            queue.clear();
        }
    }

    public void addFrameTask(Runnable task) {
        synchronized (this) {
            frameTasks.add(task);
        }
    }

    public void removeFrameTask(Runnable task) {
        synchronized (this) {
            frameTasks.remove(task);
        }
    }

    public void clearFrameTasks() {
        synchronized (this) {
            frameTasks.clear();
        }
    }

    @Override
    public boolean onPreDraw() {
        synchronized (this) {
            for (Runnable r : frameTasks)
                r.run();
        }

        long currentFrameTime = System.currentTimeMillis();

        handler.removeCallbacks(idleRunnable);
        handler.postDelayed(idleRunnable, IDLE_MS);

        if (currentFrameTime - prevFrameTime < averageTime.get()){
            idleRunnable.run();
        } else {
            idle = false;
        }

        averageTime.add(currentFrameTime - prevFrameTime);
        prevFrameTime = currentFrameTime;

        return true;
    }

    public void start(@NonNull View view) {
        if (view == this.view)
            return;

        if (this.view != null) {
            handler.removeCallbacks(idleRunnable);
            this.view.getViewTreeObserver().removeOnPreDrawListener(this);
        }
        this.view = view;
        view.getViewTreeObserver().addOnPreDrawListener(this);
        if (!thread.isAlive()) {
            running = true;
            thread.start();
        }
    }

    public void stop() {
        handler.removeCallbacks(idleRunnable);
        if (view != null) {
            view.getViewTreeObserver().removeOnPreDrawListener(this);
            view = null;
        }
        if (thread.isAlive()) {
            running = false;
            notify();
        }
    }
}