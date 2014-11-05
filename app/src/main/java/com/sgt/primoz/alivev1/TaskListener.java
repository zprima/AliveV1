package com.sgt.primoz.alivev1;

/**
 * Created by Primoz on 5.11.2014.
 */
public interface TaskListener {
    void onTaskStarted();
    void onTaskFinished(String result);
}
