package com.my.textreader.util;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class FileUtils {

    /**
     * 获取文件编码
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static String getCharset(String fileName) throws IOException {
        String charset;
        FileInputStream fis = new FileInputStream(fileName);
        byte[] buf = new byte[4096];
        // (1)
        UniversalDetector detector = new UniversalDetector(null);
        // (2)
        int nread;
        while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
        }
        // (3)
        detector.dataEnd();
        // (4)
        charset = detector.getDetectedCharset();
        // (5)
        detector.reset();
        return charset;
    }

    /**
     * 根据路径获取文件名
     *
     * @param pathandname
     * @return
     */
    public static String getFileName(String pathandname) {
        int start = pathandname.lastIndexOf("/");
        int end = pathandname.lastIndexOf(".");
        if (start != -1 && end != -1) {
            return pathandname.substring(start + 1, end);
        } else {
            return "";
        }

    }

    public static List<File> getSuffixFile(String filePath, String suffere) {
        List<File> files = new ArrayList<>();
        File f = new File(filePath);
        return getSuffixFile(files, f, suffere);
    }

    /**
     * 读取sd卡上指定后缀的所有文件
     *
     * @param files   返回的所有文件
     * @param f       路径(可传入sd卡路径)
     * @param suffere 后缀名称 比如 .gif
     * @return
     */
    public static List<File> getSuffixFile(List<File> files, File f, final String suffere) {
        if (!f.exists()) {
            return null;
        }

        File[] subFiles = f.listFiles();
        for (File subFile : subFiles) {
            if (subFile.isHidden()) {
                continue;
            }
            if (subFile.isDirectory()) {
                getSuffixFile(files, subFile, suffere);
            } else if (subFile.getName().endsWith(suffere)) {
                files.add(subFile);
            } else {
                //非指定目录文件 不做处理
            }
//            Log.e("filename",subFile.getName());
        }
        return files;
    }

    /**
     * 读取文本文件内容
     * 
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException
     */
    public static String readTextFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }
        
        StringBuilder content = new StringBuilder();
        BufferedReader reader = null;
        
        try {
            // 尝试检测文件编码
            String charset = getCharset(filePath);
            if (charset == null) {
                charset = "UTF-8"; // 默认使用UTF-8
            }
            
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        
        return content.toString();
    }

}
