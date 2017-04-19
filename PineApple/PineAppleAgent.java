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

public class PineAppleAgent extends Agent {

	// Debug flags
	public boolean debugFlag = true;
	public boolean testTrue = true;
	public static boolean debugFlagStatic = true;
	private boolean DEBUG = true;
	public boolean DEBUG_UCS = false;

	private final Logger log = Logger.getLogger(PineAppleAgent.class.getName());

	// public static String pathAndCommand = "python3.6
	// ./PinePy/__pycache__/pyjava_comm.cpython-36.pyc "; //"python
	// ./PinePy/pyjava_comm.py ";

	// Python engine and streams:
	public static Process pythonProccess;
	public static BufferedReader inputStreamPythonApp;
	public static BufferedWriter outputStreamPythonApp;
	public static BufferedReader errorStreamPythonApp;
	public static final String pathAndCommand = "python3.6 ./PinePy/pyjava_comm.py ";

	// Communication w/ server
	/**
	 * The addresses of server entities to which the agent should send the daily
	 * bids data
	 */
	private String demandAgentAddress;
	private String adxAgentAddress;
	private int simulationId;

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

	/* UNUSED */
	public enum Day {
		bid, alloc, start
	};

	// -- various objects of the game:

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

	// Times
	public static long dailyNotificationTime = 0;

	public static String cmpReportLastParams = "";
	public static int campReportSavedDay = -1;
	public static int dayLastCampOpp = -1;

	// #############################
	// Function declarations #
	// #############################

	// Communication w/ python engine

	public static String pipe(String msg, boolean waitToAns) {
		String ret = null;
		String lastRet = null;

		try {
			outputStreamPythonApp.write(msg + "\n");
			outputStreamPythonApp.flush();
			if (waitToAns) {
				while ((ret = inputStreamPythonApp.readLine()) != null) {
					System.out.println(ret);
					return ret;
				}
			}
			return lastRet;
		}

		catch (Exception err) {
			System.out.println("exception in function pipe: " + err.toString());
		}
		return "";
	}

	// run a new proccess and activate the inputed cmd - taken from
	// http://alvinalexander.com
	public static String runPythonScript(String queryToRun, boolean waitForAnswer) 
	{

		long startTime = System.currentTimeMillis();
		String stderr = null;
		String retVal = null;

		try 
		{
			System.out.println("EnterToPipe, send: " + queryToRun + " i am waiting: " + waitForAnswer);
			retVal = pipe(queryToRun, waitForAnswer);
			System.out.println("returned from pipe.");

			int i = 0;

			/*
			 * // read any errors from the attempted command while ((s =
			 * errorStreamPythonApp.readLine()) != null) {
			 * System.out.println(i++);
			 * 
			 * stderr= stderr + "\n" + s;
			 * 
			 * }
			 */

			if (debugFlagStatic)
			{

				System.out.println("DEBUG: python  returned after " + (System.currentTimeMillis() - startTime) + " ms with:");
				// System.out.println("the stderr is:");
				// System.out.println(stderr);
				System.out.println(retVal);
			}
		}
		catch (Exception e)
		{
			System.out.println("exception happened (runPythonScript) - here's what I know: ");
			e.printStackTrace();
			System.exit(-1);
		}
		return retVal;
	}

	/* constructor */
	public PineAppleAgent()
	{
		System.out.println("PineAppleAgent Constructor called! Have a tropical day!");
		try {
			log.fine("Bringing up the python engine process!");
			pythonProccess = Runtime.getRuntime().exec(pathAndCommand);
			outputStreamPythonApp = new BufferedWriter(new OutputStreamWriter(pythonProccess.getOutputStream()));
			inputStreamPythonApp = new BufferedReader(new InputStreamReader(pythonProccess.getInputStream()));
			errorStreamPythonApp = new BufferedReader(new InputStreamReader(pythonProccess.getErrorStream()));

		} catch (Exception err) {
			log.severe("Failed to bring up the python engine!");
			err.printStackTrace();
		}
		campaignReports = new LinkedList<CampaignReport>();
		pendingCampaignBudget = 0;
	}

