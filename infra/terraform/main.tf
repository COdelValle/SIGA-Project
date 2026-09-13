resource "aws_instance" "ec2_frontend" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = "t2.micro"
  subnet_id              = aws_subnet.public_subnt.id
  private_ip             = "10.0.1.10"
  key_name               = var.key_name
  iam_instance_profile   = "LabInstanceProfile"
  vpc_security_group_ids = [aws_security_group.sg_front.id]
  user_data              = local.docker_userdata

  tags = { Name = "siga-ec2-front" }
}

resource "aws_instance" "ec2_bff" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = "t2.micro"
  subnet_id              = aws_subnet.private_subnt.id
  private_ip             = "10.0.2.20"
  key_name               = var.key_name
  iam_instance_profile   = "LabInstanceProfile"
  vpc_security_group_ids = [aws_security_group.sg_bff.id]

  user_data = <<-EOF
    ${local.docker_userdata}

    # Configuración de 2GB de Swap para Spring Boot en t2.micro
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
  EOF

  tags = { Name = "siga-ec2-bff" }
}

resource "aws_instance" "ec2_backend" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = "t3.large"
  subnet_id              = aws_subnet.private_subnt.id
  private_ip             = "10.0.2.30"
  key_name               = var.key_name
  iam_instance_profile   = "LabInstanceProfile"
  vpc_security_group_ids = [aws_security_group.sg_backend.id]
  user_data              = local.backend_userdata

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  tags = { Name = "siga-ec2-backends" }
}

resource "aws_instance" "ec2_data" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = "t3.medium"
  subnet_id              = aws_subnet.private_subnt.id
  private_ip             = "10.0.2.40"
  key_name               = var.key_name
  iam_instance_profile   = "LabInstanceProfile"
  vpc_security_group_ids = [aws_security_group.sg_data.id]

  user_data = <<-EOF
    ${local.docker_userdata}
    sleep 10

    # Crear directorios de trabajo
    mkdir -p /home/ubuntu/siga-data/mariadb_data
    mkdir -p /home/ubuntu/siga-data/init_scripts

    # Generar script de inicialización DDL/DCL
    cat << 'SQL_EOF' > /home/ubuntu/siga-data/init_scripts/init.sql
    CREATE DATABASE IF NOT EXISTS siga_usuarios_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS siga_estudiantes_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS siga_asignaturas_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS siga_notas_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

    GRANT ALL PRIVILEGES ON siga_usuarios_db.* TO '${var.db_user}'@'%';
    GRANT ALL PRIVILEGES ON siga_estudiantes_db.* TO '${var.db_user}'@'%';
    GRANT ALL PRIVILEGES ON siga_asignaturas_db.* TO '${var.db_user}'@'%';
    GRANT ALL PRIVILEGES ON siga_notas_db.* TO '${var.db_user}'@'%';
    FLUSH PRIVILEGES;
    SQL_EOF

    # Generar docker-compose.yml con healthcheck y persistencia
    cat << 'COMPOSE_EOF' > /home/ubuntu/siga-data/docker-compose.yml
    version: '3.8'

    services:
      siga-mariadb:
        image: mariadb:11.4
        container_name: siga-mariadb
        restart: unless-stopped
        ports:
          - "3306:3306"
        environment:
          MARIADB_ROOT_PASSWORD: ${var.db_root_password}
          MARIADB_USER: ${var.db_user}
          MARIADB_PASSWORD: ${var.db_password}
        volumes:
          - /home/ubuntu/siga-data/mariadb_data:/var/lib/mysql
          - /home/ubuntu/siga-data/init_scripts:/docker-entrypoint-initdb.d
        command:
          - --character-set-server=utf8mb4
          - --collation-server=utf8mb4_unicode_ci
          - --max_connections=200
        healthcheck:
          test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
          interval: 10s
          timeout: 5s
          retries: 5
    COMPOSE_EOF

    chown -R ubuntu:ubuntu /home/ubuntu/siga-data
    cd /home/ubuntu/siga-data
    docker compose up -d
  EOF

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  tags = { Name = "siga-ec2-mariadb" }
}