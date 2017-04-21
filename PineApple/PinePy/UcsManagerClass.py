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


#def eprint(*args, **kwargs):
##    print(*args, file=sys.stderr, **kwargs)
#    with open("runlog.log", "a+") as logFile:
#        print(*args, file=logFile, **kwargs)
##        logFile.write(*args)
    
class ucsManager:
    
    def level_accuracy(lvl):
        '''
        gets lvl int 0..7
        return the accuracy degree of as 0.9'''
        if 0 <= lvl <= 7:
            return math.pow(0.9, lvl)
        else:
            raise ValueError("valid levels are 0..7, but got {}!".format(lvl))
    
    def get_desired_UCS_level(day, ongoing_camps):
        '''
        param ongoing_camps in a list of campaigns active tomorrow
        param day is tomorrow
        returns level in 0..7
        '''
        if len(ongoing_camps) == 0:
#            eprint ("#get_desired_UCS_level: No ongoing campaigns")
            return 7
        ''' setting lvl to argmax I_target .... as defined in the document '''
        camp = sorted(ongoing_camps, key=lambda x: max((x.impressions_goal - x.targetedImpressions), 0)/((x.endDay - day  + 1)*x.sizeOfSegments()) , reverse=True)[0]        
        level_no_round = max((camp.impressions_goal - camp.targetedImpressions), 0)/((camp.endDay - day + 1)*camp.sizeOfSegments())
        
        #find rounded level
        lvlacc = math.pow(0.9,7) #lowest
        lvl = 7
        while (lvlacc < level_no_round and lvl >= 1):
            lvlacc = lvlacc / 0.9
            lvl -= 1
        #print("#get_desired_UCS_level: desired level {}".format(lvl))
#        eprint ("#get_desired_UCS_level: level_no_round = {}, returned lvl = {}".format(level_no_round, lvl))
        return lvl
    
    def predict_required_price_to_win_desired_UCS_level(ucs_level, day, number_of_active_networks, number_of_last_day_networks):
        '''
        param day is the day of calculating the bid
        param ucs_level is the desired level tomorrow
        params number_of_* are calculated for tomorrow
        '''

        segment_networks = [sc.MarketSegment.segments[segment].number_of_active_netowrks_on_segment(day, cc.Campaign.getCampaignList()) for segment in sc.MarketSegment.segment_names]
        if ucs_level >= 7:
            return 0
        
        training = pd.read_csv('..//data//ucs_level_statistics.csv')
        
        X = list(training.columns[2:-7])
        y = [training.columns[-7 + ucs_level]]
            
        #print("#predict_required_price_to_win_desired_UCS_level: ", training.head(1))
        #print("#predict_required_price_to_win_desired_UCS_level: ", X)
        #print("#predict_required_price_to_win_desired_UCS_level: ", y)
        clf = svm.SVR()
        clf.fit(training[X], training[y].values.ravel())
        
        y_pred = clf.predict([[day, number_of_active_networks,number_of_last_day_networks]+segment_networks])
        pred = float(y_pred)
        if day<=4:
            return pred * 1.27
        elif day<18:
            return pred
        elif day<45:
            return pred
        else:
            return pred/1.05