	/**
	 * Entry point to the implementation of the PineAppleAgent
	 * 
	 * @param message:
	 *            received from game server
	 */
	@Override
	protected void messageReceived(Message message) {
		try {
			Transportable content = message.getContent();

			// log.fine(message.getContent().getClass().toString());

			if (content instanceof InitialCampaignMessage) {
				handleInitialCampaignMessage((InitialCampaignMessage) content);
			} else if (content instanceof CampaignOpportunityMessage) {
				handleICampaignOpportunityMessage((CampaignOpportunityMessage) content);
			} else if (content instanceof CampaignReport) {
				handleCampaignReport((CampaignReport) content);
			} else if (content instanceof AdNetworkDailyNotification) {
				handleAdNetworkDailyNotification((AdNetworkDailyNotification) content);
			} else if (content instanceof AdxPublisherReport) {
				handleAdxPublisherReport((AdxPublisherReport) content);
			} else if (content instanceof SimulationStatus) {
				handleSimulationStatus((SimulationStatus) content);
			} else if (content instanceof PublisherCatalog) {
				handlePublisherCatalog((PublisherCatalog) content);
			} else if (content instanceof AdNetworkReport) {
				handleAdNetworkReport((AdNetworkReport) content);
			} else if (content instanceof StartInfo) {
				handleStartInfo((StartInfo) content);
			} else if (content instanceof BankStatus) {
				handleBankStatus((BankStatus) content);
			} else if (content instanceof CampaignAuctionReport) {
				handleCampaignAuctionReport((CampaignAuctionReport) content);
			} else if (content instanceof ReservePriceInfo) {
				// ((ReservePriceInfo)content).getReservePriceType();
			} else {
				System.out.println("UNKNOWN Message Received: " + content);
			}
		} catch (NullPointerException e) {
			if (DEBUG) {
				System.out.println(message.getContent().getClass().toString());
			}
			this.log.log(Level.SEVERE,
					"Exception thrown while trying to parse message." + message.getContent().getClass().toString() + e);
			e.printStackTrace();
			return;
		}
	}

	// #######################################
	// HANDLER FUNCTIONS #
	// #######################################

	/**
	 * This doesn't come as a consideration to us during the game.
	 * 
	 * @param content
	 */
	private void handleBankStatus(BankStatus content) {
		System.out.println("Day " + day + " :" + content.toString());
		// if(debugFlag)
		// System.out.println("DEBUG: run python - BankStatus");
		// runPythonScript("BankStatus " +
		// Double.toString(content.getAccountBalance()));
	}

	/**
	 * Processes the start information.
	 * 
	 * @param startInfo
	 *            the start information.
	 */
	protected void handleStartInfo(StartInfo startInfo) {
		long startTime = System.currentTimeMillis();
		this.startInfo = startInfo;
		this.simulationId = startInfo.getSimulationID();
		// TODO: notify python for logging purposes.

		// if(debugFlag)
		// System.out.println("DEBUG: run python - StartInfo");
		// runPythonScript("StartInfo " +
		// Integer.toString(startInfo.getSimulationID()));
		// System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~\t\t\tstart info
		// elapse: "+(System.currentTimeMillis()-startTime));
	}

	/**
	 * Process the reported set of publishers
	 * 
	 * @param publisherCatalog
	 */
	private void handlePublisherCatalog(PublisherCatalog publisherCatalog) {
		System.out.println("Handling Publishers Catalog");
		this.publisherCatalog = publisherCatalog;
		generateAdxQuerySpace();
		getPublishersNames();
	}

