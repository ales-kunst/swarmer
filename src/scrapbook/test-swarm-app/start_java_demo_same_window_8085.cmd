set LOGFILE=swarm.log
java -Djava.io.tmpdir=D:\swarm_temp -Dswarm.management.http.port=9991 -Dswarm.bind.address=127.0.0.1 -Dfile.encoding=UTF-8 -Dswarm.http.port=8085 -jar .\target\demo-swarm.jar -S consul-error
pause