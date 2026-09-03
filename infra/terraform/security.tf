resource "aws_security_group" "front_sg" {
  name        = "front_sg"
  description = "Allow HTTP and SSH traffic to Frontend"
  vpc_id      = aws_vpc.main_vpc.id
  
  ingress {
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

  #PROXIMO: Aún no hay un port definido para front..
  #ingress {
    #from_port   = 8080
    #to_port     = 8080
    #protocol    = "tcp"
    #cidr_blocks = ["0.0.0.0/0"]
  #}
}