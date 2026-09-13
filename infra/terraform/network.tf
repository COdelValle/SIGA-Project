resource "aws_vpc" "main_vpc" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${var.management_name}-vpc"
  }
}

resource "aws_internet_gateway" "sga_igw" {
  vpc_id = aws_vpc.main_vpc.id

  tags = {
    Name = "${var.management_name}-igw"
  }
}

resource "aws_subnet" "public_subnt" {
  vpc_id                  = aws_vpc.main_vpc.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, 1)
  availability_zone       = "us-east-1a"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.management_name}-public-subnet"
  }
}

resource "aws_subnet" "private_subnt" {
  vpc_id            = aws_vpc.main_vpc.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, 2)
  availability_zone = "us-east-1a"

  tags = {
    Name = "${var.management_name}-private-subnet"
  }
}

resource "aws_eip" "sga_nat" {
  domain = "vpc"

  tags = {
    Name = "${var.management_name}-nat-eip"
  }
}

resource "aws_nat_gateway" "sga_nat_gw" {
  allocation_id = aws_eip.sga_nat.id
  subnet_id     = aws_subnet.public_subnt.id
  depends_on    = [aws_internet_gateway.sga_igw]

  tags = {
    Name = "${var.management_name}-nat-gateway"
  }
}

resource "aws_route_table" "public_rt" {
  vpc_id = aws_vpc.main_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.sga_igw.id
  }

  tags = {
    Name = "${var.management_name}-public-rt"
  }
}

resource "aws_route_table_association" "public_rta" {
  subnet_id      = aws_subnet.public_subnt.id
  route_table_id = aws_route_table.public_rt.id
}

resource "aws_route_table" "private_rt" {
  vpc_id = aws_vpc.main_vpc.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.sga_nat_gw.id
  }

  tags = {
    Name = "${var.management_name}-private-rt"
  }
}

resource "aws_route_table_association" "private_rta" {
  subnet_id      = aws_subnet.private_subnt.id
  route_table_id = aws_route_table.private_rt.id
}