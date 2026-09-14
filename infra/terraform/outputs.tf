output "ec2_public_ip" {
  description = "IP publica de la instancia (usar como EC2_HOST en los secretos de GitHub)"
  value       = aws_eip.app.public_ip
}

output "api_gateway_invoke_url" {
  description = "Invoke URL del API Gateway. Usar como API_GW_INVOKE_URL en los secretos; el frontend llama a <url>/api"
  value       = aws_apigatewayv2_api.http.api_endpoint
}

output "frontend_url" {
  description = "URL publica del SPA"
  value       = "http://${aws_eip.app.public_ip}"
}

output "ssh_command" {
  description = "Comando SSH para administrar la instancia"
  value       = "ssh -i tu-clave.pem ubuntu@${aws_eip.app.public_ip}"
}

output "frontend_config_json" {
  description = "Contenido esperado de config.json para el despliegue"
  value = jsonencode({
    bffBaseUrl = "${aws_apigatewayv2_api.http.api_endpoint}/api"
    msal = {
      clientId              = "afad9bb4-4804-4b86-965d-34b67774f629"
      authority             = "https://login.microsoftonline.com/c3cfc64e-def3-450f-aee6-07ff80bd6831"
      redirectUri           = "/auth"
      postLogoutRedirectUri = "/sin-acceso"
      scopes                = ["api://afad9bb4-4804-4b86-965d-34b67774f629/Acceso.Base"]
    }
  })
}
