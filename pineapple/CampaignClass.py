# -*- coding: utf-8 -*-
"""
Created on Sat Feb 18 18:24:52 2017

@author: Eyal
"""

import SegmentClass as sc
import pandas as pd
import math

class Campaign:
    
    statistic_campaigns = {}
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
        self.assignee = None
        self.targetedImpressions = 0
        self.budget = 0
        self.impressions_goal = 0
        
    
    def __repr__(self):
        return "Campaign ID:{}, start:{} ends:{}, segments:{}, reach:{}".format(
                self.cid, self.startDay, self.endDay, [seg.name for seg in self.segments], self.reach)
    
    def statistic_campaigns_init():
        camps = Campaign.statistic_campaigns
        statistics = pd.read_csv('data//campaign_statistics.csv')
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
    def assignCampaign(self, assigneeName, budget = 0):
        self.assignee = assigneeName
        Campaign.campaigns[self.cid] = self
        self.budget = budget                
        
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
    
    