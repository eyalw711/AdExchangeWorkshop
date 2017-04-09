package tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DataToCSV {
	private static final String COMMA_DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	private static final String CAMP_STAT_HEADER = "day,segment,cid,start,end,vidCoeff,mobCoeff,reach,publisher";
	private static final String UCS_STAT_HEADER = "game_number,day,active_networks,last_day_networks,OML,OMH,OFL,OFH,YML,YMH,YFL,YFH,l1_price,l2_price,l3_price,l4_price,l5_price,l6_price,l7_price";


	public static void createCSVFile(String path, boolean flag, String str){
		FileWriter fileWriter = null;
		
		try {
			fileWriter = new FileWriter(path, flag);
			
			if (!flag && path.contains("campaign_statistics.csv")) {
				str = CAMP_STAT_HEADER.toString();
			}
			
			if (!flag && path.contains("ucs_level_statistics.csv")) {
				str = UCS_STAT_HEADER.toString();
			}
			
			fileWriter.append(str);
			fileWriter.append(NEW_LINE_SEPARATOR);
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
		
		for(String seg : parsedSegment){
			String line_to_write = tokens[1] + ',' + seg + ',' + tokens[3] + ',' + tokens[5] + ',' + tokens[6] +',' + tokens[10] +',' + tokens[11] + ',' + tokens[9];
			DataToCSV.createCSVFile("./data/campaign_statistics.csv", true, line_to_write);
			
			// need to write line for probs
			
			
			if (flag)
				System.out.println(line_to_write);
		}
		if (flag)
			System.out.println("$$$$$$$$$$$$$$$$$$$$");
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
		if (flag)
			System.out.println("&&&&&&&&");
		for(String seg : parsedSegment){
			String line_to_write = tokens[1] + ',' + seg + ',' + tokens[3] + ',' + tokens[5] + ',' + tokens[6] +',' + tokens[10] +',' + tokens[11] + ',' + tokens[9];
			DataToCSV.createCSVFile("./data/campaign_statistics.csv", true, line_to_write);
			
			if (flag)
				System.out.println(line_to_write);
		}
		
		if (flag)
			System.out.println("&&&&&&&&");
	}
}
