# -*- coding: utf-8 -*-
"""
Created on Mon Mar 27 17:56:15 2017

@author: Eyal
"""
import sys
import pickle
from CampaignClass import Campaign
from AgentClass import Agent
from SegmentClass import MarketSegment
from UcsManagerClass import ucsManager

class Game:
    def __init__(self, agent, campaigns):
        self.validity = True
        self.agent = agent
        self.campaigns = campaigns
        self.day = 1
        

class Communicator:
    
    def __init__(self, queryName, argsList):
        self.queryName = queryName
        self.argsList = argsList
        self.game = Game(Agent("PineApple"), {})
    
    def loadPickle(self):
        self.game = pickle.load( open("pickle//game.p", "rb"))
        
        MarketSegment.segments_init()
        Campaign.statistic_campaigns_init()
        if self.game.validity == False:                 #New Game
            self.game.validity = True
            self.game.campaigns = {}
            self.game.agent = Agent("PineApple")
           
        else:                                           #Continue Game
            Campaign.setCampaigns(self.game.campaigns)
            
            
    def dumpPickle(self):
        pickle.dump( self.game, open( "pickle//game.p", "wb" ) ) #TODO: dump final state, not like now
    
    def handleInitPickle(self):
        self.game = Game(Agent("PineApple"),{})
        self.dumpPickle()
        
    def handleGetUCSBid(self):
        day = self.game.day
        ongoingCamps = self.game.agent.getOnGoingCampaigns(day) #TODO: what day?
        ucsLevel = ucsManager.get_desired_UCS_level(self.game.day, ongoingCamps)
        
        numberOfActiveNetworks = 0 #TODO
        numberOfLastDayNetworks = 0 #TODO
        ucsBid = ucsManager.predict_required_price_to_win_desired_UCS_level(
                ucsLevel, day, numberOfActiveNetworks, numberOfLastDayNetworks)
        
        print(ucsBid)
        pass
    
    def handleGetBidBundle(self):
        pass
    
    def handleInitialCampaignMessage(self):
        cid = int(self.argsList[0])
        reach = int(self.argsList[1])
        startDay, endDay = int(self.argsList[2]), int(self.argsList[3])
        segmentList = MarketSegment.getSegmentListFromStr(self.argsList[4])
        vidCoeff = float(self.argsList[5])
        mobileCoeff = float(self.argsList[6])
        budgetMillis = float(self.argsList[7])
        initialCampaign = Campaign(cid, startDay, endDay, segmentList,
                                   reach, vidCoeff, mobileCoeff, '')
        
        initialCampaign.assignCampaign(self.game.agent,
                                       { "Q_old" : 1 },
                                       budgetMillis) #TODO: what is the starting quality
    
    def handleGetCampaignBudgetBid(self):
        print("ArgsList is {}".format(self.argsList))
        cid = int(self.argsList[0])
        reach = int(self.argsList[1])
        startDay, endDay = int(self.argsList[2]), int(self.argsList[3])
        segmentList = MarketSegment.getSegmentListFromStr(self.argsList[4])
        vidCoeff = float(self.argsList[5])
        mobileCoeff = float(self.argsList[6])
        day = int(self.argsList[7])
        
        campaignInOpportunity = Campaign(cid, startDay, endDay, segmentList,
                                   reach, vidCoeff, mobileCoeff, '')
        
        initialBudget = self.game.agent.campaignOpportunityBid(campaignInOpportunity)
        Campaign.initialize_campaign_profitability_predictor()
        profitability = campaignInOpportunity.predict_campaign_profitability(day)
        if (profitability == -1):
            print((campaignInOpportunity.reach*self.game.agent.quality) - 0.1)
        else:
            print(initialBudget)

    def handleCampaignReport(self):
        pass
    
    def handleAdNetworkDailyNotification(self):
        pass
    
    def handleAdxPublisherReport(self):
        pass
    
    def handlePublisherCatalog(self):
        pass
    
    def handleAdNetworkReport(self):
        pass
    
    def handleStartInfo(self):
        pass
    
    def handleBankStatus(self):
        pass
      
    handlers = {
                "initPickle" : handleInitPickle,
                "GetUCSBid": handleGetUCSBid,
                "GetBidBundle": handleGetBidBundle,
                "InitialCampaignMessage": handleInitialCampaignMessage,
                "GetCampaignBudgetBid": handleGetCampaignBudgetBid,
                "CampaignReport": handleCampaignReport,
                "AdNetworkDailyNotification": handleAdNetworkDailyNotification,
                "AdxPublisherReport": handleAdxPublisherReport,
                "PublisherCatalog": handlePublisherCatalog,
                "AdNetworkReport": handleAdNetworkReport,
                "StartInfo": handleStartInfo,
                "BankStatus": handleBankStatus
                }
    
    def handleQuery(self):
        handler = Communicator.handlers[self.queryName]
        handler(self)
    
def main(queryName, argsList):
    if queryName in Communicator.handlers:
        communicator = Communicator(queryName, argsList)
        try:
            communicator.loadPickle()
        except Exception:
            print("Error Loading Pickle")

        communicator.handleQuery()
        communicator.dumpPickle()
        
    else:
        print("Unexpected query: {}".format(queryName))
        sys.exit()

if __name__ == "__main__":
    if len(sys.argv[1:]) > 0:
        main(sys.argv[1], sys.argv[2:])
    else:
        print("Expected a query name!")
        sys.exit()