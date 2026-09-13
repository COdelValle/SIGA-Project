#!/bin/bash
set -euxo pipefail

export DEBIAN_FRONTEND=noninteractive

# --- Paquetes base ---
apt-get update -y
apt-get install -y ca-certificates curl gnupg unzip lsb-release

# --- Docker Engine + compose plugin ---
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable docker
systemctl start docker
usermod -aG docker ubuntu

# --- AWS CLI v2 (se usa el LabInstanceProfile, sin credenciales en disco) ---
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscliv2.zip
unzip -q /tmp/awscliv2.zip -d /tmp
/tmp/aws/install
rm -rf /tmp/aws /tmp/awscliv2.zip

# --- Swap 2GB (t3.medium con 9 contenedores) ---
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  if ! grep -q '/swapfile' /etc/fstab; then
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
  fi
fi

# --- Montar el volumen EBS de datos (etiqueta siga-data) ---
# Se monta por LABEL para sobrevivir a que el dispositivo cambie de nombre
# (en Nitro, /dev/sdf aparece como /dev/nvme1n1). Solo se formatea si el disco
# no tiene filesystem, para no borrar datos en reinicios.
DATA_MOUNT=/home/ubuntu/siga-data
mkdir -p "$DATA_MOUNT"

DATA_DEV=""
for i in $(seq 1 30); do
  DATA_DEV=""
  for dev in $(lsblk -dpno NAME | grep -E '/dev/(nvme|sd)'); do
    MP=$(lsblk -no MOUNTPOINT "$dev" | tr -d '[:space:]')
    TYPE=$(lsblk -no TYPE "$dev")
    if [ "$TYPE" = "disk" ] && [ -z "$MP" ]; then
      DATA_DEV="$dev"
    fi
  done
  if [ -n "$DATA_DEV" ]; then
    break
  fi
  sleep 2
done

if [ -n "$DATA_DEV" ]; then
  if ! blkid "$DATA_DEV" >/dev/null 2>&1; then
    mkfs.ext4 -L siga-data "$DATA_DEV"
  fi
  if ! mountpoint -q "$DATA_MOUNT"; then
    mount LABEL=siga-data "$DATA_MOUNT" 2>/dev/null || mount "$DATA_DEV" "$DATA_MOUNT"
  fi
  if ! grep -q 'LABEL=siga-data' /etc/fstab; then
    echo 'LABEL=siga-data /home/ubuntu/siga-data ext4 defaults,nofail 0 2' >> /etc/fstab
  fi
fi

# El contenedor MariaDB corre como uid 999
mkdir -p "$DATA_MOUNT/mariadb"
chown -R 999:999 "$DATA_MOUNT/mariadb"

# --- Archivos de la aplicacion ---
APP_DIR=/home/ubuntu/siga
mkdir -p "$APP_DIR"
echo "${docker_compose_b64}" | base64 -d > "$APP_DIR/docker-compose.yml"
echo "${init_db_b64}" | base64 -d > "$APP_DIR/init-db.sh"
chmod +x "$APP_DIR/init-db.sh"

# Placeholders que el CD sobreescribe con los valores reales
[ -f "$APP_DIR/.env" ] || touch "$APP_DIR/.env"
[ -f "$APP_DIR/config.json" ] || echo '{"bffBaseUrl":"/api"}' > "$APP_DIR/config.json"

chown -R ubuntu:ubuntu /home/ubuntu/siga /home/ubuntu/siga-data
