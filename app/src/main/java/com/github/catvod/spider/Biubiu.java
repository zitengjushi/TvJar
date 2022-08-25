package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttpUtil;
import com.github.catvod.utils.Misc;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Biubiu extends Spider {

    private String ext = null;
    private JSONObject rule = null;

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        String ua = getRuleVal("UserW", Misc.CHROME).trim();
        if (ua.isEmpty()) ua = Misc.CHROME;
        headers.put("User-Agent", ua);
        return headers;
    }

    @Override
    public void init(Context context, String extend) {
        super.init(context, extend);
        this.ext = extend;
    }

    @Override
    public String homeContent(boolean filter) {
        fetchRule();
        List<Class> classes = new ArrayList<>();
        String[] fenleis = getRuleVal("fenlei", "").split("#");
        for (String fenlei : fenleis) {
            String[] info = fenlei.split("\\$");
            classes.add(new Class(info[0], info[1]));
        }
        return Result.get().classes(classes).string();
    }

    @Override
    public String homeVideoContent() {
        fetchRule();
        if (getRuleVal("shouye").equals("1")) return "";
        List<Vod> videos = new ArrayList<>();
        String[] fenleis = getRuleVal("fenlei", "").split("#");
        for (String fenlei : fenleis) {
            String[] info = fenlei.split("\\$");
            Result result = category(info[1], "1");
            for (int i = 0; i < result.getList().size(); i++) {
                videos.add(result.getList().get(i));
                if (videos.size() >= 30) break;
            }
        }
        return Result.string(videos);
    }

    private Result category(String tid, String pg) {
        fetchRule();
        String webUrl = getRuleVal("url") + tid + pg + getRuleVal("houzhui");
        String html = fetch(webUrl);
        String parseContent = html;
        boolean shifouercijiequ = getRuleVal("shifouercijiequ").equals("1");
        if (shifouercijiequ) {
            String jiequqian = getRuleVal("jiequqian");
            String jiequhou = getRuleVal("jiequhou");
            parseContent = subContent(html, jiequqian, jiequhou).get(0);
        }
        String jiequshuzuqian = getRuleVal("jiequshuzuqian");
        String jiequshuzuhou = getRuleVal("jiequshuzuhou");
        List<Vod> videos = new ArrayList<>();
        ArrayList<String> jiequContents = subContent(parseContent, jiequshuzuqian, jiequshuzuhou);
        for (int i = 0; i < jiequContents.size(); i++) {
            try {
                String jiequContent = jiequContents.get(i);
                String title = subContent(jiequContent, getRuleVal("biaotiqian"), getRuleVal("biaotihou")).get(0);
                String pic = subContent(jiequContent, getRuleVal("tupianqian"), getRuleVal("tupianhou")).get(0);
                pic = Misc.fixUrl(webUrl, pic);
                String link = subContent(jiequContent, getRuleVal("lianjieqian"), getRuleVal("lianjiehou")).get(0);
                Vod vod = new Vod();
                vod.setVodId(title + "$$$" + pic + "$$$" + link);
                vod.setVodName(title);
                vod.setVodPic(pic);
                videos.add(vod);
            } catch (Exception e) {
                break;
            }
        }
        return Result.get().vod(videos);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        return category(tid, pg).toString();
    }

    @Override
    public String detailContent(List<String> ids) {
        fetchRule();
        String[] idInfo = ids.get(0).split("\\$\\$\\$");
        String webUrl = getRuleVal("url") + idInfo[2];
        String html = fetch(webUrl);
        String parseContent = html;

        boolean bfshifouercijiequ = getRuleVal("bfshifouercijiequ").equals("1");
        if (bfshifouercijiequ) {
            String jiequqian = getRuleVal("bfjiequqian");
            String jiequhou = getRuleVal("bfjiequhou");
            parseContent = subContent(html, jiequqian, jiequhou).get(0);
        }

        List<String> playList = new ArrayList<>();
        List<String> playFrom = new ArrayList<>();

        String jiequshuzuqian = getRuleVal("bfjiequshuzuqian");
        String jiequshuzuhou = getRuleVal("bfjiequshuzuhou");
        boolean bfyshifouercijiequ = getRuleVal("bfyshifouercijiequ").equals("1");
        ArrayList<String> jiequContents = subContent(parseContent, jiequshuzuqian, jiequshuzuhou);

        for (int i = 0; i < jiequContents.size(); i++) {
            try {
                String jiequContent = jiequContents.get(i);
                String parseJqContent = bfyshifouercijiequ ? subContent(jiequContent, getRuleVal("bfyjiequqian"), getRuleVal("bfyjiequhou")).get(0) : jiequContent;
                ArrayList<String> lastParseContents = subContent(parseJqContent, getRuleVal("bfyjiequshuzuqian"), getRuleVal("bfyjiequshuzuhou"));
                List<String> vodItems = new ArrayList<>();
                for (int j = 0; j < lastParseContents.size(); j++) {
                    String title = subContent(lastParseContents.get(j), getRuleVal("bfbiaotiqian"), getRuleVal("bfbiaotihou")).get(0);
                    String link = subContent(lastParseContents.get(j), getRuleVal("bflianjieqian"), getRuleVal("bflianjiehou")).get(0);
                    vodItems.add(title + "$" + link);
                }
                playList.add(TextUtils.join("#", vodItems));
            } catch (Exception e) {
                break;
            }
        }

        Vod video = new Vod();
        video.setVodId(ids.get(0));
        video.setVodName(idInfo[0]);
        video.setVodPic(idInfo[1]);
        for (int i = 0; i < playList.size(); i++) playFrom.add("播放列表" + (i + 1));
        video.setVodPlayFrom(TextUtils.join("$$$", playFrom));
        video.setVodPlayUrl(TextUtils.join("$$$", playList));
        return Result.string(video);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        fetchRule();
        String webUrl = getRuleVal("url") + id;
        return Result.get().parse().url(webUrl).string();
    }

    @Override
    public String searchContent(String key, boolean quick) {
        try {
            fetchRule();
            boolean ssmoshiJson = getRuleVal("ssmoshi").equals("0");
            String webUrlTmp = getRuleVal("url") + getRuleVal("sousuoqian") + key + getRuleVal("sousuohou");
            String webUrl = webUrlTmp.split(";")[0];
            String webContent = webUrlTmp.contains(";post") ? fetchPost(webUrl) : fetch(webUrl);
            List<Vod> videos = new ArrayList<>();
            if (ssmoshiJson) {
                JSONObject data = new JSONObject(webContent);
                JSONArray vodArray = data.getJSONArray("list");
                for (int j = 0; j < vodArray.length(); j++) {
                    JSONObject vod = vodArray.getJSONObject(j);
                    String name = vod.optString(getRuleVal("jsname")).trim();
                    String id = vod.optString(getRuleVal("jsid")).trim();
                    String pic = vod.optString(getRuleVal("jspic")).trim();
                    pic = Misc.fixUrl(webUrl, pic);
                    Vod video = new Vod();
                    video.setVodId(name + "$$$" + pic + "$$$" + getRuleVal("sousuohouzhui") + id);
                    video.setVodName(name);
                    video.setVodPic(pic);
                    videos.add(video);
                }
            } else {
                String parseContent = webContent;
                boolean shifouercijiequ = getRuleVal("sousuoshifouercijiequ").equals("1");
                if (shifouercijiequ) {
                    String jiequqian = getRuleVal("ssjiequqian");
                    String jiequhou = getRuleVal("ssjiequhou");
                    parseContent = subContent(webContent, jiequqian, jiequhou).get(0);
                }
                String jiequshuzuqian = getRuleVal("ssjiequshuzuqian");
                String jiequshuzuhou = getRuleVal("ssjiequshuzuhou");
                ArrayList<String> jiequContents = subContent(parseContent, jiequshuzuqian, jiequshuzuhou);
                for (int i = 0; i < jiequContents.size(); i++) {
                    try {
                        String jiequContent = jiequContents.get(i);
                        String title = subContent(jiequContent, getRuleVal("ssbiaotiqian"), getRuleVal("ssbiaotihou")).get(0);
                        String pic = subContent(jiequContent, getRuleVal("sstupianqian"), getRuleVal("sstupianhou")).get(0);
                        pic = Misc.fixUrl(webUrl, pic);
                        String link = subContent(jiequContent, getRuleVal("sslianjieqian"), getRuleVal("sslianjiehou")).get(0);
                        Vod video = new Vod();
                        video.setVodId(title + "$$$" + pic + "$$$" + link);
                        video.setVodName(title);
                        video.setVodPic(pic);
                        videos.add(video);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
            return Result.string(videos);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void fetchRule() {
        try {
            if (rule != null) return;
            if (ext == null) return;
            if (ext.startsWith("http")) rule = new JSONObject(OkHttpUtil.string(ext, null));
            else rule = new JSONObject(ext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String fetch(String webUrl) {
        return OkHttpUtil.string(webUrl, getHeaders()).replaceAll("\r|\n", "");
    }

    private String fetchPost(String webUrl) {
        return OkHttpUtil.post(webUrl).replaceAll("\r|\n", "");
    }

    private String getRuleVal(String key, String defaultVal) {
        String v = rule.optString(key);
        if (v.isEmpty() || v.equals("空")) return defaultVal;
        return v;
    }

    private String getRuleVal(String key) {
        return getRuleVal(key, "");
    }

    private ArrayList<String> subContent(String content, String startFlag, String endFlag) {
        ArrayList<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(startFlag + "(.*?)" + endFlag);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) result.add(matcher.group(1));
        return result;
    }
}