	/**
	 * On day 0, a campaign (the "initial campaign") is allocated to each
	 * competing agent. The campaign starts on day 1. The address of the
	 * server's AdxAgent (to which bid bundles are sent) and DemandAgent (to
	 * which bids regarding campaign opportunities may be sent in subsequent
	 * days) are also reported in the initial campaign message
	 */
	private void handleInitialCampaignMessage(InitialCampaignMessage campaignMessage) {
		System.out.println("Initial_Campaign_Message:\n" + campaignMessage.toString());

		day = 0;

		initialCampaignMessage = campaignMessage;
		demandAgentAddress = campaignMessage.getDemandAgentAddress();
		adxAgentAddress = campaignMessage.getAdxAgentAddress();

		CampaignData campaignData = new CampaignData(initialCampaignMessage);
		campaignData.setBudget(initialCampaignMessage.getBudgetMillis() / 1000.0);
		currCampaign = campaignData;
		genCampaignQueries(currCampaign);

		/*
		 * The initial campaign is already allocated to our agent so we add it
		 * to our allocated-campaigns list.
		 */
		System.out.println("Day " + day + ": Allocated campaign - " + campaignData);

		if (day == 0) {
			DataToCSV.createCSVFile("temp_" + startInfo.getSimulationID() + ".csv", false, "");
			String s_tmp = "Day " + day + ": Allocated campaign - " + campaignData;
			DataToCSV.split_to_fields(s_tmp, DEBUG);
		}

		myCampaigns.put(initialCampaignMessage.getId(), campaignData);

		String tempName = MarketSegment.names(initialCampaignMessage.getTargetSegment());
		tempName = tempName.trim();
		String[] splitedSegments = tempName.split("\\s+");
		String initialsSeg = getSegmentsInitials(splitedSegments);

		if (debugFlag) {
			System.out.println("DEBUG: run getSegmentsInitials - InitialCampaignMessage");
			System.out.println("DEBUG: output getSegmentsInitials - InitialCampaignMessage: " + initialsSeg);

		}
		String paramString = Integer.toString(initialCampaignMessage.getId()) + " "
				+ Long.toString(initialCampaignMessage.getReachImps()) + " "
				+ Long.toString(initialCampaignMessage.getDayStart()) + " "
				+ Long.toString(initialCampaignMessage.getDayEnd()) + " " + initialsSeg + " "
				+ Double.toString(initialCampaignMessage.getVideoCoef()) + " "
				+ Double.toString(initialCampaignMessage.getMobileCoef()) + " "
				+ Long.toString(initialCampaignMessage.getBudgetMillis());

		if (debugFlag)
			System.out.println("DEBUG: run python - InitialCampaignMessage");
		runPythonScript("InitialCampaignMessage " + paramString, false);
		System.out.println("DEBUG: returned run python - InitialCampaignMessage");
	}

