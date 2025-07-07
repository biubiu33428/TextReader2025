package com.my.textreader.util;

import android.content.ContentValues;
import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.my.textreader.bean.Cache;
import com.my.textreader.db.BookCatalogue;
import com.my.textreader.db.BookList;


import org.litepal.crud.DataSupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class BookUtil {
    private String cachedPath;
    //存储的字符数
    public static final int cachedSize = 30000;
//    protected final ArrayList<WeakReference<char[]>> myArray = new ArrayList<>();

    protected final ArrayList<Cache> myArray = new ArrayList<>();
    //目录
    private List<BookCatalogue> directoryList = new ArrayList<>();

    private String m_strCharsetName;
    private String bookName;
    private String bookPath;
    private long bookLen;
    private long position;
    private BookList bookList;
    private Context context;

    public BookUtil(Context context) {
        this.context = context;
        // 使用应用专用缓存目录
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Android 10+ 使用应用专用目录
            cachedPath = context.getExternalFilesDir("cache").getAbsolutePath() + "/";
        } else {
            // Android 9 及以下使用外部存储
            cachedPath = Environment.getExternalStorageDirectory() + "/treader/";
        }
        
        File file = new File(cachedPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }
    
    // 保持向后兼容的构造函数
    public BookUtil() {
        // 使用默认路径，但可能在新版本Android上有权限问题
        cachedPath = Environment.getExternalStorageDirectory() + "/treader/";
        File file = new File(cachedPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public synchronized void openBook(BookList bookList) throws IOException {
        this.bookList = bookList;
        
        // 检查是否是通过内容创建的书籍（bookpath为空或者有content字段）
        if (bookList.getContent() != null && !bookList.getContent().isEmpty()) {
            // 使用书籍内容创建缓存
            bookName = bookList.getBookname();
            bookPath = "content://" + bookList.getId(); // 使用特殊路径标识内容书籍
            cleanCacheFile();
            cacheBookFromContent(bookList.getContent());
        } else {
            // 传统的文件路径方式
            if (bookPath == null || !bookPath.equals(bookList.getBookpath())) {
                cleanCacheFile();
                this.bookPath = bookList.getBookpath();
                bookName = FileUtils.getFileName(bookPath);
                
                // 检查是否是加密文件
                if (bookPath.endsWith(".tre")) {
                    cacheEncryptedBook();
                } else {
                    cacheBook();
                }
            }
        }
    }

    private void cleanCacheFile() {
        File file = new File(cachedPath);
        if (!file.exists()) {
            file.mkdir();
        } else {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                files[i].delete();
            }
        }
    }

    public int next(boolean back) {
        position += 1;
        if (position > bookLen) {
            position = bookLen;
            return -1;
        }
        char result = current();
        if (back) {
            position -= 1;
        }
        return result;
    }

    public char[] nextLine() {
        if (position >= bookLen) {
            return null;
        }
        String line = "";
        while (position < bookLen) {
            int word = next(false);
            if (word == -1) {
                break;
            }
            char wordChar = (char) word;
            if ((wordChar + "").equals("\r") && (((char) next(true)) + "").equals("\n")) {
                next(false);
                break;
            }
            line += wordChar;
        }
        return line.toCharArray();
    }

    public char[] preLine() {
        if (position <= 0) {
            return null;
        }
        String line = "";
        while (position >= 0) {
            int word = pre(false);
            if (word == -1) {
                break;
            }
            char wordChar = (char) word;
            if ((wordChar + "").equals("\n") && (((char) pre(true)) + "").equals("\r")) {
                pre(false);
//                line = "\r\n" + line;
                break;
            }
            line = wordChar + line;
        }
        return line.toCharArray();
    }

    //从内容字符串缓存书本
    private void cacheBookFromContent(String content) throws IOException {
        m_strCharsetName = "UTF-8"; // 内容字符串默认使用UTF-8编码
        
        int index = 0;
        bookLen = 0;
        directoryList.clear();
        myArray.clear();
        
        // 预处理内容，添加段落缩进
        String processedContent = content.replaceAll("\r\n+\\s*", "\r\n\u3000\u3000");
        processedContent = processedContent.replaceAll("\u0000", "");
        
        char[] contentChars = processedContent.toCharArray();
        int totalLength = contentChars.length;
        
        for (int start = 0; start < totalLength; start += cachedSize) {
            int end = Math.min(start + cachedSize, totalLength);
            int currentBlockSize = end - start;
            
            char[] buf = new char[currentBlockSize];
            System.arraycopy(contentChars, start, buf, 0, currentBlockSize);
            
            Cache cache = new Cache();
            cache.setSize(buf.length);
            cache.setData(new WeakReference<char[]>(buf));
            myArray.add(cache);
            bookLen += buf.length;
            
            // 保存到文件缓存
            try {
                File cacheBook = new File(fileName(index));
                if (!cacheBook.exists()) {
                    cacheBook.createNewFile();
                }
                final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName(index)), "UTF-16LE");
                writer.write(buf);
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Error during writing " + fileName(index));
            }
            
            index++;
        }
        
        new Thread() {
            @Override
            public void run() {
                getChapter();
            }
        }.start();
    }

    // 处理加密书籍文件
    private void cacheEncryptedBook() throws IOException {
        File file = new File(bookPath);
        if (!file.exists()) {
            throw new IOException("加密书籍文件不存在: " + bookPath);
        }
        
        try {
            // 读取文件内容
            String encryptedContent = FileUtils.readTextFile(bookPath);
            
            // 检查是否是加密文件
            if (!EncryptionUtil.isEncryptedFile(encryptedContent)) {
                throw new IOException("文件格式错误，不是有效的加密书籍文件");
            }
            
            // 移除文件头标识
            String encryptedData = EncryptionUtil.removeEncryptedHeader(encryptedContent);
            
            // 解密内容
            String decryptedContent = EncryptionUtil.decrypt(encryptedData);
            if (decryptedContent == null) {
                throw new IOException("解密失败，文件可能已损坏");
            }
            
            // 使用解密后的内容创建缓存
            cacheBookFromContent(decryptedContent);
            
        } catch (Exception e) {
            throw new IOException("读取加密书籍失败: " + e.getMessage());
        }
    }

    public char current() {
//        int pos = (int) (position % cachedSize);
//        int cachePos = (int) (position / cachedSize);
        int cachePos = 0;
        int pos = 0;
        int len = 0;
        for (int i = 0; i < myArray.size(); i++) {
            long size = myArray.get(i).getSize();
            if (size + len - 1 >= position) {
                cachePos = i;
                pos = (int) (position - len);
                break;
            }
            len += size;
        }

        char[] charArray = block(cachePos);
        return charArray[pos];
    }

    public int pre(boolean back) {
        position -= 1;
        if (position < 0) {
            position = 0;
            return -1;
        }
        char result = current();
        if (back) {
            position += 1;
        }
        return result;
    }

    public long getPosition() {
        return position;
    }

    public void setPostition(long position) {
        this.position = position;
    }

    //缓存书本
    private void cacheBook() throws IOException {
        if (TextUtils.isEmpty(bookList.getCharset())) {
            m_strCharsetName = FileUtils.getCharset(bookPath);
            if (m_strCharsetName == null) {
                m_strCharsetName = "utf-8";
            }
            ContentValues values = new ContentValues();
            values.put("charset", m_strCharsetName);
            DataSupport.update(BookList.class, values, bookList.getId());
        } else {
            m_strCharsetName = bookList.getCharset();
        }

        File file = new File(bookPath);
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file), m_strCharsetName);
        int index = 0;
        bookLen = 0;
        directoryList.clear();
        myArray.clear();
        while (true) {
            char[] buf = new char[cachedSize];
            int result = reader.read(buf);
            if (result == -1) {
                reader.close();
                break;
            }

            String bufStr = new String(buf);
//            bufStr = bufStr.replaceAll("\r\n","\r\n\u3000\u3000");
//            bufStr = bufStr.replaceAll("\u3000\u3000+[ ]*","\u3000\u3000");
            bufStr = bufStr.replaceAll("\r\n+\\s*", "\r\n\u3000\u3000");
//            bufStr = bufStr.replaceAll("\r\n[ {0,}]","\r\n\u3000\u3000");
//            bufStr = bufStr.replaceAll(" ","");
            bufStr = bufStr.replaceAll("\u0000", "");
            buf = bufStr.toCharArray();
            bookLen += buf.length;

            Cache cache = new Cache();
            cache.setSize(buf.length);
            cache.setData(new WeakReference<char[]>(buf));

//            bookLen += result;
            myArray.add(cache);
//            myArray.add(new WeakReference<char[]>(buf));
//            myArray.set(index,);
            try {
                File cacheBook = new File(fileName(index));
                if (!cacheBook.exists()) {
                    cacheBook.createNewFile();
                }
                final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName(index)), "UTF-16LE");
                writer.write(buf);
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Error during writing " + fileName(index));
            }
            index++;
        }

        new Thread() {
            @Override
            public void run() {
                getChapter();
            }
        }.start();
    }

    //获取章节
    public synchronized void getChapter() {
        try {
            long size = 0;
            for (int i = 0; i < myArray.size(); i++) {
                char[] buf = block(i);
                String bufStr = new String(buf);
                
                // 支持多种换行符格式
                String[] paragraphs = bufStr.split("[\r\n]+");
                
                for (String str : paragraphs) {
                    // 去除空白字符后检查
                    String trimmedStr = str.trim();
                    
                    // 扩展目录识别规则，支持更多格式
                    if (trimmedStr.length() > 0 && trimmedStr.length() <= 50 && isChapterTitle(trimmedStr)) {
                        BookCatalogue bookCatalogue = new BookCatalogue();
                        bookCatalogue.setBookCatalogueStartPos(size);
                        bookCatalogue.setBookCatalogue(trimmedStr);
                        bookCatalogue.setBookpath(bookPath);
                        directoryList.add(bookCatalogue);
                    }
                    
                    // 计算字符位置，考虑各种换行符情况
                    if (str.contains("\u3000\u3000")) {
                        size += str.length() + 2;
                    } else if (str.contains("\u3000")) {
                        size += str.length() + 1;
                    } else {
                        size += str.length() + 1; // 加1是为了换行符
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 判断是否为章节标题
    private boolean isChapterTitle(String str) {
        if (str.isEmpty()) return false;
        
        // 常见的章节标题格式
        String[] patterns = {
            ".*第.{1,8}章.*",      // 第一章、第二章等
            ".*第.{1,8}节.*",      // 第一节、第二节等
            ".*第.{1,8}回.*",      // 第一回、第二回等（古典小说）
            ".*第.{1,8}部.*",      // 第一部、第二部等
            ".*第.{1,8}卷.*",      // 第一卷、第二卷等
            ".*Chapter\\s*\\d+.*", // Chapter 1, Chapter 2等（英文）
            ".*第.{1,8}篇.*",      // 第一篇、第二篇等
            ".*序章.*",            // 序章
            ".*尾声.*",            // 尾声
            ".*楔子.*",            // 楔子
            ".*引子.*",            // 引子
            ".*后记.*",            // 后记
            ".*前言.*",            // 前言
            ".*目录.*"             // 目录
        };
        
        for (String pattern : patterns) {
            if (str.matches(pattern)) {
                return true;
            }
        }
        
        // 额外检查：纯数字章节（如 "1"、"2"等）
        if (str.matches("^\\d{1,3}$")) {
            return true;
        }
        
        // 检查是否以数字开头且长度较短（可能是章节标题）
        if (str.matches("^\\d+[\\s\\S]{0,20}$") && str.length() <= 20) {
            return true;
        }
        
        return false;
    }

    public List<BookCatalogue> getDirectoryList() {
        return directoryList;
    }

    public long getBookLen() {
        return bookLen;
    }

    protected String fileName(int index) {
        return cachedPath + bookName + index;
    }

    //获取书本缓存
    public char[] block(int index) {
        if (myArray.size() == 0) {
            return new char[1];
        }
        char[] block = myArray.get(index).getData().get();
        if (block == null) {
            try {
                File file = new File(fileName(index));
                int size = (int) file.length();
                if (size < 0) {
                    throw new RuntimeException("Error during reading " + fileName(index));
                }
                block = new char[size / 2];
                InputStreamReader reader =
                        new InputStreamReader(
                                new FileInputStream(file),
                                "UTF-16LE"
                        );
                if (reader.read(block) != block.length) {
                    throw new RuntimeException("Error during reading " + fileName(index));
                }
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException("Error during reading " + fileName(index));
            }
            Cache cache = myArray.get(index);
            cache.setData(new WeakReference<char[]>(block));
//            myArray.set(index, new WeakReference<char[]>(block));
        }
        return block;
    }

}
