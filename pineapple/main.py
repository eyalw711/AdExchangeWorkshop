# -*- coding: utf-8 -*-
"""
Created on Sat Feb 18 19:23:55 2017

@author: Eyal
"""

import CampaignClass as cc
import SegmentClass as sc
import numpy
from scipy import optimize
import matplotlib.pyplot as plt

''' log comments
import logging

logger = None

def init_logger():
    global logger = logging.getLogger('PineApple')
    handler = logging.FileHandler('/logs/pineLog.log')
    formatter = '''
    
def test1():
    #{O/Y}{M/F}{H/L}
    camps_segs = [['OMH','YFL'],['OMH'],['OMH','YFL']]#,['YML','OML','OFH']]
    for i in range(3):
        camp_seg_list = [sc.MarketSegment.segments[name] for name in camps_segs[i]]
        camp = cc.Campaign(i, i, i+3, camp_seg_list, (i+1)*1000 , 1, 1, "WALLA")
        camp.assignCampaign("Eyal")
    campaigns = cc.Campaign.getCampaignList()  
    for segment in sc.MarketSegment.getSegmentsList():
        print (segment.segment_demand_numer(3, campaigns))

    for campaign in cc.Campaign.getCampaignList():
        print(campaign)
    
    for camp in cc.Campaign.getCampaignList():
        print("cid={}, demand={}".format(camp.cid, camp.campaign_demand_temp()))
    
def test2():
    Q_old = 0.9
    camp = cc.Campaign.campaigns[2]
    camp.budget=1600
    #x = [i for i in range(0,4000,20)]
    #y = [ camp.campaign_profit_for_ImpsTarget_estim(imps, Q_old) for imps in x]
    #plt.plot(x,y)
    f = lambda x: -camp.campaign_profit_for_ImpsTarget_estim(x, Q_old)
    x0 = [camp.reach]
    res = optimize.basinhopping(f, x0, niter=1)
    print(res)
    
    
def main():
    print("PineApple!")
    sc.MarketSegment.segments_init()
    print("segments initialized!")
    test1()
    test2()
    
if __name__ == "__main__":
    main()