	/**
	 * On day n ( > 0) a campaign opportunity is announced to the competing
	 * agents. The campaign starts on day n + 2 or later and the agents may send
	 * (on day n) related bids (attempting to win the campaign). The allocation
	 * (the winner) is announced to the competing agents during day n + 1.
	 */
	private void handleICampaignOpportunityMessage(CampaignOpportunityMessage com) {
		try {
			long startTime = System.currentTimeMillis();

			day = com.getDay();
			long cmpBidMillis;

			pendingCampaign = new CampaignData(com);
			System.out.println("Day " + day + ": Campaign opportunity - " + pendingCampaign);

			String s_tmp = "Day " + day + ": Campaign opportunity - " + pendingCampaign;
			// DataToCSV.split_to_fields2(s_tmp, false);

			/*
			 * The campaign requires com.getReachImps() impressions. The
			 * competing Ad Networks bid for the total campaign Budget (that is,
			 * the ad network that offers the lowest budget gets the campaign
			 * allocated). The advertiser is willing to pay the AdNetwork at
			 * most 1$ CPM, therefore the total number of impressions may be
			 * treated as a reserve (upper bound) price for the auction.
			 */

			String tempName = MarketSegment.names(com.getTargetSegment());
			tempName = tempName.trim();
			String[] splitedSegments = tempName.split("\\s+");
			String initialsSeg = getSegmentsInitials(splitedSegments);

			String paramString = Integer.toString(com.getId()) + " " + Long.toString(com.getReachImps()) + " "
					+ Long.toString(com.getDayStart()) + " " + Long.toString(com.getDayEnd()) + " " + initialsSeg + " "
					+ Double.toString(com.getVideoCoef()) + " " + Double.toString(com.getMobileCoef()) + " "
					+ Integer.toString(com.getDay());

			if (debugFlag) {
				System.out.println(
						"handleICampaignOpportunityMessage: run python - GetUcsAndBudget param: " + paramString);
			}

			String outputString = runPythonScript("GetUcsAndBudget " + paramString, true);

			if (debugFlag)
				System.out
						.println("handleICampaignOpportunityMessage: output python - GetUcsAndBudget\n" + outputString);

			if (outputString == null) {
				System.out.println("handleICampaignOpportunityMessage: GetUcsAndBudget returned null");
				cmpBidMillis = com.getReachImps() / 5;
				ucsBid = 0.202;
				AdNetBidMessage bids = new AdNetBidMessage(ucsBid, pendingCampaign.id, cmpBidMillis);
				sendMessage(demandAgentAddress, bids);
				return;
			}

			// python also returns bidbundle:
			JSONObject obj = new JSONObject(outputString);

			cmpBidMillis = Long.parseLong(obj.getString("budgetBid"));

			pendingCampaignBudget = cmpBidMillis;

			System.out.println("handleICampaignOpportunityMessage: Day " + day
					+ ": Campaign total budget bid (millis): " + cmpBidMillis);

			ucsBid = Double.parseDouble(obj.getString("UCSBid"));

			System.out.println("handleICampaignOpportunityMessage: Day " + day + ": ucsBid reported: " + ucsBid);

			/* Note: Campaign bid is in millis */
			AdNetBidMessage bids = new AdNetBidMessage(ucsBid, pendingCampaign.id, cmpBidMillis);

			sendMessage(demandAgentAddress, bids);
			System.out.println("handleICampaignOpportunityMessage: start of func to after sendMessage elapsed: "
					+ (System.currentTimeMillis() - startTime));
			dayLastCampOpp = day;

			// python also returns a bidbundle:

			bidBundle = new AdxBidBundle();

			int dayBiddingFor = day + 1;

			JSONObject JbidBundle = new JSONObject(outputString);
			JSONArray JbidsArray = JbidBundle.getJSONArray("bidbundle");
			JSONObject JbidBundleElement;
			AdxQuery query, emptyQuery;
			Device device;
			AdType adtype;
			for (int i = 0; i < JbidsArray.length(); i++) {
				JbidBundleElement = JbidsArray.getJSONObject(i);
				JSONObject JQuery = JbidBundleElement.getJSONObject("query");
				if (JQuery.getString("Device").equals("Desktop"))
					device = Device.pc;
				else
					device = Device.mobile;

				if (JQuery.getString("adType").equals("Text"))
					adtype = AdType.text;
				else
					adtype = AdType.video;

				for (String publisherName : publisherNames) {
					String marketSegmentName = JQuery.getJSONArray("marketSegments").getJSONObject(0)
							.getString("segmentName");
					Set<MarketSegment> segment;

					if (marketSegmentName.compareTo("Unknown") == 0) // equals
						segment = new HashSet<MarketSegment>();
					else
						segment = createSegmentFromPython(marketSegmentName);

					query = new AdxQuery(publisherName, segment, device, adtype);

					bidBundle.addQuery(query, Double.parseDouble(JbidBundleElement.getString("bid")), new Ad(null),
							JbidBundleElement.getInt("campaignId"), JbidBundleElement.getInt("weight"),
							Double.parseDouble(JbidBundleElement.getString("dailyLimit")));
				}
			}

			System.out
					.println("handleICampaignOpportunityMessage: elapsed: " + (System.currentTimeMillis() - startTime));

		} catch (Exception e) {
			System.out.println(
					"CRITICAL ERROR: exception happened at : handleICampaignOpportunityMessage" + e.getMessage());
			e.printStackTrace();
			System.out.println("Exiting program");
			System.exit(-1);
		}

	}

