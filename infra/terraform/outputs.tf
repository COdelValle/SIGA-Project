# ==============================================================================
# OUTPUTS PARA GITHUB ACTIONS SECRETS & ACCESO SSH
# ==============================================================================

output "ec2_frontend_public_ip" {
  description = "IP pública de la EC2 Frontend (Usar para EC2_FRONT_HOST)"
  value       = aws_instance.ec2_frontend.public_ip
}

output "ec2_frontend_public_dns" {
  description = "DNS público de la EC2 Frontend"
  value       = aws_instance.ec2_frontend.public_dns
}

output "ec2_bff_private_ip" {
  description = "IP privada fija de la EC2 BFF (10.0.2.20)"
  value       = aws_instance.ec2_bff.private_ip
}

output "ec2_backend_private_ip" {
  description = "IP privada fija de la EC2 Backend (Usar para BACKEND_PRIVATE_IP en Secrets)"
  value       = aws_instance.ec2_backend.private_ip
}

output "ec2_data_private_ip" {
  description = "IP privada fija de la EC2 MariaDB (Usar para DB_PRIVATE_IP en Secrets)"
  value       = aws_instance.ec2_data.private_ip
}

# ==============================================================================
# URLS DE ACCESO Y COMANDOS DE CONEXIÓN RÁPIDA
# ==============================================================================

output "frontend_url" {
  description = "URL web pública de la aplicación Angular"
  value       = "http://${aws_instance.ec2_frontend.public_ip}"
}

output "ssh_frontend_bastion_cmd" {
  description = "Comando SSH para entrar directamente al Frontend (Bastion)"
  value       = "ssh -i tu-clave.pem ubuntu@${aws_instance.ec2_frontend.public_ip}"
}

output "ssh_backend_proxyjump_cmd" {
  description = "Comando SSH con ProxyJump para entrar a la EC2 Backend a través del Frontend"
  value       = "ssh -i tu-clave.pem -J ubuntu@${aws_instance.ec2_frontend.public_ip} ubuntu@${aws_instance.ec2_backend.private_ip}"
}

output "ssh_data_proxyjump_cmd" {
  description = "Comando SSH con ProxyJump para entrar a la EC2 MariaDB a través del Frontend"
  value       = "ssh -i tu-clave.pem -J ubuntu@${aws_instance.ec2_frontend.public_ip} ubuntu@${aws_instance.ec2_data.private_ip}"
}