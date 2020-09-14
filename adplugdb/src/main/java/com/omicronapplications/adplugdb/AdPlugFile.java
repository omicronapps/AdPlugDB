package com.omicronapplications.adplugdb;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Comparator;

public class AdPlugFile implements Comparator<AdPlugFile>, Comparable<AdPlugFile> {
    public AdPlugFile() {}

    public AdPlugFile(String path, String name, String type, String title, String author, String desc, long length, long songlength, int subsongs, boolean valid, boolean playlist) {
        this.path = path != null ? path : "";
        this.name = name != null ? name : "";
        this.type = type != null ? type : "";
        this.title = title != null ? title : "";
        this.author = author != null ? author : "";
        this.desc = desc != null ? desc : "";
        this.length = length;
        this.songlength = songlength;
        this.subsongs = subsongs;
        this.valid = valid;
        this.dir = false;
        this.playlist = playlist;
    }

    public AdPlugFile(String path, String name) {
        this.path = path;
        this.name = name;
        this.type = "";
        this.title = "";
        this.author = "";
        this.desc = "";
        this.length = 0;
        this.songlength = -1;
        this.subsongs = -1;
        this.valid = false;
        this.dir = true;
        this.playlist = false;
    }

    public String getFullPath() {
        String fullPath = "";
        if (path != null && name != null) {
            fullPath = new File(path, name).getAbsolutePath();
        } else if (path != null) {
            fullPath = path;
        } else if (name != null) {
            fullPath = name;
        }
        return fullPath;
    }

    public File getFile() {
        File file = null;
        if (path != null && name != null) {
            file = new File(path, name);
        } else if (path != null) {
            file = new File(path);
        } else if (name != null) {
            file = new File(name);
        }
        return file;
    }

    @Override
    @NonNull
    public String toString() {
        return path + ", " + name + ", " + type + ", " + title + ", " + author + ", " + desc + ", " + length + ", " + songlength + ", " + subsongs + ", " + valid + ", " + dir + ", " + playlist;
    }

    private static boolean equals(Object obj1, Object obj2) {
        return (obj1 == obj2) || (obj1 != null && obj1.equals(obj2));
    }

    @Override
    public int compare(AdPlugFile o1, AdPlugFile o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null || o2 == null) {
            throw new NullPointerException();
        }
        if (o1.getClass() != o2.getClass()) {
            throw new ClassCastException();
        }
        if (o1.path != null && o1.path.equals(o2.path)) {
            return o1.name.compareTo(o2.name);
        } else if (o1.path != null) {
            return o1.path.compareTo(o2.path);
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AdPlugFile other = (AdPlugFile) obj;
        return path != null &&
                equals(path, other.path) &&
                equals(name, other.name) &&
                equals(type, other.type) &&
                equals(title, other.title) &&
                equals(author, other.author) &&
                equals(desc, other.desc) &&
                length == other.length &&
                songlength == other.songlength &&
                subsongs == other.subsongs &&
                valid == other.valid &&
                dir == other.dir &&
                playlist == other.playlist;
    }

    @Override
    public int compareTo(AdPlugFile o) {
        if (this == o) {
            return 0;
        }
        if (o == null) {
            throw new NullPointerException();
        }
        if (this.getClass() != o.getClass()) {
            throw new ClassCastException();
        }
        if (path != null && path.equals(o.path)) {
            return name.compareTo(o.name);
        } else if (path != null) {
            return path.compareTo(o.path);
        } else {
            return 0;
        }
    }

    public String path;
    public String name;
    public String type;
    public String title;
    public String author;
    public String desc;
    public long length;
    public long songlength;
    public int subsongs;
    public boolean valid;
    public boolean dir;
    public boolean playlist;
}
