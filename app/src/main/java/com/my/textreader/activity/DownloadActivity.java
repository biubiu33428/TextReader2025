package com.my.textreader.activity;

import android.os.AsyncTask;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.my.textreader.R;
import com.my.textreader.base.BaseActivity;
import com.my.textreader.bean.DownloadableBook;
import com.my.textreader.db.BookList;
import com.my.textreader.db.PurchaseRecord;

import org.litepal.crud.DataSupport;

import java.util.List;

public class DownloadActivity extends BaseActivity {

    private Toolbar toolbar;
    private TextView tvBookInfo;
    private TextView tvFileSize;
    private ProgressBar progressBar;
    private TextView tvProgress;
    private TextView tvDownloadStatus;
    private Button btnClose;

    private DownloadableBook book;
    private boolean isDownloading = false;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_download;
    }

    @Override
    protected void initData() {
        toolbar = findViewById(R.id.toolbar);
        tvBookInfo = findViewById(R.id.tv_book_info);
        tvFileSize = findViewById(R.id.tv_file_size);
        progressBar = findViewById(R.id.progress_bar);
        tvProgress = findViewById(R.id.tv_progress);
        tvDownloadStatus = findViewById(R.id.tv_download_status);
        btnClose = findViewById(R.id.btn_close);

        setSupportActionBar(toolbar);
        toolbar.setTitle("下载书籍");
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 获取传递的书籍信息
        book = (DownloadableBook) getIntent().getSerializableExtra("book");
        if (book != null) {
            tvBookInfo.setText(book.getBookName() + "\n作者：" + book.getAuthor());
            tvFileSize.setText("文件大小：" + formatFileSize(book.getFileSize()));
            
            // 自动开始下载
            startDownload();
        }

        btnClose.setVisibility(View.GONE);
    }

    @Override
    protected void initListener() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDownloading) {
                    finish();
                }
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    private void startDownload() {
        isDownloading = true;
        tvDownloadStatus.setText("正在下载...");
        progressBar.setProgress(0);
        tvProgress.setText("0%");

        // 模拟下载过程
        simulateDownload();
    }

    private void simulateDownload() {
        new AsyncTask<Void, Integer, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    // 根据书籍ID生成模拟内容
                    String content = generateBookContent(book);
                    
                    // 模拟下载进度
                    for (int i = 0; i <= 100; i += 5) {
                        if (isCancelled()) break;
                        
                        publishProgress(i);
                        Thread.sleep(200); // 模拟下载延迟
                    }
                    
                    return content;
                } catch (InterruptedException e) {
                    return null;
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                int progress = values[0];
                progressBar.setProgress(progress);
                tvProgress.setText(progress + "%");
                
                if (progress < 30) {
                    tvDownloadStatus.setText("连接服务器...");
                } else if (progress < 60) {
                    tvDownloadStatus.setText("正在下载...");
                } else if (progress < 90) {
                    tvDownloadStatus.setText("下载中...");
                } else {
                    tvDownloadStatus.setText("处理文件...");
                }
            }

            @Override
            protected void onPostExecute(String content) {
                if (content != null) {
                    onDownloadComplete(content);
                } else {
                    onDownloadFailed();
                }
            }
        }.execute();
    }

    private String generateBookContent(DownloadableBook book) {
        // 根据书籍生成模拟内容
        StringBuilder content = new StringBuilder();
        
        switch (book.getBookId()) {
            case "book_001": // 三国演义
                content.append("第一回 宴桃园豪杰三结义 斩黄巾英雄首立功\n\n");
                content.append("    话说天下大势，分久必合，合久必分。周末七国分争，并入于秦。及秦灭之后，楚、汉分争，又并入于汉。汉朝自高祖斩白蛇而起义，一统天下，后来光武中兴，传至献帝，遂分为三国。\n\n");
                content.append("    推其致乱之由，殆始于桓、灵二帝。桓帝禁锢善类，崇信宦官。及桓帝崩，灵帝即位，大将军窦武、太傅陈蕃共相辅佐。时有宦官曹节等弄权，窦武、陈蕃谋诛之，机事不密，反为所害，中涓自此愈横。\n\n");
                break;
            case "book_002": // 水浒传
                content.append("第一回 张天师祈禳瘟疫 洪太尉误走妖魔\n\n");
                content.append("    话说故宋，哲宗皇帝在时，其时去仁宗天子已远，东京开封府汴梁宣和殿前太尉高俅的儿子高衙内...\n\n");
                break;
            case "book_003": // 西游记
                content.append("第一回 灵根育孕源流出 心性修持大道生\n\n");
                content.append("    盖闻天地之数，有十二万九千六百岁为一元。将一元分为十二会，乃子、丑、寅、卯、辰、巳、午、未、申、酉、戌、亥之十二支也。\n\n");
                break;
            case "book_004": // 红楼梦
                content.append("第一回 甄士隐梦幻识通灵 贾雨村风尘怀闺秀\n\n");
                content.append("    此开卷第一回也。作者自云：因曾历过一番梦幻之后，故将真事隐去，而借通灵之说，撰此《石头记》一书也。\n\n");
                break;
            case "book_005": // 平凡的世界
                content.append("第一部 第一章\n\n");
                content.append("    一九七五年二三月间，一个平平常常的日子，细蒙蒙的雨丝夹着一星半点的雪花，正纷纷淋淋地向大地飘洒着。\n\n");
                break;
            case "book_006": // 活着
                content.append("人是为活着本身而活着的，而不是为了活着之外的任何事物所活着。\n\n");
                content.append("    我比现在年轻十岁的时候，获得了一个游手好闲的职业，去乡间收集民间歌谣。那一年的整个夏天，我如同一只乱飞的麻雀，游荡在知了和阳光充斥的乡间。\n\n");
                break;
            default:
                content.append(book.getBookName()).append("\n\n");
                content.append("作者：").append(book.getAuthor()).append("\n\n");
                content.append(book.getDescription()).append("\n\n");
                content.append("这是一本精彩的书籍，为您带来愉快的阅读体验。\n\n");
        }
        
        // 智能生成更多章节内容 - 根据书籍类型生成不同内容
        generateIntelligentContent(content, book);
        
        return content.toString();
    }
    
    /**
     * 智能生成书籍内容 - 根据不同书籍类型生成相应的章节和内容
     */
    private void generateIntelligentContent(StringBuilder content, DownloadableBook book) {
        switch (book.getBookId()) {
            case "book_001": // 三国演义
                generateSanguoContent(content);
                break;
            case "book_002": // 水浒传
                generateShuihuContent(content);
                break;
            case "book_003": // 西游记
                generateXiyouContent(content);
                break;
            case "book_004": // 红楼梦
                generateHonglouContent(content);
                break;
            case "book_005": // 平凡的世界
                generatePingfanContent(content);
                break;
            case "book_006": // 活着
                generateHuozheContent(content);
                break;
            default:
                generateDefaultContent(content, book);
                break;
        }
    }
    
    /**
     * 生成三国演义的章节内容
     */
    private void generateSanguoContent(StringBuilder content) {
        String[] chapterTitles = {
            "第二回 张翼德怒鞭督邮 何国舅谋诛宦竖",
            "第三回 议温明董卓叱丁原 馈金珠李肃说吕布",
            "第四回 废汉帝陈留践位 谋董贼孟德献刀",
            "第五回 发矫诏诸镇应曹公 破关兵三英战吕布",
            "第六回 焚金阙董卓行凶 匿玉玺孙坚背约",
            "第七回 袁绍磐河战公孙 孙坚跨江击刘表",
            "第八回 王司徒巧使连环计 董太师大闹凤仪亭",
            "第九回 除暴凶吕布助司徒 犯长安李傕听贾诩",
            "第十回 勤王室马腾举义 报父仇曹操兴师"
        };
        
        String[] chapterContents = {
            "却说张飞见豹头环眼之人，知是张翼德也，便与之相见。原来此人姓张名飞，字翼德，涿郡人也。",
            "话说董卓字仲颖，陇西临洮人也，官拜河东太守，自来骄傲。",
            "且说董卓欲杀袁绍，李儒进曰：\"事未可定，不可妄杀。\"",
            "却说陈宫临欲下手杀曹操，忽转念曰：\"我为国家跟他到此，杀之不义。\"",
            "话说曹操当日对着关公赤兔马，看了一回，极其爱惜。",
            "却说孙坚被刘表围住，幸得程普、黄盖拼死杀出。",
            "话说蒙古当时见貂蝉在亭上，即来相偷看时，果然美貌。",
            "话说李傕、郭汜听了贾诩之言，遂不敢杀董卓，乃赦免之。",
            "却说李傕、郭汜争权不下，因此长安人民受苦无穷。"
        };
        
        for (int i = 0; i < chapterTitles.length; i++) {
            content.append(chapterTitles[i]).append("\n\n");
            content.append("    ").append(chapterContents[i]).append("正是英雄豪杰辈出之时，各路诸侯割据一方，天下大乱，群雄并起。\n\n");
            content.append("    此回书中详述各方势力之间的明争暗斗，展现了那个时代的风云变幻，人物形象栩栩如生，故事情节跌宕起伏。\n\n");
        }
    }
    
    /**
     * 生成水浒传的章节内容
     */
    private void generateShuihuContent(StringBuilder content) {
        String[] chapterTitles = {
            "第二回 王教头私走延安府 九纹龙大闹史家村",
            "第三回 史大郎夜走华阴县 鲁提辖拳打镇关西",
            "第四回 赵员外重修文殊院 鲁智深大闹五台山",
            "第五回 小霸王醉入销金帐 花和尚大闹桃花村",
            "第六回 九纹龙剪径赤松林 鲁智深火烧瓦罐寺",
            "第七回 花和尚倒拔垂杨柳 豹子头误入白虎堂",
            "第八回 林教头刺配沧州道 鲁智深大闹野猪林",
            "第九回 柴进门招天下客 林冲棒打洪教头",
            "第十回 林教头风雪山神庙 陆虞候火烧草料场"
        };
        
        String[] chapterContents = {
            "话说王进见了这个大虫一般的人，心中想道：\"莫不是史家村史进么？\"",
            "话说鲁达听了，跳起身来道：\"几个腌臜泼才，敢在此欺侮金翠莲！\"",
            "话说鲁智深离了桃花村，来到五台山，寻见智真长老。",
            "且说鲁智深离了桃花村，一路行来，但见山青水秀，景物非常。",
            "话说鲁智深走到瓦罐寺，见这寺已是败落不堪。",
            "话说林冲当日在东京时，是八十万禁军教头。",
            "话说当时薛霸、董超得了银两，又受了陆虞候嘱托。",
            "话说林冲当时住在柴进庄上，每日求见柴进，诉说前情。",
            "话说林冲打死陆虞候，雪夜上梁山。"
        };
        
        for (int i = 0; i < chapterTitles.length; i++) {
            content.append(chapterTitles[i]).append("\n\n");
            content.append("    ").append(chapterContents[i]).append("这正是乱世出英雄，各路豪杰因种种缘故，被逼上梁山。\n\n");
            content.append("    这回书讲述了好汉们的侠义精神和反抗精神，体现了古代人民对正义的向往和对压迫的反抗。\n\n");
        }
    }
    
    /**
     * 生成现代小说内容（平凡的世界等）
     */
    private void generatePingfanContent(StringBuilder content) {
        String[] chapterTitles = {
            "第二章 黄土高原的春天",
            "第三章 少安的抉择", 
            "第四章 润叶的心事",
            "第五章 城里的生活",
            "第六章 青春的迷茫",
            "第七章 爱情的萌芽",
            "第八章 现实的重压",
            "第九章 奋斗的足迹",
            "第十章 新的希望"
        };
        
        String[] chapterContents = {
            "春天来了，黄土高原上的冰雪开始融化，田地里泛起了绿意。",
            "少安面临着人生的重大选择，是继续在农村务农，还是外出打工？",
            "润叶心中藏着不为人知的秘密，那是青春岁月里最美好的回忆。", 
            "城市的生活虽然繁华，但对于农村来的孩子来说，处处都是挑战。",
            "年轻人总是充满理想，但现实往往比想象中要残酷得多。",
            "在那个贫穷的年代，爱情显得格外珍贵，也格外脆弱。",
            "生活的重担压在每个人的肩膀上，但人们依然要坚强地活下去。",
            "通过不懈的努力，人们终于看到了改变命运的希望。",
            "新的时代即将到来，一切都在悄悄地发生着变化。"
        };
        
        for (int i = 0; i < chapterTitles.length; i++) {
            content.append(chapterTitles[i]).append("\n\n");
            content.append("    ").append(chapterContents[i]).append("这是一个关于平凡人在平凡世界中不平凡生活的故事。\n\n");
            content.append("    作者以细腻的笔触描绘了那个时代人们的生活状态，展现了普通人面对困难时的坚韧与执着。每个人都在用自己的方式诠释着生活的意义。\n\n");
        }
    }
    
    /**
     * 生成其他书籍的默认内容
     */
    private void generateDefaultContent(StringBuilder content, DownloadableBook book) {
        String[] genericTitles = {
            "序章", "第一章 开端", "第二章 发展", "第三章 转折", 
            "第四章 高潮", "第五章 结局", "后记"
        };
        
        for (String title : genericTitles) {
            content.append(title).append("\n\n");
            content.append("    这是《").append(book.getBookName()).append("》中的精彩章节。");
            content.append("作者").append(book.getAuthor()).append("用细腻的笔触为我们描绘了一个生动的故事世界。\n\n");
            content.append("    ").append(book.getDescription()).append("每一个情节都紧扣主题，每一个角色都栩栩如生。\n\n");
        }
    }
    
    // 西游记、红楼梦、活着的内容生成方法
    private void generateXiyouContent(StringBuilder content) {
        String[] titles = {
            "第二回 悟彻菩提真妙理 断魔归本合元神",
            "第三回 四海千山皆拱伏 九幽十类尽除名", 
            "第四回 官封弼马心何足 名注齐天意未宁",
            "第五回 乱蟠桃大圣偷丹 反天宫诸神捉怪"
        };
        for (String title : titles) {
            content.append(title).append("\n\n");
            content.append("    话说孙悟空自从学得七十二变和筋斗云后，神通广大，无所不能。这回书中详述大圣的神通变化，以及天宫中的诸般奇事。\n\n");
        }
    }
    
    private void generateHonglouContent(StringBuilder content) {
        String[] titles = {
            "第二回 贾夫人仙逝扬州城 冷子兴演说荣国府",
            "第三回 贾雨村夤缘复旧职 林黛玉抛父进京都",
            "第四回 薄命女偏逢薄命郎 葫芦僧乱判葫芦案"
        };
        for (String title : titles) {
            content.append(title).append("\n\n");
            content.append("    这回书中详述贾府中的人情世故，以及各房各院的恩恩怨怨。曹雪芹以其独特的笔法，为我们展现了一个繁华而又没落的大观园。\n\n");
        }
    }
    
    private void generateHuozheContent(StringBuilder content) {
        String[] titles = {"童年时光", "青年岁月", "中年沧桑", "老年感悟"};
        for (String title : titles) {
            content.append(title).append("\n\n");
            content.append("    生活就像一条大河，有时平静，有时汹涌。每个人都在这条河里游泳，有的人被冲走了，有的人游到了岸边。\n\n");
        }
    }

    private void onDownloadComplete(String content) {
        isDownloading = false;
        
        // 保存书籍到本地数据库
        SaveBookTask task = new SaveBookTask(content);
        task.execute();
    }

    private void onDownloadFailed() {
        isDownloading = false;
        tvDownloadStatus.setText("下载失败，请重试");
        tvDownloadStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        btnClose.setVisibility(View.VISIBLE);
        Toast.makeText(this, "下载失败，请检查网络连接", Toast.LENGTH_LONG).show();
    }

    private class SaveBookTask extends AsyncTask<Void, Void, Boolean> {
        private String content;

        public SaveBookTask(String content) {
            this.content = content;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // 创建书籍记录
                BookList bookList = new BookList();
                bookList.setBookpath(""); // 内容型书籍无文件路径
                bookList.setBookname(book.getBookName());
                bookList.setContent(content); // 直接保存内容
                
                boolean saveResult = bookList.save();
                
                if (saveResult) {
                    // 更新购买记录为已下载
                    List<PurchaseRecord> records = DataSupport.where("bookId = ?", book.getBookId()).find(PurchaseRecord.class);
                    if (!records.isEmpty()) {
                        PurchaseRecord record = records.get(0);
                        record.setDownloaded(true);
                        record.update(record.getId());
                    }
                }
                
                return saveResult;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                tvDownloadStatus.setText("下载完成！");
                tvDownloadStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                tvProgress.setText("100%");
                
                Toast.makeText(DownloadActivity.this, "书籍已添加到书架！", Toast.LENGTH_LONG).show();
                
                // 延迟关闭页面
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 2000);
                
            } else {
                tvDownloadStatus.setText("保存失败，请重试");
                tvDownloadStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                btnClose.setVisibility(View.VISIBLE);
                Toast.makeText(DownloadActivity.this, "保存书籍失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!isDownloading) {
            super.onBackPressed();
        }
    }
} 