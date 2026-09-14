# SIGA - Infraestructura AWS (Terraform)

Topología para **AWS Academy Learner Lab**: una instancia EC2 `t3.medium`, un
volumen EBS dedicado para los datos de MariaDB y un API Gateway HTTP API que
valida el JWT de Azure AD antes de llegar al BFF.

```
Navegador (Angular + MSAL)
  |  HTTPS + Bearer <token Entra ID>
  v
API Gateway HTTP API  -- JWT Authorizer
  |  HTTP_PROXY http://<eip>:8080/api/{proxy}
  v
EC2 t3.medium (EIP)
  |- nginx:80        SPA + /config.json
  |- bff-web:8080
  |- ms-*:8081/8082/8086/8087
  |- mariadb:3306    4 bases
  `- /home/ubuntu/siga-data  (EBS gp3, prevent_destroy)
```

## Requisitos

- Terraform >= 1.5 y AWS CLI.
- Credenciales vigentes del laboratorio: AWS Academy -> *AWS Details* ->
  *Show* -> copiar `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y
  `AWS_SESSION_TOKEN` al entorno (o `~/.aws/credentials`). **Expiran cada ~4 h.**

## Uso

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars   # ajustar issuer/audience si aplica
terraform init
terraform fmt
terraform validate
terraform plan
terraform apply
```

Después del apply, usar los outputs:

```bash
terraform output ec2_public_ip
terraform output api_gateway_invoke_url
```

## Persistencia de datos

- Los datos de MariaDB viven en el volumen EBS etiquetado `siga-data`
  (`/home/ubuntu/siga-data`), separado del disco raíz.
- Detener/arrancar la instancia ("End Lab" / "Start Lab") conserva los datos.
- Reemplazar la instancia (cambio de AMI o de `user_data`) conserva los datos.
- `prevent_destroy` evita que `terraform destroy` borre el volumen; para
  eliminarlo a propósito hay que quitar ese `lifecycle` del `ebs.tf`.
- Sin backups por ahora: un **Reset** del laboratorio borra el volumen y los
  datos no se recuperan.
- El CD nunca usa `docker compose down -v`.

## Inicialización de la base

1. `init-db.sh` (en el primer arranque con datos vacíos) crea las 4 bases y el
   usuario.
2. **Flyway** en cada microservicio crea/evoluciona el esquema al arrancar
   (`ddl-auto: validate`). Una base vacía se auto-inicializa.

## Notas del laboratorio

- Regiones permitidas: `us-east-1` / `us-west-2`.
- Perfil IAM pre-creado: `LabInstanceProfile` (rol `LabRole`); no se crean IAM.
- Key pair por defecto en `us-east-1`: `vockey`.
- Sin NAT Gateway (consume presupuesto y sigue cobrando fuera de sesión).
- EBS gp3 de 10 GB ≈ **$0.80/mes** aunque la instancia esté detenida.

## Secretos de GitHub Actions

Ruta: **Settings -> Secrets and variables -> Actions**. Los `AWS_*` se refrescan
desde *AWS Details* antes de cada `workflow_dispatch`.

| Secreto | Origen / valor |
| --- | --- |
| `AWS_ACCESS_KEY_ID` | AWS Details (sesión vigente) |
| `AWS_SECRET_ACCESS_KEY` | AWS Details (sesión vigente) |
| `AWS_SESSION_TOKEN` | AWS Details (sesión vigente) |
| `AWS_REGION` | `us-east-1` |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | contenido de la clave privada `.pem` (`vockey`) |
| `DB_USER` | usuario de MariaDB, p. ej. `siga_user` |
| `DB_PASSWORD` | password del usuario de MariaDB |
| `MARIADB_ROOT_PASSWORD` | password de root de MariaDB |
| `AZURE_TENANT_ID` | Entra ID tenant |
| `AZURE_CLIENT_ID` | Entra ID client de la API |
| `AZURE_APP_ID_URI` | `api://<client-id>` |
| `AZURE_CLIENT_SECRET` | secreto del cliente (Microsoft Graph) |
| `AZURE_API_APP_ID` | app que define los app roles |

`EC2_HOST` y `API_GW_INVOKE_URL` **no** se configuran como secrets: el CD los
resuelve dinámicamente por tag/nombre (`SIGA-app` y `SIGA-http-api`) usando las
credenciales AWS, de modo que sobreviven a recrear la infraestructura.

## Redirect URIs de Azure (SPA)

Registrar en la app SPA:

- `http://<EIP>/auth` (y `http://localhost:4200/auth` para local)
- post-logout: `http://<EIP>/sin-acceso`

Antes de aplicar, verificar con `jwt.ms` / `jwt.io` el `iss` y el `aud` reales
del access token y ajustarlos en `terraform.tfvars` (`azure_issuer`, `azure_audience`).

