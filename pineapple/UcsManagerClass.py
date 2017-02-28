# -*- coding: utf-8 -*-
"""
Created on Wed Feb 22 00:17:33 2017

@author: Eyal
"""
import math

import CampaignClass as cc

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
    
    def predict_required_price_to_win_desired_UCS_level():
        pass        