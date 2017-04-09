# -*- coding: utf-8 -*-
"""
Created on Wed Feb 22 00:12:36 2017

@author: Eyal
"""
from CampaignClass import Campaign
from UcsManagerClass import ucsManager
import itertools
from pyjava_comm import eprint

def mean(numbers):
    return float(sum(numbers)) / max(len(numbers), 1)

class Agent:
    def __init__(self, name):
        self.name = name
        self.quality = 0.9
        ''' powers of 0.9 '''
        self.dailyUCSLevel = 0 #TODO: what is the starting UCS Level? 
        self.my_campaigns = {}
        
    def __repr__(self):
        return "Agent {}: Q: {} Campaigns: {}".format(self.name, self.quality, self.my_campaigns.values())
    
    def getOnGoingCampaigns(self, day):
        return [camp for (key, camp) in self.my_campaigns.items() if camp.activeAtDay(day)]
    
    def campaignOpportunityBid(self, campaign): # as defined in the document
        COB = campaign.initial_budget_bid()
        if (COB < campaign.reach*self.quality) and (COB > campaign.reach/(10*self.quality)):
                return COB
        elif COB >= campaign.reach*self.quality:
            return (campaign.reach*self.quality) - 0.1
        
        else:
            return campaign.reach/(10*self.quality) + 0.1
        
    def formBidBundle(self, day):
        '''
        forms a bid bundle for tomorrow
        param day is (current game day + 1)
        '''
        bidsArray = []
        ongoing_camps = [cmp for cid,cmp in self.my_campaigns.items() if cmp.activeAtDay(day)]
        #print("#formBidBundle: {}: ongoing camps {}".format(self.name, self.my_campaigns.keys()))
        ucs_level = ucsManager.get_desired_UCS_level(day, ongoing_camps) #day is tomorrow as this function expects
        if ucs_level > 0:
            ucs_level -= 1
        lvl_accuracy = ucsManager.level_accuracy(ucs_level)
        # query (segment, platform, adtype)
#        public final void addQuery(final AdxQuery query, final double bid,
#			final Ad ad, int campaignId, int weight, final double dailyLimit) {
        for cid, cmp in self.my_campaigns.items():
#            print("#formBidBundle: forming bids for cid {}".format(cid))
            cmpSegmentsSize = cmp.sizeOfSegments()
            goal_targeted_number_of_imps_for_day = min(cmpSegmentsSize*lvl_accuracy, \
                            (cmp.impressions_goal - cmp.targetedImpressions)*lvl_accuracy)
            # sort segments of campaign based on segment demand
            cmpSegmentsList = sorted(cmp.segments, key = lambda x: x.segment_demand(day, Campaign.getCampaignList()))
            bidSegments = []
            for i in range(len(cmpSegmentsList)):
#                print("#formBidBundle: i={}, segSizeSum*acc={}, impsGoal={}".format(i,sum(seg.size for seg in cmpSegmentsList[:i]) * lvl_accuracy,goal_targeted_number_of_imps_for_day ))
                if sum(seg.size for seg in cmpSegmentsList[:i]) * lvl_accuracy > goal_targeted_number_of_imps_for_day:
                    bidSegments = cmpSegmentsList[:i]
                    break
                if not bidSegments:
                    bidSegments = cmpSegmentsList
#            print(#formBidBundle: bidSegments)
            
            avgDem = mean([seg.segment_demand(day, Campaign.getCampaignList()) for seg in bidSegments])
            NORMALING_FACTOR = 1.0
            for x in itertools.product(bidSegments, ["Text","Video"], ["Desktop", "Mobile"]):
                p = cmp.avg_p_per_imp
                seg = x[0]
                demand = seg.segment_demand(day, Campaign.getCampaignList())
                if (not seg is bidSegments[-1]):
                    s = seg.size
                else:
                    s= goal_targeted_number_of_imps_for_day - sum(segTag.size for segTag in bidSegments[:-1])
                    
#                bidBundle += [{'segment': x[0].name, 'adType': x[1], 
#                        'adPlatform': x[2], 
#                        'campID': cid, 
#                        'bid': (p*demand),  # TODO: don't just multiply by demand
#                        'spendLimit' : (p*demand*s*lvl_accuracy), # TODO: don't just multiply by demand. consider how to refer to each  ad type / mobie
#                        'weight': (cmp.imps_to_go)}]
                
                query = {
                        "marketSegments" : [{"segmentName":seg.name}],
                         "Device" : x[2],
                         "adType" : x[1]
                        }
                
                eprint("p is ", p, " and deltaDemand is ", (demand - avgDem))
                bid = float(p+(demand - avgDem)*NORMALING_FACTOR)
                if bid < 0:
                    bid = p
                bidsArray += [{"query" : query, 
                         "bid" : bid, 
                         "campaignId" : int(cid), 
                         "weight" : cmp.imps_to_go(), 
                         "dailyLimit" : float(p*demand*s*lvl_accuracy)}]
        return bidsArray