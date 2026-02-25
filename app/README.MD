# TimeTutor - Proyecto Final DAM

![TimeTutor Logo](https://via.placeholder.com/150/007bff/FFFFFF?text=TimeTutor)
**Alumno:** Ismail Haddouche Rhali
**Grado:** Desarrollo de Aplicaciones Multiplataforma (DAM)

---

## 1. Descripción del Proyecto

TimeTutor es una aplicación Android nativa diseñada para ayudar a los estudiantes a gestionar su tiempo de estudio de manera eficiente. La aplicación permite a los usuarios crear sesiones de estudio personalizadas, registrar el tiempo dedicado a cada materia y visualizar estadísticas para mejorar sus hábitos de aprendizaje. El objetivo es crear una herramienta simple, intuitiva y sin distracciones.

---

## 2. Objetivos Principales (Features)

* **Temporizador de Estudio:** Un temporizador central para controlar las sesiones de estudio y los descansos (técnica Pomodoro).
* **Gestión de Asignaturas:** El usuario podrá añadir, editar y eliminar las asignaturas que está cursando.
* **Registro de Sesiones:** Cada sesión de estudio completada se guardará asociada a una asignatura y una fecha.
* **Estadísticas Básicas:** Módulo para visualizar el tiempo total de estudio por día, semana o por asignatura.
* **Interfaz Limpia y Clara:** El diseño se centrará en la usabilidad, evitando elementos superfluos que puedan distraer al usuario.

---

## 3. Tecnologías Utilizadas

* **Lenguaje:** Kotlin (100% nativo).
* **Arquitectura:** Seguirá principios de código limpio, sin frameworks complejos a menos que sea estrictamente necesario.
* **Base de Datos:** Se utilizará SQLite a través de la librería `AndroidX Room` para la persistencia de datos local.
* **Entorno de Desarrollo:**
    * **Local:** Android Studio.
    * **Nube:** GitHub Codespaces.
* **Control de Versiones:** Git y GitHub.

---

## 4. Estructura del Proyecto

El proyecto sigue la estructura estándar de una aplicación Android:

* `app/src/main/java/`: Contiene todo el código fuente en Kotlin.
* `app/src/main/res/layout/`: Contiene los ficheros de diseño de la interfaz en XML.
* `app/src/main/res/drawable/`: Contiene los recursos gráficos (iconos, imágenes).

---

## 5. Cómo Empezar

1.  Clonar el repositorio: `git clone [URL-DE-TU-REPO]`
2.  Abrir el proyecto con la última versión estable de Android Studio.
3.  Sincronizar las dependencias de Gradle.
4.  Ejecutar la aplicación en un emulador o dispositivo físico.







# TimeTutor Propuesta Proyecto Fin de Grado (FP SUPERIOR DAM ILERNA)
Título del Proyecto
TimeTutor: Aplicación de gestión de horarios y asistencia para profesores de clases particulares
Resumen
La organización de horarios, la asistencia de alumnos y el control de pagos son tareas pesadas y muchas veces liosas para los profesores de clases particulares, y en la práctica suelen llevarse a cabo con medios dispersos (mensajería, hojas de cálculo, calendarios generales) que dificultan una gestión ordenada.
TimeTutor es una aplicación móvil para Android, desarrollada en Kotlin y apoyada en Google Firebase, que busca simplificar estas tareas. El profesor podrá administrar su calendario semanal, asignar alumnos a distintas franjas horarias, registrar la asistencia y generar facturas mensuales. Por su parte, el alumno dispondrá de un acceso sencillo para consultar su horario personalizado, sus facturas y el estado de sus pagos. Con ello se pretende dotar de mayor eficiencia y profesionalidad a un ámbito que a menudo se gestiona de forma informal.
Objetivos Generales
Gestión completa para profesores: Planificación de horarios, administración de alumnos, control de asistencia y facturación simplificada.
Acceso y consulta para alumnos: Entorno accesible y claro para consultar información relevante.
Autenticación segura: Acceso mediante cuenta de Google para mayor comodidad y seguridad.
Consentimiento mutuo: Se requiere el consentimiento tanto del profesor como del alumno para formar parte de una clase, fomentando la privacidad y el control.
Optimización de recursos: Aplicación ligera y con bajo coste de mantenimiento para asegurar su rentabilidad a largo plazo.
Objetivos Específicos
Gestión de usuarios


Implementar un sistema de autenticación con Google, diferenciando perfiles de profesor y alumno.


Diseñar un flujo de vinculación mediante código único, con solicitud por parte del alumno y aprobación por parte del profesor.


Incluir la aceptación de una Política de Privacidad y Términos de Servicio durante el registro.


Garantizar la posibilidad de modificar o eliminar la cuenta y los datos asociados, cumpliendo la normativa de protección de datos.


Gestión de horarios y clases


Proporcionar al profesor una interfaz de calendario para crear, modificar o eliminar franjas horarias semanales.


Permitir la asignación de alumnos a dichas franjas.


Aplicar una norma de negocio que impida cancelar o modificar clases con menos de dos horas de antelación.


Asistencia y pagos


Registrar la asistencia de los alumnos en cada clase impartida.


Generar informes mensuales y facturas por alumno, que el profesor podrá marcar como pagadas o pendientes.


Permitir al alumno consultar el histórico de asistencia y facturación.


Informes y comunicación


Facilitar la generación de reportes digitales que integren horarios, asistencia y pagos.


Mejorar la comunicación entre profesor y alumno a través de una plataforma unificada.



Pregunta 2: Tecnologías y herramientas utilizadas
La selección de tecnologías responde a la necesidad de diseñar una aplicación moderna, fiable y enmarcada en el ecosistema de desarrollo de Google, que permita un desarrollo ágil y una fácil integración de servicios.
Android
Plataforma sobre la que se desplegará la aplicación. Ha sido elegida por ser el sistema operativo móvil más utilizado y por facilitar el acceso a millones de usuarios.


Android Studio
Entorno de desarrollo oficial para Android. Permite programar en Kotlin, probar la aplicación en simuladores y depurar el código de forma eficiente.


Kotlin
Lenguaje oficial recomendado por Google para Android. Su sintaxis clara y concisa favorece la programación orientada a objetos y un mantenimiento más sencillo que con Java.


Firebase (Google)
Se ha optado por un modelo Backend as a Service para simplificar la gestión de infraestructura. Los servicios principales serán:


Firebase Authentication: para gestionar el acceso seguro de los usuarios mediante Google.


Cloud Firestore: base de datos en la nube, NoSQL, con sincronización en tiempo real.


Firebase Cloud Messaging (FCM): envío de notificaciones en tiempo real a profesores y alumnos.


Firebase Functions: ejecución de lógica de servidor sin necesidad de infraestructura dedicada.


SQLite
Base de datos local para almacenar información crítica en el dispositivo, permitiendo acceso a datos aun sin conexión a Internet.


Jetpack Compose
Librería oficial de Google para construir interfaces modernas y dinámicas en Android, más flexible que el diseño basado en XML.


Google Calendar API
Para sincronizar horarios y eventos en tiempo real con los calendarios personales de profesores y alumnos.


Librerías para PDF (Apache PDFBox o iText)
Para la generación de informes e historiales en formato PDF.


Git
Sistema de control de versiones distribuido, imprescindible para el seguimiento de cambios, la colaboración y la integridad del proyecto.



Pregunta 3: Aplicación de las asignaturas al proyecto
El proyecto TimeTutor permite integrar, de forma transversal, los conocimientos adquiridos en el ciclo formativo:
Programación A y B: desarrollo de la lógica de la aplicación en Kotlin, aplicando estructuras de datos, POO (clases, herencia, polimorfismo) y control de flujo.


Bases de Datos A y B: diseño del modelo de datos a partir de un diagrama ER, implementado en Firestore y complementado con SQLite para almacenamiento local si el desarrollo de la aplicación lo exige.


Sistemas Operativos: comprensión de la ejecución de la app en Android, gestión de permisos y procesos, así como el uso de recursos del sistema.


Lenguajes de Marcas: empleo de JSON para estructurar y serializar datos, base de la comunicación con Firebase. Uso de XML dentro del lenguaje Kotlin para crear y ordenar las vistas de la aplicación.


Entornos de Desarrollo: selección de un entorno de desarrollo adecuado, en este caso Android Studio como IDE y Firebase como la parte de backend para la base de datos y la autentificación, metodologías de trabajo (Agile, Kanban), control de versiones con Git y diagramas UML para planificar la arquitectura.


Desarrollo de Interfaces: diseño de pantallas intuitivas y accesibles mediante Jetpack Compose, garantizando usabilidad y accesibilidad.


Programación de Servicios y Procesos: uso de corrutinas de Kotlin para gestionar operaciones asíncronas (consultas a Firebase, notificaciones) y evitar bloqueos en la aplicación .


Programación Multimedia y Dispositivos Móviles: integración de Firebase, configuración de Gradle, gestión de dependencias y optimización para Android. También es la base de mis conocimientos sobre Kotlin, y la programación en Android.


Sistemas de Gestión Empresarial (SGE): la propia app se plantea como un sistema de gestión de recursos, pagos y relaciones profesor–alumno.


Formación y Orientación Laboral (FOL): aplicación de principios de protección de datos (RGPD), ergonomía laboral y documentación.


Empresa e Iniciativa Emprendedora (EIE): análisis de viabilidad y modelo de negocio, explorando opciones de monetización y posibles vías de distribución.



Pregunta 4: Justificación de la elección de la temática
La elección de este proyecto responde tanto a motivos personales como académicos. Durante años he ejercido como profesor particular, y he comprobado de primera mano la dificultad de llevar un control riguroso de horarios, asistencia y pagos con herramientas no especializadas. Aunque existen aplicaciones generales, ninguna se ajusta realmente a las necesidades específicas de este ámbito.
El proyecto TimeTutor nace de esa experiencia y del deseo de ofrecer una solución útil, que pueda ser aplicada en la práctica real y que ayude a otros docentes “informales” en la misma situación.
Desde el punto de vista académico, representa una oportunidad excelente para poner en práctica los conocimientos adquiridos en el ciclo, combinando el diseño de una base de datos estructurada con el desarrollo de una aplicación móvil completa y la integración de servicios en la nube. La gestión de múltiples roles de usuario, la necesidad de interacción controlada entre profesor y alumno y la generación automatizada de informes suponen retos técnicos y organizativos que dan al proyecto un alto valor formativo.
En síntesis, TimeTutor une una necesidad real con un desafío técnico relevante, constituyendo un trabajo que no solo me permitirá demostrar competencias adquiridas, sino también aportar una herramienta con utilidad social y profesional.
