# SafeWalk – Mapa Comunitario de Jaurías Callejeras

Aplicación móvil Android que permite a los peatones reportar y consultar avistamientos de jaurías de perros callejeros en colonias periféricas, con el objetivo de reducir la exposición a situaciones de riesgo físico y sanitario al transitar a pie.

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Plataforma | Android 7.0 (API 24) o superior |
| Lenguaje | Kotlin + Jetpack Compose |
| Arquitectura | MVVM + Repository Pattern |
| Base de datos | Supabase — PostgreSQL + PostGIS |
| Autenticación | Supabase Auth |
| Tiempo real | Supabase Realtime |
| Almacenamiento de fotos | Supabase Storage |
| Mapas | Google Maps Platform — Maps SDK for Android |

## Requisitos previos

- Android Studio Meerkat 2024.3.2 o superior
- JDK 17 (incluido con Android Studio)
- Git
- Credenciales de Supabase y Google Maps (ver sección siguiente)

## 1. Clonar el repositorio

```bash
git clone https://github.com/RubenAmauri/SafeWalkPrivado.git
```

## 2. Configurar credenciales

El proyecto requiere un archivo `local.properties` en la raíz del proyecto con las siguientes variables:

```
SUPABASE_URL=https://[tu-proyecto].supabase.co
SUPABASE_ANON_KEY=[tu-anon-key]
MAPS_API_KEY=[tu-api-key-de-google-maps]
```

SafeWalkPrivado/
├── app/
├── gradle/
├── local.properties   ← este archivo
└── ...

## 3. Abrir el proyecto en Android Studio

1. Abrir Android Studio.
2. Seleccionar **File → Open**.
3. Navegar a la carpeta `SafeWalkPrivado` y seleccionar la raíz del proyecto (donde está el `build.gradle.kts`).
4. Clic en **OK**.
5. Si aparece el mensaje "Trust this project?" → clic en **Trust**.
6. Esperar a que Gradle sincronice.

## 4. Compilar y ejecutar

1. Conectar un dispositivo Android con depuración USB activada, o configurar un emulador con API 24 o superior en Android Studio.
2. Seleccionar el dispositivo en la barra superior de Android Studio.
3. Hacer clic en **Run** o presionar `Shift + F10`.