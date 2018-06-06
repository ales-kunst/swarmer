set LOGFILE=swarm.log
del /f .\target\demo-swarm-01.jar .\target\demo-swarm-02.jar .\target\demo-swarm-03.jar .\target\demo-swarm-04.jar .\target\demo-swarm-05.jar 
copy .\target\demo-swarm.jar .\target\demo-swarm-01.jar
copy .\target\demo-swarm.jar .\target\demo-swarm-02.jar
copy .\target\demo-swarm.jar .\target\demo-swarm-03.jar
copy .\target\demo-swarm.jar .\target\demo-swarm-04.jar
copy .\target\demo-swarm.jar .\target\demo-swarm-05.jar
start "panter BLUE 01" /D .\target cmd /c java -Djava.io.tmpdir=D:\swarm_temp -Dswarm.bind.address=127.0.0.1 -Dswarm.management.http.port=9901 -Dfile.encoding=UTF-8 -Dswarm.http.port=8001 -jar demo-swarm-01.jar
start "panter BLUE 02" /D .\target cmd /c java -Djava.io.tmpdir=D:\swarm_temp -Dswarm.bind.address=127.0.0.1 -Dswarm.management.http.port=9902 -Dfile.encoding=UTF-8 -Dswarm.http.port=8002 -jar demo-swarm-02.jar
start "panter BLUE 03" /D .\target cmd /c java -Djava.io.tmpdir=D:\swarm_temp -Dswarm.bind.address=127.0.0.1 -Dswarm.management.http.port=9903 -Dfile.encoding=UTF-8 -Dswarm.http.port=8003 -jar demo-swarm-03.jar
start "panter BLUE 04" /D .\target cmd /c java -Djava.io.tmpdir=D:\swarm_temp -Dswarm.bind.address=127.0.0.1 -Dswarm.management.http.port=9904 -Dfile.encoding=UTF-8 -Dswarm.http.port=8004 -jar demo-swarm-04.jar
start "panter BLUE 05" /D .\target cmd /c java -Djava.io.tmpdir=D:\swarm_temp -Dswarm.bind.address=127.0.0.1 -Dswarm.management.http.port=9905 -Dfile.encoding=UTF-8 -Dswarm.http.port=8005 -jar demo-swarm-05.jar