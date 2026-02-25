# Episodeo — Gestor Personal de Series

<p align="center">
  <img src="docs/logo.svg" alt="Episodeo Logo" width="120"/>
</p>

<p align="center">
  <strong>Tu gestor personal de series para Android</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-1.0-blue.svg" alt="Version">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
  <img src="https://github.com/ismailhaddouche/episodeo/actions/workflows/android.yml/badge.svg" alt="Build Status">
</p>

Episodeo es una aplicación Android nativa para llevar el seguimiento de tus series de televisión. Diseñada bajo principios de **Clean Architecture** y **MVVM**, ofrece una solución robusta para entusiastas de las series que buscan una experiencia premium, optimizada y _ad-free_.

**Autor:** Ismail Haddouche Rhali

---


## ¿Qué hace Episodeo?

- Busca cualquier serie usando la base de datos de TMDB (más de 800.000 títulos)
- Organiza tus series por estado: **Viendo**, **Pendiente**, **Terminada** o **Abandonada**
- Puntúa las series que has terminado con una nota del 1 al 10
- Crea **listas personalizadas** para agrupar series como quieras
- **Comparte tus listas** con un código único y sigue las listas de otras personas
- Tus datos se sincronizan entre dispositivos gracias a Firebase
- Funciona **offline**: la información ya vista se guarda localmente con Room
- Tema oscuro y claro configurable

---

## Capturas de Pantalla

<table>
  <tr>
    <td align="center">
      <img src="docs/screenshots/captura_boton_login.jpeg" width="180" alt="Login"/><br/>
      <em>Inicio de sesión</em>
    </td>
    <td align="center">
      <img src="docs/screenshots/captura_main.jpeg" width="180" alt="Pantalla principal"/><br/>
      <em>Dashboard principal</em>
    </td>
    <td align="center">
      <img src="docs/screenshots/captura_listado_busqueda.jpeg" width="180" alt="Búsqueda"/><br/>
      <em>Búsqueda de series</em>
    </td>
    <td align="center">
      <img src="docs/screenshots/captura_info_serie.jpeg" width="180" alt="Ficha serie"/><br/>
      <em>Ficha de serie</em>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/screenshots/captura_viendo.jpeg" width="180" alt="Viendo"/><br/>
      <em>Series: Viendo</em>
    </td>
    <td align="center">
      <img src="docs/screenshots/captura_pendientes.jpeg" width="180" alt="Pendientes"/><br/>
      <em>Series: Pendiente</em>
    </td>
    <td align="center">
      <img src="docs/screenshots/captura_terminadas.jpeg" width="180" alt="Terminadas"/><br/>
      <em>Series: Terminada</em>
    </td>
    <td align="center">
      <img src="docs/screenshots/captura_abandonadas.jpeg" width="180" alt="Abandonadas"/><br/>
      <em>Series: Abandonada</em>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/screenshots/captura_listas_personalizadas.jpeg" width="180" alt="Mis Listas"/><br/>
      <em>Mis Listas</em>
    </td>
    <td align="center">
      <img src="docs/screenshots/captura_listado_lista_personalizada.jpeg" width="180" alt="Contenido lista"/><br/>
      <em>Contenido de lista</em>
    </td>
    <td align="center">
      <img src="docs/screenshots/captura_listas_seguidas.jpeg" width="180" alt="Listas seguidas"/><br/>
      <em>Listas seguidas</em>
    </td>
    <td align="center">
      <img src="docs/screenshots/captura_listado_lista_seguidas.jpeg" width="180" alt="Contenido lista seguida"/><br/>
      <em>Contenido lista seguida</em>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/screenshots/captura_seguir_codigo.jpeg" width="180" alt="Seguir por código"/><br/>
      <em>Seguir lista por código</em>
    </td>
    <td align="center">
      <img src="docs/screenshots/captura_compartir_codigo.jpeg" width="180" alt="Compartir"/><br/>
      <em>Compartir lista</em>
    </td>
    <td align="center">
      <img src="docs/screenshots/captura_menu.jpeg" width="180" alt="Menú lateral"/><br/>
      <em>Menú lateral</em>
    </td>
    <td align="center">
      <img src="docs/screenshots/captura_ajustes.jpeg" width="180" alt="Ajustes"/><br/>
      <em>Ajustes</em>
    </td>
  </tr>
</table>

---

## Stack Tecnológico

