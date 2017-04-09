import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;


import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;
import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.demand.CampaignStats;
import tau.tac.adx.devices.Device;
import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.props.PublisherCatalogEntry;
import tau.tac.adx.props.ReservePriceInfo;
import tau.tac.adx.report.adn.AdNetworkReport;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.report.demand.AdNetBidMessage;
import tau.tac.adx.report.demand.AdNetworkDailyNotification;
import tau.tac.adx.report.demand.CampaignOpportunityMessage;
import tau.tac.adx.report.demand.CampaignReport;
import tau.tac.adx.report.demand.CampaignReportKey;
import tau.tac.adx.report.demand.InitialCampaignMessage;
import tau.tac.adx.report.demand.campaign.auction.CampaignAuctionReport;
import tau.tac.adx.report.publisher.AdxPublisherReport;
import tau.tac.adx.report.publisher.AdxPublisherReportEntry;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BankStatus;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;


import tools.DataToCSV;



public class PineAppleAgent extends Agent 
{
	//yossi 27.3 start
	public boolean globPythonUse = true;
	public boolean debugFlag = true;
	public boolean testTrue = true;
	public static boolean debugFlagStatic = true;

	public static String pathAndCommand = "python3.6 ./PinePy/pyjava_comm.py ";

	//yossi 27.3 end
	
	private final Logger log = Logger
			.getLogger(PineAppleAgent.class.getName());

	/*
	 * Basic simulation information. An agent should receive the {@link
	 * StartInfo} at the beginning of the game or during recovery.
	 */
	@SuppressWarnings("unused")
	private StartInfo startInfo;

	/**
	 * Messages received:
	 * 
	 * We keep all the {@link CampaignReport campaign reports} delivered to the
	 * agent. We also keep the initialization messages {@link PublisherCatalog}
	 * and {@link InitialCampaignMessage} and the most recent messages and
	 * reports {@link CampaignOpportunityMessage}, {@link CampaignReport}, and
	 * {@link AdNetworkDailyNotification}.
	 */
	private final Queue<CampaignReport> campaignReports;
	private PublisherCatalog publisherCatalog;
	private InitialCampaignMessage initialCampaignMessage;
	private AdNetworkDailyNotification adNetworkDailyNotification;
	
	public enum Day {
	    bid, alloc, start 
	};
	
	/**
	 * The addresses of server entities to which the agent should send the daily
	 * bids data
	 */
	private String demandAgentAddress;
	private String adxAgentAddress;

	/**
	 * we maintain a list of queries - each characterized by the web site (the
	 * publisher), the device type, the ad type, and the user market segment
	 */
	private AdxQuery[] queries;

	/**
	 * Information regarding the latest campaign opportunity announced
	 */
	private CampaignData pendingCampaign;

	/**
	 * We maintain a collection (mapped by the campaign id) of the campaigns won
	 * by our agent.
	 */
	private Map<Integer, CampaignData> myCampaigns;

	/*
	 * the bidBundle to be sent daily to the AdX
	 */
	private AdxBidBundle bidBundle;

	/*
	 * The current bid level for the user classification service
	 */
	private double ucsBid;

	/*
	 * The targeted service level for the user classification service
	 */
	private double ucsTargetLevel;

	/*
	 * current day of simulation
	 */
	private int day;
	private String[] publisherNames;
	private CampaignData currCampaign;
	private long pendingCampaignBudget;
	private boolean DEBUG = true;

	public PineAppleAgent() 
	{
		campaignReports = new LinkedList<CampaignReport>();
		pendingCampaignBudget = 0;
	}
	
	/**
	 * currently works for n+2
	 * returns the open campaigns sorted by:
	 * @return
	 */
	private LinkedList<CampaignData> getAllOpenCampaignsAtDayPlus(int n)
	{
		int eday = day + n;
		LinkedList<CampaignData> retList = new LinkedList<>();
		for (Map.Entry<Integer, CampaignData> entry : myCampaigns.entrySet())
		{
			if (entry.getValue().dayStart <= eday && entry.getValue().dayEnd >= eday )
			{
				retList.add(entry.getValue());
			}
		}
		return retList;
	}