	/**
	 * On day n ( > 0), the result of the UserClassificationService and Campaign
	 * auctions (for which the competing agents sent bids during day n -1) are
	 * reported. The reported Campaign starts in day n+1 or later and the user
	 * classification service level is applicable starting from day n+1.
	 */
	private void handleAdNetworkDailyNotification(AdNetworkDailyNotification notificationMessage) {
		dailyNotificationTime = System.currentTimeMillis();

		adNetworkDailyNotification = notificationMessage;

		System.out.println("Day " + day + ": Daily notification (results of opportunity) for campaign "
				+ adNetworkDailyNotification.getCampaignId());

		String campaignAllocatedTo = " allocated to " + notificationMessage.getWinner();

		if ((pendingCampaign.id == adNetworkDailyNotification.getCampaignId())
				&& (notificationMessage.getCostMillis() != 0)) {
			/* add campaign to list of won campaigns */
			pendingCampaign.setBudget(notificationMessage.getCostMillis() / 1000.0);
			currCampaign = pendingCampaign;
			genCampaignQueries(currCampaign);
			myCampaigns.put(pendingCampaign.id, pendingCampaign);

			campaignAllocatedTo = "PineAppleAgent WON campaign " + adNetworkDailyNotification.getCampaignId()
					+ " at cost (Millis)" + notificationMessage.getCostMillis();
		}

		System.out.println("Day " + day + ": " + campaignAllocatedTo + ". UCS Level set to "
				+ notificationMessage.getServiceLevel() + " at price " + notificationMessage.getPrice()
				+ " Quality Score is: " + notificationMessage.getQualityScore());

		String nameWinner = adNetworkDailyNotification.getWinner();
		if (nameWinner == null || nameWinner.equals(""))
			nameWinner = "NOT_ALLOCATED";
		if (debugFlag)
			System.out.println("DEBUG: run python - AdNetworkDailyNotification");

		String paramsToSend = " DAILYNOTIFICATION " + Integer.toString(adNetworkDailyNotification.getEffectiveDay())
				+ " " + Double.toString(adNetworkDailyNotification.getServiceLevel()) + " "
				+ Double.toString(adNetworkDailyNotification.getPrice()) + " "
				+ Double.toString(adNetworkDailyNotification.getQualityScore()) + " "
				+ Integer.toString(adNetworkDailyNotification.getCampaignId()) + " " + nameWinner + " "
				+ Long.toString(adNetworkDailyNotification.getCostMillis());

		if (campReportSavedDay == day) {
			paramsToSend = cmpReportLastParams + paramsToSend;
			campReportSavedDay = -1;
			cmpReportLastParams = "";
		}

		runPythonScript("AdNetworkDailyNotification " + paramsToSend, false);
	}

	/**
	 * The SimulationStatus message received on day n indicates that the
	 * calculation time is up and the agent is requested to send its bid bundle
	 * to the AdX.
	 */
	private void handleSimulationStatus(SimulationStatus simulationStatus) {
		System.out.println("Day " + day + " : Simulation Status Received");
		sendBidAndAds();
		System.out.println(
				"from dailyNotification to send of bidBundle: " + (System.currentTimeMillis() - dailyNotificationTime));
		if (day == 60) {
			// DataToCSV.fillWithZeros(60);
			System.out.println("quitting");
			pipe("quit", false);
			try {
				inputStreamPythonApp.close();
				outputStreamPythonApp.close();
				errorStreamPythonApp.close();
			} catch (Exception err) {
				err.printStackTrace();
			}
		}

		++day;
	}

	/**
	 * Campaigns performance w.r.t. each allocated campaign
	 */
	private void handleCampaignReport(CampaignReport campaignReport) {
		campaignReports.add(campaignReport);

		String paramsToSend = Integer.toString(campaignReport.keys().size());
		System.out.println("Campaign Report:");

		/*
		 * for each campaign, the accumulated statistics from day 1 up to day
		 * n-1 are reported
		 */
		for (CampaignReportKey campaignKey : campaignReport.keys()) {
			int cmpId = campaignKey.getCampaignId();
			CampaignStats cstats = campaignReport.getCampaignReportEntry(campaignKey).getCampaignStats();
			myCampaigns.get(cmpId).setStats(cstats);

			String strToPrint = "Day " + day + ": Updating campaign " + cmpId + " stats: " + cstats.getTargetedImps()
					+ " tgtImps " + cstats.getOtherImps() + " nonTgtImps. Cost of imps is " + cstats.getCost();
			System.out.println(strToPrint);

			paramsToSend = paramsToSend + " " + Integer.toString(cmpId) + " "
					+ Double.toString(cstats.getTargetedImps()) + " " + Double.toString(cstats.getOtherImps()) + " "
					+ Double.toString(cstats.getCost());
		}

		// if(debugFlag)
		// System.out.println("DEBUG: run python - CampaignReport");
		// runPythonScript("CampaignReport " + paramsToSend);
		cmpReportLastParams = paramsToSend;
		campReportSavedDay = day;

	}

