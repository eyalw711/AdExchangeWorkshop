# -*- coding: utf-8 -*-
"""
Created on Sat Feb 18 19:23:55 2017

@author: Eyal
"""

import CampaignClass as cc
import SegmentClass as sc
import UcsManagerClass as ucs
#import numpy
from scipy import optimize
#import matplotlib.pyplot as plt

''' log comments
import logging

logger = None

def init_logger():
    global logger = logging.getLogger('PineApple')
    handler = logging.FileHandler('/logs/pineLog.log')
    formatter = '''
    
def test_segments_and_demand():
    print("\ntest_segments_and_demand:")
    #{O/Y}{M/F}{H/L}
    camps_segs = [['OMH','YFL'],['OMH'],['OMH','YFL']]#,['YML','OML','OFH']]
    for i in range(3):
        camp_seg_list = [sc.MarketSegment.segments[name] for name in camps_segs[i]]
        camp = cc.Campaign(i, i, i+3, camp_seg_list, (i+1)*1000 , 1, 1, "WALLA")
        camp.assignCampaign("Eyal")
    
    campaigns = cc.Campaign.getCampaignList()  
#    for segment in sc.MarketSegment.getSegmentsList():
#        print (segment.segment_demand_numer(3, campaigns))
    
    for camp in cc.Campaign.getCampaignList():
        print(camp, "demand={}".format(camp.campaign_demand_temp()))

def test_ucs_desired_level():
    print ("test_ucs_desired_level")
    seg = sc.MarketSegment.segments['OMH']
    size = 6*seg.size
    camp = cc.Campaign(0, 1, 4, [seg], size , 1, 1, "WALLA")
    camp.targetedImpressions = size/2
    camp.impressions_goal = size+100
    print("desired level of UCS for campaigns = {}".format(ucs.ucsManager.get_desired_UCS_level(1, [camp])))
    
def test_ImpsOptimization():
    print("\ntest_ImpsOptimization:")
    Q_old = 0.9
    camp = cc.Campaign.campaigns[2]
    camp.budget=1600
    #x = [i for i in range(0,4000,20)]
    #y = [ camp.campaign_profit_for_ImpsTarget_estim(imps, Q_old) for imps in x]
    #plt.plot(x,y)
    f = lambda x: -camp.campaign_profit_for_ImpsTarget_estim(x, Q_old)
    x0 = [camp.reach]
    res = optimize.basinhopping(f, x0, niter=1)
    print("optimal number of imps for camp is {}\n\n\n".format( res.x))
    
def test_statisticalCampaigns():
    for day in cc.Campaign.statistic_campaigns:
        print("statistical campaigns for day = {}".format(day))
        for seg in cc.Campaign.statistic_campaigns[day]:
            print (seg, ':', cc.Campaign.statistic_campaigns[day][seg])
    
def main():
    print("PineApple!")
    sc.MarketSegment.segments_init()
    print("segments initialized!")
    cc.Campaign.statistic_campaigns_init()
    print("statistic campaigns initialized!")
    test_segments_and_demand()
    test_ImpsOptimization()
    test_ucs_desired_level()
#    test_statisticalCampaigns()
    
    
if __name__ == "__main__":
    main()