package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Arrays;

public class DataToCSV {

	private static final String NEW_LINE_SEPARATOR = "\n";
	private static final String CAMP_STAT_HEADER = "index,day,segment,game,start,end,vidCoeff,mobCoeff,reach";
	private static final String STAT_DIR = "./data/statistics/";
	private static String curr_filename;
	private static int lastDayWithCamp = 0;
	private static int index = 0;
	private static String[] segments = new String[]{"OML","OMH","OFL","OFH","YML","YMH","YFL","YFH"};
	
	public static void createCSVFile(String filename, boolean append, String str){
		FileWriter fileWriter = null;
		File stat_dir = null;
		curr_filename = filename;
		
		try {
			new File(STAT_DIR).mkdirs();
			fileWriter = new FileWriter(STAT_DIR+filename, append);
			
			if (!append) {
				str = CAMP_STAT_HEADER.toString();
				index = 0;
			}
			
			fileWriter.append(str);
			fileWriter.append(NEW_LINE_SEPARATOR);
			index++;
		}
		catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
			e.printStackTrace();
		}
		finally {
			try {
				fileWriter.flush();
				fileWriter.close();
			}
			catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
			}
		}
	}
	
	
	public static String[] parseSegment(String segment){
		String[] arr = segment.split(" ");
		int power = (int)(Math.pow(2, (3-arr.length)));
		String[] output = new String[power];
		String type1 = "OY";
		String type2 = "MF";
		String type3 = "HL";
		
		for(int i = 0 ; i<arr.length; i++){
			if(arr[i].charAt(0)=='O')
				type1 = "O";
			
			if(arr[i].charAt(0)=='Y')
				type1 = "Y";
			
			if(arr[i].charAt(0)=='M')
				type2 = "M";
			
			if(arr[i].charAt(0)=='F')
				type2 = "F";

			if(arr[i].charAt(0)=='L')
				type3 = "L";

			if(arr[i].charAt(0)=='H')
				type3 = "H";
		}
		
		int j = 0;	
		for(int a=0; a < type1.length();a++)
			for(int b=0; b<type2.length();b++)
				for(int c=0; c<type3.length();c++){
					output[j] = "" + type1.charAt(a) + type2.charAt(b) + type3.charAt(c);
					j++;
				}
		return output;
	}
	
	public static void split_to_fields(String str, boolean flag){
		String mySplit = "Day\\s|Allocated campaign - Campaign ID |day | to | \\[|\\]|\\:\\s| coefs\\: \\(v=|\\, m=|\\)";
		// Day 0: Allocated campaign - Campaign ID 139707662: day 1 to 5 [LOW_INCOME, YOUNG], reach: 9540 coefs: (v=2.802671451241946, m=1.75187870014404)
		//[][curr_day][][cid][][day_start][day_end][segment][][reach][v_coef][m_coef]
		String[] tokens = str.split(mySplit);
		String[] parsedSegment = parseSegment(tokens[7]);
		if (flag)
			System.out.println("$$$$$$$$$$$$$$$$$$$$");
		
		for (String el : segments) {
			Boolean is_relevant = false;
			String line_to_write = null;
			for(String seg : parsedSegment){
				if (el.equals(seg)) {
					is_relevant = true;
					line_to_write = Integer.toString(index) + ',' + "-1" + ',' + seg + ',' + "1" + ',' + tokens[5] + ',' + tokens[6] +',' + tokens[10] +',' + tokens[11] + ',' + tokens[9];
					DataToCSV.createCSVFile(curr_filename, true, line_to_write);
				}
			}
			if (!is_relevant) {
				line_to_write = Integer.toString(index) + ',' + "-1" + ',' + el + ',' + "0" + ',' + "0" + ',' + "0" +',' + "0" +',' + "0" + ',' + "0";
				DataToCSV.createCSVFile(curr_filename, true, line_to_write);
			}
			
			if (flag)
				System.out.println(line_to_write);
		}	
	
		if (flag)
			System.out.println("$$$$$$$$$$$$$$$$$$$$");
		lastDayWithCamp++;
	}

