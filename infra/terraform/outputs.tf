output "ec2_public_ip" {
  description = "IP publica de la instancia (referencia; el CD la descubre por tag SIGA-app)"
  value       = aws_eip.app.public_ip
}

output "api_gateway_invoke_url" {
  description = "Invoke URL del API Gateway (referencia; el CD la descubre por nombre SIGA-http-api)"
  value       = aws_apigatewayv2_api.http.api_endpoint
}

output "frontend_url" {
  description = "URL publica HTTPS del SPA (servido por API Gateway). Registrar en Azure como redirect URI base"
  value       = aws_apigatewayv2_api.http.api_endpoint
}

output "azure_redirect_uri" {
  description = "Redirect URI a registrar en la app SPA de Entra ID"
  value       = "${aws_apigatewayv2_api.http.api_endpoint}/auth"
}

output "azure_post_logout_redirect_uri" {
  description = "Post-logout redirect URI a registrar en la app SPA de Entra ID"
  value       = "${aws_apigatewayv2_api.http.api_endpoint}/sin-acceso"
}

output "ssh_command" {
  description = "Comando SSH para administrar la instancia"
  value       = "ssh -i tu-clave.pem ubuntu@${aws_eip.app.public_ip}"
}

locals {
  # Espejo de los scopes que pide el SPA (apps/frontend/public/config.json y el
  # config.json que genera el CD). Mantener los tres en sincronia.
  frontend_scopes = [
    "${var.azure_app_id_uri}/Acceso.Base",
    "${var.azure_app_id_uri}/usuarios:read",
    "${var.azure_app_id_uri}/usuarios:write",
    "${var.azure_app_id_uri}/usuarios:update",
    "${var.azure_app_id_uri}/usuarios:delete",
    "${var.azure_app_id_uri}/estudiantes:read",
    "${var.azure_app_id_uri}/estudiantes:write",
    "${var.azure_app_id_uri}/estudiantes:update",
    "${var.azure_app_id_uri}/estudiantes:delete",
    "${var.azure_app_id_uri}/asignaturas:read",
    "${var.azure_app_id_uri}/asignaturas:write",
    "${var.azure_app_id_uri}/asignaturas:update",
    "${var.azure_app_id_uri}/asignaturas:delete",
    "${var.azure_app_id_uri}/notas:read",
    "${var.azure_app_id_uri}/notas:write",
    "${var.azure_app_id_uri}/notas:update",
    "${var.azure_app_id_uri}/notas:delete",
    "${var.azure_app_id_uri}/docentes:read",
    "${var.azure_app_id_uri}/docentes:write",
    "${var.azure_app_id_uri}/docentes:update",
    "${var.azure_app_id_uri}/docentes:delete",
    "${var.azure_app_id_uri}/apoderados:read",
    "${var.azure_app_id_uri}/apoderados:write",
    "${var.azure_app_id_uri}/apoderados:update",
    "${var.azure_app_id_uri}/apoderados:delete",
    "${var.azure_app_id_uri}/clases:read",
    "${var.azure_app_id_uri}/clases:write",
    "${var.azure_app_id_uri}/clases:update",
    "${var.azure_app_id_uri}/clases:delete",
    "${var.azure_app_id_uri}/evaluaciones:read",
    "${var.azure_app_id_uri}/evaluaciones:write",
    "${var.azure_app_id_uri}/evaluaciones:update",
    "${var.azure_app_id_uri}/evaluaciones:delete",
    "${var.azure_app_id_uri}/horarios:write",
    "${var.azure_app_id_uri}/horarios:update",
    "${var.azure_app_id_uri}/horarios:delete",
    "${var.azure_app_id_uri}/inscripciones:read",
    "${var.azure_app_id_uri}/inscripciones:write",
    "${var.azure_app_id_uri}/inscripciones:update",
    "${var.azure_app_id_uri}/asistencias:read",
    "${var.azure_app_id_uri}/asistencias:write",
    "${var.azure_app_id_uri}/asistencias:update",
    "${var.azure_app_id_uri}/asistencias:delete",
  ]
}

output "frontend_config_json" {
  description = "Contenido esperado de config.json para el despliegue"
  value = jsonencode({
    bffBaseUrl = "${aws_apigatewayv2_api.http.api_endpoint}/api"
    msal = {
      clientId              = var.azure_client_id
      authority             = "https://login.microsoftonline.com/${var.azure_tenant_id}"
      redirectUri           = "/auth"
      postLogoutRedirectUri = "/sin-acceso"
      scopes                = local.frontend_scopes
    }
  })
}
