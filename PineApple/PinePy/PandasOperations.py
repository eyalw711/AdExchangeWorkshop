# -*- coding: utf-8 -*-
"""
Created on Mon Apr 17 01:09:40 2017

@author: Eyal
"""

import pandas as pd
from SegmentClass import MarketSegment
from CampaignClass import Campaign
import os
import glob


def compute_campaign_desicion(profit, completion):
    if profit > 0 and completion >= 1:
        return 1
    elif profit > 10 and completion >= 0.9:
        return 1
    elif profit > 20 and completion >= 0.8:
        return 1
    return -1

#TODO: get rid of the pandas
def campagin_statistics_assignment():
    first = True
    dir_path = "..//data//statistics"
    number_of_files = len(glob.glob1(dir_path,"*.csv"))
    counters = [0]*488
    for file in os.listdir(dir_path):
        if file.endswith(".csv"):
            file_path = os.path.join(dir_path, file)
            if (first):
                data = pd.read_csv(file_path)
                number_of_rows = data.shape[0]
                for i in range (0,number_of_rows):
                    for j in range (4,9):
                        data.iat[i,j] = 0
                first = False
            data2 = pd.read_csv(file_path)
            for i in range (0,number_of_rows):
                if(int(data2.iat[i,5]) != 0):
                    for j in range (4,8):   
                        data.iat[i,j] = (float(data.iat[i,j])*counters[i]+(float(data2.iat[i,j])))/(1+counters[i])
                    counters[i] += 1
                data.iat[i,8] += float(data2.iat[i,8])/number_of_files
    data.to_csv('..//data//campaign_statistics.csv',index = False)
    
    #TODO: get rid of the pandas
def campagin_protabiloity_assign_desicion():
    data = pd.read_csv('..//data//campaigns_profitability.csv')
    number_of_rows = data.shape[0]
    for i in range (0,number_of_rows):
        data.at[i,"decision"] = Campaign.compute_campaign_desicion(data.at[i,"profit"],data.at[i,"completion_percentage"])
    data.to_csv('..//data//campaigns_profitability.csv',index = False)
    
    #TODO: get rid of the pandas
def campagin_protabiloity_assign_demand():
    data = pd.read_csv('..//data//campaigns_profitability.csv')
    number_of_rows = data.shape[0]

    i = 0
    while i < number_of_rows:
        campaigns = {}
        indices = []
        (cid, budget, start, end, vidCoeff, mobCoeff, reach) = (data.at[i,"cid"], data.at[i,"budget"], data.at[i,"start"], data.at[i,"end"],
        data.at[i,"vidCoeff"], data.at[i,"mobCoeff"], data.at[i,"reach"])
        segmentsNames = [seg_name for seg_name in MarketSegment.segment_names if data.at[i,seg_name] == 1]
        cmp = Campaign(cid, start, end, segmentsNames, reach, vidCoeff, mobCoeff)       
        campaigns[cid] = cmp

        indices += [i]
        current_sim =  data.at[i,"sim"]
        i += 1
        if i < number_of_rows:
            sim = data.at[i,"sim"]
        else:
            sim = -1

        while current_sim == sim: # while the next campagin is part of the current simulation
            (cid, budget, start, end, vidCoeff, mobCoeff, reach) = (data.at[i,"cid"], data.at[i,"budget"], data.at[i,"start"], data.at[i,"end"],
            data.at[i,"vidCoeff"], data.at[i,"mobCoeff"], data.at[i,"reach"])
            segments = [seg_name for seg_name in MarketSegment.segment_names if data.at[i,seg_name] == 1]
            cmp = Campaign(cid, start, end, segments, reach, vidCoeff, mobCoeff)       
            campaigns[cid] = cmp
            
            indices += [i]
            current_sim =  data.at[i,"sim"]
            i += 1
            if i < number_of_rows:
                sim = data.at[i,"sim"]
            else:
                sim = -1

                    
        # all campaigns in current simulation were read - time to calculate the demand:
        campaign_list = campaigns.values()            
        for j in indices:
           cmp_cid = campaigns[data.at[j,"cid"]]
           data.at[j,"demand"]  = MarketSegment.segment_set_demand_forDays(cmp_cid.segments, cmp_cid.startDay, cmp_cid.endDay, campaign_list)
        
                      
    # write to CSV:     
    data.to_csv('..//data//campaigns_profitability.csv', index = False)