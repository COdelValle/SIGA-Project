# La region esta restringida en AWS Academy Learner Lab a us-east-1 / us-west-2.
provider "aws" {
  region = var.aws_region
}
