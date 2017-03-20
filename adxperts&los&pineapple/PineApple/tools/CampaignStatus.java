package tools;

public class CampaignStatus {
	private int game;
	private String cid;
	private String day;
	private String budget;
	private String budgetMillis;
	private String start;
	private String end;
	private String vidCoeff;
	private String mobCoeff;
	private String reach;
	private String demand;
	private String publisher;
	private String segments;
	private String decision;
	private String percent;
	private String tartgetedImps;
	private String other;
	private String cost;
	private String ERR;
	private String revenue;

	public CampaignStatus(){
		
	}

	// ***** GETTERS & SETTERS *****
	
	public String getRevenue() {
		return revenue;
	}

	public void setRevenue(String revenue) {
		this.revenue = revenue;
	}

	public int getGame() {
		return game;
	}

	public void setGame(int game) {
		this.game = game;
	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public String getBudgetMillis() {
		return budgetMillis;
	}

	public void setBudgetMillis(String budgetMillis) {
		this.budgetMillis = budgetMillis;
	}

	public String getStart() {
		return start;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public String getBudget() {
		return budget;
	}

	public void setBudget(String budget) {
		this.budget = budget;
	}

	public String getEnd() {
		return end;
	}

	public void setEnd(String end) {
		this.end = end;
	}

	public String getVidCoeff() {
		return vidCoeff;
	}

	public void setVidCoeff(String vidCoeff) {
		this.vidCoeff = vidCoeff;
	}

	public String getReach() {
		return reach;
	}

	public void setReach(String reach) {
		this.reach = reach;
	}

	public String getMobCoeff() {
		return mobCoeff;
	}

	public void setMobCoeff(String mobCoeff) {
		this.mobCoeff = mobCoeff;
	}

	public String getDemand() {
		return demand;
	}

	public void setDemand(String demand) {
		this.demand = demand;
	}

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public String getSegments() {
		return segments;
	}

	public void setSegments(String segments) {
		this.segments = segments;
	}

	public String getDecision() {
		return decision;
	}

	public void setDecision(String decision) {
		this.decision = decision;
	}

	public String getpercent() {
		return percent;
	}

	public void setpercent(String percent) {
		this.percent = percent;
	}

	public String getTartgetedImps() {
		return tartgetedImps;
	}

	public void setTartgetedImps(String tartgetedImps) {
		this.tartgetedImps = tartgetedImps;
	}

	public String getOther() {
		return other;
	}

	public void setOther(String other) {
		this.other = other;
	}

	public String getCost() {
		return cost;
	}

	public void setCost(String cost) {
		this.cost = cost;
	}

	public String getERR() {
		return ERR;
	}

	public void setERR(String eRR) {
		ERR = eRR;
	}
	
	//more functions
	
	public static char[] segParser(String seg){
		char [] flags = new char[8];
		
		for(int i=0;i<8;i++){
			flags[i] = '1';
		}
		
		if (seg.contains("OLD")){
			for(int i=4;i<8;i++){
				flags[i] = '0';
			}
		}
		else if(seg.contains("YOUNG")){
			for(int i=0;i<4;i++){
				flags[i] = '0';
			}
		}
							
		if (seg.contains("LOW")){
			for(int i=1;i<8;i+=2){
				flags[i] = '0';
			}
		}
		else if(seg.contains("HIGH")){
			for(int i=0;i<8;i+=2){
				flags[i] = '0';
			}
		}
		if (seg.contains("MALE")){
			for(int i=2;i<4;i++){
				flags[i] = '0';
				flags[i+4] = '0';
			}
		}
		else if(seg.contains("FEMALE")){
			for(int i=0;i<2;i++){
				flags[i] = '0';
				flags[i+4] = '0';
			}
		}
		return flags;
	}
	
	public char[] segmentToMask(){
		String seg = this.segments;
		return segParser(seg);
	}
	
	public String createLine(){
		String ltw=null;
		char [] flags = this.segmentToMask();
		this.calcpercentage();
		ltw = this.cid + ',' + Integer.toString(game) + ',' + this.day + ',' + this.budget + ',' + this.start + ',' + this.end + ',' + this.vidCoeff + ',' + this.mobCoeff + ',' + this.reach + ',' + ' ' + ',' + ' ' + ',' + flags[0] + ',' + flags[1] + ',' + flags[2] + ',' + flags[3] + ',' + flags[4] + ',' + flags[5] + ',' + flags[6] + ',' + flags[7] + ',' + this.percent + ',' + "??" + ',' + " " + ',' + this.revenue + ',' + this.budgetMillis + ',' + this.tartgetedImps + ',' + this.other + ',' + this.cost + ',' + this.ERR ;
		
		return ltw;
	}
	
	public void calcpercentage(){
		double percent;
		percent = (double)(Double.parseDouble(this.tartgetedImps) / Double.parseDouble(this.reach));
		if (percent > 1)
			this.percent = "1";
		else
			this.percent = Double.toString(percent);
	}
}
