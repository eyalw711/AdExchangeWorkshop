# -*- coding: utf-8 -*-
"""
Created on Mon Mar 27 17:56:15 2017

@author: Eyal
"""
from __future__ import print_function
import sys
import os
import traceback
import pickle
import json
from CampaignClass import Campaign
from AgentClass import Agent
from SegmentClass import MarketSegment
from UcsManagerClass import ucsManager
import time

def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)
    with open("runlog.log", "a+") as logFile:
        print(*args, file=logFile, **kwargs)
        #logFile.write(*args)

def printException(e, functionName, whileDoingString):
    template = "While " + whileDoingString + " an exception of type {0} occurred. Arguments:\n{1!r}"
    message = template.format(type(e).__name__, e.args)
    eprint(functionName, ": Error Loading Pickle: ", message)
    
class Game:
    def __init__(self):
        self.agent = Agent("PineApple")
        self.opponents = [Agent("") for i in range(7)]
        self.campaigns = {}
        self.campaignOffer = None
        self.day = 0
        
    def getGameActiveCampaignsCampaignsList(self):
        '''active campaigns today'''
        return list(filter(lambda x: x.activeAtDay(self.day), list(self.campaigns.values())))
        
    
     
    def printStatus(self):
        eprint("\nSUMMARY STATUS MESSAGE <DAY {}>".format(self.day))
        eprint("Market Segments, Sizes, and Demands:")
        
        def segByName(segName):
            return MarketSegment.segments[segName]
        
        eprint([(segByName(name).name, segByName(name).size, segByName(name).segment_demand(self.day, self.getGameActiveCampaignsCampaignsList())) for name in MarketSegment.segment_names])
        
        eprint("\nAll Active Campaigns in the Game ({} Campaigns)".format(len(self.getGameActiveCampaignsCampaignsList())))
        for agent in [self.agent]+self.opponents:
            agentActiveCampsList = list(filter( lambda x: x.activeAtDay(self.day), list(agent.my_campaigns.values())))
            eprint("\nCampaigns of Agent {} ({} Campaigns)".format(agent.name if agent.name != "" else "Unnamed", len(agentActiveCampsList)))
            eprint("\n".join(map(lambda x: str(x), agentActiveCampsList)))
                


