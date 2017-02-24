# -*- coding: utf-8 -*-
"""
Created on Wed Feb 22 00:12:36 2017

@author: Eyal
"""
import CampaignClass as cc

class Agent:
    def __init__(self):
        self.quality = 0.9
        self.my_campaigns = {}
    
    def campaignOpportunityBid(self, campaign):
        COB = campaign.initial_budget_bid()
        if (COB < campaign.reach*self.quality) and (COB > campaign.reach/(10*self.quality)):
                return COB
        elif COB >= campaign.reach*self.quality:
            return (campaign.reach*self.quality) - 0.1
        
        else:
            return campaign.reach/(10*self.quality) + 0.1
        

        
    
        