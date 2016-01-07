package com.sensetime.test.java.test.spark;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import scala.Tuple2;

import java.util.Arrays;

/**
 * Created by Hunt on 8/24/15.
 */
public class Main {
    private static SparkConf sparkConf = new SparkConf().setAppName("SparkTest").setMaster("local");

    public static void main(String[] args) {

        sparkStreamingTest(args);
    }

    private static void sparkStreamingTest(String[] args) {
        JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, Durations.seconds(1));

        JavaReceiverInputDStream<String> data = ssc.socketTextStream("localhost", 9999);
        JavaDStream<String> words = data.flatMap((line) -> Arrays.asList(line.toLowerCase().split("[ \t\n]")));
        JavaPairDStream<String, Integer> wordCount = words.mapToPair((word) -> new Tuple2<>(word, 1)).reduceByKey((a, b) -> a + b);
        wordCount.print();

        ssc.start();
        ssc.awaitTermination();
    }

    private static void sparkBatchTest(String[] args) {
        JavaSparkContext sc = new JavaSparkContext(sparkConf);

        String input = args[0];
        String output = args[1];

        JavaRDD<String> data = sc.textFile(input);

        long sparkCount = data.filter((line) -> line.toLowerCase().contains("spark")).count();
        System.out.println("The count of the lines that contain \"spark\" is " + sparkCount);

        JavaRDD<String> words = data.flatMap((line) -> Arrays.asList(line.toLowerCase().split("[ \t\n]")));
        JavaPairRDD<String, Integer> wordCount = words.mapToPair((word) -> new Tuple2<>(word, 1)).reduceByKey((a, b) -> a + b);

        Configuration conf = new Configuration();
        conf.set("mapreduce.output.fileoutputformat.outputdir", output);
        conf.set("mapreduce.job.outputformat.class", TextOutputFormat.class.getCanonicalName());
        conf.set("mapreduce.job.output.key.class", Text.class.getCanonicalName());
        conf.set("mapreduce.job.output.value.class", IntWritable.class.getCanonicalName());
        conf.set("mapreduce.output.fileoutputformat.compress", "true");
        conf.set("mapreduce.output.fileoutputformat.compress.codec", GzipCodec.class.getCanonicalName());
        wordCount.saveAsNewAPIHadoopDataset(conf);

        sc.stop();
    }
}
