# -*- coding: utf-8 -*-
"""
Created on Sat Feb 18 16:53:51 2017

@author: Eyal
"""
import pandas as pd

class MarketSegment:
    segments = {}
    
    def __init__(self, name, size):
        self.name = name
        self.size = size
    
    def __repr__(self):
        return "Segment {}, size {}".format(self.name,self.size)
    
    def addSize(self, x):
        self.size += x
    
    def segments_init():
        segments = MarketSegment.segments
        population = pd.read_csv('data//population.csv')
        for index, row in population.iterrows():
            name = row['age']+row['gender']+row['income']
            if not name in segments:
                segments[name] = MarketSegment(name, row['size'])
            else:
                segments[name].addSize(row['size'])

MarketSegment.segments_init()
