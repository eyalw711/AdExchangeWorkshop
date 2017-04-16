# -*- coding: utf-8 -*-
"""
Created on Sat Feb 18 18:24:52 2017

@author: Eyal
"""

from SegmentClass import MarketSegment
import pandas as pd
from scipy import optimize
import math
#import pickle
#import sklearn.cross_validation as cval
#from sklearn.metrics import accuracy_score
#from sklearn.metrics import classification_report
from sklearn.ensemble import AdaBoostClassifier
from sklearn.tree import DecisionTreeClassifier
import sys


def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)
#    with open("runlog.log", "a+") as logFile:
#        print(*args, file=logFile, **kwargs)
#        #logFile.write(*args)

class Campaign:
    
    statistic_campaigns = {}    # dummy campaigns <key : <key:val>> = <day ,<segment name : campagin object>>
    campaigns = {}
    bdt = None
    
    def __init__(self, cid, startDay, endDay, segmentNames, reach, 
                 videoCoeff, mobileCoeff):
        self.cid = cid
        ''' integers'''
        self.startDay = startDay
        self.endDay = endDay
        '''list of segments'''
        if len(segmentNames) > 0 and type(segmentNames[0]) != str:
            raise ValueError("segmentNames contains elements which aren't strings!")
            
        self.segments = [MarketSegment.segments[name] for name in segmentNames]
        self.reach = reach
        self.videoCoeff = videoCoeff
        self.mobileCoeff = mobileCoeff
        self.agent = None
        self.targetedImpressions = 0 # how many impressions we have already aquired
        self.budget = 0 #in millis
        self.impressions_goal = 0 # as defined in the document (target number of impressions)
        self.avg_p_per_imp = 0 # "p bar"
        
    
    def __repr__(self):
        template = "Campaign ID:{}, start:{}, ends:{}, segments:{}, reach:{}"
        if self.agent.name == "PineApple":
            return (template + ", imps_goal:{}").format(self.cid, self.startDay, self.endDay, [seg for seg in self.segments], self.reach, self.impressions_goal)
        else:
            return template.format(self.cid, self.startDay, self.endDay, [seg for seg in self.segments], self.reach)
    
    def __str__(self):
        return self.__repr__()
        
        
    ''' dummy campaigns '''
    def statistic_campaigns_init():
        camps = Campaign.statistic_campaigns
        statistics = pd.read_csv('..//data//campaign_statistics.csv')
        statistic_campaigns_cid_cntr = 100
        # adding dummy campaings to dictionary sorted by day
        for index, row in statistics.iterrows():
                day = row['day']
                if not day in camps:
                    newDictForDay = {}
                    camps[day] = newDictForDay
                dayDict = camps[day]
                segmentName = row['segment']
                dayDict[segmentName] = Campaign("{}".format(statistic_campaigns_cid_cntr), row['start'],
                       row['end'], [segmentName],
                          row['reach'], row['vidCoeff'], row['mobCoeff'])
                statistic_campaigns_cid_cntr += 1
        
#        with open( "pickle//statisticCampaigns.p", "wb" ) as pickleFile:
#            pickle.dump( camps, pickleFile)
        #print("#statistic_campaigns_init: statistic campaigns initialized!")

    def setCampaigns(campaignsDict):
        Campaign.campaigns = campaignsDict
        
    def activeAtDay(self,d):
        if d >= self.startDay and d <= self.endDay:
            return True
        else:
            return False
        
    def getCampaignsAtDay(d):
        ''' returns all campaigns at day @param d'''
        campaignsAtDay = []
        for cid, camp in Campaign.campaigns.items():
            if camp.activeAtDay(d):
                campaignsAtDay.append(camp)
        return campaignsAtDay

    def activePeriodLength(self):
        return self.endDay - self.startDay + 1
    
    def assignCampaign(self, agent, goalObject = None, budget = 0, game = None):
        '''this is called for a campaign which will start tomorrow'''
        self.agent = agent
        Campaign.campaigns[self.cid] = self
        agent.my_campaigns[self.cid] = self
        self.budget = budget
        
        #put the campaign in the game.campaigns
        if game != None:
            game.campaigns[self.cid] = self
        
        if not (goalObject is None): # compute average price per impression for this campaign
            Q_old = goalObject["Q_old"]
            B = self.budget
            demand = self.campaign_demand_temp()
            
            f = lambda x: -self.campaign_profit_for_ImpsTarget_estim(x, Q_old)
            x0 = [self.reach]
            res = optimize.basinhopping(f, x0, niter=1)

            self.impressions_goal = res.x.flatten()[0] #res.x[0]
            self.avg_p_per_imp = B*demand/self.impressions_goal #B*demand*self.impressions_goal/R
        
    def getCampaignList():
        return list(Campaign.campaigns.values())

    def getStatisticsCampaignListAtDay(day):
        if not day in Campaign.statistic_campaigns:
            return []
        return list(Campaign.statistic_campaigns[day].values()) # TODO: check if day is integer or string
    
    def getStatisticsCampaignListAtDays(start_day, end_day):
        campaings_list = []
        for i in range(start_day, end_day):
            campaings_list += Campaign.getStatisticsCampaignListAtDay(start_day) # TODO: check if day is integer or string
        return campaings_list
    
    def campaign_demand_temp(self):
        ''' this method calculates the guessed demand for the campaign with statistics '''
        campaigns = Campaign.getCampaignList() + Campaign.getStatisticsCampaignListAtDays(self.startDay, self.endDay)
        return MarketSegment.segment_set_demand_forDays(self.segments, self.startDay,
                                                    self.endDay, campaigns)
    
    def ERR(self, imps):
        a = 4.08577
        b = 3.08577
        return (2/a)*(math.atan(a*imps/self.reach - b)-math.atan(-b))
    
    def campaign_profit_for_ImpsTarget_estim(self, imps, Q_old):
        eta = 0.6
        alpha = 0.98 #todo: think about it
        B = self.budget
        R = self.reach
        demand = self.campaign_demand_temp()
        return (self.ERR(imps)*B - math.pow(B*demand*imps/R, alpha))*((1-eta)*Q_old + eta*self.ERR(imps))
    
    def contains_segment(self, segment_name):
        if segment_name in [segment.name for segment in self.segments]:
            return True
        return False
        
    
    def sizeOfSegments(self):
        if (any(seg.size == 0 for seg in self.segments)):
            raise Exception("segments are uninitialized!")
        return sum(seg.size for seg in self.segments)
    
    def initial_budget_bid(self):
        alpha = 1#math.pow(random.gauss(1,0.1),2)
        return self.campaign_demand_temp()*self.reach*alpha

    def imps_to_go(self):
        return self.impressions_goal - self.targetedImpressions
    
    def initialize_campaign_profitability_predictor():
        train = pd.read_csv('..//data//campaigns_profitability.csv')
            
        features = list(train.columns[3:-9])
       
        #features = list(train.columns[3:-18])
        #features = list(train.columns[4:5])+list(train.columns[9:10])
        #print("*** FEATURES ***")
        #print(features)
        # TODO:  consider the value of n_estimators based on predict_proba
        Campaign.bdt = AdaBoostClassifier(DecisionTreeClassifier(max_depth=1), algorithm="SAMME.R", n_estimators=50)
        Campaign.bdt.fit(train[features], train["decision"])
        
