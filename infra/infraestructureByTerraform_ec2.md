## ☁️ Modo de Trabajo de la Infraestructura (AWS & Terraform)

La topología de red y los recursos en la nube se encuentran aprovisionados de manera modular a través de archivos de configuración de Terraform:

- PRÓXIMO **`main.tf`:** Inicialización de proveedores cloud y configuraciones principales de la arquitectura global.
- **`network.tf`:** Declaración de la VPC, creación de subredes públicas (Frontend) y subredes privadas (Backend), tablas de ruteo e Internet Gateway / NAT Gateway para aislar los entornos de datos.
- **`security.tf`:** Definición de las reglas de firewalls lógicos mediante *Security Groups*. Implementa el principio de mínimo privilegio restringiendo el acceso del Backend en general.
- PRÓXIMO **`compute.tf`:** Configuración de las plantillas de lanzamiento (*Launch Templates*) e instancias EC2 destinadas a ejecutar los entornos de contenedores Docker.
- **`variables.tf`** y PRÓXIMO **`outputs.tf`:** Parametrización dinámica del entorno para mitigar datos estáticos y exportación de direcciones IP o DNS clave.

---

Debido a los límites de hardware propios de la capa gratuita, la instancia de Backend cuenta con una optimización del sistema operativo mediante la adición de **2 GB de memoria Swap virtual**, previniendo que los microservicios Java sean detenidos de forma abrupta por el gestor de memoria física (*OOM Killer*).
> *PD: Esto no es una solución a largo plazo.*


