#!/bin/bash
if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <pg_user> <pg_database>"
  exit 1
fi

PGUSER="$1"
PGDATABASE="$2"

cd ./docker/dev || exit 1
echo "Stopping and removing containers..."
docker compose down -v
echo "Starting fresh containers..."
docker compose up -d

echo "Dumping database '$PGDATABASE' as user '$PGUSER' from remote..."
ssh azureuser@ferafera.ddns.net  "docker exec prod_postgres_1 pg_dump -U $PGUSER -Fc $PGDATABASE" > ~/mydb.dump
echo "Restoring section pre-data into local container..."
docker exec -i dev-postgres-1 pg_restore -U myuser -d mydatabase --section=pre-data --disable-triggers < ~/mydb.dump
echo "Restoring section data into local container..."
docker exec -i dev-postgres-1 pg_restore -U myuser -d mydatabase --section=data --disable-triggers < ~/mydb.dump
echo "Restoring section post-data into local container..."
docker exec -i dev-postgres-1 pg_restore -U myuser -d mydatabase --section=post-data --disable-triggers < ~/mydb.dump

docker compose stop
