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
    "afad9bb4-4804-4b86-965d-34b67774f629",
    "api://afad9bb4-4804-4b86-965d-34b67774f629",
  ]
}

variable "azure_issuer" {
  description = "issuer exacto del token. Verificar v1/v2 con jwt.ms antes de aplicar"
  type        = string
  default     = "https://login.microsoftonline.com/c3cfc64e-def3-450f-aee6-07ff80bd6831/v2.0"
}
