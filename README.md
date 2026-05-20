# SafeWalk – Mapa Comunitario de Jaurías Callejeras

Aplicación Android para reportar y consultar avistamientos de perros callejeros en colonias periféricas.

---

## Requisitos previos

Antes de clonar el proyecto, asegúrate de tener instalado:

- [Android Studio Meerkat (2024.3.1) o superior](https://developer.android.com/studio)
- [Git](https://git-scm.com/downloads)
- JDK 17 (incluido con Android Studio)

---

## 1. Clonar el repositorio

Abre una terminal (PowerShell o CMD) en la carpeta donde quieras guardar el proyecto y ejecuta:

```bash
git clone https://github.com/RubenAmauri/SafeWalkPrivado.git
```

Esto creará una carpeta `SafeWalkPrivado` con todo el código.

---

## 2. Configurar las claves en `local.properties`

El proyecto usa **Supabase** y **Google Maps API**. Las claves **no están en el repositorio** por seguridad — cada integrante debe agregarlas manualmente en su máquina.

Abre (o crea) el archivo `local.properties` en la **raíz del proyecto**:

```
SafeWalkPrivado/
├── app/
├── gradle/
├── local.properties   ← este archivo
└── ...
```

Agrega las siguientes líneas con los valores que te compartirá el líder del equipo:

```properties
SUPABASE_URL=https://xxxxxxxxxxxxxxxxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
MAPS_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXX
```
(Se los paso por Whatsapp)
> **Importante:** `local.properties` está en el `.gitignore` — nunca lo subas al repositorio. Si accidentalmente lo subes, avisa al líder para rotar las claves.

---

## 3. Abrir el proyecto en Android Studio

1. Abre Android Studio
2. Selecciona **File → Open**
3. Navega a la carpeta `SafeWalkPrivado` (la que se creó al clonar)
4. Selecciona la carpeta raíz donde está el `build.gradle.kts`
5. Clic en **OK**
6. Si aparece el mensaje **"Trust this project?"** → clic en **Trust**
7. Espera a que Gradle sincronice (puede tardar unos minutos la primera vez)

---

## 4. Ramas del equipo

| Integrante | Rama |
|---|---|
| Edgar Israel Nieves Bautista | `RamaEdgar` |
| Jose Francisco Nava Casillas | `RamaFrancisco` |
| Gibran Alejandro Reyes Dueñas | `RamaGibran` |
| Rubén Amauri Cervantes Salmón | `RamaRuben` |

---