package com.xiaomi.qabot.content.generator.htmltojson;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by Hunt Tang <tangmingming@xiaomi.com> on 4/26/17.
 */
public class BaiduBaikeHtmlToJson {
    private static final Logger logger = LoggerFactory.getLogger(BaiduBaikeHtmlToJson.class);

    public static void main(String[] args) throws Exception {
        System.exit(ToolRunner.run(new BaiduBaikeHtmlToJsonRunner(), args));
    }

    public static class BaiduBaikeHtmlToJsonRunner extends Configured implements Tool {
        @Override
        public int run(String[] args) throws Exception {
            final Job job = Job.getInstance(getConf(), getClass().getEnclosingClass().getSimpleName());
            final Configuration conf = job.getConfiguration();

            String input = conf.get("input");
            String output = conf.get("output");
            if (StringUtils.isBlank(input) || StringUtils.isBlank(output)) {
                logger.error("Hadoop args -Dinput and -Doutput must be specified!");
                System.exit(1);
            }

            job.setJarByClass(getClass());

            FileInputFormat.setInputPaths(job, input);
            FileOutputFormat.setOutputPath(job, new Path(output));
            FileOutputFormat.setCompressOutput(job, true);
            FileOutputFormat.setOutputCompressorClass(job, GzipCodec.class);

            MultipleOutputs.addNamedOutput(job, "entity", TextOutputFormat.class, Text.class, NullWritable.class);
            MultipleOutputs.addNamedOutput(job, "infoKey", TextOutputFormat.class, Text.class, NullWritable.class);
            MultipleOutputs.addNamedOutput(job, "unhandled", TextOutputFormat.class, Text.class, NullWritable.class);
            MultipleOutputs.addNamedOutput(job, "needCrawl", TextOutputFormat.class, Text.class, NullWritable.class);

            job.setInputFormatClass(TextInputFormat.class);
            LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);

            job.setMapperClass(BaiduBaikeHtmlToJsonMapper.class);
            job.setMapOutputKeyClass(Text.class);
            job.setMapOutputValueClass(Text.class);

            job.setReducerClass(BaiduBaikeHtmlToJsonReducer.class);
            job.setNumReduceTasks(1);

