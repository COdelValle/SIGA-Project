variable "aws_region" {
  description = "Region AWS permitida en el Learner Lab"
  type        = string
  default     = "us-east-1"
}

variable "management_name" {
  description = "Prefijo para nombrar y etiquetar los recursos"
  type        = string
  default     = "SIGA"
}

variable "vpc_cidr" {
  description = "CIDR de la VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zone" {
  description = "Zona de disponibilidad (debe ser consistente para EC2 y EBS)"
  type        = string
  default     = "us-east-1a"
}

variable "key_name" {
  description = "Key pair del Learner Lab. En us-east-1 el par por defecto es vockey"
  type        = string
  default     = "vockey"
}

variable "instance_type" {
  description = "Tipo de instancia. Tipos soportados en el lab: nano, micro, small, medium, large"
  type        = string
  default     = "t3.medium"
}

variable "data_volume_size" {
  description = "Tamano en GB del volumen EBS gp3 para MariaDB (cobra aunque la instancia este detenida)"
  type        = number
  default     = 10
}

variable "ssh_cidr" {
  description = "CIDR autorizado para SSH. Restringir a la IP publica del laboratorio"
  type        = string
  default     = "0.0.0.0/0"
}

# --- Azure AD / Entra ID (solo lo que necesita el JWT Authorizer) ---
# Estos valores NO son secretos: se validan firma/iss/aud, no se guarda credencial.
variable "azure_audience" {
  description = "claims aud aceptados por el JWT Authorizer (GUID para tokens v2 y api://<client-id> para v1)"
  type        = list(string)
  default = [
    "448f165b-4fab-45c4-9088-766044e1a004",
    "api://448f165b-4fab-45c4-9088-766044e1a004",
  ]
}

variable "azure_issuer" {
  description = "issuer exacto del token. Verificar v1/v2 con jwt.ms antes de aplicar"
  type        = string
  default     = "https://login.microsoftonline.com/f260a804-82ac-4b57-bb01-9ed6626d71ff/v2.0"
}
