# IP elastica: mantiene estable la integracion del API Gateway y el redirect URI
# de Azure AD (SPA) entre stop/start de la instancia. Adjunta a una instancia
# en ejecucion no tiene costo extra.
resource "aws_eip" "app" {
  domain = "vpc"

  tags = {
    Name = "${var.management_name}-eip"
  }
}

resource "aws_eip_association" "app" {
  instance_id   = aws_instance.app.id
  allocation_id = aws_eip.app.id
}
