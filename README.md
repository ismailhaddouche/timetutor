Documento de Requisitos: TimeTutor
1. Resumen de la Aplicación
TimeTutor es una aplicación móvil nativa para Android, desarrollada en Kotlin y utilizando Firebase como backend. Su objetivo es proporcionar a los profesores particulares una herramienta sencilla para gestionar sus horarios, alumnos y facturación. A su vez, ofrece a los alumnos un portal para consultar sus clases programadas y el estado de sus facturas.

2. Pila Tecnológica
Plataforma: Android (Nativo)

Lenguaje de Programación: Kotlin

Backend y Base de Datos: Firebase (Firestore, Authentication)

3. Requisitos Generales
3.1. Autenticación
El inicio de sesión y registro se realizarán exclusivamente a través del proveedor de autenticación de Google (Google Sign-In) mediante Firebase Authentication.

La aplicación debe gestionar de forma segura las credenciales y el estado de la sesión del usuario.

3.2. Roles de Usuario
La aplicación contará con dos perfiles de usuario distintos:

Profesor: Rol con acceso completo a la gestión de horarios, alumnos, categorías y facturación.

Alumno: Rol con acceso de solo lectura a sus horarios y facturas, y con capacidad para editar su propio perfil.

4. Flujo de Usuario y Vistas Comunes
4.1. Vista de Inicio (Onboarding)
Al abrir la aplicación por primera vez, se presentará una pantalla que permitirá al usuario:

Registrarse con Google: Si es un usuario nuevo.

Iniciar sesión con Google: Si ya tiene una cuenta.

Tras una autenticación exitosa, el sistema redirigirá al usuario a la vista principal correspondiente a su rol (Profesor o Alumno).

5. Funcionalidades del Perfil de Profesor
5.1. Vista de Inicio (Horario)
Visualización del Horario:

Se mostrará un calendario/horario. Por defecto, la vista será diaria, pero podrá cambiarse a una vista mensual.

El horario mostrará franjas horarias ocupadas con las clases reservadas, indicando el/los alumno(s) asignado(s). Una misma franja puede tener varios alumnos.

Gestión de Clases (Franjas Horarias):

Añadir: El profesor podrá crear nuevas clases. Al hacerlo, deberá:

Asociar la clase a uno o más alumnos activos.

Definir la hora de inicio y fin (duración mínima de 30 minutos).

Establecer una repetición (opcional): semanal, bisemanal (cada dos semanas), mensual.

Definir el rango de fechas para la repetición ("desde" y "hasta").

Modificar: Permitirá cambiar los detalles de una clase ya existente.

Eliminar: Borrará una clase del horario.

Seguimiento de Asistencia:

Una vez finalizada la hora de una clase, el profesor podrá marcarla con uno de los siguientes estados: Asistida o No Asistida.

5.2. Vista de Perfil
El profesor podrá consultar y editar sus datos personales:

Nombre y Apellidos

Número de WhatsApp

Bizum (si aplica)

Correo Electrónico

5.3. Vista de Alumnos
Se mostrará un listado de alumnos organizados en tres pestañas:

Activos: Alumnos que actualmente reciben clases.

Inactivos: Alumnos que han sido archivados por el profesor.

Pendientes: Alumnos que han enviado una solicitud para unirse al calendario del profesor.

Detalle de Alumno Activo:

Al hacer clic en un alumno activo, se desplegará una vista detallada con su perfil y tres pestañas:

Clases Dadas: Historial de clases impartidas (Asistidas/No Asistidas) que aún no han sido incluidas en una factura.

Facturas: Listado de todas las facturas generadas para ese alumno. Cada factura indicará su estado (Pagada o Pendiente). El profesor podrá cambiar el estado de una factura desde aquí.

Categoría: Muestra la categoría actual del alumno. El profesor podrá cambiarla seleccionando otra categoría previamente creada. Regla de negocio: Un cambio de categoría afectará a todas las clases del mes en curso que no hayan sido facturadas, pero nunca a las clases ya facturadas.

Generación de Factura:

Dentro del detalle de un alumno, un botón "Generar Factura" calculará el importe total.

Lógica de cálculo: El sistema sumará las horas de todas las clases "Asistidas" no facturadas y las multiplicará por el precio/hora definido en la categoría actual del alumno.

5.4. Vista de Categorías
El profesor podrá gestionar las categorías de precios para sus clases.

Crear: Añadir una nueva categoría especificando un nombre (ej. "Clases de Bachillerato") y un precio por hora (€/hora).

Ver/Eliminar: Visualizar un listado de las categorías existentes y eliminar las que ya no necesite.

5.5. Vista de Facturas
Mostrará un listado completo de todas las facturas generadas, ordenadas por fecha de creación.

Cada factura mostrará su estado (Pagada/Pendiente).

El profesor podrá acceder al detalle de cada factura y cambiar su estado.

6. Funcionalidades del Perfil de Alumno
6.1. Vista de Inicio (Mis Clases)
El alumno podrá ver su horario de clases asignadas.

Dispondrá de una vista diaria (por defecto) y una vista semanal.

6.2. Vista de Perfil
El alumno podrá consultar y editar sus datos de contacto:

Nombre y Apellidos (solo lectura).

Correo Electrónico.

Número de WhatsApp.

6.3. Vista de Facturas
El alumno verá un listado de sus facturas, indicando el estado (Pagada/Pendiente).

Podrá ver el detalle de cada factura e imprimirla.

Esta vista es de solo lectura; el alumno no puede modificar el estado de las facturas.

6.4. Vista de Configuración
Tema: Permitirá cambiar la apariencia de la aplicación entre modo claro y oscuro.

Política de Privacidad: Enlace para consultar los términos de uso y la política de privacidad.

Eliminar Cuenta: Opción para que el alumno elimine su cuenta.

7. Gestión de Datos y Cuenta
7.1. Eliminación de Cuenta de Alumno
Si un alumno elimina su cuenta:

Se revocará su acceso a la aplicación.

Se eliminarán sus datos de perfil personal (correo, WhatsApp).

No se eliminarán: Su nombre, su ID de cliente, las facturas ya generadas, ni el historial de clases asociadas a él. Estos datos se conservarán para mantener la integridad de los registros del profesor.