	@Override
	protected void messageReceived(Message message) 
	{
		try 
		{
			Transportable content = message.getContent();

			// log.fine(message.getContent().getClass().toString());

			if (content instanceof InitialCampaignMessage) 
			{
				handleInitialCampaignMessage((InitialCampaignMessage) content);
			} 
			else if (content instanceof CampaignOpportunityMessage) 
			{
				handleICampaignOpportunityMessage((CampaignOpportunityMessage) content);
			} 
			else if (content instanceof CampaignReport) 
			{
				handleCampaignReport((CampaignReport) content);
			} 
			else if (content instanceof AdNetworkDailyNotification) 
			{
				handleAdNetworkDailyNotification((AdNetworkDailyNotification) content);
			} 
			else if (content instanceof AdxPublisherReport) 
			{
				handleAdxPublisherReport((AdxPublisherReport) content);
			} 
			else if (content instanceof SimulationStatus) 
			{
				handleSimulationStatus((SimulationStatus) content);
			}
			else if (content instanceof PublisherCatalog) 
			{
				handlePublisherCatalog((PublisherCatalog) content);
			}
			else if (content instanceof AdNetworkReport) 
			{
				handleAdNetworkReport((AdNetworkReport) content);
			}
			else if (content instanceof StartInfo) 
			{
				handleStartInfo((StartInfo) content);
			} 
			else if (content instanceof BankStatus) 
			{
				handleBankStatus((BankStatus) content);
			} 
			else if(content instanceof CampaignAuctionReport) 
			{
				hadnleCampaignAuctionReport((CampaignAuctionReport) content);
			} 
			else if (content instanceof ReservePriceInfo) 
			{
				// ((ReservePriceInfo)content).getReservePriceType();
			} 
			else 
			{
				System.out.println("UNKNOWN Message Received: " + content);
			}

		} 
		catch (NullPointerException e) 
		{
			if (DEBUG) {
				System.out.println(message.getContent().getClass().toString());
			}
			this.log.log(Level.SEVERE,
					"Exception thrown while trying to parse message." + message.getContent().getClass().toString() + e);
			e.printStackTrace();			
			return;
		}
	}

	private void hadnleCampaignAuctionReport(CampaignAuctionReport content) 
	{
		// ingoring - this message is obsolete
	}

	private void handleBankStatus(BankStatus content) 
	{
		System.out.println("Day " + day + " :" + content.toString());
		//yossi 27.3 start
			if(globPythonUse){
				if(debugFlag)
					System.out.println("DEBUG: run python - BankStatus");
				runPythonScript("BankStatus " + Double.toString(content.getAccountBalance()));
			}
		//yossi 27.3 end
	}

	/**
	 * Processes the start information.
	 * 
	 * @param startInfo
	 *            the start information.
	 */
	protected void handleStartInfo(StartInfo startInfo) {
		this.startInfo = startInfo;
		
		//yossi 27.3 start
		if(globPythonUse){
			if(debugFlag)
				System.out.println("DEBUG: run python - StartInfo");
			runPythonScript("StartInfo " + Integer.toString(startInfo.getSimulationID()));
		}
		//yossi 27.3 end
	}

	/**
	 * Process the reported set of publishers
	 * 
	 * @param publisherCatalog
	 */
	private void handlePublisherCatalog(PublisherCatalog publisherCatalog) {
		this.publisherCatalog = publisherCatalog;
		generateAdxQuerySpace();
		getPublishersNames();

	}
	
	//yossi 27.3 start

