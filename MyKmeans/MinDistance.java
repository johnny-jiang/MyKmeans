package MyKmeans;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MinDistance {

	String dataPath;
	String centersPath;
	ArrayList<ArrayList<Double>> data;
	ArrayList<ArrayList<Double>> center;
	double m;
	int data_size;
	static String split = ",";

	public MinDistance() throws IOException {
		dataPath = "wine.txt";
		centersPath = "centers.txt";
		data = null;
		center = new ArrayList<ArrayList<Double>>();
		m = 0.5;
		data_size = 0;
	}

	public boolean readData() {
		try {
			data = getCenters(dataPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		data_size = data.size();
		return true;

	}

	public boolean saveData() throws IOException {
		OutputStream os = new FileOutputStream(centersPath);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
		for (ArrayList<Double> d : center) {
			writer.write(ArrayToText(d));
		}
		writer.close();
		os.close();
		return true;
	}

	public static String ArrayToText(ArrayList<Double> data) {
		String list = "";
		for (double d : data)
			list += String.valueOf(d)+',';
		list=list.substring(0, list.length()-1)+'\n';
		return list;
	}

	public void readDataFromHDFS() throws IOException {
		data = Utils.getCentersFromHDFS(dataPath, false);
		data_size = data.size();
	}

	public String getDataPath() {
		return dataPath;
	}

	public void setDataPath(String dataPath) throws IOException {
		this.dataPath = dataPath;
		data = getCenters(dataPath);
		data_size = data.size();
	}

	public ArrayList<ArrayList<Double>> getCenter() {
		return center;
	}

	public double getM() {
		return m;
	}

	public void setM(double m) {
		this.m = m;
	}

	public ArrayList<ArrayList<Double>> getData() {
		return data;
	}

	public void setData(ArrayList<ArrayList<Double>> data) {
		this.data = data;
	}

	public void setCenter(ArrayList<ArrayList<Double>> center) {
		this.center = center;
	}

	public String getCenterPath() {
		return centersPath;
	}

	public void setCenterPath(String centerPath) {
		this.centersPath = centerPath;
	}

	static double computeDistance(ArrayList<Double> p1, ArrayList<Double> p2) {
		int d_size = p1.size();
		double currentDistance = 0;
		for (int i = 1; i < d_size; i++) {
			currentDistance += Math.pow(p1.get(i) - p2.get(i), 2);
		}
		return currentDistance;
	}

	// 读取中心文件的数据
	public static ArrayList<ArrayList<Double>> getCenters(String centersPath) throws IOException {
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
		String line = "";
		InputStream is = new FileInputStream(centersPath);
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		line = reader.readLine(); // 读取第一行
		while (line != null) {
			ArrayList<Double> tempList = textToArray(line);
			result.add(tempList);
			line = reader.readLine(); // 读取第一行
		}
		reader.close();
		is.close();
		return result;
	}

	public static ArrayList<Double> textToArray(String text) {
		ArrayList<Double> list = new ArrayList<Double>();
		String[] fileds = text.toString().split(split);
		for (int i = 0; i < fileds.length; i++) {
			list.add(Double.parseDouble(fileds[i]));
		}
		return list;
	}

	public int findCenters() {
		int d_size = data.size();
		double minDistance = 99999999;
		int centerIndex = 0;
		double maxDistance = 0;
		double totalDistance = 0;
		// 第一个中心点
		for (int i = 0; i < d_size; i++) {
			double currentDistance = 0;
			for (int j = 1; j < data.get(i).size(); j++)
				currentDistance += Math.pow(data.get(i).get(j), 2);
			// 循环找出距离原点最接近的中心点的ID
			if (currentDistance < minDistance) {
				minDistance = currentDistance;
				centerIndex = i;
			}
		}
		center.add(data.get(centerIndex));
		data.remove(centerIndex);
		d_size--;
		System.out.println("1:\n" + center.get(0).toString());
		// 第二个中心点
		minDistance = 99999999;
		maxDistance = 0;
		centerIndex = 0;
		for (int i = 0; i < d_size; i++) {
			double currentDistance = computeDistance(data.get(i), center.get(0));
			// 循环找出距离第一个中心点最远的中心点的ID
			if (currentDistance > maxDistance) {
				maxDistance = currentDistance;
				centerIndex = i;
			}
		}
		center.add(data.get(centerIndex));
		data.remove(centerIndex);
		d_size--;
		System.out.println("2:\n" + center.get(1).toString());

		// 第三个点
		minDistance = 99999999;
		maxDistance = 0;
		centerIndex = 0;
		for (int i = 0; i < d_size; i++) {
			double cDistance = 0;
			double mDistance = 0;
			double currentDistance = 99999999;
			for (ArrayList<Double> d : center) {
				cDistance = computeDistance(data.get(i), d);
				if (cDistance > mDistance) {
					mDistance = cDistance;
				}
			}
			currentDistance = mDistance;
			if (currentDistance > maxDistance) {
				maxDistance = currentDistance;
				centerIndex = i;
			}
		}
		if (maxDistance > m * computeDistance(center.get(0), center.get(1))) {
			center.add(data.get(centerIndex));
			data.remove(centerIndex);
		} else {
			System.out.println("only 2 points.");
			return 2;
		}
		System.out.println("3:\n" + center.get(2).toString());

		// 第三个中心点之外的点
		totalDistance = computeDistance(center.get(0), center.get(1)) + computeDistance(center.get(1), center.get(2));
		while (center.size() < (Math.sqrt(data_size) - 1)) {
			minDistance = 99999999;
			maxDistance = 0;
			centerIndex = 0;
			d_size = data.size();
			for (int i = 0; i < d_size; i++) {
				double cDistance = 0;
				double mDistance = 99999999;
				double currentDistance = 99999999;
				for (ArrayList<Double> d : center) {
					currentDistance = computeDistance(data.get(i), d);
					if (cDistance < mDistance) {
						mDistance = cDistance;
					}
				}
				currentDistance = mDistance;
				if (currentDistance > maxDistance) {
					maxDistance = currentDistance;
					centerIndex = i;
				}
			}
			if (maxDistance > m * (totalDistance / center.size())) {
				center.add(data.get(centerIndex));
				data.remove(centerIndex);
				totalDistance += computeDistance(center.get(center.size() - 2), center.get(center.size() - 1));
			} else {
				break;
			}

		}
		System.out.println("center points count: " + String.valueOf(center.size()) + " points.");

		return 0;
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		MinDistance m = new MinDistance();
		m.readData();
		m.findCenters();
		m.saveData();
	}

}
