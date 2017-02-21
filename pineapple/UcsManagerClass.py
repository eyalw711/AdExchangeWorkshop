# -*- coding: utf-8 -*-
"""
Created on Wed Feb 22 00:17:33 2017

@author: Eyal
"""

import CampaignClass as cc

class ucsManager:
    
    def get_desired_UCS_level(day, ongoing_camps):
        if len(ongoing_camps) == 0:
            return 8
        camp = sorted(ongoing_camps, key=lambda x: (x.impressions_goal - x.targetedImpressions)/((day - x.endDay + 1)*x.sizeOfSegments()) , reverse=True)[0]
        level_no_round = (camp.impressions_goal - camp.targetedImpressions)/((day - camp.endDay + 1)*camp.sizeOfSegments())
        #find rounded level
        lvlacc = 0.9**7 #lowest
        lvl = 8
        while (lvlacc < level_no_round):
            lvlacc = lvlacc / 0.9
            lvl += 1
        return lvl
    
            