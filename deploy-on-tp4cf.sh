#!/usr/bin/env bash

APP_NAME="robert"
APP_VERSION="0.0.1-SNAPSHOT"

COMMAND=$1

GENAI_CHAT_SERVICE_NAME="robert-llm"
GENAI_CHAT_PLAN_NAME="llama3.1" # plan must have chat capabilty

GENAI_EMBEDDINGS_SERVICE_NAME="robert-embedding"
GENAI_EMBEDDINGS_PLAN_NAME="nomic-embed-text" # plan must have Embeddings capabilty

PGVECTOR_SERVICE_NAME="robert-db"
PGVECTOR_PLAN_NAME="on-demand-postgres-small"
PGVECTOR_EXTERNAL_PORT=1025

case $COMMAND in

setup)

    echo && printf "\e[37mℹ️  Creating services ...\e[m\n" && echo

    cf create-service postgres $PGVECTOR_PLAN_NAME $PGVECTOR_SERVICE_NAME -c "{\"svc_gw_enable\": true, \"router_group\": \"default-tcp\", \"external_port\": $PGVECTOR_EXTERNAL_PORT}" -w
	printf "Waiting for service $PGVECTOR_SERVICE_NAME to create."
	while [ `cf services | grep 'in progress' | wc -l | sed 's/ //g'` != 0 ]; do
  		printf "."
  		sleep 5
	done
	echo "$PGVECTOR_SERVICE_NAME creation completed."

    echo && printf "\e[37mℹ️  Creating $GENAI_CHAT_SERVICE_NAME and $GENAI_EMBEDDINGS_SERVICE_NAME GenAI services ...\e[m\n" && echo
    cf create-service genai $GENAI_CHAT_PLAN_NAME $GENAI_CHAT_SERVICE_NAME
    cf create-service genai $GENAI_EMBEDDINGS_PLAN_NAME $GENAI_EMBEDDINGS_SERVICE_NAME

    echo && printf "\e[37mℹ️  Deploying $APP_NAME application ...\e[m\n" && echo
    cf push $APP_NAME -k 1GB -m 1GB -p build/libs/$APP_NAME-$APP_VERSION.jar --no-start --random-route

    echo && printf "\e[37mℹ️  Binding services ...\e[m\n" && echo
    cf bind-service $APP_NAME $PGVECTOR_SERVICE_NAME
    cf bind-service $APP_NAME $GENAI_CHAT_SERVICE_NAME
    cf bind-service $APP_NAME $GENAI_EMBEDDINGS_SERVICE_NAME

    echo && printf "\e[37mℹ️  Setting environment variables for use by $APP_NAME application ...\e[m\n" && echo
    cf set-env $APP_NAME JAVA_OPTS "-Djava.security.egd=file:///dev/urandom -XX:+UseG1GC -XX:+UseStringDeduplication"
    cf set-env $APP_NAME SPRING_PROFILES_ACTIVE "default,openai,pgvector"
    cf set-env $APP_NAME JBP_CONFIG_OPEN_JDK_JRE "{ jre: { version: 21.+ } }"
    cf set-env $APP_NAME JBP_CONFIG_SPRING_AUTO_RECONFIGURATION "{ enabled: false }"

    echo && printf "\e[37mℹ️  Starting $APP_NAME application ...\e[m\n" && echo
    cf start $APP_NAME

    ;;

teardown)
    cf unbind-service $APP_NAME $PGVECTOR_SERVICE_NAME
    cf unbind-service $APP_NAME $GENAI_CHAT_SERVICE_NAME
    cf unbind-service $APP_NAME $GENAI_EMBEDDINGS_SERVICE_NAME

    cf delete-service $PGVECTOR_SERVICE_NAME -f
    cf delete-service $GENAI_CHAT_SERVICE_NAME -f
    cf delete-service $GENAI_EMBEDDINGS_SERVICE_NAME -f

    cf delete $APP_NAME -f -r

    ;;

*)
    echo && printf "\e[31m⏹  Usage: setup/teardown \e[m\n" && echo
    ;;
esac
