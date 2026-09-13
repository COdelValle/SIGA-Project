# Guía de Configuración de Secretos - SIGA CI/CD Pipeline

Para que el pipeline de GitHub Actions despliegue automáticamente en las instancias EC2 y conecte los servicios a MariaDB y Azure Entra ID, debes registrar los siguientes secretos en el repositorio.

Ruta en GitHub: **Settings -> Secrets and variables -> Actions -> New repository secret**

---

## Lista de Secretos Requeridos

| Nombre del Secreto | Descripción | Ejemplo / Valor |
| :--- | :--- | :--- |
| `AWS_ACCESS_KEY_ID` | Access Key de la sesión activa de AWS | `ASIA...` |
| `AWS_SECRET_ACCESS_KEY` | Secret Key de la sesión activa de AWS | `wJalrXUtnFEMI...` |
| `AWS_SESSION_TOKEN` | Token temporal de sesión (AWS Learner Lab) | `IQoJb3JpZ2luX2VjE...` |
| `AWS_REGION` | Región donde se aprovisionó la infraestructura | `us-east-1` |
| `EC2_USER` | Usuario SSH del sistema operativo base | `ubuntu` |
| `EC2_SSH_KEY` | Contenido completo de tu clave privada PEM | `-----BEGIN RSA PRIVATE KEY----- ...` |
| `EC2_FRONT_HOST` | IP pública o DNS de la instancia Frontend | `54.x.x.x` |
| `EC2_BFF_HOST` | IP pública o host bastion para el BFF | `52.x.x.x` *(o IP privada vía salto)* |
| `EC2_BACKEND_HOST` | IP pública o host bastion para los Microservicios | `54.y.y.y` *(o IP privada vía salto)* |
| `BACKEND_PRIVATE_IP` | IP estática privada asignada a la EC2 Backend | `10.0.2.30` |
| `DB_PRIVATE_IP` | IP estática privada asignada a la EC2 MariaDB | `10.0.2.40` |
| `DB_USER` | Usuario de base de datos para los microservicios | `siga_user` |
| `DB_PASSWORD` | Contraseña configurada para el usuario de MariaDB | `TuPassword` |
| `AZURE_TENANT_ID` | Directory (tenant) ID de Azure Entra ID | `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` |
| `AZURE_CLIENT_ID` | Application (client) ID de la app en Azure | `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` |
| `AZURE_APP_ID_URI` | Identificador URI del recurso/API en Azure | `api://siga-backend` |
| `AZURE_CLIENT_SECRET` | Valor del secreto del cliente generado en Azure | `~xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx` |
| `AZURE_API_APP_ID` | Client ID asociado a Microsoft Graph / Roles | `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` |