<p align="left">
  <img src="https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-%234285F4.svg?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Firebase-%23039BE5.svg?style=for-the-badge&logo=firebase" alt="Firebase">
  <img src="https://img.shields.io/badge/Retrofit-%2340aeed.svg?style=for-the-badge" alt="Retrofit">
  <img src="https://img.shields.io/badge/Hilt-%23FFD600.svg?style=for-the-badge" alt="Hilt">
</p>

| Capa | Tecnología |
|------|-----------|
| **Lenguaje** | Kotlin Coroutines + Flow |
| **UI Framework** | Jetpack Compose + Material Design 3 |
| **Arquitectura** | MVVM + Repository Pattern + Clean Architecture |
| **Dependency Injection** | Hilt (Dagger) |
| **Cloud Storage** | Firebase Firestore (Real-time Sync) |
| **Auth** | Firebase Auth (Google OAuth 2.0) |
| **Local Persistence** | Room (SQLite abstraction) |
| **Networking** | Retrofit 2 + Moshi (JSON Parsing) |
| **Image Loading** | Coil (Compose optimized) |
| **Quality Assurance** | JUnit 4, Compose Testing |
| **Automation** | GitHub Actions (CI/CD) |

---

## Diagramas

<table>
  <tr>
    <td align="center">
      <img src="docs/diagrams/Diagrama_uso.png" width="300" alt="Diagrama de casos de uso"/><br/>
      <em>Casos de Uso</em>
    </td>
    <td align="center">
      <img src="docs/diagrams/Diagrama_flujo.png" width="300" alt="Diagrama de flujo"/><br/>
      <em>Flujo de la Aplicación</em>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/diagrams/Diagrama_ER.png" width="300" alt="Diagrama ER Firestore"/><br/>
      <em>Modelo de Datos Firestore</em>
    </td>
    <td align="center">
      <img src="docs/diagrams/diagrama_room.png" width="300" alt="Esquema Room"/><br/>
      <em>Tablas Room (caché local)</em>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/diagrams/Diagrama_clases_1.png" width="300" alt="Diagrama de clases 1"/><br/>
      <em>Diagrama de Clases — Modelos</em>
    </td>
    <td align="center">
      <img src="docs/diagrams/Diagrama_clases_2.png" width="300" alt="Diagrama de clases 2"/><br/>
      <em>Diagrama de Clases — Repositorio</em>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/diagrams/Diagrama_clases_3.png" width="300" alt="Diagrama de clases 3"/><br/>
      <em>Diagrama de Clases — ViewModel</em>
    </td>
    <td align="center">
      <img src="docs/diagrams/Diagrama_nosql.png" width="300" alt="Estructura NoSQL"/><br/>
      <em>Estructura NoSQL Firestore</em>
    </td>
  </tr>
</table>

---

## Arquitectura

La app sigue el patrón **MVVM** recomendado por Google para Android moderno.

