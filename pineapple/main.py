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
import matplotlib.pyplot as plt
import pandas as pd
import AgentClass as ac
    
def test_segments_and_demand():
    #{O/Y}{M/F}{H/L}
    PineAppleAgent = ac.Agent()
    camps_segs = [['OMH','YFL'],['OMH'],['OMH','YFL'],['YML','OML','OFH']]
    for i in range(4):
        camp_seg_list = [sc.MarketSegment.segments[name] for name in camps_segs[i]]
        camp = cc.Campaign(i, i, i+3, camp_seg_list, (i+1)*1000 , 1, 1, "WALLA")
        camp.assignCampaign("agent{}".format(i%2))
    print()
    print(">Campaigns:")
    for camp in cc.Campaign.getCampaignList():
        print(camp)
    print()
    
    data = [{'a_cid':camp.cid, 'b_reach':camp.reach, 'c_demand': camp.campaign_demand_temp(),
             'd_initial budget bid': camp.initial_budget_bid(),
             'e_camp opport bid': PineAppleAgent.campaignOpportunityBid(camp)} for camp in cc.Campaign.getCampaignList()]
    df = pd.DataFrame(data)
    print(">Campaigns Data:")
    print(df)
#    pd.DataFrame()
#    print(camp, "demand={}, initial budget bid={}".format(camp.campaign_demand_temp(), camp.initial_budget_bid()))

def test_ucs_desired_level():
    seg = sc.MarketSegment.segments['OMH']
    size = 6*seg.size
    camp = cc.Campaign(0, 1, 4, [seg], size , 1, 1, "WALLA")
    camp.targetedImpressions = size/2
    camp.impressions_goal = size+100
    print("desired level of UCS for campaigns = {}".format(ucs.ucsManager.get_desired_UCS_level(1, [camp])))
    
def test_ImpsOptimization():
    Q_old = 0.9
    camp = cc.Campaign.campaigns[1]
    print("optimizing target number of impressions for {}".format(camp))
    camp.budget=1600
    print("campaign demand is {}".format(camp.campaign_demand_temp()))
    
    x = [i for i in range(0,8000,20)]
    y = [ camp.campaign_profit_for_ImpsTarget_estim(imps, Q_old) for imps in x]
    plt.plot(x,y)
    
    f = lambda x: -camp.campaign_profit_for_ImpsTarget_estim(x, Q_old)
    x0 = [camp.reach]
    res = optimize.basinhopping(f, x0, niter=1)
    print("optimal number of imps for camp is {}".format( res.x))
    
def test_statisticalCampaigns():
    for day in cc.Campaign.statistic_campaigns:
        print("statistical campaigns for day = {}".format(day))
        for seg in cc.Campaign.statistic_campaigns[day]:
            print (seg, ':', cc.Campaign.statistic_campaigns[day][seg])
    
def main():
    print("PineApple!")
    init_actions = [sc.MarketSegment.segments_init, cc.Campaign.statistic_campaigns_init]
    for action in init_actions:
        action()
    
    tests = [test_segments_and_demand, test_ImpsOptimization, test_ucs_desired_level]# test_statisticalCampaigns]
    for test in tests:
        print()
        print ("Running test: {}".format(test.__name__))
        test()

    
    
if __name__ == "__main__":
    main()