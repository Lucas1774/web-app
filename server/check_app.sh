#!/bin/bash

if pgrep -f "server" > /dev/null; then
  echo "Java application is running"
else
  echo "Java application is NOT running"
  sudo nohup java -jar ~/server.jar &
fi
