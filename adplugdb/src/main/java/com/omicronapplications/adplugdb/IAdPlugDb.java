package com.omicronapplications.adplugdb;

public interface IAdPlugDb {
    int SORTBY_NONE = 0;
    int SORTBY_TITLE = 1;
    int SORTBY_AUTHOR = 2;
    int SORTBY_FILE = 3;
    int SORTBY_TYPE = 4;
    int SORTBY_LENGTH = 5;
    int ORDER_NONE = 0;
    int ORDER_ASCENDING = 1;
    int ORDER_DESCENDING = 2;

    void getStatus();
    void index(String root, boolean quick);
    void delete();
    void list(String path, int sortby, int order, boolean quick, boolean hide, boolean random);
    void playlist();
    void add(String song, long length);
    void remove(String song);
    void rename(String before, String after);
    void getCount();
    void search(String query);
    void onSongInfo(String song, String type, String title, String author, String desc, long length, long songlength, int subsongs, boolean valid, boolean playlist);
}