```
┌─────────────────────────────────────────────────────┐
│                   Capa de Vista (UI)                 │
│         Jetpack Compose — Screens + Components       │
│  LoginScreen · HomeScreen · MyListsScreen · ...      │
└────────────────────────┬────────────────────────────┘
                         │ observa StateFlow / State
┌────────────────────────▼────────────────────────────┐
│                    ViewModel                         │
│              HomeViewModel (Hilt)                    │
│  Estado de UI · Lógica de negocio · Coroutines       │
└────────────────────────┬────────────────────────────┘
                         │ llama a
┌────────────────────────▼────────────────────────────┐
│                  Repository                          │
│              SeriesRepository (Singleton)            │
│                                                      │
│   ┌─────────────────┐    ┌──────────────────────┐   │
│   │  Firebase        │    │  Room (Cache Local)   │   │
│   │  Firestore       │    │  SeriesDao            │   │
│   │  Auth            │    │  AppDatabase          │   │
│   └─────────────────┘    └──────────────────────┘   │
│                                                      │
│   ┌─────────────────────────────────────────────┐   │
│   │  TMDB API (Retrofit)                         │   │
│   │  TmdbApiService · TmdbApi                   │   │
│   └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

### Estrategia de datos (Online/Offline)

El `Repository` implementa una estrategia **Network-First con fallback local**:

1. **Online:** consulta Firestore / TMDB → guarda el resultado en Room → devuelve los datos.
2. **Offline:** la red falla → devuelve los datos de Room (caché).

En el caso de los datos de usuario, **Cloud Prevails**: Firestore sobreescribe Room en cada sincronización.

---

## Estructura del Proyecto

```
app/src/main/java/com/haddouche/episodeo/
│
├── components/             Componentes Compose reutilizables
│   ├── AddToListDialog     Diálogo para añadir serie a una lista
│   ├── FollowListDialog    Diálogo para seguir lista por código
│   ├── HomeTopAppBar       Barra superior con búsqueda
│   ├── SavedSeriesItem     Ítem de serie en lista
│   ├── SearchBar           Barra de búsqueda
│   ├── SearchResultItem    Ítem de resultado de búsqueda
│   ├── SeriesCarousel      Carrusel horizontal de series
│   ├── SeriesDetailDialog  Dialog de detalle completo de serie
│   └── SideMenu            Menú lateral de navegación
│
├── data/local/             Capa de persistencia local (Room)
│   ├── AppDatabase         Definición de la base de datos Room
│   ├── Converters          Type converters para Room
│   ├── SeriesDao           DAO con todas las consultas
│   └── entities/Entities   Entidades de Room
│
├── di/
│   └── DatabaseModule      Módulo Hilt para proveer el DAO
│
├── models/                 Modelos de datos
│   ├── AppList             Clase sellada: SystemList + CustomList
│   ├── FollowedList        Referencia a lista seguida
│   ├── ShareCode           Código de compartición de lista
│   ├── User                Datos del usuario autenticado
│   ├── UserList            Lista personalizada del usuario
│   ├── UserSeries          Estado + puntuación de una serie
│   └── tmdb/               Modelos de respuesta de la API TMDB
│       ├── TmdbCredits     Créditos (reparto y equipo)
│       ├── TmdbSearchResponse Respuesta de búsqueda paginada
│       ├── TmdbSeries      Serie con todos sus campos
│       └── WatchProvidersResponse Plataformas de streaming
│
├── navigation/             Sistema de navegación
│   ├── AppNavigation       Grafo de navegación raíz
│   ├── AppRoutes           Rutas definidas como sealed class
│   └── InternalNavigation  Navegación interna del MainScreen
│
├── network/
│   └── TmdbApiService      Interfaz Retrofit + singleton TmdbApi
│
├── repository/
│   └── SeriesRepository    Único punto de acceso a datos
│
├── screens/                Pantallas de la app
│   ├── LoginScreen         Pantalla de inicio de sesión
│   ├── MainScreen          Contenedor principal con navegación
│   ├── HomeScreen          Dashboard con carruseles por estado
│   ├── MyListsScreen       Mis listas personalizadas
│   ├── FollowedListsScreen Listas que estoy siguiendo
│   ├── ListDetailScreen    Detalle de una lista concreta
│   ├── StatusScreen        Series filtradas por estado
│   └── SettingsScreen      Tema, gestión de cuenta
│
├── ui/theme/               Tema de Material Design 3
│   ├── Color               Paleta de colores
│   ├── Theme               Configuración del tema claro/oscuro
│   └── Type                Tipografía
│
├── viewmodels/
│   └── HomeViewModel       ViewModel principal (Hilt)
│
├── EpisodeoApp             Application class con @HiltAndroidApp
└── MainActivity            Activity principal con @AndroidEntryPoint
```

---

## Modelo de Datos (Firestore)

```
usuarios/{uid}
│   ├── isDarkTheme: Boolean
│   │
│   ├── series/{tmdbId}
│   │   ├── seriesStatus: String   ("watching" | "pending" | "completed" | "dropped")
│   │   └── seriesRating: Int?     (1-10, opcional en cualquier estado)
│   │
│   ├── mis_listas/{listId}
│   │   ├── id: String
│   │   ├── name: String
│   │   ├── ownerId: String
│   │   ├── isPublic: Boolean
│   │   └── seriesIds: List<Int>
│   │
│   └── listas_seguidas/{listId}
│       ├── listId: String
│       ├── ownerId: String
│       └── listName: String

codigos_compartir/{code}
    ├── code: String
    ├── ownerId: String
    └── listId: String
```

**Cache local (Room):**

| Tabla | Campos | Propósito |
|-------|--------|-----------|
| `user_series` | seriesId, seriesStatus, seriesRating, userId | Estado offline del usuario |
| `user_lists` | id, name, ownerId, isPublic, seriesIds, userId | Listas offline |
| `followed_lists` | listId, ownerId, listName, userId | Referencias a listas seguidas |
| `cached_series` | id, name, posterPath, synopsis, releaseDate | Metadatos TMDB cacheados |

---

## Navegación

```
LoginScreen
    └── MainScreen (contenedor)
            ├── HomeScreen          /home
            ├── MyListsScreen       /mis_listas
            ├── FollowedListsScreen /listas_seguidas
            ├── StatusScreen        /estado/{statusName}
            ├── ListDetailScreen    /lista/{listName}
            └── SettingsScreen      /ajustes
