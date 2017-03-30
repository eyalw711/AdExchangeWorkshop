# -*- coding: utf-8 -*-
"""
Created on Mon Mar 27 17:56:15 2017

@author: Eyal
"""
import sys
import os
import pickle
import json
from CampaignClass import Campaign
from AgentClass import Agent
from SegmentClass import MarketSegment
from UcsManagerClass import ucsManager

class Game:
    def __init__(self):
        self.agent = Agent("PineApple")
        self.opponents = [Agent(str(i)) for i in range(7)]
        self.campaigns = {}
        self.campaignOffer = None
        self.day = 1
        

class Communicator:
    
    def __init__(self, queryName, argsList):
        self.queryName = queryName
        self.argsList = argsList
        self.game = Game()
    
    def loadPickle(self):
        try:
            self.game = pickle.load( open("pickle//game.p", "rb"))
        except (OSError, IOError) as e:
            self.game = Game()
        
        MarketSegment.segments_init()
        Campaign.statistic_campaigns_init()
        Campaign.setCampaigns(self.game.campaigns)
            
            
    def dumpPickle(self):
        #update game:
        self.game.campaigns = Campaign.campaigns
        pickle.dump( self.game, open( "pickle//game.p", "wb" ) ) #TODO: dump final state, not like now
    
    def handleGetUcsAndBundle(self):
        answer = {}
        day = self.game.day
        ongoingCamps = self.game.agent.getOnGoingCampaigns(day+1)
        ucsLevel = ucsManager.get_desired_UCS_level(day+1, ongoingCamps)
        
        numberOfActiveNetworks = sum(
                [1 for opponent in self.game.opponents if any(camp.activeAtDay(day+1)
                    for camp in opponent.getOnGoingCampaigns(day+1))])
        numberOfLastDayNetworks = sum(
                [1 for opponent in self.game.opponents if any(camp.endDay == day+1
                    for camp in opponent.getOnGoingCampaigns(day+1))])
        ucsBid = ucsManager.predict_required_price_to_win_desired_UCS_level(
                ucsLevel, day, numberOfActiveNetworks, numberOfLastDayNetworks)
        
        answer["UCSBid"] = float(ucsBid)
        
        bidBundle = self.game.agent.formBidBundle(day+1)
        answer["bidbundle"] = bidBundle
        
        print(json.dumps(answer, separators=(',', ':')))
    
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
        #experiement:
        otherInitialCampaigns = [Campaign("i{}".format(i), startDay, endDay,
                                          segmentList, reach, vidCoeff,
                                          mobileCoeff, '') for i in range(7)]
        for (inx,camp) in enumerate(otherInitialCampaigns):
            camp.assignCampaign(self.game.opponents[inx], None, budgetMillis)
            
    def handleGetCampaignBudgetBid(self):
        print("#handleGetCampaignBudgetBid: ArgsList is {}".format(self.argsList))
        cid = int(self.argsList[0])
        reach = int(self.argsList[1])
        startDay, endDay = int(self.argsList[2]), int(self.argsList[3])
        segmentList = MarketSegment.getSegmentListFromStr(self.argsList[4])
        vidCoeff = float(self.argsList[5])
        mobileCoeff = float(self.argsList[6])
        day = int(self.argsList[7])
        
        camp = Campaign(cid, startDay, endDay, segmentList,
                                   reach, vidCoeff, mobileCoeff)
        self.game.campaignOffer = camp
        
        initialBudget = self.game.agent.campaignOpportunityBid(camp)
        Campaign.initialize_campaign_profitability_predictor()
        profitability = camp.predict_campaign_profitability(day)
        if (profitability == -1):
            print(json.dumps({"budgetBid":(camp.reach*self.game.agent.quality) - 0.1})) #TODO
        else:
            print(json.dumps({"budgetBid":initialBudget}))

    def handleCampaignReport(self):
        number_of_campaign_stats = int(self.argsList[0])
        for i in range(number_of_campaign_stats):
            cid = int(self.argsList[1 + 4*i])
            targetedImpressions = int(self.argsList[2+4*i])
#            nonTargetedImpressions = self.argsList[3+4*i]
#            cost = self.argsList[4+4*i]
            #update:
            if cid in self.game.agent.my_campaigns:
                self.game.agent.my_campaigns[cid].targetedImpressions = targetedImpressions
            if cid in Campaign.campaigns:
                Campaign.campaigns[cid].targetedImpressions = targetedImpressions
                
    
    def handleAdNetworkDailyNotification(self):
        self.game.day = int(self.argsList[0])
        self.game.agent.dailyUCSLevel = float(self.argsList[1])
        #price of UCS dont care
        oldQuality = self.game.agent.quality
        self.game.agent.quality = float(self.argsList[3])
        
        cid = int(self.argsList[4])
        #TODO: Verify assign correctly
        cmp = self.game.campaignOffer
        if self.argsList[5] == self.game.agent.name: #TODO: dangerous, check
            cmp.assignCampaign(self.game.agent, goalObject = {"Q_old":oldQuality}, budget = int(self.argsList[6]))
        else:
            cmp.assignCampaign(self.game.opponents[0], goalObject = None, budget = int(self.argsList[6])) #TODO: change assignee to real one
    
    def handleAdxPublisherReport(self):
        '''currently undefined impl'''
        pass
    
    def handlePublisherCatalog(self):
        '''currently undefined impl'''
        pass
    
    def handleAdNetworkReport(self):
        '''currently undefined impl'''
        pass
    
    def handleStartInfo(self):
        #TODO: Assume OK to delete here
        try:
            os.remove("pickle//game.p")
        except OSError:
            pass
            
    
    def handleBankStatus(self):
        '''currently undefined impl'''
        pass
    
    def handleGetGameStatus(self):
        '''DEBUG'''
        print("handleGetGameStatus: day ", self.game.day)
        print("handleGetGameStatus: agent ", self.game.agent)
        
    handlers = {
                "GetGameStatus": handleGetGameStatus,
                "GetUcsAndBundle": handleGetUcsAndBundle,
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