	/**
	 * Users and Publishers statistics: popularity and ad type orientation
	 */
	private void handleAdxPublisherReport(AdxPublisherReport adxPublisherReport) {
		System.out.println("Publishers Report: ");

		if (adxPublisherReport.keys().size() == 0 && DEBUG) {
			System.out.println("DEBUG: no entries in adxPublisherReport");
		}

		for (PublisherCatalogEntry publisherKey : adxPublisherReport.keys()) {
			AdxPublisherReportEntry entry = adxPublisherReport.getEntry(publisherKey);
			if (entry.getReservePriceBaseline() != 0.0) {
				System.out.println(entry.toString());
				System.out.println("reserved price baseline: " + entry.getReservePriceBaseline());
			}
		}
	}

	/**
	 * 
	 * @param AdNetworkReport
	 */
	private void handleAdNetworkReport(AdNetworkReport adnetReport) {

		// System.out.println("Day " + day + " : AdNetworkReport");
		/*
		 * for (AdNetworkKey adnetKey : adnetReport.keys()) {
		 * 
		 * double rnd = Math.random(); if (rnd > 0.95) { AdNetworkReportEntry
		 * entry = adnetReport .getAdNetworkReportEntry(adnetKey);
		 * System.out.println(adnetKey + " " + entry); } }
		 */
	}

	// #######################################
	// UTILS #
	// #######################################

	public static String getSegmentsInitials(String[] splitedSegments) {
		char[] letterForSegmet = new char[splitedSegments.length];
		String segmentToUse = "";

		for (int i = 0; i < splitedSegments.length; i++) {
			// System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$:
			// "+splitedSegments[i]);
			letterForSegmet[i] = splitedSegments[i].charAt(0);
		}

		for (int i = 0; i < 3; i++)
			for (int j = 0; j < letterForSegmet.length; j++) {
				String segOption;
				if (i == 0) {
					segOption = "OY";
					if (segOption.indexOf(letterForSegmet[j]) != -1)
						segmentToUse = segmentToUse + Character.toString(letterForSegmet[j]);
				}
				if (i == 1) {
					segOption = "MF";
					if (segOption.indexOf(letterForSegmet[j]) != -1)
						segmentToUse = segmentToUse + Character.toString(letterForSegmet[j]);
				}
				if (i == 2) {
					segOption = "HL";
					if (segOption.indexOf(letterForSegmet[j]) != -1)
						segmentToUse = segmentToUse + Character.toString(letterForSegmet[j]);
				}

			}
		return segmentToUse;
	}

	private int numOfCampaignsCompleted() {
		printProp();
		int retval = 0;
		for (Map.Entry<Integer, CampaignData> entry : myCampaigns.entrySet()) {
			if (entry.getValue().impsTogo() <= 0)
				retval++;
		}
		if (DEBUG) {
			System.out.println(
					"DEBUG(numOfCampaignsCompleted) - day:" + day + " number of campaigns completed:" + retval);
		}
		return retval;
	}

	private void printProp() {
		if (DEBUG) {
			System.out.println("DEBUG(printProp): the day is: " + day);
			for (Map.Entry<Integer, CampaignData> entry : myCampaigns.entrySet()) {
				System.out.println("~~~~~~~~~~~~~~Campain_Properties~~~~~~~~~~~~~~~~~");
				System.out.println(entry.getValue().budget);
				System.out.println(entry.getValue().dayStart);
				System.out.println(entry.getValue().dayEnd);
				System.out.println(entry.getValue().id);
				System.out.println(entry.getValue().stats);
				System.out.println(entry.getValue().targetSegment);
				System.out.println("impsToGo: " + entry.getValue().impsTogo());
				System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			}
		}

	}

	public static Set<MarketSegment> createSegmentFromPython(String pythonSegName) {
		MarketSegment m1, m2, m3;
		if (pythonSegName.charAt(0) == 'O')
			m1 = MarketSegment.OLD;
		else
			m1 = MarketSegment.YOUNG;

		if (pythonSegName.charAt(1) == 'M')
			m2 = MarketSegment.MALE;
		else
			m2 = MarketSegment.FEMALE;

		if (pythonSegName.charAt(2) == 'H')
			m3 = MarketSegment.HIGH_INCOME;
		else
			m3 = MarketSegment.LOW_INCOME;

		return MarketSegment.compundMarketSegment3(m1, m2, m3);
	}