#        with open( "pickle//campaign_profitability_predictor.p", "wb" ) as pickleFile:
#            pickle.dump( Campaign.bdt, pickleFile)        
        
        
        #TODO: load bdt here
        
        #trainX, testX, trainY, testY = cval.train_test_split(train[features], train["decision"], test_size=0.3, random_state=0)
        #trueY, predY = testY, Campaign.bdt.predict(testX)
        #print ("accuracy_score:")
        #model = AdaBoostClassifier(DecisionTreeClassifier(max_depth=1), algorithm="SAMME.R", n_estimators=50)
        #model.fit(trainX,trainY)
        #print(classification_report(testY, model.predict(testX)))
    
    
    def predict_campaign_profitability(self, day, budget, quality):
        eprint("predict_campaign_profitability inside")
        campaigns = Campaign.getCampaignList() + Campaign.getStatisticsCampaignListAtDays(self.startDay, self.endDay)
        max_budget = self.reach*quality
        budget = budget / 1000.0
        test = [{
                "day":day,
                "budget":budget,
                "start":self.startDay,
                "end":self.endDay,
                "vidCoeff":self.videoCoeff,
                "mobCoeff":self.mobileCoeff,
                "reach":self.reach,
                "demand":MarketSegment.segment_set_demand_forDays(self.segments,self.startDay,self.endDay,campaigns),
                "OML":self.contains_segment("OML"), "OMH":self.contains_segment("OMH"),
                "OFL":self.contains_segment("OFL"), "OFH":self.contains_segment("OFH"),
                "YML":self.contains_segment("YML"), "YMH":self.contains_segment("YMH"),
                "YFL":self.contains_segment("YFL"), "YFH":self.contains_segment("YFH")}]
    
        '''test = [{
                "budget":budget,
                "reach":self.reach,
}]'''                                          
        eprint("#predict_campaign_profitability: ada boost predict_proba results for campagin number %d: the campagin is profitible with probability:%s" % (self.cid,str(Campaign.bdt.predict_proba(pd.DataFrame(test))[0,1])))
        b = budget
        y_pred = Campaign.bdt.predict(pd.DataFrame(test))
#        for b in np.arange(budget, max_budget, 0.5):
#            test[0]["budget"] = b
#            y_pred = Campaign.bdt.predict(pd.DataFrame(test))
#            if int(y_pred[0])== 1:
#                eprint("predict_campaign_profitability return")
#                return int(y_pred[0]), b*1000.0  
        eprint("predict_campaign_profitability return")
        return int(y_pred[0]), b*1000.0
    
    def is_last_day(self, day):
        if day == self.endDay:
            return True
        return False
    
    def number_of_active_netowrks(self, day):
        networks = set()
        for campaign in Campaign.campaigns:
            if self.activeAtDay(day):
                networks.add(campaign.agent)        
        return len(networks)
    
    def number_of_last_day_netowrks(self, day):
        networks = set()
        for campaign in Campaign.campaigns:
            if self.is_last_day(day):
                networks.add(campaign.agent)        
        return len(networks)
             