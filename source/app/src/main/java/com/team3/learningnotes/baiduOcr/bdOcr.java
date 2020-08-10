package com.team3.learningnotes.baiduOcr;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.baidu.aip.ocr.AipOcr;

import org.json.JSONArray;
import org.json.JSONObject;


import java.util.HashMap;
import java.util.List;

// 百度通用文字识别类
public class bdOcr {
    // TODO: 自行申请并设置APPID/AK/SK
    public static final String APP_ID = null;
    public static final String API_KEY = null;
    public static final String SECRET_KEY = null;

    public String GetText(String imagePath) {
        // 初始化一个AipOcr
        AipOcr client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);

        HashMap<String, String> options = new HashMap<String, String>();
        options.put("language_type", "CHN_ENG");
        options.put("detect_direction", "true");
        options.put("detect_language", "true");
        options.put("probability", "true");

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        // 调用接口
        String res = client.basicGeneral(imagePath, options).toString();


        try {
            Pattern patArray = Pattern.compile("words_result\":(.*)"); // 从结果中取数组
            Pattern patWords = Pattern.compile(",\"words\":\"(.*?)\"");
            Matcher mat = patArray.matcher(res);
            if (mat.find())
                res = mat.group(1);
            String ans = "";
            Matcher mat2 = patWords.matcher(res);
            while (mat2.find()) {
                String group = mat2.group();
                ans += "  ";
                ans += mat2.group(1);
                res = res.replace(group, "");
            }
            return ans;
        } catch (Exception e) {
            return "";
        }
    }
}