set LOGFILE=swarm.log
java -Djava.util.logging.manager=org.jboss.logmanager.LogManager -Djava.io.tmpdir=D:\swarm_temp -Dswarm.bind.address=127.0.0.1 -Dfile.encoding=UTF-8 -Dswarm.http.port=8100 -jar .\target\demo-swarm.jar
pause