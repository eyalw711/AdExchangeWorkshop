# -*- coding: utf-8 -*-
"""
Created on Sat Feb 18 16:53:51 2017

@author: Eyal
"""
import pandas as pd

import CampaignClass as cc

class MarketSegment:
    segments = {}
    
    def __init__(self, name, size):
        self.name = name #{O/Y}{M/F}{H/L}
        self.size = size
    
    def __repr__(self):
        return "Segment {}, size {}".format(self.name,self.size)
    
    def addSize(self, x):
        self.size += x
    
    def segments_init():
        segments = MarketSegment.segments
        population = pd.read_csv('data//population.csv')
        for index, row in population.iterrows():
            name = row['age']+row['gender']+row['income']
            if not name in segments:
                segments[name] = MarketSegment(name, row['size'])
            else:
                segments[name].addSize(row['size'])
     
    def getSegmentsList():
        return list(MarketSegment.segments.values())
    
    def segment_indicator(self, day, campaign):
        if (self in campaign.segments) and (campaign.activeAtDay(day)):
            return 1
        else:
            return 0

    def segment_demand_numer(self, day, campaignList):
        numer = sum((1/(self.size * camp.activePeriodLength())) * 
                    self.segment_indicator(day, camp) * camp.reach for camp in campaignList)
        return numer
    
    def segment_demand(self, day, campaignList):
        numer = self.segment_demand_numer(day, campaignList)
        denumer = sum(seg.segment_demand_numer(day, campaignList) for
                      seg in MarketSegment.getSegmentsList())
        return numer/denumer
    
    def segment_set_demand_forDays(segmentList, dayStart, dayEnd, campaignList):
        D = dayEnd - dayStart + 1
        return (1/D)* sum( sum(seg.segment_demand(day, campaignList) for seg in segmentList)
                for day in range(dayStart, dayEnd+1))
    
    
