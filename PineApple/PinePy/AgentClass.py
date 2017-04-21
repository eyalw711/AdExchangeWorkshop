# -*- coding: utf-8 -*-
"""
Created on Wed Feb 22 00:12:36 2017

@author: Eyal
"""

from CampaignClass import Campaign
from UcsManagerClass import ucsManager
import itertools
import math

def eprint(*args, **kwargs):
#    print(*args, file=sys.stderr, **kwargs)
    with open("../myLogs/sim{0}/PinePyEngine_Sim{0}.log".format(Agent.simId), "a+") as logFile:
        print(*args, file=logFile, **kwargs)
#        logFile.write(*args)

class Agent:
    simId = 0
       
    def __init__(self, name):
        self.name = name
        self.quality = 1.0 #starting quality is 1.0
        ''' powers of 0.9 '''
        self.dailyUCSLevel = 0.9 #starting UCS Level is 0.9 
        ''' list of CIDs'''
        self.my_cids = []
         
    def __repr__(self):
        return "Agent {}: Q: {} Campaigns: {}".format(self.name, self.quality, self.my_campaigns())
    
    def my_campaigns(self):
        return [Campaign.campaigns[cid] for cid in self.my_cids]
    
    def getOnGoingCampaigns(self, day):
        return [camp for camp in self.my_campaigns() if camp.activeAtDay(day)]
    
    def campaignOpportunityBid(self, campaign): # as defined in the document
        COB = campaign.initial_budget_bid()
        
        if (COB < campaign.reach*self.quality) and (COB > campaign.reach/(10*self.quality)): #inside interval
                return COB
        elif COB >= campaign.reach*self.quality:                                            #greater than maximum
            return (campaign.reach*self.quality) - 5
        
        else:                                                                               #lower than minimum
            return campaign.reach/(10*self.quality) + 5
        
    def formBidBundle(self, day):
        '''
        forms a bid bundle for tomorrow
        param day is (current game day + 1)
        '''
        bidsArray = []
        ongoing_camps = self.getOnGoingCampaigns(day) 
        eprint("#formBidBundle: {}: tomorrow is {}, ongoing camps tomorrow are {}".format(self.name, day, [cmp.cid for cmp in ongoing_camps]))
        ucs_level = ucsManager.get_desired_UCS_level(day, ongoing_camps) #day is tomorrow as this function expects
        if ucs_level > 0:
            ucs_level -= 1
        lvl_accuracy = ucsManager.level_accuracy(ucs_level)
      
        for cmp in ongoing_camps:
            cid = cmp.cid
            eprint("#formBidBundle: forming bids for cid {}".format(cid))
            cmpSegmentsSize = cmp.sizeOfSegments()
            goal_targeted_number_of_imps_for_day = max(min(cmpSegmentsSize*lvl_accuracy, (cmp.impressions_goal - cmp.targetedImpressions)*lvl_accuracy) ,0)
            if goal_targeted_number_of_imps_for_day < 1:
                continue
                
            eprint("#formBidBundle: cid:{} goal_targeted_number_of_imps_for_day is {}, impressionsGoal = {}, targetedImps = {}, level_accuracy = {}".format(
                    cid, goal_targeted_number_of_imps_for_day, cmp.impressions_goal, cmp.targetedImpressions, lvl_accuracy))
            # sort segments of campaign based on segment demand
