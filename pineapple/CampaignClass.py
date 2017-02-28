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

class Campaign:
    
    statistic_campaigns = {}    # dummy campaigns <kay : <key:val>> = <day ,<segment name : campagin objcect>>
    campaigns = {}
    
    def __init__(self, cid, startDay, endDay, segments, reach, 
                 videoCoeff, mobileCoeff, publisher):
        self.cid = cid
        ''' integers'''
        self.startDay = startDay
        self.endDay = endDay
        '''list of segments'''
        self.segments = segments
        self.reach = reach
        self.videoCoeff = videoCoeff
        self.mobileCoeff = mobileCoeff
        self.publisher = publisher
        self.agent = None
        self.targetedImpressions = 0 # how many impressions we have already aquired
        self.budget = 0 
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
                dayDict[segmentName] = Campaign(row['cid'], row['start'],
                       row['end'], [sc.MarketSegment.segments[segmentName]],
                          row['reach'], row['vidCoeff'], row['mobCoeff'], row['publisher'])
        print("statistic campaigns initialized!")

        
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
    
    def sizeOfSegments(self):
        return sum(seg.size for seg in self.segments)
    
    def initial_budget_bid(self):
        alpha = 1#math.pow(random.gauss(1,0.1),2)
        return self.campaign_demand_temp()*self.reach*alpha

    
    