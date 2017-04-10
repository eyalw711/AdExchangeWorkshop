package collect_profit_data;
import java.util.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CollectProfitData {
	
	private static final String NEW_LINE_SEPARATOR = "\n";
	private static final String PROF_FILE_PATH = "/home/sleviim/Workspaces/PineApple/data/campaigns_profitability.csv";
	private static final String UCS_FILE_PATH = "/home/sleviim/Workspaces/PineApple/data/ucs_level_statistics.csv";

	private static final String CAMP_PROF_HEADER = "cid,sim,day,budget,start,end,vidCoeff,mobCoeff,reach,demand,OML,OMH,OFL,OFH,YML,YMH,YFL,YFH,completion_percentage,profit,revenue,budgetMillis,tartgetedImps,other,cost,ERR,decision";
	//	places in array:							0	1	 2	 	3	 4		5	6		7		8		9	  10  11 12	  13  14  15  16  17			18			19		20		21			22			23		24	 25	  26
	
	private static final String UCS_STAT_HEADER = "index,game_number,day,active_networks,last_day_networks,OML,OMH,OFL,OFH,YML,YMH,YFL,YFH,l1_price,l2_price,l3_price,l4_price,l5_price,l6_price,l7_price";
	// 	places in array:								0		1		2				3			  4	   5  6   7   8   9   10   11  12		13			14		15		16		17			18						

	// *** TO BE REMOVED ****
	///home/sleviim/Workspaces/PineApple/data/campaigns_profitability.csv
	///home/sleviim/Documents/university/sadna/sadna_workspace/adx-server/logs/logs

	static Scanner reader=new Scanner(System.in);
	
	public static void main(String[] args) throws IOException {
		System.out.println ("Generating statistics from server: Enter the 'from' simulation number:");
		int from = reader.nextInt();
		System.out.println ("Generating statistics from server: Enter the 'to' simulation number:");
		int to = reader.nextInt();
		System.out.println ("Generating statistics from server: Enter the path for the simulation log files (on server)");
		String logPath = reader.next();
	
		FileWriter fileWriter = null;
		FileWriter fileWriter2 = null;
		HashMap<String, CampaignStatus> dict = new HashMap<String,CampaignStatus>();
		HashMap<String, DayStatus> dictDays = new HashMap<String,DayStatus>();
		
		try {
			fileWriter = new FileWriter(PROF_FILE_PATH, false);
			fileWriter.append(CAMP_PROF_HEADER);
			fileWriter.append(NEW_LINE_SEPARATOR);
			
			fileWriter2 = new FileWriter(UCS_FILE_PATH, false);
			fileWriter2.append(UCS_STAT_HEADER);
			fileWriter2.append(NEW_LINE_SEPARATOR);
		}
		catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
			e.printStackTrace();
		}
		
		String currDay = null;
		String[] tmpStringArr;
		CampaignStatus camp;
		DayStatus dayStatus;
		int counter = -1;
		
		for(int i = from; i<=to; i++){
			//System.out.println(i);
			String currSim = logPath +"/localhost_SIM_"+ Integer.toString(i)+ ".log";
			File readFromFile = new File(currSim);
			
			try(BufferedReader br = new BufferedReader(new FileReader(readFromFile))) {
				String line;
			    while ((line = br.readLine()) != null) {
			    	if (line.contains("INFO sim.Simulation|***** START OF TIME") && !line.contains("COMPLETE")) {
			    		currDay = line.split("START OF TIME | \\*\\*\\*")[1];
			    		counter++;
			    	}
			    	
			    	else if (line.contains("Notifying new campaign opportunity: CampaignImpl")|| line.contains("Allocating initial campaign")) {
			    		tmpStringArr = line.split("CampaignImpl \\[id=|, reachImps=|, dayStart=|, dayEnd=|, targetSegment=\\[|\\], videoCoef=|, mobileCoef=|, budgetMillis=|, advertiser=|\\]");
			    		List<String> ucsBids = new ArrayList<String>();
			    		camp = new CampaignStatus();
			    		dayStatus = new DayStatus();
			    		
			    		camp.setGame(i);
			    		camp.setCid(tmpStringArr[1]);
			    		camp.setDay(currDay);
			    		
			    		camp.setBudgetMillis(tmpStringArr[8]);
			    		camp.setStart(tmpStringArr[3]);
			    		camp.setEnd(tmpStringArr[4]);
			    		camp.setVidCoeff(tmpStringArr[6]);
			    		camp.setMobCoeff(tmpStringArr[7]);
			    		camp.setReach(tmpStringArr[2]);
			    		camp.setSegments(tmpStringArr[5]);
			    		camp.setAgent(tmpStringArr[9]);
			    		dict.put(tmpStringArr[1], camp);
			    		
			    		if (!line.contains("Allocating initial campaign")) {
				    		dayStatus.setDay(currDay);
				    		dayStatus.setGame(i);
				    		dayStatus.setUcsLevels(ucsBids);
				    		dayStatus.setIndex(Integer.toString(counter));
				    		dictDays.put(Integer.toString(counter), dayStatus);
			    		}
			    	}
			    	
			    	else if (line.contains("Reporting campaign auction results... CampaignImpl")) {
			    		tmpStringArr = line.split("CampaignImpl \\[id=|, reachImps=|, budgetMillis=|, advertiser=|\\]");
			    		camp = dict.get(tmpStringArr[1]);
			    		
			    		if (tmpStringArr.length == 5) {
			    			camp.setAgent(null);
			    		}
			    		else {	
			    			camp.setAgent(tmpStringArr[5]);
			    		}
			    		camp.setBudgetMillis(tmpStringArr[4]);
			    	}
			    	
			    	else if (line.contains("COMPLETE *****")) {
		    			List<String> strucsBids = new ArrayList<String>();
			    		List<Double> ucsBidsDouble = new ArrayList<Double>();
			    		//String campid = null;
			    		
			    		line = br.readLine();
			    		if (line == null)
			    			break;
			    		
			    		while (!line.contains("***** START OF TIME") && line.contains("Got AdNetBidMessage from") && line != null) {
			    			String ucsbid = line.split("\\: UCS Bid\\: | Cmp ID")[1];
			    			//campid = line.split(" Cmp ID\\: | Cmp Bid\\:")[1];
			    			ucsBidsDouble.add(Double.parseDouble(ucsbid));
			    			line = br.readLine();
			    			
				    		if (line == null){
				    			break;
				    		}
			    		}
			    		
			    		int len = ucsBidsDouble.size();
		    			for (int j = 0; j < 8-len; j++) {
		    				ucsBidsDouble.add(0.0);
		    			}
			    		
		    			Collections.sort(ucsBidsDouble);
		    			Collections.reverse(ucsBidsDouble);
		    			
		    			dayStatus = dictDays.get(Integer.toString(counter));
		    			if (dayStatus == null) 
		    				continue;
		    			
/*		    			camp = dict.get(campid);
		    			if (camp == null)
		    				continue;*/
		    			
		    			for (Double el : ucsBidsDouble)
		    				strucsBids.add(el.toString());
		    			
		    			if (dayStatus.getUcsLevels().size() != 8)
		    				dayStatus.setUcsLevels(strucsBids);
		    			
			    		if (line == null)
			    			break;
	    				
	    				if (line.contains("INFO sim.Simulation|***** START OF TIME")) {
	    					currDay = line.split("START OF TIME | \\*\\*\\*")[1];
	    					counter++;
/*	    					dayStatus.setDay(currDay);
	    					camp.setDay(currDay);*/
	    				}
			    	}
			    	
			    	else if (line.contains("ended for advertiser")) {
			    		tmpStringArr = line.split("Campaign | ended|tartgetedImps=|, otherImps=|, cost=|\\] Reach | ERR | Budget | Revenue ");

			    		camp = dict.get(tmpStringArr[1]);
			    		camp.setBudget(tmpStringArr[8]);
			    		camp.setTartgetedImps(tmpStringArr[3]);
			    		camp.setOther(tmpStringArr[4]);
			    		camp.setCost(tmpStringArr[5]);
			    		camp.setERR(tmpStringArr[7]);
			    		camp.setRevenue(tmpStringArr[9]);
			    		
			    		try {
			    			fileWriter.append(camp.createLine());
			    			fileWriter.append(NEW_LINE_SEPARATOR);
			    		}
			    		catch (Exception e) {
			    			System.out.println("Error in CsvFileWriter !!!");
			    			e.printStackTrace();
			    		}
			    	}
			    }
			    br.close();	
			}
			catch (IOException e) {
				continue;
			}
		}

		for (int inx = 0; inx < counter; inx ++) {
			Set<String> agents = new HashSet<String>();
			Set<String> agentsEnd = new HashSet<String>();
			List<String> strucsBids = new ArrayList<String>();
			int dayInDayStatus = 0;
			int simnum = 0;
			int[] segForActive = new int[8];
			
			for (Map.Entry<String,DayStatus> iday : dictDays.entrySet()) {
				if (inx == Integer.parseInt(iday.getValue().getIndex()) && !iday.getValue().getUcsLevels().isEmpty()) {
					strucsBids = iday.getValue().getUcsLevels();
					dayInDayStatus = Integer.parseInt(iday.getValue().getDay());
					simnum = iday.getValue().getGame();
				}
			}
			if (simnum == 0)
				continue;
			
			for (Map.Entry<String,CampaignStatus> campiagn : dict.entrySet()) {
			
				if (Integer.parseInt(campiagn.getValue().getStart()) <= dayInDayStatus+1 && dayInDayStatus+1 <= Integer.parseInt(campiagn.getValue().getEnd()) && campiagn.getValue().getGame() == simnum) {
					agents.add(campiagn.getValue().getAgent());
					for (int j = 0; j < 8; j++) {
						segForActive[j] = segForActive[j] + Character.getNumericValue(campiagn.getValue().segmentToMask()[j]);
					}
				}
					
				if (dayInDayStatus+1 == Integer.parseInt(campiagn.getValue().getEnd()) && campiagn.getValue().getGame() == simnum) {
					agentsEnd.add(campiagn.getValue().getAgent());
				}
			}
			
			String strbids = "";
			for (int k = 0; k<strucsBids.size(); k++) {
				strbids = strbids + ',' + strucsBids.get(k);
				if (k == 6)
					break;
			}
		
			fileWriter2.append(Integer.toString(inx)+ ',' + Integer.toString(simnum) + ',' + Integer.toString(dayInDayStatus) + ',' + Integer.toString(agents.size())+ ',' + Integer.toString(agentsEnd.size()) + ',' + Integer.toString(segForActive[0])+ ',' + Integer.toString(segForActive[1])+ ',' + Integer.toString(segForActive[2])+ ',' + Integer.toString(segForActive[3])+ ',' + Integer.toString(segForActive[4])+ ',' + Integer.toString(segForActive[5])+ ',' + Integer.toString(segForActive[6])+ ',' + Integer.toString(segForActive[7]) + strbids);
			fileWriter2.append(NEW_LINE_SEPARATOR);
		}
		
		try {
			fileWriter.flush();
			fileWriter.close();
			fileWriter2.flush();
			fileWriter2.close();
		}
		catch (IOException e) {
			System.out.println("Error while flushing/closing fileWriter !!!");
			e.printStackTrace();
		}
	}
}


		
//		for(int ix = from; ix<=to; ix++) {
//			int simnum = ix;
//
//			for (int i = 0; i < 61; i++) {
//				Set<String> agents = new HashSet<String>();
//				Set<String> agentsEnd = new HashSet<String>();
//				List<String> strucsBids = new ArrayList<String>();
//				int[] segForActive = new int[8];
//
// /*				for (Map.Entry<String,CampaignStatus> campiagn : dict.entrySet()) {
//					
//					if (i == Integer.parseInt(campiagn.getValue().getDay()) && !campiagn.getValue().getUcsLevels().isEmpty() && campiagn.getValue().getGame() == simnum) {
//						strucsBids = campiagn.getValue().getUcsLevels();
//					}
//					
//					if (Integer.parseInt(campiagn.getValue().getStart()) <= i+1 && i+1 <= Integer.parseInt(campiagn.getValue().getEnd()) && campiagn.getValue().getGame() == simnum) {
//						agents.add(campiagn.getValue().getAgent());
//						for (int j = 0; j < 8; j++) {
//							segForActive[j] = segForActive[j] + Character.getNumericValue(campiagn.getValue().segmentToMask()[j]);
//						}
//					}
//					
//					if (i+1 == Integer.parseInt(campiagn.getValue().getEnd()) && campiagn.getValue().getGame() == simnum) {
//						agentsEnd.add(campiagn.getValue().getAgent());
//					}
//				}*/
//				
//				for (Map.Entry<String,DayStatus> iday : dictDays.entrySet()) {
//					if (i == Integer.parseInt(iday.getValue().getDay()) && !iday.getValue().getUcsLevels().isEmpty() && iday.getValue().getGame() == simnum) {
//						strucsBids = iday.getValue().getUcsLevels();
//					}
//				}
//						
//				for (Map.Entry<String,CampaignStatus> campiagn : dict.entrySet()) {
//					
//					if (Integer.parseInt(campiagn.getValue().getStart()) <= i+1 && i+1 <= Integer.parseInt(campiagn.getValue().getEnd()) && campiagn.getValue().getGame() == simnum) {
//						agents.add(campiagn.getValue().getAgent());
//						for (int j = 0; j < 8; j++) {
//							segForActive[j] = segForActive[j] + Character.getNumericValue(campiagn.getValue().segmentToMask()[j]);
//						}
//					}
//						
//					if (i+1 == Integer.parseInt(campiagn.getValue().getEnd()) && campiagn.getValue().getGame() == simnum) {
//						agentsEnd.add(campiagn.getValue().getAgent());
//					}
//				}
//				
//				String strbids = "";
//				for (int k= 0; k<strucsBids.size(); k++) {
//					strbids = strbids + ',' + strucsBids.get(k);
//					if (k == 6)
//						break;
//				}
//			
//				fileWriter2.append(Integer.toString(counter)+ ',' + Integer.toString(simnum) + ',' + Integer.toString(i) + ',' + Integer.toString(agents.size())+ ',' + Integer.toString(agentsEnd.size()) + ',' + Integer.toString(segForActive[0])+ ',' + Integer.toString(segForActive[1])+ ',' + Integer.toString(segForActive[2])+ ',' + Integer.toString(segForActive[3])+ ',' + Integer.toString(segForActive[4])+ ',' + Integer.toString(segForActive[5])+ ',' + Integer.toString(segForActive[6])+ ',' + Integer.toString(segForActive[7]) + strbids);
//				fileWriter2.append(NEW_LINE_SEPARATOR);
//			}
//		}
//			
//		try {
//			fileWriter.flush();
//			fileWriter.close();
//			fileWriter2.flush();
//			fileWriter2.close();
//		}
//		catch (IOException e) {
//			System.out.println("Error while flushing/closing fileWriter !!!");
//			e.printStackTrace();
//		}
//	}
//}