#            cmpSegmentsList = sorted(cmp.segments, key = lambda x: x.segment_demand(day, Campaign.getCampaignList()))
            cmp.segments.sort(key = lambda x: x.segment_demand(day, Campaign.getCampaignList()))
            cmpSegmentsList = cmp.segments
            eprint("#formBidBundle: sorted campaigns for cid={} are {}".format(cid, cmpSegmentsList))
            bidSegments = []
            for i in range(len(cmpSegmentsList)):
                if sum(seg.size for seg in cmpSegmentsList[:i]) * lvl_accuracy > goal_targeted_number_of_imps_for_day:
                    bidSegments = cmpSegmentsList[:i]
                    break
            if not bidSegments:
                bidSegments = cmpSegmentsList
            eprint("#formBidBundle: for cid={} the bid segments are {}".format(cid ,bidSegments))
            
            def mean(numbers):
                return float(sum(numbers)) / max(len(numbers), 1)
            
            avgDem = mean([seg.segment_demand(day, Campaign.getCampaignList()) for seg in bidSegments])
            eprint("#formBidBundle: demands for segments are: {}".format([seg.segment_demand(day, Campaign.getCampaignList()) for seg in bidSegments]))
            if any(seg.segment_demand(day, Campaign.getCampaignList()) != avgDem for seg in bidSegments):
                eprint("#formBidBundle: demand varies!")
            
            NORMALING_FACTOR = 38.0 #TODO: think what that should be
            PANIC_FACTOR = 1.0
            if cmp.endDay == day-1:
                PANIC_FACTOR = 1.15
            elif cmp.endDay == day:
                PANIC_FACTOR = 1.4
                
            outputCoeff = 1
            dailyImpsAvg = cmp.impressions_goal / cmp.activePeriodLength()
            dailyImpsAvgTogo = (cmp.impressions_goal - cmp.targetedImpressions) / (cmp.endDay - day + 1)
            if dailyImpsAvgTogo > dailyImpsAvg:
                outputCoeff = dailyImpsAvgTogo / dailyImpsAvg
            
            p = cmp.avg_p_per_imp
            eprint("#formBidBundle: for camp {} the p is {} and avgDem is {}".format(cmp.cid, p, avgDem))
            for x in itertools.product(bidSegments + [None], ["Text","Video"], ["Desktop", "Mobile"]):
                seg = x[0]
                
                coeffsMult = 1
                if x[1] == "Video" and cmp.videoCoeff > 1:
                    coeffsMult *= cmp.videoCoeff
                if x[2] == "Mobile" and cmp.mobileCoeff > 1:
                    coeffsMult *= cmp.mobileCoeff
                
                if seg == None:                 #empty query (UNKNOWN)
                    #bid = p * coeffsMult
                    bid = 0.005
                    
                    #this stands for the impressions we don't expect to catch because of lack of ucs
                    s = max(min(cmpSegmentsSize, (cmp.impressions_goal - cmp.targetedImpressions)) - goal_targeted_number_of_imps_for_day,0)
                    
                    query = {
                             "marketSegments" : [{"segmentName":"Unknown"}],
                             "Device" : x[2],
                             "adType" : x[1]
                            }
                    
                else:                           #normal query
                    demand = seg.segment_demand(day, Campaign.getCampaignList())
                    #eprint("#formBidBundle: for segment {}, (demand - avgDem) is {}".format(seg, demand - avgDem))
                    
                    bid = float(max((p * (demand / avgDem) * NORMALING_FACTOR) * coeffsMult * PANIC_FACTOR * outputCoeff , 0))
                    
#                    if bid < 0:
#                        eprint("formBidBundle: warning (demand - avgDem) turned the bid to negative. fixed it somehow")
#                        bid = p * PANIC_FACTOR * outputCoeff
                
                    if (not seg is bidSegments[-1]):
                        s = seg.size
                    else:
                        s = max(goal_targeted_number_of_imps_for_day - sum(segTag.size for segTag in bidSegments[:-1]), 0)
                
                    query = {
                            "marketSegments" : [{"segmentName":seg.name}],
                             "Device" : x[2],
                             "adType" : x[1]
                            }
                
                bidsArray += [{"query" : query, 
                         "bid" : str(bid), 
                         "campaignId" : int(cid), 
                         #"weight" :1 ,
                         "weight" : int(math.sqrt(cmp.imps_to_go()) if cmp.imps_to_go() > 0 else 0), 
                         "dailyLimit" : str(float(bid*s*lvl_accuracy))}]
        eprint("#formBidBundle: out of this func")
        return bidsArray