class Communicator:
    
    def __init__(self, queryName, argsList):
        self.queryName = queryName
        self.argsList = argsList
        self.game = Game()
    
    def loadPickle(self):
        loaded = True
        try:
            self.game = pickle.load( open("pickle//game.p", "rb"))
        except (OSError, IOError) as e:
            loaded = False
            eprint("loadPickle: ", str(e), " making a new game since pickle isn't found")
            self.game = Game()
        
        try:
            MarketSegment.segments_init()
            Campaign.statistic_campaigns_init()
            #eprint("#TODO: bring back to life again when can") #Campaign.statistic_campaigns_init() #TODO: bring back to life again when can
            if loaded:
                Campaign.setCampaigns(self.game.campaigns)
        except KeyError as e:
            template = "While initializing stuff an exception of type {0} occurred. Arguments:\n{1!r}"
            message = template.format(type(e).__name__, e.args)
            eprint("loadPickle: Error Loading Pickle: ", message)  
            
            
    def dumpPickle(self):
        if self.queryName == "StartInfo":
            return

        with open( "pickle//game.p", "wb" ) as pickleFile:
            pickle.dump( self.game, pickleFile)
            
    
    def handleGetUcsAndBudget(self):
        eprint("handleGetUcsAndBudget: ArgsList is {}".format(self.argsList))
        answer = {}
        
        cid = int(self.argsList[0])
        reach = int(self.argsList[1])
        startDay, endDay = int(self.argsList[2]), int(self.argsList[3])
        segmentNamesList = MarketSegment.getSegmentListNamesFromStr(self.argsList[4])
        vidCoeff = float(self.argsList[5])
        mobileCoeff = float(self.argsList[6])
        day = int(self.argsList[7])
        
        camp = Campaign(cid, startDay, endDay, segmentNamesList,
                                   reach, vidCoeff, mobileCoeff)
        self.game.campaignOffer = camp
        
        initialBudget = self.game.agent.campaignOpportunityBid(camp)
        eprint("handleGetUcsAndBudget: initialize_campaign_profitability_predictor")
        Campaign.initialize_campaign_profitability_predictor()
        
        eprint("handleGetUcsAndBudget: predict_campaign_profitability in")
        profitability, final_budget  = camp.predict_campaign_profitability(day,initialBudget,self.game.agent.quality)
        eprint("handleGetUcsAndBudget: predict_campaign_profitability out")
        
        if (profitability == -1):
            answer["budgetBid"] = str(int((camp.reach*self.game.agent.quality) - 1))
        else:
            answer["budgetBid"] = str(int(final_budget))
            
        ongoingCamps = self.game.agent.getOnGoingCampaigns(day+1)
        ucsLevel = ucsManager.get_desired_UCS_level(day+1, ongoingCamps)
        eprint("handleGetUcsAndBudget: get_desired_UCS_level out")
       
        numberOfActiveNetworks = sum(
                [1 for opponent in self.game.opponents if any(camp.activeAtDay(day+1)
                    for camp in opponent.getOnGoingCampaigns(day+1))])
        numberOfLastDayNetworks = sum(
                [1 for opponent in self.game.opponents if any(camp.endDay == day+1
                    for camp in opponent.getOnGoingCampaigns(day+1))])
        ucsBid = ucsManager.predict_required_price_to_win_desired_UCS_level(
                ucsLevel, day, numberOfActiveNetworks, numberOfLastDayNetworks)
        
        answer["UCSBid"] = str(float(ucsBid))
        
        eprint("handleGetUcsAndBudget: print answer")
        print(json.dumps(answer, separators=(',', ':'))) #NEEDED
        eprint("handleGetUcsAndBudget: done")
        
    
    def handleInitialCampaignMessage(self):
        cid = int(self.argsList[0])
        reach = int(self.argsList[1])
        startDay, endDay = int(self.argsList[2]), int(self.argsList[3])
        segmentNamesList = MarketSegment.getSegmentListNamesFromStr(self.argsList[4])
        eprint("handleInitialCampaignMessage: segmentsList is ", segmentNamesList)
        vidCoeff = float(self.argsList[5])
        mobileCoeff = float(self.argsList[6])
        budgetMillis = float(self.argsList[7])
        initialCampaign = Campaign(cid, startDay, endDay, segmentNamesList, #TODO: names
                                   reach, vidCoeff, mobileCoeff)
        
        initialCampaign.assignCampaign(self.game.agent,
                                       { "Q_old" : 1.0 },  #The starting quality is 1.0
                                       budgetMillis,
                                       game = self.game)
        
        #TODO currently experiement - later maybe set as statistic campaigns:
        eprint("handleInitialCampaignMessage: NOTICE: making a wild assumption about initial campaigns! Think about this!")
        otherInitialCampaigns = [Campaign("{}".format(i+1), startDay, endDay,
                                          segmentNamesList, reach, vidCoeff,
                                          mobileCoeff) for i in range(7)] #TODO: what are the other IDs???
        for (inx,camp) in enumerate(otherInitialCampaigns):
            camp.assignCampaign(self.game.opponents[inx], None, budgetMillis, game = self.game)
        
        eprint("handleInitialCampaignMessage: all my CIDs are ", self.game.agent.my_campaigns.keys()) #TODO: remove
            
    def handleGetBidBundle(self):
        eprint("handleGetBidBundle: all my CIDs are ", self.game.agent.my_campaigns.keys()) #TODO: remove
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
        '''
        inputs:
            0 - AdNetworkDailyNotification.effectiveDay
        		1 - AdNetworkDailyNotification.serviceLevel
    			2 - AdNetworkDailyNotification.price
    			3 - AdNetworkDailyNotification.qualityScore
    			4 - AdNetworkDailyNotification.campaignId
    			5 - AdNetworkDailyNotification.winner
    			6 - AdNetworkDailyNotification.costMillis
        '''
        self.game.day = int(self.argsList[0])
        self.game.printStatus()
        
        self.game.agent.dailyUCSLevel = float(self.argsList[1])
        #price of UCS in argList[3] (dont care)
        oldQuality = self.game.agent.quality
        self.game.agent.quality = float(self.argsList[3])
        
        cid = int(self.argsList[4])
        if len(self.argsList) == 7:
            winner_name = self.argsList[5]
            budgetOfCampaign = int(self.argsList[6])
        if len(self.argsList) == 6:
            winner_name = "NOT_ALLOCATED"
            budgetOfCampaign = int(self.argsList[5])
        
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
        
        #NOT ALLOCATED
        if winner_name == "NOT_ALLOCATED":
            eprint("handleAdNetworkDailyNotification: campaign cid {}, not allocated to anyone".format(cid))
        
        #WON
        elif budgetOfCampaign != 0:
            eprint("handleAdNetworkDailyNotification: Won campaign cid {}, assigned to myself!".format(cid))
            cmp.assignCampaign(self.game.agent, goalObject = {"Q_old":oldQuality}, budget = budgetOfCampaign, game = self.game)
        #LOST
        else:
            #NAMED OPPONENT
            if any(agent.name == winner_name for agent in self.game.opponents):
                for agent in self.game.opponents:
                    if agent.name == winner_name:
                         cmp.assignCampaign(agent, goalObject = None, budget = budgetOfCampaign, game = self.game)
                         break
                     
            #UNNAMED OPPONENT
            else:
                for agent in self.game.opponents:
                    if agent.name == "":
                        agent.name = winner_name
                        cmp.assignCampaign(agent, goalObject = None, budget = budgetOfCampaign, game = self.game)
                        break
            eprint("handleAdNetworkDailyNotification: Lost campaign cid {}, assigned to agent {}".format(cid, winner_name))
        
        self.game.campaigns[cmp.cid] = cmp
        eprint("handleAdNetworkDailyNotification: opponents names - {}".format([agent.name for agent in self.game.opponents]))
    
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
        try:
            os.remove("pickle//game.p")
            os.remove("runlog.log")
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
        
        if queryName != "StartInfo":
            try:
                communicator.loadPickle()
            except Exception as e:
                printException(e, "main", "loading a pickle")
                traceback.print_exc()
        
        try:
            communicator.handleQuery()
        except Exception as e:
            printException(e, "main", "handling a query")
            traceback.print_exc()
            
        try:
            communicator.dumpPickle() #will not do when startInfo
        except Exception as e:
            printException(e, "main", "dumping a pickle")
            traceback.print_exc()
            
        os.chdir(origPath)
        
    else:
        eprint("Unexpected query: {}".format(queryName))
        sys.exit()

if __name__ == "__main__":
    if len(sys.argv[1:]) > 0:
        startTime = time.time()
        main(sys.argv[1], sys.argv[2:])
        endTime = time.time()
        eprint("Python {} Query elapsed time: {}".format(sys.argv[1], endTime - startTime))
    else:
        eprint("Expected a query name!")
        sys.exit()