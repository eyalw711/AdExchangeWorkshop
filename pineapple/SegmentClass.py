# -*- coding: utf-8 -*-
"""
Created on Sat Feb 18 16:53:51 2017

@author: Eyal
"""
import pandas as pd
import math
import itertools

class MarketSegment:
    segments = {}
    
    def __init__(self, name, size):
        self.name = name #{O/Y}{M/F}{H/L}
        self.size = size
    
    def __repr__(self):
        return "({},{})".format(self.name,self.size)
    
    def addSize(self, x):
        self.size += x
    
    def getSegmentListFromStr(segmentStr):
        segmentsList = []
        for comb in itertools.product(['O','Y'], ['M','F'], ['H', 'L']):
            if all(c in comb for c in segmentStr):
                segmentsList.append(MarketSegment.segments[''.join(comb)])
        return segmentsList
        
    def segments_init():
        segments = MarketSegment.segments
        population = pd.read_csv('data//population.csv')
        for index, row in population.iterrows():
            name = row['age']+row['gender']+row['income']
            if not name in segments:
                segments[name] = MarketSegment(name, row['size'])
            else:
                segments[name].addSize(row['size'])
        print("#segments_init: segments initialized!")
     
    def getSegmentsList():
        return list(MarketSegment.segments.values())
    
    def segment_indicator(self, day, campaign):
        if (self in campaign.segments) and (campaign.activeAtDay(day)):
            return True
        else:
            return False

    def segment_demand_numer(self, day, campaignList):
        numer = sum(camp.reach/(camp.sizeOfSegments() * camp.activePeriodLength()) for camp in campaignList if self.segment_indicator(day, camp))
        return numer
    
    def segment_demand(self, day, campaignList):
        numer = self.segment_demand_numer(day, campaignList)
        return numer
    
#    def segment_set_demand_forDays(segmentList, dayStart, dayEnd, campaignList):
#        D = dayEnd - dayStart + 1
#        return (1/D)* sum( sum(seg.segment_demand(day, campaignList) for seg in segmentList)
#                for day in range(dayStart, dayEnd+1))
    
    def segment_set_demand_forDay(segmentList, day, campaignList):
        '''computes equivalent demand of a set of segments in parallel'''
        if any(seg.segment_demand(day, campaignList)==0 for seg in segmentList):
            return 0
        equiv_demand_inv = sum(math.pow(seg.segment_demand(day, campaignList),-1) for seg in segmentList)
        return math.pow(equiv_demand_inv,-1)
    
    def segment_set_demand_forDays(segmentList, dayStart, dayEnd, campaignList):
        if any(MarketSegment.segment_set_demand_forDay(segmentList, day, campaignList) == 0 for day in range(dayStart, dayEnd + 1)):
            return 0
        equiv_demand_inv = sum(math.pow(MarketSegment.segment_set_demand_forDay(segmentList, day, campaignList),-1) for day in range(dayStart, dayEnd + 1))
        return math.pow(equiv_demand_inv,-1)
    
    
    
