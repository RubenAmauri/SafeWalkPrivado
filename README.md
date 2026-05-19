# SafeWalk – Mapa Comunitario de Jaurías Callejeras

Aplicación Android para reportar y consultar avistamientos de perros callejeros en colonias periféricas.

---

## Requisitos previos

Antes de clonar el proyecto, asegúrate de tener instalado:

- [Android Studio Meerkat (2024.3.1) o superior](https://developer.android.com/studio)
- [Git](https://git-scm.com/downloads)
- JDK 17 (incluido con Android Studio)

---

## ⚠️ Advertencia: ruta del proyecto

Gradle **no soporta caracteres especiales ni acentos** en la ruta del proyecto en Windows.  
Si tu usuario de Windows tiene acento (por ejemplo `C:\Users\Rubén\...`), el proyecto **no compilará**.

Clona el proyecto en una ruta sin caracteres especiales, por ejemplo:

```
D:\Dev\SafeWalkPrivado
```

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

## 4. Correr la app

### Opción A — Teléfono físico (recomendado)

1. En tu celular ve a **Ajustes → Acerca del teléfono**
2. Toca **Número de compilación** 7 veces hasta activar las opciones de desarrollador
3. Ve a **Ajustes → Opciones de desarrollador** y activa **Depuración USB**
4. Conecta el celular por USB a tu PC
5. Acepta el mensaje **"¿Permitir depuración USB?"** en el celular
6. En Android Studio selecciona tu dispositivo en el menú desplegable y presiona ▶️

> Si el celular no aparece, desliza la barra de notificaciones, toca la notificación USB y selecciona **Transferencia de archivos (MTP)**.

### Opción B — Emulador

1. En Android Studio ve a **Device Manager** (ícono de teléfono en la barra lateral)
2. Clic en **+** → **Create Virtual Device**
3. Selecciona **Pixel 7** → **API 33 (Android 13)** → **Finish**
4. Inicia el emulador desde Device Manager antes de correr la app
5. Presiona ▶️ en Android Studio

> El emulador requiere virtualización habilitada en tu PC. Puedes verificarlo en el Administrador de tareas → Rendimiento → CPU → Virtualización.

---

## 5. Flujo de trabajo con Git

Cada integrante trabaja en su propia rama. **Nunca hagas commits directo a `main`.**

### Antes de empezar a trabajar (siempre)

```bash
git pull
```

### Al terminar tu sesión de trabajo

```bash
git add .
git commit -m "descripción breve de lo que hiciste"
git push
```

### Si es la primera vez que subes tu rama

```bash
git push -u origin nombre-de-tu-rama
```

### Ver en qué rama estás

```bash
git status
```

---

## 6. Ramas del equipo

| Integrante | Rama |
|---|---|
| Edgar Israel Nieves Bautista | `RamaEdgar` |
| Jose Francisco Nava Casillas | `RamaFrancisco` |
| Gibran Alejandro Reyes Dueñas | `RamaGibran` |
| Rubén Amauri Cervantes Salmón | `RamaRuben` |

---