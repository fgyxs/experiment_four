import java.io.IOException;
import java.util.Random;
import java.util.StringTokenizer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.map.InverseMapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class IndustryMapperReducer {
    private static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {
        private final static IntWritable one = new IntWritable(1);
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line[]=value.toString().split(",");
            context.write(new Text(line[10]),one);
        }
    }

    private static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();
        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    private static class IntWritableDecreasingComparator extends IntWritable.Comparator {
        public int compare(WritableComparable a, WritableComparable b) {
            return -super.compare(a, b);
        }

        public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
            return -super.compare(b1, s1, l1, b2, s2, l2);
        }
    }

    public static class SortMapper extends Mapper<Object, Text, IntWritable, Text>{
        public void map(Object key, Text value, Mapper<Object, Text, IntWritable, Text>.Context context) throws IOException, InterruptedException {
            String line[]=value.toString().split("\t");
            System.out.println(line[0]);
            context.write(new IntWritable(Integer.parseInt(line[1])),new Text(line[0]));
            System.out.println("ppp");
        }
    }

    private static class ShowReducer extends Reducer<IntWritable, Text, Text, IntWritable> {
        private IntWritable result = new IntWritable();
        public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            String name = "name";
            for (Text val : values) {
                name = val.toString();
            }
            context.write(new Text(name), key);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: wordcount <in> <out>");
            System.exit(2);
        }

        Path tempDir=new Path("tmpforsingle");
        Job job = new Job(conf, "word count");
        job.setJarByClass(IndustryMapperReducer.class);
        try {
            job.setMapperClass(TokenizerMapper.class);
            job.setCombinerClass(IntSumReducer.class);
            job.setInputFormatClass(TextInputFormat.class);
            job.setReducerClass(IntSumReducer.class);
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(IntWritable.class);

            FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
            FileOutputFormat.setOutputPath(job, tempDir);

//            job.setOutputFormatClass(SequenceFileOutputFormat.class);
            if (job.waitForCompletion(true)) {
                Job sortJob = new Job(conf, "sort");
                sortJob.setJarByClass(IndustryMapperReducer.class);
                sortJob.setInputFormatClass(TextInputFormat.class);
                FileInputFormat.addInputPath(sortJob, tempDir);
//                sortJob.setInputFormatClass(SequenceFileInputFormat.class);
                sortJob.setMapperClass(SortMapper.class);
                sortJob.setNumReduceTasks(1);
                sortJob.setReducerClass(ShowReducer.class);
                FileOutputFormat.setOutputPath(sortJob, new Path(otherArgs[1]));

                sortJob.setOutputKeyClass(IntWritable.class);
                sortJob.setOutputValueClass(Text.class);

                sortJob.setSortComparatorClass(IntWritableDecreasingComparator.class);

                System.exit(sortJob.waitForCompletion(true) ? 0 : 1);

            }

        } finally {
            FileSystem.get(conf).deleteOnExit(tempDir);
        }
    }
}