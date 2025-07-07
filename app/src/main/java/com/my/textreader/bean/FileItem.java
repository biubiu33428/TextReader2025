package com.my.textreader.bean;

import java.io.File;

public class FileItem {
    private File file;
    private String displayName;
    private boolean isDirectory;
    
    public FileItem(File file, String displayName, boolean isDirectory) {
        this.file = file;
        this.displayName = displayName;
        this.isDirectory = isDirectory;
    }
    
    public File getFile() {
        return file;
    }
    
    public void setFile(File file) {
        this.file = file;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public boolean isDirectory() {
        return isDirectory;
    }
    
    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FileItem fileItem = (FileItem) obj;
        return file != null ? file.equals(fileItem.file) : fileItem.file == null;
    }
    
    @Override
    public int hashCode() {
        return file != null ? file.hashCode() : 0;
    }
} 