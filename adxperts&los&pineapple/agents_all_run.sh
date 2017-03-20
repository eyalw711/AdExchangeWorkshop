#!/bin/bash

function pause(){
   read -p "$*"
}

gnome-terminal --tab --working-directory /home/sleviim/Documents/university/sadna/sadna_workspace/adx-server -e ./runServer.sh
pause 'Please wait until server will start running... Then, press the [Enter] key'

gnome-terminal --window --working-directory /home/sleviim/Workspaces/PineApple -e ./runAgent.sh
gnome-terminal --window --working-directory /home/sleviim/Documents/university/sadna/sadna_workspace/adx-agent-AdXperts -e ./runAgent.sh
gnome-terminal --window --working-directory /home/sleviim/Documents/university/sadna/sadna_workspace/adx-agent-BCM -e ./runAgent.sh
gnome-terminal --window --working-directory /home/sleviim/Documents/university/sadna/sadna_workspace/adx-agent-Giza -e ./runAgent.sh
gnome-terminal --window --working-directory /home/sleviim/Documents/university/sadna/sadna_workspace/adx-agent-IBM -e ./runAgent.sh
gnome-terminal --window --working-directory /home/sleviim/Documents/university/sadna/sadna_workspace/adx-agent-LC -e ./runAgent.sh
gnome-terminal --window --working-directory /home/sleviim/Documents/university/sadna/sadna_workspace/adx-agent-nitzan -e ./runAgent.sh
gnome-terminal --window --working-directory /home/sleviim/Documents/university/sadna/sadna_workspace/adx-agent-OOS -e ./runAgent.sh
