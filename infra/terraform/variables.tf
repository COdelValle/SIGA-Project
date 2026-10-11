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
  description = "Tipo de instancia. Tipos soportados en el lab: nano, micro, small, medium, large. El stack completo (13 servicios + clúster RabbitMQ de 2 nodos + MariaDB) supera los 5.8 GB en contenedores: usar t3.large."
  type        = string
  default     = "t3.large"
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

variable "cors_allowed_origins" {
  description = "Origenes exactos (scheme://host[:port]) permitidos por CORS en el API Gateway. En produccion la SPA y /api comparten el mismo origen (URL del Gateway), por lo que CORS no aplica al flujo normal; se declara igualmente como requisito y permite pruebas desde otros origenes."
  type        = list(string)
  default     = ["http://localhost:4200"]
}

# --- Azure AD / Entra ID (solo lo que necesita el JWT Authorizer) ---
# Estos valores NO son secretos: se validan firma/iss/aud, no se guarda credencial.
variable "azure_tenant_id" {
  description = "Tenant de Entra ID que emite los tokens (parte del issuer y del authority del SPA)"
  type        = string
  default     = "f260a804-82ac-4b57-bb01-9ed6626d71ff"
}

variable "azure_client_id" {
  description = "Client ID de la app SPA/API que usa el frontend (config.json)"
  type        = string
  default     = "448f165b-4fab-45c4-9088-766044e1a004"
}

variable "azure_app_id_uri" {
  description = "App ID URI de la API (api://<client-id>); prefijo de los scopes que pide el SPA"
  type        = string
  default     = "api://448f165b-4fab-45c4-9088-766044e1a004"
}

variable "azure_audience" {
  description = "claims aud aceptados por el JWT Authorizer (GUID para tokens v2 y api://<client-id> para v1)"
  type        = list(string)
  default = [
    "448f165b-4fab-45c4-9088-766044e1a004",
    "api://448f165b-4fab-45c4-9088-766044e1a004",
  ]
}

variable "azure_issuer" {
  description = "issuer EXACTO del access token (v1: https://sts.windows.net/<tenant>/; v2: https://login.microsoftonline.com/<tenant>/v2.0). Verificar con jwt.ms: si la app tiene accessTokenAcceptedVersion nulo/1 emite v1 y este valor debe ser el de sts.windows.net"
  type        = string
  # La app actual (448f165b) emite tokens v1.0 (iss = sts.windows.net), por eso el
  # Authorizer se configura con ese issuer. Si la app pasa a emitir v2 (manifest
  # accessTokenAcceptedVersion = 2), actualizar a la URL de login.microsoftonline.com.
  default = "https://sts.windows.net/f260a804-82ac-4b57-bb01-9ed6626d71ff/"
}