	public static String getSegmentsInitials(String[] splitedSegments){
		char[] letterForSegmet = new char[splitedSegments.length];
		String segmentToUse = "";
		
		for(int i=0; i<splitedSegments.length; i++){
			System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$: "+splitedSegments[i]);
			letterForSegmet[i]=splitedSegments[i].charAt(0);
			}
		
		for(int i=0 ; i<3 ; i++)
			for(int j=0; j<letterForSegmet.length;j++)
			{
				String segOption;
				if(i==0){
					segOption="OY";
					if(segOption.indexOf(letterForSegmet[j])!=-1)
						segmentToUse = segmentToUse + Character.toString(letterForSegmet[j]);
				}
				if(i==1){
					segOption="MF";
					if(segOption.indexOf(letterForSegmet[j])!=-1)
						segmentToUse = segmentToUse + Character.toString(letterForSegmet[j]);
				}
				if(i==2){
					segOption="HL";
					if(segOption.indexOf(letterForSegmet[j])!=-1)
						segmentToUse = segmentToUse + Character.toString(letterForSegmet[j]);
				}
				
			}
		return segmentToUse;
	}
	//yossi 27.3 end


	
	/**
	 * On day 0, a campaign (the "initial campaign") is allocated to each
	 * competing agent. The campaign starts on day 1. The address of the
	 * server's AdxAgent (to which bid bundles are sent) and DemandAgent (to
	 * which bids regarding campaign opportunities may be sent in subsequent
	 * days) are also reported in the initial campaign message
	 */
	private void handleInitialCampaignMessage(InitialCampaignMessage campaignMessage) 
	{
		System.out.println(campaignMessage.toString());

		day = 0;

		initialCampaignMessage = campaignMessage;
		demandAgentAddress = campaignMessage.getDemandAgentAddress();
		adxAgentAddress = campaignMessage.getAdxAgentAddress();

		CampaignData campaignData = new CampaignData(initialCampaignMessage);
		campaignData.setBudget(initialCampaignMessage.getBudgetMillis()/1000.0);
		currCampaign = campaignData;
		genCampaignQueries(currCampaign);

		/*
		 * The initial campaign is already allocated to our agent so we add it
		 * to our allocated-campaigns list.
		 */
		System.out.println("Day " + day + ": Allocated campaign - " + campaignData);
		
		if (day == 0) {
			DataToCSV.createCSVFile("./data/campaign_statistics.csv", false, "");
			DataToCSV.createCSVFile("./data/campaigns_profitability.csv", false, "");
			String s_tmp = "Day " + day + ": Allocated campaign - " + campaignData;
			DataToCSV.split_to_fields(s_tmp, DEBUG);
		}
		
		myCampaigns.put(initialCampaignMessage.getId(), campaignData);
		
		
		//yossi 27.3 start
		
		String tempName = MarketSegment.names(initialCampaignMessage.getTargetSegment());
		tempName = tempName.trim();
		String[] splitedSegments = tempName.split("\\s+");
		String initialsSeg = getSegmentsInitials(splitedSegments);
		
		if(debugFlag){
			System.out.println("DEBUG: run getSegmentsInitials - InitialCampaignMessage");
			System.out.println("DEBUG: output getSegmentsInitials - InitialCampaignMessage: " + initialsSeg);

		}
		String paramString = Integer.toString(initialCampaignMessage.getId()) + " " + Long.toString(initialCampaignMessage.getReachImps()) + " " + Long.toString(initialCampaignMessage.getDayStart()) + " " + Long.toString(initialCampaignMessage.getDayEnd()) + " " + initialsSeg + " " + Double.toString(initialCampaignMessage.getVideoCoef())+ " " + Double.toString(initialCampaignMessage.getMobileCoef())+ " " + Long.toString(initialCampaignMessage.getBudgetMillis());
		
		if(globPythonUse){
			if(debugFlag)
				System.out.println("DEBUG: run python - InitialCampaignMessage");
			runPythonScript("InitialCampaignMessage " + paramString);
		}
		//yossi 27.3 end


		
	}

