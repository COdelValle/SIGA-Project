# Volumen EBS dedicado para los datos de MariaDB.
#
# Persistencia:
#  - Al detener/arrancar la instancia ("End Lab"/"Start Lab") los datos sobreviven.
#  - Al reemplazar la EC2 (cambio de AMI o de user_data) los datos sobreviven porque
#    viven en un volumen separado del disco raiz.
#  - prevent_destroy impide que `terraform destroy` borre el volumen por accidente.
#    Para eliminarlo a proposito hay que quitar el lifecycle primero.
#
# Nota: el volumen cobra por GB-mes aunque la instancia este detenida.
resource "aws_ebs_volume" "data" {
  availability_zone = var.availability_zone
  size              = var.data_volume_size
  type              = "gp3"

  tags = {
    Name = "${var.management_name}-data"
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_volume_attachment" "data" {
  device_name = "/dev/sdf"
  volume_id   = aws_ebs_volume.data.id
  instance_id = aws_instance.app.id
}
