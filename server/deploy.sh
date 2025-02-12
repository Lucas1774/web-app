#!/bin/bash
# Compiles using build.sh and deploys into Azure VM
# Deploys also heartbeat script unless specified with --no-heart-beat
# but does not attempt to set up a chron script
BUILD_SCRIPT="./build.sh"
OUTPUT_FILE="server.jar"
HEARTBEAT_SCRIPT="check_app.sh"
VM_USER="azureuser"
VM_URL="$VM_USER@ferafera.ddns.net"

NO_HEARTBEAT=false
for arg in "$@"; do
    if [ "$arg" == "--no-heart-beat" ]; then
        NO_HEARTBEAT=true
        break
    fi
done

echo "Building $OUTPUT_FILE with $BUILD_SCRIPT..."
if ! [ -f "$BUILD_SCRIPT" ]; then
    echo "Error: Build script not found."
    exit 1
fi
"$BUILD_SCRIPT"

if ! [ -f "$OUTPUT_FILE" ]; then
    echo "Error: $OUTPUT_FILE not created."
    exit 1
fi

echo "Transferring $OUTPUT_FILE to $VM_URL..."
if ! scp "$OUTPUT_FILE" "$VM_URL:/home/$VM_USER"; then
    echo "Error: File transfer failed."
    exit 1
fi

if [ "$NO_HEARTBEAT" = false ]; then
    if ! [ -f "$HEARTBEAT_SCRIPT" ]; then
        echo "Warning: $HEARTBEAT_SCRIPT not found, skipping transfer."
    else
        echo "Transferring $HEARTBEAT_SCRIPT to $VM_URL..."
        if ! scp "$HEARTBEAT_SCRIPT" "$VM_URL:/home/$VM_USER"; then
            echo "Error: Heartbeat script transfer failed."
            exit 1
        fi
    fi
else
    echo "Skipping heartbeat script transfer."
fi

echo "Killing previous $OUTPUT_FILE..."
if ! ssh "$VM_URL" "sudo pkill -f \"$OUTPUT_FILE\""; then
    echo "Error: Failed to stop the existing server."
    exit 1
fi
echo 'Existing server stopped, if any.'

echo "Starting $OUTPUT_FILE on the VM..."
if ! ssh "$VM_URL" "sudo nohup java -jar $OUTPUT_FILE > server.out 2>&1 &"; then
    echo "Error: Failed to start server.jar on the VM."
    exit 1
fi
echo 'Server started successfully!'

echo "Press any key to continue."
read -r -n 1