	/**
	 * On day n ( > 0) a campaign opportunity is announced to the competing
	 * agents. The campaign starts on day n + 2 or later and the agents may send
	 * (on day n) related bids (attempting to win the campaign). The allocation
	 * (the winner) is announced to the competing agents during day n + 1.
	 */
	private void handleICampaignOpportunityMessage(CampaignOpportunityMessage com) 
	{
		try{
		day = com.getDay();

		pendingCampaign = new CampaignData(com);
		System.out.println("Day " + day + ": Campaign opportunity - " + pendingCampaign);
		
		String s_tmp = "Day " + day + ": Campaign opportunity - " + pendingCampaign;
		DataToCSV.split_to_fields2(s_tmp, DEBUG);

		/*
		 * The campaign requires com.getReachImps() impressions. The competing
		 * Ad Networks bid for the total campaign Budget (that is, the ad
		 * network that offers the lowest budget gets the campaign allocated).
		 * The advertiser is willing to pay the AdNetwork at most 1$ CPM,
		 * therefore the total number of impressions may be treated as a reserve
		 * (upper bound) price for the auction.
		 */
		
		
		boolean DEBUG_UCS = false;
		
		String tempName = MarketSegment.names(com.getTargetSegment());
		tempName = tempName.trim();
		String[] splitedSegments = tempName.split("\\s+");
		String initialsSeg = getSegmentsInitials(splitedSegments);
			
		String paramString = Integer.toString(com.getId()) + " " + Long.toString(com.getReachImps()) + " " + Long.toString(com.getDayStart()) + " " + Long.toString(com.getDayEnd()) + " " + initialsSeg + " " + Double.toString(com.getVideoCoef())+ " " + Double.toString(com.getMobileCoef()) + " " + Integer.toString(com.getDay());
		
		if(debugFlag){
			System.out.println("DEBUG: run python - GetUcsAndBudget");
			System.out.println("DEBUG: run python - GetUcsAndBudget param: " + paramString);		
		}
		
		String outputString = runPythonScript("GetUcsAndBudget " + paramString);
		
		if(debugFlag)
			System.out.println("DEBUG: output python - GetUcsAndBudget\n" + outputString);
		
		JSONObject obj = new JSONObject(outputString);
		
		long cmpBidMillis = Long.parseLong(obj.getString("budgetBid"));
				
		pendingCampaignBudget = cmpBidMillis;

		System.out.println("Day " + day + ": Campaign total budget bid (millis): " + cmpBidMillis);

		/*
		 * Adjust ucs bid s.t. target level is achieved. Note: The bid for the
		 * user classification service is piggybacked
		 */


		ucsBid = Double.parseDouble(obj.getString("UCSBid"));

		System.out.println("Day " + day + ": ucsBid reported: " + ucsBid);

		
		/* Note: Campaign bid is in millis */
		AdNetBidMessage bids = new AdNetBidMessage(ucsBid, pendingCampaign.id, cmpBidMillis);
		
		sendMessage(demandAgentAddress, bids);
		}
		catch (Exception e) {
            System.out.println("exception happened at : handleICampaignOpportunityMessage" + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
		
	}

	private int numOfCampaignsCompleted()
	{
		printProp();
		int retval = 0;
		for (Map.Entry<Integer, CampaignData> entry : myCampaigns.entrySet())
		{
			if (entry.getValue().impsTogo() <= 0)
				retval++;
		}
		if (DEBUG) {
			System.out.println("############### day:" + day + " number of campaigns completed:"+retval);
		}
		return retval;
	}
	
	private void printProp() {
		if (DEBUG){
			System.out.println(day);
			for (Map.Entry<Integer, CampaignData> entry : myCampaigns.entrySet()) {
				System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
				
				System.out.println(entry.getValue().budget);
				System.out.println(entry.getValue().dayStart);
				System.out.println(entry.getValue().dayEnd);
				System.out.println(entry.getValue().id);
				System.out.println(entry.getValue().stats);
				System.out.println(entry.getValue().targetSegment);
				System.out.println("impsToGo: " + entry.getValue().impsTogo());
				System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			}
		}
		
	}
	
	private int isCampineDay0Completed() {
		for (Map.Entry<Integer, CampaignData> entry : myCampaigns.entrySet()) {
			if (entry.getValue().dayStart ==1 && entry.getValue().impsTogo() <=0 ) {
				if (DEBUG) {
					System.out.println("DAY 0 COMPLETEDDDDDDDDDDDDDDDDD");
				}
				return 1;
			}
		}
		return 0;
	}
	
	private boolean biddedYesterday() 
	{
		if (DEBUG){
			System.out.println("@@@@@@@@@@@@@@@@@@@" + pendingCampaign.budget);
		}
		if (pendingCampaignBudget == 40000)
			return false;
		else
			return true;
	}

	/**
	 * On day n ( > 0), the result of the UserClassificationService and Campaign
	 * auctions (for which the competing agents sent bids during day n -1) are
	 * reported. The reported Campaign starts in day n+1 or later and the user
	 * classification service level is applicable starting from day n+1.
	 */
	private void handleAdNetworkDailyNotification(
			AdNetworkDailyNotification notificationMessage) 
	{

		adNetworkDailyNotification = notificationMessage;

		System.out.println("Day " + day + ": Daily notification for campaign "
				+ adNetworkDailyNotification.getCampaignId());

		String campaignAllocatedTo = " allocated to "
				+ notificationMessage.getWinner();

		if ((pendingCampaign.id == adNetworkDailyNotification.getCampaignId())
				&& (notificationMessage.getCostMillis() != 0)) 
		{

			/* add campaign to list of won campaigns */
			pendingCampaign.setBudget(notificationMessage.getCostMillis()/1000.0);
			currCampaign = pendingCampaign;
			genCampaignQueries(currCampaign);
			myCampaigns.put(pendingCampaign.id, pendingCampaign);

			campaignAllocatedTo = " WON at cost (Millis)"
					+ notificationMessage.getCostMillis();
		}

		System.out.println("Day " + day + ": " + campaignAllocatedTo
				+ ". UCS Level set to " + notificationMessage.getServiceLevel()
				+ " at price " + notificationMessage.getPrice()
				+ " Quality Score is: " + notificationMessage.getQualityScore());
		
		//yossi 27.3 start
		if(globPythonUse){
			if(debugFlag)
				System.out.println("DEBUG: run python - AdNetworkDailyNotification");
			String paramsToSend = Integer.toString(adNetworkDailyNotification.getEffectiveDay()) + " " + Double.toString(adNetworkDailyNotification.getServiceLevel()) + " " + Double.toString(adNetworkDailyNotification.getPrice()) + " " + Double.toString(adNetworkDailyNotification.getQualityScore()) + " " + Integer.toString(adNetworkDailyNotification.getCampaignId()) + " " + adNetworkDailyNotification.getWinner() + " " + Long.toString(adNetworkDailyNotification.getCostMillis());
			runPythonScript("AdNetworkDailyNotification " + paramsToSend);
			
		}
		//yossi 27.3 end

		
		
	}

	/**
	 * The SimulationStatus message received on day n indicates that the
	 * calculation time is up and the agent is requested to send its bid bundle
	 * to the AdX.
	 */
	private void handleSimulationStatus(SimulationStatus simulationStatus) {
		System.out.println("Day " + day + " : Simulation Status Received");
		sendBidAndAds();
		System.out.println("Day " + day + " ended. Starting next day");
		++day;
	}

	public static Set<MarketSegment> createSegmentFromPython(String pythonSegName){
		MarketSegment m1, m2, m3;
		if(pythonSegName.charAt(0)=='O')
			m1=MarketSegment.OLD;
		else
			m1=MarketSegment.YOUNG;
		
		if(pythonSegName.charAt(1)=='M')
			m2=MarketSegment.MALE;
		else
			m2=MarketSegment.FEMALE;
		
		if(pythonSegName.charAt(2)=='H')
			m3=MarketSegment.HIGH_INCOME;
		else
			m3=MarketSegment.LOW_INCOME;
		
		return MarketSegment.compundMarketSegment3(m1,m2,m3);
	}
	
	/**
	 * 
	 */
	protected void sendBidAndAds() 
	{
		try{

		if(debugFlag)
			System.out.println("DEBUG: run python - GetBidBundle");		
		
		String outputString = runPythonScript("GetBidBundle");
		
		if(debugFlag)
			System.out.println("DEBUG: output python - GetBidBundle\n" + outputString);
		
		
		bidBundle = new AdxBidBundle();

		int dayBiddingFor = day + 1;

		JSONObject JbidBundle = new JSONObject(outputString);
		JSONArray JbidsArray = JbidBundle.getJSONArray("bidbundle");
		JSONObject JbidBundleElement;
		AdxQuery query;
		Device d;
		AdType adtype; 
		for (int i = 0; i < JbidsArray.length(); i++) {	
			JbidBundleElement = JbidsArray.getJSONObject(i);
			JSONObject JQuery = JbidBundleElement.getJSONObject("query");
			if(JQuery.getString("Device").equals("Desktop"))
				d =	Device.pc;
			else
				d =	Device.mobile;
			if(JQuery.getString("adType").equals("Text"))
				adtype =AdType.text;
			else
				adtype =AdType.video;
 
			query = new AdxQuery(JQuery.getString("publisher"), 
					createSegmentFromPython(JQuery.getJSONArray("marketSegments").getJSONObject(0).getString("segmentName")),
					d,
					adtype);
			
			bidBundle.addQuery(query, 
					Double.parseDouble(JbidBundleElement.getString("bid")),
					new Ad(null),
					Integer.parseInt(JbidBundleElement.getString("campaignId")),
					Integer.parseInt(JbidBundleElement.getString("weight")));
		}
		
		if (bidBundle != null) 
		{
			System.out.println("Day " + day + ": Sending BidBundle");
			sendMessage(adxAgentAddress, bidBundle);
		}
		}
		catch(Exception e){
			 System.out.println("exception happened at sendBidAndAds " + e.getMessage());
	            e.printStackTrace();
	            System.exit(-1);
		}
		
	}

	/**
	 * Campaigns performance w.r.t. each allocated campaign
	 */
	private void handleCampaignReport(CampaignReport campaignReport) 
	{

		campaignReports.add(campaignReport);

		
		//yossi 27.3 start
		
		String paramsToSend = Integer.toString(campaignReport.keys().size());
		
		//yossi 27.3 end
		
		
		/*
		 * for each campaign, the accumulated statistics from day 1 up to day
		 * n-1 are reported
		 */
		for (CampaignReportKey campaignKey : campaignReport.keys()) 
		{
			
					
			int cmpId = campaignKey.getCampaignId();
			CampaignStats cstats = campaignReport.getCampaignReportEntry(campaignKey).getCampaignStats();
			myCampaigns.get(cmpId).setStats(cstats);
			
			
			//Yossi
			String strToPrint = "Day " + day + ": Updating campaign " + cmpId + " stats: "
					+ cstats.getTargetedImps() + " tgtImps "
					+ cstats.getOtherImps() + " nonTgtImps. Cost of imps is "
					+ cstats.getCost();
			//yossi
			System.out.println(strToPrint);
			
			//yossi 27.3 start
			if(globPythonUse){
				
				paramsToSend = paramsToSend + " " + Integer.toString(cmpId) + " " + Double.toString(cstats.getTargetedImps()) + " " + Double.toString(cstats.getOtherImps()) + " " + Double.toString(cstats.getCost());
				
			}
			//yossi 27.3 end
			
			
		}
		
		//yossi 27.3 start
				

		if(globPythonUse){
			if(debugFlag)
				System.out.println("DEBUG: run python - CampaignReport");
			runPythonScript("CampaignReport " + paramsToSend);
		}
		
		//yossi 27.3 end
		
	}

	/**
	 * Users and Publishers statistics: popularity and ad type orientation
	 */
	private void handleAdxPublisherReport(AdxPublisherReport adxPublisherReport) 
	{
		System.out.println("Publishers Report: ");
		for (PublisherCatalogEntry publisherKey : adxPublisherReport.keys()) 
		{
			AdxPublisherReportEntry entry = adxPublisherReport
					.getEntry(publisherKey);
			System.out.println(entry.toString());
		}
	}

	/**
	 * 
	 * @param AdNetworkReport
	 */
	private void handleAdNetworkReport(AdNetworkReport adnetReport) 
	{

		System.out.println("Day " + day + " : AdNetworkReport");
		/*
		 * for (AdNetworkKey adnetKey : adnetReport.keys()) {
		 * 
		 * double rnd = Math.random(); if (rnd > 0.95) { AdNetworkReportEntry
		 * entry = adnetReport .getAdNetworkReportEntry(adnetKey);
		 * System.out.println(adnetKey + " " + entry); } }
		 */
	}

	@Override
	protected void simulationSetup() 
	{
		Random random = new Random();

		day = 0;
		bidBundle = new AdxBidBundle();

		/* initial bid between 0.1 and 0.2 */
		ucsBid = 0.1 + random.nextDouble()/10.0;

		myCampaigns = new HashMap<Integer, CampaignData>();
		log.fine("AdNet " + getName() + " simulationSetup");
		
	}

	@Override
	protected void simulationFinished() 
	{
		campaignReports.clear();
		bidBundle = null;
	}

	/**
	 * A user visit to a publisher's web-site results in an impression
	 * opportunity (a query) that is characterized by the the publisher, the
	 * market segment the user may belongs to, the device used (mobile or
	 * desktop) and the ad type (text or video).
	 * 
	 * An array of all possible queries is generated here, based on the
	 * publisher names reported at game initialization in the publishers catalog
	 * message
	 */
	private void generateAdxQuerySpace() 
	{
		if (publisherCatalog != null && queries == null) 
		{
			Set<AdxQuery> querySet = new HashSet<AdxQuery>();

			/*
			 * for each web site (publisher) we generate all possible variations
			 * of device type, ad type, and user market segment
			 */
			for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog) 
			{
				String publishersName = publisherCatalogEntry
						.getPublisherName();
				for (MarketSegment userSegment : MarketSegment.values()) 
				{
					Set<MarketSegment> singleMarketSegment = new HashSet<MarketSegment>();
					singleMarketSegment.add(userSegment);

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.video));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.video));
				}

