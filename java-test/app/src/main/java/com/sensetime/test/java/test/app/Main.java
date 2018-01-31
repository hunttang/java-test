package com.sensetime.test.java.test.app;

import com.google.common.collect.Sets;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Primitives;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.sensetime.test.java.test.common.HttpRequestSender;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.poi.ss.usermodel.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.Transient;
import java.io.*;
import java.lang.reflect.Field;
import java.net.MulticastSocket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Hunt on 9/1/15.
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static class WebCrawler extends Thread {
        private int fileNumberStart;
        private int webPageNumberStart;
        private int webPageNumberEnd;
        private int webPageCountPerFile;

        public WebCrawler(int fileNumberStart, int webPageNumberStart, int webPageNumberEnd, int webPageCountPerFile) {
            super();
            init(fileNumberStart, webPageNumberStart, webPageNumberEnd, webPageCountPerFile);
        }

        public WebCrawler(int fileNumberStart, int webPageNumberStart, int webPageNumberEnd, int webPageCountPerFile, String name) {
            super(name);
            init(fileNumberStart, webPageNumberStart, webPageNumberEnd, webPageCountPerFile);
        }

        private void init(int fileNumberStart, int webPageNumberStart, int webPageNumberEnd, int webPageCountPerFile) {
            this.fileNumberStart = fileNumberStart;
            this.webPageNumberStart = webPageNumberStart;
            this.webPageNumberEnd = webPageNumberEnd;
            this.webPageCountPerFile = webPageCountPerFile;
        }

        @Override
        public void run() {
            Pattern reg = Pattern.compile("[\r\n]");
            Pattern errorReg = Pattern.compile("<h1 class=\"baikeLogo\">[\\p{Blank}]*百度百科错误页[\\p{Blank}]*</h1>");

            HttpRequestSender sender = new HttpRequestSender();
            HashMap<HttpRequestSender.ArgsKey, Object> requestArgs = new HashMap<>();
            requestArgs.put(HttpRequestSender.ArgsKey.TIMEOUT, 1000);
            requestArgs.put(HttpRequestSender.ArgsKey.TYPE, HttpRequestSender.Type.GET);
            requestArgs.put(HttpRequestSender.ArgsKey.HOST, "baike.baidu.com");

            try (FileWriter writerUnhandled = new FileWriter(String.format("D:\\Software\\baike\\unhandled-%s.txt", getName()), true)) {
                int fileCount = fileNumberStart;
                int succeedCount = 0;
                FileWriter writer = new FileWriter(String.format("D:\\Software\\baike\\baike%d.txt", fileCount++));
                for (int i = webPageNumberStart; i < webPageNumberEnd; ++i) {
                    if (i % 10000 == 0) {
                        System.out.println(String.format("%s: %s\tProcessed %d0k words.", getName(), DateTime.now().toString(), i / 1000));
                    }

                    requestArgs.put(HttpRequestSender.ArgsKey.PATH, String.format("/view/%d", i));
                    try (CloseableHttpResponse response = sender.send(requestArgs)) {
                        if (response == null || response.getStatusLine().getStatusCode() != 200) {
                            writerUnhandled.write(String.format("%d\n", i));
                            continue;
                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        response.getEntity().writeTo(baos);
                        String line = reg.matcher(baos.toString()).replaceAll("");

                        if (errorReg.matcher(line).find()) {
                            writerUnhandled.write(String.format("%d\n", i));
                            continue;
                        }

                        writer.write(String.format("%s\n", line));
                        if (++succeedCount % webPageCountPerFile == 0) {
                            writerUnhandled.flush();
                            try {
                                writer.close();
                                writer = new FileWriter(String.format("D:\\Software\\baike\\baike%d.txt", fileCount++));
                            }
                            catch (Exception e) {
                                System.out.println(String.format("%s: Fatal error! %s", getName(), e.getMessage()));
                            }
                        }
                    }
                    catch (Exception e) {
                        writerUnhandled.write(String.format("%d\n", i));
                    }
                }

                writer.close();
            }
            catch (IOException e) {
                System.out.println(String.format("%s: %s", getName(), e.getMessage()));
            }

            System.out.println(String.format("%s: finished!", getName()));
        }
    }

    private static class WebCrawlerBySeed extends Thread {
        private static final Pattern LINEFEED_REG = Pattern.compile("[\r\n]");
        private static final Pattern ERROR_REG = Pattern.compile(
                ".*<div[^>]+>知道宝贝找不到问题了&gt;_&lt;!!</div>\\p{Blank}*<div[^>]+>该问题可能已经失效。</div>.*");
        private static final Pattern ZHIDAO_URL_REG = Pattern.compile("wenwen.sogou.com/question/?\\?qid=[0-9]+");
        private static final Pattern ZHIDAO_URL_PATH_REG = Pattern.compile("/question/?\\?qid=[0-9]+.*");
        private static final Pattern NON_NUMBER_REG = Pattern.compile("[^0-9]");
        private static final Pattern NUMBER_REG = Pattern.compile("[0-9]");

        private final CountDownLatch countDownLatch;
        private final Map<HttpRequestSender.ArgsKey, Object> requestArgs = new HashMap<>();
        private final List<NameValuePair> params = new ArrayList<>(1);
        private final HttpRequestSender sender = new HttpRequestSender();
        private final Set<Long> seedSet = new HashSet<>();
        private final Set<Long> idSet;

        public WebCrawlerBySeed(String name, CountDownLatch countDownLatch, Set<Long> seedSet, Set<Long> idSet) {
            super(name);
            this.countDownLatch = countDownLatch;
            this.seedSet.addAll(seedSet);
            this.idSet = idSet;
        }

        @Override
        public void run() {
            System.out.println(String.format("%s: %s\tStarted.", getName(), DateTime.now().toString()));

            requestArgs.put(HttpRequestSender.ArgsKey.TIMEOUT, 1000);
            requestArgs.put(HttpRequestSender.ArgsKey.TYPE, HttpRequestSender.Type.GET);
            requestArgs.put(HttpRequestSender.ArgsKey.HOST, "zhidao.baidu.com");
            requestArgs.put(HttpRequestSender.ArgsKey.PATH, "/question/984812135248620299");
            //requestArgs.put(HttpRequestSender.ArgsKey.PARAM, params);
            params.add(new BasicNameValuePair("qid", "0"));

            int count = 0;
            while (!seedSet.isEmpty()) {
                Set<Long> curSet = new HashSet<>(seedSet);
                seedSet.clear();
                for (Long seed : curSet) {
                    if (++count % 1000 == 0) {
                        System.out.println(
                                String.format("%s: %s\tProcessed %dk seeds.", getName(), DateTime.now().toString(), count / 1000));
                        if (count > 10000) {
                            countDownLatch.countDown();
                            return;
                        }
                    }

                    /*
                    requestArgs.put(HttpRequestSender.ArgsKey.PATH, String.format("/api/qbpv", seed));
                    List<NameValuePair> params = new ArrayList<>(1);
                    params.add(new BasicNameValuePair("q", seed.toString()));
                    requestArgs.put(HttpRequestSender.ArgsKey.PARAM, params);
                    List<Header> headers = new ArrayList<>(1);
                    headers.add(new BasicHeader("Referer", String.format("https://zhidao.baidu.com/question/%d", seed)));
                    requestArgs.put(HttpRequestSender.ArgsKey.HEADER, headers);
                    try (CloseableHttpResponse response = sender.send(requestArgs)) {
                        if (response == null || response.getStatusLine().getStatusCode() != 200) {
                            seedSet.add(seed);
                            continue;
                        }

                        String result = IOUtils.toString(response.getEntity().getContent(), "GB18030").trim();
                        int pv = Integer.parseInt(result);
                        if (pv < 100) {
                            continue;
                        }
                    }
                    catch (IOException e) {
                        seedSet.add(seed);
                        continue;
                    }
                    finally {
                        requestArgs.remove(HttpRequestSender.ArgsKey.PATH);
                        requestArgs.remove(HttpRequestSender.ArgsKey.PARAM);
                        requestArgs.remove(HttpRequestSender.ArgsKey.HEADER);
                    }
                    */

                    params.set(0, new BasicNameValuePair("qid", seed.toString()));
                    try (CloseableHttpResponse response = sender.send(requestArgs)) {
                        if (response == null || response.getStatusLine().getStatusCode() != 200) {
                            seedSet.add(seed);
                            continue;
                        }

                        String html = LINEFEED_REG.matcher(IOUtils.toString(response.getEntity().getContent(), "GBK")).replaceAll("");
                        if (ERROR_REG.matcher(html).matches()) {
                            continue;
                        }

                        Document doc = Jsoup.parse(html);
                        Elements refElements = doc.select("[href]");
                        for (Element refEle : refElements) {
                            try {
                                String ref = URLDecoder.decode(refEle.attr("href"), "UTF-8").trim();
                                if (ZHIDAO_URL_REG.matcher(ref).find() || ZHIDAO_URL_PATH_REG.matcher(ref).matches()) {
                                    Matcher numberMatcher = NUMBER_REG.matcher(ref);
                                    numberMatcher.find();
                                    int start = numberMatcher.start();
                                    Matcher nonNumberMatcher = NON_NUMBER_REG.matcher(ref);
                                    int end = ref.length();
                                    if (nonNumberMatcher.find(start)) {
                                        end = nonNumberMatcher.start();
                                    }
                                    String idStr = ref.substring(start, end);
                                    Long id = new Long(idStr);
                                    if (!idSet.contains(id)) {
                                        synchronized (idSet) {
                                            if (!idSet.contains(id)) {
                                                idSet.add(id);
                                                seedSet.add(id);
                                            }
                                        }
                                    }
                                }
                            }
                            catch (UnsupportedEncodingException | IllegalArgumentException e) {
                                // URLDecoder exception, will do nothing, just skip
                            }
                        }
                    }
                    catch (IOException e) {
                        seedSet.add(seed);
                    }
                }
            }
            System.out.println(String.format("%s: finished!", getName()));
            countDownLatch.countDown();
        }
    }

    public static void main(String[] args) throws Exception {
        int i = 0;
        while (true) {
            LOGGER.info(String.format("%d", i++));
            Thread.sleep(100);
        }
    }

    private static void testEncryptedZipFile() throws IOException, ZipException {
        String filename = "/Users/hunt/Temp/testEncryptedZip.zip";

        ZipParameters zipParam = new ZipParameters();
        zipParam.setSourceExternalStream(true);
        zipParam.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        zipParam.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_ULTRA);
        zipParam.setEncryptFiles(true);
        zipParam.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
        zipParam.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
        zipParam.setPassword("sensetime_15811247942");

        try (FileOutputStream fos = new FileOutputStream(filename);
             net.lingala.zip4j.io.ZipOutputStream zos = new net.lingala.zip4j.io.ZipOutputStream(fos)) {
            zipParam.setFileNameInZip("abc/test");
            zos.putNextEntry(null, zipParam);
            zos.write(new byte[1]);
            zos.closeEntry();
            zos.finish();
        }
    }

    private static void testZipFile() throws IOException {
        try (FileOutputStream fos = new FileOutputStream("temp.zip");
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            ZipEntry ze = new ZipEntry("Image/imageSchema.json");
            FileInputStream fis = new FileInputStream("/Users/hunt/Temp/imageSchema.json");
            zos.putNextEntry(ze);
            IOUtils.copy(fis, zos);
            fis.close();

            ze = new ZipEntry("Label/taskSchema.json");
            fis = new FileInputStream("/Users/hunt/Temp/taskSchema.json");
            zos.putNextEntry(ze);
            IOUtils.copy(fis, zos);
            fis.close();
        }

        FileUtils.forceDelete(new File("temp.zip"));
    }

    private static void testStream() {
        final int count = 1000000;

        List<Integer> list = new ArrayList<>(count);
        Random random = new Random();
        for (int i = 0; i < count; ++i) {
            list.add(random.nextInt());
        }

        long start = System.nanoTime();
        list.parallelStream().count();
        long end = System.nanoTime();

        System.out.println(TimeUnit.NANOSECONDS.toMillis(end - start));
    }
}
