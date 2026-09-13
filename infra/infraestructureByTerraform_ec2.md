# Requisitos

crear la lave en la interfaz con ESTE nombre:  SIGA_KEY

# Notas Operativas Críticas

1. **Renovación de credenciales (AWS Learner Lab):**  
   Las credenciales temporales (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN`) caducan al cerrar o reiniciar la sesión del laboratorio (aprox. cada 4 horas).

2. **Formato de la clave SSH (`EC2_SSH_KEY`):**  
   Asegúrate de copiar el archivo `.pem` completo, incluyendo los encabezados:
   ```text
   -----BEGIN RSA PRIVATE KEY-----
   ... (contenido de la clave) ...
   -----END RSA PRIVATE KEY-----