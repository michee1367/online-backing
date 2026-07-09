variable "aws_region" {
  type    = string
  default = "af-south-1"
}

variable "environment" {
  type    = string
  default = "prod-af"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "availability_zones" {
  type    = list(string)
  default = ["af-south-1a", "af-south-1b", "af-south-1c"]
}

variable "eks_node_instance_type" {
  type    = string
  default = "m6i.xlarge"
}

variable "eks_node_desired_size" {
  type    = number
  default = 6
}

variable "eks_node_max_size" {
  type    = number
  default = 20
}

variable "rds_instance_class" {
  type    = string
  default = "db.r6g.2xlarge"
}

variable "rds_allocated_storage" {
  type    = number
  default = 500
}

variable "redis_node_type" {
  type    = string
  default = "cache.r6g.large"
}

variable "msk_broker_instance_type" {
  type    = string
  default = "kafka.m5.2xlarge"
}
