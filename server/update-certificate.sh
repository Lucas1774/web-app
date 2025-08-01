#!/bin/bash

ENV_FILE=".env"

KEYSTORE_PASSWORD=$(grep -oP '^KEY_STORE_PASSWORD\s*=\s*\K.*' "$ENV_FILE" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

if [ -z "$KEYSTORE_PASSWORD" ]; then
  echo "Key store password not found in $ENV_FILE"
  exit 1
fi

VM_USER="ubuntu"
VM_URL="ferafera.ddns.net"
CERT_PATH="/etc/letsencrypt/live/ferafera.ddns.net"
LOCAL_CERT_PATH="src/main/resources/certificates"

# If for whatever reason the keystore exists and the password changes, just remove the existing keystore and run again
echo "Creating or updating keystore on the remote VM with renewed SSL certificate..."
ssh "$VM_USER@$VM_URL" "
  sudo certbot renew;
  sudo openssl pkcs12 -export -in $CERT_PATH/fullchain.pem -inkey $CERT_PATH/privkey.pem -out $CERT_PATH/keystore.p12 -passout pass:$KEYSTORE_PASSWORD -passin pass:$KEYSTORE_PASSWORD;
  sudo keytool -importkeystore -srckeystore $CERT_PATH/keystore.p12 -srcstoretype PKCS12 -destkeystore $CERT_PATH/keystore.jks -deststoretype PKCS12 -storepass $KEYSTORE_PASSWORD -srcstorepass $KEYSTORE_PASSWORD;
  sudo cp /etc/letsencrypt/live/ferafera.ddns.net/keystore.jks /home/ubuntu/ && sudo chown ubuntu:ubuntu /home/ubuntu/keystore.jks
"

echo "Transferring the keystore to the local machine..."
scp "$VM_USER@$VM_URL:/home/ubuntu/keystore.jks" "$LOCAL_CERT_PATH/keystore.jks"

echo "Keystore update completed successfully."
echo "Press any key to exit."
read -n 1
