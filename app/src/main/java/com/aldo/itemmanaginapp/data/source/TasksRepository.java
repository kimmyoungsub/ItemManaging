package com.aldo.itemmanaginapp.data.source;

import android.support.annotation.NonNull;

import com.aldo.itemmanaginapp.data.Task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by aldo on 2017-08-04.
 */

public class TasksRepository implements TasksDataSource{

    private static TasksRepository INSTANCE=null;

    private final TasksDataSource mTasksRemoteDataSource;

    private final TasksDataSource mTasksLocalDataSource;

    Map<String, Task> mCachedTasks;

    boolean mCacheIsDirty=false;

    private TasksRepository(@NonNull TasksDataSource tasksRemoteDataSource,@NonNull TasksDataSource tasksLocalDataSource){
        mTasksRemoteDataSource=checkNotNull(tasksRemoteDataSource);
        mTasksLocalDataSource=checkNotNull(tasksLocalDataSource);
    }
    /**
     * Returns the single instance of this class, creating it if necessary.
     *
     * @param tasksRemoteDataSource the backend data source
     * @param tasksLocalDataSource  the device storage data source
     * @return the {@link TasksRepository} instance
     */
    public static TasksRepository getInstance(TasksDataSource tasksRemoteDataSource,
                                              TasksDataSource tasksLocalDataSource) {
        if (INSTANCE == null) {
            INSTANCE = new TasksRepository(tasksRemoteDataSource, tasksLocalDataSource);
        }
        return INSTANCE;
    }

    /**
     * Used to force {@link #getInstance(TasksDataSource, TasksDataSource)} to create a new instance
     * next time it's called.
     */

    public static void destroyInstance() {
        INSTANCE = null;
    }
    @Override
    public void getTasks(@NonNull final TasksDataSource.LoadTasksCallback callback){
        checkNotNull(callback);

        if(mCacheIsDirty){
            getTasksFromRemoteDataSource(callback);
        }else{
            mTasksLocalDataSource.getTasks(new TasksDataSource.LoadTasksCallback(){
                @Override
                public void onTasksLoaded(List<Task> tasks) {
                    refreshCache(tasks);
                    callback.onTasksLoaded(new ArrayList<>(mCachedTasks.values()));
                }

                @Override
                public void onDataNotAvailable() {
                    getTasksFromRemoteDataSource(callback);
                }
            });
        }
    }

    @Override
    public void saveTask(@NonNull Task task) {
        checkNotNull(task);
        mTasksRemoteDataSource.saveTask(task);
        mTasksLocalDataSource.saveTask(task);

        if(mCachedTasks==null){
            mCachedTasks=new LinkedHashMap<>();
        }
        mCachedTasks.put(task.getId(),task);

    }

    @Override
    public void completeTask(@NonNull Task task) {
        checkNotNull(task);
        mTasksLocalDataSource.completeTask(task);
        mTasksRemoteDataSource.completeTask(task);

        Task completedTask=new Task(task.getTitle(),task.getDescription(),task.getId(),true);

        if(mCachedTasks==null){
            mCachedTasks=new LinkedHashMap<>();
        }
        mCachedTasks.put(task.getId(),completedTask);
    }

    @Override
    public void completeTask(@NonNull String taskId) {
        checkNotNull(taskId);
        completeTask(getTaskWithId(taskId));
    }

    @Override
    public void activateTask(@NonNull Task task) {
        checkNotNull(task);

        mTasksRemoteDataSource.activateTask(task);
        mTasksLocalDataSource.activateTask(task);

        Task activeTask=new Task(task.getTitle(),task.getDescription(),task.getId());

        if(mCachedTasks==null){
            mCachedTasks=new LinkedHashMap<>();
        }
        mCachedTasks.put(task.getId(),activeTask);
    }

    @Override
    public void activateTask(@NonNull String taskId) {
        checkNotNull(taskId);
        activateTask(getTaskWithId(taskId));
    }

    @Override
    public void clearCompletedTasks() {
        mTasksRemoteDataSource.clearCompletedTasks();
        mTasksLocalDataSource.clearCompletedTasks();

        if(mCachedTasks==null){
            mCachedTasks=new LinkedHashMap<>();
        }
        Iterator<Map.Entry<String,Task>> it=mCachedTasks.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String,Task> entry=it.next();
            if(entry.getValue().isCompleted()){
                it.remove();
            }
        }
    }




    /**
     * Gets tasks from local data source (sqlite) unless the table is new or empty. In that case it
     * uses the network data source. This is done to simplify the sample.
     * <p>
     * Note: {@link GetTaskCallback#onDataNotAvailable()} is fired if both data sources fail to
     * get the data.
     */
    @Override
    public void getTask(@NonNull String taskId, @NonNull final GetTaskCallback callback) {
        checkNotNull(taskId);
        checkNotNull(callback);

        Task cachedTask=getTaskWithId(taskId);

        if(cachedTask!=null){
            callback.onTaskLoaded(cachedTask);
            return;
        }

        mTasksLocalDataSource.getTask(taskId, new GetTaskCallback() {
            @Override
            public void onTaskLoaded(Task task) {
                if (mCachedTasks == null) {
                    mCachedTasks = new LinkedHashMap<>();
                }
                mCachedTasks.put(task.getId(), task);
                callback.onTaskLoaded(task);
            }

            @Override
            public void onDataNotAvailable() {
            callback.onDataNotAvailable();
            }
        });
    }

    @Override
    public void refreshTasks() {
        mCacheIsDirty=true;
    }

    @Override
    public void deleteAllTasks() {
        mTasksRemoteDataSource.deleteAllTasks();
        mTasksLocalDataSource.deleteAllTasks();
        if(mCachedTasks==null){
            mCachedTasks=new LinkedHashMap<>();
        }
        mCachedTasks.clear();
    }

    @Override
    public void deleteTask(@NonNull String taskId) {
        mTasksLocalDataSource.deleteTask(checkNotNull(taskId));
        mTasksRemoteDataSource.deleteTask(checkNotNull(taskId));

        mCachedTasks.remove(taskId);
    }

    private Task getTaskWithId(String taskId) {
        checkNotNull(taskId);
        if (mCachedTasks == null || mCachedTasks.isEmpty()) {
            return null;
        } else {
            return mCachedTasks.get(taskId);
        }
    }

    private void getTasksFromRemoteDataSource(@NonNull final TasksDataSource.LoadTasksCallback callback) {
        mTasksRemoteDataSource.getTasks(new TasksDataSource.LoadTasksCallback() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                refreshCache(tasks);
                refreshLocalDataSource(tasks);
                callback.onTasksLoaded(new ArrayList<>(mCachedTasks.values()));
            }

            @Override
            public void onDataNotAvailable() {
                callback.onDataNotAvailable();
            }
        });
    }

    private void refreshLocalDataSource(List<Task> tasks) {
        mTasksLocalDataSource.deleteAllTasks();
        for (Task task : tasks) {
            mTasksLocalDataSource.saveTask(task);
        }
    }



    private void refreshCache(List<Task> tasks) {
        if (mCachedTasks == null) {
            mCachedTasks = new LinkedHashMap<>();
        }
        mCachedTasks.clear();
        for (Task task : tasks) {
            mCachedTasks.put(task.getId(), task);
        }
        mCacheIsDirty = false;
    }
}
