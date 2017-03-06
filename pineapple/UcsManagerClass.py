# -*- coding: utf-8 -*-
"""
Created on Wed Feb 22 00:17:33 2017

@author: Eyal
"""
import math

import CampaignClass as cc
import SegmentClass as sc
import pandas as pd
from sklearn import svm

class ucsManager:
    
    ''' return the accuracy degree of some level in the range 0-7 '''
    def level_accuracy(lvl):
        if 0 <= lvl <= 7:
            return math.pow(0.9, lvl)
        else:
            return -1
    
    def get_desired_UCS_level(day, ongoing_camps):
        if len(ongoing_camps) == 0:
            print ("No ongoing campaigns")
            return 7
        ''' setting lvl to argmax I_target .... as defined in the document '''
        camp = sorted(ongoing_camps, key=lambda x: (x.impressions_goal - x.targetedImpressions)/((x.endDay - day  + 1)*x.sizeOfSegments()) , reverse=True)[0]        
        level_no_round = (camp.impressions_goal - camp.targetedImpressions)/((camp.endDay - day + 1)*camp.sizeOfSegments())
        #print ("level_no_round = {}".format(level_no_round))
        #find rounded level
        lvlacc = math.pow(0.9,7) #lowest
        lvl = 7
        while (lvlacc < level_no_round and lvlacc <= 1):
            lvlacc = lvlacc / 0.9
            lvl -= 1
        print("desired level {}".format(lvl))
        return lvl
    
    def predict_required_price_to_win_desired_UCS_level(ucs_level, day, number_of_active_networks, number_of_last_day_networks):
        segments = ["OML", "OMH", "OFL", "OFH", "YML", "YMH", "YFL", "YFH"]
        demands = [sc.MarketSegment.segments[segment].segment_demand(day,cc.Campaign.getCampaignList()) for segment in segments]
        if ucs_level >= 7:
            return 0
        training = pd.read_csv('data//ucs_level_statistics.csv')        
        X = list(training.columns[1:-7])
        y = [training.columns[-7 + ucs_level]]
        print(training.head(1))
        print(X)
        print(y)
        clf = svm.SVR()
        clf.fit(training[X], training[y].values.ravel())
        return clf.predict([[day, number_of_active_networks,number_of_last_day_networks]+demands])