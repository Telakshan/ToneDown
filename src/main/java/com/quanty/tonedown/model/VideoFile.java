package com.quanty.tonedown.model;

public class VideoFile {
    private String filePath;
    private String fileName;
    private String fileExtension;

    public VideoFile(String filePath, String fileName, String fileExtension) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileExtension = fileExtension;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

}
