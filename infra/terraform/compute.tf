locals {
  # Los templates se inyectan en base64 para que Terraform no intente interpolar
  # las variables ${...} propias de docker compose.
  user_data = templatefile("${path.module}/templates/user-data.sh", {
    docker_compose_b64 = base64encode(file("${path.module}/templates/docker-compose.yml"))
    init_db_b64        = base64encode(file("${path.module}/templates/init-db.sh"))
  })
}

resource "aws_instance" "app" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public.id
  key_name               = var.key_name
  iam_instance_profile   = "LabInstanceProfile"
  vpc_security_group_ids = [aws_security_group.app.id]

  # El rol LabRole ya concede permisos para descargar de ECR y demas servicios.
  # user_data_replace_on_change = false evita que un cambio en el script reemplace
  # la instancia y, con ello, destruya el estado local.
  user_data                   = local.user_data
  user_data_replace_on_change = false

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  tags = {
    Name = "${var.management_name}-app"
  }
}