```

---

## Configuración del Entorno

### Requisitos

- Android Studio Hedgehog o superior
- JDK 11
- Android SDK: `compileSdk 36`, `targetSdk 34`, `minSdk 24`
- Cuenta de Firebase
- API key de TMDB (gratuita)

### 1. Clonar el repositorio

```bash
git clone https://github.com/ismailhaddouche/episodeo.git
cd episodeo
```

### 2. Configurar Firebase

1. Crear un proyecto en [Firebase Console](https://console.firebase.google.com)
2. Añadir una app Android con el paquete `com.haddouche.episodeo`
3. Descargar el archivo `google-services.json` y colocarlo en `app/`
4. Habilitar en Firebase:
   - **Authentication → Proveedor Google**
   - **Firestore Database** (modo producción o prueba)
5. Añadir el SHA-1 de tu keystore de debug en la configuración de la app de Firebase:
   ```bash
   ./gradlew signingReport
   ```

### 3. Configurar la API key de TMDB

1. Registrarse en [themoviedb.org](https://www.themoviedb.org) y obtener una API key gratuita
2. Añadir la clave al archivo `local.properties` en la raíz del proyecto:
   ```properties
   CLAVE_API_TMDB=tu_clave_aqui
   ```

> `local.properties` está en `.gitignore` — nunca se sube al repositorio.

### 4. Compilar y ejecutar

Abre el proyecto en Android Studio, sincroniza Gradle y ejecuta sobre un emulador o dispositivo físico (Android 7.0+).

---

## Pruebas

### Tests unitarios

```bash
./gradlew test
```

### Tests unitarios con reporte HTML

```bash
./gradlew test
# Abre el reporte:
start app/build/reports/tests/testDebugUnitTest/index.html
```

### Cobertura con JaCoCo

```bash
./gradlew testDebugUnitTest jacocoTestReport
# Abre el reporte:
start app/build/reports/jacoco/testDebugUnitTest/html/index.html
```

### Ejecutar tests por paquete

```bash
# Solo modelos
./gradlew test --tests "com.haddouche.episodeo.models.*"

# Solo repository
./gradlew test --tests "com.haddouche.episodeo.repository.*"

# Solo ViewModel
./gradlew test --tests "com.haddouche.episodeo.viewmodels.*"
```

### Cobertura esperada

Actualmente, el enfoque principal de las pruebas recae sobre los modelos puros y utilidades (debido a limitaciones técnicas recientes con el entorno de testing y Firebase).

| Paquete | Cobertura |
|---------|-----------|
| `models` | 100% |

---

## Generar APK

```bash
# Debug (para pruebas)
./gradlew assembleDebug
# APK en: app/build/outputs/apk/debug/app-debug.apk

# Release (firmado)
./gradlew assembleRelease
```

Para firmar el APK de release, configurar el keystore en `app/build.gradle.kts`.

---

## Vías de Mejora Futuras

- **Notificaciones Push** — avisar cuando se estrena una nueva temporada
- **Soporte Kotlin Multiplatform** — versión iOS con la misma lógica
- **Funcionalidad social** — compartir listas públicas con enlace directo
- **Modo offline completo** — guardar contenido completo de listas seguidas localmente

---

## Licencia

Este proyecto está publicado bajo la licencia [MIT](./LICENSE).

---

## Documentación

| Documento | Descripción |
|-----------|-------------|
| [docs/SETUP.md](docs/SETUP.md) | Instalación del APK + configuración del entorno de desarrollo |
| [docs/USER_GUIDE.md](docs/USER_GUIDE.md) | Guía de usuario completa con capturas de pantalla |
| [docs/TESTING.md](docs/TESTING.md) | Suite de pruebas, comandos y cobertura |
| [CHANGELOG.md](CHANGELOG.md) | Historial de cambios |

---

## Reconocimientos

- [TMDB](https://www.themoviedb.org) — API de series y películas
- [Firebase](https://firebase.google.com) — Autenticación y base de datos en la nube
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — UI toolkit moderno de Android
