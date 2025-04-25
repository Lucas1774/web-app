#!/bin/bash

if pgrep -f "server" > /dev/null; then
  echo "Java application is running"
else
  echo "Java application not found. Attempting Docker restart..."
  cd "$HOME/deploy/docker/prod" || exit 1
  docker-compose down
  docker-compose build
  docker-compose up -d
  docker image prune -f
fi
