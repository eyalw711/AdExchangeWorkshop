# -*- coding: utf-8 -*-
"""
Created on Mon Mar 27 17:56:15 2017

@author: Eyal
"""
from __future__ import print_function
import sys
import os
import pickle
import json
from CampaignClass import Campaign
from AgentClass import Agent
from SegmentClass import MarketSegment
from UcsManagerClass import ucsManager


def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)
    
    
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
            eprint("loadPickle: ", str(e), " making a new game since pickle isn't found")
            self.game = Game()
        
        MarketSegment.segments_init()
        Campaign.statistic_campaigns_init()
        Campaign.setCampaigns(self.game.campaigns)
            
            
    def dumpPickle(self):
        #update game:
        self.game.campaigns = Campaign.campaigns
        with open( "pickle//game.p", "wb" ) as pickleFile:
            pickle.dump( self.game, pickleFile )
        with open( "pickle//games.json", "a+") as jsonGames:
            json.dump(self.game, jsonGames)
    
    def handleGetUcsAndBudget(self):
        eprint("handleGetUcsAndBudget: ArgsList is {}".format(self.argsList))
        answer = {}
        
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
            print(json.dumps({"budgetBid":(camp.reach*self.game.agent.quality) - 0.1})) #NEEDED #TODO
        else:
            print(json.dumps({"budgetBid":initialBudget})) #NEEDED
            
#        day = self.game.day
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
        
        print(json.dumps(answer, separators=(',', ':'))) #NEEDED
        
        
    
    def handleInitialCampaignMessage(self):
        cid = int(self.argsList[0])
        reach = int(self.argsList[1])
        startDay, endDay = int(self.argsList[2]), int(self.argsList[3])
        segmentList = MarketSegment.getSegmentListFromStr(self.argsList[4])
        vidCoeff = float(self.argsList[5])
        mobileCoeff = float(self.argsList[6])
        budgetMillis = float(self.argsList[7])
        initialCampaign = Campaign(cid, startDay, endDay, segmentList,
                                   reach, vidCoeff, mobileCoeff)
        
        initialCampaign.assignCampaign(self.game.agent,
                                       { "Q_old" : 1.0 },  #The starting quality is 1.0
                                       budgetMillis)
        #experiement:
        eprint("handleInitialCampaignMessage: NOTICE: making a wild assumption about initial campaigns! Think about this!")
        otherInitialCampaigns = [Campaign("i{}".format(i), startDay, endDay,
                                          segmentList, reach, vidCoeff,
                                          mobileCoeff) for i in range(7)]
        for (inx,camp) in enumerate(otherInitialCampaigns):
            camp.assignCampaign(self.game.opponents[inx], None, budgetMillis)
            
    def handleGetBidBundle(self):
        answer = {}
        bidBundle = self.game.agent.formBidBundle(self.game.day+1)
        answer["bidbundle"] = bidBundle
        print(json.dumps(answer, separators=(',', ':'))) #NEEDED

    def handleCampaignReport(self):
        number_of_campaign_stats = int(self.argsList[0])
        for i in range(number_of_campaign_stats):
            cid = int(self.argsList[1 + 4*i])
            targetedImpressions = int(float(self.argsList[2+4*i]))
#           nonTargetedImpressions = self.argsList[3+4*i]
#           cost = self.argsList[4+4*i]
            #update:
            if cid in self.game.agent.my_campaigns:
                self.game.agent.my_campaigns[cid].targetedImpressions = targetedImpressions
            if cid in Campaign.campaigns:
                Campaign.campaigns[cid].targetedImpressions = targetedImpressions
                
    
    def handleAdNetworkDailyNotification(self):
        self.game.day = int(self.argsList[0])
        self.game.agent.dailyUCSLevel = float(self.argsList[1])
        #price of UCS in argList[3] (dont care)
        oldQuality = self.game.agent.quality
        self.game.agent.quality = float(self.argsList[3])
        
        cid = int(self.argsList[4])
        try:
            eprint("handleAdNetworkDailyNotification processed args:" ,self.game.day,
                   self.game.agent.dailyUCSLevel,
                   self.game.agent.quality,
                   cid,
                   self.argsList[5],
                   int(self.argsList[6]))
        except Exception as e:
            eprint("handleAdNetworkDailyNotification failed! ", str(e))
            return
        
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
            eprint("removed last pickle")
        except OSError:
            eprint("Unable to remove pickle")
            
    
    def handleBankStatus(self):
        '''currently undefined impl'''
        pass
    
    def handleGetGameStatus(self):
        '''DEBUG'''
        eprint("handleGetGameStatus: day ", self.game.day)
        eprint("handleGetGameStatus: agent ", self.game.agent)
        
    handlers = {
                "GetGameStatus": handleGetGameStatus,
                "GetUcsAndBudget": handleGetUcsAndBudget,
                "InitialCampaignMessage": handleInitialCampaignMessage,
                "GetBidBundle": handleGetBidBundle,
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
    origPath = os.getcwd()
    try:
        os.chdir(origPath + "//PinePy")
    except Exception:
        eprint("couldn't change to dir", origPath + "//PinePy")
    
    if queryName in Communicator.handlers:
        communicator = Communicator(queryName, argsList)
        try:
            communicator.loadPickle()
        except Exception as e:
            template = "An exception of type {0} occurred. Arguments:\n{1!r}"
            message = template.format(type(e).__name__, e.args)
            eprint("main: Error Loading Pickle: ", message)
            return

        communicator.handleQuery()
        communicator.dumpPickle()
        os.chdir(origPath)
        
    else:
        eprint("Unexpected query: {}".format(queryName))
        sys.exit()

if __name__ == "__main__":
    if len(sys.argv[1:]) > 0:
        main(sys.argv[1], sys.argv[2:])
    else:
        eprint("Expected a query name!")
        sys.exit()