package com.omicronapplications.adplugdb;

import java.util.List;

public interface IAdPlugDbCallback {
    enum dbStatus {
        UNINITIALIZED,
        INDEXING,
        INITIALIZED
    }
    void onDBServiceConnected();
    void onDBServiceDisconnected();
    void onStatusChanged(dbStatus status);
    void requestInfo(String name, long length);
    void onList(List<AdPlugFile> songs);
    void onStatus(dbStatus status);
    void onGetCount(long count);
}
