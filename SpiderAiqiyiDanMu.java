
package com.huqitong.spiderdanmu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSONObject;

public class SpiderAiqiyiDanMu {

	public static void main(String[] args) {
		String url = "http://www.iqiyi.com/v_19rrmm2vbk.html";
		List<String> res = getDanMu(url);
		for (String r : res) {
			System.out.println(r);
		}
	}

	public static List<String> getDanMu(String url) {
		//获取电影或者电视剧的tvId
		String res = httpGetRequest(url, false);
		String REGEX = "tvId:(.+?),";
		Pattern p = Pattern.compile(REGEX);
		Matcher m = p.matcher(res);
		String tvId = "";
		if (m.find()) {
			tvId = m.group(1);
		} else {
			return null;
		}
		// 根据tvId获取电影或者电视剧的详细信息。
		String infoUrl = "http://mixer.video.iqiyi.com/jp/mixin/videos/" + tvId;
		String info = httpGetRequest(infoUrl, false);
		info = info.replace("var tvInfoJs=", "");
		JSONObject jsonObject = JSONObject.parseObject(info);
		// 电影或电视剧专辑
		String albumId = jsonObject.getString("albumId");
		// 电影或电视剧频道
		String channelId = jsonObject.getString("channelId");
		// 电影或电视剧时长
		int duration = jsonObject.getIntValue("duration");
		return getDanMu(tvId, albumId, channelId, duration);
	}

	private static List<String> getDanMu(String tvId, String albumId, String channelId, int duration) {
		// 弹幕下载地址
		String IQIYI_GET_DANMU_URL = "http://cmts.iqiyi.com/bullet/%s/%s/%s_300_%s.z?rn=%s&business=danmu&is_iqiyi=true&is_video_page=true&tvid=%s&albumid=%s&categoryid=%s&qypid=01010021010000000000";
		List<String> danmus = new ArrayList<String>();
		// 根据电影或电视剧的时长计算出需要多少个批量弹幕请求，爱奇艺平均5分钟请求一次加载弹幕
		int page = duration / (60 * 5) + 1;
		for (int i = 1; i <= page; i++) {
			try {
				String t = "0000" + tvId;
				int length = t.length();
				String first = t.substring(length - 4, length - 2);
				String second = t.substring(length - 2);
				String rn = "0." + randomNum(16);
				String danmuUrl = String.format(IQIYI_GET_DANMU_URL, first, second, tvId, i, rn, tvId, albumId,
						channelId);
				String res = httpGetRequest(danmuUrl, true);
				if (res != null) {
					danmus.add(res);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return danmus;
	}

	// httpClient 请求
	private static String httpGetRequest(String url, boolean needUnzip) {
		CloseableHttpClient httpClient = HttpClients.custom().build();
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				if (needUnzip) {
					return new String(decompress(entity.getContent()));
				}
				return EntityUtils.toString(entity);
			}
		} catch (Exception e) {
		} finally {
			try {
				response.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	// 解压缩字节流
	private static byte[] decompress(InputStream is) {
		InflaterInputStream iis = new InflaterInputStream(is);
		ByteArrayOutputStream o = new ByteArrayOutputStream(1024);
		try {
			int i = 1024;
			byte[] buf = new byte[i];
			while ((i = iis.read(buf, 0, i)) > 0) {
				o.write(buf, 0, i);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return o.toByteArray();
	}

	// 构造随机参数
	private static String randomNum(double n) {
		double s = Math.pow(10, n - 1);
		double e = Math.pow(10, n) - 1;
		double res = Math.random() * (e - s) + s;
		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
		return nf.format(res);
	}
}
