# You need to install sshpass to run the script correctly

bold=$(tput bold)
normal=$(tput sgr0)
echo "${bold}*** Script Logstash acess ***${normal}"

export SSHPASS="rumoao20"
export DOCKER="java_app_1"
export LOGSTASH="loving_raman"

###
echo -e "\n${bold}->${normal} Docker ${bold}p3g2@192.168.160.73${normal}"
sshpass -p $SSHPASS ssh p3g2@192.168.160.73 << EOF
  docker cp java_app_1:/usr/app/logs/spring-boot-logger-log4j2.log .
  exit
EOF
echo -e "\n${bold}->${normal} Aceder Maquina ${bold}p3g2@192.168.160.73${normal}"
sshpass -p $SSHPASS scp p3g2@192.168.160.73:spring-boot-logger-log4j2.log ./

docker cp spring-boot-logger-log4j2.log $LOGSTASH:./

echo -e "\n${bold}* Execução do código completo *${normal}"
