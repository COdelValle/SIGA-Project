output "ec2_public_ip" {
  description = "IP publica de la instancia (referencia; el CD la descubre por tag SIGA-app)"
  value       = aws_eip.app.public_ip
}

output "api_gateway_invoke_url" {
  description = "Invoke URL del API Gateway (referencia; el CD la descubre por nombre SIGA-http-api)"
  value       = aws_apigatewayv2_api.http.api_endpoint
}

output "frontend_url" {
  description = "URL publica HTTPS del SPA (CloudFront). Registrar en Azure como redirect URI base"
  value       = "https://${aws_cloudfront_distribution.spa.domain_name}"
}

output "azure_redirect_uri" {
  description = "Redirect URI a registrar en la app SPA de Entra ID"
  value       = "https://${aws_cloudfront_distribution.spa.domain_name}/auth"
}

output "azure_post_logout_redirect_uri" {
  description = "Post-logout redirect URI a registrar en la app SPA de Entra ID"
  value       = "https://${aws_cloudfront_distribution.spa.domain_name}/sin-acceso"
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
