# API Gateway HTTP API como entrada unica del sistema.
#
# Flujo: navegador -> API Gateway (valida JWT de Entra ID) -> BFF.
# El Authorizer solo valida firma/iss/aud; los roles y scopes los sigue
# aplicando Spring Security en el BFF y en los microservicios.
resource "aws_apigatewayv2_api" "http" {
  name          = "${var.management_name}-http-api"
  protocol_type = "HTTP"

  cors_configuration {
    allow_headers = ["authorization", "content-type"]
    allow_methods = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
    allow_origins = ["https://${aws_cloudfront_distribution.spa.domain_name}"]
    max_age       = 3600
  }

  tags = {
    Name = "${var.management_name}-http-api"
  }
}

# Integracion proxy hacia el BFF (puerto 8080). La ruta /api/{proxy+} se traduce
# 1:1 a http://<eip>:8080/api/{proxy}.
resource "aws_apigatewayv2_integration" "bff" {
  api_id                 = aws_apigatewayv2_api.http.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_eip.app.public_ip}:8080/api/{proxy}"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_authorizer" "azure" {
  api_id           = aws_apigatewayv2_api.http.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  name             = "azure-ad"

  jwt_configuration {
    audience = [var.azure_audience]
    issuer   = var.azure_issuer
  }
}

resource "aws_apigatewayv2_route" "api" {
  api_id             = aws_apigatewayv2_api.http.id
  route_key          = "ANY /api/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.bff.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure.id
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.http.id
  name        = "$default"
  auto_deploy = true
}
