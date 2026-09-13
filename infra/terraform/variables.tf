provider "aws" {
    region = "us-east-1"
}

data "aws_iam_role" "labrole" {
  name = "LabRole"
}

variable "vpc_cidr" {
  type = string
  default = "10.0.0.0/16"
}

variable "management_name" {
    type = string
  default = "SIGA"
}

variable "key_name" {
  type = string
  default = "SIGA_KEY"
}

variable "db_user" {
  type        = string
  description = "Usuario de la base de datos MariaDB"
  default     = "siga_user"
}

variable "db_password" {
  type        = string
  description = "Contraseña de la aplicación para MariaDB"
  sensitive   = true
}

variable "db_root_password" {
  type        = string
  description = "Contraseña root para el contenedor MariaDB"
  sensitive   = true
}

# Por si se requiere para disponibilidad. #

# variable "azs" {
#   type    = list(string)
#   default = ["us-east-1a", "us-east-1b"]
# }