package collect_profit_data;

import java.util.List;

public class DayStatus {
	
	private int game;
	private String day;
	private List<String> ucsLevels;
	private String index;

	public DayStatus(){
		
	}

	// ***** GETTERS & SETTERS *****
	
	public Integer getGame() {
		return game;
	}

	public void setGame(Integer game) {
		this.game = game;
	}
	
	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public List<String> getUcsLevels() {
		return ucsLevels;
	}

	public void setUcsLevels(List<String> ucsLevels) {
		this.ucsLevels = ucsLevels;
	}
	
	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}
	
/*	public String createLine(){
		String ltw=null;
		char [] flags = this.segmentToMask();
		this.calcpercentage();
		ltw = this.cid + ',' + Integer.toString(game) + ',' + this.day + ',' + this.budget + ',' + this.start + ',' + this.end + ',' + this.vidCoeff + ',' + this.mobCoeff + ',' + this.reach + ',' + ' ' + ',' + flags[0] + ',' + flags[1] + ',' + flags[2] + ',' + flags[3] + ',' + flags[4] + ',' + flags[5] + ',' + flags[6] + ',' + flags[7] + ',' +this.percent + ',' + "??" + ',' + " " + ',' + this.revenue + ',' + this.budgetMillis + ',' + this.tartgetedImps + ',' + this.other + ',' + this.cost + ',' + this.ERR ;
		// cid,#sim,day,budget,start,end,vidCoeff,mobCoeff,reach,demand,OML,OMH,OFL,OFH,YML,YMH,YFL,YFH,completion_percentage,profit,decision,revenue,budgetMillis,tartgetedImps,other,cost,ERR
		return ltw;
	}*/
}
