package com.sensetime.test.java.test.app;

import com.google.gson.*;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Hunt on 9/1/15.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        String filename = "/Users/hunttang/Temp/temp.txt";
        BufferedReader reader = new BufferedReader(new FileReader(filename));

        List<Long> tsList = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            String line = reader.readLine();
            JsonArray jsonArray = new Gson().fromJson(line, JsonObject.class).getAsJsonArray("miuiMarks");
            for (JsonElement element : jsonArray) {
                if (element.getAsJsonObject().get("catId").getAsInt() != 7) {
                    continue;
                }
                JsonArray titles = element.getAsJsonObject().getAsJsonArray("catTitle");
                JsonArray timestamps = element.getAsJsonObject().getAsJsonArray("createTime");

                for (int j = 0; j < titles.size(); ++j) {
                    JsonElement titleEle = titles.get(j);
                    if (!titleEle.isJsonNull()) {
                        if (titleEle.getAsString().contains("骚扰")) {
                            tsList.add(timestamps.get(j).getAsLong());
                        }
                    }
                }
            }
        }

        Collections.sort(tsList);

        System.out.println(tsList.get(0));
        System.out.println(new Date(tsList.get(0)).toString());

        System.out.println(tsList.get(tsList.size() - 1));
        System.out.println(new Date(tsList.get(tsList.size() - 1)).toString());
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