            return job.waitForCompletion(true) ? 0 : 1;
        }
    }

    public static class BaiduBaikeHtmlToJsonMapper extends Mapper<LongWritable, Text, Text, Text> {
        private enum Counter {ENTITY, BLANK, UNHANDLED, NAVIGATOR}

        private static final Pattern BLANK_REG = Pattern.compile("<h1 class=\"baikeLogo\">[\\p{Blank}]*百度百科错误页[\\p{Blank}]*</h1>");
        private static final Pattern COLLAPSE_REG = Pattern.compile("^(展开|收起)$");
        // \u00a0 is &nbsp; in html, \u3000 is ideographic space, while \ufeff is zero width no-break space (usually known as BOM)
        private static final Pattern INFO_KEY_REG = Pattern.compile("[\u00a0\u3000\ufeff\\p{Blank}:：]");
        private static final Pattern INFO_VALUE_REG = Pattern.compile("[\u00a0\u3000\\p{Blank}]+");

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String html = value.toString().trim();
            if (html.isEmpty()) {
                return;
            }

            // Whether it's a blank page
            if (BLANK_REG.matcher(html).find()) {
                context.getCounter(Counter.BLANK).increment(1);
                return;
            }

            Document doc = Jsoup.parse(html);

            // Whether it's a polysemant navigation page
            Elements polysemantElements = doc.select(".list-dot [href]");
            if (polysemantElements.size() > 0) {
                for (Element polysemantEle : polysemantElements) {
                    context.write(new Text("needCrawl"), new Text(polysemantEle.attr("href")));
                }
                context.getCounter(Counter.NAVIGATOR).increment(1);
                return;
            }

            // Try parse
            Set<String> infoKeySet = new HashSet<>();
            JsonObject jsonObj = parse(doc, infoKeySet);

            // Cannot parse this doc
            if (jsonObj == null) {
                context.write(new Text("unhandled"), value);
                context.getCounter(Counter.UNHANDLED).increment(1);
                return;
            }

            if (!jsonObj.has("summary") && !jsonObj.has("info") && !jsonObj.has("content")) {
                context.getCounter(Counter.BLANK).increment(1);
                return;
            }

            for (String infoKey : infoKeySet) {
                context.write(new Text("infoKey"), new Text(infoKey));
            }
            context.write(new Text("entity"), new Text(jsonObj.toString()));
            context.getCounter(Counter.ENTITY).increment(1);
        }

        private static JsonObject parse(Document doc, Set<String> keySet) {
            JsonObject jsonObj = new JsonObject();

            // Remove all useless texts
            for (Element useless : doc.select("sup,.sup-anchor,.title-prefix,.edit-icon,.lemma-album")) {
                useless.text("");
            }
            for (Element useless : doc.getElementsMatchingOwnText(COLLAPSE_REG)) {
                useless.text("");
            }

            // Extract title
            Elements titleElements = doc.select("[class~=main-content|feature_poster|feature-poster] h1");
            if (titleElements.size() != 1) {
                return null;
            }
            String title = titleElements.get(0).text().trim();
            jsonObj.addProperty("title", title);

            // Extract summary
            Elements summaryElements = doc.select(".lemma-summary");
            if (summaryElements.size() == 1) {
                String summary = summaryElements.get(0).text().trim();
                if (!summary.isEmpty()) {
                    jsonObj.addProperty("summary", summary);
                }
            }
            else if (summaryElements.size() > 1) {
                return null;
            }

            // Extract tag list
            Elements tagElements = doc.select(".taglist");
            if (tagElements.size() > 0) {
                JsonArray jsonTag = new JsonArray();
                for (Element tag : tagElements) {
                    jsonTag.add(new JsonPrimitive(tag.text()));
                }
                jsonObj.add("tag", jsonTag);
            }

            // Extract info box
            Elements infoElements = doc.select(".basic-info");
            if (infoElements.size() < 1) {
                infoElements = doc.select(".sWord-main");
                if (infoElements.size() < 1) {
                    infoElements = doc.select(".dl-baseinfo");
                    if (infoElements.size() < 1) {
                        infoElements = doc.select(".infoTable");
                    }
                }
            }

            JsonObject jsonInfo = new JsonObject();
            if (infoElements.size() > 0) {
                boolean hasError = false;
                String key = null;
                for (Element element : infoElements.select("dt,dd")) {
                    if (key == null) {
                        if (element.tagName().equals("dt")) {
                            key = element.text();
                        }
                        else {
                            hasError = true;
                            break;
                        }
                    }
                    else {
                        if (element.tagName().equals("dd")) {
                            key = INFO_KEY_REG.matcher(key).replaceAll("");
                            if (key.isEmpty()) {
                                key = null;
                                continue;
                            }
                            keySet.add(key);
                            jsonInfo.addProperty(key, INFO_VALUE_REG.matcher(element.text()).replaceAll(" "));
                            key = null;
                        }
                        else {
                            hasError = true;
                            break;
                        }
                    }
                }
                if (hasError) {
                    return null;
                }
            }

            Elements synonymElements = doc.select(".view-tip-panel");
            if (synonymElements.size() == 1) {
                Element synonymEle = synonymElements.get(0);
                Elements keyElements = synonymEle.select(".viewTip-icon");
                Elements valueElements = synonymEle.select(".viewTip-fromTitle");
                if (keyElements.size() == 1 && valueElements.size() == 1) {
                    String synonymKey = INFO_KEY_REG.matcher(keyElements.get(0).text()).replaceAll("");
                    String synonymValue = INFO_VALUE_REG.matcher(valueElements.get(0).text()).replaceAll(" ");
                    if (synonymKey.equals("同义词")) {
                        keySet.add(synonymKey);
                        jsonInfo.addProperty(synonymKey, synonymValue);
                    }
                    else {
                        return null;
                    }
                }
                else {
                    return null;
                }
            }
            else if (synonymElements.size() > 1) {
                return null;
            }

            if (jsonInfo.entrySet().size() > 0) {
                jsonObj.add("info", jsonInfo);
            }

            // Extract main content
            String headlineIndicator = "level-2";
            String contentIndicator = "para";
            Elements contentElements = doc.select(".main-content>[class~=para-title|para]");
            if (contentElements.size() < 1) {
                contentElements = doc.select(".main-content>.curTab>[class~=para-title|para]");
                if (contentElements.size() < 1) {
                    headlineIndicator = "headline-1";
                    contentElements = doc.select(".main-content>.curTab [class~=headline-1|para]");
                }
            }

            StringBuilder content = new StringBuilder();
            if (contentElements.size() < 1) {
                contentElements = doc.select(".main-content");
                if (contentElements.size() == 1) {
                    content.append(contentElements.get(0).ownText());
                }
            }
            else {
                boolean titleBegan = false;
                boolean textBegan = false;
                for (Element contentEle : contentElements) {
                    // Skip all para-title with level other than level-2
                    if (contentEle.hasClass(headlineIndicator)) {
                        if (titleBegan || textBegan) {
                            content.append('\u2063');
                        }
                        content.append(contentEle.text());
                        titleBegan = true;
                        textBegan = false;
                    }
                    else if (contentEle.hasClass(contentIndicator)) {
                        if (titleBegan) {
                            content.append('\u2063');
                        }
                        content.append(contentEle.text());
                        titleBegan = false;
                        textBegan = true;
                    }
                }
            }
            if (!content.toString().trim().isEmpty()) {
                jsonObj.addProperty("content", content.toString().trim());
            }

            return jsonObj;
        }
    }

    public static class BaiduBaikeHtmlToJsonReducer extends Reducer<Text, Text, Text, NullWritable> {
        private MultipleOutputs mo;

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text value : values) {
                mo.write(key.toString(), value.toString(), NullWritable.get());
            }
        }

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            mo = new MultipleOutputs<>(context);
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            mo.close();
        }
    }
}
