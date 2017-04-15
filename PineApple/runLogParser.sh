#!/bin/bash
#
# Usage
#   sh ./runServer.sh
#

TACAA_HOME=`pwd`
echo $TACAA_HOME
echo $CLASSPATH
java -cp "lib/*" se.sics.tasim.logtool.Main -handler tau.tac.adx.parser.GeneralHandler -file /home/sleviim/Workspaces/PineApple/logs/sims/ -ucs -rating -bank -campaign -adnet

#java -cp "lib/*" se.sics.tasim.logtool.Main -handler tau.tac.adx.parser.GeneralHandler -file LOG_FILE_PATH/game.slg.gz -ucs -rating -bank -campaign -adnet





#java -cp "lib/*" se.sics.tasim.logtool.Main -handler tau.tac.adx.parser.GeneralHandler -file /home/student/Downloads/adx_1.3.0/adx-server/logs/sims/localhost_sim7.slg -ucs -rating -bank -campaign -adnet
#java -cp "lib/*" se.sics.tasim.logtool.Main -handler tau.tac.adx.parser.GeneralHandler -file /home/student/Downloads/adx_1.3.0/adx-server/logs/sims/localhost_sim7.slg -ucs
#java -cp "lib/*" se.sics.tasim.logtool.Main -handler tau.tac.adx.parser.GeneralHandler -file /home/student/Downloads/adx_1.3.0/adx-server/public_html/localhost/history/7/game.slg.gz -ucs
#not working java -cp "lib/*" se.sics.tasim.logtool.Main -handler tau.tac.adx.parser.GeneralHandler -file http://localhost:8080/localhost/history/7/game.slg.gz -ucs
