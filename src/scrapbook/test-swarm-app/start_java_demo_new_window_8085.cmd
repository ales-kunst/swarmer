set LOGFILE=swarm.log
start "panter BLUE" /D .\target cmd /c java -Djava.io.tmpdir=D:\swarm_temp -Dswarm.management.http.port=9991 -Dswarm.bind.address=127.0.0.1 -Dfile.encoding=UTF-8 -Dswarm.http.port=8085 -jar demo-swarm.jar