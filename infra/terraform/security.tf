# Security group de la unica instancia.
#  - 80   : SPA servida por nginx
#  - 8080 : BFF, consumido por API Gateway (no tiene IPs de salida fijas, por eso
#           no se puede restringir solo al API Gateway; el BFF valida el token igual)
#  - 22   : administracion (restringir con ssh_cidr en production/entrega)
resource "aws_security_group" "app" {
  name        = "${var.management_name}-sg-app"
  description = "Acceso a la instancia unica de SIGA"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP - SPA"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "BFF - consumido por API Gateway"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH administracion"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.ssh_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.management_name}-sg-app"
  }
}
