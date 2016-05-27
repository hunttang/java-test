package com.sensetime.test.java.test.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 * Created by Hunt on 8/24/15.
 */
public class Main {
    private static String hbaseZookeeperQuorum = "101.201.106.135";

    public static void main(String[] args) {
        Configuration hbaseConf = HBaseConfiguration.create();
        hbaseConf.set("hbase.zookeeper.quorum", hbaseZookeeperQuorum);
        try (Connection connection = ConnectionFactory.createConnection(hbaseConf);
             Table table = connection.getTable(TableName.valueOf("test"))) {
            Get get = new Get(Bytes.toBytes("9"));
            get.addFamily(Bytes.toBytes("pct"));
            Result result = table.get(get);
            String win = Bytes.toString(result.getValue(Bytes.toBytes("pct"), Bytes.toBytes("win")));
            System.out.println(win);

            Put put = new Put(Bytes.toBytes("1"));
            put.addColumn(Bytes.toBytes("pct"), Bytes.toBytes("3bet"), Bytes.toBytes((short) 30));
            put.addColumn(Bytes.toBytes("pct"), Bytes.toBytes("win"), Bytes.toBytes((short) 50));
            table.put(put);

            put = new Put(Bytes.toBytes("5"));
            put.addColumn(Bytes.toBytes("pct"), Bytes.toBytes("win"), Bytes.toBytes((short) 40));
            put.addColumn(Bytes.toBytes("pct"), Bytes.toBytes("entry"), Bytes.toBytes((short) 60));
            table.put(put);

            System.out.println("Done");
        }
        catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
