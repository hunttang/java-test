package com.sensetime.test.java.test.app;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.gson.*;
import com.sensetime.test.java.test.common.HttpRequestSender;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Hunt on 9/1/15.
 */
public class Main {
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
            Pattern errorReg = Pattern.compile(".*<h1 class=\"baikeLogo\">[\\p{Blank}]*百度百科错误页[\\p{Blank}]*</h1>.*");

            HttpRequestSender sender = new HttpRequestSender();
            HashMap<HttpRequestSender.ArgsKey, Object> requestArgs = new HashMap<>();
            requestArgs.put(HttpRequestSender.ArgsKey.TIMEOUT, 1000);

            try (FileWriter writerUnhandled = new FileWriter(String.format("D:\\Software\\baike\\unhandled-%s.txt", getName()), true)) {
                int fileCount = fileNumberStart;
                FileWriter writer = null;

                for (int i = webPageNumberStart; i < webPageNumberEnd; ++i) {
                    if (i % 1000 == 0) {
                        System.out.println(String.format("%s: %s\tProcessed %dk words.", getName(), DateTime.now().toString(), i / 1000));
                        if (i % webPageCountPerFile == 0) {
                            if (writer != null) {
                                writer.close();
                            }
                            writerUnhandled.flush();
                            writer = new FileWriter(String.format("D:\\Software\\baike\\baike%d.txt", fileCount++));
                        }
                    }

                    requestArgs.put(HttpRequestSender.ArgsKey.TYPE, HttpRequestSender.Type.GET);
                    requestArgs.put(HttpRequestSender.ArgsKey.HOST, "baike.baidu.com");
                    requestArgs.put(HttpRequestSender.ArgsKey.PATH, String.format("/view/%d", i));

                    try (CloseableHttpResponse response = sender.send(requestArgs)) {
                        if (response == null || response.getStatusLine().getStatusCode() != 200) {
                            writerUnhandled.write(String.format("%d\n", i));
                            continue;
                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        response.getEntity().writeTo(baos);
                        String line = reg.matcher(baos.toString()).replaceAll("");

                        if (errorReg.matcher(line).matches()) {
                            writerUnhandled.write(String.format("%d\n", i));
                            continue;
                        }
                        writer.write(String.format("%s\n", line));
                    }
                    catch (Exception e) {
                        writerUnhandled.write(String.format("%d\n", i));
                        System.out.println(String.format("%s: %s", getName(), e.getMessage()));
                    }
                }

                if (writer != null) {
                    writer.close();
                }
            }
            catch (IOException e) {
                System.out.println(String.format("%s: %s", getName(), e.getMessage()));
            }

            System.out.println(String.format("%s: finished!", getName()));
        }
    }

    private static class WebCrawlerByFile extends Thread {
        public WebCrawlerByFile(String name) {
            super(name);
        }

        @Override
        public void run() {
            Pattern reg = Pattern.compile("[\r\n]");
            Pattern errorReg = Pattern.compile(".*<h1 class=\"baikeLogo\">[\\p{Blank}]*百度百科错误页[\\p{Blank}]*</h1>.*");

            HttpRequestSender sender = new HttpRequestSender();
            HashMap<HttpRequestSender.ArgsKey, Object> requestArgs = new HashMap<>();
            requestArgs.put(HttpRequestSender.ArgsKey.TIMEOUT, 1000);
            requestArgs.put(HttpRequestSender.ArgsKey.TYPE, HttpRequestSender.Type.GET);
            requestArgs.put(HttpRequestSender.ArgsKey.HOST, "baike.baidu.com");

            try (FileWriter writer = new FileWriter(String.format("D:\\Software\\baike\\baike-%s.txt", getName()), true);
                 FileWriter writerUnhandled = new FileWriter(String.format("D:\\Software\\baike\\unhandled5-%s.txt", getName()), true);
                 BufferedReader reader = new BufferedReader(new FileReader(String.format("D:\\Software\\baike\\unhandled4-%s.txt", getName())))) {
                int count = 0;
                String line = reader.readLine();
                while (line != null) {
                    if (++count % 1000 == 0) {
                        System.out.println(String.format("%s: %s\tProcessed %dk words.", getName(), DateTime.now().toString(), count / 1000));
                    }

                    line = line.trim();
                    if (line.isEmpty() || line.length() > 8) {
                        line = reader.readLine();
                        continue;
                    }

                    requestArgs.put(HttpRequestSender.ArgsKey.PATH, String.format("/view/%s", line));

                    try (CloseableHttpResponse response = sender.send(requestArgs)) {
                        if (response == null || response.getStatusLine().getStatusCode() != 200) {
                            writerUnhandled.write(String.format("%s\n", line));
                            line = reader.readLine();
                            continue;
                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        response.getEntity().writeTo(baos);
                        String html = reg.matcher(baos.toString()).replaceAll("");

                        if (errorReg.matcher(html).matches()) {
                            line = reader.readLine();
                            continue;
                        }
                        writer.write(String.format("%s\n", html));
                        line = reader.readLine();
                    }
                    catch (Exception e) {
                        writerUnhandled.write(String.format("%s\n", line));
                        System.out.println(String.format("%s: %s", getName(), e.getMessage()));
                        line = reader.readLine();
                    }
                }
            }
            catch (IOException e) {
                System.out.println(String.format("%s: %s", getName(), e.getMessage()));
            }

            System.out.println(String.format("%s: finished!", getName()));
        }
    }

    public static void main(String[] args) throws Exception {
        WebCrawlerByFile webCrawler1 = new WebCrawlerByFile("crawler1");
        WebCrawlerByFile webCrawler2 = new WebCrawlerByFile("crawler2");
        WebCrawlerByFile webCrawler3 = new WebCrawlerByFile("crawler3");
        WebCrawlerByFile webCrawler4 = new WebCrawlerByFile("crawler4");
        WebCrawlerByFile webCrawler5 = new WebCrawlerByFile("crawler5");
        WebCrawlerByFile webCrawler6 = new WebCrawlerByFile("crawler6");

        webCrawler1.start();
        webCrawler2.start();
        webCrawler3.start();
        webCrawler4.start();
        webCrawler5.start();
        webCrawler6.start();
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
