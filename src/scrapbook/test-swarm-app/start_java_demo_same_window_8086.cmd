set LOGFILE=swarm.log
java -Djava.io.tmpdir=D:\swarm_temp -Dswarm.bind.address=127.0.0.1 -Dswarm.management.http.port=9992 -Dfile.encoding=UTF-8 -Dswarm.http.port=8086 -jar .\target\demo-swarm.jar
pause