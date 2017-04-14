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
    public static void main(String[] args) throws Exception {
        webCrawler();
    }

    private static void webCrawler() throws Exception {
        Pattern reg = Pattern.compile("[\r\n]");

        HttpRequestSender sender = new HttpRequestSender();
        HashMap<HttpRequestSender.ArgsKey, Object> requestArgs = new HashMap<>();
        requestArgs.put(HttpRequestSender.ArgsKey.TIMEOUT, 60 * 1000);

        try (FileWriter writer = new FileWriter("/Users/hunttang/Documents/baike.txt");
             FileWriter writerUnhandled = new FileWriter("/Users/hunttang/Documents/unhandled.txt");
             BufferedReader reader = new BufferedReader(new FileReader("/Users/hunttang/Documents/baikeWords.txt"))) {
            String word = reader.readLine();
            while (word != null) {
                word = word.trim();

                requestArgs.put(HttpRequestSender.ArgsKey.TYPE, HttpRequestSender.Type.GET);
                requestArgs.put(HttpRequestSender.ArgsKey.HOST, "baike.baidu.com");
                requestArgs.put(HttpRequestSender.ArgsKey.PATH, String.format("/item/%s", word));

                try (CloseableHttpResponse response = sender.send(requestArgs)) {
                    if (response == null || response.getStatusLine().getStatusCode() != 200) {
                        writerUnhandled.write(String.format("%s\n", word));
                        continue;
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    response.getEntity().writeTo(baos);
                    String line = reg.matcher(baos.toString()).replaceAll("");

                    writer.write(String.format("%s\n", line));
                }
                catch (Exception e) {
                    writerUnhandled.write(String.format("%s\n", word));
                    System.out.println(e.getMessage());
                }
                finally {
                    word = reader.readLine();
                }
            }
        }

        System.out.println("Finished.");
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