				/**
				 * An empty segments set is used to indicate the "UNKNOWN"
				 * segment such queries are matched when the UCS fails to
				 * recover the user's segments.
				 */
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.text));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.text));
			}
			queries = new AdxQuery[querySet.size()];
			querySet.toArray(queries);
		}
	}
	
	/*generates an array of the publishers names
	 * */
	private void getPublishersNames() 
	{
		if (null == publisherNames && publisherCatalog != null) 
		{
			ArrayList<String> names = new ArrayList<String>();
			for (PublisherCatalogEntry pce : publisherCatalog) 
			{
				names.add(pce.getPublisherName());
			}

			publisherNames = new String[names.size()];
			names.toArray(publisherNames);
		}
	}
	/*
	 * generates the campaign queries relevant for the specific campaign, and assign them as the campaigns campaignQueries field 
	 */
	private void genCampaignQueries(CampaignData campaignData) 
	{
		Set<AdxQuery> campaignQueriesSet = new HashSet<AdxQuery>();
		for (String PublisherName : publisherNames) 
		{
			campaignQueriesSet.add(new AdxQuery(PublisherName,
					campaignData.targetSegment, Device.mobile, AdType.text));
			campaignQueriesSet.add(new AdxQuery(PublisherName,
					campaignData.targetSegment, Device.mobile, AdType.video));
			campaignQueriesSet.add(new AdxQuery(PublisherName,
					campaignData.targetSegment, Device.pc, AdType.text));
			campaignQueriesSet.add(new AdxQuery(PublisherName,
					campaignData.targetSegment, Device.pc, AdType.video));
		}

		campaignData.campaignQueries = new AdxQuery[campaignQueriesSet.size()];
		campaignQueriesSet.toArray(campaignData.campaignQueries);
		System.out.println("!!!!!!!!!!!!!!!!!!!!!!"+Arrays.toString(campaignData.campaignQueries)+"!!!!!!!!!!!!!!!!");
	}

	//YOSSID 27.3 start

	//run a new proccess and activate the inputed cmd  - taken from http://alvinalexander.com
	public static String runPythonScript(String queryToRun){
		String s = null;
		String stdout = null;
		String stderr = null;

        try {

            // using the Runtime exec method:
            
            if(debugFlagStatic)
                System.out.println("DEBUG: start exec");
            
            Process p = Runtime.getRuntime().exec(pathAndCommand + queryToRun);
            
            
            
            if(debugFlagStatic)
                System.out.println("DEBUG: DONE exec");
                
            BufferedReader stdInput = new BufferedReader(new 
                 InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new 
                 InputStreamReader(p.getErrorStream()));
            
            
            
            // read the output from the command
            while ((s = stdInput.readLine()) != null) {
            	stdout=s;
            }
            
            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
            	stderr=s;
            }
            
            if(debugFlagStatic)
            {
            	System.out.println("the stdout is:");
            	System.out.println(stdout);
            	System.out.println("the stderr is:");
            	System.out.println(stderr);
            }
            
        }
        catch (IOException e) {
            System.out.println("exception happened - here's what I know: ");
            e.printStackTrace();
            System.exit(-1);
        }
        return stdout;
	}
	//YOSSID 27.3 end
	
	public class CampaignData 
	{
		/* campaign attributes as set by server */
		Long reachImps;
		long dayStart;
		long dayEnd;
		Set<MarketSegment> targetSegment;
		double videoCoef;
		double mobileCoef;
		int id;
		private AdxQuery[] campaignQueries;//array of queries relevant for the campaign.

		/* campaign info as reported */
		CampaignStats stats;
		double budget;

		public CampaignData(InitialCampaignMessage icm) 
		{
			reachImps = icm.getReachImps();
			dayStart = icm.getDayStart();
			dayEnd = icm.getDayEnd();
			targetSegment = icm.getTargetSegment();
			videoCoef = icm.getVideoCoef();
			mobileCoef = icm.getMobileCoef();
			id = icm.getId();

			stats = new CampaignStats(0, 0, 0);
			budget = 0.0;
		}

		public void setBudget(double d) 
		{
			budget = d;
		}

		public double getBudget() 
		{
			return budget;
		}
		
		public CampaignData(CampaignOpportunityMessage com) 
		{
			dayStart = com.getDayStart();
			dayEnd = com.getDayEnd();
			id = com.getId();
			reachImps = com.getReachImps();
			targetSegment = com.getTargetSegment();
			mobileCoef = com.getMobileCoef();
			videoCoef = com.getVideoCoef();
			stats = new CampaignStats(0, 0, 0);
			budget = 0.0;
		}

		@Override
		public String toString() 
		{
			return "Campaign ID " + id + ": " + "day " + dayStart + " to "
					+ dayEnd + " " + targetSegment + ", reach: " + reachImps
					+ " coefs: (v=" + videoCoef + ", m=" + mobileCoef + ")";
		}

		int impsTogo() 
		{
			return (int) Math.max(0, reachImps - stats.getTargetedImps());
		}

		void setStats(CampaignStats s) 
		{
			stats.setValues(s);
		}

		public AdxQuery[] getCampaignQueries() 
		{
			return campaignQueries;
		}

		public void setCampaignQueries(AdxQuery[] campaignQueries) 
		{
			this.campaignQueries = campaignQueries;
		}

/*		public int accurityCalc() {
			char[] flags = CampaignStatus.segParser(this.targetSegment.toString());
			int count = 0;
			for (int i =0; i < 8; i++){
				if (flags[i] == '1') {
					count += population[i];
				}
			}
			
			long dayToGo = this.dayEnd - day;
			return (int)(Math.ceil(this.impsTogo()/(count*dayToGo)));
		}*/
	}

}