package MyKmeans;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class MapReduce {

	public static class Map extends Mapper<LongWritable, Text, IntWritable, Text> {

		// 中心集合
		ArrayList<ArrayList<Double>> centers = null;
		// 用k个中心
		int k = 0;

		// 读取中心
		protected void setup(Context context) throws IOException, InterruptedException {
			centers = Utils.getCentersFromHDFS(context.getConfiguration().get("centersPath"), false);
			k = centers.size();
		}

		/**
		 * 1.每次读取一条要分类的条记录与中心做对比，归类到对应的中心 2.以中心ID为key，中心包含的记录为value输出(例如： 1 0.2
		 * 。 1为聚类中心的ID，0.2为靠近聚类中心的某个值)
		 */
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			// 读取一行数据
			ArrayList<Double> fileds = Utils.textToArray(value);
			int sizeOfFileds = fileds.size();

			double minDistance = 99999999;
			int centerIndex = 0;

			// 依次取出k个中心点与当前读取的记录做计算
			for (int i = 0; i < k; i++) {
				double currentDistance = 0;
				for (int j = 1; j < sizeOfFileds; j++) {// 原文是j=0
					double centerPoint = centers.get(i).get(j);
					double filed = fileds.get(j);
					currentDistance += Math.pow((centerPoint - filed) / (centerPoint + filed), 2);
				}
				// 循环找出距离该记录最接近的中心点的ID
				if (currentDistance < minDistance) {
					minDistance = currentDistance;
					centerIndex = i;
				}
			}
			// 以中心点为Key 将记录原样输出
			context.write(new IntWritable(centerIndex + 1), value);
		}

	}

	// 利用reduce的归并功能以中心为Key将记录归并到一起
	public static class Reduce extends Reducer<IntWritable, Text, Text, Text> {

		/**
		 * 1.Key为聚类中心的ID value为该中心的记录集合 2.计数所有记录元素的平均值，求出新的中心
		 */
		protected void reduce(IntWritable key, Iterable<Text> value, Context context)
				throws IOException, InterruptedException {
			ArrayList<ArrayList<Double>> filedsList = new ArrayList<ArrayList<Double>>();

			// 依次读取记录集，每行为一个ArrayList<Double>
			for (Iterator<Text> it = value.iterator(); it.hasNext();) {
				ArrayList<Double> tempList = Utils.textToArray(it.next());
				filedsList.add(tempList);
			}

			// 计算新的中心
			// 每行的元素个数
			int filedSize = filedsList.get(0).size();
			double[] avg = new double[filedSize];
			for (int i = 1; i < filedSize; i++) {// 原文是i=0
				// 求每列的平均值
				double sum = 0;
				int size = filedsList.size();
				for (int j = 0; j < size; j++) {
					sum += filedsList.get(j).get(i);
				}
				avg[i] = sum / size;
			}
			context.write(new Text(""), new Text(Arrays.toString(avg).replace("[", "").replace("]", "")));
		}

	}

	@SuppressWarnings("deprecation")
	public static void run(String centerPath, String dataPath, String newCenterPath, boolean runReduce, int times)
			throws IOException, ClassNotFoundException, InterruptedException {

		Configuration conf = new Configuration();
		conf.set("centersPath", centerPath);

		Job job = new Job(conf, "My Kmeans :" + times + " time(s)");
		job.setJarByClass(MapReduce.class);

		job.setMapperClass(Map.class);

		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);

		if (runReduce) {
			// 最后依次输出不许要reduce
			job.setReducerClass(Reduce.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
		}
		FileInputFormat.addInputPath(job, new Path(dataPath));
		FileOutputFormat.setOutputPath(job, new Path(newCenterPath));
		System.out.println(job.waitForCompletion(true));
	}

	public static class FinalMap extends Mapper<LongWritable, Text, IntWritable, Text> {
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			// 读取一行数据
			int k = Integer.parseInt(value.toString().split(",")[0]);
			// 以中心点为Key 将记录原样输出
			context.write(new IntWritable(k), value);
		}
	}

	public static class FinalReduce extends Reducer<IntWritable, Text, Text, Text> {

		/**
		 * 1.Key为聚类中心的ID value为该中心的记录集合 2.计数所有记录元素的平均值，求出新的中心
		 */
		protected void reduce(IntWritable key, Iterable<Text> value, Context context)
				throws IOException, InterruptedException {
			ArrayList<ArrayList<Double>> filedsList = new ArrayList<ArrayList<Double>>();

			// 依次读取记录集，每行为一个ArrayList<Double>
			for (Iterator<Text> it = value.iterator(); it.hasNext();) {
				ArrayList<Double> tempList = Utils.textToArray(it.next());
				filedsList.add(tempList);
			}

			// 计算新的中心
			// 每行的元素个数
			int filedSize = filedsList.get(0).size();
			double[] avg = new double[filedSize];
			for (int i = 1; i < filedSize; i++) {// 原文是i=0
				// 求每列的平均值
				double sum = 0;
				int size = filedsList.size();
				for (int j = 0; j < size; j++) {
					sum += filedsList.get(j).get(i);
				}
				avg[i] = sum / size;
			}
			// 寻找最近的点
			int size = filedsList.size();
			double minDistance = 99999999;
			int minIndex = 0;
			for (int i = 0; i < size; i++) {
				double currentDistance = 0;
				for (int j = 1; j < filedSize; j++) {
					currentDistance += Math.pow(avg[j] - filedsList.get(i).get(j), 2);
				}
				// 循环找出距离该记录最接近的中心点的ID
				if (currentDistance < minDistance) {
					minDistance = currentDistance;
					minIndex = i;
				}
			}
			context.write(new Text(String.valueOf(key)),
					new Text(Arrays.toString(filedsList.get(minIndex).toArray()).replace("[", "").replace("]", "")));
			// context.write(new Text(""), new
			// Text(Arrays.toString(avg).replace("[", "").replace("]", "")));
		}

	}

	@SuppressWarnings("deprecation")
	public static void Finalrun(String centerPath, String dataPath, String newCenterPath)
			throws IOException, ClassNotFoundException, InterruptedException {

		Configuration conf = new Configuration();
		Job job = new Job(conf, "My Kmeans: final");
		job.setJarByClass(MapReduce.class);
		job.setMapperClass(FinalMap.class);
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setReducerClass(FinalReduce.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		FileInputFormat.addInputPath(job, new Path(dataPath));

		FileOutputFormat.setOutputPath(job, new Path(newCenterPath));

		System.out.println(job.waitForCompletion(true));
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException, InterruptedException {
		String centerPath = "hdfs://master:9000/input/centers.txt";
		String dataPath = "hdfs://master:9000/input/wine.txt";
		String newCenterPath = "hdfs://master:9000/out/kmean";

		int count = 0;

		while (true) {
			run(centerPath, dataPath, newCenterPath, true, count);
			System.out.println("compute " + ++count);
			if (Utils.compareCenters(centerPath, newCenterPath)) {
				run(centerPath, dataPath, newCenterPath + "/data", false, count);
				break;
			}
		}
		Finalrun(centerPath, dataPath, newCenterPath + "/result");
	}

}