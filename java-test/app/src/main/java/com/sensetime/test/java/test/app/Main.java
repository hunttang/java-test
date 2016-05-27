package com.sensetime.test.java.test.app;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Hunt on 9/1/15.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        /*
        URIBuilder uri = new URIBuilder();

        uri.setScheme("http");
        uri.setHost("www.originpoker.com");
        uri.setPort(80);
        uri.setPath("/UpdateAPK/1_1.apk");

        HttpUriRequest httpRequest = new HttpGet(uri.build());
        */
        HttpUriRequest httpRequest = new HttpGet("http://www.originpoker.com/UpdateAPK/1_1.apk");

        CloseableHttpResponse response = httpClient.execute(httpRequest);

        System.out.println(response.getStatusLine());
        System.out.println(response.getEntity().getContentLength());
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
