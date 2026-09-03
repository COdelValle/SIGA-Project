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

# Por si se requiere para disponibilidad. #

# variable "azs" {
#   type    = list(string)
#   default = ["us-east-1a", "us-east-1b"]
# }