/*	public static void updated_campaign_info(String cmpId , String stats , String costOfImpls){
		return;
	}*/
	
	public static void split_to_fields2(String str, Boolean flag){
		String mySplit = "Day\\s|Campaign opportunity - Campaign ID |day | to | \\[|\\]|\\:\\s| coefs\\: \\(v=|\\, m=|\\)";
		// Day 0: Campaign opportunity - Campaign ID 888159819: day 2 to 4 [HIGH_INCOME, OLD], reach: 2916 coefs: (v=2.82567156397492, m=2.339042977626665
		// [][curr_day][][cid][][day_start][day_end][segment][][reach][v_coef][m_coef]
		String[] tokens = str.split(mySplit);
		String[] parsedSegment = parseSegment(tokens[7]);
		
		DataToCSV.fillWithZeros(Integer.parseInt(tokens[1]));
		lastDayWithCamp = Integer.parseInt(tokens[1]);
		
		if (flag)
			System.out.println("&&&&&&&&");
		
		for (String el : segments) {
			Boolean is_relevant = false;
			String line_to_write = null;
			for(String seg : parsedSegment){
				if (el.equals(seg)) {
					is_relevant = true;
					line_to_write = Integer.toString(index) + ',' + tokens[1] + ',' + seg + ',' + "1" + ',' + tokens[5] + ',' + tokens[6] +',' + tokens[10] +',' + tokens[11] + ',' + tokens[9];
					DataToCSV.createCSVFile(curr_filename, true, line_to_write);
				}
			}
			if (!is_relevant) {
				line_to_write = Integer.toString(index) + ',' + tokens[1] + ',' + el + ',' + "0" + ',' + "0" + ',' + "0" +',' + "0" +',' + "0" + ',' + "0";
				DataToCSV.createCSVFile(curr_filename, true, line_to_write);
			}
			
			if (flag)
				System.out.println(line_to_write);
		}
		
		if (flag)
			System.out.println("&&&&&&&&");
		
		lastDayWithCamp++;
	}
	
	public static void fillWithZeros(int to){
		for (int i = lastDayWithCamp; i<to; i++) {
			for (String el : segments) {
				String line_to_write = Integer.toString(index) + ',' + Integer.toString(i) + ',' + el + ',' + "0" + ',' + "0" + ',' + "0" +',' + "0" +',' + "0" + ',' + "0";
				DataToCSV.createCSVFile(curr_filename, true, line_to_write);
			}
		}
	}
	
	public static void createCampStatistics() {
		FileWriter fileWriter = null;
		int countSimu = new File(STAT_DIR).list().length;

		HashMap<String, String> camp_stat_file = new HashMap<String,String>();
		
		
		for (int i = 1; i < 489; i++)
			camp_stat_file.put(Integer.toString(i), Integer.toString(i)+",0,0,0,0,0,0,0,0,0");
		
		try {
			fileWriter = new FileWriter("./data/campaign_statistics.csv", false);
			fileWriter.append(CAMP_STAT_HEADER.toString());
			fileWriter.append(NEW_LINE_SEPARATOR);
		}
		catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
			e.printStackTrace();
		}
		
		File folder = new File(STAT_DIR);
		for (File tempfile : folder.listFiles()) {
			if (tempfile.getName().startsWith("~"))
				countSimu--;
		}
		
		for (File tempfile : folder.listFiles()) {
			
			try(BufferedReader br = new BufferedReader(new FileReader(tempfile))) {
				String line;
				br.readLine();
			    while ((line = br.readLine()) != null) {
			    	String[] data = line.split(",");

			    	String index_line = camp_stat_file.get(data[0]);
			    	String[] line_data = index_line.split(",");
			    	System.out.println(index_line);
			    	line_data[1] = data[1];
			    	line_data[2] = data[2];
			    	line_data[3] = Integer.toString(Integer.parseInt(line_data[3])+1);
			    	
			    	int count = 0;
			    	
			    	if(Double.parseDouble(data[5]) != 0){
		    			count = Integer.parseInt(line_data[9]);
		    			line_data[9] = Integer.toString(count+1);
		    			for (int i = 4; i < 8; i++) {
		    				line_data[i] = Double.toString(((count*Double.parseDouble(line_data[i]))+Double.parseDouble(data[i]))/(count+1));
			    		
			    		}
			    	}
			    	
			    	line_data[8] = Double.toString(Double.parseDouble(line_data[8])+Double.parseDouble(data[8])/countSimu);
			    	
			    	String temp = line_data[0];
			    	for (int i = 1; i <= 9; i++) {
			    		temp = temp + "," +line_data[i];
			    	}
					camp_stat_file.put(line_data[0],temp);
			    }
			
			}
			catch (IOException e) {
				continue;
			}
	    }
		try {
			
			for (int i = 1; i < 489; i++){
				String curr = camp_stat_file.get(Integer.toString(i));
		    	fileWriter.append(curr);
				fileWriter.append(NEW_LINE_SEPARATOR);
			}
			
			fileWriter.flush();
			fileWriter.close();
		}
		catch (IOException e) {
			System.out.println("Error while flushing/closing fileWriter !!!");
			e.printStackTrace();
		}
	}
}