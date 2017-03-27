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
        pickle.dump( self.game, open( "pickle//game.p", "wb" ) )
    
    def handleGetUCSBid(self):
        print("TEST")
        pass
    
    def handleGetBidBundle(self):
        pass
    
    def handleInitialCampaignMessage(self):
        pass
    
    def handleGetCampaignBudgetBid(self):
        pass
    
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
    
    
        
    def handleQuery(self):
        functions = {
                "GetUCSBid": Communicator.handleGetUCSBid,
                "GetBidBundle": Communicator.handleGetBidBundle,
                "InitialCampaignMessage": Communicator.handleInitialCampaignMessage,
                "GetCampaignBudgetBid": Communicator.handleGetCampaignBudgetBid,
                "CampaignReport": Communicator.handleCampaignReport,
                "AdNetworkDailyNotification": Communicator.handleAdNetworkDailyNotification,
                "AdxPublisherReport": Communicator.handleAdxPublisherReport,
                "PublisherCatalog": Communicator.handlePublisherCatalog,
                "AdNetworkReport": Communicator.handleAdNetworkReport,
                "StartInfo": Communicator.handleStartInfo,
                "BankStatus": Communicator.handleBankStatus
                }
        
        handler = functions[self.queryName]
        handler(self)
    
    
def main(queryName, argsList):
    if queryName in ["GetUCSBid", "GetBidBundle", "InitialCampaignMessage",
                     "GetCampaignBudgetBid", "GetCampaignBudgetBid", "CampaignReport",
                     "AdNetworkDailyNotification", "AdxPublisherReport", "PublisherCatalog",
                     "AdNetworkReport", "StartInfo", "BankStatus"]:
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