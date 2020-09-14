package com.omicronapplications.adplugdb;

public interface IAdPlugDb {
    int SORT_NONE = 0;
    int SORT_ASCENDING = 1;
    int SORT_DESCENDING = 2;

    void getStatus();
    void index(String root);
    void delete();
    void list(String path, int order);
    void add(String song, long length);
    void remove(String song);
    void getCount();
    void onSongInfo(String song, String type, String title, String author, String desc, long length, long songlength, int subsongs, boolean valid, boolean playlist);
}
