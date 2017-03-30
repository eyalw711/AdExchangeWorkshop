# -*- coding: utf-8 -*-
"""
Created on Sat Feb 18 18:24:52 2017

@author: Eyal
"""

import SegmentClass as sc
import pandas as pd
from scipy import optimize
import math
import random
from sklearn.ensemble import AdaBoostClassifier
from sklearn.tree import DecisionTreeClassifier

class Campaign:
    
    statistic_campaigns = {}    # dummy campaigns <kay : <key:val>> = <day ,<segment name : campagin objcect>>
    campaigns = {}
    bdt = None
    
    def __init__(self, cid, startDay, endDay, segments, reach, 
                 videoCoeff, mobileCoeff):
        self.cid = cid
        ''' integers'''
        self.startDay = startDay
        self.endDay = endDay
        '''list of segments'''
        self.segments = segments
        self.reach = reach
        self.videoCoeff = videoCoeff
        self.mobileCoeff = mobileCoeff
        self.agent = None
        self.targetedImpressions = 0 # how many impressions we have already aquired
        self.budget = 0 #in millis
        self.impressions_goal = 0 # as defined in the document (target number of impressions)
        self.avg_p_per_imp = 0 # "p bar"
        
    
    def __repr__(self):
        return "Campaign ID:{}, start:{} ends:{}, segments:{}, reach:{}".format(
                self.cid, self.startDay, self.endDay, [seg for seg in self.segments], self.reach)
    
    ''' dummy campaigns '''
    def statistic_campaigns_init():
        camps = Campaign.statistic_campaigns
        statistics = pd.read_csv('data//campaign_statistics.csv')
        # adding dummy campaings to dictionary sorted by day
        for index, row in statistics.iterrows():
                day = row['day']
                if not day in camps:
                    newDictForDay = {}
                    camps[day] = newDictForDay
                dayDict = camps[day]
                segmentName = row['segment']
                dayDict[segmentName] = Campaign("s{}".format(index), row['start'],
                       row['end'], [sc.MarketSegment.segments[segmentName]],
                          row['reach'], row['vidCoeff'], row['mobCoeff'])
        print("#statistic_campaigns_init: statistic campaigns initialized!")

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
    
    '''TODO: unfinished!!!'''
    def assignCampaign(self, agent, goalObject = None, budget = 0):
        self.agent = agent
        Campaign.campaigns[self.cid] = self
        agent.my_campaigns[self.cid] = self
        self.budget = budget
        if not (goalObject is None): # compute average price per impression for this campaign
            Q_old = goalObject["Q_old"]
            B = self.budget
            demand = self.campaign_demand_temp()
            R = self.reach
            
            f = lambda x: -self.campaign_profit_for_ImpsTarget_estim(x, Q_old)
            x0 = [self.reach]
            res = optimize.basinhopping(f, x0, niter=1)
           
            self.impressions_goal = res.x
            self.avg_p_per_imp = B*demand*self.impressions_goal/R
        
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
        return sc.MarketSegment.segment_set_demand_forDays(self.segments, self.startDay,
                                                    self.endDay, Campaign.getCampaignList())
    
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
        return sum(seg.size for seg in self.segments)
    
    def initial_budget_bid(self):
        alpha = 1#math.pow(random.gauss(1,0.1),2)
        return self.campaign_demand_temp()*self.reach*alpha

    def imps_to_go(self):
        return self.impressions_goal - self.targetedImpressions
    
    def initialize_campaign_profitability_predictor():
        train = pd.read_csv('data//campaigns_profitability.csv')        
        features = list(train.columns[1:-3])
        # TODO:  consider the value of n_estimators based on predict_proba
        Campaign.bdt = AdaBoostClassifier(DecisionTreeClassifier(max_depth=1), algorithm="SAMME.R", n_estimators=30)
        Campaign.bdt.fit(train[features], train["decision"])
    
    def predict_campaign_profitability(self, day):
        campaigns = Campaign.getCampaignList() + Campaign.getStatisticsCampaignListAtDays(self.startDay, self.endDay)
        test = [{
                "day":day,
                "budget":self.budget,
                "start":self.startDay,
                "end":self.endDay,
                "vidCoeff":self.videoCoeff,
                "mobCoeff":self.mobileCoeff,
                "reach":self.reach,
                "demand":sc.MarketSegment.segment_set_demand_forDays(self.segments,self.startDay,self.endDay,campaigns),
                "OML":self.contains_segment("OML"), "OMH":self.contains_segment("OMH"),
                "OFL":self.contains_segment("OFL"), "OFH":self.contains_segment("OFH"),
                "YML":self.contains_segment("YML"), "YMH":self.contains_segment("YMH"),
                "YFL":self.contains_segment("YFL"), "YFH":self.contains_segment("YFH")}]                                          
        print("#predict_campaign_profitability: ada boost predict_proba results for campagin number %d: the campagin is profitible with probability:%s" % (self.cid,str(Campaign.bdt.predict_proba(pd.DataFrame(test))[0,1])))
        y_pred = Campaign.bdt.predict(pd.DataFrame(test)) 
        return int(y_pred[0])