	protected void sendBidAndAds() {
		try {

			// if(debugFlag)
			// System.out.println("DEBUG: run python - GetBidBundle");

			// String outputString = runPythonScript("GetBidBundle");

			// String outputString = bidBundleScriptOutput;

			// if(debugFlag)
			// System.out.println("DEBUG: output python - GetBidBundle\n" +
			// outputString);
			if (dayLastCampOpp != day) {
				if (debugFlag)
					System.out.println("DEBUG: run python - GetBidBundle");

				String outputString = runPythonScript("GetBidBundle", true);
				System.out.println("sendBidAndAds: got outputString with: " + outputString);

				if (outputString != null) {

					bidBundle = new AdxBidBundle();

					int dayBiddingFor = day + 1;

					JSONObject JbidBundle = new JSONObject(outputString);
					JSONArray JbidsArray = JbidBundle.getJSONArray("bidbundle");
					JSONObject JbidBundleElement;
					AdxQuery query, emptyQuery;
					Device device;
					AdType adtype;
					for (int i = 0; i < JbidsArray.length(); i++) {
						JbidBundleElement = JbidsArray.getJSONObject(i);
						JSONObject JQuery = JbidBundleElement.getJSONObject("query");
						if (JQuery.getString("Device").equals("Desktop"))
							device = Device.pc;
						else
							device = Device.mobile;
						if (JQuery.getString("adType").equals("Text"))
							adtype = AdType.text;
						else
							adtype = AdType.video;

						for (String publisherName : publisherNames) {
							String marketSegmentName = JQuery.getJSONArray("marketSegments").getJSONObject(0)
									.getString("segmentName");
							Set<MarketSegment> segment;

							if (marketSegmentName.compareTo("Unknown") == 0) // equals
							{
								segment = new HashSet<MarketSegment>();
							} else {
								segment = createSegmentFromPython(marketSegmentName);
							}
							query = new AdxQuery(publisherName, segment, device, adtype);

							bidBundle.addQuery(query, Double.parseDouble(JbidBundleElement.getString("bid")),
									new Ad(null), JbidBundleElement.getInt("campaignId"),
									JbidBundleElement.getInt("weight"),
									Double.parseDouble(JbidBundleElement.getString("dailyLimit")));
						}

					}
				}

				else {
					System.out.println("getBidBundle returned null");
					return;
				}
			}

			if (bidBundle != null) {
				System.out.println("Day " + day + ": Sending BidBundle");
				sendMessage(adxAgentAddress, bidBundle);
			}

		} catch (Exception e) {
			System.out.println("exception happened at sendBidAndAds " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}

	}

	/* UNUSED */
	@Override
	protected void simulationSetup() {
		Random random = new Random();

		day = 0;
		bidBundle = new AdxBidBundle();

		/* initial bid between 0.1 and 0.2 */
		ucsBid = 0.1 + random.nextDouble() / 10.0;

		myCampaigns = new HashMap<Integer, CampaignData>();
		log.fine("AdNet " + getName() + " simulationSetup");

	}

	/* UNUSED */
	@Override
	protected void simulationFinished() {
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
	private void generateAdxQuerySpace() {
		if (publisherCatalog != null && queries == null) {
			Set<AdxQuery> querySet = new HashSet<AdxQuery>();

			/*
			 * for each web site (publisher) we generate all possible variations
			 * of device type, ad type, and user market segment
			 */
			for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog) {
				String publishersName = publisherCatalogEntry.getPublisherName();

				for (MarketSegment userSegment : MarketSegment.values()) {
					Set<MarketSegment> singleMarketSegment = new HashSet<MarketSegment>();
					singleMarketSegment.add(userSegment);

					querySet.add(new AdxQuery(publishersName, singleMarketSegment, Device.mobile, AdType.text));

					querySet.add(new AdxQuery(publishersName, singleMarketSegment, Device.pc, AdType.text));

					querySet.add(new AdxQuery(publishersName, singleMarketSegment, Device.mobile, AdType.video));

					querySet.add(new AdxQuery(publishersName, singleMarketSegment, Device.pc, AdType.video));
				}

				/**
				 * An empty segments set is used to indicate the "UNKNOWN"
				 * segment such queries are matched when the UCS fails to
				 * recover the user's segments.
				 */
				querySet.add(new AdxQuery(publishersName, new HashSet<MarketSegment>(), Device.mobile, AdType.video));
				querySet.add(new AdxQuery(publishersName, new HashSet<MarketSegment>(), Device.mobile, AdType.text));
				querySet.add(new AdxQuery(publishersName, new HashSet<MarketSegment>(), Device.pc, AdType.video));
				querySet.add(new AdxQuery(publishersName, new HashSet<MarketSegment>(), Device.pc, AdType.text));
			}
			queries = new AdxQuery[querySet.size()];
			querySet.toArray(queries);
		}
	}

	/*
	 * generates an array of the publishers names
	 */
	private void getPublishersNames() {
		if (null == publisherNames && publisherCatalog != null) {
			ArrayList<String> names = new ArrayList<String>();
			for (PublisherCatalogEntry pce : publisherCatalog) {
				names.add(pce.getPublisherName());
			}

			publisherNames = new String[names.size()];
			names.toArray(publisherNames);
		}
	}

	/*
	 * generates the campaign queries relevant for the specific campaign, and
	 * assign them as the campaigns campaignQueries field
	 */
	private void genCampaignQueries(CampaignData campaignData) {
		Set<AdxQuery> campaignQueriesSet = new HashSet<AdxQuery>();
		for (String PublisherName : publisherNames) {
			campaignQueriesSet.add(new AdxQuery(PublisherName, campaignData.targetSegment, Device.mobile, AdType.text));
			campaignQueriesSet
					.add(new AdxQuery(PublisherName, campaignData.targetSegment, Device.mobile, AdType.video));
			campaignQueriesSet.add(new AdxQuery(PublisherName, campaignData.targetSegment, Device.pc, AdType.text));
			campaignQueriesSet.add(new AdxQuery(PublisherName, campaignData.targetSegment, Device.pc, AdType.video));
		}

		campaignData.campaignQueries = new AdxQuery[campaignQueriesSet.size()];
		campaignQueriesSet.toArray(campaignData.campaignQueries);
		// System.out.println("!!!!!!!!!!!!!!!!!!!!!!"+Arrays.toString(campaignData.campaignQueries)+"!!!!!!!!!!!!!!!!");
	}

	// ###################################
	// OBSOLETE FUNCTIONS #
	// ###################################

	private void handleCampaignAuctionReport(CampaignAuctionReport content) {
		// ingoring - this message is obsolete
	}

	// ###################################
	// UNUSED FUNCTIONS #
	// ###################################

	/**
	 * currently works for n+2 returns the open campaigns sorted by:
	 * 
	 * @return
	 */
	// UNUSED
	private LinkedList<CampaignData> getAllOpenCampaignsAtDayPlus(int n)
	{
		int eday = day + n;
		LinkedList<CampaignData> retList = new LinkedList<>();
		for (Map.Entry<Integer, CampaignData> entry : myCampaigns.entrySet()) {
			if (entry.getValue().dayStart <= eday && entry.getValue().dayEnd >= eday) {
				retList.add(entry.getValue());
			}
		}
		return retList;
	}
	
	//###########################
	//		Sub-Classes			#
	//###########################
	
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
		private AdxQuery[] campaignQueries;// array of queries relevant for the
											// campaign.

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

		public void setBudget(double d) {
			budget = d;
		}

		public double getBudget() {
			return budget;
		}

		public CampaignData(CampaignOpportunityMessage com) {
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
		public String toString() {
			return "Campaign ID " + id + ": " + "day " + dayStart + " to " + dayEnd + " " + targetSegment + ", reach: "
					+ reachImps + " coefs: (v=" + videoCoef + ", m=" + mobileCoef + ")";
		}

		int impsTogo() {
			return (int) Math.max(0, reachImps - stats.getTargetedImps());
		}

		void setStats(CampaignStats s) {
			stats.setValues(s);
		}

		public AdxQuery[] getCampaignQueries() {
			return campaignQueries;
		}

		public void setCampaignQueries(AdxQuery[] campaignQueries) {
			this.campaignQueries = campaignQueries;
		}

		/*
		 * public int accurityCalc() { char[] flags =
		 * CampaignStatus.segParser(this.targetSegment.toString()); int count =
		 * 0; for (int i =0; i < 8; i++){ if (flags[i] == '1') { count +=
		 * population[i]; } }
		 * 
		 * long dayToGo = this.dayEnd - day; return
		 * (int)(Math.ceil(this.impsTogo()/(count*dayToGo))); }
		 */
	}
}
