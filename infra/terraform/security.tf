resource "aws_security_group" "sg_front" {
  name        = "siga-sg-front"
  description = "Security group para el Frontend"
  vpc_id      = aws_vpc.main_vpc.id

  ingress {
    description = "HTTP desde Internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "sg_bff" {
  name        = "siga-sg-bff"
  description = "Security group para el BFF"
  vpc_id      = aws_vpc.main_vpc.id

  ingress {
    description     = "Trafico desde Front al BFF"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.sg_front.id]
  }

  ingress {
    description = "SSH para Deploy/Admin"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}


resource "aws_security_group" "sg_backend" {
  name        = "siga-sg-backend"
  description = "Security group para Microservicios Core"
  vpc_id      = aws_vpc.main_vpc.id

  ingress {
    description     = "ms-usuarios-auth y ms-estudiantes"
    from_port       = 8081
    to_port         = 8082
    protocol        = "tcp"
    security_groups = [aws_security_group.sg_bff.id]
  }

  ingress {
    description     = "ms-asignaturas y ms-notas"
    from_port       = 8086
    to_port         = 8087
    protocol        = "tcp"
    security_groups = [aws_security_group.sg_bff.id]
  }

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "sg_data" {
  name        = "siga-sg-data"
  description = "Security group para MariaDB"
  vpc_id      = aws_vpc.main_vpc.id

  ingress {
    description     = "MariaDB desde Microservicios"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.sg_backend.id]
  }

  ingress {
    description = "SSH para Admin"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}