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
        '''
        param ongoing_camps in a list of campaigns active tomorrow
        param day is tomorrow
        '''
        if len(ongoing_camps) == 0:
            print ("#get_desired_UCS_level: No ongoing campaigns")
            return 7
        ''' setting lvl to argmax I_target .... as defined in the document '''
        camp = sorted(ongoing_camps, key=lambda x: (x.impressions_goal - x.targetedImpressions)/((x.endDay - day  + 1)*x.sizeOfSegments()) , reverse=True)[0]        
        level_no_round = (camp.impressions_goal - camp.targetedImpressions)/((camp.endDay - day + 1)*camp.sizeOfSegments())
        #print ("#get_desired_UCS_level: level_no_round = {}".format(level_no_round))
        #find rounded level
        lvlacc = math.pow(0.9,7) #lowest
        lvl = 7
        while (lvlacc < level_no_round and lvlacc <= 1):
            lvlacc = lvlacc / 0.9
            lvl -= 1
        print("#get_desired_UCS_level: desired level {}".format(lvl))
        return lvl
    
    def predict_required_price_to_win_desired_UCS_level(ucs_level, day, number_of_active_networks, number_of_last_day_networks):
        '''
        param day is the day of calculating the bid
        param ucs_level is the desired level tomorrow
        params number_of_* are calculated for tomorrow
        '''
        segments = ["OML", "OMH", "OFL", "OFH", "YML", "YMH", "YFL", "YFH"]
        demands = [sc.MarketSegment.segments[segment].segment_demand(day,cc.Campaign.getCampaignList()) for segment in segments]
        if ucs_level >= 7:
            return 0
        training = pd.read_csv('data//ucs_level_statistics.csv')        
        X = list(training.columns[1:-7])
        y = [training.columns[-7 + ucs_level]]
        print("#predict_required_price_to_win_desired_UCS_level: ", training.head(1))
        print("#predict_required_price_to_win_desired_UCS_level: ", X)
        print("#predict_required_price_to_win_desired_UCS_level: ", y)
        clf = svm.SVR()
        clf.fit(training[X], training[y].values.ravel())
        y_pred = clf.predict([[day, number_of_active_networks,number_of_last_day_networks]+demands])
        return float(y_pred)